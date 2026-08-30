import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/api/client'
import { queryKeys } from '@/api/query-keys'
import * as resumeProfileApi from './api'

/**
 * The stored profile. A 404 means "never parsed", which is a normal state
 * rather than an error, so it resolves to `null` instead of throwing.
 */
export function useResumeProfile() {
  return useQuery({
    queryKey: queryKeys.resumeProfile,
    queryFn: async () => {
      try {
        return await resumeProfileApi.getResumeProfile()
      } catch (error) {
        if (error instanceof ApiError && error.isNotFound) return null
        throw error
      }
    },
  })
}

export function useParseResumeProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: resumeProfileApi.parseResumeProfile,
    onSuccess: (profile) => {
      queryClient.setQueryData(queryKeys.resumeProfile, profile)
      // Re-parsing invalidates every cached match score server-side, so the
      // applications holding those scores must be refetched too.
      void queryClient.invalidateQueries({ queryKey: queryKeys.applications.all })
    },
  })
}
