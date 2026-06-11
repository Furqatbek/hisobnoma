<script setup>
import { formatCurrency } from '@/utils/format'
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { reportsApi, unwrapData } from '@/services/api'
import { BanknotesIcon, CheckCircleIcon, ClockIcon, XCircleIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const loading = ref(true)
const report = ref(null)

const now = new Date()
const filterYear = ref(now.getFullYear())
const filterMonth = ref(now.getMonth() + 1)

function statusBadge(status) {
  switch (status) {
    case 'PAID': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'CANCELLED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function statusLabel(status) {
  const key = `enums.salaryStatus.${status}`
  const translated = t(key)
  return translated !== key ? translated : status
}

async function fetchReport() {
  loading.value = true
  try {
    const res = await reportsApi.getSalaryReport(filterYear.value, filterMonth.value)
    report.value = unwrapData(res)
  } catch (error) {
    console.error('Failed to fetch salary report:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchReport)
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('reports.salary.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('reports.salary.subtitle') }}</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-wrap gap-4 items-end">
        <div>
          <label class="label">{{ $t('year') }}</label>
          <input v-model.number="filterYear" type="number" min="2020" max="2030" class="input w-28" />
        </div>
        <div>
          <label class="label">{{ $t('month') }}</label>
          <select v-model.number="filterMonth" class="input w-40">
            <option v-for="m in 12" :key="m" :value="m">{{ $t(`months.${m}`) }}</option>
          </select>
        </div>
        <button @click="fetchReport" class="btn-primary">{{ $t('apply') }}</button>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else-if="report">
      <!-- Summary Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.salary.totalSalary') }}</p>
                <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(report.summary?.totalNet) }}</p>
              </div>
              <BanknotesIcon class="h-8 w-8 text-primary-400" />
            </div>
            <p class="text-sm text-gray-500 mt-2">{{ report.summary?.totalEmployees || 0 }} {{ $t('items') }} {{ $t('reports.salary.employee').toLowerCase?.() || '' }}</p>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.salary.totalPaid') }}</p>
                <p class="text-2xl font-bold text-green-600">{{ report.summary?.paidCount || 0 }}</p>
              </div>
              <CheckCircleIcon class="h-8 w-8 text-green-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.salary.totalPending') }}</p>
                <p class="text-2xl font-bold text-yellow-600">{{ report.summary?.pendingCount || 0 }}</p>
              </div>
              <ClockIcon class="h-8 w-8 text-yellow-400" />
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('reports.salary.baseSalary') }}</p>
                <p class="text-2xl font-bold text-gray-700">{{ formatCurrency(report.summary?.totalBase) }}</p>
              </div>
            </div>
            <div class="text-sm text-gray-500 mt-2">
              <span class="text-green-600">+{{ formatCurrency(report.summary?.totalBonus) }}</span> {{ $t('reports.salary.bonusTotal') }},
              <span class="text-red-600">-{{ formatCurrency(report.summary?.totalDeductions) }}</span> {{ $t('reports.salary.deductionTotal') }}
            </div>
          </div>
        </div>
      </div>

      <!-- Department Breakdown -->
      <div v-if="report.byDepartment?.length" class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('reports.salary.byDepartment') }}</h3>
        </div>
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('reports.salary.department') }}</th>
                <th class="text-right">{{ $t('reports.salary.employeesCount') }}</th>
                <th class="text-right">{{ $t('reports.salary.baseSalary') }}</th>
                <th class="text-right">{{ $t('reports.salary.totalPayment') }}</th>
                <th class="text-right">{{ $t('reports.salary.share') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="dept in report.byDepartment" :key="dept.departmentName">
                <td class="font-medium">{{ dept.departmentName }}</td>
                <td class="text-right">{{ dept.employeeCount }}</td>
                <td class="text-right">{{ formatCurrency(dept.totalBase) }}</td>
                <td class="text-right font-semibold">{{ formatCurrency(dept.totalNet) }}</td>
                <td class="text-right">
                  <div class="flex items-center justify-end gap-2">
                    <div class="w-16 bg-gray-200 rounded-full h-2">
                      <div class="bg-primary-600 h-2 rounded-full" :style="{ width: dept.percentOfTotal + '%' }"></div>
                    </div>
                    <span class="text-sm">{{ dept.percentOfTotal }}%</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Employee Details -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('reports.salary.employeeDetails') }}</h3>
        </div>
        <div v-if="report.employees?.length" class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('reports.salary.employee') }}</th>
                <th>{{ $t('reports.salary.department') }}</th>
                <th class="text-right">{{ $t('reports.salary.base') }}</th>
                <th class="text-right">{{ $t('reports.salary.bonus') }}</th>
                <th class="text-right">{{ $t('reports.salary.deduction') }}</th>
                <th class="text-right">{{ $t('total') }}</th>
                <th>{{ $t('status') }}</th>
                <th>{{ $t('reports.salary.financeStatus') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="emp in report.employees" :key="emp.employeeId">
                <td>
                  <span class="font-medium">{{ emp.employeeName }}</span>
                  <span class="text-xs text-gray-400 ml-2">{{ emp.employeeCode }}</span>
                </td>
                <td class="text-sm text-gray-500">{{ emp.departmentName }}</td>
                <td class="text-right">{{ formatCurrency(emp.baseAmount) }}</td>
                <td class="text-right text-green-600">+{{ formatCurrency(emp.bonusAmount) }}</td>
                <td class="text-right text-red-600">-{{ formatCurrency(emp.deductionAmount) }}</td>
                <td class="text-right font-semibold">{{ formatCurrency(emp.netAmount) }}</td>
                <td>
                  <span :class="['badge', statusBadge(emp.status)]">{{ statusLabel(emp.status) }}</span>
                </td>
                <td>
                  <span v-if="emp.glJournalEntryId" class="text-green-600 text-sm" :title="$t('reports.salary.recorded')">
                    <CheckCircleIcon class="h-4 w-4 inline" /> {{ $t('reports.salary.recorded') }}
                  </span>
                  <span v-else-if="emp.status === 'PAID'" class="text-red-500 text-sm">
                    <XCircleIcon class="h-4 w-4 inline" /> {{ $t('reports.salary.notRecorded') }}
                  </span>
                  <span v-else class="text-gray-400 text-sm">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="card-body text-center text-gray-500">
          {{ $t('reports.salary.noRecords') }}
        </div>
      </div>
    </template>
  </div>
</template>
