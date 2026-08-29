import { authFetch } from '@/lib/api-client'
import type { Stage } from '@/features/applications/types'

// Mirrors backend/.../analytics/dto/*.java exactly — see those files for
// the field-by-field meaning (what "responded" means, why the funnel
// counts "reached" not "passed through in order", etc.).

export interface FunnelStageCount {
  stage: Stage
  applications: number
}

export interface FunnelResponse {
  totalApplications: number
  stages: FunnelStageCount[]
}

export interface StageConversion {
  fromStage: Stage
  toStage: Stage
  fromCount: number
  toCount: number
  conversionRate: number
}

export interface SourceResponseRate {
  source: string
  totalApplications: number
  respondedApplications: number
  responseRate: number
}

export interface ConversionResponse {
  stageConversions: StageConversion[]
  responseRateBySource: SourceResponseRate[]
}

export interface StageDuration {
  stage: Stage
  averageDays: number
  sampleSize: number
}

export interface TimeInStageResponse {
  stages: StageDuration[]
}

export const analyticsApi = {
  funnel: () => authFetch<FunnelResponse>('/api/analytics/funnel'),
  conversion: () => authFetch<ConversionResponse>('/api/analytics/conversion'),
  timeInStage: () => authFetch<TimeInStageResponse>('/api/analytics/time-in-stage'),
}
