<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { accountsApi } from '@/services/api'
import {
  PlusIcon, PencilSquareIcon, TrashIcon, XMarkIcon,
  MagnifyingGlassIcon, ChevronRightIcon, ChevronDownIcon,
  ArrowPathIcon, ArrowUpTrayIcon, SparklesIcon,
  CheckCircleIcon, XCircleIcon, FunnelIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const allAccounts = ref([])
const rootAccounts = ref([])
const loading = ref(true)
const search = ref('')
const activeType = ref('')
const expandedIds = ref(new Set())

const showModal = ref(false)
const editMode = ref(false)
const modalForm = ref(createEmptyForm())
const modalErrors = ref({})
const saving = ref(false)

const showImportModal = ref(false)
const importFile = ref(null)
const importing = ref(false)
const importResult = ref(null)

const generating = ref(false)

const accountTypes = ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE']

// Quick filters
const quickFilter = ref('')

// Code lookup
const codeLookup = ref('')
const codeLookupResult = ref(null)
const codeLookupError = ref('')

function createEmptyForm() {
  return {
    code: '',
    name: '',
    accountType: 'ASSET',
    description: '',
    parentAccountId: null,
    controlAccount: false,
    allowsDirectPosting: true,
    openingBalance: null,
    currency: 'UZS',
    taxCode: '',
    bankAccountNumber: '',
    bankName: '',
    sortOrder: 0,
    notes: ''
  }
}

async function fetchAccounts() {
  loading.value = true
  try {
    if (search.value.trim()) {
      const res = await accountsApi.search(search.value)
      const data = res.data.data || res.data
      allAccounts.value = data.content || data || []
      rootAccounts.value = allAccounts.value
    } else if (quickFilter.value === 'ACTIVE') {
      const res = await accountsApi.getActive()
      allAccounts.value = res.data.data || res.data || []
      rootAccounts.value = buildTree(allAccounts.value)
    } else if (quickFilter.value === 'POSTABLE') {
      const res = await accountsApi.getPostable()
      allAccounts.value = res.data.data || res.data || []
      rootAccounts.value = buildTree(allAccounts.value)
    } else if (activeType.value) {
      const res = await accountsApi.getByType(activeType.value)
      allAccounts.value = res.data.data || res.data || []
      rootAccounts.value = buildTree(allAccounts.value)
    } else {
      const res = await accountsApi.getRoots()
      rootAccounts.value = res.data.data || res.data || []
      const allRes = await accountsApi.getAllList()
      allAccounts.value = allRes.data.data || allRes.data || []
    }
  } catch (error) {
    console.error('Failed to fetch accounts:', error)
  } finally {
    loading.value = false
  }
}

function buildTree(accounts) {
  return accounts.filter(a => !a.parentAccountId)
}

onMounted(fetchAccounts)

const childrenCache = ref({})

async function toggleExpand(account) {
  const id = account.id
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
    if (!childrenCache.value[id]) {
      try {
        // Try getTree first for full subtree, fall back to getChildren
        const res = await accountsApi.getTree(id)
        const treeData = res.data.data || res.data
        if (treeData && treeData.childAccounts) {
          childrenCache.value[id] = treeData.childAccounts
        } else if (Array.isArray(treeData)) {
          childrenCache.value[id] = treeData
        } else {
          const childRes = await accountsApi.getChildren(id)
          childrenCache.value[id] = childRes.data.data || childRes.data || []
        }
      } catch (e) {
        try {
          const childRes = await accountsApi.getChildren(id)
          childrenCache.value[id] = childRes.data.data || childRes.data || []
        } catch (e2) {
          console.error('Failed to load children:', e2)
          childrenCache.value[id] = []
        }
      }
    }
  }
}

function getChildren(parentId) {
  return childrenCache.value[parentId] || []
}

function hasChildren(account) {
  return account.childAccounts?.length > 0 || allAccounts.value.some(a => a.parentAccountId === account.id)
}

function applyQuickFilter(filter) {
  if (quickFilter.value === filter) {
    quickFilter.value = ''
  } else {
    quickFilter.value = filter
  }
  activeType.value = ''
  search.value = ''
  expandedIds.value.clear()
  childrenCache.value = {}
  fetchAccounts()
}

function filterByType(type) {
  activeType.value = type
  search.value = ''
  quickFilter.value = ''
  expandedIds.value.clear()
  childrenCache.value = {}
  fetchAccounts()
}

function handleSearch() {
  activeType.value = ''
  quickFilter.value = ''
  expandedIds.value.clear()
  childrenCache.value = {}
  fetchAccounts()
}

async function lookupByCode() {
  if (!codeLookup.value.trim()) return
  codeLookupError.value = ''
  codeLookupResult.value = null
  try {
    const res = await accountsApi.getByCode(codeLookup.value.trim())
    const data = res.data.data || res.data
    if (data) {
      codeLookupResult.value = data
    } else {
      codeLookupError.value = t('finance.accounts.codeNotFound')
    }
  } catch (e) {
    codeLookupError.value = e.response?.data?.message || t('finance.accounts.codeNotFound')
  }
}

function openCreateModal(parentAccount) {
  editMode.value = false
  modalForm.value = createEmptyForm()
  if (parentAccount) {
    modalForm.value.parentAccountId = parentAccount.id
    modalForm.value.accountType = parentAccount.accountType
  }
  modalErrors.value = {}
  showModal.value = true
}

async function openEditModal(account) {
  editMode.value = true
  modalErrors.value = {}
  try {
    const res = await accountsApi.getById(account.id)
    const freshData = res.data.data || res.data
    modalForm.value = {
      id: freshData.id,
      code: freshData.code,
      name: freshData.name,
      accountType: freshData.accountType,
      description: freshData.description || '',
      parentAccountId: freshData.parentAccountId,
      controlAccount: freshData.controlAccount || false,
      allowsDirectPosting: freshData.allowsDirectPosting ?? true,
      openingBalance: freshData.openingBalance,
      currency: freshData.currency || 'UZS',
      taxCode: freshData.taxCode || '',
      bankAccountNumber: freshData.bankAccountNumber || '',
      bankName: freshData.bankName || '',
      sortOrder: freshData.sortOrder || 0,
      notes: freshData.notes || ''
    }
  } catch (e) {
    modalForm.value = {
      id: account.id,
      code: account.code,
      name: account.name,
      accountType: account.accountType,
      description: account.description || '',
      parentAccountId: account.parentAccountId,
      controlAccount: account.controlAccount || false,
      allowsDirectPosting: account.allowsDirectPosting ?? true,
      openingBalance: account.openingBalance,
      currency: account.currency || 'UZS',
      taxCode: account.taxCode || '',
      bankAccountNumber: account.bankAccountNumber || '',
      bankName: account.bankName || '',
      sortOrder: account.sortOrder || 0,
      notes: account.notes || ''
    }
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function submitModal() {
  modalErrors.value = {}
  if (!modalForm.value.code.trim()) {
    modalErrors.value.code = t('finance.accounts.codeRequired')
    return
  }
  if (!modalForm.value.name.trim()) {
    modalErrors.value.name = t('finance.accounts.nameRequired')
    return
  }

  saving.value = true
  try {
    if (editMode.value) {
      await accountsApi.update(modalForm.value.id, modalForm.value)
    } else {
      await accountsApi.create(modalForm.value)
    }
    closeModal()
    childrenCache.value = {}
    fetchAccounts()
  } catch (error) {
    modalErrors.value.api = error.response?.data?.message || t('finance.accounts.saveError')
  } finally {
    saving.value = false
  }
}

async function deleteAccount(account) {
  if (account.systemAccount) return
  if (!confirm(t('finance.accounts.confirmDelete', { name: account.name }))) return
  try {
    await accountsApi.delete(account.id)
    childrenCache.value = {}
    fetchAccounts()
  } catch (error) {
    alert(error.response?.data?.message || t('finance.accounts.deleteError'))
  }
}

async function toggleActive(account) {
  if (account.systemAccount) return
  try {
    if (account.active) {
      await accountsApi.deactivate(account.id)
    } else {
      await accountsApi.activate(account.id)
    }
    childrenCache.value = {}
    fetchAccounts()
  } catch (error) {
    alert(error.response?.data?.message || t('finance.accounts.statusError'))
  }
}

async function generateDefaults() {
  if (!confirm(t('finance.accounts.confirmGenerate'))) return
  generating.value = true
  try {
    await accountsApi.generateDefault()
    childrenCache.value = {}
    fetchAccounts()
  } catch (error) {
    alert(error.response?.data?.message || t('finance.accounts.generateError'))
  } finally {
    generating.value = false
  }
}

function openImportModal() {
  importFile.value = null
  importResult.value = null
  showImportModal.value = true
}

function onFileChange(e) {
  importFile.value = e.target.files[0] || null
}

async function submitImport() {
  if (!importFile.value) return
  importing.value = true
  try {
    const res = await accountsApi.importCsv(importFile.value)
    importResult.value = res.data.data || res.data
    childrenCache.value = {}
    fetchAccounts()
  } catch (error) {
    importResult.value = { successCount: 0, errors: [error.response?.data?.message || 'Import failed'] }
  } finally {
    importing.value = false
  }
}

function typeClass(type) {
  const cls = {
    ASSET: 'bg-blue-100 text-blue-700',
    LIABILITY: 'bg-red-100 text-red-700',
    EQUITY: 'bg-purple-100 text-purple-700',
    REVENUE: 'bg-green-100 text-green-700',
    EXPENSE: 'bg-amber-100 text-amber-700'
  }
  return cls[type] || 'bg-gray-100 text-gray-600'
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value || 0)
}

const parentOptions = computed(() => {
  return allAccounts.value.filter(a => a.active && a.id !== modalForm.value.id)
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex justify-between items-start">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.accounts.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.accounts.subtitle') }}</p>
      </div>
      <div class="flex gap-2">
        <button @click="generateDefaults" :disabled="generating" class="btn btn-secondary" :title="$t('finance.accounts.generateHint')">
          <SparklesIcon :class="['h-5 w-5 mr-1', { 'animate-spin': generating }]" />
          {{ $t('finance.accounts.generate') }}
        </button>
        <button @click="openImportModal" class="btn btn-secondary">
          <ArrowUpTrayIcon class="h-5 w-5 mr-1" />
          {{ $t('finance.accounts.import') }}
        </button>
        <button @click="openCreateModal(null)" class="btn btn-primary">
          <PlusIcon class="h-5 w-5 mr-1" />
          {{ $t('finance.accounts.addAccount') }}
        </button>
      </div>
    </div>

    <!-- Type Filter + Search -->
    <div class="flex flex-col sm:flex-row gap-4">
      <div class="border-b border-gray-200 flex-1">
        <nav class="flex gap-1 overflow-x-auto pb-px">
          <button
            @click="filterByType('')"
            :class="['px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
              !activeType ? 'border-primary-500 text-primary-600 bg-primary-50/50' : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
          >{{ $t('all') }}</button>
          <button
            v-for="type in accountTypes" :key="type"
            @click="filterByType(type)"
            :class="['px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
              activeType === type ? 'border-primary-500 text-primary-600 bg-primary-50/50' : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
          >{{ $t(`finance.accounts.type.${type}`) }}</button>
        </nav>
      </div>
      <div class="flex gap-2 w-full sm:w-80">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input v-model="search" type="text" :placeholder="$t('finance.accounts.searchPlaceholder')" class="input pl-10" @keyup.enter="handleSearch" />
        </div>
        <button @click="handleSearch" class="btn-primary btn-sm">{{ $t('search') }}</button>
      </div>
    </div>

    <!-- Quick Filters -->
    <div class="flex flex-wrap gap-2">
      <button
        @click="applyQuickFilter('ACTIVE')"
        :class="[
          'inline-flex items-center px-3 py-1.5 text-sm font-medium rounded-lg border transition-colors',
          quickFilter === 'ACTIVE'
            ? 'bg-green-50 border-green-300 text-green-700'
            : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
        ]"
      >
        <FunnelIcon class="h-4 w-4 mr-1.5" />
        {{ $t('finance.accounts.filterActive') }}
      </button>
      <button
        @click="applyQuickFilter('POSTABLE')"
        :class="[
          'inline-flex items-center px-3 py-1.5 text-sm font-medium rounded-lg border transition-colors',
          quickFilter === 'POSTABLE'
            ? 'bg-blue-50 border-blue-300 text-blue-700'
            : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-50'
        ]"
      >
        <FunnelIcon class="h-4 w-4 mr-1.5" />
        {{ $t('finance.accounts.filterPostable') }}
      </button>
    </div>

    <!-- Code Lookup -->
    <div class="card">
      <div class="card-body">
        <div class="flex gap-2 items-end">
          <div class="flex-1 max-w-sm">
            <label class="label">{{ $t('finance.accounts.codeLookup') }}</label>
            <input
              v-model="codeLookup"
              @keyup.enter="lookupByCode"
              type="text"
              :placeholder="$t('finance.accounts.codeLookupPlaceholder')"
              class="input font-mono"
            />
          </div>
          <button @click="lookupByCode" class="btn-secondary">
            {{ $t('finance.accounts.lookup') }}
          </button>
        </div>
        <!-- Code Lookup Result -->
        <div v-if="codeLookupResult" class="mt-3 p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-center justify-between">
          <div>
            <span class="font-mono font-medium text-sm">{{ codeLookupResult.code }}</span>
            <span class="mx-2 text-gray-400">--</span>
            <span class="text-sm font-medium">{{ codeLookupResult.name }}</span>
            <span :class="['ml-2 text-xs px-2 py-0.5 rounded-full font-medium', typeClass(codeLookupResult.accountType)]">
              {{ $t(`finance.accounts.type.${codeLookupResult.accountType}`) }}
            </span>
          </div>
          <div class="flex items-center gap-2">
            <button @click="openEditModal(codeLookupResult)" class="text-primary-600 hover:text-primary-800 text-sm font-medium">
              {{ $t('edit') }}
            </button>
            <button @click="codeLookupResult = null" class="text-gray-400 hover:text-gray-600">
              <XMarkIcon class="h-4 w-4" />
            </button>
          </div>
        </div>
        <div v-if="codeLookupError" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between">
          <p class="text-sm text-red-600">{{ codeLookupError }}</p>
          <button @click="codeLookupError = ''" class="text-red-400 hover:text-red-600">
            <XMarkIcon class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- Accounts Tree Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="rootAccounts.length === 0" class="text-center py-12">
        <p class="text-gray-500 mb-4">{{ $t('finance.accounts.noAccounts') }}</p>
        <button @click="generateDefaults" :disabled="generating" class="btn btn-primary">
          <SparklesIcon class="h-5 w-5 mr-2" />
          {{ $t('finance.accounts.generate') }}
        </button>
      </div>

      <div v-else class="table-container">
        <table class="table text-sm">
          <thead>
            <tr>
              <th class="w-64">{{ $t('finance.accounts.code') }}</th>
              <th>{{ $t('finance.accounts.name') }}</th>
              <th>{{ $t('finance.accounts.accountType') }}</th>
              <th class="text-right">{{ $t('finance.accounts.balance') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="w-28"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <!-- Root level -->
            <template v-for="account in rootAccounts" :key="account.id">
              <tr :class="{ 'opacity-50': !account.active }">
                <td>
                  <div class="flex items-center">
                    <button
                      v-if="hasChildren(account)"
                      @click="toggleExpand(account)"
                      class="p-0.5 text-gray-400 hover:text-gray-600 mr-1"
                    >
                      <ChevronDownIcon v-if="expandedIds.has(account.id)" class="h-4 w-4" />
                      <ChevronRightIcon v-else class="h-4 w-4" />
                    </button>
                    <span v-else class="w-5 mr-1"></span>
                    <code class="font-mono font-semibold text-gray-900">{{ account.code }}</code>
                  </div>
                </td>
                <td class="font-medium">{{ account.name }}</td>
                <td>
                  <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', typeClass(account.accountType)]">
                    {{ $t(`finance.accounts.type.${account.accountType}`) }}
                  </span>
                </td>
                <td class="text-right font-mono">{{ formatCurrency(account.currentBalance) }}</td>
                <td>
                  <span v-if="account.active" class="text-xs text-green-600">{{ $t('active') }}</span>
                  <span v-else class="text-xs text-gray-400">{{ $t('inactive') }}</span>
                </td>
                <td>
                  <div class="flex items-center justify-end gap-1">
                    <button @click="openCreateModal(account)" class="p-1 text-gray-400 hover:text-green-600 hover:bg-gray-100 rounded" :title="$t('finance.accounts.addChild')">
                      <PlusIcon class="h-4 w-4" />
                    </button>
                    <button @click="openEditModal(account)" class="p-1 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded">
                      <PencilSquareIcon class="h-4 w-4" />
                    </button>
                    <button v-if="!account.systemAccount" @click="toggleActive(account)" class="p-1 rounded hover:bg-gray-100" :class="account.active ? 'text-gray-400 hover:text-amber-600' : 'text-gray-400 hover:text-green-600'">
                      <XCircleIcon v-if="account.active" class="h-4 w-4" />
                      <CheckCircleIcon v-else class="h-4 w-4" />
                    </button>
                    <button v-if="!account.systemAccount" @click="deleteAccount(account)" class="p-1 text-gray-400 hover:text-red-600 hover:bg-gray-100 rounded">
                      <TrashIcon class="h-4 w-4" />
                    </button>
                  </div>
                </td>
              </tr>

              <!-- Children (level 2) -->
              <template v-if="expandedIds.has(account.id)">
                <template v-for="child in getChildren(account.id)" :key="child.id">
                  <tr :class="{ 'opacity-50': !child.active, 'bg-gray-50/50': true }">
                    <td>
                      <div class="flex items-center pl-6">
                        <button
                          v-if="hasChildren(child)"
                          @click="toggleExpand(child)"
                          class="p-0.5 text-gray-400 hover:text-gray-600 mr-1"
                        >
                          <ChevronDownIcon v-if="expandedIds.has(child.id)" class="h-4 w-4" />
                          <ChevronRightIcon v-else class="h-4 w-4" />
                        </button>
                        <span v-else class="w-5 mr-1"></span>
                        <code class="font-mono text-gray-700">{{ child.code }}</code>
                      </div>
                    </td>
                    <td>{{ child.name }}</td>
                    <td>
                      <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', typeClass(child.accountType)]">
                        {{ $t(`finance.accounts.type.${child.accountType}`) }}
                      </span>
                    </td>
                    <td class="text-right font-mono">{{ formatCurrency(child.currentBalance) }}</td>
                    <td>
                      <span v-if="child.active" class="text-xs text-green-600">{{ $t('active') }}</span>
                      <span v-else class="text-xs text-gray-400">{{ $t('inactive') }}</span>
                    </td>
                    <td>
                      <div class="flex items-center justify-end gap-1">
                        <button @click="openCreateModal(child)" class="p-1 text-gray-400 hover:text-green-600 hover:bg-gray-100 rounded">
                          <PlusIcon class="h-4 w-4" />
                        </button>
                        <button @click="openEditModal(child)" class="p-1 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded">
                          <PencilSquareIcon class="h-4 w-4" />
                        </button>
                        <button v-if="!child.systemAccount" @click="toggleActive(child)" class="p-1 rounded hover:bg-gray-100" :class="child.active ? 'text-gray-400 hover:text-amber-600' : 'text-gray-400 hover:text-green-600'">
                          <XCircleIcon v-if="child.active" class="h-4 w-4" />
                          <CheckCircleIcon v-else class="h-4 w-4" />
                        </button>
                        <button v-if="!child.systemAccount" @click="deleteAccount(child)" class="p-1 text-gray-400 hover:text-red-600 hover:bg-gray-100 rounded">
                          <TrashIcon class="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>

                  <!-- Children (level 3) -->
                  <template v-if="expandedIds.has(child.id)">
                    <tr v-for="grandchild in getChildren(child.id)" :key="grandchild.id"
                      :class="{ 'opacity-50': !grandchild.active }"
                    >
                      <td>
                        <div class="flex items-center pl-12">
                          <span class="w-5 mr-1"></span>
                          <code class="font-mono text-gray-500">{{ grandchild.code }}</code>
                        </div>
                      </td>
                      <td class="text-gray-600">{{ grandchild.name }}</td>
                      <td>
                        <span :class="['text-xs px-2 py-0.5 rounded-full font-medium', typeClass(grandchild.accountType)]">
                          {{ $t(`finance.accounts.type.${grandchild.accountType}`) }}
                        </span>
                      </td>
                      <td class="text-right font-mono">{{ formatCurrency(grandchild.currentBalance) }}</td>
                      <td>
                        <span v-if="grandchild.active" class="text-xs text-green-600">{{ $t('active') }}</span>
                        <span v-else class="text-xs text-gray-400">{{ $t('inactive') }}</span>
                      </td>
                      <td>
                        <div class="flex items-center justify-end gap-1">
                          <button @click="openEditModal(grandchild)" class="p-1 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded">
                            <PencilSquareIcon class="h-4 w-4" />
                          </button>
                          <button v-if="!grandchild.systemAccount" @click="toggleActive(grandchild)" class="p-1 rounded hover:bg-gray-100" :class="grandchild.active ? 'text-gray-400 hover:text-amber-600' : 'text-gray-400 hover:text-green-600'">
                            <XCircleIcon v-if="grandchild.active" class="h-4 w-4" />
                            <CheckCircleIcon v-else class="h-4 w-4" />
                          </button>
                          <button v-if="!grandchild.systemAccount" @click="deleteAccount(grandchild)" class="p-1 text-gray-400 hover:text-red-600 hover:bg-gray-100 rounded">
                            <TrashIcon class="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  </template>
                </template>
              </template>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeModal"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-5 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">
                {{ editMode ? $t('finance.accounts.editAccount') : $t('finance.accounts.newAccount') }}
              </h3>
              <button @click="closeModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div v-if="modalErrors.api" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
              {{ modalErrors.api }}
            </div>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.accounts.code') }} <span class="text-red-500">*</span></label>
                  <input v-model="modalForm.code" type="text" class="input font-mono" :class="{ 'border-red-500': modalErrors.code }" placeholder="1000" :disabled="editMode" />
                  <p v-if="modalErrors.code" class="text-sm text-red-500 mt-1">{{ modalErrors.code }}</p>
                </div>
                <div>
                  <label class="label">{{ $t('finance.accounts.accountType') }} <span class="text-red-500">*</span></label>
                  <select v-model="modalForm.accountType" class="input" :disabled="editMode">
                    <option v-for="type in accountTypes" :key="type" :value="type">{{ $t(`finance.accounts.type.${type}`) }}</option>
                  </select>
                </div>
              </div>

              <div>
                <label class="label">{{ $t('finance.accounts.name') }} <span class="text-red-500">*</span></label>
                <input v-model="modalForm.name" type="text" class="input" :class="{ 'border-red-500': modalErrors.name }" />
                <p v-if="modalErrors.name" class="text-sm text-red-500 mt-1">{{ modalErrors.name }}</p>
              </div>

              <div>
                <label class="label">{{ $t('finance.accounts.parentAccount') }}</label>
                <select v-model="modalForm.parentAccountId" class="input">
                  <option :value="null">{{ $t('finance.accounts.noParent') }}</option>
                  <option v-for="acc in parentOptions" :key="acc.id" :value="acc.id">{{ acc.code }} — {{ acc.name }}</option>
                </select>
              </div>

              <div>
                <label class="label">{{ $t('description') }}</label>
                <textarea v-model="modalForm.description" rows="2" class="input"></textarea>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.accounts.openingBalance') }}</label>
                  <input v-model.number="modalForm.openingBalance" type="number" step="0.01" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.accounts.sortOrder') }}</label>
                  <input v-model.number="modalForm.sortOrder" type="number" class="input" min="0" />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.accounts.bankName') }}</label>
                  <input v-model="modalForm.bankName" type="text" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.accounts.bankAccount') }}</label>
                  <input v-model="modalForm.bankAccountNumber" type="text" class="input font-mono" />
                </div>
              </div>

              <div class="flex flex-wrap gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="modalForm.controlAccount" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('finance.accounts.controlAccount') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="modalForm.allowsDirectPosting" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('finance.accounts.allowsPosting') }}</span>
                </label>
              </div>

              <div>
                <label class="label">{{ $t('finance.accounts.notes') }}</label>
                <textarea v-model="modalForm.notes" rows="2" class="input"></textarea>
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button @click="closeModal" class="btn btn-secondary">{{ $t('cancel') }}</button>
              <button @click="submitModal" :disabled="saving" class="btn btn-primary">
                <span v-if="saving" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ editMode ? $t('save') : $t('create') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Import Modal -->
    <Teleport to="body">
      <div v-if="showImportModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="showImportModal = false"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-5">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ $t('finance.accounts.importTitle') }}</h3>
              <button @click="showImportModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <p class="text-sm text-gray-500">{{ $t('finance.accounts.importHint') }}</p>

            <div>
              <input type="file" accept=".csv" @change="onFileChange" class="input text-sm" />
            </div>

            <div v-if="importResult" class="rounded-lg p-3 text-sm" :class="importResult.errors?.length ? 'bg-amber-50 border border-amber-200' : 'bg-green-50 border border-green-200'">
              <p class="font-medium">{{ $t('finance.accounts.importSuccess', { count: importResult.successCount }) }}</p>
              <ul v-if="importResult.errors?.length" class="mt-2 text-red-600 list-disc list-inside">
                <li v-for="(err, i) in importResult.errors" :key="i">{{ err }}</li>
              </ul>
            </div>

            <div class="flex justify-end gap-3">
              <button @click="showImportModal = false" class="btn btn-secondary">{{ $t('close') }}</button>
              <button @click="submitImport" :disabled="!importFile || importing" class="btn btn-primary">
                <span v-if="importing" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ $t('finance.accounts.importBtn') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
