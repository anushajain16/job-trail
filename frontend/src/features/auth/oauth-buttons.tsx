import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { useScript } from '@/lib/use-script'
import { useAuth } from './auth-context'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
const GITHUB_CLIENT_ID = import.meta.env.VITE_GITHUB_CLIENT_ID
const GITHUB_STATE_KEY = 'jobtrail.githubOAuthState'

/** Whether any provider is configured — hides the whole block when not. */
export const oauthEnabled = Boolean(GOOGLE_CLIENT_ID || GITHUB_CLIENT_ID)

interface GoogleCredentialResponse {
  credential: string
}

interface GoogleIdentity {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string
        callback: (response: GoogleCredentialResponse) => void
      }) => void
      renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
    }
  }
}

declare global {
  interface Window {
    google?: GoogleIdentity
  }
}

/**
 * Google sign-in.
 *
 * The backend verifies a Google Identity Services **ID token**, which only
 * GIS's own button/One-Tap can mint — so Google's real button has to be the
 * thing that gets clicked. We render it transparently on top of our own
 * signage-styled button so the click target is Google's while the visible
 * chrome stays in the app's design language.
 */
export function GoogleSignInButton({ onError }: { onError: (error: unknown) => void }) {
  const { loginWithOAuth } = useAuth()
  const status = useScript(GOOGLE_CLIENT_ID ? 'https://accounts.google.com/gsi/client' : null)
  const overlayRef = useRef<HTMLDivElement>(null)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (status !== 'ready' || !GOOGLE_CLIENT_ID) return
    const google = window.google
    const overlay = overlayRef.current
    const wrapper = wrapperRef.current
    if (!google || !overlay || !wrapper) return

    google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: (response) => {
        setBusy(true)
        loginWithOAuth('GOOGLE', response.credential)
          .catch(onError)
          .finally(() => setBusy(false))
      },
    })
    overlay.replaceChildren()
    google.accounts.id.renderButton(overlay, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      width: Math.round(wrapper.getBoundingClientRect().width),
    })
  }, [status, loginWithOAuth, onError])

  if (!GOOGLE_CLIENT_ID) return null

  return (
    <div ref={wrapperRef} className="relative">
      <Button fullWidth type="button" loading={busy} disabled={status !== 'ready'}>
        Continue with Google
      </Button>
      {/* Google's own button, invisible but on top — it owns the click. */}
      <div
        ref={overlayRef}
        aria-hidden
        className="absolute inset-0 overflow-hidden opacity-0 [color-scheme:light]"
      />
    </div>
  )
}

/**
 * GitHub sign-in. GitHub's flow is a full-page redirect; the backend needs
 * the resulting `code` (it does the secret-bearing exchange itself), so we
 * only bounce the browser out and let `/auth/callback/github` finish.
 */
export function GitHubSignInButton() {
  if (!GITHUB_CLIENT_ID) return null

  const start = () => {
    const state = crypto.randomUUID()
    sessionStorage.setItem(GITHUB_STATE_KEY, state)
    const params = new URLSearchParams({
      client_id: GITHUB_CLIENT_ID,
      redirect_uri: `${window.location.origin}/auth/callback/github`,
      scope: 'read:user user:email',
      state,
    })
    window.location.assign(`https://github.com/login/oauth/authorize?${params}`)
  }

  return (
    <Button fullWidth type="button" onClick={start}>
      Continue with GitHub
    </Button>
  )
}

/** Reads back the state this browser stored before the redirect. */
export function consumeGitHubState(): string | null {
  const state = sessionStorage.getItem(GITHUB_STATE_KEY)
  sessionStorage.removeItem(GITHUB_STATE_KEY)
  return state
}

/** Both provider buttons plus the `OR` divider, when any are configured. */
export function OAuthProviders({ onError }: { onError: (error: unknown) => void }) {
  if (!oauthEnabled) return null

  return (
    <div className="mt-6">
      <div className="mb-5 flex items-center gap-3">
        <span className="h-px flex-1 bg-rule" />
        <span className="type-meta">OR</span>
        <span className="h-px flex-1 bg-rule" />
      </div>
      <div className="flex flex-col gap-2.5">
        <GoogleSignInButton onError={onError} />
        <GitHubSignInButton />
      </div>
    </div>
  )
}
