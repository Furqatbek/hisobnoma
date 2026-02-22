<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { warehousesApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon, BuildingStorefrontIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const warehouses = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingWarehouse = ref(null)

const form = reactive({
  name: '',
  code: '',
  locationType: 'WAREHOUSE',
  address: '',
  phone: '',
  isDefault: false,
  active: true
})

const locationTypes = computed(() => [
  { value: 'WAREHOUSE', label: t('inventory.warehouses.locationTypeWarehouse') },
  { value: 'STORE', label: t('inventory.warehouses.locationTypeStore') },
  { value: 'VIRTUAL', label: t('inventory.warehouses.locationTypeVirtual') },
  { value: 'ZONE', label: t('inventory.warehouses.locationTypeZone') },
  { value: 'BIN', label: t('inventory.warehouses.locationTypeBin') }
])

const errors = reactive({})

async function fetchWarehouses() {
  loading.value = true
  try {
    const response = await warehousesApi.getAll()
    // Backend returns a list directly (not paginated)
    warehouses.value = response.data.data || response.data || []
  } catch (error) {
    console.error('Failed to fetch warehouses:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchWarehouses)

function openModal(warehouse = null) {
  editingWarehouse.value = warehouse
  if (warehouse) {
    Object.assign(form, warehouse)
  } else {
    form.name = ''
    form.code = ''
    form.locationType = 'WAREHOUSE'
    form.address = ''
    form.phone = ''
    form.isDefault = false
    form.active = true
  }
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

async function saveWarehouse() {
  if (!form.name?.trim()) {
    errors.name = t('inventory.warehouses.nameRequired')
    return
  }
  if (!form.code?.trim()) {
    errors.code = t('required')
    return
  }

  try {
    if (editingWarehouse.value) {
      await warehousesApi.update(editingWarehouse.value.id, form)
    } else {
      await warehousesApi.create(form)
    }
    showModal.value = false
    fetchWarehouses()
  } catch (error) {
    errors.general = error.response?.data?.message || t('failedToSave')
  }
}

async function deleteWarehouse(warehouse) {
  if (!confirm(t('inventory.warehouses.confirmDelete', { name: warehouse.name }))) return
  try {
    await warehousesApi.delete(warehouse.id)
    fetchWarehouses()
  } catch (error) {
    console.error('Failed to delete warehouse:', error)
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.warehouses.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.warehouses.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.warehouses.addWarehouse') }}
      </button>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="warehouses.length === 0" class="text-center py-12">
        <BuildingStorefrontIcon class="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p class="text-gray-500">{{ $t('inventory.warehouses.noWarehouses') }}</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-6">
        <div v-for="warehouse in warehouses" :key="warehouse.id" class="border rounded-lg p-4">
          <div class="flex items-start justify-between">
            <div class="flex items-center">
              <BuildingStorefrontIcon class="h-8 w-8 text-primary-600" />
              <div class="ml-3">
                <h3 class="font-medium text-gray-900">{{ warehouse.name }}</h3>
                <p class="text-sm text-gray-500">{{ warehouse.code }}</p>
              </div>
            </div>
            <div class="flex space-x-1">
              <button @click="openModal(warehouse)" class="p-1 text-gray-400 hover:text-primary-600">
                <PencilIcon class="h-4 w-4" />
              </button>
              <button @click="deleteWarehouse(warehouse)" class="p-1 text-gray-400 hover:text-red-600">
                <TrashIcon class="h-4 w-4" />
              </button>
            </div>
          </div>
          <div class="mt-3 text-sm text-gray-500">
            <p v-if="warehouse.address">{{ warehouse.address }}</p>
            <p v-if="warehouse.phone">{{ warehouse.phone }}</p>
          </div>
          <div class="mt-3 flex items-center space-x-2">
            <span :class="['badge', warehouse.active ? 'badge-success' : 'badge-danger']">
              {{ warehouse.active ? $t('active') : $t('inactive') }}
            </span>
            <span v-if="warehouse.isDefault" class="badge badge-info">{{ $t('inventory.warehouses.isDefault') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ editingWarehouse ? $t('inventory.warehouses.editWarehouse') : $t('inventory.warehouses.newWarehouse') }}
          </h3>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('name') }} *</label>
              <input v-model="form.name" type="text" :class="[errors.name ? 'input-error' : 'input']" />
            </div>
            <div>
              <label class="label">{{ $t('code') }} *</label>
              <input v-model="form.code" type="text" :class="[errors.code ? 'input-error' : 'input']" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.warehouses.type') }} *</label>
              <select v-model="form.locationType" class="input">
                <option v-for="type in locationTypes" :key="type.value" :value="type.value">
                  {{ type.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="label">{{ $t('inventory.warehouses.address') }}</label>
              <textarea v-model="form.address" rows="2" class="input"></textarea>
            </div>
            <div>
              <label class="label">{{ $t('phone') }}</label>
              <input v-model="form.phone" type="text" class="input" />
            </div>
            <div class="flex space-x-4">
              <label class="flex items-center">
                <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('active') }}</span>
              </label>
              <label class="flex items-center">
                <input v-model="form.isDefault" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('inventory.warehouses.isDefault') }}</span>
              </label>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="saveWarehouse" class="btn-primary">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
