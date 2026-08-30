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


async def test_stub_extracts_company_from_scraper_hint_line():
    # scraper.fetch_visible_text prepends exactly this line when it finds a
    # JobPosting JSON-LD block or og:site_name tag.
    extraction = await StubLLMClient().extract("Company: Acme Corp\n\nSenior Backend Engineer\nFull-time, remote.")

    assert extraction.company == "Acme Corp"
    assert extraction.role == "Senior Backend Engineer"  # the company hint line itself isn't mistaken for the role


async def test_stub_leaves_company_null_without_a_hint_line():
    # Pasted JD text (no scrape step) never has the hint line, and a
    # "Company:" mention isn't trusted unless it's the very first line.
    extraction = await StubLLMClient().extract("Senior Backend Engineer\nCompany: definitely not this one")

    assert extraction.company is None
