import { useState } from 'react'
import { ApiError } from '@/api/client'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ui/dialog'
import { ErrorState, TrackLoader } from '@/components/ui/feedback'
import { Panel, SectionHeading } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { formatBoardDateTime } from '@/lib/format'
import { LINE_COLORS } from '@/lib/design'
import type { InterviewRoundResponse, Uuid } from '@/api/types'
import { useDeleteInterviewRound, useInterviewRounds, useSyncInterviewToCalendar } from './hooks'
import { InterviewFormDialog } from './interview-form'

function RoundNote({ label, body }: { label: string; body: string | null }) {
  if (!body) return null
  return (
    <div>
      <p className="type-meta mb-1">{label}</p>
      <p className="font-mono text-[10px] leading-[1.8] tracking-[0.02em] whitespace-pre-wrap text-ink-soft">
        {body}
      </p>
    </div>
  )
}

function RoundCard({
  round,
  applicationId,
  onEdit,
  onDelete,
}: {
  round: InterviewRoundResponse
  applicationId: Uuid
  onEdit: () => void
  onDelete: () => void
}) {
  const sync = useSyncInterviewToCalendar(applicationId)
  const { notify, notifyError } = useToast()
  const synced = Boolean(round.googleEventId)

  const runSync = async () => {
    try {
      await sync.mutateAsync(round.id)
      notify(synced ? 'Calendar event updated.' : 'Added to Google Calendar.', 'success')
    } catch (error) {
      // 409 is the one actionable failure: Calendar simply isn't connected.
      if (error instanceof ApiError && error.isConflict) {
        notify('Connect Google Calendar in Settings first.', 'error')
        return
      }
      notifyError(error)
    }
  }

  return (
    <Panel className="flex flex-col gap-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-mono text-[11px] font-semibold tracking-[0.05em] uppercase text-ink">
            {round.roundType}
          </p>
          <p className="type-meta mt-1">
            {formatBoardDateTime(round.scheduledAt)}
            {round.interviewerName ? ` · ${round.interviewerName}` : ''}
          </p>
        </div>
        {synced && <Badge color={LINE_COLORS[1]}>ON CALENDAR</Badge>}
      </div>

      <RoundNote label="QUESTIONS" body={round.questionsAsked} />
      <RoundNote label="NOTES" body={round.notes} />
      <RoundNote label="REFLECTION" body={round.reflection} />

      <div className="flex flex-wrap gap-2 border-t border-rule pt-3">
        <Button
          size="sm"
          loading={sync.isPending}
          disabled={!round.scheduledAt}
          onClick={runSync}
          title={round.scheduledAt ? undefined : 'Set a scheduled time first'}
        >
          {synced ? 'Update calendar' : 'Add to calendar'}
        </Button>
        <Button size="sm" onClick={onEdit}>
          Edit
        </Button>
        <Button size="sm" variant="danger" onClick={onDelete}>
          Delete
        </Button>
      </div>
    </Panel>
  )
}

/** Per-round prep tracking, mounted inside the application drawer. */
export function InterviewRoundsSection({ applicationId }: { applicationId: Uuid }) {
  const { data: rounds, isLoading, isError, error, refetch } = useInterviewRounds(applicationId)
  const remove = useDeleteInterviewRound(applicationId)
  const { notify, notifyError } = useToast()

  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<InterviewRoundResponse | null>(null)
  const [pendingDelete, setPendingDelete] = useState<InterviewRoundResponse | null>(null)

  const confirmDelete = async () => {
    if (!pendingDelete) return
    try {
      await remove.mutateAsync(pendingDelete.id)
      notify('Round deleted.', 'success')
      setPendingDelete(null)
    } catch (caught) {
      notifyError(caught)
    }
  }

  const ordered = [...(rounds ?? [])].sort((a, b) =>
    (a.scheduledAt ?? a.createdAt).localeCompare(b.scheduledAt ?? b.createdAt),
  )

  return (
    <section>
      <SectionHeading
        aside={
          <Button size="sm" onClick={() => setCreating(true)}>
            Add round
          </Button>
        }
      >
        Interview rounds{rounds ? ` · ${rounds.length}` : ''}
      </SectionHeading>

      <div className="mt-3 flex flex-col gap-3">
        {isLoading && <TrackLoader label="LOADING ROUNDS" />}
        {isError && <ErrorState error={error} onRetry={() => void refetch()} />}
        {rounds && rounds.length === 0 && (
          <p className="font-mono text-[10px] leading-relaxed tracking-[0.04em] text-muted">
            No rounds recorded. Add one to track questions, notes and reflections.
          </p>
        )}
        {ordered.map((round) => (
          <RoundCard
            key={round.id}
            round={round}
            applicationId={applicationId}
            onEdit={() => setEditing(round)}
            onDelete={() => setPendingDelete(round)}
          />
        ))}
      </div>

      <InterviewFormDialog
        open={creating}
        applicationId={applicationId}
        onClose={() => setCreating(false)}
      />
      <InterviewFormDialog
        open={editing !== null}
        round={editing}
        applicationId={applicationId}
        onClose={() => setEditing(null)}
      />
      <ConfirmDialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        onConfirm={confirmDelete}
        loading={remove.isPending}
        title="Delete this round?"
        description="Its notes and reflection are removed. Any calendar event stays on Google Calendar."
        confirmLabel="Delete"
      />
    </section>
  )
}
