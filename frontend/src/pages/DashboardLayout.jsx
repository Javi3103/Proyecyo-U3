import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { getMenuTree } from '../api/menu'
import Sidebar from '../components/Sidebar.jsx'
import DynamicRoutes from '../routing/DynamicRoutes.jsx'

export default function DashboardLayout() {
  const { rolNombre, logout } = useAuth()
  const navigate = useNavigate()
  const [tree, setTree] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    getMenuTree()
      .then(setTree)
      .catch(() => setError('No se pudo cargar el menú.'))
  }, [])

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen bg-slate-100">
      {tree && <Sidebar tree={tree} />}

      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
          <span className="text-sm text-slate-500">
            Rol activo: <span className="font-medium text-slate-800">{rolNombre}</span>
          </span>
          <button
            onClick={handleLogout}
            className="rounded border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            Cerrar sesión
          </button>
        </header>

        <main className="flex-1 p-6">
          {error && <p className="text-sm text-red-600">{error}</p>}
          {tree && <DynamicRoutes tree={tree} />}
        </main>
      </div>
    </div>
  )
}
