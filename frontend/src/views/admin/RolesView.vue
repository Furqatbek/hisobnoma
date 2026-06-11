<script setup>
import { useToastStore } from '@/stores/toast'
import { formatDate } from '@/utils/format'
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { rolesApi } from '@/services/api'
import {
  PlusIcon,
  PencilSquareIcon,
  TrashIcon,
  ShieldCheckIcon,
  MagnifyingGlassIcon,
  LockClosedIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

const roles = ref([])
const loading = ref(true)
const search = ref('')
const activeTab = ref('all')
const systemRoles = ref([])
const systemRolesLoading = ref(false)

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

async function fetchRoles() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (search.value) {
      params.search = search.value
    }

    const response = await rolesApi.getAll(params)
    const data = response.data.data || response.data
    roles.value = data.content || data || []
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 1
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || roles.value.length
  } catch (error) {
    console.error('Rollarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

async function deleteRole(role) {
  if (role.systemRole) {
    toast.error(t('admin.roles.systemRoleCannotDelete'))
    return
  }

  if (!confirm(t('admin.roles.confirmDelete', { name: role.name }))) return

  try {
    await rolesApi.delete(role.id)
    roles.value = roles.value.filter(r => r.id !== role.id)
  } catch (error) {
    console.error('Rolni o\'chirishda xatolik:', error)
    toast.error(t('admin.roles.deleteError'))
  }
}

async function fetchSystemRoles() {
  systemRolesLoading.value = true
  try {
    const response = await rolesApi.getSystemRoles()
    const data = response.data.data || response.data
    systemRoles.value = data.content || data || []
  } catch (error) {
    console.error('Tizim rollarini yuklashda xatolik:', error)
  } finally {
    systemRolesLoading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'system' && systemRoles.value.length === 0) {
    fetchSystemRoles()
  }
}

onMounted(fetchRoles)

function handleSearch() {
  pagination.value.page = 0
  fetchRoles()
}

</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.roles.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.roles.subtitle') }}</p>
      </div>
      <RouterLink to="/admin/roles/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('admin.roles.newRole') }}
      </RouterLink>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex gap-1 overflow-x-auto pb-px">
        <button
          @click="switchTab('all')"
          :class="['px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
            activeTab === 'all'
              ? 'border-primary-500 text-primary-600 bg-primary-50/50'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
        >
          {{ $t('admin.roles.allRoles') }}
        </button>
        <button
          @click="switchTab('system')"
          :class="['px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
            activeTab === 'system'
              ? 'border-primary-500 text-primary-600 bg-primary-50/50'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
        >
          <LockClosedIcon class="h-4 w-4 inline mr-1" />
          {{ $t('admin.roles.systemRoles') }}
        </button>
      </nav>
    </div>

    <!-- System Roles Tab -->
    <template v-if="activeTab === 'system'">
      <div class="card">
        <div v-if="systemRolesLoading" class="flex items-center justify-center h-64">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="systemRoles.length === 0" class="text-center py-12">
          <ShieldCheckIcon class="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <p class="text-gray-500">{{ $t('admin.roles.noSystemRoles') }}</p>
        </div>
        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('admin.roles.roleName') }}</th>
                <th>{{ $t('code') }}</th>
                <th>{{ $t('description') }}</th>
                <th>{{ $t('admin.roles.permissions') }}</th>
                <th class="text-right">{{ $t('actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="role in systemRoles" :key="role.id">
                <td>
                  <div class="flex items-center">
                    <LockClosedIcon class="h-5 w-5 text-amber-500 mr-2" />
                    <span class="font-medium">{{ role.name }}</span>
                  </div>
                </td>
                <td class="font-mono text-sm text-gray-500">{{ role.code }}</td>
                <td class="text-sm text-gray-500 max-w-xs truncate">{{ role.description || '-' }}</td>
                <td>
                  <span class="badge badge-info">{{ role.permissions?.length || 0 }} {{ $t('admin.roles.permissionCount') }}</span>
                </td>
                <td class="text-right">
                  <RouterLink
                    :to="`/admin/roles/${role.id}/edit`"
                    class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                    :title="$t('admin.roles.editPermissions')"
                  >
                    <PencilSquareIcon class="h-5 w-5" />
                  </RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- All Roles Tab -->
    <template v-if="activeTab === 'all'">
    <!-- Search -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            :placeholder="$t('admin.roles.searchPlaceholder')"
            class="input pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <button @click="handleSearch" class="btn-primary">
          {{ $t('search') }}
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="roles.length === 0" class="text-center py-12">
        <ShieldCheckIcon class="h-12 w-12 mx-auto text-gray-400 mb-4" />
        <p class="text-gray-500 mb-4">{{ $t('admin.roles.noRoles') }}</p>
        <RouterLink to="/admin/roles/new" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('admin.roles.addFirst') }}
        </RouterLink>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('admin.roles.roleName') }}</th>
              <th>{{ $t('code') }}</th>
              <th>{{ $t('description') }}</th>
              <th>{{ $t('admin.roles.permissions') }}</th>
              <th>{{ $t('admin.roles.type') }}</th>
              <th>{{ $t('admin.roles.createdAt') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="role in roles" :key="role.id">
              <td>
                <div class="flex items-center">
                  <ShieldCheckIcon class="h-5 w-5 text-primary-500 mr-2" />
                  <span class="font-medium">{{ role.name }}</span>
                </div>
              </td>
              <td class="font-mono text-sm text-gray-500">{{ role.code }}</td>
              <td class="text-sm text-gray-500 max-w-xs truncate">{{ role.description || '-' }}</td>
              <td>
                <span class="badge badge-info">{{ role.permissions?.length || 0 }} {{ $t('admin.roles.permissionCount') }}</span>
              </td>
              <td>
                <span v-if="role.systemRole" class="badge badge-warning flex items-center w-fit">
                  <LockClosedIcon class="h-3 w-3 mr-1" />
                  {{ $t('admin.roles.system') }}
                </span>
                <span v-else class="badge badge-success">{{ $t('admin.roles.custom') }}</span>
              </td>
              <td class="text-sm text-gray-500">{{ formatDate(role.createdAt) }}</td>
              <td class="text-right space-x-1">
                <RouterLink
                  :to="`/admin/roles/${role.id}/edit`"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  :title="$t('edit')"
                >
                  <PencilSquareIcon class="h-5 w-5" />
                </RouterLink>
                <button
                  @click="deleteRole(role)"
                  :disabled="role.systemRole"
                  :class="[
                    'p-2 rounded-lg inline-flex',
                    role.systemRole
                      ? 'text-gray-300 cursor-not-allowed'
                      : 'text-gray-400 hover:text-red-600 hover:bg-gray-100'
                  ]"
                  :title="$t('delete')"
                >
                  <TrashIcon class="h-5 w-5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <button
            @click="pagination.page--; fetchRoles()"
            :disabled="pagination.page === 0"
            class="btn-secondary"
          >
            {{ $t('previous') }}
          </button>
          <span class="text-sm text-gray-500">
            {{ $t('page') }} {{ pagination.page + 1 }} / {{ pagination.totalPages }}
            ({{ $t('totalCount') }}: {{ pagination.totalElements }})
          </span>
          <button
            @click="pagination.page++; fetchRoles()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            {{ $t('next') }}
          </button>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>
