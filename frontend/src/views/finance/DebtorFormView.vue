<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { arInvoicesApi, customersApi, productsApi } from '@/services/api'
import { ArrowLeftIcon, PlusIcon, TrashIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const loading = ref(false)
const errors = reactive({})

const isEdit = computed(() => !!route.params.id)
const invoiceId = computed(() => route.params.id)

// Customer
const allCustomers = ref([])
const customerSearch = ref('')
const loadingCustomers = ref(false)
const showCustomerDropdown = ref(false)
const selectedCustomer = ref(null)

// Products search
const productSearch = ref('')
const productResults = ref([])
const loadingProducts = ref(false)
const showProductDropdown = ref(false)
const productSearchEmpty = ref(false)

// Form
const form = reactive({
  customerId: null,
  invoiceDate: new Date().toISOString().split('T')[0],
  dueDate: '',
  description: '',
  notes: '',
  lines: []
})

// Calculate totals
const subtotal = computed(() => {
  return form.lines.reduce((sum, line) => {
    const lineTotal = (parseFloat(line.quantity) || 0) * (parseFloat(line.unitPrice) || 0)
    return sum + lineTotal
  }, 0)
})

const totalAmount = computed(() => subtotal.value)

// Filtered customers based on search
const filteredCustomers = computed(() => {
  if (!customerSearch.value) return allCustomers.value
  const q = customerSearch.value.toLowerCase()
  return allCustomers.value.filter(c =>
    (c.name || '').toLowerCase().includes(q) ||
    (c.code || '').toLowerCase().includes(q) ||
    (c.phone || '').toLowerCase().includes(q)
  )
})

onMounted(async () => {
  // Set default due date to 30 days from now
  const due = new Date()
  due.setDate(due.getDate() + 30)
  form.dueDate = due.toISOString().split('T')[0]

  // Preload all customers
  loadingCustomers.value = true
  try {
    const res = await customersApi.getAll({ size: 200 })
    const data = res.data.data || res.data
    allCustomers.value = data?.content || (Array.isArray(data) ? data : [])
  } catch (err) {
    console.error('Failed to load customers:', err)
  } finally {
    loadingCustomers.value = false
  }

  // If editing, load the existing invoice
  if (isEdit.value) {
    loading.value = true
    try {
      const res = await arInvoicesApi.getById(invoiceId.value)
      const invoice = res.data.data || res.data
      form.customerId = invoice.customerId
      form.invoiceDate = invoice.invoiceDate?.split('T')[0] || invoice.invoiceDate
      form.dueDate = invoice.dueDate?.split('T')[0] || invoice.dueDate
      form.description = invoice.description || ''
      form.notes = invoice.notes || ''
      form.lines = (invoice.lines || []).map(line => ({
        productId: line.productId || null,
        productSku: line.productSku || '',
        productName: line.productName || '',
        description: line.description || line.productName || '',
        quantity: line.quantity || 1,
        unitPrice: line.unitPrice || 0,
        unitOfMeasure: line.unitOfMeasure || 'dona'
      }))

      // Set the selected customer from the loaded customers
      const customer = allCustomers.value.find(c => c.id === invoice.customerId)
      if (customer) {
        selectedCustomer.value = customer
      } else if (invoice.customerName) {
        // Create a minimal customer object if not found in list
        selectedCustomer.value = {
          id: invoice.customerId,
          name: invoice.customerName,
          code: invoice.customerCode || ''
        }
      }
    } catch (error) {
      console.error('Failed to load invoice:', error)
      errors.general = t('finance.debtors.loadError')
    } finally {
      loading.value = false
    }
  }
})

function onCustomerFocus() {
  if (!selectedCustomer.value) {
    showCustomerDropdown.value = true
  }
}

function onCustomerInput() {
  showCustomerDropdown.value = true
}

function selectCustomer(customer) {
  selectedCustomer.value = customer
  form.customerId = customer.id
  customerSearch.value = ''
  showCustomerDropdown.value = false

  // Set payment terms as due date if available
  if (customer.paymentTermsDays) {
    const due = new Date()
    due.setDate(due.getDate() + customer.paymentTermsDays)
    form.dueDate = due.toISOString().split('T')[0]
  }
}

function clearCustomer() {
  selectedCustomer.value = null
  form.customerId = null
  customerSearch.value = ''
}

// Product search with debounce
let productSearchTimeout = null

function extractList(res) {
  const d = res.data.data || res.data
  return d?.content || (Array.isArray(d) ? d : [])
}

watch(productSearch, (val) => {
  clearTimeout(productSearchTimeout)
  productSearchEmpty.value = false

  if (!val || val.length < 1) {
    productResults.value = []
    showProductDropdown.value = false
    return
  }

  showProductDropdown.value = true
  loadingProducts.value = true

  productSearchTimeout = setTimeout(async () => {
    try {
      const res = await productsApi.search(val)
      productResults.value = extractList(res)
    } catch (err) {
      // Fallback: load all products and filter client-side
      try {
        const res = await productsApi.getAll({ size: 100 })
        const all = extractList(res)
        const q = val.toLowerCase()
        productResults.value = all.filter(p =>
          (p.name || '').toLowerCase().includes(q) ||
          (p.sku || '').toLowerCase().includes(q) ||
          (p.barcode || '').toLowerCase().includes(q)
        )
      } catch (e) {
        productResults.value = []
        console.error('Product search failed:', e)
      }
    } finally {
      loadingProducts.value = false
      productSearchEmpty.value = productResults.value.length === 0
    }
  }, 300)
})

function addProductLine(product) {
  form.lines.push({
    productId: product.id,
    productSku: product.sku || product.code || '',
    productName: product.name || '',
    description: product.name || '',
    quantity: 1,
    unitPrice: product.sellingPrice || product.price || 0,
    unitOfMeasure: product.uomCode || product.uom?.code || 'dona'
  })
  productSearch.value = ''
  productResults.value = []
  showProductDropdown.value = false
  productSearchEmpty.value = false
}

function addManualLine() {
  form.lines.push({
    productId: null,
    productSku: '',
    productName: '',
    description: '',
    quantity: 1,
    unitPrice: 0,
    unitOfMeasure: 'dona'
  })
}

function removeLine(index) {
  form.lines.splice(index, 1)
}

function lineTotal(line) {
  return (parseFloat(line.quantity) || 0) * (parseFloat(line.unitPrice) || 0)
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])

  if (!form.customerId) errors.customer = t('finance.debtors.customerRequired')
  if (!form.invoiceDate) errors.invoiceDate = t('finance.debtors.invoiceDateRequired')
  if (!form.dueDate) errors.dueDate = t('finance.debtors.dueDateRequired')
  if (form.lines.length === 0) errors.lines = t('finance.debtors.atLeastOneProduct')

  for (let i = 0; i < form.lines.length; i++) {
    const line = form.lines[i]
    if (!line.description?.trim()) {
      errors[`line_${i}_desc`] = t('finance.debtors.descriptionRequired')
    }
    if (!line.quantity || parseFloat(line.quantity) <= 0) {
      errors[`line_${i}_qty`] = t('finance.debtors.quantityRequired')
    }
    if (line.unitPrice === undefined || line.unitPrice === '' || parseFloat(line.unitPrice) < 0) {
      errors[`line_${i}_price`] = t('finance.debtors.priceRequired')
    }
  }

  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  Object.keys(errors).forEach(key => delete errors[key])

  try {
    const payload = {
      customerId: form.customerId,
      invoiceDate: form.invoiceDate,
      dueDate: form.dueDate,
      totalAmount: totalAmount.value,
      description: form.description || null,
      notes: form.notes || null,
      lines: form.lines.map(line => ({
        productId: line.productId || null,
        productSku: line.productSku || null,
        productName: line.productName || null,
        description: line.description || line.productName || t('finance.debtors.defaultProduct'),
        quantity: parseFloat(line.quantity),
        unitPrice: parseFloat(line.unitPrice),
        unitOfMeasure: line.unitOfMeasure || null
      }))
    }

    if (isEdit.value) {
      await arInvoicesApi.update(invoiceId.value, payload)
    } else {
      const res = await arInvoicesApi.create(payload)
      // Auto-post so the invoice moves from DRAFT to PENDING
      // and appears in the debtors list (balance report excludes DRAFTs)
      const newId = res.data?.data?.id || res.data?.id
      if (newId) {
        await arInvoicesApi.post(newId)
      }
    }
    router.push('/finance/debtors')
  } catch (error) {
    const response = error.response?.data
    if (response?.validationErrors && Array.isArray(response.validationErrors)) {
      response.validationErrors.forEach(err => {
        if (err.field) errors[err.field] = err.message
      })
      errors.general = t('fixErrors')
    } else {
      errors.general = response?.message || t('failedToSave')
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
        <h1 class="text-2xl font-bold text-gray-900">{{ isEdit ? $t('finance.debtors.editDebt') : $t('finance.debtors.addDebt') }}</h1>
        <p class="text-sm text-gray-500">{{ isEdit ? $t('finance.debtors.editDebtSubtitle') : $t('finance.debtors.addDebtSubtitle') }}</p>
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

      <!-- Customer & Date -->
      <div class="card !overflow-visible">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('finance.debtors.basicInfo') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-3 gap-6">
          <!-- Customer search -->
          <div class="relative md:col-span-1">
            <label class="label">{{ $t('customer') }} *</label>
            <div v-if="selectedCustomer" class="flex items-center gap-2 p-2 bg-primary-50 border border-primary-200 rounded-lg">
              <div class="flex-1">
                <p class="font-medium text-sm">{{ selectedCustomer.name }}</p>
                <p v-if="selectedCustomer.code" class="text-xs text-gray-500">{{ selectedCustomer.code }}</p>
              </div>
              <button type="button" @click="clearCustomer" class="p-1 text-gray-400 hover:text-red-500">
                <TrashIcon class="h-4 w-4" />
              </button>
            </div>
            <div v-else>
              <div class="relative">
                <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  v-model="customerSearch"
                  @input="onCustomerInput"
                  @focus="onCustomerFocus"
                  @blur="setTimeout(() => showCustomerDropdown = false, 200)"
                  type="text"
                  :placeholder="loadingCustomers ? $t('loading') : $t('finance.debtors.searchCustomer')"
                  :class="[errors.customer ? 'input-error pl-9' : 'input pl-9']"
                />
              </div>
              <!-- Dropdown -->
              <div
                v-if="showCustomerDropdown && filteredCustomers.length > 0"
                class="absolute z-20 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto"
              >
                <button
                  v-for="c in filteredCustomers"
                  :key="c.id"
                  type="button"
                  @mousedown.prevent="selectCustomer(c)"
                  class="w-full text-left px-4 py-2 hover:bg-gray-50 border-b border-gray-100 last:border-0"
                >
                  <p class="font-medium text-sm">{{ c.name }}</p>
                  <p class="text-xs text-gray-500">{{ c.code || '' }} {{ c.phone ? '| ' + c.phone : '' }}</p>
                </button>
              </div>
              <div
                v-if="showCustomerDropdown && filteredCustomers.length === 0 && !loadingCustomers && customerSearch"
                class="absolute z-20 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg px-4 py-3 text-center text-sm text-gray-400"
              >
                {{ $t('finance.debtors.customerNotFound') }}
              </div>
            </div>
            <p v-if="errors.customer" class="mt-1 text-sm text-red-600">{{ errors.customer }}</p>
          </div>

          <div>
            <label class="label">{{ $t('finance.debtors.invoiceDate') }} *</label>
            <input v-model="form.invoiceDate" type="date" :class="[errors.invoiceDate ? 'input-error' : 'input']" />
            <p v-if="errors.invoiceDate" class="mt-1 text-sm text-red-600">{{ errors.invoiceDate }}</p>
          </div>

          <div>
            <label class="label">{{ $t('finance.debtors.dueDate') }} *</label>
            <input v-model="form.dueDate" type="date" :class="[errors.dueDate ? 'input-error' : 'input']" />
            <p v-if="errors.dueDate" class="mt-1 text-sm text-red-600">{{ errors.dueDate }}</p>
          </div>
        </div>
      </div>

      <!-- Line Items -->
      <div class="card !overflow-visible">
        <div class="card-header">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-medium">{{ $t('finance.debtors.products') }}</h3>
          </div>
        </div>
        <div class="card-body space-y-4">
          <!-- Product search bar -->
          <div class="relative">
            <div class="flex gap-2">
              <div class="flex-1 relative">
                <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  v-model="productSearch"
                  @blur="setTimeout(() => { showProductDropdown = false; productSearchEmpty = false }, 200)"
                  @focus="productSearch && (showProductDropdown = true)"
                  type="text"
                  :placeholder="$t('finance.debtors.searchProduct')"
                  class="input pl-9"
                />
              </div>
              <button
                type="button"
                @click="addManualLine"
                class="btn-secondary flex items-center gap-1 whitespace-nowrap"
              >
                <PlusIcon class="h-4 w-4" />
                {{ $t('finance.debtors.manualAdd') }}
              </button>
            </div>

            <!-- Product dropdown -->
            <div
              v-if="showProductDropdown"
              class="absolute z-20 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto"
            >
              <div v-if="loadingProducts" class="px-4 py-3 text-center text-sm text-gray-400">
                {{ $t('searching') }}
              </div>
              <template v-else-if="productResults.length > 0">
                <button
                  v-for="p in productResults"
                  :key="p.id"
                  type="button"
                  @mousedown.prevent="addProductLine(p)"
                  class="w-full text-left px-4 py-3 hover:bg-gray-50 border-b border-gray-100 last:border-0 flex items-center justify-between"
                >
                  <div>
                    <p class="font-medium text-sm">{{ p.name }}</p>
                    <p class="text-xs text-gray-500">
                      {{ p.sku || '' }}
                      {{ p.barcode ? ' | ' + p.barcode : '' }}
                    </p>
                  </div>
                  <span class="text-sm font-semibold text-gray-700">{{ formatCurrency(p.sellingPrice || 0) }} {{ $t('sum') }}</span>
                </button>
              </template>
              <div v-else-if="productSearchEmpty" class="px-4 py-3 text-center text-sm text-gray-400">
                {{ $t('noData') }}
              </div>
            </div>
          </div>

          <p v-if="errors.lines" class="text-sm text-red-600">{{ errors.lines }}</p>

          <!-- Lines table -->
          <div v-if="form.lines.length > 0" class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="bg-gray-50 text-gray-600">
                  <th class="px-3 py-2 text-left font-medium w-8">№</th>
                  <th class="px-3 py-2 text-left font-medium">{{ $t('product') }} / {{ $t('description') }}</th>
                  <th class="px-3 py-2 text-center font-medium w-24">{{ $t('quantity') }}</th>
                  <th class="px-3 py-2 text-right font-medium w-36">{{ $t('price') }}</th>
                  <th class="px-3 py-2 text-right font-medium w-36">{{ $t('total') }}</th>
                  <th class="px-3 py-2 w-12"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="(line, index) in form.lines" :key="index">
                  <td class="px-3 py-2 text-gray-500">{{ index + 1 }}</td>
                  <td class="px-3 py-2">
                    <input
                      v-model="line.description"
                      type="text"
                      :placeholder="line.productName || $t('description')"
                      :class="[errors[`line_${index}_desc`] ? 'input-error text-sm' : 'input text-sm']"
                    />
                    <p v-if="line.productSku" class="text-xs text-gray-400 mt-1">{{ line.productSku }}</p>
                    <p v-if="errors[`line_${index}_desc`]" class="text-xs text-red-600 mt-1">{{ errors[`line_${index}_desc`] }}</p>
                  </td>
                  <td class="px-3 py-2">
                    <input
                      v-model="line.quantity"
                      type="number"
                      step="0.01"
                      min="0.01"
                      :class="[errors[`line_${index}_qty`] ? 'input-error text-sm text-center' : 'input text-sm text-center']"
                    />
                    <p v-if="errors[`line_${index}_qty`]" class="text-xs text-red-600 mt-1">{{ errors[`line_${index}_qty`] }}</p>
                  </td>
                  <td class="px-3 py-2">
                    <input
                      v-model="line.unitPrice"
                      type="number"
                      step="0.01"
                      min="0"
                      :class="[errors[`line_${index}_price`] ? 'input-error text-sm text-right' : 'input text-sm text-right']"
                    />
                    <p v-if="errors[`line_${index}_price`]" class="text-xs text-red-600 mt-1">{{ errors[`line_${index}_price`] }}</p>
                  </td>
                  <td class="px-3 py-2 text-right font-medium">
                    {{ formatCurrency(lineTotal(line)) }} {{ $t('sum') }}
                  </td>
                  <td class="px-3 py-2 text-center">
                    <button
                      type="button"
                      @click="removeLine(index)"
                      class="p-1 text-gray-400 hover:text-red-500 rounded"
                    >
                      <TrashIcon class="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <tr class="bg-gray-50 font-semibold">
                  <td colspan="4" class="px-3 py-3 text-right">{{ $t('total') }}:</td>
                  <td class="px-3 py-3 text-right text-lg text-red-600">{{ formatCurrency(totalAmount) }} {{ $t('sum') }}</td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div v-else class="text-center py-8 bg-gray-50 rounded-lg">
            <p class="text-gray-500">{{ $t('finance.debtors.noProducts') }}</p>
            <p class="text-sm text-gray-400 mt-1">{{ $t('finance.debtors.noProductsHint') }}</p>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('finance.debtors.additional') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('description') }}</label>
            <textarea v-model="form.description" rows="3" class="input" :placeholder="$t('description')"></textarea>
          </div>
          <div>
            <label class="label">{{ $t('notes') }}</label>
            <textarea v-model="form.notes" rows="3" class="input" :placeholder="$t('notes')"></textarea>
          </div>
        </div>
      </div>

      <!-- Submit -->
      <div class="flex justify-between items-center">
        <div class="text-sm text-gray-500">
          <span v-if="form.lines.length > 0">{{ form.lines.length }} {{ $t('items') }} {{ $t('product') }}, {{ $t('total') }}: <strong class="text-red-600">{{ formatCurrency(totalAmount) }} {{ $t('sum') }}</strong></span>
        </div>
        <div class="flex gap-3">
          <button type="button" @click="router.back()" class="btn-secondary">{{ $t('cancel') }}</button>
          <button
            type="submit"
            :disabled="saving || form.lines.length === 0"
            class="btn-primary"
          >
            {{ saving ? $t('saving') : (isEdit ? $t('save') : $t('save')) }}
          </button>
        </div>
      </div>
    </form>
  </div>
</template>
