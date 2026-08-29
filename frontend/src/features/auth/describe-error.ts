// Auth call sites historically imported this name; kept as a thin
// re-export so nothing else has to change. New code should import
// describeApiError from '@/lib/describe-api-error' directly.
export { describeApiError as describeAuthError } from '@/lib/describe-api-error'
