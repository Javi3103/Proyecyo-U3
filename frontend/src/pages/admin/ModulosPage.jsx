import { useEffect, useState } from 'react'
import * as modulosApi from '../../api/modulos'

export default function ModulosPage() {
  const [modulos, setModulos] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')

  async function cargar() {
    setLoading(true)
    setError(null)
    try {
      setModulos(await modulosApi.listarModulos())
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    cargar()
  }, [])

  async function crear(e) {
    e.preventDefault()
    if (!nombre.trim()) return
    try {
      await modulosApi.crearModulo(nombre.trim(), descripcion.trim() || null)
      setNombre('')
      setDescripcion('')
      await cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  async function eliminar(id) {
    if (!window.confirm('¿Eliminar este módulo?')) return
    try {
      await modulosApi.eliminarModulo(id)
      await cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="space-y-6">
      <div className="rounded-lg border border-slate-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-slate-800">Módulos</h2>
        <p className="mt-1 text-sm text-slate-500">Unidades funcionales del sistema (ej. Ventas, Recursos Humanos).</p>

        <form onSubmit={crear} className="mt-4 flex flex-wrap gap-2">
          <input
            className="flex-1 rounded border border-slate-300 px-3 py-1.5 text-sm"
            placeholder="Nombre del módulo"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
          />
          <input
            className="flex-1 rounded border border-slate-300 px-3 py-1.5 text-sm"
            placeholder="Descripción (opcional)"
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
          />
          <button className="rounded bg-slate-900 px-4 py-1.5 text-sm font-medium text-white hover:bg-slate-700">
            Crear
          </button>
        </form>

        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      </div>

      <div className="rounded-lg border border-slate-200 bg-white">
        {loading ? (
          <p className="p-6 text-sm text-slate-500">Cargando...</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {modulos.map((m) => (
              <li key={m.id} className="flex items-center justify-between p-4">
                <div>
                  <p className="font-medium text-slate-800">{m.nombre}</p>
                  {m.descripcion && <p className="text-sm text-slate-500">{m.descripcion}</p>}
                </div>
                <button
                  onClick={() => eliminar(m.id)}
                  className="rounded border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                >
                  Eliminar
                </button>
              </li>
            ))}
            {modulos.length === 0 && <li className="p-6 text-sm text-slate-500">No hay módulos todavía.</li>}
          </ul>
        )}
      </div>
    </div>
  )
}
