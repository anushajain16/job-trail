"""X-Internal-Api-Key gate on /parse and /match (see app/deps.py). /health
is deliberately excluded — covered by not appearing here and by
test_health.py needing no header."""

from app.config import Settings, get_settings
from app.deps import get_settings_dep
from app.main import app

SAMPLE_JOB_TEXT = "Backend Engineer, full-time, Python and Docker required."


def _settings_with_key(key: str) -> Settings:
    base = get_settings()
    return base.model_copy(update={"internal_api_key": key})


def test_parse_allows_any_caller_when_no_key_configured(client):
    # Default test settings never set MLSVC_INTERNAL_API_KEY (see conftest.py).
    response = client.post("/parse", json={"text": SAMPLE_JOB_TEXT})
    assert response.status_code == 200


def test_parse_rejects_missing_header_when_key_configured(client):
    app.dependency_overrides[get_settings_dep] = lambda: _settings_with_key("s3cr3t")
    try:
        response = client.post("/parse", json={"text": SAMPLE_JOB_TEXT})
        assert response.status_code == 401
    finally:
        app.dependency_overrides.pop(get_settings_dep, None)


def test_parse_rejects_wrong_header_when_key_configured(client):
    app.dependency_overrides[get_settings_dep] = lambda: _settings_with_key("s3cr3t")
    try:
        response = client.post(
            "/parse", json={"text": SAMPLE_JOB_TEXT}, headers={"X-Internal-Api-Key": "wrong"}
        )
        assert response.status_code == 401
    finally:
        app.dependency_overrides.pop(get_settings_dep, None)


def test_parse_accepts_matching_header_when_key_configured(client):
    app.dependency_overrides[get_settings_dep] = lambda: _settings_with_key("s3cr3t")
    try:
        response = client.post(
            "/parse", json={"text": SAMPLE_JOB_TEXT}, headers={"X-Internal-Api-Key": "s3cr3t"}
        )
        assert response.status_code == 200
    finally:
        app.dependency_overrides.pop(get_settings_dep, None)
