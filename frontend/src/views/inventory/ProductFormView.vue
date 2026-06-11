<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { productsApi, categoriesApi, brandsApi, uomApi, suppliersApi } from '@/services/api'
import { ArrowLeftIcon, PhotoIcon, TrashIcon, StarIcon, PlusIcon, PencilIcon, XMarkIcon, ScaleIcon, SparklesIcon, CheckCircleIcon, XCircleIcon } from '@heroicons/vue/24/outline'
import { StarIcon as StarIconSolid } from '@heroicons/vue/24/solid'

const toast = useToastStore()

const { t } = useI18n()

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
      uploadError.value = t('inventory.productForm.imageOnlyError')
      continue
    }
    if (file.size > 10 * 1024 * 1024) {
      uploadError.value = t('inventory.productForm.imageSizeError')
      continue
    }

    try {
      await productsApi.uploadImage(route.params.id, file)
    } catch (error) {
      uploadError.value = error.response?.data?.message || t('inventory.productForm.imageUploadError')
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
    toast.error(error.response?.data?.message || t('failedToSave'))
  } finally {
    savingVendor.value = false
  }
}

async function handleRemoveVendor(pv) {
  if (!confirm(t('inventory.productForm.confirmRemoveVendor', { name: pv.vendorName }))) return
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

// ==================== Alternate UOM Management ====================
const productAltUoms = ref([])
const showAltUomModal = ref(false)
const editingAltUom = ref(null)
const savingAltUom = ref(false)
const altUomForm = reactive({
  uomId: null,
  conversionFactor: null,
  sellingPrice: null,
  defaultSale: false,
  active: true,
  sortOrder: 0
})

async function loadProductAltUoms() {
  if (!isEdit.value) return
  try {
    const res = await productsApi.getUoms(route.params.id)
    productAltUoms.value = res.data.data || res.data || []
  } catch (error) {
    console.error('Failed to load product UOMs:', error)
  }
}

function openAddAltUomModal() {
  editingAltUom.value = null
  Object.assign(altUomForm, {
    uomId: null,
    conversionFactor: null,
    sellingPrice: null,
    defaultSale: false,
    active: true,
    sortOrder: 0
  })
  showAltUomModal.value = true
}

function openEditAltUomModal(pu) {
  editingAltUom.value = pu
  Object.assign(altUomForm, {
    uomId: pu.uomId,
    conversionFactor: pu.conversionFactor,
    sellingPrice: pu.sellingPrice,
    defaultSale: pu.defaultSale,
    active: pu.active,
    sortOrder: pu.sortOrder || 0
  })
  showAltUomModal.value = true
}

async function handleSaveAltUom() {
  if (!altUomForm.uomId || !altUomForm.conversionFactor) return

  savingAltUom.value = true
  try {
    if (editingAltUom.value) {
      await productsApi.updateUom(route.params.id, editingAltUom.value.id, altUomForm)
    } else {
      await productsApi.addUom(route.params.id, altUomForm)
    }
    showAltUomModal.value = false
    await loadProductAltUoms()
  } catch (error) {
    console.error('Failed to save product UOM:', error)
    toast.error(error.response?.data?.message || t('failedToSave'))
  } finally {
    savingAltUom.value = false
  }
}

async function handleRemoveAltUom(pu) {
  if (!confirm(t('inventory.productForm.confirmRemoveUom', { name: pu.uomName }))) return
  try {
    await productsApi.removeUom(route.params.id, pu.id)
    await loadProductAltUoms()
  } catch (error) {
    console.error('Failed to remove product UOM:', error)
  }
}

// Available UOMs for alternate (not the base UOM, not already linked)
const availableAltUoms = computed(() => {
  if (editingAltUom.value) return uoms.value
  const linkedIds = new Set(productAltUoms.value.map(pu => pu.uomId))
  linkedIds.add(form.baseUomId) // exclude base UOM
  return uoms.value.filter(u => !linkedIds.has(u.id))
})

// ==================== SKU / Barcode Generators ====================
const skuValidation = ref(null)
const barcodeValidation = ref(null)

async function generateSku() {
  try {
    const res = await productsApi.generateSku()
    form.sku = res.data.sku
    skuValidation.value = null
  } catch (error) {
    console.error('Failed to generate SKU:', error)
  }
}

async function generateSkuFromName() {
  if (!form.name?.trim()) return
  try {
    const res = await productsApi.generateSkuFromName(form.name)
    form.sku = res.data.sku
    skuValidation.value = null
  } catch (error) {
    console.error('Failed to generate SKU from name:', error)
  }
}

async function validateSku() {
  if (!form.sku?.trim()) { skuValidation.value = null; return }
  try {
    const res = await productsApi.validateSku(form.sku)
    skuValidation.value = res.data
  } catch (error) {
    console.error('Failed to validate SKU:', error)
  }
}

async function generateBarcode() {
  try {
    const res = await productsApi.generateBarcode()
    form.barcode = res.data.barcode
    barcodeValidation.value = null
  } catch (error) {
    console.error('Failed to generate barcode:', error)
  }
}

async function generateEan13() {
  try {
    const res = await productsApi.generateEan13('860')
    form.barcode = res.data.barcode
    barcodeValidation.value = null
  } catch (error) {
    console.error('Failed to generate EAN-13:', error)
  }
}

async function validateBarcode() {
  if (!form.barcode?.trim()) { barcodeValidation.value = null; return }
  try {
    const res = await productsApi.validateBarcode(form.barcode)
    barcodeValidation.value = res.data
  } catch (error) {
    console.error('Failed to validate barcode:', error)
  }
}

// ==================== Variant Management ====================
const productVariants = ref([])
const showVariantModal = ref(false)
const editingVariant = ref(null)
const savingVariant = ref(false)
const variantForm = reactive({
  name: '',
  sku: '',
  barcode: '',
  costPrice: null,
  sellingPrice: null,
  active: true
})

async function loadVariants() {
  if (!isEdit.value) return
  try {
    const res = await productsApi.getVariants(route.params.id)
    productVariants.value = res.data || []
  } catch (error) {
    console.error('Failed to load variants:', error)
  }
}

function openAddVariantModal() {
  editingVariant.value = null
  Object.assign(variantForm, { name: '', sku: '', barcode: '', costPrice: null, sellingPrice: null, active: true })
  showVariantModal.value = true
}

function openEditVariantModal(v) {
  editingVariant.value = v
  Object.assign(variantForm, {
    name: v.name || '',
    sku: v.sku || '',
    barcode: v.barcode || '',
    costPrice: v.costPrice,
    sellingPrice: v.sellingPrice,
    active: v.active
  })
  showVariantModal.value = true
}

async function handleSaveVariant() {
  if (!variantForm.name?.trim()) return
  savingVariant.value = true
  try {
    if (editingVariant.value) {
      await productsApi.updateVariant(route.params.id, editingVariant.value.id, variantForm)
    } else {
      await productsApi.addVariant(route.params.id, variantForm)
    }
    showVariantModal.value = false
    await loadVariants()
  } catch (error) {
    console.error('Failed to save variant:', error)
    toast.error(error.response?.data?.message || t('failedToSave'))
  } finally {
    savingVariant.value = false
  }
}

async function handleDeleteVariant(v) {
  if (!confirm(t('inventory.productForm.confirmDeleteVariant', { name: v.name }))) return
  try {
    await productsApi.deleteVariant(route.params.id, v.id)
    await loadVariants()
  } catch (error) {
    console.error('Failed to delete variant:', error)
  }
}

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
      await Promise.all([loadImages(), loadProductVendors(), loadProductAltUoms(), loadVariants()])
    }
  } catch (error) {
    console.error('Failed to load data:', error)
  } finally {
    loading.value = false
  }
})

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])

  if (!form.sku?.trim()) errors.sku = t('inventory.productForm.skuRequired')
  if (!form.name?.trim()) errors.name = t('inventory.productForm.nameRequired')
  if (!form.baseUomId) errors.baseUomId = t('inventory.productForm.uomRequired')
  if (form.sellingPrice <= 0) errors.sellingPrice = t('inventory.productForm.sellingPriceRequired')

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
          {{ isEdit ? $t('inventory.productForm.editProduct') : $t('inventory.productForm.newProduct') }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          {{ isEdit ? $t('inventory.productForm.editSubtitle') : $t('inventory.productForm.newSubtitle') }}
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
          <h3 class="text-lg font-medium">{{ $t('inventory.productForm.basicInfo') }}</h3>
        </div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('inventory.products.sku') }} *</label>
            <div class="flex gap-2">
              <div class="flex-1 relative">
                <input v-model="form.sku" @blur="validateSku" type="text" :class="[errors.sku ? 'input-error' : 'input']" />
                <div v-if="skuValidation" class="absolute right-2 top-1/2 -translate-y-1/2">
                  <CheckCircleIcon v-if="skuValidation.available" class="h-5 w-5 text-green-500" />
                  <XCircleIcon v-else class="h-5 w-5 text-red-500" />
                </div>
              </div>
              <button type="button" @click="generateSku" class="btn-secondary text-xs px-2" :title="$t('inventory.productForm.generateSku')">
                <SparklesIcon class="h-4 w-4" />
              </button>
              <button v-if="form.name" type="button" @click="generateSkuFromName" class="btn-secondary text-xs px-2" :title="$t('inventory.productForm.generateFromName')">
                SKU
              </button>
            </div>
            <p v-if="errors.sku" class="mt-1 text-sm text-red-600">{{ errors.sku }}</p>
            <p v-if="skuValidation && !skuValidation.available" class="mt-1 text-xs text-red-500">
              {{ skuValidation.exists ? $t('inventory.productForm.skuExists') : $t('inventory.productForm.skuInvalid') }}
            </p>
          </div>

          <div>
            <label class="label">{{ $t('inventory.productForm.barcode') }}</label>
            <div class="flex gap-2">
              <div class="flex-1 relative">
                <input v-model="form.barcode" @blur="validateBarcode" type="text" class="input" />
                <div v-if="barcodeValidation" class="absolute right-2 top-1/2 -translate-y-1/2">
                  <CheckCircleIcon v-if="barcodeValidation.available" class="h-5 w-5 text-green-500" />
                  <XCircleIcon v-else class="h-5 w-5 text-red-500" />
                </div>
              </div>
              <button type="button" @click="generateBarcode" class="btn-secondary text-xs px-2" :title="$t('inventory.productForm.generateBarcode')">
                <SparklesIcon class="h-4 w-4" />
              </button>
              <button type="button" @click="generateEan13" class="btn-secondary text-xs px-2" :title="$t('inventory.productForm.generateEan13')">
                EAN
              </button>
            </div>
            <p v-if="barcodeValidation && !barcodeValidation.available" class="mt-1 text-xs text-red-500">
              {{ barcodeValidation.exists ? $t('inventory.productForm.barcodeExists') : $t('inventory.productForm.barcodeInvalid') }}
            </p>
            <p v-if="barcodeValidation?.type" class="mt-1 text-xs text-gray-500">
              {{ $t('inventory.productForm.barcodeType') }}: {{ barcodeValidation.type }}
            </p>
          </div>

          <div class="md:col-span-2">
            <label class="label">{{ $t('name') }} *</label>
            <input v-model="form.name" type="text" :class="[errors.name ? 'input-error' : 'input']" />
            <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
          </div>

          <div class="md:col-span-2">
            <label class="label">{{ $t('description') }}</label>
            <textarea v-model="form.description" rows="3" class="input"></textarea>
          </div>

          <!-- Product Images -->
          <div class="md:col-span-2">
            <label class="label">{{ $t('inventory.productForm.images') }}</label>

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
                      :title="img.primary ? $t('inventory.productForm.primaryImage') : $t('inventory.productForm.setPrimary')"
                    >
                      <StarIconSolid v-if="img.primary" class="h-4 w-4 text-yellow-500" />
                      <StarIcon v-else class="h-4 w-4 text-gray-600" />
                    </button>
                    <button
                      type="button"
                      @click="handleDeleteImage(img.id)"
                      class="p-1.5 bg-white rounded-full hover:bg-red-50 transition-colors"
                      :title="$t('delete')"
                    >
                      <TrashIcon class="h-4 w-4 text-red-600" />
                    </button>
                  </div>
                  <div v-if="img.primary" class="absolute top-1 left-1">
                    <span class="bg-yellow-500 text-white text-[10px] font-medium px-1.5 py-0.5 rounded">{{ $t('inventory.productForm.primaryImage') }}</span>
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
                    {{ uploading ? $t('inventory.productForm.uploading') : $t('inventory.productForm.uploadImage') }}
                  </span>
                  <span class="text-xs text-gray-400 mt-0.5">{{ $t('inventory.productForm.imageFormats') }}</span>
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
              <span class="text-sm text-gray-500">{{ $t('inventory.productForm.imagesAfterCreate') }}</span>
            </div>
          </div>

          <div>
            <label class="label">{{ $t('inventory.products.category') }}</label>
            <select v-model="form.categoryId" class="input">
              <option :value="null">{{ $t('inventory.productForm.selectCategory') }}</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>

          <div>
            <label class="label">{{ $t('inventory.productForm.selectBrand') }}</label>
            <select v-model="form.brandId" class="input">
              <option :value="null">{{ $t('inventory.productForm.selectBrand') }}</option>
              <option v-for="brand in brands" :key="brand.id" :value="brand.id">
                {{ brand.name }}
              </option>
            </select>
          </div>

          <div>
            <label class="label">{{ $t('inventory.productForm.unitOfMeasure') }} *</label>
            <select v-model="form.baseUomId" :class="[errors.baseUomId ? 'input-error' : 'input']">
              <option :value="null">{{ $t('inventory.productForm.selectUom') }}</option>
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
          <h3 class="text-lg font-medium">{{ $t('inventory.productForm.pricing') }}</h3>
        </div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div>
            <label class="label">{{ $t('inventory.productForm.costPrice') }}</label>
            <input v-model.number="form.costPrice" type="number" step="0.01" min="0" class="input" />
          </div>

          <div>
            <label class="label">{{ $t('inventory.productForm.sellingPrice') }} *</label>
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
            <label class="label">{{ $t('inventory.productForm.minSellingPrice') }}</label>
            <input v-model.number="form.minSellingPrice" type="number" step="0.01" min="0" class="input" />
          </div>

          <div>
            <label class="label">{{ $t('inventory.productForm.wholesalePrice') }}</label>
            <input v-model.number="form.wholesalePrice" type="number" step="0.01" min="0" class="input" />
          </div>
        </div>
      </div>

      <!-- Vendors/Suppliers -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">{{ $t('inventory.productForm.vendors') }}</h3>
          <button
            v-if="isEdit"
            type="button"
            @click="openAddVendorModal"
            class="btn-primary text-sm flex items-center gap-1"
          >
            <PlusIcon class="h-4 w-4" />
            {{ $t('inventory.productForm.addVendor') }}
          </button>
        </div>
        <div class="card-body">
          <!-- Edit mode: vendor list -->
          <div v-if="isEdit">
            <div v-if="productVendors.length === 0" class="text-center py-6 text-gray-500 text-sm">
              {{ $t('inventory.productForm.noVendors') }}
            </div>

            <div v-else class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>{{ $t('inventory.productForm.vendorName') }}</th>
                    <th>{{ $t('inventory.productForm.vendorSku') }}</th>
                    <th class="text-right">{{ $t('inventory.productForm.unitCost') }}</th>
                    <th class="text-right">{{ $t('inventory.productForm.minOrder') }}</th>
                    <th class="text-center">{{ $t('inventory.productForm.leadTime') }}</th>
                    <th class="text-center">{{ $t('status') }}</th>
                    <th class="text-right">{{ $t('actions') }}</th>
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
                          {{ $t('inventory.productForm.preferred') }}
                        </span>
                      </div>
                    </td>
                    <td class="text-sm text-gray-500">{{ pv.vendorSku || '-' }}</td>
                    <td class="text-right text-sm">{{ pv.unitCost != null ? formatCurrency(pv.unitCost) : '-' }}</td>
                    <td class="text-right text-sm">{{ pv.minOrderQuantity != null ? formatCurrency(pv.minOrderQuantity) : '-' }}</td>
                    <td class="text-center text-sm">{{ pv.leadTimeDays != null ? pv.leadTimeDays : '-' }}</td>
                    <td class="text-center">
                      <span :class="['badge text-xs', pv.active ? 'badge-info' : 'badge-danger']">
                        {{ pv.active ? $t('active') : $t('inactive') }}
                      </span>
                    </td>
                    <td class="text-right">
                      <div class="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          @click="openEditVendorModal(pv)"
                          class="p-1.5 text-gray-400 hover:text-primary-600 rounded hover:bg-gray-100"
                          :title="$t('edit')"
                        >
                          <PencilIcon class="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          @click="handleRemoveVendor(pv)"
                          class="p-1.5 text-gray-400 hover:text-red-600 rounded hover:bg-gray-100"
                          :title="$t('delete')"
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
            <span class="text-sm text-gray-500">{{ $t('inventory.productForm.vendorsAfterCreate') }}</span>
          </div>
        </div>
      </div>

      <!-- Alternate UOMs (Sale measurement units) -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <div>
            <h3 class="text-lg font-medium">{{ $t('inventory.productForm.altUoms') }}</h3>
            <p class="text-sm text-gray-500 mt-0.5">{{ $t('inventory.productForm.altUomsSubtitle') }}</p>
          </div>
          <button
            v-if="isEdit"
            type="button"
            @click="openAddAltUomModal"
            class="btn-primary text-sm flex items-center gap-1"
          >
            <PlusIcon class="h-4 w-4" />
            {{ $t('inventory.productForm.addAltUom') }}
          </button>
        </div>
        <div class="card-body">
          <!-- Edit mode: UOM list -->
          <div v-if="isEdit">
            <div v-if="productAltUoms.length === 0" class="text-center py-6 text-gray-500 text-sm">
              {{ $t('inventory.productForm.noAltUoms') }}
            </div>

            <div v-else class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>{{ $t('inventory.productForm.uom') }}</th>
                    <th class="text-right">{{ $t('inventory.productForm.conversionFactor') }}</th>
                    <th class="text-right">{{ $t('price') }}</th>
                    <th class="text-center">{{ $t('status') }}</th>
                    <th class="text-right">{{ $t('actions') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="pu in productAltUoms" :key="pu.id">
                    <td>
                      <div class="flex items-center gap-2">
                        <ScaleIcon class="h-4 w-4 text-gray-400" />
                        <div>
                          <p class="font-medium">{{ pu.uomName }} ({{ pu.uomCode }})</p>
                          <p class="text-xs text-gray-500">1 {{ pu.uomCode }} = {{ pu.conversionFactor }} {{ $t('inventory.productForm.baseUnit') }}</p>
                        </div>
                        <span v-if="pu.defaultSale" class="bg-blue-100 text-blue-800 text-[10px] font-medium px-1.5 py-0.5 rounded">
                          {{ $t('inventory.productForm.standard') }}
                        </span>
                      </div>
                    </td>
                    <td class="text-right text-sm font-mono">{{ pu.conversionFactor }}</td>
                    <td class="text-right text-sm">
                      <div>
                        <span class="font-medium">{{ formatCurrency(pu.effectiveSellingPrice) }}</span>
                        <span v-if="pu.sellingPrice" class="text-xs text-gray-400 ml-1">({{ $t('inventory.productForm.set') }})</span>
                        <span v-else class="text-xs text-gray-400 ml-1">({{ $t('inventory.productForm.calculated') }})</span>
                      </div>
                    </td>
                    <td class="text-center">
                      <span :class="['badge text-xs', pu.active ? 'badge-info' : 'badge-danger']">
                        {{ pu.active ? $t('active') : $t('inactive') }}
                      </span>
                    </td>
                    <td class="text-right">
                      <div class="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          @click="openEditAltUomModal(pu)"
                          class="p-1.5 text-gray-400 hover:text-primary-600 rounded hover:bg-gray-100"
                          :title="$t('edit')"
                        >
                          <PencilIcon class="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          @click="handleRemoveAltUom(pu)"
                          class="p-1.5 text-gray-400 hover:text-red-600 rounded hover:bg-gray-100"
                          :title="$t('delete')"
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
            <ScaleIcon class="h-5 w-5 text-gray-400 flex-shrink-0" />
            <span class="text-sm text-gray-500">{{ $t('inventory.productForm.altUomsAfterCreate') }}</span>
          </div>
        </div>
      </div>

      <!-- Variants -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">{{ $t('inventory.productForm.variants') }}</h3>
          <button
            v-if="isEdit"
            type="button"
            @click="openAddVariantModal"
            class="btn-primary text-sm flex items-center gap-1"
          >
            <PlusIcon class="h-4 w-4" />
            {{ $t('inventory.productForm.addVariant') }}
          </button>
        </div>
        <div class="card-body">
          <div v-if="isEdit">
            <div v-if="productVariants.length === 0" class="text-center py-6 text-gray-500 text-sm">
              {{ $t('inventory.productForm.noVariants') }}
            </div>
            <div v-else class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>{{ $t('name') }}</th>
                    <th>{{ $t('inventory.products.sku') }}</th>
                    <th>{{ $t('inventory.productForm.barcode') }}</th>
                    <th class="text-right">{{ $t('inventory.productForm.costPrice') }}</th>
                    <th class="text-right">{{ $t('inventory.productForm.sellingPrice') }}</th>
                    <th class="text-center">{{ $t('status') }}</th>
                    <th class="text-right">{{ $t('actions') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="v in productVariants" :key="v.id">
                    <td class="font-medium">{{ v.name }}</td>
                    <td class="font-mono text-sm text-gray-500">{{ v.sku || '-' }}</td>
                    <td class="text-sm text-gray-500">{{ v.barcode || '-' }}</td>
                    <td class="text-right text-sm">{{ v.costPrice != null ? formatCurrency(v.costPrice) : '-' }}</td>
                    <td class="text-right text-sm">{{ v.sellingPrice != null ? formatCurrency(v.sellingPrice) : '-' }}</td>
                    <td class="text-center">
                      <span :class="['badge text-xs', v.active ? 'badge-info' : 'badge-danger']">
                        {{ v.active ? $t('active') : $t('inactive') }}
                      </span>
                    </td>
                    <td class="text-right">
                      <div class="flex items-center justify-end gap-1">
                        <button type="button" @click="openEditVariantModal(v)" class="p-1.5 text-gray-400 hover:text-primary-600 rounded hover:bg-gray-100">
                          <PencilIcon class="h-4 w-4" />
                        </button>
                        <button type="button" @click="handleDeleteVariant(v)" class="p-1.5 text-gray-400 hover:text-red-600 rounded hover:bg-gray-100">
                          <TrashIcon class="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div v-else class="flex items-center gap-2 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg">
            <span class="text-sm text-gray-500">{{ $t('inventory.productForm.variantsAfterCreate') }}</span>
          </div>
        </div>
      </div>

      <!-- Inventory -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">{{ $t('inventory.productForm.inventorySettings') }}</h3>
        </div>
        <div class="card-body space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label class="label">{{ $t('inventory.productForm.minStockLevel') }}</label>
              <input v-model.number="form.minStockLevel" type="number" min="0" class="input" />
            </div>

            <div>
              <label class="label">{{ $t('inventory.productForm.reorderPoint') }}</label>
              <input v-model.number="form.reorderPoint" type="number" min="0" class="input" />
            </div>

            <div>
              <label class="label">{{ $t('inventory.productForm.reorderQuantity') }}</label>
              <input v-model.number="form.reorderQuantity" type="number" min="0" class="input" />
            </div>
          </div>

          <div class="flex flex-wrap gap-6">
            <label class="flex items-center">
              <input v-model="form.trackInventory" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.productForm.trackInventory') }}</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.allowNegativeStock" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.productForm.allowNegativeStock') }}</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('active') }}</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.sellable" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.productForm.sellable') }}</span>
            </label>

            <label class="flex items-center">
              <input v-model="form.purchasable" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.productForm.purchasable') }}</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">
          {{ $t('cancel') }}
        </button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : (isEdit ? $t('inventory.productForm.updateProduct') : $t('inventory.productForm.createProduct')) }}
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
            {{ editingVendorLink ? $t('inventory.productForm.editVendor') : $t('inventory.productForm.addVendorTitle') }}
          </h3>
          <button @click="showVendorModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="p-6 space-y-4">
          <div>
            <label class="label">{{ $t('inventory.productForm.vendorName') }} *</label>
            <select
              v-model="vendorForm.vendorId"
              class="input"
              :disabled="!!editingVendorLink"
            >
              <option :value="null">{{ $t('inventory.productForm.vendorNamePlaceholder') }}</option>
              <option v-for="v in availableVendors" :key="v.id" :value="v.id">
                {{ v.name }} ({{ v.code }})
              </option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('inventory.productForm.vendorSku') }}</label>
              <input v-model="vendorForm.vendorSku" type="text" class="input" :placeholder="$t('inventory.productForm.vendorSkuPlaceholder')" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.productForm.vendorName') }}</label>
              <input v-model="vendorForm.vendorProductName" type="text" class="input" :placeholder="$t('inventory.productForm.vendorNamePlaceholder')" />
            </div>
          </div>

          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="label">{{ $t('inventory.productForm.unitCost') }}</label>
              <input v-model.number="vendorForm.unitCost" type="number" step="0.01" min="0" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.productForm.minOrder') }}</label>
              <input v-model.number="vendorForm.minOrderQuantity" type="number" step="1" min="0" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.productForm.leadTime') }}</label>
              <input v-model.number="vendorForm.leadTimeDays" type="number" min="0" class="input" />
            </div>
          </div>

          <div>
            <label class="label">{{ $t('notes') }}</label>
            <textarea v-model="vendorForm.notes" rows="2" class="input"></textarea>
          </div>

          <div class="flex items-center gap-6">
            <label class="flex items-center">
              <input v-model="vendorForm.preferred" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.productForm.preferred') }}</span>
            </label>
            <label class="flex items-center">
              <input v-model="vendorForm.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('active') }}</span>
            </label>
          </div>
        </div>

        <div class="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
          <button type="button" @click="showVendorModal = false" class="btn-secondary">
            {{ $t('cancel') }}
          </button>
          <button
            type="button"
            @click="handleSaveVendor"
            :disabled="savingVendor || !vendorForm.vendorId"
            class="btn-primary"
          >
            {{ savingVendor ? $t('saving') : $t('save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Variant Modal -->
    <div
      v-if="showVariantModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="showVariantModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-lg w-full">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ editingVariant ? $t('inventory.productForm.editVariant') : $t('inventory.productForm.addVariant') }}
          </h3>
          <button @click="showVariantModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="p-6 space-y-4">
          <div>
            <label class="label">{{ $t('name') }} *</label>
            <input v-model="variantForm.name" type="text" class="input" :placeholder="$t('inventory.productForm.variantNamePlaceholder')" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('inventory.products.sku') }}</label>
              <input v-model="variantForm.sku" type="text" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.productForm.barcode') }}</label>
              <input v-model="variantForm.barcode" type="text" class="input" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('inventory.productForm.costPrice') }}</label>
              <input v-model.number="variantForm.costPrice" type="number" step="0.01" min="0" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.productForm.sellingPrice') }}</label>
              <input v-model.number="variantForm.sellingPrice" type="number" step="0.01" min="0" class="input" />
            </div>
          </div>
          <label class="flex items-center">
            <input v-model="variantForm.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
            <span class="ml-2 text-sm text-gray-700">{{ $t('active') }}</span>
          </label>
        </div>

        <div class="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
          <button type="button" @click="showVariantModal = false" class="btn-secondary">
            {{ $t('cancel') }}
          </button>
          <button
            type="button"
            @click="handleSaveVariant"
            :disabled="savingVariant || !variantForm.name?.trim()"
            class="btn-primary"
          >
            {{ savingVariant ? $t('saving') : $t('save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Alternate UOM Modal -->
    <div
      v-if="showAltUomModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="showAltUomModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-lg w-full">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">
            {{ editingAltUom ? $t('inventory.productForm.editAltUom') : $t('inventory.productForm.addAltUom') }}
          </h3>
          <button @click="showAltUomModal = false" class="p-1 text-gray-400 hover:text-gray-600 rounded">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="p-6 space-y-4">
          <div>
            <label class="label">{{ $t('inventory.productForm.uom') }} *</label>
            <select
              v-model="altUomForm.uomId"
              class="input"
              :disabled="!!editingAltUom"
            >
              <option :value="null">{{ $t('inventory.productForm.vendorNamePlaceholder') }}</option>
              <option v-for="u in availableAltUoms" :key="u.id" :value="u.id">
                {{ u.name }} ({{ u.code }})
              </option>
            </select>
          </div>

          <div>
            <label class="label">{{ $t('inventory.productForm.conversionFactor') }} *</label>
            <input
              v-model.number="altUomForm.conversionFactor"
              type="number"
              step="0.000001"
              min="0.000001"
              class="input"
              :placeholder="$t('inventory.productForm.conversionFactorPlaceholder')"
            />
            <p class="text-xs text-gray-500 mt-1">
              {{ $t('inventory.productForm.conversionFactorHint') }}
            </p>
          </div>

          <div>
            <label class="label">{{ $t('inventory.productForm.altSellingPrice') }}</label>
            <input
              v-model.number="altUomForm.sellingPrice"
              type="number"
              step="0.01"
              min="0"
              class="input"
              :placeholder="$t('inventory.productForm.altSellingPriceHint')"
            />
          </div>

          <div class="flex items-center gap-6">
            <label class="flex items-center">
              <input v-model="altUomForm.defaultSale" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('inventory.productForm.defaultSaleUnit') }}</span>
            </label>
            <label class="flex items-center">
              <input v-model="altUomForm.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm text-gray-700">{{ $t('active') }}</span>
            </label>
          </div>
        </div>

        <div class="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
          <button type="button" @click="showAltUomModal = false" class="btn-secondary">
            {{ $t('cancel') }}
          </button>
          <button
            type="button"
            @click="handleSaveAltUom"
            :disabled="savingAltUom || !altUomForm.uomId || !altUomForm.conversionFactor"
            class="btn-primary"
          >
            {{ savingAltUom ? $t('saving') : $t('save') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
