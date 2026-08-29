import { ApiError } from '@/lib/api-client'

/** Backend error messages (GlobalExceptionHandler's ErrorResponse.message)
 * are already user-presentable — validation failures, 404s, conflicts —
 * so this just falls back to something generic for a request that never
 * got an HTTP response at all (network down, backend unreachable). */
export function describeApiError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }
  return 'Something went wrong. Check your connection and try again.'
}
