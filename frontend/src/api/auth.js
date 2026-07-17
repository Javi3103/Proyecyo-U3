import { request } from './client'

export function login(email, password) {
  return request('/api/auth/login', { method: 'POST', body: { email, password }, auth: false })
}

export function selectRole(tempToken, roleId) {
  return request('/api/auth/select-role', { method: 'POST', body: { tempToken, roleId }, auth: false })
}

export function refreshToken(refreshTokenValue) {
  return request('/api/auth/refresh-token', {
    method: 'POST',
    body: { refreshToken: refreshTokenValue },
    auth: false,
  })
}

export function logout() {
  return request('/api/auth/logout', { method: 'POST' })
}
