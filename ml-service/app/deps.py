"""FastAPI dependency providers. Cached per-process with lru_cache so the
(expensive-to-construct) embedding model and LLM client are built once, not
per-request — and swappable per-test via ``app.dependency_overrides``."""

from functools import lru_cache

from fastapi import Depends, Header, HTTPException, status

from app.config import Settings, get_settings
from app.services.embeddings import EmbeddingProvider, HashingEmbeddingProvider, SentenceTransformerProvider
from app.services.llm_client import LLMClient, OpenAILLMClient, StubLLMClient
from app.services.resume_profile_client import OpenAIResumeProfileClient, ResumeProfileClient, StubResumeProfileClient


@lru_cache
def get_llm_client() -> LLMClient:
    settings = get_settings()
    if settings.openai_api_key:
        return OpenAILLMClient(
            api_key=settings.openai_api_key,
            model=settings.openai_model,
            timeout_seconds=settings.openai_timeout_seconds,
        )
    return StubLLMClient()


@lru_cache
def get_resume_profile_client() -> ResumeProfileClient:
    settings = get_settings()
    if settings.openai_api_key:
        return OpenAIResumeProfileClient(
            api_key=settings.openai_api_key,
            model=settings.openai_model,
            timeout_seconds=settings.openai_timeout_seconds,
        )
    return StubResumeProfileClient()


@lru_cache
def get_embedding_provider() -> EmbeddingProvider:
    settings = get_settings()
    if settings.embedding_backend == "hashing":
        return HashingEmbeddingProvider()
    return SentenceTransformerProvider(model_name=settings.embedding_model_name)


def get_settings_dep() -> Settings:
    return get_settings()


def verify_internal_api_key(
    x_internal_api_key: str | None = Header(default=None),
    settings: Settings = Depends(get_settings_dep),
) -> None:
    """Gate on /parse and /match (never /health — that's the liveness probe
    and must stay dependency-free). A no-op when MLSVC_INTERNAL_API_KEY
    isn't set, which is the local-dev default and matches Spring's own
    "don't send the header unless configured" behavior — both sides agree
    on "no check" rather than every dev needing to mint a shared value.
    Takes ``settings`` via ``Depends`` (rather than calling ``get_settings()``
    directly) so tests can override it without fighting ``get_settings``'s
    process-wide ``lru_cache``.
    """
    if settings.internal_api_key and x_internal_api_key != settings.internal_api_key:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or missing internal API key")
