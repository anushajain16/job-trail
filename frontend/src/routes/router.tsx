import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppLayout } from '@/components/layout/app-layout'
import { GitHubCallbackPage } from '@/features/auth/github-callback-page'
import { LoginPage } from '@/features/auth/login-page'
import { ProtectedRoute, PublicOnlyRoute } from '@/features/auth/route-guards'
import { SignupPage } from '@/features/auth/signup-page'
import { ApplicationsPage } from '@/features/applications/applications-page'
import { MapPage } from '@/features/map/map-page'
import { DocumentsPage } from '@/features/documents/documents-page'
import { AnalyticsPage } from '@/features/analytics/analytics-page'
import { SettingsPage } from './settings-page'
import { NotFoundPage } from './not-found-page'
import { RootLayout } from './root-layout'

/** Route table. Feature routes replace their placeholders in Stages 3–5. */
export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      // Public: the OAuth landing pad has to run with or without a session.
      { path: '/auth/callback/github', element: <GitHubCallbackPage /> },
      {
        element: <PublicOnlyRoute />,
        children: [
          { path: '/login', element: <LoginPage /> },
          { path: '/signup', element: <SignupPage /> },
        ],
      },
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: '/',
            element: <AppLayout />,
            children: [
              { index: true, element: <Navigate to="/map" replace /> },
              { path: 'map', element: <MapPage /> },
              { path: 'applications', element: <ApplicationsPage /> },
              { path: 'documents', element: <DocumentsPage /> },
              { path: 'analytics', element: <AnalyticsPage /> },
              { path: 'settings', element: <SettingsPage /> },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
])
