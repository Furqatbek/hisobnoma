<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, onMounted, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapData, unwrapList, uomApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon, ArrowsRightLeftIcon, FunnelIcon } from '@heroicons/vue/24/outline'

const toast = useToastStore()

const { t } = useI18n()

const uoms = ref([])
const allUoms = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingUom = ref(null)

// Pagination
const pagination = ref({ page: 0, size: 20, totalPages: 0, totalElements: 0 })

// Search
const searchQuery = ref('')
let searchTimeout = null

// Base units filter
const showBaseOnly = ref(false)

// Code lookup
const codeLookup = ref('')
const codeLookupResult = ref(null)
const codeLookupError = ref('')

// Converter
const converter = reactive({ fromUomId: null, toUomId: null, quantity: 1, result: null, loading: false, error: '' })

const form = reactive({
  code: '',
  name: '',
  symbol: '',
  description: '',
  isBaseUnit: true,
  conversionFactor: 1,
  active: true
})
const errors = reactive({})

// Auto-generate code from name
function generateCode(name) {
  return name.toUpperCase().replace(/[^A-Z0-9]+/g, '').substring(0, 10)
}

function onNameChange() {
  if (!editingUom.value && form.name) {
    form.code = generateCode(form.name)
  }
}

// Fetch with getPaginated (replaces getAll for the main list)
async function fetchUoms() {
  loading.value = true
  try {
    if (searchQuery.value.trim()) {
      const response = await uomApi.search(searchQuery.value.trim())
      const data = unwrapData(response)
      uoms.value = data.content || data || []
      pagination.value.totalPages = data.page?.totalPages || 0
      pagination.value.totalElements = data.page?.totalElements || uoms.value.length
    } else if (showBaseOnly.value) {
      const response = await uomApi.getBase()
      const data = unwrapData(response)
      uoms.value = data.content || data || []
      pagination.value.totalPages = 0
      pagination.value.totalElements = uoms.value.length
    } else {
      const params = { page: pagination.value.page, size: pagination.value.size }
      const response = await uomApi.getPaginated(params)
      const data = unwrapData(response)
      uoms.value = data.content || data || []
      pagination.value.totalPages = data.page?.totalPages || 0
      pagination.value.totalElements = data.page?.totalElements || uoms.value.length
    }
  } catch (error) {
    console.error('Failed to fetch UOMs:', error)
    // Fallback to getAll
    try {
      const response = await uomApi.getAll()
      uoms.value = unwrapList(response)
    } catch (e) { console.error(e) }
  } finally {
    loading.value = false
  }
}

// Fetch all UOMs for converter dropdowns
async function fetchAllUoms() {
  try {
    const response = await uomApi.getAll()
    allUoms.value = unwrapList(response)
  } catch (error) { console.error(error) }
}

onMounted(() => { fetchUoms(); fetchAllUoms() })

// Search debounce
function onSearchInput() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    pagination.value.page = 0
    fetchUoms()
  }, 400)
}

// Toggle base units filter
function toggleBaseFilter() {
  showBaseOnly.value = !showBaseOnly.value
  pagination.value.page = 0
  fetchUoms()
}

// Code lookup
async function lookupByCode() {
  codeLookupResult.value = null
  codeLookupError.value = ''
  if (!codeLookup.value.trim()) return
  try {
    const response = await uomApi.getByCode(codeLookup.value.trim())
    codeLookupResult.value = unwrapData(response)
  } catch (error) {
    codeLookupError.value = t('inventory.uom.codeNotFound')
  }
}

// Open modal - use getById for fresh data when editing
async function openModal(uom = null) {
  editingUom.value = uom
  if (uom) {
    try {
      const response = await uomApi.getById(uom.id)
      const fresh = unwrapData(response)
      form.code = fresh.code || ''
      form.name = fresh.name || ''
      form.symbol = fresh.symbol || ''
      form.description = fresh.description || ''
      form.isBaseUnit = fresh.isBaseUnit ?? true
      form.conversionFactor = fresh.conversionFactor ?? 1
      form.active = fresh.active ?? true
    } catch (error) {
      // Fallback to passed data
      form.code = uom.code || ''
      form.name = uom.name || ''
      form.symbol = uom.symbol || ''
      form.description = uom.description || ''
      form.isBaseUnit = uom.isBaseUnit ?? true
      form.conversionFactor = uom.conversionFactor ?? 1
      form.active = uom.active ?? true
    }
  } else {
    form.code = ''
    form.name = ''
    form.symbol = ''
    form.description = ''
    form.isBaseUnit = true
    form.conversionFactor = 1
    form.active = true
  }
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

async function saveUom() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) {
    errors.name = t('inventory.uom.nameRequired')
  }
  if (!form.code?.trim()) {
    errors.code = t('inventory.uom.codeRequired')
  }
  if (!form.symbol?.trim()) {
    errors.symbol = t('required')
  }
  if (Object.keys(errors).length > 0) return

  try {
    if (editingUom.value) {
      await uomApi.update(editingUom.value.id, form)
    } else {
      await uomApi.create(form)
    }
    showModal.value = false
    fetchUoms()
    fetchAllUoms()
  } catch (error) {
    errors.general = error.response?.data?.message || t('failedToSave')
  }
}

async function deleteUom(uom) {
  if (!confirm(t('inventory.uom.confirmDelete', { name: uom.name }))) return
  try {
    await uomApi.delete(uom.id)
    fetchUoms()
    fetchAllUoms()
  } catch (error) {
    console.error('Failed to delete UOM:', error)
    toast.error(error.response?.data?.message || t('failedToDelete'))
  }
}

// Unit converter
async function doConvert() {
  converter.result = null
  converter.error = ''
  if (!converter.fromUomId || !converter.toUomId || !converter.quantity) return
  converter.loading = true
  try {
    const response = await uomApi.convert(converter.quantity, converter.fromUomId, converter.toUomId)
    converter.result = response.data.data ?? response.data
  } catch (error) {
    converter.error = error.response?.data?.message || t('inventory.uom.convertError')
  } finally {
    converter.loading = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.uom.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.uom.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.uom.addUom') }}
      </button>
    </div>

    <!-- Search, Code Lookup & Base Filter -->
    <div class="card">
      <div class="card-body">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- Search input -->
          <div class="relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="searchQuery"
              @input="onSearchInput"
              type="text"
              class="input pl-10"
              :placeholder="$t('inventory.uom.searchPlaceholder')"
            />
          </div>
          <!-- Code lookup -->
          <div class="flex gap-2">
            <input
              v-model="codeLookup"
              @keyup.enter="lookupByCode"
              type="text"
              class="input flex-1"
              :placeholder="$t('inventory.uom.codeLookupPlaceholder')"
            />
            <button @click="lookupByCode" class="btn-secondary px-3">
              {{ $t('inventory.uom.codeLookup') }}
            </button>
          </div>
          <!-- Base units toggle -->
          <div class="flex items-center">
            <button @click="toggleBaseFilter" :class="['btn-secondary flex items-center gap-2', showBaseOnly ? 'bg-primary-100 text-primary-700 border-primary-300' : '']">
              <FunnelIcon class="h-5 w-5" />
              {{ showBaseOnly ? $t('inventory.uom.baseUnitsOnly') : $t('inventory.uom.allUnits') }}
            </button>
          </div>
        </div>
        <!-- Code lookup result -->
        <div v-if="codeLookupResult" class="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
          <div class="flex items-center justify-between">
            <div class="text-sm">
              <span class="font-medium">{{ codeLookupResult.name }}</span>
              <span class="text-gray-500 ml-2">({{ codeLookupResult.code }})</span>
              <span class="text-gray-500 ml-2">{{ codeLookupResult.symbol }}</span>
            </div>
            <button @click="codeLookupResult = null" class="text-gray-400 hover:text-gray-600 text-sm">&times;</button>
          </div>
        </div>
        <div v-if="codeLookupError" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-600">{{ codeLookupError }}</p>
        </div>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="uoms.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.uom.noUoms') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('name') }}</th>
              <th>{{ $t('code') }}</th>
              <th>{{ $t('inventory.uom.symbol') }}</th>
              <th>{{ $t('description') }}</th>
              <th>{{ $t('inventory.uom.baseUnit') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="uom in uoms" :key="uom.id">
              <td class="font-medium">{{ uom.name }}</td>
              <td class="font-mono text-sm text-gray-500">{{ uom.code }}</td>
              <td class="text-gray-500">{{ uom.symbol }}</td>
              <td class="text-gray-500">{{ uom.description || '-' }}</td>
              <td>
                <span :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  uom.isBaseUnit ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]">
                  {{ uom.isBaseUnit ? $t('yes') : $t('no') }}
                </span>
              </td>
              <td>
                <span :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  uom.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                ]">
                  {{ uom.active ? $t('active') : $t('inactive') }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <button @click="openModal(uom)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button @click="deleteUom(uom)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
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
          <p class="text-sm text-gray-500">{{ $t('inventory.uom.totalItems', { total: pagination.totalElements }) }}</p>
          <div class="flex items-center space-x-2">
            <button @click="pagination.page--; fetchUoms()" :disabled="pagination.page === 0" class="btn-secondary px-3 py-1">{{ $t('inventory.uom.previousPage') }}</button>
            <span class="text-sm text-gray-600">{{ $t('inventory.uom.page') }} {{ pagination.page + 1 }} / {{ pagination.totalPages }}</span>
            <button @click="pagination.page++; fetchUoms()" :disabled="pagination.page >= pagination.totalPages - 1" class="btn-secondary px-3 py-1">{{ $t('inventory.uom.nextPage') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Unit Converter -->
    <div class="card">
      <div class="card-header">
        <h3 class="text-lg font-medium text-gray-900 flex items-center gap-2">
          <ArrowsRightLeftIcon class="h-5 w-5" />
          {{ $t('inventory.uom.converter') }}
        </h3>
      </div>
      <div class="card-body">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div>
            <label class="label">{{ $t('inventory.uom.quantity') }}</label>
            <input v-model.number="converter.quantity" type="number" step="any" min="0" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('inventory.uom.fromUom') }}</label>
            <select v-model="converter.fromUomId" class="input">
              <option :value="null">{{ $t('inventory.uom.selectUom') }}</option>
              <option v-for="u in allUoms" :key="u.id" :value="u.id">{{ u.name }} ({{ u.symbol }})</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('inventory.uom.toUom') }}</label>
            <select v-model="converter.toUomId" class="input">
              <option :value="null">{{ $t('inventory.uom.selectUom') }}</option>
              <option v-for="u in allUoms" :key="u.id" :value="u.id">{{ u.name }} ({{ u.symbol }})</option>
            </select>
          </div>
          <div>
            <button @click="doConvert" :disabled="converter.loading || !converter.fromUomId || !converter.toUomId" class="btn-primary w-full">
              {{ converter.loading ? $t('inventory.uom.converting') : $t('inventory.uom.convertBtn') }}
            </button>
          </div>
        </div>
        <div v-if="converter.result !== null" class="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
          <p class="text-sm font-medium text-green-800">{{ $t('inventory.uom.convertResult') }}: {{ converter.result }}</p>
        </div>
        <div v-if="converter.error" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-600">{{ converter.error }}</p>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ editingUom ? $t('inventory.uom.editUom') : $t('inventory.uom.newUom') }}
          </h3>

          <div v-if="errors.general" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-600">{{ errors.general }}</p>
          </div>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('name') }} *</label>
              <input v-model="form.name" @input="onNameChange" type="text" :class="[errors.name ? 'input-error' : 'input']" />
              <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">{{ $t('code') }} *</label>
                <input v-model="form.code" type="text" :class="[errors.code ? 'input-error' : 'input']" />
                <p v-if="errors.code" class="mt-1 text-sm text-red-600">{{ errors.code }}</p>
              </div>

              <div>
                <label class="label">{{ $t('inventory.uom.symbol') }} *</label>
                <input v-model="form.symbol" type="text" :class="[errors.symbol ? 'input-error' : 'input']" />
                <p v-if="errors.symbol" class="mt-1 text-sm text-red-600">{{ errors.symbol }}</p>
              </div>
            </div>

            <div>
              <label class="label">{{ $t('description') }}</label>
              <textarea v-model="form.description" rows="2" class="input"></textarea>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">{{ $t('inventory.productForm.conversionFactor') }}</label>
                <input v-model.number="form.conversionFactor" type="number" step="0.000001" min="0" class="input" />
              </div>
            </div>

            <div class="flex flex-wrap gap-4">
              <label class="flex items-center">
                <input v-model="form.isBaseUnit" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.uom.baseUnit') }}</span>
              </label>

              <label class="flex items-center">
                <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm text-gray-700">{{ $t('active') }}</span>
              </label>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="saveUom" class="btn-primary">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
