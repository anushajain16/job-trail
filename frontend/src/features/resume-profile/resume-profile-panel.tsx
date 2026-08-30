import { Button } from '@/components/ui/button'
import { Tag } from '@/components/ui/chip'
import { ErrorState, TrackLoader } from '@/components/ui/feedback'
import { DataPoint, Panel, SectionHeading } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { formatBoardDateFull, formatPercent } from '@/lib/format'
import type { DocumentResponse } from '@/api/types'
import { useParseResumeProfile, useResumeProfile } from './hooks'

/**
 * The one-time extraction every match score is computed against. Parsing
 * runs on the *latest* résumé upload, so an upload newer than the stored
 * profile is called out — that is the moment to re-parse and re-score.
 */
export function ResumeProfilePanel({ resumes }: { resumes: DocumentResponse[] }) {
  const { data: profile, isLoading, isError, error, refetch } = useResumeProfile()
  const parse = useParseResumeProfile()
  const { notify, notifyError } = useToast()

  const latestResume = resumes[0] ?? null
  const stale = Boolean(profile && latestResume && profile.sourceDocumentId !== latestResume.id)

  const run = async () => {
    try {
      await parse.mutateAsync()
      notify('Résumé parsed. Re-score applications to use it.', 'success')
    } catch (caught) {
      notifyError(caught)
    }
  }

  return (
    <section>
      <SectionHeading
        weight="heavy"
        aside={
          <Button size="sm" loading={parse.isPending} disabled={!latestResume} onClick={run}>
            {profile ? 'Re-parse latest résumé' : 'Parse résumé'}
          </Button>
        }
      >
        Résumé profile
      </SectionHeading>

      <div className="mt-4">
        {isLoading && <TrackLoader label="LOADING PROFILE" />}
        {isError && <ErrorState error={error} onRetry={() => void refetch()} />}

        {!isLoading && !isError && !profile && (
          <Panel>
            <p className="font-mono text-[10px] leading-relaxed tracking-[0.04em] text-ink-soft">
              {latestResume
                ? 'No profile parsed yet. Parse your résumé once — every application’s match score is computed against it.'
                : 'Upload a résumé first, then parse it to unlock match scoring.'}
            </p>
          </Panel>
        )}

        {profile && (
          <Panel className="flex flex-col gap-5">
            {stale && (
              <p className="rounded-[2px] border-[1.5px] border-danger px-3 py-2 font-mono text-[10px] leading-relaxed tracking-[0.04em] text-danger">
                A newer résumé has been uploaded since this profile was parsed. Re-parse to
                score against it.
              </p>
            )}

            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <DataPoint label="SENIORITY">{profile.profile.seniority ?? '—'}</DataPoint>
              <DataPoint label="EXPERIENCE">
                {profile.profile.years_experience != null ? `${profile.profile.years_experience} YRS` : '—'}
              </DataPoint>
              <DataPoint label="CONFIDENCE">{formatPercent(profile.confidence)}</DataPoint>
              <DataPoint label="PARSED">{formatBoardDateFull(profile.parsedAt)}</DataPoint>
            </div>

            {profile.profile.summary && (
              <p className="font-mono text-[10px] leading-[1.8] tracking-[0.02em] text-ink-soft">
                {profile.profile.summary}
              </p>
            )}

            {profile.profile.roles?.length > 0 && (
              <div>
                <p className="type-meta mb-2">ROLES</p>
                <div className="flex flex-wrap gap-1.5">
                  {profile.profile.roles.map((role) => (
                    <Tag key={role}>{role}</Tag>
                  ))}
                </div>
              </div>
            )}

            {profile.profile.skills?.length > 0 && (
              <div>
                <p className="type-meta mb-2">SKILLS · {profile.profile.skills.length}</p>
                <div className="flex flex-wrap gap-1.5">
                  {profile.profile.skills.map((skill) => (
                    <Tag key={skill} tone="positive">
                      {skill}
                    </Tag>
                  ))}
                </div>
              </div>
            )}
          </Panel>
        )}
      </div>
    </section>
  )
}
