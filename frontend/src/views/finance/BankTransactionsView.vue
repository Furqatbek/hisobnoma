<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { bankAccountsApi, bankReconciliationsApi, bankTransactionsApi, unwrapData } from '@/services/api'
import {
  PlusIcon, CheckIcon, XMarkIcon, NoSymbolIcon,
  ArrowsRightLeftIcon, MagnifyingGlassIcon, BanknotesIcon,
  ChartBarIcon, PlayIcon, StopIcon, DocumentCheckIcon,
  EyeIcon, ListBulletIcon, CheckBadgeIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

// ─── Shared state ───────────────────────────────────────────────
const mainTab = ref('transactions')
const bankAccounts = ref([])
const error = ref('')
const successMsg = ref('')

// ─── Transactions state ─────────────────────────────────────────
const transactions = ref([])
const txLoading = ref(false)
const txFilterAccountId = ref('')
const txStartDate = ref('')
const txEndDate = ref('')
const txTotalPages = ref(0)
const txCurrentPage = ref(0)
const txPageSize = 20

// Create transaction modal
const showTxModal = ref(false)
const txForm = reactive({
  bankAccountId: '',
  transactionType: 'DEPOSIT',
  amount: 0,
  transactionDate: '',
  description: '',
  reference: ''
})

// Transfer modal
const showTransferModal = ref(false)
const transferForm = reactive({
  fromAccountId: '',
  toAccountId: '',
  amount: 0,
  transactionDate: '',
  description: ''
})

// Void modal
const showVoidModal = ref(false)
const voidingTransaction = ref(null)
const voidReason = ref('')

// Cash flow
const showCashFlow = ref(false)
const cashFlowAccountId = ref('')
const cashFlowStartDate = ref('')
const cashFlowEndDate = ref('')
const cashFlowData = ref(null)
const cashFlowLoading = ref(false)

// ─── Reconciliation state ───────────────────────────────────────
const reconciliations = ref([])
const reconLoading = ref(false)
const reconFilterAccountId = ref('')
const reconTotalPages = ref(0)
const reconCurrentPage = ref(0)
const reconPageSize = 20

// Create reconciliation modal
const showReconModal = ref(false)
const reconForm = reactive({
  bankAccountId: '',
  statementDate: '',
  statementEndingBalance: 0
})

// Cancel reconciliation modal
const showCancelReconModal = ref(false)
const cancellingRecon = ref(null)
const cancelReconReason = ref('')

// Transaction detail modal
const showTxDetailModal = ref(false)
const txDetailData = ref(null)
const txDetailLoading = ref(false)

// Reconciliation detail modal
const showReconDetailModal = ref(false)
const reconDetailData = ref(null)
const reconDetailLoading = ref(false)

// Unreconciled transactions
const showUnreconciledPanel = ref(false)
const unreconciledTxs = ref([])
const unreconciledLoading = ref(false)
const unreconciledAccountId = ref('')
const unreconciledEndDate = ref('')

// Reconcile selected transactions
const selectedTxIds = ref([])
const reconcilingSelected = ref(false)

// ─── Helpers ────────────────────���───────────────────────────────
function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(value) || 0)
}

function getStatusClass(status) {
  switch (status) {
    case 'CLEARED': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'VOID': return 'badge-danger'
    default: return 'badge-info'
  }
}

function getReconStatusClass(status) {
  switch (status) {
    case 'DRAFT': return 'badge-info'
    case 'IN_PROGRESS': return 'badge-warning'
    case 'COMPLETED': return 'badge-success'
    case 'CANCELLED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function accountName(id) {
  const acc = bankAccounts.value.find(a => a.id === id || a.id === Number(id))
  return acc ? acc.accountName : id
}

// ─── Fetch bank accounts (shared) ──────────────────────────────
async function fetchBankAccounts() {
  try {
    const res = await bankAccountsApi.getAllList()
    const data = unwrapData(res)
    bankAccounts.value = Array.isArray(data) ? data : data.content || []
  } catch (e) {
    if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  }
}

// ─── Transactions CRUD ──────────────────────────────────────────
async function fetchTransactions(page = 0) {
  txLoading.value = true
  error.value = ''
  try {
    let res
    if (txFilterAccountId.value && txStartDate.value && txEndDate.value) {
      res = await bankTransactionsApi.getByAccountDateRange(
        txFilterAccountId.value, txStartDate.value, txEndDate.value
      )
    } else if (txFilterAccountId.value) {
      res = await bankTransactionsApi.getByAccount(txFilterAccountId.value, { page, size: txPageSize, sort: 'transactionDate,desc' })
    } else {
      res = await bankTransactionsApi.getAll({ page, size: txPageSize, sort: 'transactionDate,desc' })
    }
    const data = unwrapData(res)
    if (Array.isArray(data)) {
      transactions.value = data
      txTotalPages.value = 1
      txCurrentPage.value = 0
    } else {
      transactions.value = data.content || []
      txTotalPages.value = data.page?.totalPages || data.totalPages || 1
      txCurrentPage.value = data.page?.number ?? data.number ?? 0
    }
  } catch (e) {
    if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    txLoading.value = false
  }
}

function applyTxFilter() {
  fetchTransactions(0)
}

function openTxModal() {
  txForm.bankAccountId = ''
  txForm.transactionType = 'DEPOSIT'
  txForm.amount = 0
  txForm.transactionDate = ''
  txForm.description = ''
  txForm.reference = ''
  showTxModal.value = true
}

async function saveTransaction() {
  if (!txForm.bankAccountId || !txForm.amount || !txForm.transactionDate) return
  error.value = ''
  try {
    await bankTransactionsApi.create({ ...txForm })
    successMsg.value = t('finance.bankTransactions.createSuccess')
    showTxModal.value = false
    fetchTransactions(txCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function openTransferModal() {
  transferForm.fromAccountId = ''
  transferForm.toAccountId = ''
  transferForm.amount = 0
  transferForm.transactionDate = ''
  transferForm.description = ''
  showTransferModal.value = true
}

async function saveTransfer() {
  if (!transferForm.fromAccountId || !transferForm.toAccountId || !transferForm.amount || !transferForm.transactionDate) return
  error.value = ''
  try {
    await bankTransactionsApi.transfer(
      transferForm.fromAccountId,
      transferForm.toAccountId,
      transferForm.amount,
      transferForm.transactionDate,
      transferForm.description
    )
    successMsg.value = t('finance.bankTransactions.transferSuccess')
    showTransferModal.value = false
    fetchTransactions(txCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function clearTransaction(tx) {
  error.value = ''
  try {
    await bankTransactionsApi.clear(tx.id)
    successMsg.value = t('finance.bankTransactions.clearSuccess')
    fetchTransactions(txCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function openVoidModal(tx) {
  voidingTransaction.value = tx
  voidReason.value = ''
  showVoidModal.value = true
}

async function confirmVoid() {
  if (!voidingTransaction.value) return
  error.value = ''
  try {
    await bankTransactionsApi.void(voidingTransaction.value.id, voidReason.value)
    successMsg.value = t('finance.bankTransactions.voidSuccess')
    showVoidModal.value = false
    voidingTransaction.value = null
    fetchTransactions(txCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function fetchCashFlow() {
  if (!cashFlowAccountId.value || !cashFlowStartDate.value || !cashFlowEndDate.value) return
  cashFlowLoading.value = true
  error.value = ''
  try {
    const res = await bankTransactionsApi.getCashFlow(
      cashFlowAccountId.value, cashFlowStartDate.value, cashFlowEndDate.value
    )
    cashFlowData.value = unwrapData(res)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    cashFlowLoading.value = false
  }
}

// ─── Reconciliation CRUD ────────────────────────────────────────
async function fetchReconciliations(page = 0) {
  reconLoading.value = true
  error.value = ''
  try {
    let res
    if (reconFilterAccountId.value) {
      res = await bankReconciliationsApi.getByAccount(reconFilterAccountId.value, { page, size: reconPageSize, sort: 'statementDate,desc' })
    } else {
      res = await bankReconciliationsApi.getAll({ page, size: reconPageSize, sort: 'statementDate,desc' })
    }
    const data = unwrapData(res)
    if (Array.isArray(data)) {
      reconciliations.value = data
      reconTotalPages.value = 1
      reconCurrentPage.value = 0
    } else {
      reconciliations.value = data.content || []
      reconTotalPages.value = data.page?.totalPages || data.totalPages || 1
      reconCurrentPage.value = data.page?.number ?? data.number ?? 0
    }
  } catch (e) {
    if (e.response?.status !== 403) {
      error.value = e.response?.data?.message || t('failedToLoad')
    }
  } finally {
    reconLoading.value = false
  }
}

function applyReconFilter() {
  fetchReconciliations(0)
}

function openReconModal() {
  reconForm.bankAccountId = ''
  reconForm.statementDate = ''
  reconForm.statementEndingBalance = 0
  showReconModal.value = true
}

async function saveReconciliation() {
  if (!reconForm.bankAccountId || !reconForm.statementDate) return
  error.value = ''
  try {
    await bankReconciliationsApi.create({ ...reconForm })
    successMsg.value = t('finance.bankReconciliations.createSuccess')
    showReconModal.value = false
    fetchReconciliations(reconCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function startReconciliation(recon) {
  error.value = ''
  try {
    await bankReconciliationsApi.start(recon.id)
    successMsg.value = t('finance.bankReconciliations.startSuccess')
    fetchReconciliations(reconCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function completeReconciliation(recon) {
  error.value = ''
  try {
    await bankReconciliationsApi.complete(recon.id)
    successMsg.value = t('finance.bankReconciliations.completeSuccess')
    fetchReconciliations(reconCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

function openCancelReconModal(recon) {
  cancellingRecon.value = recon
  cancelReconReason.value = ''
  showCancelReconModal.value = true
}

async function confirmCancelRecon() {
  if (!cancellingRecon.value) return
  error.value = ''
  try {
    await bankReconciliationsApi.cancel(cancellingRecon.value.id, cancelReconReason.value)
    successMsg.value = t('finance.bankReconciliations.cancelSuccess')
    showCancelReconModal.value = false
    cancellingRecon.value = null
    fetchReconciliations(reconCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

// ─── Transaction detail (getById) ──────────────────────────────
async function viewTransactionDetail(tx) {
  showTxDetailModal.value = true
  txDetailLoading.value = true
  txDetailData.value = null
  try {
    const res = await bankTransactionsApi.getById(tx.id)
    txDetailData.value = unwrapData(res)
  } catch (e) {
    error.value = e.response?.data?.message || t('failedToLoad')
    showTxDetailModal.value = false
  } finally {
    txDetailLoading.value = false
  }
}

// ─── Reconciliation detail (getById) ──────────────────────────
async function viewReconDetail(recon) {
  showReconDetailModal.value = true
  reconDetailLoading.value = true
  reconDetailData.value = null
  try {
    const res = await bankReconciliationsApi.getById(recon.id)
    reconDetailData.value = unwrapData(res)
  } catch (e) {
    error.value = e.response?.data?.message || t('failedToLoad')
    showReconDetailModal.value = false
  } finally {
    reconDetailLoading.value = false
  }
}

// ─── Show unreconciled transactions ───────────────────────────
async function fetchUnreconciled() {
  if (!unreconciledAccountId.value || !unreconciledEndDate.value) return
  unreconciledLoading.value = true
  unreconciledTxs.value = []
  selectedTxIds.value = []
  error.value = ''
  try {
    const res = await bankReconciliationsApi.getUnreconciled(unreconciledAccountId.value, unreconciledEndDate.value)
    const data = unwrapData(res)
    unreconciledTxs.value = Array.isArray(data) ? data : data.content || data || []
    showUnreconciledPanel.value = true
  } catch (e) {
    error.value = e.response?.data?.message || t('failedToLoad')
  } finally {
    unreconciledLoading.value = false
  }
}

function toggleTxSelection(txId) {
  const idx = selectedTxIds.value.indexOf(txId)
  if (idx >= 0) selectedTxIds.value.splice(idx, 1)
  else selectedTxIds.value.push(txId)
}

async function reconcileSelectedTransactions() {
  if (selectedTxIds.value.length === 0 || !unreconciledAccountId.value) return
  reconcilingSelected.value = true
  error.value = ''
  try {
    await bankReconciliationsApi.reconcileTransactions({
      bankAccountId: unreconciledAccountId.value,
      transactionIds: selectedTxIds.value
    })
    successMsg.value = t('finance.bankReconciliations.reconcileSelectedSuccess')
    selectedTxIds.value = []
    await fetchUnreconciled()
    fetchReconciliations(reconCurrentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    reconcilingSelected.value = false
  }
}

// ─── Tab switch ─────────────────────────────────────────────────
function switchMainTab(tab) {
  mainTab.value = tab
  error.value = ''
  successMsg.value = ''
  if (tab === 'transactions') {
    fetchTransactions(0)
  } else {
    fetchReconciliations(0)
  }
}

// ─── Init ───────────────────────────────────────────────────────
onMounted(async () => {
  await fetchBankAccounts()
  fetchTransactions(0)
})

const mainTabs = computed(() => [
  { key: 'transactions', label: t('finance.bankTransactions.tabTransactions') },
  { key: 'reconciliation', label: t('finance.bankReconciliations.tabReconciliation') }
])
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.bankTransactions.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.bankTransactions.subtitle') }}</p>
      </div>
      <div class="flex gap-2">
        <template v-if="mainTab === 'transactions'">
          <button @click="openTransferModal()" class="btn-secondary">
            <ArrowsRightLeftIcon class="h-5 w-5 mr-2" />
            {{ $t('finance.bankTransactions.transfer') }}
          </button>
          <button @click="openTxModal()" class="btn-primary">
            <PlusIcon class="h-5 w-5 mr-2" />
            {{ $t('finance.bankTransactions.addTransaction') }}
          </button>
        </template>
        <template v-else>
          <button @click="openReconModal()" class="btn-primary">
            <PlusIcon class="h-5 w-5 mr-2" />
            {{ $t('finance.bankReconciliations.addReconciliation') }}
          </button>
        </template>
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

    <!-- Main Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex space-x-4">
        <button
          v-for="tab in mainTabs"
          :key="tab.key"
          @click="switchMainTab(tab.key)"
          :class="[
            'px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors',
            mainTab === tab.key
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- TRANSACTIONS TAB                                          -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <template v-if="mainTab === 'transactions'">
      <!-- Filters -->
      <div class="card">
        <div class="card-body">
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label class="label">{{ $t('finance.bankTransactions.bankAccount') }}</label>
              <select v-model="txFilterAccountId" class="input">
                <option value="">{{ $t('finance.bankTransactions.allAccounts') }}</option>
                <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                  {{ acc.accountName }}
                </option>
              </select>
            </div>
            <div>
              <label class="label">{{ $t('finance.bankTransactions.startDate') }}</label>
              <input v-model="txStartDate" type="date" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('finance.bankTransactions.endDate') }}</label>
              <input v-model="txEndDate" type="date" class="input" />
            </div>
            <div class="flex items-end">
              <button @click="applyTxFilter" class="btn-primary w-full">
                <MagnifyingGlassIcon class="h-5 w-5 mr-2" />
                {{ $t('finance.bankTransactions.filter') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Transactions Table -->
      <div class="card">
        <div v-if="txLoading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>

        <div v-else-if="transactions.length === 0" class="text-center py-12">
          <BanknotesIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
          <p class="text-gray-500">{{ $t('finance.bankTransactions.noTransactions') }}</p>
        </div>

        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('finance.bankTransactions.date') }}</th>
                <th>{{ $t('finance.bankTransactions.description') }}</th>
                <th>{{ $t('finance.bankTransactions.type') }}</th>
                <th>{{ $t('finance.bankTransactions.reference') }}</th>
                <th class="text-right">{{ $t('finance.bankTransactions.amount') }}</th>
                <th>{{ $t('status') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="tx in transactions" :key="tx.id">
                <td class="text-sm text-gray-700">{{ tx.transactionDate }}</td>
                <td>
                  <p class="font-medium text-sm">{{ tx.description || '-' }}</p>
                  <p class="text-xs text-gray-400">{{ accountName(tx.bankAccountId) }}</p>
                </td>
                <td>
                  <span class="badge badge-info text-xs">{{ tx.transactionType }}</span>
                </td>
                <td class="text-sm text-gray-500">{{ tx.reference || '-' }}</td>
                <td class="text-right">
                  <span
                    class="font-semibold text-sm"
                    :class="tx.amount >= 0 ? 'text-green-600' : 'text-red-600'"
                  >
                    {{ formatCurrency(tx.amount) }}
                  </span>
                </td>
                <td>
                  <span :class="['badge text-xs', getStatusClass(tx.status)]">
                    {{ tx.status }}
                  </span>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button
                      @click="viewTransactionDetail(tx)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.bankTransactions.viewDetail')"
                    >
                      <EyeIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="tx.status === 'PENDING'"
                      @click="clearTransaction(tx)"
                      class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.bankTransactions.clear')"
                    >
                      <CheckIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="tx.status !== 'VOID'"
                      @click="openVoidModal(tx)"
                      class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.bankTransactions.void')"
                    >
                      <NoSymbolIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="txTotalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
          <p class="text-sm text-gray-500">{{ $t('page') }} {{ txCurrentPage + 1 }} / {{ txTotalPages }}</p>
          <div class="flex gap-2">
            <button
              @click="fetchTransactions(txCurrentPage - 1)"
              :disabled="txCurrentPage === 0"
              class="btn-secondary text-sm"
            >{{ $t('previous') }}</button>
            <button
              @click="fetchTransactions(txCurrentPage + 1)"
              :disabled="txCurrentPage >= txTotalPages - 1"
              class="btn-secondary text-sm"
            >{{ $t('next') }}</button>
          </div>
        </div>
      </div>

      <!-- Cash Flow Section -->
      <div class="card">
        <div class="card-body">
          <div class="flex items-center mb-4">
            <ChartBarIcon class="h-5 w-5 text-gray-500 mr-2" />
            <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.bankTransactions.cashFlow') }}</h3>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label class="label">{{ $t('finance.bankTransactions.bankAccount') }}</label>
              <select v-model="cashFlowAccountId" class="input">
                <option value="">{{ $t('finance.bankTransactions.selectAccount') }}</option>
                <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                  {{ acc.accountName }}
                </option>
              </select>
            </div>
            <div>
              <label class="label">{{ $t('finance.bankTransactions.startDate') }}</label>
              <input v-model="cashFlowStartDate" type="date" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('finance.bankTransactions.endDate') }}</label>
              <input v-model="cashFlowEndDate" type="date" class="input" />
            </div>
            <div class="flex items-end">
              <button @click="fetchCashFlow" class="btn-primary w-full" :disabled="cashFlowLoading">
                <ChartBarIcon class="h-5 w-5 mr-2" />
                {{ $t('finance.bankTransactions.loadCashFlow') }}
              </button>
            </div>
          </div>

          <div v-if="cashFlowLoading" class="flex items-center justify-center h-24 mt-4">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>

          <div v-else-if="cashFlowData" class="grid grid-cols-3 gap-4 mt-6">
            <div class="bg-green-50 rounded-lg p-4 text-center">
              <p class="text-sm text-green-600 font-medium">{{ $t('finance.bankTransactions.inflow') }}</p>
              <p class="text-2xl font-bold text-green-700 mt-1">{{ formatCurrency(cashFlowData.inflow || cashFlowData.totalInflow || 0) }}</p>
            </div>
            <div class="bg-red-50 rounded-lg p-4 text-center">
              <p class="text-sm text-red-600 font-medium">{{ $t('finance.bankTransactions.outflow') }}</p>
              <p class="text-2xl font-bold text-red-700 mt-1">{{ formatCurrency(cashFlowData.outflow || cashFlowData.totalOutflow || 0) }}</p>
            </div>
            <div class="bg-blue-50 rounded-lg p-4 text-center">
              <p class="text-sm text-blue-600 font-medium">{{ $t('finance.bankTransactions.netFlow') }}</p>
              <p class="text-2xl font-bold text-blue-700 mt-1">{{ formatCurrency(cashFlowData.net || cashFlowData.netFlow || 0) }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- RECONCILIATION TAB                                        -->
    <!-- ═══════════════════════════════════════════════════════════ -->
    <template v-if="mainTab === 'reconciliation'">
      <!-- Filters -->
      <div class="card">
        <div class="card-body">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="label">{{ $t('finance.bankReconciliations.bankAccount') }}</label>
              <select v-model="reconFilterAccountId" class="input">
                <option value="">{{ $t('finance.bankReconciliations.allAccounts') }}</option>
                <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                  {{ acc.accountName }}
                </option>
              </select>
            </div>
            <div class="flex items-end">
              <button @click="applyReconFilter" class="btn-primary">
                <MagnifyingGlassIcon class="h-5 w-5 mr-2" />
                {{ $t('finance.bankReconciliations.filter') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Reconciliations Table -->
      <div class="card">
        <div v-if="reconLoading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>

        <div v-else-if="reconciliations.length === 0" class="text-center py-12">
          <DocumentCheckIcon class="h-12 w-12 text-gray-300 mx-auto mb-4" />
          <p class="text-gray-500">{{ $t('finance.bankReconciliations.noReconciliations') }}</p>
        </div>

        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('finance.bankReconciliations.account') }}</th>
                <th>{{ $t('finance.bankReconciliations.statementDate') }}</th>
                <th class="text-right">{{ $t('finance.bankReconciliations.statementBalance') }}</th>
                <th>{{ $t('status') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="recon in reconciliations" :key="recon.id">
                <td class="text-sm font-medium">{{ accountName(recon.bankAccountId) }}</td>
                <td class="text-sm text-gray-700">{{ recon.statementDate }}</td>
                <td class="text-right">
                  <span class="font-semibold text-sm">{{ formatCurrency(recon.statementEndingBalance) }}</span>
                </td>
                <td>
                  <span :class="['badge text-xs', getReconStatusClass(recon.status)]">
                    {{ recon.status }}
                  </span>
                </td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-1">
                    <button
                      @click="viewReconDetail(recon)"
                      class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.bankReconciliations.viewDetail')"
                    >
                      <EyeIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="recon.status === 'DRAFT'"
                      @click="startReconciliation(recon)"
                      class="p-2 text-gray-400 hover:text-blue-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.bankReconciliations.start')"
                    >
                      <PlayIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="recon.status === 'IN_PROGRESS'"
                      @click="completeReconciliation(recon)"
                      class="p-2 text-gray-400 hover:text-green-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.bankReconciliations.complete')"
                    >
                      <CheckIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="recon.status !== 'COMPLETED' && recon.status !== 'CANCELLED'"
                      @click="openCancelReconModal(recon)"
                      class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100"
                      :title="$t('finance.bankReconciliations.cancel')"
                    >
                      <StopIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="reconTotalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
          <p class="text-sm text-gray-500">{{ $t('page') }} {{ reconCurrentPage + 1 }} / {{ reconTotalPages }}</p>
          <div class="flex gap-2">
            <button
              @click="fetchReconciliations(reconCurrentPage - 1)"
              :disabled="reconCurrentPage === 0"
              class="btn-secondary text-sm"
            >{{ $t('previous') }}</button>
            <button
              @click="fetchReconciliations(reconCurrentPage + 1)"
              :disabled="reconCurrentPage >= reconTotalPages - 1"
              class="btn-secondary text-sm"
            >{{ $t('next') }}</button>
          </div>
        </div>
      </div>

      <!-- Unreconciled Transactions Tool -->
      <div class="card">
        <div class="card-body">
          <div class="flex items-center mb-4">
            <ListBulletIcon class="h-5 w-5 text-gray-500 mr-2" />
            <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.bankReconciliations.unreconciledTitle') }}</h3>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="label">{{ $t('finance.bankReconciliations.bankAccount') }}</label>
              <select v-model="unreconciledAccountId" class="input">
                <option value="">{{ $t('finance.bankReconciliations.selectAccount') }}</option>
                <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                  {{ acc.accountName }}
                </option>
              </select>
            </div>
            <div>
              <label class="label">{{ $t('finance.bankReconciliations.endDate') }}</label>
              <input v-model="unreconciledEndDate" type="date" class="input" />
            </div>
            <div class="flex items-end">
              <button @click="fetchUnreconciled" class="btn-primary w-full" :disabled="unreconciledLoading || !unreconciledAccountId || !unreconciledEndDate">
                <MagnifyingGlassIcon class="h-5 w-5 mr-2" />
                {{ $t('finance.bankReconciliations.showUnreconciled') }}
              </button>
            </div>
          </div>

          <!-- Unreconciled list -->
          <div v-if="unreconciledLoading" class="flex items-center justify-center h-24 mt-4">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>

          <div v-else-if="showUnreconciledPanel && unreconciledTxs.length === 0" class="mt-4 text-center py-6 bg-green-50 rounded-lg">
            <p class="text-green-600 text-sm font-medium">{{ $t('finance.bankReconciliations.allReconciled') }}</p>
          </div>

          <div v-else-if="showUnreconciledPanel && unreconciledTxs.length > 0" class="mt-4">
            <div class="flex items-center justify-between mb-3">
              <p class="text-sm text-gray-500">{{ unreconciledTxs.length }} {{ $t('finance.bankReconciliations.unreconciledCount') }}</p>
              <button
                v-if="selectedTxIds.length > 0"
                @click="reconcileSelectedTransactions"
                :disabled="reconcilingSelected"
                class="btn-primary text-sm"
              >
                <CheckBadgeIcon class="h-4 w-4 mr-1" />
                {{ $t('finance.bankReconciliations.reconcileSelected') }} ({{ selectedTxIds.length }})
              </button>
            </div>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th class="w-10">
                      <input type="checkbox" class="h-4 w-4 rounded border-gray-300"
                        :checked="selectedTxIds.length === unreconciledTxs.length && unreconciledTxs.length > 0"
                        @change="selectedTxIds = $event.target.checked ? unreconciledTxs.map(t => t.id) : []" />
                    </th>
                    <th>{{ $t('finance.bankTransactions.date') }}</th>
                    <th>{{ $t('finance.bankTransactions.description') }}</th>
                    <th>{{ $t('finance.bankTransactions.type') }}</th>
                    <th class="text-right">{{ $t('finance.bankTransactions.amount') }}</th>
                    <th>{{ $t('status') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="utx in unreconciledTxs" :key="utx.id" class="hover:bg-gray-50">
                    <td>
                      <input type="checkbox" class="h-4 w-4 rounded border-gray-300"
                        :checked="selectedTxIds.includes(utx.id)"
                        @change="toggleTxSelection(utx.id)" />
                    </td>
                    <td class="text-sm">{{ utx.transactionDate }}</td>
                    <td class="text-sm">{{ utx.description || '-' }}</td>
                    <td><span class="badge badge-info text-xs">{{ utx.transactionType }}</span></td>
                    <td class="text-right">
                      <span class="font-semibold text-sm" :class="utx.amount >= 0 ? 'text-green-600' : 'text-red-600'">
                        {{ formatCurrency(utx.amount) }}
                      </span>
                    </td>
                    <td><span :class="['badge text-xs', getStatusClass(utx.status)]">{{ utx.status }}</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- MODALS                                                    -->
    <!-- ═══════════════════════════════════════════════════════════ -->

    <!-- Create Transaction Modal -->
    <Teleport to="body">
      <div v-if="showTxModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showTxModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ $t('finance.bankTransactions.newTransaction') }}
              </h3>
              <button @click="showTxModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('finance.bankTransactions.bankAccount') }} <span class="text-red-500">*</span></label>
                <select v-model="txForm.bankAccountId" class="input">
                  <option value="">{{ $t('finance.bankTransactions.selectAccount') }}</option>
                  <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                    {{ acc.accountName }}
                  </option>
                </select>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.bankTransactions.type') }} <span class="text-red-500">*</span></label>
                  <select v-model="txForm.transactionType" class="input">
                    <option value="DEPOSIT">{{ $t('finance.bankTransactions.typeDeposit') }}</option>
                    <option value="WITHDRAWAL">{{ $t('finance.bankTransactions.typeWithdrawal') }}</option>
                    <option value="TRANSFER">{{ $t('finance.bankTransactions.typeTransfer') }}</option>
                  </select>
                </div>
                <div>
                  <label class="label">{{ $t('finance.bankTransactions.amount') }} <span class="text-red-500">*</span></label>
                  <input v-model.number="txForm.amount" type="number" step="0.01" class="input" />
                </div>
              </div>

              <div>
                <label class="label">{{ $t('finance.bankTransactions.transactionDate') }} <span class="text-red-500">*</span></label>
                <input v-model="txForm.transactionDate" type="date" class="input" />
              </div>

              <div>
                <label class="label">{{ $t('finance.bankTransactions.description') }}</label>
                <textarea v-model="txForm.description" rows="2" class="input"></textarea>
              </div>

              <div>
                <label class="label">{{ $t('finance.bankTransactions.reference') }}</label>
                <input v-model="txForm.reference" type="text" class="input" />
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showTxModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveTransaction" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Transfer Modal -->
    <Teleport to="body">
      <div v-if="showTransferModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showTransferModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ $t('finance.bankTransactions.newTransfer') }}
              </h3>
              <button @click="showTransferModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('finance.bankTransactions.fromAccount') }} <span class="text-red-500">*</span></label>
                <select v-model="transferForm.fromAccountId" class="input">
                  <option value="">{{ $t('finance.bankTransactions.selectAccount') }}</option>
                  <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                    {{ acc.accountName }}
                  </option>
                </select>
              </div>

              <div>
                <label class="label">{{ $t('finance.bankTransactions.toAccount') }} <span class="text-red-500">*</span></label>
                <select v-model="transferForm.toAccountId" class="input">
                  <option value="">{{ $t('finance.bankTransactions.selectAccount') }}</option>
                  <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                    {{ acc.accountName }}
                  </option>
                </select>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('finance.bankTransactions.amount') }} <span class="text-red-500">*</span></label>
                  <input v-model.number="transferForm.amount" type="number" step="0.01" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('finance.bankTransactions.transactionDate') }} <span class="text-red-500">*</span></label>
                  <input v-model="transferForm.transactionDate" type="date" class="input" />
                </div>
              </div>

              <div>
                <label class="label">{{ $t('finance.bankTransactions.description') }}</label>
                <textarea v-model="transferForm.description" rows="2" class="input"></textarea>
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showTransferModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveTransfer" class="btn-primary">{{ $t('finance.bankTransactions.transfer') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Void Transaction Modal -->
    <Teleport to="body">
      <div v-if="showVoidModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showVoidModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.bankTransactions.voidTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-4">
              {{ $t('finance.bankTransactions.voidConfirm') }}
            </p>
            <div class="mb-4">
              <label class="label">{{ $t('finance.bankTransactions.voidReason') }}</label>
              <textarea v-model="voidReason" rows="2" class="input" :placeholder="$t('finance.bankTransactions.voidReasonPlaceholder')"></textarea>
            </div>
            <div class="flex justify-end space-x-3">
              <button @click="showVoidModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="confirmVoid" class="btn-danger">{{ $t('finance.bankTransactions.void') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Create Reconciliation Modal -->
    <Teleport to="body">
      <div v-if="showReconModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showReconModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">
                {{ $t('finance.bankReconciliations.newReconciliation') }}
              </h3>
              <button @click="showReconModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">{{ $t('finance.bankReconciliations.bankAccount') }} <span class="text-red-500">*</span></label>
                <select v-model="reconForm.bankAccountId" class="input">
                  <option value="">{{ $t('finance.bankReconciliations.selectAccount') }}</option>
                  <option v-for="acc in bankAccounts" :key="acc.id" :value="acc.id">
                    {{ acc.accountName }}
                  </option>
                </select>
              </div>

              <div>
                <label class="label">{{ $t('finance.bankReconciliations.statementDate') }} <span class="text-red-500">*</span></label>
                <input v-model="reconForm.statementDate" type="date" class="input" />
              </div>

              <div>
                <label class="label">{{ $t('finance.bankReconciliations.statementEndingBalance') }}</label>
                <input v-model.number="reconForm.statementEndingBalance" type="number" step="0.01" class="input" />
              </div>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showReconModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveReconciliation" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Cancel Reconciliation Modal -->
    <Teleport to="body">
      <div v-if="showCancelReconModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCancelReconModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-2">{{ $t('finance.bankReconciliations.cancelTitle') }}</h3>
            <p class="text-sm text-gray-500 mb-4">
              {{ $t('finance.bankReconciliations.cancelConfirm') }}
            </p>
            <div class="mb-4">
              <label class="label">{{ $t('finance.bankReconciliations.cancelReason') }}</label>
              <textarea v-model="cancelReconReason" rows="2" class="input" :placeholder="$t('finance.bankReconciliations.cancelReasonPlaceholder')"></textarea>
            </div>
            <div class="flex justify-end space-x-3">
              <button @click="showCancelReconModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="confirmCancelRecon" class="btn-danger">{{ $t('finance.bankReconciliations.cancelAction') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Transaction Detail Modal -->
    <Teleport to="body">
      <div v-if="showTxDetailModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showTxDetailModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.bankTransactions.transactionDetail') }}</h3>
              <button @click="showTxDetailModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>
            <div v-if="txDetailLoading" class="flex items-center justify-center h-32">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
            <div v-else-if="txDetailData" class="space-y-4">
              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span class="text-gray-500">{{ $t('finance.bankTransactions.bankAccount') }}:</span>
                  <p class="font-medium">{{ accountName(txDetailData.bankAccountId) }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.bankTransactions.date') }}:</span>
                  <p class="font-medium">{{ txDetailData.transactionDate }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.bankTransactions.type') }}:</span>
                  <p><span class="badge badge-info text-xs">{{ txDetailData.transactionType }}</span></p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.bankTransactions.amount') }}:</span>
                  <p class="font-semibold" :class="txDetailData.amount >= 0 ? 'text-green-600' : 'text-red-600'">{{ formatCurrency(txDetailData.amount) }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('status') }}:</span>
                  <p><span :class="['badge text-xs', getStatusClass(txDetailData.status)]">{{ txDetailData.status }}</span></p>
                </div>
                <div v-if="txDetailData.reference">
                  <span class="text-gray-500">{{ $t('finance.bankTransactions.reference') }}:</span>
                  <p class="font-medium">{{ txDetailData.reference }}</p>
                </div>
              </div>
              <div v-if="txDetailData.description">
                <span class="text-sm text-gray-500">{{ $t('finance.bankTransactions.description') }}:</span>
                <p class="text-sm mt-1">{{ txDetailData.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Reconciliation Detail Modal -->
    <Teleport to="body">
      <div v-if="showReconDetailModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showReconDetailModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-medium text-gray-900">{{ $t('finance.bankReconciliations.reconDetail') }}</h3>
              <button @click="showReconDetailModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>
            <div v-if="reconDetailLoading" class="flex items-center justify-center h-32">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
            <div v-else-if="reconDetailData" class="space-y-4">
              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span class="text-gray-500">{{ $t('finance.bankReconciliations.bankAccount') }}:</span>
                  <p class="font-medium">{{ accountName(reconDetailData.bankAccountId) }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.bankReconciliations.statementDate') }}:</span>
                  <p class="font-medium">{{ reconDetailData.statementDate }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('finance.bankReconciliations.statementBalance') }}:</span>
                  <p class="font-semibold">{{ formatCurrency(reconDetailData.statementEndingBalance) }}</p>
                </div>
                <div>
                  <span class="text-gray-500">{{ $t('status') }}:</span>
                  <p><span :class="['badge text-xs', getReconStatusClass(reconDetailData.status)]">{{ reconDetailData.status }}</span></p>
                </div>
                <div v-if="reconDetailData.reconciledBalance !== undefined">
                  <span class="text-gray-500">{{ $t('finance.bankReconciliations.reconciledBalance') }}:</span>
                  <p class="font-semibold">{{ formatCurrency(reconDetailData.reconciledBalance) }}</p>
                </div>
                <div v-if="reconDetailData.difference !== undefined">
                  <span class="text-gray-500">{{ $t('finance.bankReconciliations.difference') }}:</span>
                  <p class="font-semibold" :class="reconDetailData.difference === 0 ? 'text-green-600' : 'text-red-600'">{{ formatCurrency(reconDetailData.difference) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
