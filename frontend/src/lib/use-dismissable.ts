import { useEffect } from 'react'

/**
 * Escape-to-close plus body scroll lock, shared by the drawer and the
 * dialog so overlay behaviour is defined once.
 */
export function useDismissable(open: boolean, onDismiss: () => void) {
  useEffect(() => {
    if (!open) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onDismiss()
    }
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open, onDismiss])
}
