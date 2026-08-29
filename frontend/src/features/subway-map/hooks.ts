import { useQueries, useQuery } from '@tanstack/react-query'
import { applicationsApi } from '@/features/applications/api'
import { applicationKeys } from '@/features/applications/query-keys'
import { historyApi } from '@/features/subway-map/api'

// The map is an overview of everything, not a paginated slice — one call
// at a size comfortably above what a single job search accumulates. A
// board with more than this needs cursor-based fetch-all or virtualized
// rendering, neither of which this feature needs yet.
const MAP_PAGE_SIZE = 200

export function useApplicationsForMapQuery() {
  return useQuery({
    queryKey: applicationKeys.list({ page: 0, size: MAP_PAGE_SIZE }),
    queryFn: () => applicationsApi.list({ page: 0, size: MAP_PAGE_SIZE }),
  })
}

/** One GET /api/applications/{id}/history per application, in parallel —
 * there's no bulk-history endpoint, so this is the map's literal reading
 * of "each line from its own event log." */
export function useHistoriesQuery(applicationIds: string[]) {
  return useQueries({
    queries: applicationIds.map((id) => ({
      queryKey: ['applications', 'history', id] as const,
      queryFn: () => historyApi.get(id),
    })),
    combine: (results) => ({
      historyByApplicationId: new Map(applicationIds.map((id, i) => [id, results[i].data])),
      isPending: results.some((result) => result.isPending),
      isError: results.some((result) => result.isError),
    }),
  })
}
