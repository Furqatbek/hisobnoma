<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { creditNotesApi, customersApi, unwrapData, unwrapList } from '@/services/api'
import { PlusIcon, EyeIcon, XMarkIcon, CheckIcon, NoSymbolIcon, PencilIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'

const toast = useToastStore()

const { t } = useI18n()

const entries = ref([])
const loading = ref(true)
const activeTab = ref('ALL')
const pagination = ref({ page: 0, size: 20, totalPages: 0, totalElements: 0 })
const statusTabs = ['ALL', 'DRAFT', 'APPROVED', 'APPLIED', 'CANCELLED']

async function fetchEntries() {
  loading.value = true
  try {
    const params = { page: pagination.value.page, size: pagination.value.size }
    const res = activeTab.value !== 'ALL'
      ? await creditNotesApi.getByStatus(activeTab.value, params)
      : await creditNotesApi.getAll(params)
    entries.value = unwrapList(res)
    pagination.value.totalPages = res.data.page?.totalPages || 0
    pagination.value.totalElements = res.data.page?.totalElements || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

// Customer filter
const customerFilter = ref(null)
const filterCustomers = ref([])
const availableCredit = ref(null)
const availableCreditNotes = ref([])

// Number search
const numberSearch = ref('')
const numberSearchResult = ref(null)
const numberSearchError = ref('')

// Edit
const showEdit = ref(false)
const editingId = ref(null)
const editForm = reactive({ customerId: null, reason: '', lines: [{ description: '', quantity: 1, unitPrice: 0 }] })
const updating = ref(false)

async function loadFilterCustomers() {
  if (!filterCustomers.value.length) {
    try { filterCustomers.value = (await customersApi.getActive()).data || [] } catch (e) { console.error(e) }
  }
}

async function onCustomerFilter() {
  pagination.value.page = 0
  availableCredit.value = null
  availableCreditNotes.value = []
  if (customerFilter.value) {
    loading.value = true
    try {
      const params = { page: pagination.value.page, size: pagination.value.size }
      const res = await creditNotesApi.getByCustomer(customerFilter.value, params)
      entries.value = unwrapList(res)
      pagination.value.totalPages = res.data.page?.totalPages || 0
      pagination.value.totalElements = res.data.page?.totalElements || 0
    } catch (e) { console.error(e) }
    finally { loading.value = false }
    // Also fetch available credit info
    try {
      const creditRes = await creditNotesApi.getAvailableCredit(customerFilter.value)
      availableCredit.value = creditRes.data.data ?? creditRes.data
    } catch (e) { console.error(e) }
    try {
      const notesRes = await creditNotesApi.getAvailableByCustomer(customerFilter.value)
      const data = unwrapData(notesRes)
      availableCreditNotes.value = data.content || data || []
    } catch (e) { console.error(e) }
  } else {
    fetchEntries()
  }
}

async function searchByNumber() {
  numberSearchResult.value = null
  numberSearchError.value = ''
  if (!numberSearch.value.trim()) return
  try {
    const res = await creditNotesApi.getByNumber(numberSearch.value.trim())
    numberSearchResult.value = unwrapData(res)
  } catch (e) {
    numberSearchError.value = t('finance.creditNotes.numberNotFound')
  }
}

async function openEdit(entry) {
  editingId.value = entry.id
  try {
    const res = await creditNotesApi.getById(entry.id)
    const data = unwrapData(res)
    editForm.customerId = data.customerId || null
    editForm.reason = data.reason || ''
    editForm.lines = data.lines?.length ? data.lines.map(l => ({ description: l.description || '', quantity: l.quantity || 1, unitPrice: l.unitPrice || 0 })) : [{ description: '', quantity: 1, unitPrice: 0 }]
  } catch (e) {
    editForm.customerId = entry.customerId || null
    editForm.reason = entry.reason || ''
    editForm.lines = [{ description: '', quantity: 1, unitPrice: 0 }]
  }
  if (!customers.value.length) {
    try { customers.value = (await customersApi.getActive()).data || [] } catch (e) { console.error(e) }
  }
  showEdit.value = true
  showDetail.value = false
}

function addEditLine() { editForm.lines.push({ description: '', quantity: 1, unitPrice: 0 }) }
function removeEditLine(i) { if (editForm.lines.length > 1) editForm.lines.splice(i, 1) }

async function handleUpdate() {
  if (!editForm.customerId) return
  updating.value = true
  try {
    await creditNotesApi.update(editingId.value, editForm)
    showEdit.value = false
    fetchEntries()
  } catch (e) { toast.error(e.response?.data?.message || 'Error') }
  finally { updating.value = false }
}

onMounted(() => { fetchEntries(); loadFilterCustomers() })

function switchTab(tab) { activeTab.value = tab; pagination.value.page = 0; customerFilter.value = null; availableCredit.value = null; availableCreditNotes.value = []; fetchEntries() }

// Detail
const showDetail = ref(false)
const detail = ref(null)
async function openDetail(entry) {
  showDetail.value = true
  try { detail.value = (await creditNotesApi.getById(entry.id)).data } catch (e) { console.error(e) }
}

async function approveNote() {
  try { await creditNotesApi.approve(detail.value.id); showDetail.value = false; fetchEntries() }
  catch (e) { toast.error(e.response?.data?.message || 'Error') }
}

// Cancel
const showCancelModal = ref(false)
const cancelReason = ref('')
const cancelId = ref(null)
function openCancel(entry) { cancelId.value = entry.id; cancelReason.value = ''; showCancelModal.value = true }
async function confirmCancel() {
  try { await creditNotesApi.cancel(cancelId.value, cancelReason.value); showCancelModal.value = false; showDetail.value = false; fetchEntries() }
  catch (e) { toast.error(e.response?.data?.message || 'Error') }
}

// Apply to invoice
const showApplyModal = ref(false)
const applyForm = reactive({ invoiceId: null, amount: 0 })
function openApply(entry) { applyForm.invoiceId = null; applyForm.amount = 0; showApplyModal.value = true }
async function confirmApply() {
  try { await creditNotesApi.applyToInvoice(detail.value.id, applyForm.invoiceId, applyForm.amount); showApplyModal.value = false; showDetail.value = false; fetchEntries() }
  catch (e) { toast.error(e.response?.data?.message || 'Error') }
}

// Create
const showCreate = ref(false)
const customers = ref([])
const creating = ref(false)
const createForm = reactive({ customerId: null, reason: '', lines: [{ description: '', quantity: 1, unitPrice: 0 }] })

async function openCreate() {
  showCreate.value = true
  Object.assign(createForm, { customerId: null, reason: '', lines: [{ description: '', quantity: 1, unitPrice: 0 }] })
  if (!customers.value.length) {
    try { customers.value = (await customersApi.getActive()).data || [] } catch (e) { console.error(e) }
  }
}
function addLine() { createForm.lines.push({ description: '', quantity: 1, unitPrice: 0 }) }
function removeLine(i) { if (createForm.lines.length > 1) createForm.lines.splice(i, 1) }

async function handleCreate() {
  if (!createForm.customerId) return
  creating.value = true
  try { await creditNotesApi.create(createForm); showCreate.value = false; fetchEntries() }
  catch (e) { toast.error(e.response?.data?.message || 'Error') }
  finally { creating.value = false }
}

function formatCurrency(v) { return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v || 0) }
function formatDate(d) { return d ? new Date(d).toLocaleDateString('uz-UZ') : '-' }
function statusColor(s) { return { DRAFT: 'badge-warning', APPROVED: 'badge-info', APPLIED: 'badge-success', CANCELLED: 'badge-danger' }[s] || 'badge-info' }
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.creditNotes.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.creditNotes.subtitle') }}</p>
      </div>
      <button @click="openCreate" class="btn-primary"><PlusIcon class="h-5 w-5 mr-2" />{{ $t('finance.creditNotes.newNote') }}</button>
    </div>

    <div class="border-b border-gray-200">
      <nav class="flex space-x-4 overflow-x-auto">
        <button v-for="tab in statusTabs" :key="tab" @click="switchTab(tab)" :class="['px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors', activeTab === tab ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700']">
          {{ tab === 'ALL' ? $t('finance.creditNotes.all') : $t(`finance.creditNotes.status.${tab}`) }}
        </button>
      </nav>
    </div>

    <!-- Filters: Customer & Number Search -->
    <div class="card">
      <div class="card-body">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- Customer filter -->
          <div>
            <label class="label">{{ $t('finance.creditNotes.filterByCustomer') }}</label>
            <select v-model="customerFilter" @change="onCustomerFilter" class="input">
              <option :value="null">{{ $t('finance.creditNotes.allCustomers') }}</option>
              <option v-for="c in filterCustomers" :key="c.id" :value="c.id">{{ c.name }} ({{ c.code }})</option>
            </select>
          </div>
          <!-- Number search -->
          <div>
            <label class="label">{{ $t('finance.creditNotes.numberSearch') }}</label>
            <div class="flex gap-2">
              <input v-model="numberSearch" @keyup.enter="searchByNumber" type="text" class="input flex-1" :placeholder="$t('finance.creditNotes.numberSearchPlaceholder')" />
              <button @click="searchByNumber" class="btn-secondary px-3">
                <MagnifyingGlassIcon class="h-5 w-5" />
              </button>
            </div>
          </div>
          <!-- Available credit (shown when customer selected) -->
          <div v-if="customerFilter && availableCredit !== null">
            <label class="label">{{ $t('finance.creditNotes.availableCredit') }}</label>
            <div class="p-3 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-lg font-bold text-green-800">{{ formatCurrency(availableCredit) }}</p>
            </div>
          </div>
        </div>
        <!-- Number search result -->
        <div v-if="numberSearchResult" class="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
          <div class="flex items-center justify-between">
            <div class="text-sm">
              <span class="font-mono font-medium">{{ numberSearchResult.creditNoteNumber }}</span>
              <span class="text-gray-500 ml-2">{{ numberSearchResult.customerName }}</span>
              <span class="ml-2 font-medium">{{ formatCurrency(numberSearchResult.totalAmount) }}</span>
              <span :class="['badge ml-2', statusColor(numberSearchResult.status)]">{{ $t(`finance.creditNotes.status.${numberSearchResult.status}`) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <button @click="openDetail(numberSearchResult)" class="p-1 text-primary-600 hover:text-primary-800"><EyeIcon class="h-4 w-4" /></button>
              <button @click="numberSearchResult = null" class="text-gray-400 hover:text-gray-600 text-sm">&times;</button>
            </div>
          </div>
        </div>
        <div v-if="numberSearchError" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-600">{{ numberSearchError }}</p>
        </div>
        <!-- Available credit notes for selected customer -->
        <div v-if="customerFilter && availableCreditNotes.length" class="mt-4">
          <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('finance.creditNotes.availableCreditNotes') }}</h4>
          <div class="space-y-2">
            <div v-for="cn in availableCreditNotes" :key="cn.id" class="flex items-center justify-between p-2 bg-gray-50 rounded-lg text-sm">
              <div>
                <span class="font-mono">{{ cn.creditNoteNumber }}</span>
                <span class="text-gray-500 ml-2">{{ formatCurrency(cn.totalAmount) }}</span>
              </div>
              <button @click="openDetail(cn)" class="text-primary-600 hover:text-primary-800"><EyeIcon class="h-4 w-4" /></button>
            </div>
          </div>
        </div>
        <div v-if="customerFilter && availableCreditNotes.length === 0 && availableCredit !== null" class="mt-4">
          <p class="text-sm text-gray-500">{{ $t('finance.creditNotes.noAvailableNotes') }}</p>
        </div>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64"><div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div></div>
      <div v-else-if="entries.length === 0" class="text-center py-12"><p class="text-gray-500">{{ $t('finance.creditNotes.noNotes') }}</p></div>
      <div v-else class="table-container">
        <table class="table">
          <thead><tr>
            <th>{{ $t('finance.creditNotes.number') }}</th><th>{{ $t('finance.creditNotes.customer') }}</th><th>{{ $t('finance.creditNotes.date') }}</th>
            <th class="text-right">{{ $t('finance.creditNotes.totalAmount') }}</th><th class="text-right">{{ $t('finance.creditNotes.appliedAmount') }}</th>
            <th>{{ $t('status') }}</th><th class="text-right">{{ $t('actions') }}</th>
          </tr></thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="e in entries" :key="e.id">
              <td class="font-mono text-sm">{{ e.creditNoteNumber || `#${e.id}` }}</td>
              <td>{{ e.customerName || '-' }}</td>
              <td>{{ formatDate(e.creditNoteDate || e.createdAt) }}</td>
              <td class="text-right font-medium">{{ formatCurrency(e.totalAmount) }}</td>
              <td class="text-right">{{ formatCurrency(e.appliedAmount) }}</td>
              <td><span :class="['badge', statusColor(e.status)]">{{ $t(`finance.creditNotes.status.${e.status}`) }}</span></td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-1">
                  <button @click="openDetail(e)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"><EyeIcon class="h-5 w-5" /></button>
                  <button v-if="e.status === 'DRAFT'" @click="openEdit(e)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"><PencilIcon class="h-5 w-5" /></button>
                  <button v-if="e.status === 'DRAFT'" @click="openCancel(e)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"><NoSymbolIcon class="h-5 w-5" /></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <p class="text-sm text-gray-500">{{ pagination.totalElements }} {{ $t('finance.creditNotes.totalRecords') }}</p>
          <div class="flex space-x-2">
            <button @click="pagination.page--; fetchEntries()" :disabled="pagination.page === 0" class="btn-secondary px-3 py-1">{{ $t('previous') }}</button>
            <button @click="pagination.page++; fetchEntries()" :disabled="pagination.page >= pagination.totalPages - 1" class="btn-secondary px-3 py-1">{{ $t('next') }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <Teleport to="body"><div v-if="showDetail && detail" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDetail = false"></div>
        <div class="relative bg-white rounded-lg max-w-2xl w-full max-h-[80vh] overflow-y-auto p-6">
          <div class="flex justify-between mb-4">
            <h3 class="text-lg font-medium">{{ detail.creditNoteNumber }}</h3>
            <button @click="showDetail = false" class="p-1 text-gray-400 hover:text-gray-600"><XMarkIcon class="h-5 w-5" /></button>
          </div>
          <div class="grid grid-cols-2 gap-4 mb-4 text-sm">
            <div><span class="text-gray-500">{{ $t('finance.creditNotes.customer') }}:</span> {{ detail.customerName }}</div>
            <div><span class="text-gray-500">{{ $t('finance.creditNotes.date') }}:</span> {{ formatDate(detail.creditNoteDate) }}</div>
            <div><span class="text-gray-500">{{ $t('status') }}:</span> <span :class="['badge', statusColor(detail.status)]">{{ $t(`finance.creditNotes.status.${detail.status}`) }}</span></div>
            <div><span class="text-gray-500">{{ $t('finance.creditNotes.totalAmount') }}:</span> {{ formatCurrency(detail.totalAmount) }}</div>
          </div>
          <div v-if="detail.reason" class="mb-4 text-sm"><span class="text-gray-500">{{ $t('finance.creditNotes.reason') }}:</span> {{ detail.reason }}</div>
          <div v-if="detail.lines?.length" class="table-container mb-4">
            <table class="table"><thead><tr><th>{{ $t('description') }}</th><th class="text-right">{{ $t('quantity') }}</th><th class="text-right">{{ $t('price') }}</th><th class="text-right">{{ $t('total') }}</th></tr></thead>
              <tbody class="divide-y divide-gray-200"><tr v-for="l in detail.lines" :key="l.id"><td>{{ l.description }}</td><td class="text-right">{{ l.quantity }}</td><td class="text-right">{{ formatCurrency(l.unitPrice) }}</td><td class="text-right font-medium">{{ formatCurrency(l.amount || l.quantity * l.unitPrice) }}</td></tr></tbody>
            </table>
          </div>
          <div class="flex justify-end gap-2">
            <button v-if="detail.status === 'DRAFT'" @click="openCancel(detail)" class="btn-danger"><NoSymbolIcon class="h-4 w-4 mr-1" />{{ $t('cancel') }}</button>
            <button v-if="detail.status === 'DRAFT'" @click="openEdit(detail)" class="btn-secondary"><PencilIcon class="h-4 w-4 mr-1" />{{ $t('finance.creditNotes.editNote') }}</button>
            <button v-if="detail.status === 'APPROVED'" @click="openApply(detail)" class="btn-secondary">{{ $t('finance.creditNotes.applyToInvoice') }}</button>
            <button v-if="detail.status === 'DRAFT'" @click="approveNote" class="btn-primary"><CheckIcon class="h-4 w-4 mr-1" />{{ $t('finance.creditNotes.approve') }}</button>
          </div>
        </div>
      </div>
    </div></Teleport>

    <!-- Cancel Modal -->
    <Teleport to="body"><div v-if="showCancelModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCancelModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
          <h3 class="text-lg font-medium mb-4">{{ $t('finance.creditNotes.cancelNote') }}</h3>
          <label class="label">{{ $t('finance.creditNotes.cancelReason') }}</label>
          <textarea v-model="cancelReason" rows="3" class="input"></textarea>
          <div class="mt-4 flex justify-end gap-3">
            <button @click="showCancelModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="confirmCancel" :disabled="!cancelReason.trim()" class="btn-danger">{{ $t('finance.creditNotes.confirmCancel') }}</button>
          </div>
        </div>
      </div>
    </div></Teleport>

    <!-- Apply to Invoice Modal -->
    <Teleport to="body"><div v-if="showApplyModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showApplyModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
          <h3 class="text-lg font-medium mb-4">{{ $t('finance.creditNotes.applyToInvoice') }}</h3>
          <div class="space-y-4">
            <div><label class="label">{{ $t('finance.creditNotes.invoiceId') }}</label><input v-model.number="applyForm.invoiceId" type="number" class="input" /></div>
            <div><label class="label">{{ $t('finance.creditNotes.applyAmount') }}</label><input v-model.number="applyForm.amount" type="number" step="0.01" class="input" /></div>
          </div>
          <div class="mt-4 flex justify-end gap-3">
            <button @click="showApplyModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="confirmApply" :disabled="!applyForm.invoiceId || !applyForm.amount" class="btn-primary">{{ $t('finance.creditNotes.apply') }}</button>
          </div>
        </div>
      </div>
    </div></Teleport>

    <!-- Create Modal -->
    <Teleport to="body"><div v-if="showCreate" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCreate = false"></div>
        <div class="relative bg-white rounded-lg max-w-2xl w-full max-h-[85vh] overflow-y-auto p-6">
          <h3 class="text-lg font-medium mb-4">{{ $t('finance.creditNotes.newNote') }}</h3>
          <div class="space-y-4">
            <div><label class="label">{{ $t('finance.creditNotes.customer') }} *</label>
              <select v-model="createForm.customerId" class="input">
                <option :value="null">{{ $t('finance.creditNotes.selectCustomer') }}</option>
                <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }} ({{ c.code }})</option>
              </select>
            </div>
            <div><label class="label">{{ $t('finance.creditNotes.reason') }}</label><textarea v-model="createForm.reason" rows="2" class="input"></textarea></div>
            <div>
              <div class="flex items-center justify-between mb-2"><label class="label mb-0">{{ $t('finance.creditNotes.lines') }}</label><button type="button" @click="addLine" class="text-sm text-primary-600 hover:text-primary-800">+ {{ $t('finance.creditNotes.addLine') }}</button></div>
              <div v-for="(line, i) in createForm.lines" :key="i" class="flex gap-2 mb-2">
                <input v-model="line.description" type="text" class="input flex-1" :placeholder="$t('description')" />
                <input v-model.number="line.quantity" type="number" min="1" class="input w-20" />
                <input v-model.number="line.unitPrice" type="number" step="0.01" min="0" class="input w-28" :placeholder="$t('price')" />
                <button v-if="createForm.lines.length > 1" @click="removeLine(i)" class="p-2 text-red-500 hover:text-red-700">&times;</button>
              </div>
            </div>
          </div>
          <div class="mt-6 flex justify-end gap-3">
            <button @click="showCreate = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="handleCreate" :disabled="creating || !createForm.customerId" class="btn-primary">{{ creating ? $t('saving') : $t('save') }}</button>
          </div>
        </div>
      </div>
    </div></Teleport>

    <!-- Edit Modal -->
    <Teleport to="body"><div v-if="showEdit" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showEdit = false"></div>
        <div class="relative bg-white rounded-lg max-w-2xl w-full max-h-[85vh] overflow-y-auto p-6">
          <h3 class="text-lg font-medium mb-4">{{ $t('finance.creditNotes.editNote') }}</h3>
          <div class="space-y-4">
            <div><label class="label">{{ $t('finance.creditNotes.customer') }} *</label>
              <select v-model="editForm.customerId" class="input">
                <option :value="null">{{ $t('finance.creditNotes.selectCustomer') }}</option>
                <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }} ({{ c.code }})</option>
              </select>
            </div>
            <div><label class="label">{{ $t('finance.creditNotes.reason') }}</label><textarea v-model="editForm.reason" rows="2" class="input"></textarea></div>
            <div>
              <div class="flex items-center justify-between mb-2"><label class="label mb-0">{{ $t('finance.creditNotes.lines') }}</label><button type="button" @click="addEditLine" class="text-sm text-primary-600 hover:text-primary-800">+ {{ $t('finance.creditNotes.addLine') }}</button></div>
              <div v-for="(line, i) in editForm.lines" :key="i" class="flex gap-2 mb-2">
                <input v-model="line.description" type="text" class="input flex-1" :placeholder="$t('description')" />
                <input v-model.number="line.quantity" type="number" min="1" class="input w-20" />
                <input v-model.number="line.unitPrice" type="number" step="0.01" min="0" class="input w-28" :placeholder="$t('price')" />
                <button v-if="editForm.lines.length > 1" @click="removeEditLine(i)" class="p-2 text-red-500 hover:text-red-700">&times;</button>
              </div>
            </div>
          </div>
          <div class="mt-6 flex justify-end gap-3">
            <button @click="showEdit = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="handleUpdate" :disabled="updating || !editForm.customerId" class="btn-primary">{{ updating ? $t('saving') : $t('save') }}</button>
          </div>
        </div>
      </div>
    </div></Teleport>
  </div>
</template>
