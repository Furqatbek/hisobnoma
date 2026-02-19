<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { terminalsApi, warehousesApi } from '@/services/api'
import { ArrowLeftIcon, ComputerDesktopIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const locations = ref([])
const errors = ref({})

const form = ref({
  code: '',
  name: '',
  locationId: '',
  description: '',
  status: 'ACTIVE'
})

async function fetchTerminal() {
  if (!isEdit.value) return

  loading.value = true
  try {
    const response = await terminalsApi.getById(route.params.id)
    const terminal = response.data.data || response.data
    form.value = {
      code: terminal.terminalCode || terminal.code || '',
      name: terminal.name || '',
      locationId: terminal.locationId || terminal.location?.id || '',
      description: terminal.description || '',
      status: terminal.status || (terminal.active ? 'ACTIVE' : 'INACTIVE')
    }
  } catch (error) {
    console.error('Terminalni yuklashda xatolik:', error)
    alert('Terminalni yuklashda xatolik yuz berdi')
    router.push('/admin/terminals')
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

function validateForm() {
  errors.value = {}

  if (!form.value.code?.trim()) {
    errors.value.code = 'Terminal kodi majburiy'
  } else if (!/^[A-Z0-9_-]+$/i.test(form.value.code)) {
    errors.value.code = 'Kod faqat harflar, raqamlar, tire va pastki chiziqdan iborat bo\'lishi kerak'
  }

  if (!form.value.name?.trim()) {
    errors.value.name = 'Terminal nomi majburiy'
  }

  if (!form.value.locationId) {
    errors.value.locationId = 'Joylashuv tanlanishi kerak'
  }

  return Object.keys(errors.value).length === 0
}

async function saveTerminal() {
  if (!validateForm()) return

  saving.value = true
  try {
    const data = {
      terminalCode: form.value.code.trim().toUpperCase(),
      name: form.value.name.trim(),
      locationId: parseInt(form.value.locationId),
      description: form.value.description?.trim() || null
    }

    let terminalId = route.params.id
    if (isEdit.value) {
      await terminalsApi.update(terminalId, data)
    } else {
      const res = await terminalsApi.create(data)
      const created = res.data.data || res.data
      terminalId = created.id
    }

    // Set active/inactive status via dedicated endpoints
    if (terminalId) {
      if (form.value.status === 'ACTIVE') {
        await terminalsApi.activate(terminalId)
      } else {
        await terminalsApi.deactivate(terminalId)
      }
    }

    router.push('/admin/terminals')
  } catch (error) {
    console.error('Terminalni saqlashda xatolik:', error)

    // Handle validation errors from backend
    if (error.response?.data?.validationErrors) {
      error.response.data.validationErrors.forEach(err => {
        errors.value[err.field] = err.message
      })
    } else if (error.response?.data?.message) {
      alert(error.response.data.message)
    } else {
      alert('Terminalni saqlashda xatolik yuz berdi')
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchLocations()
  fetchTerminal()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? 'Terminalni tahrirlash' : 'Yangi terminal' }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          {{ isEdit ? 'Terminal ma\'lumotlarini yangilash' : 'Yangi POS terminal qo\'shish' }}
        </p>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <div v-else class="max-w-2xl">
      <div class="card">
        <div class="card-header">
          <div class="flex items-center">
            <ComputerDesktopIcon class="h-6 w-6 text-primary-600 mr-2" />
            <h3 class="text-lg font-medium">Terminal ma'lumotlari</h3>
          </div>
        </div>

        <div class="card-body space-y-6">
          <!-- Code -->
          <div>
            <label class="label">
              Terminal kodi <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.code"
              type="text"
              class="input uppercase"
              :class="{ 'border-red-500': errors.code }"
              placeholder="TERMINAL-01"
              :disabled="isEdit"
            />
            <p v-if="errors.code" class="text-sm text-red-500 mt-1">{{ errors.code }}</p>
            <p v-else class="text-xs text-gray-500 mt-1">Unikal identifikator (masalan: KASSA-01, TERMINAL-A)</p>
          </div>

          <!-- Name -->
          <div>
            <label class="label">
              Terminal nomi <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.name"
              type="text"
              class="input"
              :class="{ 'border-red-500': errors.name }"
              placeholder="Asosiy kassa"
            />
            <p v-if="errors.name" class="text-sm text-red-500 mt-1">{{ errors.name }}</p>
          </div>

          <!-- Location -->
          <div>
            <label class="label">
              Joylashuv <span class="text-red-500">*</span>
            </label>
            <select
              v-model="form.locationId"
              class="input"
              :class="{ 'border-red-500': errors.locationId }"
            >
              <option value="">Joylashuvni tanlang</option>
              <option v-for="location in locations" :key="location.id" :value="location.id">
                {{ location.name }}
              </option>
            </select>
            <p v-if="errors.locationId" class="text-sm text-red-500 mt-1">{{ errors.locationId }}</p>
            <p v-else class="text-xs text-gray-500 mt-1">Terminal qaysi do'kon/omborga tegishli</p>
          </div>

          <!-- Description -->
          <div>
            <label class="label">Tavsif</label>
            <textarea
              v-model="form.description"
              rows="3"
              class="input"
              placeholder="Terminal haqida qo'shimcha ma'lumot..."
            ></textarea>
          </div>

          <!-- Status -->
          <div>
            <label class="label">Holat</label>
            <div class="flex items-center space-x-4">
              <label class="flex items-center">
                <input
                  v-model="form.status"
                  type="radio"
                  value="ACTIVE"
                  class="h-4 w-4 text-primary-600"
                />
                <span class="ml-2 text-sm">Faol</span>
              </label>
              <label class="flex items-center">
                <input
                  v-model="form.status"
                  type="radio"
                  value="INACTIVE"
                  class="h-4 w-4 text-primary-600"
                />
                <span class="ml-2 text-sm">Nofaol</span>
              </label>
            </div>
          </div>
        </div>

        <div class="card-footer flex justify-end space-x-3">
          <button @click="router.back()" class="btn-secondary">
            Bekor qilish
          </button>
          <button @click="saveTerminal" :disabled="saving" class="btn-primary">
            {{ saving ? 'Saqlanmoqda...' : (isEdit ? 'Yangilash' : 'Saqlash') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
