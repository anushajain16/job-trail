import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { applicationsApi } from '@/features/applications/api'
import { applicationKeys } from '@/features/applications/query-keys'
import type { Application, ApplicationInput, ApplicationListParams, Page } from '@/features/applications/types'

export function useApplicationsQuery(params: ApplicationListParams) {
  return useQuery({
    queryKey: applicationKeys.list(params),
    queryFn: () => applicationsApi.list(params),
    placeholderData: (previousData) => previousData, // keep the current page's rows on screen while the next page loads
  })
}

export function useApplicationQuery(id: string | undefined) {
  return useQuery({
    queryKey: applicationKeys.detail(id ?? ''),
    queryFn: () => applicationsApi.get(id!),
    enabled: id !== undefined,
  })
}

/** Patches `id` wherever it appears across every cached list page, plus its
 * own detail query if cached. Shared by the update and delete mutations'
 * optimistic-update step, and by features/matching's score mutation (no
 * optimistic step there — it patches in onSuccess with the real result,
 * there's nothing to guess ahead of a match score coming back). */
export function patchCachedApplication(
  queryClient: QueryClient,
  id: string,
  patch: (application: Application) => Application | null,
) {
  queryClient.setQueriesData<Page<Application>>({ queryKey: applicationKeys.lists() }, (page) => {
    if (!page) return page
    const nextContent: Application[] = []
    let removed = false
    for (const application of page.content) {
      if (application.id !== id) {
        nextContent.push(application)
        continue
      }
      const patched = patch(application)
      if (patched) nextContent.push(patched)
      else removed = true
    }
    if (!removed && nextContent.length === page.content.length) return page
    return { ...page, content: nextContent, totalElements: page.totalElements - (removed ? 1 : 0) }
  })

  queryClient.setQueryData<Application>(applicationKeys.detail(id), (existing) =>
    existing ? (patch(existing) ?? existing) : existing,
  )
}

const TEMP_ID_PREFIX = 'optimistic-'

export function useCreateApplicationMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: ApplicationInput) => applicationsApi.create(input),

    onMutate: async (input) => {
      await queryClient.cancelQueries({ queryKey: applicationKeys.lists() })
      const previousLists = queryClient.getQueriesData<Page<Application>>({ queryKey: applicationKeys.lists() })

      const now = new Date().toISOString()
      const optimisticApplication: Application = {
        id: `${TEMP_ID_PREFIX}${crypto.randomUUID()}`,
        company: input.company.trim(),
        role: input.role.trim(),
        location: input.location.trim() || null,
        salaryMin: input.salaryMin.trim() ? Number(input.salaryMin) : null,
        salaryMax: input.salaryMax.trim() ? Number(input.salaryMax) : null,
        link: input.link.trim() || null,
        source: input.source.trim() || null,
        notes: input.notes.trim() || null,
        jobDescriptionText: input.jobDescriptionText.trim() || null,
        deadline: input.deadline.trim() || null,
        currentStage: 'SAVED',
        resumeVersionId: null,
        coverLetterVersionId: null,
        matchScore: null,
        matchedSkills: [],
        missingSkills: [],
        scoredAt: null,
        createdAt: now,
        updatedAt: now,
      }

      // The list is sorted newest-first (backend default), so a new row
      // only ever visibly belongs at the top of page 0 — leave every other
      // cached page alone rather than guess where it'd land there.
      queryClient.setQueriesData<Page<Application>>(
        { queryKey: applicationKeys.lists(), predicate: (query) => (query.queryKey[2] as ApplicationListParams)?.page === 0 },
        (page) =>
          page && {
            ...page,
            content: [optimisticApplication, ...page.content].slice(0, page.size),
            totalElements: page.totalElements + 1,
          },
      )

      return { previousLists }
    },

    onError: (_error, _input, context) => {
      for (const [queryKey, data] of context?.previousLists ?? []) {
        queryClient.setQueryData(queryKey, data)
      }
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: applicationKeys.lists() })
    },
  })
}

export function useUpdateApplicationMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ApplicationInput }) => applicationsApi.update(id, input),

    onMutate: async ({ id, input }) => {
      await queryClient.cancelQueries({ queryKey: applicationKeys.lists() })
      await queryClient.cancelQueries({ queryKey: applicationKeys.detail(id) })
      const previousLists = queryClient.getQueriesData<Page<Application>>({ queryKey: applicationKeys.lists() })
      const previousDetail = queryClient.getQueryData<Application>(applicationKeys.detail(id))

      patchCachedApplication(queryClient, id, (application) => ({
        ...application,
        company: input.company.trim(),
        role: input.role.trim(),
        location: input.location.trim() || null,
        salaryMin: input.salaryMin.trim() ? Number(input.salaryMin) : null,
        salaryMax: input.salaryMax.trim() ? Number(input.salaryMax) : null,
        link: input.link.trim() || null,
        source: input.source.trim() || null,
        notes: input.notes.trim() || null,
        jobDescriptionText: input.jobDescriptionText.trim() || null,
        deadline: input.deadline.trim() || null,
        updatedAt: new Date().toISOString(),
      }))

      return { previousLists, previousDetail }
    },

    onError: (_error, { id }, context) => {
      for (const [queryKey, data] of context?.previousLists ?? []) {
        queryClient.setQueryData(queryKey, data)
      }
      if (context?.previousDetail) {
        queryClient.setQueryData(applicationKeys.detail(id), context.previousDetail)
      }
    },

    onSettled: (_data, _error, { id }) => {
      queryClient.invalidateQueries({ queryKey: applicationKeys.lists() })
      queryClient.invalidateQueries({ queryKey: applicationKeys.detail(id) })
    },
  })
}

export function useDeleteApplicationMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => applicationsApi.remove(id),

    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: applicationKeys.lists() })
      const previousLists = queryClient.getQueriesData<Page<Application>>({ queryKey: applicationKeys.lists() })

      patchCachedApplication(queryClient, id, () => null)

      return { previousLists }
    },

    onError: (_error, _id, context) => {
      for (const [queryKey, data] of context?.previousLists ?? []) {
        queryClient.setQueryData(queryKey, data)
      }
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: applicationKeys.lists() })
    },
  })
}
