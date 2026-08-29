from fastapi import APIRouter, Depends

from app.config import Settings
from app.deps import get_embedding_provider, get_settings_dep
from app.schemas import MatchRequest, MatchResponse
from app.services.embeddings import EmbeddingProvider
from app.services.matcher import extract_candidate_skills, find_matched_and_missing_skills, score_match

router = APIRouter(prefix="/match", tags=["match"])


@router.post("", response_model=MatchResponse)
async def match_resume_to_job(
    payload: MatchRequest,
    embedding_provider: EmbeddingProvider = Depends(get_embedding_provider),
    settings: Settings = Depends(get_settings_dep),
) -> MatchResponse:
    required_skills = payload.required_skills or extract_candidate_skills(payload.job_description_text)

    matched, missing = find_matched_and_missing_skills(
        required_skills, payload.resume_text, embedding_provider, settings.match_semantic_threshold,
    )
    match_score = score_match(payload.resume_text, payload.job_description_text, embedding_provider)

    return MatchResponse(
        match_score=match_score,
        matched_skills=matched,
        missing_skills=missing,
        considered_skills=required_skills,
    )
