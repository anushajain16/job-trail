RESUME_TEXT = """Senior backend engineer with 5 years of experience building
services in Python and FastAPI, backed by PostgreSQL and deployed via Docker.
Comfortable with Git, REST APIs, and unit testing."""

JOB_DESCRIPTION_TEXT = """We're hiring a backend engineer. Must have Python,
FastAPI, and PostgreSQL experience. Kubernetes and Kafka are a plus."""


def test_match_with_explicit_required_skills(client):
    response = client.post(
        "/match",
        json={
            "resume_text": RESUME_TEXT,
            "job_description_text": JOB_DESCRIPTION_TEXT,
            "required_skills": ["python", "fastapi", "postgresql", "kubernetes"],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert 0.0 <= body["match_score"] <= 1.0
    assert set(body["considered_skills"]) == {"python", "fastapi", "postgresql", "kubernetes"}
    assert set(body["matched_skills"]) == {"python", "fastapi", "postgresql"}
    assert body["missing_skills"] == ["kubernetes"]


def test_match_derives_required_skills_from_jd_when_not_given(client):
    response = client.post(
        "/match",
        json={"resume_text": RESUME_TEXT, "job_description_text": JOB_DESCRIPTION_TEXT},
    )

    assert response.status_code == 200
    body = response.json()
    considered = set(body["considered_skills"])
    assert {"python", "fastapi", "postgresql", "kubernetes", "kafka"} <= considered
    assert "kafka" in body["missing_skills"]
    assert "python" in body["matched_skills"]


def test_match_rejects_empty_resume_text(client):
    response = client.post(
        "/match",
        json={"resume_text": "", "job_description_text": JOB_DESCRIPTION_TEXT},
    )

    assert response.status_code == 422


def test_match_with_no_overlapping_skills_reports_all_missing(client):
    response = client.post(
        "/match",
        json={
            "resume_text": "I paint landscapes and play the violin.",
            "job_description_text": JOB_DESCRIPTION_TEXT,
            "required_skills": ["python", "kubernetes"],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["matched_skills"] == []
    assert set(body["missing_skills"]) == {"python", "kubernetes"}
