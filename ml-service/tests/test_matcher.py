from app.services.embeddings import HashingEmbeddingProvider
from app.services.matcher import extract_candidate_skills, find_matched_and_missing_skills, score_match


def test_extract_candidate_skills_finds_vocabulary_hits_only():
    text = "We use Python and React. No hexbotics required."

    skills = extract_candidate_skills(text)

    assert "python" in skills
    assert "react" in skills
    assert "hexbotics" not in skills


def test_extract_candidate_skills_respects_word_boundaries():
    # "r" (the language) must not match inside "for" or "your".
    text = "This role is for your career growth in Java."

    skills = extract_candidate_skills(text)

    assert "r" not in skills
    assert "java" in skills


def test_find_matched_and_missing_skills_exact_substring_match():
    provider = HashingEmbeddingProvider()

    matched, missing = find_matched_and_missing_skills(
        required_skills=["python", "docker"],
        resume_text="I write Python daily but have never touched Docker.",
        embedding_provider=provider,
        semantic_threshold=0.9,  # high threshold: only the exact match should count
    )

    assert "python" in matched
    assert "docker" in matched  # substring match ("Docker" appears verbatim) — no embedding fallback needed


def test_find_matched_and_missing_skills_returns_empty_for_no_required_skills():
    provider = HashingEmbeddingProvider()

    matched, missing = find_matched_and_missing_skills([], "anything", provider, 0.5)

    assert matched == []
    assert missing == []


def test_find_matched_and_missing_skills_falls_back_to_missing_below_threshold():
    provider = HashingEmbeddingProvider()

    matched, missing = find_matched_and_missing_skills(
        required_skills=["kubernetes"],
        resume_text="I paint landscapes and play the violin.",
        embedding_provider=provider,
        semantic_threshold=0.99,  # unrealistically strict, so the fallback can't pass
    )

    assert matched == []
    assert missing == ["kubernetes"]


def test_score_match_returns_value_in_unit_range():
    provider = HashingEmbeddingProvider()

    score = score_match("Python backend engineer", "Looking for a Python backend engineer", provider)

    assert 0.0 <= score <= 1.0
