/**
 * The session's tokens, held outside React so `api-client.ts` (a plain
 * function, not a hook) can read/refresh them on every request.
 * `AuthProvider` (src/features/auth/auth-context.tsx) is the only thing
 * that should call the mutating functions here directly — everywhere else
 * goes through `useAuth()`.
 *
 * - Access token: in-memory only, never persisted. It's short-lived
 *   (backend default 15m — app.jwt.access-token-ttl) and this keeps it out
 *   of localStorage, where an XSS payload could read it long after the
 *   fact. Lost on a hard refresh, which is fine: `AuthProvider` re-derives
 *   one from the refresh token on mount.
 * - Refresh token: localStorage, since it's what makes "stay logged in
 *   across a reload" possible at all. It's still bearer-token auth either
 *   way (this API has no httpOnly-cookie option), but it's opaque and
 *   single-use — the backend's RefreshTokenService rotates it on every use
 *   (see AuthService.refresh), so a stolen value only replays until the
 *   legitimate client's next refresh silently invalidates it.
 */

const REFRESH_TOKEN_KEY = 'jobtrail.refreshToken'

let accessToken: string | null = null

const sessionClearedListeners = new Set<() => void>()

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export function getAccessToken(): string | null {
  return accessToken
}

export function getStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setSession(tokens: AuthTokens): void {
  accessToken = tokens.accessToken
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export function clearSession(): void {
  accessToken = null
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  for (const listener of sessionClearedListeners) listener()
}

/**
 * Notified whenever the session is cleared from *outside* an explicit
 * `logout()` call — i.e. a background token refresh (see
 * `authFetch` in api-client.ts) failed because the refresh token was
 * invalid/expired/already-rotated. `AuthProvider` uses this to flip its
 * React state immediately instead of only on the next render that happens
 * to re-check it.
 */
export function onSessionCleared(listener: () => void): () => void {
  sessionClearedListeners.add(listener)
  return () => sessionClearedListeners.delete(listener)
}
