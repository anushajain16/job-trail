import { Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { STAGE_LABELS } from '@/features/applications/types'
import { ChartCard, chartTooltipStyle } from '@/features/analytics/chart-card'
import { useTimeInStageQuery } from '@/features/analytics/hooks'

export function TimeInStageCard() {
  const { data, isPending, isError, error } = useTimeInStageQuery()
  const rows = (data?.stages ?? []).filter((row) => row.sampleSize > 0)

  return (
    <ChartCard
      title="Time in stage"
      description="Average days spent in each stage before moving on, over transitions that have actually completed."
      isPending={isPending}
      isError={isError}
      error={error}
      isEmpty={rows.length === 0}
      emptyMessage="Not enough completed transitions yet."
    >
      <ResponsiveContainer width="100%" height={280}>
        <BarChart
          data={rows.map((row) => ({
            label: STAGE_LABELS[row.stage],
            days: Math.round(row.averageDays * 10) / 10,
            sampleSize: row.sampleSize,
          }))}
          margin={{ top: 16, right: 8, left: 0, bottom: 8 }}
        >
          <CartesianGrid vertical={false} stroke="var(--border)" />
          <XAxis dataKey="label" tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }} interval={0} />
          <YAxis
            tickFormatter={(value: number) => `${value}d`}
            tick={{ fontSize: 11, fill: 'var(--muted-foreground)' }}
            width={40}
          />
          <Tooltip
            {...chartTooltipStyle}
            formatter={(value, _name, item) => [
              `${value} days (n=${item.payload.sampleSize})`,
              'Average',
            ]}
          />
          <Bar dataKey="days" fill="var(--analytics-primary)" radius={[4, 4, 0, 0]} maxBarSize={48}>
            <LabelList
              dataKey="days"
              position="top"
              formatter={(value) => `${value}d`}
              fill="var(--muted-foreground)"
              fontSize={11}
            />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  )
}
