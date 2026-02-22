<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { employeesApi, departmentsApi, positionsApi, usersApi } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)

const departments = ref([])
const positions = ref([])
const users = ref([])

const form = reactive({
  employeeCode: '',
  firstName: '',
  lastName: '',
  middleName: '',
  phone: '',
  email: '',
  dateOfBirth: null,
  gender: '',
  address: '',
  hireDate: new Date().toISOString().split('T')[0],
  departmentId: null,
  positionId: null,
  userId: null,
  currentSalary: 0,
  notes: ''
})

const errors = reactive({})

onMounted(async () => {
  loading.value = true
  try {
    const [deptRes, posRes, usersRes] = await Promise.all([
      departmentsApi.getAll(),
      positionsApi.getAll(),
      usersApi.getAll({ size: 200 })
    ])
    departments.value = deptRes.data.data || deptRes.data || []
    positions.value = posRes.data.data || posRes.data || []
    users.value = usersRes.data.data?.content || usersRes.data.content || []

    if (isEdit.value) {
      const response = await employeesApi.getById(route.params.id)
      const data = response.data.data || response.data
      Object.assign(form, data)
    }
  } catch (error) {
    console.error('Failed to load data:', error)
  } finally {
    loading.value = false
  }
})

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.employeeCode?.trim()) errors.employeeCode = t('hr.employeeForm.nameRequired')
  if (!form.firstName?.trim()) errors.firstName = t('hr.employeeForm.nameRequired')
  if (!form.lastName?.trim()) errors.lastName = t('hr.employeeForm.lastNameRequired')
  if (!form.hireDate) errors.hireDate = t('hr.employeeForm.nameRequired')
  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  try {
    if (isEdit.value) {
      await employeesApi.update(route.params.id, form)
    } else {
      await employeesApi.create(form)
    }
    router.push('/hr/employees')
  } catch (error) {
    console.error('Failed to save:', error)
    if (error.response?.data?.message) {
      errors.general = error.response.data.message
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? $t('hr.employeeForm.editEmployee') : $t('hr.employeeForm.newEmployee') }}
        </h1>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <div v-if="errors.general" class="p-4 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600">{{ errors.general }}</p>
      </div>

      <!-- Personal Info -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('hr.employeeForm.personalInfo') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label class="label">{{ $t('hr.employeeForm.employeeCode') }} *</label>
            <input v-model="form.employeeCode" type="text" :class="[errors.employeeCode ? 'input-error' : 'input']" :disabled="isEdit" />
            <p v-if="errors.employeeCode" class="mt-1 text-sm text-red-600">{{ errors.employeeCode }}</p>
          </div>
          <div>
            <label class="label">{{ $t('hr.employeeForm.lastName') }} *</label>
            <input v-model="form.lastName" type="text" :class="[errors.lastName ? 'input-error' : 'input']" />
          </div>
          <div>
            <label class="label">{{ $t('hr.employeeForm.firstName') }} *</label>
            <input v-model="form.firstName" type="text" :class="[errors.firstName ? 'input-error' : 'input']" />
          </div>
          <div>
            <label class="label">{{ $t('hr.employeeForm.middleName') }}</label>
            <input v-model="form.middleName" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('phone') }}</label>
            <input v-model="form.phone" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('email') }}</label>
            <input v-model="form.email" type="email" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('hr.employeeForm.birthDate') }}</label>
            <input v-model="form.dateOfBirth" type="date" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('hr.employeeForm.gender') }}</label>
            <select v-model="form.gender" class="input">
              <option value="">{{ $t('hr.employeeForm.selectGender') }}</option>
              <option value="MALE">{{ $t('hr.employeeForm.male') }}</option>
              <option value="FEMALE">{{ $t('hr.employeeForm.female') }}</option>
            </select>
          </div>
          <div class="md:col-span-3">
            <label class="label">{{ $t('address') }}</label>
            <textarea v-model="form.address" rows="2" class="input"></textarea>
          </div>
        </div>
      </div>

      <!-- Work Info -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('hr.employeeForm.employmentInfo') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label class="label">{{ $t('hr.employees.hireDate') }} *</label>
            <input v-model="form.hireDate" type="date" :class="[errors.hireDate ? 'input-error' : 'input']" />
          </div>
          <div>
            <label class="label">{{ $t('hr.employees.department') }}</label>
            <select v-model="form.departmentId" class="input">
              <option :value="null">{{ $t('hr.employeeForm.selectDepartment') }}</option>
              <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('hr.employees.position') }}</label>
            <select v-model="form.positionId" class="input">
              <option :value="null">{{ $t('hr.employeeForm.selectPosition') }}</option>
              <option v-for="p in positions" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('hr.employees.baseSalary') }}</label>
            <input v-model.number="form.currentSalary" type="number" step="1000" min="0" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('hr.employeeForm.contactInfo') }}</label>
            <select v-model="form.userId" class="input">
              <option :value="null">{{ $t('noData') }}</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.username }} ({{ u.firstName }} {{ u.lastName }})</option>
            </select>
          </div>
          <div class="md:col-span-3">
            <label class="label">{{ $t('notes') }}</label>
            <textarea v-model="form.notes" rows="2" class="input"></textarea>
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">{{ $t('cancel') }}</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : $t('save') }}
        </button>
      </div>
    </form>
  </div>
</template>
