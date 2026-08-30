import type { DocumentType, PageParams, Uuid } from './types'

/** Single source of truth for cache keys — invalidation reads from here. */
export const queryKeys = {
  me: ['auth', 'me'] as const,

  applications: {
    all: ['applications'] as const,
    list: (params: PageParams) => ['applications', 'list', params] as const,
    detail: (id: Uuid) => ['applications', 'detail', id] as const,
    history: (id: Uuid) => ['applications', 'history', id] as const,
  },

  documents: {
    all: ['documents'] as const,
    list: (type?: DocumentType) => ['documents', 'list', type ?? 'ALL'] as const,
  },

  resumeProfile: ['resume-profile'] as const,

  interviews: {
    all: ['interviews'] as const,
    byApplication: (applicationId: Uuid) => ['interviews', 'application', applicationId] as const,
  },

  analytics: {
    all: ['analytics'] as const,
    funnel: ['analytics', 'funnel'] as const,
    conversion: ['analytics', 'conversion'] as const,
    timeInStage: ['analytics', 'time-in-stage'] as const,
    resumePerformance: ['analytics', 'resume-performance'] as const,
  },

  googleCalendar: {
    connection: ['google-calendar', 'connection'] as const,
  },
} as const
