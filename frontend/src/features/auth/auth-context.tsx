import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { type CurrentUser, authApi } from '@/features/auth/api'
import { type AuthTokens, clearSession, getStoredRefreshToken, onSessionCleared, setSession } from '@/lib/auth-tokens'

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

interface AuthContextValue {
  status: AuthStatus
  user: CurrentUser | null
  login: (email: string, password: string) => Promise<void>
  signup: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

/**
 * The single source of truth for "is anyone logged in, and as whom" —
 * everything else (ProtectedRoute, the header's user menu, login/signup
 * forms) reads it via {@link useAuth}, never `auth-tokens.ts` directly.
 *
 * On mount, a stored refresh token (survives a reload) is exchanged for a
 * fresh access token and the current user — that's the "handled" half of
 * "refresh handled"; the other half, a background 401 mid-session,
 * happens in `authFetch` (api-client.ts) and reaches here via
 * {@link onSessionCleared}.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<CurrentUser | null>(null)

  const establishSession = useCallback(async (tokens: AuthTokens) => {
    setSession(tokens)
    const currentUser = await authApi.me(tokens.accessToken)
    setUser(currentUser)
    setStatus('authenticated')
  }, [])

  useEffect(() => {
    let cancelled = false

    async function restoreSession() {
      const refreshToken = getStoredRefreshToken()
      if (!refreshToken) {
        setStatus('unauthenticated')
        return
      }
      try {
        const tokens = await authApi.refresh(refreshToken)
        if (cancelled) return
        await establishSession(tokens)
      } catch {
        if (cancelled) return
        clearSession()
        setStatus('unauthenticated')
      }
    }

    restoreSession()
    return () => {
      cancelled = true
    }
  }, [establishSession])

  // A background refresh failing (authFetch, mid-session) clears the
  // session outside of any handler here — this is what notices and syncs
  // React state to it, e.g. mid-use on an expired/revoked refresh token.
  useEffect(
    () =>
      onSessionCleared(() => {
        setUser(null)
        setStatus('unauthenticated')
      }),
    [],
  )

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await authApi.login(email, password)
      await establishSession(tokens)
    },
    [establishSession],
  )

  const signup = useCallback(
    async (email: string, password: string) => {
      const tokens = await authApi.signup(email, password)
      await establishSession(tokens)
    },
    [establishSession],
  )

  const logout = useCallback(async () => {
    const refreshToken = getStoredRefreshToken()
    if (refreshToken) {
      // Best-effort: revokes the refresh token server-side so it can't be
      // replayed, but a failed request here still logs the user out
      // locally — there's nothing useful to do with the error.
      await authApi.logout(refreshToken).catch(() => {})
    }
    clearSession()
    setUser(null)
    setStatus('unauthenticated')
  }, [])

  const value = useMemo(
    () => ({ status, user, login, signup, logout }),
    [status, user, login, signup, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
