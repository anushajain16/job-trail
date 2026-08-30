"""LLM-backed resume parsing: raw resume text -> a structured
``ResumeProfile`` (skills, years of experience, past roles, seniority).
Same two-implementation shape as ``llm_client.py``'s job-posting extraction
(``OpenAIResumeProfileClient`` behind a real key, ``StubResumeProfileClient``
as the dependency-free heuristic fallback/test default) — kept as its own
module rather than folded into ``llm_client.py`` since it's a distinct
extraction target with its own schema and prompt, not another mode of the
same one.
"""

import json
import re
from abc import ABC, abstractmethod

from openai import AsyncOpenAI, OpenAIError
from pydantic import BaseModel, Field

from app.services.skills_vocab import SKILL_VOCAB
from app.services.text_matching import word_in_text


class ResumeProfileError(Exception):
    """Raised when resume-profile extraction fails — a bad LLM response or
    a network error. Mirrors ``LLMError``'s role for job-posting extraction."""


class ResumeProfileExtraction(BaseModel):
    skills: list[str] = Field(default_factory=list)
    years_experience: float | None = None
    roles: list[str] = Field(default_factory=list)
    seniority: str | None = None
    summary: str | None = None
    confidence: float = 0.0


class ResumeProfileClient(ABC):
    @abstractmethod
    async def extract(self, resume_text: str) -> ResumeProfileExtraction: ...


_RESUME_PROFILE_JSON_SCHEMA = {
    "name": "resume_profile_extraction",
    "strict": True,
    "schema": {
        "type": "object",
        "additionalProperties": False,
        "properties": {
            "skills": {"type": "array", "items": {"type": "string"}},
            "years_experience": {"type": ["number", "null"]},
            "roles": {"type": "array", "items": {"type": "string"}},
            "seniority": {"type": ["string", "null"]},
            "summary": {"type": ["string", "null"]},
            "confidence": {
                "type": "number",
                "description": "0 to 1: how confident the extraction is, given how much of the schema the resume text actually supported.",
            },
        },
        "required": ["skills", "years_experience", "roles", "seniority", "summary", "confidence"],
    },
}

_SYSTEM_PROMPT = (
    "You extract a structured candidate profile from raw resume text. "
    "skills is a flat list of concrete technical/professional skills the resume "
    "actually demonstrates (not aspirational or soft skills). years_experience is "
    "the candidate's total professional experience in years, estimated from dates "
    "if not stated directly — null if the text gives no basis to estimate it. "
    "roles is past job titles, most recent first. Set confidence to your honest "
    "estimate of extraction reliability."
)


class OpenAIResumeProfileClient(ResumeProfileClient):
    """Same "any OpenAI-compatible endpoint" caveat as llm_client.py's
    OpenAILLMClient — see Settings.openai_base_url."""

    def __init__(self, api_key: str, model: str, timeout_seconds: float = 30.0, base_url: str | None = None) -> None:
        self._client = AsyncOpenAI(api_key=api_key, timeout=timeout_seconds, base_url=base_url)
        self._model = model

    async def extract(self, resume_text: str) -> ResumeProfileExtraction:
        try:
            response = await self._client.chat.completions.create(
                model=self._model,
                messages=[
                    {"role": "system", "content": _SYSTEM_PROMPT},
                    {"role": "user", "content": resume_text},
                ],
                response_format={"type": "json_schema", "json_schema": _RESUME_PROFILE_JSON_SCHEMA},
            )
        except OpenAIError as exc:
            raise ResumeProfileError(f"OpenAI resume extraction call failed: {exc}") from exc

        content = response.choices[0].message.content
        if not content:
            raise ResumeProfileError("OpenAI returned an empty resume extraction response")

        try:
            data = json.loads(content)
            return ResumeProfileExtraction(**data)
        except (json.JSONDecodeError, ValueError) as exc:
            raise ResumeProfileError(f"OpenAI returned an unparseable resume extraction: {exc}") from exc


# --- heuristic fallback, no external calls -----------------------------------

_YEARS_EXPERIENCE_RE = re.compile(r"(\d+(?:\.\d+)?)\+?\s*years?(?:\s+of)?\s+(?:professional\s+)?experience", re.IGNORECASE)

# A resume's role lines are usually short, title-cased, and not full
# sentences — filtering to those out of the first handful of non-empty
# lines is a rough but dependency-free stand-in for real section parsing.
_TITLE_LINE_RE = re.compile(r"^[A-Z][A-Za-z0-9/&.,\- ]{2,60}$")
_SENIORITY_LEVELS = ("intern", "junior", "entry-level", "mid-level", "senior", "staff", "principal", "lead")


class StubResumeProfileClient(ResumeProfileClient):
    """Regex + keyword-vocabulary extraction — no network call, fully
    deterministic. The no-API-key runtime fallback and the test default,
    same role ``StubLLMClient`` plays for job-posting extraction."""

    async def extract(self, resume_text: str) -> ResumeProfileExtraction:
        lower = resume_text.lower()
        lines = [line.strip() for line in resume_text.splitlines() if line.strip()]

        skills = [skill for skill in SKILL_VOCAB if word_in_text(skill, lower)]

        years_experience = None
        years_match = _YEARS_EXPERIENCE_RE.search(resume_text)
        if years_match:
            years_experience = float(years_match.group(1))

        roles = [line[:120] for line in lines[:8] if _TITLE_LINE_RE.match(line)][:5]

        seniority = next((s for s in _SENIORITY_LEVELS if s in lower), None)

        found_fields = sum(1 for v in (years_experience, seniority) if v is not None) + (1 if skills else 0) + (
            1 if roles else 0
        )
        confidence = round(min(0.6, 0.15 * found_fields), 2)  # heuristic extraction never claims high confidence

        return ResumeProfileExtraction(
            skills=skills,
            years_experience=years_experience,
            roles=roles,
            seniority=seniority,
            summary=lines[0][:200] if lines else None,
            confidence=confidence,
        )
