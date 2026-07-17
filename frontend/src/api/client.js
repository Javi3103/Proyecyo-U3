const BASE_URL = import.meta.env.VITE_API_BASE_URL

/**
 * Estado de tokens vivo en memoria del proceso del navegador. AuthContext es
 * quien lo puebla (tras login/select-role/refresh) y quien reacciona cuando
 * este módulo detecta que la sesión ya no es recuperable.
 */
let accessToken = null
let refreshToken = null
let onSessionExpired = () => {}

export function setTokens({ accessToken: access, refreshToken: refresh }) {
  accessToken = access ?? accessToken
  refreshToken = refresh ?? refreshToken
}

export function clearTokens() {
  accessToken = null
  refreshToken = null
}

export function setSessionExpiredHandler(handler) {
  onSessionExpired = handler
}

class ApiError extends Error {
  constructor(status, code, message) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function parseErrorBody(response) {
  try {
    const body = await response.json()
    return new ApiError(response.status, body.error, body.message)
  } catch {
    return new ApiError(response.status, 'UNKNOWN', response.statusText)
  }
}

async function rawRequest(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth && accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (response.status === 204) return null
  if (!response.ok) throw await parseErrorBody(response)
  return response.json()
}

/** Único punto que sabe hablar con /api/auth/refresh-token, sin pasar por request() para no reentrar. */
async function refreshAccessToken() {
  if (!refreshToken) throw new ApiError(401, 'NO_REFRESH_TOKEN', 'No hay sesión que renovar.')
  const data = await rawRequest('/api/auth/refresh-token', {
    method: 'POST',
    body: { refreshToken },
    auth: false,
  })
  setTokens(data)
  return data
}

/**
 * Wrapper de fetch para el resto de la app: adjunta el Bearer token y, ante
 * un 401 en un endpoint protegido, intenta refrescar la sesión una sola vez
 * antes de rendirse y forzar logout.
 */
export async function request(path, options = {}) {
  try {
    return await rawRequest(path, options)
  } catch (error) {
    const isAuthEndpoint = path.startsWith('/api/auth/') || path.startsWith('/api/internals/')
    if (error.status === 401 && options.auth !== false && !isAuthEndpoint) {
      try {
        await refreshAccessToken()
        return await rawRequest(path, options)
      } catch {
        clearTokens()
        onSessionExpired()
      }
    }
    throw error
  }
}

export { ApiError }
