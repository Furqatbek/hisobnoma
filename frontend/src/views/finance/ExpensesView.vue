<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { expensesApi, suppliersApi } from '@/services/api'
import {
  PlusIcon,
  EyeIcon,
  MagnifyingGlassIcon,
  BanknotesIcon,
  CheckCircleIcon,
  ClockIcon,
  ExclamationTriangleIcon,
  XCircleIcon
} from '@heroicons/vue/24/outline'

const expenses = ref([])
const vendors = ref([])
const loading = ref(true)
const search = ref('')
const statusFilter = ref('all')
const vendorFilter = ref('')

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

// Summary stats
const totalPayable = ref(0)
const overdueBalance = ref(0)

async function fetchExpenses() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }

    let response
    if (statusFilter.value !== 'all') {
      response = await expensesApi.getByStatus(statusFilter.value, params)
    } else if (vendorFilter.value) {
      response = await expensesApi.getByVendor(vendorFilter.value, params)
    } else {
      response = await expensesApi.getAll(params)
    }

    const data = response.data.data || response.data
    expenses.value = data.content || data || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 1
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || expenses.value.length
  } catch (error) {
    console.error('Xarajatlarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

async function fetchVendors() {
  try {
    const response = await suppliersApi.getAll({ size: 100 })
    const data = response.data.data || response.data
    vendors.value = data.content || data || []
  } catch (error) {
    console.error('Yetkazib beruvchilarni yuklashda xatolik:', error)
  }
}

async function fetchSummary() {
  try {
    const [payableRes, overdueRes] = await Promise.all([
      expensesApi.getTotalPayable(),
      expensesApi.getOverdueBalance()
    ])
    totalPayable.value = payableRes.data.data || payableRes.data || 0
    overdueBalance.value = overdueRes.data.data || overdueRes.data || 0
  } catch (error) {
    console.error('Jami ma\'lumotlarni yuklashda xatolik:', error)
  }
}

onMounted(() => {
  fetchExpenses()
  fetchVendors()
  fetchSummary()
})

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('uz-UZ')
}

function getStatusClass(status) {
  const classes = {
    'DRAFT': 'badge-info',
    'PENDING_APPROVAL': 'badge-warning',
    'APPROVED': 'badge-success',
    'PARTIALLY_PAID': 'badge-info',
    'PAID': 'badge-success',
    'ON_HOLD': 'badge-warning',
    'CANCELLED': 'badge-danger',
    'REJECTED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getStatusLabel(status) {
  const labels = {
    'DRAFT': 'Qoralama',
    'PENDING_APPROVAL': 'Tasdiqlanmoqda',
    'APPROVED': 'Tasdiqlangan',
    'PARTIALLY_PAID': 'Qisman to\'langan',
    'PAID': 'To\'langan',
    'ON_HOLD': 'Kutish',
    'CANCELLED': 'Bekor qilingan',
    'REJECTED': 'Rad etilgan'
  }
  return labels[status] || status
}

function getStatusIcon(status) {
  switch (status) {
    case 'APPROVED':
    case 'PAID':
      return CheckCircleIcon
    case 'PENDING_APPROVAL':
    case 'ON_HOLD':
      return ClockIcon
    case 'CANCELLED':
    case 'REJECTED':
      return XCircleIcon
    default:
      return BanknotesIcon
  }
}

function handleFilter() {
  pagination.value.page = 0
  fetchExpenses()
}

function isOverdue(expense) {
  if (!expense.dueDate || expense.status === 'PAID' || expense.status === 'CANCELLED') {
    return false
  }
  return new Date(expense.dueDate) < new Date()
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Xarajatlar</h1>
        <p class="mt-1 text-sm text-gray-500">Yetkazib beruvchi hisob-fakturalari va xarajatlar</p>
      </div>
      <RouterLink to="/finance/expenses/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        Yangi xarajat
      </RouterLink>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="card">
        <div class="card-body">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">Jami to'lanishi kerak</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(totalPayable) }} so'm</p>
            </div>
            <div class="p-3 bg-primary-100 rounded-full">
              <BanknotesIcon class="h-6 w-6 text-primary-600" />
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-body">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">Muddati o'tgan</p>
              <p class="text-2xl font-bold text-red-600">{{ formatCurrency(overdueBalance) }} so'm</p>
            </div>
            <div class="p-3 bg-red-100 rounded-full">
              <ExclamationTriangleIcon class="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-body">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-gray-500">Jami hisob-fakturalar</p>
              <p class="text-2xl font-bold text-gray-900">{{ pagination.totalElements }}</p>
            </div>
            <div class="p-3 bg-gray-100 rounded-full">
              <ClockIcon class="h-6 w-6 text-gray-600" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            placeholder="Qidiruv..."
            class="input pl-10"
            @keyup.enter="handleFilter"
          />
        </div>
        <select v-model="vendorFilter" @change="handleFilter" class="input w-auto">
          <option value="">Barcha yetkazib beruvchilar</option>
          <option v-for="vendor in vendors" :key="vendor.id" :value="vendor.id">
            {{ vendor.name }}
          </option>
        </select>
        <select v-model="statusFilter" @change="handleFilter" class="input w-auto">
          <option value="all">Barcha holatlar</option>
          <option value="DRAFT">Qoralama</option>
          <option value="PENDING_APPROVAL">Tasdiqlanmoqda</option>
          <option value="APPROVED">Tasdiqlangan</option>
          <option value="PARTIALLY_PAID">Qisman to'langan</option>
          <option value="PAID">To'langan</option>
          <option value="ON_HOLD">Kutish</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="expenses.length === 0" class="text-center py-12">
        <BanknotesIcon class="h-12 w-12 mx-auto text-gray-400 mb-4" />
        <p class="text-gray-500 mb-4">Xarajatlar topilmadi</p>
        <RouterLink to="/finance/expenses/new" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          Birinchi xarajatni qo'shish
        </RouterLink>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Hisob-faktura №</th>
              <th>Yetkazib beruvchi</th>
              <th>Sana</th>
              <th>Muddat</th>
              <th class="text-right">Summa</th>
              <th class="text-right">Qoldiq</th>
              <th>Holat</th>
              <th class="text-right">Amallar</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="expense in expenses" :key="expense.id" :class="{ 'bg-red-50': isOverdue(expense) }">
              <td class="font-mono text-sm">
                {{ expense.invoiceNumber || `#${expense.id}` }}
                <div v-if="expense.vendorInvoiceNumber" class="text-xs text-gray-500">
                  {{ expense.vendorInvoiceNumber }}
                </div>
              </td>
              <td>{{ expense.vendor?.name || '-' }}</td>
              <td class="text-sm text-gray-500">{{ formatDate(expense.invoiceDate) }}</td>
              <td :class="{ 'text-red-600 font-medium': isOverdue(expense) }">
                {{ formatDate(expense.dueDate) }}
                <span v-if="isOverdue(expense)" class="text-xs">(muddati o'tgan)</span>
              </td>
              <td class="text-right font-medium">{{ formatCurrency(expense.totalAmount) }} so'm</td>
              <td class="text-right">
                <span :class="expense.balanceDue > 0 ? 'text-red-600 font-medium' : 'text-green-600'">
                  {{ formatCurrency(expense.balanceDue) }} so'm
                </span>
              </td>
              <td>
                <span :class="['badge', getStatusClass(expense.status)]">
                  {{ getStatusLabel(expense.status) }}
                </span>
              </td>
              <td class="text-right">
                <RouterLink
                  :to="`/finance/expenses/${expense.id}`"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Ko'rish"
                >
                  <EyeIcon class="h-5 w-5" />
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
            @click="pagination.page--; fetchExpenses()"
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
            @click="pagination.page++; fetchExpenses()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            Keyingi
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
