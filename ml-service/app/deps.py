"""FastAPI dependency providers. Cached per-process with lru_cache so the
(expensive-to-construct) embedding model and LLM client are built once, not
per-request — and swappable per-test via ``app.dependency_overrides``."""

from functools import lru_cache

from app.config import Settings, get_settings
from app.services.embeddings import EmbeddingProvider, HashingEmbeddingProvider, SentenceTransformerProvider
from app.services.llm_client import LLMClient, OpenAILLMClient, StubLLMClient


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
def get_embedding_provider() -> EmbeddingProvider:
    settings = get_settings()
    if settings.embedding_backend == "hashing":
        return HashingEmbeddingProvider()
    return SentenceTransformerProvider(model_name=settings.embedding_model_name)


def get_settings_dep() -> Settings:
    return get_settings()
