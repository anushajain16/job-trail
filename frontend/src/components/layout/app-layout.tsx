import { Outlet } from 'react-router-dom'
import { BackendStatusBadge } from './backend-status-badge'
import { NavBar, type NavItem } from './nav-bar'
import { UserMenu } from './user-menu'

const NAV_ITEMS: NavItem[] = [
  { to: '/map', label: 'Transit Map' },
  { to: '/applications', label: 'Applications' },
  { to: '/documents', label: 'Documents' },
  { to: '/analytics', label: 'Analytics' },
  { to: '/settings', label: 'Settings' },
]

/** The signed-in frame: nav rule on top, page content on warm paper. */
export function AppLayout() {
  return (
    <div className="flex min-h-full flex-col bg-paper">
      <NavBar
        items={NAV_ITEMS}
        aside={
          <>
            <BackendStatusBadge className="hidden sm:flex" />
            <UserMenu />
          </>
        }
      />
      <main className="flex-1 px-8 py-11 sm:px-13">
        <Outlet />
      </main>
    </div>
  )
}
