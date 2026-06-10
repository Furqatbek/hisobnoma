<script setup>
import { ref, computed, onMounted } from 'vue'
import { webCatalogApi, productsApi } from '@/services/api'
import {
  PlusIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  ChevronUpIcon,
  ChevronDownIcon,
  EyeIcon,
  EyeSlashIcon,
  GlobeAltIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const items = ref([])
const counts = ref({ live: 0, draft: 0, total: 0 })
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(0)
const totalPages = ref(0)

// Add-products modal state
const showAddModal = ref(false)
const productSearch = ref('')
const productResults = ref([])
const selectedProductIds = ref([])
const searchingProducts = ref(false)

async function fetchItems(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 50 }
    if (searchQuery.value.trim()) params.search = searchQuery.value.trim()
    const response = await webCatalogApi.getAll(params)
    const data = response.data
    items.value = (data.content || []).map(item => ({ ...item, _dirty: false }))
    currentPage.value = data.page?.number || 0
    totalPages.value = data.page?.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch web catalog:', error)
  } finally {
    loading.value = false
  }
}

async function fetchCounts() {
  try {
    const response = await webCatalogApi.getCounts()
    counts.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch catalog counts:', error)
  }
}

function refresh() {
  fetchItems(currentPage.value)
  fetchCounts()
}

function handleSearch() {
  fetchItems(0)
}

function formatPrice(value) {
  if (value == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(value)
}

// ---- Row actions ----

function markDirty(item) {
  item._dirty = true
}

async function saveItem(item) {
  if (!item._dirty) return
  try {
    await webCatalogApi.update(item.id, {
      displayName: item.displayName || null,
      priceOverride: item.priceOverride !== '' && item.priceOverride != null
        ? item.priceOverride
        : null
    })
    item._dirty = false
    refresh()
  } catch (error) {
    alert(error.response?.data?.message || t('webCatalog.saveError'))
  }
}

async function togglePublish(item) {
  try {
    if (item.status === 'LIVE') {
      await webCatalogApi.unpublish(item.id)
    } else {
      await webCatalogApi.publish(item.id)
    }
    refresh()
  } catch (error) {
    alert(error.response?.data?.message || t('webCatalog.saveError'))
  }
}

async function moveUp(item) {
  await webCatalogApi.moveUp(item.id)
  fetchItems(currentPage.value)
}

async function moveDown(item) {
  await webCatalogApi.moveDown(item.id)
  fetchItems(currentPage.value)
}

async function removeItem(item) {
  const name = item.displayName || item.productName
  if (!confirm(t('webCatalog.confirmRemove', { name }))) return
  try {
    await webCatalogApi.remove(item.id)
    refresh()
  } catch (error) {
    alert(error.response?.data?.message || t('webCatalog.saveError'))
  }
}

async function publishAll() {
  await webCatalogApi.publishAll()
  refresh()
}

async function unpublishAll() {
  await webCatalogApi.unpublishAll()
  refresh()
}

// ---- Add products modal ----

const existingProductIds = computed(() => new Set(items.value.map(i => i.productId)))

function openAddModal() {
  showAddModal.value = true
  productSearch.value = ''
  productResults.value = []
  selectedProductIds.value = []
}

let searchTimeout = null
function handleProductSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(searchProducts, 300)
}

async function searchProducts() {
  const query = productSearch.value.trim()
  if (!query) {
    productResults.value = []
    return
  }
  searchingProducts.value = true
  try {
    const response = await productsApi.search(query)
    const data = response.data
    productResults.value = data.data || data.content || data || []
  } catch (error) {
    console.error('Product search failed:', error)
    productResults.value = []
  } finally {
    searchingProducts.value = false
  }
}

function toggleProductSelection(product) {
  if (existingProductIds.value.has(product.id)) return
  const idx = selectedProductIds.value.indexOf(product.id)
  if (idx >= 0) {
    selectedProductIds.value.splice(idx, 1)
  } else {
    selectedProductIds.value.push(product.id)
  }
}

async function addSelectedProducts() {
  if (selectedProductIds.value.length === 0) return
  try {
    await webCatalogApi.addProducts(selectedProductIds.value)
    showAddModal.value = false
    refresh()
  } catch (error) {
    alert(error.response?.data?.message || t('webCatalog.saveError'))
  }
}

onMounted(() => {
  fetchItems()
  fetchCounts()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <GlobeAltIcon class="h-7 w-7 text-teal-600" />
          {{ $t('webCatalog.title') }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('webCatalog.subtitle') }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <button class="btn-secondary text-sm" @click="unpublishAll">
          <EyeSlashIcon class="h-4 w-4 mr-1 inline" />
          {{ $t('webCatalog.unpublishAll') }}
        </button>
        <button class="btn-secondary text-sm" @click="publishAll">
          <EyeIcon class="h-4 w-4 mr-1 inline" />
          {{ $t('webCatalog.publishAll') }}
        </button>
        <button class="btn-primary" @click="openAddModal">
          <PlusIcon class="h-5 w-5 mr-1 inline" />
          {{ $t('webCatalog.addProducts') }}
        </button>
      </div>
    </div>

    <!-- Counts + search -->
    <div class="flex flex-col sm:flex-row sm:items-center gap-4">
      <div class="flex items-center gap-3 text-sm">
        <span class="inline-flex items-center px-2.5 py-1 rounded-full bg-green-100 text-green-800 font-medium">
          {{ $t('webCatalog.live') }}: {{ counts.live }}
        </span>
        <span class="inline-flex items-center px-2.5 py-1 rounded-full bg-gray-100 text-gray-700 font-medium">
          {{ $t('webCatalog.draft') }}: {{ counts.draft }}
        </span>
      </div>
      <div class="relative flex-1 max-w-md">
        <MagnifyingGlassIcon class="h-5 w-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          v-model="searchQuery"
          @input="handleSearch"
          type="text"
          :placeholder="$t('webCatalog.searchPlaceholder')"
          class="input pl-10"
        />
      </div>
    </div>

    <!-- Items table -->
    <div class="card !overflow-visible">
      <div v-if="loading" class="p-8 text-center text-gray-500">{{ $t('loading') }}</div>
      <div v-else-if="items.length === 0" class="p-8 text-center text-gray-500">
        {{ $t('webCatalog.empty') }}
      </div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase w-20"></th>
              <th class="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCatalog.product') }}</th>
              <th class="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCatalog.displayName') }}</th>
              <th class="px-3 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('webCatalog.basePrice') }}</th>
              <th class="px-3 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('webCatalog.priceOverride') }}</th>
              <th class="px-3 py-3 text-center text-xs font-medium text-gray-500 uppercase">{{ $t('webCatalog.status') }}</th>
              <th class="px-3 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200 bg-white">
            <tr v-for="(item, index) in items" :key="item.id" :class="{ 'opacity-60': !item.productActive || !item.productSellable }">
              <!-- Reorder -->
              <td class="px-3 py-2 whitespace-nowrap">
                <div class="flex items-center gap-1">
                  <button
                    class="p-1 text-gray-400 hover:text-gray-700 disabled:opacity-30"
                    :disabled="index === 0 && currentPage === 0"
                    :title="$t('webCatalog.moveUp')"
                    @click="moveUp(item)"
                  >
                    <ChevronUpIcon class="h-4 w-4" />
                  </button>
                  <button
                    class="p-1 text-gray-400 hover:text-gray-700 disabled:opacity-30"
                    :disabled="index === items.length - 1 && currentPage === totalPages - 1"
                    :title="$t('webCatalog.moveDown')"
                    @click="moveDown(item)"
                  >
                    <ChevronDownIcon class="h-4 w-4" />
                  </button>
                </div>
              </td>
              <!-- Product -->
              <td class="px-3 py-2">
                <div class="flex items-center gap-3">
                  <img
                    v-if="item.imageUrl"
                    :src="item.imageUrl"
                    class="h-10 w-10 rounded object-cover flex-shrink-0"
                    alt=""
                  />
                  <div v-else class="h-10 w-10 rounded bg-gray-100 flex-shrink-0"></div>
                  <div>
                    <div class="font-medium text-gray-900">{{ item.productName }}</div>
                    <div class="text-xs text-gray-500">
                      {{ item.sku }}
                      <span v-if="item.categoryName"> · {{ item.categoryName }}</span>
                      <span v-if="!item.productActive || !item.productSellable" class="text-red-500">
                        · {{ $t('webCatalog.inactiveProduct') }}
                      </span>
                    </div>
                  </div>
                </div>
              </td>
              <!-- Display name override -->
              <td class="px-3 py-2">
                <input
                  v-model="item.displayName"
                  @input="markDirty(item)"
                  @blur="saveItem(item)"
                  @keyup.enter="saveItem(item)"
                  type="text"
                  :placeholder="item.productName"
                  class="input text-sm py-1"
                />
              </td>
              <!-- Base price (+ current sale price from WEB promotions) -->
              <td class="px-3 py-2 text-right text-sm text-gray-500 whitespace-nowrap">
                {{ formatPrice(item.basePrice) }}
                <div v-if="item.salePrice != null" class="text-xs text-green-600 font-medium">
                  {{ formatPrice(item.salePrice) }}
                  <span class="ml-1 px-1 py-0.5 bg-green-100 rounded">{{ item.promotionLabel }}</span>
                </div>
              </td>
              <!-- Price override -->
              <td class="px-3 py-2 text-right">
                <input
                  v-model="item.priceOverride"
                  @input="markDirty(item)"
                  @blur="saveItem(item)"
                  @keyup.enter="saveItem(item)"
                  type="number"
                  min="0"
                  :placeholder="formatPrice(item.basePrice)"
                  class="input text-sm py-1 text-right w-32"
                />
              </td>
              <!-- Status -->
              <td class="px-3 py-2 text-center whitespace-nowrap">
                <button
                  @click="togglePublish(item)"
                  :class="item.status === 'LIVE'
                    ? 'bg-green-100 text-green-800 hover:bg-green-200'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium transition-colors"
                  :title="item.status === 'LIVE' ? $t('webCatalog.unpublish') : $t('webCatalog.publish')"
                >
                  <component :is="item.status === 'LIVE' ? EyeIcon : EyeSlashIcon" class="h-3.5 w-3.5 mr-1" />
                  {{ item.status === 'LIVE' ? $t('webCatalog.live') : $t('webCatalog.draft') }}
                </button>
              </td>
              <!-- Actions -->
              <td class="px-3 py-2 text-right whitespace-nowrap">
                <button
                  class="p-1.5 text-gray-400 hover:text-red-600"
                  :title="$t('webCatalog.remove')"
                  @click="removeItem(item)"
                >
                  <TrashIcon class="h-4 w-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
        <button
          class="btn-secondary text-sm py-1 px-3"
          :disabled="currentPage === 0"
          @click="fetchItems(currentPage - 1)"
        >
          {{ $t('previous') }}
        </button>
        <span class="text-sm text-gray-500">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button
          class="btn-secondary text-sm py-1 px-3"
          :disabled="currentPage >= totalPages - 1"
          @click="fetchItems(currentPage + 1)"
        >
          {{ $t('next') }}
        </button>
      </div>
    </div>

    <!-- Add products modal -->
    <div v-if="showAddModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="showAddModal = false"></div>
      <div class="relative bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">{{ $t('webCatalog.selectProducts') }}</h3>
        </div>
        <div class="px-6 py-4 flex-1 overflow-y-auto space-y-4">
          <div class="relative">
            <MagnifyingGlassIcon class="h-5 w-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              v-model="productSearch"
              @input="handleProductSearch"
              type="text"
              :placeholder="$t('webCatalog.productSearchPlaceholder')"
              class="input pl-10"
              autofocus
            />
          </div>
          <div v-if="searchingProducts" class="text-center text-sm text-gray-500 py-4">
            {{ $t('searching') }}
          </div>
          <div v-else-if="productResults.length === 0 && productSearch.trim()" class="text-center text-sm text-gray-500 py-4">
            {{ $t('webCatalog.noResults') }}
          </div>
          <ul v-else class="divide-y divide-gray-100">
            <li
              v-for="product in productResults"
              :key="product.id"
              class="py-2 px-2 flex items-center justify-between rounded"
              :class="existingProductIds.has(product.id)
                ? 'opacity-50'
                : 'cursor-pointer hover:bg-gray-50'"
              @click="toggleProductSelection(product)"
            >
              <div class="flex items-center gap-3">
                <input
                  type="checkbox"
                  :checked="selectedProductIds.includes(product.id)"
                  :disabled="existingProductIds.has(product.id)"
                  class="h-4 w-4 text-primary-600 rounded border-gray-300"
                  @click.stop="toggleProductSelection(product)"
                />
                <div>
                  <div class="text-sm font-medium text-gray-900">{{ product.name }}</div>
                  <div class="text-xs text-gray-500">{{ product.sku }} · {{ formatPrice(product.sellingPrice) }}</div>
                </div>
              </div>
              <span
                v-if="existingProductIds.has(product.id)"
                class="text-xs text-gray-400"
              >
                {{ $t('webCatalog.alreadyInCatalog') }}
              </span>
            </li>
          </ul>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-2">
          <button class="btn-secondary" @click="showAddModal = false">{{ $t('cancel') }}</button>
          <button
            class="btn-primary"
            :disabled="selectedProductIds.length === 0"
            @click="addSelectedProducts"
          >
            {{ $t('webCatalog.addSelected', { count: selectedProductIds.length }) }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
