<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { purchaseOrdersApi } from '@/services/api'
import { ArrowLeftIcon, CheckIcon, XMarkIcon, TruckIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await purchaseOrdersApi.getById(route.params.id)
    order.value = response.data
  } catch (error) {
    console.error('Failed to load order:', error)
  } finally {
    loading.value = false
  }
})

async function approveOrder() {
  try {
    await purchaseOrdersApi.approve(order.value.id)
    order.value.status = 'APPROVED'
  } catch (error) {
    alert('Failed to approve order')
  }
}

async function cancelOrder() {
  if (!confirm('Cancel this order?')) return
  try {
    await purchaseOrdersApi.cancel(order.value.id)
    order.value.status = 'CANCELLED'
  } catch (error) {
    alert('Failed to cancel order')
  }
}

async function receiveOrder() {
  try {
    await purchaseOrdersApi.receive(order.value.id, {})
    order.value.status = 'RECEIVED'
  } catch (error) {
    alert('Failed to receive order')
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}

function formatDate(date) {
  return new Date(date).toLocaleDateString()
}

function getStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'PENDING': 'badge-warning',
    'APPROVED': 'badge-success',
    'RECEIVED': 'badge-success',
    'CANCELLED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
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
            Purchase Order {{ order?.orderNumber || `#${route.params.id}` }}
          </h1>
          <p v-if="order" class="text-sm text-gray-500">Created {{ formatDate(order.createdAt) }}</p>
        </div>
      </div>

      <div v-if="order" class="flex space-x-3">
        <button
          v-if="order.status === 'PENDING'"
          @click="approveOrder"
          class="btn-success"
        >
          <CheckIcon class="h-5 w-5 mr-2" />
          Approve
        </button>
        <button
          v-if="order.status === 'APPROVED'"
          @click="receiveOrder"
          class="btn-primary"
        >
          <TruckIcon class="h-5 w-5 mr-2" />
          Mark Received
        </button>
        <button
          v-if="['DRAFT', 'PENDING'].includes(order.status)"
          @click="cancelOrder"
          class="btn-danger"
        >
          <XMarkIcon class="h-5 w-5 mr-2" />
          Cancel
        </button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="order">
      <!-- Order Info -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Status</p>
            <span :class="['badge mt-1', getStatusClass(order.status)]">{{ order.status }}</span>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Supplier</p>
            <p class="font-medium mt-1">{{ order.supplier?.name || '-' }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Total Amount</p>
            <p class="text-2xl font-bold text-primary-600 mt-1">{{ formatCurrency(order.totalAmount) }}</p>
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
                <th>Product</th>
                <th class="text-right">Quantity</th>
                <th class="text-right">Unit Price</th>
                <th class="text-right">Subtotal</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="item in order.items" :key="item.id">
                <td>
                  <div class="font-medium">{{ item.product?.name || item.productName }}</div>
                  <div class="text-sm text-gray-500">{{ item.product?.sku || item.sku }}</div>
                </td>
                <td class="text-right">{{ item.quantity }}</td>
                <td class="text-right">{{ formatCurrency(item.unitPrice) }}</td>
                <td class="text-right font-medium">{{ formatCurrency(item.quantity * item.unitPrice) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
