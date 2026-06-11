<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/services/api'
import { getRefreshToken, setTokens } from '@/services/tokenStorage'
import {
  UserCircleIcon,
  KeyIcon,
  ShieldCheckIcon,
  CalendarIcon,
  PhoneIcon,
  EyeIcon,
  EyeSlashIcon,
  CheckCircleIcon,
  BuildingOfficeIcon,
  CreditCardIcon,
  ExclamationTriangleIcon,
  ClockIcon,
  UsersIcon,
  MapPinIcon,
  FingerPrintIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const authStore = useAuthStore()
const loading = ref(true)
const profile = ref(null)

// PIN management
const showPinSection = ref(false)
const pinForm = reactive({ pin: '' })
const pinSaving = ref(false)
const pinSuccess = ref(false)
const pinError = ref('')

// Session refresh
const refreshing = ref(false)
const refreshSuccess = ref(false)
const refreshError = ref('')

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

// Subscription computed properties
const subscriptionStatus = computed(() => {
  if (!profile.value?.subscriptionExpiresAt) return 'unknown'
  const expiry = new Date(profile.value.subscriptionExpiresAt)
  const now = new Date()
  const daysLeft = Math.ceil((expiry - now) / (1000 * 60 * 60 * 24))
  if (daysLeft < 0) return 'expired'
  if (daysLeft <= 7) return 'expiring'
  return 'active'
})

const subscriptionDaysLeft = computed(() => {
  if (!profile.value?.subscriptionExpiresAt) return null
  const expiry = new Date(profile.value.subscriptionExpiresAt)
  const now = new Date()
  return Math.ceil((expiry - now) / (1000 * 60 * 60 * 24))
})

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

    setTimeout(() => {
      passwordSuccess.value = false
      showChangePassword.value = false
    }, 3000)
  } catch (error) {
    const response = error.response?.data
    if (response?.validationErrors && Array.isArray(response.validationErrors)) {
      response.validationErrors.forEach(err => {
        if (err.field && Object.hasOwn(passwordErrors, err.field)) {
          passwordErrors[err.field] = err.message
        }
      })
    }
    passwordErrors.general = response?.message || t('profile.failedToChangePassword')
  } finally {
    changingPassword.value = false
  }
}

async function handleSetPin() {
  pinError.value = ''
  pinSuccess.value = false
  if (!pinForm.pin || !/^\d{4}$/.test(pinForm.pin)) {
    pinError.value = t('profile.pinMustBe4Digits')
    return
  }
  pinSaving.value = true
  try {
    await authApi.setPin(pinForm.pin)
    pinSuccess.value = true
    pinForm.pin = ''
    setTimeout(() => {
      pinSuccess.value = false
      showPinSection.value = false
    }, 3000)
  } catch (error) {
    pinError.value = error.response?.data?.message || t('profile.pinSetError')
  } finally {
    pinSaving.value = false
  }
}

async function handleRefreshSession() {
  refreshing.value = true
  refreshSuccess.value = false
  refreshError.value = ''
  try {
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      refreshError.value = t('profile.noRefreshToken')
      return
    }
    const response = await authApi.refresh(refreshToken)
    const data = response.data.data || response.data
    setTokens(data.accessToken, data.refreshToken)
    refreshSuccess.value = true
    setTimeout(() => { refreshSuccess.value = false }, 3000)
  } catch (error) {
    refreshError.value = error.response?.data?.message || t('profile.refreshError')
  } finally {
    refreshing.value = false
  }
}

function formatDate(date) {
  if (!date) return t('never')
  return new Date(date).toLocaleString('uz-UZ')
}

function formatDateShort(date) {
  if (!date) return '—'
  return new Date(date).toLocaleDateString('uz-UZ', { year: 'numeric', month: 'long', day: 'numeric' })
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
      <!-- Main Content -->
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
                <dt class="text-sm font-medium text-gray-500">{{ $t('username') }}</dt>
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

        <!-- Subscription / Platform Info -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium flex items-center">
              <CreditCardIcon class="h-5 w-5 mr-2 text-gray-400" />
              {{ $t('profile.subscription') }}
            </h3>
          </div>
          <div class="card-body">
            <!-- Subscription status banner -->
            <div
              :class="[
                'rounded-lg p-4 mb-6',
                subscriptionStatus === 'active' ? 'bg-green-50 border border-green-200' :
                subscriptionStatus === 'expiring' ? 'bg-yellow-50 border border-yellow-200' :
                subscriptionStatus === 'expired' ? 'bg-red-50 border border-red-200' :
                'bg-gray-50 border border-gray-200'
              ]"
            >
              <div class="flex items-start justify-between">
                <div class="flex items-start gap-3">
                  <div
                    :class="[
                      'w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0',
                      subscriptionStatus === 'active' ? 'bg-green-100' :
                      subscriptionStatus === 'expiring' ? 'bg-yellow-100' :
                      subscriptionStatus === 'expired' ? 'bg-red-100' :
                      'bg-gray-100'
                    ]"
                  >
                    <CheckCircleIcon v-if="subscriptionStatus === 'active'" class="h-5 w-5 text-green-600" />
                    <ExclamationTriangleIcon v-else-if="subscriptionStatus === 'expiring'" class="h-5 w-5 text-yellow-600" />
                    <ExclamationTriangleIcon v-else-if="subscriptionStatus === 'expired'" class="h-5 w-5 text-red-600" />
                    <ClockIcon v-else class="h-5 w-5 text-gray-400" />
                  </div>
                  <div>
                    <h4
                      :class="[
                        'text-sm font-semibold',
                        subscriptionStatus === 'active' ? 'text-green-800' :
                        subscriptionStatus === 'expiring' ? 'text-yellow-800' :
                        subscriptionStatus === 'expired' ? 'text-red-800' :
                        'text-gray-800'
                      ]"
                    >
                      {{ subscriptionStatus === 'active' ? $t('profile.subscriptionActive') :
                         subscriptionStatus === 'expiring' ? $t('profile.subscriptionExpiring') :
                         subscriptionStatus === 'expired' ? $t('profile.subscriptionExpired') :
                         $t('profile.subscriptionUnknown') }}
                    </h4>
                    <p
                      :class="[
                        'text-sm mt-0.5',
                        subscriptionStatus === 'active' ? 'text-green-600' :
                        subscriptionStatus === 'expiring' ? 'text-yellow-600' :
                        subscriptionStatus === 'expired' ? 'text-red-600' :
                        'text-gray-500'
                      ]"
                    >
                      <template v-if="subscriptionDaysLeft !== null && subscriptionDaysLeft >= 0">
                        {{ subscriptionDaysLeft }} {{ $t('profile.daysRemaining') }}
                      </template>
                      <template v-else-if="subscriptionDaysLeft !== null && subscriptionDaysLeft < 0">
                        {{ Math.abs(subscriptionDaysLeft) }} {{ $t('profile.daysOverdue') }}
                      </template>
                      <template v-else>
                        {{ $t('profile.contactAdmin') }}
                      </template>
                    </p>
                  </div>
                </div>
                <span
                  :class="[
                    'px-3 py-1 text-xs font-semibold rounded-full',
                    subscriptionStatus === 'active' ? 'bg-green-200 text-green-800' :
                    subscriptionStatus === 'expiring' ? 'bg-yellow-200 text-yellow-800' :
                    subscriptionStatus === 'expired' ? 'bg-red-200 text-red-800' :
                    'bg-gray-200 text-gray-800'
                  ]"
                >
                  {{ subscriptionStatus === 'active' ? $t('active') :
                     subscriptionStatus === 'expiring' ? $t('profile.expiring') :
                     subscriptionStatus === 'expired' ? $t('profile.expired') :
                     $t('profile.unknown') }}
                </span>
              </div>
            </div>

            <!-- Subscription details grid -->
            <dl class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500 flex items-center">
                  <BuildingOfficeIcon class="h-4 w-4 mr-1" />
                  {{ $t('profile.organization') }}
                </dt>
                <dd class="mt-1 text-sm text-gray-900 font-medium">{{ profile?.tenantName || $t('notSet') }}</dd>
                <dd v-if="profile?.tenantCode" class="text-xs text-gray-400 font-mono mt-0.5">{{ profile.tenantCode }}</dd>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500 flex items-center">
                  <CalendarIcon class="h-4 w-4 mr-1" />
                  {{ $t('profile.expiresAt') }}
                </dt>
                <dd class="mt-1 text-sm font-medium" :class="subscriptionStatus === 'expired' ? 'text-red-600' : 'text-gray-900'">
                  {{ profile?.subscriptionExpiresAt ? formatDateShort(profile.subscriptionExpiresAt) : $t('notSet') }}
                </dd>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500 flex items-center">
                  <UsersIcon class="h-4 w-4 mr-1" />
                  {{ $t('profile.maxUsers') }}
                </dt>
                <dd class="mt-1 text-sm text-gray-900 font-medium">{{ profile?.maxUsers || '—' }}</dd>
              </div>
              <div class="p-4 bg-gray-50 rounded-lg">
                <dt class="text-sm font-medium text-gray-500 flex items-center">
                  <MapPinIcon class="h-4 w-4 mr-1" />
                  {{ $t('profile.maxLocations') }}
                </dt>
                <dd class="mt-1 text-sm text-gray-900 font-medium">{{ profile?.maxLocations || '—' }}</dd>
              </div>
            </dl>
          </div>
        </div>

        <!-- PIN Management -->
        <div class="card">
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg font-medium flex items-center">
              <FingerPrintIcon class="h-5 w-5 mr-2 text-gray-400" />
              {{ $t('profile.pinManagement') }}
            </h3>
            <button
              v-if="!showPinSection"
              @click="showPinSection = true"
              class="btn-secondary text-sm"
            >
              {{ $t('profile.setPin') }}
            </button>
          </div>
          <div class="card-body">
            <div v-if="!showPinSection" class="text-sm text-gray-500">
              <p>{{ $t('profile.pinDescription') }}</p>
            </div>
            <div v-else>
              <div v-if="pinSuccess" class="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center">
                <CheckCircleIcon class="h-5 w-5 text-green-600 mr-2" />
                <p class="text-sm text-green-700">{{ $t('profile.pinSetSuccess') }}</p>
              </div>
              <div v-if="pinError" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
                <p class="text-sm text-red-600">{{ pinError }}</p>
              </div>
              <form @submit.prevent="handleSetPin" class="space-y-4">
                <div>
                  <label class="label">{{ $t('profile.pinCode') }}</label>
                  <input
                    v-model="pinForm.pin"
                    type="password"
                    maxlength="4"
                    inputmode="numeric"
                    pattern="[0-9]{4}"
                    class="input w-32 text-center text-lg tracking-widest"
                    :placeholder="$t('profile.pinPlaceholder')"
                  />
                  <p class="mt-1 text-xs text-gray-400">{{ $t('profile.pinHint') }}</p>
                </div>
                <div class="flex justify-end space-x-3">
                  <button type="button" @click="showPinSection = false; pinForm.pin = ''; pinError = ''" class="btn-secondary">
                    {{ $t('cancel') }}
                  </button>
                  <button type="submit" :disabled="pinSaving" class="btn-primary">
                    {{ pinSaving ? $t('saving') : $t('profile.setPin') }}
                  </button>
                </div>
              </form>
            </div>
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

      <!-- Sidebar -->
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
            <div class="flex justify-between items-center">
              <span class="text-sm text-gray-500">{{ $t('profile.platformStatus') }}</span>
              <span
                :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  profile?.tenantActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                ]"
              >
                {{ profile?.tenantActive ? $t('active') : $t('inactive') }}
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

        <!-- Refresh Session -->
        <div class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium flex items-center">
              <ArrowPathIcon class="h-5 w-5 mr-2 text-gray-400" />
              {{ $t('profile.session') }}
            </h3>
          </div>
          <div class="card-body space-y-3">
            <p class="text-sm text-gray-500">{{ $t('profile.refreshSessionDescription') }}</p>
            <div v-if="refreshSuccess" class="p-3 bg-green-50 border border-green-200 rounded-lg">
              <p class="text-sm text-green-700">{{ $t('profile.sessionRefreshed') }}</p>
            </div>
            <div v-if="refreshError" class="p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ refreshError }}</p>
            </div>
            <button
              @click="handleRefreshSession"
              :disabled="refreshing"
              class="btn-secondary w-full justify-center"
            >
              <ArrowPathIcon :class="['h-4 w-4 mr-2', { 'animate-spin': refreshing }]" />
              {{ refreshing ? $t('profile.refreshing') : $t('profile.refreshSession') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
