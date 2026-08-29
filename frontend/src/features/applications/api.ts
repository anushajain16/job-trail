import { authFetch } from '@/lib/api-client'
import type { Application, ApplicationInput, ApplicationListParams, Page } from '@/features/applications/types'

function buildQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) search.set(key, String(value))
  }
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

/** Trims the form's string fields down to a request body: required fields
 * always included, optional ones only when non-blank (see
 * ApplicationInput's doc comment for why — the API can't null a field via
 * PATCH, so an empty string would overwrite rather than clear it). Used
 * for both create and update — ApplicationUpdateRequest accepts the same
 * shape as ApplicationCreateRequest minus the @NotBlank on company/role. */
function toRequestBody(input: ApplicationInput): Record<string, unknown> {
  const body: Record<string, unknown> = {
    company: input.company.trim(),
    role: input.role.trim(),
  }
  const optional: Record<string, string> = {
    location: input.location,
    link: input.link,
    source: input.source,
    notes: input.notes,
    jobDescriptionText: input.jobDescriptionText,
    deadline: input.deadline,
  }
  for (const [key, value] of Object.entries(optional)) {
    const trimmed = value.trim()
    if (trimmed) body[key] = trimmed
  }
  if (input.salaryMin.trim()) body.salaryMin = Number(input.salaryMin)
  if (input.salaryMax.trim()) body.salaryMax = Number(input.salaryMax)
  return body
}

export const applicationsApi = {
  list: (params: ApplicationListParams) =>
    authFetch<Page<Application>>(`/api/applications${buildQuery({ page: params.page, size: params.size })}`),

  get: (id: string) => authFetch<Application>(`/api/applications/${id}`),

  create: (input: ApplicationInput) =>
    authFetch<Application>('/api/applications', { method: 'POST', body: toRequestBody(input) }),

  update: (id: string, input: ApplicationInput) =>
    authFetch<Application>(`/api/applications/${id}`, { method: 'PATCH', body: toRequestBody(input) }),

  remove: (id: string) => authFetch<null>(`/api/applications/${id}`, { method: 'DELETE' }),
}
