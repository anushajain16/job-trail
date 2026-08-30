import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { useDismissable } from '@/lib/use-dismissable'
import { Button } from './button'

export interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  description?: ReactNode
  children?: ReactNode
  footer?: ReactNode
  width?: 'sm' | 'md' | 'lg'
}

const WIDTHS = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-3xl' } as const

/** Centred modal. Same flat chrome as the drawer, no shadow. */
export function Dialog({ open, onClose, title, description, children, footer, width = 'md' }: DialogProps) {
  useDismissable(open, onClose)
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-ink/12 p-4 sm:p-10">
      <div aria-hidden onClick={onClose} className="fixed inset-0" />
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          'relative z-10 w-full rounded-[2px] border-2 border-ink bg-paper',
          WIDTHS[width],
        )}
      >
        <div className="flex items-start justify-between gap-4 border-b border-rule px-6 py-4">
          <div>
            <h2 className="type-label">{title}</h2>
            {description && (
              <p className="mt-2 font-mono text-[10px] leading-relaxed tracking-[0.04em] text-ink-soft">
                {description}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="h-6 w-6 shrink-0 cursor-pointer rounded-[2px] border-[1.5px] border-ink font-mono text-[11px] leading-none hover:bg-ink hover:text-paper"
          >
            ×
          </button>
        </div>
        {children && <div className="px-6 py-5">{children}</div>}
        {footer && <div className="flex justify-end gap-2 border-t border-rule px-6 py-4">{footer}</div>}
      </div>
    </div>
  )
}

export interface ConfirmDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  description?: ReactNode
  confirmLabel?: string
  destructive?: boolean
  loading?: boolean
}

/** Destructive-action confirmation, so no page hand-rolls its own. */
export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = 'Confirm',
  destructive = true,
  loading,
}: ConfirmDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      width="sm"
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant={destructive ? 'danger' : 'solid'}
            size="sm"
            loading={loading}
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </>
      }
    />
  )
}
