"""Fetches a job posting URL and reduces it to plain, LLM-ready text.

Stateless and side-effect-free beyond the one outbound HTTP call — no
caching, no persistence. Failure here (timeout, 4xx/5xx, non-HTML,
no extractable text) is always the caller's problem to handle, never
retried silently, so /parse can turn it into a clear 422 rather than a
mysterious empty extraction.
"""

import json

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

    # Most job boards (LinkedIn, Greenhouse, Lever, Indeed, ...) embed the
    # employer name as machine-readable metadata — schema.org JobPosting
    # JSON-LD or an og:site_name tag — that never appears in the page's
    # visible text at all. Pull it out before the noise-tag pass below
    # deletes the <script>/<meta> it lives in, and surface it as an
    # explicit leading line so both StubLLMClient's heuristic and the LLM
    # extractor can pick it up as a real (not guessed) signal.
    company_hint = _extract_company_hint(soup)

    for tag in soup(_NOISE_TAGS):
        tag.decompose()

    lines = [line.strip() for line in soup.get_text(separator="\n").splitlines() if line.strip()]
    text = "\n".join(lines)
    if not text:
        raise ScrapeError(f"{url} had no extractable text")

    if company_hint:
        text = f"Company: {company_hint}\n\n{text}"

    return text[:max_chars]


def _extract_company_hint(soup: BeautifulSoup) -> str | None:
    for script in soup.find_all("script", type="application/ld+json"):
        name = _hiring_organization_name(script.string)
        if name:
            return name

    site_name = soup.find("meta", attrs={"property": "og:site_name"})
    if site_name and site_name.get("content"):
        return site_name["content"].strip()

    return None


def _hiring_organization_name(raw_json: str | None) -> str | None:
    if not raw_json:
        return None
    try:
        data = json.loads(raw_json)
    except json.JSONDecodeError:
        return None

    # A page can embed multiple JSON-LD blocks, or one block holding a list
    # (@graph) of entities — check every candidate for a JobPosting.
    candidates = data if isinstance(data, list) else data.get("@graph", [data]) if isinstance(data, dict) else []
    for candidate in candidates:
        if not isinstance(candidate, dict):
            continue
        if candidate.get("@type") != "JobPosting":
            continue
        organization = candidate.get("hiringOrganization")
        if isinstance(organization, dict) and organization.get("name"):
            return str(organization["name"]).strip()
        if isinstance(organization, str) and organization.strip():
            return organization.strip()

    return None
