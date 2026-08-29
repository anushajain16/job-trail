import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { googleCalendarApi } from '@/features/google-calendar/api'
import { useDisconnectGoogleCalendarMutation, useGoogleCalendarStatusQuery } from '@/features/google-calendar/hooks'
import { describeApiError } from '@/lib/describe-api-error'

/** Reads the ?calendarConnected= param GET /api/google-calendar/callback
 * redirects back with (see GoogleCalendarController) — the one signal this
 * page gets about how the just-finished connect attempt went, since the
 * actual token exchange happened entirely server-side during that redirect. */
function useCalendarConnectedBanner(): { calendarConnected: boolean | null; clear: () => void } {
  const [searchParams, setSearchParams] = useSearchParams()
  const raw = searchParams.get('calendarConnected')
  return {
    calendarConnected: raw === null ? null : raw === 'true',
    clear: () => {
      searchParams.delete('calendarConnected')
      setSearchParams(searchParams, { replace: true })
    },
  }
}

export function SettingsPage() {
  const { data: status, isPending } = useGoogleCalendarStatusQuery()
  const disconnectMutation = useDisconnectGoogleCalendarMutation()
  const { calendarConnected, clear } = useCalendarConnectedBanner()

  const [isConnecting, setIsConnecting] = useState(false)
  const [connectError, setConnectError] = useState<string | null>(null)

  async function handleConnect() {
    setIsConnecting(true)
    setConnectError(null)
    try {
      const { authorizationUrl } = await googleCalendarApi.connect()
      // A full-page navigation, not a fetch — Google's own consent screen
      // has to render, and it redirects straight back to
      // GOOGLE_CALENDAR_REDIRECT_URI (the backend), not here.
      window.location.href = authorizationUrl
    } catch (err) {
      setConnectError(describeApiError(err))
      setIsConnecting(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="text-muted-foreground">Connections this app uses on your behalf.</p>
      </div>

      {calendarConnected !== null && (
        <div
          role="status"
          className={
            calendarConnected
              ? 'rounded-lg border border-primary/30 bg-primary/5 px-4 py-3 text-sm'
              : 'rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm text-destructive'
          }
        >
          <div className="flex items-center justify-between gap-4">
            <span>
              {calendarConnected
                ? 'Google Calendar connected.'
                : "Couldn't connect Google Calendar — the request may have been denied or expired. Try again."}
            </span>
            <Button size="xs" variant="ghost" onClick={clear}>
              Dismiss
            </Button>
          </div>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Google Calendar</CardTitle>
        </CardHeader>
        <CardContent className="flex items-center justify-between gap-4">
          <div className="flex flex-col gap-1.5">
            <p className="text-sm text-muted-foreground">
              Lets "Add to Calendar" on an interview round create or update an event on your primary Google Calendar.
            </p>
            {!isPending && (
              <Badge variant={status?.connected ? 'secondary' : 'outline'} className="w-fit">
                {status?.connected ? 'Connected' : 'Not connected'}
              </Badge>
            )}
            {disconnectMutation.isError && (
              <p role="alert" className="text-xs text-destructive">
                {describeApiError(disconnectMutation.error)}
              </p>
            )}
            {connectError && (
              <p role="alert" className="text-xs text-destructive">
                {connectError}
              </p>
            )}
          </div>
          {status?.connected ? (
            <Button
              variant="outline"
              onClick={() => disconnectMutation.mutate()}
              disabled={disconnectMutation.isPending}
            >
              {disconnectMutation.isPending ? 'Disconnecting…' : 'Disconnect'}
            </Button>
          ) : (
            <Button onClick={handleConnect} disabled={isConnecting || isPending}>
              {isConnecting ? 'Redirecting…' : 'Connect Google Calendar'}
            </Button>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
