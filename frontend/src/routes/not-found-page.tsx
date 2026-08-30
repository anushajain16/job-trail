import { Link } from 'react-router-dom'
import { PageHeader } from '@/components/ui/panel'
import { EmptyState } from '@/components/ui/feedback'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <>
      <PageHeader title="No such station" meta="404" />
      <EmptyState
        title="Off the map"
        description="That route does not exist on this network."
        action={
          <Link to="/map" className="no-underline">
            <Button size="sm">Back to the map</Button>
          </Link>
        }
      />
    </>
  )
}
