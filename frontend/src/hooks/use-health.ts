import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/lib/api-client'

// Spring Boot Actuator's own shape (management.endpoints.web.exposure
// only includes "health" — see backend's application.yml). `components`
// is omitted here since nothing in the UI reads past top-level status.
export interface HealthResponse {
  status: 'UP' | 'DOWN' | (string & {})
}

export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: () => apiFetch<HealthResponse>('/actuator/health'),
    // The backend being down/starting is a real, expected state during
    // local dev, not a rare failure — poll instead of leaving a stale
    // "down" badge on screen once it comes back up.
    refetchInterval: 15_000,
  })
}
