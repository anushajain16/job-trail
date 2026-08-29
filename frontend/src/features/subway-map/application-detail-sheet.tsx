import { Link } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { STAGE_LABELS, type Application } from '@/features/applications/types'
import { InterviewRoundsSection } from '@/features/interviews/interview-rounds-section'
import { MatchScoreBadge } from '@/features/matching/match-score-badge'
import type { HistoryEntry } from '@/features/subway-map/api'
import { STATUS_LABELS, categorizeStage } from '@/features/subway-map/layout'
import { statusColor } from '@/features/subway-map/status-color'

interface ApplicationDetailSheetProps {
  application: Application | null
  history: HistoryEntry[] | undefined
  onOpenChange: (open: boolean) => void
}

export function ApplicationDetailSheet({ application, history, onOpenChange }: ApplicationDetailSheetProps) {
  return (
    <Sheet open={application !== null} onOpenChange={onOpenChange}>
      <SheetContent className="overflow-y-auto p-6">
        {application && (
          <>
            <SheetHeader className="p-0">
              <SheetTitle>{application.role}</SheetTitle>
              <SheetDescription>{application.company}</SheetDescription>
            </SheetHeader>

            <div className="flex flex-wrap items-center gap-2">
              <Badge
                style={{ backgroundColor: statusColor(categorizeStage(application.currentStage)), color: 'var(--background)' }}
              >
                {STATUS_LABELS[categorizeStage(application.currentStage)]}
              </Badge>
              <Badge variant="secondary">{STAGE_LABELS[application.currentStage]}</Badge>
            </div>

            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <Detail label="Location" value={application.location} />
              <Detail label="Source" value={application.source} />
              <Detail
                label="Salary"
                value={formatSalary(application.salaryMin, application.salaryMax)}
              />
              <Detail label="Deadline" value={application.deadline} />
            </dl>

            {application.link && (
              <a
                href={application.link}
                target="_blank"
                rel="noreferrer noopener"
                className="text-sm text-primary underline underline-offset-4"
              >
                View posting ↗
              </a>
            )}

            {application.notes && (
              <div className="flex flex-col gap-1">
                <p className="text-xs font-medium text-muted-foreground">Notes</p>
                <p className="whitespace-pre-wrap text-sm">{application.notes}</p>
              </div>
            )}

            <Separator />

            <div className="flex flex-col gap-3">
              <p className="text-xs font-medium text-muted-foreground">Resume match</p>
              <MatchScoreBadge application={application} />
              {application.matchScore !== null && application.missingSkills.length > 0 && (
                <div className="flex flex-col gap-1.5">
                  <p className="text-xs text-muted-foreground">Missing skills</p>
                  <div className="flex flex-wrap gap-1.5">
                    {application.missingSkills.map((skill) => (
                      <Badge key={skill} variant="destructive">
                        {skill}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
              {application.matchScore !== null && application.matchedSkills.length > 0 && (
                <div className="flex flex-col gap-1.5">
                  <p className="text-xs text-muted-foreground">Matched skills</p>
                  <div className="flex flex-wrap gap-1.5">
                    {application.matchedSkills.map((skill) => (
                      <Badge key={skill} variant="outline">
                        {skill}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <Separator />

            <InterviewRoundsSection applicationId={application.id} />

            <Separator />

            <div className="flex flex-col gap-3">
              <p className="text-xs font-medium text-muted-foreground">History</p>
              {history === undefined ? (
                <p className="text-sm text-muted-foreground">Loading…</p>
              ) : (
                <ol className="flex flex-col gap-3">
                  {history.map((entry) => (
                    <li key={entry.id} className="flex items-baseline justify-between gap-4 text-sm">
                      <span className="font-medium">{STAGE_LABELS[entry.stage]}</span>
                      <span className="text-muted-foreground">{new Date(entry.changedAt).toLocaleString()}</span>
                    </li>
                  ))}
                </ol>
              )}
            </div>

            <Button variant="outline" render={<Link to={`/applications/${application.id}/edit`} />}>
              Edit application
            </Button>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}

function Detail({ label, value }: { label: string; value: string | number | null }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd>{value ?? '—'}</dd>
    </div>
  )
}

function formatSalary(min: number | null, max: number | null): string | null {
  if (min === null && max === null) return null
  if (min !== null && max !== null) return `$${min.toLocaleString()}–$${max.toLocaleString()}`
  return `$${(min ?? max)!.toLocaleString()}`
}
