<script setup>
import { ref, reactive, onMounted } from 'vue'
import { positionsApi, departmentsApi } from '@/services/api'
import { PlusIcon, PencilSquareIcon, TrashIcon, XMarkIcon } from '@heroicons/vue/24/outline'

const positions = ref([])
const departments = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingId = ref(null)
const saving = ref(false)

const form = reactive({ code: '', name: '', description: '', departmentId: null, baseSalary: 0, active: true })

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ').format(value || 0)
}

async function loadData() {
  loading.value = true
  try {
    const [posRes, deptRes] = await Promise.all([
      positionsApi.getAll(),
      departmentsApi.getAll()
    ])
    positions.value = posRes.data.data || posRes.data || []
    departments.value = deptRes.data.data || deptRes.data || []
  } catch (error) {
    console.error('Failed to load:', error)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', description: '', departmentId: null, baseSalary: 0, active: true })
  showModal.value = true
}

function openEdit(pos) {
  editingId.value = pos.id
  Object.assign(form, pos)
  showModal.value = true
}

async function handleSave() {
  if (!form.code || !form.name) return
  saving.value = true
  try {
    if (editingId.value) {
      await positionsApi.update(editingId.value, form)
    } else {
      await positionsApi.create(form)
    }
    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('Failed to save:', error)
    alert(error.response?.data?.message || 'Xatolik yuz berdi')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  if (!confirm("Lavozimni o'chirmoqchimisiz?")) return
  try {
    await positionsApi.delete(id)
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || "O'chirishda xatolik")
  }
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Lavozimlar</h1>
        <p class="mt-1 text-sm text-gray-500">Lavozimlar va ish o'rinlari</p>
      </div>
      <button @click="openCreate" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" /> Yangi lavozim
      </button>
    </div>

    <div class="card">
      <div class="card-body">
        <div v-if="loading" class="flex items-center justify-center h-32">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="positions.length === 0" class="text-center py-12 text-gray-500">
          Lavozimlar topilmadi
        </div>
        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Kod</th>
                <th>Nomi</th>
                <th>Bo'lim</th>
                <th>Bazaviy maosh</th>
                <th>Holat</th>
                <th>Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="pos in positions" :key="pos.id">
                <td class="font-medium">{{ pos.code }}</td>
                <td>{{ pos.name }}</td>
                <td>{{ pos.departmentName || '-' }}</td>
                <td>{{ formatCurrency(pos.baseSalary) }}</td>
                <td>
                  <span :class="['badge', pos.active ? 'badge-success' : 'badge-danger']">
                    {{ pos.active ? 'Faol' : 'Nofaol' }}
                  </span>
                </td>
                <td>
                  <div class="flex items-center gap-2">
                    <button @click="openEdit(pos)" class="text-primary-600 hover:text-primary-800">
                      <PencilSquareIcon class="h-5 w-5" />
                    </button>
                    <button @click="handleDelete(pos.id)" class="text-red-600 hover:text-red-800">
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

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-lg font-medium">{{ editingId ? 'Lavozimni tahrirlash' : 'Yangi lavozim' }}</h3>
          <button @click="showModal = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
        </div>
        <form @submit.prevent="handleSave" class="p-6 space-y-4">
          <div>
            <label class="label">Kod *</label>
            <input v-model="form.code" type="text" class="input" :disabled="!!editingId" />
          </div>
          <div>
            <label class="label">Nomi *</label>
            <input v-model="form.name" type="text" class="input" />
          </div>
          <div>
            <label class="label">Tavsif</label>
            <textarea v-model="form.description" rows="2" class="input"></textarea>
          </div>
          <div>
            <label class="label">Bo'lim</label>
            <select v-model="form.departmentId" class="input">
              <option :value="null">Tanlang</option>
              <option v-for="d in departments" :key="d.id" :value="d.id">{{ d.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">Bazaviy maosh</label>
            <input v-model.number="form.baseSalary" type="number" step="1000" min="0" class="input" />
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn-secondary">Bekor qilish</button>
            <button type="submit" :disabled="saving" class="btn-primary">
              {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
