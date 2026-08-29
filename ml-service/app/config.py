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

    # --- LLM extraction (/parse) ---
    openai_api_key: str | None = None
    openai_model: str = "gpt-4o-mini"
    openai_timeout_seconds: float = 30.0

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
