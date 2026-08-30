import { api, requestBlob } from '@/api/client'
import type {
  ApplicationCreateRequest,
  ApplicationResponse,
  ApplicationUpdateRequest,
  Page,
  PageParams,
  Stage,
  StatusHistoryResponse,
  Uuid,
} from '@/api/types'

export function listApplications(params: PageParams = {}) {
  return api.get<Page<ApplicationResponse>>('/api/applications', {
    query: { page: params.page, size: params.size, sort: params.sort },
  })
}

export function getApplication(id: Uuid) {
  return api.get<ApplicationResponse>(`/api/applications/${id}`)
}

export function createApplication(body: ApplicationCreateRequest) {
  return api.post<ApplicationResponse>('/api/applications', body)
}

export function updateApplication(id: Uuid, body: ApplicationUpdateRequest) {
  return api.patch<ApplicationResponse>(`/api/applications/${id}`, body)
}

export function deleteApplication(id: Uuid) {
  return api.delete(`/api/applications/${id}`)
}

/** Backend rejects a change to the stage it is already in (400). */
export function changeStage(id: Uuid, stage: Stage) {
  return api.patch<ApplicationResponse>(`/api/applications/${id}/stage`, { stage })
}

export function getStatusHistory(id: Uuid) {
  return api.get<StatusHistoryResponse[]>(`/api/applications/${id}/history`)
}

export function exportApplicationsCsv() {
  return requestBlob('/api/applications/export')
}
