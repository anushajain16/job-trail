import { useState } from 'react'
import { ExportCsvButton } from '@/components/export-csv-button'
import { Button } from '@/components/ui/button'
import { ChipGroup } from '@/components/ui/chip'
import { Input, Select } from '@/components/ui/field'
import { EmptyState, ErrorState, TrackLoader } from '@/components/ui/feedback'
import { LineDot, StageBadge } from '@/components/ui/badge'
import { PageHeader } from '@/components/ui/panel'
import { Pagination } from '@/components/ui/pagination'
import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/table'
import { formatBoardDate, formatBoardDateFull } from '@/lib/format'
import { resolvedLineColor } from '@/lib/design'
import { exportApplicationsCsv } from './api'
import { ApplicationDrawer } from './application-drawer'
import { ApplicationFormDrawer } from './application-form'
import { STAGE_FILTERS, selectApplications, type StageFilter } from './filters'
import { useApplications } from './hooks'

const PAGE_SIZE = 20

/** Backend-supported orderings — sorting happens server-side, not in memory. */
const SERVER_SORTS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'createdAt,asc', label: 'Oldest first' },
  { value: 'updatedAt,desc', label: 'Recently updated' },
  { value: 'company,asc', label: 'Company A–Z' },
  { value: 'deadline,asc', label: 'Deadline soonest' },
]

/** The timetable view: the same data as the map, read as a list. */
export function ApplicationsPage() {
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState(SERVER_SORTS[0].value)
  const [filter, setFilter] = useState<StageFilter>('all')
  const [search, setSearch] = useState('')
  const [creating, setCreating] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data, isLoading, isError, error, refetch } = useApplications({
    page,
    size: PAGE_SIZE,
    sort,
  })

  // Filter and search apply to the page in hand — the backend's list
  // endpoint takes neither, and sorting is already delegated to it.
  const rows = selectApplications(data?.content ?? [], { filter, sort: 'recent', search })

  return (
    <>
      <PageHeader
        title="Applications"
        meta={data ? `${data.totalElements} APPLICATIONS ON RECORD` : 'LOADING'}
        actions={
          <>
            <ExportCsvButton fetcher={exportApplicationsCsv} />
            <Button variant="solid" size="sm" onClick={() => setCreating(true)}>
              New application
            </Button>
          </>
        }
      />

      <div className="mb-6 flex flex-wrap items-center justify-between gap-4 border-y border-rule py-3">
        <ChipGroup options={STAGE_FILTERS} value={filter} onChange={setFilter} />
        <div className="flex items-center gap-3">
          <Input
            className="w-56 py-1.5 text-[10px]"
            placeholder="Search this page…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <Select
            className="w-48 py-1.5 text-[10px]"
            value={sort}
            onChange={(event) => {
              setSort(event.target.value)
              setPage(0)
            }}
            options={SERVER_SORTS}
          />
        </div>
      </div>

      {isLoading && <TrackLoader label="LOADING TIMETABLE" />}
      {isError && <ErrorState error={error} onRetry={() => void refetch()} />}

      {data && data.totalElements === 0 && (
        <EmptyState
          title="Nothing on record"
          description="Applications you add show up here and on the transit map."
          action={
            <Button size="sm" variant="solid" onClick={() => setCreating(true)}>
              Add an application
            </Button>
          }
        />
      )}

      {data && data.totalElements > 0 && rows.length === 0 && (
        <EmptyState title="Nothing on this page matches" description="Adjust the filter or search term." />
      )}

      {rows.length > 0 && (
        <>
          <Table>
            <THead>
              <TR>
                <TH>Company</TH>
                <TH>Role</TH>
                <TH>Station</TH>
                <TH>Location</TH>
                <TH>Source</TH>
                <TH align="right">Deadline</TH>
                <TH align="right">Added</TH>
              </TR>
            </THead>
            <TBody>
              {rows.map((application) => (
                <TR key={application.id} onClick={() => setSelectedId(application.id)}>
                  <TD>
                    <span className="flex items-center gap-2">
                      <LineDot color={resolvedLineColor(application.id, application.currentStage)} />
                      <span className="font-semibold tracking-[0.05em] uppercase text-ink">
                        {application.company}
                      </span>
                    </span>
                  </TD>
                  <TD>{application.role}</TD>
                  <TD>
                    <StageBadge stage={application.currentStage} applicationId={application.id} />
                  </TD>
                  <TD>{application.location ?? '—'}</TD>
                  <TD>{application.source ?? '—'}</TD>
                  <TD align="right">{formatBoardDate(application.deadline)}</TD>
                  <TD align="right">{formatBoardDateFull(application.createdAt)}</TD>
                </TR>
              ))}
            </TBody>
          </Table>

          <Pagination
            page={data!.number}
            totalPages={data!.totalPages}
            totalElements={data!.totalElements}
            onChange={setPage}
          />
        </>
      )}

      <ApplicationFormDrawer open={creating} onClose={() => setCreating(false)} />
      <ApplicationDrawer applicationId={selectedId} onClose={() => setSelectedId(null)} />
    </>
  )
}
