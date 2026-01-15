<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customersApi } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  name: '',
  code: '',
  customerType: 'RETAIL',
  phone: '',
  email: '',
  address: '',
  city: '',
  taxId: '',
  creditLimit: 0,
  active: true
})

const errors = reactive({})

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    try {
      const response = await customersApi.getById(route.params.id)
      Object.assign(form, response.data)
    } catch (error) {
      console.error('Failed to load customer:', error)
    } finally {
      loading.value = false
    }
  }
})

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) errors.name = 'Name is required'
  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  try {
    if (isEdit.value) {
      await customersApi.update(route.params.id, form)
    } else {
      await customersApi.create(form)
    }
    router.push('/customers')
  } catch (error) {
    errors.general = error.response?.data?.message || 'Failed to save'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ isEdit ? 'Edit Customer' : 'New Customer' }}</h1>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <div v-if="errors.general" class="p-4 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600">{{ errors.general }}</p>
      </div>

      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">Customer Information</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">Name *</label>
            <input v-model="form.name" type="text" :class="[errors.name ? 'input-error' : 'input']" />
            <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
          </div>
          <div>
            <label class="label">Code</label>
            <input v-model="form.code" type="text" class="input" placeholder="Auto-generated if empty" />
          </div>
          <div>
            <label class="label">Type</label>
            <select v-model="form.customerType" class="input">
              <option value="RETAIL">Retail</option>
              <option value="WHOLESALE">Wholesale</option>
              <option value="CORPORATE">Corporate</option>
            </select>
          </div>
          <div>
            <label class="label">Tax ID</label>
            <input v-model="form.taxId" type="text" class="input" />
          </div>
          <div>
            <label class="label">Phone</label>
            <input v-model="form.phone" type="tel" class="input" />
          </div>
          <div>
            <label class="label">Email</label>
            <input v-model="form.email" type="email" class="input" />
          </div>
          <div class="md:col-span-2">
            <label class="label">Address</label>
            <textarea v-model="form.address" rows="2" class="input"></textarea>
          </div>
          <div>
            <label class="label">City</label>
            <input v-model="form.city" type="text" class="input" />
          </div>
          <div>
            <label class="label">Credit Limit</label>
            <input v-model.number="form.creditLimit" type="number" min="0" step="0.01" class="input" />
          </div>
          <div>
            <label class="flex items-center">
              <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm">Active</span>
            </label>
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">Cancel</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Saving...' : (isEdit ? 'Update' : 'Create') }}
        </button>
      </div>
    </form>
  </div>
</template>
