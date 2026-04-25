<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { usersApi, rolesApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  LockClosedIcon, LockOpenIcon, KeyIcon, ShieldCheckIcon, XMarkIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const users = ref([])
const loading = ref(true)
const search = ref('')

async function fetchUsers() {
  loading.value = true
  try {
    const response = await usersApi.getAll({ size: 50, search: search.value || undefined })
    users.value = response.data.data?.content || response.data.content || []
  } catch (error) {
    console.error('Failed to fetch users:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchUsers)

async function deleteUser(user) {
  if (!confirm(t('admin.users.confirmDelete', { name: user.username }))) return
  try {
    await usersApi.delete(user.id)
    fetchUsers()
  } catch (error) {
    console.error('Failed to delete user:', error)
  }
}

// Lock/Unlock
async function toggleLock(user) {
  const newLocked = !user.locked
  const msg = newLocked
    ? t('admin.users.confirmLock', { name: user.username })
    : t('admin.users.confirmUnlock', { name: user.username })
  if (!confirm(msg)) return
  try {
    await usersApi.lockUser(user.id, newLocked)
    fetchUsers()
  } catch (error) {
    console.error('Failed to toggle lock:', error)
  }
}

// Reset Password Modal
const showResetModal = ref(false)
const resetUser = ref(null)
const resetPassword = ref('')
const resetError = ref('')
const resetting = ref(false)

function openResetModal(user) {
  resetUser.value = user
  resetPassword.value = ''
  resetError.value = ''
  showResetModal.value = true
}

function closeResetModal() {
  showResetModal.value = false
  resetUser.value = null
}

async function submitResetPassword() {
  resetError.value = ''
  if (!resetPassword.value || resetPassword.value.length < 6) {
    resetError.value = t('admin.userForm.passwordMinLength')
    return
  }
  resetting.value = true
  try {
    await usersApi.resetPassword(resetUser.value.id, resetPassword.value)
    closeResetModal()
  } catch (error) {
    resetError.value = error.response?.data?.message || t('admin.users.resetError')
  } finally {
    resetting.value = false
  }
}

// Assign Roles Modal
const showRolesModal = ref(false)
const rolesUser = ref(null)
const availableRoles = ref([])
const selectedRoleCodes = ref(new Set())
const savingRoles = ref(false)
const rolesLoaded = ref(false)

async function openRolesModal(user) {
  rolesUser.value = user
  selectedRoleCodes.value = new Set((user.roles || []).map(r => r.code || r))
  showRolesModal.value = true
  if (!rolesLoaded.value) {
    try {
      const res = await rolesApi.getAll({ size: 100 })
      availableRoles.value = res.data.data?.content || res.data.data || []
      rolesLoaded.value = true
    } catch (error) {
      console.error('Failed to fetch roles:', error)
    }
  }
}

function closeRolesModal() {
  showRolesModal.value = false
  rolesUser.value = null
}

function toggleRole(code) {
  if (selectedRoleCodes.value.has(code)) {
    selectedRoleCodes.value.delete(code)
  } else {
    selectedRoleCodes.value.add(code)
  }
}

async function submitRoles() {
  savingRoles.value = true
  try {
    await usersApi.assignRoles(rolesUser.value.id, [...selectedRoleCodes.value])
    closeRolesModal()
    fetchUsers()
  } catch (error) {
    console.error('Failed to assign roles:', error)
  } finally {
    savingRoles.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.users.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.users.subtitle') }}</p>
      </div>
      <RouterLink to="/admin/users/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('admin.users.addUser') }}
      </RouterLink>
    </div>

    <div class="card">
      <div class="card-body">
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @input="fetchUsers"
            type="text"
            :placeholder="$t('admin.users.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="users.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('admin.users.noUsers') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('admin.users.user') }}</th>
              <th>{{ $t('phone') }}</th>
              <th>{{ $t('admin.users.roles') }}</th>
              <th>{{ $t('admin.users.lastLogin') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="user in users" :key="user.id">
              <td>
                <div class="flex items-center">
                  <div class="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                    <span class="text-primary-700 font-medium">{{ user.username?.charAt(0).toUpperCase() }}</span>
                  </div>
                  <div class="ml-3">
                    <p class="font-medium">{{ user.username }}</p>
                    <p class="text-sm text-gray-500">{{ user.firstName }} {{ user.lastName }}</p>
                  </div>
                </div>
              </td>
              <td>{{ user.phone || '-' }}</td>
              <td>
                <div class="flex flex-wrap gap-1">
                  <span v-for="role in user.roles" :key="role.id || role" class="badge badge-info">
                    {{ role.name || role }}
                  </span>
                </div>
              </td>
              <td class="text-sm text-gray-500">
                {{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : $t('admin.users.never') }}
              </td>
              <td>
                <div class="flex flex-col gap-1">
                  <span :class="['badge', user.enabled ? 'badge-success' : 'badge-danger']">
                    {{ user.enabled ? $t('active') : $t('inactive') }}
                  </span>
                  <span v-if="user.locked" class="badge badge-warning">
                    {{ $t('admin.users.locked') }}
                  </span>
                </div>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end gap-1">
                  <button
                    @click="openRolesModal(user)"
                    class="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded-lg transition-colors"
                    :title="$t('admin.users.assignRoles')"
                  >
                    <ShieldCheckIcon class="h-5 w-5" />
                  </button>
                  <button
                    @click="openResetModal(user)"
                    class="p-1.5 text-gray-400 hover:text-amber-600 hover:bg-gray-100 rounded-lg transition-colors"
                    :title="$t('admin.users.resetPassword')"
                  >
                    <KeyIcon class="h-5 w-5" />
                  </button>
                  <button
                    @click="toggleLock(user)"
                    :class="['p-1.5 rounded-lg transition-colors hover:bg-gray-100',
                      user.locked ? 'text-red-500 hover:text-green-600' : 'text-gray-400 hover:text-red-600']"
                    :title="user.locked ? $t('admin.users.unlock') : $t('admin.users.lock')"
                  >
                    <LockClosedIcon v-if="user.locked" class="h-5 w-5" />
                    <LockOpenIcon v-else class="h-5 w-5" />
                  </button>
                  <RouterLink :to="`/admin/users/${user.id}/edit`" class="p-1.5 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 transition-colors">
                    <PencilIcon class="h-5 w-5" />
                  </RouterLink>
                  <button @click="deleteUser(user)" class="p-1.5 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100 transition-colors">
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Reset Password Modal -->
    <Teleport to="body">
      <div v-if="showResetModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeResetModal"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-md p-6 space-y-5">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ $t('admin.users.resetPassword') }}</h3>
              <button @click="closeResetModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="bg-gray-50 rounded-lg p-3">
              <p class="font-medium text-gray-900">{{ resetUser?.username }}</p>
              <p class="text-sm text-gray-500">{{ resetUser?.firstName }} {{ resetUser?.lastName }}</p>
            </div>

            <div v-if="resetError" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
              {{ resetError }}
            </div>

            <div>
              <label class="label">{{ $t('admin.userForm.newPassword') }} <span class="text-red-500">*</span></label>
              <input
                v-model="resetPassword"
                type="password"
                class="input"
                :placeholder="$t('admin.users.newPasswordPlaceholder')"
                @keyup.enter="submitResetPassword"
              />
              <p class="text-xs text-gray-400 mt-1">{{ $t('admin.userForm.passwordMinLength') }}</p>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button @click="closeResetModal" class="btn btn-secondary">{{ $t('cancel') }}</button>
              <button @click="submitResetPassword" :disabled="resetting" class="btn btn-primary">
                <span v-if="resetting" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ $t('admin.users.resetPassword') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Assign Roles Modal -->
    <Teleport to="body">
      <div v-if="showRolesModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeRolesModal"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-5">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">{{ $t('admin.users.assignRoles') }}</h3>
              <button @click="closeRolesModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div class="bg-gray-50 rounded-lg p-3">
              <p class="font-medium text-gray-900">{{ rolesUser?.username }}</p>
              <p class="text-sm text-gray-500">{{ rolesUser?.firstName }} {{ rolesUser?.lastName }}</p>
            </div>

            <div class="space-y-2 max-h-[50vh] overflow-y-auto">
              <label
                v-for="role in availableRoles"
                :key="role.id"
                :class="['flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors',
                  selectedRoleCodes.has(role.code) ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:bg-gray-50']"
              >
                <input
                  type="checkbox"
                  :checked="selectedRoleCodes.has(role.code)"
                  @change="toggleRole(role.code)"
                  class="h-4 w-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
                />
                <div class="flex-1">
                  <div class="flex items-center gap-2">
                    <span class="font-medium text-gray-900">{{ role.name }}</span>
                    <span v-if="role.systemRole" class="text-xs text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">
                      {{ $t('admin.roles.system') }}
                    </span>
                  </div>
                  <p v-if="role.description" class="text-xs text-gray-500 mt-0.5">{{ role.description }}</p>
                </div>
              </label>

              <div v-if="availableRoles.length === 0" class="text-center py-4">
                <p class="text-gray-400">{{ $t('admin.userForm.rolesLoading') }}</p>
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button @click="closeRolesModal" class="btn btn-secondary">{{ $t('cancel') }}</button>
              <button @click="submitRoles" :disabled="savingRoles" class="btn btn-primary">
                <span v-if="savingRoles" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ $t('save') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
