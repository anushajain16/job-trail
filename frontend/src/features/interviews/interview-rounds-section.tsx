import { useState } from 'react'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { describeApiError } from '@/lib/describe-api-error'
import { InterviewRoundForm } from '@/features/interviews/interview-round-form'
import {
  useCreateInterviewRoundMutation,
  useDeleteInterviewRoundMutation,
  useInterviewRoundsQuery,
  useUpdateInterviewRoundMutation,
} from '@/features/interviews/hooks'
import { EMPTY_INTERVIEW_ROUND_INPUT, interviewRoundToInput, type InterviewRound } from '@/features/interviews/types'

interface InterviewRoundsSectionProps {
  applicationId: string
}

// Prep-tracker section for the application detail view: every round
// logged against this application, chronological, each editable in place
// (not append-only like status history — see the interview package's
// package-info). Add/edit both use the same InterviewRoundForm, toggled
// by which round (if any) is currently being edited.
export function InterviewRoundsSection({ applicationId }: InterviewRoundsSectionProps) {
  const { data: rounds, isPending } = useInterviewRoundsQuery(applicationId)
  const createMutation = useCreateInterviewRoundMutation(applicationId)
  const updateMutation = useUpdateInterviewRoundMutation(applicationId)
  const deleteMutation = useDeleteInterviewRoundMutation(applicationId)

  const [isAdding, setIsAdding] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [deletingRound, setDeletingRound] = useState<InterviewRound | null>(null)

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-muted-foreground">Interview rounds</p>
        {!isAdding && (
          <Button size="sm" variant="outline" onClick={() => setIsAdding(true)}>
            Add round
          </Button>
        )}
      </div>

      {isAdding && (
        <Card size="sm">
          <CardContent>
            <InterviewRoundForm
              initialValues={EMPTY_INTERVIEW_ROUND_INPUT}
              submitLabel="Add round"
              pendingLabel="Adding…"
              isPending={createMutation.isPending}
              submitError={createMutation.isError ? describeApiError(createMutation.error) : undefined}
              onSubmit={(input) => createMutation.mutate(input, { onSuccess: () => setIsAdding(false) })}
              onCancel={() => setIsAdding(false)}
            />
          </CardContent>
        </Card>
      )}

      {isPending ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : rounds && rounds.length > 0 ? (
        <ol className="flex flex-col gap-3">
          {rounds.map((round) =>
            editingId === round.id ? (
              <Card key={round.id} size="sm">
                <CardContent>
                  <InterviewRoundForm
                    initialValues={interviewRoundToInput(round)}
                    submitLabel="Save"
                    pendingLabel="Saving…"
                    isPending={updateMutation.isPending}
                    submitError={updateMutation.isError ? describeApiError(updateMutation.error) : undefined}
                    onSubmit={(input) =>
                      updateMutation.mutate({ id: round.id, input }, { onSuccess: () => setEditingId(null) })
                    }
                    onCancel={() => setEditingId(null)}
                  />
                </CardContent>
              </Card>
            ) : (
              <li key={round.id}>
                <Card size="sm">
                  <CardContent className="flex flex-col gap-2">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-sm font-medium">{round.roundType}</p>
                        <p className="text-xs text-muted-foreground">
                          {round.scheduledAt ? new Date(round.scheduledAt).toLocaleString() : 'No date set'}
                          {round.interviewerName ? ` · ${round.interviewerName}` : ''}
                        </p>
                      </div>
                      <div className="flex gap-1">
                        <Button size="xs" variant="ghost" onClick={() => setEditingId(round.id)}>
                          Edit
                        </Button>
                        <Button size="xs" variant="ghost" onClick={() => setDeletingRound(round)}>
                          Delete
                        </Button>
                      </div>
                    </div>

                    {round.questionsAsked && (
                      <div>
                        <p className="text-xs text-muted-foreground">Questions asked</p>
                        <p className="whitespace-pre-wrap text-sm">{round.questionsAsked}</p>
                      </div>
                    )}

                    {round.notes && (
                      <div>
                        <p className="text-xs text-muted-foreground">Notes</p>
                        <p className="whitespace-pre-wrap text-sm">{round.notes}</p>
                      </div>
                    )}

                    {round.reflection && (
                      // The prominent field — bordered and set off from the
                      // rest of the round, since it's the part worth
                      // re-reading before the next interview.
                      <div className="rounded-lg border border-primary/30 bg-primary/5 p-2.5">
                        <p className="text-xs font-semibold text-primary">Reflection</p>
                        <p className="whitespace-pre-wrap text-sm">{round.reflection}</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </li>
            ),
          )}
        </ol>
      ) : (
        !isAdding && <p className="text-sm text-muted-foreground">No rounds logged yet.</p>
      )}

      <AlertDialog open={deletingRound !== null} onOpenChange={(open) => !open && setDeletingRound(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this round?</AlertDialogTitle>
            <AlertDialogDescription>
              {deletingRound && <>This removes the "{deletingRound.roundType}" round, including its notes and reflection. This can't be undone.</>}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (!deletingRound) return
                deleteMutation.mutate(deletingRound.id, { onSuccess: () => setDeletingRound(null) })
              }}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
