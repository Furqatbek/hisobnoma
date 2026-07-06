<script setup>
import { ref, onMounted } from 'vue'
import { distributionVanLoadoutsApi, unwrapPage } from '@/services/api'
import { PlusIcon, TruckIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loadouts = ref([])
const loading = ref(false)
const statusFilter = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

const STATUSES = ['DRAFT', 'LOADED', 'RECONCILED', 'CANCELLED']
const statusClass = {
  DRAFT: 'bg-gray-100 text-gray-800',
  LOADED: 'bg-blue-100 text-blue-800',
  RECONCILED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800'
}

function formatMoney(v) {
  return new Intl.NumberFormat('uz-UZ').format(v || 0)
}

async function fetchLoadouts(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 20 }
    if (statusFilter.value) params.status = statusFilter.value
    const { content, page: meta } = unwrapPage(await distributionVanLoadoutsApi.getAll(params))
    loadouts.value = content
    currentPage.value = meta.number || 0
    totalPages.value = meta.totalPages || 0
    totalElements.value = meta.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch loadouts:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchLoadouts())
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('distribution.van.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('distribution.van.subtitle') }}</p>
      </div>
      <router-link to="/distribution/van-loadouts/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('distribution.van.newLoadout') }}
      </router-link>
    </div>

    <div class="card mb-6">
      <div class="card-body">
        <select v-model="statusFilter" @change="fetchLoadouts(0)" class="input md:w-56">
          <option value="">{{ $t('distribution.van.allStatuses') }}</option>
          <option v-for="s in STATUSES" :key="s" :value="s">{{ $t('distribution.vanStatus.' + s) }}</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.van.number') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.van.date') }}</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.van.loadedValue') }}</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.van.cashDiff') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('status') }}</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">{{ $t('loading') }}</td>
            </tr>
            <tr v-else-if="loadouts.length === 0">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">
                <TruckIcon class="h-12 w-12 mx-auto mb-2 text-gray-300" />
                <p>{{ $t('distribution.van.noLoadouts') }}</p>
              </td>
            </tr>
            <tr
              v-for="lo in loadouts"
              :key="lo.id"
              class="hover:bg-gray-50 cursor-pointer"
              @click="$router.push(`/distribution/van-loadouts/${lo.id}`)"
            >
              <td class="px-6 py-4 font-medium text-gray-900">{{ lo.loadoutNumber }}</td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ lo.loadoutDate }}</td>
              <td class="px-6 py-4 text-right text-sm text-gray-900">{{ formatMoney(lo.totalLoadedValue) }}</td>
              <td class="px-6 py-4 text-right text-sm" :class="Number(lo.cashDifference) < 0 ? 'text-red-600' : 'text-gray-900'">
                {{ formatMoney(lo.cashDifference) }}
              </td>
              <td class="px-6 py-4">
                <span :class="['inline-flex px-2 py-1 text-xs font-semibold rounded-full', statusClass[lo.status] || 'bg-gray-100 text-gray-800']">
                  {{ $t('distribution.vanStatus.' + lo.status) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="px-6 py-3 border-t flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ $t('distribution.van.totalLoadouts') }}: {{ totalElements }}</p>
        <div class="flex gap-2">
          <button @click="fetchLoadouts(currentPage - 1)" :disabled="currentPage === 0" class="btn-secondary text-sm py-1 px-3">{{ $t('previous') }}</button>
          <button @click="fetchLoadouts(currentPage + 1)" :disabled="currentPage >= totalPages - 1" class="btn-secondary text-sm py-1 px-3">{{ $t('next') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
