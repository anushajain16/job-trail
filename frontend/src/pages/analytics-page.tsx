import { ResponseRateBySourceCard, StageConversionCard } from '@/features/analytics/conversion-charts'
import { FunnelCard } from '@/features/analytics/funnel-chart'
import { TimeInStageCard } from '@/features/analytics/time-in-stage-chart'

export function AnalyticsPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Analytics</h1>
        <p className="text-muted-foreground">How the pipeline is actually going, from the event log.</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <FunnelCard />
        <TimeInStageCard />
        <StageConversionCard />
        <ResponseRateBySourceCard />
      </div>
    </div>
  )
}
