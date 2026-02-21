<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { expensesApi } from '@/services/api'
import {
  ArrowLeftIcon,
  PencilSquareIcon,
  CheckIcon,
  XMarkIcon,
  PauseIcon,
  PlayIcon,
  BanknotesIcon
} from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()
const expense = ref(null)
const loading = ref(true)
const actionLoading = ref(false)

async function fetchExpense() {
  loading.value = true
  try {
    const response = await expensesApi.getById(route.params.id)
    expense.value = response.data.data || response.data
  } catch (error) {
    console.error('Xarajatni yuklashda xatolik:', error)
    alert('Xarajatni yuklashda xatolik yuz berdi')
    router.push('/finance/expenses')
  } finally {
    loading.value = false
  }
}

async function submitForApproval() {
  actionLoading.value = true
  try {
    await expensesApi.submit(expense.value.id)
    await fetchExpense()
  } catch (error) {
    alert('Xatolik yuz berdi')
  } finally {
    actionLoading.value = false
  }
}

async function approveExpense() {
  actionLoading.value = true
  try {
    await expensesApi.approve(expense.value.id)
    await fetchExpense()
  } catch (error) {
    alert('Xatolik yuz berdi')
  } finally {
    actionLoading.value = false
  }
}

async function rejectExpense() {
  const reason = prompt('Rad etish sababini kiriting:')
  if (!reason) return

  actionLoading.value = true
  try {
    await expensesApi.reject(expense.value.id, reason)
    await fetchExpense()
  } catch (error) {
    alert('Xatolik yuz berdi')
  } finally {
    actionLoading.value = false
  }
}

async function holdExpense() {
  actionLoading.value = true
  try {
    await expensesApi.hold(expense.value.id)
    await fetchExpense()
  } catch (error) {
    alert('Xatolik yuz berdi')
  } finally {
    actionLoading.value = false
  }
}

async function releaseHold() {
  actionLoading.value = true
  try {
    await expensesApi.releaseHold(expense.value.id)
    await fetchExpense()
  } catch (error) {
    alert('Xatolik yuz berdi')
  } finally {
    actionLoading.value = false
  }
}

async function cancelExpense() {
  const reason = prompt('Bekor qilish sababini kiriting:')
  if (!reason) return

  actionLoading.value = true
  try {
    await expensesApi.cancel(expense.value.id, reason)
    await fetchExpense()
  } catch (error) {
    alert('Xatolik yuz berdi')
  } finally {
    actionLoading.value = false
  }
}

onMounted(fetchExpense)

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(value) || 0)
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

function canEdit() {
  return expense.value?.status === 'DRAFT' || expense.value?.status === 'REJECTED'
}

function canSubmit() {
  return expense.value?.status === 'DRAFT'
}

function canApprove() {
  return expense.value?.status === 'PENDING_APPROVAL'
}

function canHold() {
  return ['APPROVED', 'PENDING_APPROVAL'].includes(expense.value?.status)
}

function canReleaseHold() {
  return expense.value?.status === 'ON_HOLD'
}

function canCancel() {
  return ['DRAFT', 'PENDING_APPROVAL', 'ON_HOLD'].includes(expense.value?.status)
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
            Xarajat {{ expense?.invoiceNumber || `#${route.params.id}` }}
          </h1>
          <p v-if="expense" class="text-sm text-gray-500">
            {{ expense.vendorName || '-' }} • {{ formatDate(expense.invoiceDate) }}
          </p>
        </div>
      </div>

      <div v-if="expense" class="flex items-center space-x-2">
        <RouterLink
          v-if="canEdit()"
          :to="`/finance/expenses/${expense.id}/edit`"
          class="btn-secondary"
        >
          <PencilSquareIcon class="h-5 w-5 mr-2" />
          Tahrirlash
        </RouterLink>

        <button
          v-if="canSubmit()"
          @click="submitForApproval"
          :disabled="actionLoading"
          class="btn-primary"
        >
          Tasdiqlashga yuborish
        </button>

        <button
          v-if="canApprove()"
          @click="approveExpense"
          :disabled="actionLoading"
          class="btn-success"
        >
          <CheckIcon class="h-5 w-5 mr-2" />
          Tasdiqlash
        </button>

        <button
          v-if="canApprove()"
          @click="rejectExpense"
          :disabled="actionLoading"
          class="btn-danger"
        >
          <XMarkIcon class="h-5 w-5 mr-2" />
          Rad etish
        </button>

        <button
          v-if="canHold()"
          @click="holdExpense"
          :disabled="actionLoading"
          class="btn-warning"
        >
          <PauseIcon class="h-5 w-5 mr-2" />
          Kutishga olish
        </button>

        <button
          v-if="canReleaseHold()"
          @click="releaseHold"
          :disabled="actionLoading"
          class="btn-primary"
        >
          <PlayIcon class="h-5 w-5 mr-2" />
          Davom ettirish
        </button>

        <button
          v-if="canCancel()"
          @click="cancelExpense"
          :disabled="actionLoading"
          class="btn-danger"
        >
          Bekor qilish
        </button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="expense">
      <!-- Status & Summary Cards -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Holat</p>
            <span :class="['badge mt-2', getStatusClass(expense.status)]">
              {{ getStatusLabel(expense.status) }}
            </span>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Jami summa</p>
            <p class="text-xl font-bold text-gray-900">{{ formatCurrency(expense.totalAmount) }} so'm</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">To'langan</p>
            <p class="text-xl font-bold text-green-600">{{ formatCurrency(expense.paidAmount) }} so'm</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">Qoldiq</p>
            <p :class="['text-xl font-bold', Number(expense.balanceDue) > 0 ? 'text-red-600' : 'text-green-600']">
              {{ formatCurrency(expense.balanceDue) }} so'm
            </p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Details -->
        <div class="lg:col-span-2 space-y-6">
          <!-- Basic Info -->
          <div class="card">
            <div class="card-header">
              <h3 class="text-lg font-medium">Ma'lumotlar</h3>
            </div>
            <div class="card-body">
              <dl class="grid grid-cols-2 gap-4">
                <div>
                  <dt class="text-sm text-gray-500">{{ expense.vendorId ? 'Yetkazib beruvchi' : 'Xarajat nomi' }}</dt>
                  <dd class="font-medium">{{ expense.vendorName || '-' }}</dd>
                </div>
                <div>
                  <dt class="text-sm text-gray-500">Yetkazib beruvchi hisob-faktura №</dt>
                  <dd class="font-medium">{{ expense.vendorInvoiceNumber || '-' }}</dd>
                </div>
                <div>
                  <dt class="text-sm text-gray-500">Sana</dt>
                  <dd class="font-medium">{{ formatDate(expense.invoiceDate) }}</dd>
                </div>
                <div>
                  <dt class="text-sm text-gray-500">To'lov muddati</dt>
                  <dd class="font-medium">{{ formatDate(expense.dueDate) }}</dd>
                </div>
                <div v-if="expense.description" class="col-span-2">
                  <dt class="text-sm text-gray-500">Tavsif</dt>
                  <dd class="font-medium">{{ expense.description }}</dd>
                </div>
              </dl>
            </div>
          </div>

          <!-- Line Items -->
          <div class="card">
            <div class="card-header">
              <h3 class="text-lg font-medium">Xarajat qatorlari</h3>
            </div>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Tavsif</th>
                    <th class="text-right">Miqdor</th>
                    <th class="text-right">Narx</th>
                    <th class="text-right">Soliq %</th>
                    <th class="text-right">Jami</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="line in expense.lines" :key="line.id">
                    <td>
                      <div class="font-medium">{{ line.description || line.productName }}</div>
                      <div v-if="line.productName && line.description" class="text-sm text-gray-500">{{ line.productName }}</div>
                    </td>
                    <td class="text-right">{{ line.quantity }} {{ line.unitOfMeasure }}</td>
                    <td class="text-right">{{ formatCurrency(line.unitPrice) }}</td>
                    <td class="text-right">{{ line.taxRate || 0 }}%</td>
                    <td class="text-right font-medium">{{ formatCurrency(line.lineTotal) }} so'm</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <!-- Totals -->
          <div class="card">
            <div class="card-header">
              <h3 class="text-lg font-medium">Jami</h3>
            </div>
            <div class="card-body space-y-3">
              <div class="flex justify-between">
                <span class="text-gray-500">Oraliq jami:</span>
                <span>{{ formatCurrency(expense.subtotal) }} so'm</span>
              </div>
              <div v-if="Number(expense.taxAmount) > 0" class="flex justify-between">
                <span class="text-gray-500">Soliq:</span>
                <span>{{ formatCurrency(expense.taxAmount) }} so'm</span>
              </div>
              <div v-if="Number(expense.shippingAmount) > 0" class="flex justify-between">
                <span class="text-gray-500">Yetkazish:</span>
                <span>{{ formatCurrency(expense.shippingAmount) }} so'm</span>
              </div>
              <div v-if="Number(expense.discountAmount) > 0" class="flex justify-between text-green-600">
                <span>Chegirma:</span>
                <span>-{{ formatCurrency(expense.discountAmount) }} so'm</span>
              </div>
              <div class="flex justify-between text-lg font-bold border-t pt-3">
                <span>Jami:</span>
                <span>{{ formatCurrency(expense.totalAmount) }} so'm</span>
              </div>
            </div>
          </div>

          <!-- Notes -->
          <div v-if="expense.notes" class="card">
            <div class="card-header">
              <h3 class="text-lg font-medium">Izohlar</h3>
            </div>
            <div class="card-body">
              <p class="text-gray-600">{{ expense.notes }}</p>
            </div>
          </div>

          <!-- Rejection Reason -->
          <div v-if="expense.rejectionReason" class="card border-red-200 bg-red-50">
            <div class="card-header">
              <h3 class="text-lg font-medium text-red-800">Rad etish sababi</h3>
            </div>
            <div class="card-body">
              <p class="text-red-700">{{ expense.rejectionReason }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.btn-success {
  @apply inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed;
}
.btn-warning {
  @apply inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg shadow-sm text-white bg-yellow-600 hover:bg-yellow-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed;
}
.btn-danger {
  @apply inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg shadow-sm text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed;
}
</style>
