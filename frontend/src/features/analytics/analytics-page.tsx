import { StatTile } from '@/components/charts/stat-tile'
import { TrackBarChart, type TrackBarDatum } from '@/components/charts/track-bar-chart'
import { EmptyState, ErrorState, TrackLoader } from '@/components/ui/feedback'
import { PageHeader, SectionHeading } from '@/components/ui/panel'
import { TBody, TD, TH, THead, TR, Table } from '@/components/ui/table'
import { formatPercent } from '@/lib/format'
import { INK, LINE_COLORS } from '@/lib/design'
import type { ReactNode } from 'react'
import { useConversion, useFunnel, useResumePerformance, useTimeInStage } from './hooks'

/** One analytics block: heading, its own loading/error/empty handling. */
function AnalyticsSection({
  title,
  note,
  isLoading,
  error,
  onRetry,
  isEmpty,
  emptyDescription,
  children,
}: {
  title: string
  note?: string
  isLoading: boolean
  error: unknown
  onRetry: () => void
  isEmpty: boolean
  emptyDescription: string
  children: ReactNode
}) {
  return (
    <section>
      <SectionHeading weight="heavy" aside={note}>
        {title}
      </SectionHeading>
      <div className="mt-5">
        {isLoading && <TrackLoader label="COMPILING" />}
        {Boolean(error) && <ErrorState error={error} onRetry={onRetry} />}
        {!isLoading && !error && isEmpty && (
          <EmptyState title="Not enough data yet" description={emptyDescription} />
        )}
        {!isLoading && !error && !isEmpty && children}
      </div>
    </section>
  )
}

/** Read-only aggregate views over the caller's own application history. */
export function AnalyticsPage() {
  const funnel = useFunnel()
  const conversion = useConversion()
  const timeInStage = useTimeInStage()
  const resumePerformance = useResumePerformance()

  const funnelData: TrackBarDatum[] = (funnel.data?.stages ?? []).map((stage) => ({
    label: stage.stage,
    value: stage.applications,
    display: String(stage.applications),
    detail: `${stage.applications} of ${funnel.data?.totalApplications ?? 0} applications reached ${stage.stage}`,
  }))

  const conversionData: TrackBarDatum[] = (conversion.data?.stageConversions ?? []).map((step) => ({
    label: `${step.fromStage}→${step.toStage}`,
    value: step.conversionRate,
    display: formatPercent(step.conversionRate),
    detail: `${step.toCount} of ${step.fromCount} moved on`,
  }))

  const timeData: TrackBarDatum[] = (timeInStage.data?.stages ?? []).map((stage) => ({
    label: stage.stage,
    value: stage.averageDays,
    display: `${stage.averageDays.toFixed(1)}D`,
    detail: `Average over ${stage.sampleSize} application${stage.sampleSize === 1 ? '' : 's'}`,
  }))

  const sources = conversion.data?.responseRateBySource ?? []
  const versions = resumePerformance.data?.versions ?? []

  const offers = funnel.data?.stages.find((stage) => stage.stage === 'OFFER')?.applications ?? 0
  const total = funnel.data?.totalApplications ?? 0

  return (
    <>
      <PageHeader
        title="Network Analytics"
        meta={funnel.data ? `${total} APPLICATIONS ON RECORD` : 'LOADING'}
      />

      {funnel.data && total > 0 && (
        <div className="mb-11 grid gap-3 sm:grid-cols-3">
          <StatTile label="APPLICATIONS" value={total} />
          <StatTile
            label="OFFERS"
            value={offers}
            color={LINE_COLORS[1]}
            detail={total > 0 ? `${formatPercent(offers / total)} OF ALL LINES` : undefined}
          />
          <StatTile
            label="REACHED INTERVIEW"
            value={funnel.data.stages.find((stage) => stage.stage === 'INTERVIEW')?.applications ?? 0}
          />
        </div>
      )}

      <div className="flex flex-col gap-12">
        <AnalyticsSection
          title="Funnel"
          note="APPLICATIONS THAT EVER REACHED EACH STATION"
          isLoading={funnel.isLoading}
          error={funnel.error}
          onRetry={() => void funnel.refetch()}
          isEmpty={funnelData.length === 0 || total === 0}
          emptyDescription="Add applications and move them between stations to build a funnel."
        >
          <TrackBarChart data={funnelData} color={INK} max={total} />
        </AnalyticsSection>

        <AnalyticsSection
          title="Stage conversion"
          note="ADJACENT PIPELINE STEPS"
          isLoading={conversion.isLoading}
          error={conversion.error}
          onRetry={() => void conversion.refetch()}
          isEmpty={conversionData.length === 0}
          emptyDescription="Conversion appears once applications have moved between stations."
        >
          <TrackBarChart data={conversionData} color={LINE_COLORS[2]} max={1} />
        </AnalyticsSection>

        <AnalyticsSection
          title="Time in stage"
          note="AVERAGE DAYS BEFORE MOVING ON"
          isLoading={timeInStage.isLoading}
          error={timeInStage.error}
          onRetry={() => void timeInStage.refetch()}
          isEmpty={timeData.length === 0}
          emptyDescription="Dwell time needs at least one application that has left a station."
        >
          <TrackBarChart data={timeData} color={LINE_COLORS[5]} />
        </AnalyticsSection>

        <AnalyticsSection
          title="Résumé performance"
          note="APPLICATIONS SENT VS. RESPONSES"
          isLoading={resumePerformance.isLoading}
          error={resumePerformance.error}
          onRetry={() => void resumePerformance.refetch()}
          isEmpty={versions.length === 0}
          emptyDescription="Attach résumé versions to applications to compare how they perform."
        >
          <Table>
            <THead>
              <TR>
                <TH>Résumé version</TH>
                <TH align="right">Sent</TH>
                <TH align="right">Responses</TH>
                <TH align="right">Response rate</TH>
              </TR>
            </THead>
            <TBody>
              {versions.map((version) => (
                <TR key={version.documentId ?? version.label ?? 'unattached'}>
                  <TD className="font-semibold tracking-[0.05em] uppercase text-ink">
                    {version.label ?? 'No résumé attached'}
                  </TD>
                  <TD align="right">{version.totalApplications}</TD>
                  <TD align="right">{version.respondedApplications}</TD>
                  <TD align="right">{formatPercent(version.responseRate)}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </AnalyticsSection>

        <AnalyticsSection
          title="Response rate by source"
          note="WHICH CHANNELS ANSWER"
          isLoading={conversion.isLoading}
          error={conversion.error}
          onRetry={() => void conversion.refetch()}
          isEmpty={sources.length === 0}
          emptyDescription="Record a source on your applications to compare channels."
        >
          <Table>
            <THead>
              <TR>
                <TH>Source</TH>
                <TH align="right">Sent</TH>
                <TH align="right">Responses</TH>
                <TH align="right">Response rate</TH>
              </TR>
            </THead>
            <TBody>
              {sources.map((source) => (
                <TR key={source.source ?? 'unknown'}>
                  <TD className="font-semibold tracking-[0.05em] uppercase text-ink">
                    {source.source ?? 'Unspecified'}
                  </TD>
                  <TD align="right">{source.totalApplications}</TD>
                  <TD align="right">{source.respondedApplications}</TD>
                  <TD align="right">{formatPercent(source.responseRate)}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </AnalyticsSection>
      </div>
    </>
  )
}
