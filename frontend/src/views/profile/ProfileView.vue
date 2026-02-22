<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/services/api'
import {
  UserCircleIcon,
  KeyIcon,
  ShieldCheckIcon,
  CalendarIcon,
  PhoneIcon,
  EyeIcon,
  EyeSlashIcon,
  CheckCircleIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const authStore = useAuthStore()
const loading = ref(true)
const profile = ref(null)

// Change password
const showChangePassword = ref(false)
const changingPassword = ref(false)
const passwordSuccess = ref(false)
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordErrors = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
  general: ''
})

async function fetchProfile() {
  loading.value = true
  try {
    const response = await authApi.me()
    profile.value = response.data.data || response.data
  } catch (error) {
    console.error('Failed to fetch profile:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchProfile)

function validatePassword() {
  Object.keys(passwordErrors).forEach(key => passwordErrors[key] = '')

  let isValid = true

  if (!passwordForm.currentPassword) {
    passwordErrors.currentPassword = t('profile.currentPasswordRequired')
    isValid = false
  }

  if (!passwordForm.newPassword) {
    passwordErrors.newPassword = t('profile.newPasswordRequired')
    isValid = false
  } else if (passwordForm.newPassword.length < 8) {
    passwordErrors.newPassword = t('profile.passwordMinLength')
    isValid = false
  }

  if (!passwordForm.confirmPassword) {
    passwordErrors.confirmPassword = t('profile.confirmPasswordRequired')
    isValid = false
  } else if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordErrors.confirmPassword = t('profile.passwordsDoNotMatch')
    isValid = false
  }

  if (passwordForm.currentPassword === passwordForm.newPassword) {
    passwordErrors.newPassword = t('profile.newPasswordMustDiffer')
    isValid = false
  }

  return isValid
}

async function handleChangePassword() {
  if (!validatePassword()) return

  changingPassword.value = true
  passwordErrors.general = ''

  try {
    await authApi.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })

    passwordSuccess.value = true
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''

    // Hide success message after 3 seconds
    setTimeout(() => {
      passwordSuccess.value = false
      showChangePassword.value = false
    }, 3000)
  } catch (error) {
    const response = error.response?.data
    if (response?.validationErrors && Array.isArray(response.validationErrors)) {
      response.validationErrors.forEach(err => {
        if (err.field && passwordErrors.hasOwnProperty(err.field)) {
          passwordErrors[err.field] = err.message
        }
      })
    }
    passwordErrors.general = response?.message || t('profile.failedToChangePassword')
  } finally {
    changingPassword.value = false
  }
}

function formatDate(date) {
  if (!date) return t('never')
  return new Date(date).toLocaleString()
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('profile.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ $t('profile.subtitle') }}</p>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Profile Card -->
      <div class="lg:col-span-2 space-y-6">
        <!-- User Info -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium flex items-center">
              <UserCircleIcon class="h-5 w-5 mr-2 text-gray-400" />
              {{ $t('profile.accountInfo') }}
            </h3>
          </div>
          <div class="card-body">
            <div class="flex items-center mb-6">
              <div class="w-20 h-20 bg-primary-100 rounded-full flex items-center justify-center">
                <span class="text-primary-700 font-bold text-3xl">
                  {{ profile?.username?.charAt(0).toUpperCase() || 'U' }}
                </span>
              </div>
              <div class="ml-6">
                <h2 class="text-xl font-bold text-gray-900">
                  {{ profile?.fullName || profile?.username }}
                </h2>
                <p class="text-gray-500">@{{ profile?.username }}</p>
              </div>
            </div>

            <dl class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500">{{ $t('name') }}</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ profile?.username }}</dd>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500 flex items-center">
                  <PhoneIcon class="h-4 w-4 mr-1" />
                  {{ $t('phone') }}
                </dt>
                <dd class="mt-1 text-sm text-gray-900">{{ profile?.phone || $t('notSet') }}</dd>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500">{{ $t('profile.firstName') }}</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ profile?.firstName || $t('notSet') }}</dd>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500">{{ $t('profile.lastName') }}</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ profile?.lastName || $t('notSet') }}</dd>
              </div>
            </dl>
          </div>
        </div>

        <!-- Change Password -->
        <div class="card">
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg font-medium flex items-center">
              <KeyIcon class="h-5 w-5 mr-2 text-gray-400" />
              {{ $t('profile.passwordAndSecurity') }}
            </h3>
            <button
              v-if="!showChangePassword"
              @click="showChangePassword = true"
              class="btn-secondary text-sm"
            >
              {{ $t('profile.changePassword') }}
            </button>
          </div>
          <div class="card-body">
            <div v-if="!showChangePassword" class="text-sm text-gray-500">
              <p>{{ $t('profile.passwordLastChanged') }}: <span class="font-medium">{{ $t('profile.unknown') }}</span></p>
              <p class="mt-2">{{ $t('profile.passwordRecommendation') }}</p>
            </div>

            <!-- Change Password Form -->
            <div v-else>
              <!-- Success Message -->
              <div v-if="passwordSuccess" class="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center">
                <CheckCircleIcon class="h-5 w-5 text-green-600 mr-2" />
                <p class="text-sm text-green-700">{{ $t('profile.passwordChanged') }}</p>
              </div>

              <!-- Error Message -->
              <div v-if="passwordErrors.general" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
                <p class="text-sm text-red-600">{{ passwordErrors.general }}</p>
              </div>

              <form @submit.prevent="handleChangePassword" class="space-y-4">
                <!-- Current Password -->
                <div>
                  <label class="label">{{ $t('profile.currentPassword') }}</label>
                  <div class="relative">
                    <input
                      v-model="passwordForm.currentPassword"
                      :type="showCurrentPassword ? 'text' : 'password'"
                      :class="[passwordErrors.currentPassword ? 'input-error' : 'input', 'pr-10']"
                      :placeholder="$t('profile.enterCurrentPassword')"
                    />
                    <button
                      type="button"
                      class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-500"
                      @click="showCurrentPassword = !showCurrentPassword"
                    >
                      <EyeSlashIcon v-if="showCurrentPassword" class="h-5 w-5" />
                      <EyeIcon v-else class="h-5 w-5" />
                    </button>
                  </div>
                  <p v-if="passwordErrors.currentPassword" class="mt-1 text-sm text-red-600">
                    {{ passwordErrors.currentPassword }}
                  </p>
                </div>

                <!-- New Password -->
                <div>
                  <label class="label">{{ $t('profile.newPassword') }}</label>
                  <div class="relative">
                    <input
                      v-model="passwordForm.newPassword"
                      :type="showNewPassword ? 'text' : 'password'"
                      :class="[passwordErrors.newPassword ? 'input-error' : 'input', 'pr-10']"
                      :placeholder="$t('profile.enterNewPasswordMin')"
                    />
                    <button
                      type="button"
                      class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-500"
                      @click="showNewPassword = !showNewPassword"
                    >
                      <EyeSlashIcon v-if="showNewPassword" class="h-5 w-5" />
                      <EyeIcon v-else class="h-5 w-5" />
                    </button>
                  </div>
                  <p v-if="passwordErrors.newPassword" class="mt-1 text-sm text-red-600">
                    {{ passwordErrors.newPassword }}
                  </p>
                </div>

                <!-- Confirm Password -->
                <div>
                  <label class="label">{{ $t('profile.confirmNewPassword') }}</label>
                  <div class="relative">
                    <input
                      v-model="passwordForm.confirmPassword"
                      :type="showConfirmPassword ? 'text' : 'password'"
                      :class="[passwordErrors.confirmPassword ? 'input-error' : 'input', 'pr-10']"
                      :placeholder="$t('profile.confirmNewPasswordPlaceholder')"
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
                  <p v-if="passwordErrors.confirmPassword" class="mt-1 text-sm text-red-600">
                    {{ passwordErrors.confirmPassword }}
                  </p>
                </div>

                <div class="flex justify-end space-x-3 pt-2">
                  <button
                    type="button"
                    @click="showChangePassword = false"
                    class="btn-secondary"
                  >
                    {{ $t('cancel') }}
                  </button>
                  <button
                    type="submit"
                    :disabled="changingPassword"
                    class="btn-primary"
                  >
                    {{ changingPassword ? $t('profile.changing') : $t('profile.changePassword') }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>

      <!-- Sidebar Info -->
      <div class="space-y-6">
        <!-- Roles & Permissions -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium flex items-center">
              <ShieldCheckIcon class="h-5 w-5 mr-2 text-gray-400" />
              {{ $t('profile.roles') }}
            </h3>
          </div>
          <div class="card-body">
            <div class="flex flex-wrap gap-2">
              <span
                v-for="role in profile?.roles"
                :key="role"
                class="px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-sm font-medium"
              >
                {{ role }}
              </span>
              <span v-if="!profile?.roles?.length" class="text-sm text-gray-500">
                {{ $t('profile.noRolesAssigned') }}
              </span>
            </div>
          </div>
        </div>

        <!-- Account Status -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium flex items-center">
              <CalendarIcon class="h-5 w-5 mr-2 text-gray-400" />
              {{ $t('profile.accountStatus') }}
            </h3>
          </div>
          <div class="card-body space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-sm text-gray-500">{{ $t('status') }}</span>
              <span
                :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  profile?.enabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                ]"
              >
                {{ profile?.enabled ? $t('active') : $t('inactive') }}
              </span>
            </div>
            <div class="flex justify-between items-center">
              <span class="text-sm text-gray-500">{{ $t('profile.phoneVerified') }}</span>
              <span
                :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  profile?.phoneVerified ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                ]"
              >
                {{ profile?.phoneVerified ? $t('profile.verified') : $t('profile.notVerified') }}
              </span>
            </div>
            <div class="pt-2 border-t">
              <div class="text-sm text-gray-500">{{ $t('profile.lastLogin') }}</div>
              <div class="text-sm font-medium">{{ formatDate(profile?.lastLoginAt) }}</div>
            </div>
            <div>
              <div class="text-sm text-gray-500">{{ $t('profile.accountCreated') }}</div>
              <div class="text-sm font-medium">{{ formatDate(profile?.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
