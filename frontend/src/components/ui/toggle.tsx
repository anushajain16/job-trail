import { cn } from '@/lib/cn'

export interface ToggleProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
  className?: string
}

/** Squared checkbox in signage type — used for map display options. */
export function Toggle({ checked, onChange, label, className }: ToggleProps) {
  return (
    <label className={cn('flex cursor-pointer items-center gap-2 select-none', className)}>
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="sr-only"
      />
      <span
        aria-hidden
        className={cn(
          'flex h-3.5 w-3.5 items-center justify-center rounded-[2px] border-[1.5px] border-ink',
          checked ? 'bg-ink text-paper' : 'bg-transparent',
        )}
      >
        {checked && <span className="font-mono text-[8px] leading-none">×</span>}
      </span>
      <span className="type-meta">{label}</span>
    </label>
  )
}
