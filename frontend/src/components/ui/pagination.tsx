import { Button } from './button'

export interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  onChange: (page: number) => void
}

/** Page N OF M, with prev/next. Zero-indexed like Spring Data. */
export function Pagination({ page, totalPages, totalElements, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return (
      <div className="flex justify-end pt-4">
        <span className="type-meta">{totalElements} TOTAL</span>
      </div>
    )
  }

  return (
    <div className="flex items-center justify-between pt-4">
      <span className="type-meta">
        PAGE {page + 1} OF {totalPages} · {totalElements} TOTAL
      </span>
      <div className="flex gap-2">
        <Button size="sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
          Prev
        </Button>
        <Button size="sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
          Next
        </Button>
      </div>
    </div>
  )
}
