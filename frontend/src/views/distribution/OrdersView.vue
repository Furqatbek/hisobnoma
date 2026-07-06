<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, onMounted } from 'vue'
import { distributionOrdersApi, unwrapPage } from '@/services/api'
import { PlusIcon, MagnifyingGlassIcon, ClipboardDocumentListIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()

const orders = ref([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

const STATUSES = ['DRAFT', 'CONFIRMED', 'PICKING', 'LOADED', 'IN_TRANSIT', 'DELIVERED', 'INVOICED', 'CANCELLED']

const statusClass = {
  DRAFT: 'bg-gray-100 text-gray-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  PICKING: 'bg-indigo-100 text-indigo-800',
  LOADED: 'bg-violet-100 text-violet-800',
  IN_TRANSIT: 'bg-amber-100 text-amber-800',
  DELIVERED: 'bg-teal-100 text-teal-800',
  INVOICED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800'
}

function formatMoney(v) {
  return new Intl.NumberFormat('uz-UZ').format(v || 0)
}

async function fetchOrders(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 20 }
    if (statusFilter.value) params.status = statusFilter.value
    const response = searchQuery.value.trim()
      ? await distributionOrdersApi.search(searchQuery.value.trim(), params)
      : await distributionOrdersApi.getAll(params)
    const { content, page: meta } = unwrapPage(response)
    orders.value = content
    currentPage.value = meta.number || 0
    totalPages.value = meta.totalPages || 0
    totalElements.value = meta.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch orders:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchOrders(0)
}

onMounted(() => fetchOrders())
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('distribution.orders.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('distribution.orders.subtitle') }}</p>
      </div>
      <router-link to="/distribution/orders/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('distribution.orders.newOrder') }}
      </router-link>
    </div>

    <div class="card mb-6">
      <div class="card-body flex flex-col md:flex-row gap-4">
        <div class="relative flex-1">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="searchQuery"
            @input="handleSearch"
            type="text"
            :placeholder="$t('distribution.orders.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
        <select v-model="statusFilter" @change="fetchOrders(0)" class="input md:w-56">
          <option value="">{{ $t('distribution.orders.allStatuses') }}</option>
          <option v-for="s in STATUSES" :key="s" :value="s">{{ $t('distribution.status.' + s) }}</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.orders.number') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.orders.customer') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.orders.date') }}</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.orders.total') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('status') }}</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">{{ $t('loading') }}</td>
            </tr>
            <tr v-else-if="orders.length === 0">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">
                <ClipboardDocumentListIcon class="h-12 w-12 mx-auto mb-2 text-gray-300" />
                <p>{{ $t('distribution.orders.noOrders') }}</p>
              </td>
            </tr>
            <tr
              v-for="order in orders"
              :key="order.id"
              class="hover:bg-gray-50 cursor-pointer"
              @click="$router.push(`/distribution/orders/${order.id}/edit`)"
            >
              <td class="px-6 py-4 font-medium text-gray-900">{{ order.orderNumber }}</td>
              <td class="px-6 py-4 text-sm text-gray-700">{{ order.customerName }}</td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ order.orderDate }}</td>
              <td class="px-6 py-4 text-right text-sm text-gray-900">{{ formatMoney(order.totalAmount) }} {{ order.currency }}</td>
              <td class="px-6 py-4">
                <span :class="['inline-flex px-2 py-1 text-xs font-semibold rounded-full', statusClass[order.status] || 'bg-gray-100 text-gray-800']">
                  {{ $t('distribution.status.' + order.status) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="px-6 py-3 border-t flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ $t('distribution.orders.totalOrders') }}: {{ totalElements }}</p>
        <div class="flex gap-2">
          <button @click="fetchOrders(currentPage - 1)" :disabled="currentPage === 0" class="btn-secondary text-sm py-1 px-3">
            {{ $t('previous') }}
          </button>
          <button @click="fetchOrders(currentPage + 1)" :disabled="currentPage >= totalPages - 1" class="btn-secondary text-sm py-1 px-3">
            {{ $t('next') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
