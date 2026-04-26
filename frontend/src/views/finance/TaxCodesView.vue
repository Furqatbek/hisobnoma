<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { taxCodesApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  XMarkIcon, ChevronDownIcon, ChevronRightIcon, CalculatorIcon,
  DocumentChartBarIcon, FunnelIcon, ArrowPathIcon, TableCellsIcon,
  TagIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const taxCodes = ref([])
const loading = ref(true)
const error = ref('')
const successMsg = ref('')
const search = ref('')
const activeTab = ref('ALL')
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 20

const taxTypes = ['SALES', 'PURCHASE', 'BOTH']

// Modal state
const showModal = ref(false)
const editingTaxCode = ref(null)

const form = reactive({
  code: '',
  name: '',
  description: '',
  taxType: 'SALES',
  active: true
})

// Rate modal state
const showRateModal = ref(false)
const editingRate = ref(null)
const rateForm = reactive({
  taxCodeId: null,
  rate: 0,
  effectiveFrom: '',
  effectiveTo: ''
})

// Expandable rows
const expandedRows = ref({})
const ratesMap = ref({})
const ratesLoading = ref({})

// Delete confirmation
const showDeleteConfirm = ref(false)
const deletingRate = ref(null)
const deletingRateTaxCodeId = ref(null)

// Tax calculator
const calcForm = reactive({
  taxCodeId: null,
  amount: 0
})
const calcResult = ref(null)
const calcLoading = ref(false)

// VAT Summary
const vatForm = reactive({
  periodStart: '',
  periodEnd: ''
})
const vatSummary = ref(null)
const vatLoading = ref(false)

// Active Only toggle
const activeOnly = ref(false)

// All list (no pagination)
const allListLoaded = ref(false)

// Code lookup
const codeLookupInput = ref('')
const codeLookupResult = ref(null)
const codeLookupLoading = ref(false)
const codeLookupError = ref('')

// Summary by Type
const summaryByTypeForm = reactive({
  periodStart: '',
  periodEnd: ''
})
const summaryByType = ref(null)
const summaryByTypeLoading = ref(false)

async function fetchTaxCodes(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (search.value.trim()) {
      res = await taxCodesApi.search(search.value.trim(), { page, size: pageSize, sort: 'code,asc' })
    } else if (activeOnly.value) {
      res = await taxCodesApi.getActive()
    } else if (activeTab.value === 'SALES_QUICK') {
      res = await taxCodesApi.getSales()
    } else if (activeTab.value === 'PURCHASES_QUICK') {
      res = await taxCodesApi.getPurchases()
    } else if (activeTab.value !== 'ALL') {
      res = await taxCodesApi.getByType(activeTab.value)
    } else if (allListLoaded.value) {
      res = await taxCodesApi.getAllList()
    } else {
      res = await taxCodesApi.getAll({ page, size: pageSize, sort: 'code,asc' })
    }
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      taxCodes.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      taxCodes.value = data.content || []
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

onMounted(() => fetchTaxCodes(0))

function switchTab(tab) {
  activeTab.value = tab
  search.value = ''
  activeOnly.value = false
  allListLoaded.value = false
  fetchTaxCodes(0)
}

function handleSearch() {
  activeTab.value = 'ALL'
  activeOnly.value = false
  allListLoaded.value = false
  fetchTaxCodes(0)
}

function toggleActiveOnly() {
  activeOnly.value = !activeOnly.value
  allListLoaded.value = false
  fetchTaxCodes(0)
}

function loadAllList() {
  allListLoaded.value = true
  activeOnly.value = false
  activeTab.value = 'ALL'
  search.value = ''
  fetchTaxCodes(0)
}

async function openModal(taxCode = null) {
  if (taxCode) {
    // Fetch fresh data using getById before editing
    try {
      const res = await taxCodesApi.getById(taxCode.id)
      const fresh = res.data.data || res.data
      editingTaxCode.value = fresh
      form.code = fresh.code || ''
      form.name = fresh.name || ''
      form.description = fresh.description || ''
      form.taxType = fresh.taxType || 'SALES'
      form.active = fresh.active !== false
    } catch (e) {
      // Fallback to existing data if fetch fails
      editingTaxCode.value = taxCode
      form.code = taxCode.code || ''
      form.name = taxCode.name || ''
      form.description = taxCode.description || ''
      form.taxType = taxCode.taxType || 'SALES'
      form.active = taxCode.active !== false
    }
  } else {
    editingTaxCode.value = null
    form.code = ''
    form.name = ''
    form.description = ''
    form.taxType = 'SALES'
    form.active = true
  }
  showModal.value = true
}

async function saveTaxCode() {
  if (!form.code?.trim() || !form.name?.trim()) return
  error.value = ''
  try {
    if (editingTaxCode.value) {
      await taxCodesApi.update(editingTaxCode.value.id, { ...form })
      successMsg.value = t('finance.taxCodes.updateSuccess')
    } else {
      await taxCodesApi.create({ ...form })
      successMsg.value = t('finance.taxCodes.createSuccess')
    }
    showModal.value = false
    fetchTaxCodes(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function toggleStatus(taxCode) {
  error.value = ''
  try {
    if (taxCode.active) {
      await taxCodesApi.deactivate(taxCode.id)
    } else {
      await taxCodesApi.activate(taxCode.id)
    }
    fetchTaxCodes(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

// Expandable rows — rate history
async function toggleExpand(taxCode) {
  const id = taxCode.id
  if (expandedRows.value[id]) {
    expandedRows.value[id] = false
    return
  }
  expandedRows.value[id] = true
  await fetchRates(id)
}

async function fetchRates(taxCodeId) {
  ratesLoading.value[taxCodeId] = true
  try {
    const res = await taxCodesApi.getRates(taxCodeId)
    const data = res.data.data || res.data
    ratesMap.value[taxCodeId] = Array.isArray(data) ? data : data.content || []
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    ratesLoading.value[taxCodeId] = false
  }
}

// Rate modal
function openRateModal(taxCodeId, rate = null) {
  editingRate.value = rate
  rateForm.taxCodeId = taxCodeId
  if (rate) {
    rateForm.rate = rate.rate || 0
    rateForm.effectiveFrom = rate.effectiveFrom || ''
    rateForm.effectiveTo = rate.effectiveTo || ''
  } else {
    rateForm.rate = 0
    rateForm.effectiveFrom = ''
    rateForm.effectiveTo = ''
  }
  showRateModal.value = true
}

async function saveRate() {
  if (!rateForm.taxCodeId || !rateForm.effectiveFrom) return
  error.value = ''
  try {
    const payload = {
      taxCodeId: rateForm.taxCodeId,
      rate: rateForm.rate,
      effectiveFrom: rateForm.effectiveFrom,
      effectiveTo: rateForm.effectiveTo || null
    }
    if (editingRate.value) {
      await taxCodesApi.updateRate(editingRate.value.id, payload)
      successMsg.value = t('finance.taxCodes.rateUpdateSuccess')
    } else {
      await taxCodesApi.createRate(payload)
      successMsg.value = t('finance.taxCodes.rateCreateSuccess')
    }
    showRateModal.value = false
    await fetchRates(rateForm.taxCodeId)
    fetchTaxCodes(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function confirmDeleteRate(rate, taxCodeId) {
  deletingRate.value = rate
  deletingRateTaxCodeId.value = taxCodeId
  showDeleteConfirm.value = true
}

async function deleteRate() {
  if (!deletingRate.value) return
  error.value = ''
  try {
    await taxCodesApi.deleteRate(deletingRate.value.id)
    successMsg.value = t('finance.taxCodes.rateDeleteSuccess')
    showDeleteConfirm.value = false
    await fetchRates(deletingRateTaxCodeId.value)
    fetchTaxCodes(currentPage.value)
    deletingRate.value = null
    deletingRateTaxCodeId.value = null
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showDeleteConfirm.value = false
  }
}

// Tax Calculator
async function calculateTax() {
  if (!calcForm.taxCodeId || !calcForm.amount) return
  calcLoading.value = true
  error.value = ''
  try {
    const res = await taxCodesApi.calculate({ taxCodeId: calcForm.taxCodeId, amount: calcForm.amount })
    calcResult.value = res.data.data || res.data
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    calcLoading.value = false
  }
}

// VAT Summary
async function loadVatSummary() {
  if (!vatForm.periodStart || !vatForm.periodEnd) return
  vatLoading.value = true
  error.value = ''
  try {
    const res = await taxCodesApi.getVatSummary(vatForm.periodStart, vatForm.periodEnd)
    const data = res.data.data || res.data
    vatSummary.value = Array.isArray(data) ? data : [data]
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    vatLoading.value = false
  }
}

// Code Lookup (getByCode)
async function lookupByCode() {
  if (!codeLookupInput.value.trim()) return
  codeLookupLoading.value = true
  codeLookupError.value = ''
  codeLookupResult.value = null
  try {
    const res = await taxCodesApi.getByCode(codeLookupInput.value.trim())
    codeLookupResult.value = res.data.data || res.data
  } catch (e) {
    if (e.response?.status === 404) {
      codeLookupError.value = t('finance.taxCodes.codeNotFound')
    } else {
      codeLookupError.value = e.response?.data?.message || t('errorOccurred')
    }
  } finally {
    codeLookupLoading.value = false
  }
}

// Summary by Type (getSummaryByType)
async function loadSummaryByType() {
  if (!summaryByTypeForm.periodStart || !summaryByTypeForm.periodEnd) return
  summaryByTypeLoading.value = true
  error.value = ''
  try {
    const res = await taxCodesApi.getSummaryByType(summaryByTypeForm.periodStart, summaryByTypeForm.periodEnd)
    const data = res.data.data || res.data
    summaryByType.value = Array.isArray(data) ? data : [data]
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    summaryByTypeLoading.value = false
  }
}

function getTypeClass(type) {
  switch (type) {
    case 'SALES': return 'badge-info'
    case 'PURCHASE': return 'badge-warning'
    case 'BOTH': return 'badge-success'
    default: return 'badge-info'
  }
}

function formatRate(value) {
  return Number(value || 0).toFixed(2)
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number(value) || 0)
}

const tabs = computed(() => [
  { key: 'ALL', label: t('finance.taxCodes.filterAll') },
  ...taxTypes.map(type => ({
    key: type,
    label: t(`finance.taxCodes.type.${type}`)
  }))
])
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.taxCodes.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.taxCodes.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('finance.taxCodes.addTaxCode') }}
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

    <!-- Quick Filters & Search -->
    <div class="card">
      <div class="card-body space-y-4">
        <div class="flex flex-wrap items-center gap-2">
          <button
            @click="toggleActiveOnly"
            :class="['btn-secondary text-sm', activeOnly ? 'ring-2 ring-primary-500 bg-primary-50' : '']"
          >
            <FunnelIcon class="h-4 w-4 mr-1" />
            {{ $t('finance.taxCodes.activeOnly') }}
          </button>
          <button
            @click="switchTab('SALES_QUICK')"
            :class="['btn-secondary text-sm', activeTab === 'SALES_QUICK' ? 'ring-2 ring-blue-500 bg-blue-50' : '']"
          >
            <TagIcon class="h-4 w-4 mr-1" />
            {{ $t('finance.taxCodes.salesFilter') }}
          </button>
          <button
            @click="switchTab('PURCHASES_QUICK')"
            :class="['btn-secondary text-sm', activeTab === 'PURCHASES_QUICK' ? 'ring-2 ring-yellow-500 bg-yellow-50' : '']"
          >
            <TagIcon class="h-4 w-4 mr-1" />
            {{ $t('finance.taxCodes.purchasesFilter') }}
          </button>
          <button
            @click="loadAllList"
            :class="['btn-secondary text-sm', allListLoaded ? 'ring-2 ring-green-500 bg-green-50' : '']"
            :title="$t('finance.taxCodes.loadAllHint')"
          >
            <TableCellsIcon class="h-4 w-4 mr-1" />
            {{ $t('finance.taxCodes.loadAll') }}
          </button>
          <span v-if="allListLoaded" class="text-xs text-green-600 font-medium">{{ $t('finance.taxCodes.allLoaded') }}</span>
        </div>
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @keyup.enter="handleSearch"
            type="text"
            :placeholder="$t('finance.taxCodes.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
      </div>
    </div>

    <!-- Tax Codes Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="taxCodes.length === 0" class="text-center py-12">
        <DocumentChartBarIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('finance.taxCodes.noTaxCodes') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th class="w-10"></th>
              <th>{{ $t('finance.taxCodes.code') }}</th>
              <th>{{ $t('finance.taxCodes.name') }}</th>
              <th>{{ $t('finance.taxCodes.taxType') }}</th>
              <th class="text-right">{{ $t('finance.taxCodes.currentRate') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <template v-for="taxCode in taxCodes" :key="taxCode.id">
              <tr>
                <td>
                  <button
                    @click="toggleExpand(taxCode)"
                    class="p-1 text-gray-400 hover:text-gray-600 rounded"
                  >
                    <ChevronDownIcon v-if="expandedRows[taxCode.id]" class="h-4 w-4" />
                    <ChevronRightIcon v-else class="h-4 w-4" />
                  </button>
                </td>
                <td>
                  <code class="font-mono font-medium text-sm">{{ taxCode.code }}</code>
                </td>
                <td>
                  <p class="font-medium text-sm">{{ taxCode.name }}</p>
                  <p v-if="taxCode.description" class="text-xs text-gray-400">{{ taxCode.description }}</p>
                </td>
                <td>
                  <span :class="['badge text-xs', getTypeClass(taxCode.taxType)]">
                    {{ $t(`finance.taxCodes.type.${taxCode.taxType}`) }}
                  </span>
                </td>
                <td class="text-right">
                  <span class="font-semibold text-sm">{{ formatRate(taxCode.currentRate) }}%</span>
                </td>
                <td>
                  <button
                    @click="toggleStatus(taxCode)"
                    :class="['badge cursor-pointer text-xs', taxCode.active ? 'badge-success' : 'badge-danger']"
                  >
                    {{ taxCode.active ? $t('active') : $t('inactive') }}
                  </button>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button
                      @click="openRateModal(taxCode.id)"
                      class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.taxCodes.addRate')"
                    >
                      <PlusIcon class="h-5 w-5" />
                    </button>
                    <button
                      @click="openModal(taxCode)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('edit')"
                    >
                      <PencilIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
              <!-- Expanded Rate History -->
              <tr v-if="expandedRows[taxCode.id]">
                <td colspan="7" class="bg-gray-50 px-8 py-4">
                  <div class="flex items-center justify-between mb-3">
                    <h4 class="text-sm font-medium text-gray-700">{{ $t('finance.taxCodes.rateHistory') }}</h4>
                    <button @click="openRateModal(taxCode.id)" class="btn-secondary text-xs">
                      <PlusIcon class="h-4 w-4 mr-1" />
                      {{ $t('finance.taxCodes.addRate') }}
                    </button>
                  </div>
                  <div v-if="ratesLoading[taxCode.id]" class="flex items-center justify-center py-4">
                    <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
                  </div>
                  <div v-else-if="!ratesMap[taxCode.id] || ratesMap[taxCode.id].length === 0" class="text-center py-4">
                    <p class="text-sm text-gray-400">{{ $t('finance.taxCodes.noRates') }}</p>
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th>{{ $t('finance.taxCodes.ratePercent') }}</th>
                        <th>{{ $t('finance.taxCodes.effectiveFrom') }}</th>
                        <th>{{ $t('finance.taxCodes.effectiveTo') }}</th>
                        <th class="text-right">{{ $t('actions') }}</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                      <tr v-for="rate in ratesMap[taxCode.id]" :key="rate.id">
                        <td class="font-semibold text-sm">{{ formatRate(rate.rate) }}%</td>
                        <td class="text-sm text-gray-600">{{ rate.effectiveFrom }}</td>
                        <td class="text-sm text-gray-600">{{ rate.effectiveTo || '-' }}</td>
                        <td class="text-right">
                          <div class="flex items-center justify-end space-x-1">
                            <button
                              @click="openRateModal(taxCode.id, rate)"
                              class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                              :title="$t('edit')"
                            >
                              <PencilIcon class="h-4 w-4" />
                            </button>
                            <button
                              @click="confirmDeleteRate(rate, taxCode.id)"
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
            @click="fetchTaxCodes(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchTaxCodes(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm"
          >{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- Tax Calculator -->
    <div class="card">
      <div class="p-6">
        <div class="flex items-center mb-4">
          <CalculatorIcon class="h-5 w-5 text-gray-500 mr-2" />
          <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.taxCodes.taxCalculator') }}</h3>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="label">{{ $t('finance.taxCodes.selectTaxCode') }}</label>
            <select v-model="calcForm.taxCodeId" class="input">
              <option :value="null" disabled>{{ $t('finance.taxCodes.selectTaxCodePlaceholder') }}</option>
              <option v-for="tc in taxCodes" :key="tc.id" :value="tc.id">
                {{ tc.code }} - {{ tc.name }}
              </option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('finance.taxCodes.amount') }}</label>
            <input v-model.number="calcForm.amount" type="number" step="0.01" class="input" />
          </div>
          <div class="flex items-end">
            <button @click="calculateTax" class="btn-primary" :disabled="calcLoading">
              <CalculatorIcon class="h-5 w-5 mr-2" />
              {{ $t('finance.taxCodes.calculate') }}
            </button>
          </div>
        </div>
        <div v-if="calcResult" class="mt-4 p-4 bg-gray-50 rounded-lg">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div>
              <span class="text-gray-500">{{ $t('finance.taxCodes.netAmount') }}:</span>
              <span class="ml-2 font-semibold">{{ formatCurrency(calcResult.netAmount) }}</span>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.taxCodes.taxAmount') }}:</span>
              <span class="ml-2 font-semibold">{{ formatCurrency(calcResult.taxAmount) }}</span>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.taxCodes.grossAmount') }}:</span>
              <span class="ml-2 font-semibold">{{ formatCurrency(calcResult.grossAmount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- VAT Summary -->
    <div class="card">
      <div class="p-6">
        <div class="flex items-center mb-4">
          <DocumentChartBarIcon class="h-5 w-5 text-gray-500 mr-2" />
          <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.taxCodes.vatSummary') }}</h3>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="label">{{ $t('finance.taxCodes.periodStart') }}</label>
            <input v-model="vatForm.periodStart" type="date" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('finance.taxCodes.periodEnd') }}</label>
            <input v-model="vatForm.periodEnd" type="date" class="input" />
          </div>
          <div class="flex items-end">
            <button @click="loadVatSummary" class="btn-primary" :disabled="vatLoading">
              <DocumentChartBarIcon class="h-5 w-5 mr-2" />
              {{ $t('finance.taxCodes.loadSummary') }}
            </button>
          </div>
        </div>
        <div v-if="vatLoading" class="flex items-center justify-center py-6">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-if="vatSummary && vatSummary.length > 0" class="mt-4">
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('finance.taxCodes.code') }}</th>
                  <th>{{ $t('finance.taxCodes.name') }}</th>
                  <th class="text-right">{{ $t('finance.taxCodes.taxableAmount') }}</th>
                  <th class="text-right">{{ $t('finance.taxCodes.taxAmount') }}</th>
                  <th class="text-right">{{ $t('finance.taxCodes.totalAmount') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="(row, idx) in vatSummary" :key="idx">
                  <td><code class="font-mono text-sm">{{ row.taxCode || row.code || '-' }}</code></td>
                  <td class="text-sm">{{ row.taxCodeName || row.name || '-' }}</td>
                  <td class="text-right font-semibold text-sm">{{ formatCurrency(row.taxableAmount) }}</td>
                  <td class="text-right font-semibold text-sm">{{ formatCurrency(row.taxAmount) }}</td>
                  <td class="text-right font-semibold text-sm">{{ formatCurrency(row.totalAmount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Code Lookup -->
    <div class="card">
      <div class="p-6">
        <div class="flex items-center mb-4">
          <MagnifyingGlassIcon class="h-5 w-5 text-gray-500 mr-2" />
          <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.taxCodes.codeLookup') }}</h3>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="label">{{ $t('finance.taxCodes.code') }}</label>
            <input
              v-model="codeLookupInput"
              @keyup.enter="lookupByCode"
              type="text"
              class="input font-mono"
              :placeholder="$t('finance.taxCodes.codeLookupPlaceholder')"
            />
          </div>
          <div class="flex items-end">
            <button @click="lookupByCode" class="btn-primary" :disabled="codeLookupLoading || !codeLookupInput.trim()">
              <MagnifyingGlassIcon class="h-5 w-5 mr-2" />
              {{ $t('finance.taxCodes.lookup') }}
            </button>
          </div>
        </div>
        <div v-if="codeLookupLoading" class="flex items-center justify-center py-4">
          <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
        </div>
        <div v-if="codeLookupError" class="mt-4 p-4 bg-red-50 rounded-lg">
          <p class="text-sm text-red-600">{{ codeLookupError }}</p>
        </div>
        <div v-if="codeLookupResult" class="mt-4 p-4 bg-gray-50 rounded-lg">
          <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('finance.taxCodes.codeLookupResult') }}</h4>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
            <div>
              <span class="text-gray-500">{{ $t('finance.taxCodes.code') }}:</span>
              <span class="ml-2 font-mono font-semibold">{{ codeLookupResult.code }}</span>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.taxCodes.name') }}:</span>
              <span class="ml-2 font-semibold">{{ codeLookupResult.name }}</span>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.taxCodes.taxType') }}:</span>
              <span :class="['ml-2 badge text-xs', getTypeClass(codeLookupResult.taxType)]">
                {{ $t(`finance.taxCodes.type.${codeLookupResult.taxType}`) }}
              </span>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.taxCodes.currentRate') }}:</span>
              <span class="ml-2 font-semibold">{{ formatRate(codeLookupResult.currentRate) }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Summary by Type -->
    <div class="card">
      <div class="p-6">
        <div class="flex items-center mb-2">
          <DocumentChartBarIcon class="h-5 w-5 text-gray-500 mr-2" />
          <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.taxCodes.summaryByType') }}</h3>
        </div>
        <p class="text-sm text-gray-500 mb-4">{{ $t('finance.taxCodes.summaryByTypeDesc') }}</p>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="label">{{ $t('finance.taxCodes.periodStart') }}</label>
            <input v-model="summaryByTypeForm.periodStart" type="date" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('finance.taxCodes.periodEnd') }}</label>
            <input v-model="summaryByTypeForm.periodEnd" type="date" class="input" />
          </div>
          <div class="flex items-end">
            <button @click="loadSummaryByType" class="btn-primary" :disabled="summaryByTypeLoading">
              <DocumentChartBarIcon class="h-5 w-5 mr-2" />
              {{ $t('finance.taxCodes.loadSummaryByType') }}
            </button>
          </div>
        </div>
        <div v-if="summaryByTypeLoading" class="flex items-center justify-center py-6">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-if="summaryByType && summaryByType.length > 0" class="mt-4">
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('finance.taxCodes.taxType') }}</th>
                  <th class="text-right">{{ $t('finance.taxCodes.taxableAmount') }}</th>
                  <th class="text-right">{{ $t('finance.taxCodes.taxAmount') }}</th>
                  <th class="text-right">{{ $t('finance.taxCodes.totalAmount') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="(row, idx) in summaryByType" :key="idx">
                  <td>
                    <span :class="['badge text-xs', getTypeClass(row.taxType || row.type)]">
                      {{ row.taxType || row.type || '-' }}
                    </span>
                  </td>
                  <td class="text-right font-semibold text-sm">{{ formatCurrency(row.taxableAmount) }}</td>
                  <td class="text-right font-semibold text-sm">{{ formatCurrency(row.taxAmount) }}</td>
                  <td class="text-right font-semibold text-sm">{{ formatCurrency(row.totalAmount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div v-else-if="summaryByType && summaryByType.length === 0" class="mt-4 text-center py-6">
          <p class="text-sm text-gray-400">{{ $t('finance.taxCodes.noSummaryByType') }}</p>
        </div>
      </div>
    </div>

    <!-- Create/Edit Tax Code Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingTaxCode ? $t('finance.taxCodes.editTaxCode') : $t('finance.taxCodes.newTaxCode') }}
              </h3>
              <button @click="showModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.taxCodes.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="form.code" type="text" class="input font-mono" :placeholder="$t('finance.taxCodes.codePlaceholder')" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.taxCodes.taxType') }} <span class="text-red-500">*</span></label>
                  <select v-model="form.taxType" class="input">
                    <option v-for="type in taxTypes" :key="type" :value="type">
                      {{ $t(`finance.taxCodes.type.${type}`) }}
                    </option>
                  </select>
                </div>
              </div>

              <div>
                <label class="label">{{ $t('finance.taxCodes.name') }} <span class="text-red-500">*</span></label>
                <input v-model="form.name" type="text" class="input" :placeholder="$t('finance.taxCodes.namePlaceholder')" />
              </div>

              <div>
                <label class="label">{{ $t('description') }}</label>
                <textarea v-model="form.description" rows="2" class="input"></textarea>
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
              <button @click="saveTaxCode" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Rate Modal -->
    <Teleport to="body">
      <div v-if="showRateModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showRateModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-md w-full p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingRate ? $t('finance.taxCodes.editRate') : $t('finance.taxCodes.newRate') }}
              </h3>
              <button @click="showRateModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('finance.taxCodes.ratePercent') }} <span class="text-red-500">*</span></label>
                <input v-model.number="rateForm.rate" type="number" step="0.01" class="input" />
              </div>

              <div>
                <label class="label">{{ $t('finance.taxCodes.effectiveFrom') }} <span class="text-red-500">*</span></label>
                <input v-model="rateForm.effectiveFrom" type="date" class="input" />
              </div>

              <div>
                <label class="label">{{ $t('finance.taxCodes.effectiveTo') }}</label>
                <input v-model="rateForm.effectiveTo" type="date" class="input" />
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showRateModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveRate" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Delete Rate Confirmation Modal -->
    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDeleteConfirm = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.taxCodes.confirmDeleteRateTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('finance.taxCodes.confirmDeleteRate') }}
            </p>
            <div class="flex justify-end space-x-3">
              <button @click="showDeleteConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="deleteRate" class="btn-danger">{{ $t('delete') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
