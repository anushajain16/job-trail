from app.deps import get_resume_profile_client
from app.main import app
from app.services.resume_profile_client import ResumeProfileError, ResumeProfileExtraction

RESUME_TEXT = """Senior Backend Engineer

Senior backend engineer with 5 years of experience building services in
Python and FastAPI, backed by PostgreSQL and deployed via Docker.
Comfortable with Git, REST APIs, and unit testing."""


def test_parse_resume_profile_uses_stub_extraction(client):
    response = client.post("/profile", json={"resume_text": RESUME_TEXT})

    assert response.status_code == 200
    body = response.json()
    assert 0.0 <= body["confidence"] <= 1.0
    profile = body["profile"]
    assert "python" in profile["skills"]
    assert "fastapi" in profile["skills"]
    assert "postgresql" in profile["skills"]
    assert profile["years_experience"] == 5.0
    assert profile["seniority"] == "senior"


def test_parse_resume_profile_rejects_blank_text(client):
    response = client.post("/profile", json={"resume_text": ""})

    assert response.status_code == 422


def test_parse_resume_profile_maps_extraction_failure_to_502(client):
    class FailingResumeProfileClient:
        async def extract(self, resume_text: str) -> ResumeProfileExtraction:
            raise ResumeProfileError("upstream is down")

    app.dependency_overrides[get_resume_profile_client] = lambda: FailingResumeProfileClient()
    try:
        response = client.post("/profile", json={"resume_text": RESUME_TEXT})
        assert response.status_code == 502
        assert "upstream is down" in response.json()["detail"]
    finally:
        app.dependency_overrides.pop(get_resume_profile_client, None)
