<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { bankAccountsApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  StarIcon, XMarkIcon, BuildingLibraryIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const accounts = ref([])
const loading = ref(true)
const error = ref('')
const successMsg = ref('')
const search = ref('')
const activeTab = ref('ALL')
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 20

const accountTypes = ['CASH', 'BANK', 'MOBILE_MONEY', 'CREDIT_CARD']

// Modal state
const showModal = ref(false)
const editingAccount = ref(null)

const form = reactive({
  accountCode: '',
  accountName: '',
  accountType: 'BANK',
  bankName: '',
  bankAccountNumber: '',
  openingBalance: 0,
  description: '',
  isPaymentAccount: false,
  isReceiptAccount: false,
  active: true
})

// Delete confirmation
const showDeleteConfirm = ref(false)
const deletingAccount = ref(null)

async function fetchAccounts(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (search.value.trim()) {
      res = await bankAccountsApi.search(search.value.trim(), { page, size: pageSize, sort: 'accountCode,asc' })
    } else if (activeTab.value !== 'ALL') {
      res = await bankAccountsApi.getByType(activeTab.value)
    } else {
      res = await bankAccountsApi.getAll({ page, size: pageSize, sort: 'accountCode,asc' })
    }
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      accounts.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      accounts.value = data.content || []
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

onMounted(() => fetchAccounts(0))

function switchTab(tab) {
  activeTab.value = tab
  search.value = ''
  fetchAccounts(0)
}

function handleSearch() {
  activeTab.value = 'ALL'
  fetchAccounts(0)
}

function openModal(account = null) {
  editingAccount.value = account
  if (account) {
    form.accountCode = account.accountCode || ''
    form.accountName = account.accountName || ''
    form.accountType = account.accountType || 'BANK'
    form.bankName = account.bankName || ''
    form.bankAccountNumber = account.bankAccountNumber || ''
    form.openingBalance = account.openingBalance || 0
    form.description = account.description || ''
    form.isPaymentAccount = account.isPaymentAccount || false
    form.isReceiptAccount = account.isReceiptAccount || false
    form.active = account.active !== false
  } else {
    form.accountCode = ''
    form.accountName = ''
    form.accountType = 'BANK'
    form.bankName = ''
    form.bankAccountNumber = ''
    form.openingBalance = 0
    form.description = ''
    form.isPaymentAccount = false
    form.isReceiptAccount = false
    form.active = true
  }
  showModal.value = true
}

async function saveAccount() {
  if (!form.accountCode?.trim() || !form.accountName?.trim()) return
  error.value = ''
  try {
    if (editingAccount.value) {
      await bankAccountsApi.update(editingAccount.value.id, { ...form })
      successMsg.value = t('finance.bankAccounts.updateSuccess')
    } else {
      await bankAccountsApi.create({ ...form })
      successMsg.value = t('finance.bankAccounts.createSuccess')
    }
    showModal.value = false
    fetchAccounts(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function toggleStatus(account) {
  error.value = ''
  try {
    if (account.active) {
      await bankAccountsApi.deactivate(account.id)
    } else {
      await bankAccountsApi.activate(account.id)
    }
    fetchAccounts(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function setDefault(account) {
  error.value = ''
  try {
    await bankAccountsApi.setDefault(account.id)
    successMsg.value = t('finance.bankAccounts.defaultSuccess', { name: account.accountName })
    fetchAccounts(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function confirmDelete(account) {
  deletingAccount.value = account
  showDeleteConfirm.value = true
}

async function deleteAccount() {
  if (!deletingAccount.value) return
  error.value = ''
  try {
    await bankAccountsApi.delete(deletingAccount.value.id)
    successMsg.value = t('finance.bankAccounts.deleteSuccess')
    showDeleteConfirm.value = false
    deletingAccount.value = null
    fetchAccounts(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showDeleteConfirm.value = false
  }
}

function getTypeClass(type) {
  switch (type) {
    case 'CASH': return 'badge-success'
    case 'BANK': return 'badge-info'
    case 'MOBILE_MONEY': return 'badge-warning'
    case 'CREDIT_CARD': return 'badge-danger'
    default: return 'badge-info'
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(value) || 0)
}

const tabs = computed(() => [
  { key: 'ALL', label: t('finance.bankAccounts.filterAll') },
  ...accountTypes.map(type => ({
    key: type,
    label: t(`finance.bankAccounts.type.${type}`)
  }))
])
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.bankAccounts.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.bankAccounts.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('finance.bankAccounts.addAccount') }}
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

    <!-- Search -->
    <div class="card">
      <div class="card-body">
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @keyup.enter="handleSearch"
            type="text"
            :placeholder="$t('finance.bankAccounts.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
      </div>
    </div>

    <!-- Accounts Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="accounts.length === 0" class="text-center py-12">
        <BuildingLibraryIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('finance.bankAccounts.noAccounts') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('finance.bankAccounts.code') }}</th>
              <th>{{ $t('finance.bankAccounts.name') }}</th>
              <th>{{ $t('finance.bankAccounts.accountType') }}</th>
              <th>{{ $t('finance.bankAccounts.bankName') }}</th>
              <th class="text-right">{{ $t('finance.bankAccounts.currentBalance') }}</th>
              <th>{{ $t('finance.bankAccounts.default') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="account in accounts" :key="account.id">
              <td>
                <code class="font-mono font-medium text-sm">{{ account.accountCode }}</code>
              </td>
              <td>
                <p class="font-medium text-sm">{{ account.accountName }}</p>
                <p v-if="account.bankAccountNumber" class="text-xs text-gray-400">{{ account.bankAccountNumber }}</p>
              </td>
              <td>
                <span :class="['badge text-xs', getTypeClass(account.accountType)]">
                  {{ $t(`finance.bankAccounts.type.${account.accountType}`) }}
                </span>
              </td>
              <td class="text-sm text-gray-500">{{ account.bankName || '-' }}</td>
              <td class="text-right">
                <span class="font-semibold text-sm">{{ formatCurrency(account.currentBalance) }}</span>
              </td>
              <td>
                <span v-if="account.defaultAccount" class="badge badge-warning text-xs">
                  {{ $t('finance.bankAccounts.defaultBadge') }}
                </span>
              </td>
              <td>
                <button
                  @click="toggleStatus(account)"
                  :class="['badge cursor-pointer text-xs', account.active ? 'badge-success' : 'badge-danger']"
                >
                  {{ account.active ? $t('active') : $t('inactive') }}
                </button>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-1">
                  <button
                    v-if="!account.defaultAccount"
                    @click="setDefault(account)"
                    class="p-2 text-gray-400 hover:text-yellow-500 rounded-lg hover:bg-gray-100"
                    :title="$t('finance.bankAccounts.setDefault')"
                  >
                    <StarIcon class="h-5 w-5" />
                  </button>
                  <button
                    @click="openModal(account)"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                    :title="$t('edit')"
                  >
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button
                    @click="confirmDelete(account)"
                    class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                    :title="$t('delete')"
                  >
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
        <p class="text-sm text-gray-500">{{ $t('page') }} {{ currentPage + 1 }} / {{ totalPages }}</p>
        <div class="flex gap-2">
          <button
            @click="fetchAccounts(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchAccounts(currentPage + 1)"
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
                {{ editingAccount ? $t('finance.bankAccounts.editAccount') : $t('finance.bankAccounts.newAccount') }}
              </h3>
              <button @click="showModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.bankAccounts.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="form.accountCode" type="text" class="input font-mono" :placeholder="$t('finance.bankAccounts.codePlaceholder')" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.bankAccounts.accountType') }} <span class="text-red-500">*</span></label>
                  <select v-model="form.accountType" class="input">
                    <option v-for="type in accountTypes" :key="type" :value="type">
                      {{ $t(`finance.bankAccounts.type.${type}`) }}
                    </option>
                  </select>
                </div>
              </div>

              <div>
                <label class="label">{{ $t('finance.bankAccounts.name') }} <span class="text-red-500">*</span></label>
                <input v-model="form.accountName" type="text" class="input" :placeholder="$t('finance.bankAccounts.namePlaceholder')" />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.bankAccounts.bankName') }}</label>
                  <input v-model="form.bankName" type="text" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.bankAccounts.bankAccountNumber') }}</label>
                  <input v-model="form.bankAccountNumber" type="text" class="input font-mono" />
                </div>
              </div>

              <div>
                <label class="label">{{ $t('finance.bankAccounts.openingBalance') }}</label>
                <input v-model.number="form.openingBalance" type="number" step="0.01" class="input" />
              </div>

              <div>
                <label class="label">{{ $t('description') }}</label>
                <textarea v-model="form.description" rows="2" class="input"></textarea>
              </div>

              <div class="flex flex-wrap gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.isPaymentAccount" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('finance.bankAccounts.isPaymentAccount') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.isReceiptAccount" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('finance.bankAccounts.isReceiptAccount') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('active') }}</span>
                </label>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveAccount" class="btn-primary">{{ $t('save') }}</button>
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
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.bankAccounts.confirmDeleteTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('finance.bankAccounts.confirmDelete', { name: deletingAccount?.accountName }) }}
            </p>
            <div class="flex justify-end space-x-3">
              <button @click="showDeleteConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="deleteAccount" class="btn-danger">{{ $t('delete') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
