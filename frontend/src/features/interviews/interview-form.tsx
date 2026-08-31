import { useMemo, useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Field, FieldRow, Input, Textarea } from '@/components/ui/field'
import { FormError } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast'
import { splitDateTime, toIsoInstant } from '@/lib/format'
import type { InterviewRoundResponse, Uuid } from '@/api/types'
import { useCreateInterviewRound, useUpdateInterviewRound } from './hooks'

interface Values {
  roundType: string
  /**
   * Split into date + time on purpose. `<input type="datetime-local">`
   * reports an empty string for a *partial* entry — a date with no time
   * reads exactly like a blank field — so a half-filled schedule silently
   * saved as "unscheduled", and unscheduled rounds cannot sync to Google
   * Calendar.
   */
  scheduledDate: string
  scheduledTime: string
  interviewerName: string
  questionsAsked: string
  notes: string
  reflection: string
}

const EMPTY: Values = {
  roundType: '',
  scheduledDate: '',
  scheduledTime: '',
  interviewerName: '',
  questionsAsked: '',
  notes: '',
  reflection: '',
}

function toValues(round?: InterviewRoundResponse | null): Values {
  if (!round) return EMPTY
  return {
    roundType: round.roundType,
    ...splitDateTime(round.scheduledAt),
    interviewerName: round.interviewerName ?? '',
    questionsAsked: round.questionsAsked ?? '',
    notes: round.notes ?? '',
    reflection: round.reflection ?? '',
  }
}

export interface InterviewFormDialogProps {
  open: boolean
  onClose: () => void
  applicationId: Uuid
  /** Present = edit, absent = create. */
  round?: InterviewRoundResponse | null
}

/** Create / edit one interview round — same fields either way. */
export function InterviewFormDialog({ open, onClose, applicationId, round }: InterviewFormDialogProps) {
  const editing = Boolean(round)
  const create = useCreateInterviewRound(applicationId)
  const update = useUpdateInterviewRound(applicationId)
  const { notify } = useToast()

  const initial = useMemo(() => toValues(round), [round])
  const [values, setValues] = useState(initial)
  const [formKey, setFormKey] = useState(initial)
  const [errors, setErrors] = useState<Partial<Record<keyof Values, string>>>({})
  const [error, setError] = useState<unknown>(null)

  if (formKey !== initial) {
    setFormKey(initial)
    setValues(initial)
    setErrors({})
    setError(null)
  }

  const set = (key: keyof Values, value: string) =>
    setValues((current) => ({ ...current, [key]: value }))

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)

    const nextErrors: Partial<Record<keyof Values, string>> = {}
    if (!values.roundType.trim()) nextErrors.roundType = 'Name this round.'
    else if (values.roundType.length > 100) nextErrors.roundType = 'Max 100 characters.'
    if (values.interviewerName.length > 255) nextErrors.interviewerName = 'Max 255 characters.'
    if (values.notes.length > 5000) nextErrors.notes = 'Max 5000 characters.'
    if (values.scheduledTime && !values.scheduledDate) {
      nextErrors.scheduledDate = 'Pick a date to go with that time.'
    }
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    const text = (value: string) => (value.trim() ? value.trim() : null)
    const body = {
      roundType: values.roundType.trim(),
      // Local wall-clock in, instant out. A date with no time defaults to
      // 09:00 rather than dropping the schedule entirely.
      scheduledAt: toIsoInstant(values.scheduledDate, values.scheduledTime),
      interviewerName: text(values.interviewerName),
      questionsAsked: text(values.questionsAsked),
      notes: text(values.notes),
      reflection: text(values.reflection),
    }

    try {
      if (round) {
        await update.mutateAsync({ id: round.id, body })
        notify('Round updated.', 'success')
      } else {
        await create.mutateAsync(body)
        notify('Round added.', 'success')
      }
      onClose()
    } catch (caught) {
      setError(caught)
    }
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={editing ? 'Edit round' : 'Add interview round'}
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="solid"
            size="sm"
            type="submit"
            form="interview-form"
            loading={create.isPending || update.isPending}
          >
            {editing ? 'Save changes' : 'Add round'}
          </Button>
        </>
      }
    >
      <form id="interview-form" onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        <FieldRow>
          <Field label="Round type" error={errors.roundType} required>
            {(props) => (
              <Input
                {...props}
                placeholder="Technical screen"
                value={values.roundType}
                onChange={(event) => set('roundType', event.target.value)}
              />
            )}
          </Field>
          <Field
            label="Scheduled date"
            error={errors.scheduledDate}
            hint="Required to sync this round to Google Calendar"
          >
            {(props) => (
              <Input
                {...props}
                type="date"
                value={values.scheduledDate}
                onChange={(event) => set('scheduledDate', event.target.value)}
              />
            )}
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="Start time" hint={values.scheduledDate && !values.scheduledTime ? 'Defaults to 09:00' : undefined}>
            {(props) => (
              <Input
                {...props}
                type="time"
                value={values.scheduledTime}
                onChange={(event) => set('scheduledTime', event.target.value)}
              />
            )}
          </Field>
          <span />
        </FieldRow>

        <Field label="Interviewer" error={errors.interviewerName}>
          {(props) => (
            <Input
              {...props}
              value={values.interviewerName}
              onChange={(event) => set('interviewerName', event.target.value)}
            />
          )}
        </Field>

        <Field label="Questions asked" hint="Prep list before, actual questions after">
          {(props) => (
            <Textarea
              {...props}
              rows={3}
              value={values.questionsAsked}
              onChange={(event) => set('questionsAsked', event.target.value)}
            />
          )}
        </Field>

        <Field label="Notes" error={errors.notes} hint={`${values.notes.length}/5000`}>
          {(props) => (
            <Textarea
              {...props}
              rows={3}
              value={values.notes}
              onChange={(event) => set('notes', event.target.value)}
            />
          )}
        </Field>

        <Field label="Reflection" hint="What to do differently next time">
          {(props) => (
            <Textarea
              {...props}
              rows={3}
              value={values.reflection}
              onChange={(event) => set('reflection', event.target.value)}
            />
          )}
        </Field>

        <FormError error={error} />
      </form>
    </Dialog>
  )
}
