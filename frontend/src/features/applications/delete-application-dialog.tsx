import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { useDeleteApplicationMutation } from '@/features/applications/hooks'
import type { Application } from '@/features/applications/types'

interface DeleteApplicationDialogProps {
  application: Application | null
  onOpenChange: (open: boolean) => void
}

export function DeleteApplicationDialog({ application, onOpenChange }: DeleteApplicationDialogProps) {
  const deleteMutation = useDeleteApplicationMutation()

  function handleConfirm() {
    if (!application) return
    deleteMutation.mutate(application.id, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <AlertDialog open={application !== null} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete application?</AlertDialogTitle>
          <AlertDialogDescription>
            {application && (
              <>
                This removes {application.role} at {application.company} and its status history. This can't be
                undone.
              </>
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={deleteMutation.isPending}>Cancel</AlertDialogCancel>
          <AlertDialogAction onClick={handleConfirm} disabled={deleteMutation.isPending}>
            {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
