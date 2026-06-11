<script setup>
import { formatCurrency } from '@/utils/format'
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { inventoryPlanningApi, locationsApi, unwrapData, unwrapList } from '@/services/api'
import {
  ArrowPathIcon, FunnelIcon, ExclamationTriangleIcon,
  ArrowTrendingDownIcon, ArchiveBoxXMarkIcon, ChartBarIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const activeTab = ref('reorder')
const loading = ref(false)
const locations = ref([])
const selectedLocation = ref(null)

const reorderData = ref([])
const abcData = ref([])
const slowMovingData = ref([])
const deadStockData = ref([])

const abcDays = ref(365)
const slowDays = ref(90)
const deadDays = ref(180)

const tabs = [
  { key: 'reorder', icon: ExclamationTriangleIcon },
  { key: 'abc', icon: ChartBarIcon },
  { key: 'slowMoving', icon: ArrowTrendingDownIcon },
  { key: 'deadStock', icon: ArchiveBoxXMarkIcon }
]

onMounted(async () => {
  try {
    const res = await locationsApi.getAll()
    const data = unwrapData(res)
    locations.value = data?.content || data || []
  } catch (e) {
    console.error('Failed to load locations:', e)
  }
  loadTab()
})

async function loadTab() {
  loading.value = true
  try {
    switch (activeTab.value) {
      case 'reorder': {
        const res = await inventoryPlanningApi.getReorderSuggestions(selectedLocation.value)
        reorderData.value = unwrapList(res)
        break
      }
      case 'abc': {
        const res = await inventoryPlanningApi.getAbcAnalysis(abcDays.value)
        abcData.value = unwrapList(res)
        break
      }
      case 'slowMoving': {
        const res = await inventoryPlanningApi.getSlowMoving(slowDays.value, selectedLocation.value)
        slowMovingData.value = unwrapList(res)
        break
      }
      case 'deadStock': {
        const res = await inventoryPlanningApi.getDeadStock(deadDays.value, selectedLocation.value)
        deadStockData.value = unwrapList(res)
        break
      }
    }
  } catch (error) {
    console.error('Failed to load planning data:', error)
  } finally {
    loading.value = false
  }
}

function switchTab(key) {
  activeTab.value = key
  loadTab()
}

function formatNumber(value) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 1 }).format(value || 0)
}

function priorityClass(priority) {
  const cls = {
    CRITICAL: 'bg-red-100 text-red-700',
    HIGH: 'bg-orange-100 text-orange-700',
    MEDIUM: 'bg-yellow-100 text-yellow-700',
    LOW: 'bg-green-100 text-green-700'
  }
  return cls[priority] || 'bg-gray-100 text-gray-600'
}

function abcClass(classification) {
  const cls = {
    A: 'bg-green-100 text-green-700 border-green-300',
    B: 'bg-blue-100 text-blue-700 border-blue-300',
    C: 'bg-gray-100 text-gray-600 border-gray-300'
  }
  return cls[classification] || 'bg-gray-100 text-gray-600'
}

function recommendationLabel(rec) {
  return t(`inventory.planning.rec.${rec}`)
}

function abcSummary() {
  const a = abcData.value.filter(i => i.classification === 'A').length
  const b = abcData.value.filter(i => i.classification === 'B').length
  const c = abcData.value.filter(i => i.classification === 'C').length
  return { a, b, c, total: abcData.value.length }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.planning.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.planning.subtitle') }}</p>
      </div>
      <button @click="loadTab" :disabled="loading" class="btn btn-secondary">
        <ArrowPathIcon :class="['h-5 w-5 mr-2', { 'animate-spin': loading }]" />
        {{ $t('inventory.planning.refresh') }}
      </button>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex gap-1 overflow-x-auto pb-px">
        <button
          v-for="tab in tabs" :key="tab.key"
          @click="switchTab(tab.key)"
          :class="['flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
            activeTab === tab.key
              ? 'border-primary-500 text-primary-600 bg-primary-50/50'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
        >
          <component :is="tab.icon" class="h-4 w-4" />
          {{ $t(`inventory.planning.tabs.${tab.key}`) }}
        </button>
      </nav>
    </div>

    <!-- Filters -->
    <div class="flex flex-wrap gap-4 items-end">
      <div v-if="activeTab !== 'abc'" class="w-52">
        <label class="label text-xs">{{ $t('inventory.planning.filterLocation') }}</label>
        <select v-model="selectedLocation" @change="loadTab" class="input input-sm">
          <option :value="null">{{ $t('all') }}</option>
          <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
        </select>
      </div>
      <div v-if="activeTab === 'abc'" class="w-40">
        <label class="label text-xs">{{ $t('inventory.planning.period') }}</label>
        <select v-model="abcDays" @change="loadTab" class="input input-sm">
          <option :value="90">90 {{ $t('inventory.planning.days') }}</option>
          <option :value="180">180 {{ $t('inventory.planning.days') }}</option>
          <option :value="365">365 {{ $t('inventory.planning.days') }}</option>
        </select>
      </div>
      <div v-if="activeTab === 'slowMoving'" class="w-40">
        <label class="label text-xs">{{ $t('inventory.planning.noSalesDays') }}</label>
        <select v-model="slowDays" @change="loadTab" class="input input-sm">
          <option :value="30">30 {{ $t('inventory.planning.days') }}</option>
          <option :value="60">60 {{ $t('inventory.planning.days') }}</option>
          <option :value="90">90 {{ $t('inventory.planning.days') }}</option>
          <option :value="180">180 {{ $t('inventory.planning.days') }}</option>
        </select>
      </div>
      <div v-if="activeTab === 'deadStock'" class="w-40">
        <label class="label text-xs">{{ $t('inventory.planning.noSalesDays') }}</label>
        <select v-model="deadDays" @change="loadTab" class="input input-sm">
          <option :value="90">90 {{ $t('inventory.planning.days') }}</option>
          <option :value="180">180 {{ $t('inventory.planning.days') }}</option>
          <option :value="365">365 {{ $t('inventory.planning.days') }}</option>
        </select>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <!-- ============ REORDER SUGGESTIONS ============ -->
    <template v-else-if="activeTab === 'reorder'">
      <div v-if="reorderData.length === 0" class="card text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.planning.noReorder') }}</p>
      </div>
      <div v-else class="card">
        <div class="table-container">
          <table class="table text-sm">
            <thead>
              <tr>
                <th class="text-xs">{{ $t('inventory.planning.product') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.location') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.currentStock') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.reorderPoint') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.suggestedQty') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.estimatedCost') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.daysOfStock') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.vendor') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.priority') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="item in reorderData" :key="item.productId + '-' + item.locationId">
                <td>
                  <p class="font-medium">{{ item.productName }}</p>
                  <p class="text-xs text-gray-400 font-mono">{{ item.productSku }}</p>
                </td>
                <td class="text-gray-600">{{ item.locationName || '-' }}</td>
                <td class="text-right font-mono">{{ formatNumber(item.currentStock) }}</td>
                <td class="text-right font-mono text-gray-500">{{ formatNumber(item.reorderPoint) }}</td>
                <td class="text-right font-mono font-semibold text-primary-600">{{ formatNumber(item.suggestedQuantity || item.suggestedOrderQuantity) }}</td>
                <td class="text-right font-mono">{{ formatCurrency(item.estimatedCost || item.estimatedOrderValue) }}</td>
                <td class="text-right font-mono">{{ item.daysOfStock ?? '-' }}</td>
                <td class="text-gray-600 text-sm">{{ item.preferredVendorName || '-' }}</td>
                <td>
                  <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', priorityClass(item.priority)]">
                    {{ $t(`inventory.planning.priorityLevel.${item.priority}`) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ============ ABC ANALYSIS ============ -->
    <template v-else-if="activeTab === 'abc'">
      <!-- ABC Summary Cards -->
      <div v-if="abcData.length > 0" class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div v-for="cls in ['A', 'B', 'C']" :key="cls"
          :class="['card p-4 border-l-4', abcClass(cls)]"
        >
          <div class="flex justify-between items-center">
            <div>
              <p class="text-sm font-bold">{{ $t(`inventory.planning.class${cls}`) }}</p>
              <p class="text-xs text-gray-500">{{ $t(`inventory.planning.class${cls}Desc`) }}</p>
            </div>
            <span class="text-2xl font-bold">
              {{ abcData.filter(i => i.classification === cls).length }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="abcData.length === 0" class="card text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.planning.noData') }}</p>
      </div>
      <div v-else class="card">
        <div class="table-container">
          <table class="table text-sm">
            <thead>
              <tr>
                <th class="text-xs">#</th>
                <th class="text-xs">{{ $t('inventory.planning.product') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.category') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.class') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.totalValue') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.percentOfTotal') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.cumulativePercent') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.currentStock') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.stockValue') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="item in abcData" :key="item.productId">
                <td class="text-gray-400">{{ item.rank }}</td>
                <td>
                  <p class="font-medium">{{ item.productName }}</p>
                  <p class="text-xs text-gray-400 font-mono">{{ item.productSku }}</p>
                </td>
                <td class="text-gray-600">{{ item.categoryName || '-' }}</td>
                <td>
                  <span :class="['inline-flex items-center justify-center w-7 h-7 rounded-full text-sm font-bold border', abcClass(item.classification)]">
                    {{ item.classification }}
                  </span>
                </td>
                <td class="text-right font-mono">{{ formatCurrency(item.totalValue || item.annualValue) }}</td>
                <td class="text-right font-mono">{{ formatNumber(item.percentOfTotal || item.percentageOfTotal) }}%</td>
                <td class="text-right font-mono text-gray-500">{{ formatNumber(item.cumulativePercent || item.cumulativePercentage) }}%</td>
                <td class="text-right font-mono">{{ formatNumber(item.currentStock) }}</td>
                <td class="text-right font-mono">{{ formatCurrency(item.stockValue) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ============ SLOW MOVING ============ -->
    <template v-else-if="activeTab === 'slowMoving'">
      <div v-if="slowMovingData.length === 0" class="card text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.planning.noSlowMoving') }}</p>
      </div>
      <div v-else class="card">
        <div class="table-container">
          <table class="table text-sm">
            <thead>
              <tr>
                <th class="text-xs">{{ $t('inventory.planning.product') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.location') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.category') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.currentStock') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.stockValue') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.daysSinceLastSale') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.recommendation') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="item in slowMovingData" :key="item.productId">
                <td>
                  <p class="font-medium">{{ item.productName }}</p>
                  <p class="text-xs text-gray-400 font-mono">{{ item.productSku }}</p>
                </td>
                <td class="text-gray-600">{{ item.locationName || '-' }}</td>
                <td class="text-gray-600">{{ item.categoryName || '-' }}</td>
                <td class="text-right font-mono">{{ formatNumber(item.quantityOnHand || item.currentStock) }}</td>
                <td class="text-right font-mono">{{ formatCurrency(item.stockValue) }}</td>
                <td class="text-right font-mono font-medium text-amber-600">{{ item.daysSinceLastSale ?? '-' }}</td>
                <td>
                  <span v-if="item.recommendation" class="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 font-medium">
                    {{ recommendationLabel(item.recommendation) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ============ DEAD STOCK ============ -->
    <template v-else-if="activeTab === 'deadStock'">
      <div v-if="deadStockData.length === 0" class="card text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.planning.noDeadStock') }}</p>
      </div>
      <div v-else class="card">
        <div class="table-container">
          <table class="table text-sm">
            <thead>
              <tr>
                <th class="text-xs">{{ $t('inventory.planning.product') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.location') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.category') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.currentStock') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.stockValue') }}</th>
                <th class="text-xs text-right">{{ $t('inventory.planning.daysSinceLastSale') }}</th>
                <th class="text-xs">{{ $t('inventory.planning.recommendation') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="item in deadStockData" :key="item.productId" class="bg-red-50/30">
                <td>
                  <p class="font-medium">{{ item.productName }}</p>
                  <p class="text-xs text-gray-400 font-mono">{{ item.productSku }}</p>
                </td>
                <td class="text-gray-600">{{ item.locationName || '-' }}</td>
                <td class="text-gray-600">{{ item.categoryName || '-' }}</td>
                <td class="text-right font-mono">{{ formatNumber(item.quantityOnHand || item.currentStock) }}</td>
                <td class="text-right font-mono font-medium text-red-600">{{ formatCurrency(item.stockValue) }}</td>
                <td class="text-right font-mono font-medium text-red-600">{{ item.daysSinceLastSale ?? '-' }}</td>
                <td>
                  <span v-if="item.recommendation" class="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700 font-medium">
                    {{ recommendationLabel(item.recommendation) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
