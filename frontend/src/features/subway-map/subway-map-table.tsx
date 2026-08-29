import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { STAGE_LABELS, type Application } from '@/features/applications/types'
import { categorizeStage, STATUS_LABELS } from '@/features/subway-map/layout'

/** The accessibility-pass table view (dataviz skill: "a table view exists")
 * — same filtered rows as the chart, in plain text, no SVG. */
export function SubwayMapTable({ applications }: { applications: Application[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Company</TableHead>
          <TableHead>Role</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Current stage</TableHead>
          <TableHead>Last updated</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {applications.map((application) => (
          <TableRow key={application.id}>
            <TableCell className="font-medium">{application.company}</TableCell>
            <TableCell>{application.role}</TableCell>
            <TableCell>
              <Badge variant="secondary">{STATUS_LABELS[categorizeStage(application.currentStage)]}</Badge>
            </TableCell>
            <TableCell>{STAGE_LABELS[application.currentStage]}</TableCell>
            <TableCell className="text-muted-foreground">
              {new Date(application.updatedAt).toLocaleDateString()}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
