import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/auth-context'

/** Wrap route(s) that require a logged-in user. Bounces to /login,
 * remembering where the user was headed so login can send them back. */
export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return (
      <div className="flex min-h-svh items-center justify-center text-sm text-muted-foreground">
        Loading…
      </div>
    )
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}

/** Wrap /login and /signup: an already-authenticated visitor is sent
 * straight to the app instead of seeing the auth forms again. */
export function PublicOnlyRoute() {
  const { status } = useAuth()

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
