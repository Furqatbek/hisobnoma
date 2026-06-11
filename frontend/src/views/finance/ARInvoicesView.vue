<script setup>
import { formatCurrency, formatDate } from '@/utils/format'
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { arInvoicesApi } from '@/services/api'
import {
  PlusIcon, EyeIcon, MagnifyingGlassIcon, XMarkIcon,
  PaperAirplaneIcon, XCircleIcon, ClockIcon, ArrowPathIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const invoices = ref([])
const loading = ref(true)
const search = ref('')
const activeStatus = ref('')
const overdueCount = ref(0)

const showDetailModal = ref(false)
const selectedInvoice = ref(null)
const loadingDetail = ref(false)

const showCancelInput = ref(false)
const cancelReason = ref('')
const sending = ref(false)
const cancelling = ref(false)
const checkingOverdue = ref(false)

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

const statuses = ['', 'DRAFT', 'PENDING', 'SENT', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED']

async function fetchInvoices() {
  loading.value = true
  try {
    let response
    if (activeStatus.value) {
      response = await arInvoicesApi.getByStatus(activeStatus.value, {
        page: pagination.value.page,
        size: pagination.value.size,
        sort: 'invoiceDate,desc'
      })
    } else {
      response = await arInvoicesApi.getAll({
        page: pagination.value.page,
        size: pagination.value.size,
        sort: 'invoiceDate,desc'
      })
    }
    const data = response.data.data || response.data
    invoices.value = data.content || data || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 0
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch invoices:', error)
  } finally {
    loading.value = false
  }
}

async function fetchOverdueCount() {
  try {
    const res = await arInvoicesApi.getOverdue()
    const data = res.data.data || res.data || []
    overdueCount.value = data.length
  } catch (e) {
    console.error('Failed to load overdue:', e)
  }
}

async function searchByNumber() {
  if (!search.value.trim()) {
    fetchInvoices()
    return
  }
  loading.value = true
  try {
    const res = await arInvoicesApi.getByNumber(search.value.trim())
    const inv = res.data.data || res.data
    invoices.value = inv ? [inv] : []
    pagination.value.totalPages = 1
    pagination.value.totalElements = inv ? 1 : 0
  } catch (error) {
    if (error.response?.status === 404) {
      invoices.value = []
      pagination.value.totalPages = 0
      pagination.value.totalElements = 0
    } else {
      console.error('Search failed:', error)
    }
  } finally {
    loading.value = false
  }
}

async function viewInvoice(invoice) {
  loadingDetail.value = true
  showDetailModal.value = true
  selectedInvoice.value = invoice
  showCancelInput.value = false
  cancelReason.value = ''

  try {
    const res = await arInvoicesApi.getById(invoice.id)
    selectedInvoice.value = res.data.data || res.data
  } catch (error) {
    console.error('Failed to load invoice:', error)
  } finally {
    loadingDetail.value = false
  }
}

function closeModal() {
  showDetailModal.value = false
  selectedInvoice.value = null
  showCancelInput.value = false
}

async function sendInvoice() {
  if (!selectedInvoice.value) return
  sending.value = true
  try {
    const res = await arInvoicesApi.send(selectedInvoice.value.id)
    selectedInvoice.value = res.data.data || res.data
    fetchInvoices()
  } catch (error) {
    console.error('Failed to send invoice:', error)
  } finally {
    sending.value = false
  }
}

async function cancelInvoice() {
  if (!selectedInvoice.value) return
  cancelling.value = true
  try {
    const res = await arInvoicesApi.cancel(selectedInvoice.value.id, cancelReason.value)
    selectedInvoice.value = res.data.data || res.data
    showCancelInput.value = false
    fetchInvoices()
  } catch (error) {
    console.error('Failed to cancel invoice:', error)
  } finally {
    cancelling.value = false
  }
}

async function checkOverdue() {
  checkingOverdue.value = true
  try {
    await arInvoicesApi.checkOverdue()
    fetchInvoices()
    fetchOverdueCount()
  } catch (error) {
    console.error('Failed to check overdue:', error)
  } finally {
    checkingOverdue.value = false
  }
}

function filterByStatus(status) {
  activeStatus.value = status
  search.value = ''
  pagination.value.page = 0
  fetchInvoices()
}

function prevPage() {
  if (pagination.value.page > 0) {
    pagination.value.page--
    fetchInvoices()
  }
}

function nextPage() {
  if (pagination.value.page < pagination.value.totalPages - 1) {
    pagination.value.page++
    fetchInvoices()
  }
}

onMounted(() => {
  fetchInvoices()
  fetchOverdueCount()
})

function getStatusClass(status) {
  const cls = {
    DRAFT: 'bg-gray-100 text-gray-700',
    PENDING: 'bg-yellow-100 text-yellow-700',
    SENT: 'bg-blue-100 text-blue-700',
    PARTIAL: 'bg-orange-100 text-orange-700',
    PAID: 'bg-green-100 text-green-700',
    OVERDUE: 'bg-red-100 text-red-700',
    CANCELLED: 'bg-gray-200 text-gray-500',
    DISPUTED: 'bg-purple-100 text-purple-700',
    WRITTEN_OFF: 'bg-gray-300 text-gray-600'
  }
  return cls[status] || 'bg-gray-100 text-gray-700'
}

function canSend(inv) {
  return inv?.status === 'PENDING'
}

function canCancel(inv) {
  return inv?.status === 'DRAFT' || inv?.status === 'PENDING' || inv?.status === 'SENT'
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.arInvoices.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.arInvoices.subtitle') }}</p>
      </div>
      <div class="flex gap-3">
        <button
          @click="checkOverdue"
          :disabled="checkingOverdue"
          class="btn btn-secondary"
          :title="$t('finance.arInvoices.checkOverdueHint')"
        >
          <ClockIcon :class="['h-5 w-5 mr-2', { 'animate-spin': checkingOverdue }]" />
          {{ $t('finance.arInvoices.checkOverdue') }}
          <span v-if="overdueCount > 0" class="ml-2 text-xs bg-red-500 text-white rounded-full px-2 py-0.5">{{ overdueCount }}</span>
        </button>
        <RouterLink to="/finance/debtors/new" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('finance.arInvoices.newInvoice') }}
        </RouterLink>
      </div>
    </div>

    <!-- Status Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex gap-1 overflow-x-auto pb-px">
        <button
          v-for="status in statuses" :key="status"
          @click="filterByStatus(status)"
          :class="['px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
            activeStatus === status
              ? 'border-primary-500 text-primary-600 bg-primary-50/50'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
        >
          {{ status ? $t(`enums.arInvoiceStatus.${status}`) : $t('all') }}
        </button>
      </nav>
    </div>

    <!-- Search by invoice number -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            :placeholder="$t('finance.arInvoices.searchPlaceholder')"
            class="input pl-10"
            @keyup.enter="searchByNumber"
          />
        </div>
        <button @click="searchByNumber" class="btn-primary">{{ $t('search') }}</button>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="invoices.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('finance.arInvoices.noInvoices') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('finance.arInvoices.invoiceNumber') }}</th>
              <th>{{ $t('finance.arInvoices.customer') }}</th>
              <th>{{ $t('finance.arInvoices.invoiceDate') }}</th>
              <th>{{ $t('finance.arInvoices.dueDate') }}</th>
              <th class="text-right">{{ $t('finance.arInvoices.totalAmount') }}</th>
              <th class="text-right">{{ $t('finance.arInvoices.balanceDue') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="w-10"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="inv in invoices" :key="inv.id" :class="{ 'bg-red-50/50': inv.overdue }">
              <td class="font-mono text-sm">{{ inv.invoiceNumber }}</td>
              <td>{{ inv.customerName || '-' }}</td>
              <td>{{ formatDate(inv.invoiceDate) }}</td>
              <td>
                <span :class="{ 'text-red-600 font-medium': inv.overdue }">
                  {{ formatDate(inv.dueDate) }}
                </span>
                <span v-if="inv.overdue && inv.daysOverdue" class="text-xs text-red-500 ml-1">
                  ({{ inv.daysOverdue }}{{ $t('finance.arInvoices.daysLate') }})
                </span>
              </td>
              <td class="text-right font-medium">{{ formatCurrency(inv.totalAmount) }}</td>
              <td class="text-right font-medium" :class="{ 'text-red-600': inv.balanceDue > 0 }">
                {{ formatCurrency(inv.balanceDue) }}
              </td>
              <td>
                <span :class="['text-xs px-2 py-1 rounded-full font-medium', getStatusClass(inv.status)]">
                  {{ $t(`enums.arInvoiceStatus.${inv.status}`) }}
                </span>
              </td>
              <td>
                <button @click="viewInvoice(inv)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                  <EyeIcon class="h-5 w-5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-3 flex items-center justify-between border-t border-gray-200">
        <p class="text-sm text-gray-500">
          {{ $t('finance.arInvoices.totalRecords', { count: pagination.totalElements }) }}
        </p>
        <div class="flex gap-2">
          <button @click="prevPage" :disabled="pagination.page === 0" class="btn btn-secondary btn-sm">{{ $t('prev') }}</button>
          <span class="text-sm text-gray-500 flex items-center px-2">{{ pagination.page + 1 }} / {{ pagination.totalPages }}</span>
          <button @click="nextPage" :disabled="pagination.page >= pagination.totalPages - 1" class="btn btn-secondary btn-sm">{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <Teleport to="body">
      <div v-if="showDetailModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeModal"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
            <!-- Header -->
            <div class="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between rounded-t-xl z-10">
              <div>
                <h3 class="text-lg font-semibold text-gray-900">{{ selectedInvoice?.invoiceNumber }}</h3>
                <div class="flex items-center gap-2 mt-1">
                  <span :class="['text-xs px-2 py-1 rounded-full font-medium', getStatusClass(selectedInvoice?.status)]">
                    {{ $t(`enums.arInvoiceStatus.${selectedInvoice?.status}`) }}
                  </span>
                  <span v-if="selectedInvoice?.overdue" class="text-xs text-red-600 font-medium">
                    {{ selectedInvoice.daysOverdue }} {{ $t('finance.arInvoices.daysOverdue') }}
                  </span>
                </div>
              </div>
              <button @click="closeModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div v-if="loadingDetail" class="flex items-center justify-center h-64">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="selectedInvoice" class="p-6 space-y-6">
              <!-- Info Grid -->
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.customer') }}</p>
                  <p class="text-sm font-medium">{{ selectedInvoice.customerName }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.invoiceDate') }}</p>
                  <p class="text-sm font-medium">{{ formatDate(selectedInvoice.invoiceDate) }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.dueDate') }}</p>
                  <p class="text-sm font-medium" :class="{ 'text-red-600': selectedInvoice.overdue }">
                    {{ formatDate(selectedInvoice.dueDate) }}
                  </p>
                </div>
                <div v-if="selectedInvoice.paymentTerms">
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.paymentTerms') }}</p>
                  <p class="text-sm font-medium">{{ selectedInvoice.paymentTerms }}</p>
                </div>
                <div v-if="selectedInvoice.posTransactionNumber">
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.posTransaction') }}</p>
                  <p class="text-sm font-medium font-mono">{{ selectedInvoice.posTransactionNumber }}</p>
                </div>
                <div v-if="selectedInvoice.sentAt">
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.sentAt') }}</p>
                  <p class="text-sm font-medium">{{ formatDate(selectedInvoice.sentAt) }}</p>
                </div>
              </div>

              <!-- Lines Table -->
              <div v-if="selectedInvoice.lines?.length" class="border rounded-lg overflow-hidden">
                <table class="table text-sm">
                  <thead>
                    <tr>
                      <th class="text-xs">#</th>
                      <th class="text-xs">{{ $t('finance.arInvoices.description') }}</th>
                      <th class="text-xs text-right">{{ $t('finance.arInvoices.qty') }}</th>
                      <th class="text-xs text-right">{{ $t('finance.arInvoices.unitPrice') }}</th>
                      <th class="text-xs text-right">{{ $t('finance.arInvoices.tax') }}</th>
                      <th class="text-xs text-right">{{ $t('finance.arInvoices.lineTotal') }}</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-100">
                    <tr v-for="line in selectedInvoice.lines" :key="line.id">
                      <td class="text-gray-400">{{ line.lineNumber }}</td>
                      <td>
                        <div class="font-medium">{{ line.productName || line.description }}</div>
                        <div v-if="line.productSku" class="text-xs text-gray-400 font-mono">{{ line.productSku }}</div>
                        <div v-if="line.description && line.productName" class="text-xs text-gray-500">{{ line.description }}</div>
                      </td>
                      <td class="text-right">{{ line.quantity }} {{ line.unitOfMeasure || '' }}</td>
                      <td class="text-right">{{ formatCurrency(line.unitPrice) }}</td>
                      <td class="text-right text-gray-500">{{ line.taxAmount ? formatCurrency(line.taxAmount) : '-' }}</td>
                      <td class="text-right font-medium">{{ formatCurrency(line.lineTotal) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Totals -->
              <div class="flex justify-end">
                <div class="w-72 space-y-1 text-sm">
                  <div class="flex justify-between">
                    <span class="text-gray-500">{{ $t('finance.arInvoices.subtotal') }}</span>
                    <span>{{ formatCurrency(selectedInvoice.subtotal) }}</span>
                  </div>
                  <div v-if="selectedInvoice.discountAmount" class="flex justify-between text-red-600">
                    <span>{{ $t('finance.arInvoices.discount') }}</span>
                    <span>-{{ formatCurrency(selectedInvoice.discountAmount) }}</span>
                  </div>
                  <div v-if="selectedInvoice.taxAmount" class="flex justify-between">
                    <span class="text-gray-500">{{ $t('finance.arInvoices.tax') }}</span>
                    <span>{{ formatCurrency(selectedInvoice.taxAmount) }}</span>
                  </div>
                  <div v-if="selectedInvoice.shippingAmount" class="flex justify-between">
                    <span class="text-gray-500">{{ $t('finance.arInvoices.shipping') }}</span>
                    <span>{{ formatCurrency(selectedInvoice.shippingAmount) }}</span>
                  </div>
                  <div class="flex justify-between font-bold border-t pt-1">
                    <span>{{ $t('total') }}</span>
                    <span>{{ formatCurrency(selectedInvoice.totalAmount) }} {{ $t('sum') }}</span>
                  </div>
                  <div class="flex justify-between text-green-600">
                    <span>{{ $t('finance.arInvoices.paid') }}</span>
                    <span>{{ formatCurrency(selectedInvoice.paidAmount) }}</span>
                  </div>
                  <div class="flex justify-between font-bold text-lg border-t pt-1" :class="{ 'text-red-600': selectedInvoice.balanceDue > 0 }">
                    <span>{{ $t('finance.arInvoices.balanceDue') }}</span>
                    <span>{{ formatCurrency(selectedInvoice.balanceDue) }} {{ $t('sum') }}</span>
                  </div>
                </div>
              </div>

              <!-- Notes -->
              <div v-if="selectedInvoice.notes || selectedInvoice.description" class="bg-gray-50 rounded-lg p-3 space-y-2">
                <div v-if="selectedInvoice.description">
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.description') }}</p>
                  <p class="text-sm">{{ selectedInvoice.description }}</p>
                </div>
                <div v-if="selectedInvoice.notes">
                  <p class="text-xs text-gray-500">{{ $t('finance.arInvoices.notes') }}</p>
                  <p class="text-sm">{{ selectedInvoice.notes }}</p>
                </div>
              </div>

              <!-- Cancellation info -->
              <div v-if="selectedInvoice.status === 'CANCELLED' && selectedInvoice.cancellationReason" class="bg-red-50 border border-red-200 rounded-lg p-3">
                <p class="text-xs text-red-500">{{ $t('finance.arInvoices.cancellationReason') }}</p>
                <p class="text-sm text-red-700">{{ selectedInvoice.cancellationReason }}</p>
              </div>

              <!-- Cancel reason input -->
              <div v-if="showCancelInput" class="bg-red-50 border border-red-200 rounded-lg p-4 space-y-3">
                <label class="label text-red-700">{{ $t('finance.arInvoices.cancelReasonLabel') }}</label>
                <textarea v-model="cancelReason" rows="2" class="input" :placeholder="$t('finance.arInvoices.cancelReasonPlaceholder')"></textarea>
                <div class="flex gap-2 justify-end">
                  <button @click="showCancelInput = false" class="btn btn-secondary btn-sm">{{ $t('cancel') }}</button>
                  <button @click="cancelInvoice" :disabled="cancelling || !cancelReason.trim()" class="btn btn-sm bg-red-600 text-white hover:bg-red-700">
                    <span v-if="cancelling" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                    {{ $t('finance.arInvoices.confirmCancel') }}
                  </button>
                </div>
              </div>

              <!-- Actions -->
              <div v-if="(canSend(selectedInvoice) || canCancel(selectedInvoice)) && !showCancelInput" class="flex justify-end gap-3 border-t pt-4">
                <button
                  v-if="canCancel(selectedInvoice)"
                  @click="showCancelInput = true"
                  class="btn bg-red-50 text-red-700 hover:bg-red-100 border border-red-200"
                >
                  <XCircleIcon class="h-5 w-5 mr-2" />
                  {{ $t('finance.arInvoices.cancelInvoice') }}
                </button>
                <button
                  v-if="canSend(selectedInvoice)"
                  @click="sendInvoice"
                  :disabled="sending"
                  class="btn btn-primary"
                >
                  <span v-if="sending" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                  <PaperAirplaneIcon v-else class="h-5 w-5 mr-2" />
                  {{ $t('finance.arInvoices.sendInvoice') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
