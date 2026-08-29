import pytest

from app.services.llm_client import StubLLMClient


async def test_stub_extracts_salary_range_and_currency():
    extraction = await StubLLMClient().extract("Compensation: $120,000 - $150,000 per year.")

    assert extraction.salary_min == 120000.0
    assert extraction.salary_max == 150000.0
    assert extraction.currency == "USD"


async def test_stub_extracts_employment_type_and_seniority():
    extraction = await StubLLMClient().extract("Senior Backend Engineer\nFull-time role based remotely.")

    assert extraction.seniority == "senior"
    assert extraction.employment_type == "full-time"


async def test_stub_handles_empty_text_gracefully():
    extraction = await StubLLMClient().extract("")

    assert extraction.role is None
    assert extraction.required_skills == []
    assert extraction.confidence == 0.0


async def test_stub_confidence_is_capped_and_never_high():
    extraction = await StubLLMClient().extract(
        "Senior Full-time Python Docker Kubernetes Engineer $100,000 - $200,000"
    )

    assert 0.0 < extraction.confidence <= 0.6
