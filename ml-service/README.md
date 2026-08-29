# jobtrail-ml-service

Module M1 — the stateless parse & match service. Text-in, structured-data-out.
No database, no auth of its own: it's reached only by the Spring Boot API over
the internal Docker network, and every request is fully self-contained.

## Endpoints

| Method | Path      | Does |
|--------|-----------|------|
| `GET`  | `/health` | Liveness probe — no downstream calls. |
| `POST` | `/parse`  | `{url}` or `{text}` in → scrape (if `url`) → LLM extraction → structured job posting + confidence. |
| `POST` | `/match`  | `{resume_text, job_description_text, required_skills?}` in → embedding similarity score + matched/missing skills. |

Interactive docs (Swagger UI) at `/docs` once the service is running.

`/parse` and `/match` check the `X-Internal-Api-Key` header against
`MLSVC_INTERNAL_API_KEY` when that's set (unset by default — see
`.env.example`); `/health` never does, since it must stay reachable for a
liveness probe with no credentials of its own. This is defense in depth,
not the real boundary: the real boundary is that this service is reachable
only from Spring Boot over the internal Docker network, never a public one.

## Graceful degradation, by design

Neither endpoint requires external credentials to run:

- **No `OPENAI_API_KEY`** → `/parse` falls back to `StubLLMClient`, a
  deterministic regex + keyword-vocabulary extractor. Lower confidence,
  never a hard failure.
- **`MLSVC_EMBEDDING_BACKEND=hashing`** → `/match` uses a dependency-free
  hashed bag-of-words vectorizer instead of downloading the real
  sentence-transformers model. This is what the test suite runs on, so CI
  needs no model download and no network call.

The real backends (`OpenAILLMClient`, `SentenceTransformerProvider`) are the
production default; see `.env.example` for every override.

This mirrors the app-level story: if this whole service is unreachable, the
Spring Boot app falls back to manual entry. Within the service itself, a
missing LLM key degrades quality rather than availability.

## Run locally

```bash
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
cp .env.example .env   # optional — every var has a safe default
uvicorn app.main:app --reload
```

## Test

```bash
pytest
```

The suite forces the hashing embedding backend and stub LLM client (see
`tests/conftest.py`) — it never needs a real API key or a model download.

## Architecture

```
app/
  main.py             FastAPI app, CORS, router registration
  config.py           Settings (env-driven, MLSVC_ prefix)
  schemas.py           Request/response Pydantic models (the wire contract)
  deps.py               Cached dependency providers (LLM client, embedding provider)
  routers/
    health.py
    parse.py
    match.py
  services/
    scraper.py          URL → plain text (httpx + BeautifulSoup)
    llm_client.py        LLM extraction: OpenAILLMClient + StubLLMClient behind one interface
    embeddings.py         Text → vector: SentenceTransformerProvider + HashingEmbeddingProvider
    matcher.py             Score + matched/missing skills, given an EmbeddingProvider
    skills_vocab.py         Shared keyword vocabulary (stub extraction + skill derivation)
```
