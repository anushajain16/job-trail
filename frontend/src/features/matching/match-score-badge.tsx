import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useScoreApplicationMutation } from '@/features/matching/hooks'
import type { Application } from '@/features/applications/types'
import { describeApiError } from '@/lib/describe-api-error'

const HIGH_MATCH_THRESHOLD = 0.7
const MID_MATCH_THRESHOLD = 0.4

function matchBadgeVariant(matchScore: number): 'default' | 'secondary' | 'destructive' {
  if (matchScore >= HIGH_MATCH_THRESHOLD) return 'default'
  if (matchScore >= MID_MATCH_THRESHOLD) return 'secondary'
  return 'destructive'
}

/** The match % badge — shows a score once one exists, or a button to
 * compute one (POST /{id}/score) when it doesn't. Requires
 * jobDescriptionText to be set and a resume profile to already be parsed;
 * a failure here (400/404/502) shows inline rather than throwing, since
 * this sits in a list row, not a page of its own. */
export function MatchScoreBadge({ application }: { application: Application }) {
  const scoreMutation = useScoreApplicationMutation()

  if (application.matchScore !== null) {
    return (
      <div className="flex items-center gap-1.5">
        <Badge variant={matchBadgeVariant(application.matchScore)}>
          {Math.round(application.matchScore * 100)}% match
        </Badge>
        <Button
          variant="ghost"
          size="icon-sm"
          title="Re-score"
          onClick={() => scoreMutation.mutate(application.id)}
          disabled={scoreMutation.isPending}
        >
          ↻
        </Button>
      </div>
    )
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <Button
        variant="outline"
        size="sm"
        onClick={() => scoreMutation.mutate(application.id)}
        disabled={scoreMutation.isPending}
      >
        {scoreMutation.isPending ? 'Scoring…' : 'Score'}
      </Button>
      {scoreMutation.isError && (
        <span role="alert" className="text-xs text-destructive">
          {describeApiError(scoreMutation.error)}
        </span>
      )}
    </div>
  )
}
