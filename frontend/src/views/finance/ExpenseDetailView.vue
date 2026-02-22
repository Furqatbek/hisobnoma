<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { expensesApi, apPaymentsApi } from '@/services/api'
import {
  ArrowLeftIcon,
  PencilSquareIcon,
  CheckIcon,
  XMarkIcon,
  PauseIcon,
  PlayIcon,
  BanknotesIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const expense = ref(null)
const loading = ref(true)
const actionLoading = ref(false)

// Payment modal
const showPaymentModal = ref(false)
const paymentSaving = ref(false)
const paymentForm = ref({
  paymentMethod: 'BANK_TRANSFER',
  paymentAmount: 0,
  paymentDate: new Date().toISOString().split('T')[0],
  referenceNumber: '',
  memo: ''
})

const balanceDue = computed(() => Number(expense.value?.balanceDue) || 0)

async function fetchExpense() {
  loading.value = true
  try {
    const response = await expensesApi.getById(route.params.id)
    expense.value = response.data.data || response.data
  } catch (error) {
    console.error('Xarajatni yuklashda xatolik:', error)
    alert(t('failedToLoad'))
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
    alert(error.response?.data?.message || t('errorOccurred'))
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
    alert(error.response?.data?.message || t('errorOccurred'))
  } finally {
    actionLoading.value = false
  }
}

async function rejectExpense() {
  const reason = prompt(t('finance.expenseDetail.rejectReason') + ':')
  if (!reason) return

  actionLoading.value = true
  try {
    await expensesApi.reject(expense.value.id, reason)
    await fetchExpense()
  } catch (error) {
    alert(error.response?.data?.message || t('errorOccurred'))
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
    alert(error.response?.data?.message || t('errorOccurred'))
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
    alert(error.response?.data?.message || t('errorOccurred'))
  } finally {
    actionLoading.value = false
  }
}

async function cancelExpense() {
  const reason = prompt(t('finance.expenseDetail.cancelReason') + ':')
  if (!reason) return

  actionLoading.value = true
  try {
    await expensesApi.cancel(expense.value.id, reason)
    await fetchExpense()
  } catch (error) {
    alert(error.response?.data?.message || t('errorOccurred'))
  } finally {
    actionLoading.value = false
  }
}

// Payment flow
function openPaymentModal() {
  paymentForm.value = {
    paymentMethod: 'BANK_TRANSFER',
    paymentAmount: balanceDue.value,
    paymentDate: new Date().toISOString().split('T')[0],
    referenceNumber: '',
    memo: ''
  }
  showPaymentModal.value = true
}

async function submitPayment() {
  const amount = parseFloat(paymentForm.value.paymentAmount)
  if (!amount || amount <= 0) {
    alert(t('finance.expenseDetail.paymentAmountRequired'))
    return
  }
  if (amount > balanceDue.value) {
    alert(t('finance.expenseDetail.paymentExceedsBalance'))
    return
  }

  paymentSaving.value = true
  try {
    const paymentData = {
      vendorId: expense.value.vendorId || null,
      paymentDate: paymentForm.value.paymentDate,
      paymentMethod: paymentForm.value.paymentMethod,
      paymentAmount: amount,
      currency: expense.value.currency || 'UZS',
      referenceNumber: paymentForm.value.referenceNumber || null,
      memo: paymentForm.value.memo || null,
      allocations: [{
        apInvoiceId: expense.value.id,
        allocatedAmount: amount
      }]
    }

    const res = await apPaymentsApi.create(paymentData)
    const payment = res.data.data || res.data

    // Auto-submit, approve and process the payment
    await apPaymentsApi.submit(payment.id)
    await apPaymentsApi.approve(payment.id)
    await apPaymentsApi.process(payment.id)

    showPaymentModal.value = false
    await fetchExpense()
  } catch (error) {
    console.error('To\'lov xatosi:', error)
    alert(error.response?.data?.message || t('finance.expenseDetail.paymentError'))
  } finally {
    paymentSaving.value = false
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
    'PARTIAL': 'badge-warning',
    'PARTIALLY_PAID': 'badge-warning',
    'PAID': 'badge-success',
    'ON_HOLD': 'badge-warning',
    'CANCELLED': 'badge-danger',
    'REJECTED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getStatusLabel(status) {
  return t(`enums.apInvoiceStatus.${status}`, status)
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

function canPay() {
  const s = expense.value?.status
  return (s === 'APPROVED' || s === 'PARTIAL' || s === 'PARTIALLY_PAID') && balanceDue.value > 0
}

function canHold() {
  return ['APPROVED', 'PENDING_APPROVAL', 'PARTIAL', 'PARTIALLY_PAID'].includes(expense.value?.status)
}

function canReleaseHold() {
  return expense.value?.status === 'ON_HOLD'
}

function canCancel() {
  return ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ON_HOLD'].includes(expense.value?.status)
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
            {{ $t('finance.expenseDetail.expense') }} {{ expense?.invoiceNumber || `#${route.params.id}` }}
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
          {{ $t('edit') }}
        </RouterLink>

        <button
          v-if="canSubmit()"
          @click="submitForApproval"
          :disabled="actionLoading"
          class="btn-primary"
        >
          {{ $t('finance.expenseDetail.sendForApproval') }}
        </button>

        <button
          v-if="canApprove()"
          @click="approveExpense"
          :disabled="actionLoading"
          class="btn-success"
        >
          <CheckIcon class="h-5 w-5 mr-2" />
          {{ $t('finance.expenseDetail.approve') }}
        </button>

        <button
          v-if="canApprove()"
          @click="rejectExpense"
          :disabled="actionLoading"
          class="btn-danger"
        >
          <XMarkIcon class="h-5 w-5 mr-2" />
          {{ $t('finance.expenseDetail.reject') }}
        </button>

        <button
          v-if="canPay()"
          @click="openPaymentModal"
          :disabled="actionLoading"
          class="btn-pay"
        >
          <BanknotesIcon class="h-5 w-5 mr-2" />
          {{ $t('finance.expenseDetail.makePayment') }}
        </button>

        <button
          v-if="canHold()"
          @click="holdExpense"
          :disabled="actionLoading"
          class="btn-warning"
        >
          <PauseIcon class="h-5 w-5 mr-2" />
          {{ $t('finance.expenseDetail.putOnHold') }}
        </button>

        <button
          v-if="canReleaseHold()"
          @click="releaseHold"
          :disabled="actionLoading"
          class="btn-primary"
        >
          <PlayIcon class="h-5 w-5 mr-2" />
          {{ $t('finance.expenseDetail.continue') }}
        </button>

        <button
          v-if="canCancel()"
          @click="cancelExpense"
          :disabled="actionLoading"
          class="btn-danger"
        >
          {{ $t('cancel') }}
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
            <p class="text-sm text-gray-500">{{ $t('status') }}</p>
            <span :class="['badge mt-2', getStatusClass(expense.status)]">
              {{ getStatusLabel(expense.status) }}
            </span>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('total') }}</p>
            <p class="text-xl font-bold text-gray-900">{{ formatCurrency(expense.totalAmount) }} {{ $t('sum') }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('finance.debtors.paidAmount') }}</p>
            <p class="text-xl font-bold text-green-600">{{ formatCurrency(expense.paidAmount) }} {{ $t('sum') }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500">{{ $t('balance') }}</p>
            <p :class="['text-xl font-bold', balanceDue > 0 ? 'text-red-600' : 'text-green-600']">
              {{ formatCurrency(expense.balanceDue) }} {{ $t('sum') }}
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
              <h3 class="text-lg font-medium">{{ $t('finance.expenseDetail.info') }}</h3>
            </div>
            <div class="card-body">
              <dl class="grid grid-cols-2 gap-4">
                <div>
                  <dt class="text-sm text-gray-500">{{ expense.vendorId ? $t('finance.expenseForm.supplier') : $t('finance.expenseDetail.expenseName') }}</dt>
                  <dd class="font-medium">{{ expense.vendorName || '-' }}</dd>
                </div>
                <div v-if="expense.vendorId">
                  <dt class="text-sm text-gray-500">{{ $t('finance.expenseDetail.vendorInvoiceNumber') }}</dt>
                  <dd class="font-medium">{{ expense.vendorInvoiceNumber || '-' }}</dd>
                </div>
                <div>
                  <dt class="text-sm text-gray-500">{{ $t('date') }}</dt>
                  <dd class="font-medium">{{ formatDate(expense.invoiceDate) }}</dd>
                </div>
                <div>
                  <dt class="text-sm text-gray-500">{{ $t('finance.expenseForm.dueDate') }}</dt>
                  <dd class="font-medium">{{ formatDate(expense.dueDate) }}</dd>
                </div>
                <div v-if="expense.description" class="col-span-2">
                  <dt class="text-sm text-gray-500">{{ $t('description') }}</dt>
                  <dd class="font-medium">{{ expense.description }}</dd>
                </div>
              </dl>
            </div>
          </div>

          <!-- Line Items -->
          <div class="card">
            <div class="card-header">
              <h3 class="text-lg font-medium">{{ $t('finance.expenseDetail.lineItems') }}</h3>
            </div>
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>{{ $t('description') }}</th>
                    <th class="text-right">{{ $t('quantity') }}</th>
                    <th class="text-right">{{ $t('price') }}</th>
                    <th class="text-right">{{ $t('finance.expenseForm.taxPercent') }}</th>
                    <th class="text-right">{{ $t('total') }}</th>
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
                    <td class="text-right font-medium">{{ formatCurrency(line.lineTotal) }} {{ $t('sum') }}</td>
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
              <h3 class="text-lg font-medium">{{ $t('total') }}</h3>
            </div>
            <div class="card-body space-y-3">
              <div class="flex justify-between">
                <span class="text-gray-500">{{ $t('finance.expenseDetail.subtotal') }}:</span>
                <span>{{ formatCurrency(expense.subtotal) }} {{ $t('sum') }}</span>
              </div>
              <div v-if="Number(expense.taxAmount) > 0" class="flex justify-between">
                <span class="text-gray-500">{{ $t('finance.expenseDetail.taxAmount') }}:</span>
                <span>{{ formatCurrency(expense.taxAmount) }} {{ $t('sum') }}</span>
              </div>
              <div v-if="Number(expense.shippingAmount) > 0" class="flex justify-between">
                <span class="text-gray-500">{{ $t('finance.expenseDetail.shippingAmount') }}:</span>
                <span>{{ formatCurrency(expense.shippingAmount) }} {{ $t('sum') }}</span>
              </div>
              <div v-if="Number(expense.discountAmount) > 0" class="flex justify-between text-green-600">
                <span>{{ $t('finance.expenseDetail.discountAmount') }}:</span>
                <span>-{{ formatCurrency(expense.discountAmount) }} {{ $t('sum') }}</span>
              </div>
              <div class="flex justify-between text-lg font-bold border-t pt-3">
                <span>{{ $t('total') }}:</span>
                <span>{{ formatCurrency(expense.totalAmount) }} {{ $t('sum') }}</span>
              </div>
            </div>
          </div>

          <!-- Notes -->
          <div v-if="expense.notes" class="card">
            <div class="card-header">
              <h3 class="text-lg font-medium">{{ $t('notes') }}</h3>
            </div>
            <div class="card-body">
              <p class="text-gray-600">{{ expense.notes }}</p>
            </div>
          </div>

          <!-- Rejection Reason -->
          <div v-if="expense.rejectionReason" class="card border-red-200 bg-red-50">
            <div class="card-header">
              <h3 class="text-lg font-medium text-red-800">{{ $t('finance.expenseDetail.rejectReason') }}</h3>
            </div>
            <div class="card-body">
              <p class="text-red-700">{{ expense.rejectionReason }}</p>
            </div>
          </div>

          <!-- Cancellation Reason -->
          <div v-if="expense.cancellationReason" class="card border-red-200 bg-red-50">
            <div class="card-header">
              <h3 class="text-lg font-medium text-red-800">{{ $t('finance.expenseDetail.cancelReason') }}</h3>
            </div>
            <div class="card-body">
              <p class="text-red-700">{{ expense.cancellationReason }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Payment Modal -->
    <Teleport to="body">
      <div v-if="showPaymentModal" class="fixed inset-0 bg-gray-500 bg-opacity-75 flex items-center justify-center z-50">
        <div class="bg-white rounded-xl shadow-xl max-w-md w-full mx-4">
          <div class="px-6 py-4 border-b">
            <h3 class="text-lg font-semibold text-gray-900">{{ $t('finance.expenseDetail.paymentForm') }}</h3>
            <p class="text-sm text-gray-500 mt-1">
              {{ expense?.invoiceNumber }} — {{ $t('balance') }}: {{ formatCurrency(balanceDue) }} {{ $t('sum') }}
            </p>
          </div>

          <div class="px-6 py-4 space-y-4">
            <div>
              <label class="label">{{ $t('finance.expenseDetail.paymentAmount') }} <span class="text-red-500">*</span></label>
              <input
                v-model.number="paymentForm.paymentAmount"
                type="number"
                :max="balanceDue"
                min="0.01"
                step="any"
                class="input"
              />
              <p class="text-xs text-gray-400 mt-1">{{ $t('finance.expenseDetail.maxAmount') }}: {{ formatCurrency(balanceDue) }} {{ $t('sum') }}</p>
            </div>

            <div>
              <label class="label">{{ $t('finance.payments.paymentMethod') }} <span class="text-red-500">*</span></label>
              <select v-model="paymentForm.paymentMethod" class="input">
                <option value="BANK_TRANSFER">{{ $t('enums.paymentMethod.BANK_TRANSFER') }}</option>
                <option value="CASH">{{ $t('enums.paymentMethod.CASH') }}</option>
                <option value="CHECK">{{ $t('enums.paymentMethod.CHECK') }}</option>
                <option value="CREDIT_CARD">{{ $t('enums.paymentMethod.CREDIT_CARD') }}</option>
                <option value="ACH">{{ $t('enums.paymentMethod.ACH') }}</option>
                <option value="ONLINE">{{ $t('enums.paymentMethod.ONLINE_PAYMENT') }}</option>
                <option value="OTHER">{{ $t('enums.paymentMethod.OTHER') }}</option>
              </select>
            </div>

            <div>
              <label class="label">{{ $t('finance.expenseDetail.paymentDate') }} <span class="text-red-500">*</span></label>
              <input v-model="paymentForm.paymentDate" type="date" class="input" />
            </div>

            <div>
              <label class="label">{{ $t('finance.expenseDetail.referenceNumber') }}</label>
              <input v-model="paymentForm.referenceNumber" type="text" class="input" :placeholder="$t('finance.expenseDetail.transactionNumber')" />
            </div>

            <div>
              <label class="label">{{ $t('notes') }}</label>
              <input v-model="paymentForm.memo" type="text" class="input" :placeholder="$t('finance.expenseDetail.paymentNote')" />
            </div>
          </div>

          <div class="px-6 py-4 border-t flex justify-end space-x-3">
            <button @click="showPaymentModal = false" class="btn-secondary" :disabled="paymentSaving">
              {{ $t('cancel') }}
            </button>
            <button @click="submitPayment" class="btn-pay" :disabled="paymentSaving">
              {{ paymentSaving ? $t('finance.expenseDetail.processingPayment') : $t('finance.expenseDetail.makePayment') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
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
.btn-pay {
  @apply inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-lg shadow-sm text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed;
}
</style>
