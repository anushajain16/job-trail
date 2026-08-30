import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import type { Uuid } from '@/api/types'
import { scoreApplication } from './api'

export function useScoreApplication() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: Uuid) => scoreApplication(id),
    onSuccess: (_result, id) => {
      // The score is stored on the application itself, so the detail and
      // list caches both go stale.
      void queryClient.invalidateQueries({ queryKey: queryKeys.applications.detail(id) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.applications.all })
    },
  })
}
