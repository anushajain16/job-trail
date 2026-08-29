from fastapi import APIRouter, Depends, HTTPException

from app.config import Settings
from app.deps import get_llm_client, get_settings_dep
from app.schemas import ParsedJobPosting, ParseRequest, ParseResponse
from app.services.llm_client import LLMClient, LLMError
from app.services.scraper import ScrapeError, fetch_visible_text

router = APIRouter(prefix="/parse", tags=["parse"])


@router.post("", response_model=ParseResponse)
async def parse_job_posting(
    payload: ParseRequest,
    llm_client: LLMClient = Depends(get_llm_client),
    settings: Settings = Depends(get_settings_dep),
) -> ParseResponse:
    if payload.url:
        try:
            text = await fetch_visible_text(payload.url, settings.scrape_timeout_seconds, settings.scrape_max_chars)
        except ScrapeError as exc:
            raise HTTPException(status_code=422, detail=str(exc)) from exc
        source = "url"
    else:
        text = payload.text.strip()[: settings.scrape_max_chars]  # payload validator guarantees non-empty
        source = "text"

    try:
        extraction = await llm_client.extract(text)
    except LLMError as exc:
        raise HTTPException(status_code=502, detail=f"Extraction failed: {exc}") from exc

    parsed = ParsedJobPosting(**extraction.model_dump(exclude={"confidence"}))
    return ParseResponse(
        source=source,
        source_url=payload.url,
        parsed=parsed,
        confidence=extraction.confidence,
    )
