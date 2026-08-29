JOB_DESCRIPTION_TEXT = """We're hiring a backend engineer. Must have Python,
FastAPI, and PostgreSQL experience. Kubernetes and Kafka are a plus."""

BACKEND_PROFILE = {
    "skills": ["python", "fastapi", "postgresql", "docker", "git"],
    "years_experience": 5.0,
    "roles": ["Senior Backend Engineer"],
    "seniority": "senior",
    "summary": "Backend engineer focused on Python services.",
}

FRONTEND_PROFILE = {
    "skills": ["react", "typescript", "css", "figma"],
    "years_experience": 3.0,
    "roles": ["Frontend Engineer"],
    "seniority": "mid",
    "summary": "Frontend engineer building React UIs.",
}


def test_score_with_explicit_required_skills(client):
    response = client.post(
        "/score",
        json={
            "profile": BACKEND_PROFILE,
            "job_description_text": JOB_DESCRIPTION_TEXT,
            "required_skills": ["python", "fastapi", "postgresql", "kubernetes"],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert 0.0 <= body["match_pct"] <= 1.0
    assert set(body["considered_skills"]) == {"python", "fastapi", "postgresql", "kubernetes"}
    assert set(body["matched_skills"]) == {"python", "fastapi", "postgresql"}
    assert body["missing_skills"] == ["kubernetes"]


def test_score_derives_required_skills_from_jd_when_not_given(client):
    response = client.post("/score", json={"profile": BACKEND_PROFILE, "job_description_text": JOB_DESCRIPTION_TEXT})

    assert response.status_code == 200
    body = response.json()
    considered = set(body["considered_skills"])
    assert {"python", "fastapi", "postgresql", "kubernetes", "kafka"} <= considered
    assert "kafka" in body["missing_skills"]
    assert "python" in body["matched_skills"]


def test_score_rejects_blank_job_description(client):
    response = client.post("/score", json={"profile": BACKEND_PROFILE, "job_description_text": ""})

    assert response.status_code == 422


def test_backend_profile_scores_higher_than_frontend_profile_on_backend_jd(client):
    """The plausibility check the feature is actually for: a backend-heavy
    profile should score higher, and be missing fewer required skills, on a
    backend JD than an unrelated frontend profile does."""
    backend_response = client.post(
        "/score", json={"profile": BACKEND_PROFILE, "job_description_text": JOB_DESCRIPTION_TEXT}
    ).json()
    frontend_response = client.post(
        "/score", json={"profile": FRONTEND_PROFILE, "job_description_text": JOB_DESCRIPTION_TEXT}
    ).json()

    assert backend_response["match_pct"] > frontend_response["match_pct"]
    assert len(backend_response["missing_skills"]) < len(frontend_response["missing_skills"])
