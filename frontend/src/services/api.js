import axios from 'axios'
import router from '@/router'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from '@/services/tokenStorage'

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
    const token = getAccessToken()
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

    // Handle 403 - redirect to no access page
    if (error.response?.status === 403) {
      router.push('/no-access')
      return Promise.reject(error)
    }

    // Handle 401 - try to refresh token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      const refreshToken = getRefreshToken()
      if (refreshToken) {
        try {
          const response = await axios.post('/api/v1/auth/refresh', {
            refreshToken
          })

          const { accessToken, refreshToken: newRefreshToken } = response.data.data

          setTokens(accessToken, newRefreshToken)

          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return api(originalRequest)
        } catch (refreshError) {
          // Refresh failed - logout
          clearTokens()
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
  pinLogin: (credentials) => api.post('/auth/pin-login', credentials),
  getUsersList: () => api.get('/auth/users/list'),
  setPin: (pin) => api.put('/auth/set-pin', { pin }),
  logout: () => api.post('/auth/logout'),
  refresh: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
  forgotPassword: (phone) => api.post('/auth/forgot-password', { phone }),
  resetPassword: (data) => api.post('/auth/reset-password', data),
  me: () => api.get('/auth/me'),
  changePassword: (data) => api.put('/auth/change-password', data)
}

// Users API
export const usersApi = {
  getAll: (params) => api.get('/users', { params }),
  getById: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`),
  setPin: (id, pin) => api.put(`/users/${id}/set-pin`, { pin }),
  clearPin: (id) => api.put(`/users/${id}/set-pin`, { pin: null }),
  assignRoles: (id, roleCodes) => api.put(`/users/${id}/roles`, roleCodes),
  lockUser: (id, locked) => api.put(`/users/${id}/lock`, { locked }),
  resetPassword: (id, password) => api.put(`/users/${id}/reset-password`, { password })
}

// Products API - Backend: /api/v1/inventory/products
export const productsApi = {
  getAll: (params) => api.get('/inventory/products', { params }),
  getActive: (params) => api.get('/inventory/products/active', { params }),
  getById: (id) => api.get(`/inventory/products/${id}`),
  create: (data) => api.post('/inventory/products', data),
  update: (id, data) => api.put(`/inventory/products/${id}`, data),
  delete: (id) => api.delete(`/inventory/products/${id}`),
  search: (query) => api.get('/inventory/products/search', { params: { q: query } }),
  getByBarcode: (barcode) => api.get(`/inventory/products/barcode/${barcode}`),
  // Image endpoints
  getImages: (id) => api.get(`/inventory/products/${id}/images`),
  uploadImage: (id, file, altText, title) => {
    const formData = new FormData()
    formData.append('file', file)
    if (altText) formData.append('altText', altText)
    if (title) formData.append('title', title)
    return api.post(`/inventory/products/${id}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteImage: (id, imageId) => api.delete(`/inventory/products/${id}/images/${imageId}`),
  setPrimaryImage: (id, imageId) => api.put(`/inventory/products/${id}/images/${imageId}/primary`),
  // Vendor endpoints
  getVendors: (id) => api.get(`/inventory/products/${id}/vendors`),
  addVendor: (id, data) => api.post(`/inventory/products/${id}/vendors`, data),
  updateVendor: (id, linkId, data) => api.put(`/inventory/products/${id}/vendors/${linkId}`, data),
  removeVendor: (id, linkId) => api.delete(`/inventory/products/${id}/vendors/${linkId}`),
  // Product UOM (alternate units) endpoints
  getUoms: (id) => api.get(`/inventory/products/${id}/uoms`),
  getActiveUoms: (id) => api.get(`/inventory/products/${id}/uoms/active`),
  addUom: (id, data) => api.post(`/inventory/products/${id}/uoms`, data),
  updateUom: (id, uomId, data) => api.put(`/inventory/products/${id}/uoms/${uomId}`, data),
  removeUom: (id, uomId) => api.delete(`/inventory/products/${id}/uoms/${uomId}`)
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
  getByLocation: (locationId, params) => api.get(`/inventory/stock/location/${locationId}`, { params }),
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
  create: (data) => api.post('/pos/terminals', data),
  update: (id, data) => api.put(`/pos/terminals/${id}`, data),
  delete: (id) => api.delete(`/pos/terminals/${id}`),
  activate: (id) => api.put(`/pos/terminals/${id}/activate`),
  deactivate: (id) => api.put(`/pos/terminals/${id}/deactivate`)
}

// POS Shifts API - Backend: /api/v1/pos/shifts
export const shiftsApi = {
  getAll: (params) => api.get('/pos/shifts', { params }),
  getById: (id) => api.get(`/pos/shifts/${id}`),
  getCurrentForUser: () => api.get('/pos/shifts/current'),
  getCurrentForTerminal: (terminalId) => api.get(`/pos/shifts/current/terminal/${terminalId}`),
  getOpen: () => api.get('/pos/shifts/open'),
  open: (data) => api.post('/pos/shifts/open', data),
  close: (id, data) => api.post(`/pos/shifts/${id}/close`, data)
}

// POS API - Backend: /api/v1/pos/transactions
export const posApi = {
  createTransaction: (data) => api.post('/pos/transactions', data),
  getTransaction: (id) => api.get(`/pos/transactions/${id}`),
  getTransactions: (params) => api.get('/pos/transactions', { params }),
  addLineItem: (transactionId, data) => api.post(`/pos/transactions/${transactionId}/items`, data),
  removeLineItem: (transactionId, itemId) => api.delete(`/pos/transactions/${transactionId}/items/${itemId}`),
  addPayment: (transactionId, data) => api.post(`/pos/transactions/${transactionId}/payments`, data),
  applyDiscount: (transactionId, data) => api.post(`/pos/transactions/${transactionId}/discount`, data),
  completeTransaction: (transactionId) => api.post(`/pos/transactions/${transactionId}/complete`),
  voidTransaction: (transactionId, reason) => api.post(`/pos/transactions/${transactionId}/void`, { reason }),
  getUnresolved: (shiftId) => api.get(`/pos/transactions/shift/${shiftId}/unresolved`),
  getDailySummary: (date) => api.get('/pos/transactions/daily-summary', { params: { date } }),
  getByCustomer: (customerId, params) => api.get(`/pos/transactions/customer/${customerId}`, { params })
}

// Customers API - Backend: /api/v1/finance/customers
export const customersApi = {
  getAll: (params) => api.get('/finance/customers', { params }),
  getById: (id) => api.get(`/finance/customers/${id}`),
  create: (data) => api.post('/finance/customers', data),
  update: (id, data) => api.put(`/finance/customers/${id}`, data),
  delete: (id) => api.delete(`/finance/customers/${id}`),
  getNextCode: () => api.get('/finance/customers/next-code'),
  search: (query) => api.get('/finance/customers/search', { params: { query } }),
  setCreditHold: (id, hold) => api.patch(`/finance/customers/${id}/credit-hold`, null, { params: { hold } }),
  updateCreditLimit: (id, creditLimit) => api.patch(`/finance/customers/${id}/credit-limit`, null, { params: { creditLimit } })
}

// AR Reports API - Backend: /api/v1/finance/ar-reports
export const arReportsApi = {
  getAgingReport: (asOfDate) => api.get('/finance/ar-reports/aging', { params: { asOfDate } }),
  getCustomerBalanceReport: () => api.get('/finance/ar-reports/customer-balance'),
  getCustomerBalance: (customerId) => api.get(`/finance/ar-reports/customer-balance/${customerId}`)
}

// AR Invoices API - Backend: /api/v1/finance/ar-invoices
export const arInvoicesApi = {
  getUnpaidByCustomer: (customerId) => api.get(`/finance/ar-invoices/customer/${customerId}/unpaid`),
  getByCustomer: (customerId, params) => api.get(`/finance/ar-invoices/customer/${customerId}`, { params }),
  getById: (id) => api.get(`/finance/ar-invoices/${id}`),
  create: (data) => api.post('/finance/ar-invoices', data),
  update: (id, data) => api.put(`/finance/ar-invoices/${id}`, data),
  post: (id) => api.post(`/finance/ar-invoices/${id}/post`)
}

// AR Payments API - Backend: /api/v1/finance/ar-payments
export const arPaymentsApi = {
  getAll: (params) => api.get('/finance/ar-payments', { params }),
  getById: (id) => api.get(`/finance/ar-payments/${id}`),
  getByCustomer: (customerId, params) => api.get(`/finance/ar-payments/customer/${customerId}`, { params }),
  getByStatus: (status, params) => api.get(`/finance/ar-payments/status/${status}`, { params }),
  getByDateRange: (startDate, endDate) => api.get('/finance/ar-payments/date-range', { params: { startDate, endDate } }),
  create: (data) => api.post('/finance/ar-payments', data),
  createAndComplete: (data) => api.post('/finance/ar-payments/pay', data),
  complete: (id) => api.post(`/finance/ar-payments/${id}/complete`),
  cancel: (id, reason) => api.post(`/finance/ar-payments/${id}/cancel`, null, { params: { reason } })
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
  cancel: (id, reason) => api.put(`/inventory/purchase-orders/${id}/cancel`, { reason }),
  receive: (id) => api.put(`/inventory/purchase-orders/${id}/receive`)
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

export const inventoryPlanningApi = {
  getReorderSuggestions: (locationId) => api.get('/inventory/planning/reorder-suggestions', { params: locationId ? { locationId } : {} }),
  getAbcAnalysis: (days = 365) => api.get('/inventory/planning/abc-analysis', { params: { days } }),
  getSlowMoving: (days = 90, locationId) => api.get('/inventory/planning/slow-moving', { params: { days, ...(locationId ? { locationId } : {}) } }),
  getDeadStock: (days = 180, locationId) => api.get('/inventory/planning/dead-stock', { params: { days, ...(locationId ? { locationId } : {}) } })
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
  getIncomeStatement: (data) => api.post('/reports/financial/income-statement', data),
  exportIncomeStatement: (data) => api.post('/reports/financial/income-statement/export', data, { responseType: 'blob' }),
  getARAgingReport: (data) => api.post('/reports/financial/ar-aging', data),
  exportARAgingReport: (data) => api.post('/reports/financial/ar-aging/export', data, { responseType: 'blob' }),
  getAPAgingReport: (data) => api.post('/reports/financial/ap-aging', data),
  exportAPAgingReport: (data) => api.post('/reports/financial/ap-aging/export', data, { responseType: 'blob' }),

  // HR / Salary Reports
  getSalaryReport: (year, month) => api.get('/reports/hr/salary', { params: { year, month } }),

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

// Alias for locations
export const locationsApi = warehousesApi

// System Settings API - Backend: /api/v1/admin/settings/system
export const systemSettingsApi = {
  getAll: () => api.get('/admin/settings/system'),
  getCategories: () => api.get('/admin/settings/system/categories'),
  getByCategory: (category) => api.get(`/admin/settings/system/category/${category}`),
  getByKey: (key) => api.get(`/admin/settings/system/${key}`),
  create: (data) => api.post('/admin/settings/system', data),
  update: (key, data) => api.put(`/admin/settings/system/${key}`, data),
  updateValue: (key, value) => api.put(`/admin/settings/system/${key}/value`, { value }),
  batchUpdate: (settings) => api.put('/admin/settings/system/batch', settings),
  delete: (key) => api.delete(`/admin/settings/system/${key}`)
}

export const tenantSettingsApi = {
  getAll: () => api.get('/admin/settings/tenant'),
  getCategories: () => api.get('/admin/settings/tenant/categories'),
  getByCategory: (category) => api.get(`/admin/settings/tenant/category/${category}`),
  create: (data) => api.post('/admin/settings/tenant', data),
  update: (key, data) => api.put(`/admin/settings/tenant/${key}`, data),
  updateValue: (key, value) => api.put(`/admin/settings/tenant/${key}/value`, { value }),
  batchUpdate: (settings) => api.put('/admin/settings/tenant/batch', settings),
  getMap: () => api.get('/admin/settings/tenant/map')
}

// Audit Logs API - Backend: /api/v1/admin/audit-logs
export const auditLogsApi = {
  getAll: (params) => api.get('/admin/audit-logs', { params }),
  getByUser: (userId, params) => api.get(`/admin/audit-logs/user/${userId}`, { params }),
  getByEntity: (entityType, entityId, params) => api.get(`/admin/audit-logs/entity/${entityType}/${entityId}`, { params }),
  getByAction: (action, params) => api.get(`/admin/audit-logs/action/${action}`, { params }),
  getByModule: (module, params) => api.get(`/admin/audit-logs/module/${module}`, { params }),
  getByDateRange: (startDate, endDate, params) => api.get('/admin/audit-logs/date-range', { params: { startDate, endDate, ...params } }),
  getFailed: (params) => api.get('/admin/audit-logs/failed', { params }),
  getActionStats: (days = 7) => api.get('/admin/audit-logs/stats/actions', { params: { days } }),
  getModuleStats: (days = 7) => api.get('/admin/audit-logs/stats/modules', { params: { days } }),
  getActiveUsers: (days = 7) => api.get('/admin/audit-logs/stats/users', { params: { days } }),
  getFailedLogins: (hours = 24) => api.get('/admin/audit-logs/stats/failed-logins', { params: { hours } })
}

// Roles API - Backend: /api/v1/roles
export const rolesApi = {
  getAll: (params) => api.get('/roles', { params }),
  getSystemRoles: () => api.get('/roles/system'),
  getById: (id) => api.get(`/roles/${id}`),
  create: (data) => api.post('/roles', data),
  update: (id, data) => api.put(`/roles/${id}`, data),
  delete: (id) => api.delete(`/roles/${id}`),
  assignPermissions: (id, permissionCodes) => api.put(`/roles/${id}/permissions`, permissionCodes),
  getAllPermissions: () => api.get('/roles/permissions')
}

// Journal Entries API - Backend: /api/v1/finance/journal-entries
export const journalEntriesApi = {
  getAll: (params) => api.get('/finance/journal-entries', { params }),
  getById: (id) => api.get(`/finance/journal-entries/${id}`),
  getWithLines: (id) => api.get(`/finance/journal-entries/${id}/lines`),
  getBySource: (source, params) => api.get(`/finance/journal-entries/source/${source}`, { params }),
  getByStatus: (status, params) => api.get(`/finance/journal-entries/status/${status}`, { params }),
  getByDateRange: (startDate, endDate) => api.get('/finance/journal-entries/date-range', { params: { startDate, endDate } }),
  search: (q, params) => api.get('/finance/journal-entries/search', { params: { q, ...params } })
}

// Expense Records API - Backend: /api/v1/web/expenses
export const expenseRecordsApi = {
  getAll: (params) => api.get('/web/expenses', { params }),
  getTotal: () => api.get('/web/expenses/summary/total'),
  create: (data) => api.post('/web/expenses', data),
  delete: (id) => api.delete(`/web/expenses/${id}`)
}

// AP Invoices (Expenses) API - Backend: /api/v1/ap/invoices
export const expensesApi = {
  getAll: (params) => api.get('/ap/invoices', { params }),
  getById: (id) => api.get(`/ap/invoices/${id}`),
  getByVendor: (vendorId, params) => api.get(`/ap/invoices/vendor/${vendorId}`, { params }),
  getByStatus: (status, params) => api.get(`/ap/invoices/status/${status}`, { params }),
  getUnpaidByVendor: (vendorId) => api.get(`/ap/invoices/vendor/${vendorId}/unpaid`),
  getOverdue: () => api.get('/ap/invoices/overdue'),
  create: (data) => api.post('/ap/invoices', data),
  createFromReceiving: (receivingOrderId) => api.post(`/ap/invoices/from-receiving/${receivingOrderId}`),
  update: (id, data) => api.put(`/ap/invoices/${id}`, data),
  submit: (id) => api.post(`/ap/invoices/${id}/submit`),
  approve: (id) => api.post(`/ap/invoices/${id}/approve`),
  reject: (id, reason) => api.post(`/ap/invoices/${id}/reject`, { reason }),
  cancel: (id, reason) => api.post(`/ap/invoices/${id}/cancel`, { reason }),
  hold: (id) => api.post(`/ap/invoices/${id}/hold`),
  releaseHold: (id) => api.post(`/ap/invoices/${id}/release-hold`),
  getTotalPayable: () => api.get('/ap/invoices/summary/total-payable'),
  getVendorBalance: (vendorId) => api.get(`/ap/invoices/summary/vendor/${vendorId}/balance`),
  getOverdueBalance: () => api.get('/ap/invoices/summary/overdue-balance')
}

// AP Payments API - Backend: /api/v1/ap/payments
export const apPaymentsApi = {
  getAll: (params) => api.get('/ap/payments', { params }),
  getById: (id) => api.get(`/ap/payments/${id}`),
  getByVendor: (vendorId, params) => api.get(`/ap/payments/vendor/${vendorId}`, { params }),
  getByStatus: (status, params) => api.get(`/ap/payments/status/${status}`, { params }),
  create: (data) => api.post('/ap/payments', data),
  submit: (id) => api.post(`/ap/payments/${id}/submit`),
  approve: (id) => api.post(`/ap/payments/${id}/approve`),
  process: (id) => api.post(`/ap/payments/${id}/process`),
  void: (id, reason) => api.post(`/ap/payments/${id}/void`, { reason }),
}

// HR Departments API - Backend: /api/v1/hr/departments
export const departmentsApi = {
  getAll: () => api.get('/hr/departments'),
  getById: (id) => api.get(`/hr/departments/${id}`),
  create: (data) => api.post('/hr/departments', data),
  update: (id, data) => api.put(`/hr/departments/${id}`, data),
  delete: (id) => api.delete(`/hr/departments/${id}`)
}

// HR Positions API - Backend: /api/v1/hr/positions
export const positionsApi = {
  getAll: () => api.get('/hr/positions'),
  getById: (id) => api.get(`/hr/positions/${id}`),
  create: (data) => api.post('/hr/positions', data),
  update: (id, data) => api.put(`/hr/positions/${id}`, data),
  delete: (id) => api.delete(`/hr/positions/${id}`)
}

// HR Employees API - Backend: /api/v1/hr/employees
export const employeesApi = {
  getAll: (params) => api.get('/hr/employees', { params }),
  getActive: () => api.get('/hr/employees/active'),
  getById: (id) => api.get(`/hr/employees/${id}`),
  search: (query, params) => api.get('/hr/employees/search', { params: { q: query, ...params } }),
  create: (data) => api.post('/hr/employees', data),
  update: (id, data) => api.put(`/hr/employees/${id}`, data),
  terminate: (id) => api.put(`/hr/employees/${id}/terminate`),
  delete: (id) => api.delete(`/hr/employees/${id}`)
}

// HR Salary API - Backend: /api/v1/hr/salary
export const salaryApi = {
  getAll: (params) => api.get('/hr/salary', { params }),
  getByPeriod: (year, month, params) => api.get('/hr/salary/period', { params: { year, month, ...params } }),
  getByEmployee: (employeeId) => api.get(`/hr/salary/employee/${employeeId}`),
  getById: (id) => api.get(`/hr/salary/${id}`),
  create: (data) => api.post('/hr/salary', data),
  markPaid: (id) => api.put(`/hr/salary/${id}/pay`),
  cancel: (id) => api.put(`/hr/salary/${id}/cancel`)
}

// HR Salary Advances API - Backend: /api/v1/hr/advances
export const advancesApi = {
  getByPeriod: (year, month) => api.get('/hr/advances/period', { params: { year, month } }),
  getByEmployee: (employeeId) => api.get(`/hr/advances/employee/${employeeId}`),
  getUndeductedTotal: (employeeId, year, month) => api.get(`/hr/advances/employee/${employeeId}/total`, { params: { year, month } }),
  create: (data) => api.post('/hr/advances', data),
  cancel: (id) => api.put(`/hr/advances/${id}/cancel`)
}

// Telegram API - Backend: /api/v1/telegram
export const telegramApi = {
  getStatus: () => api.get('/telegram/status'),
  generateLinkCode: () => api.post('/telegram/link-code'),
  unlink: () => api.delete('/telegram/unlink'),
  // Admin endpoints
  getBotInfo: () => api.get('/telegram/admin/info'),
  getSettings: () => api.get('/telegram/admin/settings'),
  saveSettings: (data) => api.post('/telegram/admin/settings', data),
  getConnectedUsers: () => api.get('/telegram/admin/users'),
  sendMessage: (data) => api.post('/telegram/admin/send', data),
  broadcast: (data) => api.post('/telegram/admin/broadcast', data),
  adminUnlinkUser: (userId) => api.delete(`/telegram/admin/users/${userId}/unlink`),
  getDailyReportSettings: () => api.get('/telegram/admin/daily-report'),
  saveDailyReportSettings: (data) => api.post('/telegram/admin/daily-report', data)
}

// SMS API - Backend: /api/v1/sms
export const smsApi = {
  send: (data) => api.post('/sms/send', data),
  sendBulk: (data) => api.post('/sms/send-bulk', data),
  getHistory: (params) => api.get('/sms/history', { params }),
  getBalance: () => api.get('/sms/balance'),
  getStatus: (params) => api.get('/sms/status', { params }),
  getSettings: () => api.get('/sms/settings'),
  saveSettings: (data) => api.post('/sms/settings', data),
  // Templates
  getTemplates: (params) => api.get('/sms/templates', { params }),
  getTemplate: (id) => api.get(`/sms/templates/${id}`),
  createTemplate: (data) => api.post('/sms/templates', data),
  updateTemplate: (id, data) => api.put(`/sms/templates/${id}`, data),
  deleteTemplate: (id) => api.delete(`/sms/templates/${id}`)
}

// Delivery Regions API - Backend: /api/v1/delivery/regions
export const deliveryRegionsApi = {
  getAll: (params) => api.get('/delivery/regions', { params }),
  getActive: () => api.get('/delivery/regions/active'),
  getById: (id) => api.get(`/delivery/regions/${id}`),
  create: (data) => api.post('/delivery/regions', data),
  update: (id, data) => api.put(`/delivery/regions/${id}`, data),
  delete: (id) => api.delete(`/delivery/regions/${id}`)
}

// Delivery Villages API - Backend: /api/v1/delivery/villages
export const deliveryVillagesApi = {
  getAll: (params) => api.get('/delivery/villages', { params }),
  getActive: () => api.get('/delivery/villages/active'),
  getByRegion: (regionId) => api.get(`/delivery/villages/region/${regionId}`),
  getById: (id) => api.get(`/delivery/villages/${id}`),
  create: (data) => api.post('/delivery/villages', data),
  update: (id, data) => api.put(`/delivery/villages/${id}`, data),
  delete: (id) => api.delete(`/delivery/villages/${id}`)
}
