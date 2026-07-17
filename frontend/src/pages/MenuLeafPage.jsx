/**
 * Placeholder genérico para cualquier item hoja del menú. En el sistema
 * completo, esta URL apunta a un microservicio hijo real (ej. Ventas); como
 * esos microservicios todavía no existen, esto solo confirma que el
 * enrutamiento dinámico basado en el JSON del menú funciona de punta a punta.
 */
export default function MenuLeafPage({ node }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-6">
      <h2 className="text-lg font-semibold text-slate-800">{node.nombre}</h2>
      <p className="mt-2 text-sm text-slate-500">
        Ruta registrada dinámicamente desde el menú: <code className="rounded bg-slate-100 px-1.5 py-0.5">{node.path}</code>
      </p>
      <p className="mt-1 text-sm text-slate-400">
        El microservicio destino todavía no está integrado.
      </p>
    </div>
  )
}
