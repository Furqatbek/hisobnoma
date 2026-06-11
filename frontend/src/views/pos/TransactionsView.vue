<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { posApi, productsApi } from '@/services/api'
import {
  EyeIcon, MagnifyingGlassIcon, PrinterIcon, XMarkIcon, NoSymbolIcon,
  PlusIcon, TrashIcon, CalendarDaysIcon
} from '@heroicons/vue/24/outline'
import ReceiptTemplate from '@/components/ReceiptTemplate.vue'

const toast = useToastStore()

const { t } = useI18n()

const transactions = ref([])
const loading = ref(true)
const search = ref('')
const dateFilter = ref('today')

// Modal state
const showDetailModal = ref(false)
const selectedTransaction = ref(null)
const loadingDetail = ref(false)
const receiptRef = ref(null)

// Void state
const showVoidModal = ref(false)
const voidReason = ref('')
const voidLoading = ref(false)

// Daily Summary
const dailySummaryDate = ref(new Date().toISOString().split('T')[0])
const dailySummary = ref(null)
const dailySummaryLoading = ref(false)

// Add Line Item
const showAddLineModal = ref(false)
const addLineLoading = ref(false)
const lineItemForm = reactive({
  productId: null,
  quantity: 1,
  unitPrice: 0
})

// Remove Line Item
const removeLineLoading = ref(false)

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

async function fetchTransactions() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }

    if (dateFilter.value === 'today') {
      params.date = new Date().toISOString().split('T')[0]
    } else if (dateFilter.value === 'week') {
      const weekAgo = new Date()
      weekAgo.setDate(weekAgo.getDate() - 7)
      params.startDate = weekAgo.toISOString().split('T')[0]
      params.endDate = new Date().toISOString().split('T')[0]
    } else if (dateFilter.value === 'month') {
      const monthAgo = new Date()
      monthAgo.setMonth(monthAgo.getMonth() - 1)
      params.startDate = monthAgo.toISOString().split('T')[0]
      params.endDate = new Date().toISOString().split('T')[0]
    }

    if (search.value) {
      params.search = search.value
    }

    const response = await posApi.getTransactions(params)
    // Handle both response formats
    const data = response.data.data || response.data
    transactions.value = data.content || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 0
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || 0
  } catch (error) {
    console.error('Tranzaksiyalarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

async function viewTransaction(tx) {
  loadingDetail.value = true
  showDetailModal.value = true
  selectedTransaction.value = tx

  try {
    // Fetch full transaction details
    const response = await posApi.getTransaction(tx.id)
    selectedTransaction.value = response.data.data || response.data
  } catch (error) {
    console.error('Tranzaksiya ma\'lumotlarini yuklashda xatolik:', error)
    // Keep the basic transaction data if detail fetch fails
  } finally {
    loadingDetail.value = false
  }
}

function closeModal() {
  showDetailModal.value = false
  selectedTransaction.value = null
}

function printReceipt() {
  if (receiptRef.value) {
    receiptRef.value.printReceipt()
  }
}

function openVoidModal() {
  voidReason.value = ''
  showVoidModal.value = true
}

async function voidTransaction() {
  if (!voidReason.value.trim()) return
  voidLoading.value = true
  try {
    await posApi.voidTransaction(selectedTransaction.value.id, voidReason.value.trim())
    showVoidModal.value = false
    showDetailModal.value = false
    selectedTransaction.value = null
    fetchTransactions()
  } catch (error) {
    toast.error(t('pos.transactions.voidError') + ': ' + (error.response?.data?.message || error.message))
  } finally {
    voidLoading.value = false
  }
}

// Daily Summary
async function fetchDailySummary() {
  if (!dailySummaryDate.value) return
  dailySummaryLoading.value = true
  try {
    const res = await posApi.getDailySummary(dailySummaryDate.value)
    dailySummary.value = res.data.data || res.data
  } catch (error) {
    console.error('Daily summary error:', error)
    dailySummary.value = null
  } finally {
    dailySummaryLoading.value = false
  }
}

// Add Line Item
function openAddLineModal() {
  lineItemForm.productId = null
  lineItemForm.quantity = 1
  lineItemForm.unitPrice = 0
  showAddLineModal.value = true
}

async function addLineItem() {
  if (!lineItemForm.productId || !selectedTransaction.value) return
  addLineLoading.value = true
  try {
    await posApi.addLineItem(selectedTransaction.value.id, { ...lineItemForm })
    showAddLineModal.value = false
    // Refresh transaction details
    const response = await posApi.getTransaction(selectedTransaction.value.id)
    selectedTransaction.value = response.data.data || response.data
  } catch (error) {
    toast.error(t('pos.transactions.addLineError') + ': ' + (error.response?.data?.message || error.message))
  } finally {
    addLineLoading.value = false
  }
}

// Remove Line Item
async function removeLineItem(itemId) {
  if (!confirm(t('pos.transactions.confirmRemoveLine'))) return
  removeLineLoading.value = true
  try {
    await posApi.removeLineItem(selectedTransaction.value.id, itemId)
    // Refresh transaction details
    const response = await posApi.getTransaction(selectedTransaction.value.id)
    selectedTransaction.value = response.data.data || response.data
  } catch (error) {
    toast.error(t('pos.transactions.removeLineError') + ': ' + (error.response?.data?.message || error.message))
  } finally {
    removeLineLoading.value = false
  }
}

onMounted(() => {
  fetchTransactions()
  fetchDailySummary()
})

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleString('uz-UZ')
}

function getStatusClass(status) {
  switch (status) {
    case 'COMPLETED': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'VOIDED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function getStatusLabel(status) {
  const labels = {
    'COMPLETED': t('pos.transactions.completed'),
    'PENDING': t('pos.transactions.pending'),
    'VOIDED': t('pos.transactions.voided'),
    'IN_PROGRESS': t('pos.processing')
  }
  return labels[status] || status
}

function getPaymentLabel(type) {
  const labels = {
    'CASH': t('pos.cash'),
    'CARD': t('pos.card'),
    'CREDIT': t('pos.credit'),
    'MOBILE_PAYMENT': t('pos.mobile')
  }
  return labels[type] || type
}

function getPaymentBadgeClass(type) {
  const classes = {
    'CASH': 'inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700',
    'CARD': 'inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700',
    'CREDIT': 'inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-700',
    'MOBILE_PAYMENT': 'inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-700'
  }
  return classes[type] || 'inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700'
}

function handleSearch() {
  pagination.value.page = 0
  fetchTransactions()
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('pos.transactions.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ $t('pos.transactions.subtitle') }}</p>
    </div>

    <!-- Daily Summary -->
    <div class="card">
      <div class="card-body">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
            <CalendarDaysIcon class="h-5 w-5 text-primary-600" />
            {{ $t('pos.transactions.dailySummary') }}
          </h3>
          <div class="flex items-center gap-2">
            <input
              v-model="dailySummaryDate"
              type="date"
              class="input w-auto"
              @change="fetchDailySummary"
            />
          </div>
        </div>
        <div v-if="dailySummaryLoading" class="flex items-center justify-center py-4">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="dailySummary" class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div class="bg-blue-50 rounded-lg p-4 text-center">
            <label class="text-sm text-blue-700">{{ $t('pos.transactions.totalTransactions') }}</label>
            <p class="text-2xl font-bold text-blue-800">{{ dailySummary.totalTransactions ?? dailySummary.transactionCount ?? 0 }}</p>
          </div>
          <div class="bg-green-50 rounded-lg p-4 text-center">
            <label class="text-sm text-green-700">{{ $t('pos.transactions.totalSalesAmount') }}</label>
            <p class="text-2xl font-bold text-green-800">{{ formatCurrency(dailySummary.totalSales ?? dailySummary.totalAmount ?? 0) }}</p>
          </div>
          <div class="bg-purple-50 rounded-lg p-4 text-center">
            <label class="text-sm text-purple-700">{{ $t('pos.transactions.totalPaidAmount') }}</label>
            <p class="text-2xl font-bold text-purple-800">{{ formatCurrency(dailySummary.totalPaid ?? dailySummary.paidAmount ?? 0) }}</p>
          </div>
          <div class="bg-red-50 rounded-lg p-4 text-center">
            <label class="text-sm text-red-700">{{ $t('pos.transactions.totalVoided') }}</label>
            <p class="text-2xl font-bold text-red-800">{{ dailySummary.voidedCount ?? dailySummary.totalVoided ?? 0 }}</p>
          </div>
        </div>
        <div v-else class="text-center py-4">
          <p class="text-sm text-gray-500">{{ $t('pos.transactions.noSummary') }}</p>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            :placeholder="$t('pos.transactions.searchPlaceholder')"
            class="input pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <select v-model="dateFilter" @change="fetchTransactions" class="input w-auto">
          <option value="today">{{ $t('pos.transactions.today') }}</option>
          <option value="week">{{ $t('pos.transactions.thisWeek') }}</option>
          <option value="month">{{ $t('pos.transactions.thisMonth') }}</option>
          <option value="all">{{ $t('pos.transactions.all') }}</option>
        </select>
        <button @click="handleSearch" class="btn-primary">
          {{ $t('search') }}
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="transactions.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('pos.transactions.noTransactions') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('pos.transactions.transactionNumber') }}</th>
              <th>{{ $t('pos.transactions.date') }}</th>
              <th>{{ $t('pos.transactions.cashier') }}</th>
              <th>{{ $t('pos.transactions.customer') }}</th>
              <th>{{ $t('pos.shifts.terminal') }}</th>
              <th>{{ $t('pos.transactions.items') }}</th>
              <th class="text-right">{{ $t('amount') }}</th>
              <th class="text-right">{{ $t('pos.transactions.paid') }}</th>
              <th>{{ $t('status') }}</th>
              <th>{{ $t('pos.paymentMethod') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="tx in transactions" :key="tx.id">
              <td class="font-mono text-sm">{{ tx.transactionNumber || `#${tx.id}` }}</td>
              <td class="text-sm text-gray-500 whitespace-nowrap">{{ formatDate(tx.createdAt) }}</td>
              <td class="text-sm">{{ tx.cashierName || '-' }}</td>
              <td>{{ tx.customerName || $t('pos.walkInCustomer') }}</td>
              <td class="text-sm text-gray-500">{{ tx.terminalName || tx.terminalCode || '-' }}</td>
              <td>{{ tx.lineCount || 0 }} {{ $t('items') }}</td>
              <td class="text-right font-medium whitespace-nowrap">{{ formatCurrency(tx.totalAmount) }}</td>
              <td class="text-right whitespace-nowrap">
                <span v-if="tx.balanceDue > 0" class="text-orange-600 font-medium">
                  {{ formatCurrency(tx.paidAmount) }}
                  <span class="text-xs block text-orange-500">{{ $t('pos.transactions.debt') }}: {{ formatCurrency(tx.balanceDue) }}</span>
                </span>
                <span v-else class="text-green-600">{{ formatCurrency(tx.paidAmount) }}</span>
              </td>
              <td>
                <span :class="['badge', getStatusClass(tx.status)]">
                  {{ getStatusLabel(tx.status) }}
                </span>
              </td>
              <td class="text-sm">
                <span v-if="tx.payments?.length === 1">
                  <span :class="getPaymentBadgeClass(tx.payments[0]?.paymentType)">
                    {{ getPaymentLabel(tx.payments[0]?.paymentType) }}
                  </span>
                </span>
                <span v-else-if="tx.payments?.length > 1" class="flex flex-wrap gap-1">
                  <span
                    v-for="p in tx.payments"
                    :key="p.id"
                    :class="getPaymentBadgeClass(p.paymentType)"
                    class="text-xs"
                  >
                    {{ getPaymentLabel(p.paymentType) }}
                  </span>
                </span>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="text-right space-x-1">
                <button
                  @click="viewTransaction(tx)"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  :title="$t('view')"
                >
                  <EyeIcon class="h-5 w-5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <button
            @click="pagination.page--; fetchTransactions()"
            :disabled="pagination.page === 0"
            class="btn-secondary"
          >
            {{ $t('previous') }}
          </button>
          <span class="text-sm text-gray-500">
            {{ $t('page') }} {{ pagination.page + 1 }} / {{ pagination.totalPages }}
            ({{ $t('totalCount') }}: {{ pagination.totalElements }})
          </span>
          <button
            @click="pagination.page++; fetchTransactions()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            {{ $t('next') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Transaction Detail Modal -->
    <div
      v-if="showDetailModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closeModal"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
        <!-- Modal Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 class="text-xl font-bold text-gray-900">
              {{ $t('pos.transactions.transactionDetails') }}
            </h2>
            <p class="text-sm text-gray-500">
              {{ selectedTransaction?.transactionNumber || `#${selectedTransaction?.id}` }}
            </p>
          </div>
          <div class="flex items-center gap-2">
            <button
              v-if="selectedTransaction && ['PENDING', 'HELD'].includes(selectedTransaction.status)"
              @click="openVoidModal"
              class="btn-secondary flex items-center gap-2 !text-red-600 !border-red-300 hover:!bg-red-50"
              :disabled="loadingDetail"
            >
              <NoSymbolIcon class="h-5 w-5" />
              {{ $t('pos.transactions.voidTransaction') }}
            </button>
            <button
              @click="printReceipt"
              class="btn-primary flex items-center gap-2"
              :disabled="loadingDetail"
            >
              <PrinterIcon class="h-5 w-5" />
              {{ $t('print') }}
            </button>
            <button
              @click="closeModal"
              class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
            >
              <XMarkIcon class="h-6 w-6" />
            </button>
          </div>
        </div>

        <!-- Modal Content -->
        <div class="flex overflow-hidden">
          <!-- Transaction Details -->
          <div class="flex-1 p-6 overflow-y-auto max-h-[calc(90vh-80px)]">
            <div v-if="loadingDetail" class="flex items-center justify-center h-64">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="selectedTransaction" class="space-y-6">
              <!-- Basic Info -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.transactions.date') }}</label>
                  <p class="font-medium">{{ formatDate(selectedTransaction.createdAt) }}</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('status') }}</label>
                  <p>
                    <span :class="['badge', getStatusClass(selectedTransaction.status)]">
                      {{ getStatusLabel(selectedTransaction.status) }}
                    </span>
                  </p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.transactions.customer') }}</label>
                  <p class="font-medium">{{ selectedTransaction.customerName || $t('pos.walkInCustomer') }}</p>
                  <p v-if="selectedTransaction.customerPhone" class="text-sm text-gray-500">
                    {{ selectedTransaction.customerPhone }}
                  </p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.transactions.cashier') }}</label>
                  <p class="font-medium">{{ selectedTransaction.cashierName || '-' }}</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.shifts.terminal') }}</label>
                  <p class="font-medium">{{ selectedTransaction.terminalName || selectedTransaction.terminalCode || '-' }}</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.transactions.location') }}</label>
                  <p class="font-medium">{{ selectedTransaction.locationName || '-' }}</p>
                </div>
              </div>

              <!-- Void info -->
              <div v-if="selectedTransaction.status === 'VOIDED' && selectedTransaction.voidReason" class="bg-red-50 border border-red-200 rounded-lg p-4">
                <p class="text-sm font-medium text-red-800">{{ $t('pos.transactions.voidReason') }}:</p>
                <p class="text-sm text-red-700">{{ selectedTransaction.voidReason }}</p>
                <p v-if="selectedTransaction.voidedAt" class="text-xs text-red-500 mt-1">{{ formatDate(selectedTransaction.voidedAt) }}</p>
              </div>

              <!-- Items -->
              <div>
                <div class="flex items-center justify-between mb-3">
                  <h3 class="font-semibold text-gray-900">{{ $t('pos.transactions.items') }}</h3>
                  <button
                    v-if="selectedTransaction && selectedTransaction.status === 'PENDING'"
                    @click="openAddLineModal"
                    class="btn-primary text-xs flex items-center gap-1"
                  >
                    <PlusIcon class="h-4 w-4" />
                    {{ $t('pos.transactions.addLine') }}
                  </button>
                </div>
                <div class="bg-gray-50 rounded-lg overflow-hidden">
                  <table class="w-full">
                    <thead class="bg-gray-100">
                      <tr>
                        <th class="px-4 py-2 text-left text-sm font-medium text-gray-600">{{ $t('pos.transactions.product') }}</th>
                        <th class="px-4 py-2 text-right text-sm font-medium text-gray-600">{{ $t('quantity') }}</th>
                        <th class="px-4 py-2 text-right text-sm font-medium text-gray-600">{{ $t('price') }}</th>
                        <th class="px-4 py-2 text-right text-sm font-medium text-gray-600">{{ $t('total') }}</th>
                        <th v-if="selectedTransaction && selectedTransaction.status === 'PENDING'" class="px-4 py-2 text-right text-sm font-medium text-gray-600">{{ $t('actions') }}</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                      <tr v-for="item in selectedTransaction.lines" :key="item.id">
                        <td class="px-4 py-3">
                          <p class="font-medium">{{ item.productName }}</p>
                          <p v-if="item.productCode" class="text-sm text-gray-500">{{ item.productCode }}</p>
                        </td>
                        <td class="px-4 py-3 text-right">{{ item.quantity }} <span v-if="item.uomCode" class="text-xs text-gray-500">{{ item.uomCode }}</span></td>
                        <td class="px-4 py-3 text-right">{{ formatCurrency(item.unitPrice) }} {{ $t('sum') }}</td>
                        <td class="px-4 py-3 text-right font-medium">{{ formatCurrency(item.lineTotal) }} {{ $t('sum') }}</td>
                        <td v-if="selectedTransaction && selectedTransaction.status === 'PENDING'" class="px-4 py-3 text-right">
                          <button
                            @click="removeLineItem(item.id)"
                            :disabled="removeLineLoading"
                            class="p-1 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                            :title="$t('pos.transactions.removeLine')"
                          >
                            <TrashIcon class="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Totals -->
              <div class="bg-gray-50 rounded-lg p-4">
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-gray-600">{{ $t('pos.subtotal') }}:</span>
                    <span>{{ formatCurrency(selectedTransaction.subtotal) }} {{ $t('sum') }}</span>
                  </div>
                  <div class="flex justify-between" v-if="selectedTransaction.discountAmount > 0">
                    <span class="text-gray-600">{{ $t('pos.discount') }}:</span>
                    <span class="text-green-600">-{{ formatCurrency(selectedTransaction.discountAmount) }} {{ $t('sum') }}</span>
                  </div>
                  <div class="flex justify-between" v-if="selectedTransaction.taxAmount > 0">
                    <span class="text-gray-600">{{ $t('pos.tax') }}:</span>
                    <span>{{ formatCurrency(selectedTransaction.taxAmount) }} {{ $t('sum') }}</span>
                  </div>
                  <div class="flex justify-between text-lg font-bold border-t pt-2">
                    <span>{{ $t('total') }}:</span>
                    <span>{{ formatCurrency(selectedTransaction.totalAmount) }} {{ $t('sum') }}</span>
                  </div>
                </div>
              </div>

              <!-- Payments -->
              <div v-if="selectedTransaction.payments?.length">
                <h3 class="font-semibold text-gray-900 mb-3">{{ $t('pos.transactions.payments') }}</h3>
                <div class="space-y-2">
                  <div
                    v-for="payment in selectedTransaction.payments"
                    :key="payment.id"
                    class="flex justify-between items-center bg-gray-50 rounded-lg px-4 py-3"
                  >
                    <span class="flex items-center gap-2">
                      <span class="badge badge-info">{{ getPaymentLabel(payment.paymentType) }}</span>
                      <span v-if="payment.cardLastFour" class="text-sm text-gray-500">****{{ payment.cardLastFour }}</span>
                    </span>
                    <span class="font-medium">{{ formatCurrency(payment.amount) }} {{ $t('sum') }}</span>
                  </div>
                  <div v-if="selectedTransaction.changeAmount > 0" class="flex justify-between items-center px-4 py-2 text-sm text-gray-600">
                    <span>{{ $t('pos.change') }}:</span>
                    <span>{{ formatCurrency(selectedTransaction.changeAmount) }} {{ $t('sum') }}</span>
                  </div>
                </div>
              </div>

              <!-- Notes -->
              <div v-if="selectedTransaction.notes" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <p class="text-sm font-medium text-yellow-800">{{ $t('notes') }}:</p>
                <p class="text-sm text-yellow-700">{{ selectedTransaction.notes }}</p>
              </div>
            </div>
          </div>

          <!-- Receipt Preview -->
          <div class="w-80 border-l border-gray-200 bg-gray-50 p-4 overflow-y-auto max-h-[calc(90vh-80px)]">
            <h3 class="font-semibold text-gray-900 mb-4 text-center">{{ $t('pos.printReceipt') }}</h3>
            <div class="bg-white shadow-lg rounded-lg overflow-hidden">
              <ReceiptTemplate
                v-if="selectedTransaction"
                ref="receiptRef"
                :transaction="selectedTransaction"
                type="sale"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- Void Confirmation Modal -->
    <div
      v-if="showVoidModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-4"
      @click.self="showVoidModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
        <h3 class="text-lg font-bold text-gray-900 mb-2">{{ $t('pos.transactions.voidTransaction') }}</h3>
        <p class="text-sm text-gray-500 mb-4">{{ $t('pos.transactions.confirmVoid') }}</p>

        <div class="mb-4">
          <label class="label">{{ $t('pos.transactions.voidReason') }} *</label>
          <textarea
            v-model="voidReason"
            rows="3"
            class="input"
            :placeholder="$t('pos.transactions.voidReasonPlaceholder')"
          ></textarea>
        </div>

        <div class="flex space-x-3">
          <button @click="showVoidModal = false" class="btn-secondary flex-1">
            {{ $t('cancel') }}
          </button>
          <button
            @click="voidTransaction"
            :disabled="!voidReason.trim() || voidLoading"
            class="btn-primary flex-1 !bg-red-600 hover:!bg-red-700 disabled:!bg-red-300"
          >
            {{ voidLoading ? $t('pos.processing') : $t('pos.transactions.voidTransaction') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Add Line Item Modal -->
    <div
      v-if="showAddLineModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-4"
      @click.self="showAddLineModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-bold text-gray-900">{{ $t('pos.transactions.addLine') }}</h3>
          <button @click="showAddLineModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="label">{{ $t('pos.transactions.productId') }} <span class="text-red-500">*</span></label>
            <input v-model.number="lineItemForm.productId" type="number" class="input" :placeholder="$t('pos.transactions.productIdPlaceholder')" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('quantity') }} <span class="text-red-500">*</span></label>
              <input v-model.number="lineItemForm.quantity" type="number" min="1" step="1" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('price') }}</label>
              <input v-model.number="lineItemForm.unitPrice" type="number" min="0" step="100" class="input" />
            </div>
          </div>
        </div>

        <div class="mt-6 flex space-x-3">
          <button @click="showAddLineModal = false" class="btn-secondary flex-1">
            {{ $t('cancel') }}
          </button>
          <button
            @click="addLineItem"
            :disabled="!lineItemForm.productId || addLineLoading"
            class="btn-primary flex-1"
          >
            {{ addLineLoading ? $t('pos.processing') : $t('add') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
