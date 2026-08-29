from fastapi import APIRouter, Depends

from app.config import Settings
from app.deps import get_embedding_provider, get_settings_dep, verify_internal_api_key
from app.schemas import ScoreRequest, ScoreResponse
from app.services.embeddings import EmbeddingProvider
from app.services.matcher import extract_candidate_skills, find_matched_and_missing_skills, profile_to_text, score_match

router = APIRouter(prefix="/score", tags=["score"], dependencies=[Depends(verify_internal_api_key)])


@router.post("", response_model=ScoreResponse)
async def score_profile_against_job(
    payload: ScoreRequest,
    embedding_provider: EmbeddingProvider = Depends(get_embedding_provider),
    settings: Settings = Depends(get_settings_dep),
) -> ScoreResponse:
    required_skills = payload.required_skills or extract_candidate_skills(payload.job_description_text)
    profile_text = profile_to_text(payload.profile)

    matched, missing = find_matched_and_missing_skills(
        required_skills, profile_text, embedding_provider, settings.match_semantic_threshold,
    )
    match_pct = score_match(profile_text, payload.job_description_text, embedding_provider)

    return ScoreResponse(
        match_pct=match_pct,
        matched_skills=matched,
        missing_skills=missing,
        considered_skills=required_skills,
    )
