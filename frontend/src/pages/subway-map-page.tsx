import { useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Application } from '@/features/applications/types'
import { ApplicationDetailSheet } from '@/features/subway-map/application-detail-sheet'
import { useApplicationsForMapQuery, useHistoriesQuery } from '@/features/subway-map/hooks'
import { STATUS_LABELS, categorizeStage, type LineStatus } from '@/features/subway-map/layout'
import { statusColor } from '@/features/subway-map/status-color'
import { SubwayChart } from '@/features/subway-map/subway-chart'
import { SubwayMapTable } from '@/features/subway-map/subway-map-table'
import { describeApiError } from '@/lib/describe-api-error'

const ALL_STATUSES: LineStatus[] = ['active', 'offer', 'rejected', 'ghosted']

// Stable reference so useMemo below doesn't see a "new" applications array
// on every render while the query is still loading.
const EMPTY_APPLICATIONS: Application[] = []

function matchesSearch(application: Application, query: string): boolean {
  if (!query) return true
  const haystack = `${application.company} ${application.role}`.toLowerCase()
  return haystack.includes(query.toLowerCase())
}

export function SubwayMapPage() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<Set<LineStatus>>(new Set(ALL_STATUSES))
  const [view, setView] = useState<'map' | 'table'>('map')
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data: page, isPending, isError, error } = useApplicationsForMapQuery()
  const applications = page?.content ?? EMPTY_APPLICATIONS

  const filtered = useMemo(
    () =>
      applications.filter(
        (application) => matchesSearch(application, search) && statusFilter.has(categorizeStage(application.currentStage)),
      ),
    [applications, search, statusFilter],
  )

  const applicationIds = useMemo(() => filtered.map((application) => application.id), [filtered])
  const { historyByApplicationId } = useHistoriesQuery(applicationIds)

  function toggleStatus(status: LineStatus, checked: boolean) {
    setStatusFilter((current) => {
      const next = new Set(current)
      if (checked) next.add(status)
      else next.delete(status)
      return next
    })
  }

  const selectedApplication = filtered.find((application) => application.id === selectedId) ?? null

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Subway map</h1>
        <p className="text-muted-foreground">Every application's stage progression, one line each.</p>
      </div>

      {/* Filters: one row above the chart, scoping both views — per
       * dataviz skill's interaction.md ("one row, above the charts"). */}
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="map-search">Search</Label>
            <Input
              id="map-search"
              placeholder="Company or role"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              className="w-56"
            />
          </div>
          <fieldset className="flex flex-col gap-1.5">
            <legend className="text-sm font-medium">Status</legend>
            <div className="flex flex-wrap gap-3">
              {ALL_STATUSES.map((status) => (
                <label key={status} className="flex items-center gap-1.5 text-sm">
                  <Checkbox
                    checked={statusFilter.has(status)}
                    onCheckedChange={(checked) => toggleStatus(status, checked)}
                  />
                  <span
                    aria-hidden
                    className="size-2.5 rounded-full"
                    style={{ backgroundColor: statusColor(status) }}
                  />
                  {STATUS_LABELS[status]}
                </label>
              ))}
            </div>
          </fieldset>
        </div>

        <div className="flex gap-2">
          <Button variant={view === 'map' ? 'default' : 'outline'} size="sm" onClick={() => setView('map')}>
            Map
          </Button>
          <Button variant={view === 'table' ? 'default' : 'outline'} size="sm" onClick={() => setView('table')}>
            Table
          </Button>
        </div>
      </div>

      <Card>
        <CardContent className={view === 'map' ? 'p-2' : 'p-0'}>
          {isPending ? (
            <p className="p-6 text-sm text-muted-foreground">Loading…</p>
          ) : isError ? (
            <p role="alert" className="p-6 text-sm text-destructive">
              {describeApiError(error)}
            </p>
          ) : view === 'map' ? (
            <SubwayChart
              rows={filtered.map((application) => ({
                application,
                history: historyByApplicationId.get(application.id),
              }))}
              onSelect={setSelectedId}
            />
          ) : (
            <SubwayMapTable applications={filtered} />
          )}
        </CardContent>
      </Card>

      <ApplicationDetailSheet
        application={selectedApplication}
        history={selectedId ? historyByApplicationId.get(selectedId) : undefined}
        onOpenChange={(open) => !open && setSelectedId(null)}
      />
    </div>
  )
}
