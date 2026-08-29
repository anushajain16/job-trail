import { authFetch } from '@/lib/api-client'
import type { InterviewRound, InterviewRoundInput } from '@/features/interviews/types'

/** Mirrors applications/api.ts's toRequestBody: roundType is the only
 * required field, everything else is included only when non-blank —
 * InterviewRoundUpdateRequest has no way to null a field via PATCH. */
function toRequestBody(input: InterviewRoundInput): Record<string, unknown> {
  const body: Record<string, unknown> = { roundType: input.roundType.trim() }
  const optional: Record<string, string> = {
    interviewerName: input.interviewerName,
    questionsAsked: input.questionsAsked,
    notes: input.notes,
    reflection: input.reflection,
  }
  for (const [key, value] of Object.entries(optional)) {
    const trimmed = value.trim()
    if (trimmed) body[key] = trimmed
  }
  // datetime-local ("2026-09-05T20:30") has no timezone — treated as local
  // time and converted to the instant the backend expects.
  if (input.scheduledAt.trim()) body.scheduledAt = new Date(input.scheduledAt).toISOString()
  return body
}

export const interviewRoundsApi = {
  list: (applicationId: string) => authFetch<InterviewRound[]>(`/api/applications/${applicationId}/interviews`),

  create: (applicationId: string, input: InterviewRoundInput) =>
    authFetch<InterviewRound>(`/api/applications/${applicationId}/interviews`, {
      method: 'POST',
      body: toRequestBody(input),
    }),

  update: (id: string, input: InterviewRoundInput) =>
    authFetch<InterviewRound>(`/api/interviews/${id}`, { method: 'PATCH', body: toRequestBody(input) }),

  remove: (id: string) => authFetch<null>(`/api/interviews/${id}`, { method: 'DELETE' }),

  // Creates the calendar event the first time, updates that same event on
  // every call after that (see backend CalendarSyncService) — safe to
  // call again, never duplicates.
  calendarSync: (id: string) => authFetch<InterviewRound>(`/api/interviews/${id}/calendar-sync`, { method: 'POST' }),
}
