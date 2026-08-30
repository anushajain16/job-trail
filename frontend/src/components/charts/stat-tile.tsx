import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

/** A headline number — used where a one-value chart would be noise. */
export function StatTile({
  label,
  value,
  detail,
  color,
  className,
}: {
  label: string
  value: ReactNode
  detail?: ReactNode
  color?: string
  className?: string
}) {
  return (
    <div className={cn('rounded-[2px] border border-rule-soft bg-paper-raised px-4 py-3.5', className)}>
      <p className="type-meta">{label}</p>
      <p
        className="mt-1.5 font-mono text-[22px] leading-none font-bold tracking-[0.02em]"
        style={color ? { color } : undefined}
      >
        {value}
      </p>
      {detail && <p className="type-meta mt-1.5">{detail}</p>}
    </div>
  )
}
