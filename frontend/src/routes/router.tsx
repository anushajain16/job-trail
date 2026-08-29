import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '@/components/layout/app-layout'
import { LoginPage } from '@/features/auth/login-page'
import { ProtectedRoute, PublicOnlyRoute } from '@/features/auth/protected-route'
import { SignupPage } from '@/features/auth/signup-page'
import { DashboardPage } from '@/pages/dashboard-page'

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
        children: [{ index: true, element: <DashboardPage /> }],
      },
    ],
  },
])
