import { useState } from 'react'
import { ExportCsvButton } from '@/components/export-csv-button'
import { Button } from '@/components/ui/button'
import { ChipGroup } from '@/components/ui/chip'
import { Select } from '@/components/ui/field'
import { EmptyState, ErrorState, TrackLoader } from '@/components/ui/feedback'
import { PageHeader } from '@/components/ui/panel'
import { Toggle } from '@/components/ui/toggle'
import { exportApplicationsCsv } from '@/features/applications/api'
import { ApplicationDrawer } from '@/features/applications/application-drawer'
import { ApplicationFormDrawer } from '@/features/applications/application-form'
import {
  SORT_OPTIONS,
  STAGE_FILTERS,
  networkSummary,
  selectApplications,
  type SortKey,
  type StageFilter,
} from '@/features/applications/filters'
import { useAllApplications } from '@/features/applications/hooks'
import { TransitMap } from './transit-map'

/** The home view: every application as a line on one diagram. */
export function MapPage() {
  const { data: applications, isLoading, isError, error, refetch } = useAllApplications()
  const [filter, setFilter] = useState<StageFilter>('all')
  const [sort, setSort] = useState<SortKey>('grouped')
  const [showSuspended, setShowSuspended] = useState(true)
  const [showInterchanges, setShowInterchanges] = useState(true)
  const [creating, setCreating] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const visible = selectApplications(applications ?? [], { filter, sort, showSuspended })

  const counts: Record<StageFilter, number> = {
    all: applications?.length ?? 0,
    active: selectApplications(applications ?? [], { filter: 'active', sort }).length,
    suspended: selectApplications(applications ?? [], { filter: 'suspended', sort }).length,
    offers: selectApplications(applications ?? [], { filter: 'offers', sort }).length,
  }

  return (
    <>
      <PageHeader
        title="Application Transit Map"
        meta={applications ? networkSummary(applications) : 'LOADING NETWORK'}
        actions={
          <>
            <ChipGroup
              options={STAGE_FILTERS.map((option) => ({ ...option, count: counts[option.value] }))}
              value={filter}
              onChange={setFilter}
            />
            <ExportCsvButton fetcher={exportApplicationsCsv} />
            <Button variant="solid" size="sm" onClick={() => setCreating(true)}>
              New line
            </Button>
          </>
        }
      />

      {applications && applications.length > 0 && (
        <div className="mb-7 flex flex-wrap items-center gap-6 border-y border-rule py-3">
          <label className="flex items-center gap-2">
            <span className="type-meta">SORT</span>
            <Select
              className="w-52 py-1 text-[10px]"
              value={sort}
              onChange={(event) => setSort(event.target.value as SortKey)}
              options={SORT_OPTIONS}
            />
          </label>
          <Toggle checked={showSuspended} onChange={setShowSuspended} label="SHOW SUSPENDED" />
          <Toggle checked={showInterchanges} onChange={setShowInterchanges} label="SHOW INTERCHANGES" />
        </div>
      )}

      {isLoading && <TrackLoader label="PLOTTING NETWORK" />}
      {isError && <ErrorState error={error} onRetry={() => void refetch()} />}

      {applications && applications.length === 0 && (
        <EmptyState
          title="No lines in service"
          description="Add your first application and it appears here as a line running from SAVED to OFFER."
          action={
            <Button size="sm" variant="solid" onClick={() => setCreating(true)}>
              Add an application
            </Button>
          }
        />
      )}

      {applications && applications.length > 0 && visible.length === 0 && (
        <EmptyState title="No lines match this filter" description="Clear the filter to see the whole network." />
      )}

      {visible.length > 0 && (
        <TransitMap applications={visible} showInterchanges={showInterchanges} onOpen={setSelectedId} />
      )}

      <ApplicationFormDrawer open={creating} onClose={() => setCreating(false)} />
      <ApplicationDrawer applicationId={selectedId} onClose={() => setSelectedId(null)} />
    </>
  )
}
