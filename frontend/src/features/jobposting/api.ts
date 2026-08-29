import { authFetch } from '@/lib/api-client'

// Mirrors backend/.../jobposting/dto/ParsedJobPosting.java.
export interface ParsedJobPosting {
  company: string | null
  role: string | null
  location: string | null
  employmentType: string | null
  seniority: string | null
  salaryMin: number | null
  salaryMax: number | null
  currency: string | null
  requiredSkills: string[]
  niceToHaveSkills: string[]
  summary: string | null
}

// Mirrors backend/.../jobposting/dto/ParseUrlResponse.java. Always a 200 —
// `available` is what callers branch on, not the HTTP status: false means
// ml-service was unreachable/slow/erroring, the documented "fall back to
// manual entry" path, not an error to throw.
export interface ParseUrlResult {
  available: boolean
  message: string | null
  parsed: ParsedJobPosting | null
  confidence: number | null
}

export const jobPostingApi = {
  parseUrl: (url: string) =>
    authFetch<ParseUrlResult>('/api/job-postings/parse', { method: 'POST', body: { url } }),
}
