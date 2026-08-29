import type { ApplicationInput } from '@/features/applications/types'
import type { ParsedJobPosting } from '@/features/jobposting/api'

/** Merges a successful parse into the form's current values — only over
 * fields the parse actually found (a null field leaves whatever the user
 * already typed alone, same "don't clobber" rule the backend's own PATCH
 * follows). `url` becomes the posting link since ParsedJobPosting doesn't
 * echo it back; `summary` deliberately does NOT become jobDescriptionText
 * — it's one or two sentences, not the posting, and a match score computed
 * against it would look precise while being misleading. */
export function applyParsedPosting(
  current: ApplicationInput,
  parsed: ParsedJobPosting,
  url: string,
): ApplicationInput {
  return {
    ...current,
    company: parsed.company ?? current.company,
    role: parsed.role ?? current.role,
    location: parsed.location ?? current.location,
    salaryMin: parsed.salaryMin !== null ? String(Math.round(parsed.salaryMin)) : current.salaryMin,
    salaryMax: parsed.salaryMax !== null ? String(Math.round(parsed.salaryMax)) : current.salaryMax,
    link: url,
  }
}
