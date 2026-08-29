import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/features/auth/auth-context'

/** Current user's email + logout, in the header. Only ever rendered
 * inside ProtectedRoute, so `user` is always set here. */
export function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-muted-foreground">{user?.email}</span>
      <Button variant="outline" size="sm" onClick={handleLogout}>
        Log out
      </Button>
    </div>
  )
}
