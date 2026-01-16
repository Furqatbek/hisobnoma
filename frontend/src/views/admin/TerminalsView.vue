<script setup>
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
  XCircleIcon
} from '@heroicons/vue/24/outline'

const terminals = ref([])
const locations = ref([])
const loading = ref(true)
const search = ref('')
const statusFilter = ref('all')

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
    if (statusFilter.value !== 'all') {
      params.status = statusFilter.value
    }

    const response = await terminalsApi.getAll(params)
    const data = response.data.data || response.data
    terminals.value = data.content || data || []
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
    alert('Holatni o\'zgartirishda xatolik yuz berdi')
  }
}

async function deleteTerminal(terminal) {
  if (!confirm(`"${terminal.name}" terminalini o'chirmoqchimisiz?`)) return

  try {
    await terminalsApi.delete(terminal.id)
    terminals.value = terminals.value.filter(t => t.id !== terminal.id)
  } catch (error) {
    console.error('Terminalni o\'chirishda xatolik:', error)
    alert('Terminalni o\'chirishda xatolik yuz berdi')
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
  return status === 'ACTIVE' ? 'Faol' : 'Nofaol'
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
        <h1 class="text-2xl font-bold text-gray-900">POS Terminallar</h1>
        <p class="mt-1 text-sm text-gray-500">Kassa terminallarini boshqarish</p>
      </div>
      <RouterLink to="/admin/terminals/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        Yangi terminal
      </RouterLink>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            placeholder="Qidiruv (kod, nom)..."
            class="input pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <select v-model="statusFilter" @change="fetchTerminals" class="input w-auto">
          <option value="all">Barcha holatlar</option>
          <option value="ACTIVE">Faol</option>
          <option value="INACTIVE">Nofaol</option>
        </select>
        <button @click="handleSearch" class="btn-primary">
          Qidirish
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
        <p class="text-gray-500 mb-4">Terminallar topilmadi</p>
        <RouterLink to="/admin/terminals/new" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          Birinchi terminalni qo'shish
        </RouterLink>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Kod</th>
              <th>Nomi</th>
              <th>Joylashuv</th>
              <th>Holat</th>
              <th>Yaratilgan</th>
              <th class="text-right">Amallar</th>
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
                  :title="terminal.status === 'ACTIVE' ? 'Nofaol qilish' : 'Faollashtirish'"
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
                  title="Tahrirlash"
                >
                  <PencilSquareIcon class="h-5 w-5" />
                </RouterLink>
                <button
                  @click="deleteTerminal(terminal)"
                  class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="O'chirish"
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
            Oldingi
          </button>
          <span class="text-sm text-gray-500">
            Sahifa {{ pagination.page + 1 }} / {{ pagination.totalPages }}
            (Jami: {{ pagination.totalElements }})
          </span>
          <button
            @click="pagination.page++; fetchTerminals()"
            :disabled="pagination.page >= pagination.totalPages - 1"
            class="btn-secondary"
          >
            Keyingi
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
