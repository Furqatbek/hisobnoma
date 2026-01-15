<script setup>
import { ref, reactive } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'

const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})

const showPassword = ref(false)
const errors = reactive({
  username: '',
  password: '',
  general: ''
})

function validate() {
  let isValid = true
  errors.username = ''
  errors.password = ''
  errors.general = ''

  if (!form.username.trim()) {
    errors.username = 'Username is required'
    isValid = false
  }

  if (!form.password) {
    errors.password = 'Password is required'
    isValid = false
  } else if (form.password.length < 6) {
    errors.password = 'Password must be at least 6 characters'
    isValid = false
  }

  return isValid
}

async function handleSubmit() {
  if (!validate()) return

  const result = await authStore.login({
    username: form.username,
    password: form.password
  })

  if (!result.success) {
    errors.general = result.error
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
        <h2 class="text-2xl font-bold text-gray-900">Welcome back</h2>
        <p class="mt-2 text-sm text-gray-600">Sign in to your account</p>
      </div>

      <!-- Error message -->
      <div
        v-if="errors.general"
        class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg"
      >
        <p class="text-sm text-red-600">{{ errors.general }}</p>
      </div>

      <!-- Form -->
      <form @submit.prevent="handleSubmit" class="space-y-5">
        <!-- Username -->
        <div>
          <label for="username" class="label">Username</label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            autocomplete="username"
            :class="[errors.username ? 'input-error' : 'input']"
            placeholder="Enter your username"
          />
          <p v-if="errors.username" class="mt-1 text-sm text-red-600">
            {{ errors.username }}
          </p>
        </div>

        <!-- Password -->
        <div>
          <label for="password" class="label">Password</label>
          <div class="relative">
            <input
              id="password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              :class="[errors.password ? 'input-error' : 'input', 'pr-10']"
              placeholder="Enter your password"
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
          <p v-if="errors.password" class="mt-1 text-sm text-red-600">
            {{ errors.password }}
          </p>
        </div>

        <!-- Remember me -->
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <input
              id="remember"
              type="checkbox"
              class="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
            />
            <label for="remember" class="ml-2 block text-sm text-gray-700">
              Remember me
            </label>
          </div>
          <RouterLink to="/forgot-password" class="text-sm font-medium text-primary-600 hover:text-primary-500">
            Forgot password?
          </RouterLink>
        </div>

        <!-- Submit -->
        <button
          type="submit"
          :disabled="authStore.loading"
          class="btn-primary w-full py-2.5"
        >
          <svg
            v-if="authStore.loading"
            class="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              class="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              stroke-width="4"
            />
            <path
              class="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
          {{ authStore.loading ? 'Signing in...' : 'Sign in' }}
        </button>
      </form>
    </div>
  </div>
</template>
