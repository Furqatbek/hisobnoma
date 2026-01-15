import axios from 'axios'
import router from '@/router'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor - add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor - handle errors and token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Handle 401 - try to refresh token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const response = await axios.post('/api/v1/auth/refresh', {
            refreshToken
          })

          const { accessToken, refreshToken: newRefreshToken } = response.data.data

          localStorage.setItem('accessToken', accessToken)
          localStorage.setItem('refreshToken', newRefreshToken)

          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return api(originalRequest)
        } catch (refreshError) {
          // Refresh failed - logout
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          router.push('/login')
          return Promise.reject(refreshError)
        }
      } else {
        router.push('/login')
      }
    }

    return Promise.reject(error)
  }
)

export default api

// Auth API
export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
  logout: () => api.post('/auth/logout'),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  me: () => api.get('/auth/me'),
  changePassword: (data) => api.post('/auth/change-password', data)
}

// Users API
export const usersApi = {
  getAll: (params) => api.get('/users', { params }),
  getById: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`)
}

// Products API
export const productsApi = {
  getAll: (params) => api.get('/products', { params }),
  getById: (id) => api.get(`/products/${id}`),
  create: (data) => api.post('/products', data),
  update: (id, data) => api.put(`/products/${id}`, data),
  delete: (id) => api.delete(`/products/${id}`),
  search: (query) => api.get('/products/search', { params: { q: query } }),
  getByBarcode: (barcode) => api.get(`/products/barcode/${barcode}`)
}

// Categories API
export const categoriesApi = {
  getAll: (params) => api.get('/categories', { params }),
  getById: (id) => api.get(`/categories/${id}`),
  create: (data) => api.post('/categories', data),
  update: (id, data) => api.put(`/categories/${id}`, data),
  delete: (id) => api.delete(`/categories/${id}`)
}

// Brands API
export const brandsApi = {
  getAll: (params) => api.get('/brands', { params }),
  getById: (id) => api.get(`/brands/${id}`),
  create: (data) => api.post('/brands', data),
  update: (id, data) => api.put(`/brands/${id}`, data),
  delete: (id) => api.delete(`/brands/${id}`)
}

// Stock API
export const stockApi = {
  getByProduct: (productId) => api.get(`/stock/product/${productId}`),
  getByWarehouse: (warehouseId) => api.get(`/stock/warehouse/${warehouseId}`),
  getLowStock: () => api.get('/stock/low-stock'),
  adjust: (data) => api.post('/stock/adjust', data),
  transfer: (data) => api.post('/stock/transfer', data)
}

// POS API
export const posApi = {
  createTransaction: (data) => api.post('/pos/transactions', data),
  getTransaction: (id) => api.get(`/pos/transactions/${id}`),
  getTransactions: (params) => api.get('/pos/transactions', { params }),
  addLineItem: (transactionId, data) => api.post(`/pos/transactions/${transactionId}/items`, data),
  removeLineItem: (transactionId, itemId) => api.delete(`/pos/transactions/${transactionId}/items/${itemId}`),
  addPayment: (transactionId, data) => api.post(`/pos/transactions/${transactionId}/payments`, data),
  completeTransaction: (transactionId) => api.post(`/pos/transactions/${transactionId}/complete`),
  voidTransaction: (transactionId, reason) => api.post(`/pos/transactions/${transactionId}/void`, { reason }),
  getDailySummary: (date) => api.get('/pos/summary/daily', { params: { date } })
}

// Customers API
export const customersApi = {
  getAll: (params) => api.get('/customers', { params }),
  getById: (id) => api.get(`/customers/${id}`),
  create: (data) => api.post('/customers', data),
  update: (id, data) => api.put(`/customers/${id}`, data),
  delete: (id) => api.delete(`/customers/${id}`),
  search: (query) => api.get('/customers/search', { params: { q: query } })
}

// Suppliers API
export const suppliersApi = {
  getAll: (params) => api.get('/suppliers', { params }),
  getById: (id) => api.get(`/suppliers/${id}`),
  create: (data) => api.post('/suppliers', data),
  update: (id, data) => api.put(`/suppliers/${id}`, data),
  delete: (id) => api.delete(`/suppliers/${id}`)
}

// Purchase Orders API
export const purchaseOrdersApi = {
  getAll: (params) => api.get('/purchase-orders', { params }),
  getById: (id) => api.get(`/purchase-orders/${id}`),
  create: (data) => api.post('/purchase-orders', data),
  update: (id, data) => api.put(`/purchase-orders/${id}`, data),
  approve: (id) => api.post(`/purchase-orders/${id}/approve`),
  receive: (id, data) => api.post(`/purchase-orders/${id}/receive`, data),
  cancel: (id) => api.post(`/purchase-orders/${id}/cancel`)
}

// Reports API
export const reportsApi = {
  getSalesSummary: (params) => api.get('/reports/sales/summary', { params }),
  getSalesDetailed: (params) => api.get('/reports/sales/detailed', { params }),
  getInventoryReport: (params) => api.get('/reports/inventory', { params }),
  getFinancialReport: (params) => api.get('/reports/financial', { params }),
  exportReport: (type, params) => api.get(`/reports/${type}/export`, { params, responseType: 'blob' })
}

// Dashboard API
export const dashboardApi = {
  getStats: () => api.get('/admin/dashboard/stats'),
  getSalesChart: (period) => api.get('/dashboard/sales-chart', { params: { period } })
}

// Warehouses API
export const warehousesApi = {
  getAll: (params) => api.get('/warehouses', { params }),
  getById: (id) => api.get(`/warehouses/${id}`),
  create: (data) => api.post('/warehouses', data),
  update: (id, data) => api.put(`/warehouses/${id}`, data),
  delete: (id) => api.delete(`/warehouses/${id}`)
}
