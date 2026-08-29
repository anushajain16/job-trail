/**
 * Thin fetch wrapper — every call to the backend (Spring Boot, `/api/**`
 * and `/actuator/**`) goes through this, not raw `fetch`. Paths are
 * relative on purpose: in dev, Vite's proxy (see vite.config.ts) forwards
 * them to the backend same-origin, which is also how a real deploy serves
 * the built frontend from behind the same origin/gateway as the API — so
 * `VITE_API_BASE_URL` only needs setting for a split-origin deploy.
 */

import { clearSession, getAccessToken, getStoredRefreshToken, setSession } from '@/lib/auth-tokens'

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

// Concurrent 401s (e.g. several queries firing on a stale access token at
// once) share one refresh call instead of racing the backend's
// refresh-token rotation (RefreshTokenService.verifyAndRevoke revokes the
// token it's given, on every call) — the second racer would otherwise
// present an already-revoked token and fail.
let refreshInFlight: Promise<string | null> | null = null

function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) {
    return Promise.resolve(null)
  }

  refreshInFlight ??= apiFetch<{ accessToken: string; refreshToken: string }>('/api/auth/refresh', {
    method: 'POST',
    body: { refreshToken },
  })
    .then((tokens) => {
      setSession(tokens)
      return tokens.accessToken
    })
    .catch(() => {
      // The refresh token itself is bad (expired, already rotated,
      // revoked) — there's no session to recover. clearSession() notifies
      // AuthProvider via onSessionCleared so it can redirect to /login.
      clearSession()
      return null
    })
    .finally(() => {
      refreshInFlight = null
    })

  return refreshInFlight
}

/**
 * {@link apiFetch}, plus: attaches the current access token as a bearer
 * header, and on a 401 tries exactly one silent refresh-and-retry before
 * giving up. Every call to an authenticated endpoint (anything but
 * `/api/auth/signup|login|refresh` and `/actuator/**`) should go through
 * this, not `apiFetch` directly.
 */
export async function authFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const attempt = (token: string | null) =>
    apiFetch<T>(path, {
      ...options,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    })

  try {
    return await attempt(getAccessToken())
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      const refreshedToken = await refreshAccessToken()
      if (refreshedToken) {
        return attempt(refreshedToken)
      }
    }
    throw error
  }
}

/**
 * Same auth/retry shape as {@link authFetch}, but for an endpoint whose
 * response body is a file, not JSON (e.g. a CSV export) — a plain `<a
 * href>` can't carry the bearer token, so downloading one has to go
 * through fetch. Returns the raw blob plus the filename the backend chose
 * (from Content-Disposition), for the caller to hand to the browser.
 */
export async function authFetchFile(path: string): Promise<{ blob: Blob; filename: string }> {
  const attempt = async (token: string | null) => {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!response.ok) {
      const text = await response.text()
      const data = text ? JSON.parse(text) : null
      const message = (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string')
        ? data.message
        : response.statusText
      throw new ApiError(message, response.status, path)
    }
    return response
  }

  let response: Response
  try {
    response = await attempt(getAccessToken())
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      const refreshedToken = await refreshAccessToken()
      if (!refreshedToken) throw error
      response = await attempt(refreshedToken)
    } else {
      throw error
    }
  }

  const disposition = response.headers.get('Content-Disposition') ?? ''
  const filename = /filename="([^"]+)"/.exec(disposition)?.[1] ?? 'export.csv'
  return { blob: await response.blob(), filename }
}
