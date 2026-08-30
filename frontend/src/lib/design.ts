import type { Stage } from '@/api/types'

/* ── Stations ───────────────────────────────────────────────────────────
   The map's x-axis. REJECTED / GHOSTED are not stations — they suspend a
   line at whatever station it last reached, so they live outside this list.
   ---------------------------------------------------------------------- */
export const STATIONS = ['SAVED', 'APPLIED', 'SCREEN', 'INTERVIEW', 'FINAL', 'OFFER'] as const
export type Station = (typeof STATIONS)[number]

export const ALL_STAGES: Stage[] = [...STATIONS, 'REJECTED', 'GHOSTED']

/** Percentage offset of a station along the track. */
export function stationLeft(index: number): number {
  return (index / (STATIONS.length - 1)) * 100
}

/** Index of a stage on the map, or `null` for the off-map (suspended) ones. */
export function stationIndex(stage: Stage): number | null {
  const i = (STATIONS as readonly string[]).indexOf(stage)
  return i === -1 ? null : i
}

export function isSuspendedStage(stage: Stage): boolean {
  return stage === 'REJECTED' || stage === 'GHOSTED'
}

export function isTerminusStage(stage: Stage): boolean {
  return stage === 'OFFER'
}

/* ── Palette ────────────────────────────────────────────────────────────
   Six line colours, assigned deterministically per application so a line
   keeps its colour across reloads and refetches.
   ---------------------------------------------------------------------- */
export const LINE_COLORS = [
  '#D85A30', // coral
  '#1D9E75', // teal
  '#378ADD', // blue
  '#7F77DD', // purple
  '#D4537E', // pink
  '#BA7517', // amber
] as const

export const INK = '#26241F'
export const INK_SOFT = '#55534C'
export const MUTED = '#8A867B'
export const GREY = '#B4B2A9'
export const PAPER = '#F4F1EA'
export const RULE = '#DDD8CC'
export const RULE_SOFT = '#CFCBBF'

/** Stable colour for an application id (FNV-1a over the id). */
export function lineColorFor(id: string): string {
  let hash = 0x811c9dc5
  for (let i = 0; i < id.length; i++) {
    hash ^= id.charCodeAt(i)
    hash = Math.imul(hash, 0x01000193)
  }
  return LINE_COLORS[Math.abs(hash) % LINE_COLORS.length]
}

/** The colour a line actually renders in — suspended lines desaturate. */
export function resolvedLineColor(id: string, stage: Stage): string {
  return isSuspendedStage(stage) ? GREY : lineColorFor(id)
}

/** Signage label for a stage, e.g. `IN TRANSIT — SCREEN`. */
export function stageLabel(stage: Stage): string {
  switch (stage) {
    case 'OFFER':
      return 'OFFER — TERMINUS'
    case 'REJECTED':
      return 'SUSPENDED — REJECTED'
    case 'GHOSTED':
      return 'SUSPENDED — GHOSTED'
    default:
      return `IN TRANSIT — ${stage}`
  }
}

/** Ink colour for a stage chip/badge. */
export function stageColor(id: string, stage: Stage): string {
  if (isSuspendedStage(stage)) return MUTED
  if (stage === 'OFFER') return lineColorFor(id)
  return INK
}
