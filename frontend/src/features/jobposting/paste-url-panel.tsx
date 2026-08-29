import { type FormEvent, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { ParsedJobPosting } from '@/features/jobposting/api'
import { useParseJobPostingUrlMutation } from '@/features/jobposting/hooks'
import { describeApiError } from '@/lib/describe-api-error'

interface PasteUrlPanelProps {
  onAutofill: (parsed: ParsedJobPosting, url: string) => void
}

/**
 * The "paste a URL, autofill the form" entry point — a thin UI over
 * POST /api/job-postings/parse. The endpoint always answers 200: a
 * `parsed` result calls back into the form, and `available: false` (the
 * documented failure path — ml-service down, slow, or erroring) just
 * shows why, right above the same form fields the user can fill by hand
 * either way. Nothing here is required to submit the form.
 */
export function PasteUrlPanel({ onAutofill }: PasteUrlPanelProps) {
  const [url, setUrl] = useState('')
  const parseMutation = useParseJobPostingUrlMutation()

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!url.trim()) return
    parseMutation.mutate(url.trim(), {
      onSuccess: (result) => {
        if (result.available && result.parsed) {
          onAutofill(result.parsed, url.trim())
        }
      },
    })
  }

  const result = parseMutation.data

  return (
    <Card className="border-dashed">
      <CardContent className="flex flex-col gap-3 pt-6">
        <form onSubmit={handleSubmit} className="flex items-end gap-2">
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor="posting-url">Job posting URL</Label>
            <Input
              id="posting-url"
              type="url"
              placeholder="https://…"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
          </div>
          <Button type="submit" variant="secondary" disabled={parseMutation.isPending || !url.trim()}>
            {parseMutation.isPending ? 'Fetching…' : 'Autofill'}
          </Button>
        </form>

        {parseMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {describeApiError(parseMutation.error)}
          </p>
        )}

        {result && !result.available && (
          <p role="status" className="text-sm text-muted-foreground">
            {result.message ?? "Couldn't auto-fill from that URL right now."} Enter the details manually below.
          </p>
        )}

        {result?.available && (
          <p role="status" className="text-sm text-muted-foreground">
            Filled in what we found
            {result.confidence !== null && ` (confidence ${Math.round(result.confidence * 100)}%)`} — double-check
            before saving, and paste the full posting text below for match scoring.
          </p>
        )}
      </CardContent>
    </Card>
  )
}

