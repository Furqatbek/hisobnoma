<script setup>
import { ref, onMounted } from 'vue'
import { productsApi } from '@/services/api'
import { MagnifyingGlassIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

const products = ref([])
const loading = ref(true)
const search = ref('')
const filter = ref('all') // all, low, out

async function fetchProducts() {
  loading.value = true
  try {
    const response = await productsApi.getAll({ size: 100, search: search.value || undefined })
    products.value = response.data.content || []
  } catch (error) {
    console.error('Failed to fetch products:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchProducts)

function filteredProducts() {
  let result = products.value
  if (filter.value === 'low') {
    result = result.filter(p => p.stockQuantity > 0 && p.stockQuantity <= (p.minStockLevel || 10))
  } else if (filter.value === 'out') {
    result = result.filter(p => p.stockQuantity <= 0)
  }
  return result
}

function getStockStatus(product) {
  if (product.stockQuantity <= 0) return { label: 'Out of Stock', class: 'badge-danger' }
  if (product.stockQuantity <= (product.minStockLevel || 10)) return { label: 'Low Stock', class: 'badge-warning' }
  return { label: 'In Stock', class: 'badge-success' }
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Stock Overview</h1>
      <p class="mt-1 text-sm text-gray-500">Monitor inventory levels</p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <button
        @click="filter = 'all'"
        :class="['card cursor-pointer transition-all', filter === 'all' ? 'ring-2 ring-primary-500' : '']"
      >
        <div class="card-body text-center">
          <p class="text-3xl font-bold text-gray-900">{{ products.length }}</p>
          <p class="text-sm text-gray-500">Total Products</p>
        </div>
      </button>
      <button
        @click="filter = 'low'"
        :class="['card cursor-pointer transition-all', filter === 'low' ? 'ring-2 ring-yellow-500' : '']"
      >
        <div class="card-body text-center">
          <p class="text-3xl font-bold text-yellow-600">
            {{ products.filter(p => p.stockQuantity > 0 && p.stockQuantity <= (p.minStockLevel || 10)).length }}
          </p>
          <p class="text-sm text-gray-500">Low Stock</p>
        </div>
      </button>
      <button
        @click="filter = 'out'"
        :class="['card cursor-pointer transition-all', filter === 'out' ? 'ring-2 ring-red-500' : '']"
      >
        <div class="card-body text-center">
          <p class="text-3xl font-bold text-red-600">
            {{ products.filter(p => p.stockQuantity <= 0).length }}
          </p>
          <p class="text-sm text-gray-500">Out of Stock</p>
        </div>
      </button>
    </div>

    <!-- Search -->
    <div class="card">
      <div class="card-body">
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @input="fetchProducts"
            type="text"
            placeholder="Search products..."
            class="input pl-10"
          />
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
              <th>Product</th>
              <th>SKU</th>
              <th class="text-right">Stock</th>
              <th class="text-right">Min Level</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="product in filteredProducts()" :key="product.id">
              <td>
                <div class="flex items-center">
                  <ExclamationTriangleIcon
                    v-if="product.stockQuantity <= (product.minStockLevel || 10)"
                    class="h-5 w-5 text-yellow-500 mr-2"
                  />
                  <span class="font-medium">{{ product.name }}</span>
                </div>
              </td>
              <td class="font-mono text-sm text-gray-500">{{ product.sku }}</td>
              <td class="text-right font-medium">{{ product.stockQuantity || 0 }}</td>
              <td class="text-right text-gray-500">{{ product.minStockLevel || 10 }}</td>
              <td>
                <span :class="['badge', getStockStatus(product).class]">
                  {{ getStockStatus(product).label }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
