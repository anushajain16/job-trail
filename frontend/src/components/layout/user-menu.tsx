import { useCallback, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { cn } from '@/lib/cn'
import { useClickOutside } from '@/lib/use-click-outside'
import { useAuth } from '@/features/auth/auth-context'
import { useToast } from '@/components/ui/toast'

/** Account popover in the nav: identifies the session and signs it out. */
export function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { notifyError } = useToast()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useClickOutside(
    ref,
    useCallback(() => setOpen(false), []),
    open,
  )

  if (!user) return null

  const initials = user.email.slice(0, 2).toUpperCase()

  const signOut = async () => {
    try {
      await logout()
      navigate('/login', { replace: true })
    } catch (error) {
      notifyError(error)
    }
  }

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
        className={cn(
          'flex h-7 w-7 cursor-pointer items-center justify-center rounded-full border-[1.5px] border-ink',
          'font-mono text-[9px] font-bold tracking-[0.04em]',
          open ? 'bg-ink text-paper' : 'bg-transparent text-ink hover:bg-ink hover:text-paper',
        )}
      >
        {initials}
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 z-50 mt-2 w-56 rounded-[2px] border-2 border-ink bg-paper"
        >
          <div className="border-b border-rule px-4 py-3">
            <p className="type-meta">SIGNED IN AS</p>
            <p className="mt-1 truncate font-mono text-[10px] tracking-[0.04em] text-ink">
              {user.email}
            </p>
          </div>
          <button
            type="button"
            role="menuitem"
            onClick={signOut}
            className="w-full cursor-pointer px-4 py-3 text-left font-mono text-[10px] tracking-[0.1em] uppercase text-ink hover:bg-ink hover:text-paper"
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}
