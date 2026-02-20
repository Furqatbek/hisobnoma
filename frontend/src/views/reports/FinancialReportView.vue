<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { reportsApi } from '@/services/api'
import { ArrowDownTrayIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon, CheckCircleIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

const loading = ref(true)
const trialBalance = ref(null)
const arAging = ref(null)
const apAging = ref(null)

const filters = reactive({
  startDate: new Date(new Date().setDate(1)).toISOString().split('T')[0],
  endDate: new Date().toISOString().split('T')[0]
})

async function fetchReport() {
  loading.value = true
  try {
    const [tbRes, arRes, apRes] = await Promise.all([
      reportsApi.getTrialBalance({ startDate: filters.startDate, endDate: filters.endDate }),
      reportsApi.getARAgingReport({ startDate: filters.startDate, endDate: filters.endDate }).catch(() => null),
      reportsApi.getAPAgingReport({ startDate: filters.startDate, endDate: filters.endDate }).catch(() => null)
    ])
    trialBalance.value = tbRes.data.data || tbRes.data
    arAging.value = arRes?.data?.data || arRes?.data || null
    apAging.value = apRes?.data?.data || apRes?.data || null
  } catch (error) {
    console.error('Failed to fetch report:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)

// Derive P&L figures from trial balance account types
const summary = computed(() => {
  if (!trialBalance.value?.accounts) return null
  const accounts = trialBalance.value.accounts.filter(a => !a.isHeader)

  let revenue = 0
  let expenses = 0
  let assets = 0
  let liabilities = 0

  for (const acc of accounts) {
    const type = acc.accountType?.toUpperCase()
    const credit = Number(acc.creditBalance) || 0
    const debit = Number(acc.debitBalance) || 0

    if (type === 'REVENUE' || type === 'INCOME') {
      revenue += credit - debit
    } else if (type === 'EXPENSE' || type === 'COST_OF_GOODS_SOLD' || type === 'COGS') {
      expenses += debit - credit
    } else if (type === 'ASSET') {
      assets += debit - credit
    } else if (type === 'LIABILITY' || type === 'EQUITY') {
      liabilities += credit - debit
    }
  }

  return {
    revenue,
    expenses,
    netProfit: revenue - expenses,
    assets,
    liabilities
  }
})

async function exportReport() {
  try {
    const response = await reportsApi.exportTrialBalance({
      startDate: filters.startDate,
      endDate: filters.endDate,
      exportFormat: 'EXCEL'
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `financial-report-${filters.startDate}-${filters.endDate}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
  } catch (error) {
    console.error('Export failed:', error)
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value || 0)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Moliyaviy hisobot</h1>
        <p class="mt-1 text-sm text-gray-500">Daromad, xarajatlar va balans tahlili</p>
      </div>
      <button @click="exportReport" class="btn-secondary">
        <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
        Eksport
      </button>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-wrap gap-4 items-end">
        <div>
          <label class="label">Boshlanish sanasi</label>
          <input v-model="filters.startDate" type="date" class="input" />
        </div>
        <div>
          <label class="label">Tugash sanasi</label>
          <input v-model="filters.endDate" type="date" class="input" />
        </div>
        <button @click="fetchReport" class="btn-primary">Qo'llash</button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="trialBalance">
      <!-- P&L Summary Cards derived from trial balance -->
      <div v-if="summary" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Daromad</p>
                <p class="text-2xl font-bold text-green-600">{{ formatCurrency(summary.revenue) }}</p>
              </div>
              <ArrowTrendingUpIcon class="h-8 w-8 text-green-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Xarajatlar</p>
                <p class="text-2xl font-bold text-red-600">{{ formatCurrency(summary.expenses) }}</p>
              </div>
              <ArrowTrendingDownIcon class="h-8 w-8 text-red-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Sof foyda</p>
                <p class="text-2xl font-bold" :class="summary.netProfit >= 0 ? 'text-primary-600' : 'text-red-600'">
                  {{ formatCurrency(summary.netProfit) }}
                </p>
              </div>
            </div>
            <p v-if="summary.revenue > 0" class="text-sm text-gray-500 mt-2">
              Margina: {{ ((summary.netProfit / summary.revenue) * 100).toFixed(1) }}%
            </p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Balans holati</p>
                <p class="text-lg font-bold" :class="trialBalance.totals?.isBalanced ? 'text-green-600' : 'text-red-600'">
                  {{ trialBalance.totals?.isBalanced ? 'Balans to\'g\'ri' : 'Balans noto\'g\'ri' }}
                </p>
              </div>
              <CheckCircleIcon v-if="trialBalance.totals?.isBalanced" class="h-8 w-8 text-green-400" />
              <ExclamationTriangleIcon v-else class="h-8 w-8 text-red-400" />
            </div>
            <p v-if="trialBalance.totals?.difference > 0" class="text-sm text-red-500 mt-2">
              Farq: {{ formatCurrency(trialBalance.totals.difference) }}
            </p>
          </div>
        </div>
      </div>

      <!-- Receivables & Payables from aging reports -->
      <div v-if="arAging || apAging" class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div v-if="arAging" class="card">
          <div class="card-header bg-green-50">
            <h3 class="text-lg font-medium text-green-800">Debitorlik qarzlari</h3>
          </div>
          <div class="card-body">
            <div class="text-center py-4">
              <p class="text-3xl font-bold text-green-600">{{ formatCurrency(arAging.summary?.totalOutstanding) }}</p>
              <p class="text-sm text-gray-500 mt-2">{{ arAging.summary?.totalAccounts || 0 }} ta mijozdan</p>
            </div>
            <div v-if="arAging.summary" class="grid grid-cols-2 sm:grid-cols-5 gap-2 mt-4">
              <div class="text-center p-2 bg-gray-50 rounded">
                <p class="text-xs text-gray-500">Joriy</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.current) }}</p>
              </div>
              <div class="text-center p-2 bg-yellow-50 rounded">
                <p class="text-xs text-gray-500">1-30 kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.days1to30) }}</p>
              </div>
              <div class="text-center p-2 bg-orange-50 rounded">
                <p class="text-xs text-gray-500">31-60 kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.days31to60) }}</p>
              </div>
              <div class="text-center p-2 bg-red-50 rounded">
                <p class="text-xs text-gray-500">61-90 kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.days61to90) }}</p>
              </div>
              <div class="text-center p-2 bg-red-100 rounded">
                <p class="text-xs text-gray-500">90+ kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(arAging.summary.over90Days) }}</p>
              </div>
            </div>
          </div>
        </div>

        <div v-if="apAging" class="card">
          <div class="card-header bg-red-50">
            <h3 class="text-lg font-medium text-red-800">Kreditorlik qarzlari</h3>
          </div>
          <div class="card-body">
            <div class="text-center py-4">
              <p class="text-3xl font-bold text-red-600">{{ formatCurrency(apAging.summary?.totalOutstanding) }}</p>
              <p class="text-sm text-gray-500 mt-2">{{ apAging.summary?.totalAccounts || 0 }} ta yetkazuvchiga</p>
            </div>
            <div v-if="apAging.summary" class="grid grid-cols-2 sm:grid-cols-5 gap-2 mt-4">
              <div class="text-center p-2 bg-gray-50 rounded">
                <p class="text-xs text-gray-500">Joriy</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.current) }}</p>
              </div>
              <div class="text-center p-2 bg-yellow-50 rounded">
                <p class="text-xs text-gray-500">1-30 kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.days1to30) }}</p>
              </div>
              <div class="text-center p-2 bg-orange-50 rounded">
                <p class="text-xs text-gray-500">31-60 kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.days31to60) }}</p>
              </div>
              <div class="text-center p-2 bg-red-50 rounded">
                <p class="text-xs text-gray-500">61-90 kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.days61to90) }}</p>
              </div>
              <div class="text-center p-2 bg-red-100 rounded">
                <p class="text-xs text-gray-500">90+ kun</p>
                <p class="font-medium text-sm">{{ formatCurrency(apAging.summary.over90Days) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Trial Balance Table -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">Sinov balansi</h3>
          <div v-if="trialBalance.metadata" class="text-sm text-gray-500">
            <span v-if="trialBalance.metadata.fiscalYear">{{ trialBalance.metadata.fiscalYear }}</span>
            <span v-if="trialBalance.metadata.period"> / {{ trialBalance.metadata.period }}</span>
          </div>
        </div>
        <div v-if="trialBalance.accounts?.length" class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Kod</th>
                <th>Hisob nomi</th>
                <th>Turi</th>
                <th class="text-right">Debet</th>
                <th class="text-right">Kredit</th>
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
                <td colspan="3">Jami</td>
                <td class="text-right">{{ formatCurrency(trialBalance.totals.totalDebits) }}</td>
                <td class="text-right">{{ formatCurrency(trialBalance.totals.totalCredits) }}</td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div v-else class="card-body text-center text-gray-500">
          Hisob ma'lumotlari yo'q
        </div>
      </div>

      <!-- AR Aging Details -->
      <div v-if="arAging?.details?.length" class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Debitorlik tafsiloti</h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Mijoz</th>
                <th class="text-right">Jami</th>
                <th class="text-right">Joriy</th>
                <th class="text-right">1-30</th>
                <th class="text-right">31-60</th>
                <th class="text-right">61-90</th>
                <th class="text-right">90+</th>
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
          <h3 class="text-lg font-medium">Kreditorlik tafsiloti</h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Yetkazuvchi</th>
                <th class="text-right">Jami</th>
                <th class="text-right">Joriy</th>
                <th class="text-right">1-30</th>
                <th class="text-right">31-60</th>
                <th class="text-right">61-90</th>
                <th class="text-right">90+</th>
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
  </div>
</template>
