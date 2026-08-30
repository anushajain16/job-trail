import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/api/query-keys'
import type { DocumentType, Uuid } from '@/api/types'
import * as documentsApi from './api'

export function useDocuments(type?: DocumentType) {
  return useQuery({
    queryKey: queryKeys.documents.list(type),
    queryFn: () => documentsApi.listDocuments(type),
  })
}

export function useUploadDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: documentsApi.uploadDocument,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.documents.all })
      // A newer résumé makes the stored profile stale — the user is meant
      // to re-parse, and the panel says so once the list changes.
      void queryClient.invalidateQueries({ queryKey: queryKeys.resumeProfile })
    },
  })
}

export function useDeleteDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: Uuid) => documentsApi.deleteDocument(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.documents.all })
      void queryClient.invalidateQueries({ queryKey: queryKeys.applications.all })
    },
  })
}
