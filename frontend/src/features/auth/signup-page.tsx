import { useMutation } from '@tanstack/react-query'
import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthPageShell } from '@/features/auth/auth-page-shell'
import { useAuth } from '@/features/auth/auth-context'
import { describeAuthError } from '@/features/auth/describe-error'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

// Mirrors backend/.../auth/dto/SignupRequest.java's @Size(min = 8, max = 100)
// — client-side just for an immediate hint; the backend still enforces it.
const MIN_PASSWORD_LENGTH = 8

export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const signupMutation = useMutation({
    mutationFn: () => signup(email, password),
    onSuccess: () => navigate('/', { replace: true }),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    signupMutation.mutate()
  }

  return (
    <AuthPageShell>
      <Card>
        <CardHeader>
          <CardTitle>Create an account</CardTitle>
          <CardDescription>Start tracking your job search.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                required
                minLength={MIN_PASSWORD_LENGTH}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <p className="text-xs text-muted-foreground">At least {MIN_PASSWORD_LENGTH} characters.</p>
            </div>
            {signupMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {describeAuthError(signupMutation.error)}
              </p>
            )}
            <Button type="submit" disabled={signupMutation.isPending}>
              {signupMutation.isPending ? 'Creating account…' : 'Sign up'}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-foreground underline underline-offset-4">
              Log in
            </Link>
          </p>
        </CardContent>
      </Card>
    </AuthPageShell>
  )
}
