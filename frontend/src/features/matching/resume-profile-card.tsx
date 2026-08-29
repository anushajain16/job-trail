import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useParseResumeProfileMutation, useResumeProfileQuery } from '@/features/matching/hooks'
import { describeApiError } from '@/lib/describe-api-error'

/** Parses (or re-parses) the caller's resume into a structured profile —
 * the one-time step every application's match score is scored against.
 * Re-running this after uploading a newer resume is what invalidates
 * every application's cached match score (see MatchScoringService). */
export function ResumeProfileCard() {
  const { data: existingProfile, isPending, isError: hasNoProfile } = useResumeProfileQuery()
  const parseMutation = useParseResumeProfileMutation()

  // The just-parsed result (if any) is the freshest data — prefer it over
  // whatever GET /resume-profile last returned, without waiting on a refetch.
  const profile = parseMutation.data ?? existingProfile

  return (
    <Card>
      <CardHeader>
        <CardTitle>Resume profile</CardTitle>
        <CardDescription>
          Parses your most recently uploaded resume (Documents API) into skills, experience, and seniority — what
          every application's match % is scored against.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {isPending ? (
          <p className="text-sm text-muted-foreground">Checking…</p>
        ) : !profile ? (
          hasNoProfile ? (
            <p className="text-sm text-muted-foreground">No resume parsed yet.</p>
          ) : null
        ) : (
          <div className="flex flex-col gap-2">
            <div className="flex flex-wrap gap-1.5">
              {profile.profile.skills.map((skill) => (
                <Badge key={skill} variant="secondary">
                  {skill}
                </Badge>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">
              Parsed {new Date(profile.parsedAt).toLocaleString()} · confidence {Math.round(profile.confidence * 100)}%
            </p>
          </div>
        )}

        {parseMutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {describeApiError(parseMutation.error)}
          </p>
        )}

        <Button
          variant="outline"
          size="sm"
          className="self-start"
          onClick={() => parseMutation.mutate()}
          disabled={parseMutation.isPending}
        >
          {parseMutation.isPending ? 'Parsing…' : 'Parse latest resume'}
        </Button>
      </CardContent>
    </Card>
  )
}
