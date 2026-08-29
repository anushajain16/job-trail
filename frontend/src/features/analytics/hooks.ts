import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '@/features/analytics/api'

const analyticsKeys = {
  funnel: ['analytics', 'funnel'] as const,
  conversion: ['analytics', 'conversion'] as const,
  timeInStage: ['analytics', 'time-in-stage'] as const,
}

export function useFunnelQuery() {
  return useQuery({ queryKey: analyticsKeys.funnel, queryFn: analyticsApi.funnel })
}

export function useConversionQuery() {
  return useQuery({ queryKey: analyticsKeys.conversion, queryFn: analyticsApi.conversion })
}

export function useTimeInStageQuery() {
  return useQuery({ queryKey: analyticsKeys.timeInStage, queryFn: analyticsApi.timeInStage })
}
