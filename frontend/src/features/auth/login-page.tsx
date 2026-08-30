import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { FormError } from '@/components/ui/feedback'
import { AuthPageShell } from './auth-page-shell'
import { useAuth } from './auth-context'
import { CredentialsForm } from './credentials-form'
import { OAuthProviders } from './oauth-buttons'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [oauthError, setOAuthError] = useState<unknown>(null)

  const destination = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname

  return (
    <AuthPageShell
      title="Board your lines"
      subtitle="SIGN IN TO YOUR NETWORK"
      footer={
        <p className="font-mono text-[10px] tracking-[0.06em] text-muted">
          No account yet? <Link to="/signup">Open a new network</Link>
        </p>
      }
    >
      <CredentialsForm
        submitLabel="Sign in"
        onSubmit={async (credentials) => {
          await login(credentials)
          navigate(destination ?? '/map', { replace: true })
        }}
      />
      <FormError error={oauthError} className="mt-4" />
      <OAuthProviders onError={setOAuthError} />
    </AuthPageShell>
  )
}
