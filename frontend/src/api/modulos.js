import { request } from './client'

export function listarModulos() {
  return request('/api/modules')
}

export function crearModulo(nombre, descripcion) {
  return request('/api/modules', { method: 'POST', body: { nombre, descripcion } })
}

export function actualizarModulo(id, nombre, descripcion) {
  return request(`/api/modules/${id}`, { method: 'PUT', body: { nombre, descripcion } })
}

export function eliminarModulo(id) {
  return request(`/api/modules/${id}`, { method: 'DELETE' })
}
