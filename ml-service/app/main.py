"""Entry point — ``uvicorn app.main:app``.

This service is intentionally thin: stateless endpoints, no DB, no auth of
its own. It's reached only from the Spring Boot API over the internal
Docker network in every real deploy; auth (if any is ever added at this
boundary) belongs to that network boundary, not to this process.
"""

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.deps import get_embedding_provider
from app.routers import health, match, parse, profile, score
from app.services.embeddings import SentenceTransformerProvider

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    # SentenceTransformerProvider defers its (multi-second, first-time-only)
    # model download+load to first use on purpose — see embeddings.py's
    # module doc comment, it's what keeps importing that module cheap for
    # every test run. But that means the *first real* /score or /profile
    # request would otherwise pay that cost mid-request, blocking this
    # single-worker event loop long enough that Spring's client sees a
    # broken/empty response (logged there as a content-type mismatch,
    # since the connection breaks before a real Content-Type header is
    # sent) — it only recovers because of the caller's own retry. Force
    # that cost here instead, before Uvicorn starts accepting connections
    # at all, so a fresh container's first real request is already warm.
    provider = get_embedding_provider()
    if isinstance(provider, SentenceTransformerProvider):
        provider.embed_batch(["warmup"])
    yield


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
    lifespan=lifespan,
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
