import { useState } from 'react'
import { cn } from '@/lib/cn'
import { INK } from '@/lib/design'

export interface TrackBarDatum {
  /** Category name — the row label. */
  label: string
  /** The magnitude that sets bar length. */
  value: number
  /** What to print at the end of the row (e.g. `62%`, `4.5 D`). */
  display: string
  /** Extra line shown in the hover tooltip. */
  detail?: string
}

export interface TrackBarChartProps {
  data: TrackBarDatum[]
  /** Single series, so a single hue — magnitude, not identity. */
  color?: string
  /** Fix the scale across charts; defaults to the largest value present. */
  max?: number
  className?: string
}

/**
 * Horizontal magnitude bars drawn as transit track: a 13%-opacity full-width
 * route with the measured segment over it, exactly like a line on the map.
 *
 * One series only — length carries the value and the row label carries
 * identity, so there is no legend and no categorical palette to validate.
 */
export function TrackBarChart({ data, color = INK, max, className }: TrackBarChartProps) {
  const [hovered, setHovered] = useState<number | null>(null)
  const ceiling = max ?? Math.max(...data.map((datum) => datum.value), 1)

  return (
    <div className={cn('flex flex-col gap-3', className)}>
      {data.map((datum, index) => {
        const pct = ceiling > 0 ? Math.max(0, (datum.value / ceiling) * 100) : 0
        return (
          <div
            key={datum.label}
            className="grid grid-cols-[110px_1fr_64px] items-center gap-3"
            onMouseEnter={() => setHovered(index)}
            onMouseLeave={() => setHovered(null)}
          >
            <span className="truncate font-mono text-[10px] tracking-[0.08em] uppercase text-ink-soft">
              {datum.label}
            </span>

            <div className="relative h-[7px]">
              <span
                className="absolute inset-0 rounded-[4px] opacity-13"
                style={{ background: color }}
              />
              <span
                className="absolute top-0 left-0 h-[7px] rounded-[4px] transition-[width] duration-200"
                style={{ width: `${pct}%`, background: color }}
              />
              {hovered === index && (datum.detail || datum.display) && (
                <span
                  className="absolute bottom-[14px] z-30 -translate-x-1/2 rounded-[3px] bg-ink px-2.5 py-[6px] font-mono text-[9px] leading-[1.6] tracking-[0.05em] whitespace-nowrap text-paper"
                  style={{ left: `${Math.min(Math.max(pct, 8), 92)}%` }}
                >
                  <span className="block font-bold">{datum.label}</span>
                  <span className="block text-grey">{datum.detail ?? datum.display}</span>
                </span>
              )}
            </div>

            <span className="text-right font-mono text-[10px] font-semibold tracking-[0.06em] text-ink">
              {datum.display}
            </span>
          </div>
        )
      })}
    </div>
  )
}
