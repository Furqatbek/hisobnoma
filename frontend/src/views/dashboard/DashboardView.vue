<script setup>
import { ref, onMounted } from 'vue'
import { dashboardApi } from '@/services/api'
import { useI18n } from 'vue-i18n'
import {
  CurrencyDollarIcon,
  ShoppingCartIcon,
  CubeIcon,
  UsersIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  ExclamationTriangleIcon,
  ChartBarIcon,
  GlobeAltIcon,
  InboxArrowDownIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const loading = ref(true)
const stats = ref({
  totalSalesToday: 0,
  totalSalesThisWeek: 0,
  totalSalesThisMonth: 0,
  salesCountToday: 0,
  totalProducts: 0,
  activeProducts: 0,
  lowStockProducts: 0,
  outOfStockProducts: 0,
  totalUsers: 0,
  activeUsers: 0,
  newUsersToday: 0,
  totalReceivables: 0,
  totalPayables: 0,
  catalogLiveCount: 0,
  catalogDraftCount: 0,
  newOnlineOrders: 0,
  onlineOrdersToday: 0,
  recentOnlineOrders: []
})

onMounted(async () => {
  try {
    const response = await dashboardApi.getStats()
    const data = response.data.data || response.data
    stats.value = data
  } catch (error) {
    console.error('Failed to load dashboard stats:', error)
  } finally {
    loading.value = false
  }
  loadSalesChart('daily')
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatNumber(value) {
  return new Intl.NumberFormat('en-US').format(value || 0)
}

// Sales chart
const salesChartPeriod = ref('daily')
const salesChartData = ref([])
const salesChartLoading = ref(false)
const salesChartPeriods = [
  { key: 'daily', label: () => t('dashboard.salesChart.daily') },
  { key: 'weekly', label: () => t('dashboard.salesChart.weekly') },
  { key: 'monthly', label: () => t('dashboard.salesChart.monthly') }
]

async function loadSalesChart(period) {
  salesChartPeriod.value = period
  salesChartLoading.value = true
  try {
    const response = await dashboardApi.getSalesChart(period)
    const data = response.data.data || response.data
    salesChartData.value = Array.isArray(data) ? data : (data.items || data.chart || [])
  } catch (error) {
    console.error('Failed to load sales chart:', error)
    salesChartData.value = []
  } finally {
    salesChartLoading.value = false
  }
}

function getMaxChartValue() {
  if (salesChartData.value.length === 0) return 1
  return Math.max(...salesChartData.value.map(d => d.total || d.amount || d.value || 0), 1)
}

function getChartBarHeight(item) {
  const val = item.total || item.amount || item.value || 0
  const max = getMaxChartValue()
  return Math.max((val / max) * 100, 2) + '%'
}

function getChartItemValue(item) {
  return item.total || item.amount || item.value || 0
}

function getChartItemLabel(item) {
  return item.label || item.date || item.period || ''
}
</script>

<template>
  <div class="space-y-6">
    <!-- Page header + CBU informer -->
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('dashboard.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('dashboard.welcome') }}</p>
      </div>
      <a href="https://cbu.uz/" target="_blank" title="O'zbekiston Respublikasi Markaziy banki">
        <img src="https://cbu.uz/uz/informer/?txtclr=212121&brdclr=FFC700&bgclr=FFE27D&r_choose=USD_EUR_RUB" alt="CBU valyuta kurslari" />
      </a>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else>
      <!-- Stats grid -->
      <div class="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        <!-- Sales Today -->
        <div class="card">
          <div class="card-body">
            <div class="flex items-center">
              <div class="flex-shrink-0 p-3 bg-green-100 rounded-lg">
                <CurrencyDollarIcon class="h-6 w-6 text-green-600" />
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-500">{{ $t('dashboard.salesToday') }}</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatCurrency(stats.totalSalesToday) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm">
              <ArrowTrendingUpIcon class="h-4 w-4 text-green-500 mr-1" />
              <span class="text-green-600 font-medium">{{ stats.salesCountToday }} {{ $t('dashboard.transactions') }}</span>
            </div>
          </div>
        </div>

        <!-- Weekly Sales -->
        <div class="card">
          <div class="card-body">
            <div class="flex items-center">
              <div class="flex-shrink-0 p-3 bg-blue-100 rounded-lg">
                <ShoppingCartIcon class="h-6 w-6 text-blue-600" />
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-500">{{ $t('dashboard.thisWeek') }}</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatCurrency(stats.totalSalesThisWeek) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm text-gray-500">
              <span>{{ $t('dashboard.monthly') }}: {{ formatCurrency(stats.totalSalesThisMonth) }}</span>
            </div>
          </div>
        </div>

        <!-- Products -->
        <div class="card">
          <div class="card-body">
            <div class="flex items-center">
              <div class="flex-shrink-0 p-3 bg-purple-100 rounded-lg">
                <CubeIcon class="h-6 w-6 text-purple-600" />
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-500">{{ $t('dashboard.products') }}</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatNumber(stats.totalProducts) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm">
              <span class="text-green-600">{{ stats.activeProducts }} {{ $t('dashboard.activeCount') }}</span>
              <span class="mx-2 text-gray-300">|</span>
              <span v-if="stats.lowStockProducts > 0" class="text-yellow-600">
                {{ stats.lowStockProducts }} {{ $t('dashboard.lowStock') }}
              </span>
              <span v-else class="text-gray-500">{{ $t('dashboard.stockOk') }}</span>
            </div>
          </div>
        </div>

        <!-- Users -->
        <div class="card">
          <div class="card-body">
            <div class="flex items-center">
              <div class="flex-shrink-0 p-3 bg-orange-100 rounded-lg">
                <UsersIcon class="h-6 w-6 text-orange-600" />
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-500">{{ $t('dashboard.users') }}</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatNumber(stats.totalUsers) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm">
              <span class="text-green-600">{{ stats.activeUsers }} {{ $t('dashboard.activeUsers') }}</span>
              <span class="mx-2 text-gray-300">|</span>
              <span class="text-blue-600">+{{ stats.newUsersToday }} {{ $t('dashboard.newToday') }}</span>
            </div>
          </div>
        </div>

        <!-- Online catalog -->
        <router-link to="/web-catalog" class="card hover:shadow-md transition-shadow">
          <div class="card-body">
            <div class="flex items-center">
              <div class="flex-shrink-0 p-3 bg-teal-100 rounded-lg">
                <GlobeAltIcon class="h-6 w-6 text-teal-600" />
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-500">{{ $t('dashboard.onlineCatalog') }}</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatNumber(stats.catalogLiveCount) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm">
              <span class="text-green-600">{{ stats.catalogLiveCount }} {{ $t('dashboard.liveItems') }}</span>
              <span class="mx-2 text-gray-300">|</span>
              <span class="text-gray-500">{{ stats.catalogDraftCount }} {{ $t('dashboard.draftItems') }}</span>
            </div>
          </div>
        </router-link>

        <!-- New online orders -->
        <router-link
          to="/web-orders"
          class="card hover:shadow-md transition-shadow"
          :class="stats.newOnlineOrders > 0 ? 'ring-2 ring-red-400' : ''"
        >
          <div class="card-body">
            <div class="flex items-center">
              <div
                class="flex-shrink-0 p-3 rounded-lg"
                :class="stats.newOnlineOrders > 0 ? 'bg-red-100' : 'bg-gray-100'"
              >
                <InboxArrowDownIcon
                  class="h-6 w-6"
                  :class="stats.newOnlineOrders > 0 ? 'text-red-600' : 'text-gray-500'"
                />
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-gray-500">{{ $t('dashboard.newOnlineOrders') }}</p>
                <p class="text-2xl font-semibold" :class="stats.newOnlineOrders > 0 ? 'text-red-600' : 'text-gray-900'">
                  {{ formatNumber(stats.newOnlineOrders) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm text-gray-500">
              <span>{{ stats.onlineOrdersToday }} {{ $t('dashboard.onlineOrdersToday') }}</span>
            </div>
          </div>
        </router-link>
      </div>

      <!-- Recent online orders -->
      <div v-if="stats.recentOnlineOrders && stats.recentOnlineOrders.length > 0" class="card">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-semibold text-gray-900">{{ $t('dashboard.recentOnlineOrders') }}</h3>
            <router-link to="/web-orders" class="text-sm text-primary-700 hover:underline">
              {{ $t('dashboard.viewAllOrders') }}
            </router-link>
          </div>
          <div class="divide-y divide-gray-100">
            <div
              v-for="order in stats.recentOnlineOrders"
              :key="order.id"
              class="py-2 flex items-center justify-between text-sm"
            >
              <div class="flex items-center gap-3">
                <span class="font-medium text-primary-700">{{ order.orderNumber }}</span>
                <span class="text-gray-700">{{ order.customerName }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-gray-900 font-medium">{{ formatCurrency(order.totalAmount) }}</span>
                <span
                  class="inline-flex px-2 py-0.5 rounded-full text-xs font-medium"
                  :class="{
                    'bg-red-100 text-red-800': order.status === 'NEW',
                    'bg-blue-100 text-blue-800': order.status === 'CONFIRMED',
                    'bg-yellow-100 text-yellow-800': order.status === 'DELIVERING',
                    'bg-green-100 text-green-800': order.status === 'COMPLETED',
                    'bg-gray-100 text-gray-600': order.status === 'CANCELLED'
                  }"
                >
                  {{ $t('webOrders.status' + order.status) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Alerts section -->
      <div v-if="stats.lowStockProducts > 0 || stats.outOfStockProducts > 0" class="card border-yellow-200 bg-yellow-50">
        <div class="card-body">
          <div class="flex items-start">
            <ExclamationTriangleIcon class="h-6 w-6 text-yellow-600 flex-shrink-0" />
            <div class="ml-3">
              <h3 class="text-sm font-medium text-yellow-800">{{ $t('dashboard.inventoryAlerts') }}</h3>
              <div class="mt-2 text-sm text-yellow-700">
                <ul class="list-disc pl-5 space-y-1">
                  <li v-if="stats.lowStockProducts > 0">
                    {{ $t('dashboard.lowStockWarning', { count: stats.lowStockProducts }) }}
                  </li>
                  <li v-if="stats.outOfStockProducts > 0">
                    {{ $t('dashboard.outOfStockWarning', { count: stats.outOfStockProducts }) }}
                  </li>
                </ul>
              </div>
              <div class="mt-4">
                <RouterLink
                  to="/inventory/stock"
                  class="text-sm font-medium text-yellow-800 hover:text-yellow-900"
                >
                  {{ $t('dashboard.viewInventory') }} &rarr;
                </RouterLink>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Financial Summary -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium text-gray-900">{{ $t('dashboard.financialSummary') }}</h3>
        </div>
        <div class="card-body">
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div class="flex justify-between items-center p-4 bg-green-50 rounded-lg">
              <div>
                <p class="text-sm text-gray-500">{{ $t('dashboard.receivables') }}</p>
                <p class="text-xl font-semibold text-green-700">
                  {{ formatCurrency(stats.totalReceivables) }}
                </p>
              </div>
              <ArrowTrendingUpIcon class="h-8 w-8 text-green-400" />
            </div>
            <div class="flex justify-between items-center p-4 bg-red-50 rounded-lg">
              <div>
                <p class="text-sm text-gray-500">{{ $t('dashboard.payables') }}</p>
                <p class="text-xl font-semibold text-red-700">
                  {{ formatCurrency(stats.totalPayables) }}
                </p>
              </div>
              <ArrowTrendingDownIcon class="h-8 w-8 text-red-400" />
            </div>
            <div class="flex justify-between items-center p-4 bg-blue-50 rounded-lg">
              <div>
                <p class="text-sm text-gray-500">{{ $t('dashboard.inventoryValue') }}</p>
                <p class="text-xl font-semibold text-blue-700">
                  {{ formatCurrency(stats.totalInventoryValue) }}
                </p>
              </div>
              <CubeIcon class="h-8 w-8 text-blue-400" />
            </div>
          </div>
        </div>
      </div>

      <!-- Sales Chart -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <div class="flex items-center gap-2">
            <ChartBarIcon class="h-5 w-5 text-gray-500" />
            <h3 class="text-lg font-medium text-gray-900">{{ $t('dashboard.salesChart.title') }}</h3>
          </div>
          <div class="flex gap-1">
            <button
              v-for="p in salesChartPeriods"
              :key="p.key"
              @click="loadSalesChart(p.key)"
              :class="[
                'px-3 py-1.5 text-xs font-medium rounded-lg transition-colors',
                salesChartPeriod === p.key
                  ? 'bg-primary-100 text-primary-700'
                  : 'text-gray-500 hover:bg-gray-100'
              ]"
            >
              {{ p.label() }}
            </button>
          </div>
        </div>
        <div class="card-body">
          <div v-if="salesChartLoading" class="flex items-center justify-center h-48">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
          <div v-else-if="salesChartData.length === 0" class="text-center py-12 text-gray-400 text-sm">
            {{ $t('dashboard.salesChart.noData') }}
          </div>
          <template v-else>
            <!-- Bar chart visualization -->
            <div class="flex items-end gap-1 h-48 mb-4">
              <div
                v-for="(item, idx) in salesChartData"
                :key="idx"
                class="flex-1 flex flex-col items-center justify-end h-full"
              >
                <div class="text-[10px] text-gray-500 mb-1 truncate max-w-full">
                  {{ formatCurrency(getChartItemValue(item)) }}
                </div>
                <div
                  class="w-full bg-primary-500 rounded-t-sm min-h-[2px] transition-all duration-300"
                  :style="{ height: getChartBarHeight(item) }"
                ></div>
                <div class="text-[10px] text-gray-400 mt-1 truncate max-w-full text-center">
                  {{ getChartItemLabel(item) }}
                </div>
              </div>
            </div>
            <!-- Data table -->
            <div class="table-container mt-4">
              <table class="table text-sm">
                <thead>
                  <tr>
                    <th>{{ $t('dashboard.salesChart.period') }}</th>
                    <th class="text-right">{{ $t('dashboard.salesChart.amount') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in salesChartData" :key="idx">
                    <td>{{ getChartItemLabel(item) }}</td>
                    <td class="text-right font-medium">{{ formatCurrency(getChartItemValue(item)) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </div>
      </div>

      <!-- Quick actions -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium text-gray-900">{{ $t('dashboard.quickActions') }}</h3>
        </div>
        <div class="card-body">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <RouterLink
              to="/pos"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <ShoppingCartIcon class="h-8 w-8 text-primary-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">{{ $t('dashboard.newSale') }}</span>
            </RouterLink>
            <RouterLink
              to="/inventory/products/new"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <CubeIcon class="h-8 w-8 text-purple-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">{{ $t('dashboard.addProduct') }}</span>
            </RouterLink>
            <RouterLink
              to="/customers/new"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <UsersIcon class="h-8 w-8 text-orange-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">{{ $t('dashboard.addCustomer') }}</span>
            </RouterLink>
            <RouterLink
              to="/purchases/orders/new"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <CurrencyDollarIcon class="h-8 w-8 text-green-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">{{ $t('dashboard.purchaseOrder') }}</span>
            </RouterLink>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
