import { useState } from 'react'
import { useQueries } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import { getStatusHistory } from '@/features/applications/api'
import { STATIONS, isSuspendedStage, isTerminusStage, resolvedLineColor } from '@/lib/design'
import type { ApplicationResponse, StatusHistoryResponse } from '@/api/types'
import {
  LABEL_GAP,
  LABEL_WIDTH,
  ROW_HEIGHT,
  TRACK_TAIL,
  buildInterchanges,
  pctFor,
  positionIndex,
} from './geometry'
import { MapLegend } from './map-legend'

/** Station names + the hairlines they drop through the whole diagram. */
function StationScale() {
  return (
    <div className="relative h-[30px]">
      {STATIONS.map((station, index) => (
        <span
          key={station}
          className="absolute -translate-x-1/2 font-mono text-[10px] tracking-[0.08em] whitespace-nowrap text-muted"
          style={{ left: `${pctFor(index)}%` }}
        >
          {station}
        </span>
      ))}
    </div>
  )
}

interface MapLineProps {
  application: ApplicationResponse
  position: number
  hovered: boolean
  onHover: (id: string | null) => void
  onOpen: (id: string) => void
}

/** One application = one horizontal line, label plate + track + train. */
function MapLine({ application, position, hovered, onHover, onOpen }: MapLineProps) {
  const suspended = isSuspendedStage(application.currentStage)
  const color = resolvedLineColor(application.id, application.currentStage)
  const pct = pctFor(position)

  return (
    <div className="relative z-1 flex items-center" style={{ height: ROW_HEIGHT }}>
      <div
        className="shrink-0 text-right"
        style={{ width: LABEL_WIDTH, marginRight: LABEL_GAP, opacity: suspended ? 0.4 : 1 }}
      >
        <button
          type="button"
          onClick={() => onOpen(application.id)}
          className="cursor-pointer border-0 bg-transparent p-0 text-right font-mono"
        >
          <span className="flex items-center justify-end gap-2">
            <span
              className="text-[11px] font-semibold tracking-[0.05em] uppercase"
              style={{ color: suspended ? '#8A867B' : undefined }}
            >
              {application.company}
            </span>
            <span
              aria-hidden
              className="h-2 w-2 shrink-0 rounded-full"
              style={{ background: color }}
            />
          </span>
          <span className="mt-[3px] block text-[9px] tracking-[0.06em] text-muted">
            {application.role}
          </span>
        </button>
      </div>

      <div
        className="relative flex-1"
        style={{ height: ROW_HEIGHT, marginRight: TRACK_TAIL, opacity: suspended ? 0.4 : 1 }}
      >
        {/* Full route, then the travelled segment over it. */}
        <span
          className="absolute top-1/2 right-0 left-0 h-[7px] -translate-y-1/2 rounded-[4px] opacity-13"
          style={{ background: color }}
        />
        <span
          className="absolute top-1/2 left-0 h-[7px] -translate-y-1/2 rounded-[4px]"
          style={{ width: `${pct}%`, background: color }}
        />

        {Array.from({ length: position }, (_, index) => (
          <span
            key={index}
            className="absolute top-1/2 box-border h-[9px] w-[9px] -translate-x-1/2 -translate-y-1/2 rounded-full border-2 bg-white"
            style={{ left: `${pctFor(index)}%`, borderColor: color }}
          />
        ))}

        <button
          type="button"
          aria-label={`${application.company} — ${application.role}`}
          onMouseEnter={() => onHover(application.id)}
          onMouseLeave={() => onHover(null)}
          onFocus={() => onHover(application.id)}
          onBlur={() => onHover(null)}
          onClick={() => onOpen(application.id)}
          className="absolute top-1/2 z-5 -translate-x-1/2 -translate-y-1/2 cursor-pointer border-0 bg-transparent p-0"
          style={{ left: `${pct}%` }}
        >
          {isTerminusStage(application.currentStage) ? (
            <span
              className="flex h-[18px] w-[46px] items-center justify-center rounded-[9px] font-mono text-[8px] font-bold tracking-[0.14em] text-white"
              style={{ background: color }}
            >
              END
            </span>
          ) : (
            <span
              className="relative block h-3.5 w-[26px] rounded-[7px]"
              style={{ background: color }}
            >
              {suspended && (
                <span className="absolute top-1/2 left-1/2 h-[22px] w-[2px] -translate-x-1/2 -translate-y-1/2 bg-ink-soft" />
              )}
            </span>
          )}

          {hovered && (
            <span className="absolute bottom-[22px] left-1/2 z-30 block -translate-x-1/2 rounded-[3px] bg-ink px-2.5 py-[7px] font-mono text-[10px] leading-[1.6] tracking-[0.05em] whitespace-nowrap text-paper">
              <span className="block font-bold">{application.company}</span>
              <span className="block text-grey">
                {application.role} · {application.currentStage}
              </span>
            </span>
          )}
        </button>
      </div>
    </div>
  )
}

export interface TransitMapProps {
  applications: ApplicationResponse[]
  showInterchanges: boolean
  onOpen: (id: string) => void
}

/** The whole network on one diagram. */
export function TransitMap({ applications, showInterchanges, onOpen }: TransitMapProps) {
  const [hoveredId, setHoveredId] = useState<string | null>(null)

  // Suspended lines stall wherever they got to, which only their history
  // knows. Fetch it for those lines only — the same query the drawer uses,
  // so opening one afterwards is already warm.
  const suspended = applications.filter((application) => isSuspendedStage(application.currentStage))
  const historyQueries = useQueries({
    queries: suspended.map((application) => ({
      queryKey: queryKeys.applications.history(application.id),
      queryFn: () => getStatusHistory(application.id),
      staleTime: 5 * 60_000,
    })),
  })

  const historyById = new Map<string, StatusHistoryResponse[] | undefined>(
    suspended.map((application, index) => [application.id, historyQueries[index]?.data]),
  )

  const positions = applications.map((application) =>
    positionIndex(application.currentStage, historyById.get(application.id)),
  )
  const interchanges = showInterchanges ? buildInterchanges(applications, positions) : []

  const trackInset = { left: LABEL_WIDTH + LABEL_GAP, right: TRACK_TAIL }

  return (
    <div>
      <div className="min-w-[860px] overflow-x-auto">
        <div className="relative">
          <div className="flex">
            <div className="shrink-0" style={{ width: LABEL_WIDTH, marginRight: LABEL_GAP }} />
            <div className="relative flex-1" style={{ marginRight: TRACK_TAIL }}>
              <StationScale />
            </div>
          </div>

          <div className="relative">
            {/* Station hairlines, behind every line. */}
            <div
              aria-hidden
              className="pointer-events-none absolute z-0"
              style={{ ...trackInset, top: -4, bottom: -4 }}
            >
              {STATIONS.map((station, index) => (
                <span
                  key={station}
                  className="absolute top-0 bottom-0 w-px bg-rule"
                  style={{ left: `${pctFor(index)}%` }}
                />
              ))}
            </div>

            {applications.map((application, index) => (
              <MapLine
                key={application.id}
                application={application}
                position={positions[index]}
                hovered={hoveredId === application.id}
                onHover={setHoveredId}
                onOpen={onOpen}
              />
            ))}

            {/* Interchange bars, above the tracks. */}
            <div
              aria-hidden
              className="pointer-events-none absolute top-0 bottom-0 z-3"
              style={trackInset}
            >
              {interchanges.map((interchange, index) => (
                <span
                  key={index}
                  title={interchange.reason}
                  className="absolute box-border block h-[83px] w-[15px] -translate-x-1/2 rounded-lg border-[2.5px] border-ink bg-white"
                  style={{ left: `${interchange.left}%`, top: interchange.top }}
                />
              ))}
            </div>
          </div>
        </div>
      </div>

      <MapLegend />
    </div>
  )
}
