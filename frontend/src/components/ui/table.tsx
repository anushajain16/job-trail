import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

/**
 * Timetable-style table primitives: ruled rows, letterspaced uppercase
 * headers, no zebra striping.
 */
export function Table({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className="w-full overflow-x-auto">
      <table className={cn('w-full border-collapse text-left', className)}>{children}</table>
    </div>
  )
}

export function THead({ children }: { children: ReactNode }) {
  return <thead className="border-b-2 border-ink">{children}</thead>
}

export function TBody({ children }: { children: ReactNode }) {
  return <tbody>{children}</tbody>
}

export function TR({
  children,
  onClick,
  className,
}: {
  children: ReactNode
  onClick?: () => void
  className?: string
}) {
  return (
    <tr
      onClick={onClick}
      className={cn(
        'border-b border-rule last:border-b-0',
        onClick && 'cursor-pointer hover:bg-rule/40',
        className,
      )}
    >
      {children}
    </tr>
  )
}

export function TH({
  children,
  className,
  align = 'left',
}: {
  children?: ReactNode
  className?: string
  align?: 'left' | 'right'
}) {
  return (
    <th
      scope="col"
      className={cn(
        'px-3 py-2.5 font-mono text-[9px] font-bold tracking-[0.12em] whitespace-nowrap uppercase text-ink',
        align === 'right' && 'text-right',
        className,
      )}
    >
      {children}
    </th>
  )
}

export function TD({
  children,
  className,
  align = 'left',
}: {
  children?: ReactNode
  className?: string
  align?: 'left' | 'right'
}) {
  return (
    <td
      className={cn(
        'px-3 py-3 font-mono text-[10px] tracking-[0.04em] text-ink-soft',
        align === 'right' && 'text-right',
        className,
      )}
    >
      {children}
    </td>
  )
}
