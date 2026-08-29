import type { LineStatus } from '@/features/subway-map/layout'

// validate_palette.js flags offer-green vs rejected-red as a real deutan
// confusion (ΔE 4.1, below the categorical CVD floor) — expected for a
// red/green status pair, and not fixable by re-hueing without losing the
// "good outcome vs bad outcome" association these colors carry everywhere
// else in the app. Mitigated the way the skill prescribes for a status
// pair: never color alone — every mark also carries a distinct shape
// (circle vs diamond) and a text label (badge, legend, exit-marker label,
// tooltip), in the chart and in the accessible table view alike.
export function statusColor(status: LineStatus): string {
  switch (status) {
    case 'active':
      return 'var(--subway-active)'
    case 'offer':
      return 'var(--subway-offer)'
    case 'rejected':
      return 'var(--subway-rejected)'
    case 'ghosted':
      return 'var(--subway-ghosted)'
  }
}
