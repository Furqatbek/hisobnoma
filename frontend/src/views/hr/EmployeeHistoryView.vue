<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { employeesApi, salaryApi, advancesApi } from '@/services/api'
import {
  MagnifyingGlassIcon,
  UserIcon,
  BanknotesIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  PhoneIcon,
  EnvelopeIcon,
  BriefcaseIcon,
  BuildingOfficeIcon,
  CalendarIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const route = useRoute()

// State
const loading = ref(true)
const employees = ref([])
const selectedEmployeeId = ref(null)
const employeeSearch = ref('')
const activeTab = ref('overview')

// Employee data
const employee = ref(null)
const salaryRecords = ref([])
const advanceRecords = ref([])
const loadingDetail = ref(false)

// Expanded rows
const expandedSalary = ref(null)

// Undeducted advance total
const undeductedTotal = ref(null)
const undeductedLoading = ref(false)
const undeductedYear = ref(new Date().getFullYear())
const undeductedMonth = ref(new Date().getMonth() + 1)

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleDateString('uz-UZ')
}

function formatPeriod(year, month) {
  return `${t('months.' + month)} ${year}`
}

// Fetch employees list
async function fetchEmployees() {
  try {
    const res = await employeesApi.getAll({ page: 0, size: 1000 })
    const data = res.data.data || res.data
    employees.value = data.content || data || []
  } catch (e) {
    console.error('Failed to load employees:', e)
  }
}

const filteredEmployees = computed(() => {
  if (!employeeSearch.value) return employees.value
  const q = employeeSearch.value.toLowerCase()
  return employees.value.filter(e =>
    e.fullName?.toLowerCase().includes(q) ||
    e.firstName?.toLowerCase().includes(q) ||
    e.lastName?.toLowerCase().includes(q) ||
    e.employeeCode?.toLowerCase().includes(q) ||
    e.phone?.includes(q)
  )
})

// Select employee
async function selectEmployee(e) {
  selectedEmployeeId.value = e.id
  employee.value = e
  activeTab.value = 'overview'
  undeductedTotal.value = null
  await loadEmployeeData()
}

async function loadEmployeeData() {
  if (!selectedEmployeeId.value) return
  loadingDetail.value = true

  try {
    const [salaryRes, advanceRes] = await Promise.all([
      salaryApi.getByEmployee(selectedEmployeeId.value).catch(() => null),
      advancesApi.getByEmployee(selectedEmployeeId.value).catch(() => null)
    ])

    if (salaryRes) {
      const sData = salaryRes.data.data || salaryRes.data
      salaryRecords.value = Array.isArray(sData) ? sData : (sData.content || sData || [])
    }

    if (advanceRes) {
      const aData = advanceRes.data.data || advanceRes.data
      advanceRecords.value = Array.isArray(aData) ? aData : (aData.content || aData || [])
    }
  } catch (e) {
    console.error('Failed to load employee data:', e)
  } finally {
    loadingDetail.value = false
  }
}

async function loadUndeductedTotal() {
  if (!selectedEmployeeId.value) return
  undeductedLoading.value = true
  try {
    const res = await advancesApi.getUndeductedTotal(selectedEmployeeId.value, undeductedYear.value, undeductedMonth.value)
    const data = res.data.data != null ? res.data.data : res.data
    undeductedTotal.value = typeof data === 'number' ? data : (data?.total ?? data?.amount ?? 0)
  } catch (e) {
    console.error('Failed to load undeducted total:', e)
    undeductedTotal.value = null
  } finally {
    undeductedLoading.value = false
  }
}

const undeductedMonths = computed(() => [
  t('months.1'), t('months.2'), t('months.3'),
  t('months.4'), t('months.5'), t('months.6'),
  t('months.7'), t('months.8'), t('months.9'),
  t('months.10'), t('months.11'), t('months.12')
])

// Summary stats
const stats = computed(() => {
  const paidSalaries = salaryRecords.value.filter(s => s.status === 'PAID')
  const totalNet = paidSalaries.reduce((sum, s) => sum + (s.netAmount || 0), 0)
  const totalCashPaid = paidSalaries.reduce((sum, s) => sum + (s.payAmount != null ? s.payAmount : s.netAmount || 0), 0)
  const totalAdvances = advanceRecords.value
    .filter(a => a.status !== 'CANCELLED')
    .reduce((sum, a) => sum + (a.amount || 0), 0)
  const pendingSalaries = salaryRecords.value
    .filter(s => s.status === 'PENDING')
    .reduce((sum, s) => sum + (s.netAmount || 0), 0)

  return {
    totalSalary: totalNet,
    totalPaid: totalCashPaid,
    totalAdvances,
    pendingBalance: pendingSalaries
  }
})

function getSalaryStatusClass(status) {
  const classes = {
    'PAID': 'badge-success',
    'PENDING': 'badge-warning',
    'CANCELLED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getAdvanceStatusClass(status) {
  const classes = {
    'GIVEN': 'badge-info',
    'DEDUCTED': 'badge-success',
    'CANCELLED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

function getEmployeeStatusClass(status) {
  const classes = {
    'ACTIVE': 'badge-success',
    'ON_LEAVE': 'badge-warning',
    'TERMINATED': 'badge-danger'
  }
  return classes[status] || 'badge-info'
}

onMounted(async () => {
  await fetchEmployees()

  // If navigated with employee id param
  if (route.params.id) {
    const e = employees.value.find(e => e.id == route.params.id)
    if (e) {
      await selectEmployee(e)
    } else {
      try {
        const res = await employeesApi.getById(route.params.id)
        employee.value = res.data.data || res.data
        selectedEmployeeId.value = employee.value.id
        await loadEmployeeData()
      } catch (e) {
        console.error('Employee not found:', e)
      }
    }
  }

  loading.value = false
})
</script>

<template>
  <div>
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ t('employeeHistory.title') }}</h1>
      <p class="text-sm text-gray-500 mt-1">{{ t('employeeHistory.subtitle') }}</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
      <!-- Left sidebar: Employee list -->
      <div class="lg:col-span-1">
        <div class="card">
          <div class="card-body">
            <!-- Search -->
            <div class="relative mb-4">
              <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                v-model="employeeSearch"
                type="text"
                :placeholder="t('employeeHistory.searchEmployee')"
                class="input pl-9 w-full text-sm"
              />
            </div>

            <!-- Employee list -->
            <div v-if="loading" class="flex justify-center py-8">
              <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
            </div>
            <div v-else class="space-y-1 max-h-[calc(100vh-280px)] overflow-y-auto">
              <button
                v-for="e in filteredEmployees"
                :key="e.id"
                @click="selectEmployee(e)"
                :class="[
                  'w-full text-left px-3 py-2.5 rounded-lg transition-colors text-sm',
                  selectedEmployeeId === e.id
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'hover:bg-gray-50 text-gray-700'
                ]"
              >
                <div class="font-medium truncate">{{ e.fullName || (e.firstName + ' ' + e.lastName) }}</div>
                <div class="flex items-center justify-between mt-0.5">
                  <span class="text-xs text-gray-400">{{ e.employeeCode }}</span>
                  <span :class="['text-xs', e.status === 'ACTIVE' ? 'text-green-600' : e.status === 'ON_LEAVE' ? 'text-yellow-600' : 'text-red-600']">
                    {{ t('enums.employeeStatus.' + e.status) }}
                  </span>
                </div>
              </button>
              <p v-if="filteredEmployees.length === 0" class="text-sm text-gray-400 text-center py-4">
                {{ t('employeeHistory.noEmployees') }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Right content: Employee details -->
      <div class="lg:col-span-3">
        <!-- No employee selected -->
        <div v-if="!selectedEmployeeId" class="card">
          <div class="card-body flex flex-col items-center justify-center py-16">
            <UserIcon class="h-12 w-12 text-gray-300 mb-3" />
            <p class="text-gray-400">{{ t('employeeHistory.selectEmployeeHint') }}</p>
          </div>
        </div>

        <!-- Employee detail -->
        <div v-else>
          <!-- Loading -->
          <div v-if="loadingDetail" class="flex items-center justify-center h-64">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>

          <div v-else>
            <!-- Employee profile card -->
            <div class="card mb-6">
              <div class="card-body">
                <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                  <div>
                    <h2 class="text-xl font-bold text-gray-900">{{ employee?.fullName || (employee?.firstName + ' ' + employee?.lastName) }}</h2>
                    <div class="flex flex-wrap items-center gap-3 mt-2 text-sm text-gray-500">
                      <span v-if="employee?.employeeCode" class="inline-flex items-center">
                        <span class="font-mono bg-gray-100 px-2 py-0.5 rounded text-xs">{{ employee.employeeCode }}</span>
                      </span>
                      <span v-if="employee?.phone" class="inline-flex items-center gap-1">
                        <PhoneIcon class="h-3.5 w-3.5" /> {{ employee.phone }}
                      </span>
                      <span v-if="employee?.email" class="inline-flex items-center gap-1">
                        <EnvelopeIcon class="h-3.5 w-3.5" /> {{ employee.email }}
                      </span>
                      <span v-if="employee?.departmentName" class="inline-flex items-center gap-1">
                        <BuildingOfficeIcon class="h-3.5 w-3.5" /> {{ employee.departmentName }}
                      </span>
                      <span v-if="employee?.positionName" class="inline-flex items-center gap-1">
                        <BriefcaseIcon class="h-3.5 w-3.5" /> {{ employee.positionName }}
                      </span>
                      <span v-if="employee?.hireDate" class="inline-flex items-center gap-1">
                        <CalendarIcon class="h-3.5 w-3.5" /> {{ formatDate(employee.hireDate) }}
                      </span>
                    </div>
                  </div>
                  <div class="flex-shrink-0 flex items-center gap-2">
                    <span v-if="employee?.currentSalary" class="text-sm text-gray-500">
                      {{ t('employeeHistory.currentSalaryRate') }}: <span class="font-semibold text-gray-900">{{ formatCurrency(employee.currentSalary) }}</span>
                    </span>
                    <span v-if="employee?.status" :class="['badge', getEmployeeStatusClass(employee.status)]">
                      {{ t('enums.employeeStatus.' + employee.status) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Summary cards -->
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('employeeHistory.totalSalary') }}</p>
                  <p class="text-lg font-bold text-gray-900 mt-1">{{ formatCurrency(stats.totalSalary) }}</p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('employeeHistory.totalPaid') }}</p>
                  <p class="text-lg font-bold text-green-600 mt-1">{{ formatCurrency(stats.totalPaid) }}</p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('employeeHistory.totalAdvances') }}</p>
                  <p class="text-lg font-bold text-blue-600 mt-1">{{ formatCurrency(stats.totalAdvances) }}</p>
                </div>
              </div>
              <div class="card">
                <div class="card-body py-3 px-4">
                  <p class="text-xs text-gray-500">{{ t('employeeHistory.currentBalance') }}</p>
                  <p :class="['text-lg font-bold mt-1', stats.pendingBalance > 0 ? 'text-orange-600' : 'text-gray-900']">
                    {{ formatCurrency(stats.pendingBalance) }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Tabs -->
            <div class="border-b border-gray-200 mb-4">
              <nav class="flex -mb-px space-x-6">
                <button
                  v-for="tab in ['overview', 'salaryRecords', 'advances']"
                  :key="tab"
                  @click="activeTab = tab"
                  :class="[
                    'py-3 px-1 border-b-2 text-sm font-medium transition-colors',
                    activeTab === tab
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  ]"
                >
                  {{ t('employeeHistory.tabs.' + tab) }}
                  <span v-if="tab === 'salaryRecords' && salaryRecords.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ salaryRecords.length }}
                  </span>
                  <span v-if="tab === 'advances' && advanceRecords.length" class="ml-1.5 bg-gray-100 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">
                    {{ advanceRecords.length }}
                  </span>
                </button>
              </nav>
            </div>

            <!-- ==================== OVERVIEW TAB ==================== -->
            <div v-if="activeTab === 'overview'" class="space-y-6">
              <!-- Recent salary records -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('employeeHistory.recentSalary') }}</h3>
                  <button @click="activeTab = 'salaryRecords'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('employeeHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="salaryRecords.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('employeeHistory.noSalaryRecords') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('employeeHistory.period') }}</th>
                        <th class="text-right">{{ t('employeeHistory.base') }}</th>
                        <th class="text-right">{{ t('employeeHistory.bonus') }}</th>
                        <th class="text-right">{{ t('employeeHistory.deduction') }}</th>
                        <th class="text-right">{{ t('employeeHistory.net') }}</th>
                        <th class="text-right">{{ t('employeeHistory.cashPaid') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="rec in salaryRecords.slice(0, 5)" :key="rec.id" class="hover:bg-gray-50">
                        <td class="text-sm font-medium">{{ formatPeriod(rec.periodYear, rec.periodMonth) }}</td>
                        <td class="text-right text-sm">{{ formatCurrency(rec.baseAmount) }}</td>
                        <td class="text-right text-sm text-green-600">{{ rec.bonusAmount > 0 ? '+' + formatCurrency(rec.bonusAmount) : '—' }}</td>
                        <td class="text-right text-sm text-red-600">{{ rec.deductionAmount > 0 ? '-' + formatCurrency(rec.deductionAmount) : '—' }}</td>
                        <td class="text-right text-sm font-semibold">{{ formatCurrency(rec.netAmount) }}</td>
                        <td class="text-right text-sm font-medium text-green-600">
                          {{ rec.status === 'PAID' ? formatCurrency(rec.payAmount != null ? rec.payAmount : rec.netAmount) : '—' }}
                        </td>
                        <td class="text-center">
                          <span :class="['badge', getSalaryStatusClass(rec.status)]">
                            {{ t('enums.salaryStatus.' + rec.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- Recent advances -->
              <div class="card">
                <div class="card-header flex items-center justify-between">
                  <h3 class="text-sm font-semibold text-gray-900">{{ t('employeeHistory.recentAdvances') }}</h3>
                  <button @click="activeTab = 'advances'" class="text-xs text-primary-600 hover:text-primary-700">
                    {{ t('employeeHistory.viewAll') }} &rarr;
                  </button>
                </div>
                <div class="card-body p-0">
                  <div v-if="advanceRecords.length === 0" class="py-8 text-center text-sm text-gray-400">
                    {{ t('employeeHistory.noAdvances') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('employeeHistory.period') }}</th>
                        <th class="text-left">{{ t('employeeHistory.advanceDate') }}</th>
                        <th class="text-right">{{ t('amount') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="adv in advanceRecords.slice(0, 5)" :key="adv.id" class="hover:bg-gray-50">
                        <td class="text-sm font-medium">{{ formatPeriod(adv.periodYear, adv.periodMonth) }}</td>
                        <td class="text-sm">{{ formatDate(adv.advanceDate) }}</td>
                        <td class="text-right text-sm font-medium text-blue-600">{{ formatCurrency(adv.amount) }}</td>
                        <td class="text-center">
                          <span :class="['badge', getAdvanceStatusClass(adv.status)]">
                            {{ t('enums.salaryStatus.' + adv.status) }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- ==================== SALARY RECORDS TAB ==================== -->
            <div v-if="activeTab === 'salaryRecords'">
              <div class="card">
                <div class="card-body p-0">
                  <div v-if="salaryRecords.length === 0" class="py-12 text-center text-sm text-gray-400">
                    {{ t('employeeHistory.noSalaryRecords') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th></th>
                        <th class="text-left">{{ t('employeeHistory.period') }}</th>
                        <th class="text-right">{{ t('employeeHistory.base') }}</th>
                        <th class="text-right">{{ t('employeeHistory.bonus') }}</th>
                        <th class="text-right">{{ t('employeeHistory.deduction') }}</th>
                        <th class="text-right">{{ t('employeeHistory.net') }}</th>
                        <th class="text-right">{{ t('employeeHistory.advanceDeducted') }}</th>
                        <th class="text-right">{{ t('employeeHistory.cashPaid') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template v-for="rec in salaryRecords" :key="rec.id">
                        <tr class="hover:bg-gray-50 cursor-pointer" @click="expandedSalary = expandedSalary === rec.id ? null : rec.id">
                          <td class="w-8">
                            <ChevronDownIcon v-if="expandedSalary !== rec.id" class="h-4 w-4 text-gray-400" />
                            <ChevronUpIcon v-else class="h-4 w-4 text-primary-600" />
                          </td>
                          <td class="font-medium text-sm">{{ formatPeriod(rec.periodYear, rec.periodMonth) }}</td>
                          <td class="text-right text-sm">{{ formatCurrency(rec.baseAmount) }}</td>
                          <td class="text-right text-sm text-green-600">{{ rec.bonusAmount > 0 ? '+' + formatCurrency(rec.bonusAmount) : '—' }}</td>
                          <td class="text-right text-sm text-red-600">{{ rec.deductionAmount > 0 ? '-' + formatCurrency(rec.deductionAmount) : '—' }}</td>
                          <td class="text-right text-sm font-semibold">{{ formatCurrency(rec.netAmount) }}</td>
                          <td class="text-right text-sm text-blue-600">
                            {{ rec.advanceAmount > 0 ? '-' + formatCurrency(rec.advanceAmount) : '—' }}
                          </td>
                          <td class="text-right text-sm font-semibold text-green-600">
                            {{ rec.status === 'PAID' ? formatCurrency(rec.payAmount != null ? rec.payAmount : rec.netAmount) : '—' }}
                          </td>
                          <td class="text-center">
                            <span :class="['badge', getSalaryStatusClass(rec.status)]">
                              {{ t('enums.salaryStatus.' + rec.status) }}
                            </span>
                          </td>
                        </tr>
                        <!-- Expanded details -->
                        <tr v-if="expandedSalary === rec.id">
                          <td colspan="9" class="bg-gray-50 px-6 py-4">
                            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
                              <div>
                                <span class="text-gray-500">{{ t('employeeHistory.base') }}:</span>
                                <span class="ml-1 font-medium">{{ formatCurrency(rec.baseAmount) }}</span>
                              </div>
                              <div v-if="rec.bonusAmount > 0">
                                <span class="text-gray-500">{{ t('employeeHistory.bonus') }}:</span>
                                <span class="ml-1 font-medium text-green-600">+{{ formatCurrency(rec.bonusAmount) }}</span>
                              </div>
                              <div v-if="rec.deductionAmount > 0">
                                <span class="text-gray-500">{{ t('employeeHistory.deduction') }}:</span>
                                <span class="ml-1 font-medium text-red-600">-{{ formatCurrency(rec.deductionAmount) }}</span>
                              </div>
                              <div>
                                <span class="text-gray-500">{{ t('employeeHistory.net') }}:</span>
                                <span class="ml-1 font-bold">{{ formatCurrency(rec.netAmount) }}</span>
                              </div>
                              <div v-if="rec.advanceAmount > 0">
                                <span class="text-gray-500">{{ t('employeeHistory.advanceDeducted') }}:</span>
                                <span class="ml-1 font-medium text-blue-600">-{{ formatCurrency(rec.advanceAmount) }}</span>
                              </div>
                              <div v-if="rec.status === 'PAID'">
                                <span class="text-gray-500">{{ t('employeeHistory.cashPaid') }}:</span>
                                <span class="ml-1 font-bold text-green-600">{{ formatCurrency(rec.payAmount != null ? rec.payAmount : rec.netAmount) }}</span>
                              </div>
                              <div v-if="rec.paidDate">
                                <span class="text-gray-500">{{ t('employeeHistory.paidDate') }}:</span>
                                <span class="ml-1 font-medium">{{ formatDate(rec.paidDate) }}</span>
                              </div>
                              <div v-if="rec.glJournalEntryId">
                                <span class="text-gray-500">{{ t('employeeHistory.glEntry') }}:</span>
                                <span class="ml-1 font-mono text-xs bg-green-50 text-green-700 px-1.5 py-0.5 rounded">#{{ rec.glJournalEntryId }}</span>
                              </div>
                            </div>
                            <!-- Salary breakdown visualization -->
                            <div v-if="rec.status === 'PAID'" class="mt-4 p-3 bg-white rounded-lg border border-gray-200">
                              <h4 class="text-xs font-semibold text-gray-500 uppercase mb-3">{{ t('employeeHistory.cashPaid') }}</h4>
                              <div class="space-y-2 text-sm">
                                <div class="flex justify-between">
                                  <span>{{ t('employeeHistory.base') }}</span>
                                  <span class="font-medium">{{ formatCurrency(rec.baseAmount) }}</span>
                                </div>
                                <div v-if="rec.bonusAmount > 0" class="flex justify-between text-green-600">
                                  <span>+ {{ t('employeeHistory.bonus') }}</span>
                                  <span class="font-medium">{{ formatCurrency(rec.bonusAmount) }}</span>
                                </div>
                                <div v-if="rec.deductionAmount > 0" class="flex justify-between text-red-600">
                                  <span>- {{ t('employeeHistory.deduction') }}</span>
                                  <span class="font-medium">{{ formatCurrency(rec.deductionAmount) }}</span>
                                </div>
                                <div class="border-t pt-1 flex justify-between font-semibold">
                                  <span>= {{ t('employeeHistory.net') }}</span>
                                  <span>{{ formatCurrency(rec.netAmount) }}</span>
                                </div>
                                <div v-if="rec.advanceAmount > 0" class="flex justify-between text-blue-600">
                                  <span>- {{ t('employeeHistory.advanceDeducted') }}</span>
                                  <span class="font-medium">{{ formatCurrency(rec.advanceAmount) }}</span>
                                </div>
                                <div class="border-t pt-1 flex justify-between font-bold text-green-700">
                                  <span>= {{ t('employeeHistory.cashPaid') }}</span>
                                  <span>{{ formatCurrency(rec.payAmount != null ? rec.payAmount : rec.netAmount) }}</span>
                                </div>
                              </div>
                            </div>
                            <p v-if="rec.notes" class="text-xs text-gray-500 mt-2">{{ rec.notes }}</p>
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- ==================== ADVANCES TAB ==================== -->
            <div v-if="activeTab === 'advances'">
              <!-- Undeducted advance total -->
              <div class="card mb-4">
                <div class="card-body">
                  <h4 class="text-sm font-semibold text-gray-900 mb-3">{{ t('employeeHistory.undeductedTotal') }}</h4>
                  <div class="flex items-end gap-3">
                    <div>
                      <label class="label text-xs">{{ t('year') }}</label>
                      <input v-model.number="undeductedYear" type="number" min="2020" max="2030" class="input w-24 text-sm" />
                    </div>
                    <div>
                      <label class="label text-xs">{{ t('month') }}</label>
                      <select v-model.number="undeductedMonth" class="input w-32 text-sm">
                        <option v-for="(m, i) in undeductedMonths" :key="i" :value="i + 1">{{ m }}</option>
                      </select>
                    </div>
                    <button @click="loadUndeductedTotal" class="btn-secondary text-sm">{{ t('apply') }}</button>
                    <div class="ml-4 flex items-center gap-2">
                      <div v-if="undeductedLoading" class="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-600"></div>
                      <template v-else-if="undeductedTotal !== null">
                        <span class="text-sm text-gray-500">{{ t('employeeHistory.undeductedAmount') }}:</span>
                        <span class="text-lg font-bold text-orange-600">{{ formatCurrency(undeductedTotal) }}</span>
                      </template>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card">
                <div class="card-body p-0">
                  <div v-if="advanceRecords.length === 0" class="py-12 text-center text-sm text-gray-400">
                    {{ t('employeeHistory.noAdvances') }}
                  </div>
                  <table v-else class="table w-full">
                    <thead>
                      <tr>
                        <th class="text-left">{{ t('employeeHistory.period') }}</th>
                        <th class="text-left">{{ t('employeeHistory.advanceDate') }}</th>
                        <th class="text-right">{{ t('amount') }}</th>
                        <th class="text-center">{{ t('status') }}</th>
                        <th class="text-left">{{ t('employeeHistory.glEntry') }}</th>
                        <th class="text-left">{{ t('notes') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="adv in advanceRecords" :key="adv.id" class="hover:bg-gray-50">
                        <td class="text-sm font-medium">{{ formatPeriod(adv.periodYear, adv.periodMonth) }}</td>
                        <td class="text-sm">{{ formatDate(adv.advanceDate) }}</td>
                        <td class="text-right text-sm font-medium text-blue-600">{{ formatCurrency(adv.amount) }}</td>
                        <td class="text-center">
                          <span :class="['badge', getAdvanceStatusClass(adv.status)]">
                            {{ t('enums.salaryStatus.' + adv.status) }}
                          </span>
                        </td>
                        <td>
                          <span v-if="adv.glJournalEntryId" class="font-mono text-xs bg-green-50 text-green-700 px-1.5 py-0.5 rounded">#{{ adv.glJournalEntryId }}</span>
                          <span v-else class="text-gray-400">—</span>
                        </td>
                        <td class="text-sm text-gray-500">{{ adv.notes || '—' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
