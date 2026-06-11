<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, onMounted } from 'vue'
import { deliveryVillagesApi } from '@/services/api'
import {
  PlusIcon,
  PencilSquareIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  MapPinIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

const villages = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const activeOnly = ref(false)

async function fetchVillages(page = 0) {
  loading.value = true
  try {
    let response
    if (activeOnly.value) {
      response = await deliveryVillagesApi.getActive()
      const data = response.data.data || response.data
      villages.value = Array.isArray(data) ? data : data.content || []
      currentPage.value = 0
      totalPages.value = 1
      totalElements.value = villages.value.length
    } else {
      const params = { page, size: 20 }
      response = searchQuery.value.trim()
        ? await deliveryVillagesApi.getAll({ ...params, search: searchQuery.value })
        : await deliveryVillagesApi.getAll(params)

      const data = response.data
      villages.value = data.content || []
      currentPage.value = data.page?.number || 0
      totalPages.value = data.page?.totalPages || 0
      totalElements.value = data.page?.totalElements || 0
    }
  } catch (error) {
    console.error('Failed to fetch villages:', error)
  } finally {
    loading.value = false
  }
}

function toggleActiveOnly() {
  activeOnly.value = !activeOnly.value
  fetchVillages(0)
}

async function deleteVillage(village) {
  if (!confirm(t('admin.villages.confirmDelete', { name: village.name }))) return

  try {
    await deliveryVillagesApi.delete(village.id)
    fetchVillages(currentPage.value)
  } catch (error) {
    toast.error(error.response?.data?.message || t('admin.villages.deleteError'))
  }
}

function handleSearch() {
  fetchVillages(0)
}

onMounted(() => fetchVillages())
</script>

<template>
  <div>
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.villages.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('admin.villages.subtitle') }}</p>
      </div>
      <router-link to="/admin/villages/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('admin.villages.newVillage') }}
      </router-link>
    </div>

    <!-- Search & Active Filter -->
    <div class="card mb-6">
      <div class="card-body flex flex-col sm:flex-row gap-4 items-center">
        <div class="relative flex-1">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="searchQuery"
            @input="handleSearch"
            type="text"
            :placeholder="$t('admin.villages.searchPlaceholder')"
            class="input pl-10"
            :disabled="activeOnly"
          />
        </div>
        <label class="flex items-center gap-2 cursor-pointer whitespace-nowrap">
          <input
            v-model="activeOnly"
            type="checkbox"
            class="h-4 w-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
            @change="toggleActiveOnly"
          />
          <span class="text-sm font-medium text-gray-700">{{ $t('admin.villages.activeOnly') }}</span>
        </label>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('name') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.villages.region') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('code') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('status') }}</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">{{ $t('loading') }}</td>
            </tr>
            <tr v-else-if="villages.length === 0">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">
                <MapPinIcon class="h-12 w-12 mx-auto mb-2 text-gray-300" />
                <p>{{ $t('admin.villages.noVillages') }}</p>
              </td>
            </tr>
            <tr v-for="village in villages" :key="village.id" class="hover:bg-gray-50">
              <td class="px-6 py-4">
                <div class="font-medium text-gray-900">{{ village.name }}</div>
                <div v-if="village.description" class="text-sm text-gray-500">{{ village.description }}</div>
              </td>
              <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800">
                  {{ village.regionName }}
                </span>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ village.code || '-' }}</td>
              <td class="px-6 py-4">
                <span
                  :class="[
                    'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
                    village.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  ]"
                >
                  {{ village.active ? $t('active') : $t('inactive') }}
                </span>
              </td>
              <td class="px-6 py-4 text-right space-x-2">
                <router-link
                  :to="`/admin/villages/${village.id}/edit`"
                  class="inline-flex items-center text-primary-600 hover:text-primary-700"
                >
                  <PencilSquareIcon class="h-4 w-4" />
                </router-link>
                <button
                  @click="deleteVillage(village)"
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
          {{ $t('admin.villages.totalVillages') }}: {{ totalElements }}
        </p>
        <div class="flex gap-2">
          <button
            @click="fetchVillages(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm py-1 px-3"
          >
            {{ $t('previous') }}
          </button>
          <button
            @click="fetchVillages(currentPage + 1)"
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
