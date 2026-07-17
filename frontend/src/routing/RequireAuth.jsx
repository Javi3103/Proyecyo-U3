import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

export default function RequireAuth({ children }) {
  const { isAuthenticated, bootstrapping } = useAuth()

  if (bootstrapping) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-slate-500">
        Cargando sesión...
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return children
}
