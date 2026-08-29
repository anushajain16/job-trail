import { useMutation } from '@tanstack/react-query'
import { jobPostingApi } from '@/features/jobposting/api'

export function useParseJobPostingUrlMutation() {
  return useMutation({
    mutationFn: (url: string) => jobPostingApi.parseUrl(url),
  })
}
