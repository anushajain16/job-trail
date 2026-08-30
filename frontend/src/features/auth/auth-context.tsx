import { createContext, use, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import { hasSession, onSessionChange } from '@/api/tokens'
import type { CurrentUser, OAuthProvider } from '@/api/types'
import * as authApi from './api'

type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

interface AuthContextValue {
  status: AuthStatus
  user: CurrentUser | null
  login: (credentials: authApi.Credentials) => Promise<void>
  signup: (credentials: authApi.Credentials) => Promise<void>
  loginWithOAuth: (provider: OAuthProvider, token: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

/**
 * Session state for the whole app. The token store is the source of truth
 * (it survives reloads and syncs across tabs); this provider mirrors it
 * into React and resolves the token into a user via `/api/auth/me`.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [tokenPresent, setTokenPresent] = useState(hasSession)

  useEffect(() => onSessionChange(setTokenPresent), [])

  const meQuery = useQuery({
    queryKey: queryKeys.me,
    queryFn: authApi.fetchMe,
    enabled: tokenPresent,
    staleTime: 5 * 60_000,
    retry: false,
  })

  // A stored token the backend no longer honours (revoked, restarted with a
  // new signing key) must not leave the app stuck on a loading screen.
  useEffect(() => {
    if (tokenPresent && meQuery.isError) void authApi.logout()
  }, [tokenPresent, meQuery.isError])

  const afterAuth = useCallback(async () => {
    setTokenPresent(true)
    await queryClient.invalidateQueries({ queryKey: queryKeys.me })
  }, [queryClient])

  const value = useMemo<AuthContextValue>(() => {
    const status: AuthStatus = !tokenPresent
      ? 'anonymous'
      : meQuery.data
        ? 'authenticated'
        : meQuery.isError
          ? 'anonymous'
          : 'loading'

    return {
      status,
      user: meQuery.data ?? null,
      login: async (credentials) => {
        await authApi.login(credentials)
        await afterAuth()
      },
      signup: async (credentials) => {
        await authApi.signup(credentials)
        await afterAuth()
      },
      loginWithOAuth: async (provider, token) => {
        await authApi.oauthLogin(provider, token)
        await afterAuth()
      },
      logout: async () => {
        await authApi.logout()
        setTokenPresent(false)
        queryClient.clear()
      },
    }
  }, [tokenPresent, meQuery.data, meQuery.isError, afterAuth, queryClient])

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthContextValue {
  const context = use(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
