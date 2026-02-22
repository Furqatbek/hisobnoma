<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { usersApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const users = ref([])
const loading = ref(true)
const search = ref('')

async function fetchUsers() {
  loading.value = true
  try {
    const response = await usersApi.getAll({ size: 50, search: search.value || undefined })
    // API returns { success: true, data: { content: [...] } }
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
                <span :class="['badge', user.enabled ? 'badge-success' : 'badge-danger']">
                  {{ user.enabled ? $t('active') : $t('inactive') }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <RouterLink :to="`/admin/users/${user.id}/edit`" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </RouterLink>
                  <button @click="deleteUser(user)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
