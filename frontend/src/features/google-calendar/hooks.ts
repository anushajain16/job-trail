import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { googleCalendarApi } from '@/features/google-calendar/api'

const googleCalendarKeys = {
  status: ['google-calendar', 'status'] as const,
}

export function useGoogleCalendarStatusQuery() {
  return useQuery({
    queryKey: googleCalendarKeys.status,
    queryFn: googleCalendarApi.status,
  })
}

export function useDisconnectGoogleCalendarMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: googleCalendarApi.disconnect,
    onSuccess: () => {
      queryClient.setQueryData(googleCalendarKeys.status, { connected: false })
    },
  })
}
