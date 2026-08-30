import { Outlet } from 'react-router-dom'
import { AuthProvider } from '@/features/auth/auth-context'

/**
 * Router root. AuthProvider lives inside the router (not above it) so its
 * consumers can navigate, and so guards and pages share one session.
 */
export function RootLayout() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}
