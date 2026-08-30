import { toDateInputValue } from '@/lib/format'
import type { ApplicationCreateRequest, ApplicationResponse, ApplicationUpdateRequest } from '@/api/types'

/** Form state is all-strings — inputs are strings; coercion happens on submit. */
export interface ApplicationFormValues {
  company: string
  role: string
  location: string
  salaryMin: string
  salaryMax: string
  link: string
  source: string
  deadline: string
  notes: string
  jobDescriptionText: string
  /** Attachments are update-only — the create endpoint takes neither. */
  resumeVersionId: string
  coverLetterVersionId: string
}

export const EMPTY_APPLICATION_FORM: ApplicationFormValues = {
  company: '',
  role: '',
  location: '',
  salaryMin: '',
  salaryMax: '',
  link: '',
  source: '',
  deadline: '',
  notes: '',
  jobDescriptionText: '',
  resumeVersionId: '',
  coverLetterVersionId: '',
}

export function toFormValues(application?: ApplicationResponse | null): ApplicationFormValues {
  if (!application) return EMPTY_APPLICATION_FORM
  return {
    company: application.company,
    role: application.role,
    location: application.location ?? '',
    salaryMin: application.salaryMin?.toString() ?? '',
    salaryMax: application.salaryMax?.toString() ?? '',
    link: application.link ?? '',
    source: application.source ?? '',
    deadline: toDateInputValue(application.deadline),
    notes: application.notes ?? '',
    jobDescriptionText: application.jobDescriptionText ?? '',
    resumeVersionId: application.resumeVersionId ?? '',
    coverLetterVersionId: application.coverLetterVersionId ?? '',
  }
}

export type ApplicationFieldErrors = Partial<Record<keyof ApplicationFormValues, string>>

/** Mirrors the backend's bean validation so bad input never round-trips. */
export function validateApplication(values: ApplicationFormValues): ApplicationFieldErrors {
  const errors: ApplicationFieldErrors = {}
  if (!values.company.trim()) errors.company = 'Company is required.'
  else if (values.company.length > 255) errors.company = 'Max 255 characters.'

  if (!values.role.trim()) errors.role = 'Role is required.'
  else if (values.role.length > 255) errors.role = 'Max 255 characters.'

  if (values.location.length > 255) errors.location = 'Max 255 characters.'
  if (values.source.length > 100) errors.source = 'Max 100 characters.'
  if (values.notes.length > 5000) errors.notes = 'Max 5000 characters.'

  const min = values.salaryMin ? Number(values.salaryMin) : null
  const max = values.salaryMax ? Number(values.salaryMax) : null
  if (min != null && (!Number.isInteger(min) || min < 0)) errors.salaryMin = 'Whole number, 0 or more.'
  if (max != null && (!Number.isInteger(max) || max < 0)) errors.salaryMax = 'Whole number, 0 or more.'
  if (min != null && max != null && min > max) errors.salaryMax = 'Max must be at least the minimum.'

  if (values.link) {
    if (values.link.length > 2048) errors.link = 'Max 2048 characters.'
    else {
      try {
        new URL(values.link)
      } catch {
        errors.link = 'Enter a full URL, including https://'
      }
    }
  }
  return errors
}

const text = (value: string) => (value.trim() ? value.trim() : null)

/** Blank strings become `null`, so the backend clears rather than stores "". */
export function toCreatePayload(values: ApplicationFormValues): ApplicationCreateRequest {
  return {
    company: values.company.trim(),
    role: values.role.trim(),
    location: text(values.location),
    salaryMin: values.salaryMin ? Number(values.salaryMin) : null,
    salaryMax: values.salaryMax ? Number(values.salaryMax) : null,
    link: text(values.link),
    source: text(values.source),
    deadline: values.deadline || null,
    notes: values.notes.trim() ? values.notes : null,
    jobDescriptionText: values.jobDescriptionText.trim() ? values.jobDescriptionText : null,
  }
}

export function toUpdatePayload(values: ApplicationFormValues): ApplicationUpdateRequest {
  return {
    ...toCreatePayload(values),
    resumeVersionId: values.resumeVersionId || null,
    coverLetterVersionId: values.coverLetterVersionId || null,
  }
}
