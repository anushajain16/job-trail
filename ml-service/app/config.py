"""Runtime settings, all overridable via ``MLSVC_*`` env vars (see .env.example).

No secrets have hardcoded defaults except the ones that are safe to ship —
this service holds no DB credentials and no user auth of its own, so the
only real secret it ever touches is OPENAI_API_KEY, which defaults to unset
and switches the LLM client to a heuristic stub (see services/llm_client.py)
rather than failing to start.
"""

from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="MLSVC_", env_file=".env", extra="ignore")

    app_name: str = "jobtrail-ml-service"
    version: str = "0.1.0"

    # CORS is permissive-by-list only for local dev; the real deploy only
    # ever needs the Spring Boot API to reach this over the Docker network,
    # not a browser origin, so a locked-down empty list is fine there.
    cors_origins: list[str] = ["http://localhost:5173", "http://localhost:8080"]

    # --- scraping (/parse with a url) ---
    scrape_timeout_seconds: float = 10.0
    scrape_max_chars: int = 15_000

    # Shared secret Spring Boot sends as the X-Internal-Api-Key header on
    # every call (see backend's app.ml-service.shared-secret). Unset by
    # default — this service has no auth of its own and relies on network
    # placement (only reachable from Spring over the internal Docker
    # network) — but set the same value on both sides in any deploy where
    # that boundary isn't airtight, e.g. a shared/multi-tenant network.
    internal_api_key: str | None = None

    # --- LLM extraction (/parse, /profile) ---
    openai_api_key: str | None = None
    openai_model: str = "gpt-4o-mini"
    openai_timeout_seconds: float = 30.0
    # None -> the openai package's own default (api.openai.com). Set to any
    # OpenAI-compatible endpoint to use a different provider through the same
    # client code — e.g. Gemini's compat layer at
    # https://generativelanguage.googleapis.com/v1beta/openai/ with an
    # MLSVC_OPENAI_API_KEY from aistudio.google.com/apikey and
    # MLSVC_OPENAI_MODEL=gemini-3.6-flash. Structured-output strictness
    # (the json_schema "strict": true this service relies on) is an OpenAI
    # feature other providers approximate rather than guarantee — see
    # llm_client.py's OpenAILLMClient doc comment.
    openai_base_url: str | None = None

    # --- embeddings (/match) ---
    # "sentence-transformer" is the real, semantically-meaningful backend
    # this ships with; "hashing" is a dependency-free deterministic
    # fallback used by the test suite (and available as an emergency
    # runtime fallback) so CI never needs to download model weights.
    embedding_backend: Literal["sentence-transformer", "hashing"] = "sentence-transformer"
    embedding_model_name: str = "sentence-transformers/all-MiniLM-L6-v2"
    match_semantic_threshold: float = 0.45


@lru_cache
def get_settings() -> Settings:
    return Settings()
