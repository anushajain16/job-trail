import { useMemo, useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Field, FieldRow, Input, Textarea } from '@/components/ui/field'
import { FormError } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast'
import { toDateTimeInputValue } from '@/lib/format'
import type { InterviewRoundResponse, Uuid } from '@/api/types'
import { useCreateInterviewRound, useUpdateInterviewRound } from './hooks'

interface Values {
  roundType: string
  scheduledAt: string
  interviewerName: string
  questionsAsked: string
  notes: string
  reflection: string
}

const EMPTY: Values = {
  roundType: '',
  scheduledAt: '',
  interviewerName: '',
  questionsAsked: '',
  notes: '',
  reflection: '',
}

function toValues(round?: InterviewRoundResponse | null): Values {
  if (!round) return EMPTY
  return {
    roundType: round.roundType,
    scheduledAt: toDateTimeInputValue(round.scheduledAt),
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
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    const text = (value: string) => (value.trim() ? value.trim() : null)
    const body = {
      roundType: values.roundType.trim(),
      // `datetime-local` is wall-clock; the API stores an instant.
      scheduledAt: values.scheduledAt ? new Date(values.scheduledAt).toISOString() : null,
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
          <Field label="Scheduled at" hint="Used for calendar sync">
            {(props) => (
              <Input
                {...props}
                type="datetime-local"
                value={values.scheduledAt}
                onChange={(event) => set('scheduledAt', event.target.value)}
              />
            )}
          </Field>
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
