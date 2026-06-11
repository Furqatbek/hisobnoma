<script setup>
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapData, unwrapList, warehousesApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, BuildingStorefrontIcon,
  MagnifyingGlassIcon, ChevronDownIcon, ChevronRightIcon,
  ChevronLeftIcon, StarIcon, ListBulletIcon, QueueListIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const warehouses = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingWarehouse = ref(null)

// Search & code lookup
const searchQuery = ref('')
const codeLookupQuery = ref('')
const codeLookupResult = ref(null)
const codeLookupError = ref(false)
const searchLoading = ref(false)

// View mode: 'flat' or 'tree'
const viewMode = ref('flat')
const treeData = ref([])
const treeLoading = ref(false)

// Type filter
const activeTypeFilter = ref('ALL')
const typeFilterOptions = computed(() => [
  { value: 'ALL', label: t('inventory.warehouses.allTypes') },
  { value: 'WAREHOUSE_STORE', label: t('inventory.warehouses.warehousesAndStores') },
  { value: 'WAREHOUSE', label: t('inventory.warehouses.locationTypeWarehouse') },
  { value: 'STORE', label: t('inventory.warehouses.locationTypeStore') },
  { value: 'VIRTUAL', label: t('inventory.warehouses.locationTypeVirtual') },
  { value: 'ZONE', label: t('inventory.warehouses.locationTypeZone') },
  { value: 'BIN', label: t('inventory.warehouses.locationTypeBin') }
])

// Default warehouse
const defaultWarehouse = ref(null)

// Pagination
const pagination = reactive({
  page: 0,
  size: 12,
  totalPages: 0,
  totalElements: 0
})

// Expanded warehouse details
const expandedWarehouseId = ref(null)
const expandedDetails = ref(null)
const expandedChildren = ref([])
const expandedLoading = ref(false)

// Tree expanded nodes
const treeExpandedIds = ref(new Set())
const treeChildrenMap = ref({})
const treeChildrenLoading = ref({})

const form = reactive({
  name: '',
  code: '',
  locationType: 'WAREHOUSE',
  address: '',
  phone: '',
  isDefault: false,
  active: true
})

const locationTypes = computed(() => [
  { value: 'WAREHOUSE', label: t('inventory.warehouses.locationTypeWarehouse') },
  { value: 'STORE', label: t('inventory.warehouses.locationTypeStore') },
  { value: 'VIRTUAL', label: t('inventory.warehouses.locationTypeVirtual') },
  { value: 'ZONE', label: t('inventory.warehouses.locationTypeZone') },
  { value: 'BIN', label: t('inventory.warehouses.locationTypeBin') }
])

const errors = reactive({})

// --- Fetch: paginated list (getPaginated) ---
async function fetchWarehouses() {
  loading.value = true
  try {
    const response = await warehousesApi.getPaginated({
      page: pagination.page,
      size: pagination.size
    })
    const data = unwrapData(response)
    warehouses.value = data.content || data || []
    pagination.totalPages = data.totalPages || 1
    pagination.totalElements = data.totalElements || warehouses.value.length
  } catch (error) {
    console.error('Failed to fetch warehouses:', error)
    // Fallback to getAll
    try {
      const response = await warehousesApi.getAll()
      warehouses.value = unwrapList(response)
      pagination.totalPages = 1
      pagination.totalElements = warehouses.value.length
    } catch (e) {
      console.error('Fallback getAll also failed:', e)
    }
  } finally {
    loading.value = false
  }
}

// --- Fetch default warehouse (getDefault) ---
async function fetchDefaultWarehouse() {
  try {
    const response = await warehousesApi.getDefault()
    const data = unwrapData(response)
    defaultWarehouse.value = data || null
  } catch (error) {
    console.error('Failed to fetch default warehouse:', error)
  }
}

// --- Search (search) ---
let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => performSearch(), 400)
}

async function performSearch() {
  const query = searchQuery.value.trim()
  if (!query) {
    fetchWarehouses()
    return
  }
  searchLoading.value = true
  loading.value = true
  try {
    const response = await warehousesApi.search(query)
    const data = unwrapData(response)
    warehouses.value = data.content || data || []
    pagination.totalPages = data.totalPages || 1
    pagination.totalElements = data.totalElements || warehouses.value.length
    pagination.page = 0
  } catch (error) {
    console.error('Failed to search warehouses:', error)
  } finally {
    searchLoading.value = false
    loading.value = false
  }
}

// --- Code lookup (getByCode) ---
async function lookupByCode() {
  const code = codeLookupQuery.value.trim()
  if (!code) {
    codeLookupResult.value = null
    codeLookupError.value = false
    return
  }
  try {
    const response = await warehousesApi.getByCode(code)
    const data = unwrapData(response)
    codeLookupResult.value = data || null
    codeLookupError.value = !data
  } catch (error) {
    codeLookupResult.value = null
    codeLookupError.value = true
  }
}

function clearCodeLookup() {
  codeLookupQuery.value = ''
  codeLookupResult.value = null
  codeLookupError.value = false
}

// --- Type filter (getByType, getWarehouseAndStore) ---
async function applyTypeFilter(type) {
  activeTypeFilter.value = type
  loading.value = true
  searchQuery.value = ''
  pagination.page = 0
  try {
    let response
    if (type === 'ALL') {
      response = await warehousesApi.getPaginated({ page: 0, size: pagination.size })
    } else if (type === 'WAREHOUSE_STORE') {
      response = await warehousesApi.getWarehouseAndStore()
    } else {
      response = await warehousesApi.getByType(type)
    }
    const data = unwrapData(response)
    warehouses.value = data.content || data || []
    pagination.totalPages = data.totalPages || 1
    pagination.totalElements = data.totalElements || warehouses.value.length
  } catch (error) {
    console.error('Failed to filter by type:', error)
  } finally {
    loading.value = false
  }
}

// --- Tree view (getTree, getRoots, getChildren) ---
async function fetchTree() {
  treeLoading.value = true
  try {
    const response = await warehousesApi.getTree()
    const data = unwrapData(response)
    treeData.value = data || []
  } catch (error) {
    console.error('Failed to fetch tree:', error)
    // Fallback to roots
    try {
      const response = await warehousesApi.getRoots()
      const data = unwrapData(response)
      treeData.value = (data.content || data || []).map(item => ({ ...item, children: [] }))
    } catch (e) {
      console.error('Fallback getRoots also failed:', e)
    }
  } finally {
    treeLoading.value = false
  }
}

async function toggleTreeNode(nodeId) {
  if (treeExpandedIds.value.has(nodeId)) {
    treeExpandedIds.value.delete(nodeId)
    treeExpandedIds.value = new Set(treeExpandedIds.value)
  } else {
    treeExpandedIds.value.add(nodeId)
    treeExpandedIds.value = new Set(treeExpandedIds.value)
    if (!treeChildrenMap.value[nodeId]) {
      treeChildrenLoading.value = { ...treeChildrenLoading.value, [nodeId]: true }
      try {
        const response = await warehousesApi.getChildren(nodeId)
        const data = unwrapData(response)
        treeChildrenMap.value = { ...treeChildrenMap.value, [nodeId]: data.content || data || [] }
      } catch (error) {
        console.error('Failed to fetch children:', error)
        treeChildrenMap.value = { ...treeChildrenMap.value, [nodeId]: [] }
      } finally {
        treeChildrenLoading.value = { ...treeChildrenLoading.value, [nodeId]: false }
      }
    }
  }
}

function switchViewMode(mode) {
  viewMode.value = mode
  if (mode === 'tree') {
    fetchTree()
  } else {
    fetchWarehouses()
  }
}

// --- Expand warehouse row: getById + getChildren ---
async function toggleExpandWarehouse(warehouse) {
  if (expandedWarehouseId.value === warehouse.id) {
    expandedWarehouseId.value = null
    expandedDetails.value = null
    expandedChildren.value = []
    return
  }
  expandedWarehouseId.value = warehouse.id
  expandedLoading.value = true
  try {
    const [detailsRes, childrenRes] = await Promise.all([
      warehousesApi.getById(warehouse.id),
      warehousesApi.getChildren(warehouse.id)
    ])
    const detailsData = unwrapData(detailsRes)
    expandedDetails.value = detailsData || warehouse
    const childrenData = unwrapData(childrenRes)
    expandedChildren.value = childrenData.content || childrenData || []
  } catch (error) {
    console.error('Failed to fetch warehouse details:', error)
    expandedDetails.value = warehouse
    expandedChildren.value = []
  } finally {
    expandedLoading.value = false
  }
}

// --- Pagination ---
function goToPage(page) {
  if (page < 0 || page >= pagination.totalPages) return
  pagination.page = page
  if (searchQuery.value.trim()) {
    performSearch()
  } else if (activeTypeFilter.value !== 'ALL') {
    applyTypeFilter(activeTypeFilter.value)
  } else {
    fetchWarehouses()
  }
}

onMounted(() => {
  fetchWarehouses()
  fetchDefaultWarehouse()
})

function openModal(warehouse = null) {
  editingWarehouse.value = warehouse
  if (warehouse) {
    Object.assign(form, warehouse)
  } else {
    form.name = ''
    form.code = ''
    form.locationType = 'WAREHOUSE'
    form.address = ''
    form.phone = ''
    form.isDefault = false
    form.active = true
  }
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

async function saveWarehouse() {
  if (!form.name?.trim()) {
    errors.name = t('inventory.warehouses.nameRequired')
    return
  }
  if (!form.code?.trim()) {
    errors.code = t('required')
    return
  }

  try {
    if (editingWarehouse.value) {
      await warehousesApi.update(editingWarehouse.value.id, form)
    } else {
      await warehousesApi.create(form)
    }
    showModal.value = false
    fetchWarehouses()
    fetchDefaultWarehouse()
  } catch (error) {
    errors.general = error.response?.data?.message || t('failedToSave')
  }
}

async function deleteWarehouse(warehouse) {
  if (!confirm(t('inventory.warehouses.confirmDelete', { name: warehouse.name }))) return
  try {
    await warehousesApi.delete(warehouse.id)
    fetchWarehouses()
    fetchDefaultWarehouse()
  } catch (error) {
    console.error('Failed to delete warehouse:', error)
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.warehouses.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.warehouses.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.warehouses.addWarehouse') }}
      </button>
    </div>

    <!-- Default warehouse badge -->
    <div v-if="defaultWarehouse" class="card">
      <div class="card-body flex items-center space-x-3 py-3">
        <StarIcon class="h-5 w-5 text-yellow-500" />
        <span class="text-sm text-gray-700">{{ $t('inventory.warehouses.defaultWarehouse') }}:</span>
        <span class="font-medium text-gray-900">{{ defaultWarehouse.name }}</span>
        <span class="badge badge-info">{{ defaultWarehouse.code }}</span>
      </div>
    </div>

    <!-- Search, code lookup, view toggle, type filters -->
    <div class="card">
      <div class="card-body space-y-4">
        <!-- Search + code lookup row -->
        <div class="flex flex-col sm:flex-row gap-3">
          <div class="flex-1 relative">
            <MagnifyingGlassIcon class="h-5 w-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              v-model="searchQuery"
              @input="onSearchInput"
              type="text"
              class="input pl-10"
              :placeholder="$t('inventory.warehouses.searchPlaceholder')"
            />
          </div>
          <div class="flex gap-2">
            <input
              v-model="codeLookupQuery"
              @keyup.enter="lookupByCode"
              type="text"
              class="input w-48"
              :placeholder="$t('inventory.warehouses.codeLookupPlaceholder')"
            />
            <button @click="lookupByCode" class="btn-secondary">
              {{ $t('inventory.warehouses.codeLookup') }}
            </button>
            <button v-if="codeLookupQuery" @click="clearCodeLookup" class="btn-secondary">
              &times;
            </button>
          </div>
        </div>

        <!-- Code lookup result -->
        <div v-if="codeLookupResult" class="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
          <div class="flex items-center space-x-3">
            <BuildingStorefrontIcon class="h-6 w-6 text-green-600" />
            <div>
              <span class="font-medium text-gray-900">{{ codeLookupResult.name }}</span>
              <span class="ml-2 badge badge-info">{{ codeLookupResult.code }}</span>
            </div>
          </div>
          <div class="flex space-x-1">
            <button @click="openModal(codeLookupResult)" class="p-1 text-gray-400 hover:text-primary-600">
              <PencilIcon class="h-4 w-4" />
            </button>
          </div>
        </div>
        <div v-if="codeLookupError" class="p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-600">{{ $t('inventory.warehouses.codeNotFound') }}</p>
        </div>

        <!-- View mode toggle + type filter tabs -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div class="flex items-center space-x-2">
            <button
              @click="switchViewMode('flat')"
              :class="['btn-secondary flex items-center space-x-1', viewMode === 'flat' ? 'bg-primary-100 text-primary-700 border-primary-300' : '']"
            >
              <ListBulletIcon class="h-4 w-4" />
              <span>{{ $t('inventory.warehouses.flatView') }}</span>
            </button>
            <button
              @click="switchViewMode('tree')"
              :class="['btn-secondary flex items-center space-x-1', viewMode === 'tree' ? 'bg-primary-100 text-primary-700 border-primary-300' : '']"
            >
              <QueueListIcon class="h-4 w-4" />
              <span>{{ $t('inventory.warehouses.treeView') }}</span>
            </button>
          </div>
          <div v-if="viewMode === 'flat'" class="flex flex-wrap gap-1">
            <button
              v-for="opt in typeFilterOptions"
              :key="opt.value"
              @click="applyTypeFilter(opt.value)"
              :class="['px-3 py-1 text-sm rounded-full border transition-colors', activeTypeFilter === opt.value ? 'bg-primary-600 text-white border-primary-600' : 'bg-white text-gray-600 border-gray-300 hover:border-primary-300']"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Flat view -->
    <div v-if="viewMode === 'flat'" class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="warehouses.length === 0" class="text-center py-12">
        <BuildingStorefrontIcon class="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p class="text-gray-500">{{ searchQuery ? $t('inventory.warehouses.noSearchResults') : $t('inventory.warehouses.noWarehouses') }}</p>
      </div>

      <div v-else>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-6">
          <div v-for="warehouse in warehouses" :key="warehouse.id" class="border rounded-lg p-4">
            <div class="flex items-start justify-between">
              <div class="flex items-center cursor-pointer" @click="toggleExpandWarehouse(warehouse)">
                <component :is="expandedWarehouseId === warehouse.id ? ChevronDownIcon : ChevronRightIcon" class="h-4 w-4 text-gray-400 mr-1" />
                <BuildingStorefrontIcon class="h-8 w-8 text-primary-600" />
                <div class="ml-3">
                  <h3 class="font-medium text-gray-900">{{ warehouse.name }}</h3>
                  <p class="text-sm text-gray-500">{{ warehouse.code }}</p>
                </div>
              </div>
              <div class="flex space-x-1">
                <button @click="openModal(warehouse)" class="p-1 text-gray-400 hover:text-primary-600">
                  <PencilIcon class="h-4 w-4" />
                </button>
                <button @click="deleteWarehouse(warehouse)" class="p-1 text-gray-400 hover:text-red-600">
                  <TrashIcon class="h-4 w-4" />
                </button>
              </div>
            </div>
            <div class="mt-3 text-sm text-gray-500">
              <p v-if="warehouse.address">{{ warehouse.address }}</p>
              <p v-if="warehouse.phone">{{ warehouse.phone }}</p>
            </div>
            <div class="mt-3 flex items-center space-x-2">
              <span :class="['badge', warehouse.active ? 'badge-success' : 'badge-danger']">
                {{ warehouse.active ? $t('active') : $t('inactive') }}
              </span>
              <span v-if="warehouse.isDefault || (defaultWarehouse && defaultWarehouse.id === warehouse.id)" class="badge badge-warning flex items-center space-x-1">
                <StarIcon class="h-3 w-3" />
                <span>{{ $t('inventory.warehouses.defaultWarehouse') }}</span>
              </span>
            </div>

            <!-- Expanded details (getById + getChildren) -->
            <div v-if="expandedWarehouseId === warehouse.id" class="mt-4 pt-4 border-t border-gray-200">
              <div v-if="expandedLoading" class="flex items-center justify-center py-4">
                <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
              </div>
              <div v-else>
                <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('inventory.warehouses.details') }}</h4>
                <div v-if="expandedDetails" class="text-sm text-gray-600 space-y-1 mb-3">
                  <p v-if="expandedDetails.locationType"><span class="font-medium">{{ $t('inventory.warehouses.type') }}:</span> {{ expandedDetails.locationType }}</p>
                  <p v-if="expandedDetails.address"><span class="font-medium">{{ $t('inventory.warehouses.address') }}:</span> {{ expandedDetails.address }}</p>
                  <p v-if="expandedDetails.phone"><span class="font-medium">{{ $t('phone') }}:</span> {{ expandedDetails.phone }}</p>
                </div>
                <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('inventory.warehouses.children') }}</h4>
                <div v-if="expandedChildren.length === 0" class="text-sm text-gray-400 italic">
                  {{ $t('inventory.warehouses.noChildren') }}
                </div>
                <ul v-else class="space-y-1">
                  <li v-for="child in expandedChildren" :key="child.id" class="flex items-center text-sm text-gray-600 space-x-2">
                    <BuildingStorefrontIcon class="h-4 w-4 text-gray-400" />
                    <span>{{ child.name }}</span>
                    <span class="text-gray-400">({{ child.code }})</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="pagination.totalPages > 1" class="flex items-center justify-between px-6 py-4 border-t border-gray-200">
          <span class="text-sm text-gray-600">{{ $t('inventory.warehouses.totalItems', { total: pagination.totalElements }) }}</span>
          <div class="flex items-center space-x-2">
            <button
              @click="goToPage(pagination.page - 1)"
              :disabled="pagination.page === 0"
              class="btn-secondary text-sm disabled:opacity-50"
            >
              <ChevronLeftIcon class="h-4 w-4" />
              <span>{{ $t('inventory.warehouses.previousPage') }}</span>
            </button>
            <span class="text-sm text-gray-600">{{ $t('inventory.warehouses.page') }} {{ pagination.page + 1 }} / {{ pagination.totalPages }}</span>
            <button
              @click="goToPage(pagination.page + 1)"
              :disabled="pagination.page >= pagination.totalPages - 1"
              class="btn-secondary text-sm disabled:opacity-50"
            >
              <span>{{ $t('inventory.warehouses.nextPage') }}</span>
              <ChevronRightIcon class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Tree view -->
    <div v-if="viewMode === 'tree'" class="card">
      <div v-if="treeLoading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="treeData.length === 0" class="text-center py-12">
        <BuildingStorefrontIcon class="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('inventory.warehouses.noWarehouses') }}</p>
      </div>

      <div v-else class="p-6">
        <h3 class="text-sm font-medium text-gray-500 mb-4">{{ $t('inventory.warehouses.rootLocations') }}</h3>
        <ul class="space-y-2">
          <li v-for="node in treeData" :key="node.id">
            <div class="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50">
              <div class="flex items-center cursor-pointer" @click="toggleTreeNode(node.id)">
                <component :is="treeExpandedIds.has(node.id) ? ChevronDownIcon : ChevronRightIcon" class="h-4 w-4 text-gray-400 mr-2" />
                <BuildingStorefrontIcon class="h-5 w-5 text-primary-600 mr-2" />
                <span class="font-medium text-gray-900">{{ node.name }}</span>
                <span class="ml-2 text-sm text-gray-500">({{ node.code }})</span>
              </div>
              <div class="flex items-center space-x-2">
                <span :class="['badge', node.active !== false ? 'badge-success' : 'badge-danger']">
                  {{ node.active !== false ? $t('active') : $t('inactive') }}
                </span>
                <button @click="openModal(node)" class="p-1 text-gray-400 hover:text-primary-600">
                  <PencilIcon class="h-4 w-4" />
                </button>
                <button @click="deleteWarehouse(node)" class="p-1 text-gray-400 hover:text-red-600">
                  <TrashIcon class="h-4 w-4" />
                </button>
              </div>
            </div>
            <!-- Tree children (loaded via getChildren) -->
            <div v-if="treeExpandedIds.has(node.id)" class="ml-8 mt-1 space-y-1">
              <div v-if="treeChildrenLoading[node.id]" class="text-sm text-gray-400 py-2">
                {{ $t('inventory.warehouses.loadingChildren') }}
              </div>
              <div v-else-if="!treeChildrenMap[node.id] || treeChildrenMap[node.id].length === 0" class="text-sm text-gray-400 italic py-2">
                {{ $t('inventory.warehouses.noChildren') }}
              </div>
              <div
                v-else
                v-for="child in treeChildrenMap[node.id]"
                :key="child.id"
                class="flex items-center justify-between p-2 border rounded-lg hover:bg-gray-50"
              >
                <div class="flex items-center">
                  <BuildingStorefrontIcon class="h-4 w-4 text-gray-400 mr-2" />
                  <span class="text-sm text-gray-800">{{ child.name }}</span>
                  <span class="ml-2 text-xs text-gray-500">({{ child.code }})</span>
                </div>
                <div class="flex items-center space-x-2">
                  <span v-if="child.locationType" class="text-xs text-gray-500">{{ child.locationType }}</span>
                  <button @click="openModal(child)" class="p-1 text-gray-400 hover:text-primary-600">
                    <PencilIcon class="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ editingWarehouse ? $t('inventory.warehouses.editWarehouse') : $t('inventory.warehouses.newWarehouse') }}
          </h3>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('name') }} *</label>
              <input v-model="form.name" type="text" :class="[errors.name ? 'input-error' : 'input']" />
            </div>
            <div>
              <label class="label">{{ $t('code') }} *</label>
              <input v-model="form.code" type="text" :class="[errors.code ? 'input-error' : 'input']" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.warehouses.type') }} *</label>
              <select v-model="form.locationType" class="input">
                <option v-for="type in locationTypes" :key="type.value" :value="type.value">
                  {{ type.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="label">{{ $t('inventory.warehouses.address') }}</label>
              <textarea v-model="form.address" rows="2" class="input"></textarea>
            </div>
            <div>
              <label class="label">{{ $t('phone') }}</label>
              <input v-model="form.phone" type="text" class="input" />
            </div>
            <div class="flex space-x-4">
              <label class="flex items-center">
                <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('active') }}</span>
              </label>
              <label class="flex items-center">
                <input v-model="form.isDefault" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('inventory.warehouses.isDefault') }}</span>
              </label>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="saveWarehouse" class="btn-primary">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
