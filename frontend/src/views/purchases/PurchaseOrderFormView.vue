<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { purchaseOrdersApi, suppliersApi, productsApi, locationsApi, unwrapList } from '@/services/api'
import { ArrowLeftIcon, PlusIcon, TrashIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const router = useRouter()
const saving = ref(false)
const suppliers = ref([])
const locations = ref([])
const products = ref([])
const productSearch = ref('')
const errors = reactive({})

const form = reactive({
  vendorId: null,
  locationId: null,
  orderDate: new Date().toISOString().split('T')[0], // Default to today
  expectedDate: '',
  currency: 'UZS',
  paymentTerms: '',
  shippingMethod: '',
  shippingAddress: '',
  notes: '',
  internalNotes: '',
  vendorReference: '',
  lines: []
})

const total = computed(() => {
  return form.lines.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0)
})

onMounted(async () => {
  try {
    const [suppliersRes, locationsRes] = await Promise.all([
      suppliersApi.getAll({ size: 100 }),
      locationsApi.getAll({ size: 100 })
    ])
    suppliers.value = unwrapList(suppliersRes)
    locations.value = unwrapList(locationsRes)
  } catch (error) {
    console.error('Failed to load data:', error)
  }
})

async function searchProducts() {
  if (!productSearch.value.trim()) {
    products.value = []
    return
  }
  try {
    const response = await productsApi.search(productSearch.value)
    products.value = unwrapList(response)
  } catch (error) {
    console.error('Search failed:', error)
  }
}

function addProduct(product) {
  const existing = form.lines.find(i => i.productId === product.id)
  if (existing) {
    existing.quantity++
  } else {
    form.lines.push({
      productId: product.id,
      productName: product.name,
      sku: product.sku,
      quantity: 1,
      uomId: product.baseUomId || product.baseUom?.id,
      uomName: product.baseUom?.name || 'PCS',
      unitPrice: product.costPrice || 0,
      discountPercent: 0,
      taxPercent: 0,
      notes: ''
    })
  }
  productSearch.value = ''
  products.value = []
}

function removeItem(index) {
  form.lines.splice(index, 1)
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])

  if (!form.vendorId) errors.vendorId = t('purchases.orderForm.supplierRequired')
  if (!form.locationId) errors.locationId = t('purchases.orderForm.warehouseRequired')
  if (!form.orderDate) errors.orderDate = t('required')
  if (form.lines.length === 0) errors.lines = t('purchases.orderForm.itemsRequired')

  // Validate line items
  form.lines.forEach((line, index) => {
    if (!line.uomId) errors[`line_${index}_uom`] = t('required')
  })

  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  Object.keys(errors).forEach(key => delete errors[key])

  try {
    await purchaseOrdersApi.create({
      vendorId: form.vendorId,
      locationId: form.locationId,
      orderDate: form.orderDate,
      expectedDate: form.expectedDate || null,
      currency: form.currency || null,
      paymentTerms: form.paymentTerms || null,
      shippingMethod: form.shippingMethod || null,
      shippingAddress: form.shippingAddress || null,
      notes: form.notes || null,
      internalNotes: form.internalNotes || null,
      vendorReference: form.vendorReference || null,
      lines: form.lines.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        uomId: item.uomId,
        unitPrice: item.unitPrice,
        discountPercent: item.discountPercent || null,
        taxPercent: item.taxPercent || null,
        notes: item.notes || null
      }))
    })
    router.push('/purchases/orders')
  } catch (error) {
    const response = error.response?.data
    if (response?.validationErrors && Array.isArray(response.validationErrors)) {
      response.validationErrors.forEach(err => {
        if (err.field) {
          errors[err.field] = err.message
        }
      })
      errors.general = t('fixErrors')
    } else {
      errors.general = response?.message || t('failedToSave')
    }
  } finally {
    saving.value = false
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('purchases.orderForm.newOrder') }}</h1>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-6">
      <!-- General Error -->
      <div v-if="errors.general" class="p-4 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600">{{ errors.general }}</p>
      </div>

      <!-- Order Info -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('purchases.orderForm.basicInfo') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label class="label">{{ $t('purchases.orders.supplier') }} *</label>
            <select v-model="form.vendorId" :class="[errors.vendorId ? 'input-error' : 'input']">
              <option :value="null">{{ $t('purchases.orderForm.selectSupplier') }}</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
            <p v-if="errors.vendorId" class="mt-1 text-sm text-red-600">{{ errors.vendorId }}</p>
          </div>
          <div>
            <label class="label">{{ $t('purchases.orders.warehouse') }} *</label>
            <select v-model="form.locationId" :class="[errors.locationId ? 'input-error' : 'input']">
              <option :value="null">{{ $t('purchases.orderForm.selectWarehouse') }}</option>
              <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
            </select>
            <p v-if="errors.locationId" class="mt-1 text-sm text-red-600">{{ errors.locationId }}</p>
          </div>
          <div>
            <label class="label">{{ $t('purchases.orderForm.orderDate') }} *</label>
            <input v-model="form.orderDate" type="date" :class="[errors.orderDate ? 'input-error' : 'input']" />
            <p v-if="errors.orderDate" class="mt-1 text-sm text-red-600">{{ errors.orderDate }}</p>
          </div>
          <div>
            <label class="label">{{ $t('purchases.orderForm.expectedDate') }}</label>
            <input v-model="form.expectedDate" type="date" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('purchases.orderForm.currency') }}</label>
            <select v-model="form.currency" class="input">
              <option value="UZS">UZS</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
              <option value="RUB">RUB</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('purchases.orderForm.vendorReference') }}</label>
            <input v-model="form.vendorReference" type="text" class="input" :placeholder="$t('purchases.orderForm.vendorReference')" />
          </div>
        </div>
      </div>

      <!-- Shipping & Payment -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('purchases.orderForm.shippingAndPayment') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('purchases.orderForm.paymentTerms') }}</label>
            <select v-model="form.paymentTerms" class="input">
              <option value="">{{ $t('purchases.orderForm.selectPaymentTerms') }}</option>
              <option value="PREPAID">{{ $t('purchases.orderForm.prepaid') }}</option>
              <option value="NET15">{{ $t('purchases.orderForm.net15') }}</option>
              <option value="NET30">{{ $t('purchases.orderForm.net30') }}</option>
              <option value="NET60">{{ $t('purchases.orderForm.net60') }}</option>
              <option value="COD">{{ $t('purchases.orderForm.cod') }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('purchases.orderForm.shippingMethod') }}</label>
            <input v-model="form.shippingMethod" type="text" class="input" :placeholder="$t('purchases.orderForm.shippingMethod')" />
          </div>
          <div class="md:col-span-2">
            <label class="label">{{ $t('purchases.orderForm.shippingAddress') }}</label>
            <textarea v-model="form.shippingAddress" rows="2" class="input" :placeholder="$t('address')"></textarea>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('notes') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('purchases.orderForm.notes') }}</label>
            <textarea v-model="form.notes" rows="3" class="input" :placeholder="$t('notes')"></textarea>
          </div>
          <div>
            <label class="label">{{ $t('purchases.orderForm.internalNotes') }}</label>
            <textarea v-model="form.internalNotes" rows="3" class="input" :placeholder="$t('purchases.orderForm.internalNotes')"></textarea>
          </div>
        </div>
      </div>

      <!-- Products -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('purchases.orders.productsCount') }}</h3></div>
        <div class="card-body">
          <!-- Error for lines -->
          <div v-if="errors.lines" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-600">{{ errors.lines }}</p>
          </div>

          <!-- Search -->
          <div class="relative mb-4">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="productSearch"
              @input="searchProducts"
              type="text"
              :placeholder="$t('purchases.orderForm.selectProduct')"
              class="input pl-10"
            />
          </div>

          <!-- Search Results -->
          <div v-if="products.length > 0" class="mb-4 border rounded-lg divide-y max-h-48 overflow-y-auto">
            <button
              v-for="product in products"
              :key="product.id"
              @click="addProduct(product)"
              type="button"
              class="w-full p-3 text-left hover:bg-gray-50 flex justify-between"
            >
              <div>
                <div class="font-medium">{{ product.name }}</div>
                <div class="text-sm text-gray-500">{{ product.sku }} | {{ $t('purchases.orderForm.uom') }}: {{ product.baseUom?.name || '-' }}</div>
              </div>
              <div class="text-right">
                <div class="font-medium">{{ formatCurrency(product.costPrice) }}</div>
              </div>
            </button>
          </div>

          <!-- Items Table -->
          <div v-if="form.lines.length > 0" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ $t('purchases.orderForm.product') }}</th>
                  <th class="text-center">{{ $t('quantity') }}</th>
                  <th class="text-center">{{ $t('purchases.orderForm.uom') }}</th>
                  <th class="text-right">{{ $t('purchases.orderForm.unitPrice') }}</th>
                  <th class="text-right">{{ $t('purchases.orderForm.lineTotal') }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="(item, index) in form.lines" :key="item.productId">
                  <td>
                    <div class="font-medium">{{ item.productName }}</div>
                    <div class="text-sm text-gray-500">{{ item.sku }}</div>
                  </td>
                  <td class="text-center">
                    <input
                      v-model.number="item.quantity"
                      type="number"
                      min="1"
                      class="input w-20 text-center"
                    />
                  </td>
                  <td class="text-center">
                    <span class="text-sm text-gray-600">{{ item.uomName }}</span>
                  </td>
                  <td class="text-right">
                    <input
                      v-model.number="item.unitPrice"
                      type="number"
                      min="0"
                      step="0.01"
                      class="input w-28 text-right"
                    />
                  </td>
                  <td class="text-right font-medium">{{ formatCurrency(item.quantity * item.unitPrice) }}</td>
                  <td>
                    <button @click="removeItem(index)" type="button" class="p-2 text-gray-400 hover:text-red-600">
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr class="bg-gray-50">
                  <td colspan="4" class="text-right font-medium">{{ $t('total') }}:</td>
                  <td class="text-right text-lg font-bold text-primary-600">{{ formatCurrency(total) }}</td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div v-else class="text-center py-8 text-gray-500">
            {{ $t('purchases.orderForm.noItems') }}
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">{{ $t('cancel') }}</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : $t('save') }}
        </button>
      </div>
    </form>
  </div>
</template>
