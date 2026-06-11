<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { terminalsApi, warehousesApi } from '@/services/api'
import {
  PlusIcon,
  PencilSquareIcon,
  TrashIcon,
  ComputerDesktopIcon,
  MagnifyingGlassIcon,
  CheckCircleIcon,
  XCircleIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

const terminals = ref([])
const locations = ref([])
const loading = ref(true)
const search = ref('')
const statusFilter = ref('all')
const codeLookup = ref('')
const codeLookupLoading = ref(false)
const codeLookupResult = ref(null)
const codeLookupError = ref('')
const locationFilter = ref('')

const pagination = ref({
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0
})

async function fetchTerminals() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (search.value) {
      params.search = search.value
    }

    const response = await terminalsApi.getAll(params)
    const data = response.data.data || response.data
    let items = data.content || data || []
    // Backend returns boolean `active`, map to status string for frontend
    items = items.map(t => ({
      ...t,
      status: t.status || (t.active ? 'ACTIVE' : 'INACTIVE')
    }))
    // Client-side status filter
    if (statusFilter.value !== 'all') {
      items = items.filter(t => t.status === statusFilter.value)
    }
    terminals.value = items
    pagination.value.totalPages = data.page?.totalPages || data.totalPages || 1
    pagination.value.totalElements = data.page?.totalElements || data.totalElements || terminals.value.length
  } catch (error) {
    console.error('Terminallarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

async function fetchLocations() {
  try {
    const response = await warehousesApi.getAll()
    locations.value = response.data.data || response.data || []
  } catch (error) {
    console.error('Joylashuvlarni yuklashda xatolik:', error)
  }
}

function getLocationName(locationId) {
  const location = locations.value.find(l => l.id === locationId)
  return location?.name || '-'
}

async function toggleStatus(terminal) {
  try {
    if (terminal.status === 'ACTIVE') {
      await terminalsApi.deactivate(terminal.id)
      terminal.status = 'INACTIVE'
    } else {
      await terminalsApi.activate(terminal.id)
      terminal.status = 'ACTIVE'
    }
  } catch (error) {
    console.error('Holatni o\'zgartirishda xatolik:', error)
    toast.error(t('admin.terminals.statusChangeError'))
  }
}

async function deleteTerminal(terminal) {
  if (!confirm(t('admin.terminals.confirmDelete', { name: terminal.name }))) return

  try {
    await terminalsApi.delete(terminal.id)
    terminals.value = terminals.value.filter(t => t.id !== terminal.id)
  } catch (error) {
    console.error('Terminalni o\'chirishda xatolik:', error)
    toast.error(t('admin.terminals.deleteError'))
  }
}

async function lookupByCode() {
  if (!codeLookup.value.trim()) return
  codeLookupLoading.value = true
  codeLookupError.value = ''
  codeLookupResult.value = null
  try {
    const response = await terminalsApi.getByCode(codeLookup.value.trim())
    const data = response.data.data || response.data
    codeLookupResult.value = data
  } catch (error) {
    codeLookupError.value = error.response?.status === 404
      ? t('admin.terminals.codeNotFound')
      : (error.response?.data?.message || t('admin.terminals.codeLookupError'))
  } finally {
    codeLookupLoading.value = false
  }
}

function clearCodeLookup() {
  codeLookup.value = ''
  codeLookupResult.value = null
  codeLookupError.value = ''
}

async function filterByLocation() {
  if (!locationFilter.value) {
    fetchTerminals()
    return
  }
  loading.value = true
  try {
    const response = await terminalsApi.getByLocation(locationFilter.value)
    const data = response.data.data || response.data
    let items = data.content || data || []
    items = items.map(t => ({
      ...t,
      status: t.status || (t.active ? 'ACTIVE' : 'INACTIVE')
    }))
    terminals.value = items
    pagination.value.totalPages = 1
    pagination.value.totalElements = items.length
  } catch (error) {
    console.error('Жойлашув бўйича фильтрлашда хатолик:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTerminals()
  fetchLocations()
})

function getStatusClass(status) {
  return status === 'ACTIVE' ? 'badge-success' : 'badge-danger'
}

function getStatusLabel(status) {
  return status === 'ACTIVE' ? t('active') : t('inactive')
}

function handleSearch() {
  pagination.value.page = 0
  fetchTerminals()
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.terminals.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.terminals.subtitle') }}</p>
      </div>
      <RouterLink to="/admin/terminals/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('admin.terminals.newTerminal') }}
      </RouterLink>
    </div>

    <!-- Code Lookup -->
    <div class="card">
      <div class="card-body">
        <div class="flex items-center gap-3">
          <div class="flex-1 relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="codeLookup"
              type="text"
              :placeholder="$t('admin.terminals.codeLookupPlaceholder')"
              class="input pl-10 font-mono"
              @keyup.enter="lookupByCode"
            />
          </div>
          <button @click="lookupByCode" :disabled="codeLookupLoading || !codeLookup.trim()" class="btn btn-secondary">
            {{ codeLookupLoading ? '...' : $t('admin.terminals.lookupCode') }}
          </button>
          <button v-if="codeLookupResult || codeLookupError" @click="clearCodeLookup" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg">
            <XCircleIcon class="h-5 w-5" />
          </button>
        </div>
        <div v-if="codeLookupResult" class="mt-3 p-4 bg-primary-50 border border-primary-200 rounded-lg">
          <div class="flex items-center justify-between">
            <div>
              <div class="flex items-center gap-2">
                <ComputerDesktopIcon class="h-5 w-5 text-gray-400" />
                <span class="font-medium">{{ codeLookupResult.name }}</span>
                <code class="text-sm text-gray-500 bg-white px-2 py-0.5 rounded">{{ codeLookupResult.terminalCode || codeLookupResult.code }}</code>
                <span :class="['badge', codeLookupResult.active || codeLookupResult.status === 'ACTIVE' ? 'badge-success' : 'badge-danger']">
                  {{ codeLookupResult.active || codeLookupResult.status === 'ACTIVE' ? $t('active') : $t('inactive') }}
                </span>
              </div>
              <p class="text-sm text-gray-500 mt-1">{{ $t('admin.terminals.location') }}: {{ getLocationName(codeLookupResult.locationId) }}</p>
            </div>
            <RouterLink :to="`/admin/terminals/${codeLookupResult.id}/edit`" class="btn btn-secondary text-sm">{{ $t('edit') }}</RouterLink>
          </div>
        </div>
        <div v-if="codeLookupError" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
          {{ codeLookupError }}
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            :placeholder="$t('admin.terminals.searchPlaceholder')"
            class="input pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <select v-model="locationFilter" @change="filterByLocation" class="input w-auto">
          <option value="">{{ $t('admin.terminals.allLocations') }}</option>
          <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
        </select>
        <select v-model="statusFilter" @change="fetchTerminals" class="input w-auto">
          <option value="all">{{ $t('admin.terminals.allStatuses') }}</option>
          <option value="ACTIVE">{{ $t('active') }}</option>
          <option value="INACTIVE">{{ $t('inactive') }}</option>
        </select>
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

      <div v-else-if="terminals.length === 0" class="text-center py-12">
        <ComputerDesktopIcon class="h-12 w-12 mx-auto text-gray-400 mb-4" />
        <p class="text-gray-500 mb-4">{{ $t('admin.terminals.noTerminals') }}</p>
        <RouterLink to="/admin/terminals/new" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('admin.terminals.addFirst') }}
        </RouterLink>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('admin.terminals.terminalCode') }}</th>
              <th>{{ $t('name') }}</th>
              <th>{{ $t('admin.terminals.location') }}</th>
              <th>{{ $t('status') }}</th>
              <th>{{ $t('admin.roles.createdAt') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="terminal in terminals" :key="terminal.id">
              <td class="font-mono text-sm">{{ terminal.terminalCode || terminal.code }}</td>
              <td>
                <div class="flex items-center">
                  <ComputerDesktopIcon class="h-5 w-5 text-gray-400 mr-2" />
                  <span class="font-medium">{{ terminal.name }}</span>
                </div>
              </td>
              <td>{{ getLocationName(terminal.locationId) || terminal.location?.name || '-' }}</td>
              <td>
                <button
                  @click="toggleStatus(terminal)"
                  :class="['badge cursor-pointer hover:opacity-80', getStatusClass(terminal.status)]"
                  :title="terminal.status === 'ACTIVE' ? $t('admin.terminals.deactivate') : $t('admin.terminals.activate')"
                >
                  <component
                    :is="terminal.status === 'ACTIVE' ? CheckCircleIcon : XCircleIcon"
                    class="h-4 w-4 mr-1"
                  />
                  {{ getStatusLabel(terminal.status) }}
                </button>
              </td>
              <td class="text-sm text-gray-500">
                {{ terminal.createdAt ? new Date(terminal.createdAt).toLocaleDateString('uz-UZ') : '-' }}
              </td>
              <td class="text-right space-x-1">
                <RouterLink
                  :to="`/admin/terminals/${terminal.id}/edit`"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  :title="$t('edit')"
                >
                  <PencilSquareIcon class="h-5 w-5" />
                </RouterLink>
                <button
                  @click="deleteTerminal(terminal)"
                  class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100 inline-flex"
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
            @click="pagination.page--; fetchTerminals()"
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
            @click="pagination.page++; fetchTerminals()"
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
