import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { describeError } from '@/lib/describe-error'
import { Button } from './button'

/** Indeterminate progress drawn as a track with a running train pill. */
export function TrackLoader({ label = 'LOADING', className }: { label?: string; className?: string }) {
  return (
    <div className={cn('flex flex-col items-center gap-3 py-14', className)}>
      <div className="relative h-[7px] w-40 overflow-hidden rounded-full bg-ink/13">
        <span className="absolute top-0 h-[7px] w-10 animate-[transit_1.4s_ease-in-out_infinite] rounded-full bg-ink" />
      </div>
      <span className="type-meta">{label}</span>
      <style>{`@keyframes transit{0%{left:-2.5rem}100%{left:100%}}`}</style>
    </div>
  )
}

export function EmptyState({
  title,
  description,
  action,
  className,
}: {
  title: string
  description?: ReactNode
  action?: ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center gap-3 rounded-[2px] border border-dashed border-rule-soft px-6 py-14 text-center',
        className,
      )}
    >
      <p className="type-label">{title}</p>
      {description && (
        <p className="max-w-md font-mono text-[10px] leading-relaxed tracking-[0.04em] text-muted">
          {description}
        </p>
      )}
      {action}
    </div>
  )
}

export function ErrorState({
  error,
  onRetry,
  className,
}: {
  error: unknown
  onRetry?: () => void
  className?: string
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center gap-3 rounded-[2px] border-[1.5px] border-danger px-6 py-12 text-center',
        className,
      )}
    >
      <p className="type-label text-danger">SERVICE DISRUPTION</p>
      <p className="max-w-md font-mono text-[10px] leading-relaxed tracking-[0.04em] text-ink-soft">
        {describeError(error)}
      </p>
      {onRetry && (
        <Button size="sm" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  )
}

/** Inline one-line error, for forms. */
export function FormError({ error, className }: { error: unknown; className?: string }) {
  if (!error) return null
  return (
    <p
      role="alert"
      className={cn(
        'rounded-[2px] border-[1.5px] border-danger px-3 py-2',
        'font-mono text-[10px] leading-relaxed tracking-[0.04em] text-danger',
        className,
      )}
    >
      {describeError(error)}
    </p>
  )
}
