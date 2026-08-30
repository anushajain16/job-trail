import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FormError } from '@/components/ui/feedback'
import { AuthPageShell } from './auth-page-shell'
import { useAuth } from './auth-context'
import { CredentialsForm } from './credentials-form'
import { OAuthProviders } from './oauth-buttons'

export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()
  const [oauthError, setOAuthError] = useState<unknown>(null)

  return (
    <AuthPageShell
      title="Open a new network"
      subtitle="CREATE YOUR JOBTRAIL ACCOUNT"
      footer={
        <p className="font-mono text-[10px] tracking-[0.06em] text-muted">
          Already running lines? <Link to="/login">Sign in</Link>
        </p>
      }
    >
      <CredentialsForm
        submitLabel="Create account"
        minPasswordLength={8}
        onSubmit={async (credentials) => {
          await signup(credentials)
          navigate('/map', { replace: true })
        }}
      />
      <FormError error={oauthError} className="mt-4" />
      <OAuthProviders onError={setOAuthError} />
    </AuthPageShell>
  )
}
