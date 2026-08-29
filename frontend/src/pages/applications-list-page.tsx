import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ExportCsvButton } from '@/components/export-csv-button'
import { DeleteApplicationDialog } from '@/features/applications/delete-application-dialog'
import { useApplicationsQuery } from '@/features/applications/hooks'
import { STAGE_LABELS, type Application } from '@/features/applications/types'
import { MatchScoreBadge } from '@/features/matching/match-score-badge'
import { describeApiError } from '@/lib/describe-api-error'

const PAGE_SIZE = 10

function readPage(searchParams: URLSearchParams): number {
  const raw = Number(searchParams.get('page'))
  return Number.isInteger(raw) && raw >= 0 ? raw : 0
}

export function ApplicationsListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = readPage(searchParams)
  const [pendingDelete, setPendingDelete] = useState<Application | null>(null)

  const { data, isPending, isError, error, isPlaceholderData } = useApplicationsQuery({ page, size: PAGE_SIZE })

  function goToPage(next: number) {
    setSearchParams(next === 0 ? {} : { page: String(next) })
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Applications</h1>
          <p className="text-muted-foreground">Everything you're tracking, newest first.</p>
        </div>
        <div className="flex gap-2">
          <ExportCsvButton path="/api/interviews/export" label="Export interview rounds" pendingLabel="Exporting…" />
          <ExportCsvButton path="/api/applications/export" label="Export CSV" pendingLabel="Exporting…" />
          <Button render={<Link to="/applications/new" />}>New application</Button>
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          {isPending ? (
            <p className="p-6 text-sm text-muted-foreground">Loading…</p>
          ) : isError ? (
            <p role="alert" className="p-6 text-sm text-destructive">
              {describeApiError(error)}
            </p>
          ) : data.content.length === 0 ? (
            <p className="p-6 text-sm text-muted-foreground">
              No applications yet. <Link to="/applications/new" className="underline">Add your first one</Link>.
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Company</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Stage</TableHead>
                  <TableHead>Match</TableHead>
                  <TableHead>Location</TableHead>
                  <TableHead>Deadline</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((application) => (
                  <TableRow key={application.id} className={application.id.startsWith('optimistic-') ? 'opacity-60' : undefined}>
                    <TableCell className="font-medium">{application.company}</TableCell>
                    <TableCell>{application.role}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{STAGE_LABELS[application.currentStage]}</Badge>
                    </TableCell>
                    <TableCell>
                      <MatchScoreBadge application={application} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">{application.location ?? '—'}</TableCell>
                    <TableCell className="text-muted-foreground">{application.deadline ?? '—'}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button variant="outline" size="sm" render={<Link to={`/applications/${application.id}/edit`} />}>
                          Edit
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => setPendingDelete(application)}>
                          Delete
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Page {data.number + 1} of {data.totalPages} · {data.totalElements} total
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={data.first} onClick={() => goToPage(page - 1)}>
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last || isPlaceholderData}
              onClick={() => goToPage(page + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      <DeleteApplicationDialog application={pendingDelete} onOpenChange={(open) => !open && setPendingDelete(null)} />
    </div>
  )
}
