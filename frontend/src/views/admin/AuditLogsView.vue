<script setup>
import { ref, onMounted, reactive } from 'vue'
import api from '@/services/api'
import { MagnifyingGlassIcon, FunnelIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const logs = ref([])
const loading = ref(true)

const filters = reactive({
  action: '',
  module: '',
  search: ''
})

const pagination = ref({
  page: 0,
  size: 50,
  totalPages: 0
})

async function fetchLogs() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size,
      ...filters
    }
    const response = await api.get('/admin/audit-logs', { params })
    logs.value = response.data.content || []
    pagination.value.totalPages = response.data.page?.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch audit logs:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchLogs)

function formatDate(date) {
  return new Date(date).toLocaleString()
}

function getActionClass(action) {
  const classes = {
    'CREATE': 'badge-success',
    'UPDATE': 'badge-info',
    'DELETE': 'badge-danger',
    'LOGIN': 'badge-info',
    'LOGOUT': 'badge-info',
    'LOGIN_FAILED': 'badge-warning'
  }
  return classes[action] || 'badge-info'
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.auditLogs.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ $t('admin.auditLogs.subtitle') }}</p>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-wrap gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="filters.search"
            type="text"
            :placeholder="$t('admin.auditLogs.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
        <select v-model="filters.action" class="input w-auto">
          <option value="">{{ $t('admin.auditLogs.allActions') }}</option>
          <option value="CREATE">{{ $t('admin.auditLogs.create') }}</option>
          <option value="UPDATE">{{ $t('admin.auditLogs.update') }}</option>
          <option value="DELETE">{{ $t('admin.auditLogs.deleteAction') }}</option>
          <option value="LOGIN">{{ $t('admin.auditLogs.login') }}</option>
          <option value="LOGOUT">{{ $t('admin.auditLogs.logout') }}</option>
        </select>
        <select v-model="filters.module" class="input w-auto">
          <option value="">{{ $t('admin.auditLogs.allModules') }}</option>
          <option value="AUTH">{{ $t('admin.auditLogs.auth') }}</option>
          <option value="INVENTORY">{{ $t('admin.auditLogs.inventory') }}</option>
          <option value="POS">{{ $t('admin.auditLogs.pos') }}</option>
          <option value="CUSTOMER">{{ $t('admin.auditLogs.customer') }}</option>
          <option value="PURCHASE">{{ $t('admin.auditLogs.purchase') }}</option>
        </select>
        <button @click="fetchLogs" class="btn-primary">
          <FunnelIcon class="h-5 w-5 mr-2" />
          {{ $t('admin.auditLogs.filter') }}
        </button>
      </div>
    </div>

    <!-- Logs Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="logs.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('admin.auditLogs.noLogs') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('admin.auditLogs.timestamp') }}</th>
              <th>{{ $t('admin.auditLogs.user') }}</th>
              <th>{{ $t('admin.auditLogs.action') }}</th>
              <th>{{ $t('admin.auditLogs.module') }}</th>
              <th>{{ $t('description') }}</th>
              <th>{{ $t('admin.auditLogs.ipAddress') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="log in logs" :key="log.id">
              <td class="text-sm text-gray-500 whitespace-nowrap">
                {{ formatDate(log.actionTimestamp || log.createdAt) }}
              </td>
              <td>
                <div class="flex items-center">
                  <div class="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                    <span class="text-gray-600 text-xs font-medium">
                      {{ log.username?.charAt(0).toUpperCase() || '?' }}
                    </span>
                  </div>
                  <span class="ml-2 font-medium">{{ log.username || $t('admin.auditLogs.system') }}</span>
                </div>
              </td>
              <td>
                <span :class="['badge', getActionClass(log.action)]">
                  {{ log.action }}
                </span>
              </td>
              <td class="text-sm">{{ log.module || log.entityType || '-' }}</td>
              <td class="text-sm text-gray-600 max-w-md truncate">
                {{ log.description }}
              </td>
              <td class="text-sm text-gray-500 font-mono">{{ log.ipAddress || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <button
            @click="pagination.page--; fetchLogs()"
            :disabled="pagination.page === 0"
            class="btn-secondary"
          >
            {{ $t('previous') }}
          </button>
          <span class="text-sm text-gray-500">
            {{ $t('page') }} {{ pagination.page + 1 }} {{ $t('of') }} {{ pagination.totalPages }}
          </span>
          <button
            @click="pagination.page++; fetchLogs()"
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
