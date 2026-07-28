import { request } from './client'

export function listarRoles() {
  return request('/api/roles')
}

export function crearRol(nombre, descripcion) {
  return request('/api/roles', { method: 'POST', body: { nombre, descripcion } })
}

export function actualizarRol(id, nombre, descripcion) {
  return request(`/api/roles/${id}`, { method: 'PUT', body: { nombre, descripcion } })
}

export function eliminarRol(id) {
  return request(`/api/roles/${id}`, { method: 'DELETE' })
}

export function asignarUsuario(rolId, usuarioId) {
  return request(`/api/roles/${rolId}/users`, { method: 'POST', body: { usuarioId } })
}

export function desasignarUsuario(rolId, usuarioId) {
  return request(`/api/roles/${rolId}/users/${usuarioId}`, { method: 'DELETE' })
}

export function asignarModulo(rolId, moduloId) {
  return request(`/api/roles/${rolId}/modules`, { method: 'POST', body: { moduloId } })
}

export function asignarMenu(rolId, menuId) {
  return request(`/api/roles/${rolId}/menus`, { method: 'POST', body: { menuId } })
}
