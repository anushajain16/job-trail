import { useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ErrorState, TrackLoader } from '@/components/ui/feedback'
import { Panel, SectionHeading } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { LINE_COLORS, MUTED } from '@/lib/design'
import { useCalendarConnection, useConnectCalendar, useDisconnectCalendar } from './hooks'

/**
 * Connect / disconnect Google Calendar.
 *
 * Google redirects the browser back to this page with
 * `?calendarConnected=true|false` (the backend's
 * GOOGLE_CALENDAR_FRONTEND_REDIRECT_URI), so the outcome is read off the
 * URL on mount and then cleared.
 */
export function CalendarConnectionPanel() {
  const { data, isLoading, isError, error, refetch } = useCalendarConnection()
  const connect = useConnectCalendar()
  const disconnect = useDisconnectCalendar()
  const { notify, notifyError } = useToast()
  const [searchParams, setSearchParams] = useSearchParams()

  const outcome = searchParams.get('calendarConnected')

  useEffect(() => {
    if (!outcome) return
    if (outcome === 'true') {
      notify('Google Calendar connected.', 'success')
      void refetch()
    } else {
      notify('Google Calendar connection failed or was declined.', 'error')
    }
    const next = new URLSearchParams(searchParams)
    next.delete('calendarConnected')
    setSearchParams(next, { replace: true })
  }, [outcome, notify, refetch, searchParams, setSearchParams])

  const startConnect = async () => {
    try {
      const { authorizationUrl } = await connect.mutateAsync()
      window.location.assign(authorizationUrl)
    } catch (caught) {
      notifyError(caught)
    }
  }

  const revoke = async () => {
    try {
      await disconnect.mutateAsync()
      notify('Google Calendar disconnected.', 'success')
    } catch (caught) {
      notifyError(caught)
    }
  }

  const connected = data?.connected ?? false

  return (
    <section>
      <SectionHeading weight="heavy">Google Calendar</SectionHeading>
      <div className="mt-4">
        {isLoading && <TrackLoader label="CHECKING CONNECTION" />}
        {isError && <ErrorState error={error} onRetry={() => void refetch()} />}

        {data && (
          <Panel className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <Badge color={connected ? LINE_COLORS[1] : MUTED}>
                {connected ? 'CONNECTED' : 'NOT CONNECTED'}
              </Badge>
              <p className="mt-3 max-w-md font-mono text-[10px] leading-relaxed tracking-[0.04em] text-ink-soft">
                {connected
                  ? 'Interview rounds with a scheduled time can be pushed to your calendar. Re-syncing a round updates the same event rather than creating a new one.'
                  : 'Connect to add interview rounds to your Google Calendar. Only calendar.events access is requested.'}
              </p>
            </div>
            {connected ? (
              <Button variant="danger" size="sm" loading={disconnect.isPending} onClick={revoke}>
                Disconnect
              </Button>
            ) : (
              <Button variant="solid" size="sm" loading={connect.isPending} onClick={startConnect}>
                Connect
              </Button>
            )}
          </Panel>
        )}
      </div>
    </section>
  )
}
