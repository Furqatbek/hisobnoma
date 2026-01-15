<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { purchaseOrdersApi } from '@/services/api'
import { PlusIcon, EyeIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'

const orders = ref([])
const loading = ref(true)

async function fetchOrders() {
  loading.value = true
  try {
    const response = await purchaseOrdersApi.getAll({ size: 50 })
    orders.value = response.data.content || []
  } catch (error) {
    console.error('Failed to fetch orders:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchOrders)

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
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Purchase Orders</h1>
        <p class="mt-1 text-sm text-gray-500">Manage purchase orders</p>
      </div>
      <RouterLink to="/purchases/orders/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        New Order
      </RouterLink>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="orders.length === 0" class="text-center py-12">
        <p class="text-gray-500">No purchase orders yet</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Order #</th>
              <th>Supplier</th>
              <th>Date</th>
              <th>Items</th>
              <th class="text-right">Total</th>
              <th>Status</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="order in orders" :key="order.id">
              <td class="font-mono">{{ order.orderNumber || `PO-${order.id}` }}</td>
              <td>{{ order.supplier?.name || '-' }}</td>
              <td>{{ formatDate(order.orderDate || order.createdAt) }}</td>
              <td>{{ order.items?.length || 0 }} items</td>
              <td class="text-right font-medium">{{ formatCurrency(order.totalAmount) }}</td>
              <td>
                <span :class="['badge', getStatusClass(order.status)]">{{ order.status }}</span>
              </td>
              <td class="text-right">
                <RouterLink
                  :to="`/purchases/orders/${order.id}`"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                >
                  <EyeIcon class="h-5 w-5" />
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
