<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { purchaseOrdersApi } from '@/services/api'
import { PlusIcon, EyeIcon, MagnifyingGlassIcon, PrinterIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import ReceiptTemplate from '@/components/ReceiptTemplate.vue'

const orders = ref([])
const loading = ref(true)
const search = ref('')

// Modal state
const showDetailModal = ref(false)
const selectedOrder = ref(null)
const loadingDetail = ref(false)
const receiptRef = ref(null)

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

async function fetchOrders() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (search.value) {
      params.search = search.value
    }
    const response = await purchaseOrdersApi.getAll(params)
    const data = response.data.data || response.data
    orders.value = data.content || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 0
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || 0
  } catch (error) {
    console.error('Buyurtmalarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

async function viewOrder(order) {
  loadingDetail.value = true
  showDetailModal.value = true
  selectedOrder.value = order

  try {
    const response = await purchaseOrdersApi.getById(order.id)
    selectedOrder.value = response.data.data || response.data
  } catch (error) {
    console.error('Buyurtma ma\'lumotlarini yuklashda xatolik:', error)
  } finally {
    loadingDetail.value = false
  }
}

function closeModal() {
  showDetailModal.value = false
  selectedOrder.value = null
}

function printReceipt() {
  if (receiptRef.value) {
    receiptRef.value.printReceipt()
  }
}

onMounted(fetchOrders)

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(date) {
  return new Date(date).toLocaleDateString('uz-UZ')
}

function getStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'PENDING': 'badge-warning',
    'APPROVED': 'badge-success',
    'PARTIAL': 'badge-warning',
    'RECEIVED': 'badge-success',
    'CANCELLED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getStatusLabel(status) {
  const labels = {
    'DRAFT': 'Qoralama',
    'PENDING': 'Kutilmoqda',
    'APPROVED': 'Tasdiqlangan',
    'PARTIAL': 'Qisman qabul',
    'RECEIVED': 'Qabul qilingan',
    'CANCELLED': 'Bekor qilingan'
  }
  return labels[status] || status
}

function handleSearch() {
  pagination.value.page = 0
  fetchOrders()
}

// Transform purchase order to transaction format for receipt
function transformOrderForReceipt(order) {
  return {
    id: order.id,
    orderNumber: order.poNumber || `PO-${order.id}`,
    orderDate: order.orderDate || order.createdAt,
    createdAt: order.createdAt,
    customer: { name: order.vendorName },
    items: (order.lines || []).map(item => ({
      ...item,
      productName: item.productName,
      price: item.unitPrice,
      unitPrice: item.unitPrice
    })),
    totalAmount: order.totalAmount,
    subtotal: order.subtotal,
    tax: order.taxAmount,
    payments: []
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Xarid buyurtmalari</h1>
        <p class="mt-1 text-sm text-gray-500">Yetkazib beruvchilardan buyurtmalar</p>
      </div>
      <RouterLink to="/purchases/orders/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        Yangi buyurtma
      </RouterLink>
    </div>

    <!-- Search -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            placeholder="Qidiruv (buyurtma raqami, yetkazib beruvchi)..."
            class="input pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <button @click="handleSearch" class="btn-primary">
          Qidirish
        </button>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="orders.length === 0" class="text-center py-12">
        <p class="text-gray-500">Buyurtmalar topilmadi</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Buyurtma №</th>
              <th>Yetkazib beruvchi</th>
              <th>Sana</th>
              <th>Mahsulotlar</th>
              <th class="text-right">Summa</th>
              <th>Holat</th>
              <th class="text-right">Amallar</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="order in orders" :key="order.id">
              <td class="font-mono">{{ order.poNumber || `PO-${order.id}` }}</td>
              <td>{{ order.vendorName || '-' }}</td>
              <td>{{ formatDate(order.orderDate || order.createdAt) }}</td>
              <td>{{ order.lines?.length || '-' }}</td>
              <td class="text-right font-medium">{{ formatCurrency(order.totalAmount) }} so'm</td>
              <td>
                <span :class="['badge', getStatusClass(order.status)]">{{ getStatusLabel(order.status) }}</span>
              </td>
              <td class="text-right space-x-1">
                <button
                  @click="viewOrder(order)"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Ko'rish va chop etish"
                >
                  <EyeIcon class="h-5 w-5" />
                </button>
                <RouterLink
                  :to="`/purchases/orders/${order.id}`"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Tafsilotlar"
                >
                  <PrinterIcon class="h-5 w-5" />
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
            @click="pagination.page--; fetchOrders()"
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
            @click="pagination.page++; fetchOrders()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            Keyingi
          </button>
        </div>
      </div>
    </div>

    <!-- Order Detail Modal -->
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
              Buyurtma tafsilotlari
            </h2>
            <p class="text-sm text-gray-500">
              {{ selectedOrder?.poNumber || `PO-${selectedOrder?.id}` }}
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
          <!-- Order Details -->
          <div class="flex-1 p-6 overflow-y-auto max-h-[calc(90vh-80px)]">
            <div v-if="loadingDetail" class="flex items-center justify-center h-64">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="selectedOrder" class="space-y-6">
              <!-- Basic Info -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-sm text-gray-500">Sana</label>
                  <p class="font-medium">{{ formatDate(selectedOrder.orderDate || selectedOrder.createdAt) }}</p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">Holat</label>
                  <p>
                    <span :class="['badge', getStatusClass(selectedOrder.status)]">
                      {{ getStatusLabel(selectedOrder.status) }}
                    </span>
                  </p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">Yetkazib beruvchi</label>
                  <p class="font-medium">{{ selectedOrder.vendorName || '-' }}</p>
                  <p v-if="selectedOrder.vendorCode" class="text-sm text-gray-500">
                    {{ selectedOrder.vendorCode }}
                  </p>
                </div>
                <div>
                  <label class="text-sm text-gray-500">Ombor</label>
                  <p class="font-medium">{{ selectedOrder.locationName || '-' }}</p>
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
                      <tr v-for="line in selectedOrder.lines" :key="line.id">
                        <td class="px-4 py-3">
                          <p class="font-medium">{{ line.productName }}</p>
                          <p v-if="line.productSku" class="text-sm text-gray-500">{{ line.productSku }}</p>
                        </td>
                        <td class="px-4 py-3 text-right">{{ line.quantity }}</td>
                        <td class="px-4 py-3 text-right">{{ formatCurrency(line.unitPrice) }} so'm</td>
                        <td class="px-4 py-3 text-right font-medium">{{ formatCurrency(line.lineTotal) }} so'm</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Totals -->
              <div class="bg-gray-50 rounded-lg p-4">
                <div class="space-y-2">
                  <div class="flex justify-between" v-if="selectedOrder.subtotal">
                    <span class="text-gray-600">Oraliq summa:</span>
                    <span>{{ formatCurrency(selectedOrder.subtotal) }} so'm</span>
                  </div>
                  <div class="flex justify-between" v-if="selectedOrder.taxAmount">
                    <span class="text-gray-600">Soliq:</span>
                    <span>{{ formatCurrency(selectedOrder.taxAmount) }} so'm</span>
                  </div>
                  <div class="flex justify-between text-lg font-bold border-t pt-2">
                    <span>Jami:</span>
                    <span>{{ formatCurrency(selectedOrder.totalAmount) }} so'm</span>
                  </div>
                </div>
              </div>

              <!-- Notes -->
              <div v-if="selectedOrder.notes">
                <h3 class="font-semibold text-gray-900 mb-2">Izohlar</h3>
                <p class="text-gray-600 bg-gray-50 rounded-lg p-3">{{ selectedOrder.notes }}</p>
              </div>
            </div>
          </div>

          <!-- Receipt Preview -->
          <div class="w-80 border-l border-gray-200 bg-gray-50 p-4 overflow-y-auto max-h-[calc(90vh-80px)]">
            <h3 class="font-semibold text-gray-900 mb-4 text-center">Chek ko'rinishi</h3>
            <div class="bg-white shadow-lg rounded-lg overflow-hidden">
              <ReceiptTemplate
                v-if="selectedOrder"
                ref="receiptRef"
                :transaction="transformOrderForReceipt(selectedOrder)"
                type="purchase"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
