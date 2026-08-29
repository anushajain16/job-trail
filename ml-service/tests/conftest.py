"""Test-wide setup. Forces the dependency-free backends (hashing embeddings,
stub LLM) before ``app`` is ever imported, so the suite never needs a real
OpenAI key or a sentence-transformers model download — see
app/config.py and app/services/embeddings.py for why those backends exist."""

import os

os.environ.setdefault("MLSVC_EMBEDDING_BACKEND", "hashing")
os.environ.pop("MLSVC_OPENAI_API_KEY", None)  # ensure the stub LLM client is selected

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)
