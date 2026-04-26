<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { reportsApi } from '@/services/api'
import { ArrowDownTrayIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const loading = ref(true)
const report = ref(null)
const filter = ref('all') // all, low, out
const activeTab = ref('stockOnHand')

// Valuation state
const valuation = ref(null)
const loadingValuation = ref(false)

async function fetchReport() {
  loading.value = true
  try {
    const response = await reportsApi.getStockOnHand({})
    report.value = response.data.data || response.data
  } catch (error) {
    console.error('Failed to fetch report:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)

const filteredItems = computed(() => {
  if (!report.value?.items) return []
  if (filter.value === 'low') return report.value.items.filter(p => p.stockStatus === 'LOW_STOCK')
  if (filter.value === 'out') return report.value.items.filter(p => p.stockStatus === 'OUT_OF_STOCK')
  return report.value.items
})

async function exportReport() {
  try {
    const response = await reportsApi.exportStockOnHand({ exportFormat: 'EXCEL' })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'inventory-report.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Export failed:', error)
  }
}

async function fetchValuation() {
  loadingValuation.value = true
  try {
    const response = await reportsApi.getInventoryValuation({})
    const data = response.data.data || response.data
    valuation.value = data
  } catch (error) {
    console.error('Failed to fetch valuation:', error)
  } finally {
    loadingValuation.value = false
  }
}

async function exportValuation() {
  try {
    const response = await reportsApi.exportInventoryValuation({ exportFormat: 'EXCEL' })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'inventory-valuation.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Export valuation failed:', error)
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'valuation' && !valuation.value && !loadingValuation.value) {
    fetchValuation()
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value || 0)
}

function formatQty(value) {
  if (value == null) return '0'
  return new Intl.NumberFormat('uz-UZ').format(value)
}

function getStatusClass(status) {
  if (status === 'OUT_OF_STOCK') return 'badge-danger'
  if (status === 'LOW_STOCK') return 'badge-warning'
  return 'badge-success'
}

function getStatusLabel(status) {
  if (status === 'OUT_OF_STOCK') return t('reports.inventory.outOfStock')
  if (status === 'LOW_STOCK') return t('reports.inventory.lowStock')
  return t('reports.inventory.inStock')
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('reports.inventory.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('reports.inventory.subtitle') }}</p>
      </div>
      <button v-if="activeTab === 'stockOnHand'" @click="exportReport" class="btn-secondary">
        <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
        {{ $t('export') }}
      </button>
      <button v-if="activeTab === 'valuation'" @click="exportValuation" class="btn-secondary">
        <ArrowDownTrayIcon class="h-5 w-5 mr-2" />
        {{ $t('reports.inventory.exportValuation') }}
      </button>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="-mb-px flex space-x-8">
        <button
          @click="switchTab('stockOnHand')"
          :class="[
            'whitespace-nowrap py-3 px-1 border-b-2 font-medium text-sm',
            activeTab === 'stockOnHand'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ $t('reports.inventory.tabStockOnHand') }}
        </button>
        <button
          @click="switchTab('valuation')"
          :class="[
            'whitespace-nowrap py-3 px-1 border-b-2 font-medium text-sm',
            activeTab === 'valuation'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ $t('reports.inventory.tabValuation') }}
        </button>
      </nav>
    </div>

    <!-- Stock on Hand Tab -->
    <template v-if="activeTab === 'stockOnHand'">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <template v-else-if="report">
        <!-- Summary -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <button
            @click="filter = 'all'"
            :class="['card cursor-pointer transition-all text-left', filter === 'all' ? 'ring-2 ring-primary-500' : '']"
          >
            <div class="card-body">
              <p class="text-sm text-gray-500">{{ $t('reports.inventory.totalProducts') }}</p>
              <p class="text-2xl font-bold">{{ report.summary?.totalSkus || 0 }}</p>
              <p v-if="report.summary?.totalQuantity" class="text-xs text-gray-400 mt-1">
                {{ formatQty(report.summary.totalQuantity) }} {{ $t('reports.sales.pcs') }}
              </p>
            </div>
          </button>
          <button
            @click="filter = 'low'"
            :class="['card cursor-pointer transition-all text-left', filter === 'low' ? 'ring-2 ring-yellow-500' : '']"
          >
            <div class="card-body">
              <p class="text-sm text-gray-500">{{ $t('reports.inventory.lowStockCount') }}</p>
              <p class="text-2xl font-bold text-yellow-600">{{ report.summary?.lowStockCount || 0 }}</p>
            </div>
          </button>
          <button
            @click="filter = 'out'"
            :class="['card cursor-pointer transition-all text-left', filter === 'out' ? 'ring-2 ring-red-500' : '']"
          >
            <div class="card-body">
              <p class="text-sm text-gray-500">{{ $t('reports.inventory.outOfStockCount') }}</p>
              <p class="text-2xl font-bold text-red-600">{{ report.summary?.outOfStockCount || 0 }}</p>
            </div>
          </button>
          <div class="card">
            <div class="card-body">
              <p class="text-sm text-gray-500">{{ $t('reports.inventory.totalValue') }}</p>
              <p class="text-2xl font-bold text-primary-600">{{ formatCurrency(report.summary?.totalValue) }}</p>
            </div>
          </div>
        </div>

        <!-- Products Table -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">
              {{ filter === 'all' ? $t('reports.inventory.allProducts') : filter === 'low' ? $t('reports.inventory.lowStockProducts') : $t('reports.inventory.outOfStockProducts') }}
            </h3>
          </div>
          <div v-if="filteredItems.length" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('reports.inventory.product') }}</th>
                  <th>{{ $t('reports.inventory.sku') }}</th>
                  <th>{{ $t('reports.inventory.location') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.available') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.reserved') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.free') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.minLevel') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.value') }}</th>
                  <th>{{ $t('status') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="item in filteredItems" :key="item.productId">
                  <td>
                    <div class="flex items-center">
                      <ExclamationTriangleIcon
                        v-if="item.stockStatus !== 'IN_STOCK'"
                        class="h-5 w-5 mr-2"
                        :class="item.stockStatus === 'OUT_OF_STOCK' ? 'text-red-500' : 'text-yellow-500'"
                      />
                      <div>
                        <span class="font-medium">{{ item.productName }}</span>
                        <span v-if="item.category" class="text-xs text-gray-400 block">{{ item.category }}</span>
                      </div>
                    </div>
                  </td>
                  <td class="font-mono text-sm text-gray-500">{{ item.sku }}</td>
                  <td class="text-sm text-gray-500">{{ item.location }}</td>
                  <td class="text-right font-medium">{{ formatQty(item.quantityOnHand) }}</td>
                  <td class="text-right text-gray-500">{{ formatQty(item.quantityReserved) }}</td>
                  <td class="text-right font-medium">{{ formatQty(item.quantityAvailable) }}</td>
                  <td class="text-right text-gray-500">{{ formatQty(item.reorderPoint) }}</td>
                  <td class="text-right">{{ formatCurrency(item.totalValue) }}</td>
                  <td>
                    <span :class="['badge', getStatusClass(item.stockStatus)]">
                      {{ getStatusLabel(item.stockStatus) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="card-body text-center text-gray-500">
            {{ $t('noData') }}
          </div>
        </div>
      </template>
    </template>

    <!-- Valuation Tab -->
    <template v-if="activeTab === 'valuation'">
      <div v-if="loadingValuation" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <template v-else-if="valuation">
        <!-- Valuation Summary -->
        <div v-if="valuation.summary" class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div class="card">
            <div class="card-body">
              <p class="text-sm text-gray-500">{{ $t('reports.inventory.totalProducts') }}</p>
              <p class="text-2xl font-bold">{{ valuation.summary.totalSkus || valuation.summary.totalProducts || 0 }}</p>
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <p class="text-sm text-gray-500">{{ $t('reports.inventory.quantity') }}</p>
              <p class="text-2xl font-bold">{{ formatQty(valuation.summary.totalQuantity) }}</p>
            </div>
          </div>
          <div class="card">
            <div class="card-body">
              <p class="text-sm text-gray-500">{{ $t('reports.inventory.totalValuation') }}</p>
              <p class="text-2xl font-bold text-primary-600">{{ formatCurrency(valuation.summary.totalValue || valuation.summary.totalValuation) }}</p>
            </div>
          </div>
        </div>

        <!-- Valuation Table -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">{{ $t('reports.inventory.valuationTitle') }}</h3>
          </div>
          <div v-if="(valuation.items || valuation.content || []).length" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('reports.inventory.productName') }}</th>
                  <th>{{ $t('reports.inventory.sku') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.quantity') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.unitCost') }}</th>
                  <th class="text-right">{{ $t('reports.inventory.totalCost') }}</th>
                  <th>{{ $t('reports.inventory.valuationMethod') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="item in (valuation.items || valuation.content || [])" :key="item.productId || item.id">
                  <td class="font-medium">{{ item.productName || item.name }}</td>
                  <td class="font-mono text-sm text-gray-500">{{ item.sku }}</td>
                  <td class="text-right">{{ formatQty(item.quantity || item.quantityOnHand) }}</td>
                  <td class="text-right">{{ formatCurrency(item.unitCost || item.averageCost) }}</td>
                  <td class="text-right font-medium">{{ formatCurrency(item.totalCost || item.totalValue) }}</td>
                  <td class="text-sm text-gray-500">{{ item.valuationMethod || '-' }}</td>
                </tr>
              </tbody>
              <tfoot v-if="valuation.summary" class="border-t-2 border-gray-300">
                <tr class="font-bold">
                  <td colspan="4">{{ $t('reports.inventory.totalValuation') }}</td>
                  <td class="text-right">{{ formatCurrency(valuation.summary.totalValue || valuation.summary.totalValuation) }}</td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>
          <div v-else class="card-body text-center text-gray-500">
            {{ $t('reports.inventory.noValuationData') }}
          </div>
        </div>
      </template>

      <div v-else class="card-body text-center text-gray-500">
        {{ $t('reports.inventory.noValuationData') }}
      </div>
    </template>
  </div>
</template>
