<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productsApi, categoriesApi, brandsApi, uomApi, suppliersApi } from '@/services/api'
import { ArrowLeftIcon, PhotoIcon, TrashIcon, StarIcon, PlusIcon, PencilIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import { StarIcon as StarIconSolid } from '@heroicons/vue/24/solid'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)

const categories = ref([])
const brands = ref([])
const uoms = ref([])

// Image upload state
const productImages = ref([])
const uploading = ref(false)
const uploadError = ref('')
const fileInput = ref(null)

// Vendor state
const vendors = ref([])
const productVendors = ref([])
const showVendorModal = ref(false)
const editingVendorLink = ref(null)
const savingVendor = ref(false)
const vendorForm = reactive({
  vendorId: null,
  vendorSku: '',
  vendorProductName: '',
  unitCost: null,
  minOrderQuantity: null,
  leadTimeDays: null,
  preferred: false,
  active: true,
  notes: ''
})

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

async function loadImages() {
  if (!isEdit.value) return
  try {
    const res = await productsApi.getImages(route.params.id)
    productImages.value = res.data.data || res.data || []
  } catch (error) {
    console.error('Failed to load images:', error)
  }
}

async function loadProductVendors() {
  if (!isEdit.value) return
  try {
    const res = await productsApi.getVendors(route.params.id)
    productVendors.value = res.data.data || res.data || []
  } catch (error) {
    console.error('Failed to load product vendors:', error)
  }
}

async function handleImageUpload(event) {
  const files = event.target.files
  if (!files?.length) return

  uploading.value = true
  uploadError.value = ''

  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      uploadError.value = 'Faqat rasm fayllarini yuklash mumkin'
      continue
    }
    if (file.size > 10 * 1024 * 1024) {
      uploadError.value = 'Fayl hajmi 10MB dan oshmasligi kerak'
      continue
    }

    try {
      await productsApi.uploadImage(route.params.id, file)
    } catch (error) {
      uploadError.value = error.response?.data?.message || 'Rasmni yuklashda xatolik yuz berdi'
    }
  }

  await loadImages()
  uploading.value = false

  // Reset file input
  if (fileInput.value) fileInput.value.value = ''
}

async function handleDeleteImage(imageId) {
  try {
    await productsApi.deleteImage(route.params.id, imageId)
    await loadImages()
  } catch (error) {
    console.error('Failed to delete image:', error)
  }
}

async function handleSetPrimary(imageId) {
  try {
    await productsApi.setPrimaryImage(route.params.id, imageId)
    await loadImages()
  } catch (error) {
    console.error('Failed to set primary image:', error)
  }
}

// Vendor modal functions
function openAddVendorModal() {
  editingVendorLink.value = null
  Object.assign(vendorForm, {
    vendorId: null,
    vendorSku: '',
    vendorProductName: '',
    unitCost: null,
    minOrderQuantity: null,
    leadTimeDays: null,
    preferred: false,
    active: true,
    notes: ''
  })
  showVendorModal.value = true
}

function openEditVendorModal(pv) {
  editingVendorLink.value = pv
  Object.assign(vendorForm, {
    vendorId: pv.vendorId,
    vendorSku: pv.vendorSku || '',
    vendorProductName: pv.vendorProductName || '',
    unitCost: pv.unitCost,
    minOrderQuantity: pv.minOrderQuantity,
    leadTimeDays: pv.leadTimeDays,
    preferred: pv.preferred,
    active: pv.active,
    notes: pv.notes || ''
  })
  showVendorModal.value = true
}

async function handleSaveVendor() {
  if (!vendorForm.vendorId) return

  savingVendor.value = true
  try {
    if (editingVendorLink.value) {
      await productsApi.updateVendor(route.params.id, editingVendorLink.value.id, vendorForm)
    } else {
      await productsApi.addVendor(route.params.id, vendorForm)
    }
    showVendorModal.value = false
    await loadProductVendors()
  } catch (error) {
    console.error('Failed to save vendor:', error)
    alert(error.response?.data?.message || 'Xatolik yuz berdi')
  } finally {
    savingVendor.value = false
  }
}

async function handleRemoveVendor(pv) {
  if (!confirm(`${pv.vendorName} ni o'chirmoqchimisiz?`)) return
  try {
    await productsApi.removeVendor(route.params.id, pv.id)
    await loadProductVendors()
  } catch (error) {
    console.error('Failed to remove vendor:', error)
  }
}

// Available vendors (not yet linked)
const availableVendors = computed(() => {
  if (editingVendorLink.value) return vendors.value
  const linkedIds = new Set(productVendors.value.map(pv => pv.vendorId))
  return vendors.value.filter(v => !linkedIds.has(v.id))
})

function formatCurrency(value) {
  if (value == null) return '-'
  return new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value)
}

onMounted(async () => {
  loading.value = true
  try {
    const promises = [
      categoriesApi.getAll(),
      brandsApi.getAll(),
      uomApi.getAll(),
      suppliersApi.getAll({ size: 500 })
    ]

    const [categoriesRes, brandsRes, uomsRes, vendorsRes] = await Promise.all(promises)

    // Backend returns a list directly (not paginated)
    categories.value = categoriesRes.data.data || categoriesRes.data || []
    brands.value = brandsRes.data.data || brandsRes.data || []
    uoms.value = uomsRes.data.data || uomsRes.data || []
    const vendorData = vendorsRes.data.data || vendorsRes.data || []
    vendors.value = Array.isArray(vendorData) ? vendorData : vendorData.content || []

    if (isEdit.value) {
      const productRes = await productsApi.getById(route.params.id)
      const productData = productRes.data.data || productRes.data
      Object.assign(form, productData)
      form.categoryId = productData.category?.id || productData.categoryId
      form.brandId = productData.brand?.id || productData.brandId
      form.baseUomId = productData.baseUom?.id || productData.baseUomId
      await Promise.all([loadImages(), loadProductVendors()])
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
      router.push('/inventory/products')
    } else {
      const res = await productsApi.create(form)
      const created = res.data.data || res.data
      // Redirect to edit page so user can immediately add vendors/images
      router.replace(`/inventory/products/${created.id}/edit`)
    }
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

          <!-- Product Images -->
          <div class="md:col-span-2">
            <label class="label">Rasmlar</label>

            <!-- Edit mode: full image management -->
            <div v-if="isEdit" class="space-y-3">
              <!-- Image gallery -->
              <div v-if="productImages.length" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
                <div
                  v-for="img in productImages"
                  :key="img.id"
                  class="relative group rounded-lg border border-gray-200 overflow-hidden bg-gray-50"
                >
                  <img :src="img.imageUrl" :alt="img.altText || 'Product image'" class="w-full h-28 object-cover" />
                  <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                    <button
                      type="button"
                      @click="handleSetPrimary(img.id)"
                      class="p-1.5 bg-white rounded-full hover:bg-yellow-50 transition-colors"
                      :title="img.primary ? 'Asosiy rasm' : 'Asosiy qilish'"
                    >
                      <StarIconSolid v-if="img.primary" class="h-4 w-4 text-yellow-500" />
                      <StarIcon v-else class="h-4 w-4 text-gray-600" />
                    </button>
                    <button
                      type="button"
                      @click="handleDeleteImage(img.id)"
                      class="p-1.5 bg-white rounded-full hover:bg-red-50 transition-colors"
                      title="O'chirish"
                    >
                      <TrashIcon class="h-4 w-4 text-red-600" />
                    </button>
                  </div>
                  <div v-if="img.primary" class="absolute top-1 left-1">
                    <span class="bg-yellow-500 text-white text-[10px] font-medium px-1.5 py-0.5 rounded">Asosiy</span>
                  </div>
                </div>
              </div>

              <!-- Upload area -->
              <label
                class="flex flex-col items-center justify-center w-full h-28 border-2 border-dashed border-gray-300 rounded-lg cursor-pointer hover:border-primary-400 hover:bg-primary-50/50 transition-colors"
                :class="{ 'opacity-50 pointer-events-none': uploading }"
              >
                <div class="flex flex-col items-center">
                  <PhotoIcon class="h-8 w-8 text-gray-400" />
                  <span class="mt-1 text-sm text-gray-500">
                    {{ uploading ? 'Yuklanmoqda...' : 'Rasm yuklash uchun bosing' }}
                  </span>
                  <span class="text-xs text-gray-400 mt-0.5">PNG, JPG, max 10MB</span>
                </div>
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  multiple
                  class="hidden"
                  @change="handleImageUpload"
                />
              </label>

              <p v-if="uploadError" class="text-sm text-red-600">{{ uploadError }}</p>
            </div>

            <!-- Create mode: hint -->
            <div v-else class="flex items-center gap-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg">
              <PhotoIcon class="h-5 w-5 text-gray-400 flex-shrink-0" />
              <span class="text-sm text-gray-500">Rasmlarni mahsulot yaratilgandan keyin qo'shish mumkin</span>
            </div>
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

      <!-- Vendors/Suppliers -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">Yetkazib beruvchilar</h3>
          <button
            v-if="isEdit"
            type="button"
            @click="openAddVendorModal"
            class="btn-primary text-sm flex items-center gap-1"
          >
            <PlusIcon class="h-4 w-4" />
            Qo'shish
          </button>
        </div>
        <div class="card-body">
          <!-- Edit mode: vendor list -->
          <div v-if="isEdit">
            <div v-if="productVendors.length === 0" class="text-center py-6 text-gray-500 text-sm">
              Yetkazib beruvchilar hali qo'shilmagan
            </div>

            <div v-else class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Yetkazib beruvchi</th>
                    <th>Vendor SKU</th>
                    <th class="text-right">Narxi</th>
                    <th class="text-right">Min buyurtma</th>
                    <th class="text-center">Yetkazish (kun)</th>
                    <th class="text-center">Holat</th>
                    <th class="text-right">Amallar</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="pv in productVendors" :key="pv.id">
                    <td>
                      <div class="flex items-center gap-2">
                        <div>
                          <p class="font-medium">{{ pv.vendorName }}</p>
                          <p class="text-xs text-gray-500">{{ pv.vendorCode }}</p>
                        </div>
                        <span v-if="pv.preferred" class="bg-yellow-100 text-yellow-800 text-[10px] font-medium px-1.5 py-0.5 rounded">
                          Asosiy
                        </span>
                      </div>
                    </td>
                    <td class="text-sm text-gray-500">{{ pv.vendorSku || '-' }}</td>
                    <td class="text-right text-sm">{{ pv.unitCost != null ? formatCurrency(pv.unitCost) : '-' }}</td>
                    <td class="text-right text-sm">{{ pv.minOrderQuantity != null ? formatCurrency(pv.minOrderQuantity) : '-' }}</td>
                    <td class="text-center text-sm">{{ pv.leadTimeDays != null ? pv.leadTimeDays : '-' }}</td>
                    <td class="text-center">
                      <span :class="['badge text-xs', pv.active ? 'badge-info' : 'badge-danger']">
                        {{ pv.active ? 'Faol' : 'Nofaol' }}
                      </span>
                    </td>
                    <td class="text-right">
                      <div class="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          @click="openEditVendorModal(pv)"
                          class="p-1.5 text-gray-400 hover:text-primary-600 rounded hover:bg-gray-100"
                          title="Tahrirlash"
                        >
                          <PencilIcon class="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          @click="handleRemoveVendor(pv)"
                          class="p-1.5 text-gray-400 hover:text-red-600 rounded hover:bg-gray-100"
                          title="O'chirish"
                        >
                          <TrashIcon class="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Create mode: hint -->
          <div v-else class="flex items-center gap-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg">
            <span class="text-sm text-gray-500">Yetkazib beruvchilarni mahsulot yaratilgandan keyin qo'shish mumkin</span>
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

    <!-- Vendor Modal -->
    <div
      v-if="showVendorModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="showVendorModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-lg w-full">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ editingVendorLink ? 'Yetkazib beruvchini tahrirlash' : 'Yetkazib beruvchi qo\'shish' }}
          </h3>
          <button @click="showVendorModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="p-6 space-y-4">
          <div>
            <label class="label">Yetkazib beruvchi *</label>
            <select
              v-model="vendorForm.vendorId"
              class="input"
              :disabled="!!editingVendorLink"
            >
              <option :value="null">Tanlang...</option>
              <option v-for="v in availableVendors" :key="v.id" :value="v.id">
                {{ v.name }} ({{ v.code }})
              </option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Vendor SKU</label>
              <input v-model="vendorForm.vendorSku" type="text" class="input" placeholder="Yetkazib beruvchi artikuli" />
            </div>
            <div>
              <label class="label">Vendor nomi</label>
              <input v-model="vendorForm.vendorProductName" type="text" class="input" placeholder="Vendordagi nomi" />
            </div>
          </div>

          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="label">Narxi</label>
              <input v-model.number="vendorForm.unitCost" type="number" step="0.01" min="0" class="input" />
            </div>
            <div>
              <label class="label">Min buyurtma</label>
              <input v-model.number="vendorForm.minOrderQuantity" type="number" step="1" min="0" class="input" />
            </div>
            <div>
              <label class="label">Yetkazish (kun)</label>
              <input v-model.number="vendorForm.leadTimeDays" type="number" min="0" class="input" />
            </div>
          </div>

          <div>
            <label class="label">Izoh</label>
            <textarea v-model="vendorForm.notes" rows="2" class="input" placeholder="Qo'shimcha ma'lumot..."></textarea>
          </div>

          <div class="flex items-center gap-6">
            <label class="flex items-center">
              <input v-model="vendorForm.preferred" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">Asosiy yetkazib beruvchi</span>
            </label>
            <label class="flex items-center">
              <input v-model="vendorForm.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">Faol</span>
            </label>
          </div>
        </div>

        <div class="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
          <button type="button" @click="showVendorModal = false" class="btn-secondary">
            Bekor qilish
          </button>
          <button
            type="button"
            @click="handleSaveVendor"
            :disabled="savingVendor || !vendorForm.vendorId"
            class="btn-primary"
          >
            {{ savingVendor ? 'Saqlanmoqda...' : 'Saqlash' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
