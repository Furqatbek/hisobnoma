<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { recurringJournalsApi, accountsApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  PlayIcon, XMarkIcon, ArrowPathIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const journals = ref([])
const accounts = ref([])
const loading = ref(true)
const error = ref('')
const successMsg = ref('')
const search = ref('')
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = 20

const frequencies = ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY']

// Modal state
const showModal = ref(false)
const editingJournal = ref(null)

const form = reactive({
  name: '',
  description: '',
  frequency: 'MONTHLY',
  nextRunDate: '',
  lines: [],
  active: true
})

// Delete confirmation
const showDeleteConfirm = ref(false)
const deletingJournal = ref(null)

function createEmptyLine() {
  return { accountId: '', debit: 0, credit: 0, description: '' }
}

async function fetchJournals(page = 0) {
  loading.value = true
  error.value = ''
  try {
    const res = await recurringJournalsApi.getAll({ page, size: pageSize, search: search.value.trim() || undefined })
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      journals.value = data
      totalPages.value = 1
      currentPage.value = 0
    } else {
      journals.value = data.content || []
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

async function fetchAccounts() {
  try {
    const res = await accountsApi.getAll()
    const data = res.data.data || res.data
    if (Array.isArray(data)) {
      accounts.value = data
    } else {
      accounts.value = data.content || []
    }
  } catch (e) {
    // silent — accounts are supplementary
  }
}

onMounted(() => {
  fetchJournals(0)
  fetchAccounts()
})

function handleSearch() {
  fetchJournals(0)
}

function openModal(journal = null) {
  editingJournal.value = journal
  if (journal) {
    form.name = journal.name || ''
    form.description = journal.description || ''
    form.frequency = journal.frequency || 'MONTHLY'
    form.nextRunDate = journal.nextRunDate || ''
    form.lines = journal.lines && journal.lines.length
      ? journal.lines.map(l => ({
          accountId: l.accountId || l.account?.id || '',
          debit: l.debit || 0,
          credit: l.credit || 0,
          description: l.description || ''
        }))
      : [createEmptyLine()]
    form.active = journal.active !== false
  } else {
    form.name = ''
    form.description = ''
    form.frequency = 'MONTHLY'
    form.nextRunDate = ''
    form.lines = [createEmptyLine()]
    form.active = true
  }
  showModal.value = true
}

function addLine() {
  form.lines.push(createEmptyLine())
}

function removeLine(index) {
  form.lines.splice(index, 1)
}

async function saveJournal() {
  if (!form.name?.trim()) return
  error.value = ''
  try {
    const payload = {
      name: form.name,
      description: form.description,
      frequency: form.frequency,
      nextRunDate: form.nextRunDate,
      lines: form.lines,
      active: form.active
    }
    if (editingJournal.value) {
      await recurringJournalsApi.update(editingJournal.value.id, payload)
      successMsg.value = t('finance.recurringJournals.updateSuccess')
    } else {
      await recurringJournalsApi.create(payload)
      successMsg.value = t('finance.recurringJournals.createSuccess')
    }
    showModal.value = false
    fetchJournals(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function toggleStatus(journal) {
  error.value = ''
  try {
    if (journal.active) {
      await recurringJournalsApi.deactivate(journal.id)
    } else {
      await recurringJournalsApi.activate(journal.id)
    }
    fetchJournals(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function executeJournal(journal) {
  error.value = ''
  try {
    await recurringJournalsApi.execute(journal.id)
    successMsg.value = t('finance.recurringJournals.executeSuccess')
    fetchJournals(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function confirmDelete(journal) {
  deletingJournal.value = journal
  showDeleteConfirm.value = true
}

async function deleteJournal() {
  if (!deletingJournal.value) return
  error.value = ''
  try {
    await recurringJournalsApi.delete(deletingJournal.value.id)
    successMsg.value = t('finance.recurringJournals.deleteSuccess')
    showDeleteConfirm.value = false
    deletingJournal.value = null
    fetchJournals(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showDeleteConfirm.value = false
  }
}

function getFrequencyClass(frequency) {
  switch (frequency) {
    case 'DAILY': return 'badge-info'
    case 'WEEKLY': return 'badge-success'
    case 'MONTHLY': return 'badge-warning'
    case 'QUARTERLY': return 'badge-primary'
    case 'YEARLY': return 'badge-danger'
    default: return 'badge-info'
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.recurringJournals.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.recurringJournals.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('finance.recurringJournals.addJournal') }}
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

    <!-- Search -->
    <div class="card">
      <div class="card-body">
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @keyup.enter="handleSearch"
            type="text"
            :placeholder="$t('finance.recurringJournals.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
      </div>
    </div>

    <!-- Journals Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="journals.length === 0" class="text-center py-12">
        <ArrowPathIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('finance.recurringJournals.noJournals') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('finance.recurringJournals.name') }}</th>
              <th>{{ $t('finance.recurringJournals.description') }}</th>
              <th>{{ $t('finance.recurringJournals.frequency') }}</th>
              <th>{{ $t('finance.recurringJournals.nextRunDate') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="journal in journals" :key="journal.id">
              <td>
                <p class="font-medium text-sm">{{ journal.name }}</p>
              </td>
              <td class="text-sm text-gray-500">{{ journal.description || '-' }}</td>
              <td>
                <span :class="['badge text-xs', getFrequencyClass(journal.frequency)]">
                  {{ $t(`finance.recurringJournals.freq.${journal.frequency}`) }}
                </span>
              </td>
              <td class="text-sm text-gray-500">{{ journal.nextRunDate || '-' }}</td>
              <td>
                <button
                  @click="toggleStatus(journal)"
                  :class="['badge cursor-pointer text-xs', journal.active ? 'badge-success' : 'badge-danger']"
                >
                  {{ journal.active ? $t('active') : $t('inactive') }}
                </button>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-1">
                  <button
                    @click="executeJournal(journal)"
                    class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100"
                    :title="$t('finance.recurringJournals.execute')"
                  >
                    <PlayIcon class="h-5 w-5" />
                  </button>
                  <button
                    @click="openModal(journal)"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                    :title="$t('edit')"
                  >
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button
                    @click="confirmDelete(journal)"
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
            @click="fetchJournals(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >{{ $t('previous') }}</button>
          <button
            @click="fetchJournals(currentPage + 1)"
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
          <div class="relative bg-white rounded-lg max-w-2xl w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ editingJournal ? $t('finance.recurringJournals.editJournal') : $t('finance.recurringJournals.newJournal') }}
              </h3>
              <button @click="showModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('finance.recurringJournals.name') }} <span class="text-red-500">*</span></label>
                <input v-model="form.name" type="text" class="input" :placeholder="$t('finance.recurringJournals.namePlaceholder')" />
              </div>

              <div>
                <label class="label">{{ $t('finance.recurringJournals.description') }}</label>
                <textarea v-model="form.description" rows="2" class="input" :placeholder="$t('finance.recurringJournals.descriptionPlaceholder')"></textarea>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.recurringJournals.frequency') }} <span class="text-red-500">*</span></label>
                  <select v-model="form.frequency" class="input">
                    <option v-for="freq in frequencies" :key="freq" :value="freq">
                      {{ $t(`finance.recurringJournals.freq.${freq}`) }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('finance.recurringJournals.nextRunDate') }}</label>
                  <input v-model="form.nextRunDate" type="date" class="input" />
                </div>
              </div>

              <!-- Journal Lines -->
              <div>
                <div class="flex items-center justify-between mb-2">
                  <label class="label mb-0">{{ $t('finance.recurringJournals.journalLines') }}</label>
                  <button @click="addLine" type="button" class="btn-secondary text-xs">
                    <PlusIcon class="h-4 w-4 mr-1" />
                    {{ $t('finance.recurringJournals.addLine') }}
                  </button>
                </div>
                <div class="space-y-3">
                  <div
                    v-for="(line, index) in form.lines"
                    :key="index"
                    class="border border-gray-200 rounded-lg p-3 space-y-2"
                  >
                    <div class="flex items-center justify-between">
                      <span class="text-xs font-medium text-gray-500">{{ $t('finance.recurringJournals.line') }} {{ index + 1 }}</span>
                      <button
                        v-if="form.lines.length > 1"
                        @click="removeLine(index)"
                        type="button"
                        class="p-1 text-gray-400 hover:text-red-600 rounded"
                      >
                        <XMarkIcon class="h-4 w-4" />
                      </button>
                    </div>
                    <div>
                      <label class="label text-xs">{{ $t('finance.recurringJournals.account') }}</label>
                      <select v-model="line.accountId" class="input text-sm">
                        <option value="">{{ $t('finance.recurringJournals.selectAccount') }}</option>
                        <option v-for="account in accounts" :key="account.id" :value="account.id">
                          {{ account.accountCode }} - {{ account.name }}
                        </option>
                      </select>
                    </div>
                    <div class="grid grid-cols-2 gap-3">
                      <div>
                        <label class="label text-xs">{{ $t('finance.recurringJournals.debit') }}</label>
                        <input v-model.number="line.debit" type="number" step="0.01" min="0" class="input text-sm" />
                      </div>
                      <div>
                        <label class="label text-xs">{{ $t('finance.recurringJournals.credit') }}</label>
                        <input v-model.number="line.credit" type="number" step="0.01" min="0" class="input text-sm" />
                      </div>
                    </div>
                    <div>
                      <label class="label text-xs">{{ $t('finance.recurringJournals.lineDescription') }}</label>
                      <input v-model="line.description" type="text" class="input text-sm" :placeholder="$t('finance.recurringJournals.lineDescriptionPlaceholder')" />
                    </div>
                  </div>
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
              <button @click="saveJournal" class="btn-primary">{{ $t('save') }}</button>
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
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.recurringJournals.confirmDeleteTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-6">
              {{ $t('finance.recurringJournals.confirmDelete', { name: deletingJournal?.name }) }}
            </p>
            <div class="flex justify-end space-x-3">
              <button @click="showDeleteConfirm = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="deleteJournal" class="btn-danger">{{ $t('delete') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
