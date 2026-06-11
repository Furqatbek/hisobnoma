<script setup>
import { formatCurrency, formatDate } from '@/utils/format'
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { expensesApi, receivingApi, unwrapData } from '@/services/api'
import {
  PlusIcon, EyeIcon, MagnifyingGlassIcon, XMarkIcon,
  CheckCircleIcon, XCircleIcon, ArrowUpIcon, ArrowDownIcon,
  DocumentTextIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const orders = ref([])
const loading = ref(true)
const search = ref('')
const activeStatus = ref('')

const showDetailModal = ref(false)
const selectedOrder = ref(null)
const loadingDetail = ref(false)

const cancelReason = ref('')
const showCancelInput = ref(false)
const confirming = ref(false)
const cancelling = ref(false)
const creatingInvoice = ref(false)
const invoiceError = ref('')
const invoiceSuccess = ref('')

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

const poFilter = ref('')

const statuses = ['', 'DRAFT', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']

async function fetchOrders() {
  loading.value = true
  try {
    const params = { page: pagination.value.page, size: pagination.value.size }
    let response

    if (poFilter.value.trim()) {
      response = await receivingApi.getByPurchaseOrder(poFilter.value.trim(), params)
    } else if (search.value.trim()) {
      response = await receivingApi.search(search.value, params)
    } else if (activeStatus.value) {
      response = await receivingApi.getByStatus(activeStatus.value, params)
    } else {
      response = await receivingApi.getAll(params)
    }

    const data = unwrapData(response)
    orders.value = data.content || data || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 0
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch receiving orders:', error)
  } finally {
    loading.value = false
  }
}

async function viewOrder(order) {
  loadingDetail.value = true
  showDetailModal.value = true
  selectedOrder.value = order
  showCancelInput.value = false
  cancelReason.value = ''

  try {
    const response = await receivingApi.getById(order.id)
    selectedOrder.value = unwrapData(response)
  } catch (error) {
    console.error('Failed to load receiving order:', error)
  } finally {
    loadingDetail.value = false
  }
}

function closeModal() {
  showDetailModal.value = false
  selectedOrder.value = null
  showCancelInput.value = false
}

async function confirmOrder() {
  if (!selectedOrder.value) return
  confirming.value = true
  try {
    await receivingApi.confirm(selectedOrder.value.id)
    closeModal()
    fetchOrders()
  } catch (error) {
    console.error('Failed to confirm:', error)
  } finally {
    confirming.value = false
  }
}

async function cancelOrder() {
  if (!selectedOrder.value) return
  cancelling.value = true
  try {
    await receivingApi.cancel(selectedOrder.value.id, cancelReason.value)
    closeModal()
    fetchOrders()
  } catch (error) {
    console.error('Failed to cancel:', error)
  } finally {
    cancelling.value = false
  }
}

async function createInvoiceFromReceiving(order) {
  if (!order) return
  creatingInvoice.value = true
  invoiceError.value = ''
  invoiceSuccess.value = ''
  try {
    await expensesApi.createFromReceiving(order.id)
    invoiceSuccess.value = t('inventory.receiving.createInvoiceSuccess')
  } catch (e) {
    invoiceError.value = e.response?.data?.message || t('inventory.receiving.createInvoiceError')
  } finally {
    creatingInvoice.value = false
  }
}

function handleSearch() {
  pagination.value.page = 0
  activeStatus.value = ''
  poFilter.value = ''
  fetchOrders()
}

function handlePoFilter() {
  pagination.value.page = 0
  activeStatus.value = ''
  search.value = ''
  fetchOrders()
}

function clearPoFilter() {
  poFilter.value = ''
  pagination.value.page = 0
  fetchOrders()
}

function filterByStatus(status) {
  activeStatus.value = status
  search.value = ''
  poFilter.value = ''
  pagination.value.page = 0
  fetchOrders()
}

function prevPage() {
  if (pagination.value.page > 0) {
    pagination.value.page--
    fetchOrders()
  }
}

function nextPage() {
  if (pagination.value.page < pagination.value.totalPages - 1) {
    pagination.value.page++
    fetchOrders()
  }
}

onMounted(fetchOrders)

function getStatusClass(status) {
  const classes = {
    DRAFT: 'bg-gray-100 text-gray-700',
    PENDING: 'bg-yellow-100 text-yellow-700',
    IN_PROGRESS: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-red-100 text-red-700'
  }
  return classes[status] || 'bg-gray-100 text-gray-700'
}

function getStatusLabel(status) {
  return t(`inventory.receiving.status.${status}`)
}

function canConfirm(order) {
  return order?.status === 'DRAFT' || order?.status === 'PENDING'
}

function canCancel(order) {
  return order?.status !== 'COMPLETED' && order?.status !== 'CANCELLED'
}

function canCreateInvoice(order) {
  return order?.status === 'COMPLETED'
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.receiving.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.receiving.subtitle') }}</p>
      </div>
      <RouterLink to="/inventory/receiving/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.receiving.newReceiving') }}
      </RouterLink>
    </div>

    <!-- Messages -->
    <div v-if="invoiceError" class="p-4 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-red-600">{{ invoiceError }}</p>
      <button @click="invoiceError = ''" class="text-red-400 hover:text-red-600"><XMarkIcon class="h-4 w-4" /></button>
    </div>
    <div v-if="invoiceSuccess" class="p-4 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-green-600">{{ invoiceSuccess }}</p>
      <button @click="invoiceSuccess = ''" class="text-green-400 hover:text-green-600"><XMarkIcon class="h-4 w-4" /></button>
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
          {{ status ? getStatusLabel(status) : $t('all') }}
        </button>
      </nav>
    </div>

    <!-- Search & PO Filter -->
    <div class="card">
      <div class="card-body space-y-4">
        <div class="flex flex-col sm:flex-row gap-4">
          <div class="flex-1 relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="search"
              type="text"
              :placeholder="$t('inventory.receiving.searchPlaceholder')"
              class="input pl-10"
              @keyup.enter="handleSearch"
            />
          </div>
          <button @click="handleSearch" class="btn-primary">{{ $t('search') }}</button>
        </div>
        <div class="flex flex-col sm:flex-row gap-4">
          <div class="flex-1 relative">
            <input
              v-model="poFilter"
              type="text"
              :placeholder="$t('inventory.receiving.poFilterPlaceholder')"
              class="input"
              @keyup.enter="handlePoFilter"
            />
          </div>
          <button @click="handlePoFilter" :disabled="!poFilter.trim()" class="btn-secondary">
            {{ $t('inventory.receiving.filterByPo') }}
          </button>
          <button v-if="poFilter" @click="clearPoFilter" class="btn-secondary">
            {{ $t('inventory.receiving.clearPoFilter') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="orders.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.receiving.noOrders') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('inventory.receiving.receivingNumber') }}</th>
              <th>{{ $t('inventory.receiving.vendor') }}</th>
              <th>{{ $t('inventory.receiving.poNumber') }}</th>
              <th>{{ $t('inventory.receiving.location') }}</th>
              <th>{{ $t('date') }}</th>
              <th class="text-right">{{ $t('amount') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="w-10"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="order in orders" :key="order.id">
              <td class="font-mono text-sm">{{ order.receivingNumber }}</td>
              <td>{{ order.vendorName || '-' }}</td>
              <td class="font-mono text-sm text-gray-500">{{ order.poNumber || '-' }}</td>
              <td>{{ order.locationName || '-' }}</td>
              <td>{{ formatDate(order.receivingDate) }}</td>
              <td class="text-right font-medium">{{ formatCurrency(order.totalAmount) }} {{ $t('sum') }}</td>
              <td>
                <span :class="['text-xs px-2 py-1 rounded-full font-medium', getStatusClass(order.status)]">
                  {{ getStatusLabel(order.status) }}
                </span>
              </td>
              <td>
                <button @click="viewOrder(order)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
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
          {{ $t('inventory.receiving.totalRecords', { count: pagination.totalElements }) }}
        </p>
        <div class="flex gap-2">
          <button @click="prevPage" :disabled="pagination.page === 0" class="btn btn-secondary btn-sm">{{ $t('prev') }}</button>
          <span class="text-sm text-gray-500 flex items-center px-2">
            {{ pagination.page + 1 }} / {{ pagination.totalPages }}
          </span>
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
                <h3 class="text-lg font-semibold text-gray-900">
                  {{ selectedOrder?.receivingNumber }}
                </h3>
                <span :class="['text-xs px-2 py-1 rounded-full font-medium', getStatusClass(selectedOrder?.status)]">
                  {{ getStatusLabel(selectedOrder?.status) }}
                </span>
              </div>
              <button @click="closeModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div v-if="loadingDetail" class="flex items-center justify-center h-64">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="selectedOrder" class="p-6 space-y-6">
              <!-- Info Grid -->
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <p class="text-xs text-gray-500">{{ $t('inventory.receiving.vendor') }}</p>
                  <p class="text-sm font-medium">{{ selectedOrder.vendorName }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500">{{ $t('inventory.receiving.location') }}</p>
                  <p class="text-sm font-medium">{{ selectedOrder.locationName }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500">{{ $t('date') }}</p>
                  <p class="text-sm font-medium">{{ formatDate(selectedOrder.receivingDate) }}</p>
                </div>
                <div v-if="selectedOrder.poNumber">
                  <p class="text-xs text-gray-500">{{ $t('inventory.receiving.poNumber') }}</p>
                  <p class="text-sm font-medium font-mono">{{ selectedOrder.poNumber }}</p>
                </div>
                <div v-if="selectedOrder.vendorInvoiceNumber">
                  <p class="text-xs text-gray-500">{{ $t('inventory.receiving.vendorInvoice') }}</p>
                  <p class="text-sm font-medium">{{ selectedOrder.vendorInvoiceNumber }}</p>
                </div>
                <div v-if="selectedOrder.vendorDeliveryNote">
                  <p class="text-xs text-gray-500">{{ $t('inventory.receiving.deliveryNote') }}</p>
                  <p class="text-sm font-medium">{{ selectedOrder.vendorDeliveryNote }}</p>
                </div>
              </div>

              <!-- Lines Table -->
              <div v-if="selectedOrder.lines?.length" class="border rounded-lg overflow-hidden">
                <table class="table text-sm">
                  <thead>
                    <tr>
                      <th class="text-xs">#</th>
                      <th class="text-xs">{{ $t('inventory.receiving.product') }}</th>
                      <th class="text-xs text-right">{{ $t('inventory.receiving.expected') }}</th>
                      <th class="text-xs text-right">{{ $t('inventory.receiving.received') }}</th>
                      <th class="text-xs text-right">{{ $t('inventory.receiving.accepted') }}</th>
                      <th class="text-xs text-right">{{ $t('inventory.receiving.unitCost') }}</th>
                      <th class="text-xs text-right">{{ $t('inventory.receiving.lineTotal') }}</th>
                      <th class="text-xs">{{ $t('inventory.receiving.variance') }}</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-100">
                    <tr v-for="line in selectedOrder.lines" :key="line.id">
                      <td class="text-gray-400">{{ line.lineNumber }}</td>
                      <td>
                        <div class="font-medium">{{ line.productName }}</div>
                        <div v-if="line.productSku" class="text-xs text-gray-400 font-mono">{{ line.productSku }}</div>
                      </td>
                      <td class="text-right">{{ line.expectedQuantity ?? '-' }}</td>
                      <td class="text-right font-medium">{{ line.receivedQuantity }}</td>
                      <td class="text-right">{{ line.acceptedQuantity ?? line.receivedQuantity }}</td>
                      <td class="text-right">{{ formatCurrency(line.unitCost) }}</td>
                      <td class="text-right font-medium">{{ formatCurrency(line.lineTotal) }}</td>
                      <td>
                        <span v-if="line.hasVariance" :class="['inline-flex items-center gap-0.5 text-xs font-medium',
                          line.overReceived ? 'text-blue-600' : 'text-amber-600']">
                          <ArrowUpIcon v-if="line.overReceived" class="h-3 w-3" />
                          <ArrowDownIcon v-else class="h-3 w-3" />
                          {{ Math.abs(line.varianceQuantity) }}
                        </span>
                        <span v-else class="text-xs text-gray-400">-</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Totals -->
              <div class="flex justify-end">
                <div class="w-64 space-y-1 text-sm">
                  <div class="flex justify-between">
                    <span class="text-gray-500">{{ $t('inventory.receiving.subtotal') }}</span>
                    <span>{{ formatCurrency(selectedOrder.subtotal) }} {{ $t('sum') }}</span>
                  </div>
                  <div v-if="selectedOrder.discountAmount" class="flex justify-between text-red-600">
                    <span>{{ $t('inventory.receiving.discount') }}</span>
                    <span>-{{ formatCurrency(selectedOrder.discountAmount) }}</span>
                  </div>
                  <div v-if="selectedOrder.taxAmount" class="flex justify-between">
                    <span class="text-gray-500">{{ $t('inventory.receiving.tax') }}</span>
                    <span>{{ formatCurrency(selectedOrder.taxAmount) }}</span>
                  </div>
                  <div v-if="selectedOrder.shippingAmount" class="flex justify-between">
                    <span class="text-gray-500">{{ $t('inventory.receiving.shipping') }}</span>
                    <span>{{ formatCurrency(selectedOrder.shippingAmount) }}</span>
                  </div>
                  <div class="flex justify-between font-bold border-t pt-1">
                    <span>{{ $t('total') }}</span>
                    <span>{{ formatCurrency(selectedOrder.totalAmount) }} {{ $t('sum') }}</span>
                  </div>
                </div>
              </div>

              <!-- Notes -->
              <div v-if="selectedOrder.notes" class="bg-gray-50 rounded-lg p-3">
                <p class="text-xs text-gray-500 mb-1">{{ $t('inventory.receiving.notes') }}</p>
                <p class="text-sm">{{ selectedOrder.notes }}</p>
              </div>

              <!-- Cancel reason input -->
              <div v-if="showCancelInput" class="bg-red-50 border border-red-200 rounded-lg p-4 space-y-3">
                <label class="label text-red-700">{{ $t('inventory.receiving.cancelReason') }}</label>
                <textarea v-model="cancelReason" rows="2" class="input" :placeholder="$t('inventory.receiving.cancelReasonPlaceholder')"></textarea>
                <div class="flex gap-2 justify-end">
                  <button @click="showCancelInput = false" class="btn btn-secondary btn-sm">{{ $t('cancel') }}</button>
                  <button @click="cancelOrder" :disabled="cancelling" class="btn btn-sm bg-red-600 text-white hover:bg-red-700">
                    <span v-if="cancelling" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                    {{ $t('inventory.receiving.confirmCancel') }}
                  </button>
                </div>
              </div>

              <!-- Actions -->
              <div v-if="canConfirm(selectedOrder) || canCancel(selectedOrder) || canCreateInvoice(selectedOrder)" class="flex justify-end gap-3 border-t pt-4">
                <button
                  v-if="canCreateInvoice(selectedOrder)"
                  @click="createInvoiceFromReceiving(selectedOrder)"
                  :disabled="creatingInvoice"
                  class="btn bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200"
                >
                  <DocumentTextIcon class="h-5 w-5 mr-2" />
                  {{ creatingInvoice ? $t('saving') : $t('inventory.receiving.createInvoice') }}
                </button>
                <button
                  v-if="canCancel(selectedOrder) && !showCancelInput"
                  @click="showCancelInput = true"
                  class="btn bg-red-50 text-red-700 hover:bg-red-100 border border-red-200"
                >
                  <XCircleIcon class="h-5 w-5 mr-2" />
                  {{ $t('inventory.receiving.cancelReceiving') }}
                </button>
                <button
                  v-if="canConfirm(selectedOrder)"
                  @click="confirmOrder"
                  :disabled="confirming"
                  class="btn btn-primary"
                >
                  <span v-if="confirming" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                  <CheckCircleIcon v-else class="h-5 w-5 mr-2" />
                  {{ $t('inventory.receiving.confirmReceiving') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
