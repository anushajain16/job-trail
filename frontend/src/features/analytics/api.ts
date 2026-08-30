import { api } from '@/api/client'
import type {
  ConversionResponse,
  FunnelResponse,
  ResumePerformanceResponse,
  TimeInStageResponse,
} from '@/api/types'

export const analyticsApi = {
  funnel: () => api.get<FunnelResponse>('/api/analytics/funnel'),
  conversion: () => api.get<ConversionResponse>('/api/analytics/conversion'),
  timeInStage: () => api.get<TimeInStageResponse>('/api/analytics/time-in-stage'),
  resumePerformance: () => api.get<ResumePerformanceResponse>('/api/analytics/resume-performance'),
}
