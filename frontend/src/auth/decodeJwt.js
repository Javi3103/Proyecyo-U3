/**
 * Decodifica el payload de un JWT SOLO para lectura en la UI (rolNombre,
 * permisos). No verifica la firma -- esa validación ya la hizo el backend;
 * esto es puramente para no volver a pedirle esa info al servidor.
 */
export function decodeJwtPayload(token) {
  const payload = token.split('.')[1]
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
  const json = decodeURIComponent(
    atob(base64)
      .split('')
      .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
      .join(''),
  )
  return JSON.parse(json)
}
