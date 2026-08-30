import { api } from '@/api/client'
import type { ResumeProfileResponse } from '@/api/types'

/** Always parses the caller's most recent RESUME document — no body. */
export function parseResumeProfile() {
  return api.post<ResumeProfileResponse>('/api/resume-profile/parse')
}

export function getResumeProfile() {
  return api.get<ResumeProfileResponse>('/api/resume-profile')
}
