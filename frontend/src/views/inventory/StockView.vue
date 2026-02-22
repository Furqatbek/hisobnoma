<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { stockApi, warehousesApi } from '@/services/api'
import { MagnifyingGlassIcon, ExclamationTriangleIcon, BuildingStorefrontIcon } from '@heroicons/vue/24/outline'

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
            </tr>
            <tr v-if="filteredStock.length === 0">
              <td colspan="7" class="text-center text-gray-400 py-8">{{ $t('inventory.stock.noStock') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
