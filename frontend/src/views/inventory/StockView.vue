<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { stockApi, warehousesApi } from '@/services/api'
import { MagnifyingGlassIcon, ExclamationTriangleIcon, BuildingStorefrontIcon, PencilSquareIcon, XMarkIcon, ClockIcon, ArrowUpIcon, ArrowDownIcon, ChevronDownIcon, ChevronUpIcon, CurrencyDollarIcon, ArrowsRightLeftIcon, CheckCircleIcon, XCircleIcon, InboxArrowDownIcon, ArrowRightStartOnRectangleIcon, ShieldCheckIcon, FunnelIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const stockItems = ref([])
const locations = ref([])
const loading = ref(true)
const search = ref('')
const filter = ref('all') // all, low, out
const selectedLocationId = ref('')
const page = ref(0)
const totalElements = ref(0)
const pageSize = 50

// Tab state
const activeTab = ref('stockList')

// Product detail state
const selectedProduct = ref(null)
const productDetail = ref(null)
const productDetailLoading = ref(false)
const productAvailable = ref(null)
const productLocationStock = ref(null)
const detailLocationId = ref('')

// Low stock tab state
const lowStockItems = ref([])
const lowStockLoading = ref(false)

// Valuation tab state
const valuationData = ref(null)
const valuationLoading = ref(false)

// Check availability state
const showAvailabilityTool = ref(false)
const availabilityForm = ref({ productId: '', locationId: '', quantity: '' })
const availabilityResult = ref(null)
const availabilityErrors = ref({})
const availabilityChecking = ref(false)

// Operations state
const operationSubTab = ref('receive')
const operationSubmitting = ref(false)
const operationErrors = ref({})
const operationSuccess = ref('')
const receiveForm = ref({ productId: '', locationId: '', quantity: '', reference: '', notes: '' })
const issueForm = ref({ productId: '', locationId: '', quantity: '', reference: '', notes: '' })
const transferForm = ref({ productId: '', fromLocationId: '', toLocationId: '', quantity: '', reference: '', notes: '' })

// History location filter
const historyLocationId = ref('')

// Adjust modal state
const showAdjustModal = ref(false)
const adjusting = ref(false)
const adjustItem = ref(null)
const adjustForm = ref({ newQuantity: '', reason: '', notes: '' })
const adjustErrors = ref({})

async function fetchLocations() {
  try {
    const response = await warehousesApi.getActive()
    locations.value = response.data || []
  } catch (error) {
    console.error('Failed to fetch locations:', error)
  }
}

async function fetchStock() {
  loading.value = true
  try {
    let response
    if (search.value) {
      response = await stockApi.search(search.value, { page: page.value, size: pageSize })
    } else if (selectedLocationId.value) {
      response = await stockApi.getByLocation(selectedLocationId.value, { page: page.value, size: pageSize })
    } else {
      response = await stockApi.getAll({ page: page.value, size: pageSize })
    }
    stockItems.value = response.data.content || []
    totalElements.value = response.data.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch stock:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchLocations()
  fetchStock()
})

watch(selectedLocationId, () => {
  page.value = 0
  fetchStock()
})

const filteredStock = computed(() => {
  let result = stockItems.value
  if (filter.value === 'low') {
    result = result.filter(s => s.quantityOnHand > 0 && s.belowMinimum)
  } else if (filter.value === 'out') {
    result = result.filter(s => s.quantityOnHand <= 0)
  }
  return result
})

const lowStockCount = computed(() => stockItems.value.filter(s => s.quantityOnHand > 0 && s.belowMinimum).length)
const outOfStockCount = computed(() => stockItems.value.filter(s => s.quantityOnHand <= 0).length)

function getStockStatus(item) {
  if (item.quantityOnHand <= 0) return { label: t('inventory.stock.outOfStock'), class: 'badge-danger' }
  if (item.belowMinimum) return { label: t('inventory.stock.lowStock'), class: 'badge-warning' }
  return { label: t('inventory.stock.inStock'), class: 'badge-success' }
}

function formatQty(val) {
  if (val == null) return '0'
  const n = Number(val)
  return n % 1 === 0 ? n.toFixed(0) : n.toFixed(2)
}

function openAdjustModal(item) {
  adjustItem.value = item
  adjustForm.value = {
    newQuantity: item.quantityOnHand ?? 0,
    reason: '',
    notes: ''
  }
  adjustErrors.value = {}
  showAdjustModal.value = true
}

function closeAdjustModal() {
  showAdjustModal.value = false
  adjustItem.value = null
}

async function submitAdjust() {
  adjustErrors.value = {}
  const qty = adjustForm.value.newQuantity
  if (qty === '' || qty === null || isNaN(Number(qty))) {
    adjustErrors.value.newQuantity = t('inventory.stock.qtyRequired')
  }
  if (!adjustForm.value.reason.trim()) {
    adjustErrors.value.reason = t('inventory.stock.reasonRequired')
  }
  if (Object.keys(adjustErrors.value).length > 0) return

  adjusting.value = true
  try {
    await stockApi.adjust({
      locationId: adjustItem.value.locationId,
      reason: adjustForm.value.reason.trim(),
      notes: adjustForm.value.notes.trim() || null,
      items: [{
        productId: adjustItem.value.productId,
        newQuantity: Number(qty),
        adjustmentQuantity: Number(qty) - (adjustItem.value.quantityOnHand ?? 0)
      }]
    })
    closeAdjustModal()
    fetchStock()
  } catch (error) {
    console.error('Adjust failed:', error)
    adjustErrors.value.api = error.response?.data?.message || t('inventory.stock.adjustError')
  } finally {
    adjusting.value = false
  }
}

// History modal state
const showHistoryModal = ref(false)
const historyItem = ref(null)
const historyLoading = ref(false)
const historyMovements = ref([])
const historyPage = ref(0)
const historyTotalPages = ref(0)

async function openHistoryModal(item) {
  historyItem.value = item
  historyPage.value = 0
  historyMovements.value = []
  historyLocationId.value = ''
  showHistoryModal.value = true
  await fetchHistory()
}

function closeHistoryModal() {
  showHistoryModal.value = false
  historyItem.value = null
  historyLocationId.value = ''
}

function onHistoryLocationChange() {
  historyPage.value = 0
  fetchHistory()
}

function historyPrev() {
  if (historyPage.value > 0) {
    historyPage.value--
    fetchHistory()
  }
}

function historyNext() {
  if (historyPage.value < historyTotalPages.value - 1) {
    historyPage.value++
    fetchHistory()
  }
}

const movementTypeLabels = {
  STOCK_IN: 'inventory.stock.history.stockIn',
  STOCK_OUT: 'inventory.stock.history.stockOut',
  TRANSFER: 'inventory.stock.history.transfer',
  ADJUSTMENT: 'inventory.stock.history.adjustment',
  INITIAL: 'inventory.stock.history.initial',
  RETURN: 'inventory.stock.history.return',
  RESERVATION: 'inventory.stock.history.reservation',
  UNRESERVATION: 'inventory.stock.history.unreservation',
  WRITE_OFF: 'inventory.stock.history.writeOff',
  PRODUCTION: 'inventory.stock.history.production',
  CONSUMPTION: 'inventory.stock.history.consumption'
}

function movementTypeLabel(type) {
  return movementTypeLabels[type] ? t(movementTypeLabels[type]) : type
}

function movementTypeClass(type) {
  switch (type) {
    case 'STOCK_IN': case 'RETURN': case 'PRODUCTION': case 'INITIAL': return 'text-green-700 bg-green-50'
    case 'STOCK_OUT': case 'CONSUMPTION': case 'WRITE_OFF': return 'text-red-700 bg-red-50'
    case 'TRANSFER': return 'text-blue-700 bg-blue-50'
    case 'ADJUSTMENT': return 'text-amber-700 bg-amber-50'
    default: return 'text-gray-700 bg-gray-50'
  }
}

function isIncoming(m) {
  return ['STOCK_IN', 'RETURN', 'PRODUCTION', 'INITIAL', 'UNRESERVATION'].includes(m.movementType) ||
    (m.movementType === 'ADJUSTMENT' && m.quantityAfter > m.quantityBefore)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('uz-Cyrl', { day: '2-digit', month: '2-digit', year: 'numeric' }) +
    ' ' + d.toLocaleTimeString('uz-Cyrl', { hour: '2-digit', minute: '2-digit' })
}

// Product detail functions (getByProduct, getAvailable, getByProductAndLocation)
async function selectProduct(item) {
  if (selectedProduct.value?.productId === item.productId) {
    selectedProduct.value = null
    productDetail.value = null
    productAvailable.value = null
    productLocationStock.value = null
    detailLocationId.value = ''
    return
  }
  selectedProduct.value = item
  productDetailLoading.value = true
  productLocationStock.value = null
  detailLocationId.value = ''
  try {
    const [detailRes, availRes] = await Promise.all([
      stockApi.getByProduct(item.productId),
      stockApi.getAvailable(item.productId)
    ])
    productDetail.value = detailRes.data.data || detailRes.data || []
    productAvailable.value = availRes.data.data ?? availRes.data ?? null
  } catch (error) {
    console.error('Failed to fetch product detail:', error)
    productDetail.value = null
    productAvailable.value = null
  } finally {
    productDetailLoading.value = false
  }
}

async function fetchProductLocationStock() {
  if (!selectedProduct.value || !detailLocationId.value) {
    productLocationStock.value = null
    return
  }
  try {
    const response = await stockApi.getByProductAndLocation(selectedProduct.value.productId, detailLocationId.value)
    productLocationStock.value = response.data.data || response.data || null
  } catch (error) {
    console.error('Failed to fetch product location stock:', error)
    productLocationStock.value = null
  }
}

watch(detailLocationId, () => {
  fetchProductLocationStock()
})

// Low stock tab
async function fetchLowStock() {
  lowStockLoading.value = true
  try {
    const response = await stockApi.getLowStock()
    const data = response.data.data || response.data
    lowStockItems.value = data.content || data || []
  } catch (error) {
    console.error('Failed to fetch low stock:', error)
  } finally {
    lowStockLoading.value = false
  }
}

// Valuation tab
async function fetchValuation() {
  valuationLoading.value = true
  try {
    const response = await stockApi.getValuation()
    valuationData.value = response.data.data || response.data || null
  } catch (error) {
    console.error('Failed to fetch valuation:', error)
  } finally {
    valuationLoading.value = false
  }
}

// Tab change handler
watch(activeTab, (tab) => {
  if (tab === 'lowStock') fetchLowStock()
  if (tab === 'valuation') fetchValuation()
})

// Check availability
async function checkAvailability() {
  availabilityErrors.value = {}
  availabilityResult.value = null
  if (!availabilityForm.value.productId) {
    availabilityErrors.value.productId = t('inventory.stock.availability.productIdRequired')
  }
  if (!availabilityForm.value.quantity || Number(availabilityForm.value.quantity) <= 0) {
    availabilityErrors.value.quantity = t('inventory.stock.availability.quantityRequired')
  }
  if (Object.keys(availabilityErrors.value).length > 0) return

  availabilityChecking.value = true
  try {
    const params = {
      productId: availabilityForm.value.productId,
      quantity: Number(availabilityForm.value.quantity)
    }
    if (availabilityForm.value.locationId) {
      params.locationId = availabilityForm.value.locationId
    }
    const response = await stockApi.checkAvailability(params)
    availabilityResult.value = response.data.data ?? response.data
  } catch (error) {
    console.error('Check availability failed:', error)
    availabilityErrors.value.api = error.response?.data?.message || t('inventory.stock.operations.error')
  } finally {
    availabilityChecking.value = false
  }
}

function closeAvailabilityTool() {
  showAvailabilityTool.value = false
  availabilityForm.value = { productId: '', locationId: '', quantity: '' }
  availabilityResult.value = null
  availabilityErrors.value = {}
}

// History location filter - getMovementsByLocation
async function fetchHistory() {
  historyLoading.value = true
  try {
    let response
    if (historyLocationId.value) {
      response = await stockApi.getMovementsByLocation(historyLocationId.value, {
        page: historyPage.value,
        size: 20,
        sort: 'movementDate,desc'
      })
    } else {
      response = await stockApi.getMovementsByProduct(historyItem.value.productId, {
        page: historyPage.value,
        size: 20,
        sort: 'movementDate,desc'
      })
    }
    historyMovements.value = response.data.content || []
    historyTotalPages.value = response.data.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch history:', error)
  } finally {
    historyLoading.value = false
  }
}

// Operations: receive, issue, transfer
function resetOperationForms() {
  receiveForm.value = { productId: '', locationId: '', quantity: '', reference: '', notes: '' }
  issueForm.value = { productId: '', locationId: '', quantity: '', reference: '', notes: '' }
  transferForm.value = { productId: '', fromLocationId: '', toLocationId: '', quantity: '', reference: '', notes: '' }
  operationErrors.value = {}
  operationSuccess.value = ''
}

function validateOperationForm(form, type) {
  const errors = {}
  if (!form.productId) errors.productId = t('inventory.stock.operations.productIdRequired')
  if (type === 'transfer') {
    if (!form.fromLocationId) errors.fromLocationId = t('inventory.stock.operations.fromLocationRequired')
    if (!form.toLocationId) errors.toLocationId = t('inventory.stock.operations.toLocationRequired')
  } else {
    if (!form.locationId) errors.locationId = t('inventory.stock.operations.locationIdRequired')
  }
  if (!form.quantity || Number(form.quantity) <= 0) errors.quantity = t('inventory.stock.operations.quantityPositive')
  return errors
}

async function submitReceive() {
  operationErrors.value = {}
  operationSuccess.value = ''
  const errors = validateOperationForm(receiveForm.value, 'receive')
  if (Object.keys(errors).length > 0) { operationErrors.value = errors; return }

  operationSubmitting.value = true
  try {
    await stockApi.receive({
      productId: receiveForm.value.productId,
      locationId: receiveForm.value.locationId,
      quantity: Number(receiveForm.value.quantity),
      reference: receiveForm.value.reference || null,
      notes: receiveForm.value.notes || null
    })
    operationSuccess.value = t('inventory.stock.operations.success')
    receiveForm.value = { productId: '', locationId: '', quantity: '', reference: '', notes: '' }
    fetchStock()
  } catch (error) {
    operationErrors.value.api = error.response?.data?.message || t('inventory.stock.operations.error')
  } finally {
    operationSubmitting.value = false
  }
}

async function submitIssue() {
  operationErrors.value = {}
  operationSuccess.value = ''
  const errors = validateOperationForm(issueForm.value, 'issue')
  if (Object.keys(errors).length > 0) { operationErrors.value = errors; return }

  operationSubmitting.value = true
  try {
    await stockApi.issue({
      productId: issueForm.value.productId,
      locationId: issueForm.value.locationId,
      quantity: Number(issueForm.value.quantity),
      reference: issueForm.value.reference || null,
      notes: issueForm.value.notes || null
    })
    operationSuccess.value = t('inventory.stock.operations.success')
    issueForm.value = { productId: '', locationId: '', quantity: '', reference: '', notes: '' }
    fetchStock()
  } catch (error) {
    operationErrors.value.api = error.response?.data?.message || t('inventory.stock.operations.error')
  } finally {
    operationSubmitting.value = false
  }
}

async function submitTransfer() {
  operationErrors.value = {}
  operationSuccess.value = ''
  const errors = validateOperationForm(transferForm.value, 'transfer')
  if (Object.keys(errors).length > 0) { operationErrors.value = errors; return }

  operationSubmitting.value = true
  try {
    await stockApi.transfer({
      productId: transferForm.value.productId,
      fromLocationId: transferForm.value.fromLocationId,
      toLocationId: transferForm.value.toLocationId,
      quantity: Number(transferForm.value.quantity),
      reference: transferForm.value.reference || null,
      notes: transferForm.value.notes || null
    })
    operationSuccess.value = t('inventory.stock.operations.success')
    transferForm.value = { productId: '', fromLocationId: '', toLocationId: '', quantity: '', reference: '', notes: '' }
    fetchStock()
  } catch (error) {
    operationErrors.value.api = error.response?.data?.message || t('inventory.stock.operations.error')
  } finally {
    operationSubmitting.value = false
  }
}

function formatCurrency(val) {
  if (val == null) return '0'
  return Number(val).toLocaleString('uz-Cyrl', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.stock.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.stock.subtitle') }}</p>
      </div>
      <button @click="showAvailabilityTool = true" class="btn btn-secondary inline-flex items-center gap-2">
        <ShieldCheckIcon class="h-5 w-5" />
        {{ $t('inventory.stock.availability.title') }}
      </button>
    </div>

    <!-- Tab Navigation -->
    <div class="border-b border-gray-200">
      <nav class="flex gap-4 -mb-px">
        <button
          @click="activeTab = 'stockList'"
          :class="['pb-3 px-1 text-sm font-medium border-b-2 transition-colors', activeTab === 'stockList' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300']"
        >
          {{ $t('inventory.stock.tabs.stockList') }}
        </button>
        <button
          @click="activeTab = 'lowStock'"
          :class="['pb-3 px-1 text-sm font-medium border-b-2 transition-colors', activeTab === 'lowStock' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300']"
        >
          <ExclamationTriangleIcon class="h-4 w-4 inline mr-1" />
          {{ $t('inventory.stock.tabs.lowStock') }}
        </button>
        <button
          @click="activeTab = 'valuation'"
          :class="['pb-3 px-1 text-sm font-medium border-b-2 transition-colors', activeTab === 'valuation' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300']"
        >
          <CurrencyDollarIcon class="h-4 w-4 inline mr-1" />
          {{ $t('inventory.stock.tabs.valuation') }}
        </button>
        <button
          @click="activeTab = 'operations'"
          :class="['pb-3 px-1 text-sm font-medium border-b-2 transition-colors', activeTab === 'operations' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300']"
        >
          <ArrowsRightLeftIcon class="h-4 w-4 inline mr-1" />
          {{ $t('inventory.stock.tabs.operations') }}
        </button>
      </nav>
    </div>

    <!-- Stock List Tab -->
    <template v-if="activeTab === 'stockList'">

    <!-- Stats -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <button
        @click="filter = 'all'"
        :class="['card cursor-pointer transition-all', filter === 'all' ? 'ring-2 ring-primary-500' : '']"
      >
        <div class="card-body text-center">
          <p class="text-3xl font-bold text-gray-900">{{ totalElements }}</p>
          <p class="text-sm text-gray-500">{{ $t('totalCount') }}</p>
        </div>
      </button>
      <button
        @click="filter = 'low'"
        :class="['card cursor-pointer transition-all', filter === 'low' ? 'ring-2 ring-yellow-500' : '']"
      >
        <div class="card-body text-center">
          <p class="text-3xl font-bold text-yellow-600">{{ lowStockCount }}</p>
          <p class="text-sm text-gray-500">{{ $t('inventory.stock.lowStock') }}</p>
        </div>
      </button>
      <button
        @click="filter = 'out'"
        :class="['card cursor-pointer transition-all', filter === 'out' ? 'ring-2 ring-red-500' : '']"
      >
        <div class="card-body text-center">
          <p class="text-3xl font-bold text-red-600">{{ outOfStockCount }}</p>
          <p class="text-sm text-gray-500">{{ $t('inventory.stock.outOfStock') }}</p>
        </div>
      </button>
    </div>

    <!-- Search & Filter -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-3">
        <div class="relative flex-1">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @input="() => { page = 0; fetchStock() }"
            type="text"
            :placeholder="$t('inventory.stock.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
        <div class="relative sm:w-64">
          <BuildingStorefrontIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <select
            v-model="selectedLocationId"
            class="input pl-10"
          >
            <option value="">{{ $t('inventory.stock.allWarehouses') }}</option>
            <option v-for="loc in locations" :key="loc.id" :value="loc.id">
              {{ loc.name }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('inventory.stock.product') }}</th>
              <th>{{ $t('inventory.products.sku') }}</th>
              <th>{{ $t('inventory.stock.warehouse') }}</th>
              <th class="text-right">{{ $t('inventory.stock.available') }}</th>
              <th class="text-right">{{ $t('inventory.stock.reserved') }}</th>
              <th class="text-right">{{ $t('inventory.stock.free') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="w-20"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="item in filteredStock" :key="item.id" @click="selectProduct(item)" class="cursor-pointer" :class="{ 'bg-primary-50': selectedProduct?.productId === item.productId }">
              <td>
                <div class="flex items-center">
                  <ExclamationTriangleIcon
                    v-if="item.belowMinimum"
                    class="h-5 w-5 text-yellow-500 mr-2 flex-shrink-0"
                  />
                  <span class="font-medium">{{ item.productName }}</span>
                  <ChevronDownIcon v-if="selectedProduct?.productId !== item.productId" class="h-4 w-4 text-gray-400 ml-1" />
                  <ChevronUpIcon v-else class="h-4 w-4 text-primary-500 ml-1" />
                </div>
              </td>
              <td class="font-mono text-sm text-gray-500">{{ item.productSku }}</td>
              <td>
                <span class="inline-flex items-center gap-1 text-sm text-gray-700">
                  <BuildingStorefrontIcon class="h-4 w-4 text-gray-400" />
                  {{ item.locationName }}
                </span>
              </td>
              <td class="text-right font-medium">{{ formatQty(item.quantityOnHand) }}</td>
              <td class="text-right text-gray-500">{{ formatQty(item.quantityReserved) }}</td>
              <td class="text-right font-medium text-primary-600">{{ formatQty(item.quantityAvailable) }}</td>
              <td>
                <span :class="['badge', getStockStatus(item).class]">
                  {{ getStockStatus(item).label }}
                </span>
              </td>
              <td>
                <div class="flex items-center gap-1">
                  <button
                    @click="openHistoryModal(item)"
                    class="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded-lg transition-colors"
                    :title="$t('inventory.stock.history.title')"
                  >
                    <ClockIcon class="h-5 w-5" />
                  </button>
                  <button
                    @click="openAdjustModal(item)"
                    class="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded-lg transition-colors"
                    :title="$t('inventory.stock.adjust')"
                  >
                    <PencilSquareIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredStock.length === 0">
              <td colspan="8" class="text-center text-gray-400 py-8">{{ $t('inventory.stock.noStock') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Product Detail Panel -->
    <div v-if="selectedProduct" class="card">
      <div class="card-header flex items-center justify-between">
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('inventory.stock.detail.title') }}: {{ selectedProduct.productName }}</h3>
        <button @click="selectedProduct = null; productDetail = null; productAvailable = null; productLocationStock = null" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
          <XMarkIcon class="h-5 w-5" />
        </button>
      </div>
      <div class="card-body">
        <div v-if="productDetailLoading" class="flex items-center justify-center h-24">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-else class="space-y-4">
          <!-- Available quantity summary -->
          <div v-if="productAvailable != null" class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <p class="text-2xl font-bold text-gray-900">{{ formatQty(productAvailable.totalOnHand ?? productAvailable) }}</p>
              <p class="text-sm text-gray-500">{{ $t('inventory.stock.detail.totalOnHand') }}</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <p class="text-2xl font-bold text-amber-600">{{ formatQty(productAvailable.totalReserved ?? 0) }}</p>
              <p class="text-sm text-gray-500">{{ $t('inventory.stock.detail.totalReserved') }}</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-3 text-center">
              <p class="text-2xl font-bold text-primary-600">{{ formatQty(productAvailable.totalAvailable ?? productAvailable) }}</p>
              <p class="text-sm text-gray-500">{{ $t('inventory.stock.detail.totalAvailable') }}</p>
            </div>
          </div>

          <!-- Stock by location from getByProduct -->
          <div v-if="Array.isArray(productDetail) && productDetail.length > 0">
            <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('inventory.stock.detail.stockByProduct') }}</h4>
            <div class="space-y-2">
              <div v-for="s in productDetail" :key="s.id || s.locationId" class="flex items-center justify-between p-2 rounded-lg border border-gray-100">
                <span class="text-sm text-gray-700 inline-flex items-center gap-1">
                  <BuildingStorefrontIcon class="h-4 w-4 text-gray-400" />
                  {{ s.locationName }}
                </span>
                <span class="text-sm font-medium">{{ formatQty(s.quantityOnHand) }} / {{ formatQty(s.quantityAvailable) }}</span>
              </div>
            </div>
          </div>
          <div v-else-if="!Array.isArray(productDetail)">
            <p class="text-sm text-gray-400">{{ $t('inventory.stock.detail.noData') }}</p>
          </div>

          <!-- Stock at specific location (getByProductAndLocation) -->
          <div class="border-t pt-4">
            <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('inventory.stock.detail.stockAtLocation') }}</h4>
            <div class="flex gap-3 items-end">
              <div class="flex-1">
                <select v-model="detailLocationId" class="input">
                  <option value="">{{ $t('inventory.stock.detail.selectLocation') }}</option>
                  <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
                </select>
              </div>
            </div>
            <div v-if="productLocationStock" class="mt-3 bg-gray-50 rounded-lg p-3">
              <div class="grid grid-cols-3 gap-4 text-center text-sm">
                <div>
                  <p class="font-semibold">{{ formatQty(productLocationStock.quantityOnHand) }}</p>
                  <p class="text-gray-500">{{ $t('inventory.stock.available') }}</p>
                </div>
                <div>
                  <p class="font-semibold">{{ formatQty(productLocationStock.quantityReserved) }}</p>
                  <p class="text-gray-500">{{ $t('inventory.stock.reserved') }}</p>
                </div>
                <div>
                  <p class="font-semibold text-primary-600">{{ formatQty(productLocationStock.quantityAvailable) }}</p>
                  <p class="text-gray-500">{{ $t('inventory.stock.free') }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    </template>

    <!-- Low Stock Tab -->
    <template v-if="activeTab === 'lowStock'">
    <div class="card">
      <div class="card-header">
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('inventory.stock.lowStockTab.title') }}</h3>
        <p class="text-sm text-gray-500">{{ $t('inventory.stock.lowStockTab.subtitle') }}</p>
      </div>
      <div v-if="lowStockLoading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
      <div v-else-if="lowStockItems.length > 0" class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('inventory.stock.product') }}</th>
              <th>{{ $t('inventory.products.sku') }}</th>
              <th>{{ $t('inventory.stock.warehouse') }}</th>
              <th class="text-right">{{ $t('inventory.stock.available') }}</th>
              <th class="text-right">{{ $t('inventory.stock.lowStockTab.reorderPoint') }}</th>
              <th class="text-right">{{ $t('inventory.stock.lowStockTab.deficit') }}</th>
              <th>{{ $t('status') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="item in lowStockItems" :key="item.id">
              <td>
                <div class="flex items-center">
                  <ExclamationTriangleIcon class="h-5 w-5 text-yellow-500 mr-2 flex-shrink-0" />
                  <span class="font-medium">{{ item.productName }}</span>
                </div>
              </td>
              <td class="font-mono text-sm text-gray-500">{{ item.productSku }}</td>
              <td>
                <span class="inline-flex items-center gap-1 text-sm text-gray-700">
                  <BuildingStorefrontIcon class="h-4 w-4 text-gray-400" />
                  {{ item.locationName }}
                </span>
              </td>
              <td class="text-right font-medium">{{ formatQty(item.quantityOnHand) }}</td>
              <td class="text-right text-gray-500">{{ formatQty(item.reorderPoint ?? item.minimumQuantity) }}</td>
              <td class="text-right font-medium text-red-600">{{ formatQty((item.reorderPoint ?? item.minimumQuantity ?? 0) - (item.quantityOnHand ?? 0)) }}</td>
              <td>
                <span :class="['badge', getStockStatus(item).class]">
                  {{ getStockStatus(item).label }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="card-body text-center text-gray-400 py-8">
        {{ $t('inventory.stock.lowStockTab.noItems') }}
      </div>
    </div>
    </template>

    <!-- Valuation Tab -->
    <template v-if="activeTab === 'valuation'">
    <div class="space-y-4">
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-semibold text-gray-900">{{ $t('inventory.stock.valuationTab.title') }}</h3>
          <p class="text-sm text-gray-500">{{ $t('inventory.stock.valuationTab.subtitle') }}</p>
        </div>
        <div v-if="valuationLoading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
        <template v-else-if="valuationData">
          <div class="card-body">
            <div class="grid grid-cols-1 sm:grid-cols-4 gap-4 mb-6">
              <div class="bg-primary-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-primary-700">{{ formatCurrency(valuationData.totalValue ?? valuationData.totalCost) }}</p>
                <p class="text-sm text-primary-600">{{ $t('inventory.stock.valuationTab.totalValue') }}</p>
              </div>
              <div class="bg-gray-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-gray-900">{{ valuationData.totalItems ?? valuationData.itemCount ?? '-' }}</p>
                <p class="text-sm text-gray-500">{{ $t('inventory.stock.valuationTab.totalItems') }}</p>
              </div>
              <div class="bg-gray-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-gray-900">{{ formatQty(valuationData.totalQuantity) }}</p>
                <p class="text-sm text-gray-500">{{ $t('inventory.stock.valuationTab.totalQuantity') }}</p>
              </div>
              <div class="bg-gray-50 rounded-lg p-4 text-center">
                <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(valuationData.averageCost ?? valuationData.avgCost) }}</p>
                <p class="text-sm text-gray-500">{{ $t('inventory.stock.valuationTab.avgCost') }}</p>
              </div>
            </div>
          </div>
          <div v-if="valuationData.items || valuationData.content" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('inventory.stock.valuationTab.product') }}</th>
                  <th class="text-right">{{ $t('inventory.stock.valuationTab.quantity') }}</th>
                  <th class="text-right">{{ $t('inventory.stock.valuationTab.unitCost') }}</th>
                  <th class="text-right">{{ $t('inventory.stock.valuationTab.totalCost') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="item in (valuationData.items || valuationData.content)" :key="item.productId || item.id">
                  <td class="font-medium">{{ item.productName }}</td>
                  <td class="text-right">{{ formatQty(item.quantity ?? item.quantityOnHand) }}</td>
                  <td class="text-right text-gray-500">{{ formatCurrency(item.unitCost ?? item.costPrice) }}</td>
                  <td class="text-right font-medium">{{ formatCurrency(item.totalCost ?? item.totalValue) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
        <div v-else class="card-body text-center text-gray-400 py-8">
          {{ $t('inventory.stock.valuationTab.noData') }}
        </div>
      </div>
    </div>
    </template>

    <!-- Operations Tab -->
    <template v-if="activeTab === 'operations'">
    <div class="space-y-4">
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-semibold text-gray-900">{{ $t('inventory.stock.operations.title') }}</h3>
          <p class="text-sm text-gray-500">{{ $t('inventory.stock.operations.subtitle') }}</p>
        </div>
        <div class="card-body">
          <!-- Operation sub-tabs -->
          <div class="flex gap-2 mb-6">
            <button
              @click="operationSubTab = 'receive'; resetOperationForms()"
              :class="['btn btn-sm inline-flex items-center gap-1', operationSubTab === 'receive' ? 'btn-primary' : 'btn-secondary']"
            >
              <InboxArrowDownIcon class="h-4 w-4" />
              {{ $t('inventory.stock.operations.receive') }}
            </button>
            <button
              @click="operationSubTab = 'issue'; resetOperationForms()"
              :class="['btn btn-sm inline-flex items-center gap-1', operationSubTab === 'issue' ? 'btn-primary' : 'btn-secondary']"
            >
              <ArrowRightStartOnRectangleIcon class="h-4 w-4" />
              {{ $t('inventory.stock.operations.issue') }}
            </button>
            <button
              @click="operationSubTab = 'transfer'; resetOperationForms()"
              :class="['btn btn-sm inline-flex items-center gap-1', operationSubTab === 'transfer' ? 'btn-primary' : 'btn-secondary']"
            >
              <ArrowsRightLeftIcon class="h-4 w-4" />
              {{ $t('inventory.stock.operations.transfer') }}
            </button>
          </div>

          <!-- Success message -->
          <div v-if="operationSuccess" class="bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg p-3 mb-4 flex items-center gap-2">
            <CheckCircleIcon class="h-5 w-5 flex-shrink-0" />
            {{ operationSuccess }}
          </div>

          <!-- API error -->
          <div v-if="operationErrors.api" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3 mb-4 flex items-center gap-2">
            <XCircleIcon class="h-5 w-5 flex-shrink-0" />
            {{ operationErrors.api }}
          </div>

          <!-- Receive Form -->
          <div v-if="operationSubTab === 'receive'" class="space-y-4 max-w-lg">
            <h4 class="font-medium text-gray-900">{{ $t('inventory.stock.operations.receiveTitle') }}</h4>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.productId') }} <span class="text-red-500">*</span></label>
              <input v-model="receiveForm.productId" type="text" class="input" :class="{ 'border-red-500': operationErrors.productId }" />
              <p v-if="operationErrors.productId" class="text-sm text-red-500 mt-1">{{ operationErrors.productId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.locationId') }} <span class="text-red-500">*</span></label>
              <select v-model="receiveForm.locationId" class="input" :class="{ 'border-red-500': operationErrors.locationId }">
                <option value="">{{ $t('inventory.stock.detail.selectLocation') }}</option>
                <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
              </select>
              <p v-if="operationErrors.locationId" class="text-sm text-red-500 mt-1">{{ operationErrors.locationId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.quantity') }} <span class="text-red-500">*</span></label>
              <input v-model.number="receiveForm.quantity" type="number" step="any" min="0" class="input" :class="{ 'border-red-500': operationErrors.quantity }" />
              <p v-if="operationErrors.quantity" class="text-sm text-red-500 mt-1">{{ operationErrors.quantity }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.reference') }}</label>
              <input v-model="receiveForm.reference" type="text" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.notes') }}</label>
              <textarea v-model="receiveForm.notes" rows="2" class="input"></textarea>
            </div>
            <button @click="submitReceive" :disabled="operationSubmitting" class="btn btn-primary">
              <span v-if="operationSubmitting" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
              {{ $t('inventory.stock.operations.submit') }}
            </button>
          </div>

          <!-- Issue Form -->
          <div v-if="operationSubTab === 'issue'" class="space-y-4 max-w-lg">
            <h4 class="font-medium text-gray-900">{{ $t('inventory.stock.operations.issueTitle') }}</h4>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.productId') }} <span class="text-red-500">*</span></label>
              <input v-model="issueForm.productId" type="text" class="input" :class="{ 'border-red-500': operationErrors.productId }" />
              <p v-if="operationErrors.productId" class="text-sm text-red-500 mt-1">{{ operationErrors.productId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.locationId') }} <span class="text-red-500">*</span></label>
              <select v-model="issueForm.locationId" class="input" :class="{ 'border-red-500': operationErrors.locationId }">
                <option value="">{{ $t('inventory.stock.detail.selectLocation') }}</option>
                <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
              </select>
              <p v-if="operationErrors.locationId" class="text-sm text-red-500 mt-1">{{ operationErrors.locationId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.quantity') }} <span class="text-red-500">*</span></label>
              <input v-model.number="issueForm.quantity" type="number" step="any" min="0" class="input" :class="{ 'border-red-500': operationErrors.quantity }" />
              <p v-if="operationErrors.quantity" class="text-sm text-red-500 mt-1">{{ operationErrors.quantity }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.reference') }}</label>
              <input v-model="issueForm.reference" type="text" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.notes') }}</label>
              <textarea v-model="issueForm.notes" rows="2" class="input"></textarea>
            </div>
            <button @click="submitIssue" :disabled="operationSubmitting" class="btn btn-primary">
              <span v-if="operationSubmitting" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
              {{ $t('inventory.stock.operations.submit') }}
            </button>
          </div>

          <!-- Transfer Form -->
          <div v-if="operationSubTab === 'transfer'" class="space-y-4 max-w-lg">
            <h4 class="font-medium text-gray-900">{{ $t('inventory.stock.operations.transferTitle') }}</h4>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.productId') }} <span class="text-red-500">*</span></label>
              <input v-model="transferForm.productId" type="text" class="input" :class="{ 'border-red-500': operationErrors.productId }" />
              <p v-if="operationErrors.productId" class="text-sm text-red-500 mt-1">{{ operationErrors.productId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.fromLocationId') }} <span class="text-red-500">*</span></label>
              <select v-model="transferForm.fromLocationId" class="input" :class="{ 'border-red-500': operationErrors.fromLocationId }">
                <option value="">{{ $t('inventory.stock.detail.selectLocation') }}</option>
                <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
              </select>
              <p v-if="operationErrors.fromLocationId" class="text-sm text-red-500 mt-1">{{ operationErrors.fromLocationId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.toLocationId') }} <span class="text-red-500">*</span></label>
              <select v-model="transferForm.toLocationId" class="input" :class="{ 'border-red-500': operationErrors.toLocationId }">
                <option value="">{{ $t('inventory.stock.detail.selectLocation') }}</option>
                <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
              </select>
              <p v-if="operationErrors.toLocationId" class="text-sm text-red-500 mt-1">{{ operationErrors.toLocationId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.quantity') }} <span class="text-red-500">*</span></label>
              <input v-model.number="transferForm.quantity" type="number" step="any" min="0" class="input" :class="{ 'border-red-500': operationErrors.quantity }" />
              <p v-if="operationErrors.quantity" class="text-sm text-red-500 mt-1">{{ operationErrors.quantity }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.reference') }}</label>
              <input v-model="transferForm.reference" type="text" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.stock.operations.notes') }}</label>
              <textarea v-model="transferForm.notes" rows="2" class="input"></textarea>
            </div>
            <button @click="submitTransfer" :disabled="operationSubmitting" class="btn btn-primary">
              <span v-if="operationSubmitting" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
              {{ $t('inventory.stock.operations.submit') }}
            </button>
          </div>
        </div>
      </div>
    </div>
    </template>

    <!-- Check Availability Modal -->
    <Teleport to="body">
      <div v-if="showAvailabilityTool" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeAvailabilityTool"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-5">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ $t('inventory.stock.availability.title') }}</h3>
              <button @click="closeAvailabilityTool" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div v-if="availabilityErrors.api" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
              {{ availabilityErrors.api }}
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('inventory.stock.availability.productId') }} <span class="text-red-500">*</span></label>
                <input v-model="availabilityForm.productId" type="text" class="input" :class="{ 'border-red-500': availabilityErrors.productId }" />
                <p v-if="availabilityErrors.productId" class="text-sm text-red-500 mt-1">{{ availabilityErrors.productId }}</p>
              </div>
              <div>
                <label class="label">{{ $t('inventory.stock.availability.locationId') }}</label>
                <select v-model="availabilityForm.locationId" class="input">
                  <option value="">{{ $t('inventory.stock.allWarehouses') }}</option>
                  <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
                </select>
              </div>
              <div>
                <label class="label">{{ $t('inventory.stock.availability.quantity') }} <span class="text-red-500">*</span></label>
                <input v-model.number="availabilityForm.quantity" type="number" step="any" min="0" class="input" :class="{ 'border-red-500': availabilityErrors.quantity }" />
                <p v-if="availabilityErrors.quantity" class="text-sm text-red-500 mt-1">{{ availabilityErrors.quantity }}</p>
              </div>
            </div>

            <!-- Result -->
            <div v-if="availabilityResult != null" class="rounded-lg p-4" :class="availabilityResult === true || availabilityResult.available ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'">
              <div class="flex items-center gap-2">
                <CheckCircleIcon v-if="availabilityResult === true || availabilityResult.available" class="h-6 w-6 text-green-600" />
                <XCircleIcon v-else class="h-6 w-6 text-red-600" />
                <span class="font-medium" :class="availabilityResult === true || availabilityResult.available ? 'text-green-700' : 'text-red-700'">
                  {{ (availabilityResult === true || availabilityResult.available) ? $t('inventory.stock.availability.isAvailable') : $t('inventory.stock.availability.notAvailable') }}
                </span>
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button @click="closeAvailabilityTool" class="btn btn-secondary">{{ $t('cancel') }}</button>
              <button @click="checkAvailability" :disabled="availabilityChecking" class="btn btn-primary">
                <span v-if="availabilityChecking" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ $t('inventory.stock.availability.check') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Adjust Modal -->
    <Teleport to="body">
      <div v-if="showAdjustModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeAdjustModal"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-5">
            <!-- Header -->
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ $t('inventory.stock.adjustTitle') }}</h3>
              <button @click="closeAdjustModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <!-- Product info -->
            <div class="bg-gray-50 rounded-lg p-3 space-y-1">
              <p class="font-medium text-gray-900">{{ adjustItem?.productName }}</p>
              <p class="text-sm text-gray-500">{{ adjustItem?.locationName }} &middot; {{ adjustItem?.productSku }}</p>
            </div>

            <!-- API error -->
            <div v-if="adjustErrors.api" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
              {{ adjustErrors.api }}
            </div>

            <!-- Form -->
            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('inventory.stock.currentQty') }}</label>
                <input type="text" class="input bg-gray-50" :value="formatQty(adjustItem?.quantityOnHand)" disabled />
              </div>

              <div>
                <label class="label">{{ $t('inventory.stock.newQty') }} <span class="text-red-500">*</span></label>
                <input
                  v-model.number="adjustForm.newQuantity"
                  type="number"
                  step="any"
                  min="0"
                  class="input"
                  :class="{ 'border-red-500': adjustErrors.newQuantity }"
                  @keyup.enter="submitAdjust"
                />
                <p v-if="adjustErrors.newQuantity" class="text-sm text-red-500 mt-1">{{ adjustErrors.newQuantity }}</p>
              </div>

              <div>
                <label class="label">{{ $t('inventory.stock.adjustReason') }} <span class="text-red-500">*</span></label>
                <input
                  v-model="adjustForm.reason"
                  type="text"
                  class="input"
                  :class="{ 'border-red-500': adjustErrors.reason }"
                  :placeholder="$t('inventory.stock.adjustReasonPlaceholder')"
                  @keyup.enter="submitAdjust"
                />
                <p v-if="adjustErrors.reason" class="text-sm text-red-500 mt-1">{{ adjustErrors.reason }}</p>
              </div>

              <div>
                <label class="label">{{ $t('inventory.stock.adjustNotes') }}</label>
                <textarea
                  v-model="adjustForm.notes"
                  rows="2"
                  class="input"
                  :placeholder="$t('inventory.stock.adjustNotesPlaceholder')"
                ></textarea>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex justify-end gap-3 pt-2">
              <button @click="closeAdjustModal" class="btn btn-secondary">
                {{ $t('cancel') }}
              </button>
              <button @click="submitAdjust" :disabled="adjusting" class="btn btn-primary">
                <span v-if="adjusting" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ $t('save') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- History Modal -->
    <Teleport to="body">
      <div v-if="showHistoryModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeHistoryModal"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 space-y-4">
            <!-- Header -->
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-lg font-semibold text-gray-900">{{ $t('inventory.stock.history.title') }}</h3>
                <p class="text-sm text-gray-500">{{ historyItem?.productName }} &middot; {{ historyItem?.productSku }}</p>
              </div>
              <button @click="closeHistoryModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <!-- Location filter for movements -->
            <div class="flex items-center gap-2">
              <FunnelIcon class="h-4 w-4 text-gray-400 flex-shrink-0" />
              <select
                v-model="historyLocationId"
                @change="onHistoryLocationChange"
                class="input text-sm"
              >
                <option value="">{{ $t('inventory.stock.history.allLocations') }}</option>
                <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
              </select>
            </div>

            <!-- Loading -->
            <div v-if="historyLoading" class="flex items-center justify-center h-32">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>

            <!-- Movements list -->
            <div v-else-if="historyMovements.length > 0" class="space-y-2 max-h-[60vh] overflow-y-auto">
              <div
                v-for="m in historyMovements"
                :key="m.id"
                class="flex items-start gap-3 p-3 rounded-lg border border-gray-100 hover:bg-gray-50"
              >
                <div
                  :class="['flex-shrink-0 p-1.5 rounded-full', isIncoming(m) ? 'bg-green-100' : 'bg-red-100']"
                >
                  <ArrowDownIcon v-if="isIncoming(m)" class="h-4 w-4 text-green-600" />
                  <ArrowUpIcon v-else class="h-4 w-4 text-red-600" />
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span :class="['text-xs font-medium px-2 py-0.5 rounded-full', movementTypeClass(m.movementType)]">
                      {{ movementTypeLabel(m.movementType) }}
                    </span>
                    <span class="text-xs text-gray-400">{{ formatDate(m.movementDate) }}</span>
                  </div>
                  <div class="mt-1 flex items-baseline gap-2">
                    <span class="text-sm text-gray-500">{{ formatQty(m.quantityBefore) }}</span>
                    <span class="text-xs text-gray-400">&rarr;</span>
                    <span class="text-sm font-semibold text-gray-900">{{ formatQty(m.quantityAfter) }}</span>
                    <span :class="['text-xs font-medium', isIncoming(m) ? 'text-green-600' : 'text-red-600']">
                      {{ isIncoming(m) ? '+' : '-' }}{{ formatQty(m.quantity) }}
                    </span>
                  </div>
                  <p v-if="m.reason" class="mt-0.5 text-xs text-gray-500">{{ m.reason }}</p>
                  <p v-if="m.toLocationName || m.fromLocationName" class="mt-0.5 text-xs text-gray-400">
                    <template v-if="m.fromLocationName && m.toLocationName">
                      {{ m.fromLocationName }} &rarr; {{ m.toLocationName }}
                    </template>
                    <template v-else>
                      {{ m.toLocationName || m.fromLocationName }}
                    </template>
                  </p>
                </div>
              </div>
            </div>

            <!-- Empty -->
            <div v-else class="text-center text-gray-400 py-8">
              {{ $t('inventory.stock.history.noMovements') }}
            </div>

            <!-- Pagination -->
            <div v-if="historyTotalPages > 1" class="flex items-center justify-between pt-2 border-t">
              <button
                @click="historyPrev"
                :disabled="historyPage === 0"
                class="btn btn-secondary btn-sm"
              >
                {{ $t('previous') }}
              </button>
              <span class="text-sm text-gray-500">
                {{ historyPage + 1 }} / {{ historyTotalPages }}
              </span>
              <button
                @click="historyNext"
                :disabled="historyPage >= historyTotalPages - 1"
                class="btn btn-secondary btn-sm"
              >
                {{ $t('next') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
