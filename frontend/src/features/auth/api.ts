import { api } from '@/api/client'
import { clearTokens, getRefreshToken, setTokens } from '@/api/tokens'
import type { AuthResponse, CurrentUser, OAuthProvider } from '@/api/types'

export interface Credentials {
  email: string
  password: string
}

/** Signup and login both return a token pair — store it before resolving. */
async function authenticate(path: string, body: unknown): Promise<AuthResponse> {
  const auth = await api.post<AuthResponse>(path, body, { anonymous: true })
  setTokens(auth)
  return auth
}

export function signup(credentials: Credentials) {
  return authenticate('/api/auth/signup', credentials)
}

export function login(credentials: Credentials) {
  return authenticate('/api/auth/login', credentials)
}

/**
 * `token` is provider-shaped: a Google Identity Services ID token for
 * GOOGLE, GitHub's authorization `code` for GITHUB (the backend does that
 * exchange server-side, since it needs the client secret).
 */
export function oauthLogin(provider: OAuthProvider, token: string) {
  return authenticate(`/api/auth/oauth/${provider}`, { token })
}

export function fetchMe() {
  return api.get<CurrentUser>('/api/auth/me')
}

/** Best-effort revoke — local tokens are cleared either way. */
export async function logout() {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) await api.post<void>('/api/auth/logout', { refreshToken }, { anonymous: true })
  } finally {
    clearTokens()
  }
}
