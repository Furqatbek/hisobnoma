<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { shiftsApi } from '@/services/api'
import { EyeIcon, MagnifyingGlassIcon, XMarkIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const shifts = ref([])
const loading = ref(true)

// Modal state
const showDetailModal = ref(false)
const selectedShift = ref(null)

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

async function fetchShifts() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    const response = await shiftsApi.getAll(params)
    const data = response.data.data || response.data
    shifts.value = data.content || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 0
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || 0
  } catch (error) {
    console.error('Smenalarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

function viewShift(shift) {
  selectedShift.value = shift
  showDetailModal.value = true
}

function closeModal() {
  showDetailModal.value = false
  selectedShift.value = null
}

onMounted(fetchShifts)

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDateTime(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('uz-UZ', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getStatusClass(status) {
  const classes = {
    'OPEN': 'badge-success',
    'CLOSED': 'badge-warning',
    'RECONCILED': 'badge-info'
  }
  return classes[status] || 'badge-info'
}

function getStatusLabel(status) {
  const labels = {
    'OPEN': t('pos.shifts.open'),
    'CLOSED': t('pos.shifts.closed'),
    'RECONCILED': t('pos.shifts.reconciled')
  }
  return labels[status] || status
}

function getDifferenceClass(value) {
  if (!value || value == 0) return 'text-gray-900'
  return value > 0 ? 'text-green-600' : 'text-red-600'
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('pos.shifts.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('pos.shifts.subtitle') }}</p>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="shifts.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('pos.shifts.noShifts') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('pos.shifts.shiftNumber') }}</th>
              <th>{{ $t('pos.shifts.terminal') }}</th>
              <th>{{ $t('pos.shifts.openedBy') }}</th>
              <th>{{ $t('pos.shifts.openedAt') }}</th>
              <th>{{ $t('pos.shifts.closedAt') }}</th>
              <th class="text-right">{{ $t('pos.shifts.openingBalance') }}</th>
              <th class="text-right">{{ $t('pos.shifts.totalSales') }}</th>
              <th class="text-center">{{ $t('pos.transactions.title') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="shift in shifts" :key="shift.id">
              <td class="font-mono text-sm">{{ shift.shiftNumber }}</td>
              <td>{{ shift.terminalName || shift.terminalCode || '-' }}</td>
              <td>{{ shift.cashierName || '-' }}</td>
              <td class="text-sm">{{ formatDateTime(shift.openedAt) }}</td>
              <td class="text-sm">{{ formatDateTime(shift.closedAt) }}</td>
              <td class="text-right">{{ formatCurrency(shift.openingCash) }}</td>
              <td class="text-right font-medium">{{ formatCurrency(shift.totalSales) }}</td>
              <td class="text-center">{{ shift.transactionCount || 0 }}</td>
              <td>
                <span :class="['badge', getStatusClass(shift.status)]">{{ getStatusLabel(shift.status) }}</span>
              </td>
              <td class="text-right">
                <button
                  @click="viewShift(shift)"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Batafsil ko'rish"
                >
                  <EyeIcon class="h-5 w-5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <button
            @click="pagination.page--; fetchShifts()"
            :disabled="pagination.page === 0"
            class="btn-secondary"
          >
            {{ $t('previous') }}
          </button>
          <span class="text-sm text-gray-500">
            {{ $t('page') }} {{ pagination.page + 1 }} / {{ pagination.totalPages }}
            ({{ $t('totalCount') }}: {{ pagination.totalElements }})
          </span>
          <button
            @click="pagination.page++; fetchShifts()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            {{ $t('next') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Shift Detail Modal -->
    <div
      v-if="showDetailModal && selectedShift"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closeModal"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden">
        <!-- Modal Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 class="text-xl font-bold text-gray-900">{{ $t('pos.shifts.shiftDetails') }}</h2>
            <p class="text-sm text-gray-500">{{ selectedShift.shiftNumber }}</p>
          </div>
          <div class="flex items-center gap-2">
            <span :class="['badge', getStatusClass(selectedShift.status)]">
              {{ getStatusLabel(selectedShift.status) }}
            </span>
            <button
              @click="closeModal"
              class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
            >
              <XMarkIcon class="h-6 w-6" />
            </button>
          </div>
        </div>

        <!-- Modal Content -->
        <div class="p-6 overflow-y-auto max-h-[calc(90vh-80px)] space-y-6">

          <!-- Basic Info -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <label class="text-sm text-gray-500">{{ $t('pos.shifts.terminal') }}</label>
              <p class="font-medium">{{ selectedShift.terminalName || '-' }}</p>
              <p v-if="selectedShift.terminalCode" class="text-xs text-gray-400">{{ selectedShift.terminalCode }}</p>
            </div>
            <div>
              <label class="text-sm text-gray-500">{{ $t('pos.shifts.openedBy') }}</label>
              <p class="font-medium">{{ selectedShift.cashierName || '-' }}</p>
            </div>
            <div>
              <label class="text-sm text-gray-500">{{ $t('pos.shifts.openedAt') }}</label>
              <p class="font-medium text-sm">{{ formatDateTime(selectedShift.openedAt) }}</p>
            </div>
            <div>
              <label class="text-sm text-gray-500">{{ $t('pos.shifts.closedAt') }}</label>
              <p class="font-medium text-sm">{{ formatDateTime(selectedShift.closedAt) }}</p>
            </div>
          </div>

          <!-- Cash Summary -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-3">{{ $t('pos.shifts.cashReport') }}</h3>
            <div class="bg-gray-50 rounded-lg p-4">
              <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.shifts.openingBalance') }}</label>
                  <p class="text-lg font-semibold">{{ formatCurrency(selectedShift.openingCash) }} so'm</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.shifts.closingBalance') }}</label>
                  <p class="text-lg font-semibold">{{ formatCurrency(selectedShift.closingCash) }} so'm</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.shifts.expectedCash') }}</label>
                  <p class="text-lg font-semibold">{{ formatCurrency(selectedShift.expectedCash) }} so'm</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.shifts.difference') }}</label>
                  <p :class="['text-lg font-semibold', getDifferenceClass(selectedShift.cashDifference)]">
                    {{ selectedShift.cashDifference > 0 ? '+' : '' }}{{ formatCurrency(selectedShift.cashDifference) }} so'm
                  </p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.shifts.cashIn') }}</label>
                  <p class="text-lg font-semibold text-green-600">+{{ formatCurrency(selectedShift.cashIn) }} so'm</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">{{ $t('pos.shifts.cashOut') }}</label>
                  <p class="text-lg font-semibold text-red-600">-{{ formatCurrency(selectedShift.cashOut) }} so'm</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Sales Summary -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-3">{{ $t('pos.shifts.salesInfo') }}</h3>
            <div class="bg-gray-50 rounded-lg overflow-hidden">
              <table class="w-full">
                <tbody class="divide-y divide-gray-200">
                  <tr>
                    <td class="px-4 py-3 text-gray-600">{{ $t('pos.shifts.totalSales') }}</td>
                    <td class="px-4 py-3 text-right font-semibold">{{ formatCurrency(selectedShift.totalSales) }} so'm</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-3 text-gray-600">{{ $t('pos.shifts.returns') }}</td>
                    <td class="px-4 py-3 text-right font-semibold text-red-600">-{{ formatCurrency(selectedShift.totalReturns) }} so'm</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-3 text-gray-600">{{ $t('pos.shifts.discounts') }}</td>
                    <td class="px-4 py-3 text-right font-semibold text-orange-600">-{{ formatCurrency(selectedShift.totalDiscounts) }} so'm</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-3 text-gray-600">{{ $t('pos.shifts.taxes') }}</td>
                    <td class="px-4 py-3 text-right font-semibold">{{ formatCurrency(selectedShift.totalTaxes) }} so'm</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Payment Breakdown -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-3">{{ $t('pos.shifts.paymentTypes') }}</h3>
            <div class="grid grid-cols-3 gap-4">
              <div class="bg-green-50 rounded-lg p-4 text-center">
                <label class="text-sm text-green-700">{{ $t('pos.cash') }}</label>
                <p class="text-lg font-bold text-green-800">{{ formatCurrency(selectedShift.cashPayments) }}</p>
              </div>
              <div class="bg-blue-50 rounded-lg p-4 text-center">
                <label class="text-sm text-blue-700">{{ $t('pos.card') }}</label>
                <p class="text-lg font-bold text-blue-800">{{ formatCurrency(selectedShift.cardPayments) }}</p>
              </div>
              <div class="bg-purple-50 rounded-lg p-4 text-center">
                <label class="text-sm text-purple-700">{{ $t('pos.shifts.other') }}</label>
                <p class="text-lg font-bold text-purple-800">{{ formatCurrency(selectedShift.otherPayments) }}</p>
              </div>
            </div>
          </div>

          <!-- Transaction Counts -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-3">{{ $t('pos.transactions.title') }}</h3>
            <div class="grid grid-cols-3 gap-4">
              <div class="bg-gray-50 rounded-lg p-4 text-center">
                <label class="text-sm text-gray-600">{{ $t('total') }}</label>
                <p class="text-2xl font-bold text-gray-900">{{ selectedShift.transactionCount || 0 }}</p>
              </div>
              <div class="bg-red-50 rounded-lg p-4 text-center">
                <label class="text-sm text-red-700">{{ $t('pos.transactions.voided') }}</label>
                <p class="text-2xl font-bold text-red-800">{{ selectedShift.voidedCount || 0 }}</p>
              </div>
              <div class="bg-orange-50 rounded-lg p-4 text-center">
                <label class="text-sm text-orange-700">{{ $t('pos.shifts.returned') }}</label>
                <p class="text-2xl font-bold text-orange-800">{{ selectedShift.returnCount || 0 }}</p>
              </div>
            </div>
          </div>

          <!-- Notes -->
          <div v-if="selectedShift.notes || selectedShift.closingNotes">
            <h3 class="font-semibold text-gray-900 mb-3">{{ $t('notes') }}</h3>
            <div class="space-y-2">
              <div v-if="selectedShift.notes" class="bg-gray-50 rounded-lg p-3">
                <label class="text-xs text-gray-500">{{ $t('pos.shifts.openingNote') }}</label>
                <p class="text-gray-700">{{ selectedShift.notes }}</p>
              </div>
              <div v-if="selectedShift.closingNotes" class="bg-gray-50 rounded-lg p-3">
                <label class="text-xs text-gray-500">{{ $t('pos.shifts.closingNote') }}</label>
                <p class="text-gray-700">{{ selectedShift.closingNotes }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
