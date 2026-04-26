<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { reportsApi } from '@/services/api'
import { ArrowDownTrayIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon, CheckCircleIcon, ExclamationTriangleIcon, ClockIcon, DocumentTextIcon, CalendarDaysIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const loading = ref(true)
const activeTab = ref('financial')
const trialBalance = ref(null)
const incomeStatement = ref(null)
const arAging = ref(null)
const apAging = ref(null)

// Report Management state
const definitions = ref([])
const schedules = ref([])
const executions = ref([])
const loadingDefinitions = ref(false)
const loadingSchedules = ref(false)
const loadingExecutions = ref(false)

const filters = reactive({
  startDate: new Date(new Date().setDate(1)).toISOString().split('T')[0],
  endDate: new Date().toISOString().split('T')[0]
})

async function fetchReport() {
  loading.value = true
  try {
    const [tbRes, isRes, arRes, apRes] = await Promise.all([
      reportsApi.getTrialBalance({ startDate: filters.startDate, endDate: filters.endDate }),
      reportsApi.getIncomeStatement({ startDate: filters.startDate, endDate: filters.endDate }),
      reportsApi.getARAgingReport({ startDate: filters.startDate, endDate: filters.endDate }).catch(() => null),
      reportsApi.getAPAgingReport({ startDate: filters.startDate, endDate: filters.endDate }).catch(() => null)
    ])
    trialBalance.value = tbRes.data.data || tbRes.data
    incomeStatement.value = isRes.data.data || isRes.data
    arAging.value = arRes?.data?.data || arRes?.data || null
    apAging.value = apRes?.data?.data || apRes?.data || null
  } catch (error) {
    console.error('Failed to fetch report:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)

async function exportReport() {
  try {
    const response = await reportsApi.exportTrialBalance({
      startDate: filters.startDate,
      endDate: filters.endDate,
      exportFormat: 'EXCEL'
    })
    downloadBlob(response.data, `financial-report-${filters.startDate}-${filters.endDate}.xlsx`)
  } catch (error) {
    console.error('Export failed:', error)
  }
}

async function exportIncomeStatement() {
  try {
    const response = await reportsApi.exportIncomeStatement({
      startDate: filters.startDate,
      endDate: filters.endDate,
      exportFormat: 'EXCEL'
    })
    downloadBlob(response.data, `income-statement-${filters.startDate}-${filters.endDate}.xlsx`)
  } catch (error) {
    console.error('Export income statement failed:', error)
  }
}

async function exportARAgingReport() {
  try {
    const response = await reportsApi.exportARAgingReport({
      startDate: filters.startDate,
      endDate: filters.endDate,
      exportFormat: 'EXCEL'
    })
    downloadBlob(response.data, `ar-aging-${filters.startDate}-${filters.endDate}.xlsx`)
  } catch (error) {
    console.error('Export AR aging failed:', error)
  }
}

async function exportAPAgingReport() {
  try {
    const response = await reportsApi.exportAPAgingReport({
      startDate: filters.startDate,
      endDate: filters.endDate,
      exportFormat: 'EXCEL'
    })
    downloadBlob(response.data, `ap-aging-${filters.startDate}-${filters.endDate}.xlsx`)
  } catch (error) {
    console.error('Export AP aging failed:', error)
  }
}

function downloadBlob(data, filename) {
  const url = window.URL.createObjectURL(new Blob([data]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

async function fetchDefinitions() {
  loadingDefinitions.value = true
  try {
    const response = await reportsApi.getDefinitions({})
    const data = response.data.data || response.data
    definitions.value = data.content || data || []
  } catch (error) {
    console.error('Failed to fetch definitions:', error)
  } finally {
    loadingDefinitions.value = false
  }
}

async function fetchSchedules() {
  loadingSchedules.value = true
  try {
    const response = await reportsApi.getSchedules({})
    const data = response.data.data || response.data
    schedules.value = data.content || data || []
  } catch (error) {
    console.error('Failed to fetch schedules:', error)
  } finally {
    loadingSchedules.value = false
  }
}

async function fetchExecutions() {
  loadingExecutions.value = true
  try {
    const response = await reportsApi.getExecutions({})
    const data = response.data.data || response.data
    executions.value = data.content || data || []
  } catch (error) {
    console.error('Failed to fetch executions:', error)
  } finally {
    loadingExecutions.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'management' && definitions.value.length === 0 && !loadingDefinitions.value) {
    fetchDefinitions()
    fetchSchedules()
    fetchExecutions()
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value || 0)
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('uz-UZ')
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('reports.financial.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('reports.financial.subtitle') }}</p>
      </div>
      <button v-if="activeTab === 'financial'" @click="exportReport" class="btn-secondary">
        <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
        {{ $t('export') }}
      </button>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="-mb-px flex space-x-8">
        <button
          @click="switchTab('financial')"
          :class="[
            'whitespace-nowrap py-3 px-1 border-b-2 font-medium text-sm',
            activeTab === 'financial'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ $t('reports.financial.tabFinancial') }}
        </button>
        <button
          @click="switchTab('management')"
          :class="[
            'whitespace-nowrap py-3 px-1 border-b-2 font-medium text-sm',
            activeTab === 'management'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ $t('reports.financial.tabManagement') }}
        </button>
      </nav>
    </div>

    <!-- Financial Reports Tab -->
    <template v-if="activeTab === 'financial'">

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-wrap gap-4 items-end">
        <div>
          <label class="label">{{ $t('reports.financial.startDate') }}</label>
          <input v-model="filters.startDate" type="date" class="input" />
        </div>
        <div>
          <label class="label">{{ $t('reports.financial.endDate') }}</label>
          <input v-model="filters.endDate" type="date" class="input" />
        </div>
        <button @click="fetchReport" class="btn-primary">{{ $t('apply') }}</button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="trialBalance">
      <!-- P&L Summary Cards from Income Statement -->
      <div v-if="incomeStatement?.summary" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.financial.revenue') }}</p>
                <p class="text-2xl font-bold text-green-600">{{ formatCurrency(incomeStatement.summary.totalRevenue) }}</p>
              </div>
              <ArrowTrendingUpIcon class="h-8 w-8 text-green-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.financial.expensesTotal') }}</p>
                <p class="text-2xl font-bold text-red-600">{{ formatCurrency(incomeStatement.summary.totalExpenses) }}</p>
              </div>
              <ArrowTrendingDownIcon class="h-8 w-8 text-red-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.financial.netProfit') }}</p>
                <p class="text-2xl font-bold" :class="incomeStatement.summary.netIncome >= 0 ? 'text-primary-600' : 'text-red-600'">
                  {{ formatCurrency(incomeStatement.summary.netIncome) }}
                </p>
              </div>
            </div>
            <p v-if="incomeStatement.summary.totalRevenue > 0" class="text-sm text-gray-500 mt-2">
              {{ $t('reports.financial.margin') }}: {{ ((incomeStatement.summary.netIncome / incomeStatement.summary.totalRevenue) * 100).toFixed(1) }}%
            </p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.financial.balanceStatus') }}</p>
                <p class="text-lg font-bold" :class="trialBalance.totals?.isBalanced ? 'text-green-600' : 'text-red-600'">
                  {{ trialBalance.totals?.isBalanced ? $t('reports.financial.balanceCorrect') : $t('reports.financial.balanceIncorrect') }}
                </p>
              </div>
              <CheckCircleIcon v-if="trialBalance.totals?.isBalanced" class="h-8 w-8 text-green-400" />
              <ExclamationTriangleIcon v-else class="h-8 w-8 text-red-400" />
            </div>
            <p v-if="trialBalance.totals?.difference > 0" class="text-sm text-red-500 mt-2">
              {{ $t('reports.financial.difference') }}: {{ formatCurrency(trialBalance.totals.difference) }}
            </p>
          </div>
        </div>
      </div>

      <!-- Income Statement Breakdown -->
      <div v-if="incomeStatement" class="space-y-4">
        <div class="flex justify-end">
          <button @click="exportIncomeStatement" class="btn-secondary">
            <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
            {{ $t('reports.financial.exportIncomeStatement') }}
          </button>
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Revenue breakdown -->
        <div class="card">
          <div class="card-header bg-green-50">
            <h3 class="text-lg font-medium text-green-800">{{ $t('reports.financial.revenues') }}</h3>
          </div>
          <div v-if="incomeStatement.revenueItems?.length" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('reports.financial.accountCode') }}</th>
                  <th>{{ $t('reports.financial.accountName') }}</th>
                  <th class="text-right">{{ $t('amount') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="item in incomeStatement.revenueItems" :key="item.accountId">
                  <td class="font-mono text-sm">{{ item.accountCode }}</td>
                  <td class="font-medium">{{ item.accountName }}</td>
                  <td class="text-right font-medium text-green-600">{{ formatCurrency(item.amount) }}</td>
                </tr>
              </tbody>
              <tfoot class="border-t-2 border-gray-300">
                <tr class="font-bold">
                  <td colspan="2">{{ $t('reports.financial.totalRevenue') }}</td>
                  <td class="text-right text-green-600">{{ formatCurrency(incomeStatement.summary.totalRevenue) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div v-else class="card-body text-center text-gray-500">{{ $t('reports.financial.noRevenue') }}</div>
        </div>

        <!-- Expense breakdown -->
        <div class="card">
          <div class="card-header bg-red-50">
            <h3 class="text-lg font-medium text-red-800">{{ $t('reports.financial.expensesList') }}</h3>
          </div>
          <div v-if="incomeStatement.expenseItems?.length" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('reports.financial.accountCode') }}</th>
                  <th>{{ $t('reports.financial.accountName') }}</th>
                  <th class="text-right">{{ $t('amount') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="item in incomeStatement.expenseItems" :key="item.accountId">
                  <td class="font-mono text-sm">{{ item.accountCode }}</td>
                  <td class="font-medium">{{ item.accountName }}</td>
                  <td class="text-right font-medium text-red-600">{{ formatCurrency(item.amount) }}</td>
                </tr>
              </tbody>
              <tfoot class="border-t-2 border-gray-300">
                <tr class="font-bold">
                  <td colspan="2">{{ $t('reports.financial.expensesTotal') }}</td>
                  <td class="text-right text-red-600">{{ formatCurrency(incomeStatement.summary.totalExpenses) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div v-else class="card-body text-center text-gray-500">{{ $t('reports.financial.noExpenses') }}</div>
        </div>
        </div>
      </div>

      <!-- Receivables & Payables from aging reports -->
      <div v-if="arAging || apAging" class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div v-if="arAging" class="card">
          <div class="card-header bg-green-50 flex items-center justify-between">
            <h3 class="text-lg font-medium text-green-800">{{ $t('reports.financial.receivables') }}</h3>
            <button @click="exportARAgingReport" class="btn-secondary text-sm">
              <ArrowDownTrayIcon class="h-4 w-4 mr-1" />
              {{ $t('export') }}
            </button>
          </div>
          <div class="card-body">
            <div class="text-center py-4">
              <p class="text-3xl font-bold text-green-600">{{ formatCurrency(arAging.summary?.totalOutstanding) }}</p>
              <p class="text-sm text-gray-500 mt-2">{{ $t('reports.financial.fromCustomers', { count: arAging.summary?.totalAccounts || 0 }) }}</p>
            </div>
            <div v-if="arAging.summary" class="grid grid-cols-2 sm:grid-cols-5 gap-2 mt-4">
              <div class="text-center p-2 bg-gray-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.current') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.current) }}</p>
              </div>
              <div class="text-center p-2 bg-yellow-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days1to30') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.days1to30) }}</p>
              </div>
              <div class="text-center p-2 bg-orange-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days31to60') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.days31to60) }}</p>
              </div>
              <div class="text-center p-2 bg-red-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days61to90') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.days61to90) }}</p>
              </div>
              <div class="text-center p-2 bg-red-100 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days90plus') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.over90Days) }}</p>
              </div>
            </div>
          </div>
        </div>

        <div v-if="apAging" class="card">
          <div class="card-header bg-red-50 flex items-center justify-between">
            <h3 class="text-lg font-medium text-red-800">{{ $t('reports.financial.payables') }}</h3>
            <button @click="exportAPAgingReport" class="btn-secondary text-sm">
              <ArrowDownTrayIcon class="h-4 w-4 mr-1" />
              {{ $t('export') }}
            </button>
          </div>
          <div class="card-body">
            <div class="text-center py-4">
              <p class="text-3xl font-bold text-red-600">{{ formatCurrency(apAging.summary?.totalOutstanding) }}</p>
              <p class="text-sm text-gray-500 mt-2">{{ $t('reports.financial.toSuppliers', { count: apAging.summary?.totalAccounts || 0 }) }}</p>
            </div>
            <div v-if="apAging.summary" class="grid grid-cols-2 sm:grid-cols-5 gap-2 mt-4">
              <div class="text-center p-2 bg-gray-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.current') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.current) }}</p>
              </div>
              <div class="text-center p-2 bg-yellow-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days1to30') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.days1to30) }}</p>
              </div>
              <div class="text-center p-2 bg-orange-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days31to60') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.days31to60) }}</p>
              </div>
              <div class="text-center p-2 bg-red-50 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days61to90') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.days61to90) }}</p>
              </div>
              <div class="text-center p-2 bg-red-100 rounded">
                <p class="text-xs text-gray-500">{{ $t('reports.financial.days90plus') }}</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.over90Days) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Trial Balance Table -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">{{ $t('reports.financial.trialBalance') }}</h3>
          <div v-if="trialBalance.metadata" class="text-sm text-gray-500">
            <span v-if="trialBalance.metadata.fiscalYear">{{ trialBalance.metadata.fiscalYear }}</span>
            <span v-if="trialBalance.metadata.period"> / {{ trialBalance.metadata.period }}</span>
          </div>
        </div>
        <div v-if="trialBalance.accounts?.length" class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('reports.financial.accountCode') }}</th>
                <th>{{ $t('reports.financial.accountName') }}</th>
                <th>{{ $t('reports.financial.type') }}</th>
                <th class="text-right">{{ $t('reports.financial.debit') }}</th>
                <th class="text-right">{{ $t('reports.financial.credit') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr
                v-for="acc in trialBalance.accounts"
                :key="acc.accountId"
                :class="acc.isHeader ? 'bg-gray-50 font-semibold' : ''"
              >
                <td class="font-mono text-sm" :style="{ paddingLeft: (acc.level || 0) * 16 + 12 + 'px' }">
                  {{ acc.accountCode }}
                </td>
                <td :class="acc.isHeader ? 'font-semibold' : 'font-medium'">
                  {{ acc.accountName }}
                </td>
                <td class="text-sm text-gray-500">{{ acc.accountSubType || acc.accountType }}</td>
                <td class="text-right">
                  <span v-if="acc.debitBalance > 0">{{ formatCurrency(acc.debitBalance) }}</span>
                </td>
                <td class="text-right">
                  <span v-if="acc.creditBalance > 0">{{ formatCurrency(acc.creditBalance) }}</span>
                </td>
              </tr>
            </tbody>
            <tfoot v-if="trialBalance.totals" class="border-t-2 border-gray-300">
              <tr class="font-bold">
                <td colspan="3">{{ $t('total') }}</td>
                <td class="text-right">{{ formatCurrency(trialBalance.totals.totalDebits) }}</td>
                <td class="text-right">{{ formatCurrency(trialBalance.totals.totalCredits) }}</td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div v-else class="card-body text-center text-gray-500">
          {{ $t('reports.financial.noAccountData') }}
        </div>
      </div>

      <!-- AR Aging Details -->
      <div v-if="arAging?.details?.length" class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('reports.financial.receivablesDetail') }}</h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('customer') }}</th>
                <th class="text-right">{{ $t('total') }}</th>
                <th class="text-right">{{ $t('reports.financial.current') }}</th>
                <th class="text-right">{{ $t('reports.financial.days1to30') }}</th>
                <th class="text-right">{{ $t('reports.financial.days31to60') }}</th>
                <th class="text-right">{{ $t('reports.financial.days61to90') }}</th>
                <th class="text-right">{{ $t('reports.financial.days90plus') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="d in arAging.details" :key="d.entityId">
                <td>
                  <span class="font-medium">{{ d.entityName }}</span>
                  <span v-if="d.entityCode" class="text-xs text-gray-400 ml-2">{{ d.entityCode }}</span>
                </td>
                <td class="text-right font-medium">{{ formatCurrency(d.totalOutstanding) }}</td>
                <td class="text-right">{{ formatCurrency(d.current) }}</td>
                <td class="text-right">{{ formatCurrency(d.days1to30) }}</td>
                <td class="text-right">{{ formatCurrency(d.days31to60) }}</td>
                <td class="text-right">{{ formatCurrency(d.days61to90) }}</td>
                <td class="text-right">{{ formatCurrency(d.over90Days) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- AP Aging Details -->
      <div v-if="apAging?.details?.length" class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('reports.financial.payablesDetail') }}</h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('supplier') }}</th>
                <th class="text-right">{{ $t('total') }}</th>
                <th class="text-right">{{ $t('reports.financial.current') }}</th>
                <th class="text-right">{{ $t('reports.financial.days1to30') }}</th>
                <th class="text-right">{{ $t('reports.financial.days31to60') }}</th>
                <th class="text-right">{{ $t('reports.financial.days61to90') }}</th>
                <th class="text-right">{{ $t('reports.financial.days90plus') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="d in apAging.details" :key="d.entityId">
                <td>
                  <span class="font-medium">{{ d.entityName }}</span>
                  <span v-if="d.entityCode" class="text-xs text-gray-400 ml-2">{{ d.entityCode }}</span>
                </td>
                <td class="text-right font-medium">{{ formatCurrency(d.totalOutstanding) }}</td>
                <td class="text-right">{{ formatCurrency(d.current) }}</td>
                <td class="text-right">{{ formatCurrency(d.days1to30) }}</td>
                <td class="text-right">{{ formatCurrency(d.days31to60) }}</td>
                <td class="text-right">{{ formatCurrency(d.days61to90) }}</td>
                <td class="text-right">{{ formatCurrency(d.over90Days) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
    </template>

    <!-- Report Management Tab -->
    <template v-if="activeTab === 'management'">
      <div class="space-y-6">
        <!-- Report Definitions -->
        <div class="card">
          <div class="card-header">
            <div>
              <h3 class="text-lg font-medium">{{ $t('reports.financial.definitions') }}</h3>
              <p class="text-sm text-gray-500 mt-1">{{ $t('reports.financial.definitionsSubtitle') }}</p>
            </div>
          </div>
          <div v-if="loadingDefinitions" class="card-body text-center text-gray-500">
            {{ $t('reports.financial.loadingDefinitions') }}
          </div>
          <div v-else-if="definitions.length" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('reports.financial.reportName') }}</th>
                  <th>{{ $t('reports.financial.reportType') }}</th>
                  <th>{{ $t('reports.financial.description') }}</th>
                  <th>{{ $t('status') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="def in definitions" :key="def.id">
                  <td class="font-medium">{{ def.name }}</td>
                  <td class="text-sm text-gray-500">{{ def.reportType || def.type }}</td>
                  <td class="text-sm text-gray-500">{{ def.description || '-' }}</td>
                  <td>
                    <span :class="['badge', def.active || def.status === 'ACTIVE' ? 'badge-success' : 'badge-secondary']">
                      {{ def.active || def.status === 'ACTIVE' ? $t('active') : $t('inactive') }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="card-body text-center text-gray-500">
            {{ $t('reports.financial.noDefinitions') }}
          </div>
        </div>

        <!-- Report Schedules -->
        <div class="card">
          <div class="card-header">
            <div>
              <h3 class="text-lg font-medium">{{ $t('reports.financial.schedules') }}</h3>
              <p class="text-sm text-gray-500 mt-1">{{ $t('reports.financial.schedulesSubtitle') }}</p>
            </div>
          </div>
          <div v-if="loadingSchedules" class="card-body text-center text-gray-500">
            {{ $t('reports.financial.loadingSchedules') }}
          </div>
          <div v-else-if="schedules.length" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('reports.financial.reportName') }}</th>
                  <th>{{ $t('reports.financial.frequency') }}</th>
                  <th>{{ $t('reports.financial.nextRun') }}</th>
                  <th>{{ $t('reports.financial.lastRun') }}</th>
                  <th>{{ $t('status') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="schedule in schedules" :key="schedule.id">
                  <td class="font-medium">{{ schedule.name || schedule.reportName }}</td>
                  <td class="text-sm text-gray-500">{{ schedule.frequency || schedule.cronExpression }}</td>
                  <td class="text-sm text-gray-500">{{ formatDateTime(schedule.nextRunAt || schedule.nextRun) }}</td>
                  <td class="text-sm text-gray-500">{{ formatDateTime(schedule.lastRunAt || schedule.lastRun) }}</td>
                  <td>
                    <span :class="['badge', schedule.active || schedule.status === 'ACTIVE' ? 'badge-success' : 'badge-secondary']">
                      {{ schedule.active || schedule.status === 'ACTIVE' ? $t('active') : $t('inactive') }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="card-body text-center text-gray-500">
            {{ $t('reports.financial.noSchedules') }}
          </div>
        </div>

        <!-- Report Executions -->
        <div class="card">
          <div class="card-header">
            <div>
              <h3 class="text-lg font-medium">{{ $t('reports.financial.executions') }}</h3>
              <p class="text-sm text-gray-500 mt-1">{{ $t('reports.financial.executionsSubtitle') }}</p>
            </div>
          </div>
          <div v-if="loadingExecutions" class="card-body text-center text-gray-500">
            {{ $t('reports.financial.loadingExecutions') }}
          </div>
          <div v-else-if="executions.length" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('reports.financial.reportName') }}</th>
                  <th>{{ $t('reports.financial.executedAt') }}</th>
                  <th>{{ $t('reports.financial.executedBy') }}</th>
                  <th>{{ $t('reports.financial.duration') }}</th>
                  <th>{{ $t('status') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="exec in executions" :key="exec.id">
                  <td class="font-medium">{{ exec.reportName || exec.name }}</td>
                  <td class="text-sm text-gray-500">{{ formatDateTime(exec.executedAt || exec.startedAt) }}</td>
                  <td class="text-sm text-gray-500">{{ exec.executedBy || exec.userName || '-' }}</td>
                  <td class="text-sm text-gray-500">{{ exec.duration || exec.durationMs ? (exec.durationMs || exec.duration) + ' ms' : '-' }}</td>
                  <td>
                    <span :class="['badge', exec.status === 'COMPLETED' || exec.status === 'SUCCESS' ? 'badge-success' : exec.status === 'FAILED' ? 'badge-danger' : 'badge-warning']">
                      {{ exec.status }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="card-body text-center text-gray-500">
            {{ $t('reports.financial.noExecutions') }}
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
