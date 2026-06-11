<script setup>
import { formatDate } from '@/utils/format'
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { apPaymentsApi, arPaymentsApi, unwrapData } from '@/services/api'
import { MagnifyingGlassIcon, XMarkIcon, EyeIcon, XCircleIcon, ArrowDownTrayIcon, ArrowUpTrayIcon, CheckCircleIcon, CalendarDaysIcon, FunnelIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const activeTab = ref('ar') // 'ar' or 'ap'
const loading = ref(true)
const error = ref('')
const successMsg = ref('')

// AR Payments state
const arPayments = ref([])
const arTotalPages = ref(0)
const arCurrentPage = ref(0)

// AP Payments state
const apPayments = ref([])
const apTotalPages = ref(0)
const apCurrentPage = ref(0)

const search = ref('')
const statusFilter = ref('all')

// Date range filter for AR
const arDateStart = ref('')
const arDateEnd = ref('')

// Date range filter for AP
const apDateStart = ref('')
const apDateEnd = ref('')
const apDateRangeTotal = ref(null)
const showUnreconciled = ref(false)

// Detail modal
const showDetailModal = ref(false)
const selectedPayment = ref(null)
const detailType = ref('ar') // which type is shown in the modal

// ==================== AR Payments ====================
async function fetchARPayments(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (arDateStart.value && arDateEnd.value) {
      res = await arPaymentsApi.getByDateRange(arDateStart.value, arDateEnd.value)
    } else if (statusFilter.value !== 'all') {
      res = await arPaymentsApi.getByStatus(statusFilter.value, { page, size: 20, sort: 'createdAt,desc' })
    } else {
      res = await arPaymentsApi.getAll({ page, size: 20, sort: 'createdAt,desc' })
    }
    const data = unwrapData(res)
    arPayments.value = data.content || data || []
    arTotalPages.value = data.page?.totalPages || data.totalPages || 1
    arCurrentPage.value = data.page?.number ?? data.number ?? 0
  } catch (e) {
    if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    loading.value = false
  }
}

function applyArDateFilter() {
  statusFilter.value = 'all'
  fetchARPayments(0)
}

function clearArDateFilter() {
  arDateStart.value = ''
  arDateEnd.value = ''
  fetchARPayments(0)
}

async function completeARPayment(payment) {
  error.value = ''
  try {
    await arPaymentsApi.complete(payment.id)
    successMsg.value = t('finance.payments.completeSuccess')
    fetchARPayments(arCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

// ==================== AP Payments ====================
async function fetchAPPayments(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (showUnreconciled.value) {
      res = await apPaymentsApi.getUnreconciled()
    } else if (apDateStart.value && apDateEnd.value) {
      res = await apPaymentsApi.getByDateRange(apDateStart.value, apDateEnd.value)
    } else if (statusFilter.value !== 'all') {
      res = await apPaymentsApi.getByStatus(statusFilter.value, { page, size: 20, sort: 'createdAt,desc' })
    } else {
      res = await apPaymentsApi.getAll({ page, size: 20, sort: 'createdAt,desc' })
    }
    const data = unwrapData(res)
    apPayments.value = data.content || data || []
    apTotalPages.value = data.page?.totalPages || data.totalPages || 1
    apCurrentPage.value = data.page?.number ?? data.number ?? 0
  } catch (e) {
    if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    loading.value = false
  }
}

async function fetchDateRangeTotal() {
  if (!apDateStart.value || !apDateEnd.value) {
    apDateRangeTotal.value = null
    return
  }
  try {
    const res = await apPaymentsApi.getDateRangeTotal(apDateStart.value, apDateEnd.value)
    const data = unwrapData(res)
    apDateRangeTotal.value = data
  } catch (e) {
    apDateRangeTotal.value = null
  }
}

function applyApDateFilter() {
  showUnreconciled.value = false
  statusFilter.value = 'all'
  fetchAPPayments(0)
  fetchDateRangeTotal()
}

function clearApDateFilter() {
  apDateStart.value = ''
  apDateEnd.value = ''
  apDateRangeTotal.value = null
  fetchAPPayments(0)
}

function toggleUnreconciled() {
  showUnreconciled.value = !showUnreconciled.value
  if (showUnreconciled.value) {
    statusFilter.value = 'all'
    apDateStart.value = ''
    apDateEnd.value = ''
    apDateRangeTotal.value = null
  }
  fetchAPPayments(0)
}

async function reconcilePayment(payment) {
  error.value = ''
  try {
    await apPaymentsApi.reconcile(payment.id)
    successMsg.value = t('finance.payments.reconcileSuccess')
    fetchAPPayments(apCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('finance.payments.reconcileError')
  }
}

function fetchCurrentTab(page = 0) {
  if (activeTab.value === 'ar') {
    fetchARPayments(page)
  } else {
    fetchAPPayments(page)
  }
}

function switchTab(tab) {
  activeTab.value = tab
  statusFilter.value = 'all'
  search.value = ''
  fetchCurrentTab(0)
}

function onFilterChange() {
  fetchCurrentTab(0)
}

onMounted(() => {
  fetchARPayments(0)
  fetchAPPayments(0)
})

// ==================== Filtered lists ====================
const filteredARPayments = computed(() => {
  if (!search.value) return arPayments.value
  const q = search.value.toLowerCase()
  return arPayments.value.filter(p =>
    p.paymentNumber?.toLowerCase().includes(q) ||
    p.customerName?.toLowerCase().includes(q) ||
    p.notes?.toLowerCase().includes(q) ||
    p.allocations?.some(a => a.invoiceNumber?.toLowerCase().includes(q))
  )
})

const filteredAPPayments = computed(() => {
  if (!search.value) return apPayments.value
  const q = search.value.toLowerCase()
  return apPayments.value.filter(p =>
    p.paymentNumber?.toLowerCase().includes(q) ||
    p.vendorName?.toLowerCase().includes(q) ||
    p.notes?.toLowerCase().includes(q) ||
    p.memo?.toLowerCase().includes(q) ||
    p.allocations?.some(a => a.invoiceNumber?.toLowerCase().includes(q))
  )
})

// ==================== Detail Modal ====================
async function viewARPayment(payment) {
  try {
    const res = await arPaymentsApi.getById(payment.id)
    selectedPayment.value = unwrapData(res)
    detailType.value = 'ar'
    showDetailModal.value = true
  } catch (e) {
    error.value = t('failedToLoad')
  }
}

async function viewAPPayment(payment) {
  try {
    const res = await apPaymentsApi.getById(payment.id)
    selectedPayment.value = unwrapData(res)
    detailType.value = 'ap'
    showDetailModal.value = true
  } catch (e) {
    error.value = t('failedToLoad')
  }
}

async function cancelARPayment(payment) {
  const reason = prompt(t('finance.payments.cancelReason') + ':')
  if (!reason) return
  try {
    await arPaymentsApi.cancel(payment.id, reason)
    successMsg.value = `${payment.paymentNumber}`
    showDetailModal.value = false
    await fetchARPayments(arCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function voidAPPayment(payment) {
  const reason = prompt(t('finance.payments.cancelReason') + ':')
  if (!reason) return
  try {
    await apPaymentsApi.void(payment.id, reason)
    successMsg.value = `${payment.paymentNumber}`
    showDetailModal.value = false
    await fetchAPPayments(apCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

// ==================== Formatters ====================
function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(value) || 0)
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('uz-UZ', { dateStyle: 'short', timeStyle: 'short' })
}

// AR statuses
function getARStatusLabel(status) {
  return t(`enums.arPaymentStatus.${status}`, status)
}

function getARStatusClass(status) {
  switch (status) {
    case 'COMPLETED': case 'DEPOSITED': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'CANCELLED': case 'REFUNDED': case 'BOUNCED': case 'FAILED': return 'badge-danger'
    default: return 'badge-info'
  }
}

// AP statuses
function getAPStatusLabel(status) {
  return t(`enums.apPaymentStatus.${status}`, status)
}

function getAPStatusClass(status) {
  switch (status) {
    case 'COMPLETED': return 'badge-success'
    case 'APPROVED': return 'badge-info'
    case 'DRAFT': case 'PENDING_APPROVAL': return 'badge-warning'
    case 'VOIDED': case 'FAILED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function getMethodLabel(method) {
  return t(`enums.paymentMethod.${method}`, method)
}

function getInvoiceNumbers(payment) {
  if (!payment.allocations?.length) return '-'
  return payment.allocations
    .map(a => a.invoiceNumber || `#${a.arInvoiceId || a.apInvoiceId}`)
    .join(', ')
}

// Computed status filter options per tab
const statusOptions = computed(() => {
  if (activeTab.value === 'ar') {
    return [
      { value: 'all', label: t('finance.payments.allStatuses') },
      { value: 'PENDING', label: t('enums.arPaymentStatus.PENDING') },
      { value: 'COMPLETED', label: t('enums.arPaymentStatus.COMPLETED') },
      { value: 'CANCELLED', label: t('enums.arPaymentStatus.CANCELLED') }
    ]
  } else {
    return [
      { value: 'all', label: t('finance.payments.allStatuses') },
      { value: 'DRAFT', label: t('enums.apPaymentStatus.DRAFT') },
      { value: 'PENDING_APPROVAL', label: t('enums.apPaymentStatus.PENDING_APPROVAL') },
      { value: 'APPROVED', label: t('enums.apPaymentStatus.APPROVED') },
      { value: 'COMPLETED', label: t('enums.apPaymentStatus.COMPLETED') },
      { value: 'VOIDED', label: t('enums.apPaymentStatus.VOIDED') }
    ]
  }
})
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.payments.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ $t('finance.payments.subtitle') }}</p>
    </div>

    <!-- Error / Success -->
    <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-red-600">{{ error }}</p>
      <button @click="error = ''" class="text-red-400 hover:text-red-600"><XMarkIcon class="h-4 w-4" /></button>
    </div>
    <div v-if="successMsg" class="p-4 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-green-600">{{ successMsg }}</p>
      <button @click="successMsg = ''" class="text-green-400 hover:text-green-600"><XMarkIcon class="h-4 w-4" /></button>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="-mb-px flex space-x-8">
        <button
          @click="switchTab('ar')"
          :class="[
            'flex items-center gap-2 py-3 px-1 border-b-2 font-medium text-sm whitespace-nowrap',
            activeTab === 'ar'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <ArrowDownTrayIcon class="h-5 w-5" />
          {{ $t('finance.payments.arPayments') }}
          <span v-if="arPayments.length" class="ml-1 bg-green-100 text-green-700 text-xs font-semibold px-2 py-0.5 rounded-full">
            {{ arPayments.length }}
          </span>
        </button>
        <button
          @click="switchTab('ap')"
          :class="[
            'flex items-center gap-2 py-3 px-1 border-b-2 font-medium text-sm whitespace-nowrap',
            activeTab === 'ap'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <ArrowUpTrayIcon class="h-5 w-5" />
          {{ $t('finance.payments.apPayments') }}
          <span v-if="apPayments.length" class="ml-1 bg-red-100 text-red-700 text-xs font-semibold px-2 py-0.5 rounded-full">
            {{ apPayments.length }}
          </span>
        </button>
      </nav>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            :placeholder="activeTab === 'ar' ? $t('finance.payments.arSearchPlaceholder') : $t('finance.payments.apSearchPlaceholder')"
            class="input pl-10"
          />
        </div>
        <select v-model="statusFilter" @change="onFilterChange" class="input w-auto">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
    </div>

    <!-- AR Date Range Filter -->
    <div v-if="activeTab === 'ar'" class="card">
      <div class="card-body">
        <div class="flex flex-col sm:flex-row gap-4 items-end">
          <div>
            <label class="label">{{ $t('finance.payments.startDate') }}</label>
            <input v-model="arDateStart" type="date" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('finance.payments.endDate') }}</label>
            <input v-model="arDateEnd" type="date" class="input" />
          </div>
          <div class="flex gap-2">
            <button @click="applyArDateFilter" class="btn-primary" :disabled="!arDateStart || !arDateEnd">
              <CalendarDaysIcon class="h-5 w-5 mr-1" />
              {{ $t('finance.payments.applyDateFilter') }}
            </button>
            <button v-if="arDateStart || arDateEnd" @click="clearArDateFilter" class="btn-secondary">
              {{ $t('finance.payments.clearDateFilter') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- AP Date Range Filter & Unreconciled Toggle -->
    <div v-if="activeTab === 'ap'" class="card">
      <div class="card-body">
        <div class="flex flex-col sm:flex-row gap-4 items-end">
          <div>
            <label class="label">{{ $t('finance.payments.startDate') }}</label>
            <input v-model="apDateStart" type="date" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('finance.payments.endDate') }}</label>
            <input v-model="apDateEnd" type="date" class="input" />
          </div>
          <div class="flex gap-2">
            <button @click="applyApDateFilter" class="btn-primary" :disabled="!apDateStart || !apDateEnd">
              <CalendarDaysIcon class="h-5 w-5 mr-1" />
              {{ $t('finance.payments.applyDateFilter') }}
            </button>
            <button v-if="apDateStart || apDateEnd" @click="clearApDateFilter" class="btn-secondary">
              {{ $t('finance.payments.clearDateFilter') }}
            </button>
          </div>
          <div class="sm:ml-auto">
            <button
              @click="toggleUnreconciled"
              :class="[
                'inline-flex items-center px-3 py-1.5 text-sm font-medium rounded-lg border transition-colors',
                showUnreconciled
                  ? 'bg-amber-50 border-amber-300 text-amber-700'
                  : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
              ]"
            >
              <FunnelIcon class="h-4 w-4 mr-1.5" />
              {{ $t('finance.payments.unreconciledTab') }}
            </button>
          </div>
        </div>
        <!-- Date Range Total -->
        <div v-if="apDateRangeTotal !== null && apDateStart && apDateEnd" class="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <p class="text-sm text-blue-600 font-medium">{{ $t('finance.payments.dateRangeTotal') }}</p>
          <p class="text-2xl font-bold text-blue-700 mt-1">{{ formatCurrency(apDateRangeTotal) }} {{ $t('sum') }}</p>
        </div>
      </div>
    </div>

    <!-- ==================== AR Payments Table ==================== -->
    <div v-if="activeTab === 'ar'" class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="filteredARPayments.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('finance.payments.noArPayments') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('finance.payments.paymentNumber') }}</th>
              <th>{{ $t('finance.payments.customer') }}</th>
              <th>{{ $t('finance.payments.invoice') }}</th>
              <th>{{ $t('date') }}</th>
              <th>{{ $t('finance.payments.method') }}</th>
              <th class="text-right">{{ $t('amount') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="payment in filteredARPayments" :key="payment.id">
              <td>
                <p class="font-medium text-sm">{{ payment.paymentNumber }}</p>
              </td>
              <td>
                <p class="text-sm">{{ payment.customerName || '-' }}</p>
              </td>
              <td>
                <p class="text-sm text-blue-600">{{ getInvoiceNumbers(payment) }}</p>
              </td>
              <td class="text-sm text-gray-500">
                {{ formatDate(payment.paymentDate) }}
              </td>
              <td class="text-sm text-gray-500">
                {{ getMethodLabel(payment.paymentMethod) }}
              </td>
              <td class="text-right">
                <span class="font-semibold text-green-600">{{ formatCurrency(payment.paymentAmount) }} {{ $t('sum') }}</span>
              </td>
              <td>
                <span :class="['badge text-xs', getARStatusClass(payment.status)]">
                  {{ getARStatusLabel(payment.status) }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-1">
                  <button
                    @click="viewARPayment(payment)"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                    :title="$t('finance.payments.viewDetails')"
                  >
                    <EyeIcon class="h-5 w-5" />
                  </button>
                  <button
                    v-if="payment.status === 'PENDING'"
                    @click="completeARPayment(payment)"
                    class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100 inline-flex"
                    :title="$t('finance.payments.completePayment')"
                  >
                    <CheckCircleIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="arTotalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
        <p class="text-sm text-gray-500">{{ $t('page') }} {{ arCurrentPage + 1 }} / {{ arTotalPages }}</p>
        <div class="flex gap-2">
          <button
            @click="fetchARPayments(arCurrentPage - 1)"
            :disabled="arCurrentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchARPayments(arCurrentPage + 1)"
            :disabled="arCurrentPage >= arTotalPages - 1"
            class="btn-secondary text-sm"
          >{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- ==================== AP Payments Table ==================== -->
    <div v-if="activeTab === 'ap'" class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="filteredAPPayments.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('finance.payments.noApPayments') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('finance.payments.paymentNumber') }}</th>
              <th>{{ $t('finance.payments.supplier') }}</th>
              <th>{{ $t('finance.payments.invoice') }}</th>
              <th>{{ $t('date') }}</th>
              <th>{{ $t('finance.payments.method') }}</th>
              <th class="text-right">{{ $t('amount') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="payment in filteredAPPayments" :key="payment.id">
              <td>
                <p class="font-medium text-sm">{{ payment.paymentNumber }}</p>
              </td>
              <td>
                <p class="text-sm">{{ payment.vendorName || '-' }}</p>
              </td>
              <td>
                <p class="text-sm text-blue-600">{{ getInvoiceNumbers(payment) }}</p>
              </td>
              <td class="text-sm text-gray-500">
                {{ formatDate(payment.paymentDate) }}
              </td>
              <td class="text-sm text-gray-500">
                {{ getMethodLabel(payment.paymentMethod) }}
              </td>
              <td class="text-right">
                <span class="font-semibold text-red-600">{{ formatCurrency(payment.paymentAmount) }} {{ $t('sum') }}</span>
              </td>
              <td>
                <span :class="['badge text-xs', getAPStatusClass(payment.status)]">
                  {{ getAPStatusLabel(payment.status) }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-1">
                  <button
                    @click="viewAPPayment(payment)"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                    :title="$t('finance.payments.viewDetails')"
                  >
                    <EyeIcon class="h-5 w-5" />
                  </button>
                  <button
                    v-if="payment.status === 'COMPLETED' && !payment.reconciled"
                    @click="reconcilePayment(payment)"
                    class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100 inline-flex"
                    :title="$t('finance.payments.reconcile')"
                  >
                    <CheckCircleIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="apTotalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
        <p class="text-sm text-gray-500">{{ $t('page') }} {{ apCurrentPage + 1 }} / {{ apTotalPages }}</p>
        <div class="flex gap-2">
          <button
            @click="fetchAPPayments(apCurrentPage - 1)"
            :disabled="apCurrentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchAPPayments(apCurrentPage + 1)"
            :disabled="apCurrentPage >= apTotalPages - 1"
            class="btn-secondary text-sm"
          >{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- ==================== Detail Modal ==================== -->
    <div
      v-if="showDetailModal && selectedPayment"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="showDetailModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedPayment.paymentNumber }}</h2>
            <p class="text-sm text-gray-500">
              {{ detailType === 'ar' ? $t('finance.payments.arDetails') : $t('finance.payments.apDetails') }}
            </p>
          </div>
          <button @click="showDetailModal = false" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
            <XMarkIcon class="h-6 w-6" />
          </button>
        </div>

        <div class="p-6 space-y-5">
          <!-- Amount -->
          <div :class="[detailType === 'ar' ? 'bg-green-50' : 'bg-red-50', 'rounded-lg p-4 text-center']">
            <p :class="[detailType === 'ar' ? 'text-green-600' : 'text-red-600', 'text-sm']">{{ $t('finance.payments.paymentAmount') }}</p>
            <p :class="[detailType === 'ar' ? 'text-green-700' : 'text-red-700', 'text-2xl font-bold']">
              {{ formatCurrency(selectedPayment.paymentAmount) }} {{ $t('sum') }}
            </p>
          </div>

          <!-- Info grid -->
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div v-if="detailType === 'ar'">
              <span class="text-gray-500">{{ $t('finance.payments.customer') }}:</span>
              <p class="font-medium">{{ selectedPayment.customerName || '-' }}</p>
            </div>
            <div v-else>
              <span class="text-gray-500">{{ $t('finance.payments.supplier') }}:</span>
              <p class="font-medium">{{ selectedPayment.vendorName || '-' }}</p>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.payments.paymentDate') }}:</span>
              <p class="font-medium">{{ formatDate(selectedPayment.paymentDate) }}</p>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.payments.paymentMethod') }}:</span>
              <p class="font-medium">{{ getMethodLabel(selectedPayment.paymentMethod) }}</p>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('status') }}:</span>
              <p>
                <span v-if="detailType === 'ar'" :class="['badge text-xs', getARStatusClass(selectedPayment.status)]">
                  {{ getARStatusLabel(selectedPayment.status) }}
                </span>
                <span v-else :class="['badge text-xs', getAPStatusClass(selectedPayment.status)]">
                  {{ getAPStatusLabel(selectedPayment.status) }}
                </span>
              </p>
            </div>
            <div v-if="selectedPayment.referenceNumber">
              <span class="text-gray-500">{{ $t('finance.payments.reference') }}:</span>
              <p class="font-medium">{{ selectedPayment.referenceNumber }}</p>
            </div>
            <div v-if="selectedPayment.checkNumber">
              <span class="text-gray-500">{{ $t('finance.payments.checkNumber') }}:</span>
              <p class="font-medium">{{ selectedPayment.checkNumber }}</p>
            </div>
            <div v-if="selectedPayment.bankAccountName">
              <span class="text-gray-500">{{ $t('finance.payments.bankAccount') }}:</span>
              <p class="font-medium">{{ selectedPayment.bankAccountName }}</p>
            </div>
            <div v-if="selectedPayment.memo">
              <span class="text-gray-500">{{ $t('finance.payments.memo') }}:</span>
              <p class="font-medium">{{ selectedPayment.memo }}</p>
            </div>
            <div v-if="selectedPayment.notes">
              <span class="text-gray-500">{{ $t('notes') }}:</span>
              <p class="font-medium">{{ selectedPayment.notes }}</p>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('createdAt') }}:</span>
              <p class="font-medium">{{ formatDateTime(selectedPayment.createdAt) }}</p>
            </div>
            <div v-if="selectedPayment.glPosted">
              <span class="text-gray-500">{{ $t('finance.payments.glJournalized') }}:</span>
              <p class="font-medium text-green-600">{{ $t('yes') }}</p>
            </div>
          </div>

          <!-- Void reason -->
          <div v-if="selectedPayment.voidReason" class="bg-red-50 border border-red-200 rounded-lg p-4">
            <p class="text-sm font-medium text-red-800">{{ $t('finance.payments.cancelReason') }}:</p>
            <p class="text-sm text-red-700">{{ selectedPayment.voidReason }}</p>
          </div>

          <!-- Allocations -->
          <div v-if="selectedPayment.allocations?.length">
            <h3 class="font-semibold text-gray-900 mb-2">{{ $t('finance.payments.invoiceAllocations') }}</h3>
            <div class="bg-gray-50 rounded-lg overflow-hidden">
              <table class="w-full text-sm">
                <thead class="bg-gray-100">
                  <tr>
                    <th class="px-4 py-2 text-left text-gray-600">{{ $t('finance.payments.invoice') }}</th>
                    <th class="px-4 py-2 text-right text-gray-600">{{ $t('amount') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="alloc in selectedPayment.allocations" :key="alloc.id">
                    <td class="px-4 py-2">
                      <p class="font-medium">{{ alloc.invoiceNumber || `${$t('finance.payments.invoice')} #${alloc.arInvoiceId || alloc.apInvoiceId}` }}</p>
                      <p v-if="alloc.notes" class="text-xs text-gray-400">{{ alloc.notes }}</p>
                    </td>
                    <td class="px-4 py-2 text-right font-medium" :class="detailType === 'ar' ? 'text-green-600' : 'text-red-600'">
                      {{ formatCurrency(alloc.allocatedAmount) }} {{ $t('sum') }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Action buttons -->
          <div class="pt-2 border-t border-gray-200">
            <!-- AR: Cancel completed payment -->
            <button
              v-if="detailType === 'ar' && selectedPayment.status === 'COMPLETED'"
              @click="cancelARPayment(selectedPayment)"
              class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100"
            >
              <XCircleIcon class="h-4 w-4" />
              {{ $t('finance.payments.cancelPayment') }}
            </button>
            <!-- AP: Void approved/completed payment -->
            <button
              v-if="detailType === 'ap' && (selectedPayment.status === 'APPROVED' || selectedPayment.status === 'COMPLETED')"
              @click="voidAPPayment(selectedPayment)"
              class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100"
            >
              <XCircleIcon class="h-4 w-4" />
              {{ $t('finance.payments.cancelPayment') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
