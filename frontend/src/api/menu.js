import { request } from './client'

export function getMenuTree() {
  return request('/api/menus/tree')
}

/** Lista plana (no el árbol) de los menús de un módulo específico. */
export function listarMenusPorModulo(moduloId) {
  return request(`/api/menus?moduloId=${moduloId}`)
}

export function crearMenu({ nombre, url, moduloId, parentId }) {
  return request('/api/menus', {
    method: 'POST',
    body: { nombre, url: url || null, moduloId, parentId: parentId || null },
  })
}

export function actualizarMenu(id, { nombre, url, parentId }) {
  return request(`/api/menus/${id}`, {
    method: 'PUT',
    body: { nombre, url: url || null, parentId: parentId || null },
  })
}

export function eliminarMenu(id) {
  return request(`/api/menus/${id}`, { method: 'DELETE' })
}
