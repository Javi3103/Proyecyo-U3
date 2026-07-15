import { NavLink } from 'react-router-dom'

function MenuNode({ node, depth }) {
  const isLink = !!node.url

  if (isLink) {
    return (
      <NavLink
        to={`/app${node.url}`}
        className={({ isActive }) =>
          `block rounded px-3 py-1.5 text-sm ${
            isActive ? 'bg-slate-700 text-white' : 'text-slate-300 hover:bg-slate-800 hover:text-white'
          }`
        }
        style={{ paddingLeft: `${0.75 + depth * 0.75}rem` }}
      >
        {node.nombre}
      </NavLink>
    )
  }

  return (
    <div>
      <p
        className="px-3 py-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500"
        style={{ paddingLeft: `${0.75 + depth * 0.75}rem` }}
      >
        {node.nombre}
      </p>
      {node.hijos?.map((child) => <MenuNode key={child.id} node={child} depth={depth + 1} />)}
    </div>
  )
}

/** Renderiza el árbol de menú tal cual llega del backend -- sin rutas fijas de por medio. */
export default function Sidebar({ tree }) {
  return (
    <nav className="flex w-64 shrink-0 flex-col gap-1 bg-slate-900 p-3">
      {tree.map((node) => (
        <MenuNode key={node.id} node={node} depth={0} />
      ))}
    </nav>
  )
}
