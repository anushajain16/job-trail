import httpx
import pytest
import respx

from app.deps import get_llm_client
from app.main import app
from app.services.llm_client import LLMError, LLMExtraction

SAMPLE_JOB_TEXT = """Backend Engineer
Full-time, Senior
$150,000 - $190,000
We need Python, FastAPI, PostgreSQL and Docker experience. Kubernetes a plus."""


def test_parse_from_text_uses_stub_extraction(client):
    response = client.post("/parse", json={"text": SAMPLE_JOB_TEXT})

    assert response.status_code == 200
    body = response.json()
    assert body["source"] == "text"
    assert body["source_url"] is None
    assert 0.0 <= body["confidence"] <= 1.0
    parsed = body["parsed"]
    assert parsed["employment_type"] == "full-time"
    assert parsed["seniority"] == "senior"
    assert parsed["salary_min"] == 150000.0
    assert parsed["salary_max"] == 190000.0
    assert parsed["currency"] == "USD"
    assert "python" in parsed["required_skills"]
    assert "docker" in parsed["required_skills"]


def test_parse_rejects_neither_url_nor_text(client):
    response = client.post("/parse", json={})

    assert response.status_code == 422


def test_parse_rejects_both_url_and_text(client):
    response = client.post("/parse", json={"url": "https://example.com/job", "text": "some text"})

    assert response.status_code == 422


@respx.mock
def test_parse_from_url_scrapes_then_extracts(client):
    respx.get("https://boards.example.com/job/42").mock(
        return_value=httpx.Response(
            200,
            headers={"content-type": "text/html"},
            text=f"<html><body><script>ignored()</script><p>{SAMPLE_JOB_TEXT}</p></body></html>",
        )
    )

    response = client.post("/parse", json={"url": "https://boards.example.com/job/42"})

    assert response.status_code == 200
    body = response.json()
    assert body["source"] == "url"
    assert body["source_url"] == "https://boards.example.com/job/42"
    assert "python" in body["parsed"]["required_skills"]


@respx.mock
def test_parse_from_url_maps_scrape_failure_to_422(client):
    respx.get("https://boards.example.com/gone").mock(return_value=httpx.Response(404))

    response = client.post("/parse", json={"url": "https://boards.example.com/gone"})

    assert response.status_code == 422
    assert "404" in response.json()["detail"]


def test_parse_maps_llm_failure_to_502(client):
    class FailingLLMClient:
        async def extract(self, text: str) -> LLMExtraction:
            raise LLMError("upstream is down")

    app.dependency_overrides[get_llm_client] = lambda: FailingLLMClient()
    try:
        response = client.post("/parse", json={"text": SAMPLE_JOB_TEXT})
        assert response.status_code == 502
        assert "upstream is down" in response.json()["detail"]
    finally:
        app.dependency_overrides.pop(get_llm_client, None)
