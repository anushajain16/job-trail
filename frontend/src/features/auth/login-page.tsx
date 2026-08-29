import { useMutation } from '@tanstack/react-query'
import { type FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthPageShell } from '@/features/auth/auth-page-shell'
import { useAuth } from '@/features/auth/auth-context'
import { describeAuthError } from '@/features/auth/describe-error'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

interface LocationState {
  from?: { pathname: string }
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const loginMutation = useMutation({
    mutationFn: () => login(email, password),
    onSuccess: () => {
      const from = (location.state as LocationState | null)?.from
      navigate(from?.pathname ?? '/', { replace: true })
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    loginMutation.mutate()
  }

  return (
    <AuthPageShell>
      <Card>
        <CardHeader>
          <CardTitle>Log in</CardTitle>
          <CardDescription>Track your applications, one login away.</CardDescription>
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
                autoComplete="current-password"
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </div>
            {loginMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {describeAuthError(loginMutation.error)}
              </p>
            )}
            <Button type="submit" disabled={loginMutation.isPending}>
              {loginMutation.isPending ? 'Logging in…' : 'Log in'}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            No account?{' '}
            <Link to="/signup" className="font-medium text-foreground underline underline-offset-4">
              Sign up
            </Link>
          </p>
        </CardContent>
      </Card>
    </AuthPageShell>
  )
}
