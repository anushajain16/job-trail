import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { useDismissable } from '@/lib/use-dismissable'

export interface DrawerProps {
  open: boolean
  onClose: () => void
  /** Header content — typically a `<LineName />`. */
  title: ReactNode
  subtitle?: ReactNode
  children: ReactNode
  /** Pinned action row at the bottom of the panel. */
  footer?: ReactNode
  width?: 'md' | 'lg'
}

/**
 * Right-side detail drawer: a sheet of paper slid in from the edge, held
 * by a 2px ink rule. Used for application detail and any nested editor.
 */
export function Drawer({ open, onClose, title, subtitle, children, footer, width = 'md' }: DrawerProps) {
  useDismissable(open, onClose)
  if (!open) return null

  return (
    <>
      <div
        aria-hidden
        onClick={onClose}
        className="fixed inset-0 z-40 bg-ink/8 animate-[fade_120ms_ease-out]"
      />
      <aside
        role="dialog"
        aria-modal="true"
        className={cn(
          'fixed inset-y-0 right-0 z-50 flex w-full flex-col border-l-2 border-ink bg-paper',
          'animate-[slide_160ms_ease-out]',
          width === 'md' ? 'sm:w-[400px]' : 'sm:w-[560px]',
        )}
      >
        <div className="flex items-start justify-between gap-4 border-b border-rule px-7 pt-8 pb-5">
          <div className="min-w-0">
            {title}
            {subtitle && <div className="type-meta mt-1.5">{subtitle}</div>}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="h-6 w-6 shrink-0 cursor-pointer rounded-[2px] border-[1.5px] border-ink font-mono text-[11px] leading-none text-ink hover:bg-ink hover:text-paper"
          >
            ×
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-7 py-6">{children}</div>

        {footer && <div className="border-t border-rule px-7 py-4">{footer}</div>}
        <style>{`@keyframes slide{from{transform:translateX(16px);opacity:0}to{transform:none;opacity:1}}@keyframes fade{from{opacity:0}to{opacity:1}}`}</style>
      </aside>
    </>
  )
}
