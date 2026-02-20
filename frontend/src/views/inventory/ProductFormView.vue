<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productsApi, categoriesApi, brandsApi, uomApi } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)

const categories = ref([])
const brands = ref([])
const uoms = ref([])

const form = reactive({
  sku: '',
  barcode: '',
  name: '',
  description: '',
  primaryImageUrl: '',
  categoryId: null,
  brandId: null,
  baseUomId: null,
  costPrice: 0,
  sellingPrice: 0,
  minSellingPrice: null,
  wholesalePrice: null,
  minStockLevel: 0,
  reorderPoint: 0,
  reorderQuantity: 0,
  trackInventory: true,
  allowNegativeStock: false,
  active: true,
  sellable: true,
  purchasable: true
})

const errors = reactive({})

onMounted(async () => {
  loading.value = true
  try {
    const [categoriesRes, brandsRes, uomsRes] = await Promise.all([
      categoriesApi.getAll(),
      brandsApi.getAll(),
      uomApi.getAll()
    ])

    // Backend returns a list directly (not paginated)
    categories.value = categoriesRes.data.data || categoriesRes.data || []
    brands.value = brandsRes.data.data || brandsRes.data || []
    uoms.value = uomsRes.data.data || uomsRes.data || []

    if (isEdit.value) {
      const productRes = await productsApi.getById(route.params.id)
      const productData = productRes.data.data || productRes.data
      Object.assign(form, productData)
      form.categoryId = productData.category?.id || productData.categoryId
      form.brandId = productData.brand?.id || productData.brandId
      form.baseUomId = productData.baseUom?.id || productData.baseUomId
    }
  } catch (error) {
    console.error('Failed to load data:', error)
  } finally {
    loading.value = false
  }
})

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])

  if (!form.sku?.trim()) errors.sku = 'SKU is required'
  if (!form.name?.trim()) errors.name = 'Name is required'
  if (!form.baseUomId) errors.baseUomId = 'Unit of Measure is required'
  if (form.sellingPrice <= 0) errors.sellingPrice = 'Selling price must be greater than 0'

  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  try {
    if (isEdit.value) {
      await productsApi.update(route.params.id, form)
    } else {
      await productsApi.create(form)
    }
    router.push('/inventory/products')
  } catch (error) {
    console.error('Failed to save product:', error)
    if (error.response?.data?.message) {
      errors.general = error.response.data.message
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? 'Edit Product' : 'New Product' }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          {{ isEdit ? 'Update product information' : 'Add a new product to your catalog' }}
        </p>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <!-- General error -->
      <div v-if="errors.general" class="p-4 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600">{{ errors.general }}</p>
      </div>

      <!-- Basic Information -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Basic Information</h3>
        </div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">SKU *</label>
            <input v-model="form.sku" type="text" :class="[errors.sku ? 'input-error' : 'input']" />
            <p v-if="errors.sku" class="mt-1 text-sm text-red-600">{{ errors.sku }}</p>
          </div>

          <div>
            <label class="label">Barcode</label>
            <input v-model="form.barcode" type="text" class="input" />
          </div>

          <div class="md:col-span-2">
            <label class="label">Name *</label>
            <input v-model="form.name" type="text" :class="[errors.name ? 'input-error' : 'input']" />
            <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
          </div>

          <div class="md:col-span-2">
            <label class="label">Description</label>
            <textarea v-model="form.description" rows="3" class="input"></textarea>
          </div>

          <div class="md:col-span-2">
            <label class="label">Image URL</label>
            <input v-model="form.primaryImageUrl" type="url" class="input" placeholder="https://" />
          </div>

          <div>
            <label class="label">Category</label>
            <select v-model="form.categoryId" class="input">
              <option :value="null">Select category</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>

          <div>
            <label class="label">Brand</label>
            <select v-model="form.brandId" class="input">
              <option :value="null">Select brand</option>
              <option v-for="brand in brands" :key="brand.id" :value="brand.id">
                {{ brand.name }}
              </option>
            </select>
          </div>

          <div>
            <label class="label">Unit of Measure *</label>
            <select v-model="form.baseUomId" :class="[errors.baseUomId ? 'input-error' : 'input']">
              <option :value="null">Select UOM</option>
              <option v-for="uom in uoms" :key="uom.id" :value="uom.id">
                {{ uom.name }} ({{ uom.code }})
              </option>
            </select>
            <p v-if="errors.baseUomId" class="mt-1 text-sm text-red-600">{{ errors.baseUomId }}</p>
          </div>
        </div>
      </div>

      <!-- Pricing -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Pricing</h3>
        </div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div>
            <label class="label">Cost Price</label>
            <input v-model.number="form.costPrice" type="number" step="0.01" min="0" class="input" />
          </div>

          <div>
            <label class="label">Selling Price *</label>
            <input
              v-model.number="form.sellingPrice"
              type="number"
              step="0.01"
              min="0"
              :class="[errors.sellingPrice ? 'input-error' : 'input']"
            />
            <p v-if="errors.sellingPrice" class="mt-1 text-sm text-red-600">{{ errors.sellingPrice }}</p>
          </div>

          <div>
            <label class="label">Min Selling Price</label>
            <input v-model.number="form.minSellingPrice" type="number" step="0.01" min="0" class="input" />
          </div>

          <div>
            <label class="label">Wholesale Price</label>
            <input v-model.number="form.wholesalePrice" type="number" step="0.01" min="0" class="input" />
          </div>
        </div>
      </div>

      <!-- Inventory -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Inventory Settings</h3>
        </div>
        <div class="card-body space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label class="label">Min Stock Level</label>
              <input v-model.number="form.minStockLevel" type="number" min="0" class="input" />
            </div>

            <div>
              <label class="label">Reorder Point</label>
              <input v-model.number="form.reorderPoint" type="number" min="0" class="input" />
            </div>

            <div>
              <label class="label">Reorder Quantity</label>
              <input v-model.number="form.reorderQuantity" type="number" min="0" class="input" />
            </div>
          </div>

          <div class="flex flex-wrap gap-6">
            <label class="flex items-center">
              <input v-model="form.trackInventory" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">Track Inventory</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.allowNegativeStock" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">Allow Negative Stock</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">Active</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.sellable" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">Sellable</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.purchasable" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">Purchasable</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">
          Cancel
        </button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Saving...' : (isEdit ? 'Update Product' : 'Create Product') }}
        </button>
      </div>
    </form>
  </div>
</template>
