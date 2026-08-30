import { useQuery } from '@tanstack/react-query'
import { cn } from '@/lib/cn'

/**
 * Live service indicator, read from Spring Boot Actuator's health endpoint
 * (the only actuator endpoint the backend exposes). Unauthenticated on
 * purpose — it should still report when the session is gone.
 */
async function fetchHealth(): Promise<'UP' | 'DOWN'> {
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
  const response = await fetch(`${base}/actuator/health`)
  if (!response.ok) return 'DOWN'
  const body = (await response.json()) as { status?: string }
  return body.status === 'UP' ? 'UP' : 'DOWN'
}

export function BackendStatusBadge({ className }: { className?: string }) {
  const { data, isError } = useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
    refetchInterval: 30_000,
    retry: false,
    staleTime: 0,
  })

  const up = data === 'UP' && !isError
  const pending = data === undefined && !isError

  return (
    <span className={cn('flex items-center gap-2', className)} title="Backend service status">
      <span
        aria-hidden
        className={cn(
          'inline-block h-2 w-2 rounded-full',
          pending ? 'bg-grey' : up ? 'bg-success' : 'bg-danger',
        )}
      />
      <span className="type-meta">{pending ? 'CHECKING' : up ? 'SERVICE OK' : 'NO SIGNAL'}</span>
    </span>
  )
}
