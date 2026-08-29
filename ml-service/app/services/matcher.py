"""Resume ↔ job-description matching: an overall similarity score plus a
matched/missing skill breakdown.

Skill matching runs two passes on purpose: an exact (word-boundary,
case-insensitive) substring check first — cheap and precise for the common
case where the resume literally says "PostgreSQL" — then an embedding
similarity fallback only for skills the substring pass missed, to catch
phrasing gaps like "Postgres" vs. "PostgreSQL" or "K8s" vs. "Kubernetes"
without paying the embedding cost for every skill on every request.
"""

from app.schemas import ResumeProfile
from app.services.embeddings import EmbeddingProvider, cosine_similarity
from app.services.skills_vocab import SKILL_VOCAB
from app.services.text_matching import word_in_text


def extract_candidate_skills(job_description_text: str) -> list[str]:
    """Derives a required-skills list from raw JD text via vocabulary
    keyword matching. Only used when the caller doesn't already have one
    from a prior /parse call — that's the higher-quality, LLM-derived path."""
    lower = job_description_text.lower()
    return [skill for skill in SKILL_VOCAB if word_in_text(skill, lower)]


def score_match(resume_text: str, job_description_text: str, embedding_provider: EmbeddingProvider) -> float:
    resume_vector, jd_vector = embedding_provider.embed_batch([resume_text, job_description_text])
    similarity = cosine_similarity(resume_vector, jd_vector)
    return round(max(0.0, min(1.0, similarity)), 4)


def find_matched_and_missing_skills(
    required_skills: list[str],
    resume_text: str,
    embedding_provider: EmbeddingProvider,
    semantic_threshold: float,
) -> tuple[list[str], list[str]]:
    if not required_skills:
        return [], []

    lower_resume = resume_text.lower()
    matched: list[str] = []
    needs_semantic_check: list[str] = []
    for skill in required_skills:
        if word_in_text(skill, lower_resume):
            matched.append(skill)
        else:
            needs_semantic_check.append(skill)

    missing: list[str] = []
    if needs_semantic_check:
        vectors = embedding_provider.embed_batch([resume_text, *needs_semantic_check])
        resume_vector, skill_vectors = vectors[0], vectors[1:]
        for skill, skill_vector in zip(needs_semantic_check, skill_vectors):
            similarity = cosine_similarity(skill_vector, resume_vector)
            (matched if similarity >= semantic_threshold else missing).append(skill)

    return matched, missing


def profile_to_text(profile: ResumeProfile) -> str:
    """Flattens a structured ResumeProfile back into one text blob — the
    /score endpoint's stand-in for "resume text" everywhere this module's
    functions expect one (embedding input, word_in_text skill checks).
    Built from the *parsed* fields (skills/roles/summary), not the original
    resume text: /score only ever receives the profile, never the raw
    resume again, so scoring quality rides entirely on how good the one-time
    /profile extraction was.
    """
    parts = [*profile.skills, *profile.roles]
    if profile.seniority:
        parts.append(profile.seniority)
    if profile.summary:
        parts.append(profile.summary)
    return ". ".join(parts)

