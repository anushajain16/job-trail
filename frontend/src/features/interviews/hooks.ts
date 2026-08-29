import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { interviewRoundsApi } from '@/features/interviews/api'
import { interviewRoundKeys } from '@/features/interviews/query-keys'
import type { InterviewRoundInput } from '@/features/interviews/types'

export function useInterviewRoundsQuery(applicationId: string | undefined) {
  return useQuery({
    queryKey: interviewRoundKeys.list(applicationId ?? ''),
    queryFn: () => interviewRoundsApi.list(applicationId!),
    enabled: applicationId !== undefined,
  })
}

// No optimistic step for any of these three — rounds are a low-frequency,
// form-driven list (not something the UI needs to feel instant for), so a
// plain invalidate-on-success keeps this simple.

export function useCreateInterviewRoundMutation(applicationId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: InterviewRoundInput) => interviewRoundsApi.create(applicationId, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: interviewRoundKeys.list(applicationId) })
    },
  })
}

export function useUpdateInterviewRoundMutation(applicationId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: InterviewRoundInput }) => interviewRoundsApi.update(id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: interviewRoundKeys.list(applicationId) })
    },
  })
}

export function useDeleteInterviewRoundMutation(applicationId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => interviewRoundsApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: interviewRoundKeys.list(applicationId) })
    },
  })
}
