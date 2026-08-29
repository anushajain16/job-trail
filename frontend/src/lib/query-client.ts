import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // A dropped/failed request against a dev backend that isn't running
      // yet shouldn't retry 3 times with backoff before the UI can show
      // anything — one retry is enough signal without the wait.
      retry: 1,
      staleTime: 30_000,
    },
  },
})
