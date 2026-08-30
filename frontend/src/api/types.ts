/* Wire types — one-to-one with the Spring Boot DTOs in docs/backend.md. */

export type Uuid = string
/** ISO-8601 instant, e.g. `2026-03-14T09:30:00Z`. */
export type IsoDateTime = string
/** ISO-8601 local date, e.g. `2026-03-14`. */
export type IsoDate = string

/* ── §11 Errors ────────────────────────────────────────────────────── */
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

/* ── §1 Auth ───────────────────────────────────────────────────────── */
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
}

export interface CurrentUser {
  id: Uuid
  email: string
}

export type OAuthProvider = 'GOOGLE' | 'GITHUB'

/* ── §2 Applications ───────────────────────────────────────────────── */
export type Stage =
  | 'SAVED'
  | 'APPLIED'
  | 'SCREEN'
  | 'INTERVIEW'
  | 'FINAL'
  | 'OFFER'
  | 'REJECTED'
  | 'GHOSTED'

export interface ApplicationResponse {
  id: Uuid
  company: string
  role: string
  location: string | null
  salaryMin: number | null
  salaryMax: number | null
  link: string | null
  source: string | null
  notes: string | null
  deadline: IsoDate | null
  jobDescriptionText: string | null
  currentStage: Stage
  resumeVersionId: Uuid | null
  coverLetterVersionId: Uuid | null
  matchScore: number | null
  matchedSkills: string[] | null
  missingSkills: string[] | null
  scoredAt: IsoDateTime | null
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

export interface ApplicationCreateRequest {
  company: string
  role: string
  location?: string | null
  salaryMin?: number | null
  salaryMax?: number | null
  link?: string | null
  source?: string | null
  notes?: string | null
  deadline?: IsoDate | null
  jobDescriptionText?: string | null
}

/** Every field optional; an omitted field leaves the stored value untouched. */
export interface ApplicationUpdateRequest extends Partial<ApplicationCreateRequest> {
  resumeVersionId?: Uuid | null
  coverLetterVersionId?: Uuid | null
}

export interface StatusHistoryResponse {
  id: Uuid
  stage: Stage
  changedAt: IsoDateTime
}

/** Spring Data `Page<T>` envelope. */
export interface Page<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface PageParams {
  page?: number
  size?: number
  sort?: string
}

/* ── §3 Documents ──────────────────────────────────────────────────── */
export type DocumentType = 'RESUME' | 'COVER_LETTER'

export interface DocumentResponse {
  id: Uuid
  type: DocumentType
  label: string | null
  originalFilename: string
  contentType: string
  size: number
  uploadedAt: IsoDateTime
}

export interface DocumentDownloadResponse {
  /** Time-limited presigned URL — the client downloads from object storage. */
  downloadUrl: string
  filename: string
  contentType: string
  expiresAt: IsoDateTime
}

/* ── §4 Job posting parsing ────────────────────────────────────────── */
export interface ParsedJobPosting {
  company: string | null
  role: string | null
  location: string | null
  employmentType: string | null
  seniority: string | null
  salaryMin: number | null
  salaryMax: number | null
  currency: string | null
  requiredSkills: string[] | null
  niceToHaveSkills: string[] | null
  summary: string | null
}

export interface ParseUrlResponse {
  available: boolean
  message: string | null
  parsed: ParsedJobPosting | null
  confidence: number | null
}

/* ── §5 Resume profile ─────────────────────────────────────────────── */
export interface ResumeProfile {
  skills: string[]
  /**
   * snake_case on purpose: this object is ml-service's Pydantic schema
   * passed through verbatim (the backend maps it with @JsonProperty and
   * never re-derives it), so the wire really does say `years_experience`.
   */
  years_experience: number | null
  roles: string[]
  seniority: string | null
  summary: string | null
}

export interface ResumeProfileResponse {
  id: Uuid
  sourceDocumentId: Uuid
  profile: ResumeProfile
  confidence: number | null
  parsedAt: IsoDateTime
}

/* ── §6 Matching ───────────────────────────────────────────────────── */
export interface MatchScoreResponse {
  matchScore: number
  matchedSkills: string[]
  missingSkills: string[]
  scoredAt: IsoDateTime
  cached: boolean
}

/* ── §7 Interview rounds ───────────────────────────────────────────── */
export interface InterviewRoundResponse {
  id: Uuid
  applicationId: Uuid
  roundType: string
  scheduledAt: IsoDateTime | null
  interviewerName: string | null
  questionsAsked: string | null
  notes: string | null
  reflection: string | null
  googleEventId: string | null
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

export interface InterviewRoundCreateRequest {
  roundType: string
  scheduledAt?: IsoDateTime | null
  interviewerName?: string | null
  questionsAsked?: string | null
  notes?: string | null
  reflection?: string | null
}

export type InterviewRoundUpdateRequest = Partial<InterviewRoundCreateRequest>

/* ── §8 Analytics ──────────────────────────────────────────────────── */
export interface FunnelStageCount {
  stage: Stage
  applications: number
}

export interface FunnelResponse {
  totalApplications: number
  stages: FunnelStageCount[]
}

export interface StageConversion {
  fromStage: Stage
  toStage: Stage
  fromCount: number
  toCount: number
  conversionRate: number
}

export interface SourceResponseRate {
  source: string | null
  totalApplications: number
  respondedApplications: number
  responseRate: number
}

export interface ConversionResponse {
  stageConversions: StageConversion[]
  responseRateBySource: SourceResponseRate[]
}

export interface StageDuration {
  stage: Stage
  averageDays: number
  sampleSize: number
}

export interface TimeInStageResponse {
  stages: StageDuration[]
}

export interface ResumeVersionPerformance {
  documentId: Uuid | null
  label: string | null
  totalApplications: number
  respondedApplications: number
  responseRate: number
}

export interface ResumePerformanceResponse {
  versions: ResumeVersionPerformance[]
}

/* ── §9 Google Calendar ────────────────────────────────────────────── */
export interface CalendarConnectResponse {
  authorizationUrl: string
}

export interface CalendarConnectionResponse {
  connected: boolean
}
