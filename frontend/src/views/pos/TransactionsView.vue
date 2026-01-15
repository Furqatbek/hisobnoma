<script setup>
import { ref, onMounted } from 'vue'
import { posApi } from '@/services/api'
import { EyeIcon, MagnifyingGlassIcon, PrinterIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import ReceiptTemplate from '@/components/ReceiptTemplate.vue'

const transactions = ref([])
const loading = ref(true)
const search = ref('')
const dateFilter = ref('today')

// Modal state
const showDetailModal = ref(false)
const selectedTransaction = ref(null)
const loadingDetail = ref(false)
const receiptRef = ref(null)

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

async function fetchTransactions() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }

    if (dateFilter.value === 'today') {
      params.date = new Date().toISOString().split('T')[0]
    } else if (dateFilter.value === 'week') {
      const weekAgo = new Date()
      weekAgo.setDate(weekAgo.getDate() - 7)
      params.startDate = weekAgo.toISOString().split('T')[0]
      params.endDate = new Date().toISOString().split('T')[0]
    } else if (dateFilter.value === 'month') {
      const monthAgo = new Date()
      monthAgo.setMonth(monthAgo.getMonth() - 1)
      params.startDate = monthAgo.toISOString().split('T')[0]
      params.endDate = new Date().toISOString().split('T')[0]
    }

    if (search.value) {
      params.search = search.value
    }

    const response = await posApi.getTransactions(params)
    // Handle both response formats
    const data = response.data.data || response.data
    transactions.value = data.content || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 0
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || 0
  } catch (error) {
    console.error('Tranzaksiyalarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

async function viewTransaction(tx) {
  loadingDetail.value = true
  showDetailModal.value = true
  selectedTransaction.value = tx

  try {
    // Fetch full transaction details
    const response = await posApi.getTransaction(tx.id)
    selectedTransaction.value = response.data.data || response.data
  } catch (error) {
    console.error('Tranzaksiya ma\'lumotlarini yuklashda xatolik:', error)
    // Keep the basic transaction data if detail fetch fails
  } finally {
    loadingDetail.value = false
  }
}

function closeModal() {
  showDetailModal.value = false
  selectedTransaction.value = null
}

function printReceipt() {
  if (receiptRef.value) {
    receiptRef.value.printReceipt()
  }
}

onMounted(fetchTransactions)

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleString('uz-UZ')
}

function getStatusClass(status) {
  switch (status) {
    case 'COMPLETED': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'VOIDED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function getStatusLabel(status) {
  const labels = {
    'COMPLETED': 'Yakunlangan',
    'PENDING': 'Kutilmoqda',
    'VOIDED': 'Bekor qilingan',
    'IN_PROGRESS': 'Jarayonda'
  }
  return labels[status] || status
}

function getPaymentLabel(type) {
  const labels = {
    'CASH': 'Naqd',
    'CARD': 'Karta',
    'CREDIT': 'Nasiya',
    'MOBILE_PAYMENT': 'Mobil'
  }
  return labels[type] || type
}

function handleSearch() {
  pagination.value.page = 0
  fetchTransactions()
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Sotuv tarixi</h1>
      <p class="mt-1 text-sm text-gray-500">Barcha tranzaksiyalar ro'yxati</p>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            placeholder="Qidiruv (chek raqami, mijoz)..."
            class="input pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <select v-model="dateFilter" @change="fetchTransactions" class="input w-auto">
          <option value="today">Bugun</option>
          <option value="week">Shu hafta</option>
          <option value="month">Shu oy</option>
          <option value="all">Barchasi</option>
        </select>
        <button @click="handleSearch" class="btn-primary">
          Qidirish
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="transactions.length === 0" class="text-center py-12">
        <p class="text-gray-500">Tranzaksiyalar topilmadi</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Chek №</th>
              <th>Sana</th>
              <th>Mijoz</th>
              <th>Mahsulotlar</th>
              <th class="text-right">Summa</th>
              <th>Holat</th>
              <th>To'lov turi</th>
              <th class="text-right">Amallar</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="tx in transactions" :key="tx.id">
              <td class="font-mono text-sm">{{ tx.transactionNumber || `#${tx.id}` }}</td>
              <td class="text-sm text-gray-500">{{ formatDate(tx.createdAt) }}</td>
              <td>{{ tx.customer?.name || 'Tashrif buyuruvchi' }}</td>
              <td>{{ tx.items?.length || 0 }} ta</td>
              <td class="text-right font-medium">{{ formatCurrency(tx.totalAmount) }} so'm</td>
              <td>
                <span :class="['badge', getStatusClass(tx.status)]">
                  {{ getStatusLabel(tx.status) }}
                </span>
              </td>
              <td class="text-sm">
                <span v-if="tx.payments?.length === 1">
                  {{ getPaymentLabel(tx.payments[0]?.paymentType) }}
                </span>
                <span v-else-if="tx.payments?.length > 1">
                  Aralash ({{ tx.payments.length }})
                </span>
                <span v-else>-</span>
              </td>
              <td class="text-right space-x-1">
                <button
                  @click="viewTransaction(tx)"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Ko'rish"
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
            @click="pagination.page--; fetchTransactions()"
            :disabled="pagination.page === 0"
            class="btn-secondary"
          >
            Oldingi
          </button>
          <span class="text-sm text-gray-500">
            Sahifa {{ pagination.page + 1 }} / {{ pagination.totalPages }}
            (Jami: {{ pagination.totalElements }})
          </span>
          <button
            @click="pagination.page++; fetchTransactions()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            Keyingi
          </button>
        </div>
      </div>
    </div>

    <!-- Transaction Detail Modal -->
    <div
      v-if="showDetailModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closeModal"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
        <!-- Modal Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 class="text-xl font-bold text-gray-900">
              Tranzaksiya tafsilotlari
            </h2>
            <p class="text-sm text-gray-500">
              {{ selectedTransaction?.transactionNumber || `#${selectedTransaction?.id}` }}
            </p>
          </div>
          <div class="flex items-center gap-2">
            <button
              @click="printReceipt"
              class="btn-primary flex items-center gap-2"
              :disabled="loadingDetail"
            >
              <PrinterIcon class="h-5 w-5" />
              Chop etish
            </button>
            <button
              @click="closeModal"
              class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
            >
              <XMarkIcon class="h-6 w-6" />
            </button>
          </div>
        </div>

        <!-- Modal Content -->
        <div class="flex overflow-hidden">
          <!-- Transaction Details -->
          <div class="flex-1 p-6 overflow-y-auto max-h-[calc(90vh-80px)]">
            <div v-if="loadingDetail" class="flex items-center justify-center h-64">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="selectedTransaction" class="space-y-6">
              <!-- Basic Info -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-sm text-gray-500">Sana</label>
                  <p class="font-medium">{{ formatDate(selectedTransaction.createdAt) }}</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">Holat</label>
                  <p>
                    <span :class="['badge', getStatusClass(selectedTransaction.status)]">
                      {{ getStatusLabel(selectedTransaction.status) }}
                    </span>
                  </p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">Mijoz</label>
                  <p class="font-medium">{{ selectedTransaction.customer?.name || 'Tashrif buyuruvchi' }}</p>
                  <p v-if="selectedTransaction.customer?.phone" class="text-sm text-gray-500">
                    {{ selectedTransaction.customer.phone }}
                  </p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">Terminal</label>
                  <p class="font-medium">{{ selectedTransaction.terminal?.name || '-' }}</p>
                </div>
              </div>

              <!-- Items -->
              <div>
                <h3 class="font-semibold text-gray-900 mb-3">Mahsulotlar</h3>
                <div class="bg-gray-50 rounded-lg overflow-hidden">
                  <table class="w-full">
                    <thead class="bg-gray-100">
                      <tr>
                        <th class="px-4 py-2 text-left text-sm font-medium text-gray-600">Mahsulot</th>
                        <th class="px-4 py-2 text-right text-sm font-medium text-gray-600">Soni</th>
                        <th class="px-4 py-2 text-right text-sm font-medium text-gray-600">Narxi</th>
                        <th class="px-4 py-2 text-right text-sm font-medium text-gray-600">Jami</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                      <tr v-for="item in selectedTransaction.items" :key="item.id">
                        <td class="px-4 py-3">
                          <p class="font-medium">{{ item.product?.name || item.productName }}</p>
                          <p v-if="item.product?.sku" class="text-sm text-gray-500">{{ item.product.sku }}</p>
                        </td>
                        <td class="px-4 py-3 text-right">{{ item.quantity }}</td>
                        <td class="px-4 py-3 text-right">{{ formatCurrency(item.unitPrice) }} so'm</td>
                        <td class="px-4 py-3 text-right font-medium">{{ formatCurrency(item.quantity * item.unitPrice) }} so'm</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Totals -->
              <div class="bg-gray-50 rounded-lg p-4">
                <div class="space-y-2">
                  <div class="flex justify-between" v-if="selectedTransaction.subtotal">
                    <span class="text-gray-600">Oraliq summa:</span>
                    <span>{{ formatCurrency(selectedTransaction.subtotal) }} so'm</span>
                  </div>
                  <div class="flex justify-between" v-if="selectedTransaction.discount">
                    <span class="text-gray-600">Chegirma:</span>
                    <span class="text-green-600">-{{ formatCurrency(selectedTransaction.discount) }} so'm</span>
                  </div>
                  <div class="flex justify-between" v-if="selectedTransaction.tax">
                    <span class="text-gray-600">Soliq:</span>
                    <span>{{ formatCurrency(selectedTransaction.tax) }} so'm</span>
                  </div>
                  <div class="flex justify-between text-lg font-bold border-t pt-2">
                    <span>Jami:</span>
                    <span>{{ formatCurrency(selectedTransaction.totalAmount) }} so'm</span>
                  </div>
                </div>
              </div>

              <!-- Payments -->
              <div v-if="selectedTransaction.payments?.length">
                <h3 class="font-semibold text-gray-900 mb-3">To'lovlar</h3>
                <div class="space-y-2">
                  <div
                    v-for="payment in selectedTransaction.payments"
                    :key="payment.id"
                    class="flex justify-between items-center bg-gray-50 rounded-lg px-4 py-3"
                  >
                    <span class="flex items-center gap-2">
                      <span class="badge badge-info">{{ getPaymentLabel(payment.paymentType) }}</span>
                    </span>
                    <span class="font-medium">{{ formatCurrency(payment.amount) }} so'm</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Receipt Preview -->
          <div class="w-80 border-l border-gray-200 bg-gray-50 p-4 overflow-y-auto max-h-[calc(90vh-80px)]">
            <h3 class="font-semibold text-gray-900 mb-4 text-center">Chek ko'rinishi</h3>
            <div class="bg-white shadow-lg rounded-lg overflow-hidden">
              <ReceiptTemplate
                v-if="selectedTransaction"
                ref="receiptRef"
                :transaction="selectedTransaction"
                type="sale"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
