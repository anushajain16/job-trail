import { isSuspendedStage, stationIndex } from '@/lib/design'
import type { ApplicationResponse } from '@/api/types'

export type StageFilter = 'all' | 'active' | 'suspended' | 'offers'
export type SortKey = 'grouped' | 'progress' | 'a-z' | 'recent'

export const STAGE_FILTERS: { value: StageFilter; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'active', label: 'Active' },
  { value: 'suspended', label: 'Suspended' },
  { value: 'offers', label: 'Offers' },
]

export const SORT_OPTIONS: { value: SortKey; label: string }[] = [
  { value: 'grouped', label: 'Grouped by station' },
  { value: 'progress', label: 'Furthest first' },
  { value: 'a-z', label: 'Company A–Z' },
  { value: 'recent', label: 'Recently added' },
]

function matchesFilter(application: ApplicationResponse, filter: StageFilter): boolean {
  switch (filter) {
    case 'active':
      return !isSuspendedStage(application.currentStage) && application.currentStage !== 'OFFER'
    case 'suspended':
      return isSuspendedStage(application.currentStage)
    case 'offers':
      return application.currentStage === 'OFFER'
    default:
      return true
  }
}

function matchesSearch(application: ApplicationResponse, search: string): boolean {
  if (!search) return true
  const needle = search.toLowerCase()
  return [application.company, application.role, application.location, application.source]
    .filter(Boolean)
    .some((value) => value!.toLowerCase().includes(needle))
}

/** One filter/sort pipeline, shared by the map and the applications table. */
export function selectApplications(
  applications: ApplicationResponse[],
  options: { filter: StageFilter; sort: SortKey; search?: string; showSuspended?: boolean },
): ApplicationResponse[] {
  const { filter, sort, search = '', showSuspended = true } = options

  const visible = applications.filter(
    (application) =>
      (showSuspended || !isSuspendedStage(application.currentStage)) &&
      matchesFilter(application, filter) &&
      matchesSearch(application, search),
  )

  const rank = (application: ApplicationResponse) => stationIndex(application.currentStage) ?? -1

  switch (sort) {
    case 'progress':
      return [...visible].sort((a, b) => rank(b) - rank(a) || a.company.localeCompare(b.company))
    case 'a-z':
      return [...visible].sort((a, b) => a.company.localeCompare(b.company))
    case 'recent':
      return [...visible].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    case 'grouped':
    default:
      // Suspended lines sink to the bottom; the rest group by station.
      return [...visible].sort(
        (a, b) =>
          Number(isSuspendedStage(a.currentStage)) - Number(isSuspendedStage(b.currentStage)) ||
          rank(a) - rank(b) ||
          a.company.localeCompare(b.company),
      )
  }
}

/** `7 LINES · 1 OFFER · 2 SUSPENDED`. */
export function networkSummary(applications: ApplicationResponse[]): string {
  const offers = applications.filter((a) => a.currentStage === 'OFFER').length
  const suspended = applications.filter((a) => isSuspendedStage(a.currentStage)).length
  return `${applications.length} LINE${applications.length === 1 ? '' : 'S'} · ${offers} OFFER${offers === 1 ? '' : 'S'} · ${suspended} SUSPENDED`
}
