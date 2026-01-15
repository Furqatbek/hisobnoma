import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/services/api'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => !!user.value)
  const userPermissions = computed(() => user.value?.permissions || [])
  const userRoles = computed(() => user.value?.roles || [])

  function hasPermission(permission) {
    return userPermissions.value.includes(permission)
  }

  function hasRole(role) {
    return userRoles.value.includes(role)
  }

  function hasAnyRole(roles) {
    return roles.some(role => userRoles.value.includes(role))
  }

  async function login(credentials) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.login(credentials)
      const { accessToken, refreshToken, user: userData } = response.data

      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      user.value = userData

      router.push('/dashboard')
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.message || 'Login failed'
      return { success: false, error: error.value }
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (err) {
      console.error('Logout error:', err)
    } finally {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      user.value = null
      router.push('/login')
    }
  }

  async function fetchUser() {
    const token = localStorage.getItem('accessToken')
    if (!token) return

    loading.value = true
    try {
      const response = await authApi.me()
      user.value = response.data
    } catch (err) {
      console.error('Failed to fetch user:', err)
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      user.value = null
    } finally {
      loading.value = false
    }
  }

  async function initializeAuth() {
    const token = localStorage.getItem('accessToken')
    if (token) {
      await fetchUser()
    }
  }

  async function changePassword(currentPassword, newPassword) {
    loading.value = true
    error.value = null

    try {
      await authApi.changePassword({ currentPassword, newPassword })
      return { success: true }
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to change password'
      return { success: false, error: error.value }
    } finally {
      loading.value = false
    }
  }

  return {
    user,
    loading,
    error,
    isAuthenticated,
    userPermissions,
    userRoles,
    hasPermission,
    hasRole,
    hasAnyRole,
    login,
    logout,
    fetchUser,
    initializeAuth,
    changePassword
  }
})
