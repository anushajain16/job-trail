import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ApplicationForm } from '@/features/applications/application-form'
import { useCreateApplicationMutation } from '@/features/applications/hooks'
import { EMPTY_APPLICATION_INPUT, type ApplicationInput } from '@/features/applications/types'
import { applyParsedPosting } from '@/features/jobposting/apply-parsed-posting'
import type { ParsedJobPosting } from '@/features/jobposting/api'
import { PasteUrlPanel } from '@/features/jobposting/paste-url-panel'
import { describeApiError } from '@/lib/describe-api-error'

export function ApplicationCreatePage() {
  const navigate = useNavigate()
  const createMutation = useCreateApplicationMutation()

  // ApplicationForm owns its own field state once mounted (so typing stays
  // fast and uncontrolled-from-above) — autofilling after a URL parse
  // means handing it a new set of initialValues, which only takes if the
  // form actually remounts. formVersion forces that via ApplicationForm's
  // key, the same trick ApplicationEditPage uses for the same reason.
  const [initialValues, setInitialValues] = useState(EMPTY_APPLICATION_INPUT)
  const [formVersion, setFormVersion] = useState(0)

  function handleAutofill(parsed: ParsedJobPosting, url: string) {
    setInitialValues((current) => applyParsedPosting(current, parsed, url))
    setFormVersion((version) => version + 1)
  }

  function handleSubmit(input: ApplicationInput) {
    createMutation.mutate(input, { onSuccess: () => navigate('/applications') })
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <PasteUrlPanel onAutofill={handleAutofill} />

      <Card>
        <CardHeader>
          <CardTitle>New application</CardTitle>
        </CardHeader>
        <CardContent>
          <ApplicationForm
            key={formVersion}
            initialValues={initialValues}
            submitLabel="Create application"
            pendingLabel="Creating…"
            isPending={createMutation.isPending}
            submitError={createMutation.isError ? describeApiError(createMutation.error) : undefined}
            onSubmit={handleSubmit}
            onCancel={() => navigate('/applications')}
          />
        </CardContent>
      </Card>
    </div>
  )
}
