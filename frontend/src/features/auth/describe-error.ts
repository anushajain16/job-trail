import { ApiError } from '@/lib/api-client'

/** Backend validation/auth error messages (GlobalExceptionHandler) are
 * already user-presentable — "Invalid email or password", "An account
 * with this email already exists", "email: must not be blank" — so this
 * just falls back to something generic for a request that never got an
 * HTTP response at all (network down, ml-service-style timeout, etc.). */
export function describeAuthError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }
  return 'Something went wrong. Check your connection and try again.'
}
