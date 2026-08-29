import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ApplicationForm } from '@/features/applications/application-form'
import { useApplicationQuery, useUpdateApplicationMutation } from '@/features/applications/hooks'
import { applicationToInput, type ApplicationInput } from '@/features/applications/types'
import { describeApiError } from '@/lib/describe-api-error'

export function ApplicationEditPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: application, isPending, isError, error } = useApplicationQuery(id)
  const updateMutation = useUpdateApplicationMutation()

  if (!id) {
    return <Navigate to="/applications" replace />
  }

  function handleSubmit(input: ApplicationInput) {
    updateMutation.mutate(
      { id: id!, input },
      { onSuccess: () => navigate('/applications') },
    )
  }

  return (
    <div className="mx-auto max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle>Edit application</CardTitle>
        </CardHeader>
        <CardContent>
          {isPending ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : isError ? (
            <p role="alert" className="text-sm text-destructive">
              {describeApiError(error)}
            </p>
          ) : (
            <ApplicationForm
              key={application.id}
              initialValues={applicationToInput(application)}
              submitLabel="Save changes"
              pendingLabel="Saving…"
              isPending={updateMutation.isPending}
              submitError={updateMutation.isError ? describeApiError(updateMutation.error) : undefined}
              onSubmit={handleSubmit}
              onCancel={() => navigate('/applications')}
            />
          )}
        </CardContent>
      </Card>
    </div>
  )
}
