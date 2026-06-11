<script setup>
import { useToastStore } from '@/stores/toast'
import { formatPrice } from '@/utils/format'
import { ref, onMounted } from 'vue'
import { webOrdersApi } from '@/services/api'
import {
  InboxArrowDownIcon,
  CheckCircleIcon,
  TruckIcon,
  XCircleIcon,
  DocumentTextIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

const orders = ref([])
const loading = ref(false)
const statusFilter = ref('')
const currentPage = ref(0)
const totalPages = ref(0)

const selectedOrder = ref(null)
const cancelReason = ref('')
const showCancelInput = ref(false)
const actionBusy = ref(false)

const STATUSES = ['NEW', 'CONFIRMED', 'DELIVERING', 'COMPLETED', 'CANCELLED']

const statusClasses = {
  NEW: 'bg-red-100 text-red-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  DELIVERING: 'bg-yellow-100 text-yellow-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-100 text-gray-600'
}

async function fetchOrders(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 20 }
    if (statusFilter.value) params.status = statusFilter.value
    const response = await webOrdersApi.getAll(params)
    const data = response.data
    orders.value = data.content || []
    currentPage.value = data.page?.number || 0
    totalPages.value = data.page?.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch web orders:', error)
  } finally {
    loading.value = false
  }
}

function setFilter(status) {
  statusFilter.value = status
  fetchOrders(0)
}

function openOrder(order) {
  selectedOrder.value = order
  showCancelInput.value = false
  cancelReason.value = ''
}

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('uz-UZ', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

async function updateStatus(order, status, reason = null) {
  actionBusy.value = true
  try {
    const response = await webOrdersApi.updateStatus(order.id, status, reason)
    selectedOrder.value = response.data.data
    fetchOrders(currentPage.value)
  } catch (error) {
    toast.error(error.response?.data?.message || t('webOrders.actionError'))
  } finally {
    actionBusy.value = false
  }
}

function submitCancel(order) {
  if (!cancelReason.value.trim()) return
  updateStatus(order, 'CANCELLED', cancelReason.value.trim())
  showCancelInput.value = false
}

async function convertToInvoice(order) {
  actionBusy.value = true
  try {
    const response = await webOrdersApi.convertToInvoice(order.id)
    selectedOrder.value = response.data.data
    fetchOrders(currentPage.value)
  } catch (error) {
    toast.error(error.response?.data?.message || t('webOrders.actionError'))
  } finally {
    actionBusy.value = false
  }
}

onMounted(() => fetchOrders())
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <InboxArrowDownIcon class="h-7 w-7 text-teal-600" />
          {{ $t('webOrders.title') }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('webOrders.subtitle') }}</p>
      </div>
      <button class="btn-secondary text-sm" @click="fetchOrders(currentPage)">
        <ArrowPathIcon class="h-4 w-4 mr-1 inline" />
        {{ $t('refresh') || 'Янгилаш' }}
      </button>
    </div>

    <!-- Status filter -->
    <div class="flex flex-wrap gap-2">
      <button
        :class="statusFilter === '' ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        class="px-3 py-1.5 rounded-full text-sm font-medium transition-colors"
        @click="setFilter('')"
      >
        {{ $t('webOrders.all') }}
      </button>
      <button
        v-for="status in STATUSES"
        :key="status"
        :class="statusFilter === status ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        class="px-3 py-1.5 rounded-full text-sm font-medium transition-colors"
        @click="setFilter(status)"
      >
        {{ $t('webOrders.status' + status) }}
      </button>
    </div>

    <!-- Orders table -->
    <div class="card">
      <div v-if="loading" class="p-8 text-center text-gray-500">{{ $t('loading') }}</div>
      <div v-else-if="orders.length === 0" class="p-8 text-center text-gray-500">
        {{ $t('webOrders.empty') }}
      </div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webOrders.orderNumber') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webOrders.customer') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webOrders.delivery') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('webOrders.total') }}</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">{{ $t('webOrders.status') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webOrders.date') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200 bg-white">
            <tr
              v-for="order in orders"
              :key="order.id"
              class="cursor-pointer hover:bg-gray-50"
              :class="{ 'font-semibold': order.status === 'NEW' }"
              @click="openOrder(order)"
            >
              <td class="px-4 py-3 whitespace-nowrap text-sm text-primary-700">{{ order.orderNumber }}</td>
              <td class="px-4 py-3 text-sm">
                <div class="text-gray-900">{{ order.customerName }}</div>
                <div class="text-xs text-gray-500">{{ order.phone }}</div>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600 whitespace-nowrap">
                {{ [order.deliveryRegionName, order.deliveryVillageName].filter(Boolean).join(', ') || '—' }}
              </td>
              <td class="px-4 py-3 text-sm text-right whitespace-nowrap">{{ formatPrice(order.totalAmount) }}</td>
              <td class="px-4 py-3 text-center whitespace-nowrap">
                <span :class="statusClasses[order.status]" class="inline-flex px-2.5 py-1 rounded-full text-xs font-medium">
                  {{ $t('webOrders.status' + order.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-500 whitespace-nowrap">{{ formatDate(order.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
        <button class="btn-secondary text-sm py-1 px-3" :disabled="currentPage === 0" @click="fetchOrders(currentPage - 1)">
          {{ $t('previous') }}
        </button>
        <span class="text-sm text-gray-500">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="btn-secondary text-sm py-1 px-3" :disabled="currentPage >= totalPages - 1" @click="fetchOrders(currentPage + 1)">
          {{ $t('next') }}
        </button>
      </div>
    </div>

    <!-- Detail modal -->
    <div v-if="selectedOrder" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="selectedOrder = null"></div>
      <div class="relative bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[85vh] flex flex-col">
        <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-gray-900">
              {{ selectedOrder.orderNumber }}
            </h3>
            <span :class="statusClasses[selectedOrder.status]" class="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium mt-1">
              {{ $t('webOrders.status' + selectedOrder.status) }}
            </span>
          </div>
          <button class="text-gray-400 hover:text-gray-600 text-2xl leading-none" @click="selectedOrder = null">&times;</button>
        </div>

        <div class="px-6 py-4 flex-1 overflow-y-auto space-y-4">
          <!-- Customer info -->
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <div class="text-gray-500">{{ $t('webOrders.customer') }}</div>
              <div class="font-medium">{{ selectedOrder.customerName }}</div>
            </div>
            <div>
              <div class="text-gray-500">{{ $t('webOrders.phone') }}</div>
              <a :href="'tel:' + selectedOrder.phone" class="font-medium text-primary-700">{{ selectedOrder.phone }}</a>
            </div>
            <div>
              <div class="text-gray-500">{{ $t('webOrders.delivery') }}</div>
              <div class="font-medium">
                {{ [selectedOrder.deliveryRegionName, selectedOrder.deliveryVillageName].filter(Boolean).join(', ') || '—' }}
              </div>
            </div>
            <div>
              <div class="text-gray-500">{{ $t('webOrders.date') }}</div>
              <div class="font-medium">{{ formatDate(selectedOrder.createdAt) }}</div>
            </div>
          </div>

          <div v-if="selectedOrder.customerNote" class="text-sm bg-yellow-50 border border-yellow-200 rounded p-3">
            <span class="text-gray-500">{{ $t('webOrders.note') }}:</span> {{ selectedOrder.customerNote }}
          </div>

          <div v-if="selectedOrder.cancellationReason" class="text-sm bg-red-50 border border-red-200 rounded p-3">
            <span class="text-gray-500">{{ $t('webOrders.cancelReason') }}:</span> {{ selectedOrder.cancellationReason }}
          </div>

          <!-- Lines -->
          <div>
            <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('webOrders.lines') }}</h4>
            <table class="min-w-full text-sm">
              <thead>
                <tr class="text-xs text-gray-500 uppercase border-b">
                  <th class="py-2 text-left">{{ $t('webOrders.product') }}</th>
                  <th class="py-2 text-right">{{ $t('webOrders.quantity') }}</th>
                  <th class="py-2 text-right">{{ $t('webOrders.price') }}</th>
                  <th class="py-2 text-right">{{ $t('webOrders.lineTotal') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="line in selectedOrder.lines" :key="line.id">
                  <td class="py-2">{{ line.productName }}</td>
                  <td class="py-2 text-right">{{ line.quantity }} {{ line.unitName || '' }}</td>
                  <td class="py-2 text-right">{{ formatPrice(line.unitPrice) }}</td>
                  <td class="py-2 text-right">{{ formatPrice(line.lineTotal) }}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr v-if="selectedOrder.discountTotal > 0" class="border-t text-green-700">
                  <td colspan="3" class="py-2 text-right">
                    {{ $t('webOrders.discount') }}
                    <span v-if="selectedOrder.appliedPromotions" class="text-xs text-gray-500">
                      ({{ selectedOrder.appliedPromotions }})
                    </span>:
                  </td>
                  <td class="py-2 text-right">−{{ formatPrice(selectedOrder.discountTotal) }}</td>
                </tr>
                <tr v-if="selectedOrder.couponDiscount > 0" class="border-t text-green-700">
                  <td colspan="3" class="py-2 text-right">
                    {{ $t('webOrders.coupon') }}
                    <span v-if="selectedOrder.couponCode" class="font-mono text-xs text-gray-500">
                      ({{ selectedOrder.couponCode }})
                    </span>:
                  </td>
                  <td class="py-2 text-right">−{{ formatPrice(selectedOrder.couponDiscount) }}</td>
                </tr>
                <tr v-if="selectedOrder.pointsSpent > 0" class="border-t text-purple-700">
                  <td colspan="3" class="py-2 text-right">{{ $t('webOrders.pointsSpent') }}:</td>
                  <td class="py-2 text-right">−{{ formatPrice(selectedOrder.pointsSpent) }}</td>
                </tr>
                <tr v-if="selectedOrder.deliveryFee > 0" class="border-t">
                  <td colspan="3" class="py-2 text-right">{{ $t('webOrders.deliveryFee') }}:</td>
                  <td class="py-2 text-right">{{ formatPrice(selectedOrder.deliveryFee) }}</td>
                </tr>
                <tr class="border-t font-semibold">
                  <td colspan="3" class="py-2 text-right">{{ $t('webOrders.total') }}:</td>
                  <td class="py-2 text-right">{{ formatPrice(selectedOrder.totalAmount) }}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          <!-- Invoice link -->
          <div v-if="selectedOrder.arInvoiceId" class="text-sm bg-green-50 border border-green-200 rounded p-3">
            {{ $t('webOrders.invoiceCreated') }}:
            <router-link
              :to="`/finance/invoices`"
              class="font-medium text-primary-700 underline"
            >
              {{ selectedOrder.arInvoiceNumber }}
            </router-link>
          </div>

          <!-- Cancel reason input -->
          <div v-if="showCancelInput" class="space-y-2">
            <label class="text-sm text-gray-700">{{ $t('webOrders.cancelReason') }}</label>
            <input
              v-model="cancelReason"
              type="text"
              :placeholder="$t('webOrders.cancelReasonPlaceholder')"
              class="input"
            />
            <div class="flex gap-2">
              <button
                class="btn-primary text-sm bg-red-600 hover:bg-red-700"
                :disabled="!cancelReason.trim() || actionBusy"
                @click="submitCancel(selectedOrder)"
              >
                {{ $t('webOrders.cancel') }}
              </button>
              <button class="btn-secondary text-sm" @click="showCancelInput = false">{{ $t('cancel') }}</button>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="px-6 py-4 border-t border-gray-200 flex flex-wrap justify-end gap-2">
          <button
            v-if="selectedOrder.status === 'NEW'"
            class="btn-primary text-sm"
            :disabled="actionBusy"
            @click="updateStatus(selectedOrder, 'CONFIRMED')"
          >
            <CheckCircleIcon class="h-4 w-4 mr-1 inline" />
            {{ $t('webOrders.confirm') }}
          </button>
          <button
            v-if="selectedOrder.status === 'CONFIRMED'"
            class="btn-secondary text-sm"
            :disabled="actionBusy"
            @click="updateStatus(selectedOrder, 'DELIVERING')"
          >
            <TruckIcon class="h-4 w-4 mr-1 inline" />
            {{ $t('webOrders.startDelivery') }}
          </button>
          <button
            v-if="['CONFIRMED', 'DELIVERING'].includes(selectedOrder.status)"
            class="btn-secondary text-sm"
            :disabled="actionBusy"
            @click="updateStatus(selectedOrder, 'COMPLETED')"
          >
            <CheckCircleIcon class="h-4 w-4 mr-1 inline" />
            {{ $t('webOrders.complete') }}
          </button>
          <button
            v-if="!selectedOrder.arInvoiceId && selectedOrder.status !== 'CANCELLED'"
            class="btn-primary text-sm"
            :disabled="actionBusy"
            @click="convertToInvoice(selectedOrder)"
          >
            <DocumentTextIcon class="h-4 w-4 mr-1 inline" />
            {{ $t('webOrders.convertToInvoice') }}
          </button>
          <button
            v-if="['NEW', 'CONFIRMED', 'DELIVERING'].includes(selectedOrder.status) && !showCancelInput"
            class="btn-secondary text-sm text-red-600"
            :disabled="actionBusy"
            @click="showCancelInput = true"
          >
            <XCircleIcon class="h-4 w-4 mr-1 inline" />
            {{ $t('webOrders.cancel') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
