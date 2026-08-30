import { NavLink } from 'react-router-dom'
import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

export interface NavItem {
  to: string
  label: string
}

/** Signage nav: uppercase links, active one underscored by a 2px ink rule. */
export function NavBar({ items, aside }: { items: NavItem[]; aside?: ReactNode }) {
  return (
    <nav className="flex items-center justify-between gap-6 border-b-2 border-ink px-8 py-4 sm:px-13">
      <div className="flex items-center gap-7">
        <NavLink to="/" className="no-underline">
          <span className="font-mono text-[13px] font-bold tracking-[0.16em] uppercase">
            Job<span className="text-line-coral">Trail</span>
          </span>
        </NavLink>
        <ul className="flex list-none items-center gap-5 p-0">
          {items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'block border-b-2 pb-0.5 font-mono text-[10px] tracking-[0.1em] uppercase no-underline transition-colors',
                    isActive
                      ? 'border-ink text-ink'
                      : 'border-transparent text-muted hover:border-rule hover:text-ink',
                  )
                }
              >
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </div>
      {aside && <div className="flex items-center gap-3">{aside}</div>}
    </nav>
  )
}
