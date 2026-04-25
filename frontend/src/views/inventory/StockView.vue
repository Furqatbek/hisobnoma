<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { stockApi, warehousesApi } from '@/services/api'
import { MagnifyingGlassIcon, ExclamationTriangleIcon, BuildingStorefrontIcon, PencilSquareIcon, XMarkIcon } from '@heroicons/vue/24/outline'

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
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.stock.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.stock.subtitle') }}</p>
    </div>

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
              <th class="w-10"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="item in filteredStock" :key="item.id">
              <td>
                <div class="flex items-center">
                  <ExclamationTriangleIcon
                    v-if="item.belowMinimum"
                    class="h-5 w-5 text-yellow-500 mr-2 flex-shrink-0"
                  />
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
              <td class="text-right text-gray-500">{{ formatQty(item.quantityReserved) }}</td>
              <td class="text-right font-medium text-primary-600">{{ formatQty(item.quantityAvailable) }}</td>
              <td>
                <span :class="['badge', getStockStatus(item).class]">
                  {{ getStockStatus(item).label }}
                </span>
              </td>
              <td>
                <button
                  @click="openAdjustModal(item)"
                  class="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded-lg transition-colors"
                  :title="$t('inventory.stock.adjust')"
                >
                  <PencilSquareIcon class="h-5 w-5" />
                </button>
              </td>
            </tr>
            <tr v-if="filteredStock.length === 0">
              <td colspan="8" class="text-center text-gray-400 py-8">{{ $t('inventory.stock.noStock') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

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
  </div>
</template>
