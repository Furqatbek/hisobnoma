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
// Note: GET /inventory/categories returns a list (not paginated)
export const categoriesApi = {
  getAll: () => api.get('/inventory/categories'),
  getTree: () => api.get('/inventory/categories/tree'),
  getRoots: () => api.get('/inventory/categories/roots'),
  getById: (id) => api.get(`/inventory/categories/${id}`),
  getChildren: (id) => api.get(`/inventory/categories/${id}/children`),
  search: (query, params) => api.get('/inventory/categories/search', { params: { q: query, ...params } }),
  create: (data) => api.post('/inventory/categories', data),
  update: (id, data) => api.put(`/inventory/categories/${id}`, data),
  delete: (id) => api.delete(`/inventory/categories/${id}`)
}

// Brands API - Backend: /api/v1/inventory/brands
// Note: GET /inventory/brands returns a list; use /paginated for pagination
export const brandsApi = {
  getAll: () => api.get('/inventory/brands'),
  getPaginated: (params) => api.get('/inventory/brands/paginated', { params }),
  getActive: () => api.get('/inventory/brands/active'),
  getById: (id) => api.get(`/inventory/brands/${id}`),
  search: (query) => api.get('/inventory/brands/search', { params: { q: query } }),
  create: (data) => api.post('/inventory/brands', data),
  update: (id, data) => api.put(`/inventory/brands/${id}`, data),
  delete: (id) => api.delete(`/inventory/brands/${id}`)
}

// Unit of Measure API - Backend: /api/v1/inventory/uom
export const uomApi = {
  getAll: () => api.get('/inventory/uom'),
  getPaginated: (params) => api.get('/inventory/uom/paginated', { params }),
  getActive: () => api.get('/inventory/uom/active'),
  getBase: () => api.get('/inventory/uom/base'),
  getById: (id) => api.get(`/inventory/uom/${id}`),
  getByCode: (code) => api.get(`/inventory/uom/code/${code}`),
  search: (query) => api.get('/inventory/uom/search', { params: { q: query } }),
  create: (data) => api.post('/inventory/uom', data),
  update: (id, data) => api.put(`/inventory/uom/${id}`, data),
  delete: (id) => api.delete(`/inventory/uom/${id}`),
  convert: (quantity, fromUomId, toUomId) => api.get('/inventory/uom/convert', { params: { quantity, fromUomId, toUomId } })
}

// Stock API - Backend: /api/v1/inventory/stock
export const stockApi = {
  getAll: (params) => api.get('/inventory/stock', { params }),
  getByProduct: (productId) => api.get(`/inventory/stock/product/${productId}`),
  getByLocation: (locationId) => api.get(`/inventory/stock/location/${locationId}`),
  getByProductAndLocation: (productId, locationId) => api.get(`/inventory/stock/product/${productId}/location/${locationId}`),
  getLowStock: () => api.get('/inventory/stock/low-stock'),
  getValuation: () => api.get('/inventory/stock/valuation'),
  getAvailable: (productId) => api.get(`/inventory/stock/available/${productId}`),
  checkAvailability: (params) => api.get('/inventory/stock/check-availability', { params }),
  search: (query, params) => api.get('/inventory/stock/search', { params: { q: query, ...params } }),
  getMovements: (params) => api.get('/inventory/stock/movements', { params }),
  getMovementsByProduct: (productId, params) => api.get(`/inventory/stock/movements/product/${productId}`, { params }),
  getMovementsByLocation: (locationId, params) => api.get(`/inventory/stock/movements/location/${locationId}`, { params }),
  receive: (data) => api.post('/inventory/stock/receive', data),
  issue: (data) => api.post('/inventory/stock/issue', data),
  transfer: (data) => api.post('/inventory/stock/transfer', data),
  adjust: (data) => api.post('/inventory/stock/adjust', data)
}

// POS Terminals API - Backend: /api/v1/pos/terminals
export const terminalsApi = {
  getAll: (params) => api.get('/pos/terminals', { params }),
  getActive: () => api.get('/pos/terminals/active'),
  getById: (id) => api.get(`/pos/terminals/${id}`),
  getByCode: (code) => api.get(`/pos/terminals/code/${code}`),
  getByLocation: (locationId) => api.get(`/pos/terminals/location/${locationId}`),
  create: (data) => api.post('/pos/terminals', data)
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
  search: (query) => api.get('/finance/customers/search', { params: { query } })
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
  getByStatus: (status, params) => api.get(`/inventory/purchase-orders/status/${status}`, { params }),
  getByVendor: (vendorId, params) => api.get(`/inventory/purchase-orders/vendor/${vendorId}`, { params }),
  search: (query, params) => api.get('/inventory/purchase-orders/search', { params: { q: query, ...params } }),
  create: (data) => api.post('/inventory/purchase-orders', data),
  submit: (id) => api.put(`/inventory/purchase-orders/${id}/submit`),
  approve: (id) => api.put(`/inventory/purchase-orders/${id}/approve`),
  cancel: (id, reason) => api.put(`/inventory/purchase-orders/${id}/cancel`, { reason })
}

// Receiving API - Backend: /api/v1/inventory/receiving
export const receivingApi = {
  getAll: (params) => api.get('/inventory/receiving', { params }),
  getById: (id) => api.get(`/inventory/receiving/${id}`),
  getByStatus: (status, params) => api.get(`/inventory/receiving/status/${status}`, { params }),
  getByPurchaseOrder: (poId, params) => api.get(`/inventory/receiving/purchase-order/${poId}`, { params }),
  search: (query, params) => api.get('/inventory/receiving/search', { params: { q: query, ...params } }),
  create: (data) => api.post('/inventory/receiving', data),
  confirm: (id) => api.put(`/inventory/receiving/${id}/confirm`),
  cancel: (id, reason) => api.put(`/inventory/receiving/${id}/cancel`, { reason })
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
  getAll: () => api.get('/inventory/locations'),
  getPaginated: (params) => api.get('/inventory/locations/paginated', { params }),
  getActive: () => api.get('/inventory/locations/active'),
  getTree: () => api.get('/inventory/locations/tree'),
  getRoots: () => api.get('/inventory/locations/roots'),
  getByType: (type) => api.get(`/inventory/locations/type/${type}`),
  getWarehouseAndStore: () => api.get('/inventory/locations/warehouse-store'),
  getDefault: () => api.get('/inventory/locations/default'),
  getById: (id) => api.get(`/inventory/locations/${id}`),
  getByCode: (code) => api.get(`/inventory/locations/code/${code}`),
  getChildren: (id) => api.get(`/inventory/locations/${id}/children`),
  search: (query, params) => api.get('/inventory/locations/search', { params: { q: query, ...params } }),
  create: (data) => api.post('/inventory/locations', data),
  update: (id, data) => api.put(`/inventory/locations/${id}`, data),
  delete: (id) => api.delete(`/inventory/locations/${id}`)
}

// Audit Logs API - Backend: /api/v1/admin/audit-logs
export const auditLogsApi = {
  getAll: (params) => api.get('/admin/audit-logs', { params })
}
