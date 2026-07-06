<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, onMounted } from 'vue'
import { distributionAgentsApi } from '@/services/api'
import {
  PlusIcon,
  PencilSquareIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  TruckIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()

const agents = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

const statusClass = {
  ACTIVE: 'bg-green-100 text-green-800',
  SUSPENDED: 'bg-amber-100 text-amber-800',
  TERMINATED: 'bg-red-100 text-red-800'
}

async function fetchAgents(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 20 }
    const response = searchQuery.value.trim()
      ? await distributionAgentsApi.search(searchQuery.value.trim(), params)
      : await distributionAgentsApi.getPaginated(params)

    const data = response.data
    agents.value = data.content || []
    currentPage.value = data.page?.number || 0
    totalPages.value = data.page?.totalPages || 0
    totalElements.value = data.page?.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch agents:', error)
  } finally {
    loading.value = false
  }
}

async function deleteAgent(agent) {
  if (!confirm(t('distribution.agents.confirmDelete', { name: agent.name }))) return
  try {
    await distributionAgentsApi.delete(agent.id)
    fetchAgents(currentPage.value)
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.agents.deleteError'))
  }
}

function handleSearch() {
  fetchAgents(0)
}

onMounted(() => fetchAgents())
</script>

<template>
  <div>
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('distribution.agents.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('distribution.agents.subtitle') }}</p>
      </div>
      <router-link to="/distribution/agents/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('distribution.agents.newAgent') }}
      </router-link>
    </div>

    <!-- Search -->
    <div class="card mb-6">
      <div class="card-body">
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="searchQuery"
            @input="handleSearch"
            type="text"
            :placeholder="$t('distribution.agents.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('name') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('code') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.agents.phone') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.agents.territories') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('status') }}</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading">
              <td colspan="6" class="px-6 py-8 text-center text-gray-500">{{ $t('loading') }}</td>
            </tr>
            <tr v-else-if="agents.length === 0">
              <td colspan="6" class="px-6 py-8 text-center text-gray-500">
                <TruckIcon class="h-12 w-12 mx-auto mb-2 text-gray-300" />
                <p>{{ $t('distribution.agents.noAgents') }}</p>
              </td>
            </tr>
            <tr v-for="agent in agents" :key="agent.id" class="hover:bg-gray-50">
              <td class="px-6 py-4">
                <div class="font-medium text-gray-900">{{ agent.name }}</div>
                <div v-if="agent.vehicleName || agent.vehiclePlate" class="text-sm text-gray-500">
                  {{ [agent.vehicleName, agent.vehiclePlate].filter(Boolean).join(' · ') }}
                </div>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ agent.code }}</td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ agent.phone || '-' }}</td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ agent.territories?.length || 0 }}</td>
              <td class="px-6 py-4">
                <span
                  :class="['inline-flex px-2 py-1 text-xs font-semibold rounded-full', statusClass[agent.status] || 'bg-gray-100 text-gray-800']"
                >
                  {{ $t('distribution.status.' + agent.status) }}
                </span>
              </td>
              <td class="px-6 py-4 text-right space-x-2">
                <router-link
                  :to="`/distribution/agents/${agent.id}/edit`"
                  class="inline-flex items-center text-primary-600 hover:text-primary-700"
                >
                  <PencilSquareIcon class="h-4 w-4" />
                </router-link>
                <button
                  @click="deleteAgent(agent)"
                  class="inline-flex items-center text-red-600 hover:text-red-700"
                >
                  <TrashIcon class="h-4 w-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="px-6 py-3 border-t flex items-center justify-between">
        <p class="text-sm text-gray-500">
          {{ $t('distribution.agents.totalAgents') }}: {{ totalElements }}
        </p>
        <div class="flex gap-2">
          <button
            @click="fetchAgents(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm py-1 px-3"
          >
            {{ $t('previous') }}
          </button>
          <button
            @click="fetchAgents(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm py-1 px-3"
          >
            {{ $t('next') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
