"""Entry point — ``uvicorn app.main:app``.

This service is intentionally thin: stateless endpoints, no DB, no auth of
its own. It's reached only from the Spring Boot API over the internal
Docker network in every real deploy; auth (if any is ever added at this
boundary) belongs to that network boundary, not to this process.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import health, match, parse, profile, score

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version=settings.version,
    description=(
        "Stateless parse & match service for JobTrail: scrape+LLM job-posting "
        "extraction, resume/JD embedding match, resume-profile extraction, and "
        "profile/JD scoring. No database, no auth of its own — the Spring Boot "
        "API is the only public surface, holds the persisted results, and calls "
        "this internally; if this service is unreachable, /parse's callers fall "
        "back to manual entry, and /profile+/score's callers surface the failure "
        "(there's no meaningful fallback for a match score)."
    ),
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(parse.router)
app.include_router(match.router)
app.include_router(profile.router)
app.include_router(score.router)
