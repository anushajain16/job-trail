import { STATIONS, stationIndex } from '@/lib/design'
import type { ApplicationResponse, Stage, StatusHistoryResponse } from '@/api/types'

export const ROW_HEIGHT = 68
export const LABEL_WIDTH = 180
export const LABEL_GAP = 24
export const TRACK_TAIL = 36

/** Percentage along the track for a station index. */
export function pctFor(index: number): number {
  return (index / (STATIONS.length - 1)) * 100
}

/**
 * Where a line's train sits. On-map stages read straight off the stage;
 * REJECTED/GHOSTED have no station of their own, so the train stalls at
 * the furthest station the line actually reached (from its history).
 */
export function positionIndex(
  stage: Stage,
  history: StatusHistoryResponse[] | undefined,
): number {
  const direct = stationIndex(stage)
  if (direct != null) return direct
  if (!history?.length) return 1 // Suspended, history not loaded yet — assume APPLIED.
  return history.reduce((max, entry) => {
    const index = stationIndex(entry.stage)
    return index != null && index > max ? index : max
  }, 0)
}

export interface Interchange {
  left: number
  top: number
  reason: string
}

/**
 * Interchange markers: two adjacent lines sharing a company, a résumé
 * version, or a source travel together, so the diagram links them the way
 * a transit map links platforms.
 */
export function buildInterchanges(
  applications: ApplicationResponse[],
  positions: number[],
): Interchange[] {
  const interchanges: Interchange[] = []
  for (let i = 0; i < applications.length - 1; i++) {
    const a = applications[i]
    const b = applications[i + 1]
    const reason =
      a.company.toLowerCase() === b.company.toLowerCase()
        ? 'Same company'
        : a.resumeVersionId && a.resumeVersionId === b.resumeVersionId
          ? 'Same résumé version'
          : a.source && b.source && a.source.toLowerCase() === b.source.toLowerCase()
            ? 'Same source'
            : null
    if (!reason) continue
    interchanges.push({
      left: pctFor(Math.min(positions[i], positions[i + 1])),
      top: i * ROW_HEIGHT + ROW_HEIGHT / 2 - 7.5,
      reason,
    })
  }
  return interchanges
}
