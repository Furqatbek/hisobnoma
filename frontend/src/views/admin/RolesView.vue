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
    alert('Tizim rollari o\'chirib bo\'lmaydi')
    return
  }

  if (!confirm(`"${role.name}" rolini o'chirmoqchimisiz?`)) return

  try {
    await rolesApi.delete(role.id)
    roles.value = roles.value.filter(r => r.id !== role.id)
  } catch (error) {
    console.error('Rolni o\'chirishda xatolik:', error)
    alert('Rolni o\'chirishda xatolik yuz berdi')
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
        <h1 class="text-2xl font-bold text-gray-900">Rollar va ruxsatlar</h1>
        <p class="mt-1 text-sm text-gray-500">Foydalanuvchi rollari va ularning ruxsatlarini boshqarish</p>
      </div>
      <RouterLink to="/admin/roles/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        Yangi rol
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
            placeholder="Qidiruv (rol nomi, kodi)..."
            class="input pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
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

      <div v-else-if="roles.length === 0" class="text-center py-12">
        <ShieldCheckIcon class="h-12 w-12 mx-auto text-gray-400 mb-4" />
        <p class="text-gray-500 mb-4">Rollar topilmadi</p>
        <RouterLink to="/admin/roles/new" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          Birinchi rolni qo'shish
        </RouterLink>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Rol nomi</th>
              <th>Kod</th>
              <th>Tavsif</th>
              <th>Ruxsatlar</th>
              <th>Turi</th>
              <th>Yaratilgan</th>
              <th class="text-right">Amallar</th>
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
                <span class="badge badge-info">{{ role.permissions?.length || 0 }} ta ruxsat</span>
              </td>
              <td>
                <span v-if="role.systemRole" class="badge badge-warning flex items-center w-fit">
                  <LockClosedIcon class="h-3 w-3 mr-1" />
                  Tizim
                </span>
                <span v-else class="badge badge-success">Maxsus</span>
              </td>
              <td class="text-sm text-gray-500">{{ formatDate(role.createdAt) }}</td>
              <td class="text-right space-x-1">
                <RouterLink
                  :to="`/admin/roles/${role.id}/edit`"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Tahrirlash"
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
            @click="pagination.page--; fetchRoles()"
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
            @click="pagination.page++; fetchRoles()"
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
