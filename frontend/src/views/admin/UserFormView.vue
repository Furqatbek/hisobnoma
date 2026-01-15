<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usersApi } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  username: '',
  password: '',
  firstName: '',
  lastName: '',
  phone: '',
  enabled: true,
  roles: []
})

const errors = reactive({})

const availableRoles = [
  { code: 'ADMIN', name: 'Administrator' },
  { code: 'MANAGER', name: 'Manager' },
  { code: 'CASHIER', name: 'Cashier' },
  { code: 'INVENTORY', name: 'Inventory Staff' },
  { code: 'VIEWER', name: 'Viewer' }
]

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    try {
      const response = await usersApi.getById(route.params.id)
      const user = response.data
      form.username = user.username
      form.firstName = user.firstName || ''
      form.lastName = user.lastName || ''
      form.phone = user.phone || ''
      form.enabled = user.enabled
      form.roles = user.roles?.map(r => r.code || r) || []
    } catch (error) {
      console.error('Failed to load user:', error)
    } finally {
      loading.value = false
    }
  }
})

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])

  if (!form.username?.trim()) errors.username = 'Username is required'
  if (!isEdit.value && !form.password) errors.password = 'Password is required'
  if (!isEdit.value && form.password && form.password.length < 6) {
    errors.password = 'Password must be at least 6 characters'
  }

  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  // Clear previous errors
  Object.keys(errors).forEach(key => delete errors[key])

  try {
    const data = { ...form }
    if (isEdit.value && !data.password) {
      delete data.password
    }

    if (isEdit.value) {
      await usersApi.update(route.params.id, data)
    } else {
      await usersApi.create(data)
    }
    router.push('/admin/users')
  } catch (error) {
    const response = error.response?.data

    // Handle validation errors from backend
    if (response?.validationErrors && Array.isArray(response.validationErrors)) {
      response.validationErrors.forEach(err => {
        if (err.field) {
          errors[err.field] = err.message
        }
      })
      errors.general = 'Please fix the errors below'
    } else {
      errors.general = response?.message || 'Failed to save user'
    }
  } finally {
    saving.value = false
  }
}

function toggleRole(roleCode) {
  const index = form.roles.indexOf(roleCode)
  if (index > -1) {
    form.roles.splice(index, 1)
  } else {
    form.roles.push(roleCode)
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
        <h1 class="text-2xl font-bold text-gray-900">{{ isEdit ? 'Edit User' : 'New User' }}</h1>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <div v-if="errors.general" class="p-4 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600">{{ errors.general }}</p>
      </div>

      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">User Information</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">Username *</label>
            <input v-model="form.username" type="text" :class="[errors.username ? 'input-error' : 'input']" />
            <p v-if="errors.username" class="mt-1 text-sm text-red-600">{{ errors.username }}</p>
          </div>
          <div>
            <label class="label">{{ isEdit ? 'New Password (leave blank to keep)' : 'Password *' }}</label>
            <input v-model="form.password" type="password" :class="[errors.password ? 'input-error' : 'input']" />
            <p v-if="errors.password" class="mt-1 text-sm text-red-600">{{ errors.password }}</p>
          </div>
          <div>
            <label class="label">First Name</label>
            <input v-model="form.firstName" type="text" class="input" />
          </div>
          <div>
            <label class="label">Last Name</label>
            <input v-model="form.lastName" type="text" class="input" />
          </div>
          <div>
            <label class="label">Phone</label>
            <input v-model="form.phone" type="tel" :class="[errors.phone ? 'input-error' : 'input']" placeholder="+998901234567" />
            <p v-if="errors.phone" class="mt-1 text-sm text-red-600">{{ errors.phone }}</p>
          </div>
          <div class="flex items-center">
            <label class="flex items-center">
              <input v-model="form.enabled" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm">Account Enabled</span>
            </label>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">Roles</h3></div>
        <div class="card-body">
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
            <button
              v-for="role in availableRoles"
              :key="role.code"
              type="button"
              @click="toggleRole(role.code)"
              :class="[
                'p-4 border-2 rounded-lg text-center transition-colors',
                form.roles.includes(role.code)
                  ? 'border-primary-500 bg-primary-50 text-primary-700'
                  : 'border-gray-200 hover:border-gray-300'
              ]"
            >
              <p class="font-medium">{{ role.name }}</p>
              <p class="text-xs text-gray-500 mt-1">{{ role.code }}</p>
            </button>
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">Cancel</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Saving...' : (isEdit ? 'Update' : 'Create') }}
        </button>
      </div>
    </form>
  </div>
</template>
