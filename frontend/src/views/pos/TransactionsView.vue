<script setup>
import { ref, onMounted } from 'vue'
import { posApi } from '@/services/api'
import { EyeIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'

const transactions = ref([])
const loading = ref(true)
const search = ref('')
const dateFilter = ref('today')

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0
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
    }

    const response = await posApi.getTransactions(params)
    transactions.value = response.data.content || []
    pagination.value.totalPages = response.data.page?.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch transactions:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchTransactions)

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value || 0)
}

function formatDate(dateString) {
  return new Date(dateString).toLocaleString()
}

function getStatusClass(status) {
  switch (status) {
    case 'COMPLETED': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'VOIDED': return 'badge-danger'
    default: return 'badge-info'
  }
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Transactions</h1>
      <p class="mt-1 text-sm text-gray-500">View sales history</p>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            placeholder="Search by transaction ID or customer..."
            class="input pl-10"
          />
        </div>
        <select v-model="dateFilter" @change="fetchTransactions" class="input w-auto">
          <option value="today">Today</option>
          <option value="week">This Week</option>
          <option value="month">This Month</option>
          <option value="all">All Time</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="transactions.length === 0" class="text-center py-12">
        <p class="text-gray-500">No transactions found</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Transaction ID</th>
              <th>Date</th>
              <th>Customer</th>
              <th>Items</th>
              <th class="text-right">Total</th>
              <th>Status</th>
              <th>Payment</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="tx in transactions" :key="tx.id">
              <td class="font-mono text-sm">{{ tx.transactionNumber || `#${tx.id}` }}</td>
              <td class="text-sm text-gray-500">{{ formatDate(tx.createdAt) }}</td>
              <td>{{ tx.customer?.name || 'Walk-in' }}</td>
              <td>{{ tx.items?.length || 0 }} items</td>
              <td class="text-right font-medium">{{ formatCurrency(tx.totalAmount) }}</td>
              <td>
                <span :class="['badge', getStatusClass(tx.status)]">
                  {{ tx.status }}
                </span>
              </td>
              <td class="text-sm">{{ tx.payments?.[0]?.paymentType || '-' }}</td>
              <td class="text-right">
                <button class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
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
            Previous
          </button>
          <span class="text-sm text-gray-500">
            Page {{ pagination.page + 1 }} of {{ pagination.totalPages }}
          </span>
          <button
            @click="pagination.page++; fetchTransactions()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
