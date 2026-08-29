import type { HistoryEntry } from '@/features/subway-map/api'
import type { Stage } from '@/features/applications/types'

/** The "main line" — every application starts at Saved and, absent an
 * exit, progresses rightward through these in order. Rejected/Ghosted are
 * exits, not stations on this line — see {@link LineStatus}. */
export const LINE_STAGES: Stage[] = ['SAVED', 'APPLIED', 'SCREEN', 'INTERVIEW', 'FINAL', 'OFFER']

export type LineStatus = 'active' | 'offer' | 'rejected' | 'ghosted'

export const STATUS_LABELS: Record<LineStatus, string> = {
  active: 'In progress',
  offer: 'Offer',
  rejected: 'Rejected',
  ghosted: 'Ghosted',
}

export interface LineStation {
  stage: Stage
  index: number
  reached: boolean
  /** When the application first arrived here — null if never reached. */
  reachedAt: string | null
}

export interface SubwayLine {
  status: LineStatus
  /** Furthest index into LINE_STAGES this application's history ever
   * touched — the travelled track goes this far even if the line later
   * exited (rejected/ghosted) or, in principle, moved backward. */
  reachedIndex: number
  stations: LineStation[]
  /** Index of the live train marker — null once the line has exited
   * (rejected/ghosted): there's no "current position" for a dead line. */
  currentIndex: number | null
  /** When the exit (rejection/ghosting) happened, for the terminal marker
   * and its tooltip. Null for an active or offer-terminated line. */
  exitedAt: string | null
}

/** Status is a function of one stage, not the full history — usable
 * immediately from `Application.currentStage` (list/filter bar) as well
 * as from a history entry's `stage` (the map's per-line track). The two
 * always agree: currentStage is defined as a cached read of the latest
 * status_history row (see backend's Application.java doc comment). */
export function categorizeStage(stage: Stage): LineStatus {
  if (stage === 'REJECTED') return 'rejected'
  if (stage === 'GHOSTED') return 'ghosted'
  if (stage === 'OFFER') return 'offer'
  return 'active'
}

/**
 * Turns one application's raw event log (GET /api/applications/{id}/history,
 * oldest-first) into everything the map needs to draw its line. Status
 * comes from the *last* entry, not "was ever rejected" — the backend
 * doesn't forbid a stage moving backward (e.g. reopened after rejection),
 * so a line currently back in the active pipeline reads as active even if
 * REJECTED appears earlier in its history.
 */
export function buildSubwayLine(history: HistoryEntry[]): SubwayLine {
  if (history.length === 0) {
    // Shouldn't happen — StatusHistoryService.recordInitial always writes
    // the first row at creation — but don't let a data gap crash the map.
    return {
      status: 'active',
      reachedIndex: 0,
      stations: LINE_STAGES.map((stage, index) => ({ stage, index, reached: index === 0, reachedAt: null })),
      currentIndex: 0,
      exitedAt: null,
    }
  }

  const firstReachedAt = new Map<Stage, string>()
  let reachedIndex = 0
  for (const entry of history) {
    const index = LINE_STAGES.indexOf(entry.stage)
    if (index === -1) continue // REJECTED/GHOSTED aren't on the main line
    if (!firstReachedAt.has(entry.stage)) firstReachedAt.set(entry.stage, entry.changedAt)
    reachedIndex = Math.max(reachedIndex, index)
  }

  const last = history[history.length - 1]
  const status = categorizeStage(last.stage)
  const currentIndex = status === 'rejected' || status === 'ghosted' ? null : LINE_STAGES.indexOf(last.stage)

  return {
    status,
    reachedIndex,
    stations: LINE_STAGES.map((stage, index) => ({
      stage,
      index,
      reached: index <= reachedIndex,
      reachedAt: firstReachedAt.get(stage) ?? null,
    })),
    currentIndex: currentIndex === -1 ? null : currentIndex,
    exitedAt: status === 'rejected' || status === 'ghosted' ? last.changedAt : null,
  }
}
