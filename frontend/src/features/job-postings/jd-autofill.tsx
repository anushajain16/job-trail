import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Tag } from '@/components/ui/chip'
import { Field, Input } from '@/components/ui/field'
import { FormError } from '@/components/ui/feedback'
import { Panel } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { formatPercent } from '@/lib/format'
import type { ParsedJobPosting } from '@/api/types'
import type { ApplicationFormValues } from '@/features/applications/form-values'
import { parseJobPostingUrl } from './api'

/** Only fills blanks — never overwrites something already typed. */
function merge(current: ApplicationFormValues, parsed: ParsedJobPosting, url: string) {
  const fill = (existing: string, incoming: string | number | null | undefined) =>
    existing || (incoming == null ? '' : String(incoming))

  return {
    ...current,
    company: fill(current.company, parsed.company),
    role: fill(current.role, parsed.role),
    location: fill(current.location, parsed.location),
    salaryMin: fill(current.salaryMin, parsed.salaryMin != null ? Math.round(parsed.salaryMin) : null),
    salaryMax: fill(current.salaryMax, parsed.salaryMax != null ? Math.round(parsed.salaryMax) : null),
    link: current.link || url,
  }
}

export interface JdAutofillProps {
  values: ApplicationFormValues
  setValues: (updater: (current: ApplicationFormValues) => ApplicationFormValues) => void
}

/**
 * Paste a posting URL, fill the form. The parsed `summary` is deliberately
 * *not* written into the job description field — it is one or two sentences
 * and would look precise while being useless for match scoring, which needs
 * the full text pasted by hand.
 */
export function JdAutofill({ values, setValues }: JdAutofillProps) {
  const { notify } = useToast()
  const [url, setUrl] = useState('')
  const [unavailable, setUnavailable] = useState<string | null>(null)
  const [parsed, setParsed] = useState<ParsedJobPosting | null>(null)
  const [confidence, setConfidence] = useState<number | null>(null)

  const parse = useMutation({
    mutationFn: parseJobPostingUrl,
    onSuccess: (response) => {
      if (!response.available || !response.parsed) {
        setParsed(null)
        setUnavailable(response.message ?? 'Parsing is unavailable right now — fill the form manually.')
        return
      }
      setUnavailable(null)
      setParsed(response.parsed)
      setConfidence(response.confidence)
      setValues((current) => merge(current, response.parsed!, url))
      notify('Form filled from the posting.', 'success')
    },
  })

  const disabled = !url.trim() || parse.isPending

  return (
    <Panel className="flex flex-col gap-3">
      <div className="flex items-end gap-2">
        <Field label="Autofill from a posting URL" className="flex-1">
          {(props) => (
            <Input
              {...props}
              type="url"
              placeholder="https://…"
              value={url}
              onChange={(event) => setUrl(event.target.value)}
              onKeyDown={(event) => {
                // Inside a form — Enter here must parse, not submit.
                if (event.key === 'Enter') {
                  event.preventDefault()
                  if (!disabled) parse.mutate(url.trim())
                }
              }}
            />
          )}
        </Field>
        <Button
          type="button"
          size="md"
          className="mb-[1px]"
          loading={parse.isPending}
          disabled={disabled}
          onClick={() => parse.mutate(url.trim())}
        >
          Autofill
        </Button>
      </div>

      <FormError error={parse.error} />

      {unavailable && (
        <p className="font-mono text-[10px] leading-relaxed tracking-[0.04em] text-muted">
          {unavailable}
        </p>
      )}

      {parsed && (
        <div className="flex flex-col gap-3 border-t border-rule pt-3">
          <p className="type-meta">
            EXTRACTED · CONFIDENCE {formatPercent(confidence)} · BLANK FIELDS ONLY
          </p>
          {parsed.summary && (
            <p className="font-mono text-[10px] leading-[1.8] tracking-[0.02em] text-ink-soft">
              {parsed.summary}
            </p>
          )}
          {(parsed.seniority || parsed.employmentType) && (
            <div className="flex flex-wrap gap-1.5">
              {parsed.seniority && <Tag>{parsed.seniority}</Tag>}
              {parsed.employmentType && <Tag>{parsed.employmentType}</Tag>}
            </div>
          )}
          {parsed.requiredSkills && parsed.requiredSkills.length > 0 && (
            <div>
              <p className="type-meta mb-2">REQUIRED SKILLS</p>
              <div className="flex flex-wrap gap-1.5">
                {parsed.requiredSkills.map((skill) => (
                  <Tag key={skill}>{skill}</Tag>
                ))}
              </div>
            </div>
          )}
          <p className="font-mono text-[9px] leading-relaxed tracking-[0.06em] text-muted">
            Paste the full job description below — the summary is too short to score against.
          </p>
        </div>
      )}

      {!parsed && !unavailable && values.company === '' && (
        <p className="font-mono text-[9px] leading-relaxed tracking-[0.06em] text-muted">
          Optional. Anything the extractor misses stays yours to fill in.
        </p>
      )}
    </Panel>
  )
}
