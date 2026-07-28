import { request } from './client'

/** GET /api/users devuelve una Page de Spring Data — solo interesa el contenido. */
export function listarUsuarios() {
  return request('/api/users').then((page) => page.content ?? page)
}
