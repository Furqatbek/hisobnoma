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

/**
 * Tenant configured for this deployment, injected at container start into
 * window.__APP_CONFIG__ (see public/config.js + docker-entrypoint.sh).
 * Used to seed the pre-auth PIN/user-list screen on a fresh device where no
 * prior login has remembered a tenant. Returns null unless it's a plain
 * positive integer, so an unreplaced placeholder or empty value is ignored.
 */
export function getConfiguredTenantId() {
  const raw = (typeof window !== 'undefined' && window.__APP_CONFIG__)
    ? window.__APP_CONFIG__.defaultTenantId
    : undefined
  return typeof raw === 'string' && /^\d+$/.test(raw) ? raw : null
}

/**
 * Tenant to use for the pre-auth user list: the one remembered from the last
 * successful login, falling back to the deployment's configured tenant.
 */
export function resolvePreAuthTenantId() {
  return getLastTenantId() || getConfiguredTenantId()
}
