import { type MouseEvent, useRef, useState } from 'react'
import type { Application, Stage } from '@/features/applications/types'
import { STAGE_LABELS } from '@/features/applications/types'
import type { HistoryEntry } from '@/features/subway-map/api'
import { LINE_STAGES, STATUS_LABELS, buildSubwayLine } from '@/features/subway-map/layout'
import { statusColor } from '@/features/subway-map/status-color'
import { SubwayTooltip, type TooltipState } from '@/features/subway-map/subway-tooltip'

const LABEL_WIDTH = 224
const STATION_START_X = 256
const COLUMN_WIDTH = 108
const ROW_HEIGHT = 44
const HEADER_Y = 24
const ROWS_START_Y = 60
const STATION_R = 5
const HOLLOW_R = 4
const TRAIN_R = 7
const RIGHT_PADDING = 168

function stationX(index: number) {
  return STATION_START_X + index * COLUMN_WIDTH
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

interface Row {
  application: Application
  history: HistoryEntry[] | undefined
}

interface SubwayChartProps {
  rows: Row[]
  onSelect: (applicationId: string) => void
}

export function SubwayChart({ rows, onSelect }: SubwayChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [tooltip, setTooltip] = useState<TooltipState | null>(null)

  const width = stationX(LINE_STAGES.length - 1) + RIGHT_PADDING
  const height = ROWS_START_Y + rows.length * ROW_HEIGHT + 16

  function showTooltip(event: MouseEvent, title: string, subtitle: string) {
    const bounds = containerRef.current?.getBoundingClientRect()
    if (!bounds) return
    setTooltip({ x: event.clientX - bounds.left, y: event.clientY - bounds.top, title, subtitle })
  }

  if (rows.length === 0) {
    return <p className="p-6 text-sm text-muted-foreground">No applications match the current filters.</p>
  }

  return (
    <div ref={containerRef} className="relative overflow-x-auto">
      <svg
        width={width}
        height={height}
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label="Subway map of application progress"
        className="min-w-full"
        onMouseLeave={() => setTooltip(null)}
      >
        {/* Station columns: header labels + a recessive gridline the full
         * height of the chart, per marks-and-anatomy.md ("hairline, solid,
         * recessive"). */}
        {LINE_STAGES.map((stage, index) => (
          <g key={stage}>
            <line
              x1={stationX(index)}
              y1={ROWS_START_Y - 20}
              x2={stationX(index)}
              y2={height - 8}
              stroke="var(--border)"
              strokeWidth={1}
            />
            <text
              x={stationX(index)}
              y={HEADER_Y}
              textAnchor="middle"
              className="fill-muted-foreground text-[11px] font-medium"
            >
              {STAGE_LABELS[stage]}
            </text>
          </g>
        ))}

        {rows.map((row, rowIndex) => {
          const y = ROWS_START_Y + rowIndex * ROW_HEIGHT
          const { application, history } = row
          return (
            <SubwayRow
              key={application.id}
              application={application}
              history={history}
              y={y}
              onHover={showTooltip}
              onLeave={() => setTooltip(null)}
              onSelect={() => onSelect(application.id)}
              rowWidth={width}
            />
          )
        })}
      </svg>
      <SubwayTooltip tooltip={tooltip} />
    </div>
  )
}

interface SubwayRowProps {
  application: Application
  history: HistoryEntry[] | undefined
  y: number
  rowWidth: number
  onHover: (event: MouseEvent, title: string, subtitle: string) => void
  onLeave: () => void
  onSelect: () => void
}

function SubwayRow({ application, history, y, rowWidth, onHover, onLeave, onSelect }: SubwayRowProps) {
  const label = `${application.company} — ${application.role}`
  const truncatedLabel = label.length > 30 ? `${label.slice(0, 29)}…` : label

  // Loading: this row's own history hasn't resolved yet (each application's
  // history is fetched independently — see useHistoriesQuery). Draw just
  // the Saved station so the row still appears, without guessing progress.
  if (!history) {
    return (
      <g>
        <title>{label}</title>
        <text x={LABEL_WIDTH} y={y + 4} textAnchor="end" className="fill-foreground text-[12px]">
          {truncatedLabel}
        </text>
        <circle cx={stationX(0)} cy={y} r={HOLLOW_R} className="fill-muted-foreground/40" />
      </g>
    )
  }

  const line = buildSubwayLine(history)
  const color = statusColor(line.status)
  const reachedX = stationX(line.reachedIndex)

  return (
    <g className="group cursor-pointer" onClick={onSelect}>
      <title>{label}</title>

      {/* Full-row hit target — bigger than any single mark, per
       * interaction.md, and what makes the whole line clickable, not just
       * its dots. */}
      <rect x={0} y={y - ROW_HEIGHT / 2} width={rowWidth} height={ROW_HEIGHT} fill="transparent" />

      <text
        x={LABEL_WIDTH}
        y={y + 4}
        textAnchor="end"
        className="fill-foreground text-[12px] transition-colors group-hover:fill-primary"
      >
        {truncatedLabel}
      </text>

      {/* Remaining (untravelled) track — only an active line still has a
       * future; offer/rejected/ghosted lines have already stopped. */}
      {line.status === 'active' && line.reachedIndex < LINE_STAGES.length - 1 && (
        <line
          x1={reachedX}
          y1={y}
          x2={stationX(LINE_STAGES.length - 1)}
          y2={y}
          stroke="var(--subway-track-remaining)"
          strokeWidth={2}
          strokeDasharray="4 4"
        />
      )}

      {/* Travelled track — solid, 2px, the line's status color throughout
       * (see layout.ts: color reflects where the line ended up, not a
       * segment-by-segment blend). */}
      {line.reachedIndex > 0 && (
        <line x1={stationX(0)} y1={y} x2={reachedX} y2={y} stroke={color} strokeWidth={2} strokeLinecap="round" />
      )}

      {/* Stations: filled + a surface ring where reached, hollow where not
       * — marks-and-anatomy.md's "2px surface ring" so dots stay legible
       * crossing the line beneath them. */}
      {line.stations.map((station) => (
        <circle
          key={station.stage}
          cx={stationX(station.index)}
          cy={y}
          r={station.reached ? STATION_R : HOLLOW_R}
          fill={station.reached ? color : 'var(--background)'}
          stroke={station.reached ? 'var(--background)' : 'var(--border)'}
          strokeWidth={station.reached ? 2 : 1.5}
          onMouseMove={(event) =>
            onHover(
              event,
              STAGE_LABELS[station.stage],
              station.reachedAt ? `Reached ${formatDate(station.reachedAt)}` : 'Not reached',
            )
          }
          onMouseLeave={onLeave}
        />
      ))}

      {/* Live train / arrival marker. Pulses only while still moving
       * (status active) — an offer is a stop, not motion. */}
      {line.currentIndex !== null && (
        <g
          onMouseMove={(event) =>
            onHover(
              event,
              line.status === 'offer' ? 'Offer' : STAGE_LABELS[LINE_STAGES[line.currentIndex!]],
              line.status === 'offer' ? 'Arrived' : 'Currently here',
            )
          }
          onMouseLeave={onLeave}
        >
          {line.status === 'active' && (
            <circle cx={stationX(line.currentIndex)} cy={y} r={TRAIN_R} fill={color} opacity={0.35}>
              <animate attributeName="r" values={`${TRAIN_R};${TRAIN_R + 6};${TRAIN_R}`} dur="1.8s" repeatCount="indefinite" />
              <animate attributeName="opacity" values="0.35;0;0.35" dur="1.8s" repeatCount="indefinite" />
            </circle>
          )}
          <circle
            cx={stationX(line.currentIndex)}
            cy={y}
            r={TRAIN_R}
            fill={color}
            stroke="var(--background)"
            strokeWidth={2}
          />
        </g>
      )}

      {/* Exit marker — a diamond just past where the line left the main
       * corridor, for a rejected or ghosted application. */}
      {line.exitedAt && (
        <ExitMarker
          x={reachedX + COLUMN_WIDTH * 0.55}
          y={y}
          color={color}
          label={STATUS_LABELS[line.status]}
          onHover={(event) => onHover(event, STATUS_LABELS[line.status], formatDate(line.exitedAt!))}
          onLeave={onLeave}
        />
      )}
    </g>
  )
}

function ExitMarker({
  x,
  y,
  color,
  label,
  onHover,
  onLeave,
}: {
  x: number
  y: number
  color: string
  label: string
  onHover: (event: MouseEvent) => void
  onLeave: () => void
}) {
  const size = 6
  return (
    <g onMouseMove={onHover} onMouseLeave={onLeave}>
      <rect
        x={x - size / 2}
        y={y - size / 2}
        width={size}
        height={size}
        transform={`rotate(45 ${x} ${y})`}
        fill={color}
        stroke="var(--background)"
        strokeWidth={1.5}
      />
      <text x={x + 12} y={y + 4} className="fill-muted-foreground text-[11px]">
        {label}
      </text>
    </g>
  )
}

// Re-exported for the accessible table view, which needs the same
// stage-ordering/labels without re-deriving them.
export const MAIN_LINE_STAGES: readonly Stage[] = LINE_STAGES
