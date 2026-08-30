import { useNavigate } from 'react-router-dom'
import { ExportCsvButton } from '@/components/export-csv-button'
import { Button } from '@/components/ui/button'
import { DataPoint, Panel, PageHeader, SectionHeading } from '@/components/ui/panel'
import { useToast } from '@/components/ui/toast'
import { useAuth } from '@/features/auth/auth-context'
import { exportApplicationsCsv } from '@/features/applications/api'
import { CalendarConnectionPanel } from '@/features/google-calendar/calendar-connection-panel'
import { exportInterviewsCsv } from '@/features/interviews/api'

/** Account, integrations and data export. */
export function SettingsPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { notifyError } = useToast()

  const signOut = async () => {
    try {
      await logout()
      navigate('/login', { replace: true })
    } catch (error) {
      notifyError(error)
    }
  }

  return (
    <>
      <PageHeader title="Settings" meta="ACCOUNT · INTEGRATIONS · DATA" />

      <div className="flex max-w-3xl flex-col gap-12">
        <section>
          <SectionHeading weight="heavy">Account</SectionHeading>
          <Panel className="mt-4 flex flex-wrap items-center justify-between gap-4">
            <DataPoint label="SIGNED IN AS">{user?.email ?? '—'}</DataPoint>
            <Button size="sm" onClick={signOut}>
              Sign out
            </Button>
          </Panel>
        </section>

        <CalendarConnectionPanel />

        <section>
          <SectionHeading weight="heavy">Export</SectionHeading>
          <Panel className="mt-4 flex flex-wrap items-center justify-between gap-4">
            <p className="max-w-md font-mono text-[10px] leading-relaxed tracking-[0.04em] text-ink-soft">
              Download everything you own as CSV — every application, and every interview round
              across all of them.
            </p>
            <div className="flex gap-2">
              <ExportCsvButton fetcher={exportApplicationsCsv} label="Applications" />
              <ExportCsvButton fetcher={exportInterviewsCsv} label="Interviews" />
            </div>
          </Panel>
        </section>
      </div>
    </>
  )
}
