import { ApiError } from '@/api/client'

/**
 * Turn any thrown value into one line a user can act on. The backend's
 * `ErrorResponse.message` is already human-readable, so it wins whenever
 * present; the status-based fallbacks cover network/proxy failures where
 * there is no body at all.
 */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.payload?.message) return error.payload.message
    switch (error.status) {
      case 400:
        return 'That request was rejected — check the fields and try again.'
      case 401:
        return 'Your session has expired. Sign in again.'
      case 403:
        return 'You do not have access to that.'
      case 404:
        return 'Not found.'
      case 409:
        return 'That conflicts with something that already exists.'
      case 413:
        return 'That file is too large.'
      case 502:
        return 'An upstream service is unavailable. Try again shortly.'
      default:
        return error.message
    }
  }
  if (error instanceof TypeError) return 'Cannot reach the server. Is the backend running?'
  if (error instanceof Error) return error.message
  return 'Something went wrong.'
}
