import { authFetch } from '@/lib/api-client'
import type { GoogleCalendarConnectResult, GoogleCalendarConnectionStatus } from '@/features/google-calendar/types'

export const googleCalendarApi = {
  status: () => authFetch<GoogleCalendarConnectionStatus>('/api/google-calendar/connection'),

  // Returns the URL to navigate the whole browser to — Google's own
  // consent screen has to render, which isn't something this fetch call
  // itself can do. See SettingsPage for the actual `window.location`
  // navigation.
  connect: () => authFetch<GoogleCalendarConnectResult>('/api/google-calendar/connect', { method: 'POST' }),

  disconnect: () => authFetch<null>('/api/google-calendar/connection', { method: 'DELETE' }),
}
