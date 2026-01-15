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
  changePassword: (data) => api.put('/auth/change-password', data)
}

// Users API
export const usersApi = {
  getAll: (params) => api.get('/users', { params }),
  getById: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`)
}

// Products API - Backend: /api/v1/inventory/products
export const productsApi = {
  getAll: (params) => api.get('/inventory/products', { params }),
  getById: (id) => api.get(`/inventory/products/${id}`),
  create: (data) => api.post('/inventory/products', data),
  update: (id, data) => api.put(`/inventory/products/${id}`, data),
  delete: (id) => api.delete(`/inventory/products/${id}`),
  search: (query) => api.get('/inventory/products/search', { params: { q: query } }),
  getByBarcode: (barcode) => api.get(`/inventory/products/barcode/${barcode}`)
}

// Categories API - Backend: /api/v1/inventory/categories
export const categoriesApi = {
  getAll: (params) => api.get('/inventory/categories', { params }),
  getById: (id) => api.get(`/inventory/categories/${id}`),
  create: (data) => api.post('/inventory/categories', data),
  update: (id, data) => api.put(`/inventory/categories/${id}`, data),
  delete: (id) => api.delete(`/inventory/categories/${id}`)
}

// Brands API - Backend: /api/v1/inventory/brands
export const brandsApi = {
  getAll: (params) => api.get('/inventory/brands', { params }),
  getById: (id) => api.get(`/inventory/brands/${id}`),
  create: (data) => api.post('/inventory/brands', data),
  update: (id, data) => api.put(`/inventory/brands/${id}`, data),
  delete: (id) => api.delete(`/inventory/brands/${id}`)
}

// Stock API - Backend: /api/v1/inventory/stock
export const stockApi = {
  getByProduct: (productId) => api.get(`/inventory/stock/product/${productId}`),
  getByLocation: (locationId) => api.get(`/inventory/stock/location/${locationId}`),
  getLowStock: () => api.get('/inventory/stock/low-stock'),
  adjust: (data) => api.post('/inventory/stock/adjust', data),
  transfer: (data) => api.post('/inventory/stock/transfer', data)
}

// POS API - Backend: /api/v1/pos/transactions
export const posApi = {
  createTransaction: (data) => api.post('/pos/transactions', data),
  getTransaction: (id) => api.get(`/pos/transactions/${id}`),
  getTransactions: (params) => api.get('/pos/transactions', { params }),
  addLineItem: (transactionId, data) => api.post(`/pos/transactions/${transactionId}/items`, data),
  removeLineItem: (transactionId, itemId) => api.delete(`/pos/transactions/${transactionId}/items/${itemId}`),
  addPayment: (transactionId, data) => api.post(`/pos/transactions/${transactionId}/payments`, data),
  completeTransaction: (transactionId) => api.post(`/pos/transactions/${transactionId}/complete`),
  voidTransaction: (transactionId, reason) => api.post(`/pos/transactions/${transactionId}/void`, { reason }),
  getDailySummary: (date) => api.get('/pos/transactions/daily-summary', { params: { date } })
}

// Customers API - Backend: /api/v1/finance/customers
export const customersApi = {
  getAll: (params) => api.get('/finance/customers', { params }),
  getById: (id) => api.get(`/finance/customers/${id}`),
  create: (data) => api.post('/finance/customers', data),
  update: (id, data) => api.put(`/finance/customers/${id}`, data),
  delete: (id) => api.delete(`/finance/customers/${id}`),
  search: (query) => api.get('/finance/customers/search', { params: { q: query } })
}

// Suppliers/Vendors API - Backend: /api/v1/inventory/vendors
export const suppliersApi = {
  getAll: (params) => api.get('/inventory/vendors', { params }),
  getById: (id) => api.get(`/inventory/vendors/${id}`),
  create: (data) => api.post('/inventory/vendors', data),
  update: (id, data) => api.put(`/inventory/vendors/${id}`, data),
  delete: (id) => api.delete(`/inventory/vendors/${id}`)
}

// Purchase Orders API - Backend: /api/v1/inventory/purchase-orders
export const purchaseOrdersApi = {
  getAll: (params) => api.get('/inventory/purchase-orders', { params }),
  getById: (id) => api.get(`/inventory/purchase-orders/${id}`),
  create: (data) => api.post('/inventory/purchase-orders', data),
  update: (id, data) => api.put(`/inventory/purchase-orders/${id}`, data),
  approve: (id) => api.post(`/inventory/purchase-orders/${id}/approve`),
  receive: (id, data) => api.post(`/inventory/purchase-orders/${id}/receive`, data),
  cancel: (id) => api.post(`/inventory/purchase-orders/${id}/cancel`)
}

// Reports API - Backend: /api/v1/reports (uses POST with request body)
export const reportsApi = {
  // Sales Reports
  getSalesSummary: (data) => api.post('/reports/sales/summary', data),
  exportSalesSummary: (data) => api.post('/reports/sales/summary/export', data, { responseType: 'blob' }),

  // Inventory Reports
  getStockOnHand: (data) => api.post('/reports/inventory/stock-on-hand', data),
  exportStockOnHand: (data) => api.post('/reports/inventory/stock-on-hand/export', data, { responseType: 'blob' }),
  getInventoryValuation: (data) => api.post('/reports/inventory/valuation', data),
  exportInventoryValuation: (data) => api.post('/reports/inventory/valuation/export', data, { responseType: 'blob' }),

  // Financial Reports
  getTrialBalance: (data) => api.post('/reports/financial/trial-balance', data),
  exportTrialBalance: (data) => api.post('/reports/financial/trial-balance/export', data, { responseType: 'blob' }),
  getARAgingReport: (data) => api.post('/reports/financial/ar-aging', data),
  exportARAgingReport: (data) => api.post('/reports/financial/ar-aging/export', data, { responseType: 'blob' }),
  getAPAgingReport: (data) => api.post('/reports/financial/ap-aging', data),
  exportAPAgingReport: (data) => api.post('/reports/financial/ap-aging/export', data, { responseType: 'blob' }),

  // Report definitions & schedules
  getDefinitions: (params) => api.get('/reports/definitions', { params }),
  getSchedules: (params) => api.get('/reports/schedules', { params }),
  getExecutions: (params) => api.get('/reports/executions', { params })
}

// Dashboard API - Backend: /api/v1/admin/dashboard
export const dashboardApi = {
  getStats: () => api.get('/admin/dashboard/stats'),
  getSalesChart: (period) => api.get('/admin/dashboard/sales-chart', { params: { period } })
}

// Warehouses/Locations API - Backend: /api/v1/inventory/locations
export const warehousesApi = {
  getAll: (params) => api.get('/inventory/locations', { params }),
  getById: (id) => api.get(`/inventory/locations/${id}`),
  create: (data) => api.post('/inventory/locations', data),
  update: (id, data) => api.put(`/inventory/locations/${id}`, data),
  delete: (id) => api.delete(`/inventory/locations/${id}`)
}

// Audit Logs API - Backend: /api/v1/admin/audit-logs
export const auditLogsApi = {
  getAll: (params) => api.get('/admin/audit-logs', { params })
}
