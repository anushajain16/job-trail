import { useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { Drawer } from '@/components/ui/drawer'
import { Field, FieldRow, Input, Select, Textarea } from '@/components/ui/field'
import { FormError } from '@/components/ui/feedback'
import { SectionHeading } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { useDocuments } from '@/features/documents/hooks'
import { JdAutofill } from '@/features/job-postings/jd-autofill'
import type { ApplicationResponse } from '@/api/types'
import {
  toCreatePayload,
  toFormValues,
  toUpdatePayload,
  validateApplication,
  type ApplicationFieldErrors,
} from './form-values'
import { useCreateApplication, useUpdateApplication } from './hooks'

/** Résumé / cover-letter attachment pickers — update-only, so edit-only. */
function AttachmentFields({
  resumeVersionId,
  coverLetterVersionId,
  onChange,
}: {
  resumeVersionId: string
  coverLetterVersionId: string
  onChange: (key: 'resumeVersionId' | 'coverLetterVersionId', value: string) => void
}) {
  const { data: documents } = useDocuments()
  const options = (type: 'RESUME' | 'COVER_LETTER') =>
    (documents ?? [])
      .filter((document_) => document_.type === type)
      .map((document_) => ({
        value: document_.id,
        label: document_.label ?? document_.originalFilename,
      }))

  return (
    <div className="flex flex-col gap-3">
      <SectionHeading>Attachments</SectionHeading>
      <FieldRow>
        <Field label="Résumé version" hint="Feeds résumé performance analytics">
          {(props) => (
            <Select
              {...props}
              placeholder="None"
              value={resumeVersionId}
              onChange={(event) => onChange('resumeVersionId', event.target.value)}
              options={options('RESUME')}
            />
          )}
        </Field>
        <Field label="Cover letter">
          {(props) => (
            <Select
              {...props}
              placeholder="None"
              value={coverLetterVersionId}
              onChange={(event) => onChange('coverLetterVersionId', event.target.value)}
              options={options('COVER_LETTER')}
            />
          )}
        </Field>
      </FieldRow>
    </div>
  )
}

export interface ApplicationFormDrawerProps {
  open: boolean
  onClose: () => void
  /** Present = edit, absent = create. */
  application?: ApplicationResponse | null
  /** Replaces the JD-autofill block at the top of the form. */
  headerSlot?: ReactNode
}

/**
 * Create / edit an application. One form for both, because the field set
 * and every validation rule are identical — only the verb differs.
 */
export function ApplicationFormDrawer({
  open,
  onClose,
  application,
  headerSlot,
}: ApplicationFormDrawerProps) {
  const editing = Boolean(application)
  const { notify } = useToast()
  const create = useCreateApplication()
  const update = useUpdateApplication()

  // Remount the field state whenever the drawer opens on a different record.
  const initial = useMemo(() => toFormValues(application), [application])
  const [values, setValues] = useState(initial)
  const [formKey, setFormKey] = useState(initial)
  const [errors, setErrors] = useState<ApplicationFieldErrors>({})
  const [error, setError] = useState<unknown>(null)

  if (formKey !== initial) {
    setFormKey(initial)
    setValues(initial)
    setErrors({})
    setError(null)
  }

  const set = (key: keyof typeof values, value: string) =>
    setValues((current) => ({ ...current, [key]: value }))

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    const nextErrors = validateApplication(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    try {
      if (application) {
        await update.mutateAsync({ id: application.id, body: toUpdatePayload(values) })
        notify('Application updated.', 'success')
      } else {
        await create.mutateAsync(toCreatePayload(values))
        notify('Line added to the map.', 'success')
      }
      onClose()
    } catch (caught) {
      setError(caught)
    }
  }

  return (
    <Drawer
      open={open}
      onClose={onClose}
      width="lg"
      title={<h2 className="type-title uppercase">{editing ? 'Edit line' : 'New line'}</h2>}
      subtitle={editing ? application?.company : 'ADD AN APPLICATION TO THE NETWORK'}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="ghost" size="sm" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="solid"
            size="sm"
            form="application-form"
            type="submit"
            loading={create.isPending || update.isPending}
          >
            {editing ? 'Save changes' : 'Create'}
          </Button>
        </div>
      }
    >
      <form id="application-form" onSubmit={handleSubmit} className="flex flex-col gap-5" noValidate>
        {headerSlot ?? <JdAutofill values={values} setValues={setValues} />}

        <FieldRow>
          <Field label="Company" error={errors.company} required>
            {(props) => (
              <Input {...props} value={values.company} onChange={(e) => set('company', e.target.value)} />
            )}
          </Field>
          <Field label="Role" error={errors.role} required>
            {(props) => (
              <Input {...props} value={values.role} onChange={(e) => set('role', e.target.value)} />
            )}
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="Location" error={errors.location}>
            {(props) => (
              <Input {...props} value={values.location} onChange={(e) => set('location', e.target.value)} />
            )}
          </Field>
          <Field label="Source" hint="Where you found it — board, referral, site" error={errors.source}>
            {(props) => (
              <Input {...props} value={values.source} onChange={(e) => set('source', e.target.value)} />
            )}
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="Salary min" error={errors.salaryMin}>
            {(props) => (
              <Input
                {...props}
                type="number"
                min={0}
                step={1}
                value={values.salaryMin}
                onChange={(e) => set('salaryMin', e.target.value)}
              />
            )}
          </Field>
          <Field label="Salary max" error={errors.salaryMax}>
            {(props) => (
              <Input
                {...props}
                type="number"
                min={0}
                step={1}
                value={values.salaryMax}
                onChange={(e) => set('salaryMax', e.target.value)}
              />
            )}
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="Link" error={errors.link}>
            {(props) => (
              <Input
                {...props}
                type="url"
                placeholder="https://"
                value={values.link}
                onChange={(e) => set('link', e.target.value)}
              />
            )}
          </Field>
          <Field label="Deadline" error={errors.deadline}>
            {(props) => (
              <Input
                {...props}
                type="date"
                value={values.deadline}
                onChange={(e) => set('deadline', e.target.value)}
              />
            )}
          </Field>
        </FieldRow>

        {editing && (
          <AttachmentFields
            resumeVersionId={values.resumeVersionId}
            coverLetterVersionId={values.coverLetterVersionId}
            onChange={set}
          />
        )}

        <Field label="Notes" error={errors.notes} hint={`${values.notes.length}/5000`}>
          {(props) => (
            <Textarea {...props} rows={4} value={values.notes} onChange={(e) => set('notes', e.target.value)} />
          )}
        </Field>

        <div className="flex flex-col gap-3">
          <SectionHeading>Job description</SectionHeading>
          <Field hint="Pasted in full — this is what match scoring runs against.">
            {(props) => (
              <Textarea
                {...props}
                rows={7}
                value={values.jobDescriptionText}
                onChange={(e) => set('jobDescriptionText', e.target.value)}
              />
            )}
          </Field>
        </div>

        <FormError error={error} />
      </form>
    </Drawer>
  )
}
