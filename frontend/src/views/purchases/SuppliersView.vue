<script setup>
import { ref, onMounted, reactive } from 'vue'
import { suppliersApi } from '@/services/api'
import { PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'

const suppliers = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingSupplier = ref(null)

const form = reactive({
  name: '',
  code: '',
  phone: '',
  email: '',
  address: '',
  contactPerson: '',
  active: true
})

async function fetchSuppliers() {
  loading.value = true
  try {
    const response = await suppliersApi.getAll({ size: 100 })
    suppliers.value = response.data.content || []
  } catch (error) {
    console.error('Failed to fetch suppliers:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchSuppliers)

function openModal(supplier = null) {
  editingSupplier.value = supplier
  if (supplier) {
    Object.assign(form, supplier)
  } else {
    form.name = ''
    form.code = ''
    form.phone = ''
    form.email = ''
    form.address = ''
    form.contactPerson = ''
    form.active = true
  }
  showModal.value = true
}

async function saveSupplier() {
  if (!form.name?.trim()) return

  try {
    if (editingSupplier.value) {
      await suppliersApi.update(editingSupplier.value.id, form)
    } else {
      await suppliersApi.create(form)
    }
    showModal.value = false
    fetchSuppliers()
  } catch (error) {
    console.error('Failed to save supplier:', error)
  }
}

async function deleteSupplier(supplier) {
  if (!confirm(`Delete "${supplier.name}"?`)) return
  try {
    await suppliersApi.delete(supplier.id)
    fetchSuppliers()
  } catch (error) {
    console.error('Failed to delete supplier:', error)
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Suppliers</h1>
        <p class="mt-1 text-sm text-gray-500">Manage your suppliers</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        Add Supplier
      </button>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="suppliers.length === 0" class="text-center py-12">
        <p class="text-gray-500">No suppliers yet</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Supplier</th>
              <th>Contact</th>
              <th>Contact Person</th>
              <th>Status</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="supplier in suppliers" :key="supplier.id">
              <td>
                <div class="font-medium">{{ supplier.name }}</div>
                <div class="text-sm text-gray-500">{{ supplier.code || `#${supplier.id}` }}</div>
              </td>
              <td>
                <div class="text-sm">{{ supplier.phone || '-' }}</div>
                <div class="text-sm text-gray-500">{{ supplier.email || '-' }}</div>
              </td>
              <td>{{ supplier.contactPerson || '-' }}</td>
              <td>
                <span :class="['badge', supplier.active !== false ? 'badge-success' : 'badge-danger']">
                  {{ supplier.active !== false ? 'Active' : 'Inactive' }}
                </span>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <button @click="openModal(supplier)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button @click="deleteSupplier(supplier)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
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
        <div class="relative bg-white rounded-lg max-w-lg w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ editingSupplier ? 'Edit Supplier' : 'New Supplier' }}
          </h3>

          <div class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">Name *</label>
                <input v-model="form.name" type="text" class="input" />
              </div>
              <div>
                <label class="label">Code</label>
                <input v-model="form.code" type="text" class="input" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">Phone</label>
                <input v-model="form.phone" type="tel" class="input" />
              </div>
              <div>
                <label class="label">Email</label>
                <input v-model="form.email" type="email" class="input" />
              </div>
            </div>
            <div>
              <label class="label">Contact Person</label>
              <input v-model="form.contactPerson" type="text" class="input" />
            </div>
            <div>
              <label class="label">Address</label>
              <textarea v-model="form.address" rows="2" class="input"></textarea>
            </div>
            <label class="flex items-center">
              <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm">Active</span>
            </label>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">Cancel</button>
            <button @click="saveSupplier" class="btn-primary">Save</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
