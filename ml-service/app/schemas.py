"""Pydantic v2 request/response models — the wire contract for all three
endpoints. Kept in one module since the service is small and every schema
here is public API surface, not an internal implementation detail."""

from typing import Literal

from pydantic import BaseModel, Field, model_validator


# ---------------------------------------------------------------------------
# /parse
# ---------------------------------------------------------------------------

class ParseRequest(BaseModel):
    """Exactly one of ``url`` or ``text`` must be given. ``url`` is scraped
    first; ``text`` (e.g. a pasted job description) skips scraping entirely
    — both funnel into the same LLM extraction step."""

    url: str | None = Field(default=None, description="Job posting URL to scrape.")
    text: str | None = Field(default=None, description="Raw job posting text, if already in hand.")

    @model_validator(mode="after")
    def _exactly_one_source(self) -> "ParseRequest":
        has_url = bool(self.url and self.url.strip())
        has_text = bool(self.text and self.text.strip())
        if has_url == has_text:
            raise ValueError("Provide exactly one of 'url' or 'text'.")
        return self


class ParsedJobPosting(BaseModel):
    company: str | None = None
    role: str | None = None
    location: str | None = None
    employment_type: str | None = Field(default=None, description="e.g. full-time, contract, internship")
    seniority: str | None = Field(default=None, description="e.g. junior, mid, senior, staff")
    salary_min: float | None = None
    salary_max: float | None = None
    currency: str | None = None
    required_skills: list[str] = Field(default_factory=list)
    nice_to_have_skills: list[str] = Field(default_factory=list)
    summary: str | None = Field(default=None, description="One or two sentence summary of the role.")


class ParseResponse(BaseModel):
    source: Literal["url", "text"]
    source_url: str | None = None
    parsed: ParsedJobPosting
    confidence: float = Field(ge=0.0, le=1.0, description="Extraction confidence, 0 (unreliable) to 1 (high).")


# ---------------------------------------------------------------------------
# /match
# ---------------------------------------------------------------------------

class MatchRequest(BaseModel):
    resume_text: str = Field(min_length=1)
    job_description_text: str = Field(min_length=1)
    # Usually the ParsedJobPosting.required_skills from a prior /parse call —
    # passing it skips this service re-deriving skills from raw JD text.
    required_skills: list[str] | None = None


class MatchResponse(BaseModel):
    match_score: float = Field(ge=0.0, le=1.0, description="Embedding cosine similarity, resume vs. job description.")
    matched_skills: list[str]
    missing_skills: list[str]
    considered_skills: list[str] = Field(description="Full skill set the score/gap list was computed against.")


# ---------------------------------------------------------------------------
# /profile
# ---------------------------------------------------------------------------

class ProfileRequest(BaseModel):
    resume_text: str = Field(min_length=1)


class ResumeProfile(BaseModel):
    """The structured shape a resume gets parsed into, once — this is what
    the Java side persists and what every /score call for that user reuses,
    instead of re-sending the raw resume text (and re-running extraction)
    on every scoring request."""

    skills: list[str] = Field(default_factory=list)
    years_experience: float | None = Field(default=None, description="Total years of professional experience.")
    roles: list[str] = Field(default_factory=list, description="Past job titles, most recent first.")
    seniority: str | None = Field(default=None, description="e.g. junior, mid, senior, staff")
    summary: str | None = Field(default=None, description="One or two sentence summary of the candidate.")


class ProfileResponse(BaseModel):
    profile: ResumeProfile
    confidence: float = Field(ge=0.0, le=1.0, description="Extraction confidence, 0 (unreliable) to 1 (high).")


# ---------------------------------------------------------------------------
# /score
# ---------------------------------------------------------------------------

class ScoreRequest(BaseModel):
    profile: ResumeProfile
    job_description_text: str = Field(min_length=1)
    # Same deal as MatchRequest.required_skills: usually the caller's prior
    # /parse result: passing it skips re-deriving skills from raw JD text.
    required_skills: list[str] | None = None


class ScoreResponse(BaseModel):
    match_pct: float = Field(ge=0.0, le=1.0, description="Embedding cosine similarity, profile vs. job description.")
    matched_skills: list[str]
    missing_skills: list[str]
    considered_skills: list[str] = Field(description="Full skill set the score/gap list was computed against.")


# ---------------------------------------------------------------------------
# /health
# ---------------------------------------------------------------------------

class HealthResponse(BaseModel):
    status: Literal["ok"] = "ok"
    service: str
    version: str
