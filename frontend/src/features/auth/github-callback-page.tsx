import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { ErrorState, TrackLoader } from '@/components/ui/feedback'
import { useAuth } from './auth-context'
import { consumeGitHubState } from './oauth-buttons'

/**
 * Where GitHub drops the browser after consent. Exchanges the `code` for a
 * JobTrail session via the backend, then continues onto the map.
 */
export function GitHubCallbackPage() {
  const [searchParams] = useSearchParams()
  const { loginWithOAuth } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<unknown>(null)
  const started = useRef(false)

  useEffect(() => {
    // StrictMode double-invokes effects; the code is single-use, so guard it.
    if (started.current) return
    started.current = true

    const code = searchParams.get('code')
    const returnedState = searchParams.get('state')
    const expectedState = consumeGitHubState()

    if (searchParams.get('error')) {
      setError(new Error(searchParams.get('error_description') ?? 'GitHub sign-in was cancelled.'))
      return
    }
    if (!code) {
      setError(new Error('GitHub did not return an authorization code.'))
      return
    }
    if (!expectedState || returnedState !== expectedState) {
      setError(new Error('Sign-in state did not match. Start again from the sign-in page.'))
      return
    }

    loginWithOAuth('GITHUB', code)
      .then(() => navigate('/map', { replace: true }))
      .catch(setError)
  }, [searchParams, loginWithOAuth, navigate])

  if (error) {
    return (
      <div className="mx-auto max-w-md px-8 py-24">
        <ErrorState error={error} />
        <div className="mt-5 flex justify-center">
          <Button size="sm" onClick={() => navigate('/login', { replace: true })}>
            Back to sign in
          </Button>
        </div>
      </div>
    )
  }

  return <TrackLoader label="COMPLETING GITHUB SIGN-IN" />
}
