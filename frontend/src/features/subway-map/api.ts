import { authFetch } from '@/lib/api-client'
import type { Stage } from '@/features/applications/types'

// Mirrors backend/.../status/dto/StatusHistoryResponse.java. Ordered
// oldest-first (StatusHistoryRepository.findByApplicationIdOrderByCreatedAtAsc).
export interface HistoryEntry {
  id: string
  stage: Stage
  changedAt: string
}

export const historyApi = {
  get: (applicationId: string) => authFetch<HistoryEntry[]>(`/api/applications/${applicationId}/history`),
}
