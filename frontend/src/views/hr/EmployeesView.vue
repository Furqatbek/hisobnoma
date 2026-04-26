<script setup>
import { ref, onMounted } from 'vue'
import { employeesApi } from '@/services/api'
import { PlusIcon, PencilSquareIcon, MagnifyingGlassIcon, NoSymbolIcon, TrashIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const employees = ref([])
const loading = ref(true)
const searchQuery = ref('')

async function loadEmployees() {
  loading.value = true
  try {
    const response = await employeesApi.getAll({ size: 100 })
    employees.value = response.data.content || response.data.data?.content || []
  } catch (error) {
    console.error('Failed to load employees:', error)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    await loadEmployees()
    return
  }
  loading.value = true
  try {
    const response = await employeesApi.search(searchQuery.value, { size: 100 })
    employees.value = response.data.content || response.data.data?.content || []
  } catch (error) {
    console.error('Failed to search:', error)
  } finally {
    loading.value = false
  }
}

function statusBadge(status) {
  switch (status) {
    case 'ACTIVE': return 'badge-success'
    case 'ON_LEAVE': return 'badge-warning'
    case 'TERMINATED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function statusLabel(status) {
  switch (status) {
    case 'ACTIVE': return t('active')
    case 'ON_LEAVE': return t('enums.employeeStatus.ON_LEAVE')
    case 'TERMINATED': return t('enums.employeeStatus.TERMINATED')
    default: return status
  }
}

async function handleTerminate(emp) {
  if (!confirm(t('hr.employees.confirmTerminate', { name: emp.fullName }))) return
  try {
    await employeesApi.terminate(emp.id)
    await loadEmployees()
  } catch (error) {
    alert(error.response?.data?.message || t('errorOccurred'))
  }
}

async function handleDelete(emp) {
  if (!confirm(t('hr.employees.confirmDelete', { name: emp.fullName }))) return
  try {
    await employeesApi.delete(emp.id)
    await loadEmployees()
  } catch (error) {
    alert(error.response?.data?.message || t('errorOccurred'))
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ').format(value || 0)
}

onMounted(loadEmployees)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('hr.employees.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('hr.employees.subtitle') }}</p>
      </div>
      <RouterLink to="/hr/employees/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('hr.employees.addEmployee') }}
      </RouterLink>
    </div>

    <div class="card">
      <div class="card-body">
        <div class="flex gap-3 mb-4">
          <div class="relative flex-1">
            <MagnifyingGlassIcon class="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
            <input
              v-model="searchQuery"
              @keyup.enter="handleSearch"
              type="text"
              :placeholder="$t('hr.employees.searchPlaceholder')"
              class="input pl-10"
            />
          </div>
          <button @click="handleSearch" class="btn-secondary">{{ $t('search') }}</button>
        </div>

        <div v-if="loading" class="flex items-center justify-center h-32">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>

        <div v-else-if="employees.length === 0" class="text-center py-12 text-gray-500">
          {{ $t('hr.employees.noEmployees') }}
        </div>

        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('code') }}</th>
                <th>{{ $t('hr.employees.employee') }}</th>
                <th>{{ $t('hr.employees.department') }}</th>
                <th>{{ $t('hr.employees.position') }}</th>
                <th>{{ $t('phone') }}</th>
                <th>{{ $t('hr.employees.baseSalary') }}</th>
                <th>{{ $t('status') }}</th>
                <th>{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="emp in employees" :key="emp.id">
                <td class="font-medium">{{ emp.employeeCode }}</td>
                <td>{{ emp.fullName }}</td>
                <td>{{ emp.departmentName || '-' }}</td>
                <td>{{ emp.positionName || '-' }}</td>
                <td>{{ emp.phone || '-' }}</td>
                <td>{{ formatCurrency(emp.currentSalary) }}</td>
                <td>
                  <span :class="['badge', statusBadge(emp.status)]">
                    {{ statusLabel(emp.status) }}
                  </span>
                </td>
                <td>
                  <div class="flex items-center gap-2">
                    <RouterLink :to="`/hr/employees/${emp.id}/edit`" class="text-primary-600 hover:text-primary-800" :title="$t('edit')">
                      <PencilSquareIcon class="h-5 w-5" />
                    </RouterLink>
                    <button
                      v-if="emp.status === 'ACTIVE' || emp.status === 'ON_LEAVE'"
                      @click="handleTerminate(emp)"
                      class="text-orange-600 hover:text-orange-800"
                      :title="$t('hr.employees.terminate')"
                    >
                      <NoSymbolIcon class="h-5 w-5" />
                    </button>
                    <button
                      v-if="emp.status === 'TERMINATED'"
                      @click="handleDelete(emp)"
                      class="text-red-600 hover:text-red-800"
                      :title="$t('delete')"
                    >
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
