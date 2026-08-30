import { useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { Field, Input, Select } from '@/components/ui/field'
import { FormError } from '@/components/ui/feedback'
import { useToast } from '@/components/ui/toast'
import { formatBytes } from '@/lib/format'
import type { DocumentType } from '@/api/types'
import { ACCEPTED_EXTENSIONS, ACCEPTED_MIME_TYPES, MAX_DOCUMENT_BYTES } from './api'
import { useUploadDocument } from './hooks'

const TYPE_OPTIONS = [
  { value: 'RESUME', label: 'Résumé' },
  { value: 'COVER_LETTER', label: 'Cover letter' },
]

export interface UploadDialogProps {
  open: boolean
  onClose: () => void
  /** Pre-selects the section the user opened it from. */
  defaultType?: DocumentType
}

/**
 * Upload a new document. Every upload is a new immutable row server-side —
 * there is no edit path, only upload-new / delete-old.
 */
export function UploadDocumentDialog({ open, onClose, defaultType = 'RESUME' }: UploadDialogProps) {
  const upload = useUploadDocument()
  const { notify } = useToast()
  const [type, setType] = useState<DocumentType>(defaultType)
  const [label, setLabel] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)

  const reset = () => {
    setLabel('')
    setFile(null)
    setFieldError(null)
    setError(null)
  }

  const close = () => {
    reset()
    onClose()
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)

    if (!file) return setFieldError('Choose a PDF or DOCX file.')
    // Checked here as well as server-side so a 10MB upload is not spent
    // discovering it is 10MB.
    if (!ACCEPTED_MIME_TYPES.includes(file.type)) {
      return setFieldError('Only PDF and DOCX files are accepted.')
    }
    if (file.size > MAX_DOCUMENT_BYTES) {
      return setFieldError(`Maximum size is ${formatBytes(MAX_DOCUMENT_BYTES)}.`)
    }
    if (!label.trim()) return setFieldError('Give this version a label, e.g. "v4 — product".')

    try {
      await upload.mutateAsync({ type, label: label.trim(), file })
      notify('Document uploaded.', 'success')
      close()
    } catch (caught) {
      setError(caught)
    }
  }

  return (
    <Dialog
      open={open}
      onClose={close}
      title="Upload document"
      description="PDF or DOCX, up to 10 MB. Each upload is kept as its own version."
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={close}>
            Cancel
          </Button>
          <Button
            variant="solid"
            size="sm"
            type="submit"
            form="upload-document"
            loading={upload.isPending}
          >
            Upload
          </Button>
        </>
      }
    >
      <form id="upload-document" onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        <Field label="Type" required>
          {(props) => (
            <Select
              {...props}
              value={type}
              onChange={(event) => setType(event.target.value as DocumentType)}
              options={TYPE_OPTIONS}
            />
          )}
        </Field>

        <Field label="Label" hint="How you will recognise this version later" required>
          {(props) => (
            <Input
              {...props}
              maxLength={255}
              placeholder="v4 — product roles"
              value={label}
              onChange={(event) => setLabel(event.target.value)}
            />
          )}
        </Field>

        <Field
          label="File"
          required
          error={fieldError}
          hint={file ? `${file.name} · ${formatBytes(file.size)}` : 'PDF or DOCX'}
        >
          {(props) => (
            <Input
              {...props}
              type="file"
              accept={ACCEPTED_EXTENSIONS}
              onChange={(event) => {
                setFile(event.target.files?.[0] ?? null)
                setFieldError(null)
              }}
              className="cursor-pointer py-1.5 file:mr-3 file:cursor-pointer file:rounded-[2px] file:border-[1.5px] file:border-ink file:bg-transparent file:px-2 file:py-1 file:font-mono file:text-[9px] file:tracking-[0.1em] file:uppercase"
            />
          )}
        </Field>

        <FormError error={error} />
      </form>
    </Dialog>
  )
}
