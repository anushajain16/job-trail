import type { ReactNode } from 'react'

function LegendItem({ swatch, label }: { swatch: ReactNode; label: string }) {
  return (
    <div className="flex items-center gap-2">
      {swatch}
      <span className="type-meta">{label}</span>
    </div>
  )
}

/** The map's key, in the same vocabulary as the diagram itself. */
export function MapLegend() {
  return (
    <div className="mt-9 flex flex-wrap items-center gap-x-7 gap-y-3 border-t-2 border-ink pt-3.5">
      <LegendItem swatch={<span className="inline-block h-[5px] w-[22px] rounded-full bg-ink" />} label="TRAVELLED" />
      <LegendItem
        swatch={<span className="inline-block h-[5px] w-[22px] rounded-full bg-ink opacity-13" />}
        label="ROUTE AHEAD"
      />
      <LegendItem
        swatch={
          <span className="box-border inline-block h-[9px] w-[9px] rounded-full border-2 border-ink bg-white" />
        }
        label="STATION PASSED"
      />
      <LegendItem
        swatch={
          <span className="box-border inline-block h-5 w-2.5 rounded-[5px] border-2 border-ink bg-white" />
        }
        label="INTERCHANGE"
      />
      <LegendItem
        swatch={<span className="inline-block h-[5px] w-[22px] rounded-full bg-grey opacity-50" />}
        label="SUSPENDED"
      />
      <LegendItem
        swatch={
          <span className="inline-flex h-3.5 w-8 items-center justify-center rounded-[7px] bg-ink font-mono text-[7px] font-bold tracking-[0.12em] text-paper">
            END
          </span>
        }
        label="OFFER TERMINUS"
      />
    </div>
  )
}
