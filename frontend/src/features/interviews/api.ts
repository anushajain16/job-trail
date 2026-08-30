import { api, requestBlob } from '@/api/client'
import type {
  InterviewRoundCreateRequest,
  InterviewRoundResponse,
  InterviewRoundUpdateRequest,
  Uuid,
} from '@/api/types'

export function listInterviewRounds(applicationId: Uuid) {
  return api.get<InterviewRoundResponse[]>(`/api/applications/${applicationId}/interviews`)
}

export function createInterviewRound(applicationId: Uuid, body: InterviewRoundCreateRequest) {
  return api.post<InterviewRoundResponse>(`/api/applications/${applicationId}/interviews`, body)
}

/** Flat by round id — the owning application is implied. */
export function updateInterviewRound(id: Uuid, body: InterviewRoundUpdateRequest) {
  return api.patch<InterviewRoundResponse>(`/api/interviews/${id}`, body)
}

export function deleteInterviewRound(id: Uuid) {
  return api.delete(`/api/interviews/${id}`)
}

/**
 * Creates the Google Calendar event on the first call and updates the same
 * event on every call after (by stored `googleEventId`), so re-clicking
 * never duplicates. 409 when Google Calendar is not connected.
 */
export function syncInterviewToCalendar(id: Uuid) {
  return api.post<InterviewRoundResponse>(`/api/interviews/${id}/calendar-sync`)
}

export function exportInterviewsCsv() {
  return requestBlob('/api/interviews/export')
}
