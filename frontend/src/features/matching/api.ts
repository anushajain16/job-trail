import { authFetch } from '@/lib/api-client'

// Mirrors backend/.../matching/dto/MlResumeProfile.java.
export interface ResumeProfile {
  skills: string[]
  yearsExperience: number | null
  roles: string[]
  seniority: string | null
  summary: string | null
}

// Mirrors backend/.../matching/dto/ResumeProfileResponse.java.
export interface ResumeProfileResult {
  id: string
  sourceDocumentId: string
  profile: ResumeProfile
  confidence: number
  parsedAt: string
}

// Mirrors backend/.../matching/dto/MatchScoreResponse.java.
export interface MatchScoreResult {
  matchScore: number
  matchedSkills: string[]
  missingSkills: string[]
  scoredAt: string
  cached: boolean
}

export const resumeProfileApi = {
  parse: () => authFetch<ResumeProfileResult>('/api/resume-profile/parse', { method: 'POST' }),
  get: () => authFetch<ResumeProfileResult>('/api/resume-profile'),
}

export const matchApi = {
  score: (applicationId: string) =>
    authFetch<MatchScoreResult>(`/api/applications/${applicationId}/score`, { method: 'POST' }),
}
