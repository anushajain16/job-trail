import { api } from '@/api/client'
import type { ParseUrlResponse } from '@/api/types'

/**
 * Never rejects on ml-service being down — the backend answers
 * `{ available: false, message }` so the caller can fall back to manual
 * entry instead of losing the form.
 */
export function parseJobPostingUrl(url: string) {
  return api.post<ParseUrlResponse>('/api/job-postings/parse', { url })
}
