import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import * as calendarApi from './api'

export function useCalendarConnection() {
  return useQuery({
    queryKey: queryKeys.googleCalendar.connection,
    queryFn: calendarApi.getCalendarConnection,
  })
}

export function useConnectCalendar() {
  return useMutation({ mutationFn: calendarApi.startCalendarConnect })
}

export function useDisconnectCalendar() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: calendarApi.disconnectCalendar,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.googleCalendar.connection }),
  })
}
