<script setup>
import { ref, reactive, onMounted } from 'vue'
import { reportsApi } from '@/services/api'
import { ArrowDownTrayIcon, CalendarIcon } from '@heroicons/vue/24/outline'

const loading = ref(true)
const report = ref(null)

const filters = reactive({
  startDate: new Date(new Date().setDate(1)).toISOString().split('T')[0], // First of month
  endDate: new Date().toISOString().split('T')[0],
  groupBy: 'day'
})

async function fetchReport() {
  loading.value = true
  try {
    const response = await reportsApi.getSalesSummary({
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

async function exportReport() {
  try {
    const response = await reportsApi.exportSalesSummary({
      startDate: filters.startDate,
      endDate: filters.endDate,
      exportFormat: 'EXCEL'
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `sales-report-${filters.startDate}-${filters.endDate}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
  } catch (error) {
    console.error('Export failed:', error)
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0)
}

function formatNumber(value) {
  return new Intl.NumberFormat('en-US').format(value || 0)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Sales Report</h1>
        <p class="mt-1 text-sm text-gray-500">Analyze your sales performance</p>
      </div>
      <button @click="exportReport" class="btn-secondary">
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
        <div>
          <label class="label">Group By</label>
          <select v-model="filters.groupBy" class="input">
            <option value="day">Day</option>
            <option value="week">Week</option>
            <option value="month">Month</option>
          </select>
        </div>
        <button @click="fetchReport" class="btn-primary">
          Apply Filters
        </button>
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
            <p class="text-sm text-gray-500">Total Sales</p>
            <p class="text-2xl font-bold text-primary-600">{{ formatCurrency(report.totalSales) }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Transactions</p>
            <p class="text-2xl font-bold">{{ formatNumber(report.transactionCount) }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Average Sale</p>
            <p class="text-2xl font-bold">{{ formatCurrency(report.averageSale) }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Items Sold</p>
            <p class="text-2xl font-bold">{{ formatNumber(report.itemsSold) }}</p>
          </div>
        </div>
      </div>

      <!-- Top Products -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Top Products</h3>
          </div>
          <div class="card-body">
            <div v-if="report.topProducts?.length" class="space-y-3">
              <div
                v-for="(product, index) in report.topProducts.slice(0, 10)"
                :key="index"
                class="flex items-center justify-between"
              >
                <div class="flex items-center">
                  <span class="w-6 h-6 rounded-full bg-primary-100 text-primary-700 text-xs flex items-center justify-center mr-3">
                    {{ index + 1 }}
                  </span>
                  <span class="font-medium">{{ product.name }}</span>
                </div>
                <span class="text-gray-500">{{ formatCurrency(product.totalSales) }}</span>
              </div>
            </div>
            <p v-else class="text-gray-500 text-center py-4">No data available</p>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Sales by Payment Method</h3>
          </div>
          <div class="card-body">
            <div v-if="report.salesByPaymentMethod?.length" class="space-y-3">
              <div
                v-for="item in report.salesByPaymentMethod"
                :key="item.method"
                class="flex items-center justify-between"
              >
                <span class="font-medium">{{ item.method }}</span>
                <div class="text-right">
                  <span class="font-medium">{{ formatCurrency(item.total) }}</span>
                  <span class="text-sm text-gray-500 ml-2">({{ item.count }} txns)</span>
                </div>
              </div>
            </div>
            <p v-else class="text-gray-500 text-center py-4">No data available</p>
          </div>
        </div>
      </div>

      <!-- Daily/Weekly/Monthly Breakdown -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Sales Breakdown</h3>
        </div>
        <div v-if="report.breakdown?.length" class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Period</th>
                <th class="text-right">Transactions</th>
                <th class="text-right">Items</th>
                <th class="text-right">Total</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="row in report.breakdown" :key="row.period">
                <td class="font-medium">{{ row.period }}</td>
                <td class="text-right">{{ row.transactions }}</td>
                <td class="text-right">{{ row.items }}</td>
                <td class="text-right font-medium">{{ formatCurrency(row.total) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="card-body text-center text-gray-500">
          No breakdown data available
        </div>
      </div>
    </template>
  </div>
</template>
