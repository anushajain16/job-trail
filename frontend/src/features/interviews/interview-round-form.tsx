import { type FormEvent, type ReactNode, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { InterviewRoundInput } from '@/features/interviews/types'

type FieldErrors = Partial<Record<keyof InterviewRoundInput, string>>

// Mirrors backend/.../interview/dto/InterviewRoundCreateRequest.java's
// constraints — a UX nicety, the backend is still the real gate.
function validate(input: InterviewRoundInput): FieldErrors {
  const errors: FieldErrors = {}
  if (!input.roundType.trim()) errors.roundType = 'Round type is required.'
  else if (input.roundType.length > 100) errors.roundType = 'Must be 100 characters or fewer.'
  if (input.interviewerName.length > 255) errors.interviewerName = 'Must be 255 characters or fewer.'
  if (input.notes.length > 5000) errors.notes = 'Must be 5000 characters or fewer.'
  return errors
}

interface InterviewRoundFormProps {
  initialValues: InterviewRoundInput
  submitLabel: string
  pendingLabel: string
  isPending: boolean
  submitError?: string
  onSubmit: (input: InterviewRoundInput) => void
  onCancel: () => void
}

export function InterviewRoundForm({
  initialValues,
  submitLabel,
  pendingLabel,
  isPending,
  submitError,
  onSubmit,
  onCancel,
}: InterviewRoundFormProps) {
  const [values, setValues] = useState<InterviewRoundInput>(initialValues)
  const [errors, setErrors] = useState<FieldErrors>({})

  function setField<K extends keyof InterviewRoundInput>(field: K, value: InterviewRoundInput[K]) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const fieldErrors = validate(values)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length === 0) {
      onSubmit(values)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Round type" error={errors.roundType}>
          <Input
            value={values.roundType}
            onChange={(e) => setField('roundType', e.target.value)}
            placeholder="e.g. Technical R1"
            maxLength={100}
            required
          />
        </Field>
        <Field label="Date" error={undefined}>
          <Input type="datetime-local" value={values.scheduledAt} onChange={(e) => setField('scheduledAt', e.target.value)} />
        </Field>
        <Field label="Interviewer" error={errors.interviewerName}>
          <Input
            value={values.interviewerName}
            onChange={(e) => setField('interviewerName', e.target.value)}
            maxLength={255}
          />
        </Field>
      </div>

      <Field label="Questions asked" error={undefined}>
        <Textarea
          value={values.questionsAsked}
          onChange={(e) => setField('questionsAsked', e.target.value)}
          rows={3}
          placeholder="One per line…"
        />
      </Field>

      <Field label="Notes" error={errors.notes}>
        <Textarea value={values.notes} onChange={(e) => setField('notes', e.target.value)} rows={3} maxLength={5000} />
      </Field>

      {/* Deliberately the most prominent field on the form — it's the one
          worth re-reading before the next round. */}
      <Field label="Reflection" error={undefined} emphasize>
        <Textarea
          value={values.reflection}
          onChange={(e) => setField('reflection', e.target.value)}
          rows={4}
          placeholder="How did it go? What would you do differently next time?"
          className="border-primary/40"
        />
      </Field>

      {submitError && (
        <p role="alert" className="text-sm text-destructive">
          {submitError}
        </p>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel} disabled={isPending}>
          Cancel
        </Button>
        <Button type="submit" disabled={isPending}>
          {isPending ? pendingLabel : submitLabel}
        </Button>
      </div>
    </form>
  )
}

function Field({
  label,
  error,
  emphasize,
  children,
}: {
  label: string
  error?: string
  emphasize?: boolean
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label className={emphasize ? 'font-semibold' : undefined}>{label}</Label>
      {children}
      {error && (
        <p role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}
