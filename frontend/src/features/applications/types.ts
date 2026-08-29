// Mirrors backend/.../status/Stage.java exactly — keep in sync by hand,
// there's no shared schema between the two apps.
export const STAGES = [
  'SAVED',
  'APPLIED',
  'SCREEN',
  'INTERVIEW',
  'FINAL',
  'OFFER',
  'REJECTED',
  'GHOSTED',
] as const

export type Stage = (typeof STAGES)[number]

export const STAGE_LABELS: Record<Stage, string> = {
  SAVED: 'Saved',
  APPLIED: 'Applied',
  SCREEN: 'Screen',
  INTERVIEW: 'Interview',
  FINAL: 'Final',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  GHOSTED: 'Ghosted',
}

// Mirrors backend/.../application/dto/ApplicationResponse.java.
export interface Application {
  id: string
  company: string
  role: string
  location: string | null
  salaryMin: number | null
  salaryMax: number | null
  link: string | null
  source: string | null
  notes: string | null
  jobDescriptionText: string | null
  deadline: string | null // ISO date (YYYY-MM-DD), LocalDate on the wire
  currentStage: Stage
  resumeVersionId: string | null
  coverLetterVersionId: string | null
  // Null until POST /{id}/score has run at least once — see
  // features/matching. matchedSkills/missingSkills are [] (never null)
  // once a score exists.
  matchScore: number | null
  matchedSkills: string[]
  missingSkills: string[]
  scoredAt: string | null
  createdAt: string
  updatedAt: string
}

// Spring Data's Page<T> JSON shape (org.springframework.data.domain.Page) —
// only the fields this app actually reads.
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number // current page, 0-based
  size: number
  first: boolean
  last: boolean
}

export interface ApplicationListParams {
  page: number
  size: number
}

/** Shape the create/edit form collects and submits. Optional fields are
 * omitted from the actual request body when blank (see api.ts) rather
 * than sent as "" — ApplicationUpdateRequest has no way to null a field
 * out, and an empty string would just overwrite it with "" instead. */
export interface ApplicationInput {
  company: string
  role: string
  location: string
  salaryMin: string // form fields are always strings; parsed/dropped in api.ts
  salaryMax: string
  link: string
  source: string
  notes: string
  jobDescriptionText: string
  deadline: string
}

export const EMPTY_APPLICATION_INPUT: ApplicationInput = {
  company: '',
  role: '',
  location: '',
  salaryMin: '',
  salaryMax: '',
  link: '',
  source: '',
  notes: '',
  jobDescriptionText: '',
  deadline: '',
}

export function applicationToInput(application: Application): ApplicationInput {
  return {
    company: application.company,
    role: application.role,
    location: application.location ?? '',
    salaryMin: application.salaryMin?.toString() ?? '',
    salaryMax: application.salaryMax?.toString() ?? '',
    link: application.link ?? '',
    source: application.source ?? '',
    notes: application.notes ?? '',
    jobDescriptionText: application.jobDescriptionText ?? '',
    deadline: application.deadline ?? '',
  }
}
