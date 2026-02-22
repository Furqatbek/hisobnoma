<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { categoriesApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const categories = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingCategory = ref(null)

const form = reactive({
  name: '',
  code: '',
  description: '',
  parentId: null
})

const errors = reactive({})

// Auto-generate code from name
function generateCode(name) {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').substring(0, 50)
}

function onNameChange() {
  if (!editingCategory.value && form.name) {
    form.code = generateCode(form.name)
  }
}

async function fetchCategories() {
  loading.value = true
  try {
    const response = await categoriesApi.getAll()
    // Backend returns a list directly (not paginated)
    categories.value = response.data.data || response.data || []
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchCategories)

function openModal(category = null) {
  editingCategory.value = category
  if (category) {
    form.name = category.name
    form.code = category.code || ''
    form.description = category.description || ''
    form.parentId = category.parent?.id || null
  } else {
    form.name = ''
    form.code = ''
    form.description = ''
    form.parentId = null
  }
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) errors.name = t('inventory.categories.nameRequired')
  if (!form.code?.trim()) errors.code = t('required')
  return Object.keys(errors).length === 0
}

async function saveCategory() {
  if (!validate()) return

  try {
    if (editingCategory.value) {
      await categoriesApi.update(editingCategory.value.id, form)
    } else {
      await categoriesApi.create(form)
    }
    showModal.value = false
    fetchCategories()
  } catch (error) {
    console.error('Failed to save category:', error)
    errors.general = error.response?.data?.message || t('failedToSave')
  }
}

async function deleteCategory(category) {
  if (!confirm(t('inventory.categories.confirmDelete', { name: category.name }))) return

  try {
    await categoriesApi.delete(category.id)
    fetchCategories()
  } catch (error) {
    console.error('Failed to delete category:', error)
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.categories.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.categories.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.categories.addCategory') }}
      </button>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="categories.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.categories.noCategories') }}</p>
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
            <tr v-for="category in categories" :key="category.id">
              <td class="font-medium">{{ category.name }}</td>
              <td class="font-mono text-sm text-gray-500">{{ category.code }}</td>
              <td class="text-gray-500">{{ category.description || '-' }}</td>
              <td>{{ category.productCount || 0 }}</td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <button @click="openModal(category)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button @click="deleteCategory(category)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
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
            {{ editingCategory ? $t('inventory.categories.editCategory') : $t('inventory.categories.newCategory') }}
          </h3>

          <div v-if="errors.general" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-600">{{ errors.general }}</p>
          </div>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('name') }} *</label>
              <input v-model="form.name" @input="onNameChange" type="text" :class="[errors.name ? 'input-error' : 'input']" />
              <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
            </div>

            <div>
              <label class="label">{{ $t('code') }} *</label>
              <input v-model="form.code" type="text" :class="[errors.code ? 'input-error' : 'input']" :placeholder="$t('inventory.categories.autoGenerated')" />
              <p v-if="errors.code" class="mt-1 text-sm text-red-600">{{ errors.code }}</p>
            </div>

            <div>
              <label class="label">{{ $t('description') }}</label>
              <textarea v-model="form.description" rows="3" class="input"></textarea>
            </div>

            <div>
              <label class="label">{{ $t('inventory.categories.parentCategory') }}</label>
              <select v-model="form.parentId" class="input">
                <option :value="null">{{ $t('inventory.categories.noParent') }}</option>
                <option
                  v-for="cat in categories.filter(c => c.id !== editingCategory?.id)"
                  :key="cat.id"
                  :value="cat.id"
                >
                  {{ cat.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="saveCategory" class="btn-primary">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
