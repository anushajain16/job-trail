"""X-Internal-Api-Key gate on /parse and /match (see app/deps.py). /health
is deliberately excluded — covered by not appearing here and by
test_health.py needing no header.

The reject/accept cases exercise verify_internal_api_key directly with a
constructed Settings object rather than routing through the app +
TestClient + app.dependency_overrides — that indirection makes the
assertion depend on FastAPI's override plumbing picking up a mutated
module-global dict before the request is dispatched, which isn't something
this test needs to entangle itself with just to check "wrong key -> 401"."""

import pytest
from fastapi import HTTPException

from app.config import get_settings
from app.deps import verify_internal_api_key

SAMPLE_JOB_TEXT = "Backend Engineer, full-time, Python and Docker required."


def _settings_with_key(key: str | None):
    return get_settings().model_copy(update={"internal_api_key": key})


def test_parse_allows_any_caller_when_no_key_configured(client):
    # Default test settings never set MLSVC_INTERNAL_API_KEY (see conftest.py).
    response = client.post("/parse", json={"text": SAMPLE_JOB_TEXT})
    assert response.status_code == 200


def test_verify_rejects_missing_header_when_key_configured():
    with pytest.raises(HTTPException) as exc_info:
        verify_internal_api_key(x_internal_api_key=None, settings=_settings_with_key("s3cr3t"))
    assert exc_info.value.status_code == 401


def test_verify_rejects_wrong_header_when_key_configured():
    with pytest.raises(HTTPException) as exc_info:
        verify_internal_api_key(x_internal_api_key="wrong", settings=_settings_with_key("s3cr3t"))
    assert exc_info.value.status_code == 401


def test_verify_accepts_matching_header_when_key_configured():
    verify_internal_api_key(x_internal_api_key="s3cr3t", settings=_settings_with_key("s3cr3t"))  # no raise


def test_verify_is_a_noop_when_no_key_configured():
    verify_internal_api_key(x_internal_api_key=None, settings=_settings_with_key(None))  # no raise
