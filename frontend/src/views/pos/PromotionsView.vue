<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { promotionsApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  XMarkIcon, TagIcon, EyeIcon
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

// Code lookup
const codeLookup = ref('')
const codeLookupResult = ref(null)
const codeLookupError = ref('')
const codeLookupLoading = ref(false)

// Modal state
const showModal = ref(false)
const editingPromotion = ref(null)

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
const deletingPromotion = ref(null)

// Detail / expand view
const expandedPromotion = ref(null)
const expandedDetail = ref(null)
const detailLoading = ref(false)

// Condition form
const showConditionForm = ref(false)
const conditionForm = reactive({
  conditionType: '',
  conditionValue: ''
})

// Action form
const showActionForm = ref(false)
const actionForm = reactive({
  actionType: '',
  actionValue: ''
})

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
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      promotions.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      promotions.value = data.content || []
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

onMounted(() => fetchPromotions(0))

function switchTab(tab) {
  activeTab.value = tab
  search.value = ''
  fetchPromotions(0)
}

function handleSearch() {
  activeTab.value = 'ALL'
  fetchPromotions(0)
}

// Code lookup
async function lookupByCode() {
  if (!codeLookup.value.trim()) return
  codeLookupLoading.value = true
  codeLookupError.value = ''
  codeLookupResult.value = null
  try {
    const res = await promotionsApi.getByCode(codeLookup.value.trim())
    codeLookupResult.value = res.data.data || res.data
  } catch (e) {
    if (e.response?.status === 404) {
      codeLookupError.value = t('pos.promotions.codeNotFound')
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

function openModal(promotion = null) {
  editingPromotion.value = promotion
  if (promotion) {
    form.code = promotion.code || ''
    form.name = promotion.name || ''
    form.description = promotion.description || ''
    form.startDate = promotion.startDate || ''
    form.endDate = promotion.endDate || ''
    form.priority = promotion.priority || 0
    form.active = promotion.active !== false
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

async function savePromotion() {
  if (!form.code?.trim() || !form.name?.trim()) return
  error.value = ''
  try {
    if (editingPromotion.value) {
      await promotionsApi.update(editingPromotion.value.id, { ...form })
      successMsg.value = t('pos.promotions.updateSuccess')
    } else {
      await promotionsApi.create({ ...form })
      successMsg.value = t('pos.promotions.createSuccess')
    }
    showModal.value = false
    fetchPromotions(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function toggleStatus(promotion) {
  error.value = ''
  try {
    if (promotion.active) {
      await promotionsApi.deactivate(promotion.id)
    } else {
      await promotionsApi.activate(promotion.id)
    }
    fetchPromotions(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function confirmDelete(promotion) {
  deletingPromotion.value = promotion
  showDeleteConfirm.value = true
}

async function deletePromotion() {
  if (!deletingPromotion.value) return
  error.value = ''
  try {
    await promotionsApi.delete(deletingPromotion.value.id)
    successMsg.value = t('pos.promotions.deleteSuccess')
    showDeleteConfirm.value = false
    deletingPromotion.value = null
    if (expandedPromotion.value === deletingPromotion.value?.id) {
      expandedPromotion.value = null
      expandedDetail.value = null
    }
    fetchPromotions(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showDeleteConfirm.value = false
  }
}

async function toggleDetail(promotion) {
  if (expandedPromotion.value === promotion.id) {
    expandedPromotion.value = null
    expandedDetail.value = null
    return
  }
  expandedPromotion.value = promotion.id
  detailLoading.value = true
  try {
    const res = await promotionsApi.getById(promotion.id)
    expandedDetail.value = res.data.data || res.data
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    expandedPromotion.value = null
  } finally {
    detailLoading.value = false
  }
}

// Conditions
function openConditionForm() {
  conditionForm.conditionType = ''
  conditionForm.conditionValue = ''
  showConditionForm.value = true
}

async function addCondition() {
  if (!conditionForm.conditionType?.trim() || !conditionForm.conditionValue?.trim()) return
  error.value = ''
  try {
    await promotionsApi.addCondition(expandedPromotion.value, { ...conditionForm })
    successMsg.value = t('pos.promotions.conditionAdded')
    showConditionForm.value = false
    const res = await promotionsApi.getById(expandedPromotion.value)
    expandedDetail.value = res.data.data || res.data
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function removeCondition(conditionId) {
  error.value = ''
  try {
    await promotionsApi.removeCondition(expandedPromotion.value, conditionId)
    successMsg.value = t('pos.promotions.conditionRemoved')
    const res = await promotionsApi.getById(expandedPromotion.value)
    expandedDetail.value = res.data.data || res.data
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

// Actions
function openActionForm() {
  actionForm.actionType = ''
  actionForm.actionValue = ''
  showActionForm.value = true
}

async function addAction() {
  if (!actionForm.actionType?.trim() || !actionForm.actionValue?.trim()) return
  error.value = ''
  try {
    await promotionsApi.addAction(expandedPromotion.value, { ...actionForm })
    successMsg.value = t('pos.promotions.actionAdded')
    showActionForm.value = false
    const res = await promotionsApi.getById(expandedPromotion.value)
    expandedDetail.value = res.data.data || res.data
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function removeAction(actionId) {
  error.value = ''
  try {
    await promotionsApi.removeAction(expandedPromotion.value, actionId)
    successMsg.value = t('pos.promotions.actionRemoved')
    const res = await promotionsApi.getById(expandedPromotion.value)
    expandedDetail.value = res.data.data || res.data
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
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

    <!-- Search & Code Lookup -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
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
              :placeholder="$t('pos.promotions.searchPlaceholder')"
              class="input pl-10"
            />
          </div>
        </div>
      </div>

      <!-- Code Lookup -->
      <div class="card">
        <div class="card-body">
          <label class="text-sm font-medium text-gray-700 mb-2 block">{{ $t('pos.promotions.codeLookup') }}</label>
          <div class="flex gap-2">
            <input
              v-model="codeLookup"
              @keyup.enter="lookupByCode"
              type="text"
              :placeholder="$t('pos.promotions.codeLookupPlaceholder')"
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
            <p class="text-gray-500">
              {{ $t('code') }}: {{ codeLookupResult.code }}
              | {{ $t('status') }}: {{ codeLookupResult.active ? $t('active') : $t('inactive') }}
              | {{ $t('pos.promotions.priority') }}: {{ codeLookupResult.priority }}
            </p>
            <div class="mt-1 flex gap-2">
              <button @click="toggleDetail(codeLookupResult)" class="text-sm text-primary-600 hover:underline">{{ $t('pos.promotions.viewDetails') }}</button>
              <button @click="openModal(codeLookupResult)" class="text-sm text-primary-600 hover:underline">{{ $t('edit') }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Promotions Table -->
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
              <th>{{ $t('pos.promotions.code') }}</th>
              <th>{{ $t('pos.promotions.name') }}</th>
              <th>{{ $t('pos.promotions.startDate') }}</th>
              <th>{{ $t('pos.promotions.endDate') }}</th>
              <th>{{ $t('pos.promotions.priority') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <template v-for="promotion in promotions" :key="promotion.id">
              <tr>
                <td>
                  <code class="font-mono font-medium text-sm">{{ promotion.code }}</code>
                </td>
                <td>
                  <p class="font-medium text-sm">{{ promotion.name }}</p>
                  <p v-if="promotion.description" class="text-xs text-gray-400">{{ promotion.description }}</p>
                </td>
                <td class="text-sm text-gray-500">{{ formatDate(promotion.startDate) }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(promotion.endDate) }}</td>
                <td class="text-sm text-gray-500">{{ promotion.priority }}</td>
                <td>
                  <button
                    @click="toggleStatus(promotion)"
                    :class="['badge cursor-pointer text-xs', promotion.active ? 'badge-success' : 'badge-danger']"
                  >
                    {{ promotion.active ? $t('active') : $t('inactive') }}
                  </button>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button
                      @click="toggleDetail(promotion)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('pos.promotions.viewDetails')"
                    >
                      <EyeIcon class="h-5 w-5" />
                    </button>
                    <button
                      @click="openModal(promotion)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('edit')"
                    >
                      <PencilIcon class="h-5 w-5" />
                    </button>
                    <button
                      @click="confirmDelete(promotion)"
                      class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                      :title="$t('delete')"
                    >
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Expanded Detail Row -->
              <tr v-if="expandedPromotion === promotion.id">
                <td colspan="7" class="bg-gray-50 px-6 py-4">
                  <div v-if="detailLoading" class="flex items-center justify-center py-8">
                    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                  </div>
                  <div v-else-if="expandedDetail" class="space-y-6">
                    <!-- Promotion Info -->
                    <div>
                      <h4 class="text-sm font-semibold text-gray-900 mb-2">{{ $t('pos.promotions.promotionInfo') }}</h4>
                      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                        <div>
                          <span class="text-gray-500">{{ $t('pos.promotions.code') }}:</span>
                          <span class="ml-1 font-medium">{{ expandedDetail.code }}</span>
                        </div>
                        <div>
                          <span class="text-gray-500">{{ $t('pos.promotions.name') }}:</span>
                          <span class="ml-1 font-medium">{{ expandedDetail.name }}</span>
                        </div>
                        <div>
                          <span class="text-gray-500">{{ $t('pos.promotions.priority') }}:</span>
                          <span class="ml-1 font-medium">{{ expandedDetail.priority }}</span>
                        </div>
                        <div>
                          <span class="text-gray-500">{{ $t('status') }}:</span>
                          <span :class="['ml-1 badge text-xs', expandedDetail.active ? 'badge-success' : 'badge-danger']">
                            {{ expandedDetail.active ? $t('active') : $t('inactive') }}
                          </span>
                        </div>
                      </div>
                      <p v-if="expandedDetail.description" class="mt-2 text-sm text-gray-500">{{ expandedDetail.description }}</p>
                    </div>

                    <!-- Conditions -->
                    <div>
                      <div class="flex items-center justify-between mb-2">
                        <h4 class="text-sm font-semibold text-gray-900">{{ $t('pos.promotions.conditions') }}</h4>
                        <button @click="openConditionForm" class="btn-secondary text-xs">
                          <PlusIcon class="h-4 w-4 mr-1" />
                          {{ $t('pos.promotions.addCondition') }}
                        </button>
                      </div>

                      <!-- Add Condition Form -->
                      <div v-if="showConditionForm" class="mb-3 p-3 bg-white border border-gray-200 rounded-lg">
                        <div class="grid grid-cols-2 gap-3">
                          <div>
                            <label class="label">{{ $t('pos.promotions.conditionType') }}</label>
                            <input v-model="conditionForm.conditionType" type="text" class="input" :placeholder="$t('pos.promotions.conditionTypePlaceholder')" />
                          </div>
                          <div>
                            <label class="label">{{ $t('pos.promotions.conditionValue') }}</label>
                            <input v-model="conditionForm.conditionValue" type="text" class="input" :placeholder="$t('pos.promotions.conditionValuePlaceholder')" />
                          </div>
                        </div>
                        <div class="mt-3 flex justify-end space-x-2">
                          <button @click="showConditionForm = false" class="btn-secondary text-xs">{{ $t('cancel') }}</button>
                          <button @click="addCondition" class="btn-primary text-xs">{{ $t('save') }}</button>
                        </div>
                      </div>

                      <div v-if="expandedDetail.conditions && expandedDetail.conditions.length > 0" class="space-y-2">
                        <div
                          v-for="condition in expandedDetail.conditions"
                          :key="condition.id"
                          class="flex items-center justify-between p-2 bg-white border border-gray-200 rounded-lg"
                        >
                          <div class="text-sm">
                            <span class="font-medium">{{ condition.conditionType }}</span>
                            <span class="text-gray-500 ml-2">{{ condition.conditionValue }}</span>
                          </div>
                          <button
                            @click="removeCondition(condition.id)"
                            class="p-1 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                            :title="$t('delete')"
                          >
                            <TrashIcon class="h-4 w-4" />
                          </button>
                        </div>
                      </div>
                      <p v-else class="text-sm text-gray-400">{{ $t('pos.promotions.noConditions') }}</p>
                    </div>

                    <!-- Actions -->
                    <div>
                      <div class="flex items-center justify-between mb-2">
                        <h4 class="text-sm font-semibold text-gray-900">{{ $t('pos.promotions.actions') }}</h4>
                        <button @click="openActionForm" class="btn-secondary text-xs">
                          <PlusIcon class="h-4 w-4 mr-1" />
                          {{ $t('pos.promotions.addAction') }}
                        </button>
                      </div>

                      <!-- Add Action Form -->
                      <div v-if="showActionForm" class="mb-3 p-3 bg-white border border-gray-200 rounded-lg">
                        <div class="grid grid-cols-2 gap-3">
                          <div>
                            <label class="label">{{ $t('pos.promotions.actionType') }}</label>
                            <input v-model="actionForm.actionType" type="text" class="input" :placeholder="$t('pos.promotions.actionTypePlaceholder')" />
                          </div>
                          <div>
                            <label class="label">{{ $t('pos.promotions.actionValue') }}</label>
                            <input v-model="actionForm.actionValue" type="text" class="input" :placeholder="$t('pos.promotions.actionValuePlaceholder')" />
                          </div>
                        </div>
                        <div class="mt-3 flex justify-end space-x-2">
                          <button @click="showActionForm = false" class="btn-secondary text-xs">{{ $t('cancel') }}</button>
                          <button @click="addAction" class="btn-primary text-xs">{{ $t('save') }}</button>
                        </div>
                      </div>

                      <div v-if="expandedDetail.actions && expandedDetail.actions.length > 0" class="space-y-2">
                        <div
                          v-for="action in expandedDetail.actions"
                          :key="action.id"
                          class="flex items-center justify-between p-2 bg-white border border-gray-200 rounded-lg"
                        >
                          <div class="text-sm">
                            <span class="font-medium">{{ action.actionType }}</span>
                            <span class="text-gray-500 ml-2">{{ action.actionValue }}</span>
                          </div>
                          <button
                            @click="removeAction(action.id)"
                            class="p-1 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                            :title="$t('delete')"
                          >
                            <TrashIcon class="h-4 w-4" />
                          </button>
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
          <button
            @click="fetchPromotions(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchPromotions(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm"
          >{{ $t('next') }}</button>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingPromotion ? $t('pos.promotions.editPromotion') : $t('pos.promotions.newPromotion') }}
              </h3>
              <button @click="showModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('pos.promotions.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="form.code" type="text" class="input font-mono" :placeholder="$t('pos.promotions.codePlaceholder')" />
                </div>
                <div>
                  <label class="label">{{ $t('pos.promotions.priority') }}</label>
                  <input v-model.number="form.priority" type="number" class="input" />
                </div>
              </div>

              <div>
                <label class="label">{{ $t('pos.promotions.name') }} <span class="text-red-500">*</span></label>
                <input v-model="form.name" type="text" class="input" :placeholder="$t('pos.promotions.namePlaceholder')" />
              </div>

              <div>
                <label class="label">{{ $t('description') }}</label>
                <textarea v-model="form.description" rows="2" class="input" :placeholder="$t('pos.promotions.descriptionPlaceholder')"></textarea>
              </div>

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

              <div class="flex flex-wrap gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('active') }}</span>
                </label>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="savePromotion" class="btn-primary">{{ $t('save') }}</button>
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
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('pos.promotions.confirmDelete', { name: deletingPromotion?.name }) }}
            </p>
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
