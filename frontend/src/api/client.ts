import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './tokens'
import type { AuthResponse, ErrorResponse } from './types'

/** Empty in dev — Vite proxies `/api` to the backend (see vite.config.ts). */
const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

/** Every non-2xx surfaces as this, carrying the backend's `ErrorResponse`. */
export class ApiError extends Error {
  readonly status: number
  readonly payload: ErrorResponse | null

  constructor(status: number, message: string, payload: ErrorResponse | null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }

  get isUnauthorized() {
    return this.status === 401
  }

  get isNotFound() {
    return this.status === 404
  }

  get isConflict() {
    return this.status === 409
  }
}

export type QueryValue = string | number | boolean | null | undefined
export type Query = Record<string, QueryValue | QueryValue[]>

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'
  /** JSON body. Mutually exclusive with `formData`. */
  body?: unknown
  /** Multipart body, for document upload. */
  formData?: FormData
  query?: Query
  /** Skip the Authorization header (public auth routes). */
  anonymous?: boolean
  signal?: AbortSignal
}

function buildUrl(path: string, query?: Query): string {
  const url = `${BASE_URL}${path}`
  if (!query) return url
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value == null || value === '') continue
    if (Array.isArray(value)) {
      for (const item of value) if (item != null && item !== '') params.append(key, String(item))
    } else {
      params.append(key, String(value))
    }
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

async function toApiError(response: Response): Promise<ApiError> {
  let payload: ErrorResponse | null = null
  let message = response.statusText || `Request failed (${response.status})`
  try {
    const text = await response.text()
    if (text) {
      const parsed = JSON.parse(text) as ErrorResponse
      payload = parsed
      if (parsed?.message) message = parsed.message
    }
  } catch {
    // Non-JSON error body (proxy/gateway page) — keep the status text.
  }
  return new ApiError(response.status, message, payload)
}

/* ── Refresh rotation ───────────────────────────────────────────────────
   The backend rotates refresh tokens: presenting one revokes it. So two
   concurrent 401s must NOT each call /refresh — the second would present
   an already-revoked token and log the user out. All callers share a
   single in-flight refresh promise.
   ---------------------------------------------------------------------- */
let refreshInFlight: Promise<boolean> | null = null

async function refreshSession(): Promise<boolean> {
  const token = getRefreshToken()
  if (!token) return false

  refreshInFlight ??= (async () => {
    try {
      const response = await fetch(buildUrl('/api/auth/refresh'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: token }),
      })
      if (!response.ok) {
        clearTokens()
        return false
      }
      setTokens((await response.json()) as AuthResponse)
      return true
    } catch {
      clearTokens()
      return false
    } finally {
      refreshInFlight = null
    }
  })()

  return refreshInFlight
}

async function send(path: string, options: RequestOptions): Promise<Response> {
  const { method = 'GET', body, formData, query, anonymous, signal } = options

  const perform = async (): Promise<Response> => {
    const headers: Record<string, string> = {}
    if (!anonymous) {
      const token = getAccessToken()
      if (token) headers.Authorization = `Bearer ${token}`
    }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    // `formData` deliberately sets no Content-Type — the browser must add
    // its own multipart boundary.
    return fetch(buildUrl(path, query), {
      method,
      headers,
      body: formData ?? (body === undefined ? undefined : JSON.stringify(body)),
      signal,
    })
  }

  let response = await perform()
  if (response.status === 401 && !anonymous && getRefreshToken()) {
    if (await refreshSession()) response = await perform()
  }
  if (!response.ok) throw await toApiError(response)
  return response
}

/** JSON request. `T = void` for 204-returning endpoints. */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await send(path, options)
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** File-stream request (CSV exports) — returns the blob plus its filename. */
export async function requestBlob(
  path: string,
  options: RequestOptions = {},
): Promise<{ blob: Blob; filename: string }> {
  const response = await send(path, options)
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition)
  return {
    blob: await response.blob(),
    filename: match ? decodeURIComponent(match[1]) : 'export.csv',
  }
}

/** Trigger a browser download for a fetched blob. */
export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

export const api = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'GET' }),
  post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'POST', body }),
  patch: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T = void>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'DELETE' }),
  upload: <T>(path: string, formData: FormData, options?: Omit<RequestOptions, 'method'>) =>
    request<T>(path, { ...options, method: 'POST', formData }),
}
