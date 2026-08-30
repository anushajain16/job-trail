import { createContext, use, useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { describeError } from '@/lib/describe-error'

type ToastTone = 'info' | 'success' | 'error'

interface Toast {
  id: number
  message: string
  tone: ToastTone
}

interface ToastContextValue {
  notify: (message: string, tone?: ToastTone) => void
  notifyError: (error: unknown) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

/** Departure-board notices, bottom-left, auto-dismissing. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(0)

  const notify = useCallback((message: string, tone: ToastTone = 'info') => {
    const id = nextId.current++
    setToasts((current) => [...current, { id, message, tone }])
    setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), 4200)
  }, [])

  const notifyError = useCallback((error: unknown) => notify(describeError(error), 'error'), [notify])

  const value = useMemo(() => ({ notify, notifyError }), [notify, notifyError])

  return (
    <ToastContext value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-5 left-5 z-[60] flex flex-col gap-2">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="status"
            className={cn(
              'pointer-events-auto max-w-xs rounded-[3px] px-3.5 py-2.5',
              'font-mono text-[10px] leading-relaxed tracking-[0.05em]',
              toast.tone === 'error' && 'bg-danger text-paper',
              toast.tone === 'success' && 'bg-success text-paper',
              toast.tone === 'info' && 'bg-ink text-paper',
            )}
          >
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext>
  )
}

export function useToast(): ToastContextValue {
  const context = use(ToastContext)
  if (!context) throw new Error('useToast must be used inside <ToastProvider>')
  return context
}
