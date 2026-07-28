import { useEffect, useState } from 'react'
import * as rolesApi from '../../api/roles'
import * as modulosApi from '../../api/modulos'
import * as usuariosApi from '../../api/usuarios'
import * as menuApi from '../../api/menu'

export default function RolesPage() {
  const [roles, setRoles] = useState([])
  const [modulos, setModulos] = useState([])
  const [usuarios, setUsuarios] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [nuevoNombre, setNuevoNombre] = useState('')
  const [nuevaDescripcion, setNuevaDescripcion] = useState('')
  const [expandedId, setExpandedId] = useState(null)

  async function cargarTodo() {
    setLoading(true)
    setError(null)
    try {
      const [rolesData, modulosData, usuariosData] = await Promise.all([
        rolesApi.listarRoles(),
        modulosApi.listarModulos(),
        usuariosApi.listarUsuarios(),
      ])
      setRoles(rolesData)
      setModulos(modulosData)
      setUsuarios(usuariosData)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    cargarTodo()
  }, [])

  async function crearRol(e) {
    e.preventDefault()
    if (!nuevoNombre.trim()) return
    try {
      await rolesApi.crearRol(nuevoNombre.trim(), nuevaDescripcion.trim() || null)
      setNuevoNombre('')
      setNuevaDescripcion('')
      await cargarTodo()
    } catch (e) {
      setError(e.message)
    }
  }

  async function eliminarRol(id) {
    if (!window.confirm('¿Eliminar este rol?')) return
    try {
      await rolesApi.eliminarRol(id)
      await cargarTodo()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="space-y-6">
      <div className="rounded-lg border border-slate-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-slate-800">Roles</h2>
        <p className="mt-1 text-sm text-slate-500">Crea roles y asígnales usuarios, módulos y menús.</p>

        <form onSubmit={crearRol} className="mt-4 flex flex-wrap gap-2">
          <input
            className="flex-1 rounded border border-slate-300 px-3 py-1.5 text-sm"
            placeholder="Nombre del rol"
            value={nuevoNombre}
            onChange={(e) => setNuevoNombre(e.target.value)}
          />
          <input
            className="flex-1 rounded border border-slate-300 px-3 py-1.5 text-sm"
            placeholder="Descripción (opcional)"
            value={nuevaDescripcion}
            onChange={(e) => setNuevaDescripcion(e.target.value)}
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
            {roles.map((rol) => (
              <RolRow
                key={rol.id}
                rol={rol}
                modulos={modulos}
                usuarios={usuarios}
                expanded={expandedId === rol.id}
                onToggle={() => setExpandedId(expandedId === rol.id ? null : rol.id)}
                onEliminar={() => eliminarRol(rol.id)}
                onError={setError}
              />
            ))}
            {roles.length === 0 && <li className="p-6 text-sm text-slate-500">No hay roles todavía.</li>}
          </ul>
        )}
      </div>
    </div>
  )
}

function RolRow({ rol, modulos, usuarios, expanded, onToggle, onEliminar, onError }) {
  const [usuarioId, setUsuarioId] = useState('')
  const [moduloId, setModuloId] = useState('')
  const [menuId, setMenuId] = useState('')
  const [menusDelModulo, setMenusDelModulo] = useState([])
  const [ok, setOk] = useState('')

  useEffect(() => {
    if (!moduloId) {
      setMenusDelModulo([])
      return
    }
    menuApi
      .listarMenusPorModulo(moduloId)
      .then(setMenusDelModulo)
      .catch((e) => onError(e.message))
  }, [moduloId])

  async function asignarUsuario() {
    if (!usuarioId) return
    try {
      await rolesApi.asignarUsuario(rol.id, usuarioId)
      setOk('Usuario asignado.')
      setUsuarioId('')
    } catch (e) {
      onError(e.message)
    }
  }

  async function asignarModulo() {
    if (!moduloId) return
    try {
      await rolesApi.asignarModulo(rol.id, moduloId)
      setOk('Módulo asignado.')
    } catch (e) {
      onError(e.message)
    }
  }

  async function asignarMenu() {
    if (!menuId) return
    try {
      await rolesApi.asignarMenu(rol.id, menuId)
      setOk('Menú asignado.')
      setMenuId('')
    } catch (e) {
      onError(e.message)
    }
  }

  return (
    <li className="p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="font-medium text-slate-800">{rol.nombre}</p>
          {rol.descripcion && <p className="text-sm text-slate-500">{rol.descripcion}</p>}
        </div>
        <div className="flex gap-2">
          <button
            onClick={onToggle}
            className="rounded border border-slate-300 px-3 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50"
          >
            {expanded ? 'Ocultar' : 'Asignar'}
          </button>
          <button
            onClick={onEliminar}
            className="rounded border border-red-200 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
          >
            Eliminar
          </button>
        </div>
      </div>

      {expanded && (
        <div className="mt-4 grid grid-cols-1 gap-3 rounded bg-slate-50 p-4 sm:grid-cols-3">
          <div>
            <label className="text-xs font-medium text-slate-500">Asignar usuario</label>
            <div className="mt-1 flex gap-2">
              <select
                className="flex-1 rounded border border-slate-300 px-2 py-1 text-sm"
                value={usuarioId}
                onChange={(e) => setUsuarioId(e.target.value)}
              >
                <option value="">Elige un usuario</option>
                {usuarios.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.email}
                  </option>
                ))}
              </select>
              <button onClick={asignarUsuario} className="rounded bg-slate-900 px-2 py-1 text-xs text-white">
                OK
              </button>
            </div>
          </div>

          <div>
            <label className="text-xs font-medium text-slate-500">Asignar módulo</label>
            <div className="mt-1 flex gap-2">
              <select
                className="flex-1 rounded border border-slate-300 px-2 py-1 text-sm"
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
              <button onClick={asignarModulo} className="rounded bg-slate-900 px-2 py-1 text-xs text-white">
                OK
              </button>
            </div>
          </div>

          <div>
            <label className="text-xs font-medium text-slate-500">Asignar menú (del módulo elegido)</label>
            <div className="mt-1 flex gap-2">
              <select
                className="flex-1 rounded border border-slate-300 px-2 py-1 text-sm"
                value={menuId}
                onChange={(e) => setMenuId(e.target.value)}
                disabled={!moduloId}
              >
                <option value="">Elige un menú</option>
                {menusDelModulo.map((mn) => (
                  <option key={mn.id} value={mn.id}>
                    {mn.nombre}
                    {mn.url ? ` (${mn.url})` : ''}
                  </option>
                ))}
              </select>
              <button
                onClick={asignarMenu}
                className="rounded bg-slate-900 px-2 py-1 text-xs text-white"
                disabled={!moduloId}
              >
                OK
              </button>
            </div>
          </div>

          {ok && <p className="text-xs text-emerald-600 sm:col-span-3">{ok}</p>}
        </div>
      )}
    </li>
  )
}
