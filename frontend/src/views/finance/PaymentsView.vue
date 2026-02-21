<script setup>
import { ref, computed, onMounted } from 'vue'
import { arPaymentsApi } from '@/services/api'
import { MagnifyingGlassIcon, XMarkIcon, EyeIcon, XCircleIcon } from '@heroicons/vue/24/outline'

const loading = ref(true)
const payments = ref([])
const totalPages = ref(0)
const currentPage = ref(0)
const search = ref('')
const statusFilter = ref('all')
const error = ref('')
const successMsg = ref('')

// Detail modal
const showDetailModal = ref(false)
const selectedPayment = ref(null)

async function fetchPayments(page = 0) {
  loading.value = true
  error.value = ''
  try {
    let res
    if (statusFilter.value !== 'all') {
      res = await arPaymentsApi.getByStatus(statusFilter.value, { page, size: 20, sort: 'createdAt,desc' })
    } else {
      res = await arPaymentsApi.getAll({ page, size: 20, sort: 'createdAt,desc' })
    }
    const data = res.data.data || res.data
    payments.value = data.content || data || []
    totalPages.value = data.totalPages || 1
    currentPage.value = data.number || 0
  } catch (e) {
    error.value = e.response?.data?.message || 'To\'lovlarni yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchPayments())

const filteredPayments = computed(() => {
  if (!search.value) return payments.value
  const q = search.value.toLowerCase()
  return payments.value.filter(p =>
    p.paymentNumber?.toLowerCase().includes(q) ||
    p.customerName?.toLowerCase().includes(q) ||
    p.notes?.toLowerCase().includes(q)
  )
})

function onFilterChange() {
  fetchPayments(0)
}

async function viewPayment(payment) {
  try {
    const res = await arPaymentsApi.getById(payment.id)
    selectedPayment.value = res.data.data || res.data
    showDetailModal.value = true
  } catch (e) {
    error.value = 'To\'lov ma\'lumotlarini yuklashda xatolik'
  }
}

async function cancelPayment(payment) {
  const reason = prompt('Bekor qilish sababi:')
  if (!reason) return
  try {
    await arPaymentsApi.cancel(payment.id, reason)
    successMsg.value = `${payment.paymentNumber} bekor qilindi`
    showDetailModal.value = false
    await fetchPayments(currentPage.value)
  } catch (e) {
    error.value = e.response?.data?.message || 'Bekor qilishda xatolik'
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('uz-UZ')
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('uz-UZ', { dateStyle: 'short', timeStyle: 'short' })
}

function getStatusLabel(status) {
  const labels = {
    'PENDING': 'Kutilmoqda',
    'COMPLETED': 'Bajarildi',
    'DEPOSITED': 'Depozitga qo\'yildi',
    'CANCELLED': 'Bekor qilingan',
    'REFUNDED': 'Qaytarilgan'
  }
  return labels[status] || status
}

function getStatusClass(status) {
  switch (status) {
    case 'COMPLETED': case 'DEPOSITED': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'CANCELLED': case 'REFUNDED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function getMethodLabel(method) {
  const labels = {
    'CASH': 'Naqd pul',
    'BANK_TRANSFER': 'Bank o\'tkazmasi',
    'CREDIT_CARD': 'Kredit karta',
    'DEBIT_CARD': 'Debet karta',
    'MOBILE_PAYMENT': 'Mobil to\'lov',
    'ONLINE_PAYMENT': 'Onlayn to\'lov',
    'CHECK': 'Chek',
    'STORE_CREDIT': 'Do\'kon krediti',
    'OTHER': 'Boshqa'
  }
  return labels[method] || method
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">To'lovlar</h1>
      <p class="mt-1 text-sm text-gray-500">Qabul qilingan to'lovlar ro'yxati</p>
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

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            placeholder="Qidiruv (raqam, mijoz)..."
            class="input pl-10"
          />
        </div>
        <select v-model="statusFilter" @change="onFilterChange" class="input w-auto">
          <option value="all">Barcha holatlar</option>
          <option value="PENDING">Kutilmoqda</option>
          <option value="COMPLETED">Bajarildi</option>
          <option value="CANCELLED">Bekor qilingan</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="filteredPayments.length === 0" class="text-center py-12">
        <p class="text-gray-500">To'lovlar topilmadi</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>To'lov raqami</th>
              <th>Mijoz</th>
              <th>Sana</th>
              <th>Usul</th>
              <th class="text-right">Summa</th>
              <th>Holat</th>
              <th class="text-right">Amallar</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="payment in filteredPayments" :key="payment.id">
              <td>
                <p class="font-medium text-sm">{{ payment.paymentNumber }}</p>
              </td>
              <td>
                <p class="text-sm">{{ payment.customerName || '-' }}</p>
              </td>
              <td class="text-sm text-gray-500">
                {{ formatDate(payment.paymentDate) }}
              </td>
              <td class="text-sm text-gray-500">
                {{ getMethodLabel(payment.paymentMethod) }}
              </td>
              <td class="text-right">
                <span class="font-semibold text-green-600">{{ formatCurrency(payment.paymentAmount) }} so'm</span>
              </td>
              <td>
                <span :class="['badge text-xs', getStatusClass(payment.status)]">
                  {{ getStatusLabel(payment.status) }}
                </span>
              </td>
              <td class="text-right">
                <button
                  @click="viewPayment(payment)"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Batafsil"
                >
                  <EyeIcon class="h-5 w-5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
        <p class="text-sm text-gray-500">Sahifa {{ currentPage + 1 }} / {{ totalPages }}</p>
        <div class="flex gap-2">
          <button
            @click="fetchPayments(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm"
          >Oldingi</button>
          <button
            @click="fetchPayments(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm"
          >Keyingi</button>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <div
      v-if="showDetailModal && selectedPayment"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="showDetailModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedPayment.paymentNumber }}</h2>
            <p class="text-sm text-gray-500">To'lov tafsilotlari</p>
          </div>
          <button @click="showDetailModal = false" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
            <XMarkIcon class="h-6 w-6" />
          </button>
        </div>

        <div class="p-6 space-y-5">
          <!-- Amount -->
          <div class="bg-green-50 rounded-lg p-4 text-center">
            <p class="text-sm text-green-600">To'lov summasi</p>
            <p class="text-2xl font-bold text-green-700">{{ formatCurrency(selectedPayment.paymentAmount) }} so'm</p>
          </div>

          <!-- Info grid -->
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span class="text-gray-500">Mijoz:</span>
              <p class="font-medium">{{ selectedPayment.customerName }}</p>
            </div>
            <div>
              <span class="text-gray-500">Sana:</span>
              <p class="font-medium">{{ formatDate(selectedPayment.paymentDate) }}</p>
            </div>
            <div>
              <span class="text-gray-500">To'lov usuli:</span>
              <p class="font-medium">{{ getMethodLabel(selectedPayment.paymentMethod) }}</p>
            </div>
            <div>
              <span class="text-gray-500">Holat:</span>
              <p><span :class="['badge text-xs', getStatusClass(selectedPayment.status)]">{{ getStatusLabel(selectedPayment.status) }}</span></p>
            </div>
            <div v-if="selectedPayment.referenceNumber">
              <span class="text-gray-500">Referens:</span>
              <p class="font-medium">{{ selectedPayment.referenceNumber }}</p>
            </div>
            <div v-if="selectedPayment.notes">
              <span class="text-gray-500">Izoh:</span>
              <p class="font-medium">{{ selectedPayment.notes }}</p>
            </div>
            <div>
              <span class="text-gray-500">Yaratilgan:</span>
              <p class="font-medium">{{ formatDateTime(selectedPayment.createdAt) }}</p>
            </div>
            <div v-if="selectedPayment.glPosted">
              <span class="text-gray-500">GL jurnalga yozilgan:</span>
              <p class="font-medium text-green-600">Ha</p>
            </div>
          </div>

          <!-- Allocations -->
          <div v-if="selectedPayment.allocations?.length">
            <h3 class="font-semibold text-gray-900 mb-2">Fakturalarga taqsimlash</h3>
            <div class="bg-gray-50 rounded-lg overflow-hidden">
              <table class="w-full text-sm">
                <thead class="bg-gray-100">
                  <tr>
                    <th class="px-4 py-2 text-left text-gray-600">Faktura</th>
                    <th class="px-4 py-2 text-right text-gray-600">Summa</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="alloc in selectedPayment.allocations" :key="alloc.id">
                    <td class="px-4 py-2">
                      <p class="font-medium">{{ alloc.invoiceNumber || `Faktura #${alloc.arInvoiceId}` }}</p>
                      <p v-if="alloc.notes" class="text-xs text-gray-400">{{ alloc.notes }}</p>
                    </td>
                    <td class="px-4 py-2 text-right font-medium text-green-600">
                      {{ formatCurrency(alloc.allocatedAmount) }} so'm
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Cancel button -->
          <div v-if="selectedPayment.status === 'COMPLETED'" class="pt-2 border-t border-gray-200">
            <button
              @click="cancelPayment(selectedPayment)"
              class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100"
            >
              <XCircleIcon class="h-4 w-4" />
              To'lovni bekor qilish
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
