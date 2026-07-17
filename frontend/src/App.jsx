import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage.jsx'
import WorkspaceSelectorPage from './pages/WorkspaceSelectorPage.jsx'
import DashboardLayout from './pages/DashboardLayout.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import RequireAuth from './routing/RequireAuth.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/select-role" element={<WorkspaceSelectorPage />} />
      <Route
        path="/app/*"
        element={
          <RequireAuth>
            <DashboardLayout />
          </RequireAuth>
        }
      />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
