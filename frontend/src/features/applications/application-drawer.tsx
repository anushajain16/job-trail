import { useState, type ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ui/dialog'
import { Drawer } from '@/components/ui/drawer'
import { ErrorState, TrackLoader } from '@/components/ui/feedback'
import { LineName, StageBadge } from '@/components/ui/badge'
import { DataPoint, SectionHeading } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { daysUntil, formatBoardDateFull } from '@/lib/format'
import { useDocuments } from '@/features/documents/hooks'
import { MatchScorePanel } from '@/features/matching/match-score-panel'
import { InterviewRoundsSection } from '@/features/interviews/interview-rounds-section'
import type { ApplicationResponse, Uuid } from '@/api/types'
import { ApplicationFormDrawer } from './application-form'
import { useApplication, useDeleteApplication, useStatusHistory } from './hooks'
import { StageChanger } from './stage-changer'
import { StatusTimeline } from './status-timeline'

function SalaryRange({ application }: { application: ApplicationResponse }) {
  const { salaryMin, salaryMax } = application
  if (salaryMin == null && salaryMax == null) return <>—</>
  const format = (value: number) => value.toLocaleString()
  if (salaryMin != null && salaryMax != null) return <>{`${format(salaryMin)} – ${format(salaryMax)}`}</>
  return <>{format((salaryMin ?? salaryMax)!)}</>
}

function Deadline({ deadline }: { deadline: string | null }) {
  if (!deadline) return <>—</>
  const days = daysUntil(deadline)
  const urgent = days != null && days <= 3
  return (
    <span className={urgent ? 'text-danger' : undefined}>
      {formatBoardDateFull(deadline)}
      {days != null && (
        <span className="ml-2 text-[9px] tracking-[0.06em] text-muted">
          {days < 0 ? `${Math.abs(days)}D AGO` : days === 0 ? 'TODAY' : `IN ${days}D`}
        </span>
      )}
    </span>
  )
}

export interface ApplicationDrawerProps {
  applicationId: Uuid | null
  onClose: () => void
  /** Later stages mount match score / interview rounds here. */
  sections?: (application: ApplicationResponse) => ReactNode
}

/**
 * The line's detail sheet: where it is, how it got there, and everything
 * recorded about it. Opened from the map and from the applications table.
 */
export function ApplicationDrawer({ applicationId, onClose, sections }: ApplicationDrawerProps) {
  const { notify, notifyError } = useToast()
  const { data: application, isLoading, isError, error, refetch } = useApplication(applicationId)
  const { data: documents } = useDocuments()
  const { data: history } = useStatusHistory(applicationId)
  const remove = useDeleteApplication()

  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  /** Attachments are stored as ids; the label is what a person recognises. */
  const documentLabel = (id: string | null) => {
    if (!id) return '—'
    const match = documents?.find((document_) => document_.id === id)
    return match?.label ?? match?.originalFilename ?? '—'
  }

  const handleDelete = async () => {
    if (!application) return
    const id = application.id
    // Close first: while the drawer is open its detail/history queries are
    // live observers, and they would refetch a row that is being deleted.
    setConfirmingDelete(false)
    onClose()
    try {
      await remove.mutateAsync(id)
      notify('Line removed from the network.', 'success')
    } catch (caught) {
      notifyError(caught)
    }
  }

  return (
    <>
      <Drawer
        open={Boolean(applicationId) && !editing}
        onClose={onClose}
        title={
          application ? (
            <LineName
              id={application.id}
              stage={application.currentStage}
              company={application.company}
              role={application.role}
            />
          ) : (
            <h2 className="type-label">LINE DETAIL</h2>
          )
        }
        footer={
          application && (
            <div className="flex justify-between gap-2">
              <Button variant="danger" size="sm" onClick={() => setConfirmingDelete(true)}>
                Delete
              </Button>
              <Button variant="solid" size="sm" onClick={() => setEditing(true)}>
                Edit
              </Button>
            </div>
          )
        }
      >
        {isLoading && <TrackLoader label="LOADING LINE" />}
        {isError && <ErrorState error={error} onRetry={() => void refetch()} />}

        {application && (
          <div className="flex flex-col gap-7">
            <div className="flex flex-col gap-4">
              <StageBadge stage={application.currentStage} applicationId={application.id} />
              <StageChanger applicationId={application.id} currentStage={application.currentStage} />
            </div>

            <div>
              <SectionHeading weight="heavy">Status timeline</SectionHeading>
              <StatusTimeline
                className="mt-3"
                applicationId={application.id}
                currentStage={application.currentStage}
                history={history}
              />
            </div>

            <div>
              <SectionHeading>Details</SectionHeading>
              <div className="mt-3 grid grid-cols-2 gap-4">
                <DataPoint label="LOCATION">{application.location ?? '—'}</DataPoint>
                <DataPoint label="SOURCE">{application.source ?? '—'}</DataPoint>
                <DataPoint label="SALARY">
                  <SalaryRange application={application} />
                </DataPoint>
                <DataPoint label="DEADLINE">
                  <Deadline deadline={application.deadline} />
                </DataPoint>
                <DataPoint label="RÉSUMÉ">{documentLabel(application.resumeVersionId)}</DataPoint>
                <DataPoint label="COVER LETTER">
                  {documentLabel(application.coverLetterVersionId)}
                </DataPoint>
                <DataPoint label="ADDED" className="col-span-2">
                  {formatBoardDateFull(application.createdAt)}
                </DataPoint>
                {application.link && (
                  <DataPoint label="POSTING" className="col-span-2">
                    <a href={application.link} target="_blank" rel="noreferrer" className="break-all">
                      {application.link}
                    </a>
                  </DataPoint>
                )}
              </div>
            </div>

            <div>
              <SectionHeading>Notes</SectionHeading>
              <p className="mt-3 font-mono text-[10px] leading-[1.8] tracking-[0.02em] whitespace-pre-wrap text-ink-soft">
                {application.notes || 'No notes recorded.'}
              </p>
            </div>

            <MatchScorePanel application={application} />

            <InterviewRoundsSection applicationId={application.id} />

            {sections?.(application)}
          </div>
        )}
      </Drawer>

      {application && (
        <ApplicationFormDrawer
          open={editing}
          application={application}
          onClose={() => setEditing(false)}
        />
      )}

      <ConfirmDialog
        open={confirmingDelete}
        onClose={() => setConfirmingDelete(false)}
        onConfirm={handleDelete}
        loading={remove.isPending}
        title="Delete this line?"
        description="Its status history and interview rounds go with it. This cannot be undone."
        confirmLabel="Delete"
      />
    </>
  )
}
