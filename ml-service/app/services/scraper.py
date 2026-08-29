"""Fetches a job posting URL and reduces it to plain, LLM-ready text.

Stateless and side-effect-free beyond the one outbound HTTP call — no
caching, no persistence. Failure here (timeout, 4xx/5xx, non-HTML,
no extractable text) is always the caller's problem to handle, never
retried silently, so /parse can turn it into a clear 422 rather than a
mysterious empty extraction.
"""

import httpx
from bs4 import BeautifulSoup

_USER_AGENT = "JobTrailParser/1.0 (+https://jobtrail.dev; internal parsing service)"

# Tags that are never part of a job posting's readable body — stripped
# before text extraction so the LLM isn't paying context budget on nav
# chrome, inline scripts, or cookie-banner boilerplate.
_NOISE_TAGS = ("script", "style", "noscript", "svg", "header", "footer", "nav", "form")


class ScrapeError(Exception):
    """Raised for any failure to turn a URL into usable text — the /parse
    router maps this to a 422 rather than letting it surface as a 500."""


async def fetch_visible_text(url: str, timeout_seconds: float, max_chars: int) -> str:
    try:
        async with httpx.AsyncClient(
            timeout=timeout_seconds,
            follow_redirects=True,
            headers={"User-Agent": _USER_AGENT},
        ) as client:
            response = await client.get(url)
            response.raise_for_status()
    except httpx.TimeoutException as exc:
        raise ScrapeError(f"Timed out fetching {url}") from exc
    except httpx.HTTPStatusError as exc:
        raise ScrapeError(f"{url} returned HTTP {exc.response.status_code}") from exc
    except httpx.HTTPError as exc:
        raise ScrapeError(f"Failed to fetch {url}: {exc}") from exc

    content_type = response.headers.get("content-type", "")
    if "html" not in content_type.lower() and "<html" not in response.text[:1000].lower():
        raise ScrapeError(f"{url} did not return HTML content (content-type: {content_type or 'unknown'})")

    soup = BeautifulSoup(response.text, "html.parser")
    for tag in soup(_NOISE_TAGS):
        tag.decompose()

    lines = [line.strip() for line in soup.get_text(separator="\n").splitlines() if line.strip()]
    text = "\n".join(lines)
    if not text:
        raise ScrapeError(f"{url} had no extractable text")

    return text[:max_chars]
