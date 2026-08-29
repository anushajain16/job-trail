import { apiFetch } from '@/lib/api-client'
import type { AuthTokens } from '@/lib/auth-tokens'

// Mirrors backend/.../auth/dto/AuthResponse.java — tokenType is always
// "Bearer" and unused here (authFetch hardcodes the scheme itself).
interface AuthResponse extends AuthTokens {
  tokenType: string
}

export interface CurrentUser {
  id: string
  email: string
}

/**
 * `/api/auth/*` calls, unauthenticated (signup/login/refresh are public
 * routes — see backend's SecurityConfig — and `me` takes its own token
 * explicitly since it's called mid-login, before AuthProvider has
 * committed the new session to auth-tokens.ts). None of this goes through
 * `authFetch` — that's for already-authenticated feature calls.
 */
export const authApi = {
  signup: (email: string, password: string) =>
    apiFetch<AuthResponse>('/api/auth/signup', { method: 'POST', body: { email, password } }),

  login: (email: string, password: string) =>
    apiFetch<AuthResponse>('/api/auth/login', { method: 'POST', body: { email, password } }),

  refresh: (refreshToken: string) =>
    apiFetch<AuthResponse>('/api/auth/refresh', { method: 'POST', body: { refreshToken } }),

  logout: (refreshToken: string) =>
    apiFetch<null>('/api/auth/logout', { method: 'POST', body: { refreshToken } }),

  me: (accessToken: string) =>
    apiFetch<CurrentUser>('/api/auth/me', { headers: { Authorization: `Bearer ${accessToken}` } }),
}
