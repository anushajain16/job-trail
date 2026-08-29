import { NavLink, Outlet } from 'react-router-dom'
import { BackendStatusBadge } from '@/components/layout/backend-status-badge'
import { cn } from '@/lib/utils'

const NAV_LINKS = [{ to: '/', label: 'Dashboard' }]

/** Shell every route renders inside: header (brand, nav, backend status)
 * over a content area. Nothing here is route-specific — that's what
 * `<Outlet />` is for. */
export function AppLayout() {
  return (
    <div className="min-h-svh bg-background text-foreground">
      <header className="border-b">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-6">
            <span className="text-lg font-semibold tracking-tight">JobTrail</span>
            <nav className="flex items-center gap-4">
              {NAV_LINKS.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  end
                  className={({ isActive }) =>
                    cn(
                      'text-sm text-muted-foreground transition-colors hover:text-foreground',
                      isActive && 'font-medium text-foreground',
                    )
                  }
                >
                  {link.label}
                </NavLink>
              ))}
            </nav>
          </div>
          <BackendStatusBadge />
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  )
}
