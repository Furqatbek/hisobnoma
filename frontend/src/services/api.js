import axios from 'axios'
import router from '@/router'
import { getAccessToken, getRefreshToken, setTokens, clearTokens, getLastTenantId } from '@/services/tokenStorage'

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

// ---------------------------------------------------------------------------
// Response unwrapping
//
// The backend uses two envelope shapes:
//   single objects/lists:  { success, message, data, timestamp }
//   paginated results:     { content: [...], page: { number, totalElements, ... } }
//
// Use these instead of hand-rolled fallback chains like
// `res.data?.data?.content || res.data?.data || res.data?.content || []`.
// ---------------------------------------------------------------------------

/**
 * Payload of an ApiResponse envelope, tolerating non-enveloped bodies —
 * the exact equivalent of the old `res.data.data || res.data` idiom.
 */
export function unwrapData(response, fallback = null) {
  const body = response?.data
  if (body == null) return fallback
  return (typeof body === 'object' && body.data) || body
}

/** List payload, tolerating both envelope shapes; always returns an array. */
export function unwrapList(response) {
  const value = unwrapData(response, [])
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  return []
}

/** Paginated payload: { content, page } with safe defaults. */
export function unwrapPage(response) {
  const body = response?.data
  const source = body && typeof body === 'object' && 'content' in body ? body : body?.data
  return {
    content: source?.content || [],
    page: source?.page || { number: 0, size: 0, totalElements: 0, totalPages: 0 }
  }
}

// Auth API
export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  pinLogin: (credentials) => api.post('/auth/pin-login', credentials),
  // Pre-auth PIN screen: the backend requires X-Tenant-ID here (fails closed),
  // so send the tenant remembered from the last successful login.
  getUsersList: () => {
    const tenantId = getLastTenantId()
    return api.get('/auth/users/list', tenantId ? { headers: { 'X-Tenant-ID': tenantId } } : {})
  },
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
  removeUom: (id, uomId) => api.delete(`/inventory/products/${id}/uoms/${uomId}`),
  getBySku: (sku) => api.get(`/inventory/products/sku/${sku}`),
  getByCategory: (categoryId, params) => api.get(`/inventory/products/category/${categoryId}`, { params }),
  getByBrand: (brandId, params) => api.get(`/inventory/products/brand/${brandId}`, { params }),
  getCount: () => api.get('/inventory/products/count'),
  generateSku: (prefix) => api.get('/inventory/products/generate-sku', { params: { prefix: prefix || undefined } }),
  generateSkuFromName: (name) => api.get('/inventory/products/generate-sku-from-name', { params: { name } }),
  validateSku: (sku) => api.get(`/inventory/products/validate-sku/${sku}`),
  generateBarcode: (prefix) => api.get('/inventory/products/generate-barcode', { params: { prefix: prefix || undefined } }),
  generateEan13: (countryCode, companyCode) => api.get('/inventory/products/generate-ean13', { params: { countryCode, companyCode: companyCode || undefined } }),
  validateBarcode: (barcode) => api.get(`/inventory/products/validate-barcode/${barcode}`),
  getVariants: (id) => api.get(`/inventory/products/${id}/variants`),
  addVariant: (id, data) => api.post(`/inventory/products/${id}/variants`, data),
  updateVariant: (id, variantId, data) => api.put(`/inventory/products/${id}/variants/${variantId}`, data),
  deleteVariant: (id, variantId) => api.delete(`/inventory/products/${id}/variants/${variantId}`),
  importProducts: (file, format) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/inventory/products/import', formData, {
      params: { format: format || 'csv' },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getImportTemplate: (format) => api.get('/inventory/products/import/template', {
    params: { format: format || 'csv' },
    responseType: 'blob'
  }),
  exportProducts: (params) => api.get('/inventory/products/export', {
    params,
    responseType: 'blob'
  })
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
  getActive: () => api.get('/finance/customers/active'),
  getById: (id) => api.get(`/finance/customers/${id}`),
  getByCode: (code) => api.get(`/finance/customers/code/${code}`),
  create: (data) => api.post('/finance/customers', data),
  update: (id, data) => api.put(`/finance/customers/${id}`, data),
  delete: (id) => api.delete(`/finance/customers/${id}`),
  getNextCode: () => api.get('/finance/customers/next-code'),
  search: (query) => api.get('/finance/customers/search', { params: { query } }),
  activate: (id) => api.patch(`/finance/customers/${id}/activate`),
  setCreditHold: (id, hold) => api.patch(`/finance/customers/${id}/credit-hold`, null, { params: { hold } }),
  updateCreditLimit: (id, creditLimit) => api.patch(`/finance/customers/${id}/credit-limit`, null, { params: { creditLimit } }),
  getCreditHold: () => api.get('/finance/customers/credit-hold'),
  getOverCreditLimit: () => api.get('/finance/customers/over-credit-limit'),
  canInvoice: (id, amount) => api.get(`/finance/customers/${id}/can-invoice`, { params: { amount } })
}

// AR Reports API - Backend: /api/v1/finance/ar-reports
export const arReportsApi = {
  getAgingReport: (asOfDate) => api.get('/finance/ar-reports/aging', { params: { asOfDate } }),
  getCustomerBalanceReport: () => api.get('/finance/ar-reports/customer-balance'),
  getCustomerBalance: (customerId) => api.get(`/finance/ar-reports/customer-balance/${customerId}`)
}

// AR Invoices API - Backend: /api/v1/finance/ar-invoices
export const arInvoicesApi = {
  getAll: (params) => api.get('/finance/ar-invoices', { params }),
  getById: (id) => api.get(`/finance/ar-invoices/${id}`),
  getByStatus: (status, params) => api.get(`/finance/ar-invoices/status/${status}`, { params }),
  getByNumber: (invoiceNumber) => api.get(`/finance/ar-invoices/number/${invoiceNumber}`),
  getByCustomer: (customerId, params) => api.get(`/finance/ar-invoices/customer/${customerId}`, { params }),
  getUnpaidByCustomer: (customerId) => api.get(`/finance/ar-invoices/customer/${customerId}/unpaid`),
  getOutstandingByCustomer: (customerId) => api.get(`/finance/ar-invoices/customer/${customerId}/outstanding`),
  getOverdue: () => api.get('/finance/ar-invoices/overdue'),
  create: (data) => api.post('/finance/ar-invoices', data),
  update: (id, data) => api.put(`/finance/ar-invoices/${id}`, data),
  post: (id) => api.post(`/finance/ar-invoices/${id}/post`),
  send: (id) => api.post(`/finance/ar-invoices/${id}/send`),
  cancel: (id, reason) => api.post(`/finance/ar-invoices/${id}/cancel`, null, { params: { reason } }),
  checkOverdue: () => api.post('/finance/ar-invoices/check-overdue')
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
  getActive: () => api.get('/inventory/vendors/active'),
  getPreferred: () => api.get('/inventory/vendors/preferred'),
  getById: (id) => api.get(`/inventory/vendors/${id}`),
  getByCode: (code) => api.get(`/inventory/vendors/code/${code}`),
  search: (q, params) => api.get('/inventory/vendors/search', { params: { q, ...params } }),
  create: (data) => api.post('/inventory/vendors', data),
  update: (id, data) => api.put(`/inventory/vendors/${id}`, data),
  delete: (id) => api.delete(`/inventory/vendors/${id}`),
  activate: (id) => api.put(`/inventory/vendors/${id}/activate`),
  getContacts: (vendorId) => api.get(`/inventory/vendors/${vendorId}/contacts`),
  getPrimaryContact: (vendorId) => api.get(`/inventory/vendors/${vendorId}/contacts/primary`),
  createContact: (vendorId, data) => api.post(`/inventory/vendors/${vendorId}/contacts`, data),
  updateContact: (id, data) => api.put(`/inventory/vendors/contacts/${id}`, data),
  deleteContact: (id) => api.delete(`/inventory/vendors/contacts/${id}`),
  setPrimaryContact: (id) => api.put(`/inventory/vendors/contacts/${id}/set-primary`)
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

// Subscription API - Backend: /api/v1/admin/subscription
export const subscriptionApi = {
  get: () => api.get('/admin/subscription'),
  changePlan: (plan) => api.post('/admin/subscription/plan', { plan })
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

// Accounts / Chart of Accounts API - Backend: /api/v1/finance/accounts
export const accountsApi = {
  getAll: (params) => api.get('/finance/accounts', { params }),
  getAllList: () => api.get('/finance/accounts/all'),
  getRoots: () => api.get('/finance/accounts/roots'),
  getChildren: (id) => api.get(`/finance/accounts/${id}/children`),
  getActive: () => api.get('/finance/accounts/active'),
  getPostable: () => api.get('/finance/accounts/postable'),
  getByType: (accountType) => api.get(`/finance/accounts/type/${accountType}`),
  getById: (id) => api.get(`/finance/accounts/${id}`),
  getTree: (id) => api.get(`/finance/accounts/${id}/tree`),
  getByCode: (code) => api.get(`/finance/accounts/code/${code}`),
  search: (query, params) => api.get('/finance/accounts/search', { params: { q: query, ...params } }),
  create: (data) => api.post('/finance/accounts', data),
  update: (id, data) => api.put(`/finance/accounts/${id}`, data),
  delete: (id) => api.delete(`/finance/accounts/${id}`),
  activate: (id) => api.put(`/finance/accounts/${id}/activate`),
  deactivate: (id) => api.put(`/finance/accounts/${id}/deactivate`),
  importCsv: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/finance/accounts/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  generateDefault: () => api.post('/finance/accounts/generate-default')
}

// Journal Entries API - Backend: /api/v1/finance/journal-entries
export const journalEntriesApi = {
  getAll: (params) => api.get('/finance/journal-entries', { params }),
  getById: (id) => api.get(`/finance/journal-entries/${id}`),
  getWithLines: (id) => api.get(`/finance/journal-entries/${id}/lines`),
  getBySource: (source, params) => api.get(`/finance/journal-entries/source/${source}`, { params }),
  getByStatus: (status, params) => api.get(`/finance/journal-entries/status/${status}`, { params }),
  getByDateRange: (startDate, endDate) => api.get('/finance/journal-entries/date-range', { params: { startDate, endDate } }),
  search: (q, params) => api.get('/finance/journal-entries/search', { params: { q, ...params } }),
  create: (data) => api.post('/finance/journal-entries', data),
  post: (id) => api.post(`/finance/journal-entries/${id}/post`),
  reverse: (id, reversalDate) => api.post(`/finance/journal-entries/${id}/reverse`, null, { params: { reversalDate } }),
  delete: (id) => api.delete(`/finance/journal-entries/${id}`)
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
  getByDateRange: (startDate, endDate) => api.get('/ap/payments/date-range', { params: { startDate, endDate } }),
  getUnreconciled: () => api.get('/ap/payments/unreconciled'),
  create: (data) => api.post('/ap/payments', data),
  submit: (id) => api.post(`/ap/payments/${id}/submit`),
  approve: (id) => api.post(`/ap/payments/${id}/approve`),
  process: (id) => api.post(`/ap/payments/${id}/process`),
  void: (id, reason) => api.post(`/ap/payments/${id}/void`, { reason }),
  reconcile: (id) => api.post(`/ap/payments/${id}/reconcile`),
  getVendorTotal: (vendorId) => api.get(`/ap/payments/summary/vendor/${vendorId}/total`),
  getDateRangeTotal: (startDate, endDate) => api.get('/ap/payments/summary/date-range/total', { params: { startDate, endDate } })
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

// Distribution Agents API - Backend: /api/v1/distribution/agents
export const distributionAgentsApi = {
  getAll: () => api.get('/distribution/agents'),
  getPaginated: (params) => api.get('/distribution/agents/paginated', { params }),
  getActive: () => api.get('/distribution/agents/active'),
  search: (query, params) => api.get('/distribution/agents/search', { params: { q: query, ...params } }),
  getById: (id) => api.get(`/distribution/agents/${id}`),
  create: (data) => api.post('/distribution/agents', data),
  update: (id, data) => api.put(`/distribution/agents/${id}`, data),
  delete: (id) => api.delete(`/distribution/agents/${id}`)
}

// Distribution Orders API - Backend: /api/v1/distribution/orders
export const distributionOrdersApi = {
  getAll: (params) => api.get('/distribution/orders', { params }),
  search: (query, params) => api.get('/distribution/orders/search', { params: { q: query, ...params } }),
  getById: (id) => api.get(`/distribution/orders/${id}`),
  create: (data) => api.post('/distribution/orders', data),
  update: (id, data) => api.put(`/distribution/orders/${id}`, data),
  delete: (id) => api.delete(`/distribution/orders/${id}`),
  confirm: (id) => api.post(`/distribution/orders/${id}/confirm`),
  pick: (id) => api.post(`/distribution/orders/${id}/pick`),
  load: (id) => api.post(`/distribution/orders/${id}/load`),
  transit: (id) => api.post(`/distribution/orders/${id}/transit`),
  deliver: (id, data) => api.post(`/distribution/orders/${id}/deliver`, data || {}),
  invoice: (id) => api.post(`/distribution/orders/${id}/invoice`),
  cancel: (id, data) => api.post(`/distribution/orders/${id}/cancel`, data || {})
}

// Distribution Van Loadouts API - Backend: /api/v1/distribution/van-loadouts
export const distributionVanLoadoutsApi = {
  getAll: (params) => api.get('/distribution/van-loadouts', { params }),
  getById: (id) => api.get(`/distribution/van-loadouts/${id}`),
  create: (data) => api.post('/distribution/van-loadouts', data),
  delete: (id) => api.delete(`/distribution/van-loadouts/${id}`),
  load: (id) => api.post(`/distribution/van-loadouts/${id}/load`),
  reconcile: (id, data) => api.post(`/distribution/van-loadouts/${id}/reconcile`, data),
  cancel: (id) => api.post(`/distribution/van-loadouts/${id}/cancel`)
}

// Distribution Routes API - Backend: /api/v1/distribution/routes
export const distributionRoutesApi = {
  getAll: (params) => api.get('/distribution/routes', { params }),
  search: (query, params) => api.get('/distribution/routes/search', { params: { q: query, ...params } }),
  getById: (id) => api.get(`/distribution/routes/${id}`),
  create: (data) => api.post('/distribution/routes', data),
  update: (id, data) => api.put(`/distribution/routes/${id}`, data),
  delete: (id) => api.delete(`/distribution/routes/${id}`)
}

// Distribution Visits API - Backend: /api/v1/distribution/visits
export const distributionVisitsApi = {
  getAll: (params) => api.get('/distribution/visits', { params }),
  getById: (id) => api.get(`/distribution/visits/${id}`),
  checkIn: (data) => api.post('/distribution/visits/check-in', data),
  checkOut: (id, data) => api.post(`/distribution/visits/${id}/check-out`, data)
}

// Distribution KPI + Targets API - Backend: /api/v1/distribution
export const distributionKpiApi = {
  dashboard: (from, to) => api.get('/distribution/kpi/dashboard', { params: { from, to } })
}
export const distributionAgentTargetsApi = {
  byAgent: (agentId) => api.get(`/distribution/agent-targets/by-agent/${agentId}`),
  create: (data) => api.post('/distribution/agent-targets', data),
  update: (id, data) => api.put(`/distribution/agent-targets/${id}`, data),
  delete: (id) => api.delete(`/distribution/agent-targets/${id}`)
}

// Web Catalog API (online shop management) - Backend: /api/v1/web-catalog
export const webCatalogApi = {
  getAll: (params) => api.get('/web-catalog', { params }),
  getCounts: () => api.get('/web-catalog/counts'),
  addProducts: (productIds) => api.post('/web-catalog/items', { productIds }),
  update: (id, data) => api.put(`/web-catalog/items/${id}`, data),
  remove: (id) => api.delete(`/web-catalog/items/${id}`),
  publish: (id) => api.post(`/web-catalog/items/${id}/publish`),
  unpublish: (id) => api.post(`/web-catalog/items/${id}/unpublish`),
  moveUp: (id) => api.post(`/web-catalog/items/${id}/move-up`),
  moveDown: (id) => api.post(`/web-catalog/items/${id}/move-down`),
  publishAll: () => api.post('/web-catalog/publish-all'),
  unpublishAll: () => api.post('/web-catalog/unpublish-all'),
  getMostWished: (limit = 10) => api.get('/web-catalog/most-wished', { params: { limit } })
}

// Web Orders API (online shop orders) - Backend: /api/v1/web-orders
export const webOrdersApi = {
  getAll: (params) => api.get('/web-orders', { params }),
  getNewCount: () => api.get('/web-orders/counts/new'),
  getById: (id) => api.get(`/web-orders/${id}`),
  updateStatus: (id, status, reason) => api.post(`/web-orders/${id}/status`, { status, reason }),
  convertToInvoice: (id) => api.post(`/web-orders/${id}/convert-to-invoice`)
}

// Web Customers API (online shop accounts) - Backend: /api/v1/web-customers
export const webCustomersApi = {
  getAll: (params) => api.get('/web-customers', { params }),
  getSegments: (params) => api.get('/web-customers/segments', { params }),
  getSegmentCustomers: (segment, params) => api.get(`/web-customers/segments/${segment}/customers`, { params }),
  issueCoupon: (id, data) => api.post(`/web-customers/${id}/coupons`, data),
  issueSegmentCoupons: (segment, param, data) =>
    api.post(`/web-customers/segments/${segment}/coupons`, data, { params: { param } }),
  sendPush: (id, data) => api.post(`/web-customers/${id}/push`, data),
  sendSegmentPush: (segment, param, data) =>
    api.post(`/web-customers/segments/${segment}/push`, data, { params: { param } }),
  getById: (id) => api.get(`/web-customers/${id}`),
  linkCustomer: (id, customerId) => api.post(`/web-customers/${id}/link-customer`, { customerId }),
  unlinkCustomer: (id) => api.post(`/web-customers/${id}/unlink-customer`),
  setSmsOptOut: (id, optOut) => api.post(`/web-customers/${id}/sms-opt-out`, { optOut }),
  getWishlist: (id, params) => api.get(`/web-customers/${id}/wishlist`, { params }),
  getLoyalty: (id) => api.get(`/web-customers/${id}/loyalty`),
  adjustLoyalty: (id, amount, reason) => api.post(`/web-customers/${id}/loyalty/adjust`, { amount, reason })
}

// Web Campaigns API (online shop SMS marketing) - Backend: /api/v1/web-campaigns
export const webCampaignsApi = {
  getAll: (params) => api.get('/web-campaigns', { params }),
  getById: (id) => api.get(`/web-campaigns/${id}`),
  create: (data) => api.post('/web-campaigns', data),
  update: (id, data) => api.put(`/web-campaigns/${id}`, data),
  remove: (id) => api.delete(`/web-campaigns/${id}`),
  preview: (id) => api.post(`/web-campaigns/${id}/preview`),
  send: (id) => api.post(`/web-campaigns/${id}/send`)
}

// AP Reports API - Backend: /api/v1/ap/reports
export const apReportsApi = {
  getAging: () => api.get('/ap/reports/aging'),
  getVendorBalance: () => api.get('/ap/reports/vendor-balance'),
  getVendorStatement: (vendorId) => api.get(`/ap/reports/vendor/${vendorId}/statement`)
}

// Bank Accounts API - Backend: /api/v1/finance/bank-accounts
export const bankAccountsApi = {
  getAll: (params) => api.get('/finance/bank-accounts', { params }),
  getAllList: () => api.get('/finance/bank-accounts/all'),
  getActive: () => api.get('/finance/bank-accounts/active'),
  getByType: (type) => api.get(`/finance/bank-accounts/by-type/${type}`),
  getPaymentAccounts: () => api.get('/finance/bank-accounts/payment-accounts'),
  getReceiptAccounts: () => api.get('/finance/bank-accounts/receipt-accounts'),
  getDefault: () => api.get('/finance/bank-accounts/default'),
  getById: (id) => api.get(`/finance/bank-accounts/${id}`),
  getByCode: (code) => api.get(`/finance/bank-accounts/code/${code}`),
  search: (query, params) => api.get('/finance/bank-accounts/search', { params: { query, ...params } }),
  create: (data) => api.post('/finance/bank-accounts', data),
  update: (id, data) => api.put(`/finance/bank-accounts/${id}`, data),
  activate: (id) => api.put(`/finance/bank-accounts/${id}/activate`),
  deactivate: (id) => api.put(`/finance/bank-accounts/${id}/deactivate`),
  setDefault: (id) => api.put(`/finance/bank-accounts/${id}/set-default`)
}

// Bank Transactions API - Backend: /api/v1/finance/bank-transactions
export const bankTransactionsApi = {
  getAll: (params) => api.get('/finance/bank-transactions', { params }),
  getByAccount: (bankAccountId, params) => api.get(`/finance/bank-transactions/by-account/${bankAccountId}`, { params }),
  getByAccountDateRange: (bankAccountId, startDate, endDate) => api.get(`/finance/bank-transactions/by-account/${bankAccountId}/date-range`, { params: { startDate, endDate } }),
  getById: (id) => api.get(`/finance/bank-transactions/${id}`),
  create: (data) => api.post('/finance/bank-transactions', data),
  transfer: (fromAccountId, toAccountId, amount, transactionDate, description) => api.post('/finance/bank-transactions/transfer', null, { params: { fromAccountId, toAccountId, amount, transactionDate, description: description || undefined } }),
  clear: (id) => api.put(`/finance/bank-transactions/${id}/clear`),
  void: (id, reason) => api.put(`/finance/bank-transactions/${id}/void`, null, { params: { reason: reason || undefined } }),
  getCashFlow: (bankAccountId, startDate, endDate) => api.get(`/finance/bank-transactions/cash-flow/${bankAccountId}`, { params: { startDate, endDate } })
}

// Bank Reconciliation API - Backend: /api/v1/finance/bank-reconciliations
export const bankReconciliationsApi = {
  getAll: (params) => api.get('/finance/bank-reconciliations', { params }),
  getByAccount: (bankAccountId, params) => api.get(`/finance/bank-reconciliations/by-account/${bankAccountId}`, { params }),
  getById: (id) => api.get(`/finance/bank-reconciliations/${id}`),
  getUnreconciled: (bankAccountId, endDate) => api.get(`/finance/bank-reconciliations/unreconciled/${bankAccountId}`, { params: { endDate } }),
  create: (data) => api.post('/finance/bank-reconciliations', data),
  start: (id) => api.put(`/finance/bank-reconciliations/${id}/start`),
  reconcileTransactions: (data) => api.post('/finance/bank-reconciliations/reconcile-transactions', data),
  complete: (id) => api.put(`/finance/bank-reconciliations/${id}/complete`),
  cancel: (id, reason) => api.put(`/finance/bank-reconciliations/${id}/cancel`, null, { params: { reason: reason || undefined } })
}

// Credit Notes API - Backend: /api/v1/finance/credit-notes
export const creditNotesApi = {
  getAll: (params) => api.get('/finance/credit-notes', { params }),
  getByCustomer: (customerId, params) => api.get(`/finance/credit-notes/customer/${customerId}`, { params }),
  getByStatus: (status, params) => api.get(`/finance/credit-notes/status/${status}`, { params }),
  getById: (id) => api.get(`/finance/credit-notes/${id}`),
  getByNumber: (number) => api.get(`/finance/credit-notes/number/${number}`),
  getAvailableByCustomer: (customerId) => api.get(`/finance/credit-notes/customer/${customerId}/available`),
  getAvailableCredit: (customerId) => api.get(`/finance/credit-notes/customer/${customerId}/available-credit`),
  create: (data) => api.post('/finance/credit-notes', data),
  update: (id, data) => api.put(`/finance/credit-notes/${id}`, data),
  approve: (id) => api.post(`/finance/credit-notes/${id}/approve`),
  applyToInvoice: (id, invoiceId, amount) => api.post(`/finance/credit-notes/${id}/apply-to-invoice`, null, { params: { invoiceId, amount } }),
  cancel: (id, reason) => api.post(`/finance/credit-notes/${id}/cancel`, null, { params: { reason } })
}

// Currencies API - Backend: /api/v1/finance/currencies
export const currenciesApi = {
  getAll: (params) => api.get('/finance/currencies', { params }),
  getAllList: () => api.get('/finance/currencies/all'),
  getActive: () => api.get('/finance/currencies/active'),
  getBase: () => api.get('/finance/currencies/base'),
  getById: (id) => api.get(`/finance/currencies/${id}`),
  getByCode: (code) => api.get(`/finance/currencies/code/${code}`),
  create: (data) => api.post('/finance/currencies', data),
  update: (id, data) => api.put(`/finance/currencies/${id}`, data),
  activate: (id) => api.put(`/finance/currencies/${id}/activate`),
  deactivate: (id) => api.put(`/finance/currencies/${id}/deactivate`),
  setBase: (id) => api.put(`/finance/currencies/${id}/set-base`)
}

// Exchange Rates API - Backend: /api/v1/finance/exchange-rates
export const exchangeRatesApi = {
  getAll: (params) => api.get('/finance/exchange-rates', { params }),
  getAllList: () => api.get('/finance/exchange-rates/all'),
  getCurrent: () => api.get('/finance/exchange-rates/current'),
  getById: (id) => api.get(`/finance/exchange-rates/${id}`),
  getEffective: (from, to, date) => api.get('/finance/exchange-rates/effective', { params: { from, to, date } }),
  getPair: (from, to) => api.get('/finance/exchange-rates/pair', { params: { from, to } }),
  getByDate: (date) => api.get('/finance/exchange-rates/by-date', { params: { date } }),
  convert: (amount, from, to, date) => api.get('/finance/exchange-rates/convert', { params: { amount, from, to, date } }),
  create: (data) => api.post('/finance/exchange-rates', data),
  update: (id, data) => api.put(`/finance/exchange-rates/${id}`, data),
  activate: (id) => api.put(`/finance/exchange-rates/${id}/activate`),
  deactivate: (id) => api.put(`/finance/exchange-rates/${id}/deactivate`),
  delete: (id) => api.delete(`/finance/exchange-rates/${id}`)
}

// Fiscal Periods API - Backend: /api/v1/finance/fiscal
export const fiscalApi = {
  getYears: (params) => api.get('/finance/fiscal/years', { params }),
  getAllYears: () => api.get('/finance/fiscal/years/all'),
  getCurrentYear: () => api.get('/finance/fiscal/years/current'),
  getYearById: (id) => api.get(`/finance/fiscal/years/${id}`),
  getYearDetails: (id) => api.get(`/finance/fiscal/years/${id}/details`),
  getYearByDate: (date) => api.get('/finance/fiscal/years/by-date', { params: { date } }),
  createYear: (data) => api.post('/finance/fiscal/years', data),
  updateYear: (id, data) => api.put(`/finance/fiscal/years/${id}`, data),
  setCurrentYear: (id) => api.put(`/finance/fiscal/years/${id}/set-current`),
  closeYear: (id) => api.post(`/finance/fiscal/years/${id}/close`),
  getPeriods: (yearId) => api.get(`/finance/fiscal/years/${yearId}/periods`),
  getPeriodById: (id) => api.get(`/finance/fiscal/periods/${id}`),
  getPeriodByDate: (date) => api.get('/finance/fiscal/periods/by-date', { params: { date } }),
  getOpenPeriods: () => api.get('/finance/fiscal/periods/open'),
  closePeriod: (id) => api.post(`/finance/fiscal/periods/${id}/close`),
  reopenPeriod: (id, reason) => api.post(`/finance/fiscal/periods/${id}/reopen`, null, { params: { reason } })
}

// Recurring Journals API - Backend: /api/v1/finance/recurring-journals
export const recurringJournalsApi = {
  getAll: (params) => api.get('/finance/recurring-journals', { params }),
  getById: (id) => api.get(`/finance/recurring-journals/${id}`),
  create: (data) => api.post('/finance/recurring-journals', data),
  update: (id, data) => api.put(`/finance/recurring-journals/${id}`, data),
  delete: (id) => api.delete(`/finance/recurring-journals/${id}`),
  activate: (id) => api.put(`/finance/recurring-journals/${id}/activate`),
  deactivate: (id) => api.put(`/finance/recurring-journals/${id}/deactivate`),
  execute: (id) => api.post(`/finance/recurring-journals/${id}/execute`)
}

// Tax Codes API - Backend: /api/v1/finance/tax-codes
export const taxCodesApi = {
  getAll: (params) => api.get('/finance/tax-codes', { params }),
  getAllList: () => api.get('/finance/tax-codes/all'),
  getActive: () => api.get('/finance/tax-codes/active'),
  getByType: (type) => api.get(`/finance/tax-codes/by-type/${type}`),
  getSales: () => api.get('/finance/tax-codes/sales'),
  getPurchases: () => api.get('/finance/tax-codes/purchases'),
  getById: (id) => api.get(`/finance/tax-codes/${id}`),
  getByCode: (code) => api.get(`/finance/tax-codes/code/${code}`),
  search: (query, params) => api.get('/finance/tax-codes/search', { params: { query, ...params } }),
  create: (data) => api.post('/finance/tax-codes', data),
  update: (id, data) => api.put(`/finance/tax-codes/${id}`, data),
  activate: (id) => api.put(`/finance/tax-codes/${id}/activate`),
  deactivate: (id) => api.put(`/finance/tax-codes/${id}/deactivate`),
  getRates: (taxCodeId) => api.get(`/finance/tax-codes/${taxCodeId}/rates`),
  createRate: (data) => api.post('/finance/tax-codes/rates', data),
  updateRate: (rateId, data) => api.put(`/finance/tax-codes/rates/${rateId}`, data),
  deleteRate: (rateId) => api.delete(`/finance/tax-codes/rates/${rateId}`),
  calculate: (data) => api.post('/finance/tax-codes/calculate', data),
  getVatSummary: (periodStart, periodEnd) => api.get('/finance/tax-codes/reports/vat-summary', { params: { periodStart, periodEnd } }),
  getSummaryByType: (periodStart, periodEnd) => api.get('/finance/tax-codes/reports/summary-by-type', { params: { periodStart, periodEnd } })
}

// POS Coupons API - Backend: /api/v1/pos/coupons
export const couponsApi = {
  getAll: (params) => api.get('/pos/coupons', { params }),
  getByPromotion: (promotionId, params) => api.get(`/pos/coupons/promotion/${promotionId}`, { params }),
  getByStatus: (status, params) => api.get(`/pos/coupons/status/${status}`, { params }),
  getById: (id) => api.get(`/pos/coupons/${id}`),
  getByCode: (code) => api.get(`/pos/coupons/code/${code}`),
  create: (data) => api.post('/pos/coupons', data),
  generate: (promotionId, count, data) => api.post(`/pos/coupons/generate/${promotionId}`, data, { params: { count } }),
  update: (id, data) => api.put(`/pos/coupons/${id}`, data),
  delete: (id) => api.delete(`/pos/coupons/${id}`),
  activate: (id) => api.post(`/pos/coupons/${id}/activate`),
  deactivate: (id) => api.post(`/pos/coupons/${id}/deactivate`),
  cancel: (id) => api.post(`/pos/coupons/${id}/cancel`),
  getRedemptions: (id) => api.get(`/pos/coupons/${id}/redemptions`),
  updateExpired: () => api.post('/pos/coupons/update-expired'),
  updateDepleted: () => api.post('/pos/coupons/update-depleted')
}

// POS Promotions API - Backend: /api/v1/pos/promotions
export const promotionsApi = {
  getAll: (params) => api.get('/pos/promotions', { params }),
  search: (query, params) => api.get('/pos/promotions/search', { params: { query, ...params } }),
  getActive: (locationId) => api.get('/pos/promotions/active', { params: { locationId: locationId || undefined } }),
  getById: (id) => api.get(`/pos/promotions/${id}`),
  getByCode: (code) => api.get(`/pos/promotions/code/${code}`),
  create: (data) => api.post('/pos/promotions', data),
  update: (id, data) => api.put(`/pos/promotions/${id}`, data),
  delete: (id) => api.delete(`/pos/promotions/${id}`),
  activate: (id) => api.post(`/pos/promotions/${id}/activate`),
  deactivate: (id) => api.post(`/pos/promotions/${id}/deactivate`),
  addCondition: (promotionId, data) => api.post(`/pos/promotions/${promotionId}/conditions`, data),
  removeCondition: (promotionId, conditionId) => api.delete(`/pos/promotions/${promotionId}/conditions/${conditionId}`),
  addAction: (promotionId, data) => api.post(`/pos/promotions/${promotionId}/actions`, data),
  removeAction: (promotionId, actionId) => api.delete(`/pos/promotions/${promotionId}/actions/${actionId}`)
}

// POS Price Lists API - Backend: /api/v1/pos/price-lists
export const priceListsApi = {
  getAll: (params) => api.get('/pos/price-lists', { params }),
  getActive: () => api.get('/pos/price-lists/active'),
  getById: (id) => api.get(`/pos/price-lists/${id}`),
  getByCode: (code) => api.get(`/pos/price-lists/code/${code}`),
  create: (data) => api.post('/pos/price-lists', data),
  update: (id, data) => api.put(`/pos/price-lists/${id}`, data),
  delete: (id) => api.delete(`/pos/price-lists/${id}`),
  activate: (id) => api.post(`/pos/price-lists/${id}/activate`),
  deactivate: (id) => api.post(`/pos/price-lists/${id}/deactivate`),
  getItems: (priceListId, params) => api.get(`/pos/price-lists/${priceListId}/items`, { params }),
  getItem: (priceListId, itemId) => api.get(`/pos/price-lists/${priceListId}/items/${itemId}`),
  addItem: (priceListId, data) => api.post(`/pos/price-lists/${priceListId}/items`, data),
  updateItem: (priceListId, itemId, data) => api.put(`/pos/price-lists/${priceListId}/items/${itemId}`, data),
  removeItem: (priceListId, itemId) => api.delete(`/pos/price-lists/${priceListId}/items/${itemId}`),
  getByCustomer: (customerId) => api.get(`/pos/price-lists/customer/${customerId}`),
  assignCustomer: (priceListId, customerId, priority) => api.post(`/pos/price-lists/${priceListId}/assign-customer/${customerId}`, null, { params: { priority: priority || undefined } }),
  unassignCustomer: (priceListId, customerId) => api.delete(`/pos/price-lists/${priceListId}/unassign-customer/${customerId}`)
}

// POS Pricing API - Backend: /api/v1/pos/pricing
export const pricingApi = {
  calculate: (data) => api.post('/pos/pricing/calculate', data),
  getProductPrice: (productId, params) => api.get(`/pos/pricing/product/${productId}`, { params }),
  applyCoupon: (data) => api.post('/pos/pricing/apply-coupon', data),
  validateCoupon: (couponCode, customerId) => api.post('/pos/pricing/validate-coupon', null, { params: { couponCode, customerId: customerId || undefined } }),
  recordCouponRedemption: (couponCode, customerId, orderId, discountApplied) => api.post('/pos/pricing/record-coupon-redemption', null, { params: { couponCode, customerId, orderId, discountApplied } })
}
