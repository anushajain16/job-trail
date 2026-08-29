import httpx
import pytest
import respx

from app.services.scraper import ScrapeError, fetch_visible_text


@respx.mock
async def test_fetch_visible_text_strips_noise_tags():
    respx.get("https://example.com/job").mock(
        return_value=httpx.Response(
            200,
            headers={"content-type": "text/html"},
            text="<html><body><nav>Home</nav><script>evil()</script><p>Role: Engineer</p></body></html>",
        )
    )

    text = await fetch_visible_text("https://example.com/job", timeout_seconds=5, max_chars=1000)

    assert "Role: Engineer" in text
    assert "evil()" not in text
    assert "Home" not in text


@respx.mock
async def test_fetch_visible_text_truncates_to_max_chars():
    respx.get("https://example.com/long").mock(
        return_value=httpx.Response(200, headers={"content-type": "text/html"}, text="<p>" + "x" * 5000 + "</p>")
    )

    text = await fetch_visible_text("https://example.com/long", timeout_seconds=5, max_chars=100)

    assert len(text) == 100


@respx.mock
async def test_fetch_visible_text_raises_on_http_error():
    respx.get("https://example.com/missing").mock(return_value=httpx.Response(500))

    with pytest.raises(ScrapeError, match="500"):
        await fetch_visible_text("https://example.com/missing", timeout_seconds=5, max_chars=1000)


@respx.mock
async def test_fetch_visible_text_raises_on_non_html():
    respx.get("https://example.com/data.json").mock(
        return_value=httpx.Response(200, headers={"content-type": "application/json"}, text='{"a": 1}')
    )

    with pytest.raises(ScrapeError, match="HTML"):
        await fetch_visible_text("https://example.com/data.json", timeout_seconds=5, max_chars=1000)


@respx.mock
async def test_fetch_visible_text_raises_on_timeout():
    respx.get("https://example.com/slow").mock(side_effect=httpx.TimeoutException("timed out"))

    with pytest.raises(ScrapeError, match="Timed out"):
        await fetch_visible_text("https://example.com/slow", timeout_seconds=5, max_chars=1000)
