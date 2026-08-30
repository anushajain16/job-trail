import { api } from '@/api/client'
import type { MatchScoreResponse, Uuid } from '@/api/types'

/**
 * 400 if the application has no `jobDescriptionText`, 404 if no résumé
 * profile has been parsed, 502 if ml-service is unreachable — there is no
 * fallback for a score, unlike job-posting parsing.
 */
export function scoreApplication(id: Uuid) {
  return api.post<MatchScoreResponse>(`/api/applications/${id}/score`)
}
