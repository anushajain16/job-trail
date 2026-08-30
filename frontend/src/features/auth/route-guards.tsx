import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { TrackLoader } from '@/components/ui/feedback'
import { useAuth } from './auth-context'

/** Gate for everything behind a session. Remembers where you were headed. */
export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <TrackLoader label="CHECKING TICKET" />
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location }} />
  return <Outlet />
}

/** Login/signup: bounce an already-signed-in user back onto the map. */
export function PublicOnlyRoute() {
  const { status } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: Location } | null)?.from

  if (status === 'loading') return <TrackLoader label="CHECKING TICKET" />
  if (status === 'authenticated') return <Navigate to={from?.pathname ?? '/map'} replace />
  return <Outlet />
}
