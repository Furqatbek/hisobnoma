<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { couponsApi, promotionsApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  XMarkIcon, TicketIcon, ArrowPathIcon, ChevronDownIcon,
  ChevronUpIcon, NoSymbolIcon, BoltIcon, FunnelIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const coupons = ref([])
const loading = ref(true)
const error = ref('')
const successMsg = ref('')
const search = ref('')
const activeTab = ref('ALL')
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 20

const statusFilters = ['ALL', 'ACTIVE', 'EXPIRED', 'DEPLETED', 'CANCELLED']
const discountTypes = ['PERCENTAGE', 'FIXED']

// Create/Edit Modal state
const showModal = ref(false)
const editingCoupon = ref(null)

const form = reactive({
  code: '',
  promotionId: '',
  discountType: 'PERCENTAGE',
  discountValue: 0,
  maxUses: 1,
  expiryDate: ''
})

// Generate Modal state
const showGenerateModal = ref(false)

const generateForm = reactive({
  promotionId: '',
  count: 1,
  prefix: '',
  discountType: 'PERCENTAGE',
  discountValue: 0,
  maxUses: 1,
  expiryDate: ''
})

// Delete confirmation
const showDeleteConfirm = ref(false)
const deletingCoupon = ref(null)

// Promotion filter
const promotionsList = ref([])
const selectedPromotionFilter = ref('')

// Coupon detail (getById)
const couponDetail = ref(null)
const couponDetailLoading = ref(false)

// Redemption history
const expandedCouponId = ref(null)
const redemptions = ref([])
const loadingRedemptions = ref(false)

async function fetchCoupons(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (activeTab.value !== 'ALL') {
      res = await couponsApi.getByStatus(activeTab.value, { page, size: pageSize, sort: 'id,desc' })
    } else {
      res = await couponsApi.getAll({ page, size: pageSize, sort: 'id,desc' })
    }
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      coupons.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      coupons.value = data.content || []
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

async function fetchPromotionsList() {
  try {
    const res = await promotionsApi.getAll({ size: 100 })
    const data = res.data.data || res.data
    promotionsList.value = data.content || (Array.isArray(data) ? data : [])
  } catch (e) {
    promotionsList.value = []
  }
}

async function filterByPromotion() {
  if (!selectedPromotionFilter.value) {
    fetchCoupons(0)
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await couponsApi.getByPromotion(selectedPromotionFilter.value, { page: 0, size: pageSize, sort: 'id,desc' })
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      coupons.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      coupons.value = data.content || []
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

onMounted(() => {
  fetchCoupons(0)
  fetchPromotionsList()
})

function switchTab(tab) {
  activeTab.value = tab
  search.value = ''
  fetchCoupons(0)
}

async function handleSearch() {
  if (!search.value.trim()) {
    fetchCoupons(0)
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await couponsApi.getByCode(search.value.trim())
    const data = res.data.data || res.data
    coupons.value = data ? [data] : []
    totalPages.value = 1
    currentPage.value = 0
  } catch (e) {
    if (e.response?.status === 404) {
      coupons.value = []
      totalPages.value = 1
      currentPage.value = 0
    } else if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    loading.value = false
  }
}

function openModal(coupon = null) {
  editingCoupon.value = coupon
  if (coupon) {
    form.code = coupon.code || ''
    form.promotionId = coupon.promotionId || ''
    form.discountType = coupon.discountType || 'PERCENTAGE'
    form.discountValue = coupon.discountValue || 0
    form.maxUses = coupon.maxUses || 1
    form.expiryDate = coupon.expiryDate ? coupon.expiryDate.substring(0, 10) : ''
  } else {
    form.code = ''
    form.promotionId = ''
    form.discountType = 'PERCENTAGE'
    form.discountValue = 0
    form.maxUses = 1
    form.expiryDate = ''
  }
  showModal.value = true
}

async function saveCoupon() {
  if (!form.code?.trim()) return
  error.value = ''
  try {
    const payload = { ...form }
    if (editingCoupon.value) {
      await couponsApi.update(editingCoupon.value.id, payload)
      successMsg.value = t('pos.coupons.updateSuccess')
    } else {
      await couponsApi.create(payload)
      successMsg.value = t('pos.coupons.createSuccess')
    }
    showModal.value = false
    fetchCoupons(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function openGenerateModal() {
  generateForm.promotionId = ''
  generateForm.count = 1
  generateForm.prefix = ''
  generateForm.discountType = 'PERCENTAGE'
  generateForm.discountValue = 0
  generateForm.maxUses = 1
  generateForm.expiryDate = ''
  showGenerateModal.value = true
}

async function generateCoupons() {
  if (!generateForm.promotionId || !generateForm.count) return
  error.value = ''
  try {
    const { promotionId, count, ...data } = generateForm
    await couponsApi.generate(promotionId, count, data)
    successMsg.value = t('pos.coupons.generateSuccess', { count: generateForm.count })
    showGenerateModal.value = false
    fetchCoupons(0)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function toggleStatus(coupon) {
  error.value = ''
  try {
    if (coupon.status === 'ACTIVE') {
      await couponsApi.deactivate(coupon.id)
    } else {
      await couponsApi.activate(coupon.id)
    }
    fetchCoupons(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function cancelCoupon(coupon) {
  error.value = ''
  try {
    await couponsApi.cancel(coupon.id)
    successMsg.value = t('pos.coupons.cancelSuccess')
    fetchCoupons(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function confirmDelete(coupon) {
  deletingCoupon.value = coupon
  showDeleteConfirm.value = true
}

async function deleteCoupon() {
  if (!deletingCoupon.value) return
  error.value = ''
  try {
    await couponsApi.delete(deletingCoupon.value.id)
    successMsg.value = t('pos.coupons.deleteSuccess')
    showDeleteConfirm.value = false
    deletingCoupon.value = null
    fetchCoupons(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showDeleteConfirm.value = false
  }
}

async function toggleRedemptions(coupon) {
  if (expandedCouponId.value === coupon.id) {
    expandedCouponId.value = null
    redemptions.value = []
    couponDetail.value = null
    return
  }
  expandedCouponId.value = coupon.id
  loadingRedemptions.value = true
  couponDetailLoading.value = true
  couponDetail.value = null
  try {
    const [detailRes, redemptionsRes] = await Promise.all([
      couponsApi.getById(coupon.id),
      couponsApi.getRedemptions(coupon.id)
    ])
    couponDetail.value = detailRes.data.data || detailRes.data
    const data = redemptionsRes.data.data || redemptionsRes.data
    redemptions.value = Array.isArray(data) ? data : data.content || []
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    redemptions.value = []
  } finally {
    loadingRedemptions.value = false
    couponDetailLoading.value = false
  }
}

async function updateExpired() {
  error.value = ''
  try {
    await couponsApi.updateExpired()
    successMsg.value = t('pos.coupons.updateExpiredSuccess')
    fetchCoupons(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function updateDepleted() {
  error.value = ''
  try {
    await couponsApi.updateDepleted()
    successMsg.value = t('pos.coupons.updateDepletedSuccess')
    fetchCoupons(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function getStatusClass(status) {
  switch (status) {
    case 'ACTIVE': return 'badge-success'
    case 'EXPIRED': return 'badge-warning'
    case 'DEPLETED': return 'badge-info'
    case 'CANCELLED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function formatDiscount(coupon) {
  if (coupon.discountType === 'PERCENTAGE') {
    return `${coupon.discountValue}%`
  }
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(coupon.discountValue) || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('uz-UZ')
}

const tabs = computed(() =>
  statusFilters.map(status => ({
    key: status,
    label: status === 'ALL'
      ? t('pos.coupons.filterAll')
      : t(`pos.coupons.status.${status}`)
  }))
)
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('pos.coupons.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('pos.coupons.subtitle') }}</p>
      </div>
      <div class="flex items-center space-x-2">
        <!-- Maintenance buttons -->
        <button @click="updateExpired" class="btn-secondary">
          <ArrowPathIcon class="h-5 w-5 mr-2" />
          {{ $t('pos.coupons.updateExpired') }}
        </button>
        <button @click="updateDepleted" class="btn-secondary">
          <ArrowPathIcon class="h-5 w-5 mr-2" />
          {{ $t('pos.coupons.updateDepleted') }}
        </button>
        <button @click="openGenerateModal()" class="btn-secondary">
          <BoltIcon class="h-5 w-5 mr-2" />
          {{ $t('pos.coupons.generate') }}
        </button>
        <button @click="openModal()" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('pos.coupons.addCoupon') }}
        </button>
      </div>
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

    <!-- Search & Promotion Filter -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @keyup.enter="handleSearch"
            type="text"
            :placeholder="$t('pos.coupons.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
        <div class="flex items-center gap-2">
          <FunnelIcon class="h-5 w-5 text-gray-400" />
          <select
            v-model="selectedPromotionFilter"
            @change="filterByPromotion"
            class="input w-auto"
          >
            <option value="">{{ $t('pos.coupons.allPromotions') }}</option>
            <option v-for="promo in promotionsList" :key="promo.id" :value="promo.id">
              {{ promo.name || promo.code }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- Coupons Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="coupons.length === 0" class="text-center py-12">
        <TicketIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('pos.coupons.noCoupons') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th></th>
              <th>{{ $t('pos.coupons.code') }}</th>
              <th>{{ $t('pos.coupons.promotion') }}</th>
              <th>{{ $t('pos.coupons.discount') }}</th>
              <th class="text-right">{{ $t('pos.coupons.maxUses') }}</th>
              <th class="text-right">{{ $t('pos.coupons.usedCount') }}</th>
              <th>{{ $t('pos.coupons.expiryDate') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <template v-for="coupon in coupons" :key="coupon.id">
              <tr>
                <td>
                  <button
                    @click="toggleRedemptions(coupon)"
                    class="p-1 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                    :title="$t('pos.coupons.viewRedemptions')"
                  >
                    <ChevronDownIcon v-if="expandedCouponId !== coupon.id" class="h-4 w-4" />
                    <ChevronUpIcon v-else class="h-4 w-4" />
                  </button>
                </td>
                <td>
                  <code class="font-mono font-medium text-sm">{{ coupon.code }}</code>
                </td>
                <td class="text-sm text-gray-500">{{ coupon.promotionId || coupon.promotionName || '-' }}</td>
                <td>
                  <span class="text-sm font-medium">{{ formatDiscount(coupon) }}</span>
                  <span class="text-xs text-gray-400 ml-1">({{ coupon.discountType }})</span>
                </td>
                <td class="text-right text-sm">{{ coupon.maxUses ?? '-' }}</td>
                <td class="text-right text-sm">{{ coupon.usedCount ?? 0 }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(coupon.expiryDate) }}</td>
                <td>
                  <button
                    @click="toggleStatus(coupon)"
                    :class="['badge cursor-pointer text-xs', getStatusClass(coupon.status)]"
                    :disabled="coupon.status === 'CANCELLED' || coupon.status === 'EXPIRED' || coupon.status === 'DEPLETED'"
                  >
                    {{ $t(`pos.coupons.status.${coupon.status}`) }}
                  </button>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button
                      @click="openModal(coupon)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('edit')"
                    >
                      <PencilIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="coupon.status === 'ACTIVE'"
                      @click="cancelCoupon(coupon)"
                      class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                      :title="$t('pos.coupons.cancelCoupon')"
                    >
                      <NoSymbolIcon class="h-5 w-5" />
                    </button>
                    <button
                      @click="confirmDelete(coupon)"
                      class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                      :title="$t('delete')"
                    >
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
              <!-- Redemption History (expandable row) -->
              <tr v-if="expandedCouponId === coupon.id">
                <td :colspan="9" class="bg-gray-50 px-8 py-4">
                  <!-- Coupon Detail (via getById) -->
                  <div v-if="couponDetailLoading" class="flex items-center justify-center py-4">
                    <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
                  </div>
                  <div v-else-if="couponDetail" class="mb-4">
                    <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('pos.coupons.couponDetails') }}</h4>
                    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
                      <div>
                        <span class="text-gray-500">{{ $t('pos.coupons.code') }}:</span>
                        <span class="ml-1 font-mono font-medium">{{ couponDetail.code }}</span>
                      </div>
                      <div>
                        <span class="text-gray-500">{{ $t('pos.coupons.discount') }}:</span>
                        <span class="ml-1 font-medium">{{ formatDiscount(couponDetail) }}</span>
                      </div>
                      <div>
                        <span class="text-gray-500">{{ $t('pos.coupons.maxUses') }}:</span>
                        <span class="ml-1">{{ couponDetail.maxUses ?? '-' }}</span>
                      </div>
                      <div>
                        <span class="text-gray-500">{{ $t('pos.coupons.usedCount') }}:</span>
                        <span class="ml-1">{{ couponDetail.usedCount ?? 0 }}</span>
                      </div>
                      <div v-if="couponDetail.promotionName || couponDetail.promotionId">
                        <span class="text-gray-500">{{ $t('pos.coupons.promotion') }}:</span>
                        <span class="ml-1">{{ couponDetail.promotionName || couponDetail.promotionId }}</span>
                      </div>
                      <div>
                        <span class="text-gray-500">{{ $t('pos.coupons.expiryDate') }}:</span>
                        <span class="ml-1">{{ formatDate(couponDetail.expiryDate) }}</span>
                      </div>
                    </div>
                  </div>

                  <!-- Redemption History -->
                  <div v-if="loadingRedemptions" class="flex items-center justify-center py-4">
                    <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
                  </div>
                  <div v-else-if="redemptions.length === 0" class="text-center py-4">
                    <p class="text-sm text-gray-500">{{ $t('pos.coupons.noRedemptions') }}</p>
                  </div>
                  <div v-else>
                    <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('pos.coupons.redemptionHistory') }}</h4>
                    <table class="table w-full">
                      <thead>
                        <tr>
                          <th>{{ $t('pos.coupons.redemption.customer') }}</th>
                          <th>{{ $t('pos.coupons.redemption.order') }}</th>
                          <th>{{ $t('pos.coupons.redemption.date') }}</th>
                          <th class="text-right">{{ $t('pos.coupons.redemption.discountApplied') }}</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-gray-200">
                        <tr v-for="redemption in redemptions" :key="redemption.id">
                          <td class="text-sm">{{ redemption.customerName || redemption.customerId || '-' }}</td>
                          <td class="text-sm">{{ redemption.orderNumber || redemption.orderId || '-' }}</td>
                          <td class="text-sm text-gray-500">{{ formatDate(redemption.date || redemption.createdAt) }}</td>
                          <td class="text-sm text-right font-medium">{{ redemption.discountApplied ?? redemption.discountAmount ?? '-' }}</td>
                        </tr>
                      </tbody>
                    </table>
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
            @click="fetchCoupons(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchCoupons(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm"
          >{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- Create/Edit Coupon Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingCoupon ? $t('pos.coupons.editCoupon') : $t('pos.coupons.newCoupon') }}
              </h3>
              <button @click="showModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.coupons.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="form.code" type="text" class="input font-mono" :placeholder="$t('pos.coupons.codePlaceholder')" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.coupons.promotionId') }}</label>
                  <input v-model="form.promotionId" type="text" class="input" :placeholder="$t('pos.coupons.promotionIdPlaceholder')" />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.coupons.discountType') }} <span class="text-red-500">*</span></label>
                  <select v-model="form.discountType" class="input">
                    <option v-for="dtype in discountTypes" :key="dtype" :value="dtype">
                      {{ $t(`pos.coupons.discountTypes.${dtype}`) }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('pos.coupons.discountValue') }} <span class="text-red-500">*</span></label>
                  <input v-model.number="form.discountValue" type="number" step="0.01" min="0" class="input" />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.coupons.maxUses') }}</label>
                  <input v-model.number="form.maxUses" type="number" min="1" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.coupons.expiryDate') }}</label>
                  <input v-model="form.expiryDate" type="date" class="input" />
                </div>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveCoupon" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Generate Coupons Modal -->
    <Teleport to="body">
      <div v-if="showGenerateModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showGenerateModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ $t('pos.coupons.generateTitle') }}
              </h3>
              <button @click="showGenerateModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.coupons.promotionId') }} <span class="text-red-500">*</span></label>
                  <input v-model="generateForm.promotionId" type="text" class="input" :placeholder="$t('pos.coupons.promotionIdPlaceholder')" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.coupons.count') }} <span class="text-red-500">*</span></label>
                  <input v-model.number="generateForm.count" type="number" min="1" class="input" />
                </div>
              </div>

              <div>
                <label class="label">{{ $t('pos.coupons.prefix') }}</label>
                <input v-model="generateForm.prefix" type="text" class="input font-mono" :placeholder="$t('pos.coupons.prefixPlaceholder')" />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.coupons.discountType') }} <span class="text-red-500">*</span></label>
                  <select v-model="generateForm.discountType" class="input">
                    <option v-for="dtype in discountTypes" :key="dtype" :value="dtype">
                      {{ $t(`pos.coupons.discountTypes.${dtype}`) }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('pos.coupons.discountValue') }} <span class="text-red-500">*</span></label>
                  <input v-model.number="generateForm.discountValue" type="number" step="0.01" min="0" class="input" />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.coupons.maxUses') }}</label>
                  <input v-model.number="generateForm.maxUses" type="number" min="1" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.coupons.expiryDate') }}</label>
                  <input v-model="generateForm.expiryDate" type="date" class="input" />
                </div>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showGenerateModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="generateCoupons" class="btn-primary">
                <BoltIcon class="h-5 w-5 mr-2" />
                {{ $t('pos.coupons.generateBtn') }}
              </button>
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
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('pos.coupons.confirmDeleteTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('pos.coupons.confirmDelete', { code: deletingCoupon?.code }) }}
            </p>
            <div class="flex justify-end space-x-3">
              <button @click="showDeleteConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="deleteCoupon" class="btn-danger">{{ $t('delete') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
