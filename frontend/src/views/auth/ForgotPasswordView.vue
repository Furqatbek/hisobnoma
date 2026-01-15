<script setup>
import { ref, reactive } from 'vue'
import { RouterLink } from 'vue-router'
import { authApi } from '@/services/api'
import { ArrowLeftIcon, PhoneIcon } from '@heroicons/vue/24/outline'

const loading = ref(false)
const success = ref(false)
const form = reactive({
  phone: ''
})
const errors = reactive({
  phone: '',
  general: ''
})

function validate() {
  errors.phone = ''
  errors.general = ''

  if (!form.phone.trim()) {
    errors.phone = 'Phone number is required'
    return false
  }

  // Basic phone validation
  const phoneRegex = /^\+?[0-9]{9,15}$/
  if (!phoneRegex.test(form.phone.replace(/\s/g, ''))) {
    errors.phone = 'Please enter a valid phone number'
    return false
  }

  return true
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  errors.general = ''

  try {
    await authApi.forgotPassword(form.phone)
    success.value = true
  } catch (error) {
    const response = error.response?.data
    errors.general = response?.message || 'Failed to process request. Please try again.'
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
        <h2 class="text-2xl font-bold text-gray-900">Forgot Password</h2>
        <p class="mt-2 text-sm text-gray-600">Enter your phone number to receive a reset code</p>
      </div>

      <!-- Success message -->
      <div v-if="success" class="text-center">
        <div class="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
          <p class="text-sm text-green-700">
            If an account exists with this phone number, you will receive a reset code via SMS.
          </p>
        </div>
        <RouterLink to="/reset-password" class="btn-primary inline-block">
          Enter Reset Code
        </RouterLink>
        <div class="mt-4">
          <RouterLink to="/login" class="text-sm font-medium text-primary-600 hover:text-primary-500">
            Back to login
          </RouterLink>
        </div>
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
          <div class="relative">
            <PhoneIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              id="phone"
              v-model="form.phone"
              type="tel"
              :class="[errors.phone ? 'input-error' : 'input', 'pl-10']"
              placeholder="+998901234567"
            />
          </div>
          <p v-if="errors.phone" class="mt-1 text-sm text-red-600">{{ errors.phone }}</p>
        </div>

        <!-- Submit -->
        <button type="submit" :disabled="loading" class="btn-primary w-full py-2.5">
          <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          {{ loading ? 'Sending...' : 'Send Reset Code' }}
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
