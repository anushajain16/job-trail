import type { ReactNode } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { describeApiError } from '@/lib/describe-api-error'

interface ChartCardProps {
  title: string
  description: string
  isPending: boolean
  isError: boolean
  error: unknown
  isEmpty?: boolean
  emptyMessage?: string
  children: ReactNode
}

/** One card per chart: title/description chrome + the three states every
 * query-backed chart needs (loading, error, empty) so each chart component
 * only has to handle its actual data. */
export function ChartCard({
  title,
  description,
  isPending,
  isError,
  error,
  isEmpty,
  emptyMessage = 'No data yet.',
  children,
}: ChartCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>
        {isPending ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : isError ? (
          <p role="alert" className="text-sm text-destructive">
            {describeApiError(error)}
          </p>
        ) : isEmpty ? (
          <p className="text-sm text-muted-foreground">{emptyMessage}</p>
        ) : (
          children
        )}
      </CardContent>
    </Card>
  )
}

/** Shared Recharts Tooltip styling — the library's own default is an
 * unthemed white box, so every chart here points contentStyle/itemStyle at
 * the app's actual tokens instead. */
export const chartTooltipStyle = {
  contentStyle: {
    background: 'var(--popover)',
    borderColor: 'var(--border)',
    borderRadius: 'var(--radius-md)',
    fontSize: 12,
  },
  labelStyle: { color: 'var(--popover-foreground)', fontWeight: 500 },
  itemStyle: { color: 'var(--popover-foreground)' },
}
