<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { promotionsApi, unwrapData } from '@/services/api'
import { formatDate, formatCurrency } from '@/utils/format'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  XMarkIcon, TagIcon, EyeIcon, ChevronDownIcon, ChevronUpIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const promotions = ref([])
const loading = ref(true)
const error = ref('')
const successMsg = ref('')
const search = ref('')
const activeTab = ref('ALL')
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 20

const showModal = ref(false)
const editingPromotion = ref(null)
const saving = ref(false)

const showDeleteConfirm = ref(false)
const deletingPromotion = ref(null)

const expandedPromotion = ref(null)
const expandedDetail = ref(null)
const detailLoading = ref(false)

const codeLookup = ref('')
const codeLookupResult = ref(null)
const codeLookupError = ref('')
const codeLookupLoading = ref(false)

const showConditionModal = ref(false)
const showActionModal = ref(false)

const promotionTypes = computed(() => [
  { value: 'PERCENTAGE_OFF', label: t('pos.promotions.typePercentageOff') },
  { value: 'FIXED_AMOUNT_OFF', label: t('pos.promotions.typeFixedAmountOff') },
  { value: 'BUY_X_GET_Y', label: t('pos.promotions.typeBuyXGetY') },
  { value: 'BUNDLE', label: t('pos.promotions.typeBundle') },
  { value: 'TIERED_DISCOUNT', label: t('pos.promotions.typeTieredDiscount') },
  { value: 'SPEND_X_GET_Y', label: t('pos.promotions.typeSpendXGetY') },
  { value: 'FREE_ITEM', label: t('pos.promotions.typeFreeItem') }
])

const scopes = computed(() => [
  { value: 'ORDER', label: t('pos.promotions.scopeOrder') },
  { value: 'LINE_ITEM', label: t('pos.promotions.scopeLineItem') },
  { value: 'CATEGORY', label: t('pos.promotions.scopeCategory') },
  { value: 'PRODUCT', label: t('pos.promotions.scopeProduct') }
])

const channels = computed(() => [
  { value: 'POS', label: t('pos.promotions.channelPos') },
  { value: 'WEB', label: t('pos.promotions.channelWeb') },
  { value: 'ALL', label: t('pos.promotions.channelAll') }
])

const conditionTypes = computed(() => [
  { value: 'MINIMUM_PURCHASE', label: t('pos.promotions.condMinPurchase') },
  { value: 'MINIMUM_QUANTITY', label: t('pos.promotions.condMinQuantity') },
  { value: 'SPECIFIC_PRODUCTS', label: t('pos.promotions.condProducts') },
  { value: 'CATEGORY', label: t('pos.promotions.condCategory') },
  { value: 'BRAND', label: t('pos.promotions.condBrand') },
  { value: 'CUSTOMER_GROUP', label: t('pos.promotions.condCustomerGroup') },
  { value: 'FIRST_PURCHASE', label: t('pos.promotions.condFirstPurchase') },
  { value: 'TIME_BASED', label: t('pos.promotions.condTimeBased') },
  { value: 'DAY_OF_WEEK', label: t('pos.promotions.condDayOfWeek') }
])

const actionTypes = computed(() => [
  { value: 'PERCENTAGE_OFF', label: t('pos.promotions.actPercentOff') },
  { value: 'FIXED_AMOUNT_OFF', label: t('pos.promotions.actFixedOff') },
  { value: 'FREE_ITEM', label: t('pos.promotions.actFreeItem') },
  { value: 'SET_PRICE', label: t('pos.promotions.actSetPrice') }
])

const daysOfWeekOptions = [
  { value: 'MON', label: t('pos.promotions.mon') },
  { value: 'TUE', label: t('pos.promotions.tue') },
  { value: 'WED', label: t('pos.promotions.wed') },
  { value: 'THU', label: t('pos.promotions.thu') },
  { value: 'FRI', label: t('pos.promotions.fri') },
  { value: 'SAT', label: t('pos.promotions.sat') },
  { value: 'SUN', label: t('pos.promotions.sun') }
]

function defaultForm() {
  return {
    code: '', name: '', description: '',
    type: 'PERCENTAGE_OFF', scope: 'ORDER', channel: 'POS',
    priority: 0, discountValue: null, maxDiscountAmount: null,
    buyQuantity: null, getQuantity: null, getDiscountPercent: null,
    startDate: '', endDate: '', startTime: '', endTime: '',
    daysOfWeek: '',
    active: true, stackable: false, requiresCoupon: false,
    maxUses: null, maxUsesPerCustomer: null, minOrderAmount: null,
    notes: ''
  }
}
const form = reactive(defaultForm())

function defaultConditionForm() {
  return {
    conditionType: 'MINIMUM_PURCHASE', operator: 'GTE', value: '',
    thresholdAmount: null, productIds: '', categoryIds: '', brandIds: '',
    customerGroups: '', required: true
  }
}
const conditionForm = reactive(defaultConditionForm())

function defaultActionForm() {
  return {
    actionType: 'PERCENTAGE_OFF', discountPercent: null, discountAmount: null,
    setPrice: null, maxDiscount: null, freeProductId: null, freeQuantity: 1,
    targetProductIds: '', targetCategoryIds: '', applyTo: 'ALL', applyCount: null,
    sortOrder: 0
  }
}
const actionForm = reactive(defaultActionForm())

const isBogo = computed(() => form.type === 'BUY_X_GET_Y')
const isBundle = computed(() => form.type === 'BUNDLE')

function channelLabel(ch) {
  return channels.value.find(c => c.value === ch)?.label || ch || t('pos.promotions.channelPos')
}
function typeLabel(tp) {
  return promotionTypes.value.find(c => c.value === tp)?.label || tp
}
function condTypeLabel(ct) {
  return conditionTypes.value.find(c => c.value === ct)?.label || ct
}
function actTypeLabel(at) {
  return actionTypes.value.find(a => a.value === at)?.label || at
}

// ---------- Data fetching ----------

async function fetchPromotions(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (search.value.trim()) {
      res = await promotionsApi.search(search.value.trim(), { page, size: pageSize, sort: 'priority,asc' })
    } else if (activeTab.value === 'ACTIVE') {
      res = await promotionsApi.getActive()
    } else {
      res = await promotionsApi.getAll({ page, size: pageSize, sort: 'priority,asc' })
    }
    const data = unwrapData(res)
    if (Array.isArray(data)) {
      promotions.value = data; totalPages.value = 1; currentPage.value = 0
    } else {
      promotions.value = data.content || []
      totalPages.value = data.page?.totalPages || data.totalPages || 1
      currentPage.value = data.page?.number ?? data.number ?? 0
    }
  } catch (e) {
    if (e.response?.status !== 403) error.value = e.response?.data?.message || t('failedToLoad')
  } finally { loading.value = false }
}

onMounted(() => fetchPromotions(0))

function switchTab(tab) { activeTab.value = tab; search.value = ''; fetchPromotions(0) }
function handleSearch() { activeTab.value = 'ALL'; fetchPromotions(0) }

// ---------- Code lookup ----------
async function lookupByCode() {
  if (!codeLookup.value.trim()) return
  codeLookupLoading.value = true; codeLookupError.value = ''; codeLookupResult.value = null
  try { codeLookupResult.value = unwrapData(await promotionsApi.getByCode(codeLookup.value.trim()))
  } catch (e) { codeLookupError.value = e.response?.status === 404 ? t('pos.promotions.codeNotFound') : (e.response?.data?.message || t('errorOccurred'))
  } finally { codeLookupLoading.value = false }
}
function clearCodeLookup() { codeLookup.value = ''; codeLookupResult.value = null; codeLookupError.value = '' }

// ---------- CRUD ----------

function openModal(promotion = null) {
  editingPromotion.value = promotion
  const f = promotion || defaultForm()
  Object.keys(defaultForm()).forEach(k => {
    form[k] = f[k] ?? defaultForm()[k]
  })
  if (promotion) {
    form.active = promotion.active !== false
    form.startDate = promotion.startDate || ''
    form.endDate = promotion.endDate || ''
    form.startTime = promotion.startTime || ''
    form.endTime = promotion.endTime || ''
  }
  showModal.value = true
}

async function savePromotion() {
  if (!form.code?.trim() || !form.name?.trim()) return
  saving.value = true; error.value = ''
  try {
    const payload = { ...form }
    if (!payload.daysOfWeek) delete payload.daysOfWeek
    if (!payload.startTime) delete payload.startTime
    if (!payload.endTime) delete payload.endTime
    Object.keys(payload).forEach(k => { if (payload[k] === '' || payload[k] === null) delete payload[k] })
    payload.active = form.active
    payload.stackable = form.stackable
    payload.requiresCoupon = form.requiresCoupon
    if (editingPromotion.value) {
      await promotionsApi.update(editingPromotion.value.id, payload)
      successMsg.value = t('pos.promotions.updateSuccess')
    } else {
      await promotionsApi.create(payload)
      successMsg.value = t('pos.promotions.createSuccess')
    }
    showModal.value = false
    fetchPromotions(currentPage.value)
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred')
  } finally { saving.value = false }
}

async function toggleStatus(promotion) {
  error.value = ''
  try {
    if (promotion.active) await promotionsApi.deactivate(promotion.id)
    else await promotionsApi.activate(promotion.id)
    fetchPromotions(currentPage.value)
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

function confirmDelete(promotion) { deletingPromotion.value = promotion; showDeleteConfirm.value = true }

async function deletePromotion() {
  if (!deletingPromotion.value) return
  error.value = ''
  try {
    await promotionsApi.delete(deletingPromotion.value.id)
    successMsg.value = t('pos.promotions.deleteSuccess')
    showDeleteConfirm.value = false; deletingPromotion.value = null
    if (expandedPromotion.value === deletingPromotion.value?.id) { expandedPromotion.value = null; expandedDetail.value = null }
    fetchPromotions(currentPage.value)
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred'); showDeleteConfirm.value = false }
}

// ---------- Detail expand ----------

async function toggleDetail(promotion) {
  if (expandedPromotion.value === promotion.id) { expandedPromotion.value = null; expandedDetail.value = null; return }
  expandedPromotion.value = promotion.id; detailLoading.value = true
  try { expandedDetail.value = unwrapData(await promotionsApi.getById(promotion.id))
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred'); expandedPromotion.value = null
  } finally { detailLoading.value = false }
}

async function reloadDetail() {
  if (!expandedPromotion.value) return
  try { expandedDetail.value = unwrapData(await promotionsApi.getById(expandedPromotion.value))
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

// ---------- Conditions ----------

function openConditionModal() { Object.assign(conditionForm, defaultConditionForm()); showConditionModal.value = true }

async function addCondition() {
  if (!conditionForm.conditionType) return
  error.value = ''
  try {
    await promotionsApi.addCondition(expandedPromotion.value, { ...conditionForm })
    successMsg.value = t('pos.promotions.conditionAdded')
    showConditionModal.value = false
    reloadDetail()
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

async function removeCondition(conditionId) {
  error.value = ''
  try {
    await promotionsApi.removeCondition(expandedPromotion.value, conditionId)
    successMsg.value = t('pos.promotions.conditionRemoved')
    reloadDetail()
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

// ---------- Actions ----------

function openActionModal() { Object.assign(actionForm, defaultActionForm()); showActionModal.value = true }

async function addAction() {
  if (!actionForm.actionType) return
  error.value = ''
  try {
    const payload = { ...actionForm }
    Object.keys(payload).forEach(k => { if (payload[k] === null || payload[k] === '') delete payload[k] })
    await promotionsApi.addAction(expandedPromotion.value, payload)
    successMsg.value = t('pos.promotions.actionAdded')
    showActionModal.value = false
    reloadDetail()
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

async function removeAction(actionId) {
  error.value = ''
  try {
    await promotionsApi.removeAction(expandedPromotion.value, actionId)
    successMsg.value = t('pos.promotions.actionRemoved')
    reloadDetail()
  } catch (e) { error.value = e.response?.data?.message || t('errorOccurred') }
}

// ---------- Helpers ----------

function formatDiscountSummary(p) {
  if (p.type === 'PERCENTAGE_OFF' && p.discountValue) return `${p.discountValue}%`
  if (p.type === 'FIXED_AMOUNT_OFF' && p.discountValue) return formatCurrency(p.discountValue)
  if (p.type === 'BUY_X_GET_Y') return `${p.buyQuantity || '?'}+${p.getQuantity || '?'}`
  return typeLabel(p.type)
}

const tabs = computed(() => [
  { key: 'ALL', label: t('pos.promotions.filterAll') },
  { key: 'ACTIVE', label: t('pos.promotions.filterActive') }
])
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('pos.promotions.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('pos.promotions.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('pos.promotions.addPromotion') }}
      </button>
    </div>

    <!-- Alerts -->
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
        <button v-for="tab in tabs" :key="tab.key" @click="switchTab(tab.key)"
          :class="['px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors',
            activeTab === tab.key ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300']">
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Search & Code Lookup -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="card">
        <div class="card-body">
          <label class="text-sm font-medium text-gray-700 mb-2 block">{{ $t('search') }}</label>
          <div class="relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input v-model="search" @keyup.enter="handleSearch" type="text" :placeholder="$t('pos.promotions.searchPlaceholder')" class="input pl-10" />
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-body">
          <label class="text-sm font-medium text-gray-700 mb-2 block">{{ $t('pos.promotions.codeLookup') }}</label>
          <div class="flex gap-2">
            <input v-model="codeLookup" @keyup.enter="lookupByCode" type="text" :placeholder="$t('pos.promotions.codeLookupPlaceholder')" class="input flex-1 font-mono" />
            <button @click="lookupByCode" :disabled="codeLookupLoading" class="btn-primary text-sm">{{ $t('search') }}</button>
            <button v-if="codeLookupResult || codeLookupError" @click="clearCodeLookup" class="btn-secondary text-sm">{{ $t('clear') }}</button>
          </div>
          <div v-if="codeLookupLoading" class="mt-2 text-sm text-gray-500">{{ $t('loading') }}</div>
          <div v-if="codeLookupError" class="mt-2 text-sm text-red-600">{{ codeLookupError }}</div>
          <div v-if="codeLookupResult" class="mt-2 p-2 bg-green-50 border border-green-200 rounded-lg text-sm">
            <p class="font-medium">{{ codeLookupResult.name }}</p>
            <p class="text-gray-500">{{ codeLookupResult.code }} | {{ channelLabel(codeLookupResult.channel) }} | {{ codeLookupResult.active ? $t('active') : $t('inactive') }}</p>
            <div class="mt-1 flex gap-2">
              <button @click="toggleDetail(codeLookupResult)" class="text-sm text-primary-600 hover:underline">{{ $t('pos.promotions.viewDetails') }}</button>
              <button @click="openModal(codeLookupResult)" class="text-sm text-primary-600 hover:underline">{{ $t('edit') }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
      <div v-else-if="promotions.length === 0" class="text-center py-12">
        <TagIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('pos.promotions.noPromotions') }}</p>
      </div>
      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th></th>
              <th>{{ $t('pos.promotions.code') }}</th>
              <th>{{ $t('pos.promotions.name') }}</th>
              <th>{{ $t('pos.promotions.type') }}</th>
              <th>{{ $t('pos.promotions.channel') }}</th>
              <th>{{ $t('pos.promotions.discount') }}</th>
              <th>{{ $t('pos.promotions.dates') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <template v-for="promotion in promotions" :key="promotion.id">
              <tr>
                <td>
                  <button @click="toggleDetail(promotion)" class="p-1 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <ChevronDownIcon v-if="expandedPromotion !== promotion.id" class="h-4 w-4" />
                    <ChevronUpIcon v-else class="h-4 w-4" />
                  </button>
                </td>
                <td><code class="font-mono font-medium text-sm">{{ promotion.code }}</code></td>
                <td>
                  <p class="font-medium text-sm">{{ promotion.name }}</p>
                  <p v-if="promotion.description" class="text-xs text-gray-400 truncate max-w-xs">{{ promotion.description }}</p>
                </td>
                <td><span class="text-xs">{{ typeLabel(promotion.type) }}</span></td>
                <td>
                  <span :class="['badge text-xs', promotion.channel === 'WEB' ? 'badge-info' : promotion.channel === 'ALL' ? 'badge-warning' : 'badge-secondary']">
                    {{ channelLabel(promotion.channel) }}
                  </span>
                </td>
                <td class="text-sm font-medium">{{ formatDiscountSummary(promotion) }}</td>
                <td class="text-xs text-gray-500">
                  <span v-if="promotion.startDate || promotion.endDate">{{ formatDate(promotion.startDate) }} — {{ formatDate(promotion.endDate) }}</span>
                  <span v-else>-</span>
                </td>
                <td>
                  <button @click="toggleStatus(promotion)" :class="['badge cursor-pointer text-xs', promotion.active ? 'badge-success' : 'badge-danger']">
                    {{ promotion.active ? $t('active') : $t('inactive') }}
                  </button>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button @click="openModal(promotion)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100" :title="$t('edit')">
                      <PencilIcon class="h-5 w-5" />
                    </button>
                    <button @click="confirmDelete(promotion)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100" :title="$t('delete')">
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Expanded Detail -->
              <tr v-if="expandedPromotion === promotion.id">
                <td colspan="9" class="bg-gray-50 px-6 py-4">
                  <div v-if="detailLoading" class="flex items-center justify-center py-8">
                    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                  </div>
                  <div v-else-if="expandedDetail" class="space-y-6">
                    <!-- Info grid -->
                    <div>
                      <h4 class="text-sm font-semibold text-gray-900 mb-3">{{ $t('pos.promotions.promotionInfo') }}</h4>
                      <div class="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                        <div><span class="text-gray-500">{{ $t('pos.promotions.type') }}:</span> <span class="ml-1 font-medium">{{ typeLabel(expandedDetail.type) }}</span></div>
                        <div><span class="text-gray-500">{{ $t('pos.promotions.scope') }}:</span> <span class="ml-1">{{ expandedDetail.scope || 'ORDER' }}</span></div>
                        <div><span class="text-gray-500">{{ $t('pos.promotions.channel') }}:</span> <span class="ml-1">{{ channelLabel(expandedDetail.channel) }}</span></div>
                        <div><span class="text-gray-500">{{ $t('pos.promotions.priority') }}:</span> <span class="ml-1">{{ expandedDetail.priority }}</span></div>
                        <div v-if="expandedDetail.discountValue"><span class="text-gray-500">{{ $t('pos.promotions.discountValue') }}:</span> <span class="ml-1 font-medium">{{ expandedDetail.discountValue }}</span></div>
                        <div v-if="expandedDetail.maxDiscountAmount"><span class="text-gray-500">{{ $t('pos.promotions.maxDiscountAmount') }}:</span> <span class="ml-1">{{ formatCurrency(expandedDetail.maxDiscountAmount) }}</span></div>
                        <div v-if="expandedDetail.minOrderAmount"><span class="text-gray-500">{{ $t('pos.promotions.minOrderAmount') }}:</span> <span class="ml-1">{{ formatCurrency(expandedDetail.minOrderAmount) }}</span></div>
                        <div v-if="expandedDetail.maxUses"><span class="text-gray-500">{{ $t('pos.promotions.maxUses') }}:</span> <span class="ml-1">{{ expandedDetail.currentUses || 0 }} / {{ expandedDetail.maxUses }}</span></div>
                        <div v-if="expandedDetail.maxUsesPerCustomer"><span class="text-gray-500">{{ $t('pos.promotions.maxUsesPerCustomer') }}:</span> <span class="ml-1">{{ expandedDetail.maxUsesPerCustomer }}</span></div>
                        <div><span class="text-gray-500">{{ $t('pos.promotions.stackable') }}:</span> <span class="ml-1">{{ expandedDetail.stackable ? $t('yes') : $t('no') }}</span></div>
                        <div><span class="text-gray-500">{{ $t('pos.promotions.requiresCoupon') }}:</span> <span class="ml-1">{{ expandedDetail.requiresCoupon ? $t('yes') : $t('no') }}</span></div>
                        <div v-if="expandedDetail.couponCount"><span class="text-gray-500">{{ $t('pos.promotions.couponCount') }}:</span> <span class="ml-1">{{ expandedDetail.couponCount }}</span></div>
                        <div v-if="expandedDetail.buyQuantity"><span class="text-gray-500">{{ $t('pos.promotions.buyQuantity') }}:</span> <span class="ml-1">{{ expandedDetail.buyQuantity }}</span></div>
                        <div v-if="expandedDetail.getQuantity"><span class="text-gray-500">{{ $t('pos.promotions.getQuantity') }}:</span> <span class="ml-1">{{ expandedDetail.getQuantity }} ({{ expandedDetail.getDiscountPercent }}% {{ $t('pos.promotions.off') }})</span></div>
                        <div v-if="expandedDetail.daysOfWeek"><span class="text-gray-500">{{ $t('pos.promotions.daysOfWeek') }}:</span> <span class="ml-1">{{ expandedDetail.daysOfWeek }}</span></div>
                        <div v-if="expandedDetail.startTime"><span class="text-gray-500">{{ $t('pos.promotions.timeRange') }}:</span> <span class="ml-1">{{ expandedDetail.startTime }} — {{ expandedDetail.endTime }}</span></div>
                      </div>
                      <p v-if="expandedDetail.description" class="mt-2 text-sm text-gray-500">{{ expandedDetail.description }}</p>
                    </div>

                    <!-- Conditions -->
                    <div>
                      <div class="flex items-center justify-between mb-2">
                        <h4 class="text-sm font-semibold text-gray-900">{{ $t('pos.promotions.conditions') }}</h4>
                        <button @click="openConditionModal" class="btn-secondary text-xs"><PlusIcon class="h-4 w-4 mr-1" />{{ $t('pos.promotions.addCondition') }}</button>
                      </div>
                      <div v-if="expandedDetail.conditions?.length" class="space-y-2">
                        <div v-for="c in expandedDetail.conditions" :key="c.id" class="flex items-center justify-between p-2 bg-white border border-gray-200 rounded-lg">
                          <div class="text-sm">
                            <span class="font-medium">{{ condTypeLabel(c.conditionType) }}</span>
                            <span v-if="c.thresholdAmount" class="text-gray-500 ml-2">{{ c.operator || 'GTE' }} {{ formatCurrency(c.thresholdAmount) }}</span>
                            <span v-else-if="c.value" class="text-gray-500 ml-2">{{ c.operator }} {{ c.value }}</span>
                            <span v-if="c.productIds" class="text-gray-400 ml-2 text-xs">({{ $t('pos.promotions.products') }}: {{ c.productIds }})</span>
                            <span v-if="c.categoryIds" class="text-gray-400 ml-2 text-xs">({{ $t('pos.promotions.categories') }}: {{ c.categoryIds }})</span>
                            <span v-if="c.brandIds" class="text-gray-400 ml-2 text-xs">({{ $t('pos.promotions.brands') }}: {{ c.brandIds }})</span>
                          </div>
                          <button @click="removeCondition(c.id)" class="p-1 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"><TrashIcon class="h-4 w-4" /></button>
                        </div>
                      </div>
                      <p v-else class="text-sm text-gray-400">{{ $t('pos.promotions.noConditions') }}</p>
                    </div>

                    <!-- Actions -->
                    <div>
                      <div class="flex items-center justify-between mb-2">
                        <h4 class="text-sm font-semibold text-gray-900">{{ $t('pos.promotions.actions') }}</h4>
                        <button @click="openActionModal" class="btn-secondary text-xs"><PlusIcon class="h-4 w-4 mr-1" />{{ $t('pos.promotions.addAction') }}</button>
                      </div>
                      <div v-if="expandedDetail.actions?.length" class="space-y-2">
                        <div v-for="a in expandedDetail.actions" :key="a.id" class="flex items-center justify-between p-2 bg-white border border-gray-200 rounded-lg">
                          <div class="text-sm">
                            <span class="font-medium">{{ actTypeLabel(a.actionType) }}</span>
                            <span v-if="a.discountPercent" class="text-gray-500 ml-2">{{ a.discountPercent }}%</span>
                            <span v-if="a.discountAmount" class="text-gray-500 ml-2">{{ formatCurrency(a.discountAmount) }}</span>
                            <span v-if="a.setPrice" class="text-gray-500 ml-2">= {{ formatCurrency(a.setPrice) }}</span>
                            <span v-if="a.freeProductId" class="text-gray-400 ml-2 text-xs">(#{{ a.freeProductId }} x{{ a.freeQuantity || 1 }})</span>
                            <span v-if="a.targetProductIds" class="text-gray-400 ml-2 text-xs">({{ $t('pos.promotions.targets') }}: {{ a.targetProductIds }})</span>
                            <span v-if="a.applyTo && a.applyTo !== 'ALL'" class="text-gray-400 ml-1 text-xs">[{{ a.applyTo }}]</span>
                          </div>
                          <button @click="removeAction(a.id)" class="p-1 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"><TrashIcon class="h-4 w-4" /></button>
                        </div>
                      </div>
                      <p v-else class="text-sm text-gray-400">{{ $t('pos.promotions.noActions') }}</p>
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
          <button @click="fetchPromotions(currentPage - 1)" :disabled="currentPage === 0" class="btn-secondary text-sm">{{ $t('previous') }}</button>
          <button @click="fetchPromotions(currentPage + 1)" :disabled="currentPage >= totalPages - 1" class="btn-secondary text-sm">{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- ========== Create/Edit Promotion Modal ========== -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-2xl w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">{{ editingPromotion ? $t('pos.promotions.editPromotion') : $t('pos.promotions.newPromotion') }}</h3>
              <button @click="showModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg"><XMarkIcon class="h-5 w-5" /></button>
            </div>

            <div class="space-y-5">
              <!-- Row 1: Code, Name -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="form.code" type="text" class="input font-mono" :placeholder="$t('pos.promotions.codePlaceholder')" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.name') }} <span class="text-red-500">*</span></label>
                  <input v-model="form.name" type="text" class="input" :placeholder="$t('pos.promotions.namePlaceholder')" />
                </div>
              </div>

              <!-- Description -->
              <div>
                <label class="label">{{ $t('description') }}</label>
                <textarea v-model="form.description" rows="2" class="input" :placeholder="$t('pos.promotions.descriptionPlaceholder')"></textarea>
              </div>

              <!-- Row 2: Type, Scope, Channel -->
              <div class="grid grid-cols-3 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.type') }} <span class="text-red-500">*</span></label>
                  <select v-model="form.type" class="input">
                    <option v-for="tp in promotionTypes" :key="tp.value" :value="tp.value">{{ tp.label }}</option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.scope') }}</label>
                  <select v-model="form.scope" class="input">
                    <option v-for="s in scopes" :key="s.value" :value="s.value">{{ s.label }}</option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.channel') }}</label>
                  <select v-model="form.channel" class="input">
                    <option v-for="ch in channels" :key="ch.value" :value="ch.value">{{ ch.label }}</option>
                  </select>
                </div>
              </div>

              <!-- Row 3: Discount value, Max discount, Priority -->
              <div class="grid grid-cols-3 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.discountValue') }}</label>
                  <input v-model.number="form.discountValue" type="number" min="0" step="0.01" class="input" />
                  <p class="mt-1 text-xs text-gray-400">{{ $t('pos.promotions.discountValueHint') }}</p>
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.maxDiscountAmount') }}</label>
                  <input v-model.number="form.maxDiscountAmount" type="number" min="0" step="1" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.priority') }}</label>
                  <input v-model.number="form.priority" type="number" class="input" />
                </div>
              </div>

              <!-- BOGO fields -->
              <div v-if="isBogo" class="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <p class="text-sm font-medium text-blue-800 mb-3">{{ $t('pos.promotions.bogoSettings') }}</p>
                <div class="grid grid-cols-3 gap-4">
                  <div>
                    <label class="label">{{ $t('pos.promotions.buyQuantity') }}</label>
                    <input v-model.number="form.buyQuantity" type="number" min="1" class="input" />
                  </div>
                  <div>
                    <label class="label">{{ $t('pos.promotions.getQuantity') }}</label>
                    <input v-model.number="form.getQuantity" type="number" min="1" class="input" />
                  </div>
                  <div>
                    <label class="label">{{ $t('pos.promotions.getDiscountPercent') }}</label>
                    <input v-model.number="form.getDiscountPercent" type="number" min="0" max="100" class="input" />
                    <p class="mt-1 text-xs text-gray-400">100 = {{ $t('pos.promotions.free') }}</p>
                  </div>
                </div>
              </div>

              <!-- Min order, Max uses -->
              <div class="grid grid-cols-3 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.minOrderAmount') }}</label>
                  <input v-model.number="form.minOrderAmount" type="number" min="0" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.maxUses') }}</label>
                  <input v-model.number="form.maxUses" type="number" min="1" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.maxUsesPerCustomer') }}</label>
                  <input v-model.number="form.maxUsesPerCustomer" type="number" min="1" class="input" />
                </div>
              </div>

              <!-- Dates -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.startDate') }}</label>
                  <input v-model="form.startDate" type="date" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.endDate') }}</label>
                  <input v-model="form.endDate" type="date" class="input" />
                </div>
              </div>

              <!-- Time range -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.startTime') }}</label>
                  <input v-model="form.startTime" type="time" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.endTime') }}</label>
                  <input v-model="form.endTime" type="time" class="input" />
                </div>
              </div>

              <!-- Days of week -->
              <div>
                <label class="label">{{ $t('pos.promotions.daysOfWeek') }}</label>
                <div class="flex flex-wrap gap-2 mt-1">
                  <label v-for="day in daysOfWeekOptions" :key="day.value" class="flex items-center gap-1.5 cursor-pointer">
                    <input type="checkbox" :value="day.value" :checked="form.daysOfWeek?.includes(day.value)"
                      @change="e => {
                        const days = form.daysOfWeek ? form.daysOfWeek.split(',').filter(Boolean) : []
                        if (e.target.checked) days.push(day.value); else days.splice(days.indexOf(day.value), 1)
                        form.daysOfWeek = days.join(',')
                      }" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                    <span class="text-sm text-gray-700">{{ day.label }}</span>
                  </label>
                </div>
              </div>

              <!-- Toggles -->
              <div class="flex flex-wrap gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('active') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.stackable" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('pos.promotions.stackable') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.requiresCoupon" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('pos.promotions.requiresCoupon') }}</span>
                </label>
              </div>

              <!-- Notes -->
              <div>
                <label class="label">{{ $t('pos.promotions.notes') }}</label>
                <textarea v-model="form.notes" rows="2" class="input"></textarea>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="savePromotion" :disabled="saving" class="btn-primary">{{ saving ? $t('saving') : $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== Add Condition Modal ========== -->
    <Teleport to="body">
      <div v-if="showConditionModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showConditionModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">{{ $t('pos.promotions.addCondition') }}</h3>
              <button @click="showConditionModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg"><XMarkIcon class="h-5 w-5" /></button>
            </div>
            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('pos.promotions.conditionType') }}</label>
                <select v-model="conditionForm.conditionType" class="input">
                  <option v-for="ct in conditionTypes" :key="ct.value" :value="ct.value">{{ ct.label }}</option>
                </select>
              </div>
              <div v-if="['MINIMUM_PURCHASE', 'MINIMUM_QUANTITY'].includes(conditionForm.conditionType)" class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.operator') }}</label>
                  <select v-model="conditionForm.operator" class="input">
                    <option value="GTE">>=</option><option value="GT">></option>
                    <option value="EQ">=</option><option value="LTE"><=</option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.threshold') }}</label>
                  <input v-model.number="conditionForm.thresholdAmount" type="number" min="0" class="input" />
                </div>
              </div>
              <div v-if="conditionForm.conditionType === 'SPECIFIC_PRODUCTS'">
                <label class="label">{{ $t('pos.promotions.productIds') }}</label>
                <input v-model="conditionForm.productIds" type="text" class="input" placeholder="1,2,3" />
                <p class="mt-1 text-xs text-gray-400">{{ $t('pos.promotions.commaSeparatedIds') }}</p>
              </div>
              <div v-if="conditionForm.conditionType === 'CATEGORY'">
                <label class="label">{{ $t('pos.promotions.categoryIds') }}</label>
                <input v-model="conditionForm.categoryIds" type="text" class="input" placeholder="1,2" />
              </div>
              <div v-if="conditionForm.conditionType === 'BRAND'">
                <label class="label">{{ $t('pos.promotions.brandIds') }}</label>
                <input v-model="conditionForm.brandIds" type="text" class="input" placeholder="1,2" />
              </div>
              <div v-if="conditionForm.conditionType === 'CUSTOMER_GROUP'">
                <label class="label">{{ $t('pos.promotions.customerGroups') }}</label>
                <input v-model="conditionForm.customerGroups" type="text" class="input" placeholder="VIP,WHOLESALE" />
              </div>
              <div v-if="['TIME_BASED', 'DAY_OF_WEEK'].includes(conditionForm.conditionType)">
                <label class="label">{{ $t('pos.promotions.condValue') }}</label>
                <input v-model="conditionForm.value" type="text" class="input" :placeholder="conditionForm.conditionType === 'DAY_OF_WEEK' ? 'MON,TUE,WED' : '09:00-18:00'" />
              </div>
              <label class="flex items-center gap-2 cursor-pointer">
                <input v-model="conditionForm.required" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                <span class="text-sm text-gray-700">{{ $t('pos.promotions.condRequired') }}</span>
              </label>
            </div>
            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showConditionModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="addCondition" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== Add Action Modal ========== -->
    <Teleport to="body">
      <div v-if="showActionModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showActionModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">{{ $t('pos.promotions.addAction') }}</h3>
              <button @click="showActionModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg"><XMarkIcon class="h-5 w-5" /></button>
            </div>
            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('pos.promotions.actionType') }}</label>
                <select v-model="actionForm.actionType" class="input">
                  <option v-for="at in actionTypes" :key="at.value" :value="at.value">{{ at.label }}</option>
                </select>
              </div>
              <div v-if="actionForm.actionType === 'PERCENTAGE_OFF'" class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.discountPercent') }}</label>
                  <input v-model.number="actionForm.discountPercent" type="number" min="0" max="100" step="0.01" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.maxDiscount') }}</label>
                  <input v-model.number="actionForm.maxDiscount" type="number" min="0" class="input" />
                </div>
              </div>
              <div v-if="actionForm.actionType === 'FIXED_AMOUNT_OFF'">
                <label class="label">{{ $t('pos.promotions.discountAmount') }}</label>
                <input v-model.number="actionForm.discountAmount" type="number" min="0" class="input" />
              </div>
              <div v-if="actionForm.actionType === 'SET_PRICE'">
                <label class="label">{{ $t('pos.promotions.setPrice') }}</label>
                <input v-model.number="actionForm.setPrice" type="number" min="0" class="input" />
              </div>
              <div v-if="actionForm.actionType === 'FREE_ITEM'" class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.freeProductId') }}</label>
                  <input v-model.number="actionForm.freeProductId" type="number" min="1" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.freeQuantity') }}</label>
                  <input v-model.number="actionForm.freeQuantity" type="number" min="1" class="input" />
                </div>
              </div>
              <div>
                <label class="label">{{ $t('pos.promotions.targetProductIds') }}</label>
                <input v-model="actionForm.targetProductIds" type="text" class="input" placeholder="1,2,3" />
                <p class="mt-1 text-xs text-gray-400">{{ $t('pos.promotions.commaSeparatedIds') }}</p>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.applyTo') }}</label>
                  <select v-model="actionForm.applyTo" class="input">
                    <option value="ALL">{{ $t('pos.promotions.applyAll') }}</option>
                    <option value="CHEAPEST">{{ $t('pos.promotions.applyCheapest') }}</option>
                    <option value="MOST_EXPENSIVE">{{ $t('pos.promotions.applyExpensive') }}</option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.applyCount') }}</label>
                  <input v-model.number="actionForm.applyCount" type="number" min="1" class="input" />
                </div>
              </div>
            </div>
            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showActionModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="addAction" class="btn-primary">{{ $t('save') }}</button>
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
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('pos.promotions.confirmDeleteTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">{{ $t('pos.promotions.confirmDelete', { name: deletingPromotion?.name }) }}</p>
            <div class="flex justify-end space-x-3">
              <button @click="showDeleteConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="deletePromotion" class="btn-danger">{{ $t('delete') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
