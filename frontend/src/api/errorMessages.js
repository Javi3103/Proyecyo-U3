const MESSAGES = {
  INVALID_CREDENTIALS: 'Correo o contraseña incorrectos.',
  INVALID_TOKEN: 'Tu sesión no es válida. Inicia sesión de nuevo.',
  ROLE_NOT_ASSIGNED: 'No tienes ese rol asignado.',
  TOO_MANY_ATTEMPTS: 'Demasiados intentos. Espera un momento antes de volver a intentar.',
  VALIDATION_ERROR: 'Revisa los datos ingresados.',
  UNAUTHORIZED: 'Tu sesión expiró. Inicia sesión de nuevo.',
}

export function friendlyErrorMessage(error) {
  return MESSAGES[error?.code] ?? error?.message ?? 'Ocurrió un error inesperado.'
}
