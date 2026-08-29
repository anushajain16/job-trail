from fastapi import APIRouter, Depends, HTTPException

from app.deps import get_resume_profile_client, verify_internal_api_key
from app.schemas import ProfileRequest, ProfileResponse, ResumeProfile
from app.services.resume_profile_client import ResumeProfileClient, ResumeProfileError

router = APIRouter(prefix="/profile", tags=["profile"], dependencies=[Depends(verify_internal_api_key)])


@router.post("", response_model=ProfileResponse)
async def parse_resume_profile(
    payload: ProfileRequest,
    resume_profile_client: ResumeProfileClient = Depends(get_resume_profile_client),
) -> ProfileResponse:
    try:
        extraction = await resume_profile_client.extract(payload.resume_text)
    except ResumeProfileError as exc:
        raise HTTPException(status_code=502, detail=f"Resume extraction failed: {exc}") from exc

    profile = ResumeProfile(**extraction.model_dump(exclude={"confidence"}))
    return ProfileResponse(profile=profile, confidence=extraction.confidence)
