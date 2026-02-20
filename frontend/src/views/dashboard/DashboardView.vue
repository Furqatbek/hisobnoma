<script setup>
import { ref, onMounted } from 'vue'
import { dashboardApi } from '@/services/api'
import {
  CurrencyDollarIcon,
  ShoppingCartIcon,
  CubeIcon,
  UsersIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  ExclamationTriangleIcon
} from '@heroicons/vue/24/outline'

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
  totalPayables: 0
})

const recentActivities = ref([])

onMounted(async () => {
  try {
    const response = await dashboardApi.getStats()
    const data = response.data.data || response.data
    stats.value = data
    recentActivities.value = data.recentActivities || []
  } catch (error) {
    console.error('Failed to load dashboard stats:', error)
  } finally {
    loading.value = false
  }
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
</script>

<template>
  <div class="space-y-6">
    <!-- Page header -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Dashboard</h1>
      <p class="mt-1 text-sm text-gray-500">Welcome back! Here's an overview of your business.</p>
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
                <p class="text-sm font-medium text-gray-500">Sales Today</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatCurrency(stats.totalSalesToday) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm">
              <ArrowTrendingUpIcon class="h-4 w-4 text-green-500 mr-1" />
              <span class="text-green-600 font-medium">{{ stats.salesCountToday }} transactions</span>
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
                <p class="text-sm font-medium text-gray-500">This Week</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatCurrency(stats.totalSalesThisWeek) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm text-gray-500">
              <span>Monthly: {{ formatCurrency(stats.totalSalesThisMonth) }}</span>
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
                <p class="text-sm font-medium text-gray-500">Products</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatNumber(stats.totalProducts) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm">
              <span class="text-green-600">{{ stats.activeProducts }} active</span>
              <span class="mx-2 text-gray-300">|</span>
              <span v-if="stats.lowStockProducts > 0" class="text-yellow-600">
                {{ stats.lowStockProducts }} low stock
              </span>
              <span v-else class="text-gray-500">Stock OK</span>
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
                <p class="text-sm font-medium text-gray-500">Users</p>
                <p class="text-2xl font-semibold text-gray-900">
                  {{ formatNumber(stats.totalUsers) }}
                </p>
              </div>
            </div>
            <div class="mt-4 flex items-center text-sm">
              <span class="text-green-600">{{ stats.activeUsers }} active</span>
              <span class="mx-2 text-gray-300">|</span>
              <span class="text-blue-600">+{{ stats.newUsersToday }} today</span>
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
              <h3 class="text-sm font-medium text-yellow-800">Inventory Alerts</h3>
              <div class="mt-2 text-sm text-yellow-700">
                <ul class="list-disc pl-5 space-y-1">
                  <li v-if="stats.lowStockProducts > 0">
                    {{ stats.lowStockProducts }} products are running low on stock
                  </li>
                  <li v-if="stats.outOfStockProducts > 0">
                    {{ stats.outOfStockProducts }} products are out of stock
                  </li>
                </ul>
              </div>
              <div class="mt-4">
                <RouterLink
                  to="/inventory/stock"
                  class="text-sm font-medium text-yellow-800 hover:text-yellow-900"
                >
                  View inventory &rarr;
                </RouterLink>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Two column layout -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Financial Summary -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium text-gray-900">Financial Summary</h3>
          </div>
          <div class="card-body">
            <div class="space-y-4">
              <div class="flex justify-between items-center p-4 bg-green-50 rounded-lg">
                <div>
                  <p class="text-sm text-gray-500">Receivables</p>
                  <p class="text-xl font-semibold text-green-700">
                    {{ formatCurrency(stats.totalReceivables) }}
                  </p>
                </div>
                <ArrowTrendingUpIcon class="h-8 w-8 text-green-400" />
              </div>
              <div class="flex justify-between items-center p-4 bg-red-50 rounded-lg">
                <div>
                  <p class="text-sm text-gray-500">Payables</p>
                  <p class="text-xl font-semibold text-red-700">
                    {{ formatCurrency(stats.totalPayables) }}
                  </p>
                </div>
                <ArrowTrendingDownIcon class="h-8 w-8 text-red-400" />
              </div>
              <div class="flex justify-between items-center p-4 bg-blue-50 rounded-lg">
                <div>
                  <p class="text-sm text-gray-500">Inventory Value</p>
                  <p class="text-xl font-semibold text-blue-700">
                    {{ formatCurrency(stats.totalInventoryValue) }}
                  </p>
                </div>
                <CubeIcon class="h-8 w-8 text-blue-400" />
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activity -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium text-gray-900">Recent Activity</h3>
          </div>
          <div class="card-body">
            <div v-if="recentActivities.length === 0" class="text-center py-8 text-gray-500">
              No recent activity
            </div>
            <ul v-else class="divide-y divide-gray-200 -my-4">
              <li
                v-for="(activity, index) in recentActivities.slice(0, 8)"
                :key="index"
                class="py-4"
              >
                <div class="flex items-start space-x-3">
                  <div class="flex-shrink-0">
                    <div class="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                      <span class="text-xs font-medium text-gray-600">
                        {{ activity.username?.charAt(0).toUpperCase() || '?' }}
                      </span>
                    </div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm text-gray-900">
                      {{ activity.description }}
                    </p>
                    <p class="text-xs text-gray-500 mt-1">
                      {{ activity.username }} &bull; {{ activity.timestamp }}
                    </p>
                  </div>
                  <span
                    :class="[
                      'badge',
                      activity.action === 'CREATE' ? 'badge-success' :
                      activity.action === 'UPDATE' ? 'badge-info' :
                      activity.action === 'DELETE' ? 'badge-danger' : 'badge-info'
                    ]"
                  >
                    {{ activity.action }}
                  </span>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Currency rates informer -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium text-gray-900">Valyuta kurslari (CBU)</h3>
        </div>
        <div class="card-body flex justify-center">
          <a href="https://cbu.uz/" target="_blank" title="O'zbekiston Respublikasi Markaziy banki">
            <img src="https://cbu.uz/uz/informer/?txtclr=212121&brdclr=FFC700&bgclr=FFE27D&r_choose=USD_EUR_RUB" alt="CBU valyuta kurslari" />
          </a>
        </div>
      </div>

      <!-- Quick actions -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium text-gray-900">Quick Actions</h3>
        </div>
        <div class="card-body">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <RouterLink
              to="/pos"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <ShoppingCartIcon class="h-8 w-8 text-primary-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">New Sale</span>
            </RouterLink>
            <RouterLink
              to="/inventory/products/new"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <CubeIcon class="h-8 w-8 text-purple-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">Add Product</span>
            </RouterLink>
            <RouterLink
              to="/customers/new"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <UsersIcon class="h-8 w-8 text-orange-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">Add Customer</span>
            </RouterLink>
            <RouterLink
              to="/purchases/orders/new"
              class="flex flex-col items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <CurrencyDollarIcon class="h-8 w-8 text-green-600 mb-2" />
              <span class="text-sm font-medium text-gray-900">Purchase Order</span>
            </RouterLink>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
