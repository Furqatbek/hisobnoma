<script setup>
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

const { t } = useI18n()

const roles = ref([])
const loading = ref(true)
const search = ref('')

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
    alert(t('admin.roles.systemRoleCannotDelete'))
    return
  }

  if (!confirm(t('admin.roles.confirmDelete', { name: role.name }))) return

  try {
    await rolesApi.delete(role.id)
    roles.value = roles.value.filter(r => r.id !== role.id)
  } catch (error) {
    console.error('Rolni o\'chirishda xatolik:', error)
    alert(t('admin.roles.deleteError'))
  }
}

onMounted(fetchRoles)

function handleSearch() {
  pagination.value.page = 0
  fetchRoles()
}

function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('uz-UZ')
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
  </div>
</template>
