import { Button } from '@/components/ui/button'
import { Tag } from '@/components/ui/chip'
import { SectionHeading } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { formatBoardDateFull } from '@/lib/format'
import { lineColorFor } from '@/lib/design'
import type { ApplicationResponse } from '@/api/types'
import { useScoreApplication } from './hooks'

/** Score bar drawn as a track segment, in the application's line colour. */
function ScoreTrack({ score, color }: { score: number; color: string }) {
  const pct = Math.max(0, Math.min(100, Math.round(score * 100)))
  return (
    <div className="flex items-center gap-3">
      <div className="relative h-[7px] flex-1 rounded-[4px]" style={{ background: `${color}22` }}>
        <span
          className="absolute top-0 left-0 h-[7px] rounded-[4px]"
          style={{ width: `${pct}%`, background: color }}
        />
      </div>
      <span className="font-mono text-[13px] font-bold tracking-[0.06em]" style={{ color }}>
        {pct}%
      </span>
    </div>
  )
}

/**
 * Résumé-to-JD match. Scoring is cached server-side: re-running returns the
 * stored result unless the résumé profile or the JD text changed.
 */
export function MatchScorePanel({ application }: { application: ApplicationResponse }) {
  const score = useScoreApplication()
  const { notify, notifyError } = useToast()
  const hasJd = Boolean(application.jobDescriptionText?.trim())
  const color = lineColorFor(application.id)

  const run = async () => {
    try {
      const result = await score.mutateAsync(application.id)
      notify(
        result.cached ? 'Score unchanged since it was last computed.' : 'Match score updated.',
        'success',
      )
    } catch (error) {
      notifyError(error)
    }
  }

  return (
    <section>
      <SectionHeading
        aside={
          <Button size="sm" loading={score.isPending} disabled={!hasJd} onClick={run}>
            {application.matchScore == null ? 'Score match' : 'Re-score'}
          </Button>
        }
      >
        Match score
      </SectionHeading>

      <div className="mt-3 flex flex-col gap-4">
        {!hasJd && (
          <p className="font-mono text-[10px] leading-relaxed tracking-[0.04em] text-muted">
            Add the job description text to this application to score it against your résumé
            profile.
          </p>
        )}

        {hasJd && application.matchScore == null && (
          <p className="font-mono text-[10px] leading-relaxed tracking-[0.04em] text-muted">
            Not scored yet. Needs a parsed résumé profile — parse one on the Documents page first.
          </p>
        )}

        {application.matchScore != null && (
          <>
            <ScoreTrack score={application.matchScore} color={color} />
            <p className="type-meta">SCORED {formatBoardDateFull(application.scoredAt)}</p>

            {application.matchedSkills && application.matchedSkills.length > 0 && (
              <div>
                <p className="type-meta mb-2">MATCHED · {application.matchedSkills.length}</p>
                <div className="flex flex-wrap gap-1.5">
                  {application.matchedSkills.map((skill) => (
                    <Tag key={skill} tone="positive">
                      {skill}
                    </Tag>
                  ))}
                </div>
              </div>
            )}

            {application.missingSkills && application.missingSkills.length > 0 && (
              <div>
                <p className="type-meta mb-2">GAPS · {application.missingSkills.length}</p>
                <div className="flex flex-wrap gap-1.5">
                  {application.missingSkills.map((skill) => (
                    <Tag key={skill} tone="negative">
                      {skill}
                    </Tag>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  )
}
