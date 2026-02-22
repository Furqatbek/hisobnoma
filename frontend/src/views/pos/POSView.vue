<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { productsApi, customersApi, posApi, terminalsApi, shiftsApi, deliveryRegionsApi, deliveryVillagesApi } from '@/services/api'
import { ScaleIcon } from '@heroicons/vue/24/outline'
import {
  MagnifyingGlassIcon,
  PlusIcon,
  MinusIcon,
  TrashIcon,
  UserIcon,
  CreditCardIcon,
  BanknotesIcon,
  XMarkIcon,
  CheckIcon,
  DocumentTextIcon,
  ClockIcon,
  ArrowRightStartOnRectangleIcon,
  MapPinIcon,
  PencilSquareIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

// State
const products = ref([])
const quickProducts = ref([])
const customers = ref([])
const searchQuery = ref('')
const loading = ref(false)

// Terminal state
const terminals = ref([])
const selectedTerminalId = ref(null)

// Shift state
const currentShift = ref(null)
const showOpenShiftModal = ref(false)
const showCloseShiftModal = ref(false)
const openingCash = ref(0)
const closingCash = ref(0)
const shiftLoading = ref(false)

const cart = reactive({
  items: [],
  customerId: null,
  customerName: '',
  deliveryRegionId: null,
  deliveryRegionName: '',
  deliveryVillageId: null,
  deliveryVillageName: ''
})

// Delivery address state
const showDeliveryModal = ref(false)
const deliveryRegions = ref([])
const deliveryVillages = ref([])
const selectedRegionId = ref(null)
const selectedVillageId = ref(null)

const showPaymentModal = ref(false)
const showCustomerModal = ref(false)
const customerSearch = ref('')

// Split payment support - array of payments
const payments = ref([])
const currentPayment = reactive({
  method: 'CASH',
  amount: 0,
  received: 0
})

// Tax settings - can be manually controlled
const taxRate = ref(0) // 0% tax by default

// Discount state
const showDiscountModal = ref(false)
const discountTarget = ref('transaction') // 'transaction' or item index
const discountForm = reactive({
  type: 'percent', // 'percent' or 'amount'
  value: 0,
  reason: ''
})
const transactionDiscount = reactive({
  type: null, // 'percent' or 'amount'
  value: 0,
  reason: ''
})

const showNewCustomerModal = ref(false)
const newCustomer = reactive({
  name: '',
  phone: '',
  email: ''
})

// Payment methods available
const paymentMethods = computed(() => [
  { value: 'CASH', label: t('pos.cash'), icon: BanknotesIcon, color: 'text-green-600' },
  { value: 'CARD', label: t('pos.card'), icon: CreditCardIcon, color: 'text-blue-600' },
  { value: 'CREDIT', label: t('pos.credit'), icon: DocumentTextIcon, color: 'text-orange-600' },
  { value: 'MOBILE_PAYMENT', label: t('pos.mobile'), icon: CreditCardIcon, color: 'text-purple-600' }
])

// Computed
const subtotal = computed(() => {
  return cart.items.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

// Total item-level discounts
const itemDiscountTotal = computed(() => {
  return cart.items.reduce((sum, item) => sum + (item.discount || 0), 0)
})

// Transaction-level discount amount
const transactionDiscountAmount = computed(() => {
  if (!transactionDiscount.type) return 0
  const afterItemDiscounts = subtotal.value - itemDiscountTotal.value
  if (transactionDiscount.type === 'percent') {
    return afterItemDiscounts * (transactionDiscount.value / 100)
  }
  return Math.min(transactionDiscount.value, afterItemDiscounts)
})

const totalDiscount = computed(() => itemDiscountTotal.value + transactionDiscountAmount.value)

const discountedSubtotal = computed(() => subtotal.value - totalDiscount.value)
const tax = computed(() => discountedSubtotal.value * (taxRate.value / 100))
const total = computed(() => discountedSubtotal.value + tax.value)

// Total paid from split payments
const totalPaid = computed(() => {
  return payments.value.reduce((sum, p) => sum + p.amount, 0)
})

// Remaining amount to pay
const remainingAmount = computed(() => {
  return Math.max(0, total.value - totalPaid.value)
})

// Change calculation for cash payments
const change = computed(() => {
  if (currentPayment.method === 'CASH') {
    return Math.max(0, currentPayment.received - currentPayment.amount)
  }
  return 0
})

// Check if debt sale requires customer
const isDebtSale = computed(() => {
  return payments.value.some(p => p.method === 'CREDIT') || currentPayment.method === 'CREDIT'
})

// Methods
async function fetchQuickProducts() {
  try {
    const response = await productsApi.getActive({ size: 50 })
    quickProducts.value = response.data.content || response.data || []
  } catch (error) {
    console.error('Failed to load products:', error)
  }
}

async function searchProducts() {
  if (!searchQuery.value.trim()) {
    products.value = []
    return
  }

  loading.value = true
  try {
    const response = await productsApi.search(searchQuery.value)
    products.value = response.data.content || response.data || []
  } catch (error) {
    console.error('Search failed:', error)
  } finally {
    loading.value = false
  }
}

async function searchCustomers() {
  if (!customerSearch.value.trim()) {
    customers.value = []
    return
  }

  try {
    const response = await customersApi.search(customerSearch.value)
    customers.value = response.data.content || response.data.data || response.data || []
  } catch (error) {
    console.error('Customer search failed:', error)
  }
}

// UOM selection state
const showUomModal = ref(false)
const pendingProduct = ref(null)
const productUoms = ref([])
const uomLoading = ref(false)

// Cache of product UOMs to avoid re-fetching
const productUomCache = {}

async function addToCart(product) {
  searchQuery.value = ''
  products.value = []

  // Check if product has alternate UOMs (fetch once, then cache)
  if (!productUomCache[product.id]) {
    try {
      const res = await productsApi.getActiveUoms(product.id)
      productUomCache[product.id] = res.data.data || res.data || []
    } catch {
      productUomCache[product.id] = []
    }
  }

  const altUoms = productUomCache[product.id]

  if (altUoms.length > 0) {
    // Show UOM selection modal
    pendingProduct.value = product
    productUoms.value = altUoms
    showUomModal.value = true
  } else {
    // No alternate UOMs — add directly in base UOM
    addToCartWithUom(product, null)
  }
}

function addToCartWithUom(product, selectedUom) {
  showUomModal.value = false

  const uomKey = selectedUom ? `${product.id}_uom_${selectedUom.id}` : `${product.id}`

  const existingItem = cart.items.find(item => item._uomKey === uomKey)

  if (existingItem) {
    existingItem.quantity++
  } else {
    cart.items.push({
      _uomKey: uomKey,
      productId: product.id,
      name: product.name,
      sku: product.sku,
      price: selectedUom ? selectedUom.effectiveSellingPrice : product.sellingPrice,
      quantity: 1,
      // Alternate UOM info (null = base UOM)
      productUomId: selectedUom?.id || null,
      uomCode: selectedUom?.uomCode || null,
      uomName: selectedUom?.uomName || null,
      conversionFactor: selectedUom?.conversionFactor || null
    })
  }

  pendingProduct.value = null
  productUoms.value = []
}

function selectBaseUom() {
  if (pendingProduct.value) {
    addToCartWithUom(pendingProduct.value, null)
  }
}

function updateQuantity(item, delta) {
  item.quantity += delta
  if (item.quantity <= 0) {
    removeFromCart(item)
  }
}

function removeFromCart(item) {
  const index = cart.items.indexOf(item)
  if (index > -1) {
    cart.items.splice(index, 1)
  }
}

function selectCustomer(customer) {
  cart.customerId = customer.id
  cart.customerName = customer.name
  showCustomerModal.value = false
  customerSearch.value = ''
  customers.value = []
}

function clearCustomer() {
  cart.customerId = null
  cart.customerName = ''
}

// Delivery address functions
async function openDeliveryModal() {
  showDeliveryModal.value = true
  selectedRegionId.value = cart.deliveryRegionId
  selectedVillageId.value = cart.deliveryVillageId
  if (deliveryRegions.value.length === 0) {
    try {
      const response = await deliveryRegionsApi.getActive()
      deliveryRegions.value = response.data.data || response.data || []
    } catch (error) {
      console.error('Failed to fetch regions:', error)
    }
  }
  if (selectedRegionId.value) {
    await fetchVillagesForRegion(selectedRegionId.value)
  }
}

async function fetchVillagesForRegion(regionId) {
  if (!regionId) {
    deliveryVillages.value = []
    return
  }
  try {
    const response = await deliveryVillagesApi.getByRegion(regionId)
    deliveryVillages.value = response.data.data || response.data || []
  } catch (error) {
    console.error('Failed to fetch villages:', error)
    deliveryVillages.value = []
  }
}

async function onRegionChange() {
  selectedVillageId.value = null
  await fetchVillagesForRegion(selectedRegionId.value)
}

function confirmDeliveryAddress() {
  const region = deliveryRegions.value.find(r => r.id === selectedRegionId.value)
  const village = deliveryVillages.value.find(v => v.id === selectedVillageId.value)

  cart.deliveryRegionId = selectedRegionId.value
  cart.deliveryRegionName = region?.name || ''
  cart.deliveryVillageId = selectedVillageId.value
  cart.deliveryVillageName = village?.name || ''
  showDeliveryModal.value = false
}

function clearDeliveryAddress() {
  cart.deliveryRegionId = null
  cart.deliveryRegionName = ''
  cart.deliveryVillageId = null
  cart.deliveryVillageName = ''
}

// Discount functions
function openTransactionDiscount() {
  discountTarget.value = 'transaction'
  discountForm.type = transactionDiscount.type || 'percent'
  discountForm.value = transactionDiscount.value || 0
  discountForm.reason = transactionDiscount.reason || ''
  showDiscountModal.value = true
}

function openItemDiscount(index) {
  const item = cart.items[index]
  discountTarget.value = index
  const itemTotal = item.price * item.quantity
  if (item.discountPercent) {
    discountForm.type = 'percent'
    discountForm.value = item.discountPercent
  } else {
    discountForm.type = 'amount'
    discountForm.value = item.discount || 0
  }
  discountForm.reason = item.discountReason || ''
  showDiscountModal.value = true
}

function applyDiscountFromModal() {
  if (discountForm.value < 0) return

  if (discountTarget.value === 'transaction') {
    transactionDiscount.type = discountForm.type
    transactionDiscount.value = discountForm.value
    transactionDiscount.reason = discountForm.reason
  } else {
    const item = cart.items[discountTarget.value]
    const itemTotal = item.price * item.quantity
    if (discountForm.type === 'percent') {
      item.discountPercent = discountForm.value
      item.discount = itemTotal * (discountForm.value / 100)
    } else {
      item.discountPercent = null
      item.discount = Math.min(discountForm.value, itemTotal)
    }
    item.discountReason = discountForm.reason
  }
  showDiscountModal.value = false
}

function clearTransactionDiscount() {
  transactionDiscount.type = null
  transactionDiscount.value = 0
  transactionDiscount.reason = ''
}

function openPayment() {
  if (cart.items.length === 0) return
  payments.value = []
  currentPayment.method = 'CASH'
  currentPayment.amount = total.value
  currentPayment.received = total.value
  showPaymentModal.value = true
}

function addPaymentSplit() {
  if (currentPayment.amount <= 0) return

  // For debt sales, customer is required
  if (currentPayment.method === 'CREDIT' && !cart.customerId) {
    alert(t('pos.customerRequiredForDebt'))
    return
  }

  const paymentAmount = Math.min(currentPayment.amount, remainingAmount.value)

  payments.value.push({
    method: currentPayment.method,
    amount: paymentAmount,
    received: currentPayment.method === 'CASH' ? currentPayment.received : paymentAmount
  })

  // Reset for next payment
  const newRemaining = total.value - payments.value.reduce((sum, p) => sum + p.amount, 0)
  currentPayment.amount = Math.max(0, newRemaining)
  currentPayment.received = currentPayment.amount
}

function removePaymentSplit(index) {
  payments.value.splice(index, 1)
  // Recalculate current payment amount
  const newRemaining = total.value - payments.value.reduce((sum, p) => sum + p.amount, 0)
  currentPayment.amount = Math.max(0, newRemaining)
  currentPayment.received = currentPayment.amount
}

function selectPaymentMethod(method) {
  currentPayment.method = method
  if (method !== 'CASH') {
    currentPayment.received = currentPayment.amount
  }
}

async function processPayment() {
  // Validate debt sales require customer
  const hasDebt = payments.value.some(p => p.method === 'CREDIT') ||
                  (remainingAmount.value > 0 && currentPayment.method === 'CREDIT')

  if (hasDebt && !cart.customerId) {
    alert(t('pos.customerRequiredForDebt'))
    return
  }

  // Add current payment if there's remaining amount
  if (remainingAmount.value > 0 && currentPayment.amount > 0) {
    addPaymentSplit()
  }

  // Verify total payments
  const finalTotal = payments.value.reduce((sum, p) => sum + p.amount, 0)
  if (finalTotal < total.value) {
    alert(t('pos.paymentLessThanTotal'))
    return
  }

  // Validate terminal is selected
  if (!selectedTerminalId.value) {
    alert(t('pos.noTerminalAvailable'))
    return
  }

  try {
    // Create transaction
    const transactionData = {
      terminalId: selectedTerminalId.value,
      transactionType: 'SALE',
      customerId: cart.customerId,
      deliveryRegionId: cart.deliveryRegionId || undefined,
      deliveryVillageId: cart.deliveryVillageId || undefined,
      items: cart.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.price,
        discountAmount: item.discount || undefined,
        discountReason: item.discountReason || undefined,
        productUomId: item.productUomId || undefined
      }))
    }

    const txResponse = await posApi.createTransaction(transactionData)
    const transactionId = txResponse.data.data?.id || txResponse.data.id

    // Apply transaction-level discount if set
    if (transactionDiscount.type && transactionDiscount.value > 0) {
      const discountData = { reason: transactionDiscount.reason || undefined }
      if (transactionDiscount.type === 'percent') {
        discountData.percent = transactionDiscount.value
      } else {
        discountData.amount = transactionDiscountAmount.value
      }
      await posApi.applyDiscount(transactionId, discountData)
    }

    // Add all payments
    for (const payment of payments.value) {
      await posApi.addPayment(transactionId, {
        paymentType: payment.method,
        amount: payment.amount
      })
    }

    // Complete transaction
    await posApi.completeTransaction(transactionId)

    // Check before clearing
    const hasDebtPayment = payments.value.some(p => p.method === 'CREDIT')

    // Reset cart
    cart.items = []
    cart.customerId = null
    cart.customerName = ''
    clearDeliveryAddress()
    payments.value = []
    clearTransactionDiscount()
    showPaymentModal.value = false

    alert(hasDebtPayment ? t('pos.saleCompletedWithDebt') : t('pos.saleComplete'))
  } catch (error) {
    console.error('Payment failed:', error)
    alert(t('pos.paymentFailed') + ': ' + (error.response?.data?.message || error.message))
  }
}

// Quick debt sale - sell entire amount as debt
async function sellAsDebt() {
  if (!cart.customerId) {
    alert(t('pos.customerRequiredForDebt'))
    showCustomerModal.value = true
    return
  }

  if (!confirm(t('pos.confirmDebtSale', { amount: formatCurrency(total.value), customer: cart.customerName }))) {
    return
  }

  payments.value = [{
    method: 'CREDIT',
    amount: total.value,
    received: 0
  }]

  currentPayment.amount = 0
  await processPayment()
}

function clearCart() {
  if (confirm(t('pos.clearCart') + '?')) {
    cart.items = []
    cart.customerId = null
    cart.customerName = ''
    clearDeliveryAddress()
    clearTransactionDiscount()
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value || 0)
}

async function createQuickCustomer() {
  if (!newCustomer.name?.trim()) {
    alert(t('customers.form.nameRequired'))
    return
  }

  try {
    const response = await customersApi.create({
      name: newCustomer.name,
      phone: newCustomer.phone || null,
      email: newCustomer.email || null
    })
    const customer = response.data.data || response.data
    cart.customerId = customer.id
    cart.customerName = customer.name
    showNewCustomerModal.value = false
    newCustomer.name = ''
    newCustomer.phone = ''
    newCustomer.email = ''
  } catch (error) {
    console.error('Failed to create customer:', error)
    alert(t('pos.failedToCreateCustomer') + ': ' + (error.response?.data?.message || error.message))
  }
}

// Handle barcode scanner input
let barcodeBuffer = ''
let barcodeTimeout = null

async function handleKeydown(event) {
  // Ignore if focused on input
  if (event.target.tagName === 'INPUT') return

  // Barcode scanners typically send characters quickly
  clearTimeout(barcodeTimeout)
  barcodeBuffer += event.key

  barcodeTimeout = setTimeout(async () => {
    if (barcodeBuffer.length >= 6) {
      try {
        const response = await productsApi.getByBarcode(barcodeBuffer.trim())
        if (response.data) {
          addToCart(response.data)
        }
      } catch (error) {
        console.log('Product not found for barcode:', barcodeBuffer)
      }
    }
    barcodeBuffer = ''
  }, 100)
}

async function fetchTerminals() {
  try {
    const response = await terminalsApi.getActive()
    terminals.value = response.data.data || response.data || []
    // Auto-select first terminal if available
    if (terminals.value.length > 0) {
      selectedTerminalId.value = terminals.value[0].id
    }
  } catch (error) {
    console.error('Failed to fetch terminals:', error)
  }
}

// Shift management
async function fetchCurrentShift() {
  if (!selectedTerminalId.value) return
  try {
    const response = await shiftsApi.getCurrentForTerminal(selectedTerminalId.value)
    currentShift.value = response.data.data || null
  } catch (error) {
    currentShift.value = null
  }
}

async function openShift() {
  if (!selectedTerminalId.value) return
  shiftLoading.value = true
  try {
    const response = await shiftsApi.open({
      terminalId: selectedTerminalId.value,
      openingCash: openingCash.value
    })
    currentShift.value = response.data.data || response.data
    showOpenShiftModal.value = false
    openingCash.value = 0
  } catch (error) {
    alert(t('pos.shifts.openError') + ': ' + (error.response?.data?.message || error.message))
  } finally {
    shiftLoading.value = false
  }
}

async function closeShift() {
  if (!currentShift.value) return
  shiftLoading.value = true
  try {
    await shiftsApi.close(currentShift.value.id, {
      closingCash: closingCash.value
    })
    currentShift.value = null
    showCloseShiftModal.value = false
    closingCash.value = 0
  } catch (error) {
    alert(t('pos.shifts.closeError') + ': ' + (error.response?.data?.message || error.message))
  } finally {
    shiftLoading.value = false
  }
}

// Re-fetch shift when terminal changes
watch(selectedTerminalId, () => {
  fetchCurrentShift()
})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  fetchTerminals().then(() => fetchCurrentShift())
  fetchQuickProducts()
})
</script>

<template>
  <div class="h-[calc(100vh-10rem)] flex gap-6">
    <!-- Left Panel - Product Search & Quick Add -->
    <div class="flex-1 flex flex-col">
      <!-- Search -->
      <div class="card mb-4">
        <div class="card-body">
          <div class="relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="searchQuery"
              @input="searchProducts"
              type="text"
              :placeholder="$t('pos.searchPlaceholder')"
              class="input pl-10 text-lg"
              autofocus
            />
          </div>
        </div>
      </div>

      <!-- Search Results -->
      <div v-if="searchQuery && products.length > 0" class="card flex-1 overflow-hidden">
        <div class="card-body overflow-y-auto h-full">
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            <button
              v-for="product in products"
              :key="product.id"
              @click="addToCart(product)"
              class="p-3 border rounded-lg hover:border-primary-500 hover:bg-primary-50 transition-colors text-left"
            >
              <div class="flex items-center gap-2 mb-1">
                <img
                  v-if="product.primaryImageUrl"
                  :src="product.primaryImageUrl"
                  :alt="product.name"
                  class="h-10 w-10 rounded object-cover flex-shrink-0"
                />
                <div class="min-w-0 flex-1">
                  <div class="font-medium text-gray-900 truncate">{{ product.name }}</div>
                  <div class="text-xs text-gray-500">{{ product.sku }}</div>
                </div>
              </div>
              <div class="flex items-center justify-between mt-2">
                <div class="text-lg font-bold text-primary-600">
                  {{ formatCurrency(product.sellingPrice) }}
                </div>
                <div
                  :class="[
                    'text-xs',
                    (product.stockQuantity || 0) > 0 ? 'text-green-600' : 'text-red-600'
                  ]"
                >
                  {{ product.stockQuantity || 0 }}
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Search - No Results -->
      <div v-else-if="searchQuery && products.length === 0 && !loading" class="card flex-1 flex items-center justify-center">
        <div class="text-center text-gray-500">
          <MagnifyingGlassIcon class="h-12 w-12 mx-auto mb-4 text-gray-300" />
          <p>{{ $t('pos.noProductsFound') }}</p>
        </div>
      </div>

      <!-- Quick Product List -->
      <div v-else class="card flex-1 overflow-hidden">
        <div class="card-body overflow-y-auto h-full">
          <div v-if="quickProducts.length > 0" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            <button
              v-for="product in quickProducts"
              :key="product.id"
              @click="addToCart(product)"
              class="p-3 border rounded-lg hover:border-primary-500 hover:bg-primary-50 transition-colors text-left"
            >
              <div class="flex items-center gap-2 mb-1">
                <img
                  v-if="product.primaryImageUrl"
                  :src="product.primaryImageUrl"
                  :alt="product.name"
                  class="h-10 w-10 rounded object-cover flex-shrink-0"
                />
                <div class="min-w-0 flex-1">
                  <div class="font-medium text-gray-900 truncate">{{ product.name }}</div>
                  <div class="text-xs text-gray-500">{{ product.sku }}</div>
                </div>
              </div>
              <div class="flex items-center justify-between mt-2">
                <div class="text-lg font-bold text-primary-600">
                  {{ formatCurrency(product.sellingPrice) }}
                </div>
                <div
                  :class="[
                    'text-xs',
                    (product.stockQuantity || 0) > 0 ? 'text-green-600' : 'text-red-600'
                  ]"
                >
                  {{ product.stockQuantity || 0 }}
                </div>
              </div>
            </button>
          </div>
          <div v-else class="flex items-center justify-center h-full text-gray-500">
            <div class="text-center">
              <MagnifyingGlassIcon class="h-12 w-12 mx-auto mb-4 text-gray-300" />
              <p>{{ $t('pos.noProductsFound') }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Panel - Cart -->
    <div class="w-96 flex flex-col">
      <!-- Terminal Warning -->
      <div v-if="terminals.length === 0" class="mb-2 p-3 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-700">{{ $t('pos.noTerminalAvailable') }}</p>
      </div>

      <!-- Terminal Selector (if multiple) -->
      <div v-if="terminals.length > 1" class="mb-2">
        <select v-model="selectedTerminalId" class="input text-sm">
          <option v-for="terminal in terminals" :key="terminal.id" :value="terminal.id">
            {{ terminal.name || terminal.terminalCode }}
          </option>
        </select>
      </div>

      <!-- Shift Status -->
      <div v-if="currentShift" class="mb-2 p-3 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
        <div class="flex items-center">
          <ClockIcon class="h-4 w-4 text-green-600 mr-2" />
          <span class="text-sm text-green-700 font-medium">{{ $t('pos.shifts.shiftOpen') }}: {{ currentShift.shiftNumber }}</span>
        </div>
        <button @click="showCloseShiftModal = true" class="text-sm text-red-600 hover:text-red-700 font-medium">
          {{ $t('pos.shifts.closeShift') }}
        </button>
      </div>
      <div v-else-if="terminals.length > 0" class="mb-2 p-3 bg-yellow-50 border border-yellow-200 rounded-lg flex items-center justify-between">
        <span class="text-sm text-yellow-700">{{ $t('pos.shifts.noOpenShift') }}</span>
        <button @click="showOpenShiftModal = true" class="btn-primary text-sm py-1 px-3">
          {{ $t('pos.shifts.openShift') }}
        </button>
      </div>

      <div class="card flex-1 flex flex-col overflow-hidden">
        <!-- Cart Header -->
        <div class="card-header flex items-center justify-between">
          <div>
            <h2 class="text-lg font-medium">{{ $t('pos.cart') }}</h2>
            <p v-if="terminals.length === 1" class="text-xs text-gray-500">{{ terminals[0]?.name || terminals[0]?.terminalCode }}</p>
          </div>
          <button
            v-if="cart.items.length > 0"
            @click="clearCart"
            class="text-sm text-red-600 hover:text-red-700"
          >
            {{ $t('pos.clearCart') }}
          </button>
        </div>

        <!-- Customer -->
        <div class="px-4 py-3 border-b bg-gray-50">
          <div v-if="cart.customerId" class="flex items-center justify-between">
            <div class="flex items-center">
              <UserIcon class="h-5 w-5 text-gray-400 mr-2" />
              <span class="font-medium">{{ cart.customerName }}</span>
            </div>
            <button @click="clearCustomer" class="text-gray-400 hover:text-red-500">
              <XMarkIcon class="h-4 w-4" />
            </button>
          </div>
          <div v-else class="flex items-center gap-2">
            <button
              @click="showCustomerModal = true"
              class="flex items-center text-primary-600 hover:text-primary-700 text-sm"
            >
              <MagnifyingGlassIcon class="h-4 w-4 mr-1" />
              {{ $t('pos.selectCustomer') }}
            </button>
            <span class="text-gray-300">|</span>
            <button
              @click="showNewCustomerModal = true"
              class="flex items-center text-green-600 hover:text-green-700 text-sm"
            >
              <PlusIcon class="h-4 w-4 mr-1" />
              {{ $t('pos.quickAdd') }}
            </button>
          </div>
        </div>

        <!-- Delivery Address -->
        <div class="px-4 py-2 border-b bg-gray-50">
          <div v-if="cart.deliveryRegionId" class="flex items-center justify-between">
            <div class="flex items-center min-w-0">
              <MapPinIcon class="h-4 w-4 text-orange-500 mr-2 flex-shrink-0" />
              <div class="truncate">
                <span class="text-sm font-medium text-gray-700">{{ cart.deliveryRegionName }}</span>
                <span v-if="cart.deliveryVillageName" class="text-sm text-gray-500"> / {{ cart.deliveryVillageName }}</span>
              </div>
            </div>
            <div class="flex items-center gap-1 flex-shrink-0">
              <button @click="openDeliveryModal" class="text-primary-500 hover:text-primary-600">
                <PencilSquareIcon class="h-3.5 w-3.5" />
              </button>
              <button @click="clearDeliveryAddress" class="text-gray-400 hover:text-red-500">
                <XMarkIcon class="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
          <div v-else>
            <button
              @click="openDeliveryModal"
              class="flex items-center text-orange-600 hover:text-orange-700 text-sm"
            >
              <MapPinIcon class="h-4 w-4 mr-1" />
              {{ $t('pos.deliveryAddress') }}
            </button>
          </div>
        </div>

        <!-- Cart Items -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="cart.items.length === 0" class="flex items-center justify-center h-full text-gray-500">
            <p>{{ $t('pos.cartEmpty') }}</p>
          </div>

          <ul v-else class="divide-y">
            <li v-for="(item, index) in cart.items" :key="item._uomKey" class="p-4">
              <div class="flex justify-between">
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-gray-900 truncate">{{ item.name }}</p>
                  <div class="flex items-center gap-1.5">
                    <span class="text-sm text-gray-500">{{ item.sku }}</span>
                    <span v-if="item.uomName" class="inline-flex items-center gap-0.5 text-xs px-1.5 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                      <ScaleIcon class="h-3 w-3" />
                      {{ item.uomName }}
                    </span>
                  </div>
                </div>
                <button
                  @click="removeFromCart(item)"
                  class="ml-2 text-gray-400 hover:text-red-500"
                >
                  <TrashIcon class="h-4 w-4" />
                </button>
              </div>

              <div class="flex items-center justify-between mt-2">
                <div class="flex items-center space-x-2">
                  <button
                    @click="updateQuantity(item, -1)"
                    class="p-1 rounded-lg border hover:bg-gray-100"
                  >
                    <MinusIcon class="h-4 w-4" />
                  </button>
                  <span class="w-8 text-center font-medium">{{ item.quantity }}</span>
                  <button
                    @click="updateQuantity(item, 1)"
                    class="p-1 rounded-lg border hover:bg-gray-100"
                  >
                    <PlusIcon class="h-4 w-4" />
                  </button>
                </div>
                <div class="text-right">
                  <span class="font-medium">{{ formatCurrency(item.price * item.quantity) }}</span>
                  <button
                    @click="openItemDiscount(index)"
                    class="ml-2 text-xs px-1.5 py-0.5 rounded border hover:bg-gray-100"
                    :class="item.discount ? 'text-green-600 border-green-300 bg-green-50' : 'text-gray-400 border-gray-200'"
                  >
                    {{ item.discount ? ('-' + formatCurrency(item.discount)) : '%' }}
                  </button>
                </div>
              </div>
            </li>
          </ul>
        </div>

        <!-- Cart Summary -->
        <div class="border-t bg-gray-50 p-4 space-y-2">
          <div class="flex justify-between text-sm">
            <span class="text-gray-500">{{ $t('pos.subtotal') }}</span>
            <span>{{ formatCurrency(subtotal) }}</span>
          </div>

          <!-- Discount row -->
          <div v-if="totalDiscount > 0" class="flex justify-between text-sm text-green-600">
            <span>
              {{ $t('pos.discount') }}
              <span v-if="transactionDiscount.type === 'percent'" class="text-xs">({{ transactionDiscount.value }}%)</span>
            </span>
            <span>-{{ formatCurrency(totalDiscount) }}</span>
          </div>

          <!-- Discount button -->
          <div class="flex items-center gap-2">
            <button
              @click="openTransactionDiscount"
              :disabled="cart.items.length === 0"
              class="text-xs px-2 py-1 rounded border transition-colors"
              :class="transactionDiscount.type ? 'text-green-600 border-green-300 bg-green-50 hover:bg-green-100' : 'text-gray-500 border-gray-200 hover:bg-gray-100'"
            >
              {{ transactionDiscount.type ? $t('pos.editDiscount') : '+ ' + $t('pos.discount') }}
            </button>
            <button
              v-if="transactionDiscount.type"
              @click="clearTransactionDiscount"
              class="text-xs text-red-500 hover:text-red-700"
            >
              <XMarkIcon class="h-3.5 w-3.5" />
            </button>
          </div>

          <div class="flex justify-between items-center text-sm">
            <div class="flex items-center gap-2">
              <span class="text-gray-500">{{ $t('pos.tax') }}</span>
              <input
                v-model.number="taxRate"
                type="number"
                min="0"
                max="100"
                step="0.1"
                class="w-16 px-2 py-1 text-xs border rounded"
              />
              <span class="text-gray-400 text-xs">%</span>
            </div>
            <span>{{ formatCurrency(tax) }}</span>
          </div>
          <div class="flex justify-between text-lg font-bold pt-2 border-t">
            <span>{{ $t('pos.grandTotal') }}</span>
            <span class="text-primary-600">{{ formatCurrency(total) }}</span>
          </div>
        </div>

        <!-- Payment Buttons -->
        <div class="p-4 border-t space-y-2">
          <button
            @click="openPayment"
            :disabled="cart.items.length === 0 || !currentShift"
            class="btn-primary w-full py-3 text-lg"
          >
            <CreditCardIcon class="h-6 w-6 mr-2" />
            {{ $t('pos.checkout') }} {{ formatCurrency(total) }}
          </button>
          <button
            @click="sellAsDebt"
            :disabled="cart.items.length === 0 || !currentShift"
            class="w-full py-2 text-sm border-2 border-orange-500 text-orange-600 rounded-lg hover:bg-orange-50 transition-colors flex items-center justify-center"
          >
            <DocumentTextIcon class="h-5 w-5 mr-2" />
            {{ $t('pos.sellAsDebt') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Payment Modal -->
    <div v-if="showPaymentModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showPaymentModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-lg w-full p-6">
          <h3 class="text-xl font-bold text-gray-900 mb-4">{{ $t('pos.paymentMethod') }}</h3>

          <!-- Total and Remaining -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div class="text-center p-3 bg-gray-100 rounded-lg">
              <p class="text-sm text-gray-500">{{ $t('total') }}</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(total) }}</p>
            </div>
            <div class="text-center p-3 rounded-lg" :class="remainingAmount > 0 ? 'bg-orange-100' : 'bg-green-100'">
              <p class="text-sm" :class="remainingAmount > 0 ? 'text-orange-600' : 'text-green-600'">{{ $t('pos.remaining') }}</p>
              <p class="text-2xl font-bold" :class="remainingAmount > 0 ? 'text-orange-700' : 'text-green-700'">
                {{ formatCurrency(remainingAmount) }}
              </p>
            </div>
          </div>

          <!-- Added Payments (Split) -->
          <div v-if="payments.length > 0" class="mb-4">
            <p class="text-sm font-medium text-gray-700 mb-2">{{ $t('pos.splitPayments') }}:</p>
            <div class="space-y-2">
              <div
                v-for="(payment, index) in payments"
                :key="index"
                class="flex items-center justify-between p-2 bg-gray-50 rounded-lg"
              >
                <div class="flex items-center">
                  <span class="text-sm font-medium">{{ payment.method }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="font-medium">{{ formatCurrency(payment.amount) }}</span>
                  <button @click="removePaymentSplit(index)" class="text-red-500 hover:text-red-700">
                    <XMarkIcon class="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Payment Method Selection -->
          <div v-if="remainingAmount > 0" class="space-y-4">
            <p class="text-sm font-medium text-gray-700">{{ $t('pos.addPayment') }}:</p>

            <div class="grid grid-cols-4 gap-2">
              <button
                v-for="method in paymentMethods"
                :key="method.value"
                @click="selectPaymentMethod(method.value)"
                :class="[
                  'p-3 border-2 rounded-lg flex flex-col items-center transition-colors',
                  currentPayment.method === method.value ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'
                ]"
              >
                <component :is="method.icon" :class="['h-6 w-6 mb-1', method.color]" />
                <span class="text-xs font-medium">{{ method.label }}</span>
              </button>
            </div>

            <!-- Debt warning -->
            <div v-if="currentPayment.method === 'CREDIT' && !cart.customerId" class="p-3 bg-orange-50 border border-orange-200 rounded-lg">
              <p class="text-sm text-orange-700">{{ $t('pos.customerRequiredForDebt') }}</p>
            </div>

            <!-- Payment Amount -->
            <div>
              <label class="label">{{ $t('amount') }}</label>
              <input
                v-model.number="currentPayment.amount"
                type="number"
                step="0.01"
                min="0"
                :max="remainingAmount"
                class="input text-lg text-center"
              />
            </div>

            <!-- Cash Received -->
            <div v-if="currentPayment.method === 'CASH'">
              <label class="label">{{ $t('pos.received') }}</label>
              <input
                v-model.number="currentPayment.received"
                type="number"
                step="0.01"
                min="0"
                class="input text-lg text-center"
              />
              <div v-if="change > 0" class="mt-2 p-3 bg-green-50 rounded-lg text-center">
                <p class="text-sm text-green-600">{{ $t('pos.change') }}</p>
                <p class="text-2xl font-bold text-green-700">{{ formatCurrency(change) }}</p>
              </div>
            </div>

            <!-- Add Split Payment Button -->
            <button
              @click="addPaymentSplit"
              :disabled="currentPayment.amount <= 0 || (currentPayment.method === 'CREDIT' && !cart.customerId)"
              class="w-full py-2 border-2 border-primary-500 text-primary-600 rounded-lg hover:bg-primary-50 transition-colors"
            >
              <PlusIcon class="h-5 w-5 inline mr-1" />
              {{ $t('pos.addPayment') }}
            </button>
          </div>

          <!-- Action Buttons -->
          <div class="flex space-x-3 mt-6">
            <button @click="showPaymentModal = false" class="btn-secondary flex-1">
              {{ $t('cancel') }}
            </button>
            <button
              @click="processPayment"
              :disabled="totalPaid < total && remainingAmount > 0"
              class="btn-primary flex-1"
            >
              <CheckIcon class="h-5 w-5 mr-2" />
              {{ $t('pos.completeSale') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Customer Modal -->
    <div v-if="showCustomerModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCustomerModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('pos.selectCustomer') }}</h3>

          <div class="relative mb-4">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="customerSearch"
              @input="searchCustomers"
              type="text"
              :placeholder="$t('customers.searchPlaceholder')"
              class="input pl-10"
            />
          </div>

          <div class="max-h-64 overflow-y-auto">
            <button
              v-for="customer in customers"
              :key="customer.id"
              @click="selectCustomer(customer)"
              class="w-full p-3 text-left hover:bg-gray-50 rounded-lg border mb-2"
            >
              <p class="font-medium">{{ customer.name }}</p>
              <p class="text-sm text-gray-500">{{ customer.phone || customer.email }}</p>
            </button>
          </div>

          <button @click="showCustomerModal = false" class="btn-secondary w-full mt-4">
            {{ $t('cancel') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Quick Add Customer Modal -->
    <div v-if="showNewCustomerModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showNewCustomerModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('pos.quickAddCustomer') }}</h3>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('name') }} *</label>
              <input v-model="newCustomer.name" type="text" class="input" :placeholder="$t('name')" />
            </div>
            <div>
              <label class="label">{{ $t('phone') }}</label>
              <input v-model="newCustomer.phone" type="text" class="input" :placeholder="$t('phone')" />
            </div>
            <div>
              <label class="label">{{ $t('customers.form.email') }}</label>
              <input v-model="newCustomer.email" type="email" class="input" :placeholder="$t('customers.form.email')" />
            </div>
          </div>

          <div class="flex space-x-3 mt-6">
            <button @click="showNewCustomerModal = false" class="btn-secondary flex-1">
              {{ $t('cancel') }}
            </button>
            <button @click="createQuickCustomer" class="btn-primary flex-1">
              <PlusIcon class="h-5 w-5 mr-2" />
              {{ $t('pos.createAndSelect') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Open Shift Modal -->
    <div v-if="showOpenShiftModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showOpenShiftModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('pos.shifts.openShift') }}</h3>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('pos.shifts.terminal') }}</label>
              <p class="text-sm text-gray-700 font-medium">
                {{ terminals.find(t => t.id === selectedTerminalId)?.name || terminals.find(t => t.id === selectedTerminalId)?.terminalCode }}
              </p>
            </div>
            <div>
              <label class="label">{{ $t('pos.shifts.openingBalance') }}</label>
              <input
                v-model.number="openingCash"
                type="number"
                min="0"
                step="1000"
                class="input text-lg"
                placeholder="0"
              />
            </div>
          </div>

          <div class="flex space-x-3 mt-6">
            <button @click="showOpenShiftModal = false" class="btn-secondary flex-1">
              {{ $t('cancel') }}
            </button>
            <button @click="openShift" :disabled="shiftLoading" class="btn-primary flex-1">
              <ClockIcon class="h-5 w-5 mr-2" />
              {{ shiftLoading ? $t('pos.processing') : $t('pos.shifts.openShift') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Close Shift Modal -->
    <div v-if="showCloseShiftModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showCloseShiftModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('pos.shifts.closeShift') }}</h3>

          <div class="space-y-4">
            <div class="p-3 bg-gray-50 rounded-lg space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-gray-500">{{ $t('pos.shifts.shiftNumber') }}:</span>
                <span class="font-medium">{{ currentShift?.shiftNumber }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-500">{{ $t('pos.shifts.openingBalance') }}:</span>
                <span class="font-medium">{{ formatCurrency(currentShift?.openingCash || 0) }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-500">{{ $t('pos.shifts.salesCount') }}:</span>
                <span class="font-medium">{{ currentShift?.transactionCount || 0 }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-500">{{ $t('pos.shifts.totalSales') }}:</span>
                <span class="font-medium">{{ formatCurrency(currentShift?.totalSales || 0) }}</span>
              </div>
            </div>
            <div>
              <label class="label">{{ $t('pos.shifts.closingBalance') }}</label>
              <input
                v-model.number="closingCash"
                type="number"
                min="0"
                step="1000"
                class="input text-lg"
                placeholder="0"
              />
            </div>
          </div>

          <div class="flex space-x-3 mt-6">
            <button @click="showCloseShiftModal = false" class="btn-secondary flex-1">
              {{ $t('cancel') }}
            </button>
            <button @click="closeShift" :disabled="shiftLoading" class="btn-primary flex-1 !bg-red-600 hover:!bg-red-700">
              <ArrowRightStartOnRectangleIcon class="h-5 w-5 mr-2" />
              {{ shiftLoading ? $t('pos.processing') : $t('pos.shifts.closeShift') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Delivery Address Modal -->
    <div v-if="showDeliveryModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDeliveryModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            <MapPinIcon class="h-5 w-5 inline mr-2 text-orange-500" />
            {{ $t('pos.deliveryAddress') }}
          </h3>

          <div class="space-y-4">
            <!-- Region -->
            <div>
              <label class="label">{{ $t('customers.form.region') }} *</label>
              <select v-model="selectedRegionId" @change="onRegionChange" class="input">
                <option :value="null">{{ $t('customers.form.selectRegion') }}</option>
                <option v-for="region in deliveryRegions" :key="region.id" :value="region.id">
                  {{ region.name }}
                </option>
              </select>
            </div>

            <!-- Village -->
            <div>
              <label class="label">{{ $t('customers.form.village') }}</label>
              <select v-model="selectedVillageId" class="input" :disabled="!selectedRegionId">
                <option :value="null">{{ $t('customers.form.selectVillage') }}</option>
                <option v-for="village in deliveryVillages" :key="village.id" :value="village.id">
                  {{ village.name }}
                </option>
              </select>
              <p v-if="selectedRegionId && deliveryVillages.length === 0" class="text-xs text-gray-400 mt-1">
                {{ $t('pos.noVillagesFound') }}
              </p>
            </div>
          </div>

          <div class="flex space-x-3 mt-6">
            <button @click="showDeliveryModal = false" class="btn-secondary flex-1">
              {{ $t('cancel') }}
            </button>
            <button
              @click="confirmDeliveryAddress"
              :disabled="!selectedRegionId"
              class="btn-primary flex-1"
            >
              <CheckIcon class="h-5 w-5 mr-2" />
              {{ $t('pos.confirm') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- UOM Selection Modal -->
    <div v-if="showUomModal && pendingProduct" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showUomModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-1">{{ $t('pos.selectUom') }}</h3>
          <p class="text-sm text-gray-500 mb-4">{{ pendingProduct.name }}</p>

          <div class="space-y-2">
            <!-- Base UOM option -->
            <button
              @click="selectBaseUom()"
              class="w-full p-3 text-left rounded-lg border-2 border-gray-200 hover:border-primary-500 hover:bg-primary-50 transition-colors"
            >
              <div class="flex justify-between items-center">
                <div>
                  <p class="font-medium text-gray-900">{{ $t('pos.baseUnit') }}</p>
                  <p class="text-xs text-gray-500">{{ $t('pos.baseUnitDescription') }}</p>
                </div>
                <span class="font-bold text-primary-600">{{ formatCurrency(pendingProduct.sellingPrice) }}</span>
              </div>
            </button>

            <!-- Alternate UOM options -->
            <button
              v-for="puom in productUoms"
              :key="puom.id"
              @click="addToCartWithUom(pendingProduct, puom)"
              class="w-full p-3 text-left rounded-lg border-2 transition-colors"
              :class="puom.defaultSale ? 'border-primary-300 bg-primary-50 hover:border-primary-500' : 'border-gray-200 hover:border-primary-500 hover:bg-primary-50'"
            >
              <div class="flex justify-between items-center">
                <div>
                  <p class="font-medium text-gray-900">
                    {{ puom.uomName }}
                    <span v-if="puom.defaultSale" class="text-xs text-primary-600">({{ $t('pos.standard') }})</span>
                  </p>
                  <p class="text-xs text-gray-500">1 {{ puom.uomCode }} = {{ puom.conversionFactor }} {{ $t('pos.baseUnitSuffix') }}</p>
                </div>
                <span class="font-bold text-primary-600">{{ formatCurrency(puom.effectiveSellingPrice) }}</span>
              </div>
            </button>
          </div>

          <button @click="showUomModal = false" class="btn-secondary w-full mt-4">
            {{ $t('cancel') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Discount Modal -->
    <div v-if="showDiscountModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showDiscountModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ discountTarget === 'transaction' ? $t('pos.discountTransaction') : $t('pos.discountItem') }}
          </h3>

          <div class="space-y-4">
            <!-- Discount type toggle -->
            <div class="flex rounded-lg border overflow-hidden">
              <button
                @click="discountForm.type = 'percent'"
                :class="['flex-1 py-2 text-sm font-medium transition-colors', discountForm.type === 'percent' ? 'bg-primary-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']"
              >
                {{ $t('pos.percent') }} (%)
              </button>
              <button
                @click="discountForm.type = 'amount'"
                :class="['flex-1 py-2 text-sm font-medium transition-colors', discountForm.type === 'amount' ? 'bg-primary-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']"
              >
                {{ $t('amount') }}
              </button>
            </div>

            <!-- Discount value -->
            <div>
              <label class="label">
                {{ discountForm.type === 'percent' ? $t('pos.percent') + ' (%)' : $t('amount') }}
              </label>
              <input
                v-model.number="discountForm.value"
                type="number"
                step="0.01"
                min="0"
                :max="discountForm.type === 'percent' ? 100 : undefined"
                class="input text-lg text-center"
                autofocus
              />
            </div>

            <!-- Quick percent buttons -->
            <div v-if="discountForm.type === 'percent'" class="flex gap-2">
              <button
                v-for="pct in [5, 10, 15, 20, 25]"
                :key="pct"
                @click="discountForm.value = pct"
                :class="['flex-1 py-2 text-sm border rounded-lg transition-colors', discountForm.value === pct ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 hover:bg-gray-50']"
              >
                {{ pct }}%
              </button>
            </div>

            <!-- Reason -->
            <div>
              <label class="label">{{ $t('pos.discountReason') }}</label>
              <input
                v-model="discountForm.reason"
                type="text"
                class="input"
                :placeholder="$t('pos.discountReason')"
              />
            </div>
          </div>

          <div class="flex space-x-3 mt-6">
            <button @click="showDiscountModal = false" class="btn-secondary flex-1">
              {{ $t('cancel') }}
            </button>
            <button
              @click="applyDiscountFromModal"
              :disabled="discountForm.value <= 0"
              class="btn-primary flex-1"
            >
              {{ $t('pos.apply') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
