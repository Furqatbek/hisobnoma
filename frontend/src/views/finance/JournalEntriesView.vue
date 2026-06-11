<script setup>
import { useToastStore } from '@/stores/toast'
import { formatDate } from '@/utils/format'
import { ref, onMounted, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { accountsApi, journalEntriesApi, unwrapData, unwrapList } from '@/services/api'
import {
  PlusIcon, MagnifyingGlassIcon, EyeIcon, TrashIcon,
  CheckIcon, ArrowUturnLeftIcon, XMarkIcon, CalendarDaysIcon
} from '@heroicons/vue/24/outline'

const toast = useToastStore()

const { t } = useI18n()

const entries = ref([])
const loading = ref(true)
const search = ref('')
const activeTab = ref('ALL')
const pagination = ref({ page: 0, size: 20, totalPages: 0, totalElements: 0 })

const statusTabs = ['ALL', 'DRAFT', 'POSTED', 'REVERSED']
const sourceTypes = ['MANUAL', 'AR_INVOICE', 'AR_PAYMENT', 'AP_INVOICE', 'AP_PAYMENT', 'PAYROLL']

const dateRangeStart = ref('')
const dateRangeEnd = ref('')
const dateRangeActive = ref(false)

async function fetchEntries() {
  loading.value = true
  try {
    let response
    if (dateRangeActive.value && dateRangeStart.value && dateRangeEnd.value) {
      response = await journalEntriesApi.getByDateRange(dateRangeStart.value, dateRangeEnd.value)
      const data = unwrapData(response)
      entries.value = data.content || data || []
      pagination.value.totalPages = data.page?.totalPages || 0
      pagination.value.totalElements = data.page?.totalElements || Array.isArray(entries.value) ? entries.value.length : 0
    } else if (search.value) {
      response = await journalEntriesApi.search(search.value, {
        page: pagination.value.page,
        size: pagination.value.size
      })
      entries.value = unwrapList(response)
      pagination.value.totalPages = response.data.page?.totalPages || 0
      pagination.value.totalElements = response.data.page?.totalElements || 0
    } else if (activeTab.value !== 'ALL') {
      response = await journalEntriesApi.getByStatus(activeTab.value, {
        page: pagination.value.page,
        size: pagination.value.size
      })
      entries.value = unwrapList(response)
      pagination.value.totalPages = response.data.page?.totalPages || 0
      pagination.value.totalElements = response.data.page?.totalElements || 0
    } else {
      response = await journalEntriesApi.getAll({
        page: pagination.value.page,
        size: pagination.value.size
      })
      entries.value = unwrapList(response)
      pagination.value.totalPages = response.data.page?.totalPages || 0
      pagination.value.totalElements = response.data.page?.totalElements || 0
    }
  } catch (error) {
    console.error('Failed to fetch journal entries:', error)
  } finally {
    loading.value = false
  }
}

function applyDateFilter() {
  if (!dateRangeStart.value || !dateRangeEnd.value) return
  dateRangeActive.value = true
  search.value = ''
  activeTab.value = 'ALL'
  pagination.value.page = 0
  fetchEntries()
}

function clearDateFilter() {
  dateRangeStart.value = ''
  dateRangeEnd.value = ''
  dateRangeActive.value = false
  pagination.value.page = 0
  fetchEntries()
}

onMounted(fetchEntries)

function switchTab(tab) {
  activeTab.value = tab
  pagination.value.page = 0
  search.value = ''
  dateRangeActive.value = false
  fetchEntries()
}

function handleSearch() {
  pagination.value.page = 0
  activeTab.value = 'ALL'
  dateRangeActive.value = false
  fetchEntries()
}

// Detail modal
const showDetail = ref(false)
const detailEntry = ref(null)
const detailLoading = ref(false)

async function openDetail(entry) {
  showDetail.value = true
  detailLoading.value = true
  try {
    // Fetch fresh base data via getById, then enrich with lines
    const [baseRes, linesRes] = await Promise.all([
      journalEntriesApi.getById(entry.id),
      journalEntriesApi.getWithLines(entry.id)
    ])
    const baseData = unwrapData(baseRes)
    const linesData = unwrapData(linesRes)
    detailEntry.value = { ...baseData, lines: linesData.lines || baseData.lines || [] }
  } catch (error) {
    console.error('Failed to load journal entry:', error)
  } finally {
    detailLoading.value = false
  }
}

async function postEntry(entry) {
  if (!confirm(t('finance.journalEntries.confirmPost'))) return
  try {
    await journalEntriesApi.post(entry.id)
    showDetail.value = false
    fetchEntries()
  } catch (error) {
    console.error('Failed to post entry:', error)
    toast.error(error.response?.data?.message || t('finance.journalEntries.postError'))
  }
}

// Reverse modal
const showReverseModal = ref(false)
const reverseEntryId = ref(null)
const reversalDate = ref(new Date().toISOString().split('T')[0])

function openReverseModal(entry) {
  reverseEntryId.value = entry.id
  reversalDate.value = new Date().toISOString().split('T')[0]
  showReverseModal.value = true
}

async function reverseEntry() {
  try {
    await journalEntriesApi.reverse(reverseEntryId.value, reversalDate.value)
    showReverseModal.value = false
    showDetail.value = false
    fetchEntries()
  } catch (error) {
    console.error('Failed to reverse entry:', error)
    toast.error(error.response?.data?.message || t('finance.journalEntries.reverseError'))
  }
}

async function deleteEntry(entry) {
  if (!confirm(t('finance.journalEntries.confirmDelete'))) return
  try {
    await journalEntriesApi.delete(entry.id)
    showDetail.value = false
    fetchEntries()
  } catch (error) {
    console.error('Failed to delete entry:', error)
    toast.error(error.response?.data?.message || t('finance.journalEntries.deleteError'))
  }
}

// Create modal
const showCreateModal = ref(false)
const creating = ref(false)
const accounts = ref([])
const createForm = reactive({
  entryDate: new Date().toISOString().split('T')[0],
  description: '',
  reference: '',
  source: 'MANUAL',
  lines: [
    { accountId: null, description: '', debitAmount: 0, creditAmount: 0 },
    { accountId: null, description: '', debitAmount: 0, creditAmount: 0 }
  ]
})

const totalDebit = computed(() => createForm.lines.reduce((s, l) => s + (Number(l.debitAmount) || 0), 0))
const totalCredit = computed(() => createForm.lines.reduce((s, l) => s + (Number(l.creditAmount) || 0), 0))
const isBalanced = computed(() => Math.abs(totalDebit.value - totalCredit.value) < 0.01)

async function openCreateModal() {
  showCreateModal.value = true
  createForm.entryDate = new Date().toISOString().split('T')[0]
  createForm.description = ''
  createForm.reference = ''
  createForm.source = 'MANUAL'
  createForm.lines = [
    { accountId: null, description: '', debitAmount: 0, creditAmount: 0 },
    { accountId: null, description: '', debitAmount: 0, creditAmount: 0 }
  ]
  if (accounts.value.length === 0) {
    try {
      // Use getPostable() to only show accounts that allow direct posting
      const res = await accountsApi.getPostable()
      const data = unwrapData(res)
      accounts.value = data.content || data || []
    } catch (e) {
      // Fallback to getAll if getPostable fails
      try {
        const res = await accountsApi.getAll()
        accounts.value = unwrapList(res)
      } catch (error) {
        console.error('Failed to load accounts:', error)
      }
    }
  }
}

function addLine() {
  createForm.lines.push({ accountId: null, description: '', debitAmount: 0, creditAmount: 0 })
}

function removeLine(index) {
  if (createForm.lines.length <= 2) return
  createForm.lines.splice(index, 1)
}

async function handleCreate() {
  if (!createForm.description?.trim() || !isBalanced.value) return
  creating.value = true
  try {
    await journalEntriesApi.create({
      entryDate: createForm.entryDate,
      description: createForm.description,
      reference: createForm.reference || undefined,
      source: createForm.source,
      lines: createForm.lines.filter(l => l.accountId).map(l => ({
        accountId: l.accountId,
        description: l.description || undefined,
        debitAmount: Number(l.debitAmount) || 0,
        creditAmount: Number(l.creditAmount) || 0
      }))
    })
    showCreateModal.value = false
    fetchEntries()
  } catch (error) {
    console.error('Failed to create journal entry:', error)
    toast.error(error.response?.data?.message || t('finance.journalEntries.createError'))
  } finally {
    creating.value = false
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0)
}

function statusColor(status) {
  const map = { DRAFT: 'badge-warning', POSTED: 'badge-success', REVERSED: 'badge-danger' }
  return map[status] || 'badge-info'
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.journalEntries.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.journalEntries.subtitle') }}</p>
      </div>
      <button @click="openCreateModal" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('finance.journalEntries.newEntry') }}
      </button>
    </div>

    <!-- Status Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex space-x-4 overflow-x-auto">
        <button
          v-for="tab in statusTabs"
          :key="tab"
          @click="switchTab(tab)"
          :class="[
            'px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors',
            activeTab === tab
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ tab === 'ALL' ? $t('finance.journalEntries.all') : $t(`finance.journalEntries.status.${tab}`) }}
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
            @input="handleSearch"
            type="text"
            :placeholder="$t('finance.journalEntries.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
      </div>
    </div>

    <!-- Date Range Filter -->
    <div class="card">
      <div class="card-body">
        <div class="flex items-center gap-2 mb-3">
          <CalendarDaysIcon class="h-5 w-5 text-gray-500" />
          <h3 class="text-sm font-medium text-gray-700">{{ $t('finance.journalEntries.dateRangeFilter') }}</h3>
        </div>
        <div class="flex flex-wrap items-end gap-3">
          <div>
            <label class="label text-xs">{{ $t('finance.journalEntries.startDate') }}</label>
            <input v-model="dateRangeStart" type="date" class="input text-sm" />
          </div>
          <div>
            <label class="label text-xs">{{ $t('finance.journalEntries.endDate') }}</label>
            <input v-model="dateRangeEnd" type="date" class="input text-sm" />
          </div>
          <button
            @click="applyDateFilter"
            :disabled="!dateRangeStart || !dateRangeEnd"
            class="btn-primary text-sm px-4 py-2"
          >
            {{ $t('finance.journalEntries.applyDateFilter') }}
          </button>
          <button
            v-if="dateRangeActive"
            @click="clearDateFilter"
            class="btn-secondary text-sm px-4 py-2"
          >
            {{ $t('finance.journalEntries.clearDateFilter') }}
          </button>
        </div>
        <div v-if="dateRangeActive" class="mt-2 text-xs text-primary-600">
          {{ $t('finance.journalEntries.dateRangeFilter') }}: {{ dateRangeStart }} — {{ dateRangeEnd }}
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="entries.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('finance.journalEntries.noEntries') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('finance.journalEntries.entryNumber') }}</th>
              <th>{{ $t('finance.journalEntries.date') }}</th>
              <th>{{ $t('description') }}</th>
              <th>{{ $t('finance.journalEntries.source') }}</th>
              <th class="text-right">{{ $t('finance.journalEntries.totalAmount') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="entry in entries" :key="entry.id">
              <td class="font-mono text-sm">{{ entry.entryNumber || `#${entry.id}` }}</td>
              <td>{{ formatDate(entry.entryDate) }}</td>
              <td>
                <div class="max-w-xs truncate">{{ entry.description }}</div>
                <div v-if="entry.reference" class="text-xs text-gray-500">{{ entry.reference }}</div>
              </td>
              <td>
                <span class="badge badge-info text-xs">
                  {{ $t(`finance.journalEntries.sources.${entry.source || 'MANUAL'}`) }}
                </span>
              </td>
              <td class="text-right font-medium">{{ formatCurrency(entry.totalDebit || entry.totalAmount) }}</td>
              <td>
                <span :class="['badge', statusColor(entry.status)]">
                  {{ $t(`finance.journalEntries.status.${entry.status}`) }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-1">
                  <button @click="openDetail(entry)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <EyeIcon class="h-5 w-5" />
                  </button>
                  <button
                    v-if="entry.status === 'DRAFT'"
                    @click="postEntry(entry)"
                    class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100"
                    :title="$t('finance.journalEntries.post')"
                  >
                    <CheckIcon class="h-5 w-5" />
                  </button>
                  <button
                    v-if="entry.status === 'POSTED'"
                    @click="openReverseModal(entry)"
                    class="p-2 text-gray-400 hover:text-orange-600 rounded-lg hover:bg-gray-100"
                    :title="$t('finance.journalEntries.reverse')"
                  >
                    <ArrowUturnLeftIcon class="h-5 w-5" />
                  </button>
                  <button
                    v-if="entry.status === 'DRAFT'"
                    @click="deleteEntry(entry)"
                    class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
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
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <p class="text-sm text-gray-500">{{ pagination.totalElements }} {{ $t('finance.journalEntries.totalRecords') }}</p>
          <div class="flex space-x-2">
            <button @click="pagination.page--; fetchEntries()" :disabled="pagination.page === 0" class="btn-secondary px-3 py-1">
              {{ $t('previous') }}
            </button>
            <button @click="pagination.page++; fetchEntries()" :disabled="pagination.page >= pagination.totalPages - 1" class="btn-secondary px-3 py-1">
              {{ $t('next') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <Teleport to="body">
      <div v-if="showDetail" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDetail = false"></div>
          <div class="relative bg-white rounded-lg max-w-3xl w-full max-h-[80vh] overflow-y-auto">
            <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 sticky top-0 bg-white z-10">
              <h3 class="text-lg font-medium text-gray-900">
                {{ $t('finance.journalEntries.entryDetail') }}
              </h3>
              <button @click="showDetail = false" class="p-1 text-gray-400 hover:text-gray-600">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div v-if="detailLoading" class="flex justify-center py-12">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="detailEntry" class="p-6 space-y-4">
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <p class="text-xs text-gray-500">{{ $t('finance.journalEntries.entryNumber') }}</p>
                  <p class="font-mono font-medium">{{ detailEntry.entryNumber || `#${detailEntry.id}` }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500">{{ $t('finance.journalEntries.date') }}</p>
                  <p class="font-medium">{{ formatDate(detailEntry.entryDate) }}</p>
                </div>
                <div>
                  <p class="text-xs text-gray-500">{{ $t('status') }}</p>
                  <span :class="['badge', statusColor(detailEntry.status)]">
                    {{ $t(`finance.journalEntries.status.${detailEntry.status}`) }}
                  </span>
                </div>
                <div>
                  <p class="text-xs text-gray-500">{{ $t('finance.journalEntries.source') }}</p>
                  <span class="badge badge-info text-xs">
                    {{ $t(`finance.journalEntries.sources.${detailEntry.source || 'MANUAL'}`) }}
                  </span>
                </div>
              </div>

              <div>
                <p class="text-xs text-gray-500">{{ $t('description') }}</p>
                <p>{{ detailEntry.description }}</p>
              </div>

              <div v-if="detailEntry.reference">
                <p class="text-xs text-gray-500">{{ $t('finance.journalEntries.reference') }}</p>
                <p>{{ detailEntry.reference }}</p>
              </div>

              <!-- Lines -->
              <div v-if="detailEntry.lines?.length" class="table-container">
                <table class="table">
                  <thead>
                    <tr>
                      <th>{{ $t('finance.journalEntries.account') }}</th>
                      <th>{{ $t('description') }}</th>
                      <th class="text-right">{{ $t('finance.journalEntries.debit') }}</th>
                      <th class="text-right">{{ $t('finance.journalEntries.credit') }}</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-200">
                    <tr v-for="line in detailEntry.lines" :key="line.id">
                      <td>
                        <span class="font-mono text-sm">{{ line.accountCode }}</span>
                        <span class="ml-1 text-sm text-gray-500">{{ line.accountName }}</span>
                      </td>
                      <td class="text-sm text-gray-500">{{ line.description || '-' }}</td>
                      <td class="text-right font-medium">{{ line.debitAmount > 0 ? formatCurrency(line.debitAmount) : '' }}</td>
                      <td class="text-right font-medium">{{ line.creditAmount > 0 ? formatCurrency(line.creditAmount) : '' }}</td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr class="font-bold bg-gray-50">
                      <td colspan="2" class="text-right">{{ $t('total') }}</td>
                      <td class="text-right">{{ formatCurrency(detailEntry.lines.reduce((s, l) => s + (l.debitAmount || 0), 0)) }}</td>
                      <td class="text-right">{{ formatCurrency(detailEntry.lines.reduce((s, l) => s + (l.creditAmount || 0), 0)) }}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>

              <!-- Actions -->
              <div class="flex justify-end gap-2 pt-4 border-t border-gray-200">
                <button
                  v-if="detailEntry.status === 'DRAFT'"
                  @click="deleteEntry(detailEntry)"
                  class="btn-danger"
                >
                  <TrashIcon class="h-4 w-4 mr-1" />
                  {{ $t('delete') }}
                </button>
                <button
                  v-if="detailEntry.status === 'POSTED'"
                  @click="openReverseModal(detailEntry)"
                  class="btn-secondary"
                >
                  <ArrowUturnLeftIcon class="h-4 w-4 mr-1" />
                  {{ $t('finance.journalEntries.reverse') }}
                </button>
                <button
                  v-if="detailEntry.status === 'DRAFT'"
                  @click="postEntry(detailEntry)"
                  class="btn-primary"
                >
                  <CheckIcon class="h-4 w-4 mr-1" />
                  {{ $t('finance.journalEntries.post') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Reverse Modal -->
    <Teleport to="body">
      <div v-if="showReverseModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showReverseModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('finance.journalEntries.reverseTitle') }}</h3>
            <div>
              <label class="label">{{ $t('finance.journalEntries.reversalDate') }}</label>
              <input v-model="reversalDate" type="date" class="input" />
            </div>
            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showReverseModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="reverseEntry" class="btn-primary">{{ $t('finance.journalEntries.confirmReverse') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Create Modal -->
    <Teleport to="body">
      <div v-if="showCreateModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCreateModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-4xl w-full max-h-[85vh] overflow-y-auto">
            <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 sticky top-0 bg-white z-10">
              <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.journalEntries.newEntry') }}</h3>
              <button @click="showCreateModal = false" class="p-1 text-gray-400 hover:text-gray-600">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="p-6 space-y-4">
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label class="label">{{ $t('finance.journalEntries.date') }} *</label>
                  <input v-model="createForm.entryDate" type="date" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.journalEntries.reference') }}</label>
                  <input v-model="createForm.reference" type="text" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.journalEntries.source') }}</label>
                  <select v-model="createForm.source" class="input">
                    <option v-for="s in sourceTypes" :key="s" :value="s">
                      {{ $t(`finance.journalEntries.sources.${s}`) }}
                    </option>
                  </select>
                </div>
              </div>

              <div>
                <label class="label">{{ $t('description') }} *</label>
                <input v-model="createForm.description" type="text" class="input" :placeholder="$t('finance.journalEntries.descriptionPlaceholder')" />
              </div>

              <!-- Lines -->
              <div>
                <div class="flex items-center justify-between mb-2">
                  <label class="label mb-0">{{ $t('finance.journalEntries.lines') }}</label>
                  <button type="button" @click="addLine" class="text-sm text-primary-600 hover:text-primary-800 flex items-center gap-1">
                    <PlusIcon class="h-4 w-4" />
                    {{ $t('finance.journalEntries.addLine') }}
                  </button>
                </div>

                <div class="table-container">
                  <table class="table">
                    <thead>
                      <tr>
                        <th class="w-1/3">{{ $t('finance.journalEntries.account') }}</th>
                        <th>{{ $t('description') }}</th>
                        <th class="text-right w-32">{{ $t('finance.journalEntries.debit') }}</th>
                        <th class="text-right w-32">{{ $t('finance.journalEntries.credit') }}</th>
                        <th class="w-10"></th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                      <tr v-for="(line, i) in createForm.lines" :key="i">
                        <td>
                          <select v-model="line.accountId" class="input text-sm">
                            <option :value="null">{{ $t('finance.journalEntries.selectAccount') }}</option>
                            <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
                              {{ acc.code }} — {{ acc.name }}
                            </option>
                          </select>
                        </td>
                        <td>
                          <input v-model="line.description" type="text" class="input text-sm" />
                        </td>
                        <td>
                          <input v-model.number="line.debitAmount" type="number" step="0.01" min="0" class="input text-sm text-right" />
                        </td>
                        <td>
                          <input v-model.number="line.creditAmount" type="number" step="0.01" min="0" class="input text-sm text-right" />
                        </td>
                        <td>
                          <button
                            v-if="createForm.lines.length > 2"
                            @click="removeLine(i)"
                            class="p-1 text-gray-400 hover:text-red-600 rounded"
                          >
                            <TrashIcon class="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    </tbody>
                    <tfoot>
                      <tr class="font-bold">
                        <td colspan="2" class="text-right">{{ $t('total') }}</td>
                        <td class="text-right">{{ formatCurrency(totalDebit) }}</td>
                        <td class="text-right">{{ formatCurrency(totalCredit) }}</td>
                        <td></td>
                      </tr>
                    </tfoot>
                  </table>
                </div>

                <div v-if="!isBalanced && (totalDebit > 0 || totalCredit > 0)" class="mt-2 text-sm text-red-600">
                  {{ $t('finance.journalEntries.notBalanced', { diff: formatCurrency(Math.abs(totalDebit - totalCredit)) }) }}
                </div>
              </div>
            </div>

            <div class="flex justify-end gap-3 px-6 py-4 border-t border-gray-200 sticky bottom-0 bg-white">
              <button @click="showCreateModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button
                @click="handleCreate"
                :disabled="creating || !isBalanced || !createForm.description?.trim()"
                class="btn-primary"
              >
                {{ creating ? $t('saving') : $t('finance.journalEntries.createEntry') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
