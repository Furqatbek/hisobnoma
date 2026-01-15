<script setup>
import { ref, reactive, onMounted } from 'vue'
import { reportsApi } from '@/services/api'
import { ArrowDownTrayIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon } from '@heroicons/vue/24/outline'

const loading = ref(true)
const report = ref(null)

const filters = reactive({
  startDate: new Date(new Date().setDate(1)).toISOString().split('T')[0],
  endDate: new Date().toISOString().split('T')[0]
})

async function fetchReport() {
  loading.value = true
  try {
    const response = await reportsApi.getTrialBalance({
      startDate: filters.startDate,
      endDate: filters.endDate
    })
    report.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch report:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Financial Report</h1>
        <p class="mt-1 text-sm text-gray-500">Revenue, expenses, and profit analysis</p>
      </div>
      <button class="btn-secondary">
        <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
        Export
      </button>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-wrap gap-4 items-end">
        <div>
          <label class="label">Start Date</label>
          <input v-model="filters.startDate" type="date" class="input" />
        </div>
        <div>
          <label class="label">End Date</label>
          <input v-model="filters.endDate" type="date" class="input" />
        </div>
        <button @click="fetchReport" class="btn-primary">Apply</button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="report">
      <!-- Summary Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Revenue</p>
                <p class="text-2xl font-bold text-green-600">{{ formatCurrency(report.revenue) }}</p>
              </div>
              <ArrowTrendingUpIcon class="h-8 w-8 text-green-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Cost of Goods</p>
                <p class="text-2xl font-bold text-red-600">{{ formatCurrency(report.costOfGoods) }}</p>
              </div>
              <ArrowTrendingDownIcon class="h-8 w-8 text-red-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Gross Profit</p>
                <p class="text-2xl font-bold text-blue-600">{{ formatCurrency(report.grossProfit) }}</p>
              </div>
            </div>
            <p class="text-sm text-gray-500 mt-2">
              Margin: {{ ((report.grossProfit / report.revenue) * 100 || 0).toFixed(1) }}%
            </p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">Net Profit</p>
                <p class="text-2xl font-bold text-primary-600">{{ formatCurrency(report.netProfit) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Receivables & Payables -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card">
          <div class="card-header bg-green-50">
            <h3 class="text-lg font-medium text-green-800">Accounts Receivable</h3>
          </div>
          <div class="card-body">
            <div class="text-center py-4">
              <p class="text-3xl font-bold text-green-600">{{ formatCurrency(report.receivables) }}</p>
              <p class="text-sm text-gray-500 mt-2">Outstanding from {{ report.receivablesCount || 0 }} customers</p>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header bg-red-50">
            <h3 class="text-lg font-medium text-red-800">Accounts Payable</h3>
          </div>
          <div class="card-body">
            <div class="text-center py-4">
              <p class="text-3xl font-bold text-red-600">{{ formatCurrency(report.payables) }}</p>
              <p class="text-sm text-gray-500 mt-2">Owed to {{ report.payablesCount || 0 }} suppliers</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Cash Flow -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Cash Flow Summary</h3>
        </div>
        <div class="card-body">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div class="p-4 bg-green-50 rounded-lg">
              <p class="text-sm text-green-700">Cash In</p>
              <p class="text-xl font-bold text-green-700">{{ formatCurrency(report.cashIn) }}</p>
            </div>
            <div class="p-4 bg-red-50 rounded-lg">
              <p class="text-sm text-red-700">Cash Out</p>
              <p class="text-xl font-bold text-red-700">{{ formatCurrency(report.cashOut) }}</p>
            </div>
            <div class="p-4 bg-blue-50 rounded-lg">
              <p class="text-sm text-blue-700">Net Cash Flow</p>
              <p class="text-xl font-bold text-blue-700">{{ formatCurrency((report.cashIn || 0) - (report.cashOut || 0)) }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
