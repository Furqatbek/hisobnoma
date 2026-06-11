<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { priceListsApi, unwrapData } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  XMarkIcon, TagIcon, ChevronDownIcon, ChevronUpIcon,
  UserPlusIcon, UserMinusIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const priceLists = ref([])
const loading = ref(true)
const error = ref('')
const successMsg = ref('')
const search = ref('')
const activeTab = ref('ALL')
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 20

// Code lookup
const codeLookup = ref('')
const codeLookupResult = ref(null)
const codeLookupError = ref('')
const codeLookupLoading = ref(false)

// Customer filter
const customerFilterId = ref('')
const customerFilterLoading = ref(false)

// Expanded rows
const expandedRows = ref({})

// Modal state
const showModal = ref(false)
const editingPriceList = ref(null)

const form = reactive({
  code: '',
  name: '',
  description: '',
  startDate: '',
  endDate: '',
  priority: 0,
  active: true
})

// Delete confirmation
const showDeleteConfirm = ref(false)
const deletingPriceList = ref(null)

// Items sub-table state
const itemsMap = ref({})
const itemsLoading = ref({})
const itemsTotalPages = ref({})
const itemsCurrentPage = ref({})

// Item modal
const showItemModal = ref(false)
const editingItem = ref(null)
const itemPriceListId = ref(null)

const itemForm = reactive({
  productId: null,
  price: 0,
  minQuantity: 0,
  maxQuantity: 0,
  discountPercent: 0
})

// Customer assignment state
const customerAssignForm = reactive({
  customerId: null,
  priority: 0
})
const assigningPriceListId = ref(null)

// Detail data cache (for customer assignments)
const detailDataMap = ref({})

async function fetchPriceLists(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (activeTab.value === 'ACTIVE') {
      res = await priceListsApi.getActive()
    } else {
      res = await priceListsApi.getAll({ page, size: pageSize, sort: 'code,asc' })
    }
    const data = unwrapData(res)
    if (Array.isArray(data)) {
      priceLists.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      priceLists.value = data.content || []
      totalPages.value = data.page?.totalPages || data.totalPages || 1
      currentPage.value = data.page?.number ?? data.number ?? 0
    }
  } catch (e) {
    if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchPriceLists(0))

function switchTab(tab) {
  activeTab.value = tab
  search.value = ''
  fetchPriceLists(0)
}

function handleSearch() {
  activeTab.value = 'ALL'
  fetchPriceLists(0)
}

// Code lookup
async function lookupByCode() {
  if (!codeLookup.value.trim()) return
  codeLookupLoading.value = true
  codeLookupError.value = ''
  codeLookupResult.value = null
  try {
    const res = await priceListsApi.getByCode(codeLookup.value.trim())
    codeLookupResult.value = unwrapData(res)
  } catch (e) {
    if (e.response?.status === 404) {
      codeLookupError.value = t('pos.priceLists.codeNotFound')
    } else {
      codeLookupError.value = e.response?.data?.message || t('errorOccurred')
    }
  } finally {
    codeLookupLoading.value = false
  }
}

function clearCodeLookup() {
  codeLookup.value = ''
  codeLookupResult.value = null
  codeLookupError.value = ''
}

// Customer filter
async function filterByCustomer() {
  if (!customerFilterId.value) {
    fetchPriceLists(0)
    return
  }
  customerFilterLoading.value = true
  error.value = ''
  try {
    const res = await priceListsApi.getByCustomer(customerFilterId.value)
    const data = unwrapData(res)
    priceLists.value = Array.isArray(data) ? data : data.content || []
    totalPages.value = 1
    currentPage.value = 0
  } catch (e) {
    if (e.response?.status === 404) {
      priceLists.value = []
      totalPages.value = 1
      currentPage.value = 0
    } else if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    customerFilterLoading.value = false
  }
}

function clearCustomerFilter() {
  customerFilterId.value = ''
  fetchPriceLists(0)
}

const tabs = computed(() => [
  { key: 'ALL', label: t('pos.priceLists.filterAll') },
  { key: 'ACTIVE', label: t('pos.priceLists.filterActive') }
])

function openModal(priceList = null) {
  editingPriceList.value = priceList
  if (priceList) {
    form.code = priceList.code || ''
    form.name = priceList.name || ''
    form.description = priceList.description || ''
    form.startDate = priceList.startDate || ''
    form.endDate = priceList.endDate || ''
    form.priority = priceList.priority || 0
    form.active = priceList.active !== false
  } else {
    form.code = ''
    form.name = ''
    form.description = ''
    form.startDate = ''
    form.endDate = ''
    form.priority = 0
    form.active = true
  }
  showModal.value = true
}

async function savePriceList() {
  if (!form.code?.trim() || !form.name?.trim()) return
  error.value = ''
  try {
    if (editingPriceList.value) {
      await priceListsApi.update(editingPriceList.value.id, { ...form })
      successMsg.value = t('pos.priceLists.updateSuccess')
    } else {
      await priceListsApi.create({ ...form })
      successMsg.value = t('pos.priceLists.createSuccess')
    }
    showModal.value = false
    fetchPriceLists(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function toggleStatus(priceList) {
  error.value = ''
  try {
    if (priceList.active) {
      await priceListsApi.deactivate(priceList.id)
    } else {
      await priceListsApi.activate(priceList.id)
    }
    fetchPriceLists(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function confirmDelete(priceList) {
  deletingPriceList.value = priceList
  showDeleteConfirm.value = true
}

async function deletePriceList() {
  if (!deletingPriceList.value) return
  error.value = ''
  try {
    await priceListsApi.delete(deletingPriceList.value.id)
    successMsg.value = t('pos.priceLists.deleteSuccess')
    showDeleteConfirm.value = false
    deletingPriceList.value = null
    fetchPriceLists(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showDeleteConfirm.value = false
  }
}

// Expand / Collapse rows
async function toggleExpand(priceList) {
  const id = priceList.id
  if (expandedRows.value[id]) {
    expandedRows.value[id] = false
    return
  }
  expandedRows.value[id] = true
  await Promise.all([
    fetchItems(id, 0),
    fetchDetail(id)
  ])
}

// Fetch detail data (for customer assignments)
async function fetchDetail(priceListId) {
  try {
    const res = await priceListsApi.getById(priceListId)
    const data = unwrapData(res)
    detailDataMap.value[priceListId] = data
  } catch (e) {
    // silently ignore
  }
}

// Items management
async function fetchItems(priceListId, page = 0) {
  itemsLoading.value[priceListId] = true
  try {
    const res = await priceListsApi.getItems(priceListId, { page, size: pageSize, sort: 'id,asc' })
    const data = unwrapData(res)
    if (Array.isArray(data)) {
      itemsMap.value[priceListId] = data
      itemsTotalPages.value[priceListId] = 1
      itemsCurrentPage.value[priceListId] = 0
    } else {
      itemsMap.value[priceListId] = data.content || []
      itemsTotalPages.value[priceListId] = data.page?.totalPages || data.totalPages || 1
      itemsCurrentPage.value[priceListId] = data.page?.number ?? data.number ?? 0
    }
  } catch (e) {
    if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    itemsLoading.value[priceListId] = false
  }
}

function openItemModal(priceListId, item = null) {
  itemPriceListId.value = priceListId
  editingItem.value = item
  if (item) {
    itemForm.productId = item.productId || null
    itemForm.price = item.price || 0
    itemForm.minQuantity = item.minQuantity || 0
    itemForm.maxQuantity = item.maxQuantity || 0
    itemForm.discountPercent = item.discountPercent || 0
  } else {
    itemForm.productId = null
    itemForm.price = 0
    itemForm.minQuantity = 0
    itemForm.maxQuantity = 0
    itemForm.discountPercent = 0
  }
  showItemModal.value = true
}

async function saveItem() {
  if (!itemForm.productId || !itemPriceListId.value) return
  error.value = ''
  try {
    if (editingItem.value) {
      await priceListsApi.updateItem(itemPriceListId.value, editingItem.value.id, { ...itemForm })
      successMsg.value = t('pos.priceLists.itemUpdateSuccess')
    } else {
      await priceListsApi.addItem(itemPriceListId.value, { ...itemForm })
      successMsg.value = t('pos.priceLists.itemAddSuccess')
    }
    showItemModal.value = false
    fetchItems(itemPriceListId.value, itemsCurrentPage.value[itemPriceListId.value] || 0)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function removeItem(priceListId, itemId) {
  error.value = ''
  try {
    await priceListsApi.removeItem(priceListId, itemId)
    successMsg.value = t('pos.priceLists.itemRemoveSuccess')
    fetchItems(priceListId, itemsCurrentPage.value[priceListId] || 0)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

// Customer assignment
async function assignCustomer(priceListId) {
  if (!customerAssignForm.customerId) return
  error.value = ''
  try {
    await priceListsApi.assignCustomer(priceListId, customerAssignForm.customerId, customerAssignForm.priority)
    successMsg.value = t('pos.priceLists.customerAssignSuccess')
    customerAssignForm.customerId = null
    customerAssignForm.priority = 0
    assigningPriceListId.value = null
    fetchDetail(priceListId)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function unassignCustomer(priceListId, customerId) {
  error.value = ''
  try {
    await priceListsApi.unassignCustomer(priceListId, customerId)
    successMsg.value = t('pos.priceLists.customerUnassignSuccess')
    fetchDetail(priceListId)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleDateString()
  } catch {
    return dateStr
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('pos.priceLists.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('pos.priceLists.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('pos.priceLists.addPriceList') }}
      </button>
    </div>

    <!-- Error / Success -->
    <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-red-600">{{ error }}</p>
      <button @click="error = ''" class="text-red-400 hover:text-red-600"><XMarkIcon class="h-4 w-4" /></button>
    </div>
    <div v-if="successMsg" class="p-4 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-green-600">{{ successMsg }}</p>
      <button @click="successMsg = ''" class="text-green-400 hover:text-green-600"><XMarkIcon class="h-4 w-4" /></button>
    </div>

    <!-- Filter Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex space-x-4">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="switchTab(tab.key)"
          :class="[
            'px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors',
            activeTab === tab.key
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Search, Code Lookup & Customer Filter -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- Search -->
      <div class="card">
        <div class="card-body">
          <label class="text-sm font-medium text-gray-700 mb-2 block">{{ $t('search') }}</label>
          <div class="relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="search"
              @keyup.enter="handleSearch"
              type="text"
              :placeholder="$t('pos.priceLists.searchPlaceholder')"
              class="input pl-10"
            />
          </div>
        </div>
      </div>

      <!-- Code Lookup -->
      <div class="card">
        <div class="card-body">
          <label class="text-sm font-medium text-gray-700 mb-2 block">{{ $t('pos.priceLists.codeLookup') }}</label>
          <div class="flex gap-2">
            <input
              v-model="codeLookup"
              @keyup.enter="lookupByCode"
              type="text"
              :placeholder="$t('pos.priceLists.codeLookupPlaceholder')"
              class="input flex-1 font-mono"
            />
            <button @click="lookupByCode" :disabled="codeLookupLoading" class="btn-primary text-sm">
              {{ $t('search') }}
            </button>
            <button v-if="codeLookupResult || codeLookupError" @click="clearCodeLookup" class="btn-secondary text-sm">
              {{ $t('clear') }}
            </button>
          </div>
          <div v-if="codeLookupLoading" class="mt-2 text-sm text-gray-500">{{ $t('loading') }}</div>
          <div v-if="codeLookupError" class="mt-2 text-sm text-red-600">{{ codeLookupError }}</div>
          <div v-if="codeLookupResult" class="mt-2 p-2 bg-green-50 border border-green-200 rounded-lg text-sm">
            <p class="font-medium">{{ codeLookupResult.name }}</p>
            <p class="text-gray-500">{{ $t('code') }}: {{ codeLookupResult.code }} | {{ $t('status') }}: {{ codeLookupResult.active ? $t('active') : $t('inactive') }}</p>
          </div>
        </div>
      </div>

      <!-- Customer Filter -->
      <div class="card">
        <div class="card-body">
          <label class="text-sm font-medium text-gray-700 mb-2 block">{{ $t('pos.priceLists.customerFilter') }}</label>
          <div class="flex gap-2">
            <input
              v-model.number="customerFilterId"
              @keyup.enter="filterByCustomer"
              type="number"
              :placeholder="$t('pos.priceLists.customerFilterPlaceholder')"
              class="input flex-1"
            />
            <button @click="filterByCustomer" :disabled="customerFilterLoading" class="btn-primary text-sm">
              {{ $t('filter') }}
            </button>
            <button v-if="customerFilterId" @click="clearCustomerFilter" class="btn-secondary text-sm">
              {{ $t('clear') }}
            </button>
          </div>
          <div v-if="customerFilterLoading" class="mt-2 text-sm text-gray-500">{{ $t('loading') }}</div>
        </div>
      </div>
    </div>

    <!-- Price Lists Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="priceLists.length === 0" class="text-center py-12">
        <TagIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('pos.priceLists.noPriceLists') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th></th>
              <th>{{ $t('pos.priceLists.code') }}</th>
              <th>{{ $t('pos.priceLists.name') }}</th>
              <th>{{ $t('pos.priceLists.description') }}</th>
              <th>{{ $t('pos.priceLists.startDate') }}</th>
              <th>{{ $t('pos.priceLists.endDate') }}</th>
              <th>{{ $t('pos.priceLists.priority') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <template v-for="priceList in priceLists" :key="priceList.id">
              <tr>
                <td>
                  <button
                    @click="toggleExpand(priceList)"
                    class="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
                  >
                    <ChevronUpIcon v-if="expandedRows[priceList.id]" class="h-5 w-5" />
                    <ChevronDownIcon v-else class="h-5 w-5" />
                  </button>
                </td>
                <td>
                  <code class="font-mono font-medium text-sm">{{ priceList.code }}</code>
                </td>
                <td>
                  <p class="font-medium text-sm">{{ priceList.name }}</p>
                </td>
                <td class="text-sm text-gray-500">{{ priceList.description || '-' }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(priceList.startDate) }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(priceList.endDate) }}</td>
                <td class="text-sm text-gray-500">{{ priceList.priority ?? '-' }}</td>
                <td>
                  <button
                    @click="toggleStatus(priceList)"
                    :class="['badge cursor-pointer text-xs', priceList.active ? 'badge-success' : 'badge-danger']"
                  >
                    {{ priceList.active ? $t('active') : $t('inactive') }}
                  </button>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button
                      @click="openModal(priceList)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('edit')"
                    >
                      <PencilIcon class="h-5 w-5" />
                    </button>
                    <button
                      @click="confirmDelete(priceList)"
                      class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                      :title="$t('delete')"
                    >
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Expanded Row -->
              <tr v-if="expandedRows[priceList.id]">
                <td :colspan="9" class="bg-gray-50 p-0">
                  <div class="p-6 space-y-6">

                    <!-- Items Sub-Table -->
                    <div>
                      <div class="flex items-center justify-between mb-3">
                        <h4 class="text-sm font-semibold text-gray-700">{{ $t('pos.priceLists.items') }}</h4>
                        <button @click="openItemModal(priceList.id)" class="btn-primary text-xs">
                          <PlusIcon class="h-4 w-4 mr-1" />
                          {{ $t('pos.priceLists.addItem') }}
                        </button>
                      </div>

                      <div v-if="itemsLoading[priceList.id]" class="flex items-center justify-center py-8">
                        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                      </div>

                      <div v-else-if="!itemsMap[priceList.id] || itemsMap[priceList.id].length === 0" class="text-center py-6">
                        <p class="text-sm text-gray-400">{{ $t('pos.priceLists.noItems') }}</p>
                      </div>

                      <div v-else class="table-container">
                        <table class="table">
                          <thead>
                            <tr>
                              <th>{{ $t('pos.priceLists.product') }}</th>
                              <th class="text-right">{{ $t('pos.priceLists.price') }}</th>
                              <th class="text-right">{{ $t('pos.priceLists.minQty') }}</th>
                              <th class="text-right">{{ $t('pos.priceLists.maxQty') }}</th>
                              <th class="text-right">{{ $t('pos.priceLists.discountPercent') }}</th>
                              <th class="text-right">{{ $t('actions') }}</th>
                            </tr>
                          </thead>
                          <tbody class="divide-y divide-gray-200">
                            <tr v-for="item in itemsMap[priceList.id]" :key="item.id">
                              <td class="text-sm">{{ item.productName || item.productId }}</td>
                              <td class="text-sm text-right">{{ item.price }}</td>
                              <td class="text-sm text-right">{{ item.minQuantity ?? '-' }}</td>
                              <td class="text-sm text-right">{{ item.maxQuantity ?? '-' }}</td>
                              <td class="text-sm text-right">{{ item.discountPercent != null ? item.discountPercent + '%' : '-' }}</td>
                              <td class="text-right">
                                <div class="flex items-center justify-end space-x-1">
                                  <button
                                    @click="openItemModal(priceList.id, item)"
                                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                                    :title="$t('edit')"
                                  >
                                    <PencilIcon class="h-4 w-4" />
                                  </button>
                                  <button
                                    @click="removeItem(priceList.id, item.id)"
                                    class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                                    :title="$t('delete')"
                                  >
                                    <TrashIcon class="h-4 w-4" />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>

                      <!-- Items Pagination -->
                      <div v-if="(itemsTotalPages[priceList.id] || 0) > 1" class="flex items-center justify-between px-2 py-2 mt-2">
                        <p class="text-xs text-gray-500">{{ $t('page') }} {{ (itemsCurrentPage[priceList.id] || 0) + 1 }} / {{ itemsTotalPages[priceList.id] }}</p>
                        <div class="flex gap-2">
                          <button
                            @click="fetchItems(priceList.id, (itemsCurrentPage[priceList.id] || 0) - 1)"
                            :disabled="(itemsCurrentPage[priceList.id] || 0) === 0"
                            class="btn-secondary text-xs"
                          >{{ $t('previous') }}</button>
                          <button
                            @click="fetchItems(priceList.id, (itemsCurrentPage[priceList.id] || 0) + 1)"
                            :disabled="(itemsCurrentPage[priceList.id] || 0) >= (itemsTotalPages[priceList.id] || 1) - 1"
                            class="btn-secondary text-xs"
                          >{{ $t('next') }}</button>
                        </div>
                      </div>
                    </div>

                    <!-- Customer Assignments Section -->
                    <div>
                      <div class="flex items-center justify-between mb-3">
                        <h4 class="text-sm font-semibold text-gray-700">{{ $t('pos.priceLists.customerAssignments') }}</h4>
                        <button
                          @click="assigningPriceListId = assigningPriceListId === priceList.id ? null : priceList.id"
                          class="btn-primary text-xs"
                        >
                          <UserPlusIcon class="h-4 w-4 mr-1" />
                          {{ $t('pos.priceLists.assignCustomer') }}
                        </button>
                      </div>

                      <!-- Assign Customer Form -->
                      <div v-if="assigningPriceListId === priceList.id" class="mb-4 p-4 bg-white rounded-lg border border-gray-200">
                        <div class="flex items-end gap-3">
                          <div>
                            <label class="label">{{ $t('pos.priceLists.customerId') }}</label>
                            <input v-model.number="customerAssignForm.customerId" type="number" class="input" :placeholder="$t('pos.priceLists.customerIdPlaceholder')" />
                          </div>
                          <div>
                            <label class="label">{{ $t('pos.priceLists.priority') }}</label>
                            <input v-model.number="customerAssignForm.priority" type="number" class="input" />
                          </div>
                          <button @click="assignCustomer(priceList.id)" class="btn-primary text-sm">{{ $t('pos.priceLists.assign') }}</button>
                          <button @click="assigningPriceListId = null" class="btn-secondary text-sm">{{ $t('cancel') }}</button>
                        </div>
                      </div>

                      <!-- Assigned Customers List -->
                      <div v-if="detailDataMap[priceList.id]?.customerAssignments && detailDataMap[priceList.id].customerAssignments.length > 0" class="table-container">
                        <table class="table">
                          <thead>
                            <tr>
                              <th>{{ $t('pos.priceLists.customerId') }}</th>
                              <th>{{ $t('pos.priceLists.customerName') }}</th>
                              <th>{{ $t('pos.priceLists.priority') }}</th>
                              <th class="text-right">{{ $t('actions') }}</th>
                            </tr>
                          </thead>
                          <tbody class="divide-y divide-gray-200">
                            <tr v-for="assignment in detailDataMap[priceList.id].customerAssignments" :key="assignment.customerId">
                              <td class="text-sm">{{ assignment.customerId }}</td>
                              <td class="text-sm">{{ assignment.customerName || '-' }}</td>
                              <td class="text-sm">{{ assignment.priority ?? '-' }}</td>
                              <td class="text-right">
                                <button
                                  @click="unassignCustomer(priceList.id, assignment.customerId)"
                                  class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                                  :title="$t('pos.priceLists.unassign')"
                                >
                                  <UserMinusIcon class="h-4 w-4" />
                                </button>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>

                      <div v-else class="text-center py-4">
                        <p class="text-sm text-gray-400">{{ $t('pos.priceLists.noCustomerAssignments') }}</p>
                      </div>
                    </div>

                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
        <p class="text-sm text-gray-500">{{ $t('page') }} {{ currentPage + 1 }} / {{ totalPages }}</p>
        <div class="flex gap-2">
          <button
            @click="fetchPriceLists(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchPriceLists(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm"
          >{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- Create/Edit Price List Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingPriceList ? $t('pos.priceLists.editPriceList') : $t('pos.priceLists.newPriceList') }}
              </h3>
              <button @click="showModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.priceLists.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="form.code" type="text" class="input font-mono" :placeholder="$t('pos.priceLists.codePlaceholder')" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.priceLists.priority') }}</label>
                  <input v-model.number="form.priority" type="number" class="input" />
                </div>
              </div>

              <div>
                <label class="label">{{ $t('pos.priceLists.name') }} <span class="text-red-500">*</span></label>
                <input v-model="form.name" type="text" class="input" :placeholder="$t('pos.priceLists.namePlaceholder')" />
              </div>

              <div>
                <label class="label">{{ $t('pos.priceLists.description') }}</label>
                <textarea v-model="form.description" rows="2" class="input"></textarea>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.priceLists.startDate') }}</label>
                  <input v-model="form.startDate" type="date" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.priceLists.endDate') }}</label>
                  <input v-model="form.endDate" type="date" class="input" />
                </div>
              </div>

              <div class="flex flex-wrap gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('active') }}</span>
                </label>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="savePriceList" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Add/Edit Item Modal -->
    <Teleport to="body">
      <div v-if="showItemModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showItemModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingItem ? $t('pos.priceLists.editItem') : $t('pos.priceLists.newItem') }}
              </h3>
              <button @click="showItemModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('pos.priceLists.productId') }} <span class="text-red-500">*</span></label>
                <input v-model.number="itemForm.productId" type="number" class="input" :placeholder="$t('pos.priceLists.productIdPlaceholder')" />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.priceLists.price') }} <span class="text-red-500">*</span></label>
                  <input v-model.number="itemForm.price" type="number" step="0.01" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.priceLists.discountPercent') }}</label>
                  <input v-model.number="itemForm.discountPercent" type="number" step="0.01" class="input" />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.priceLists.minQty') }}</label>
                  <input v-model.number="itemForm.minQuantity" type="number" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.priceLists.maxQty') }}</label>
                  <input v-model.number="itemForm.maxQuantity" type="number" class="input" />
                </div>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showItemModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveItem" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Delete Confirmation Modal -->
    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDeleteConfirm = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('pos.priceLists.confirmDeleteTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('pos.priceLists.confirmDelete', { name: deletingPriceList?.name }) }}
            </p>
            <div class="flex justify-end space-x-3">
              <button @click="showDeleteConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="deletePriceList" class="btn-danger">{{ $t('delete') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
