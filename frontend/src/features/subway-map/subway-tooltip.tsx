export interface TooltipState {
  x: number
  y: number
  title: string
  subtitle: string
}

/** One shared floating tooltip for every mark in the chart (station dots,
 * the train, exit markers) — positioned by mouse coordinates relative to
 * the chart's own container, not a per-mark popover. Dozens of marks
 * sharing one tooltip element is both simpler and cheaper than mounting a
 * popover primitive per dot. */
export function SubwayTooltip({ tooltip }: { tooltip: TooltipState | null }) {
  if (!tooltip) return null
  return (
    <div
      role="tooltip"
      className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-md border bg-popover px-2.5 py-1.5 text-xs text-popover-foreground shadow-md"
      style={{ left: tooltip.x, top: tooltip.y - 10 }}
    >
      <p className="font-medium">{tooltip.title}</p>
      <p className="text-muted-foreground">{tooltip.subtitle}</p>
    </div>
  )
}
