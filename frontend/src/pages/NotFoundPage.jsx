import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-2 bg-slate-100">
      <h1 className="text-2xl font-semibold text-slate-800">404</h1>
      <p className="text-sm text-slate-500">Esta página no existe.</p>
      <Link to="/login" className="text-sm text-slate-700 underline">
        Volver al inicio
      </Link>
    </div>
  )
}
