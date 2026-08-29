// Mirrors backend/.../interview/dto/InterviewRoundResponse.java.
export interface InterviewRound {
  id: string
  applicationId: string
  roundType: string
  scheduledAt: string | null // ISO instant, Instant on the wire
  interviewerName: string | null
  questionsAsked: string | null
  notes: string | null
  reflection: string | null
  createdAt: string
  updatedAt: string
}

/** Shape the add/edit form collects and submits. Form fields are always
 * strings; api.ts trims and, for optional ones, omits blanks from the
 * request body — same reasoning as ApplicationInput (there's no way to
 * null a field out via PATCH, so an empty string would overwrite rather
 * than clear it). */
export interface InterviewRoundInput {
  roundType: string
  scheduledAt: string // datetime-local value, or ''
  interviewerName: string
  questionsAsked: string
  notes: string
  reflection: string
}

export const EMPTY_INTERVIEW_ROUND_INPUT: InterviewRoundInput = {
  roundType: '',
  scheduledAt: '',
  interviewerName: '',
  questionsAsked: '',
  notes: '',
  reflection: '',
}

export function interviewRoundToInput(round: InterviewRound): InterviewRoundInput {
  return {
    roundType: round.roundType,
    scheduledAt: round.scheduledAt ? toDatetimeLocal(round.scheduledAt) : '',
    interviewerName: round.interviewerName ?? '',
    questionsAsked: round.questionsAsked ?? '',
    notes: round.notes ?? '',
    reflection: round.reflection ?? '',
  }
}

// Instant ("2026-09-05T15:00:00Z") -> the local value <input
// type="datetime-local"> expects ("2026-09-05T20:30"), trimmed to minutes.
function toDatetimeLocal(iso: string): string {
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
