import { cn } from '@/lib/cn'
import { formatBoardDate } from '@/lib/format'
import { GREY, PAPER, RULE_SOFT, STATIONS, isSuspendedStage, resolvedLineColor, stationIndex } from '@/lib/design'
import type { Stage, StatusHistoryResponse } from '@/api/types'

interface TimelineStop {
  name: string
  date: string | null
  state: 'passed' | 'current' | 'ahead'
}

/**
 * Turn the append-only history log into stops. A stage can be revisited —
 * the log is not monotonic — so each station shows the *first* time it was
 * reached, and everything up to the furthest station reached counts as
 * travelled.
 */
export function buildTimeline(
  currentStage: Stage,
  history: StatusHistoryResponse[] = [],
): TimelineStop[] {
  const firstVisit = new Map<Stage, string>()
  for (const entry of history) {
    if (!firstVisit.has(entry.stage)) firstVisit.set(entry.stage, entry.changedAt)
  }

  const furthest = history.reduce((max, entry) => {
    const index = stationIndex(entry.stage)
    return index != null && index > max ? index : max
  }, stationIndex(currentStage) ?? -1)

  const currentIndex = stationIndex(currentStage)

  const stops: TimelineStop[] = STATIONS.map((station, index) => ({
    name: station,
    date: firstVisit.get(station) ?? null,
    state: index === currentIndex ? 'current' : index <= furthest ? 'passed' : 'ahead',
  }))

  // A suspended line still occupies its last station; the suspension is an
  // extra terminal stop rather than a seventh station on the map.
  if (isSuspendedStage(currentStage)) {
    stops.push({
      name: currentStage,
      date: firstVisit.get(currentStage) ?? null,
      state: 'current',
    })
  }

  return stops
}

export function StatusTimeline({
  applicationId,
  currentStage,
  history,
  className,
}: {
  applicationId: string
  currentStage: Stage
  history?: StatusHistoryResponse[]
  className?: string
}) {
  const color = resolvedLineColor(applicationId, currentStage)
  const stops = buildTimeline(currentStage, history)

  return (
    <ol className={cn('m-0 list-none p-0', className)}>
      {stops.map((stop) => {
        const active = stop.state !== 'ahead'
        return (
          <li key={stop.name} className="flex items-center gap-3 py-[7px]">
            <span
              aria-hidden
              className="h-[11px] w-[11px] shrink-0 rounded-full border-2 box-border"
              style={{
                background: stop.state === 'current' ? color : stop.state === 'passed' ? '#FFFFFF' : PAPER,
                borderColor: active ? color : RULE_SOFT,
              }}
            />
            <span
              className="flex-1 font-mono text-[10px] tracking-[0.08em] uppercase"
              style={{
                color: active ? undefined : GREY,
                fontWeight: stop.state === 'current' ? 700 : 400,
              }}
            >
              {stop.name}
            </span>
            <span className="font-mono text-[9px] tracking-[0.06em] text-muted">
              {formatBoardDate(stop.date)}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
