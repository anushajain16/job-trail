import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

/** The 2px black rule that sits above every legend/section footer. */
export function Rule({ className, weight = 'heavy' }: { className?: string; weight?: 'heavy' | 'hair' }) {
  return (
    <hr
      className={cn(
        'my-0 border-0',
        weight === 'heavy' ? 'border-t-2 border-ink' : 'border-t border-rule',
        className,
      )}
    />
  )
}

export interface SectionHeadingProps {
  children: ReactNode
  /** Right-aligned slot for actions or a count. */
  aside?: ReactNode
  /** Heavy rule above (a major break) vs. hairline (a minor one). */
  weight?: 'heavy' | 'hair'
  className?: string
}

/** `STATUS TIMELINE` / `NOTES` — a ruled, letterspaced section label. */
export function SectionHeading({ children, aside, weight = 'hair', className }: SectionHeadingProps) {
  return (
    <div
      className={cn(
        'flex items-baseline justify-between gap-4 pt-3',
        weight === 'heavy' ? 'border-t-2 border-ink' : 'border-t border-rule',
        className,
      )}
    >
      <h2 className="type-label text-ink">{children}</h2>
      {aside && <div className="type-meta shrink-0">{aside}</div>}
    </div>
  )
}

/** A bordered card. Flat — hairline rule, 2px radius, no elevation. */
export function Panel({
  children,
  className,
  padded = true,
}: {
  children: ReactNode
  className?: string
  padded?: boolean
}) {
  return (
    <div
      className={cn(
        'rounded-[2px] border border-rule-soft bg-paper-raised',
        padded && 'p-5',
        className,
      )}
    >
      {children}
    </div>
  )
}

export interface PageHeaderProps {
  title: string
  meta?: ReactNode
  actions?: ReactNode
  className?: string
}

/** The `APPLICATION TRANSIT MAP` header block, reused by every page. */
export function PageHeader({ title, meta, actions, className }: PageHeaderProps) {
  return (
    <header className={cn('mb-9 flex flex-wrap items-end justify-between gap-4', className)}>
      <div>
        <h1 className="type-title uppercase">{title}</h1>
        {meta && <div className="type-meta mt-1.5">{meta}</div>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-1.5">{actions}</div>}
    </header>
  )
}

/** A `LABEL  value` definition pair, stacked. */
export function DataPoint({
  label,
  children,
  className,
}: {
  label: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <div className={cn('flex flex-col gap-1', className)}>
      <span className="type-meta">{label}</span>
      <span className="font-mono text-[11px] tracking-[0.02em] text-ink">{children}</span>
    </div>
  )
}
