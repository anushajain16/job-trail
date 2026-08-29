import { type FormEvent, type ReactNode, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { ApplicationInput } from '@/features/applications/types'

type FieldErrors = Partial<Record<keyof ApplicationInput, string>>

// Mirrors backend/.../application/dto/ApplicationCreateRequest.java's
// constraints — client-side validation is a UX nicety, the backend is
// still the real gate (its rejection surfaces as the submit-level error).
function validate(input: ApplicationInput): FieldErrors {
  const errors: FieldErrors = {}

  if (!input.company.trim()) errors.company = 'Company is required.'
  else if (input.company.length > 255) errors.company = 'Must be 255 characters or fewer.'

  if (!input.role.trim()) errors.role = 'Role is required.'
  else if (input.role.length > 255) errors.role = 'Must be 255 characters or fewer.'

  if (input.location.length > 255) errors.location = 'Must be 255 characters or fewer.'
  if (input.source.length > 100) errors.source = 'Must be 100 characters or fewer.'
  if (input.notes.length > 5000) errors.notes = 'Must be 5000 characters or fewer.'

  if (input.link.trim()) {
    if (input.link.length > 2048) errors.link = 'Must be 2048 characters or fewer.'
    else if (!isValidUrl(input.link.trim())) errors.link = 'Must be a valid URL, e.g. https://example.com/job.'
  }

  const min = parseNonNegativeInt(input.salaryMin)
  const max = parseNonNegativeInt(input.salaryMax)
  if (input.salaryMin.trim() && min === null) errors.salaryMin = 'Must be a whole number, 0 or more.'
  if (input.salaryMax.trim() && max === null) errors.salaryMax = 'Must be a whole number, 0 or more.'
  if (min !== null && max !== null && min > max) errors.salaryMax = 'Must be greater than or equal to minimum salary.'

  return errors
}

function isValidUrl(value: string): boolean {
  try {
    new URL(value)
    return true
  } catch {
    return false
  }
}

function parseNonNegativeInt(value: string): number | null {
  if (!value.trim() || !/^\d+$/.test(value.trim())) return null
  return Number(value)
}

interface ApplicationFormProps {
  initialValues: ApplicationInput
  submitLabel: string
  pendingLabel: string
  isPending: boolean
  submitError?: string
  onSubmit: (input: ApplicationInput) => void
  onCancel: () => void
}

export function ApplicationForm({
  initialValues,
  submitLabel,
  pendingLabel,
  isPending,
  submitError,
  onSubmit,
  onCancel,
}: ApplicationFormProps) {
  const [values, setValues] = useState<ApplicationInput>(initialValues)
  const [errors, setErrors] = useState<FieldErrors>({})

  function setField<K extends keyof ApplicationInput>(field: K, value: ApplicationInput[K]) {
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
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Company" error={errors.company}>
          <Input value={values.company} onChange={(e) => setField('company', e.target.value)} maxLength={255} required />
        </Field>
        <Field label="Role" error={errors.role}>
          <Input value={values.role} onChange={(e) => setField('role', e.target.value)} maxLength={255} required />
        </Field>
        <Field label="Location" error={errors.location}>
          <Input value={values.location} onChange={(e) => setField('location', e.target.value)} maxLength={255} />
        </Field>
        <Field label="Source" error={errors.source}>
          <Input
            value={values.source}
            onChange={(e) => setField('source', e.target.value)}
            maxLength={100}
            placeholder="e.g. LinkedIn, referral"
          />
        </Field>
        <Field label="Minimum salary" error={errors.salaryMin}>
          <Input
            type="number"
            inputMode="numeric"
            min={0}
            value={values.salaryMin}
            onChange={(e) => setField('salaryMin', e.target.value)}
          />
        </Field>
        <Field label="Maximum salary" error={errors.salaryMax}>
          <Input
            type="number"
            inputMode="numeric"
            min={0}
            value={values.salaryMax}
            onChange={(e) => setField('salaryMax', e.target.value)}
          />
        </Field>
        <Field label="Deadline" error={undefined}>
          <Input type="date" value={values.deadline} onChange={(e) => setField('deadline', e.target.value)} />
        </Field>
        <Field label="Posting link" error={errors.link}>
          <Input
            type="url"
            value={values.link}
            onChange={(e) => setField('link', e.target.value)}
            placeholder="https://…"
            maxLength={2048}
          />
        </Field>
      </div>

      <Field label="Notes" error={errors.notes}>
        <Textarea value={values.notes} onChange={(e) => setField('notes', e.target.value)} maxLength={5000} rows={4} />
      </Field>

      <Field label="Job description" error={undefined}>
        <Textarea
          value={values.jobDescriptionText}
          onChange={(e) => setField('jobDescriptionText', e.target.value)}
          rows={6}
          placeholder="Paste the posting text here — this is what match scoring runs against."
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

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      {children}
      {error && (
        <p role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}
