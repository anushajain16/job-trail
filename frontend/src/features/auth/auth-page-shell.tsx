import type { ReactNode } from 'react'

/** Centered card shell for /login and /signup — deliberately not
 * <AppLayout>: a signed-out visitor shouldn't see app nav or the backend
 * status badge, just the form. */
export function AuthPageShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-8 bg-background px-4 text-foreground">
      <span className="text-xl font-semibold tracking-tight">JobTrail</span>
      <div className="w-full max-w-sm">{children}</div>
    </div>
  )
}
