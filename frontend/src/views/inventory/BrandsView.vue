<script setup>
import { ref, onMounted, reactive } from 'vue'
import { brandsApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon } from '@heroicons/vue/24/outline'

const brands = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingBrand = ref(null)

const form = reactive({ name: '', description: '' })
const errors = reactive({})

async function fetchBrands() {
  loading.value = true
  try {
    const response = await brandsApi.getAll({ size: 100 })
    brands.value = response.data.content || []
  } catch (error) {
    console.error('Failed to fetch brands:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchBrands)

function openModal(brand = null) {
  editingBrand.value = brand
  form.name = brand?.name || ''
  form.description = brand?.description || ''
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

async function saveBrand() {
  if (!form.name?.trim()) {
    errors.name = 'Name is required'
    return
  }

  try {
    if (editingBrand.value) {
      await brandsApi.update(editingBrand.value.id, form)
    } else {
      await brandsApi.create(form)
    }
    showModal.value = false
    fetchBrands()
  } catch (error) {
    errors.general = error.response?.data?.message || 'Failed to save'
  }
}

async function deleteBrand(brand) {
  if (!confirm(`Delete "${brand.name}"?`)) return
  try {
    await brandsApi.delete(brand.id)
    fetchBrands()
  } catch (error) {
    console.error('Failed to delete brand:', error)
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Brands</h1>
        <p class="mt-1 text-sm text-gray-500">Manage product brands</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        Add Brand
      </button>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="brands.length === 0" class="text-center py-12">
        <p class="text-gray-500">No brands yet</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Description</th>
              <th>Products</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="brand in brands" :key="brand.id">
              <td class="font-medium">{{ brand.name }}</td>
              <td class="text-gray-500">{{ brand.description || '-' }}</td>
              <td>{{ brand.productCount || 0 }}</td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <button @click="openModal(brand)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button @click="deleteBrand(brand)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ editingBrand ? 'Edit Brand' : 'New Brand' }}
          </h3>

          <div class="space-y-4">
            <div>
              <label class="label">Name *</label>
              <input v-model="form.name" type="text" :class="[errors.name ? 'input-error' : 'input']" />
              <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
            </div>

            <div>
              <label class="label">Description</label>
              <textarea v-model="form.description" rows="3" class="input"></textarea>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">Cancel</button>
            <button @click="saveBrand" class="btn-primary">Save</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
