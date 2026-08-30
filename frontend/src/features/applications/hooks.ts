import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import type {
  ApplicationCreateRequest,
  ApplicationUpdateRequest,
  PageParams,
  Stage,
  Uuid,
} from '@/api/types'
import * as applicationsApi from './api'

/** One page of applications — the table view. */
export function useApplications(params: PageParams) {
  return useQuery({
    queryKey: queryKeys.applications.list(params),
    queryFn: () => applicationsApi.listApplications(params),
  })
}

/**
 * Every application in one shot, for the map — the map draws the whole
 * network, so paging it would hide lines rather than defer them.
 */
export function useAllApplications() {
  return useQuery({
    queryKey: queryKeys.applications.list({ size: 200, sort: 'createdAt,desc' }),
    queryFn: () => applicationsApi.listApplications({ size: 200, sort: 'createdAt,desc' }),
    select: (page) => page.content,
  })
}

export function useApplication(id: Uuid | null) {
  return useQuery({
    queryKey: queryKeys.applications.detail(id ?? ''),
    queryFn: () => applicationsApi.getApplication(id!),
    enabled: Boolean(id),
  })
}

export function useStatusHistory(id: Uuid | null) {
  return useQuery({
    queryKey: queryKeys.applications.history(id ?? ''),
    queryFn: () => applicationsApi.getStatusHistory(id!),
    enabled: Boolean(id),
  })
}

/** Everything downstream of an application changing: lists, map, analytics. */
function useApplicationInvalidator() {
  const queryClient = useQueryClient()
  return (id?: Uuid) => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.applications.all })
    void queryClient.invalidateQueries({ queryKey: queryKeys.analytics.all })
    if (id) void queryClient.invalidateQueries({ queryKey: queryKeys.applications.history(id) })
  }
}

export function useCreateApplication() {
  const invalidate = useApplicationInvalidator()
  return useMutation({
    mutationFn: (body: ApplicationCreateRequest) => applicationsApi.createApplication(body),
    onSuccess: () => invalidate(),
  })
}

export function useUpdateApplication() {
  const invalidate = useApplicationInvalidator()
  return useMutation({
    mutationFn: ({ id, body }: { id: Uuid; body: ApplicationUpdateRequest }) =>
      applicationsApi.updateApplication(id, body),
    onSuccess: (application) => invalidate(application.id),
  })
}

export function useDeleteApplication() {
  const invalidate = useApplicationInvalidator()
  return useMutation({
    mutationFn: (id: Uuid) => applicationsApi.deleteApplication(id),
    onSuccess: () => invalidate(),
  })
}

export function useChangeStage() {
  const invalidate = useApplicationInvalidator()
  return useMutation({
    mutationFn: ({ id, stage }: { id: Uuid; stage: Stage }) =>
      applicationsApi.changeStage(id, stage),
    onSuccess: (application) => invalidate(application.id),
  })
}
