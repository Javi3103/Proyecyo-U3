import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { friendlyErrorMessage } from '../api/errorMessages'

/**
 * OE3 del PDF: tras el login clásico el usuario NUNCA cae directo al
 * dashboard -- debe elegir explícitamente con qué rol quiere operar en esta
 * sesión. Sin un tempToken vivo (ej. si alguien navega aquí directo) no hay
 * nada que seleccionar, así que se manda de vuelta a /login.
 */
export default function WorkspaceSelectorPage() {
  const { tempToken, pendingRoles, selectRole } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState(null)
  const [selectingId, setSelectingId] = useState(null)

  if (!tempToken) {
    return <Navigate to="/login" replace />
  }

  async function handleSelect(roleId) {
    setError(null)
    setSelectingId(roleId)
    try {
      await selectRole(roleId)
      navigate('/app')
    } catch (err) {
      setError(friendlyErrorMessage(err))
    } finally {
      setSelectingId(null)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100">
      <div className="w-full max-w-md rounded-lg bg-white p-8 shadow-md">
        <h1 className="mb-1 text-2xl font-semibold text-slate-800">Espacio de trabajo</h1>
        <p className="mb-6 text-sm text-slate-500">Elige con qué rol quieres trabajar en esta sesión</p>

        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

        <div className="flex flex-col gap-2">
          {pendingRoles.map((rol) => (
            <button
              key={rol.id}
              onClick={() => handleSelect(rol.id)}
              disabled={selectingId !== null}
              className="rounded border border-slate-300 px-4 py-3 text-left font-medium text-slate-800 hover:border-slate-500 hover:bg-slate-50 disabled:opacity-50"
            >
              {selectingId === rol.id ? 'Entrando...' : rol.nombre}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
