import type { AuthResponse } from './types'

/**
 * Token storage. Kept in localStorage so a refresh survives a reload, and
 * mirrored in memory so the request path never touches storage per-call.
 * A `storage` event listener keeps multiple tabs in sync (logging out in
 * one tab logs out the others).
 */

const ACCESS_KEY = 'jobtrail.accessToken'
const REFRESH_KEY = 'jobtrail.refreshToken'

let accessToken: string | null = null
let refreshToken: string | null = null
let hydrated = false

type Listener = (hasSession: boolean) => void
const listeners = new Set<Listener>()

function hydrate() {
  if (hydrated) return
  hydrated = true
  try {
    accessToken = localStorage.getItem(ACCESS_KEY)
    refreshToken = localStorage.getItem(REFRESH_KEY)
  } catch {
    // Private mode / storage blocked — session stays in memory only.
  }
  try {
    window.addEventListener('storage', (event) => {
      if (event.key !== ACCESS_KEY && event.key !== REFRESH_KEY) return
      accessToken = localStorage.getItem(ACCESS_KEY)
      refreshToken = localStorage.getItem(REFRESH_KEY)
      emit()
    })
  } catch {
    // Non-browser context (tests) — nothing to sync.
  }
}

function emit() {
  for (const listener of listeners) listener(accessToken != null)
}

export function getAccessToken(): string | null {
  hydrate()
  return accessToken
}

export function getRefreshToken(): string | null {
  hydrate()
  return refreshToken
}

export function setTokens(auth: AuthResponse) {
  hydrate()
  accessToken = auth.accessToken
  refreshToken = auth.refreshToken
  try {
    localStorage.setItem(ACCESS_KEY, auth.accessToken)
    localStorage.setItem(REFRESH_KEY, auth.refreshToken)
  } catch {
    // Ignore — in-memory tokens still work for this tab's lifetime.
  }
  emit()
}

export function clearTokens() {
  hydrate()
  accessToken = null
  refreshToken = null
  try {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
  } catch {
    // Ignore.
  }
  emit()
}

export function hasSession(): boolean {
  return getAccessToken() != null
}

/** Subscribe to session gain/loss (used by the auth context). */
export function onSessionChange(listener: Listener): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
