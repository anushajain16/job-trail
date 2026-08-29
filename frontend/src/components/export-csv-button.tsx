import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { authFetchFile } from '@/lib/api-client'
import { describeApiError } from '@/lib/describe-api-error'
import { downloadFile } from '@/lib/download-file'

interface ExportCsvButtonProps {
  path: string
  label: string
  pendingLabel: string
}

/** Hits a GET /export endpoint and saves the CSV it streams back. No
 * export-specific state to manage beyond "is a download in flight right
 * now" — there's nothing to cache, so this doesn't go through TanStack
 * Query, just a plain fetch-and-save on click. */
export function ExportCsvButton({ path, label, pendingLabel }: ExportCsvButtonProps) {
  const [isPending, setIsPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleClick() {
    setIsPending(true)
    setError(null)
    try {
      const { blob, filename } = await authFetchFile(path)
      downloadFile(blob, filename)
    } catch (err) {
      setError(describeApiError(err))
    } finally {
      setIsPending(false)
    }
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <Button variant="outline" onClick={handleClick} disabled={isPending}>
        {isPending ? pendingLabel : label}
      </Button>
      {error && (
        <p role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}
