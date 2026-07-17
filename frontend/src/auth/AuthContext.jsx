import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import * as authApi from '../api/auth'
import { setTokens, clearTokens, setSessionExpiredHandler } from '../api/client'
import { decodeJwtPayload } from './decodeJwt'

const REFRESH_TOKEN_STORAGE_KEY = 'mg_refresh_token'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null)
  const [tempToken, setTempToken] = useState(null)
  const [pendingRoles, setPendingRoles] = useState([])
  const [bootstrapping, setBootstrapping] = useState(true)

  const persistSession = useCallback((tokens) => {
    setTokens(tokens)
    setAccessToken(tokens.accessToken)
    localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokens.refreshToken)
  }, [])

  const clearSession = useCallback(() => {
    clearTokens()
    setAccessToken(null)
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY)
  }, [])

  // Al arrancar la SPA: si hay un refresh token guardado, intenta reautenticar
  // en silencio antes de mostrar cualquier pantalla.
  useEffect(() => {
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)
    if (!storedRefreshToken) {
      setBootstrapping(false)
      return
    }

    authApi
      .refreshToken(storedRefreshToken)
      .then(persistSession)
      .catch(clearSession)
      .finally(() => setBootstrapping(false))
  }, [persistSession, clearSession])

  // Si una llamada protegida cualquiera agota el refresh automático, cae aquí.
  useEffect(() => {
    setSessionExpiredHandler(clearSession)
  }, [clearSession])

  const login = useCallback(async (email, password) => {
    const data = await authApi.login(email, password)
    setTempToken(data.tempToken)
    setPendingRoles(data.roles)
    return data.roles
  }, [])

  const selectRole = useCallback(
    async (roleId) => {
      const tokens = await authApi.selectRole(tempToken, roleId)
      persistSession(tokens)
      setTempToken(null)
      setPendingRoles([])
    },
    [tempToken, persistSession],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }, [clearSession])

  const claims = useMemo(() => (accessToken ? decodeJwtPayload(accessToken) : null), [accessToken])

  const value = useMemo(
    () => ({
      isAuthenticated: !!accessToken,
      bootstrapping,
      tempToken,
      pendingRoles,
      rolId: claims?.rolId ?? null,
      rolNombre: claims?.rolNombre ?? null,
      permisos: claims?.permisos ?? [],
      login,
      selectRole,
      logout,
    }),
    [accessToken, bootstrapping, tempToken, pendingRoles, claims, login, selectRole, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe usarse dentro de un AuthProvider')
  return context
}
