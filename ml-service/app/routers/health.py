from fastapi import APIRouter, Depends

from app.config import Settings
from app.deps import get_settings_dep
from app.schemas import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health(settings: Settings = Depends(get_settings_dep)) -> HealthResponse:
    """Liveness/readiness probe. Deliberately does nothing but confirm the
    process is up — no DB, no downstream calls, so it never flaps on a
    slow scrape target or a rate-limited LLM."""
    return HealthResponse(service=settings.app_name, version=settings.version)
