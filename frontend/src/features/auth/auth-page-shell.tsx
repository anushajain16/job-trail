import type { ReactNode } from 'react'
import { LINE_COLORS, STATIONS, stationLeft } from '@/lib/design'

/** Decorative mini-map — the product's own visual language as the artwork. */
function AuthDiagram() {
  const lines = [
    { color: LINE_COLORS[0], pct: 60 },
    { color: LINE_COLORS[1], pct: 100 },
    { color: LINE_COLORS[2], pct: 40 },
    { color: LINE_COLORS[3], pct: 20 },
    { color: LINE_COLORS[5], pct: 80 },
  ]

  return (
    <div className="w-full max-w-md">
      <div className="relative mb-4 h-4">
        {STATIONS.map((station, index) => (
          <span
            key={station}
            className="absolute -translate-x-1/2 font-mono text-[8px] tracking-[0.08em] whitespace-nowrap text-muted"
            style={{ left: `${stationLeft(index)}%` }}
          >
            {station}
          </span>
        ))}
      </div>
      <div className="relative">
        {STATIONS.map((station, index) => (
          <span
            key={station}
            className="absolute top-0 bottom-0 w-px bg-rule"
            style={{ left: `${stationLeft(index)}%` }}
          />
        ))}
        {lines.map((line) => (
          <div key={line.color} className="relative h-14">
            <span
              className="absolute top-1/2 right-0 left-0 h-[7px] -translate-y-1/2 rounded-full opacity-13"
              style={{ background: line.color }}
            />
            <span
              className="absolute top-1/2 left-0 h-[7px] -translate-y-1/2 rounded-full"
              style={{ width: `${line.pct}%`, background: line.color }}
            />
            <span
              className="absolute top-1/2 h-3.5 w-6.5 -translate-x-1/2 -translate-y-1/2 rounded-full"
              style={{ left: `${line.pct}%`, background: line.color }}
            />
          </div>
        ))}
      </div>
    </div>
  )
}

export interface AuthPageShellProps {
  title: string
  subtitle: string
  children: ReactNode
  footer?: ReactNode
}

/** Shared frame for login and signup — form left, diagram right. */
export function AuthPageShell({ title, subtitle, children, footer }: AuthPageShellProps) {
  return (
    <div className="grid min-h-full lg:grid-cols-2">
      <div className="flex items-center justify-center px-8 py-14 sm:px-14">
        <div className="w-full max-w-sm">
          <span className="font-mono text-[13px] font-bold tracking-[0.16em] uppercase">
            Job<span className="text-line-coral">Trail</span>
          </span>
          <h1 className="type-title mt-9 uppercase">{title}</h1>
          <p className="type-meta mt-2">{subtitle}</p>
          <div className="mt-8">{children}</div>
          {footer && <div className="mt-7 border-t border-rule pt-5">{footer}</div>}
        </div>
      </div>

      <div className="hidden items-center justify-center border-l-2 border-ink px-14 lg:flex">
        <div className="w-full max-w-md">
          <AuthDiagram />
          <p className="mt-10 border-t-2 border-ink pt-4 font-mono text-[10px] leading-relaxed tracking-[0.06em] text-muted">
            Every application is a line. Six stations from SAVED to OFFER.
            Watch the whole network move at once instead of dragging cards
            between columns.
          </p>
        </div>
      </div>
    </div>
  )
}
