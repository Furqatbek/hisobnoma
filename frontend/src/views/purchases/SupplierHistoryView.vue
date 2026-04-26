<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { suppliersApi, expensesApi, apPaymentsApi, purchaseOrdersApi, apReportsApi } from '@/services/api'
import {
  MagnifyingGlassIcon,
  BuildingStorefrontIcon,
  BanknotesIcon,
  DocumentTextIcon,
  TruckIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  PhoneIcon,
  EnvelopeIcon,
  MapPinIcon,
  ClockIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// State
const loading = ref(true)
const suppliers = ref([])
const selectedSupplierId = ref(null)
const supplierSearch = ref('')
const activeTab = ref('overview')

// Supplier data
const supplier = ref(null)
const supplierBalance = ref(null)
const invoices = ref([])
const payments = ref([])
const purchaseOrders = ref([])
const loadingDetail = ref(false)

// AP Reports data
const vendorStatement = ref(null)
const agingSummary = ref(null)
const loadingAging = ref(false)

// Pagination
const invoicePage = ref(0)
const invoiceTotalPages = ref(0)
const paymentPage = ref(0)
const paymentTotalPages = ref(0)
const poPage = ref(0)
const poTotalPages = ref(0)

// Expanded rows
const expandedInvoice = ref(null)
const expandedPO = ref(null)

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleDateString('uz-UZ')
}

// Fetch suppliers list
async function fetchSuppliers() {
  try {
    const res = await suppliersApi.getAll({ page: 0, size: 1000 })
    const data = res.data.data || res.data
    suppliers.value = data.content || data || []
  } catch (e) {
    console.error('Failed to load suppliers:', e)
  }
}

const filteredSuppliers = computed(() => {
  if (!supplierSearch.value) return suppliers.value
  const q = supplierSearch.value.toLowerCase()
  return suppliers.value.filter(s =>
    s.name?.toLowerCase().includes(q) ||
    s.code?.toLowerCase().includes(q) ||
    s.phone?.includes(q)
  )
})

// Select supplier
async function selectSupplier(s) {
  selectedSupplierId.value = s.id
  supplier.value = s
  activeTab.value = 'overview'
  await loadSupplierData()
}

async function loadSupplierData() {
  if (!selectedSupplierId.value) return
  loadingDetail.value = true

  try {
    const [balanceRes, invoiceRes, paymentRes, poRes, statementRes] = await Promise.all([
      expensesApi.getVendorBalance(selectedSupplierId.value).catch(() => null),
      expensesApi.getByVendor(selectedSupplierId.value, { page: 0, size: 10, sort: 'invoiceDate,desc' }).catch(() => null),
      apPaymentsApi.getByVendor(selectedSupplierId.value, { page: 0, size: 10, sort: 'createdAt,desc' }).catch(() => null),
      purchaseOrdersApi.getByVendor(selectedSupplierId.value, { page: 0, size: 10, sort: 'createdAt,desc' }).catch(() => null),
      apReportsApi.getVendorStatement(selectedSupplierId.value).catch(() => null)
    ])

    if (balanceRes) {
      supplierBalance.value = balanceRes.data.data || balanceRes.data
    }

    if (invoiceRes) {
      const iData = invoiceRes.data.data || invoiceRes.data
      invoices.value = iData.content || iData || []
      invoiceTotalPages.value = iData.totalPages || 0
      invoicePage.value = 0
    }

    if (paymentRes) {
      const pData = paymentRes.data.data || paymentRes.data
      payments.value = pData.content || pData || []
      paymentTotalPages.value = pData.totalPages || 0
      paymentPage.value = 0
    }

    if (poRes) {
      const poData = poRes.data.data || poRes.data
      purchaseOrders.value = poData.content || poData || []
      poTotalPages.value = poData.totalPages || 0
      poPage.value = 0
    }

    if (statementRes) {
      vendorStatement.value = statementRes.data.data || statementRes.data
    }
  } catch (e) {
    console.error('Failed to load supplier data:', e)
  } finally {
    loadingDetail.value = false
  }
}

// Pagination handlers
async function loadInvoicePage(page) {
  try {
    const res = await expensesApi.getByVendor(selectedSupplierId.value, { page, size: 10, sort: 'invoiceDate,desc' })
    const data = res.data.data || res.data
    invoices.value = data.content || data || []
    invoiceTotalPages.value = data.totalPages || 0
    invoicePage.value = page
  } catch (e) {
    console.error('Failed to load invoices:', e)
  }
}

async function loadPaymentPage(page) {
  try {
    const res = await apPaymentsApi.getByVendor(selectedSupplierId.value, { page, size: 10, sort: 'createdAt,desc' })
    const data = res.data.data || res.data
    payments.value = data.content || data || []
    paymentTotalPages.value = data.totalPages || 0
    paymentPage.value = page
  } catch (e) {
    console.error('Failed to load payments:', e)
  }
}

async function loadPOPage(page) {
  try {
    const res = await purchaseOrdersApi.getByVendor(selectedSupplierId.value, { page, size: 10, sort: 'createdAt,desc' })
    const data = res.data.data || res.data
    purchaseOrders.value = data.content || data || []
    poTotalPages.value = data.totalPages || 0
    poPage.value = page
  } catch (e) {
    console.error('Failed to load purchase orders:', e)
  }
}

// Summary stats
const stats = computed(() => {
  const bal = supplierBalance.value
  return {
    totalInvoiced: bal?.totalInvoiced || supplier.value?.totalInvoiced || 0,
    totalPaid: bal?.totalPaid || supplier.value?.totalPaid || 0,
    currentBalance: bal?.netBalance || supplier.value?.currentBalance || 0,
    paymentTerms: supplier.value?.paymentTerms || '—',
    paymentTermsDays: supplier.value?.paymentTermsDays || 0
  }
})

function getInvoiceStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'PENDING': 'badge-warning',
    'APPROVED': 'badge-info',
    'PARTIAL': 'badge-warning',
    'PAID': 'badge-success',
    'OVERDUE': 'badge-danger',
    'CANCELLED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getPaymentStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'SUBMITTED': 'badge-warning',
    'APPROVED': 'badge-info',
    'COMPLETED': 'badge-success',
    'VOIDED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getPOStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'SUBMITTED': 'badge-warning',
    'APPROVED': 'badge-info',
    'PARTIALLY_RECEIVED': 'badge-warning',
    'RECEIVED': 'badge-success',
    'CANCELLED': 'badge-danger',
    'CLOSED': 'badge-success'
  }
  return classes[status] || 'badge-info'
}

async function loadAgingSummary() {
  loadingAging.value = true
  try {
    const res = await apReportsApi.getAging()
    agingSummary.value = res.data.data || res.data
  } catch (e) {
    console.error('Failed to load aging summary:', e)
  } finally {
    loadingAging.value = false
  }
}

async function loadVendorBalance() {
  try {
    const res = await apReportsApi.getVendorBalance()
    return res.data.data || res.data
  } catch (e) {
    return null
  }
}

onMounted(async () => {
  await fetchSuppliers()

  // If navigated with supplier id param
  if (route.params.id) {
    const s = suppliers.value.find(s => s.id == route.params.id)
    if (s) {
      await selectSupplier(s)
    } else {
      try {
        const res = await suppliersApi.getById(route.params.id)
        supplier.value = res.data.data || res.data
        selectedSupplierId.value = supplier.value.id
        await loadSupplierData()
      } catch (e) {
        console.error('Supplier not found:', e)
      }
    }
  }

  loading.value = false
})
</script>

<template>
  <div>
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ t('supplierHistory.title') }}</h1>
      <p class="text-sm text-gray-500 mt-1">{{ t('supplierHistory.subtitle') }}</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
      <!-- Left sidebar: Supplier list -->
      <div class="lg:col-span-1">
        <div class="card">
          <div class="card-body">
            <!-- Search -->
            <div class="relative mb-4">
              <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                v-model="supplierSearch"
                type="text"
                :placeholder="t('supplierHistory.searchSupplier')"
                class="input pl-9 w-full text-sm"
              />
            </div>

            <!-- Supplier list -->
            <div v-if="loading" class="flex justify-center py-8">
              <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
            </div>
            <div v-else class="space-y-1 max-h-[calc(100vh-280px)] overflow-y-auto">
              <button
                v-for="s in filteredSuppliers"
                :key="s.id"
                @click="selectSupplier(s)"
                :class="[
                  'w-full text-left px-3 py-2.5 rounded-lg transition-colors text-sm',
                  selectedSupplierId === s.id
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'hover:bg-gray-50 text-gray-700'
                ]"
              >
                <div class="font-medium truncate">{{ s.name }}</div>
                <div class="flex items-center justify-between mt-0.5">
                  <span class="text-xs text-gray-400">{{ s.code }}</span>
                  <span v-if="s.currentBalance > 0" class="text-xs font-medium text-red-600">
                    {{ formatCurrency(s.currentBalance) }} {{ t('sum') }}
                  </span>
                </div>
              </button>
              <p v-if="filteredSuppliers.length === 0" class="text-sm text-gray-400 text-center py-4">
                {{ t('supplierHistory.noSuppliers') }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Right content: Supplier details -->
      <div class="lg:col-span-3">
        <!-- No supplier selected -->
        <div v-if="!selectedSupplierId" class="card">
          <div class="card-body flex flex-col items-center justify-center py-16">
            <BuildingStorefrontIcon class="h-12 w-12 text-gray-300 mb-3" />
            <p class="text-gray-400">{{ t('supplierHistory.selectSupplierHint') }}</p>
          </div>
        </div>

        <!-- Supplier detail -->
        <div v-else>
          <!-- Loading -->
          <div v-if="loadingDetail" class="flex items-center justify-center h-64">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>

          <div v-else>
            <!-- Supplier profile card -->
            <div class="card mb-6">
              <div class="card-body">
                <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                  <div>
                    <h2 class="text-xl font-bold text-gray-900">{{ supplier?.name }}</h2>
                    <div class="flex flex-wrap items-center gap-3 mt-2 text-sm text-gray-500">
                      <span v-if="supplier?.code" class="inline-flex items-center">
                        <span class="font-mono bg-gray-100 px-2 py-0.5 rounded text-xs">{{ supplier.code }}</span>
                      </span>
                      <span v-if="supplier?.contactPerson" class="inline-flex items-center gap-1">
                        {{ supplier.contactPerson }}
                      </span>
                      <span v-if="supplier?.phone" class="inline-flex items-center gap-1">
                        <PhoneIcon class="h-3.5 w-3.5" /> {{ supplier.phone }}
                      </span>
                      <span v-if="supplier?.email" class="inline-flex items-center gap-1">
                        <EnvelopeIcon class="h-3.5 w-3.5" /> {{ supplier.email }}
                      </span>
                      <span v-if="supplier?.address" class="inline-flex items-center gap-1">
                        <MapPinIcon class="h-3.5 w-3.5" /> {{ supplier.address }}
                      </span>
                    </div>
                    <div v-if="supplier?.bankName || supplier?.bankAccount" class="mt-2 text-sm text-gray-500">
                      <span v-if="supplier?.bankName">{{ supplier.bankName }}</span>
                      <span v-if="supplier?.bankAccount" class="ml-2 font-mono text-xs">{{ supplier.bankAccount }}</span>
                    </div>
                  </div>
                  <div class="flex-shrink-0 flex items-center gap-2">
                    <span v-if="supplier?.preferred" class="badge badge-success">{{ t('supplierHistory.preferred') }}</span>
                    <span v-if="!supplier?.active" class="badge badge-danger">{{ t('supplierHistory.inactive') }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Summary cards -->
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('supplierHistory.totalInvoiced') }}</p>
                  <p class="text-lg font-bold text-gray-900 mt-1">{{ formatCurrency(stats.totalInvoiced) }}</p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('supplierHistory.totalPaid') }}</p>
                  <p class="text-lg font-bold text-green-600 mt-1">{{ formatCurrency(stats.totalPaid) }}</p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('supplierHistory.currentDebt') }}</p>
                  <p :class="['text-lg font-bold mt-1', stats.currentBalance > 0 ? 'text-red-600' : 'text-gray-900']">
                    {{ formatCurrency(stats.currentBalance) }}
                  </p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('supplierHistory.paymentTerms') }}</p>
                  <p class="text-lg font-bold text-gray-900 mt-1">
                    {{ stats.paymentTermsDays > 0 ? stats.paymentTermsDays + ' ' + t('supplierHistory.days') : stats.paymentTerms }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Tabs -->
            <div class="border-b border-gray-200 mb-4">
              <nav class="flex -mb-px space-x-6">
                <button
                  v-for="tab in ['overview', 'invoices', 'payments', 'purchaseOrders', 'aging']"
                  :key="tab"
                  @click="activeTab = tab"
                  :class="[
                    'py-3 px-1 border-b-2 text-sm font-medium transition-colors',
                    activeTab === tab
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  ]"
                >
                  {{ t('supplierHistory.tabs.' + tab) }}
                  <span v-if="tab === 'invoices' && invoices.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ invoices.length }}
                  </span>
                  <span v-if="tab === 'payments' && payments.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ payments.length }}
                  </span>
                  <span v-if="tab === 'purchaseOrders' && purchaseOrders.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ purchaseOrders.length }}
                  </span>
                </button>
              </nav>
            </div>

            <!-- ==================== OVERVIEW TAB ==================== -->
            <div v-if="activeTab === 'overview'" class="space-y-6">
              <!-- Recent invoices -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('supplierHistory.recentInvoices') }}</h3>
                  <button @click="activeTab = 'invoices'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('supplierHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="invoices.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.noInvoices') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('supplierHistory.invoiceNumber') }}</th>
                        <th class="text-left">{{ t('date') }}</th>
                        <th class="text-right">{{ t('total') }}</th>
                        <th class="text-right">{{ t('supplierHistory.paid') }}</th>
                        <th class="text-right">{{ t('balance') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="inv in invoices.slice(0, 5)" :key="inv.id" class="hover:bg-gray-50">
                        <td class="font-mono text-sm">{{ inv.invoiceNumber }}</td>
                        <td class="text-sm">{{ formatDate(inv.invoiceDate) }}</td>
                        <td class="text-right text-sm font-medium">{{ formatCurrency(inv.totalAmount) }}</td>
                        <td class="text-right text-sm text-green-600">{{ formatCurrency(inv.paidAmount) }}</td>
                        <td class="text-right text-sm" :class="inv.balanceDue > 0 ? 'text-red-600 font-medium' : 'text-gray-500'">
                          {{ formatCurrency(inv.balanceDue) }}
                        </td>
                        <td class="text-center">
                          <span :class="['badge', getInvoiceStatusClass(inv.status)]">
                            {{ t('enums.apInvoiceStatus.' + inv.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Recent payments -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('supplierHistory.recentPayments') }}</h3>
                  <button @click="activeTab = 'payments'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('supplierHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="payments.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.noPayments') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('supplierHistory.paymentNumber') }}</th>
                        <th class="text-left">{{ t('date') }}</th>
                        <th class="text-left">{{ t('supplierHistory.method') }}</th>
                        <th class="text-right">{{ t('amount') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="pay in payments.slice(0, 5)" :key="pay.id" class="hover:bg-gray-50">
                        <td class="font-mono text-sm">{{ pay.paymentNumber }}</td>
                        <td class="text-sm">{{ formatDate(pay.paymentDate) }}</td>
                        <td class="text-sm">{{ t('enums.paymentMethod.' + pay.paymentMethod) }}</td>
                        <td class="text-right text-sm font-medium text-green-600">{{ formatCurrency(pay.paymentAmount) }}</td>
                        <td class="text-center">
                          <span :class="['badge', getPaymentStatusClass(pay.status)]">
                            {{ t('enums.apPaymentStatus.' + pay.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Recent purchase orders -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('supplierHistory.recentPurchaseOrders') }}</h3>
                  <button @click="activeTab = 'purchaseOrders'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('supplierHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="purchaseOrders.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.noPurchaseOrders') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('supplierHistory.poNumber') }}</th>
                        <th class="text-left">{{ t('date') }}</th>
                        <th class="text-right">{{ t('total') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="po in purchaseOrders.slice(0, 5)" :key="po.id" class="hover:bg-gray-50">
                        <td class="font-mono text-sm">{{ po.orderNumber }}</td>
                        <td class="text-sm">{{ formatDate(po.orderDate || po.createdAt) }}</td>
                        <td class="text-right text-sm font-medium">{{ formatCurrency(po.totalAmount) }}</td>
                        <td class="text-center">
                          <span :class="['badge', getPOStatusClass(po.status)]">
                            {{ t('enums.purchaseOrderStatus.' + po.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- ==================== INVOICES TAB ==================== -->
            <div v-if="activeTab === 'invoices'">
              <div class="card">
                <div class="card-body p-0">
                  <div v-if="invoices.length === 0" class="py-12 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.noInvoices') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th></th>
                        <th class="text-left">{{ t('supplierHistory.invoiceNumber') }}</th>
                        <th class="text-left">{{ t('supplierHistory.invoiceDate') }}</th>
                        <th class="text-left">{{ t('supplierHistory.dueDate') }}</th>
                        <th class="text-right">{{ t('total') }}</th>
                        <th class="text-right">{{ t('supplierHistory.paid') }}</th>
                        <th class="text-right">{{ t('balance') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template v-for="inv in invoices" :key="inv.id">
                        <tr class="hover:bg-gray-50 cursor-pointer" @click="expandedInvoice = expandedInvoice === inv.id ? null : inv.id">
                          <td class="w-8">
                            <ChevronDownIcon v-if="expandedInvoice !== inv.id" class="h-4 w-4 text-gray-400" />
                            <ChevronUpIcon v-else class="h-4 w-4 text-primary-600" />
                          </td>
                          <td class="font-mono text-sm">{{ inv.invoiceNumber }}</td>
                          <td class="text-sm">{{ formatDate(inv.invoiceDate) }}</td>
                          <td class="text-sm">
                            {{ formatDate(inv.dueDate) }}
                            <span v-if="inv.overdue" class="ml-1 text-xs text-red-500">({{ inv.daysOverdue }}{{ t('supplierHistory.daysLate') }})</span>
                          </td>
                          <td class="text-right text-sm font-medium">{{ formatCurrency(inv.totalAmount) }}</td>
                          <td class="text-right text-sm text-green-600">{{ formatCurrency(inv.paidAmount) }}</td>
                          <td class="text-right text-sm" :class="inv.balanceDue > 0 ? 'text-red-600 font-medium' : 'text-gray-500'">
                            {{ formatCurrency(inv.balanceDue) }}
                          </td>
                          <td class="text-center">
                            <span :class="['badge', getInvoiceStatusClass(inv.status)]">
                              {{ t('enums.apInvoiceStatus.' + inv.status) }}
                            </span>
                          </td>
                        </tr>
                        <!-- Expanded details -->
                        <tr v-if="expandedInvoice === inv.id">
                          <td colspan="8" class="bg-gray-50 px-6 py-4">
                            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm mb-3">
                              <div>
                                <span class="text-gray-500">{{ t('supplierHistory.subtotal') }}:</span>
                                <span class="ml-1 font-medium">{{ formatCurrency(inv.subtotal) }}</span>
                              </div>
                              <div v-if="inv.discountAmount > 0">
                                <span class="text-gray-500">{{ t('supplierHistory.discount') }}:</span>
                                <span class="ml-1 font-medium text-orange-600">-{{ formatCurrency(inv.discountAmount) }}</span>
                              </div>
                              <div v-if="inv.taxAmount > 0">
                                <span class="text-gray-500">{{ t('supplierHistory.tax') }}:</span>
                                <span class="ml-1 font-medium">{{ formatCurrency(inv.taxAmount) }}</span>
                              </div>
                              <div v-if="inv.purchaseOrderNumber">
                                <span class="text-gray-500">{{ t('supplierHistory.poRef') }}:</span>
                                <span class="ml-1 font-mono text-xs">{{ inv.purchaseOrderNumber }}</span>
                              </div>
                            </div>
                            <!-- Invoice lines -->
                            <div v-if="inv.lines && inv.lines.length > 0">
                              <table class="w-full text-sm">
                                <thead>
                                  <tr class="text-gray-500 border-b">
                                    <th class="text-left py-1 font-medium">{{ t('supplierHistory.product') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('quantity') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('price') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('total') }}</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr v-for="line in inv.lines" :key="line.id" class="border-b border-gray-100">
                                    <td class="py-1.5">{{ line.productName || line.description }}</td>
                                    <td class="text-right py-1.5">{{ line.quantity }}</td>
                                    <td class="text-right py-1.5">{{ formatCurrency(line.unitPrice) }}</td>
                                    <td class="text-right py-1.5 font-medium">{{ formatCurrency(line.lineTotal) }}</td>
                                  </tr>
                                </tbody>
                              </table>
                            </div>
                            <p v-if="inv.notes" class="text-xs text-gray-500 mt-2">{{ inv.notes }}</p>
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                  <!-- Pagination -->
                  <div v-if="invoiceTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
                    <button @click="loadInvoicePage(invoicePage - 1)" :disabled="invoicePage === 0" class="btn btn-secondary text-sm" :class="{ 'opacity-50': invoicePage === 0 }">
                      {{ t('previous') }}
                    </button>
                    <span class="text-sm text-gray-500">{{ t('page') }} {{ invoicePage + 1 }} {{ t('of') }} {{ invoiceTotalPages }}</span>
                    <button @click="loadInvoicePage(invoicePage + 1)" :disabled="invoicePage >= invoiceTotalPages - 1" class="btn btn-secondary text-sm" :class="{ 'opacity-50': invoicePage >= invoiceTotalPages - 1 }">
                      {{ t('next') }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- ==================== PAYMENTS TAB ==================== -->
            <div v-if="activeTab === 'payments'">
              <div class="card">
                <div class="card-body p-0">
                  <div v-if="payments.length === 0" class="py-12 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.noPayments') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('supplierHistory.paymentNumber') }}</th>
                        <th class="text-left">{{ t('date') }}</th>
                        <th class="text-left">{{ t('supplierHistory.method') }}</th>
                        <th class="text-right">{{ t('amount') }}</th>
                        <th class="text-right">{{ t('supplierHistory.allocated') }}</th>
                        <th class="text-left">{{ t('supplierHistory.reference') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="pay in payments" :key="pay.id" class="hover:bg-gray-50">
                        <td class="font-mono text-sm">{{ pay.paymentNumber }}</td>
                        <td class="text-sm">{{ formatDate(pay.paymentDate) }}</td>
                        <td class="text-sm">{{ t('enums.paymentMethod.' + pay.paymentMethod) }}</td>
                        <td class="text-right text-sm font-medium text-green-600">{{ formatCurrency(pay.paymentAmount) }}</td>
                        <td class="text-right text-sm">{{ formatCurrency(pay.allocatedAmount) }}</td>
                        <td class="text-sm text-gray-500">{{ pay.referenceNumber || '—' }}</td>
                        <td class="text-center">
                          <span :class="['badge', getPaymentStatusClass(pay.status)]">
                            {{ t('enums.apPaymentStatus.' + pay.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <!-- Pagination -->
                  <div v-if="paymentTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
                    <button @click="loadPaymentPage(paymentPage - 1)" :disabled="paymentPage === 0" class="btn btn-secondary text-sm" :class="{ 'opacity-50': paymentPage === 0 }">
                      {{ t('previous') }}
                    </button>
                    <span class="text-sm text-gray-500">{{ t('page') }} {{ paymentPage + 1 }} {{ t('of') }} {{ paymentTotalPages }}</span>
                    <button @click="loadPaymentPage(paymentPage + 1)" :disabled="paymentPage >= paymentTotalPages - 1" class="btn btn-secondary text-sm" :class="{ 'opacity-50': paymentPage >= paymentTotalPages - 1 }">
                      {{ t('next') }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- ==================== PURCHASE ORDERS TAB ==================== -->
            <div v-if="activeTab === 'purchaseOrders'">
              <div class="card">
                <div class="card-body p-0">
                  <div v-if="purchaseOrders.length === 0" class="py-12 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.noPurchaseOrders') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th></th>
                        <th class="text-left">{{ t('supplierHistory.poNumber') }}</th>
                        <th class="text-left">{{ t('date') }}</th>
                        <th class="text-left">{{ t('supplierHistory.expectedDate') }}</th>
                        <th class="text-right">{{ t('supplierHistory.itemCount') }}</th>
                        <th class="text-right">{{ t('total') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template v-for="po in purchaseOrders" :key="po.id">
                        <tr class="hover:bg-gray-50 cursor-pointer" @click="expandedPO = expandedPO === po.id ? null : po.id">
                          <td class="w-8">
                            <ChevronDownIcon v-if="expandedPO !== po.id" class="h-4 w-4 text-gray-400" />
                            <ChevronUpIcon v-else class="h-4 w-4 text-primary-600" />
                          </td>
                          <td class="font-mono text-sm">{{ po.orderNumber }}</td>
                          <td class="text-sm">{{ formatDate(po.orderDate || po.createdAt) }}</td>
                          <td class="text-sm">{{ formatDate(po.expectedDate) }}</td>
                          <td class="text-right text-sm">{{ po.itemCount || (po.lines ? po.lines.length : 0) }}</td>
                          <td class="text-right text-sm font-medium">{{ formatCurrency(po.totalAmount) }}</td>
                          <td class="text-center">
                            <span :class="['badge', getPOStatusClass(po.status)]">
                              {{ t('enums.purchaseOrderStatus.' + po.status) }}
                            </span>
                          </td>
                        </tr>
                        <!-- Expanded: line items -->
                        <tr v-if="expandedPO === po.id">
                          <td colspan="7" class="bg-gray-50 px-6 py-4">
                            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm mb-3">
                              <div v-if="po.subtotal">
                                <span class="text-gray-500">{{ t('supplierHistory.subtotal') }}:</span>
                                <span class="ml-1 font-medium">{{ formatCurrency(po.subtotal) }}</span>
                              </div>
                              <div v-if="po.discountAmount > 0">
                                <span class="text-gray-500">{{ t('supplierHistory.discount') }}:</span>
                                <span class="ml-1 font-medium text-orange-600">-{{ formatCurrency(po.discountAmount) }}</span>
                              </div>
                              <div v-if="po.taxAmount > 0">
                                <span class="text-gray-500">{{ t('supplierHistory.tax') }}:</span>
                                <span class="ml-1 font-medium">{{ formatCurrency(po.taxAmount) }}</span>
                              </div>
                              <div v-if="po.receivedDate">
                                <span class="text-gray-500">{{ t('supplierHistory.receivedDate') }}:</span>
                                <span class="ml-1">{{ formatDate(po.receivedDate) }}</span>
                              </div>
                            </div>
                            <!-- PO lines -->
                            <div v-if="po.lines && po.lines.length > 0">
                              <table class="w-full text-sm">
                                <thead>
                                  <tr class="text-gray-500 border-b">
                                    <th class="text-left py-1 font-medium">{{ t('supplierHistory.product') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('quantity') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('supplierHistory.received') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('price') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('total') }}</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr v-for="line in po.lines" :key="line.id" class="border-b border-gray-100">
                                    <td class="py-1.5">{{ line.productName || line.description }}</td>
                                    <td class="text-right py-1.5">{{ line.quantity }}</td>
                                    <td class="text-right py-1.5">{{ line.receivedQuantity || 0 }}</td>
                                    <td class="text-right py-1.5">{{ formatCurrency(line.unitPrice) }}</td>
                                    <td class="text-right py-1.5 font-medium">{{ formatCurrency(line.lineTotal) }}</td>
                                  </tr>
                                </tbody>
                              </table>
                            </div>
                            <p v-if="po.notes" class="text-xs text-gray-500 mt-2">{{ po.notes }}</p>
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                  <!-- Pagination -->
                  <div v-if="poTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
                    <button @click="loadPOPage(poPage - 1)" :disabled="poPage === 0" class="btn btn-secondary text-sm" :class="{ 'opacity-50': poPage === 0 }">
                      {{ t('previous') }}
                    </button>
                    <span class="text-sm text-gray-500">{{ t('page') }} {{ poPage + 1 }} {{ t('of') }} {{ poTotalPages }}</span>
                    <button @click="loadPOPage(poPage + 1)" :disabled="poPage >= poTotalPages - 1" class="btn btn-secondary text-sm" :class="{ 'opacity-50': poPage >= poTotalPages - 1 }">
                      {{ t('next') }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- ==================== AGING TAB ==================== -->
            <div v-if="activeTab === 'aging'" class="space-y-6">
              <!-- Vendor Statement -->
              <div class="card">
                <div class="card-header">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('supplierHistory.vendorStatement') }}</h3>
                </div>
                <div class="card-body">
                  <div v-if="!vendorStatement" class="py-8 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.noStatement') }}
                  </div>
                  <template v-else>
                    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
                      <div>
                        <p class="text-xs text-gray-500">{{ t('supplierHistory.totalInvoiced') }}</p>
                        <p class="text-lg font-bold text-gray-900">{{ formatCurrency(vendorStatement.totalInvoiced || vendorStatement.totalAmount) }}</p>
                      </div>
                      <div>
                        <p class="text-xs text-gray-500">{{ t('supplierHistory.totalPaid') }}</p>
                        <p class="text-lg font-bold text-green-600">{{ formatCurrency(vendorStatement.totalPaid) }}</p>
                      </div>
                      <div>
                        <p class="text-xs text-gray-500">{{ t('supplierHistory.currentDebt') }}</p>
                        <p class="text-lg font-bold text-red-600">{{ formatCurrency(vendorStatement.balance || vendorStatement.outstandingBalance) }}</p>
                      </div>
                      <div>
                        <p class="text-xs text-gray-500">{{ t('supplierHistory.overdueAmount') }}</p>
                        <p class="text-lg font-bold text-orange-600">{{ formatCurrency(vendorStatement.overdueAmount) }}</p>
                      </div>
                    </div>
                    <!-- Aging brackets -->
                    <div v-if="vendorStatement.agingBrackets || vendorStatement.aging" class="mt-4">
                      <h4 class="text-sm font-medium text-gray-700 mb-2">{{ t('supplierHistory.agingBreakdown') }}</h4>
                      <table class="table w-full">
                        <thead>
                          <tr>
                            <th class="text-left">{{ t('supplierHistory.period') }}</th>
                            <th class="text-right">{{ t('amount') }}</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(amount, bracket) in (vendorStatement.agingBrackets || vendorStatement.aging)" :key="bracket">
                            <td class="text-sm">{{ bracket }}</td>
                            <td class="text-right text-sm font-medium" :class="amount > 0 ? 'text-red-600' : 'text-gray-500'">
                              {{ formatCurrency(amount) }}
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </template>
                </div>
              </div>

              <!-- Global AP Aging Summary -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('supplierHistory.apAgingSummary') }}</h3>
                  <button @click="loadAgingSummary" class="btn-secondary text-sm" :disabled="loadingAging">
                    {{ loadingAging ? '...' : t('supplierHistory.loadAging') }}
                  </button>
                </div>
                <div class="card-body">
                  <div v-if="!agingSummary" class="py-8 text-center text-sm text-gray-400">
                    {{ t('supplierHistory.clickToLoadAging') }}
                  </div>
                  <template v-else>
                    <div v-if="Array.isArray(agingSummary)" class="table-container">
                      <table class="table w-full">
                        <thead>
                          <tr>
                            <th class="text-left">{{ t('supplierHistory.vendor') }}</th>
                            <th class="text-right">{{ t('supplierHistory.current') }}</th>
                            <th class="text-right">1-30</th>
                            <th class="text-right">31-60</th>
                            <th class="text-right">61-90</th>
                            <th class="text-right">90+</th>
                            <th class="text-right">{{ t('total') }}</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="row in agingSummary" :key="row.vendorId || row.vendorName">
                            <td class="text-sm font-medium">{{ row.vendorName }}</td>
                            <td class="text-right text-sm">{{ formatCurrency(row.current) }}</td>
                            <td class="text-right text-sm">{{ formatCurrency(row.days1to30) }}</td>
                            <td class="text-right text-sm">{{ formatCurrency(row.days31to60) }}</td>
                            <td class="text-right text-sm">{{ formatCurrency(row.days61to90) }}</td>
                            <td class="text-right text-sm text-red-600 font-medium">{{ formatCurrency(row.over90) }}</td>
                            <td class="text-right text-sm font-bold">{{ formatCurrency(row.total) }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                    <div v-else class="grid grid-cols-2 sm:grid-cols-3 gap-4">
                      <div v-for="(value, key) in agingSummary" :key="key">
                        <p class="text-xs text-gray-500">{{ key }}</p>
                        <p class="text-lg font-bold" :class="value > 0 ? 'text-red-600' : 'text-gray-900'">{{ formatCurrency(value) }}</p>
                      </div>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
