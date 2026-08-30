import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { resolvedLineColor, stageColor, stageLabel } from '@/lib/design'
import type { Stage } from '@/api/types'

/** Small filled circle in a line's colour — the line's identity mark. */
export function LineDot({ color, size = 8, className }: { color: string; size?: number; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn('inline-block shrink-0 rounded-full', className)}
      style={{ width: size, height: size, background: color }}
    />
  )
}

/** Outlined signage badge, coloured by whatever it labels. */
export function Badge({
  children,
  color,
  className,
}: {
  children: ReactNode
  color?: string
  className?: string
}) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-[2px] border-[1.5px] px-2 py-1',
        'font-mono text-[9px] font-semibold tracking-[0.1em] uppercase',
        className,
      )}
      style={color ? { borderColor: color, color } : undefined}
    >
      {children}
    </span>
  )
}

/** `IN TRANSIT — SCREEN` / `OFFER — TERMINUS` / `SUSPENDED — GHOSTED`. */
export function StageBadge({
  stage,
  applicationId,
  className,
}: {
  stage: Stage
  applicationId: string
  className?: string
}) {
  return (
    <Badge color={stageColor(applicationId, stage)} className={className}>
      {stageLabel(stage)}
    </Badge>
  )
}

/** Company + role, prefixed by the line dot — the line's name plate. */
export function LineName({
  id,
  stage,
  company,
  role,
  align = 'left',
  className,
}: {
  id: string
  stage: Stage
  company: string
  role: string
  align?: 'left' | 'right'
  className?: string
}) {
  const color = resolvedLineColor(id, stage)
  return (
    <div className={cn(align === 'right' && 'text-right', className)}>
      <div
        className={cn(
          'flex items-center gap-2',
          align === 'right' ? 'flex-row-reverse justify-start' : 'justify-start',
        )}
      >
        <LineDot color={color} />
        <span className="font-mono text-[11px] font-semibold tracking-[0.05em] uppercase">
          {company}
        </span>
      </div>
      <div className="mt-1 font-mono text-[9px] tracking-[0.06em] text-muted">{role}</div>
    </div>
  )
}
