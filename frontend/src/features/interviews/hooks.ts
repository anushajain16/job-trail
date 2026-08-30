import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import type { InterviewRoundCreateRequest, InterviewRoundUpdateRequest, Uuid } from '@/api/types'
import * as interviewsApi from './api'

export function useInterviewRounds(applicationId: Uuid | null) {
  return useQuery({
    queryKey: queryKeys.interviews.byApplication(applicationId ?? ''),
    queryFn: () => interviewsApi.listInterviewRounds(applicationId!),
    enabled: Boolean(applicationId),
  })
}

/** Rounds are always read through their application's list. */
function useRoundsInvalidator(applicationId: Uuid) {
  const queryClient = useQueryClient()
  return () =>
    queryClient.invalidateQueries({ queryKey: queryKeys.interviews.byApplication(applicationId) })
}

export function useCreateInterviewRound(applicationId: Uuid) {
  const invalidate = useRoundsInvalidator(applicationId)
  return useMutation({
    mutationFn: (body: InterviewRoundCreateRequest) =>
      interviewsApi.createInterviewRound(applicationId, body),
    onSuccess: () => void invalidate(),
  })
}

export function useUpdateInterviewRound(applicationId: Uuid) {
  const invalidate = useRoundsInvalidator(applicationId)
  return useMutation({
    mutationFn: ({ id, body }: { id: Uuid; body: InterviewRoundUpdateRequest }) =>
      interviewsApi.updateInterviewRound(id, body),
    onSuccess: () => void invalidate(),
  })
}

export function useDeleteInterviewRound(applicationId: Uuid) {
  const invalidate = useRoundsInvalidator(applicationId)
  return useMutation({
    mutationFn: (id: Uuid) => interviewsApi.deleteInterviewRound(id),
    onSuccess: () => void invalidate(),
  })
}

export function useSyncInterviewToCalendar(applicationId: Uuid) {
  const invalidate = useRoundsInvalidator(applicationId)
  return useMutation({
    mutationFn: (id: Uuid) => interviewsApi.syncInterviewToCalendar(id),
    onSuccess: () => void invalidate(),
  })
}
