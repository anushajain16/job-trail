import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { matchApi, resumeProfileApi } from '@/features/matching/api'
import { patchCachedApplication } from '@/features/applications/hooks'

const resumeProfileKeys = {
  current: ['resume-profile'] as const,
}

export function useResumeProfileQuery() {
  return useQuery({
    queryKey: resumeProfileKeys.current,
    queryFn: resumeProfileApi.get,
    retry: false, // a 404 ("no profile yet") is an expected, common state — don't retry it
  })
}

export function useParseResumeProfileMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: resumeProfileApi.parse,
    onSuccess: (profile) => {
      queryClient.setQueryData(resumeProfileKeys.current, profile)
    },
  })
}

export function useScoreApplicationMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (applicationId: string) => matchApi.score(applicationId),
    // No optimistic step: there's nothing to guess ahead of a match score
    // — patch the cache with the real result once it comes back.
    onSuccess: (result, applicationId) => {
      patchCachedApplication(queryClient, applicationId, (application) => ({
        ...application,
        matchScore: result.matchScore,
        matchedSkills: result.matchedSkills,
        missingSkills: result.missingSkills,
        scoredAt: result.scoredAt,
      }))
    },
  })
}
