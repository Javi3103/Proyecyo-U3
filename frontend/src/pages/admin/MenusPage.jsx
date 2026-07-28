import { useEffect, useState } from 'react'
import * as modulosApi from '../../api/modulos'
import * as menuApi from '../../api/menu'

export default function MenusPage() {
  const [modulos, setModulos] = useState([])
  const [moduloId, setModuloId] = useState('')
  const [menus, setMenus] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [nombre, setNombre] = useState('')
  const [url, setUrl] = useState('')
  const [parentId, setParentId] = useState('')

  useEffect(() => {
    modulosApi.listarModulos().then(setModulos).catch((e) => setError(e.message))
  }, [])

  async function cargarMenus(id) {
    if (!id) {
      setMenus([])
      return
    }
    setLoading(true)
    setError(null)
    try {
      setMenus(await menuApi.listarMenusPorModulo(id))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    cargarMenus(moduloId)
  }, [moduloId])

  async function crear(e) {
    e.preventDefault()
    if (!nombre.trim() || !moduloId) return
    try {
      await menuApi.crearMenu({ nombre: nombre.trim(), url: url.trim() || null, moduloId, parentId: parentId || null })
      setNombre('')
      setUrl('')
      setParentId('')
      await cargarMenus(moduloId)
    } catch (e) {
      setError(e.message)
    }
  }

  async function eliminar(id) {
    if (!window.confirm('¿Eliminar este ítem de menú?')) return
    try {
      await menuApi.eliminarMenu(id)
      await cargarMenus(moduloId)
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="space-y-6">
      <div className="rounded-lg border border-slate-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-slate-800">Menús y Submenús</h2>
        <p className="mt-1 text-sm text-slate-500">
          Estructura recursiva (Adjacency List): elige un módulo y crea menús principales, submenús o ítems finales dentro de él.
        </p>

        <div className="mt-4">
          <label className="text-xs font-medium text-slate-500">Módulo</label>
          <select
            className="mt-1 block w-full max-w-xs rounded border border-slate-300 px-3 py-1.5 text-sm"
            value={moduloId}
            onChange={(e) => setModuloId(e.target.value)}
          >
            <option value="">Elige un módulo</option>
            {modulos.map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre}
              </option>
            ))}
          </select>
        </div>

        {moduloId && (
          <form onSubmit={crear} className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-4">
            <input
              className="rounded border border-slate-300 px-3 py-1.5 text-sm"
              placeholder="Nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
            />
            <input
              className="rounded border border-slate-300 px-3 py-1.5 text-sm"
              placeholder="URL (solo si es ítem final)"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
            <select
              className="rounded border border-slate-300 px-3 py-1.5 text-sm"
              value={parentId}
              onChange={(e) => setParentId(e.target.value)}
            >
              <option value="">Sin padre (Menú Principal)</option>
              {menus.map((mn) => (
                <option key={mn.id} value={mn.id}>
                  {mn.nombre}
                </option>
              ))}
            </select>
            <button className="rounded bg-slate-900 px-4 py-1.5 text-sm font-medium text-white hover:bg-slate-700">
              Crear
            </button>
          </form>
        )}

        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      </div>

      {moduloId && (
        <div className="rounded-lg border border-slate-200 bg-white">
          {loading ? (
            <p className="p-6 text-sm text-slate-500">Cargando...</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {menus.map((mn) => (
                <li key={mn.id} className="flex items-center justify-between p-4">
                  <div>
                    <p className="font-medium text-slate-800">
                      {mn.nombre}{' '}
                      <span className="text-xs font-normal text-slate-400">
                        ({mn.parentId ? 'submenú/ítem' : 'principal'})
                      </span>
                    </p>
                    {mn.url && <p className="text-sm text-slate-500">{mn.url}</p>}
                  </div>
                  <button
                    onClick={() => eliminar(mn.id)}
                    className="rounded border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                  >
                    Eliminar
                  </button>
                </li>
              ))}
              {menus.length === 0 && <li className="p-6 text-sm text-slate-500">Este módulo no tiene menús todavía.</li>}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
