import { useState } from 'react'
import { saveBlob } from '@/api/client'
import { Button, type ButtonProps } from '@/components/ui/button'
import { useToast } from '@/components/ui/toast'

export interface ExportCsvButtonProps extends Omit<ButtonProps, 'onClick' | 'loading'> {
  /** Fetcher returning the streamed file and its server-supplied name. */
  fetcher: () => Promise<{ blob: Blob; filename: string }>
  label?: string
}

/**
 * CSV download. The export endpoints need an Authorization header, so a
 * plain `<a href>` cannot fetch them — pull the blob, then hand it to the
 * browser. Shared by application and interview exports.
 */
export function ExportCsvButton({ fetcher, label = 'Export CSV', ...props }: ExportCsvButtonProps) {
  const { notifyError, notify } = useToast()
  const [busy, setBusy] = useState(false)

  const run = async () => {
    setBusy(true)
    try {
      const { blob, filename } = await fetcher()
      saveBlob(blob, filename)
      notify('Export downloaded.', 'success')
    } catch (error) {
      notifyError(error)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Button size="sm" loading={busy} onClick={run} {...props}>
      {label}
    </Button>
  )
}
