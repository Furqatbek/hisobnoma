<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, onMounted } from 'vue'
import { departmentsApi, employeesApi, unwrapData, unwrapList } from '@/services/api'
import { PlusIcon, PencilSquareIcon, TrashIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()

const departments = ref([])
const employees = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingId = ref(null)
const saving = ref(false)

const form = reactive({ code: '', name: '', description: '', parentId: null, managerId: null, active: true })

async function loadData() {
  loading.value = true
  try {
    const [deptRes, empRes] = await Promise.all([
      departmentsApi.getAll(),
      employeesApi.getActive()
    ])
    departments.value = unwrapList(deptRes)
    employees.value = unwrapList(empRes)
  } catch (error) {
    console.error('Failed to load:', error)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', description: '', parentId: null, managerId: null, active: true })
  showModal.value = true
}

async function openEdit(dept) {
  editingId.value = dept.id
  Object.assign(form, dept)
  showModal.value = true
  try {
    const response = await departmentsApi.getById(dept.id)
    const freshData = unwrapData(response)
    Object.assign(form, freshData)
  } catch (error) {
    console.error('Failed to fetch department details:', error)
  }
}

async function handleSave() {
  if (!form.code || !form.name) return
  saving.value = true
  try {
    if (editingId.value) {
      await departmentsApi.update(editingId.value, form)
    } else {
      await departmentsApi.create(form)
    }
    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('Failed to save:', error)
    toast.error(error.response?.data?.message || t('noData'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  if (!confirm(t('hr.departments.confirmDelete'))) return
  try {
    await departmentsApi.delete(id)
    await loadData()
  } catch (error) {
    toast.error(error.response?.data?.message || t('noData'))
  }
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('hr.departments.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('hr.departments.subtitle') }}</p>
      </div>
      <button @click="openCreate" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" /> {{ $t('hr.departments.addDepartment') }}
      </button>
    </div>

    <div class="card">
      <div class="card-body">
        <div v-if="loading" class="flex items-center justify-center h-32">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="departments.length === 0" class="text-center py-12 text-gray-500">
          {{ $t('hr.departments.noDepartments') }}
        </div>
        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('code') }}</th>
                <th>{{ $t('name') }}</th>
                <th>{{ $t('description') }}</th>
                <th>{{ $t('hr.departments.head') }}</th>
                <th>{{ $t('status') }}</th>
                <th>{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="dept in departments" :key="dept.id">
                <td class="font-medium">{{ dept.code }}</td>
                <td>{{ dept.name }}</td>
                <td class="text-gray-500">{{ dept.description || '-' }}</td>
                <td>{{ dept.managerName || '-' }}</td>
                <td>
                  <span :class="['badge', dept.active ? 'badge-success' : 'badge-danger']">
                    {{ dept.active ? $t('active') : $t('inactive') }}
                  </span>
                </td>
                <td>
                  <div class="flex items-center gap-2">
                    <button @click="openEdit(dept)" class="text-primary-600 hover:text-primary-800">
                      <PencilSquareIcon class="h-5 w-5" />
                    </button>
                    <button @click="handleDelete(dept.id)" class="text-red-600 hover:text-red-800">
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

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-lg font-medium">{{ editingId ? $t('hr.departments.editDepartment') : $t('hr.departments.newDepartment') }}</h3>
          <button @click="showModal = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
        </div>
        <form @submit.prevent="handleSave" class="p-6 space-y-4">
          <div>
            <label class="label">{{ $t('code') }} *</label>
            <input v-model="form.code" type="text" class="input" :disabled="!!editingId" />
          </div>
          <div>
            <label class="label">{{ $t('name') }} *</label>
            <input v-model="form.name" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('description') }}</label>
            <textarea v-model="form.description" rows="2" class="input"></textarea>
          </div>
          <div>
            <label class="label">{{ $t('hr.departments.title') }}</label>
            <select v-model="form.parentId" class="input">
              <option :value="null">{{ $t('no') }}</option>
              <option v-for="d in departments.filter(d => d.id !== editingId)" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('hr.departments.head') }}</label>
            <select v-model="form.managerId" class="input">
              <option :value="null">{{ $t('hr.employeeForm.selectDepartment') }}</option>
              <option v-for="e in employees" :key="e.id" :value="e.id">{{ e.fullName }}</option>
            </select>
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
  </div>
</template>
