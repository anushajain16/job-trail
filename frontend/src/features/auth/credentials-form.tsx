import { useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Field, Input } from '@/components/ui/field'
import { FormError } from '@/components/ui/feedback'

export interface CredentialsFormProps {
  submitLabel: string
  /** Signup enforces the backend's 8–100 char password rule up front. */
  minPasswordLength?: number
  onSubmit: (credentials: { email: string; password: string }) => Promise<void>
}

/** Email + password, shared by login and signup so validation lives once. */
export function CredentialsForm({ submitLabel, minPasswordLength, onSubmit }: CredentialsFormProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({})
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  const validate = () => {
    const next: { email?: string; password?: string } = {}
    if (!/^\S+@\S+\.\S+$/.test(email)) next.email = 'Enter a valid email address.'
    if (minPasswordLength && password.length < minPasswordLength) {
      next.password = `Use at least ${minPasswordLength} characters.`
    } else if (!password) {
      next.password = 'Enter your password.'
    }
    setFieldErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      await onSubmit({ email: email.trim(), password })
    } catch (caught) {
      setError(caught)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
      <Field label="Email" error={fieldErrors.email} required>
        {(props) => (
          <Input
            {...props}
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        )}
      </Field>

      <Field
        label="Password"
        error={fieldErrors.password}
        hint={minPasswordLength ? `${minPasswordLength}–100 characters` : undefined}
        required
      >
        {(props) => (
          <Input
            {...props}
            type="password"
            autoComplete={minPasswordLength ? 'new-password' : 'current-password'}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        )}
      </Field>

      <FormError error={error} />

      <Button type="submit" variant="solid" size="lg" fullWidth loading={submitting}>
        {submitLabel}
      </Button>
    </form>
  )
}
