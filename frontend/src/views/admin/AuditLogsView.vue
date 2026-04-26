<script setup>
import { ref, onMounted, reactive, watch } from 'vue'
import { auditLogsApi, usersApi } from '@/services/api'
import {
  MagnifyingGlassIcon, FunnelIcon, ExclamationTriangleIcon,
  ChartBarIcon, UserGroupIcon, ShieldExclamationIcon, CalendarDaysIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const logs = ref([])
const loading = ref(true)
const usersList = ref([])

const filters = reactive({
  action: '',
  module: '',
  failedOnly: false,
  startDate: '',
  endDate: '',
  userId: '',
  entityType: '',
  entityId: ''
})

const pagination = ref({ page: 0, size: 50, totalPages: 0, totalElements: 0 })

// Stats
const statsLoading = ref(true)
const actionStats = ref([])
const moduleStats = ref([])
const activeUsers = ref([])
const failedLoginCount = ref(0)
const statsDays = ref(7)
const showStatsPanel = ref(true)

async function fetchLogs() {
  loading.value = true
  try {
    const pageParams = { page: pagination.value.page, size: pagination.value.size }
    let response

    if (filters.userId) {
      response = await auditLogsApi.getByUser(filters.userId, pageParams)
    } else if (filters.entityType && filters.entityId) {
      response = await auditLogsApi.getByEntity(filters.entityType, filters.entityId, pageParams)
    } else if (filters.failedOnly) {
      response = await auditLogsApi.getFailed(pageParams)
    } else if (filters.action) {
      response = await auditLogsApi.getByAction(filters.action, pageParams)
    } else if (filters.module) {
      response = await auditLogsApi.getByModule(filters.module, pageParams)
    } else if (filters.startDate && filters.endDate) {
      const start = new Date(filters.startDate).toISOString()
      const end = new Date(filters.endDate + 'T23:59:59').toISOString()
      response = await auditLogsApi.getByDateRange(start, end, pageParams)
    } else {
      response = await auditLogsApi.getAll(pageParams)
    }

    const data = response.data.data || response.data
    logs.value = data.content || data.items || []
    pagination.value.totalPages = data.totalPages || data.page?.totalPages || 0
    pagination.value.totalElements = data.totalElements || data.page?.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch audit logs:', error)
  } finally {
    loading.value = false
  }
}

async function fetchUsers() {
  try {
    const response = await usersApi.getAll({ size: 1000 })
    const data = response.data.data || response.data
    usersList.value = data.content || data || []
  } catch (error) {
    console.error('Failed to fetch users:', error)
  }
}

async function fetchStats() {
  statsLoading.value = true
  try {
    const [actionRes, moduleRes, usersRes, failedRes] = await Promise.all([
      auditLogsApi.getActionStats(statsDays.value),
      auditLogsApi.getModuleStats(statsDays.value),
      auditLogsApi.getActiveUsers(statsDays.value),
      auditLogsApi.getFailedLogins(24)
    ])
    actionStats.value = (actionRes.data.data || []).map(r => ({ action: r[0], count: r[1] }))
    moduleStats.value = (moduleRes.data.data || []).map(r => ({ module: r[0], count: r[1] }))
    activeUsers.value = (usersRes.data.data || []).map(r => ({ userId: r[0], username: r[1], count: r[2] }))
    failedLoginCount.value = failedRes.data.data?.count ?? 0
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  fetchLogs()
  fetchStats()
  fetchUsers()
})

function applyFilter() {
  pagination.value.page = 0
  fetchLogs()
}

function clearFilters() {
  filters.action = ''
  filters.module = ''
  filters.failedOnly = false
  filters.startDate = ''
  filters.endDate = ''
  filters.userId = ''
  filters.entityType = ''
  filters.entityId = ''
  pagination.value.page = 0
  fetchLogs()
}

function filterByAction(action) {
  filters.action = action
  filters.module = ''
  filters.failedOnly = false
  filters.startDate = ''
  filters.endDate = ''
  filters.userId = ''
  filters.entityType = ''
  filters.entityId = ''
  applyFilter()
}

function filterByModule(module) {
  filters.module = module
  filters.action = ''
  filters.failedOnly = false
  filters.startDate = ''
  filters.endDate = ''
  filters.userId = ''
  filters.entityType = ''
  filters.entityId = ''
  applyFilter()
}

function showFailed() {
  filters.failedOnly = true
  filters.action = ''
  filters.module = ''
  filters.startDate = ''
  filters.endDate = ''
  filters.userId = ''
  filters.entityType = ''
  filters.entityId = ''
  applyFilter()
}

function filterByUser(userId) {
  filters.userId = userId
  filters.action = ''
  filters.module = ''
  filters.failedOnly = false
  filters.startDate = ''
  filters.endDate = ''
  filters.entityType = ''
  filters.entityId = ''
  applyFilter()
}

function prevPage() {
  if (pagination.value.page > 0) {
    pagination.value.page--
    fetchLogs()
  }
}

function nextPage() {
  if (pagination.value.page < pagination.value.totalPages - 1) {
    pagination.value.page++
    fetchLogs()
  }
}

watch(statsDays, () => fetchStats())

const hasActiveFilter = () => filters.action || filters.module || filters.failedOnly || (filters.startDate && filters.endDate) || filters.userId || (filters.entityType && filters.entityId)

function formatDate(date) {
  if (!date) return '-'
  const d = new Date(date)
  return d.toLocaleDateString('uz-Cyrl', { day: '2-digit', month: '2-digit', year: 'numeric' }) +
    ' ' + d.toLocaleTimeString('uz-Cyrl', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function getActionClass(action) {
  const classes = {
    CREATE: 'badge-success',
    UPDATE: 'badge-info',
    DELETE: 'badge-danger',
    LOGIN: 'bg-blue-100 text-blue-700',
    LOGOUT: 'bg-gray-100 text-gray-700',
    LOGIN_FAILED: 'badge-warning',
    APPROVE: 'bg-green-100 text-green-700',
    REJECT: 'bg-red-100 text-red-700',
    EXPORT: 'bg-purple-100 text-purple-700',
    IMPORT: 'bg-purple-100 text-purple-700',
    SUBMIT: 'bg-blue-100 text-blue-700',
    CANCEL: 'bg-gray-100 text-gray-700',
    ACTIVATE: 'bg-green-100 text-green-700',
    DEACTIVATE: 'bg-red-100 text-red-700',
    TRANSFER: 'bg-indigo-100 text-indigo-700',
    ADJUSTMENT: 'bg-amber-100 text-amber-700',
    PASSWORD_CHANGE: 'bg-yellow-100 text-yellow-700',
    PERMISSION_CHANGE: 'bg-orange-100 text-orange-700'
  }
  return classes[action] || 'badge-info'
}

const actionLabels = {
  CREATE: 'admin.auditLogs.create',
  UPDATE: 'admin.auditLogs.update',
  DELETE: 'admin.auditLogs.deleteAction',
  LOGIN: 'admin.auditLogs.login',
  LOGOUT: 'admin.auditLogs.logout',
  LOGIN_FAILED: 'admin.auditLogs.loginFailed',
  APPROVE: 'admin.auditLogs.approve',
  REJECT: 'admin.auditLogs.reject',
  EXPORT: 'admin.auditLogs.export',
  IMPORT: 'admin.auditLogs.import',
  SUBMIT: 'admin.auditLogs.submit',
  CANCEL: 'admin.auditLogs.cancelAction',
  ACTIVATE: 'admin.auditLogs.activate',
  DEACTIVATE: 'admin.auditLogs.deactivate',
  TRANSFER: 'admin.auditLogs.transfer',
  ADJUSTMENT: 'admin.auditLogs.adjustment',
  PASSWORD_CHANGE: 'admin.auditLogs.passwordChange',
  PERMISSION_CHANGE: 'admin.auditLogs.permissionChange'
}

function actionLabel(action) {
  return actionLabels[action] ? t(actionLabels[action]) : action
}

function totalActions() {
  return actionStats.value.reduce((sum, s) => sum + s.count, 0)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.auditLogs.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.auditLogs.subtitle') }}</p>
      </div>
      <button
        @click="showStatsPanel = !showStatsPanel"
        :class="['btn btn-secondary', showStatsPanel ? 'bg-primary-50' : '']"
      >
        <ChartBarIcon class="h-5 w-5 mr-2" />
        {{ $t('admin.auditLogs.statistics') }}
      </button>
    </div>

    <!-- Stats Dashboard -->
    <div v-if="showStatsPanel" class="space-y-4">
      <!-- Period selector -->
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">{{ $t('admin.auditLogs.statsPeriod') }}:</span>
        <button
          v-for="d in [1, 7, 30]" :key="d"
          @click="statsDays = d"
          :class="['px-3 py-1 text-sm rounded-full transition-colors',
            statsDays === d ? 'bg-primary-100 text-primary-700 font-medium' : 'text-gray-500 hover:bg-gray-100']"
        >
          {{ d === 1 ? $t('admin.auditLogs.today') : $t('admin.auditLogs.lastNDays', { n: d }) }}
        </button>
      </div>

      <!-- Stats Cards Row -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Total Actions -->
        <div class="card">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.auditLogs.totalActions') }}</p>
                <p class="text-2xl font-bold text-gray-900 mt-1">
                  {{ statsLoading ? '...' : totalActions() }}
                </p>
              </div>
              <div class="p-3 bg-primary-100 rounded-xl">
                <ChartBarIcon class="h-6 w-6 text-primary-600" />
              </div>
            </div>
          </div>
        </div>

        <!-- Failed Logins (24h) -->
        <button class="card text-left hover:ring-2 hover:ring-red-200 transition-all" @click="showFailed">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.auditLogs.failedLogins24h') }}</p>
                <p :class="['text-2xl font-bold mt-1', failedLoginCount > 0 ? 'text-red-600' : 'text-gray-900']">
                  {{ statsLoading ? '...' : failedLoginCount }}
                </p>
              </div>
              <div :class="['p-3 rounded-xl', failedLoginCount > 0 ? 'bg-red-100' : 'bg-gray-100']">
                <ShieldExclamationIcon :class="['h-6 w-6', failedLoginCount > 0 ? 'text-red-600' : 'text-gray-400']" />
              </div>
            </div>
          </div>
        </button>

        <!-- Top Actions -->
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500 mb-2">{{ $t('admin.auditLogs.topActions') }}</p>
            <div v-if="statsLoading" class="text-sm text-gray-400">...</div>
            <div v-else class="space-y-1.5">
              <button
                v-for="s in actionStats.slice(0, 4)" :key="s.action"
                @click="filterByAction(s.action)"
                class="flex items-center justify-between w-full text-left hover:bg-gray-50 rounded px-1 -mx-1 transition-colors"
              >
                <span :class="['text-xs font-medium px-2 py-0.5 rounded-full', getActionClass(s.action)]">
                  {{ actionLabel(s.action) }}
                </span>
                <span class="text-sm font-medium text-gray-700">{{ s.count }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Top Users -->
        <div class="card">
          <div class="card-body">
            <p class="text-sm text-gray-500 mb-2">{{ $t('admin.auditLogs.activeUsers') }}</p>
            <div v-if="statsLoading" class="text-sm text-gray-400">...</div>
            <div v-else class="space-y-1.5">
              <div v-for="u in activeUsers.slice(0, 4)" :key="u.userId" class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <div class="w-6 h-6 bg-primary-100 rounded-full flex items-center justify-center">
                    <span class="text-primary-700 text-xs font-medium">{{ u.username?.charAt(0).toUpperCase() }}</span>
                  </div>
                  <span class="text-sm text-gray-700">{{ u.username }}</span>
                </div>
                <span class="text-sm font-medium text-gray-700">{{ u.count }}</span>
              </div>
              <p v-if="activeUsers.length === 0" class="text-sm text-gray-400">-</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Module Stats Bar -->
      <div v-if="!statsLoading && moduleStats.length > 0" class="card">
        <div class="card-body">
          <p class="text-sm text-gray-500 mb-3">{{ $t('admin.auditLogs.moduleActivity') }}</p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="m in moduleStats" :key="m.module"
              @click="filterByModule(m.module)"
              class="flex items-center gap-2 px-3 py-1.5 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <span class="text-sm font-medium text-gray-700">{{ m.module || '-' }}</span>
              <span class="text-xs bg-white text-gray-500 px-1.5 py-0.5 rounded-full border">{{ m.count }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-wrap gap-3 items-end">
        <!-- User filter -->
        <div class="min-w-[180px]">
          <select v-model="filters.userId" class="input" @change="if (filters.userId) { filters.action = ''; filters.module = ''; filters.failedOnly = false; filters.entityType = ''; filters.entityId = '' }">
            <option value="">{{ $t('admin.auditLogs.allUsers') }}</option>
            <option v-for="u in usersList" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
          </select>
        </div>
        <div class="flex-1 min-w-[200px]">
          <select v-model="filters.action" class="input" @change="filters.module = ''; filters.failedOnly = false; filters.userId = ''; filters.entityType = ''; filters.entityId = ''">
            <option value="">{{ $t('admin.auditLogs.allActions') }}</option>
            <option value="CREATE">{{ $t('admin.auditLogs.create') }}</option>
            <option value="UPDATE">{{ $t('admin.auditLogs.update') }}</option>
            <option value="DELETE">{{ $t('admin.auditLogs.deleteAction') }}</option>
            <option value="LOGIN">{{ $t('admin.auditLogs.login') }}</option>
            <option value="LOGOUT">{{ $t('admin.auditLogs.logout') }}</option>
            <option value="LOGIN_FAILED">{{ $t('admin.auditLogs.loginFailed') }}</option>
            <option value="APPROVE">{{ $t('admin.auditLogs.approve') }}</option>
            <option value="REJECT">{{ $t('admin.auditLogs.reject') }}</option>
            <option value="EXPORT">{{ $t('admin.auditLogs.export') }}</option>
            <option value="IMPORT">{{ $t('admin.auditLogs.import') }}</option>
            <option value="SUBMIT">{{ $t('admin.auditLogs.submit') }}</option>
            <option value="CANCEL">{{ $t('admin.auditLogs.cancelAction') }}</option>
            <option value="ACTIVATE">{{ $t('admin.auditLogs.activate') }}</option>
            <option value="DEACTIVATE">{{ $t('admin.auditLogs.deactivate') }}</option>
            <option value="TRANSFER">{{ $t('admin.auditLogs.transfer') }}</option>
            <option value="ADJUSTMENT">{{ $t('admin.auditLogs.adjustment') }}</option>
            <option value="PASSWORD_CHANGE">{{ $t('admin.auditLogs.passwordChange') }}</option>
            <option value="PERMISSION_CHANGE">{{ $t('admin.auditLogs.permissionChange') }}</option>
          </select>
        </div>
        <div class="flex-1 min-w-[200px]">
          <select v-model="filters.module" class="input" @change="filters.action = ''; filters.failedOnly = false; filters.userId = ''; filters.entityType = ''; filters.entityId = ''">
            <option value="">{{ $t('admin.auditLogs.allModules') }}</option>
            <option value="AUTH">{{ $t('admin.auditLogs.auth') }}</option>
            <option value="INVENTORY">{{ $t('admin.auditLogs.inventory') }}</option>
            <option value="POS">{{ $t('admin.auditLogs.pos') }}</option>
            <option value="CUSTOMER">{{ $t('admin.auditLogs.customer') }}</option>
            <option value="PURCHASE">{{ $t('admin.auditLogs.purchase') }}</option>
            <option value="FINANCE">{{ $t('admin.auditLogs.finance') }}</option>
            <option value="HR">{{ $t('admin.auditLogs.hr') }}</option>
            <option value="ADMIN">{{ $t('admin.auditLogs.admin') }}</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <input v-model="filters.startDate" type="date" class="input w-auto" />
          <span class="text-gray-400">—</span>
          <input v-model="filters.endDate" type="date" class="input w-auto" />
        </div>
        <div class="flex items-center gap-2">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              v-model="filters.failedOnly"
              type="checkbox"
              class="h-4 w-4 text-red-600 rounded border-gray-300 focus:ring-red-500"
              @change="if (filters.failedOnly) { filters.action = ''; filters.module = ''; filters.userId = ''; filters.entityType = ''; filters.entityId = '' }"
            />
            <span class="text-sm text-red-600 font-medium">{{ $t('admin.auditLogs.failedOnly') }}</span>
          </label>
        </div>
        <!-- Entity filter -->
        <div class="flex items-center gap-2">
          <select v-model="filters.entityType" class="input w-auto" @change="if (filters.entityType) { filters.action = ''; filters.module = ''; filters.failedOnly = false; filters.userId = '' }">
            <option value="">{{ $t('admin.auditLogs.entityType') }}</option>
            <option value="PRODUCT">PRODUCT</option>
            <option value="CATEGORY">CATEGORY</option>
            <option value="BRAND">BRAND</option>
            <option value="CUSTOMER">CUSTOMER</option>
            <option value="VENDOR">VENDOR</option>
            <option value="USER">USER</option>
            <option value="ROLE">ROLE</option>
            <option value="TERMINAL">TERMINAL</option>
            <option value="TRANSACTION">TRANSACTION</option>
            <option value="INVOICE">INVOICE</option>
            <option value="PAYMENT">PAYMENT</option>
            <option value="EMPLOYEE">EMPLOYEE</option>
            <option value="LOCATION">LOCATION</option>
            <option value="SETTING">SETTING</option>
          </select>
          <input
            v-model="filters.entityId"
            type="text"
            :placeholder="$t('admin.auditLogs.entityIdPlaceholder')"
            class="input w-32"
            :disabled="!filters.entityType"
          />
        </div>
        <div class="flex gap-2">
          <button @click="applyFilter" class="btn btn-primary">
            <FunnelIcon class="h-5 w-5 mr-1" />
            {{ $t('admin.auditLogs.filter') }}
          </button>
          <button v-if="hasActiveFilter()" @click="clearFilters" class="btn btn-secondary">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>
      </div>
      <!-- Active filter indicator -->
      <div v-if="hasActiveFilter()" class="px-6 pb-4">
        <div class="flex items-center gap-2 text-sm">
          <span class="text-gray-500">{{ $t('admin.auditLogs.activeFilter') }}:</span>
          <span v-if="filters.failedOnly" class="badge badge-danger">{{ $t('admin.auditLogs.failedOnly') }}</span>
          <span v-if="filters.action" class="badge badge-info">{{ actionLabel(filters.action) }}</span>
          <span v-if="filters.module" class="badge badge-info">{{ filters.module }}</span>
          <span v-if="filters.userId" class="badge badge-info">
            {{ $t('admin.auditLogs.user') }}: {{ usersList.find(u => u.id == filters.userId)?.username || filters.userId }}
          </span>
          <span v-if="filters.entityType && filters.entityId" class="badge badge-info">
            {{ filters.entityType }}:{{ filters.entityId }}
          </span>
          <span v-if="filters.startDate && filters.endDate" class="badge bg-gray-100 text-gray-700">
            {{ filters.startDate }} — {{ filters.endDate }}
          </span>
          <span class="text-gray-400">({{ pagination.totalElements }} {{ $t('admin.auditLogs.records') }})</span>
        </div>
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
              <th>{{ $t('admin.auditLogs.entity') }}</th>
              <th>{{ $t('admin.auditLogs.ipAddress') }}</th>
              <th>{{ $t('status') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="log in logs" :key="log.id" :class="{ 'bg-red-50/50': !log.success }">
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
                  <div class="ml-2">
                    <span class="font-medium text-sm">{{ log.username || $t('admin.auditLogs.system') }}</span>
                    <p v-if="log.userFullName" class="text-xs text-gray-400">{{ log.userFullName }}</p>
                  </div>
                </div>
              </td>
              <td>
                <span :class="['text-xs font-medium px-2 py-0.5 rounded-full', getActionClass(log.action)]">
                  {{ actionLabel(log.action) }}
                </span>
              </td>
              <td class="text-sm">{{ log.module || '-' }}</td>
              <td class="text-sm text-gray-600 max-w-xs truncate" :title="log.description">
                {{ log.description }}
              </td>
              <td class="text-sm text-gray-500">
                <span v-if="log.entityType">{{ log.entityType }}<span v-if="log.entityName" class="text-gray-400"> · {{ log.entityName }}</span></span>
                <span v-else>-</span>
              </td>
              <td class="text-sm text-gray-500 font-mono">{{ log.ipAddress || '-' }}</td>
              <td>
                <span v-if="log.success === false" class="flex items-center gap-1 text-red-600 text-xs font-medium">
                  <ExclamationTriangleIcon class="h-4 w-4" />
                  {{ $t('admin.auditLogs.failed') }}
                </span>
                <span v-else class="text-xs text-green-600">OK</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <button @click="prevPage" :disabled="pagination.page === 0" class="btn btn-secondary">
            {{ $t('previous') }}
          </button>
          <span class="text-sm text-gray-500">
            {{ pagination.page + 1 }} / {{ pagination.totalPages }}
          </span>
          <button @click="nextPage" :disabled="pagination.page >= pagination.totalPages - 1" class="btn btn-secondary">
            {{ $t('next') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
