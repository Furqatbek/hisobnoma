<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deliveryRegionsApi } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const errors = reactive({})

const form = reactive({
  name: '',
  code: '',
  description: '',
  active: true,
  sortOrder: 0
})

async function fetchRegion() {
  loading.value = true
  try {
    const response = await deliveryRegionsApi.getById(route.params.id)
    const region = response.data.data || response.data
    Object.assign(form, {
      name: region.name || '',
      code: region.code || '',
      description: region.description || '',
      active: region.active !== false,
      sortOrder: region.sortOrder || 0
    })
  } catch (error) {
    console.error('Failed to fetch region:', error)
    alert('Hududni yuklashda xatolik')
    router.push('/admin/regions')
  } finally {
    loading.value = false
  }
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) errors.name = 'Hudud nomi kiritilishi shart'
  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  try {
    if (isEdit.value) {
      await deliveryRegionsApi.update(route.params.id, form)
    } else {
      await deliveryRegionsApi.create(form)
    }
    router.push('/admin/regions')
  } catch (error) {
    const msg = error.response?.data?.message || 'Saqlashda xatolik yuz berdi'
    alert(msg)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    fetchRegion()
  }
})
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <!-- Header -->
    <div class="flex items-center mb-6">
      <router-link to="/admin/regions" class="mr-4 p-2 rounded-lg hover:bg-gray-100">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </router-link>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? 'Hududni tahrirlash' : 'Yangi hudud qo\'shish' }}
        </h1>
      </div>
    </div>

    <div v-if="loading" class="card p-8 text-center text-gray-500">Yuklanmoqda...</div>

    <form v-else @submit.prevent="handleSubmit" class="card">
      <div class="card-body space-y-6">
        <!-- Name -->
        <div>
          <label class="label">Nomi *</label>
          <input v-model="form.name" type="text" class="input" placeholder="Hudud nomi" />
          <p v-if="errors.name" class="text-sm text-red-600 mt-1">{{ errors.name }}</p>
        </div>

        <!-- Code -->
        <div>
          <label class="label">Kod</label>
          <input v-model="form.code" type="text" class="input" placeholder="Hudud kodi (ixtiyoriy)" />
        </div>

        <!-- Description -->
        <div>
          <label class="label">Tavsif</label>
          <textarea v-model="form.description" class="input" rows="3" placeholder="Hudud haqida ma'lumot"></textarea>
        </div>

        <!-- Sort Order -->
        <div>
          <label class="label">Tartib raqami</label>
          <input v-model.number="form.sortOrder" type="number" class="input" min="0" />
        </div>

        <!-- Active -->
        <div class="flex items-center">
          <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 border-gray-300 rounded" />
          <label class="ml-2 text-sm text-gray-700">Faol</label>
        </div>
      </div>

      <!-- Actions -->
      <div class="card-footer flex justify-end space-x-3">
        <router-link to="/admin/regions" class="btn-secondary">
          Bekor qilish
        </router-link>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Saqlanmoqda...' : (isEdit ? 'Saqlash' : 'Yaratish') }}
        </button>
      </div>
    </form>
  </div>
</template>
