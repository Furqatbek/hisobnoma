<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { deliveryRegionsApi } from '@/services/api'
import {
  PlusIcon,
  PencilSquareIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  MapPinIcon
} from '@heroicons/vue/24/outline'

const router = useRouter()
const regions = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

async function fetchRegions(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 20 }
    const response = searchQuery.value.trim()
      ? await deliveryRegionsApi.getAll({ ...params, search: searchQuery.value })
      : await deliveryRegionsApi.getAll(params)

    const data = response.data
    regions.value = data.content || []
    currentPage.value = data.page?.number || 0
    totalPages.value = data.page?.totalPages || 0
    totalElements.value = data.page?.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch regions:', error)
  } finally {
    loading.value = false
  }
}

async function deleteRegion(region) {
  if (!confirm(`"${region.name}" hududni o'chirishni xohlaysizmi?`)) return

  try {
    await deliveryRegionsApi.delete(region.id)
    fetchRegions(currentPage.value)
  } catch (error) {
    alert(error.response?.data?.message || 'O\'chirishda xatolik yuz berdi')
  }
}

function handleSearch() {
  fetchRegions(0)
}

onMounted(() => fetchRegions())
</script>

<template>
  <div>
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Hududlar (Regionlar)</h1>
        <p class="text-sm text-gray-500 mt-1">Yetkazib berish hududlarini boshqaring</p>
      </div>
      <router-link to="/admin/regions/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        Yangi hudud
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
            placeholder="Hudud nomi yoki kodi bo'yicha qidirish..."
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
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nomi</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Kod</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Mahallalar soni</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Holat</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amallar</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">Yuklanmoqda...</td>
            </tr>
            <tr v-else-if="regions.length === 0">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">
                <MapPinIcon class="h-12 w-12 mx-auto mb-2 text-gray-300" />
                <p>Hududlar topilmadi</p>
              </td>
            </tr>
            <tr v-for="region in regions" :key="region.id" class="hover:bg-gray-50">
              <td class="px-6 py-4">
                <div class="font-medium text-gray-900">{{ region.name }}</div>
                <div v-if="region.description" class="text-sm text-gray-500">{{ region.description }}</div>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ region.code || '-' }}</td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ region.villageCount || 0 }}</td>
              <td class="px-6 py-4">
                <span
                  :class="[
                    'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
                    region.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  ]"
                >
                  {{ region.active ? 'Faol' : 'Nofaol' }}
                </span>
              </td>
              <td class="px-6 py-4 text-right space-x-2">
                <router-link
                  :to="`/admin/regions/${region.id}/edit`"
                  class="inline-flex items-center text-primary-600 hover:text-primary-700"
                >
                  <PencilSquareIcon class="h-4 w-4" />
                </router-link>
                <button
                  @click="deleteRegion(region)"
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
          Jami: {{ totalElements }} ta hudud
        </p>
        <div class="flex gap-2">
          <button
            @click="fetchRegions(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-secondary text-sm py-1 px-3"
          >
            Oldingi
          </button>
          <button
            @click="fetchRegions(currentPage + 1)"
            :disabled="currentPage >= totalPages - 1"
            class="btn-secondary text-sm py-1 px-3"
          >
            Keyingi
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
