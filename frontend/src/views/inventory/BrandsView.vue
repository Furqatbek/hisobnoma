<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { brandsApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const brands = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingBrand = ref(null)

const form = reactive({ name: '', code: '', description: '' })
const errors = reactive({})

// Auto-generate code from name
function generateCode(name) {
  return name.toUpperCase().replace(/[^A-Z0-9]+/g, '-').replace(/^-|-$/g, '').substring(0, 50)
}

function onNameChange() {
  if (!editingBrand.value && form.name) {
    form.code = generateCode(form.name)
  }
}

async function fetchBrands() {
  loading.value = true
  try {
    const response = await brandsApi.getAll()
    // Backend returns a list directly (not paginated)
    brands.value = response.data.data || response.data || []
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
  form.code = brand?.code || ''
  form.description = brand?.description || ''
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

async function saveBrand() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) {
    errors.name = t('inventory.brands.nameRequired')
  }
  if (!form.code?.trim()) {
    errors.code = t('inventory.brands.codeRequired')
  }
  if (Object.keys(errors).length > 0) return

  try {
    if (editingBrand.value) {
      await brandsApi.update(editingBrand.value.id, form)
    } else {
      await brandsApi.create(form)
    }
    showModal.value = false
    fetchBrands()
  } catch (error) {
    errors.general = error.response?.data?.message || t('inventory.brands.failedToSave')
  }
}

async function deleteBrand(brand) {
  if (!confirm(t('inventory.brands.confirmDelete', { name: brand.name }))) return
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
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.brands.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.brands.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.brands.addBrand') }}
      </button>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="brands.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.brands.noBrands') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('name') }}</th>
              <th>{{ $t('code') }}</th>
              <th>{{ $t('description') }}</th>
              <th>{{ $t('inventory.brands.productsCount') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="brand in brands" :key="brand.id">
              <td class="font-medium">{{ brand.name }}</td>
              <td class="font-mono text-sm text-gray-500">{{ brand.code }}</td>
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
            {{ editingBrand ? $t('inventory.brands.editBrand') : $t('inventory.brands.newBrand') }}
          </h3>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('name') }} *</label>
              <input v-model="form.name" @input="onNameChange" type="text" :class="[errors.name ? 'input-error' : 'input']" />
              <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
            </div>

            <div>
              <label class="label">{{ $t('code') }} *</label>
              <input v-model="form.code" type="text" :class="[errors.code ? 'input-error' : 'input']" :placeholder="$t('inventory.brands.codePlaceholder')" />
              <p v-if="errors.code" class="mt-1 text-sm text-red-600">{{ errors.code }}</p>
            </div>

            <div>
              <label class="label">{{ $t('description') }}</label>
              <textarea v-model="form.description" rows="3" class="input"></textarea>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="saveBrand" class="btn-primary">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
