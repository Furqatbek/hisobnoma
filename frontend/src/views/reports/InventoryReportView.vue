<script setup>
import { ref, onMounted } from 'vue'
import { reportsApi } from '@/services/api'
import { ArrowDownTrayIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

const loading = ref(true)
const report = ref(null)
const filter = ref('all') // all, low, out

async function fetchReport() {
  loading.value = true
  try {
    const response = await reportsApi.getStockOnHand({})
    report.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch report:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)

function filteredProducts() {
  if (!report.value?.products) return []
  if (filter.value === 'low') return report.value.products.filter(p => p.status === 'LOW_STOCK')
  if (filter.value === 'out') return report.value.products.filter(p => p.status === 'OUT_OF_STOCK')
  return report.value.products
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}

function getStatusClass(status) {
  if (status === 'OUT_OF_STOCK') return 'badge-danger'
  if (status === 'LOW_STOCK') return 'badge-warning'
  return 'badge-success'
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Inventory Report</h1>
        <p class="mt-1 text-sm text-gray-500">Stock levels and valuation</p>
      </div>
      <button class="btn-secondary">
        <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
        Export
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="report">
      <!-- Summary -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <button
          @click="filter = 'all'"
          :class="['card cursor-pointer transition-all text-left', filter === 'all' ? 'ring-2 ring-primary-500' : '']"
        >
          <div class="card-body">
            <p class="text-sm text-gray-500">Total Products</p>
            <p class="text-2xl font-bold">{{ report.totalProducts || 0 }}</p>
          </div>
        </button>
        <button
          @click="filter = 'low'"
          :class="['card cursor-pointer transition-all text-left', filter === 'low' ? 'ring-2 ring-yellow-500' : '']"
        >
          <div class="card-body">
            <p class="text-sm text-gray-500">Low Stock</p>
            <p class="text-2xl font-bold text-yellow-600">{{ report.lowStockCount || 0 }}</p>
          </div>
        </button>
        <button
          @click="filter = 'out'"
          :class="['card cursor-pointer transition-all text-left', filter === 'out' ? 'ring-2 ring-red-500' : '']"
        >
          <div class="card-body">
            <p class="text-sm text-gray-500">Out of Stock</p>
            <p class="text-2xl font-bold text-red-600">{{ report.outOfStockCount || 0 }}</p>
          </div>
        </button>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Total Value</p>
            <p class="text-2xl font-bold text-primary-600">{{ formatCurrency(report.totalValue) }}</p>
          </div>
        </div>
      </div>

      <!-- Products Table -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">
            {{ filter === 'all' ? 'All Products' : filter === 'low' ? 'Low Stock Products' : 'Out of Stock Products' }}
          </h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Product</th>
                <th>SKU</th>
                <th class="text-right">Stock</th>
                <th class="text-right">Min Level</th>
                <th class="text-right">Value</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="product in filteredProducts()" :key="product.id">
                <td>
                  <div class="flex items-center">
                    <ExclamationTriangleIcon
                      v-if="product.status !== 'IN_STOCK'"
                      class="h-5 w-5 text-yellow-500 mr-2"
                    />
                    <span class="font-medium">{{ product.name }}</span>
                  </div>
                </td>
                <td class="font-mono text-sm text-gray-500">{{ product.sku }}</td>
                <td class="text-right font-medium">{{ product.quantity }}</td>
                <td class="text-right text-gray-500">{{ product.minStockLevel }}</td>
                <td class="text-right">{{ formatCurrency(product.value) }}</td>
                <td>
                  <span :class="['badge', getStatusClass(product.status)]">
                    {{ product.status?.replace('_', ' ') }}
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
