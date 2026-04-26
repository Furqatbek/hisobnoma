<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { fiscalApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, StarIcon, LockClosedIcon, LockOpenIcon,
  XMarkIcon, ChevronDownIcon, ChevronRightIcon, MagnifyingGlassIcon,
  CalendarDaysIcon, DocumentMagnifyingGlassIcon, InformationCircleIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const years = ref([])
const currentYearId = ref(null)
const loading = ref(true)
const error = ref('')
const successMsg = ref('')

// Pagination (for getYears paginated mode)
const usePaginated = ref(false)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 20

// Year modal
const showYearModal = ref(false)
const editingYear = ref(null)
const yearForm = reactive({ year: new Date().getFullYear(), name: '', startDate: '', endDate: '' })

// Close year confirmation
const showCloseYearConfirm = ref(false)
const closingYear = ref(null)

// Close period confirmation
const showClosePeriodConfirm = ref(false)
const closingPeriod = ref(null)

// Reopen period
const showReopenModal = ref(false)
const reopeningPeriod = ref(null)
const reopenReason = ref('')

// Expanded rows: yearId -> periods array
const expandedYears = ref({})
const periodsLoading = ref({})
const periodsData = ref({})

// Open periods
const openPeriods = ref([])
const openPeriodsLoading = ref(false)

// Date lookup
const dateLookupInput = ref('')
const dateLookupYearResult = ref(null)
const dateLookupPeriodResult = ref(null)
const dateLookupLoading = ref(false)
const dateLookupError = ref('')

// Year details cache
const yearDetailsCache = ref({})

// Period detail modal
const showPeriodDetailModal = ref(false)
const periodDetail = ref(null)
const periodDetailLoading = ref(false)

function clearMessages() { error.value = ''; successMsg.value = '' }

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('uz-UZ')
}

async function fetchYears(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (usePaginated.value) {
      res = await fiscalApi.getYears({ page, size: pageSize, sort: 'year,desc' })
    } else {
      res = await fiscalApi.getAllYears()
    }
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      years.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      years.value = data.content || []
      totalPages.value = data.page?.totalPages || data.totalPages || 1
      currentPage.value = data.page?.number ?? data.number ?? 0
    }
  } catch (e) {
    if (e.response?.status !== 403) error.value = e.response?.data?.message || t('failedToLoad')
  } finally {
    loading.value = false
  }
}

function togglePaginated() {
  usePaginated.value = !usePaginated.value
  fetchYears(0)
}

async function fetchCurrentYear() {
  try {
    const res = await fiscalApi.getCurrentYear()
    const data = res.data.data || res.data
    currentYearId.value = data?.id || null
  } catch {
    currentYearId.value = null
  }
}

function openYearModal(y = null) {
  editingYear.value = y
  if (y) {
    yearForm.year = y.year
    yearForm.name = y.name || ''
    yearForm.startDate = y.startDate?.substring(0, 10) || ''
    yearForm.endDate = y.endDate?.substring(0, 10) || ''
  } else {
    yearForm.year = new Date().getFullYear()
    yearForm.name = ''
    yearForm.startDate = ''
    yearForm.endDate = ''
  }
  showYearModal.value = true
}

async function saveYear() {
  if (!yearForm.year || !yearForm.name?.trim() || !yearForm.startDate || !yearForm.endDate) return
  clearMessages()
  try {
    if (editingYear.value) {
      await fiscalApi.updateYear(editingYear.value.id, { ...yearForm })
      successMsg.value = t('finance.fiscal.updateYearSuccess')
    } else {
      await fiscalApi.createYear({ ...yearForm })
      successMsg.value = t('finance.fiscal.createYearSuccess')
    }
    showYearModal.value = false
    fetchYears()
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function handleSetCurrentYear(y) {
  clearMessages()
  try {
    await fiscalApi.setCurrentYear(y.id)
    currentYearId.value = y.id
    successMsg.value = t('finance.fiscal.setCurrentSuccess', { name: y.name })
    fetchYears()
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function confirmCloseYear(y) {
  closingYear.value = y
  showCloseYearConfirm.value = true
}

async function doCloseYear() {
  if (!closingYear.value) return
  clearMessages()
  try {
    await fiscalApi.closeYear(closingYear.value.id)
    successMsg.value = t('finance.fiscal.closeYearSuccess', { name: closingYear.value.name })
    showCloseYearConfirm.value = false
    closingYear.value = null
    fetchYears()
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showCloseYearConfirm.value = false
  }
}

async function toggleExpand(y) {
  const id = y.id
  if (expandedYears.value[id]) {
    delete expandedYears.value[id]
    return
  }
  expandedYears.value[id] = true
  periodsLoading.value[id] = true
  try {
    const res = await fiscalApi.getPeriods(id)
    const data = res.data.data || res.data
    periodsData.value[id] = Array.isArray(data) ? data : (data.content || [])
  } catch (e) {
    error.value = e.response?.data?.message || t('failedToLoad')
    delete expandedYears.value[id]
  } finally {
    periodsLoading.value[id] = false
  }
}

function confirmClosePeriod(period) {
  closingPeriod.value = period
  showClosePeriodConfirm.value = true
}

async function doClosePeriod() {
  if (!closingPeriod.value) return
  clearMessages()
  try {
    await fiscalApi.closePeriod(closingPeriod.value.id)
    successMsg.value = t('finance.fiscal.closePeriodSuccess', { name: closingPeriod.value.name })
    showClosePeriodConfirm.value = false
    // Refresh periods for the parent year
    const yearId = closingPeriod.value.fiscalYearId
    closingPeriod.value = null
    if (yearId && expandedYears.value[yearId]) {
      periodsLoading.value[yearId] = true
      try {
        const res = await fiscalApi.getPeriods(yearId)
        const data = res.data.data || res.data
        periodsData.value[yearId] = Array.isArray(data) ? data : (data.content || [])
      } finally {
        periodsLoading.value[yearId] = false
      }
    }
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showClosePeriodConfirm.value = false
  }
}

function openReopenModal(period) {
  reopeningPeriod.value = period
  reopenReason.value = ''
  showReopenModal.value = true
}

async function doReopenPeriod() {
  if (!reopeningPeriod.value || !reopenReason.value.trim()) return
  clearMessages()
  try {
    await fiscalApi.reopenPeriod(reopeningPeriod.value.id, reopenReason.value.trim())
    successMsg.value = t('finance.fiscal.reopenPeriodSuccess', { name: reopeningPeriod.value.name })
    showReopenModal.value = false
    const yearId = reopeningPeriod.value.fiscalYearId
    reopeningPeriod.value = null
    reopenReason.value = ''
    if (yearId && expandedYears.value[yearId]) {
      periodsLoading.value[yearId] = true
      try {
        const res = await fiscalApi.getPeriods(yearId)
        const data = res.data.data || res.data
        periodsData.value[yearId] = Array.isArray(data) ? data : (data.content || [])
      } finally {
        periodsLoading.value[yearId] = false
      }
    }
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showReopenModal.value = false
  }
}

// Open periods (getOpenPeriods)
async function fetchOpenPeriods() {
  openPeriodsLoading.value = true
  try {
    const res = await fiscalApi.getOpenPeriods()
    const data = res.data.data || res.data
    openPeriods.value = Array.isArray(data) ? data : (data.content || [])
  } catch (e) {
    if (e.response?.status !== 403) error.value = e.response?.data?.message || t('failedToLoad')
  } finally {
    openPeriodsLoading.value = false
  }
}

// Date lookup (getYearByDate + getPeriodByDate)
async function lookupByDate() {
  if (!dateLookupInput.value) return
  dateLookupLoading.value = true
  dateLookupError.value = ''
  dateLookupYearResult.value = null
  dateLookupPeriodResult.value = null
  try {
    const [yearRes, periodRes] = await Promise.allSettled([
      fiscalApi.getYearByDate(dateLookupInput.value),
      fiscalApi.getPeriodByDate(dateLookupInput.value)
    ])
    if (yearRes.status === 'fulfilled') {
      dateLookupYearResult.value = yearRes.value.data.data || yearRes.value.data
    } else {
      dateLookupError.value = t('finance.fiscal.noYearForDate')
    }
    if (periodRes.status === 'fulfilled') {
      dateLookupPeriodResult.value = periodRes.value.data.data || periodRes.value.data
    }
  } catch (e) {
    dateLookupError.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    dateLookupLoading.value = false
  }
}

// Year details (getYearDetails) - used when expanding year row
async function toggleExpandWithDetails(y) {
  const id = y.id
  if (expandedYears.value[id]) {
    delete expandedYears.value[id]
    return
  }
  expandedYears.value[id] = true
  periodsLoading.value[id] = true
  try {
    const res = await fiscalApi.getYearDetails(id)
    const data = res.data.data || res.data
    yearDetailsCache.value[id] = data
    periodsData.value[id] = Array.isArray(data.periods) ? data.periods : (Array.isArray(data) ? data : [])
  } catch (e) {
    // Fallback to getPeriods if getYearDetails fails
    try {
      const res = await fiscalApi.getPeriods(id)
      const data = res.data.data || res.data
      periodsData.value[id] = Array.isArray(data) ? data : (data.content || [])
    } catch (e2) {
      error.value = e2.response?.data?.message || t('failedToLoad')
      delete expandedYears.value[id]
    }
  } finally {
    periodsLoading.value[id] = false
  }
}

// Edit year using getYearById for fresh data
async function openYearModalFresh(y = null) {
  if (y) {
    try {
      const res = await fiscalApi.getYearById(y.id)
      const fresh = res.data.data || res.data
      editingYear.value = fresh
      yearForm.year = fresh.year
      yearForm.name = fresh.name || ''
      yearForm.startDate = fresh.startDate?.substring(0, 10) || ''
      yearForm.endDate = fresh.endDate?.substring(0, 10) || ''
    } catch (e) {
      // Fallback to existing data
      editingYear.value = y
      yearForm.year = y.year
      yearForm.name = y.name || ''
      yearForm.startDate = y.startDate?.substring(0, 10) || ''
      yearForm.endDate = y.endDate?.substring(0, 10) || ''
    }
  } else {
    editingYear.value = null
    yearForm.year = new Date().getFullYear()
    yearForm.name = ''
    yearForm.startDate = ''
    yearForm.endDate = ''
  }
  showYearModal.value = true
}

// Period detail (getPeriodById)
async function openPeriodDetail(period) {
  periodDetailLoading.value = true
  periodDetail.value = null
  showPeriodDetailModal.value = true
  try {
    const res = await fiscalApi.getPeriodById(period.id)
    periodDetail.value = res.data.data || res.data
  } catch (e) {
    // Fallback to existing data
    periodDetail.value = period
  } finally {
    periodDetailLoading.value = false
  }
}

onMounted(() => {
  fetchYears()
  fetchCurrentYear()
  fetchOpenPeriods()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.fiscal.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.fiscal.subtitle') }}</p>
      </div>
      <button @click="openYearModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('finance.fiscal.addYear') }}
      </button>
    </div>

    <!-- Messages -->
    <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-red-600">{{ error }}</p>
      <button @click="error = ''" class="text-red-400 hover:text-red-600"><XMarkIcon class="h-4 w-4" /></button>
    </div>
    <div v-if="successMsg" class="p-4 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-green-600">{{ successMsg }}</p>
      <button @click="successMsg = ''" class="text-green-400 hover:text-green-600"><XMarkIcon class="h-4 w-4" /></button>
    </div>

    <!-- Open Periods Quick View -->
    <div class="card">
      <div class="p-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center">
            <LockOpenIcon class="h-5 w-5 text-green-500 mr-2" />
            <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.fiscal.openPeriods') }}</h3>
          </div>
          <button @click="fetchOpenPeriods" class="btn-secondary text-sm" :title="$t('update')">
            <ArrowPathIcon class="h-4 w-4" />
          </button>
        </div>
        <p class="text-sm text-gray-500 mb-4">{{ $t('finance.fiscal.openPeriodsDesc') }}</p>
        <div v-if="openPeriodsLoading" class="flex items-center justify-center py-6">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="openPeriods.length === 0" class="text-center py-6">
          <p class="text-sm text-gray-400">{{ $t('finance.fiscal.noOpenPeriods') }}</p>
        </div>
        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('finance.fiscal.periodName') }}</th>
                <th>{{ $t('finance.fiscal.parentYear') }}</th>
                <th>{{ $t('finance.fiscal.startDate') }}</th>
                <th>{{ $t('finance.fiscal.endDate') }}</th>
                <th>{{ $t('status') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="p in openPeriods" :key="p.id">
                <td class="text-sm">
                  <button @click="openPeriodDetail(p)" class="text-primary-600 hover:text-primary-800 hover:underline">
                    {{ p.name }}
                  </button>
                </td>
                <td class="text-sm text-gray-500">{{ p.fiscalYearName || p.fiscalYear?.name || '-' }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(p.startDate) }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(p.endDate) }}</td>
                <td>
                  <span class="badge badge-success text-xs">{{ $t('finance.fiscal.statusOpen') }}</span>
                </td>
                <td class="text-right">
                  <button
                    @click="confirmClosePeriod(p)"
                    class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                    :title="$t('finance.fiscal.closePeriod')"
                  >
                    <LockClosedIcon class="h-5 w-5" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Date Lookup -->
    <div class="card">
      <div class="p-6">
        <div class="flex items-center mb-2">
          <CalendarDaysIcon class="h-5 w-5 text-gray-500 mr-2" />
          <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.fiscal.dateLookup') }}</h3>
        </div>
        <p class="text-sm text-gray-500 mb-4">{{ $t('finance.fiscal.dateLookupDesc') }}</p>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="label">{{ $t('finance.fiscal.lookupDate') }}</label>
            <input
              v-model="dateLookupInput"
              type="date"
              class="input"
            />
          </div>
          <div class="flex items-end">
            <button @click="lookupByDate" class="btn-primary" :disabled="dateLookupLoading || !dateLookupInput">
              <MagnifyingGlassIcon class="h-5 w-5 mr-2" />
              {{ $t('finance.fiscal.lookupSearch') }}
            </button>
          </div>
        </div>
        <div v-if="dateLookupLoading" class="flex items-center justify-center py-4">
          <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
        </div>
        <div v-if="dateLookupError" class="mt-4 p-4 bg-red-50 rounded-lg">
          <p class="text-sm text-red-600">{{ dateLookupError }}</p>
        </div>
        <div v-if="dateLookupYearResult || dateLookupPeriodResult" class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-if="dateLookupYearResult" class="p-4 bg-blue-50 rounded-lg border border-blue-200">
            <h4 class="text-sm font-medium text-blue-800 mb-2">{{ $t('finance.fiscal.lookupYear') }}</h4>
            <div class="space-y-1 text-sm">
              <div><span class="text-gray-500">{{ $t('finance.fiscal.name') }}:</span> <span class="font-semibold">{{ dateLookupYearResult.name }}</span></div>
              <div><span class="text-gray-500">{{ $t('finance.fiscal.year') }}:</span> <span class="font-mono font-semibold">{{ dateLookupYearResult.year }}</span></div>
              <div><span class="text-gray-500">{{ $t('finance.fiscal.startDate') }}:</span> <span>{{ formatDate(dateLookupYearResult.startDate) }}</span></div>
              <div><span class="text-gray-500">{{ $t('finance.fiscal.endDate') }}:</span> <span>{{ formatDate(dateLookupYearResult.endDate) }}</span></div>
              <div>
                <span class="text-gray-500">{{ $t('status') }}:</span>
                <span :class="['ml-1 badge text-xs', dateLookupYearResult.status === 'OPEN' ? 'badge-success' : 'badge-danger']">
                  {{ dateLookupYearResult.status === 'OPEN' ? $t('finance.fiscal.statusOpen') : $t('finance.fiscal.statusClosed') }}
                </span>
              </div>
            </div>
          </div>
          <div v-if="dateLookupPeriodResult" class="p-4 bg-green-50 rounded-lg border border-green-200">
            <h4 class="text-sm font-medium text-green-800 mb-2">{{ $t('finance.fiscal.lookupPeriod') }}</h4>
            <div class="space-y-1 text-sm">
              <div><span class="text-gray-500">{{ $t('finance.fiscal.periodName') }}:</span> <span class="font-semibold">{{ dateLookupPeriodResult.name }}</span></div>
              <div><span class="text-gray-500">{{ $t('finance.fiscal.periodNumber') }}:</span> <span class="font-mono font-semibold">{{ dateLookupPeriodResult.periodNumber }}</span></div>
              <div><span class="text-gray-500">{{ $t('finance.fiscal.startDate') }}:</span> <span>{{ formatDate(dateLookupPeriodResult.startDate) }}</span></div>
              <div><span class="text-gray-500">{{ $t('finance.fiscal.endDate') }}:</span> <span>{{ formatDate(dateLookupPeriodResult.endDate) }}</span></div>
              <div>
                <span class="text-gray-500">{{ $t('status') }}:</span>
                <span :class="['ml-1 badge text-xs', dateLookupPeriodResult.status === 'OPEN' ? 'badge-success' : 'badge-danger']">
                  {{ dateLookupPeriodResult.status === 'OPEN' ? $t('finance.fiscal.statusOpen') : $t('finance.fiscal.statusClosed') }}
                </span>
              </div>
            </div>
          </div>
          <div v-else-if="dateLookupYearResult && !dateLookupPeriodResult" class="p-4 bg-yellow-50 rounded-lg border border-yellow-200">
            <p class="text-sm text-yellow-700">{{ $t('finance.fiscal.noPeriodForDate') }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Fiscal Years Table -->
    <div class="card">
      <div class="px-6 py-3 border-b border-gray-200 flex items-center justify-between">
        <h3 class="text-sm font-medium text-gray-700">{{ $t('finance.fiscal.title') }}</h3>
        <button
          @click="togglePaginated"
          :class="['btn-secondary text-xs', usePaginated ? 'ring-2 ring-primary-500 bg-primary-50' : '']"
        >
          {{ $t('finance.fiscal.paginatedView') }}
        </button>
      </div>
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="years.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('finance.fiscal.noYears') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th class="w-10"></th>
              <th>{{ $t('finance.fiscal.year') }}</th>
              <th>{{ $t('finance.fiscal.name') }}</th>
              <th>{{ $t('finance.fiscal.startDate') }}</th>
              <th>{{ $t('finance.fiscal.endDate') }}</th>
              <th>{{ $t('finance.fiscal.current') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <template v-for="y in years" :key="y.id">
              <!-- Year row -->
              <tr :class="currentYearId === y.id ? 'bg-primary-50' : ''">
                <td>
                  <button
                    @click="toggleExpandWithDetails(y)"
                    class="p-1 text-gray-400 hover:text-gray-600 rounded"
                  >
                    <ChevronDownIcon v-if="expandedYears[y.id]" class="h-4 w-4" />
                    <ChevronRightIcon v-else class="h-4 w-4" />
                  </button>
                </td>
                <td class="font-mono font-medium text-sm">{{ y.year }}</td>
                <td class="text-sm">{{ y.name }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(y.startDate) }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(y.endDate) }}</td>
                <td>
                  <span v-if="currentYearId === y.id" class="badge badge-warning text-xs">
                    <StarIcon class="h-3 w-3 inline mr-1" />{{ $t('finance.fiscal.currentBadge') }}
                  </span>
                </td>
                <td>
                  <span :class="['badge text-xs', y.status === 'OPEN' ? 'badge-success' : 'badge-danger']">
                    {{ y.status === 'OPEN' ? $t('finance.fiscal.statusOpen') : $t('finance.fiscal.statusClosed') }}
                  </span>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button
                      v-if="currentYearId !== y.id && y.status === 'OPEN'"
                      @click="handleSetCurrentYear(y)"
                      class="p-2 text-gray-400 hover:text-yellow-500 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.fiscal.setCurrent')"
                    >
                      <StarIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="y.status === 'OPEN'"
                      @click="openYearModalFresh(y)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('edit')"
                    >
                      <PencilIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="y.status === 'OPEN'"
                      @click="confirmCloseYear(y)"
                      class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.fiscal.closeYear')"
                    >
                      <LockClosedIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Expanded periods -->
              <tr v-if="expandedYears[y.id]" :key="'periods-' + y.id">
                <td colspan="8" class="p-0">
                  <div class="bg-gray-50 px-6 py-4">
                    <h4 class="text-sm font-medium text-gray-700 mb-3">
                      {{ $t('finance.fiscal.periodsFor', { name: y.name }) }}
                    </h4>

                    <!-- Year Details Summary -->
                    <div v-if="yearDetailsCache[y.id]" class="mb-4 grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                      <div class="p-3 bg-white rounded-lg border border-gray-200">
                        <span class="text-gray-500">{{ $t('finance.fiscal.totalPeriods') }}:</span>
                        <span class="ml-2 font-semibold">{{ yearDetailsCache[y.id].totalPeriods ?? periodsData[y.id]?.length ?? '-' }}</span>
                      </div>
                      <div class="p-3 bg-white rounded-lg border border-gray-200">
                        <span class="text-gray-500">{{ $t('finance.fiscal.openPeriodsCount') }}:</span>
                        <span class="ml-2 font-semibold text-green-600">{{ yearDetailsCache[y.id].openPeriodsCount ?? '-' }}</span>
                      </div>
                      <div class="p-3 bg-white rounded-lg border border-gray-200">
                        <span class="text-gray-500">{{ $t('finance.fiscal.closedPeriodsCount') }}:</span>
                        <span class="ml-2 font-semibold text-red-600">{{ yearDetailsCache[y.id].closedPeriodsCount ?? '-' }}</span>
                      </div>
                    </div>

                    <div v-if="periodsLoading[y.id]" class="flex items-center justify-center py-8">
                      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                    </div>

                    <div v-else-if="!periodsData[y.id] || periodsData[y.id].length === 0" class="text-center py-6">
                      <p class="text-sm text-gray-400">{{ $t('finance.fiscal.noPeriods') }}</p>
                    </div>

                    <div v-else class="table-container rounded-lg border border-gray-200">
                      <table class="table">
                        <thead>
                          <tr>
                            <th>{{ $t('finance.fiscal.periodNumber') }}</th>
                            <th>{{ $t('finance.fiscal.periodName') }}</th>
                            <th>{{ $t('finance.fiscal.startDate') }}</th>
                            <th>{{ $t('finance.fiscal.endDate') }}</th>
                            <th>{{ $t('status') }}</th>
                            <th class="text-right">{{ $t('actions') }}</th>
                          </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-200">
                          <tr v-for="p in periodsData[y.id]" :key="p.id">
                            <td class="font-mono text-sm">{{ p.periodNumber }}</td>
                            <td class="text-sm">
                              <button @click="openPeriodDetail(p)" class="text-primary-600 hover:text-primary-800 hover:underline">
                                {{ p.name }}
                              </button>
                            </td>
                            <td class="text-sm text-gray-500">{{ formatDate(p.startDate) }}</td>
                            <td class="text-sm text-gray-500">{{ formatDate(p.endDate) }}</td>
                            <td>
                              <span :class="['badge text-xs', p.status === 'OPEN' ? 'badge-success' : 'badge-danger']">
                                {{ p.status === 'OPEN' ? $t('finance.fiscal.statusOpen') : $t('finance.fiscal.statusClosed') }}
                              </span>
                            </td>
                            <td class="text-right">
                              <div class="flex items-center justify-end space-x-1">
                                <button
                                  v-if="p.status === 'OPEN'"
                                  @click="confirmClosePeriod(p)"
                                  class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                                  :title="$t('finance.fiscal.closePeriod')"
                                >
                                  <LockClosedIcon class="h-5 w-5" />
                                </button>
                                <button
                                  v-if="p.status === 'CLOSED'"
                                  @click="openReopenModal(p)"
                                  class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100"
                                  :title="$t('finance.fiscal.reopenPeriod')"
                                >
                                  <LockOpenIcon class="h-5 w-5" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="usePaginated && totalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
        <p class="text-sm text-gray-500">{{ $t('page') }} {{ currentPage + 1 }} / {{ totalPages }}</p>
        <div class="flex gap-2">
          <button
            @click="fetchYears(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchYears(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm"
          >{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- ========== CREATE/EDIT YEAR MODAL ========== -->
    <Teleport to="body">
      <div v-if="showYearModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showYearModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-md w-full p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingYear ? $t('finance.fiscal.editYear') : $t('finance.fiscal.newYear') }}
              </h3>
              <button @click="showYearModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>
            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.fiscal.year') }} <span class="text-red-500">*</span></label>
                  <input v-model.number="yearForm.year" type="number" min="2000" max="2100" class="input font-mono" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.fiscal.name') }} <span class="text-red-500">*</span></label>
                  <input v-model="yearForm.name" type="text" class="input" />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.fiscal.startDate') }} <span class="text-red-500">*</span></label>
                  <input v-model="yearForm.startDate" type="date" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.fiscal.endDate') }} <span class="text-red-500">*</span></label>
                  <input v-model="yearForm.endDate" type="date" class="input" />
                </div>
              </div>
            </div>
            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showYearModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveYear" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== CLOSE YEAR CONFIRMATION ========== -->
    <Teleport to="body">
      <div v-if="showCloseYearConfirm" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCloseYearConfirm = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.fiscal.confirmCloseYearTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('finance.fiscal.confirmCloseYear', { name: closingYear?.name }) }}
            </p>
            <div class="flex justify-end space-x-3">
              <button @click="showCloseYearConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="doCloseYear" class="btn-danger">
                <LockClosedIcon class="h-4 w-4 mr-2" />
                {{ $t('finance.fiscal.closeYear') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== CLOSE PERIOD CONFIRMATION ========== -->
    <Teleport to="body">
      <div v-if="showClosePeriodConfirm" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showClosePeriodConfirm = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.fiscal.confirmClosePeriodTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('finance.fiscal.confirmClosePeriod', { name: closingPeriod?.name }) }}
            </p>
            <div class="flex justify-end space-x-3">
              <button @click="showClosePeriodConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="doClosePeriod" class="btn-danger">
                <LockClosedIcon class="h-4 w-4 mr-2" />
                {{ $t('finance.fiscal.closePeriod') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== REOPEN PERIOD MODAL ========== -->
    <Teleport to="body">
      <div v-if="showReopenModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showReopenModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-md w-full p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ $t('finance.fiscal.reopenPeriod') }}
              </h3>
              <button @click="showReopenModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>
            <p class="text-sm text-gray-500 mb-4">
              {{ $t('finance.fiscal.reopenPeriodDesc', { name: reopeningPeriod?.name }) }}
            </p>
            <div>
              <label class="label">{{ $t('finance.fiscal.reopenReason') }} <span class="text-red-500">*</span></label>
              <textarea
                v-model="reopenReason"
                rows="3"
                class="input"
                :placeholder="$t('finance.fiscal.reopenReasonPlaceholder')"
              ></textarea>
            </div>
            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showReopenModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="doReopenPeriod" :disabled="!reopenReason.trim()" class="btn-primary">
                <LockOpenIcon class="h-4 w-4 mr-2" />
                {{ $t('finance.fiscal.reopenPeriod') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== PERIOD DETAIL MODAL ========== -->
    <Teleport to="body">
      <div v-if="showPeriodDetailModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showPeriodDetailModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-md w-full p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ $t('finance.fiscal.periodDetails') }}
              </h3>
              <button @click="showPeriodDetailModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>
            <div v-if="periodDetailLoading" class="flex items-center justify-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
            <div v-else-if="periodDetail" class="space-y-3 text-sm">
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <span class="text-gray-500">{{ $t('finance.fiscal.periodName') }}</span>
                  <p class="font-semibold">{{ periodDetail.name }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.fiscal.periodNumber') }}</span>
                  <p class="font-mono font-semibold">{{ periodDetail.periodNumber }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.fiscal.startDate') }}</span>
                  <p>{{ formatDate(periodDetail.startDate) }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.fiscal.endDate') }}</span>
                  <p>{{ formatDate(periodDetail.endDate) }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('status') }}</span>
                  <p>
                    <span :class="['badge text-xs', periodDetail.status === 'OPEN' ? 'badge-success' : 'badge-danger']">
                      {{ periodDetail.status === 'OPEN' ? $t('finance.fiscal.statusOpen') : $t('finance.fiscal.statusClosed') }}
                    </span>
                  </p>
                </div>
                <div v-if="periodDetail.fiscalYearName || periodDetail.fiscalYear?.name">
                  <span class="text-gray-500">{{ $t('finance.fiscal.parentYear') }}</span>
                  <p class="font-semibold">{{ periodDetail.fiscalYearName || periodDetail.fiscalYear?.name }}</p>
                </div>
              </div>
            </div>
            <div class="mt-6 flex justify-end">
              <button @click="showPeriodDetailModal = false" class="btn-secondary">{{ $t('close') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
