import { api } from '@/api/client'
import type { DocumentDownloadResponse, DocumentResponse, DocumentType, Uuid } from '@/api/types'

/** Backend default; overridden by DOCUMENT_MAX_SIZE_BYTES server-side. */
export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024

export const ACCEPTED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]

export const ACCEPTED_EXTENSIONS = '.pdf,.docx'

export function listDocuments(type?: DocumentType) {
  return api.get<DocumentResponse[]>('/api/documents', { query: { type } })
}

export function uploadDocument(input: { type: DocumentType; label: string; file: File }) {
  const formData = new FormData()
  formData.append('type', input.type)
  formData.append('label', input.label)
  formData.append('file', input.file)
  return api.upload<DocumentResponse>('/api/documents', formData)
}

/** Returns a short-lived presigned URL, not the bytes. */
export function getDownloadUrl(id: Uuid) {
  return api.get<DocumentDownloadResponse>(`/api/documents/${id}`)
}

export function deleteDocument(id: Uuid) {
  return api.delete(`/api/documents/${id}`)
}
