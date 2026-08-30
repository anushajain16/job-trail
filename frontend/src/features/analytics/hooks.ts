import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import { analyticsApi } from './api'

export function useFunnel() {
  return useQuery({ queryKey: queryKeys.analytics.funnel, queryFn: analyticsApi.funnel })
}

export function useConversion() {
  return useQuery({ queryKey: queryKeys.analytics.conversion, queryFn: analyticsApi.conversion })
}

export function useTimeInStage() {
  return useQuery({ queryKey: queryKeys.analytics.timeInStage, queryFn: analyticsApi.timeInStage })
}

export function useResumePerformance() {
  return useQuery({
    queryKey: queryKeys.analytics.resumePerformance,
    queryFn: analyticsApi.resumePerformance,
  })
}
