import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ui/dialog'
import { EmptyState, ErrorState, TrackLoader } from '@/components/ui/feedback'
import { PageHeader, SectionHeading } from '@/components/ui/panel'
import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/table'
import { useToast } from '@/components/ui/toast'
import { formatBoardDateFull, formatBytes } from '@/lib/format'
import { ResumeProfilePanel } from '@/features/resume-profile/resume-profile-panel'
import type { DocumentResponse, DocumentType } from '@/api/types'
import { getDownloadUrl } from './api'
import { useDeleteDocument, useDocuments } from './hooks'
import { UploadDocumentDialog } from './upload-dialog'

function DocumentTable({
  documents,
  onDelete,
}: {
  documents: DocumentResponse[]
  onDelete: (document: DocumentResponse) => void
}) {
  const { notifyError } = useToast()
  const [busyId, setBusyId] = useState<string | null>(null)

  /**
   * Downloads go straight from object storage via a short-lived presigned
   * URL, so this fetches the URL first and then hands it to the browser.
   */
  const download = async (document_: DocumentResponse) => {
    setBusyId(document_.id)
    try {
      const { downloadUrl } = await getDownloadUrl(document_.id)
      window.open(downloadUrl, '_blank', 'noopener')
    } catch (error) {
      notifyError(error)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <Table>
      <THead>
        <TR>
          <TH>Label</TH>
          <TH>File</TH>
          <TH align="right">Size</TH>
          <TH align="right">Uploaded</TH>
          <TH align="right" />
        </TR>
      </THead>
      <TBody>
        {documents.map((document_) => (
          <TR key={document_.id}>
            <TD className="font-semibold tracking-[0.05em] uppercase text-ink">
              {document_.label ?? '—'}
            </TD>
            <TD className="break-all">{document_.originalFilename}</TD>
            <TD align="right">{formatBytes(document_.size)}</TD>
            <TD align="right">{formatBoardDateFull(document_.uploadedAt)}</TD>
            <TD align="right">
              <span className="flex justify-end gap-2">
                <Button size="sm" loading={busyId === document_.id} onClick={() => download(document_)}>
                  Download
                </Button>
                <Button size="sm" variant="danger" onClick={() => onDelete(document_)}>
                  Delete
                </Button>
              </span>
            </TD>
          </TR>
        ))}
      </TBody>
    </Table>
  )
}

function DocumentSection({
  title,
  documents,
  onUpload,
  onDelete,
  emptyDescription,
}: {
  title: string
  documents: DocumentResponse[]
  onUpload: () => void
  onDelete: (document: DocumentResponse) => void
  emptyDescription: string
}) {
  return (
    <section>
      <SectionHeading
        weight="heavy"
        aside={
          <Button size="sm" onClick={onUpload}>
            Upload
          </Button>
        }
      >
        {title} · {documents.length}
      </SectionHeading>
      <div className="mt-4">
        {documents.length === 0 ? (
          <EmptyState title="Nothing uploaded" description={emptyDescription} />
        ) : (
          <DocumentTable documents={documents} onDelete={onDelete} />
        )}
      </div>
    </section>
  )
}

/** Résumé and cover-letter versions, plus the parsed résumé profile. */
export function DocumentsPage() {
  const { data: documents, isLoading, isError, error, refetch } = useDocuments()
  const remove = useDeleteDocument()
  const { notify, notifyError } = useToast()

  const [uploadType, setUploadType] = useState<DocumentType | null>(null)
  const [pendingDelete, setPendingDelete] = useState<DocumentResponse | null>(null)

  const byUploadedAtDesc = (a: DocumentResponse, b: DocumentResponse) =>
    b.uploadedAt.localeCompare(a.uploadedAt)
  const resumes = (documents ?? []).filter((d) => d.type === 'RESUME').sort(byUploadedAtDesc)
  const coverLetters = (documents ?? [])
    .filter((d) => d.type === 'COVER_LETTER')
    .sort(byUploadedAtDesc)

  const confirmDelete = async () => {
    if (!pendingDelete) return
    try {
      await remove.mutateAsync(pendingDelete.id)
      notify('Document deleted.', 'success')
      setPendingDelete(null)
    } catch (caught) {
      notifyError(caught)
    }
  }

  return (
    <>
      <PageHeader
        title="Documents"
        meta={documents ? `${resumes.length} RÉSUMÉS · ${coverLetters.length} COVER LETTERS` : 'LOADING'}
        actions={
          <Button variant="solid" size="sm" onClick={() => setUploadType('RESUME')}>
            Upload document
          </Button>
        }
      />

      {isLoading && <TrackLoader label="LOADING DOCUMENTS" />}
      {isError && <ErrorState error={error} onRetry={() => void refetch()} />}

      {documents && (
        <div className="flex flex-col gap-11">
          <DocumentSection
            title="Résumés"
            documents={resumes}
            onUpload={() => setUploadType('RESUME')}
            onDelete={setPendingDelete}
            emptyDescription="Upload a résumé to attach versions to applications and unlock match scoring."
          />

          <ResumeProfilePanel resumes={resumes} />

          <DocumentSection
            title="Cover letters"
            documents={coverLetters}
            onUpload={() => setUploadType('COVER_LETTER')}
            onDelete={setPendingDelete}
            emptyDescription="Cover letters can be attached to individual applications."
          />
        </div>
      )}

      <UploadDocumentDialog
        open={uploadType !== null}
        defaultType={uploadType ?? 'RESUME'}
        onClose={() => setUploadType(null)}
      />

      <ConfirmDialog
        open={pendingDelete !== null}
        onClose={() => setPendingDelete(null)}
        onConfirm={confirmDelete}
        loading={remove.isPending}
        title="Delete this document?"
        description="The stored file is removed permanently. Applications linked to it keep their other details."
        confirmLabel="Delete"
      />
    </>
  )
}
