<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { customersApi, arInvoicesApi, arPaymentsApi, arReportsApi, posApi } from '@/services/api'
import {
  MagnifyingGlassIcon,
  UserIcon,
  BanknotesIcon,
  DocumentTextIcon,
  ShoppingCartIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ArrowLeftIcon,
  PhoneIcon,
  EnvelopeIcon,
  MapPinIcon,
  ExclamationTriangleIcon,
  ClockIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// State
const loading = ref(true)
const customers = ref([])
const selectedCustomerId = ref(null)
const customerSearch = ref('')
const activeTab = ref('overview')

// Customer data
const customer = ref(null)
const customerBalance = ref(null)
const invoices = ref([])
const payments = ref([])
const transactions = ref([])
const loadingDetail = ref(false)

// Pagination
const invoicePage = ref(0)
const invoiceTotalPages = ref(0)
const paymentPage = ref(0)
const paymentTotalPages = ref(0)
const txPage = ref(0)
const txTotalPages = ref(0)

// Expanded rows
const expandedInvoice = ref(null)
const expandedTx = ref(null)

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

// Fetch customers list
async function fetchCustomers() {
  try {
    const res = await customersApi.getAll({ page: 0, size: 1000 })
    const data = res.data.data || res.data
    customers.value = data.content || data || []
  } catch (e) {
    console.error('Failed to load customers:', e)
  }
}

const filteredCustomers = computed(() => {
  if (!customerSearch.value) return customers.value
  const q = customerSearch.value.toLowerCase()
  return customers.value.filter(c =>
    c.name?.toLowerCase().includes(q) ||
    c.code?.toLowerCase().includes(q) ||
    c.phone?.includes(q)
  )
})

// Select customer
async function selectCustomer(c) {
  selectedCustomerId.value = c.id
  customer.value = c
  activeTab.value = 'overview'
  await loadCustomerData()
}

async function loadCustomerData() {
  if (!selectedCustomerId.value) return
  loadingDetail.value = true

  try {
    const [balanceRes, invoiceRes, paymentRes, txRes] = await Promise.all([
      arReportsApi.getCustomerBalance(selectedCustomerId.value).catch(() => null),
      arInvoicesApi.getByCustomer(selectedCustomerId.value, { page: 0, size: 10, sort: 'invoiceDate,desc' }).catch(() => null),
      arPaymentsApi.getByCustomer(selectedCustomerId.value, { page: 0, size: 10, sort: 'createdAt,desc' }).catch(() => null),
      posApi.getByCustomer(selectedCustomerId.value, { page: 0, size: 10, sort: 'createdAt,desc' }).catch(() => null)
    ])

    if (balanceRes) {
      customerBalance.value = balanceRes.data.data || balanceRes.data
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

    if (txRes) {
      const tData = txRes.data.data || txRes.data
      transactions.value = tData.content || tData || []
      txTotalPages.value = tData.totalPages || 0
      txPage.value = 0
    }
  } catch (e) {
    console.error('Failed to load customer data:', e)
  } finally {
    loadingDetail.value = false
  }
}

// Pagination handlers
async function loadInvoicePage(page) {
  try {
    const res = await arInvoicesApi.getByCustomer(selectedCustomerId.value, { page, size: 10, sort: 'invoiceDate,desc' })
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
    const res = await arPaymentsApi.getByCustomer(selectedCustomerId.value, { page, size: 10, sort: 'createdAt,desc' })
    const data = res.data.data || res.data
    payments.value = data.content || data || []
    paymentTotalPages.value = data.totalPages || 0
    paymentPage.value = page
  } catch (e) {
    console.error('Failed to load payments:', e)
  }
}

async function loadTxPage(page) {
  try {
    const res = await posApi.getByCustomer(selectedCustomerId.value, { page, size: 10, sort: 'createdAt,desc' })
    const data = res.data.data || res.data
    transactions.value = data.content || data || []
    txTotalPages.value = data.totalPages || 0
    txPage.value = page
  } catch (e) {
    console.error('Failed to load transactions:', e)
  }
}

// Summary stats
const stats = computed(() => {
  const bal = customerBalance.value
  return {
    totalInvoiced: customer.value?.totalInvoiced || bal?.outstandingInvoices || 0,
    totalReceived: customer.value?.totalReceived || 0,
    currentBalance: customer.value?.currentBalance || bal?.netBalance || 0,
    creditLimit: customer.value?.creditLimit || bal?.creditLimit || 0,
    availableCredit: customer.value?.availableCredit || bal?.availableCreditLimit || 0
  }
})

function getInvoiceStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'PENDING': 'badge-warning',
    'SENT': 'badge-info',
    'PARTIAL': 'badge-warning',
    'PAID': 'badge-success',
    'OVERDUE': 'badge-danger',
    'CANCELLED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getPaymentStatusClass(status) {
  const classes = {
    'PENDING': 'badge-warning',
    'COMPLETED': 'badge-success',
    'DEPOSITED': 'badge-success',
    'CANCELLED': 'badge-danger',
    'REFUNDED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getTxStatusClass(status) {
  const classes = {
    'PENDING': 'badge-warning',
    'COMPLETED': 'badge-success',
    'VOIDED': 'badge-danger',
    'HELD': 'badge-info',
    'CANCELLED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

onMounted(async () => {
  await fetchCustomers()

  // If navigated with customer id param
  if (route.params.id) {
    const c = customers.value.find(c => c.id == route.params.id)
    if (c) {
      await selectCustomer(c)
    } else {
      // try loading directly
      try {
        const res = await customersApi.getById(route.params.id)
        customer.value = res.data.data || res.data
        selectedCustomerId.value = customer.value.id
        await loadCustomerData()
      } catch (e) {
        console.error('Customer not found:', e)
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
      <h1 class="text-2xl font-bold text-gray-900">{{ t('customerHistory.title') }}</h1>
      <p class="text-sm text-gray-500 mt-1">{{ t('customerHistory.subtitle') }}</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
      <!-- Left sidebar: Customer list -->
      <div class="lg:col-span-1">
        <div class="card">
          <div class="card-body">
            <!-- Search -->
            <div class="relative mb-4">
              <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                v-model="customerSearch"
                type="text"
                :placeholder="t('customerHistory.searchCustomer')"
                class="input pl-9 w-full text-sm"
              />
            </div>

            <!-- Customer list -->
            <div v-if="loading" class="flex justify-center py-8">
              <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
            </div>
            <div v-else class="space-y-1 max-h-[calc(100vh-280px)] overflow-y-auto">
              <button
                v-for="c in filteredCustomers"
                :key="c.id"
                @click="selectCustomer(c)"
                :class="[
                  'w-full text-left px-3 py-2.5 rounded-lg transition-colors text-sm',
                  selectedCustomerId === c.id
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'hover:bg-gray-50 text-gray-700'
                ]"
              >
                <div class="font-medium truncate">{{ c.name }}</div>
                <div class="flex items-center justify-between mt-0.5">
                  <span class="text-xs text-gray-400">{{ c.code }}</span>
                  <span v-if="c.currentBalance > 0" class="text-xs font-medium text-red-600">
                    {{ formatCurrency(c.currentBalance) }} {{ t('common.sum') }}
                  </span>
                </div>
              </button>
              <p v-if="filteredCustomers.length === 0" class="text-sm text-gray-400 text-center py-4">
                {{ t('customerHistory.noCustomers') }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Right content: Customer details -->
      <div class="lg:col-span-3">
        <!-- No customer selected -->
        <div v-if="!selectedCustomerId" class="card">
          <div class="card-body flex flex-col items-center justify-center py-16">
            <UserIcon class="h-12 w-12 text-gray-300 mb-3" />
            <p class="text-gray-400">{{ t('customerHistory.selectCustomerHint') }}</p>
          </div>
        </div>

        <!-- Customer detail -->
        <div v-else>
          <!-- Loading -->
          <div v-if="loadingDetail" class="flex items-center justify-center h-64">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>

          <div v-else>
            <!-- Customer profile card -->
            <div class="card mb-6">
              <div class="card-body">
                <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                  <div>
                    <h2 class="text-xl font-bold text-gray-900">{{ customer?.name }}</h2>
                    <div class="flex flex-wrap items-center gap-3 mt-2 text-sm text-gray-500">
                      <span v-if="customer?.code" class="inline-flex items-center">
                        <span class="font-mono bg-gray-100 px-2 py-0.5 rounded text-xs">{{ customer.code }}</span>
                      </span>
                      <span v-if="customer?.phone" class="inline-flex items-center gap-1">
                        <PhoneIcon class="h-3.5 w-3.5" /> {{ customer.phone }}
                      </span>
                      <span v-if="customer?.email" class="inline-flex items-center gap-1">
                        <EnvelopeIcon class="h-3.5 w-3.5" /> {{ customer.email }}
                      </span>
                      <span v-if="customer?.address" class="inline-flex items-center gap-1">
                        <MapPinIcon class="h-3.5 w-3.5" /> {{ customer.address }}
                      </span>
                    </div>
                    <div v-if="customer?.creditHold" class="mt-2">
                      <span class="badge badge-danger inline-flex items-center gap-1">
                        <ExclamationTriangleIcon class="h-3.5 w-3.5" />
                        {{ t('customerHistory.creditHold') }}
                      </span>
                    </div>
                  </div>
                  <div v-if="customer?.customerType" class="flex-shrink-0">
                    <span class="badge badge-info">{{ t('enums.customerType.' + customer.customerType) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Summary cards -->
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('customerHistory.totalInvoiced') }}</p>
                  <p class="text-lg font-bold text-gray-900 mt-1">{{ formatCurrency(stats.totalInvoiced) }}</p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('customerHistory.totalPaid') }}</p>
                  <p class="text-lg font-bold text-green-600 mt-1">{{ formatCurrency(stats.totalReceived) }}</p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('customerHistory.currentDebt') }}</p>
                  <p :class="['text-lg font-bold mt-1', stats.currentBalance > 0 ? 'text-red-600' : 'text-gray-900']">
                    {{ formatCurrency(stats.currentBalance) }}
                  </p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('customerHistory.creditLimit') }}</p>
                  <p class="text-lg font-bold text-gray-900 mt-1">
                    {{ stats.creditLimit > 0 ? formatCurrency(stats.creditLimit) : t('customerHistory.unlimited') }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Tabs -->
            <div class="border-b border-gray-200 mb-4">
              <nav class="flex -mb-px space-x-6">
                <button
                  v-for="tab in ['overview', 'invoices', 'payments', 'purchases']"
                  :key="tab"
                  @click="activeTab = tab"
                  :class="[
                    'py-3 px-1 border-b-2 text-sm font-medium transition-colors',
                    activeTab === tab
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  ]"
                >
                  {{ t('customerHistory.tabs.' + tab) }}
                  <span v-if="tab === 'invoices' && invoices.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ invoices.length }}
                  </span>
                  <span v-if="tab === 'payments' && payments.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ payments.length }}
                  </span>
                  <span v-if="tab === 'purchases' && transactions.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ transactions.length }}
                  </span>
                </button>
              </nav>
            </div>

            <!-- ==================== OVERVIEW TAB ==================== -->
            <div v-if="activeTab === 'overview'" class="space-y-6">
              <!-- Recent invoices -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('customerHistory.recentInvoices') }}</h3>
                  <button @click="activeTab = 'invoices'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('customerHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="invoices.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('customerHistory.noInvoices') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('customerHistory.invoiceNumber') }}</th>
                        <th class="text-left">{{ t('common.date') }}</th>
                        <th class="text-right">{{ t('common.total') }}</th>
                        <th class="text-right">{{ t('customerHistory.paid') }}</th>
                        <th class="text-right">{{ t('common.balance') }}</th>
                        <th class="text-center">{{ t('common.status') }}</th>
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
                            {{ t('enums.arInvoiceStatus.' + inv.status) }}
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
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('customerHistory.recentPayments') }}</h3>
                  <button @click="activeTab = 'payments'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('customerHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="payments.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('customerHistory.noPayments') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('customerHistory.paymentNumber') }}</th>
                        <th class="text-left">{{ t('common.date') }}</th>
                        <th class="text-left">{{ t('customerHistory.method') }}</th>
                        <th class="text-right">{{ t('common.amount') }}</th>
                        <th class="text-center">{{ t('common.status') }}</th>
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
                            {{ t('enums.arPaymentStatus.' + pay.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Recent POS purchases -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('customerHistory.recentPurchases') }}</h3>
                  <button @click="activeTab = 'purchases'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('customerHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="transactions.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('customerHistory.noPurchases') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('customerHistory.txNumber') }}</th>
                        <th class="text-left">{{ t('common.date') }}</th>
                        <th class="text-right">{{ t('customerHistory.items') }}</th>
                        <th class="text-right">{{ t('common.total') }}</th>
                        <th class="text-center">{{ t('common.status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="tx in transactions.slice(0, 5)" :key="tx.id" class="hover:bg-gray-50">
                        <td class="font-mono text-sm">{{ tx.transactionNumber }}</td>
                        <td class="text-sm">{{ formatDate(tx.createdAt) }}</td>
                        <td class="text-right text-sm">{{ tx.itemCount }} {{ t('common.items') }}</td>
                        <td class="text-right text-sm font-medium">{{ formatCurrency(tx.totalAmount) }}</td>
                        <td class="text-center">
                          <span :class="['badge', getTxStatusClass(tx.status)]">
                            {{ t('enums.transactionStatus.' + tx.status) }}
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
                    {{ t('customerHistory.noInvoices') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th></th>
                        <th class="text-left">{{ t('customerHistory.invoiceNumber') }}</th>
                        <th class="text-left">{{ t('customerHistory.invoiceDate') }}</th>
                        <th class="text-left">{{ t('customerHistory.dueDate') }}</th>
                        <th class="text-right">{{ t('common.total') }}</th>
                        <th class="text-right">{{ t('customerHistory.paid') }}</th>
                        <th class="text-right">{{ t('common.balance') }}</th>
                        <th class="text-center">{{ t('common.status') }}</th>
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
                            <span v-if="inv.overdue" class="ml-1 text-xs text-red-500">({{ inv.daysOverdue }}{{ t('customerHistory.daysLate') }})</span>
                          </td>
                          <td class="text-right text-sm font-medium">{{ formatCurrency(inv.totalAmount) }}</td>
                          <td class="text-right text-sm text-green-600">{{ formatCurrency(inv.paidAmount) }}</td>
                          <td class="text-right text-sm" :class="inv.balanceDue > 0 ? 'text-red-600 font-medium' : 'text-gray-500'">
                            {{ formatCurrency(inv.balanceDue) }}
                          </td>
                          <td class="text-center">
                            <span :class="['badge', getInvoiceStatusClass(inv.status)]">
                              {{ t('enums.arInvoiceStatus.' + inv.status) }}
                            </span>
                          </td>
                        </tr>
                        <!-- Expanded details -->
                        <tr v-if="expandedInvoice === inv.id">
                          <td colspan="8" class="bg-gray-50 px-6 py-4">
                            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm mb-3">
                              <div>
                                <span class="text-gray-500">{{ t('customerHistory.subtotal') }}:</span>
                                <span class="ml-1 font-medium">{{ formatCurrency(inv.subtotal) }}</span>
                              </div>
                              <div v-if="inv.discountAmount > 0">
                                <span class="text-gray-500">{{ t('customerHistory.discount') }}:</span>
                                <span class="ml-1 font-medium text-orange-600">-{{ formatCurrency(inv.discountAmount) }}</span>
                              </div>
                              <div v-if="inv.taxAmount > 0">
                                <span class="text-gray-500">{{ t('customerHistory.tax') }}:</span>
                                <span class="ml-1 font-medium">{{ formatCurrency(inv.taxAmount) }}</span>
                              </div>
                              <div v-if="inv.posTransactionId">
                                <span class="text-gray-500">POS:</span>
                                <span class="ml-1 font-mono text-xs">{{ inv.posTransactionNumber || '#' + inv.posTransactionId }}</span>
                              </div>
                            </div>
                            <!-- Invoice lines -->
                            <div v-if="inv.lines && inv.lines.length > 0">
                              <table class="w-full text-sm">
                                <thead>
                                  <tr class="text-gray-500 border-b">
                                    <th class="text-left py-1 font-medium">{{ t('customerHistory.product') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('common.quantity') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('common.price') }}</th>
                                    <th class="text-right py-1 font-medium">{{ t('common.total') }}</th>
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
                      {{ t('common.previous') }}
                    </button>
                    <span class="text-sm text-gray-500">{{ t('common.page') }} {{ invoicePage + 1 }} {{ t('common.of') }} {{ invoiceTotalPages }}</span>
                    <button @click="loadInvoicePage(invoicePage + 1)" :disabled="invoicePage >= invoiceTotalPages - 1" class="btn btn-secondary text-sm" :class="{ 'opacity-50': invoicePage >= invoiceTotalPages - 1 }">
                      {{ t('common.next') }}
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
                    {{ t('customerHistory.noPayments') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('customerHistory.paymentNumber') }}</th>
                        <th class="text-left">{{ t('common.date') }}</th>
                        <th class="text-left">{{ t('customerHistory.method') }}</th>
                        <th class="text-right">{{ t('common.amount') }}</th>
                        <th class="text-right">{{ t('customerHistory.allocated') }}</th>
                        <th class="text-left">{{ t('customerHistory.reference') }}</th>
                        <th class="text-center">{{ t('common.status') }}</th>
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
                            {{ t('enums.arPaymentStatus.' + pay.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <!-- Pagination -->
                  <div v-if="paymentTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
                    <button @click="loadPaymentPage(paymentPage - 1)" :disabled="paymentPage === 0" class="btn btn-secondary text-sm" :class="{ 'opacity-50': paymentPage === 0 }">
                      {{ t('common.previous') }}
                    </button>
                    <span class="text-sm text-gray-500">{{ t('common.page') }} {{ paymentPage + 1 }} {{ t('common.of') }} {{ paymentTotalPages }}</span>
                    <button @click="loadPaymentPage(paymentPage + 1)" :disabled="paymentPage >= paymentTotalPages - 1" class="btn btn-secondary text-sm" :class="{ 'opacity-50': paymentPage >= paymentTotalPages - 1 }">
                      {{ t('common.next') }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- ==================== PURCHASES TAB ==================== -->
            <div v-if="activeTab === 'purchases'">
              <div class="card">
                <div class="card-body p-0">
                  <div v-if="transactions.length === 0" class="py-12 text-center text-sm text-gray-400">
                    {{ t('customerHistory.noPurchases') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th></th>
                        <th class="text-left">{{ t('customerHistory.txNumber') }}</th>
                        <th class="text-left">{{ t('common.date') }}</th>
                        <th class="text-left">{{ t('customerHistory.cashier') }}</th>
                        <th class="text-right">{{ t('customerHistory.items') }}</th>
                        <th class="text-right">{{ t('customerHistory.discount') }}</th>
                        <th class="text-right">{{ t('common.total') }}</th>
                        <th class="text-right">{{ t('customerHistory.paid') }}</th>
                        <th class="text-center">{{ t('common.status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template v-for="tx in transactions" :key="tx.id">
                        <tr class="hover:bg-gray-50 cursor-pointer" @click="expandedTx = expandedTx === tx.id ? null : tx.id">
                          <td class="w-8">
                            <ChevronDownIcon v-if="expandedTx !== tx.id" class="h-4 w-4 text-gray-400" />
                            <ChevronUpIcon v-else class="h-4 w-4 text-primary-600" />
                          </td>
                          <td class="font-mono text-sm">{{ tx.transactionNumber }}</td>
                          <td class="text-sm">{{ formatDate(tx.createdAt) }}</td>
                          <td class="text-sm">{{ tx.cashierName || '—' }}</td>
                          <td class="text-right text-sm">{{ tx.itemCount }}</td>
                          <td class="text-right text-sm text-orange-600">
                            {{ tx.discountAmount > 0 ? '-' + formatCurrency(tx.discountAmount) : '—' }}
                          </td>
                          <td class="text-right text-sm font-medium">{{ formatCurrency(tx.totalAmount) }}</td>
                          <td class="text-right text-sm text-green-600">{{ formatCurrency(tx.paidAmount) }}</td>
                          <td class="text-center">
                            <span :class="['badge', getTxStatusClass(tx.status)]">
                              {{ t('enums.transactionStatus.' + tx.status) }}
                            </span>
                          </td>
                        </tr>
                        <!-- Expanded: line items & payments -->
                        <tr v-if="expandedTx === tx.id">
                          <td colspan="9" class="bg-gray-50 px-6 py-4">
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                              <!-- Line items -->
                              <div>
                                <h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">{{ t('customerHistory.productList') }}</h4>
                                <div v-if="tx.lines && tx.lines.length > 0">
                                  <table class="w-full text-sm">
                                    <thead>
                                      <tr class="text-gray-500 border-b">
                                        <th class="text-left py-1 font-medium">{{ t('customerHistory.product') }}</th>
                                        <th class="text-right py-1 font-medium">{{ t('common.quantity') }}</th>
                                        <th class="text-right py-1 font-medium">{{ t('common.price') }}</th>
                                        <th class="text-right py-1 font-medium">{{ t('common.total') }}</th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      <tr v-for="line in tx.lines" :key="line.id" class="border-b border-gray-100">
                                        <td class="py-1.5">{{ line.productName }}</td>
                                        <td class="text-right py-1.5">{{ line.quantity }}</td>
                                        <td class="text-right py-1.5">{{ formatCurrency(line.unitPrice) }}</td>
                                        <td class="text-right py-1.5 font-medium">{{ formatCurrency(line.lineTotal) }}</td>
                                      </tr>
                                    </tbody>
                                  </table>
                                </div>
                                <p v-else class="text-sm text-gray-400">{{ t('customerHistory.noProductDetails') }}</p>
                              </div>
                              <!-- Payments -->
                              <div>
                                <h4 class="text-xs font-semibold text-gray-500 uppercase mb-2">{{ t('customerHistory.paymentInfo') }}</h4>
                                <div v-if="tx.payments && tx.payments.length > 0" class="space-y-2">
                                  <div v-for="p in tx.payments" :key="p.id" class="flex items-center justify-between bg-white rounded px-3 py-2 text-sm border border-gray-100">
                                    <span>{{ t('enums.paymentType.' + p.paymentType) }}</span>
                                    <span class="font-medium text-green-600">{{ formatCurrency(p.amount) }}</span>
                                  </div>
                                </div>
                                <div class="mt-3 text-sm space-y-1">
                                  <div v-if="tx.deliveryRegionName" class="flex justify-between">
                                    <span class="text-gray-500">{{ t('customerHistory.region') }}:</span>
                                    <span>{{ tx.deliveryRegionName }}</span>
                                  </div>
                                  <div v-if="tx.deliveryVillageName" class="flex justify-between">
                                    <span class="text-gray-500">{{ t('customerHistory.village') }}:</span>
                                    <span>{{ tx.deliveryVillageName }}</span>
                                  </div>
                                  <div v-if="tx.notes" class="flex justify-between">
                                    <span class="text-gray-500">{{ t('common.notes') }}:</span>
                                    <span>{{ tx.notes }}</span>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                  <!-- Pagination -->
                  <div v-if="txTotalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
                    <button @click="loadTxPage(txPage - 1)" :disabled="txPage === 0" class="btn btn-secondary text-sm" :class="{ 'opacity-50': txPage === 0 }">
                      {{ t('common.previous') }}
                    </button>
                    <span class="text-sm text-gray-500">{{ t('common.page') }} {{ txPage + 1 }} {{ t('common.of') }} {{ txTotalPages }}</span>
                    <button @click="loadTxPage(txPage + 1)" :disabled="txPage >= txTotalPages - 1" class="btn btn-secondary text-sm" :class="{ 'opacity-50': txPage >= txTotalPages - 1 }">
                      {{ t('common.next') }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
