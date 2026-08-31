# JobTrail Backend — Features & API Reference

Spring Boot 4 / Java, stateless JWT auth, PostgreSQL + Flyway, Redis, MinIO
(object storage), calls out to a separate FastAPI `ml-service` for
LLM/embedding work. Base path for every endpoint below is the app's own
origin (`http://localhost:8080` locally); every path already includes
`/api/...`.

Unless marked **Public**, every endpoint requires `Authorization: Bearer
<accessToken>` and is scoped to the authenticated caller — nothing here
returns or mutates another user's data (ownership is enforced in each
service's repository lookup, not just at the controller).

Every error response (any non-2xx) has this shape (`ErrorResponse`):
```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "/api/..." }
```

---

## 1. Auth (`/api/auth`) — `AuthController`

Stateless JWT (access + refresh token pair). Signup/login/refresh/logout are
the only public auth routes.

| Method | Path | Auth | Body | Returns | Notes |
|---|---|---|---|---|---|
| POST | `/api/auth/signup` | Public | `{ email, password }` (password 8–100 chars) | `201` `AuthResponse` | `409` if email already registered |
| POST | `/api/auth/login` | Public | `{ email, password }` | `AuthResponse` | `401` on bad credentials |
| POST | `/api/auth/oauth/{provider}` | Public | `{ token }` | `AuthResponse` | `provider` = `GOOGLE` or `GITHUB`. For Google, `token` is the Google Identity Services ID token (verified against `GOOGLE_OAUTH_CLIENT_ID` as audience — frontend does the Google-side flow). For GitHub, `token` is the OAuth `code` from GitHub's redirect; backend exchanges it server-side. `401` on verification failure. |
| POST | `/api/auth/refresh` | Public | `{ refreshToken }` | `AuthResponse` | Rotates the refresh token — the one presented is revoked, a new pair is issued. `401` if invalid/expired/already-used. |
| POST | `/api/auth/logout` | Public | `{ refreshToken }` | `204` | Revokes the given refresh token. |
| GET | `/api/auth/me` | ✓ | — | `{ id, email }` | Whoami, from the access token. |

`AuthResponse` = `{ accessToken, refreshToken, tokenType: "Bearer" }`.

---

## 2. Applications (`/api/applications`) — `ApplicationController`

The core entity: one row per job application, with a `currentStage`
(pipeline position) and an append-only status-history log.

**Stage enum**: `SAVED → APPLIED → SCREEN → INTERVIEW → FINAL → OFFER |
REJECTED | GHOSTED`. Nothing enforces a linear order — any transition to
any other stage is allowed; the only rejected case is "changing" to the
stage it's already in (`400`).

| Method | Path | Body | Returns | Notes |
|---|---|---|---|---|
| GET | `/api/applications` | — (query: `page`, `size`, `sort`) | `Page<ApplicationResponse>` | Default sort `createdAt DESC`, page size 20. |
| GET | `/api/applications/{id}` | — | `ApplicationResponse` | `404` if not owned/found. |
| GET | `/api/applications/export` | — | CSV file stream | Every application the caller owns, no pagination. |
| POST | `/api/applications` | `ApplicationCreateRequest` | `201` `ApplicationResponse` | Starts at `SAVED`; writes the first status-history row. |
| PATCH | `/api/applications/{id}` | `ApplicationUpdateRequest` | `ApplicationResponse` | Every field optional; an omitted field leaves the stored value untouched (no way to null a field via this endpoint). |
| DELETE | `/api/applications/{id}` | — | `204` | Cascades its status history / interview rounds. |
| PATCH | `/api/applications/{id}/stage` | `{ stage }` | `ApplicationResponse` | Writes a new status-history row; `400` if `stage` equals current. |
| GET | `/api/applications/{id}/history` | — | `StatusHistoryResponse[]` | Oldest-first; every stage the application has ever been in, with `changedAt`. |
| POST | `/api/applications/{id}/score` | — | `MatchScoreResponse` | See §6 Matching. `400` if the application has no `jobDescriptionText` yet; `404` if no resume profile has been parsed yet. |

`ApplicationCreateRequest` fields: `company*`, `role*` (both ≤255,
required), `location` (≤255), `salaryMin`/`salaryMax` (≥0 ints,
min≤max enforced), `link` (valid URL, ≤2048), `source` (≤100), `notes`
(≤5000), `deadline` (LocalDate), `jobDescriptionText` (free-form, no cap
— this is what `/score` runs against).

`ApplicationUpdateRequest` = same shape, all optional, plus
`resumeVersionId`/`coverLetterVersionId` (must be a Document the caller
owns of the matching type).

`ApplicationResponse` includes everything above plus `currentStage`,
`resumeVersionId`, `coverLetterVersionId`, `matchScore`, `matchedSkills`,
`missingSkills`, `scoredAt` (all null until `/score` has run once),
`createdAt`, `updatedAt`.

---

## 3. Documents (`/api/documents`) — `DocumentController`

Resume/cover-letter file storage (MinIO/S3-compatible). Every upload is a
new, immutable row — no edit path, only upload-new / delete-old.

| Method | Path | Body | Returns | Notes |
|---|---|---|---|---|
| POST | `/api/documents` | multipart: `type` (`RESUME`\|`COVER_LETTER`), `label`, `file` | `201` `DocumentResponse` | Accepts PDF and DOCX only (`415`-equivalent `400` otherwise); rejects over `DOCUMENT_MAX_SIZE_BYTES` (default 10MB) with `413`. |
| GET | `/api/documents` | — (query: `type`, optional) | `DocumentResponse[]` | |
| GET | `/api/documents/{id}` | — | `DocumentDownloadResponse` | A time-limited presigned URL (`MINIO_PRESIGNED_URL_TTL`, default 10min) — the client downloads directly from object storage, not through this endpoint. |
| DELETE | `/api/documents/{id}` | — | `204` | Deletes storage bytes first, then the row. |

`DocumentResponse` = `{ id, type, label, originalFilename, contentType,
size, uploadedAt }`.

---

## 4. Job posting parsing (`/api/job-postings`) — `JobPostingParseController`

"Paste a URL, autofill the form." Calls ml-service's `/parse`, which
scrapes + LLM-extracts (or falls back to a regex/keyword heuristic if
ml-service has no LLM key configured).

| Method | Path | Body | Returns | Notes |
|---|---|---|---|---|
| POST | `/api/job-postings/parse` | `{ url }` | `ParseUrlResponse` | If ml-service is unreachable/slow/erroring, returns `{ available: false, message }` rather than failing the request — caller falls back to manual entry. |

`ParsedJobPosting` (nested in the response's `parsed` field, when
`available: true`): `company`, `role`, `location`, `employmentType`,
`seniority`, `salaryMin`, `salaryMax`, `currency`, `requiredSkills[]`,
`niceToHaveSkills[]`, `summary` — every field individually nullable; a
missing field just means the extractor found nothing there, not an error.
Plus top-level `confidence` (0–1). The full JD text is **not** returned
here — `summary` is 1–2 sentences only, deliberately never auto-filled
into `jobDescriptionText` (would look precise while being misleading for
match scoring).

---

## 5. Resume profile (`/api/resume-profile`) — `ResumeProfileController`

Parses the caller's most recently uploaded resume (via Documents) into a
structured profile — the one-time extraction every application's match
score is computed against.

| Method | Path | Body | Returns | Notes |
|---|---|---|---|---|
| POST | `/api/resume-profile/parse` | — | `ResumeProfileResponse` | No body — always operates on the caller's latest `RESUME`-type document. Re-running this after a new resume upload is what should precede re-scoring. `404` if no resume document exists yet. |
| GET | `/api/resume-profile` | — | `ResumeProfileResponse` | `404` if never parsed. |

`ResumeProfileResponse` = `{ id, sourceDocumentId, profile: { skills[],
yearsExperience, roles[], seniority, summary }, confidence, parsedAt }`.

---

## 6. Matching / scoring

Not its own controller — `POST /api/applications/{id}/score` (§2) is the
entry point, backed by `MatchScoringService`.

- Computes embedding-similarity match % between the caller's resume
  profile and the application's `jobDescriptionText`, via ml-service's
  `/score`.
- **Cached**: a stored result is reused (no ml-service call) as long as
  neither the resume profile nor the application's JD text has changed
  since it was computed. Either changing invalidates the cache.
- `MatchScoreResponse` = `{ matchScore, matchedSkills[], missingSkills[],
  scoredAt, cached }`.

---

## 7. Interview rounds (`/api/applications/{applicationId}/interviews`,
`/api/interviews/{id}`) — `InterviewRoundController`

Per-round interview prep tracking, nested under the owning application for
list/create, flat by round id for update/delete/sync.

| Method | Path | Body | Returns | Notes |
|---|---|---|---|---|
| GET | `/api/interviews/export` | — | CSV file stream | Every round across every application the caller owns. |
| GET | `/api/applications/{applicationId}/interviews` | — | `InterviewRoundResponse[]` | |
| POST | `/api/applications/{applicationId}/interviews` | `InterviewRoundCreateRequest` | `201` `InterviewRoundResponse` | `roundType*` (≤100), `scheduledAt`, `interviewerName` (≤255), `questionsAsked`, `notes` (≤5000), `reflection`. |
| PATCH | `/api/interviews/{id}` | `InterviewRoundUpdateRequest` | `InterviewRoundResponse` | All fields optional, omitted = unchanged (same null-means-untouched rule as Applications' PATCH). |
| DELETE | `/api/interviews/{id}` | — | `204` | |
| POST | `/api/interviews/{id}/calendar-sync` | — | `InterviewRoundResponse` | "Add to Calendar" — creates the Google Calendar event on first call, updates the same event (by stored `googleEventId`) on every call after, so re-clicking never duplicates. `409` if the caller hasn't connected Google Calendar (see §9). |

---

## 8. Analytics (`/api/analytics`) — `AnalyticsController`

Read-only aggregate views over the caller's own application history.

| Method | Path | Returns | Notes |
|---|---|---|---|
| GET | `/api/analytics/funnel` | `FunnelResponse` | `totalApplications` + per-stage counts (`FunnelStageCount[]`) — how many applications have ever reached each stage. |
| GET | `/api/analytics/conversion` | `ConversionResponse` | `StageConversion[]` — from-stage → to-stage counts and rates, adjacent pipeline steps. |
| GET | `/api/analytics/time-in-stage` | `TimeInStageResponse` | `StageDuration[]` — average days spent in each stage before moving on. |
| GET | `/api/analytics/resume-performance` | `ResumePerformanceResponse` | Per resume version (`ResumeVersionPerformance[]`) and per source (`SourceResponseRate[]`) — applications sent vs. responses received, to see which resume/channel actually works. |

---

## 9. Google Calendar sync (`/api/google-calendar`) — `GoogleCalendarController`

OAuth2 authorization-code connect flow (separate from Google sign-in in
§1 — this one needs a client secret + `offline` access for a refresh
token, scoped to `calendar.events` only).

| Method | Path | Auth | Returns | Notes |
|---|---|---|---|---|
| POST | `/api/google-calendar/connect` | ✓ | `{ authorizationUrl }` | Frontend redirects the browser here to start Google's consent screen. `state` param ties the callback back to the caller. |
| GET | `/api/google-calendar/callback` | **Public** | `302` redirect | Google redirects here after consent (full-page nav, can't carry a bearer token — that's why it's public; `state` is the actual auth). Redirects to `GOOGLE_CALENDAR_FRONTEND_REDIRECT_URI?calendarConnected=true|false`. |
| GET | `/api/google-calendar/connection` | ✓ | `{ connected: boolean }` | |
| DELETE | `/api/google-calendar/connection` | ✓ | `204` | Revokes the stored connection. |

Stored refresh token is encrypted at rest (`GOOGLE_CALENDAR_TOKEN_ENCRYPTION_KEY`,
AES-256). Actual event create/update lives in `CalendarSyncService`,
invoked via Interview Rounds' `/calendar-sync` (§7), not its own endpoint.

---

## 10. Background jobs (no HTTP surface)

| Job | Schedule (default) | What it does |
|---|---|---|
| `AutoGhostJob` | `0 0 3 * * *` (3am daily, `AUTO_GHOST_CRON`) | Finds applications stuck without movement for `AUTO_GHOST_STALE_AFTER` (default 14 days) that aren't already `SAVED`/`OFFER`/`REJECTED`/`GHOSTED`, and transitions them to `GHOSTED` — writes a real status-history row, same as a manual stage change. |
| `ReminderJob` | `0 0 9 * * *` (9am daily, `REMINDER_CRON`) | Finds applications with a `deadline` within `REMINDER_LOOKAHEAD` (default 3 days) that aren't already resolved, and emails a reminder for each (async, via `EmailSender` → SMTP; local dev catches these in Mailpit at `localhost:8025`, nothing is really delivered). |

---

## 11. Error handling reference (`GlobalExceptionHandler`)

| Exception | Status | When |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entity not found / not owned by caller |
| `NoResourceFoundException` | 404 | No such route |
| `EmailAlreadyInUseException` | 409 | Signup with an existing email |
| `InvalidRefreshTokenException` | 401 | Bad/expired/reused refresh token |
| `OAuthVerificationException` | 401 | OAuth token/code verification failed |
| `AuthenticationException` (Spring Security) | 401 | Missing/invalid access token |
| `UnsupportedDocumentTypeException` | 400 | Upload isn't PDF/DOCX |
| `DocumentTooLargeException` / `MaxUploadSizeExceededException` | 413 | Upload over the size cap |
| `MethodArgumentNotValidException` | 400 | `@Valid` body failed (field-level messages joined) |
| `MethodArgumentTypeMismatchException` | 400 | Bad path/query param type (e.g. non-UUID `{id}`) |
| `HttpMessageNotReadableException` | 400 | Malformed JSON body |
| `HttpRequestMethodNotSupportedException` | 405 | Wrong HTTP method for the path |
| `MlServiceUnavailableException` | 502 | ml-service unreachable for `/score` or `/resume-profile/parse` (unlike job-posting parse, there's no manual-entry fallback for a match score — this surfaces as a real error) |
| `GoogleCalendarNotConnectedException` | 409 | `/calendar-sync` called without a connected Google Calendar |
| `GoogleCalendarUnavailableException` | 502 | Google Calendar API call failed |
| `IllegalArgumentException` | 400 | Domain-rule violations (e.g. stage-change no-op) |
| anything else | 500 | Generic fallback, message hidden (`"An unexpected error occurred"`) |

---

## 12. Module map (`backend/src/main/java/.../job_trail/`)

| Package | Owns |
|---|---|
| `auth` | Signup/login/OAuth/JWT issuance+refresh, `SecurityConfig`, `JwtAuthFilter` |
| `application` | The core `Application` entity, CRUD, CSV export |
| `status` | `Stage` enum, append-only `StatusHistory` log, stage-change endpoint |
| `document` | Resume/cover-letter upload, MinIO storage, presigned downloads |
| `jobposting` | JD URL parsing — thin client to ml-service's `/parse` |
| `matching` | Resume profile parsing + JD match scoring — clients to ml-service's `/profile` and `/score` |
| `interview` | Interview rounds, CSV export, Google Calendar sync trigger |
| `googlecalendar` | OAuth2 connect flow, encrypted token storage, Calendar API client |
| `analytics` | Funnel/conversion/time-in-stage/resume-performance aggregate queries |
| `scheduler` | `AutoGhostJob`, `ReminderJob` and their supporting services |
| `notification.mail` | Provider-agnostic `EmailSender` interface + SMTP implementation |
| `user` | The `User` entity |
| `common` | `GlobalExceptionHandler`, `ErrorResponse`, base JPA entity, Flyway config |

---

## 13. External integration summary

- **ml-service** (FastAPI, separate deploy) — `/parse`, `/profile`,
  `/score` endpoints called via `MlServiceMatchClient`/`JobPostingParseService`
  clients, retried a couple times on transient failure. Its own LLM
  extraction degrades gracefully (regex/keyword stub) with no key
  configured, so this backend never has to special-case "ml-service has
  no LLM key" — it only ever sees "ml-service unreachable" vs. "ml-service
  returned a (possibly low-confidence) result."
- **Google** — Identity Services ID-token verification (sign-in),
  separate OAuth2 authorization-code flow (Calendar sync).
- **GitHub** — OAuth2 code exchange (sign-in).
- **SMTP** — deadline reminder emails, provider-agnostic (Mailpit in
  local dev).
- **MinIO/S3** — document storage, presigned URLs for download.
