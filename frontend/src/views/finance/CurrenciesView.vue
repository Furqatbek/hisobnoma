<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { currenciesApi, exchangeRatesApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, StarIcon, XMarkIcon, ArrowsRightLeftIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

// Shared state
const activeTab = ref('currencies')
const error = ref('')
const successMsg = ref('')
const loading = ref(true)

// --- Currencies ---
const currencies = ref([])
const currTotalPages = ref(0)
const currPage = ref(0)
const showCurrModal = ref(false)
const editingCurr = ref(null)
const currForm = reactive({ code: '', name: '', symbol: '', decimalPlaces: 2, active: true })
const activeCurrencies = ref([])

// --- Exchange Rates ---
const rates = ref([])
const rateTotalPages = ref(0)
const ratePage = ref(0)
const showRateModal = ref(false)
const editingRate = ref(null)
const rateForm = reactive({ fromCurrencyCode: '', toCurrencyCode: '', rate: 0, effectiveDate: '', expiryDate: '' })
const showDeleteConfirm = ref(false)
const deletingRate = ref(null)
const filterDate = ref('')
const showCurrent = ref(false)

// --- Converter ---
const conv = reactive({ amount: 1, from: '', to: '', date: '', result: null, loading: false })

const pageSize = 20

function clearMessages() { error.value = ''; successMsg.value = '' }

function extractPage(res) {
  const data = res.data.data || res.data
  if (Array.isArray(data)) return { items: data, total: 1, page: 0 }
  return { items: data.content || [], total: data.page?.totalPages || data.totalPages || 1, page: data.page?.number ?? data.number ?? 0 }
}

// ---- Currencies CRUD ----
async function fetchCurrencies(page = 0) {
  loading.value = true; error.value = ''
  try {
    const { items, total, page: p } = extractPage(await currenciesApi.getAll({ page, size: pageSize, sort: 'code,asc' }))
    currencies.value = items; currTotalPages.value = total; currPage.value = p
  } catch (e) { if (e.response?.status !== 403) error.value = e.response?.data?.message || t('failedToLoad') }
  finally { loading.value = false }
}

async function fetchActiveCurrencies() {
  try { const res = await currenciesApi.getActive(); activeCurrencies.value = res.data.data || res.data || [] } catch {}
}

function openCurrModal(c = null) {
  editingCurr.value = c
  Object.assign(currForm, c ? { code: c.code, name: c.name, symbol: c.symbol || '', decimalPlaces: c.decimalPlaces ?? 2, active: c.active !== false }
    : { code: '', name: '', symbol: '', decimalPlaces: 2, active: true })
  showCurrModal.value = true
}

async function saveCurrency() {
  if (!currForm.code?.trim() || !currForm.name?.trim()) return
  clearMessages()
  try {
    if (editingCurr.value) {
      await currenciesApi.update(editingCurr.value.id, { ...currForm })
      successMsg.value = t('finance.currencies.updateSuccess')
    } else {
      await currenciesApi.create({ ...currForm })
      successMsg.value = t('finance.currencies.createSuccess')
    }
    showCurrModal.value = false; fetchCurrencies(currPage.value); fetchActiveCurrencies()
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

async function toggleCurrStatus(c) {
  clearMessages()
  try { c.active ? await currenciesApi.deactivate(c.id) : await currenciesApi.activate(c.id); fetchCurrencies(currPage.value); fetchActiveCurrencies() }
  catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

async function setBaseCurrency(c) {
  clearMessages()
  try { await currenciesApi.setBase(c.id); successMsg.value = t('finance.currencies.baseSuccess', { code: c.code }); fetchCurrencies(currPage.value) }
  catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

// ---- Exchange Rates CRUD ----
async function fetchRates(page = 0) {
  loading.value = true; error.value = ''
  try {
    let res
    if (showCurrent.value) res = await exchangeRatesApi.getCurrent()
    else if (filterDate.value) res = await exchangeRatesApi.getByDate(filterDate.value)
    else res = await exchangeRatesApi.getAll({ page, size: pageSize, sort: 'effectiveDate,desc' })
    const { items, total, page: p } = extractPage(res)
    rates.value = items; rateTotalPages.value = total; ratePage.value = p
  } catch (e) { if (e.response?.status !== 403) error.value = e.response?.data?.message || t('failedToLoad') }
  finally { loading.value = false }
}

function openRateModal(r = null) {
  editingRate.value = r
  Object.assign(rateForm, r ? { fromCurrencyCode: r.fromCurrencyCode, toCurrencyCode: r.toCurrencyCode, rate: r.rate, effectiveDate: r.effectiveDate?.substring(0, 10) || '', expiryDate: r.expiryDate?.substring(0, 10) || '' }
    : { fromCurrencyCode: '', toCurrencyCode: '', rate: 0, effectiveDate: '', expiryDate: '' })
  showRateModal.value = true
}

async function saveRate() {
  if (!rateForm.fromCurrencyCode || !rateForm.toCurrencyCode || !rateForm.rate) return
  clearMessages()
  try {
    const payload = { ...rateForm, expiryDate: rateForm.expiryDate || null }
    if (editingRate.value) {
      await exchangeRatesApi.update(editingRate.value.id, payload)
      successMsg.value = t('finance.exchangeRates.updateSuccess')
    } else {
      await exchangeRatesApi.create(payload)
      successMsg.value = t('finance.exchangeRates.createSuccess')
    }
    showRateModal.value = false; fetchRates(ratePage.value)
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

async function toggleRateStatus(r) {
  clearMessages()
  try { r.active ? await exchangeRatesApi.deactivate(r.id) : await exchangeRatesApi.activate(r.id); fetchRates(ratePage.value) }
  catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

function confirmDeleteRate(r) { deletingRate.value = r; showDeleteConfirm.value = true }

async function deleteRate() {
  if (!deletingRate.value) return
  clearMessages()
  try {
    await exchangeRatesApi.delete(deletingRate.value.id)
    successMsg.value = t('finance.exchangeRates.deleteSuccess')
    showDeleteConfirm.value = false; deletingRate.value = null; fetchRates(ratePage.value)
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred'); showDeleteConfirm.value = false }
}

async function convertCurrency() {
  if (!conv.amount || !conv.from || !conv.to) return
  conv.loading = true; conv.result = null
  try {
    const res = await exchangeRatesApi.convert(conv.amount, conv.from, conv.to, conv.date || undefined)
    conv.result = res.data.data ?? res.data
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
  finally { conv.loading = false }
}

function switchTab(tab) {
  activeTab.value = tab; clearMessages()
  if (tab === 'currencies') fetchCurrencies(0)
  else { fetchRates(0); fetchActiveCurrencies() }
}

function applyRateFilter() { showCurrent.value = false; fetchRates(0) }
function showCurrentRates() { showCurrent.value = true; filterDate.value = ''; fetchRates(0) }

function fmtNum(v, dec = 2) { return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: dec, maximumFractionDigits: dec }).format(Number(v) || 0) }
function fmtRate(v) { return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 2, maximumFractionDigits: 6 }).format(Number(v) || 0) }

onMounted(() => { fetchCurrencies(0); fetchActiveCurrencies() })
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.currencies.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.currencies.subtitle') }}</p>
      </div>
      <button @click="activeTab === 'currencies' ? openCurrModal() : openRateModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ activeTab === 'currencies' ? $t('finance.currencies.add') : $t('finance.exchangeRates.add') }}
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

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex space-x-4">
        <button v-for="tab in [{ key: 'currencies', label: $t('finance.currencies.tabCurrencies') }, { key: 'rates', label: $t('finance.exchangeRates.tabRates') }]"
          :key="tab.key" @click="switchTab(tab.key)"
          :class="['px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors', activeTab === tab.key ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300']">
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- ========== TAB 1: CURRENCIES ========== -->
    <template v-if="activeTab === 'currencies'">
      <div class="card">
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="currencies.length === 0" class="text-center py-12">
          <p class="text-gray-500">{{ $t('finance.currencies.noCurrencies') }}</p>
        </div>
        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('finance.currencies.code') }}</th>
                <th>{{ $t('finance.currencies.name') }}</th>
                <th>{{ $t('finance.currencies.symbol') }}</th>
                <th>{{ $t('finance.currencies.decimalPlaces') }}</th>
                <th>{{ $t('finance.currencies.base') }}</th>
                <th>{{ $t('status') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="c in currencies" :key="c.id">
                <td><code class="font-mono font-medium text-sm">{{ c.code }}</code></td>
                <td class="text-sm">{{ c.name }}</td>
                <td class="text-sm">{{ c.symbol || '-' }}</td>
                <td class="text-sm">{{ c.decimalPlaces }}</td>
                <td>
                  <span v-if="c.baseCurrency" class="badge badge-warning text-xs"><StarIcon class="h-3 w-3 inline mr-1" />{{ $t('finance.currencies.baseBadge') }}</span>
                </td>
                <td>
                  <button @click="toggleCurrStatus(c)" :class="['badge cursor-pointer text-xs', c.active ? 'badge-success' : 'badge-danger']">
                    {{ c.active ? $t('active') : $t('inactive') }}
                  </button>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button v-if="!c.baseCurrency" @click="setBaseCurrency(c)" class="p-2 text-gray-400 hover:text-yellow-500 rounded-lg hover:bg-gray-100" :title="$t('finance.currencies.setBase')">
                      <StarIcon class="h-5 w-5" />
                    </button>
                    <button @click="openCurrModal(c)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100" :title="$t('edit')">
                      <PencilIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="currTotalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
          <p class="text-sm text-gray-500">{{ $t('page') }} {{ currPage + 1 }} / {{ currTotalPages }}</p>
          <div class="flex gap-2">
            <button @click="fetchCurrencies(currPage - 1)" :disabled="currPage === 0" class="btn-secondary text-sm">{{ $t('previous') }}</button>
            <button @click="fetchCurrencies(currPage + 1)" :disabled="currPage >= currTotalPages - 1" class="btn-secondary text-sm">{{ $t('next') }}</button>
          </div>
        </div>
      </div>
    </template>

    <!-- ========== TAB 2: EXCHANGE RATES ========== -->
    <template v-else>
      <!-- Filters & Converter -->
      <div class="card">
        <div class="card-body">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <!-- Date filter -->
            <div>
              <h3 class="text-sm font-medium text-gray-700 mb-2">{{ $t('finance.exchangeRates.filterByDate') }}</h3>
              <div class="flex gap-2">
                <input v-model="filterDate" type="date" class="input flex-1" />
                <button @click="applyRateFilter" class="btn-secondary text-sm">{{ $t('finance.exchangeRates.filter') }}</button>
                <button @click="showCurrentRates" class="btn-secondary text-sm">{{ $t('finance.exchangeRates.currentRates') }}</button>
              </div>
            </div>
            <!-- Converter -->
            <div>
              <h3 class="text-sm font-medium text-gray-700 mb-2"><ArrowsRightLeftIcon class="h-4 w-4 inline mr-1" />{{ $t('finance.exchangeRates.converter') }}</h3>
              <div class="flex gap-2 items-end flex-wrap">
                <input v-model.number="conv.amount" type="number" step="0.01" class="input w-24" :placeholder="$t('finance.exchangeRates.amount')" />
                <select v-model="conv.from" class="input w-28">
                  <option value="">{{ $t('finance.exchangeRates.from') }}</option>
                  <option v-for="c in activeCurrencies" :key="c.code" :value="c.code">{{ c.code }}</option>
                </select>
                <select v-model="conv.to" class="input w-28">
                  <option value="">{{ $t('finance.exchangeRates.to') }}</option>
                  <option v-for="c in activeCurrencies" :key="c.code" :value="c.code">{{ c.code }}</option>
                </select>
                <input v-model="conv.date" type="date" class="input w-36" />
                <button @click="convertCurrency" :disabled="conv.loading" class="btn-primary text-sm">{{ $t('finance.exchangeRates.convert') }}</button>
              </div>
              <p v-if="conv.result !== null" class="mt-2 text-sm font-semibold text-primary-600">
                {{ fmtNum(conv.amount) }} {{ conv.from }} = {{ fmtRate(conv.result) }} {{ conv.to }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Rates Table -->
      <div class="card">
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="rates.length === 0" class="text-center py-12">
          <ArrowsRightLeftIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
          <p class="text-gray-500">{{ $t('finance.exchangeRates.noRates') }}</p>
        </div>
        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('finance.exchangeRates.fromCurrency') }}</th>
                <th>{{ $t('finance.exchangeRates.toCurrency') }}</th>
                <th class="text-right">{{ $t('finance.exchangeRates.rate') }}</th>
                <th>{{ $t('finance.exchangeRates.effectiveDate') }}</th>
                <th>{{ $t('status') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="r in rates" :key="r.id">
                <td><code class="font-mono text-sm">{{ r.fromCurrencyCode }}</code></td>
                <td><code class="font-mono text-sm">{{ r.toCurrencyCode }}</code></td>
                <td class="text-right font-semibold text-sm">{{ fmtRate(r.rate) }}</td>
                <td class="text-sm text-gray-500">{{ r.effectiveDate?.substring(0, 10) }}</td>
                <td>
                  <button @click="toggleRateStatus(r)" :class="['badge cursor-pointer text-xs', r.active ? 'badge-success' : 'badge-danger']">
                    {{ r.active ? $t('active') : $t('inactive') }}
                  </button>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button @click="openRateModal(r)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100" :title="$t('edit')">
                      <PencilIcon class="h-5 w-5" />
                    </button>
                    <button @click="confirmDeleteRate(r)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100" :title="$t('delete')">
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="rateTotalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
          <p class="text-sm text-gray-500">{{ $t('page') }} {{ ratePage + 1 }} / {{ rateTotalPages }}</p>
          <div class="flex gap-2">
            <button @click="fetchRates(ratePage - 1)" :disabled="ratePage === 0" class="btn-secondary text-sm">{{ $t('previous') }}</button>
            <button @click="fetchRates(ratePage + 1)" :disabled="ratePage >= rateTotalPages - 1" class="btn-secondary text-sm">{{ $t('next') }}</button>
          </div>
        </div>
      </div>
    </template>

    <!-- ========== CURRENCY MODAL ========== -->
    <Teleport to="body">
      <div v-if="showCurrModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCurrModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-md w-full p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingCurr ? $t('finance.currencies.edit') : $t('finance.currencies.new') }}
              </h3>
              <button @click="showCurrModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg"><XMarkIcon class="h-5 w-5" /></button>
            </div>
            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.currencies.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="currForm.code" type="text" class="input font-mono" maxlength="3" :disabled="!!editingCurr" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.currencies.symbol') }}</label>
                  <input v-model="currForm.symbol" type="text" class="input" maxlength="5" />
                </div>
              </div>
              <div>
                <label class="label">{{ $t('finance.currencies.name') }} <span class="text-red-500">*</span></label>
                <input v-model="currForm.name" type="text" class="input" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.currencies.decimalPlaces') }}</label>
                  <input v-model.number="currForm.decimalPlaces" type="number" min="0" max="8" class="input" />
                </div>
                <div class="flex items-end pb-1">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input v-model="currForm.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                    <span class="text-sm text-gray-700">{{ $t('active') }}</span>
                  </label>
                </div>
              </div>
            </div>
            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showCurrModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveCurrency" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== EXCHANGE RATE MODAL ========== -->
    <Teleport to="body">
      <div v-if="showRateModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showRateModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-md w-full p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingRate ? $t('finance.exchangeRates.edit') : $t('finance.exchangeRates.new') }}
              </h3>
              <button @click="showRateModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg"><XMarkIcon class="h-5 w-5" /></button>
            </div>
            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.exchangeRates.fromCurrency') }} <span class="text-red-500">*</span></label>
                  <select v-model="rateForm.fromCurrencyCode" class="input">
                    <option value="">--</option>
                    <option v-for="c in activeCurrencies" :key="c.code" :value="c.code">{{ c.code }} - {{ c.name }}</option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('finance.exchangeRates.toCurrency') }} <span class="text-red-500">*</span></label>
                  <select v-model="rateForm.toCurrencyCode" class="input">
                    <option value="">--</option>
                    <option v-for="c in activeCurrencies" :key="c.code" :value="c.code">{{ c.code }} - {{ c.name }}</option>
                  </select>
                </div>
              </div>
              <div>
                <label class="label">{{ $t('finance.exchangeRates.rate') }} <span class="text-red-500">*</span></label>
                <input v-model.number="rateForm.rate" type="number" step="0.000001" class="input" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.exchangeRates.effectiveDate') }} <span class="text-red-500">*</span></label>
                  <input v-model="rateForm.effectiveDate" type="date" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.exchangeRates.expiryDate') }}</label>
                  <input v-model="rateForm.expiryDate" type="date" class="input" />
                </div>
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

    <!-- ========== DELETE CONFIRMATION ========== -->
    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDeleteConfirm = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.exchangeRates.confirmDeleteTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('finance.exchangeRates.confirmDelete', { from: deletingRate?.fromCurrencyCode, to: deletingRate?.toCurrencyCode }) }}
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
