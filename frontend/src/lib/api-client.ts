/**
 * Thin fetch wrapper — every call to the backend (Spring Boot, `/api/**`
 * and `/actuator/**`) goes through this, not raw `fetch`. Paths are
 * relative on purpose: in dev, Vite's proxy (see vite.config.ts) forwards
 * them to the backend same-origin, which is also how a real deploy serves
 * the built frontend from behind the same origin/gateway as the API — so
 * `VITE_API_BASE_URL` only needs setting for a split-origin deploy.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export class ApiError extends Error {
  readonly status: number
  readonly path: string

  constructor(message: string, status: number, path: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.path = path
  }
}

export type ApiRequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
}

/**
 * Issues a request and parses the JSON response body. Throws {@link ApiError}
 * on a non-2xx status or an empty/malformed body where JSON was expected —
 * callers (TanStack Query hooks) treat that as the query's error state
 * rather than a thrown-away promise rejection with no context.
 */
export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    const message = (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string')
      ? data.message
      : response.statusText
    throw new ApiError(message, response.status, path)
  }

  return data as T
}
