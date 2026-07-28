import { Routes, Route } from 'react-router-dom'
import { buildRoutesFromMenu } from './buildRoutesFromMenu'
import MenuLeafPage from '../pages/MenuLeafPage.jsx'
import RolesPage from '../pages/admin/RolesPage.jsx'
import ModulosPage from '../pages/admin/ModulosPage.jsx'
import MenusPage from '../pages/admin/MenusPage.jsx'
import { useAuth } from '../auth/AuthContext.jsx'

/**
 * Pantallas reales para ciertas URLs administrativas conocidas — el resto de
 * items hoja del menú (ej. un futuro "Ventas") sigue cayendo en el placeholder
 * genérico MenuLeafPage. La ruta en sí sigue siendo 100% dinámica (solo
 * existe si el backend la incluyó en el árbol de menú del rol); esto solo
 * decide qué componente renderizar para una URL ya presente en ese árbol.
 */
const PAGINAS_ADMIN = {
  '/admin/roles': RolesPage,
  '/admin/modulos': ModulosPage,
  '/admin/menus': MenusPage,
}

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
      {leaves.map((leaf) => {
        const PaginaAdmin = PAGINAS_ADMIN[leaf.path]
        return (
          <Route
            key={leaf.id}
            path={leaf.path.replace(/^\//, '')}
            element={PaginaAdmin ? <PaginaAdmin /> : <MenuLeafPage node={leaf} />}
          />
        )
      })}
      <Route
        path="*"
        element={<p className="text-sm text-slate-500">No se encontró esa sección del menú.</p>}
      />
    </Routes>
  )
}
