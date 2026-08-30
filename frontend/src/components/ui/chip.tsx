import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

export interface ChipOption<T extends string> {
  value: T
  label: string
  count?: number
}

export interface ChipGroupProps<T extends string> {
  options: ChipOption<T>[]
  value: T
  onChange: (value: T) => void
  className?: string
}

/**
 * Filter chips — squared, active chip is a solid black fill. Used for the
 * map filters and anywhere else a small exclusive choice appears.
 */
export function ChipGroup<T extends string>({
  options,
  value,
  onChange,
  className,
}: ChipGroupProps<T>) {
  return (
    <div className={cn('flex flex-wrap gap-1.5', className)}>
      {options.map((option) => {
        const active = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(option.value)}
            className={cn(
              'cursor-pointer rounded-[2px] border-[1.5px] border-ink px-2.5 py-1.5',
              'font-mono text-[10px] tracking-[0.08em] uppercase transition-colors duration-100',
              active ? 'bg-ink text-paper' : 'bg-transparent text-ink hover:bg-rule/60',
            )}
          >
            {option.label}
            {option.count != null && (
              <span className={cn('ml-1.5', active ? 'text-paper/60' : 'text-muted')}>
                {option.count}
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}

/** A non-interactive tag, e.g. a matched/missing skill. */
export function Tag({
  children,
  tone = 'neutral',
  className,
}: {
  children: ReactNode
  tone?: 'neutral' | 'positive' | 'negative'
  className?: string
}) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-[2px] border px-1.5 py-0.5',
        'font-mono text-[9px] tracking-[0.06em] uppercase',
        tone === 'positive' && 'border-success/50 text-success',
        tone === 'negative' && 'border-grey text-muted',
        tone === 'neutral' && 'border-rule-soft text-ink-soft',
        className,
      )}
    >
      {children}
    </span>
  )
}
