import { api } from '@/api/client'
import type { CalendarConnectResponse, CalendarConnectionResponse } from '@/api/types'

/** Returns Google's consent URL — the browser navigates there full-page. */
export function startCalendarConnect() {
  return api.post<CalendarConnectResponse>('/api/google-calendar/connect')
}

export function getCalendarConnection() {
  return api.get<CalendarConnectionResponse>('/api/google-calendar/connection')
}

export function disconnectCalendar() {
  return api.delete('/api/google-calendar/connection')
}
