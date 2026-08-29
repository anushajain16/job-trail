import { Funnel, FunnelChart, LabelList, ResponsiveContainer, Tooltip } from 'recharts'
import { STAGE_LABELS } from '@/features/applications/types'
import { ChartCard, chartTooltipStyle } from '@/features/analytics/chart-card'
import { useFunnelQuery } from '@/features/analytics/hooks'

export function FunnelCard() {
  const { data, isPending, isError, error } = useFunnelQuery()
  const rows = data?.stages ?? []

  return (
    <ChartCard
      title="Funnel"
      description={
        data ? `${data.totalApplications} application${data.totalApplications === 1 ? '' : 's'} tracked` : ''
      }
      isPending={isPending}
      isError={isError}
      error={error}
      isEmpty={rows.length === 0 || rows.every((row) => row.applications === 0)}
      emptyMessage="No applications yet."
    >
      <ResponsiveContainer width="100%" height={280}>
        <FunnelChart>
          <Tooltip
            {...chartTooltipStyle}
            formatter={(value) => [`${value} application${value === 1 ? '' : 's'}`, undefined]}
          />
          <Funnel
            dataKey="applications"
            data={rows.map((row) => ({ ...row, name: STAGE_LABELS[row.stage] }))}
            nameKey="name"
            fill="var(--analytics-primary)"
            stroke="var(--background)"
            strokeWidth={2}
            isAnimationActive={false}
          >
            <LabelList
              dataKey="name"
              position="right"
              fill="var(--foreground)"
              stroke="none"
              fontSize={12}
            />
            <LabelList
              dataKey="applications"
              position="center"
              fill="var(--background)"
              stroke="none"
              fontSize={12}
              fontWeight={600}
            />
          </Funnel>
        </FunnelChart>
      </ResponsiveContainer>
    </ChartCard>
  )
}
