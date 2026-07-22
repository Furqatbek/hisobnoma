/**
 * Token storage that switches between localStorage (remember me)
 * and sessionStorage (session only) based on user preference.
 */

const STORAGE_KEY = 'rememberMe'

function getStorage() {
  return localStorage.getItem(STORAGE_KEY) === 'true' ? localStorage : sessionStorage
}

export function setRememberMe(value) {
  if (value) {
    localStorage.setItem(STORAGE_KEY, 'true')
  } else {
    localStorage.removeItem(STORAGE_KEY)
  }
}

export function getAccessToken() {
  return getStorage().getItem('accessToken')
}

export function getRefreshToken() {
  return getStorage().getItem('refreshToken')
}

export function setTokens(accessToken, refreshToken) {
  const storage = getStorage()
  storage.setItem('accessToken', accessToken)
  storage.setItem('refreshToken', refreshToken)
}

export function clearTokens() {
  // Clear from both storages to be safe
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem(STORAGE_KEY)
  sessionStorage.removeItem('accessToken')
  sessionStorage.removeItem('refreshToken')
}

/**
 * Tenant of the last successful login. Kept in localStorage (survives logout)
 * so the pre-auth PIN login screen can request this tenant's user list —
 * the backend requires X-Tenant-ID on /auth/users/list and fails closed.
 */
export function setLastTenantId(tenantId) {
  if (tenantId != null) {
    localStorage.setItem('lastTenantId', String(tenantId))
  }
}

export function getLastTenantId() {
  return localStorage.getItem('lastTenantId')
}
