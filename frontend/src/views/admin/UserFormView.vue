<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usersApi, rolesApi } from '@/services/api'
import { ArrowLeftIcon, LockClosedIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

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

const availableRoles = ref([])

// PIN management state
const userHasPin = ref(false)
const pinInput = ref('')
const pinSaving = ref(false)
const pinMessage = ref('')
const pinError = ref('')

const pinDots = computed(() => {
  const dots = []
  for (let i = 0; i < 4; i++) {
    dots.push(i < pinInput.value.length)
  }
  return dots
})

onMounted(async () => {
  loading.value = true
  try {
    // Fetch all available roles from API
    const rolesResponse = await rolesApi.getAll({ size: 100 })
    // Response structure: { data: { content: [...], totalElements, ... } }
    availableRoles.value = rolesResponse.data.data?.content || rolesResponse.data.data || []

    // If editing, also fetch user data
    if (isEdit.value) {
      const response = await usersApi.getById(route.params.id)
      const user = response.data.data
      form.username = user.username
      form.firstName = user.firstName || ''
      form.lastName = user.lastName || ''
      form.phone = user.phone || ''
      form.enabled = user.enabled
      form.roles = user.roles?.map(r => r.code || r) || []
      userHasPin.value = user.hasPin || false
    }
  } catch (error) {
    console.error('Failed to load data:', error)
  } finally {
    loading.value = false
  }
})

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])

  if (!form.username?.trim()) errors.username = t('admin.userForm.usernameRequired')
  if (!isEdit.value && !form.password) errors.password = t('admin.userForm.passwordRequired')
  if (!isEdit.value && form.password && form.password.length < 6) {
    errors.password = t('admin.userForm.passwordMinLength')
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
      errors.general = t('admin.userForm.fixErrors')
    } else {
      errors.general = response?.message || t('failedToSave')
    }
  } finally {
    saving.value = false
  }
}

function handlePinDigit(digit) {
  if (pinInput.value.length >= 4) return
  pinError.value = ''
  pinMessage.value = ''
  pinInput.value += digit
}

function handlePinBackspace() {
  pinInput.value = pinInput.value.slice(0, -1)
  pinError.value = ''
  pinMessage.value = ''
}

function handlePinClear() {
  pinInput.value = ''
  pinError.value = ''
  pinMessage.value = ''
}

async function savePin() {
  if (pinInput.value.length !== 4) {
    pinError.value = t('admin.userForm.pinMustBe4Digits')
    return
  }

  pinSaving.value = true
  pinError.value = ''
  pinMessage.value = ''
  try {
    await usersApi.setPin(route.params.id, pinInput.value)
    userHasPin.value = true
    pinInput.value = ''
    pinMessage.value = t('admin.userForm.pinSetSuccess')
  } catch (err) {
    pinError.value = err.response?.data?.message || t('admin.userForm.pinSetError')
  } finally {
    pinSaving.value = false
  }
}

async function clearUserPin() {
  pinSaving.value = true
  pinError.value = ''
  pinMessage.value = ''
  try {
    await usersApi.clearPin(route.params.id)
    userHasPin.value = false
    pinInput.value = ''
    pinMessage.value = t('admin.userForm.pinCleared')
  } catch (err) {
    pinError.value = err.response?.data?.message || t('admin.userForm.pinClearError')
  } finally {
    pinSaving.value = false
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
        <h1 class="text-2xl font-bold text-gray-900">{{ isEdit ? $t('admin.userForm.editUser') : $t('admin.userForm.newUser') }}</h1>
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
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('admin.userForm.userInformation') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('admin.userForm.username') }} *</label>
            <input v-model="form.username" type="text" :class="[errors.username ? 'input-error' : 'input']" />
            <p v-if="errors.username" class="mt-1 text-sm text-red-600">{{ errors.username }}</p>
          </div>
          <div>
            <label class="label">{{ isEdit ? $t('admin.userForm.newPassword') : $t('admin.userForm.password') + ' *' }}</label>
            <input v-model="form.password" type="password" :class="[errors.password ? 'input-error' : 'input']" />
            <p v-if="errors.password" class="mt-1 text-sm text-red-600">{{ errors.password }}</p>
          </div>
          <div>
            <label class="label">{{ $t('admin.userForm.firstName') }}</label>
            <input v-model="form.firstName" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('admin.userForm.lastName') }}</label>
            <input v-model="form.lastName" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('phone') }}</label>
            <input v-model="form.phone" type="tel" :class="[errors.phone ? 'input-error' : 'input']" placeholder="+998901234567" />
            <p v-if="errors.phone" class="mt-1 text-sm text-red-600">{{ errors.phone }}</p>
          </div>
          <div class="flex items-center">
            <label class="flex items-center">
              <input v-model="form.enabled" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm">{{ $t('admin.userForm.accountEnabled') }}</span>
            </label>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('admin.users.roles') }}</h3></div>
        <div class="card-body">
          <div v-if="availableRoles.length === 0" class="text-gray-500 text-center py-4">
            {{ $t('admin.userForm.rolesLoading') }}
          </div>
          <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <button
              v-for="role in availableRoles"
              :key="role.code"
              type="button"
              @click="toggleRole(role.code)"
              :class="[
                'p-4 border-2 rounded-lg text-left transition-colors',
                form.roles.includes(role.code)
                  ? 'border-primary-500 bg-primary-50 text-primary-700'
                  : 'border-gray-200 hover:border-gray-300'
              ]"
            >
              <p class="font-medium">{{ role.name }}</p>
              <p class="text-xs text-gray-500 mt-1">{{ role.code }}</p>
              <p v-if="role.description" class="text-xs text-gray-400 mt-1">{{ role.description }}</p>
              <span v-if="role.systemRole" class="inline-block mt-2 px-2 py-0.5 text-xs bg-blue-100 text-blue-700 rounded">{{ $t('admin.roles.system') }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- PIN Management (edit mode only) -->
      <div v-if="isEdit" class="card">
        <div class="card-header">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <LockClosedIcon class="h-5 w-5 text-gray-500" />
              <h3 class="text-lg font-medium">{{ $t('admin.userForm.pinCode') }}</h3>
            </div>
            <span v-if="userHasPin" class="px-2 py-1 text-xs font-medium bg-green-100 text-green-700 rounded-full">{{ $t('admin.userForm.pinSet') }}</span>
            <span v-else class="px-2 py-1 text-xs font-medium bg-gray-100 text-gray-500 rounded-full">{{ $t('admin.userForm.pinNotSet') }}</span>
          </div>
        </div>
        <div class="card-body">
          <p class="text-sm text-gray-500 mb-4">
            {{ $t('admin.userForm.pinDescription') }}
          </p>

          <!-- PIN success/error messages -->
          <div v-if="pinMessage" class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
            <p class="text-sm text-green-600 text-center">{{ pinMessage }}</p>
          </div>
          <div v-if="pinError" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-600 text-center">{{ pinError }}</p>
          </div>

          <!-- PIN dots -->
          <div class="flex items-center justify-center gap-4 mb-5">
            <div
              v-for="(filled, i) in pinDots"
              :key="i"
              :class="[
                'w-4 h-4 rounded-full transition-all duration-150',
                filled ? 'bg-primary-600 scale-110' : 'bg-gray-200'
              ]"
            ></div>
          </div>

          <!-- Numpad -->
          <div class="grid grid-cols-3 gap-2 max-w-[260px] mx-auto">
            <button
              v-for="digit in [1,2,3,4,5,6,7,8,9]"
              :key="digit"
              type="button"
              @click="handlePinDigit(String(digit))"
              :disabled="pinSaving"
              class="h-14 text-xl font-semibold text-gray-700 rounded-xl bg-gray-50 hover:bg-gray-100 active:bg-gray-200 active:scale-95 transition-all select-none disabled:opacity-50"
            >
              {{ digit }}
            </button>

            <button
              type="button"
              @click="handlePinClear"
              :disabled="pinSaving"
              class="h-14 text-xs font-medium text-red-500 rounded-xl bg-gray-50 hover:bg-red-50 active:bg-red-100 transition-all select-none disabled:opacity-50"
            >
              {{ $t('admin.userForm.clear') }}
            </button>
            <button
              type="button"
              @click="handlePinDigit('0')"
              :disabled="pinSaving"
              class="h-14 text-xl font-semibold text-gray-700 rounded-xl bg-gray-50 hover:bg-gray-100 active:bg-gray-200 active:scale-95 transition-all select-none disabled:opacity-50"
            >
              0
            </button>
            <button
              type="button"
              @click="handlePinBackspace"
              :disabled="pinSaving"
              class="h-14 text-lg text-gray-500 rounded-xl bg-gray-50 hover:bg-gray-100 active:bg-gray-200 transition-all select-none disabled:opacity-50 flex items-center justify-center"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9.75L14.25 12m0 0l2.25 2.25M14.25 12l2.25-2.25M14.25 12L12 14.25m-2.58 4.92l-6.374-6.375a1.125 1.125 0 010-1.59L9.42 4.83c.21-.211.497-.33.795-.33H19.5a2.25 2.25 0 012.25 2.25v10.5a2.25 2.25 0 01-2.25 2.25h-9.284c-.298 0-.585-.119-.795-.33z" />
              </svg>
            </button>
          </div>

          <!-- Save / Clear PIN buttons -->
          <div class="flex items-center justify-center gap-3 mt-5 pt-4 border-t border-gray-100">
            <button
              type="button"
              @click="savePin"
              :disabled="pinSaving || pinInput.length !== 4"
              class="btn-primary px-6"
            >
              {{ pinSaving ? $t('saving') : $t('admin.userForm.setPin') }}
            </button>
            <button
              v-if="userHasPin"
              type="button"
              @click="clearUserPin"
              :disabled="pinSaving"
              class="btn-secondary px-6 text-red-600 hover:text-red-700"
            >
              {{ $t('admin.userForm.clearPin') }}
            </button>
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">{{ $t('cancel') }}</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : (isEdit ? $t('admin.userForm.update') : $t('admin.userForm.create')) }}
        </button>
      </div>
    </form>
  </div>
</template>
