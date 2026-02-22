<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { uomApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const uoms = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingUom = ref(null)

const form = reactive({
  code: '',
  name: '',
  symbol: '',
  description: '',
  isBaseUnit: true,
  conversionFactor: 1,
  active: true
})
const errors = reactive({})

// Auto-generate code from name
function generateCode(name) {
  return name.toUpperCase().replace(/[^A-Z0-9]+/g, '').substring(0, 10)
}

function onNameChange() {
  if (!editingUom.value && form.name) {
    form.code = generateCode(form.name)
  }
}

async function fetchUoms() {
  loading.value = true
  try {
    const response = await uomApi.getAll()
    // Backend returns a list directly (not paginated)
    uoms.value = response.data.data || response.data || []
  } catch (error) {
    console.error('Failed to fetch UOMs:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchUoms)

function openModal(uom = null) {
  editingUom.value = uom
  form.code = uom?.code || ''
  form.name = uom?.name || ''
  form.symbol = uom?.symbol || ''
  form.description = uom?.description || ''
  form.isBaseUnit = uom?.isBaseUnit ?? true
  form.conversionFactor = uom?.conversionFactor ?? 1
  form.active = uom?.active ?? true
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

async function saveUom() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) {
    errors.name = t('inventory.uom.nameRequired')
  }
  if (!form.code?.trim()) {
    errors.code = t('inventory.uom.codeRequired')
  }
  if (!form.symbol?.trim()) {
    errors.symbol = t('required')
  }
  if (Object.keys(errors).length > 0) return

  try {
    if (editingUom.value) {
      await uomApi.update(editingUom.value.id, form)
    } else {
      await uomApi.create(form)
    }
    showModal.value = false
    fetchUoms()
  } catch (error) {
    errors.general = error.response?.data?.message || t('failedToSave')
  }
}

async function deleteUom(uom) {
  if (!confirm(t('inventory.uom.confirmDelete', { name: uom.name }))) return
  try {
    await uomApi.delete(uom.id)
    fetchUoms()
  } catch (error) {
    console.error('Failed to delete UOM:', error)
    alert(error.response?.data?.message || t('failedToDelete'))
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.uom.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.uom.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.uom.addUom') }}
      </button>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="uoms.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.uom.noUoms') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('name') }}</th>
              <th>{{ $t('code') }}</th>
              <th>{{ $t('inventory.uom.symbol') }}</th>
              <th>{{ $t('description') }}</th>
              <th>{{ $t('inventory.uom.baseUnit') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="uom in uoms" :key="uom.id">
              <td class="font-medium">{{ uom.name }}</td>
              <td class="font-mono text-sm text-gray-500">{{ uom.code }}</td>
              <td class="text-gray-500">{{ uom.symbol }}</td>
              <td class="text-gray-500">{{ uom.description || '-' }}</td>
              <td>
                <span :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  uom.isBaseUnit ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]">
                  {{ uom.isBaseUnit ? $t('inventory.uom.yes') : $t('inventory.uom.no') }}
                </span>
              </td>
              <td>
                <span :class="[
                  'px-2 py-1 text-xs font-medium rounded-full',
                  uom.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                ]">
                  {{ uom.active ? $t('active') : $t('inactive') }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <button @click="openModal(uom)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button @click="deleteUom(uom)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
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
            {{ editingUom ? $t('inventory.uom.editUom') : $t('inventory.uom.newUom') }}
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

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">{{ $t('code') }} *</label>
                <input v-model="form.code" type="text" :class="[errors.code ? 'input-error' : 'input']" />
                <p v-if="errors.code" class="mt-1 text-sm text-red-600">{{ errors.code }}</p>
              </div>

              <div>
                <label class="label">{{ $t('inventory.uom.symbol') }} *</label>
                <input v-model="form.symbol" type="text" :class="[errors.symbol ? 'input-error' : 'input']" />
                <p v-if="errors.symbol" class="mt-1 text-sm text-red-600">{{ errors.symbol }}</p>
              </div>
            </div>

            <div>
              <label class="label">{{ $t('description') }}</label>
              <textarea v-model="form.description" rows="2" class="input"></textarea>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">{{ $t('inventory.productForm.conversionFactor') }}</label>
                <input v-model.number="form.conversionFactor" type="number" step="0.000001" min="0" class="input" />
              </div>
            </div>

            <div class="flex flex-wrap gap-4">
              <label class="flex items-center">
                <input v-model="form.isBaseUnit" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.uom.baseUnit') }}</span>
              </label>

              <label class="flex items-center">
                <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm text-gray-700">{{ $t('active') }}</span>
              </label>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="saveUom" class="btn-primary">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
