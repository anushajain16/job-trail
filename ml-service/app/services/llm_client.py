"""LLM-backed job-posting extraction, behind an interface with two
implementations:

- ``OpenAILLMClient`` — the real thing, used whenever OPENAI_API_KEY is set.
- ``StubLLMClient`` — a deterministic, dependency-free heuristic extractor
  used when no key is configured, so the service starts and /parse still
  returns a (lower-confidence) result instead of failing outright. This
  mirrors the app-level graceful-degradation story: if this whole service
  is down, the core app falls back to manual entry; if just the LLM key is
  missing, /parse itself degrades instead of 500ing.

Both raise ``LLMError`` on failure so the router has one exception type to
catch regardless of which backend is active.
"""

import json
import re
from abc import ABC, abstractmethod

from openai import AsyncOpenAI, OpenAIError
from pydantic import BaseModel, Field

from app.services.skills_vocab import SKILL_VOCAB
from app.services.text_matching import word_in_text


class LLMError(Exception):
    """Raised when extraction fails for any reason — a bad LLM response, a
    network error, or (for the stub) nothing at all going wrong, this type
    just never gets raised there."""


class LLMExtraction(BaseModel):
    company: str | None = None
    role: str | None = None
    location: str | None = None
    employment_type: str | None = None
    seniority: str | None = None
    salary_min: float | None = None
    salary_max: float | None = None
    currency: str | None = None
    required_skills: list[str] = Field(default_factory=list)
    nice_to_have_skills: list[str] = Field(default_factory=list)
    summary: str | None = None
    confidence: float = 0.0


class LLMClient(ABC):
    @abstractmethod
    async def extract(self, text: str) -> LLMExtraction: ...


_JOB_POSTING_JSON_SCHEMA = {
    "name": "job_posting_extraction",
    "strict": True,
    "schema": {
        "type": "object",
        "additionalProperties": False,
        "properties": {
            "company": {"type": ["string", "null"]},
            "role": {"type": ["string", "null"]},
            "location": {"type": ["string", "null"]},
            "employment_type": {"type": ["string", "null"]},
            "seniority": {"type": ["string", "null"]},
            "salary_min": {"type": ["number", "null"]},
            "salary_max": {"type": ["number", "null"]},
            "currency": {"type": ["string", "null"]},
            "required_skills": {"type": "array", "items": {"type": "string"}},
            "nice_to_have_skills": {"type": "array", "items": {"type": "string"}},
            "summary": {"type": ["string", "null"]},
            "confidence": {
                "type": "number",
                "description": "0 to 1: how confident the extraction is, given how much of the schema the source text actually supported.",
            },
        },
        "required": [
            "company", "role", "location", "employment_type", "seniority",
            "salary_min", "salary_max", "currency", "required_skills",
            "nice_to_have_skills", "summary", "confidence",
        ],
    },
}

_SYSTEM_PROMPT = (
    "You extract structured job-posting data from raw scraped or pasted text. "
    "Fill every schema field you can support from the text; leave a field null "
    "(not a guess) when the text doesn't say. required_skills are things the "
    "posting states as mandatory; nice_to_have_skills are explicitly optional/bonus. "
    "Set confidence to your honest estimate of extraction reliability — near 1.0 for "
    "a clean, complete posting, lower for a truncated or ambiguous one."
)


class OpenAILLMClient(LLMClient):
    """Despite the name, this talks to any OpenAI-compatible chat-completions
    endpoint, not just OpenAI itself — see Settings.openai_base_url. The one
    thing worth knowing before pointing it elsewhere: the strict json_schema
    mode this relies on (server-enforced "the response matches this schema,
    full stop") is an OpenAI-specific guarantee. A compatible provider (e.g.
    Gemini) may honor it approximately rather than exactly, so a field
    landing null/mistyped despite being right there in the text is more
    likely off-OpenAI than on it."""

    def __init__(self, api_key: str, model: str, timeout_seconds: float = 30.0, base_url: str | None = None) -> None:
        self._client = AsyncOpenAI(api_key=api_key, timeout=timeout_seconds, base_url=base_url)
        self._model = model

    async def extract(self, text: str) -> LLMExtraction:
        try:
            response = await self._client.chat.completions.create(
                model=self._model,
                messages=[
                    {"role": "system", "content": _SYSTEM_PROMPT},
                    {"role": "user", "content": text},
                ],
                response_format={"type": "json_schema", "json_schema": _JOB_POSTING_JSON_SCHEMA},
            )
        except OpenAIError as exc:
            raise LLMError(f"OpenAI extraction call failed: {exc}") from exc

        content = response.choices[0].message.content
        if not content:
            raise LLMError("OpenAI returned an empty extraction response")

        try:
            data = json.loads(content)
            return LLMExtraction(**data)
        except (json.JSONDecodeError, ValueError) as exc:
            raise LLMError(f"OpenAI returned an unparseable extraction: {exc}") from exc


# --- heuristic fallback, no external calls -----------------------------------

_SALARY_RANGE_RE = re.compile(
    r"(?P<currency>[$€£₹]|USD|EUR|GBP|INR)\s?"
    r"(?P<min>\d{2,3}(?:[,.]\d{3})*(?:k)?)"
    r"\s?(?:-|–|to)\s?"
    r"(?:[$€£₹]|USD|EUR|GBP|INR)?\s?"
    r"(?P<max>\d{2,3}(?:[,.]\d{3})*(?:k)?)",
    re.IGNORECASE,
)
_CURRENCY_SYMBOLS = {"$": "USD", "€": "EUR", "£": "GBP", "₹": "INR"}
_EMPLOYMENT_TYPES = ("full-time", "part-time", "contract", "internship", "freelance", "temporary")
_SENIORITY_LEVELS = ("intern", "junior", "entry-level", "mid-level", "senior", "staff", "principal", "lead")
# Matches the "Company: X" line scraper.fetch_visible_text prepends when it
# finds a JobPosting JSON-LD block or og:site_name tag — the one company
# signal reliable enough to trust without an LLM (see that function's
# _extract_company_hint). Anchored to the very start of the text: a
# "Company:" mention buried in the body isn't the same guarantee.
_COMPANY_HINT_RE = re.compile(r"\A\s*Company:\s*(.+)")


def _to_number(raw: str) -> float:
    raw = raw.strip().lower()
    if raw.endswith("k"):
        return float(raw[:-1].replace(",", "")) * 1_000
    return float(raw.replace(",", ""))


class StubLLMClient(LLMClient):
    """Regex + keyword-vocabulary extraction. No network call, fully
    deterministic — used as the no-API-key runtime fallback and as the
    default in tests, so the suite never needs a real OpenAI key."""

    async def extract(self, text: str) -> LLMExtraction:
        lower = text.lower()
        lines = [line.strip() for line in text.splitlines() if line.strip()]

        salary_min = salary_max = None
        currency = None
        salary_match = _SALARY_RANGE_RE.search(text)
        if salary_match:
            salary_min = _to_number(salary_match.group("min"))
            salary_max = _to_number(salary_match.group("max"))
            currency = _CURRENCY_SYMBOLS.get(salary_match.group("currency"), salary_match.group("currency").upper())

        found_skills = [skill for skill in SKILL_VOCAB if word_in_text(skill, lower)]

        employment_type = next((t for t in _EMPLOYMENT_TYPES if t in lower), None)
        seniority = next((s for s in _SENIORITY_LEVELS if s in lower), None)

        # There's no reliable way to guess a company name from body text
        # alone, so this only trusts the explicit "Company: X" line the
        # scraper prepends from structured markup (see _COMPANY_HINT_RE) —
        # never a guess, same as the LLM path's own instruction to leave
        # fields null rather than invent them. Pasted text (no scrape step)
        # never has this line, so company stays null there, same as before.
        company_match = _COMPANY_HINT_RE.match(text)
        company = company_match.group(1).strip()[:255] if company_match else None

        # first non-empty line still not consumed by the company hint above
        # is a reasonable role/title guess for most postings and job boards.
        role_lines = lines[1:] if company_match and lines else lines
        role = role_lines[0][:120] if role_lines else None

        found_fields = sum(
            1 for v in (company, role, employment_type, seniority, salary_min, currency) if v is not None
        ) + (1 if found_skills else 0)
        confidence = round(min(0.6, 0.15 * found_fields), 2)  # heuristic extraction never claims high confidence

        return LLMExtraction(
            company=company,
            role=role,
            location=None,
            employment_type=employment_type,
            seniority=seniority,
            salary_min=salary_min,
            salary_max=salary_max,
            currency=currency,
            required_skills=found_skills,
            nice_to_have_skills=[],
            summary=role_lines[0][:200] if role_lines else None,
            confidence=confidence,
        )

