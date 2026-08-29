import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useHealth } from '@/hooks/use-health'

export function DashboardPage() {
  const { data, dataUpdatedAt, isPending } = useHealth()

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">Base layout scaffold — application list lands here.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Backend connectivity</CardTitle>
          <CardDescription>
            GET /actuator/health through the API client, proxied by Vite to the Spring backend.
          </CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          {isPending
            ? 'Checking…'
            : data
              ? `Last checked ${new Date(dataUpdatedAt).toLocaleTimeString()} — status ${data.status}`
              : 'No response yet.'}
        </CardContent>
      </Card>
    </div>
  )
}
