import { useNavigate } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ApplicationForm } from '@/features/applications/application-form'
import { useCreateApplicationMutation } from '@/features/applications/hooks'
import { EMPTY_APPLICATION_INPUT, type ApplicationInput } from '@/features/applications/types'
import { describeApiError } from '@/lib/describe-api-error'

export function ApplicationCreatePage() {
  const navigate = useNavigate()
  const createMutation = useCreateApplicationMutation()

  function handleSubmit(input: ApplicationInput) {
    createMutation.mutate(input, { onSuccess: () => navigate('/applications') })
  }

  return (
    <div className="mx-auto max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle>New application</CardTitle>
        </CardHeader>
        <CardContent>
          <ApplicationForm
            initialValues={EMPTY_APPLICATION_INPUT}
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
