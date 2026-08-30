/**
 * Signage-style formatters. Dates on the map read like a departure board —
 * uppercase, abbreviated, no punctuation.
 */

const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC']

/** `2026-03-14T…` → `MAR 14`. Returns an em dash for null/invalid input. */
export function formatBoardDate(value: string | null | undefined): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return `${MONTHS[d.getMonth()]} ${String(d.getDate()).padStart(2, '0')}`
}

/** `2026-03-14T…` → `MAR 14 2026`. */
export function formatBoardDateFull(value: string | null | undefined): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return `${formatBoardDate(value)} ${d.getFullYear()}`
}

/** `2026-03-14T09:30…` → `MAR 14 · 09:30`. */
export function formatBoardDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${formatBoardDate(value)} · ${hh}:${mm}`
}

/** ISO date (`2026-03-14`) for `<input type="date">`, from any ISO string. */
export function toDateInputValue(value: string | null | undefined): string {
  if (!value) return ''
  return value.slice(0, 10)
}

/** ISO local datetime (`2026-03-14T09:30`) for `<input type="datetime-local">`. */
export function toDateTimeInputValue(value: string | null | undefined): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** `112640` → `110 KB`. */
export function formatBytes(bytes: number | null | undefined): string {
  if (bytes == null) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** `0.734` → `73%`. */
export function formatPercent(ratio: number | null | undefined, digits = 0): string {
  if (ratio == null) return '—'
  return `${(ratio * 100).toFixed(digits)}%`
}

/** Days between now and an ISO date; negative when the date has passed. */
export function daysUntil(value: string | null | undefined): number | null {
  if (!value) return null
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return null
  const ms = d.setHours(0, 0, 0, 0) - new Date().setHours(0, 0, 0, 0)
  return Math.round(ms / 86_400_000)
}
