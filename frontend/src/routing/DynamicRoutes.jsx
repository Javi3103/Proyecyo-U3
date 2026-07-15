import { Routes, Route } from 'react-router-dom'
import { buildRoutesFromMenu } from './buildRoutesFromMenu'
import MenuLeafPage from '../pages/MenuLeafPage.jsx'
import { useAuth } from '../auth/AuthContext.jsx'

/**
 * Registra una <Route> por cada item hoja del árbol de menú -- nada
 * hardcodeado, todo sale del JSON que devolvió el backend (OE del PDF,
 * sección 5.4: "el Frontend no debe tener las rutas hardcodeadas").
 */
export default function DynamicRoutes({ tree }) {
  const { rolNombre } = useAuth()
  const leaves = buildRoutesFromMenu(tree)

  return (
    <Routes>
      <Route
        index
        element={
          <div className="rounded-lg border border-slate-200 bg-white p-6">
            <h2 className="text-lg font-semibold text-slate-800">Bienvenido</h2>
            <p className="mt-2 text-sm text-slate-500">
              Sesión activa con el rol <span className="font-medium">{rolNombre}</span>. Elige una
              opción del menú lateral para continuar.
            </p>
          </div>
        }
      />
      {leaves.map((leaf) => (
        <Route
          key={leaf.id}
          path={leaf.path.replace(/^\//, '')}
          element={<MenuLeafPage node={leaf} />}
        />
      ))}
      <Route
        path="*"
        element={<p className="text-sm text-slate-500">No se encontró esa sección del menú.</p>}
      />
    </Routes>
  )
}
