<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { receivingApi, suppliersApi, locationsApi, productsApi, uomApi, purchaseOrdersApi } from '@/services/api'
import { ArrowLeftIcon, PlusIcon, TrashIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const router = useRouter()
const saving = ref(false)
const vendors = ref([])
const locations = ref([])
const products = ref([])
const uoms = ref([])
const purchaseOrders = ref([])
const productSearch = ref('')
const errors = reactive({})

const form = reactive({
  vendorId: null,
  locationId: null,
  purchaseOrderId: null,
  receivingDate: new Date().toISOString().split('T')[0],
  vendorDeliveryNote: '',
  vendorInvoiceNumber: '',
  vendorInvoiceDate: '',
  discountAmount: null,
  shippingAmount: null,
  currency: 'UZS',
  notes: '',
  internalNotes: '',
  lines: []
})

const total = computed(() => {
  const subtotal = form.lines.reduce((sum, l) => sum + (l.receivedQuantity * l.unitCost), 0)
  return subtotal - (form.discountAmount || 0) + (form.shippingAmount || 0)
})

onMounted(async () => {
  try {
    const [vendorsRes, locationsRes, uomsRes, posRes] = await Promise.all([
      suppliersApi.getAll({ size: 200 }),
      locationsApi.getAll(),
      uomApi.getActive(),
      purchaseOrdersApi.getByStatus('APPROVED', { size: 200 })
    ])
    vendors.value = vendorsRes.data.data?.content || vendorsRes.data.content || vendorsRes.data || []
    const locData = locationsRes.data.data || locationsRes.data
    locations.value = locData?.content || locData || []
    uoms.value = uomsRes.data.data || uomsRes.data || []
    purchaseOrders.value = posRes.data.data?.content || posRes.data.content || posRes.data || []
  } catch (error) {
    console.error('Failed to load form data:', error)
  }
})

async function searchProducts() {
  if (!productSearch.value.trim()) {
    products.value = []
    return
  }
  try {
    const response = await productsApi.search(productSearch.value)
    products.value = response.data.data?.content || response.data.content || response.data || []
  } catch (error) {
    console.error('Search failed:', error)
  }
}

function addProduct(product) {
  const defaultUom = uoms.value.find(u => u.id === product.uomId) || uoms.value[0]
  form.lines.push({
    productId: product.id,
    productName: product.name,
    productSku: product.sku,
    receivedQuantity: 1,
    expectedQuantity: null,
    rejectedQuantity: 0,
    uomId: defaultUom?.id || null,
    uomName: defaultUom?.name || '',
    unitCost: product.costPrice || 0,
    taxPercent: null,
    batchNumber: '',
    notes: ''
  })
  productSearch.value = ''
  products.value = []
}

function removeLine(index) {
  form.lines.splice(index, 1)
}

function loadPOLines() {
  if (!form.purchaseOrderId) return

  const po = purchaseOrders.value.find(p => p.id === form.purchaseOrderId)
  if (po) {
    form.vendorId = po.vendorId
    form.locationId = po.locationId
  }

  purchaseOrdersApi.getById(form.purchaseOrderId).then(res => {
    const poData = res.data.data || res.data
    if (poData.lines?.length) {
      form.lines = poData.lines.map(line => ({
        poLineId: line.id,
        productId: line.productId,
        productName: line.productName,
        productSku: line.productSku || '',
        expectedQuantity: line.quantity,
        receivedQuantity: line.quantity - (line.receivedQuantity || 0),
        rejectedQuantity: 0,
        uomId: line.uomId,
        uomName: line.uomName || '',
        unitCost: line.unitPrice || 0,
        taxPercent: line.taxPercent || null,
        batchNumber: '',
        notes: ''
      }))
    }
  }).catch(err => console.error('Failed to load PO lines:', err))
}

function validate() {
  Object.keys(errors).forEach(k => delete errors[k])
  if (!form.vendorId) errors.vendorId = t('inventory.receiving.form.vendorRequired')
  if (!form.locationId) errors.locationId = t('inventory.receiving.form.locationRequired')
  if (!form.receivingDate) errors.receivingDate = t('inventory.receiving.form.dateRequired')
  if (form.lines.length === 0) errors.lines = t('inventory.receiving.form.linesRequired')
  return Object.keys(errors).length === 0
}

async function submitForm() {
  if (!validate()) return

  saving.value = true
  try {
    const payload = {
      vendorId: form.vendorId,
      locationId: form.locationId,
      purchaseOrderId: form.purchaseOrderId || null,
      receivingDate: form.receivingDate,
      vendorDeliveryNote: form.vendorDeliveryNote || null,
      vendorInvoiceNumber: form.vendorInvoiceNumber || null,
      vendorInvoiceDate: form.vendorInvoiceDate || null,
      discountAmount: form.discountAmount,
      shippingAmount: form.shippingAmount,
      currency: form.currency,
      notes: form.notes || null,
      internalNotes: form.internalNotes || null,
      lines: form.lines.map(l => ({
        poLineId: l.poLineId || null,
        productId: l.productId,
        expectedQuantity: l.expectedQuantity,
        receivedQuantity: l.receivedQuantity,
        rejectedQuantity: l.rejectedQuantity || 0,
        uomId: l.uomId,
        unitCost: l.unitCost,
        taxPercent: l.taxPercent,
        batchNumber: l.batchNumber || null,
        notes: l.notes || null
      }))
    }

    await receivingApi.create(payload)
    router.push('/inventory/receiving')
  } catch (error) {
    errors.api = error.response?.data?.message || t('inventory.receiving.form.saveError')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center gap-4">
      <RouterLink to="/inventory/receiving" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
        <ArrowLeftIcon class="h-5 w-5" />
      </RouterLink>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.receiving.newReceiving') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.receiving.form.subtitle') }}</p>
      </div>
    </div>

    <div v-if="errors.api" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-4">
      {{ errors.api }}
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Main Form -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Basic Info Card -->
        <div class="card p-6 space-y-4">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">{{ $t('inventory.receiving.form.basicInfo') }}</h3>

          <!-- PO Selector -->
          <div>
            <label class="label">{{ $t('inventory.receiving.form.purchaseOrder') }}</label>
            <select v-model="form.purchaseOrderId" @change="loadPOLines" class="input">
              <option :value="null">{{ $t('inventory.receiving.form.noPO') }}</option>
              <option v-for="po in purchaseOrders" :key="po.id" :value="po.id">
                {{ po.poNumber }} — {{ po.vendorName }}
              </option>
            </select>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('inventory.receiving.vendor') }} <span class="text-red-500">*</span></label>
              <select v-model="form.vendorId" :class="['input', { 'border-red-500': errors.vendorId }]">
                <option :value="null" disabled>{{ $t('inventory.receiving.form.selectVendor') }}</option>
                <option v-for="v in vendors" :key="v.id" :value="v.id">{{ v.name }}</option>
              </select>
              <p v-if="errors.vendorId" class="text-sm text-red-500 mt-1">{{ errors.vendorId }}</p>
            </div>
            <div>
              <label class="label">{{ $t('inventory.receiving.location') }} <span class="text-red-500">*</span></label>
              <select v-model="form.locationId" :class="['input', { 'border-red-500': errors.locationId }]">
                <option :value="null" disabled>{{ $t('inventory.receiving.form.selectLocation') }}</option>
                <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
              </select>
              <p v-if="errors.locationId" class="text-sm text-red-500 mt-1">{{ errors.locationId }}</p>
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label class="label">{{ $t('date') }} <span class="text-red-500">*</span></label>
              <input v-model="form.receivingDate" type="date" :class="['input', { 'border-red-500': errors.receivingDate }]" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.receiving.vendorInvoice') }}</label>
              <input v-model="form.vendorInvoiceNumber" type="text" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('inventory.receiving.deliveryNote') }}</label>
              <input v-model="form.vendorDeliveryNote" type="text" class="input" />
            </div>
          </div>
        </div>

        <!-- Lines Card -->
        <div class="card p-6 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">{{ $t('inventory.receiving.form.items') }}</h3>
          </div>

          <!-- Product Search -->
          <div class="relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="productSearch"
              type="text"
              :placeholder="$t('inventory.receiving.form.searchProduct')"
              class="input pl-10"
              @input="searchProducts"
            />
            <div v-if="products.length > 0" class="absolute z-20 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
              <button
                v-for="product in products" :key="product.id"
                @click="addProduct(product)"
                class="w-full text-left px-4 py-2.5 hover:bg-gray-50 flex justify-between items-center border-b border-gray-100 last:border-0"
              >
                <div>
                  <p class="text-sm font-medium">{{ product.name }}</p>
                  <p class="text-xs text-gray-400 font-mono">{{ product.sku }}</p>
                </div>
                <PlusIcon class="h-4 w-4 text-primary-600" />
              </button>
            </div>
          </div>

          <p v-if="errors.lines" class="text-sm text-red-500">{{ errors.lines }}</p>

          <!-- Lines Table -->
          <div v-if="form.lines.length > 0" class="border rounded-lg overflow-x-auto">
            <table class="table text-sm">
              <thead>
                <tr>
                  <th class="text-xs">{{ $t('inventory.receiving.product') }}</th>
                  <th class="text-xs w-20">{{ $t('inventory.receiving.expected') }}</th>
                  <th class="text-xs w-20">{{ $t('inventory.receiving.received') }}</th>
                  <th class="text-xs w-28">{{ $t('inventory.receiving.uom') }}</th>
                  <th class="text-xs w-28">{{ $t('inventory.receiving.unitCost') }}</th>
                  <th class="text-xs w-20">{{ $t('inventory.receiving.form.taxPercent') }}</th>
                  <th class="text-xs text-right w-28">{{ $t('inventory.receiving.lineTotal') }}</th>
                  <th class="w-10"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="(line, index) in form.lines" :key="index">
                  <td>
                    <p class="font-medium text-sm">{{ line.productName }}</p>
                    <p v-if="line.productSku" class="text-xs text-gray-400 font-mono">{{ line.productSku }}</p>
                  </td>
                  <td>
                    <input v-model.number="line.expectedQuantity" type="number" min="0" class="input input-sm w-full" />
                  </td>
                  <td>
                    <input v-model.number="line.receivedQuantity" type="number" min="0" step="1" class="input input-sm w-full" />
                  </td>
                  <td>
                    <select v-model="line.uomId" class="input input-sm w-full">
                      <option v-for="u in uoms" :key="u.id" :value="u.id">{{ u.name }}</option>
                    </select>
                  </td>
                  <td>
                    <input v-model.number="line.unitCost" type="number" min="0" step="0.01" class="input input-sm w-full" />
                  </td>
                  <td>
                    <input v-model.number="line.taxPercent" type="number" min="0" max="100" step="0.1" class="input input-sm w-full" />
                  </td>
                  <td class="text-right font-medium text-sm">
                    {{ new Intl.NumberFormat('uz-UZ').format((line.receivedQuantity || 0) * (line.unitCost || 0)) }}
                  </td>
                  <td>
                    <button @click="removeLine(index)" class="p-1 text-gray-400 hover:text-red-600 rounded">
                      <TrashIcon class="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="text-center py-8 text-gray-400 text-sm">
            {{ $t('inventory.receiving.form.noItems') }}
          </div>
        </div>
      </div>

      <!-- Sidebar -->
      <div class="space-y-6">
        <!-- Totals -->
        <div class="card p-6 space-y-3">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">{{ $t('inventory.receiving.form.summary') }}</h3>

          <div>
            <label class="label">{{ $t('inventory.receiving.discount') }}</label>
            <input v-model.number="form.discountAmount" type="number" min="0" step="0.01" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('inventory.receiving.shipping') }}</label>
            <input v-model.number="form.shippingAmount" type="number" min="0" step="0.01" class="input" />
          </div>
          <div class="border-t pt-3 flex justify-between font-bold text-lg">
            <span>{{ $t('total') }}</span>
            <span>{{ new Intl.NumberFormat('uz-UZ').format(total) }} {{ $t('sum') }}</span>
          </div>
        </div>

        <!-- Notes -->
        <div class="card p-6 space-y-3">
          <h3 class="text-sm font-semibold text-gray-900 uppercase tracking-wide">{{ $t('inventory.receiving.notes') }}</h3>
          <div>
            <label class="label">{{ $t('inventory.receiving.form.publicNotes') }}</label>
            <textarea v-model="form.notes" rows="2" class="input"></textarea>
          </div>
          <div>
            <label class="label">{{ $t('inventory.receiving.form.internalNotes') }}</label>
            <textarea v-model="form.internalNotes" rows="2" class="input"></textarea>
          </div>
        </div>

        <!-- Submit -->
        <button @click="submitForm" :disabled="saving" class="btn btn-primary w-full">
          <span v-if="saving" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
          {{ $t('inventory.receiving.form.createReceiving') }}
        </button>
      </div>
    </div>
  </div>
</template>
