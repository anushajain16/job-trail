import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

export type ButtonVariant = 'solid' | 'outline' | 'ghost' | 'danger'
export type ButtonSize = 'sm' | 'md' | 'lg'

const VARIANTS: Record<ButtonVariant, string> = {
  solid: 'bg-ink text-paper border-ink hover:bg-ink-soft',
  outline: 'bg-transparent text-ink border-ink hover:bg-ink hover:text-paper',
  ghost: 'bg-transparent text-muted border-transparent hover:text-ink hover:border-rule',
  danger: 'bg-transparent text-danger border-danger hover:bg-danger hover:text-paper',
}

const SIZES: Record<ButtonSize, string> = {
  sm: 'text-[9px] px-2.5 py-1.5 gap-1.5',
  md: 'text-[10px] px-3.5 py-2 gap-2',
  lg: 'text-[11px] px-5 py-2.5 gap-2',
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  /** Renders a stalled-line bar instead of the label while true. */
  loading?: boolean
  fullWidth?: boolean
}

/**
 * The one button in the system. Squared (2px radius), 1.5px rule border,
 * uppercase signage label — never a shadow or a gradient.
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { className, variant = 'outline', size = 'md', loading, fullWidth, disabled, children, ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center rounded-[2px] border-[1.5px]',
        'font-mono font-medium uppercase tracking-[0.1em] whitespace-nowrap',
        'cursor-pointer transition-colors duration-100',
        'disabled:cursor-not-allowed disabled:opacity-40',
        VARIANTS[variant],
        SIZES[size],
        fullWidth && 'w-full',
        className,
      )}
      {...props}
    >
      {loading ? <span className="tracking-[0.3em]">···</span> : children}
    </button>
  )
})
