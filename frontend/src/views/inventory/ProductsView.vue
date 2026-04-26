<script setup>
import { ref, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { productsApi, categoriesApi, brandsApi } from '@/services/api'
import {
  PlusIcon, MagnifyingGlassIcon, PencilIcon, TrashIcon, FunnelIcon,
  ArrowDownTrayIcon, ArrowUpTrayIcon, DocumentArrowDownIcon
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

const productCount = ref({ total: 0, active: 0 })
const categories = ref([])
const brands = ref([])
const selectedCategory = ref(null)
const selectedBrand = ref(null)

const skuLookup = ref('')
const skuLookupResult = ref(null)
const skuLookupLoading = ref(false)
const skuLookupError = ref('')

const showDeleteModal = ref(false)
const productToDelete = ref(null)

const showImportModal = ref(false)
const importFile = ref(null)
const importFormat = ref('csv')
const importing = ref(false)
const importResult = ref(null)

async function fetchProducts() {
  loading.value = true
  try {
    let response
    if (selectedCategory.value) {
      response = await productsApi.getByCategory(selectedCategory.value, {
        page: pagination.value.page,
        size: pagination.value.size
      })
    } else if (selectedBrand.value) {
      response = await productsApi.getByBrand(selectedBrand.value, {
        page: pagination.value.page,
        size: pagination.value.size
      })
    } else {
      response = await productsApi.getAll({
        page: pagination.value.page,
        size: pagination.value.size,
        search: search.value || undefined
      })
    }
    products.value = response.data.content || []
    pagination.value.totalPages = response.data.page?.totalPages || 0
    pagination.value.totalElements = response.data.page?.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch products:', error)
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  try {
    const [countRes, catRes, brandRes] = await Promise.all([
      productsApi.getCount(),
      categoriesApi.getAll(),
      brandsApi.getAll()
    ])
    productCount.value = countRes.data || { total: 0, active: 0 }
    categories.value = catRes.data.data || catRes.data || []
    brands.value = brandRes.data.data || brandRes.data || []
  } catch (error) {
    console.error('Failed to load metadata:', error)
  }
}

onMounted(() => {
  fetchProducts()
  loadMeta()
})

watch(search, () => {
  pagination.value.page = 0
  selectedCategory.value = null
  selectedBrand.value = null
  fetchProducts()
})

function applyFilter() {
  pagination.value.page = 0
  fetchProducts()
}

function clearFilters() {
  selectedCategory.value = null
  selectedBrand.value = null
  search.value = ''
  pagination.value.page = 0
  fetchProducts()
}

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
    loadMeta()
  } catch (error) {
    console.error('Failed to delete product:', error)
  }
}

async function handleExport(format) {
  try {
    const res = await productsApi.exportProducts({
      format,
      categoryId: selectedCategory.value || undefined,
      brandId: selectedBrand.value || undefined
    })
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `products_export.${format === 'excel' ? 'xlsx' : 'csv'}`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Failed to export products:', error)
  }
}

async function handleDownloadTemplate() {
  try {
    const res = await productsApi.getImportTemplate(importFormat.value)
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `product_import_template.${importFormat.value === 'excel' ? 'xlsx' : 'csv'}`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Failed to download template:', error)
  }
}

async function handleImport() {
  if (!importFile.value) return
  importing.value = true
  importResult.value = null
  try {
    const res = await productsApi.importProducts(importFile.value, importFormat.value)
    importResult.value = res.data
    fetchProducts()
    loadMeta()
  } catch (error) {
    console.error('Failed to import products:', error)
    importResult.value = { error: error.response?.data?.message || t('inventory.products.importError') }
  } finally {
    importing.value = false
  }
}

function onFileSelected(event) {
  importFile.value = event.target.files[0] || null
}

async function lookupBySku() {
  if (!skuLookup.value.trim()) return
  skuLookupLoading.value = true
  skuLookupError.value = ''
  skuLookupResult.value = null
  try {
    const response = await productsApi.getBySku(skuLookup.value.trim())
    const product = response.data.data || response.data
    if (product && product.id) {
      skuLookupResult.value = product
      // Check if the product is already in the current list and scroll to it
      const idx = products.value.findIndex(p => p.id === product.id)
      if (idx >= 0) {
        // Highlight existing row
        const row = document.getElementById(`product-row-${product.id}`)
        if (row) {
          row.scrollIntoView({ behavior: 'smooth', block: 'center' })
          row.classList.add('ring-2', 'ring-primary-500', 'bg-primary-50')
          setTimeout(() => {
            row.classList.remove('ring-2', 'ring-primary-500', 'bg-primary-50')
          }, 3000)
        }
      }
    } else {
      skuLookupError.value = t('inventory.products.skuNotFound')
    }
  } catch (error) {
    skuLookupError.value = t('inventory.products.skuNotFound')
  } finally {
    skuLookupLoading.value = false
  }
}

function clearSkuLookup() {
  skuLookup.value = ''
  skuLookupResult.value = null
  skuLookupError.value = ''
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
      <div class="flex items-center gap-2">
        <button @click="showImportModal = true" class="btn-secondary">
          <ArrowUpTrayIcon class="h-5 w-5 mr-1" />
          {{ $t('inventory.products.import') }}
        </button>
        <div class="relative group">
          <button class="btn-secondary">
            <ArrowDownTrayIcon class="h-5 w-5 mr-1" />
            {{ $t('inventory.products.export') }}
          </button>
          <div class="hidden group-hover:block absolute right-0 mt-1 w-36 bg-white rounded-lg shadow-lg border border-gray-200 z-10">
            <button @click="handleExport('csv')" class="w-full text-left px-4 py-2 text-sm hover:bg-gray-50 rounded-t-lg">CSV</button>
            <button @click="handleExport('excel')" class="w-full text-left px-4 py-2 text-sm hover:bg-gray-50 rounded-b-lg">Excel</button>
          </div>
        </div>
        <RouterLink to="/inventory/products/new" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('inventory.products.addProduct') }}
        </RouterLink>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 gap-4">
      <div class="card p-4">
        <p class="text-sm text-gray-500">{{ $t('inventory.products.totalProducts') }}</p>
        <p class="text-2xl font-bold text-gray-900">{{ productCount.total }}</p>
      </div>
      <div class="card p-4">
        <p class="text-sm text-gray-500">{{ $t('inventory.products.activeProducts') }}</p>
        <p class="text-2xl font-bold text-green-600">{{ productCount.active }}</p>
      </div>
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
          <select v-model="selectedCategory" @change="applyFilter" class="input w-auto">
            <option :value="null">{{ $t('inventory.products.allCategories') }}</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
          <select v-model="selectedBrand" @change="applyFilter" class="input w-auto">
            <option :value="null">{{ $t('inventory.products.allBrands') }}</option>
            <option v-for="brand in brands" :key="brand.id" :value="brand.id">{{ brand.name }}</option>
          </select>
          <button v-if="selectedCategory || selectedBrand" @click="clearFilters" class="btn-secondary">
            {{ $t('inventory.products.clearFilters') }}
          </button>
        </div>
      </div>
    </div>

    <!-- SKU Lookup -->
    <div class="card">
      <div class="card-body">
        <div class="flex flex-col sm:flex-row gap-4 items-start">
          <div class="flex-1">
            <label class="label text-sm font-medium text-gray-700 mb-1">{{ $t('inventory.products.skuLookup') }}</label>
            <div class="flex gap-2">
              <input
                v-model="skuLookup"
                type="text"
                :placeholder="$t('inventory.products.skuLookupPlaceholder')"
                class="input"
                @keyup.enter="lookupBySku"
              />
              <button @click="lookupBySku" :disabled="skuLookupLoading || !skuLookup.trim()" class="btn-primary whitespace-nowrap">
                {{ skuLookupLoading ? $t('searching') : $t('inventory.products.skuLookupBtn') }}
              </button>
              <button v-if="skuLookup || skuLookupResult || skuLookupError" @click="clearSkuLookup" class="btn-secondary">
                {{ $t('inventory.products.clearFilters') }}
              </button>
            </div>
          </div>
        </div>
        <!-- SKU Lookup Result -->
        <div v-if="skuLookupResult" class="mt-3 p-3 bg-green-50 border border-green-200 rounded-lg">
          <p class="text-sm font-medium text-green-800">
            {{ $t('inventory.products.skuFound') }}:
            <span class="font-bold">{{ skuLookupResult.name }}</span>
            <span class="font-mono text-xs ml-2">({{ skuLookupResult.sku }})</span>
          </p>
          <div class="mt-1 flex gap-4 text-xs text-green-700">
            <span>{{ $t('price') }}: {{ formatCurrency(skuLookupResult.sellingPrice) }}</span>
            <span>{{ $t('inventory.products.stockQty') }}: {{ skuLookupResult.stockQuantity || 0 }}</span>
            <RouterLink :to="`/inventory/products/${skuLookupResult.id}/edit`" class="text-primary-600 hover:underline">
              {{ $t('inventory.brands.editBrand') }}
            </RouterLink>
          </div>
        </div>
        <div v-if="skuLookupError" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-700">{{ skuLookupError }}</p>
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
            <tr v-for="product in products" :key="product.id" :id="`product-row-${product.id}`" class="transition-all duration-300">
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
    <Teleport to="body">
      <div v-if="showDeleteModal" class="fixed inset-0 z-50 overflow-y-auto">
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
    </Teleport>

    <!-- Import Modal -->
    <Teleport to="body">
      <div v-if="showImportModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showImportModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-md w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('inventory.products.importTitle') }}</h3>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('inventory.products.importFormat') }}</label>
                <select v-model="importFormat" class="input">
                  <option value="csv">CSV</option>
                  <option value="excel">Excel (XLSX)</option>
                </select>
              </div>

              <div>
                <label class="label">{{ $t('inventory.products.importFile') }}</label>
                <input type="file" @change="onFileSelected" :accept="importFormat === 'csv' ? '.csv' : '.xlsx'" class="input" />
              </div>

              <button @click="handleDownloadTemplate" class="text-sm text-primary-600 hover:text-primary-800 flex items-center gap-1">
                <DocumentArrowDownIcon class="h-4 w-4" />
                {{ $t('inventory.products.downloadTemplate') }}
              </button>

              <div v-if="importResult" class="p-3 rounded-lg text-sm" :class="importResult.error ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'">
                <template v-if="importResult.error">{{ importResult.error }}</template>
                <template v-else>
                  {{ $t('inventory.products.importSuccess', { created: importResult.created || 0, updated: importResult.updated || 0, failed: importResult.failed || 0 }) }}
                </template>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showImportModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="handleImport" :disabled="!importFile || importing" class="btn-primary">
                {{ importing ? $t('saving') : $t('inventory.products.importBtn') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
