import {
  forwardRef,
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'
import { cn } from '@/lib/cn'

/* Shared control chrome: flat, squared, hairline rule that darkens on focus. */
const CONTROL = cn(
  'w-full rounded-[2px] border border-rule-soft bg-paper-raised',
  'px-3 py-2 font-mono text-[11px] tracking-[0.02em] text-ink',
  'transition-colors duration-100 outline-none',
  'focus:border-ink focus-visible:outline-none',
  'disabled:cursor-not-allowed disabled:bg-rule/40 disabled:text-muted',
  'aria-[invalid=true]:border-danger',
)

export interface FieldProps {
  label?: ReactNode
  hint?: ReactNode
  error?: ReactNode
  required?: boolean
  className?: string
  children: (props: { id: string; 'aria-invalid': boolean }) => ReactNode
}

/**
 * Label + control + hint/error, so no form ever re-implements that stack.
 * Takes a render prop so it can wrap any control and still own the id.
 */
export function Field({ label, hint, error, required, className, children }: FieldProps) {
  const id = useId()
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      {label && (
        <label htmlFor={id} className="type-label text-ink">
          {label}
          {required && <span className="ml-1 text-danger">*</span>}
        </label>
      )}
      {children({ id, 'aria-invalid': Boolean(error) })}
      {error ? (
        <p className="font-mono text-[9px] tracking-[0.06em] text-danger">{error}</p>
      ) : hint ? (
        <p className="font-mono text-[9px] tracking-[0.06em] text-muted">{hint}</p>
      ) : null}
    </div>
  )
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function Input({ className, ...props }, ref) {
    return <input ref={ref} className={cn(CONTROL, className)} {...props} />
  },
)

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, rows = 4, ...props }, ref) {
    return (
      <textarea ref={ref} rows={rows} className={cn(CONTROL, 'resize-y leading-relaxed', className)} {...props} />
    )
  },
)

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  options: { value: string; label: string }[]
  placeholder?: string
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { className, options, placeholder, ...props },
  ref,
) {
  return (
    <select ref={ref} className={cn(CONTROL, 'cursor-pointer appearance-none pr-8', className)} {...props}>
      {placeholder && <option value="">{placeholder}</option>}
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  )
})

/** Two-column form row that collapses on narrow viewports. */
export function FieldRow({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('grid gap-4 sm:grid-cols-2', className)}>{children}</div>
}
