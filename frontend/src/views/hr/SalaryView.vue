<script setup>
import { useToastStore } from '@/stores/toast'
import { formatCurrency } from '@/utils/format'
import { ref, reactive, onMounted, computed } from 'vue'
import { salaryApi, advancesApi, employeesApi } from '@/services/api'
import { PlusIcon, CheckCircleIcon, XCircleIcon, XMarkIcon, BanknotesIcon, EyeIcon, ListBulletIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()

const records = ref([])
const advances = ref([])
const employees = ref([])
const loading = ref(true)
const showModal = ref(false)
const showAdvanceModal = ref(false)
const saving = ref(false)

// All Salaries tab
const activeTab = ref('period')
const allSalaries = ref([])
const allSalariesLoading = ref(false)
const allSalariesPage = ref(0)
const allSalariesTotalPages = ref(0)

// Salary detail modal
const showDetailModal = ref(false)
const salaryDetail = ref(null)
const loadingDetail = ref(false)

const now = new Date()
const filterYear = ref(now.getFullYear())
const filterMonth = ref(now.getMonth() + 1)

const form = reactive({
  employeeId: null,
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
  baseAmount: 0,
  bonusAmount: 0,
  deductionAmount: 0,
  notes: ''
})

const advanceForm = reactive({
  employeeId: null,
  amount: 0,
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
  notes: ''
})

const netAmount = computed(() => {
  return (form.baseAmount || 0) + (form.bonusAmount || 0) - (form.deductionAmount || 0)
})

const totalAdvances = computed(() => {
  return advances.value
    .filter(a => a.status === 'GIVEN')
    .reduce((sum, a) => sum + (a.amount || 0), 0)
})

const months = computed(() => [
  t('hr.salary.months.1'), t('hr.salary.months.2'), t('hr.salary.months.3'),
  t('hr.salary.months.4'), t('hr.salary.months.5'), t('hr.salary.months.6'),
  t('hr.salary.months.7'), t('hr.salary.months.8'), t('hr.salary.months.9'),
  t('hr.salary.months.10'), t('hr.salary.months.11'), t('hr.salary.months.12')
])

function statusBadge(status) {
  switch (status) {
    case 'PAID': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'CANCELLED': return 'badge-danger'
    case 'GIVEN': return 'badge-info'
    case 'DEDUCTED': return 'badge-success'
    default: return 'badge-info'
  }
}

function statusLabel(status) {
  switch (status) {
    case 'PAID': return t('hr.salary.paid')
    case 'PENDING': return t('hr.salary.pending')
    case 'CANCELLED': return t('hr.salary.cancelled')
    case 'GIVEN': return t('enums.salaryStatus.GIVEN')
    case 'DEDUCTED': return t('enums.salaryStatus.DEDUCTED')
    default: return status
  }
}

async function loadData() {
  loading.value = true
  try {
    const [salaryRes, advRes, empRes] = await Promise.all([
      salaryApi.getByPeriod(filterYear.value, filterMonth.value, { size: 200 }),
      advancesApi.getByPeriod(filterYear.value, filterMonth.value).catch(() => ({ data: [] })),
      employeesApi.getActive()
    ])
    records.value = salaryRes.data.content || salaryRes.data.data?.content || []
    advances.value = advRes.data.data || advRes.data || []
    employees.value = empRes.data.data || empRes.data || []
  } catch (error) {
    console.error('Failed to load:', error)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    employeeId: null,
    periodYear: filterYear.value,
    periodMonth: filterMonth.value,
    baseAmount: 0,
    bonusAmount: 0,
    deductionAmount: 0,
    notes: ''
  })
  showModal.value = true
}

function openAdvanceCreate() {
  Object.assign(advanceForm, {
    employeeId: null,
    amount: 0,
    periodYear: filterYear.value,
    periodMonth: filterMonth.value,
    notes: ''
  })
  showAdvanceModal.value = true
}

function onEmployeeSelect() {
  const emp = employees.value.find(e => e.id === form.employeeId)
  if (emp && emp.currentSalary) {
    form.baseAmount = emp.currentSalary
  }
}

async function handleSave() {
  if (!form.employeeId || !form.baseAmount) return
  saving.value = true
  try {
    await salaryApi.create(form)
    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('Failed to save:', error)
    toast.error(error.response?.data?.message || t('noData'))
  } finally {
    saving.value = false
  }
}

async function handleAdvanceSave() {
  if (!advanceForm.employeeId || !advanceForm.amount) return
  saving.value = true
  try {
    await advancesApi.create(advanceForm)
    showAdvanceModal.value = false
    await loadData()
  } catch (error) {
    console.error('Failed to save advance:', error)
    toast.error(error.response?.data?.message || t('noData'))
  } finally {
    saving.value = false
  }
}

async function handlePay(id) {
  if (!confirm(t('hr.salary.markPaid'))) return
  try {
    await salaryApi.markPaid(id)
    await loadData()
  } catch (error) {
    toast.error(error.response?.data?.message || t('noData'))
  }
}

async function handleCancel(id) {
  if (!confirm(t('cancel'))) return
  try {
    await salaryApi.cancel(id)
    await loadData()
  } catch (error) {
    toast.error(error.response?.data?.message || t('noData'))
  }
}

async function handleCancelAdvance(id) {
  if (!confirm(t('cancel'))) return
  try {
    await advancesApi.cancel(id)
    await loadData()
  } catch (error) {
    toast.error(error.response?.data?.message || t('noData'))
  }
}

async function loadAllSalaries(page = 0) {
  allSalariesLoading.value = true
  try {
    const response = await salaryApi.getAll({ page, size: 50 })
    const data = response.data.data || response.data
    if (Array.isArray(data)) {
      allSalaries.value = data
      allSalariesTotalPages.value = 1
      allSalariesPage.value = 0
    } else {
      allSalaries.value = data.content || []
      allSalariesTotalPages.value = data.page?.totalPages || data.totalPages || 1
      allSalariesPage.value = data.page?.number ?? data.number ?? 0
    }
  } catch (error) {
    console.error('Failed to load all salaries:', error)
  } finally {
    allSalariesLoading.value = false
  }
}

async function viewSalaryDetail(id) {
  loadingDetail.value = true
  showDetailModal.value = true
  salaryDetail.value = null
  try {
    const response = await salaryApi.getById(id)
    salaryDetail.value = response.data.data || response.data
  } catch (error) {
    console.error('Failed to load salary detail:', error)
    toast.error(error.response?.data?.message || t('errorOccurred'))
    showDetailModal.value = false
  } finally {
    loadingDetail.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'all' && allSalaries.value.length === 0) {
    loadAllSalaries(0)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('hr.salary.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('hr.salary.subtitle') }}</p>
      </div>
      <div class="flex gap-2">
        <button @click="openAdvanceCreate" class="btn-secondary">
          <BanknotesIcon class="h-5 w-5 mr-2" /> {{ $t('hr.salary.pay') }}
        </button>
        <button @click="openCreate" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" /> {{ $t('hr.salary.generate') }}
        </button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex -mb-px space-x-6">
        <button
          @click="switchTab('period')"
          :class="[
            'py-3 px-1 border-b-2 text-sm font-medium transition-colors',
            activeTab === 'period'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ $t('hr.salary.byPeriod') }}
        </button>
        <button
          @click="switchTab('all')"
          :class="[
            'py-3 px-1 border-b-2 text-sm font-medium transition-colors',
            activeTab === 'all'
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          <ListBulletIcon class="h-4 w-4 inline mr-1" />
          {{ $t('hr.salary.allSalaries') }}
        </button>
      </nav>
    </div>

    <!-- Period tab content -->
    <template v-if="activeTab === 'period'">
    <!-- Filter -->
    <div class="card">
      <div class="card-body">
        <div class="flex items-end gap-4">
          <div>
            <label class="label">{{ $t('hr.salary.year') }}</label>
            <input v-model.number="filterYear" type="number" min="2020" max="2030" class="input w-28" />
          </div>
          <div>
            <label class="label">{{ $t('hr.salary.month') }}</label>
            <select v-model.number="filterMonth" class="input w-40">
              <option v-for="(m, i) in months" :key="i" :value="i + 1">{{ m }}</option>
            </select>
          </div>
          <button @click="loadData" class="btn-primary">{{ $t('apply') }}</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
    </div>

    <template v-else>
      <!-- Advances section -->
      <div v-if="advances.length > 0" class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">{{ $t('hr.salary.pay') }}</h3>
          <div class="text-sm">
            {{ $t('hr.salary.totalSalary') }}:
            <span class="font-semibold text-blue-600">{{ formatCurrency(totalAdvances) }}</span>
          </div>
        </div>
        <div class="card-body">
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('hr.salary.employee') }}</th>
                  <th>{{ $t('code') }}</th>
                  <th>{{ $t('hr.employees.hireDate') }}</th>
                  <th class="text-right">{{ $t('amount') }}</th>
                  <th>{{ $t('status') }}</th>
                  <th>{{ $t('actions') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="adv in advances" :key="adv.id">
                  <td>{{ adv.employeeName }}</td>
                  <td class="font-medium">{{ adv.employeeCode }}</td>
                  <td>{{ adv.advanceDate }}</td>
                  <td class="text-right font-semibold text-blue-600">{{ formatCurrency(adv.amount) }}</td>
                  <td>
                    <span :class="['badge', statusBadge(adv.status)]">{{ statusLabel(adv.status) }}</span>
                  </td>
                  <td>
                    <button v-if="adv.status === 'GIVEN'" @click="handleCancelAdvance(adv.id)"
                      class="text-red-600 hover:text-red-800" :title="$t('cancel')">
                      <XCircleIcon class="h-5 w-5" />
                    </button>
                    <span v-else class="text-gray-400 text-sm">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Salary records -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('hr.salary.title') }}</h3>
        </div>
        <div class="card-body">
          <div v-if="records.length === 0" class="text-center py-12 text-gray-500">
            {{ $t('hr.salary.noRecords') }}
          </div>
          <div v-else class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('hr.salary.employee') }}</th>
                  <th>{{ $t('code') }}</th>
                  <th class="text-right">{{ $t('hr.salary.base') }}</th>
                  <th class="text-right">{{ $t('hr.salary.bonus') }}</th>
                  <th class="text-right">{{ $t('hr.salary.deduction') }}</th>
                  <th class="text-right">{{ $t('hr.salary.netSalary') }}</th>
                  <th class="text-right">{{ $t('hr.salary.pay') }}</th>
                  <th class="text-right">{{ $t('hr.salary.totalPaid') }}</th>
                  <th>{{ $t('status') }}</th>
                  <th>{{ $t('actions') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="rec in records" :key="rec.id">
                  <td>{{ rec.employeeName }}</td>
                  <td class="font-medium">{{ rec.employeeCode }}</td>
                  <td class="text-right">{{ formatCurrency(rec.baseAmount) }}</td>
                  <td class="text-right text-green-600">+{{ formatCurrency(rec.bonusAmount) }}</td>
                  <td class="text-right text-red-600">-{{ formatCurrency(rec.deductionAmount) }}</td>
                  <td class="text-right font-semibold">{{ formatCurrency(rec.netAmount) }}</td>
                  <td class="text-right text-blue-600">
                    <template v-if="rec.advanceAmount > 0">-{{ formatCurrency(rec.advanceAmount) }}</template>
                    <template v-else>-</template>
                  </td>
                  <td class="text-right font-semibold">
                    <template v-if="rec.payAmount != null">{{ formatCurrency(rec.payAmount) }}</template>
                    <template v-else>{{ formatCurrency(rec.netAmount) }}</template>
                  </td>
                  <td>
                    <span :class="['badge', statusBadge(rec.status)]">{{ statusLabel(rec.status) }}</span>
                  </td>
                  <td>
                    <div class="flex items-center gap-2">
                      <button @click="viewSalaryDetail(rec.id)" class="text-primary-600 hover:text-primary-800" :title="$t('details')">
                        <EyeIcon class="h-5 w-5" />
                      </button>
                      <template v-if="rec.status === 'PENDING'">
                        <button @click="handlePay(rec.id)" class="text-green-600 hover:text-green-800" :title="$t('hr.salary.pay')">
                          <CheckCircleIcon class="h-5 w-5" />
                        </button>
                        <button @click="handleCancel(rec.id)" class="text-red-600 hover:text-red-800" :title="$t('cancel')">
                          <XCircleIcon class="h-5 w-5" />
                        </button>
                      </template>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>
    </template>

    <!-- All Salaries tab content -->
    <template v-if="activeTab === 'all'">
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('hr.salary.allSalaries') }}</h3>
        </div>
        <div class="card-body">
          <div v-if="allSalariesLoading" class="flex items-center justify-center h-32">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
          <div v-else-if="allSalaries.length === 0" class="text-center py-12 text-gray-500">
            {{ $t('hr.salary.noRecords') }}
          </div>
          <div v-else class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('hr.salary.employee') }}</th>
                  <th>{{ $t('code') }}</th>
                  <th>{{ $t('hr.salary.year') }}</th>
                  <th>{{ $t('hr.salary.month') }}</th>
                  <th class="text-right">{{ $t('hr.salary.base') }}</th>
                  <th class="text-right">{{ $t('hr.salary.bonus') }}</th>
                  <th class="text-right">{{ $t('hr.salary.deduction') }}</th>
                  <th class="text-right">{{ $t('hr.salary.netSalary') }}</th>
                  <th>{{ $t('status') }}</th>
                  <th>{{ $t('actions') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="rec in allSalaries" :key="rec.id">
                  <td>{{ rec.employeeName }}</td>
                  <td class="font-medium">{{ rec.employeeCode }}</td>
                  <td>{{ rec.periodYear }}</td>
                  <td>{{ months[rec.periodMonth - 1] }}</td>
                  <td class="text-right">{{ formatCurrency(rec.baseAmount) }}</td>
                  <td class="text-right text-green-600">+{{ formatCurrency(rec.bonusAmount) }}</td>
                  <td class="text-right text-red-600">-{{ formatCurrency(rec.deductionAmount) }}</td>
                  <td class="text-right font-semibold">{{ formatCurrency(rec.netAmount) }}</td>
                  <td>
                    <span :class="['badge', statusBadge(rec.status)]">{{ statusLabel(rec.status) }}</span>
                  </td>
                  <td>
                    <button @click="viewSalaryDetail(rec.id)" class="text-primary-600 hover:text-primary-800" :title="$t('details')">
                      <EyeIcon class="h-5 w-5" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- Pagination -->
          <div v-if="allSalariesTotalPages > 1" class="flex items-center justify-between mt-4 pt-4 border-t">
            <p class="text-sm text-gray-500">{{ $t('page') }} {{ allSalariesPage + 1 }} / {{ allSalariesTotalPages }}</p>
            <div class="flex gap-2">
              <button
                @click="loadAllSalaries(allSalariesPage - 1)"
                :disabled="allSalariesPage === 0"
                class="btn-secondary text-sm"
              >{{ $t('previous') }}</button>
              <button
                @click="loadAllSalaries(allSalariesPage + 1)"
                :disabled="allSalariesPage >= allSalariesTotalPages - 1"
                class="btn-secondary text-sm"
              >{{ $t('next') }}</button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Salary Detail Modal -->
    <div v-if="showDetailModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-lg font-medium">{{ $t('hr.salary.salaryDetail') }}</h3>
          <button @click="showDetailModal = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
        </div>
        <div class="p-6">
          <div v-if="loadingDetail" class="flex items-center justify-center h-32">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
          <div v-else-if="salaryDetail" class="space-y-3">
            <div class="flex justify-between">
              <span class="text-gray-500">{{ $t('hr.salary.employee') }}</span>
              <span class="font-medium">{{ salaryDetail.employeeName }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-500">{{ $t('code') }}</span>
              <span class="font-mono text-sm">{{ salaryDetail.employeeCode }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-500">{{ $t('hr.salary.year') }} / {{ $t('hr.salary.month') }}</span>
              <span>{{ salaryDetail.periodYear }} / {{ months[(salaryDetail.periodMonth || 1) - 1] }}</span>
            </div>
            <div class="border-t pt-3 space-y-2">
              <div class="flex justify-between">
                <span class="text-gray-500">{{ $t('hr.salary.base') }}</span>
                <span>{{ formatCurrency(salaryDetail.baseAmount) }}</span>
              </div>
              <div class="flex justify-between text-green-600">
                <span>{{ $t('hr.salary.bonus') }}</span>
                <span>+{{ formatCurrency(salaryDetail.bonusAmount) }}</span>
              </div>
              <div class="flex justify-between text-red-600">
                <span>{{ $t('hr.salary.deduction') }}</span>
                <span>-{{ formatCurrency(salaryDetail.deductionAmount) }}</span>
              </div>
              <div class="flex justify-between font-semibold border-t pt-2">
                <span>{{ $t('hr.salary.netSalary') }}</span>
                <span>{{ formatCurrency(salaryDetail.netAmount) }}</span>
              </div>
              <div v-if="salaryDetail.advanceAmount > 0" class="flex justify-between text-blue-600">
                <span>{{ $t('hr.salary.pay') }}</span>
                <span>-{{ formatCurrency(salaryDetail.advanceAmount) }}</span>
              </div>
              <div v-if="salaryDetail.payAmount != null" class="flex justify-between font-bold border-t pt-2">
                <span>{{ $t('hr.salary.totalPaid') }}</span>
                <span>{{ formatCurrency(salaryDetail.payAmount) }}</span>
              </div>
            </div>
            <div class="flex justify-between pt-2">
              <span class="text-gray-500">{{ $t('status') }}</span>
              <span :class="['badge', statusBadge(salaryDetail.status)]">{{ statusLabel(salaryDetail.status) }}</span>
            </div>
            <div v-if="salaryDetail.notes" class="pt-2">
              <span class="text-gray-500 text-sm">{{ $t('notes') }}:</span>
              <p class="text-sm mt-1">{{ salaryDetail.notes }}</p>
            </div>
          </div>
        </div>
        <div class="flex justify-end px-6 py-4 border-t">
          <button @click="showDetailModal = false" class="btn-secondary">{{ $t('close') }}</button>
        </div>
      </div>
    </div>

    <!-- Salary Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-lg font-medium">{{ $t('hr.salary.generate') }}</h3>
          <button @click="showModal = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
        </div>
        <form @submit.prevent="handleSave" class="p-6 space-y-4">
          <div>
            <label class="label">{{ $t('hr.salary.employee') }} *</label>
            <select v-model="form.employeeId" @change="onEmployeeSelect" class="input">
              <option :value="null">{{ $t('hr.employeeForm.selectDepartment') }}</option>
              <option v-for="e in employees" :key="e.id" :value="e.id">
                {{ e.fullName }} ({{ e.employeeCode }})
              </option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('hr.salary.year') }}</label>
              <input v-model.number="form.periodYear" type="number" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('hr.salary.month') }}</label>
              <select v-model.number="form.periodMonth" class="input">
                <option v-for="(m, i) in months" :key="i" :value="i + 1">{{ m }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="label">{{ $t('hr.salary.base') }} *</label>
            <input v-model.number="form.baseAmount" type="number" step="1000" min="0" class="input" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('hr.salary.bonus') }}</label>
              <input v-model.number="form.bonusAmount" type="number" step="1000" min="0" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('hr.salary.deduction') }}</label>
              <input v-model.number="form.deductionAmount" type="number" step="1000" min="0" class="input" />
            </div>
          </div>
          <div class="p-3 bg-gray-50 rounded-lg text-center">
            <span class="text-sm text-gray-500">{{ $t('total') }}:</span>
            <span class="ml-2 text-lg font-semibold">{{ formatCurrency(netAmount) }}</span>
          </div>
          <div>
            <label class="label">{{ $t('notes') }}</label>
            <textarea v-model="form.notes" rows="2" class="input"></textarea>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button type="submit" :disabled="saving" class="btn-primary">
              {{ saving ? $t('saving') : $t('save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Advance Modal -->
    <div v-if="showAdvanceModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-lg font-medium">{{ $t('hr.salary.pay') }}</h3>
          <button @click="showAdvanceModal = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
        </div>
        <form @submit.prevent="handleAdvanceSave" class="p-6 space-y-4">
          <div>
            <label class="label">{{ $t('hr.salary.employee') }} *</label>
            <select v-model="advanceForm.employeeId" class="input">
              <option :value="null">{{ $t('hr.employeeForm.selectDepartment') }}</option>
              <option v-for="e in employees" :key="e.id" :value="e.id">
                {{ e.fullName }} ({{ e.employeeCode }})
              </option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('hr.salary.year') }}</label>
              <input v-model.number="advanceForm.periodYear" type="number" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('hr.salary.month') }}</label>
              <select v-model.number="advanceForm.periodMonth" class="input">
                <option v-for="(m, i) in months" :key="i" :value="i + 1">{{ m }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="label">{{ $t('amount') }} *</label>
            <input v-model.number="advanceForm.amount" type="number" step="1000" min="0" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('notes') }}</label>
            <textarea v-model="advanceForm.notes" rows="2" class="input"></textarea>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showAdvanceModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button type="submit" :disabled="saving" class="btn-primary">
              {{ saving ? $t('saving') : $t('hr.salary.pay') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
