<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { expensesApi, suppliersApi, journalEntriesApi } from '@/services/api'
import {
  PlusIcon,
  EyeIcon,
  MagnifyingGlassIcon,
  BanknotesIcon,
  CheckCircleIcon,
  ClockIcon,
  ExclamationTriangleIcon,
  XCircleIcon,
  UserGroupIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const activeTab = ref('invoices') // 'invoices' or 'salary'

// AP Invoices state
const expenses = ref([])
const vendors = ref([])
const loading = ref(true)
const search = ref('')
const statusFilter = ref('all')
const vendorFilter = ref('')

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

// Summary stats
const totalPayable = ref(0)
const overdueBalance = ref(0)

// Salary GL entries state
const salaryEntries = ref([])
const salaryLoading = ref(false)
const salaryPagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})
const salaryTotal = ref(0)

// Detail modal
const selectedEntry = ref(null)
const detailLoading = ref(false)

async function fetchExpenses() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }

    let response
    if (statusFilter.value !== 'all') {
      response = await expensesApi.getByStatus(statusFilter.value, params)
    } else if (vendorFilter.value) {
      response = await expensesApi.getByVendor(vendorFilter.value, params)
    } else {
      response = await expensesApi.getAll(params)
    }

    const data = response.data.data || response.data
    expenses.value = data.content || data || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 1
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || expenses.value.length
  } catch (error) {
    console.error('Xarajatlarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

async function fetchVendors() {
  try {
    const response = await suppliersApi.getAll({ size: 100 })
    const data = response.data.data || response.data
    vendors.value = data.content || data || []
  } catch (error) {
    console.error('Yetkazib beruvchilarni yuklashda xatolik:', error)
  }
}

async function fetchSummary() {
  try {
    const [payableRes, overdueRes] = await Promise.all([
      expensesApi.getTotalPayable(),
      expensesApi.getOverdueBalance()
    ])
    totalPayable.value = Number(payableRes.data.data ?? payableRes.data) || 0
    overdueBalance.value = Number(overdueRes.data.data ?? overdueRes.data) || 0
  } catch (error) {
    console.error('Jami ma\'lumotlarni yuklashda xatolik:', error)
  }
}

async function fetchSalaryEntries() {
  salaryLoading.value = true
  try {
    const response = await journalEntriesApi.getBySource('SALARY_PAYMENT', {
      page: salaryPagination.value.page,
      size: salaryPagination.value.size
    })
    const data = response.data.data || response.data
    salaryEntries.value = data.content || data || []
    salaryPagination.value.totalPages = data.page?.totalPages || data.totalPages || 1
    salaryPagination.value.totalElements = data.page?.totalElements || data.totalElements || salaryEntries.value.length

    // Calculate salary total
    salaryTotal.value = salaryEntries.value.reduce((sum, e) => sum + (Number(e.totalDebit) || 0), 0)
  } catch (error) {
    console.error('Ish haqi yozuvlarini yuklashda xatolik:', error)
  } finally {
    salaryLoading.value = false
  }
}

async function showEntryDetail(entry) {
  detailLoading.value = true
  selectedEntry.value = { ...entry, lines: [] }
  try {
    const response = await journalEntriesApi.getWithLines(entry.id)
    const data = response.data.data || response.data
    selectedEntry.value = data
  } catch (error) {
    console.error('Yozuv tafsilotini yuklashda xatolik:', error)
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  selectedEntry.value = null
}

onMounted(() => {
  fetchExpenses()
  fetchVendors()
  fetchSummary()
  fetchSalaryEntries()
})

function switchTab(tab) {
  activeTab.value = tab
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(value) || 0)
}

function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('uz-UZ')
}

function getStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'PENDING_APPROVAL': 'badge-warning',
    'APPROVED': 'badge-success',
    'PARTIALLY_PAID': 'badge-info',
    'PAID': 'badge-success',
    'ON_HOLD': 'badge-warning',
    'CANCELLED': 'badge-danger',
    'REJECTED': 'badge-danger',
    'POSTED': 'badge-success',
    'REVERSED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getStatusLabel(status) {
  return t(`enums.apInvoiceStatus.${status}`, status)
}

function getEntryTypeLabel(entry) {
  if (entry.referenceType === 'SALARY_ADVANCE') return t('finance.expenses.advance')
  if (entry.referenceType === 'SALARY_PAYMENT') return t('finance.expenses.salaryType')
  return entry.referenceType || '-'
}

function getEntryTypeClass(entry) {
  if (entry.referenceType === 'SALARY_ADVANCE') return 'badge-warning'
  return 'badge-info'
}

function handleFilter() {
  pagination.value.page = 0
  fetchExpenses()
}

function isOverdue(expense) {
  if (!expense.dueDate || expense.status === 'PAID' || expense.status === 'CANCELLED') {
    return false
  }
  return new Date(expense.dueDate) < new Date()
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.expenses.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.expenses.subtitle') }}</p>
      </div>
      <RouterLink to="/finance/expenses/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('finance.expenses.newExpense') }}
      </RouterLink>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="card">
        <div class="card-body">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">{{ $t('finance.expenses.totalPayable') }}</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(totalPayable) }} {{ $t('sum') }}</p>
            </div>
            <div class="p-3 bg-primary-100 rounded-full">
              <BanknotesIcon class="h-6 w-6 text-primary-600" />
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-body">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">{{ $t('finance.expenses.overdueCount') }}</p>
              <p class="text-2xl font-bold text-red-600">{{ formatCurrency(overdueBalance) }} {{ $t('sum') }}</p>
            </div>
            <div class="p-3 bg-red-100 rounded-full">
              <ExclamationTriangleIcon class="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-body">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">{{ $t('finance.expenses.salaryPayments') }}</p>
              <p class="text-2xl font-bold text-blue-600">{{ formatCurrency(salaryTotal) }} {{ $t('sum') }}</p>
            </div>
            <div class="p-3 bg-blue-100 rounded-full">
              <UserGroupIcon class="h-6 w-6 text-blue-600" />
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-body">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">{{ $t('finance.expenses.totalInvoices') }}</p>
              <p class="text-2xl font-bold text-gray-900">{{ pagination.totalElements }}</p>
            </div>
            <div class="p-3 bg-gray-100 rounded-full">
              <ClockIcon class="h-6 w-6 text-gray-600" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex space-x-8">
        <button
          @click="switchTab('invoices')"
          :class="[
            'py-4 px-1 border-b-2 font-medium text-sm',
            activeTab === 'invoices'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <BanknotesIcon class="h-5 w-5 inline mr-2" />
          {{ $t('finance.expenses.invoicesTab') }} ({{ pagination.totalElements }})
        </button>
        <button
          @click="switchTab('salary')"
          :class="[
            'py-4 px-1 border-b-2 font-medium text-sm',
            activeTab === 'salary'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <UserGroupIcon class="h-5 w-5 inline mr-2" />
          {{ $t('finance.expenses.salaryTab') }} ({{ salaryPagination.totalElements }})
        </button>
      </nav>
    </div>

    <!-- AP Invoices Tab -->
    <template v-if="activeTab === 'invoices'">
      <!-- Filters -->
      <div class="card">
        <div class="card-body flex flex-col sm:flex-row gap-4">
          <div class="flex-1 relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="search"
              type="text"
              :placeholder="$t('finance.expenses.searchPlaceholder')"
              class="input pl-10"
              @keyup.enter="handleFilter"
            />
          </div>
          <select v-model="vendorFilter" @change="handleFilter" class="input w-auto">
            <option value="">{{ $t('finance.expenses.allSuppliers') }}</option>
            <option v-for="vendor in vendors" :key="vendor.id" :value="vendor.id">
              {{ vendor.name }}
            </option>
          </select>
          <select v-model="statusFilter" @change="handleFilter" class="input w-auto">
            <option value="all">{{ $t('finance.expenses.allStatuses') }}</option>
            <option value="DRAFT">{{ $t('enums.apInvoiceStatus.DRAFT') }}</option>
            <option value="PENDING_APPROVAL">{{ $t('enums.apInvoiceStatus.PENDING_APPROVAL') }}</option>
            <option value="APPROVED">{{ $t('enums.apInvoiceStatus.APPROVED') }}</option>
            <option value="PARTIALLY_PAID">{{ $t('enums.apInvoiceStatus.PARTIALLY_PAID') }}</option>
            <option value="PAID">{{ $t('enums.apInvoiceStatus.PAID') }}</option>
            <option value="ON_HOLD">{{ $t('enums.apInvoiceStatus.ON_HOLD') }}</option>
          </select>
        </div>
      </div>

      <!-- Table -->
      <div class="card">
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>

        <div v-else-if="expenses.length === 0" class="text-center py-12">
          <BanknotesIcon class="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <p class="text-gray-500 mb-4">{{ $t('finance.expenses.noExpenses') }}</p>
          <RouterLink to="/finance/expenses/new" class="btn-primary">
            <PlusIcon class="h-5 w-5 mr-2" />
            {{ $t('finance.expenses.addFirst') }}
          </RouterLink>
        </div>

        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('finance.expenses.invoiceNumber') }}</th>
                <th>{{ $t('finance.expenses.supplierOrDescription') }}</th>
                <th>{{ $t('date') }}</th>
                <th>{{ $t('finance.expenses.dueDate') }}</th>
                <th class="text-right">{{ $t('amount') }}</th>
                <th class="text-right">{{ $t('balance') }}</th>
                <th>{{ $t('status') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="expense in expenses" :key="expense.id" :class="{ 'bg-red-50': isOverdue(expense) }">
                <td class="font-mono text-sm">
                  {{ expense.invoiceNumber || `#${expense.id}` }}
                  <div v-if="expense.vendorInvoiceNumber" class="text-xs text-gray-500">
                    {{ expense.vendorInvoiceNumber }}
                  </div>
                </td>
                <td>{{ expense.vendorName || '-' }}</td>
                <td class="text-sm text-gray-500">{{ formatDate(expense.invoiceDate) }}</td>
                <td :class="{ 'text-red-600 font-medium': isOverdue(expense) }">
                  {{ formatDate(expense.dueDate) }}
                  <span v-if="isOverdue(expense)" class="text-xs">({{ $t('finance.expenses.overdue') }})</span>
                </td>
                <td class="text-right font-medium">{{ formatCurrency(expense.totalAmount) }} {{ $t('sum') }}</td>
                <td class="text-right">
                  <span :class="Number(expense.balanceDue) > 0 ? 'text-red-600 font-medium' : 'text-green-600'">
                    {{ formatCurrency(expense.balanceDue) }} {{ $t('sum') }}
                  </span>
                </td>
                <td>
                  <span :class="['badge', getStatusClass(expense.status)]">
                    {{ getStatusLabel(expense.status) }}
                  </span>
                </td>
                <td class="text-right">
                  <RouterLink
                    :to="`/finance/expenses/${expense.id}`"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                    :title="$t('details')"
                  >
                    <EyeIcon class="h-5 w-5" />
                  </RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
          <div class="flex items-center justify-between">
            <button
              @click="pagination.page--; fetchExpenses()"
              :disabled="pagination.page === 0"
              class="btn-secondary"
            >
              {{ $t('previous') }}
            </button>
            <span class="text-sm text-gray-500">
              {{ $t('page') }} {{ pagination.page + 1 }} / {{ pagination.totalPages }}
              ({{ $t('total') }}: {{ pagination.totalElements }})
            </span>
            <button
              @click="pagination.page++; fetchExpenses()"
              :disabled="pagination.page >= pagination.totalPages - 1"
              class="btn-secondary"
            >
              {{ $t('next') }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Salary & Advances Tab -->
    <template v-if="activeTab === 'salary'">
      <div class="card">
        <div v-if="salaryLoading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>

        <div v-else-if="salaryEntries.length === 0" class="text-center py-12">
          <UserGroupIcon class="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <p class="text-gray-500">{{ $t('finance.expenses.noSalaryEntries') }}</p>
          <p class="text-sm text-gray-400 mt-2">{{ $t('finance.expenses.noSalaryHint') }}</p>
        </div>

        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('finance.expenses.entryNumber') }}</th>
                <th>{{ $t('description') }}</th>
                <th>{{ $t('finance.expenses.entryType') }}</th>
                <th>{{ $t('date') }}</th>
                <th class="text-right">{{ $t('amount') }}</th>
                <th>{{ $t('status') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="entry in salaryEntries" :key="entry.id">
                <td class="font-mono text-sm">
                  {{ entry.entryNumber }}
                  <div v-if="entry.referenceNumber" class="text-xs text-gray-500">
                    {{ entry.referenceNumber }}
                  </div>
                </td>
                <td class="font-medium">{{ entry.description }}</td>
                <td>
                  <span :class="['badge', getEntryTypeClass(entry)]">
                    {{ getEntryTypeLabel(entry) }}
                  </span>
                </td>
                <td class="text-sm text-gray-500">{{ formatDate(entry.entryDate) }}</td>
                <td class="text-right font-medium">{{ formatCurrency(entry.totalDebit) }} {{ $t('sum') }}</td>
                <td>
                  <span :class="['badge', getStatusClass(entry.status)]">
                    {{ getStatusLabel(entry.status) }}
                  </span>
                </td>
                <td class="text-right">
                  <button
                    @click="showEntryDetail(entry)"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                    :title="$t('details')"
                  >
                    <EyeIcon class="h-5 w-5" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="salaryPagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
          <div class="flex items-center justify-between">
            <button
              @click="salaryPagination.page--; fetchSalaryEntries()"
              :disabled="salaryPagination.page === 0"
              class="btn-secondary"
            >
              {{ $t('previous') }}
            </button>
            <span class="text-sm text-gray-500">
              {{ $t('page') }} {{ salaryPagination.page + 1 }} / {{ salaryPagination.totalPages }}
              ({{ $t('total') }}: {{ salaryPagination.totalElements }})
            </span>
            <button
              @click="salaryPagination.page++; fetchSalaryEntries()"
              :disabled="salaryPagination.page >= salaryPagination.totalPages - 1"
              class="btn-secondary"
            >
              {{ $t('next') }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Entry Detail Modal -->
    <div v-if="selectedEntry" class="fixed inset-0 bg-gray-500 bg-opacity-75 flex items-center justify-center z-50" @click.self="closeDetail">
      <div class="bg-white rounded-lg shadow-xl max-w-3xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div>
            <h3 class="text-lg font-medium text-gray-900">{{ selectedEntry.description }}</h3>
            <p class="text-sm text-gray-500">{{ selectedEntry.entryNumber }} - {{ formatDate(selectedEntry.entryDate) }}</p>
          </div>
          <button @click="closeDetail" class="text-gray-400 hover:text-gray-500">
            <XCircleIcon class="h-6 w-6" />
          </button>
        </div>

        <div class="px-6 py-4 space-y-4">
          <!-- Entry info -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <dt class="text-sm text-gray-500">{{ $t('finance.expenses.entryType') }}</dt>
              <dd class="font-medium">
                <span :class="['badge', getEntryTypeClass(selectedEntry)]">
                  {{ getEntryTypeLabel(selectedEntry) }}
                </span>
              </dd>
            </div>
            <div>
              <dt class="text-sm text-gray-500">{{ $t('status') }}</dt>
              <dd>
                <span :class="['badge', getStatusClass(selectedEntry.status)]">
                  {{ getStatusLabel(selectedEntry.status) }}
                </span>
              </dd>
            </div>
            <div>
              <dt class="text-sm text-gray-500">{{ $t('finance.expenses.debit') }}</dt>
              <dd class="font-medium">{{ formatCurrency(selectedEntry.totalDebit) }} {{ $t('sum') }}</dd>
            </div>
            <div>
              <dt class="text-sm text-gray-500">{{ $t('finance.expenses.credit') }}</dt>
              <dd class="font-medium">{{ formatCurrency(selectedEntry.totalCredit) }} {{ $t('sum') }}</dd>
            </div>
          </div>

          <!-- Journal lines -->
          <div v-if="detailLoading" class="flex items-center justify-center py-8">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
          <div v-else-if="selectedEntry.lines?.length" class="mt-4">
            <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('finance.expenses.accountingEntries') }}</h4>
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('finance.expenses.account') }}</th>
                  <th>{{ $t('description') }}</th>
                  <th class="text-right">{{ $t('finance.expenses.debit') }}</th>
                  <th class="text-right">{{ $t('finance.expenses.credit') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="line in selectedEntry.lines" :key="line.id">
                  <td>
                    <span class="font-mono text-sm">{{ line.accountCode }}</span>
                    <span class="text-sm text-gray-500 ml-2">{{ line.accountName }}</span>
                  </td>
                  <td class="text-sm">{{ line.description }}</td>
                  <td class="text-right">
                    <span v-if="Number(line.debitAmount) > 0" class="font-medium">{{ formatCurrency(line.debitAmount) }}</span>
                  </td>
                  <td class="text-right">
                    <span v-if="Number(line.creditAmount) > 0" class="font-medium">{{ formatCurrency(line.creditAmount) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="px-6 py-4 border-t border-gray-200 flex justify-end">
          <button @click="closeDetail" class="btn-secondary">{{ $t('close') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
