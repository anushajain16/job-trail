import { Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { STAGE_LABELS } from '@/features/applications/types'
import { ChartCard, chartTooltipStyle } from '@/features/analytics/chart-card'
import { useConversionQuery } from '@/features/analytics/hooks'
import type { SourceResponseRate, StageConversion } from '@/features/analytics/api'

function toPercent(rate: number): number {
  return Math.round(rate * 1000) / 10
}

export function StageConversionCard() {
  const { data, isPending, isError, error } = useConversionQuery()
  const rows = data?.stageConversions ?? []

  return (
    <ChartCard
      title="Stage-to-stage conversion"
      description="Of applications that reached a stage, the share that reached the next one."
      isPending={isPending}
      isError={isError}
      error={error}
      isEmpty={rows.length === 0}
    >
      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={rows.map(toConversionDatum)} margin={{ top: 16, right: 8, left: 0, bottom: 8 }}>
          <CartesianGrid vertical={false} stroke="var(--border)" />
          <XAxis dataKey="label" tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }} interval={0} />
          <YAxis
            tickFormatter={(value: number) => `${value}%`}
            tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
            width={40}
            domain={[0, 100]}
          />
          <Tooltip
            {...chartTooltipStyle}
            formatter={(value, _name, item) => [
              `${value}% (${item.payload.toCount} of ${item.payload.fromCount})`,
              'Conversion',
            ]}
          />
          <Bar dataKey="percent" fill="var(--analytics-primary)" radius={[4, 4, 0, 0]} maxBarSize={48}>
            <LabelList
              dataKey="percent"
              position="top"
              formatter={(value) => `${value}%`}
              fill="var(--muted-foreground)"
              fontSize={11}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  )
}

function toConversionDatum(row: StageConversion) {
  return {
    label: `${STAGE_LABELS[row.fromStage]} → ${STAGE_LABELS[row.toStage]}`,
    percent: toPercent(row.conversionRate),
    fromCount: row.fromCount,
    toCount: row.toCount,
  }
}

export function ResponseRateBySourceCard() {
  const { data, isPending, isError, error } = useConversionQuery()
  const rows = data?.responseRateBySource ?? []

  return (
    <ChartCard
      title="Response rate by source"
      description="Share of applications from each source that ever got a response (not ghosted)."
      isPending={isPending}
      isError={isError}
      error={error}
      isEmpty={rows.length === 0}
    >
      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={rows.map(toSourceDatum)} margin={{ top: 16, right: 8, left: 0, bottom: 8 }}>
          <CartesianGrid vertical={false} stroke="var(--border)" />
          <XAxis dataKey="source" tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }} interval={0} />
          <YAxis
            tickFormatter={(value: number) => `${value}%`}
            tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
            width={40}
            domain={[0, 100]}
          />
          <Tooltip
            {...chartTooltipStyle}
            formatter={(value, _name, item) => [
              `${value}% (${item.payload.respondedApplications} of ${item.payload.totalApplications})`,
              'Response rate',
            ]}
          />
          <Bar dataKey="percent" fill="var(--analytics-primary)" radius={[4, 4, 0, 0]} maxBarSize={48}>
            <LabelList
              dataKey="percent"
              position="top"
              formatter={(value) => `${value}%`}
              fill="var(--muted-foreground)"
              fontSize={11}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  )
}

function toSourceDatum(row: SourceResponseRate) {
  return {
    source: row.source,
    percent: toPercent(row.responseRate),
    totalApplications: row.totalApplications,
    respondedApplications: row.respondedApplications,
  }
}
