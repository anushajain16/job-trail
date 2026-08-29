import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '@/components/layout/app-layout'
import { LoginPage } from '@/features/auth/login-page'
import { ProtectedRoute, PublicOnlyRoute } from '@/features/auth/protected-route'
import { SignupPage } from '@/features/auth/signup-page'
import { ApplicationCreatePage } from '@/pages/application-create-page'
import { ApplicationEditPage } from '@/pages/application-edit-page'
import { AnalyticsPage } from '@/pages/analytics-page'
import { ApplicationsListPage } from '@/pages/applications-list-page'
import { DashboardPage } from '@/pages/dashboard-page'
import { SubwayMapPage } from '@/pages/subway-map-page'

export const router = createBrowserRouter([
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
          { index: true, element: <DashboardPage /> },
          { path: 'map', element: <SubwayMapPage /> },
          { path: 'analytics', element: <AnalyticsPage /> },
          { path: 'applications', element: <ApplicationsListPage /> },
          { path: 'applications/new', element: <ApplicationCreatePage /> },
          { path: 'applications/:id/edit', element: <ApplicationEditPage /> },
        ],
      },
    ],
  },
])
