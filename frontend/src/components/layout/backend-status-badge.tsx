import { Badge } from '@/components/ui/badge'
import { useHealth } from '@/hooks/use-health'

/** Small "is the backend reachable" indicator in the header — the visible
 * proof that the API client + TanStack Query + Vite proxy chain works,
 * not just that the app renders. */
export function BackendStatusBadge() {
  const { data, isPending, isError } = useHealth()

  if (isPending) {
    return <Badge variant="secondary">Backend: checking…</Badge>
  }

  if (isError || data?.status !== 'UP') {
    return <Badge variant="destructive">Backend: unreachable</Badge>
  }

  return <Badge variant="default">Backend: up</Badge>
}
