<script setup>
import { ref, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { productsApi } from '@/services/api'
import {
  PlusIcon,
  MagnifyingGlassIcon,
  PencilIcon,
  TrashIcon,
  FunnelIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const products = ref([])
const loading = ref(true)
const search = ref('')
const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

const showDeleteModal = ref(false)
const productToDelete = ref(null)

async function fetchProducts() {
  loading.value = true
  try {
    const response = await productsApi.getAll({
      page: pagination.value.page,
      size: pagination.value.size,
      search: search.value || undefined
    })
    products.value = response.data.content || []
    pagination.value.totalPages = response.data.page?.totalPages || 0
    pagination.value.totalElements = response.data.page?.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch products:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchProducts)

watch(search, () => {
  pagination.value.page = 0
  fetchProducts()
})

function changePage(newPage) {
  pagination.value.page = newPage
  fetchProducts()
}

function confirmDelete(product) {
  productToDelete.value = product
  showDeleteModal.value = true
}

async function deleteProduct() {
  if (!productToDelete.value) return

  try {
    await productsApi.delete(productToDelete.value.id)
    showDeleteModal.value = false
    productToDelete.value = null
    fetchProducts()
  } catch (error) {
    console.error('Failed to delete product:', error)
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value || 0)
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.products.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.products.subtitle') }}</p>
      </div>
      <RouterLink to="/inventory/products/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.products.addProduct') }}
      </RouterLink>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body">
        <div class="flex flex-col sm:flex-row gap-4">
          <div class="flex-1 relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="search"
              type="text"
              :placeholder="$t('inventory.products.searchPlaceholder')"
              class="input pl-10"
            />
          </div>
          <button class="btn-secondary">
            <FunnelIcon class="h-5 w-5 mr-2" />
            {{ $t('search') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Products table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="products.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.products.noProducts') }}</p>
        <RouterLink to="/inventory/products/new" class="btn-primary mt-4 inline-flex">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('inventory.products.addFirst') }}
        </RouterLink>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('inventory.products.product') }}</th>
              <th>{{ $t('inventory.products.sku') }}</th>
              <th>{{ $t('inventory.products.category') }}</th>
              <th>{{ $t('price') }}</th>
              <th>{{ $t('inventory.products.stockQty') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="product in products" :key="product.id">
              <td>
                <div class="flex items-center">
                  <div class="h-10 w-10 flex-shrink-0 bg-gray-100 rounded-lg flex items-center justify-center">
                    <img
                      v-if="product.primaryImageUrl"
                      :src="product.primaryImageUrl"
                      :alt="product.name"
                      class="h-10 w-10 rounded-lg object-cover"
                    />
                    <span v-else class="text-gray-400 text-xs">{{ $t('inventory.products.noImage') }}</span>
                  </div>
                  <div class="ml-4">
                    <div class="font-medium text-gray-900">{{ product.name }}</div>
                    <div class="text-sm text-gray-500">{{ product.barcode || $t('inventory.products.noBarcode') }}</div>
                  </div>
                </div>
              </td>
              <td class="font-mono text-sm">{{ product.sku }}</td>
              <td>{{ product.category?.name || '-' }}</td>
              <td>
                <div class="font-medium">{{ formatCurrency(product.sellingPrice) }}</div>
                <div class="text-xs text-gray-500">{{ $t('inventory.products.cost') }}: {{ formatCurrency(product.costPrice) }}</div>
              </td>
              <td>
                <span
                  :class="[
                    'font-medium',
                    product.stockQuantity <= 0 ? 'text-red-600' :
                    product.stockQuantity <= product.minStockLevel ? 'text-yellow-600' : 'text-green-600'
                  ]"
                >
                  {{ product.stockQuantity || 0 }}
                </span>
              </td>
              <td>
                <span
                  :class="[
                    'badge',
                    product.active ? 'badge-success' : 'badge-danger'
                  ]"
                >
                  {{ product.active ? $t('active') : $t('inactive') }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <RouterLink
                    :to="`/inventory/products/${product.id}/edit`"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                  >
                    <PencilIcon class="h-5 w-5" />
                  </RouterLink>
                  <button
                    @click="confirmDelete(product)"
                    class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                  >
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <p class="text-sm text-gray-500">
            {{ $t('inventory.products.showingProducts', {
              from: pagination.page * pagination.size + 1,
              to: Math.min((pagination.page + 1) * pagination.size, pagination.totalElements),
              total: pagination.totalElements
            }) }}
          </p>
          <div class="flex space-x-2">
            <button
              @click="changePage(pagination.page - 1)"
              :disabled="pagination.page === 0"
              class="btn-secondary px-3 py-1"
            >
              {{ $t('previous') }}
            </button>
            <button
              @click="changePage(pagination.page + 1)"
              :disabled="pagination.page >= pagination.totalPages - 1"
              class="btn-secondary px-3 py-1"
            >
              {{ $t('next') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Modal -->
    <div
      v-if="showDeleteModal"
      class="fixed inset-0 z-50 overflow-y-auto"
    >
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDeleteModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('inventory.products.deleteProduct') }}</h3>
          <p class="text-gray-500 mb-6">
            {{ $t('inventory.products.deleteConfirm', { name: productToDelete?.name }) }}
          </p>
          <div class="flex justify-end space-x-3">
            <button @click="showDeleteModal = false" class="btn-secondary">
              {{ $t('cancel') }}
            </button>
            <button @click="deleteProduct" class="btn-danger">
              {{ $t('delete') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
