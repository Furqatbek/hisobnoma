<script setup>
import { ref, reactive } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { authApi } from '@/services/api'
import { ArrowLeftIcon, EyeIcon, EyeSlashIcon, KeyIcon } from '@heroicons/vue/24/outline'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const success = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const form = reactive({
  token: route.query.token || '',
  phone: route.query.phone || '',
  newPassword: '',
  confirmPassword: ''
})

const errors = reactive({
  token: '',
  phone: '',
  newPassword: '',
  confirmPassword: '',
  general: ''
})

function validate() {
  Object.keys(errors).forEach(key => errors[key] = '')

  let isValid = true

  if (!form.token.trim()) {
    errors.token = 'Reset code is required'
    isValid = false
  }

  if (!form.phone.trim()) {
    errors.phone = 'Phone number is required'
    isValid = false
  }

  if (!form.newPassword) {
    errors.newPassword = 'New password is required'
    isValid = false
  } else if (form.newPassword.length < 8) {
    errors.newPassword = 'Password must be at least 8 characters'
    isValid = false
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = 'Please confirm your password'
    isValid = false
  } else if (form.newPassword !== form.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match'
    isValid = false
  }

  return isValid
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  errors.general = ''

  try {
    await authApi.resetPassword({
      token: form.token,
      phone: form.phone,
      newPassword: form.newPassword
    })
    success.value = true
    // Redirect to login after 3 seconds
    setTimeout(() => {
      router.push('/login')
    }, 3000)
  } catch (error) {
    const response = error.response?.data
    if (response?.validationErrors && Array.isArray(response.validationErrors)) {
      response.validationErrors.forEach(err => {
        if (err.field && errors.hasOwnProperty(err.field)) {
          errors[err.field] = err.message
        }
      })
      errors.general = 'Please fix the errors below'
    } else {
      errors.general = response?.message || 'Failed to reset password. The code may be invalid or expired.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="card">
    <div class="card-body">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-primary-600 rounded-2xl mb-4">
          <span class="text-white font-bold text-3xl">H</span>
        </div>
        <h2 class="text-2xl font-bold text-gray-900">Reset Password</h2>
        <p class="mt-2 text-sm text-gray-600">Enter your reset code and new password</p>
      </div>

      <!-- Success message -->
      <div v-if="success" class="text-center">
        <div class="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
          <p class="text-sm text-green-700">
            Your password has been reset successfully! Redirecting to login...
          </p>
        </div>
        <RouterLink to="/login" class="btn-primary inline-block">
          Go to Login
        </RouterLink>
      </div>

      <!-- Form -->
      <form v-else @submit.prevent="handleSubmit" class="space-y-5">
        <!-- Error message -->
        <div v-if="errors.general" class="p-4 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-sm text-red-600">{{ errors.general }}</p>
        </div>

        <!-- Phone -->
        <div>
          <label for="phone" class="label">Phone Number</label>
          <input
            id="phone"
            v-model="form.phone"
            type="tel"
            :class="[errors.phone ? 'input-error' : 'input']"
            placeholder="+998901234567"
          />
          <p v-if="errors.phone" class="mt-1 text-sm text-red-600">{{ errors.phone }}</p>
        </div>

        <!-- Reset Code -->
        <div>
          <label for="token" class="label">Reset Code</label>
          <div class="relative">
            <KeyIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              id="token"
              v-model="form.token"
              type="text"
              :class="[errors.token ? 'input-error' : 'input', 'pl-10']"
              placeholder="Enter the code from SMS"
            />
          </div>
          <p v-if="errors.token" class="mt-1 text-sm text-red-600">{{ errors.token }}</p>
        </div>

        <!-- New Password -->
        <div>
          <label for="newPassword" class="label">New Password</label>
          <div class="relative">
            <input
              id="newPassword"
              v-model="form.newPassword"
              :type="showPassword ? 'text' : 'password'"
              :class="[errors.newPassword ? 'input-error' : 'input', 'pr-10']"
              placeholder="Enter new password"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-500"
              @click="showPassword = !showPassword"
            >
              <EyeSlashIcon v-if="showPassword" class="h-5 w-5" />
              <EyeIcon v-else class="h-5 w-5" />
            </button>
          </div>
          <p v-if="errors.newPassword" class="mt-1 text-sm text-red-600">{{ errors.newPassword }}</p>
        </div>

        <!-- Confirm Password -->
        <div>
          <label for="confirmPassword" class="label">Confirm Password</label>
          <div class="relative">
            <input
              id="confirmPassword"
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              :class="[errors.confirmPassword ? 'input-error' : 'input', 'pr-10']"
              placeholder="Confirm new password"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-500"
              @click="showConfirmPassword = !showConfirmPassword"
            >
              <EyeSlashIcon v-if="showConfirmPassword" class="h-5 w-5" />
              <EyeIcon v-else class="h-5 w-5" />
            </button>
          </div>
          <p v-if="errors.confirmPassword" class="mt-1 text-sm text-red-600">{{ errors.confirmPassword }}</p>
        </div>

        <!-- Submit -->
        <button type="submit" :disabled="loading" class="btn-primary w-full py-2.5">
          <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          {{ loading ? 'Resetting...' : 'Reset Password' }}
        </button>

        <!-- Back to login -->
        <div class="text-center">
          <RouterLink to="/login" class="inline-flex items-center text-sm font-medium text-primary-600 hover:text-primary-500">
            <ArrowLeftIcon class="h-4 w-4 mr-1" />
            Back to login
          </RouterLink>
        </div>
      </form>
    </div>
  </div>
</template>
