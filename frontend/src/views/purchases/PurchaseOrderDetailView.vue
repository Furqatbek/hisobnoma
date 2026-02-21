<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { purchaseOrdersApi } from '@/services/api'
import { ArrowLeftIcon, CheckIcon, XMarkIcon, TruckIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)

async function loadOrder() {
  try {
    const response = await purchaseOrdersApi.getById(route.params.id)
    order.value = response.data
  } catch (error) {
    console.error('Failed to load order:', error)
  } finally {
    loading.value = false
  }
}

onMounted(loadOrder)

async function approveOrder() {
  try {
    await purchaseOrdersApi.approve(order.value.id)
    await loadOrder()
  } catch (error) {
    alert('Failed to approve order')
  }
}

async function cancelOrder() {
  if (!confirm('Cancel this order?')) return
  try {
    await purchaseOrdersApi.cancel(order.value.id)
    await loadOrder()
  } catch (error) {
    alert('Failed to cancel order')
  }
}

async function receiveOrder() {
  try {
    await purchaseOrdersApi.receive(order.value.id)
    await loadOrder()
  } catch (error) {
    alert('Failed to receive order')
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0)
}

function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString()
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
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="flex items-center space-x-4">
        <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
          <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
        </button>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">
            Purchase Order {{ order?.poNumber || `#${route.params.id}` }}
          </h1>
          <p v-if="order" class="text-sm text-gray-500">Created {{ formatDate(order.createdAt) }}</p>
        </div>
      </div>

      <div v-if="order" class="flex space-x-3">
        <button
          v-if="['DRAFT', 'PENDING'].includes(order.status)"
          @click="approveOrder"
          class="btn-success"
        >
          <CheckIcon class="h-5 w-5 mr-2" />
          Tasdiqlash
        </button>
        <button
          v-if="['APPROVED', 'PARTIAL'].includes(order.status)"
          @click="receiveOrder"
          class="btn-primary"
        >
          <TruckIcon class="h-5 w-5 mr-2" />
          Qabul qilish
        </button>
        <button
          v-if="['DRAFT', 'PENDING', 'APPROVED'].includes(order.status)"
          @click="cancelOrder"
          class="btn-danger"
        >
          <XMarkIcon class="h-5 w-5 mr-2" />
          Bekor qilish
        </button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="order">
      <!-- Order Info -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Status</p>
            <span :class="['badge mt-1', getStatusClass(order.status)]">{{ getStatusLabel(order.status) }}</span>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Vendor</p>
            <p class="font-medium mt-1">{{ order.vendorName || '-' }}</p>
            <p class="text-xs text-gray-400">{{ order.vendorCode }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Location</p>
            <p class="font-medium mt-1">{{ order.locationName || '-' }}</p>
            <p class="text-xs text-gray-400">{{ order.locationCode }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Total Amount</p>
            <p class="text-2xl font-bold text-primary-600 mt-1">{{ formatCurrency(order.totalAmount) }} {{ order.currency }}</p>
          </div>
        </div>
      </div>

      <!-- Order Details -->
      <div class="card">
        <div class="card-body">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <p class="text-gray-500">Order Date</p>
              <p class="font-medium">{{ formatDate(order.orderDate) }}</p>
            </div>
            <div>
              <p class="text-gray-500">Expected Date</p>
              <p class="font-medium">{{ formatDate(order.expectedDate) }}</p>
            </div>
            <div>
              <p class="text-gray-500">Payment Terms</p>
              <p class="font-medium">{{ order.paymentTerms || '-' }}</p>
            </div>
            <div>
              <p class="text-gray-500">Currency</p>
              <p class="font-medium">{{ order.currency }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Items -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Order Items</h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Product</th>
                <th>UOM</th>
                <th class="text-right">Quantity</th>
                <th class="text-right">Received</th>
                <th class="text-right">Pending</th>
                <th class="text-right">Unit Price</th>
                <th class="text-right">Line Total</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="line in order.lines" :key="line.id">
                <td class="text-gray-500">{{ line.lineNumber }}</td>
                <td>
                  <div class="font-medium">{{ line.productName }}</div>
                  <div class="text-sm text-gray-500">{{ line.productSku }}</div>
                </td>
                <td>{{ line.uomName }}</td>
                <td class="text-right">{{ line.quantity }}</td>
                <td class="text-right">{{ line.receivedQuantity }}</td>
                <td class="text-right">{{ line.pendingQuantity }}</td>
                <td class="text-right">{{ formatCurrency(line.unitPrice) }}</td>
                <td class="text-right font-medium">{{ formatCurrency(line.lineTotal) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Totals -->
        <div class="card-body border-t">
          <div class="flex justify-end">
            <div class="w-64 space-y-2 text-sm">
              <div class="flex justify-between">
                <span class="text-gray-500">Subtotal</span>
                <span class="font-medium">{{ formatCurrency(order.subtotal) }}</span>
              </div>
              <div v-if="order.discountAmount > 0" class="flex justify-between text-red-600">
                <span>Discount</span>
                <span>-{{ formatCurrency(order.discountAmount) }}</span>
              </div>
              <div v-if="order.taxAmount > 0" class="flex justify-between">
                <span class="text-gray-500">Tax</span>
                <span>{{ formatCurrency(order.taxAmount) }}</span>
              </div>
              <div v-if="order.shippingAmount > 0" class="flex justify-between">
                <span class="text-gray-500">Shipping</span>
                <span>{{ formatCurrency(order.shippingAmount) }}</span>
              </div>
              <div class="flex justify-between pt-2 border-t font-bold text-base">
                <span>Total</span>
                <span>{{ formatCurrency(order.totalAmount) }} {{ order.currency }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div v-if="order.notes || order.internalNotes" class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Notes</h3>
        </div>
        <div class="card-body space-y-3">
          <div v-if="order.notes">
            <p class="text-sm text-gray-500">Notes</p>
            <p class="mt-1">{{ order.notes }}</p>
          </div>
          <div v-if="order.internalNotes">
            <p class="text-sm text-gray-500">Internal Notes</p>
            <p class="mt-1">{{ order.internalNotes }}</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
