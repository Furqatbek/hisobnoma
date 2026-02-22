<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/services/api'
import { ArrowLeftIcon, LockClosedIcon, KeyIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const authStore = useAuthStore()

// State
const step = ref('users') // 'users' | 'pin' | 'password'
const users = ref([])
const selectedUser = ref(null)
const pin = ref('')
const loadingUsers = ref(false)
const errors = reactive({ general: '' })

// Password fallback
const passwordForm = reactive({ username: '', password: '' })
const showPassword = ref(false)

// Color palette for user avatars
const avatarColors = [
  'bg-blue-500', 'bg-emerald-500', 'bg-violet-500', 'bg-amber-500',
  'bg-rose-500', 'bg-cyan-500', 'bg-indigo-500', 'bg-teal-500',
  'bg-orange-500', 'bg-pink-500', 'bg-lime-600', 'bg-fuchsia-500'
]

function getAvatarColor(index) {
  return avatarColors[index % avatarColors.length]
}

const pinDots = computed(() => {
  const dots = []
  for (let i = 0; i < 4; i++) {
    dots.push(i < pin.value.length)
  }
  return dots
})

onMounted(async () => {
  loadingUsers.value = true
  try {
    const res = await authApi.getUsersList()
    users.value = res.data.data || res.data || []
  } catch (error) {
    console.error('Failed to load users:', error)
    // Fall back to password login if user list fails
    step.value = 'password'
  } finally {
    loadingUsers.value = false
  }
})

function selectUser(user) {
  selectedUser.value = user
  pin.value = ''
  errors.general = ''
  if (user.hasPin) {
    step.value = 'pin'
  } else {
    // No PIN set - go to password login prefilled
    passwordForm.username = user.username
    passwordForm.password = ''
    step.value = 'password'
  }
}

function goBack() {
  step.value = 'users'
  selectedUser.value = null
  pin.value = ''
  errors.general = ''
}

function switchToPassword() {
  passwordForm.username = selectedUser.value?.username || ''
  passwordForm.password = ''
  errors.general = ''
  step.value = 'password'
}

function switchToPin() {
  if (users.value.length > 0) {
    step.value = 'users'
    selectedUser.value = null
    pin.value = ''
    errors.general = ''
  }
}

async function handlePinDigit(digit) {
  if (pin.value.length >= 4) return
  errors.general = ''

  pin.value += digit

  // Auto-submit when 4 digits entered
  if (pin.value.length === 4) {
    const result = await authStore.pinLogin({
      username: selectedUser.value.username,
      pin: pin.value
    })
    if (!result.success) {
      errors.general = result.error
      pin.value = ''
    }
  }
}

function handlePinBackspace() {
  pin.value = pin.value.slice(0, -1)
  errors.general = ''
}

function handlePinClear() {
  pin.value = ''
  errors.general = ''
}

async function handlePasswordSubmit() {
  if (!passwordForm.username.trim() || !passwordForm.password) return

  const result = await authStore.login({
    username: passwordForm.username,
    password: passwordForm.password,
    rememberMe: false
  })
  if (!result.success) {
    errors.general = result.error
  }
}
</script>

<template>
  <div class="card max-w-md mx-auto" :class="{ 'max-w-2xl': step === 'users' }">
    <div class="card-body">
      <!-- Logo -->
      <div class="text-center mb-6">
        <div class="inline-flex items-center justify-center w-14 h-14 bg-primary-600 rounded-2xl mb-3">
          <span class="text-white font-bold text-2xl">H</span>
        </div>
        <h2 class="text-xl font-bold text-gray-900">{{ $t('appName') }}</h2>
      </div>

      <!-- Error message -->
      <div v-if="errors.general" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600 text-center">{{ errors.general }}</p>
      </div>

      <!-- ==================== STEP 1: User Selection ==================== -->
      <div v-if="step === 'users'">
        <p class="text-center text-sm text-gray-500 mb-5">{{ $t('auth.selectUser') }}</p>

        <div v-if="loadingUsers" class="flex items-center justify-center py-12">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600"></div>
        </div>

        <div v-else-if="users.length === 0" class="text-center py-8 text-gray-500 text-sm">
          {{ $t('auth.noUsersFound') }}
        </div>

        <div v-else class="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">
          <button
            v-for="(u, idx) in users"
            :key="u.id"
            @click="selectUser(u)"
            class="flex flex-col items-center gap-2 p-4 rounded-xl border-2 border-gray-100 hover:border-primary-300 hover:bg-primary-50/50 transition-all active:scale-95"
          >
            <div :class="['w-14 h-14 rounded-full flex items-center justify-center text-white font-bold text-lg', getAvatarColor(idx)]">
              {{ u.initials }}
            </div>
            <div class="text-center">
              <p class="text-sm font-medium text-gray-900 truncate max-w-[120px]">{{ u.fullName }}</p>
              <div class="flex items-center justify-center gap-1 mt-0.5">
                <LockClosedIcon v-if="u.hasPin" class="h-3 w-3 text-green-500" />
                <KeyIcon v-else class="h-3 w-3 text-gray-400" />
                <span class="text-[10px] text-gray-400">{{ u.hasPin ? 'PIN' : $t('auth.password') }}</span>
              </div>
            </div>
          </button>
        </div>

        <!-- Switch to password -->
        <div class="text-center pt-2 border-t border-gray-100">
          <button
            @click="step = 'password'; passwordForm.username = ''; passwordForm.password = ''"
            class="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            {{ $t('auth.loginWithPassword') }}
          </button>
        </div>
      </div>

      <!-- ==================== STEP 2: PIN Entry ==================== -->
      <div v-else-if="step === 'pin'">
        <!-- Back + User info -->
        <div class="flex items-center mb-5">
          <button @click="goBack" class="p-2 -ml-2 hover:bg-gray-100 rounded-lg">
            <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
          </button>
          <div class="flex-1 text-center">
            <p class="font-semibold text-gray-900">{{ selectedUser.fullName }}</p>
            <p class="text-xs text-gray-500">{{ $t('auth.enterPin') }}</p>
          </div>
          <div class="w-9"></div>
        </div>

        <!-- PIN dots -->
        <div class="flex items-center justify-center gap-4 mb-6">
          <div
            v-for="(filled, i) in pinDots"
            :key="i"
            :class="[
              'w-4 h-4 rounded-full transition-all duration-150',
              filled ? 'bg-primary-600 scale-110' : 'bg-gray-200'
            ]"
          ></div>
        </div>

        <!-- Loading indicator -->
        <div v-if="authStore.loading" class="flex justify-center mb-4">
          <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
        </div>

        <!-- Numpad -->
        <div class="grid grid-cols-3 gap-2 max-w-[280px] mx-auto">
          <button
            v-for="digit in [1,2,3,4,5,6,7,8,9]"
            :key="digit"
            @click="handlePinDigit(String(digit))"
            :disabled="authStore.loading"
            class="h-16 text-2xl font-semibold text-gray-700 rounded-xl bg-gray-50 hover:bg-gray-100 active:bg-gray-200 active:scale-95 transition-all select-none disabled:opacity-50"
          >
            {{ digit }}
          </button>

          <!-- Bottom row: Clear, 0, Backspace -->
          <button
            @click="handlePinClear"
            :disabled="authStore.loading"
            class="h-16 text-sm font-medium text-red-500 rounded-xl bg-gray-50 hover:bg-red-50 active:bg-red-100 transition-all select-none disabled:opacity-50"
          >
            {{ $t('clear') }}
          </button>
          <button
            @click="handlePinDigit('0')"
            :disabled="authStore.loading"
            class="h-16 text-2xl font-semibold text-gray-700 rounded-xl bg-gray-50 hover:bg-gray-100 active:bg-gray-200 active:scale-95 transition-all select-none disabled:opacity-50"
          >
            0
          </button>
          <button
            @click="handlePinBackspace"
            :disabled="authStore.loading"
            class="h-16 text-xl text-gray-500 rounded-xl bg-gray-50 hover:bg-gray-100 active:bg-gray-200 transition-all select-none disabled:opacity-50 flex items-center justify-center"
          >
            <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9.75L14.25 12m0 0l2.25 2.25M14.25 12l2.25-2.25M14.25 12L12 14.25m-2.58 4.92l-6.374-6.375a1.125 1.125 0 010-1.59L9.42 4.83c.21-.211.497-.33.795-.33H19.5a2.25 2.25 0 012.25 2.25v10.5a2.25 2.25 0 01-2.25 2.25h-9.284c-.298 0-.585-.119-.795-.33z" />
            </svg>
          </button>
        </div>

        <!-- Switch to password -->
        <div class="text-center mt-5 pt-3 border-t border-gray-100">
          <button
            @click="switchToPassword"
            class="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            {{ $t('auth.loginWithPassword') }}
          </button>
        </div>
      </div>

      <!-- ==================== STEP 3: Password Login (fallback) ==================== -->
      <div v-else-if="step === 'password'">
        <!-- Back button (if users available) -->
        <div v-if="users.length > 0" class="flex items-center mb-4">
          <button @click="switchToPin" class="p-2 -ml-2 hover:bg-gray-100 rounded-lg">
            <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
          </button>
          <div class="flex-1 text-center">
            <p class="text-sm text-gray-500">{{ $t('auth.loginWithPassword') }}</p>
          </div>
          <div class="w-9"></div>
        </div>

        <form @submit.prevent="handlePasswordSubmit" class="space-y-4">
          <div>
            <label for="username" class="label">{{ $t('username') }}</label>
            <input
              id="username"
              v-model="passwordForm.username"
              type="text"
              autocomplete="username"
              class="input text-lg py-3"
              :placeholder="$t('username')"
            />
          </div>

          <div>
            <label for="password" class="label">{{ $t('auth.password') }}</label>
            <div class="relative">
              <input
                id="password"
                v-model="passwordForm.password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                class="input text-lg py-3 pr-12"
                :placeholder="$t('auth.yourPassword')"
              />
              <button
                type="button"
                class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-500"
                @click="showPassword = !showPassword"
              >
                <svg v-if="showPassword" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                </svg>
                <svg v-else class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div>
          </div>

          <button
            type="submit"
            :disabled="authStore.loading"
            class="btn-primary w-full py-3 text-lg"
          >
            <svg
              v-if="authStore.loading"
              class="animate-spin -ml-1 mr-2 h-5 w-5 text-white"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            {{ authStore.loading ? $t('auth.signingIn') : $t('auth.login') }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
