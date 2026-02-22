<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { reportsApi } from '@/services/api'
import { ArrowDownTrayIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const loading = ref(true)
const report = ref(null)

const filters = reactive({
  startDate: new Date(new Date().setDate(1)).toISOString().split('T')[0], // First of month
  endDate: new Date().toISOString().split('T')[0],
})

async function fetchReport() {
  loading.value = true
  try {
    const response = await reportsApi.getSalesSummary({
      startDate: filters.startDate,
      endDate: filters.endDate
    })
    report.value = response.data.data || response.data
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
  return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value || 0)
}

function formatNumber(value) {
  return new Intl.NumberFormat('uz-UZ').format(value || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('uz-UZ', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function getPaymentLabel(method) {
  const key = `enums.paymentType.${method}`
  const translated = t(key)
  return translated !== key ? translated : method
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('reports.sales.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('reports.sales.subtitle') }}</p>
      </div>
      <button @click="exportReport" class="btn-secondary">
        <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
        {{ $t('export') }}
      </button>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-wrap gap-4 items-end">
        <div>
          <label class="label">{{ $t('reports.sales.startDate') }}</label>
          <input v-model="filters.startDate" type="date" class="input" />
        </div>
        <div>
          <label class="label">{{ $t('reports.sales.endDate') }}</label>
          <input v-model="filters.endDate" type="date" class="input" />
        </div>
        <button @click="fetchReport" class="btn-primary">
          {{ $t('apply') }}
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
            <p class="text-sm text-gray-500">{{ $t('reports.sales.netSales') }}</p>
            <p class="text-2xl font-bold text-primary-600">{{ formatCurrency(report.summary?.netSales) }}</p>
            <p v-if="report.summary?.grossSales > report.summary?.netSales" class="text-xs text-gray-400 mt-1">
              {{ $t('reports.sales.grossSales') }}: {{ formatCurrency(report.summary?.grossSales) }}
            </p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('reports.sales.transactionsCount') }}</p>
            <p class="text-2xl font-bold">{{ formatNumber(report.summary?.transactionCount) }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('reports.sales.averageCheck') }}</p>
            <p class="text-2xl font-bold">{{ formatCurrency(report.summary?.averageTransactionValue) }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('reports.sales.soldProducts') }}</p>
            <p class="text-2xl font-bold">{{ formatNumber(report.summary?.itemsSold) }}</p>
          </div>
        </div>
      </div>

      <!-- Extra summary row -->
      <div v-if="report.summary?.discounts > 0 || report.summary?.returns > 0 || report.summary?.grossProfit" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div v-if="report.summary?.discounts > 0" class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('reports.sales.discounts') }}</p>
            <p class="text-2xl font-bold text-orange-600">-{{ formatCurrency(report.summary.discounts) }}</p>
          </div>
        </div>
        <div v-if="report.summary?.returns > 0" class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('reports.sales.returns') }}</p>
            <p class="text-2xl font-bold text-red-600">-{{ formatCurrency(report.summary.returns) }}</p>
          </div>
        </div>
        <div v-if="report.summary?.grossProfit != null" class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('reports.sales.grossProfit') }}</p>
            <p class="text-2xl font-bold text-green-600">{{ formatCurrency(report.summary.grossProfit) }}</p>
          </div>
        </div>
        <div v-if="report.summary?.grossMarginPercent != null" class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('reports.sales.profitMargin') }}</p>
            <p class="text-2xl font-bold text-green-600">{{ Number(report.summary.grossMarginPercent).toFixed(1) }}%</p>
          </div>
        </div>
      </div>

      <!-- Top Products & Payment Methods -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">{{ $t('reports.sales.topProducts') }}</h3>
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
                  <div>
                    <span class="font-medium">{{ product.productName }}</span>
                    <span v-if="product.sku" class="text-xs text-gray-400 ml-2">{{ product.sku }}</span>
                  </div>
                </div>
                <div class="text-right">
                  <span class="font-medium">{{ formatCurrency(product.netSales) }}</span>
                  <span class="text-xs text-gray-400 ml-1">({{ product.quantitySold }} {{ $t('reports.sales.pcs') }})</span>
                </div>
              </div>
            </div>
            <p v-else class="text-gray-500 text-center py-4">{{ $t('noData') }}</p>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">{{ $t('reports.sales.byPaymentType') }}</h3>
          </div>
          <div class="card-body">
            <div v-if="report.byPaymentMethod?.length" class="space-y-3">
              <div
                v-for="item in report.byPaymentMethod"
                :key="item.paymentMethod"
                class="flex items-center justify-between"
              >
                <div class="flex items-center">
                  <span class="font-medium">{{ getPaymentLabel(item.paymentMethod) }}</span>
                  <span v-if="item.percentOfTotal" class="text-xs text-gray-400 ml-2">({{ Number(item.percentOfTotal).toFixed(1) }}%)</span>
                </div>
                <div class="text-right">
                  <span class="font-medium">{{ formatCurrency(item.amount) }}</span>
                  <span class="text-sm text-gray-500 ml-2">({{ item.transactionCount }} {{ $t('items') }})</span>
                </div>
              </div>
            </div>
            <p v-else class="text-gray-500 text-center py-4">{{ $t('noData') }}</p>
          </div>
        </div>
      </div>

      <!-- Daily Breakdown -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('reports.sales.dailyBreakdown') }}</h3>
        </div>
        <div v-if="report.dailyBreakdown?.length" class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('date') }}</th>
                <th>{{ $t('reports.sales.weekday') }}</th>
                <th class="text-right">{{ $t('reports.sales.transactionsCount') }}</th>
                <th class="text-right">{{ $t('reports.sales.netSales') }}</th>
                <th class="text-right">{{ $t('reports.sales.averageCheck') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="row in report.dailyBreakdown" :key="row.date">
                <td class="font-medium">{{ formatDate(row.date) }}</td>
                <td class="text-sm text-gray-500">{{ row.dayOfWeek }}</td>
                <td class="text-right">{{ row.transactionCount }}</td>
                <td class="text-right font-medium">{{ formatCurrency(row.netSales) }}</td>
                <td class="text-right text-gray-500">{{ formatCurrency(row.averageTransaction) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="card-body text-center text-gray-500">
          {{ $t('reports.sales.noBreakdownData') }}
        </div>
      </div>

      <!-- Category Breakdown -->
      <div v-if="report.byCategory?.length" class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('reports.sales.byCategory') }}</h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('reports.sales.category') }}</th>
                <th class="text-right">{{ $t('reports.sales.soldCount') }}</th>
                <th class="text-right">{{ $t('reports.sales.netSales') }}</th>
                <th class="text-right">{{ $t('reports.sales.share') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="cat in report.byCategory" :key="cat.categoryId">
                <td class="font-medium">{{ cat.categoryName }}</td>
                <td class="text-right">{{ cat.itemsSold }} {{ $t('items') }}</td>
                <td class="text-right font-medium">{{ formatCurrency(cat.netSales) }}</td>
                <td class="text-right text-gray-500">{{ Number(cat.percentOfTotal).toFixed(1) }}%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
