<script setup>
import PosCustomerModal from '@/components/pos/PosCustomerModal.vue'
import PosQuickAddCustomerModal from '@/components/pos/PosQuickAddCustomerModal.vue'
import PosUomModal from '@/components/pos/PosUomModal.vue'
import PosDiscountModal from '@/components/pos/PosDiscountModal.vue'
import PosOpenShiftModal from '@/components/pos/PosOpenShiftModal.vue'
import PosDeliveryModal from '@/components/pos/PosDeliveryModal.vue'
import PosCloseShiftModal from '@/components/pos/PosCloseShiftModal.vue'
import { useToastStore } from '@/stores/toast'
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { customersApi, deliveryRegionsApi, deliveryVillagesApi, posApi, pricingApi, productsApi, shiftsApi, terminalsApi, unwrapData, unwrapList } from '@/services/api'
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
  MapPinIcon,
  PencilSquareIcon
} from '@heroicons/vue/24/outline'

const toast = useToastStore()

const { t } = useI18n()

// State
const products = ref([])
const quickProducts = ref([])
const searchQuery = ref('')
const loading = ref(false)

// Customer list state (tap-based)
const allCustomers = ref([])
const customersLoading = ref(false)

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
const unresolvedTransactions = ref([])
const unresolvedLoading = ref(false)
const voidingTransactionId = ref(null)
const shiftVoidReason = ref('')

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

// Price editing state
const editingPriceKey = ref(null)
const editingPriceValue = ref('')

// Quantity editing state
const editingQtyKey = ref(null)
const editingQtyValue = ref('')

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
const discountInitial = ref({
  type: 'percent', // 'percent' or 'amount'
  value: 0,
  reason: ''
})
const transactionDiscount = reactive({
  type: null, // 'percent' or 'amount'
  value: 0,
  reason: ''
})

// Coupon state
const couponCode = ref('')
const couponValid = ref(null)
const couponDiscount = ref(null)
const couponError = ref('')
const couponValidating = ref(false)

const showNewCustomerModal = ref(false)

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

// Whether the sale can be completed (existing splits + current form cover the total)
const canCompleteSale = computed(() => {
  const currentAmount = currentPayment.amount > 0 ? Math.min(currentPayment.amount, remainingAmount.value) : 0
  return (totalPaid.value + currentAmount) >= total.value
})

// Check if debt sale requires customer

// Methods
async function fetchQuickProducts() {
  try {
    const response = await productsApi.getActive({ size: 50 })
    quickProducts.value = unwrapList(response)
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
    products.value = unwrapList(response)
  } catch (error) {
    console.error('Search failed:', error)
  } finally {
    loading.value = false
  }
}

async function loadAllCustomers() {
  if (allCustomers.value.length > 0) return // already loaded
  customersLoading.value = true
  try {
    const response = await customersApi.getAll({ size: 1000, sort: 'name,asc' })
    const data = unwrapList(response)
    allCustomers.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Failed to load customers:', error)
  } finally {
    customersLoading.value = false
  }
}

// UOM selection state
const showUomModal = ref(false)
const pendingProduct = ref(null)
const productUoms = ref([])

// Cache of product UOMs to avoid re-fetching
const productUomCache = {}

async function addToCart(product) {
  searchQuery.value = ''
  products.value = []

  // Check if product has alternate UOMs (fetch once, then cache)
  if (!productUomCache[product.id]) {
    try {
      const res = await productsApi.getActiveUoms(product.id)
      productUomCache[product.id] = unwrapList(res)
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

async function addToCartWithUom(product, selectedUom) {
  showUomModal.value = false

  const uomKey = selectedUom ? `${product.id}_uom_${selectedUom.id}` : `${product.id}`

  const existingItem = cart.items.find(item => item._uomKey === uomKey)

  if (existingItem) {
    existingItem.quantity++
  } else {
    let price = selectedUom ? selectedUom.effectiveSellingPrice : product.sellingPrice

    if (cart.customerId) {
      const customerPrice = await fetchProductPrice(product.id)
      if (customerPrice?.price != null) {
        price = customerPrice.price
      }
    }

    cart.items.push({
      _uomKey: uomKey,
      productId: product.id,
      name: product.name,
      sku: product.sku,
      price,
      quantity: 1,
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
  const newQty = Math.round((item.quantity + delta) * 10) / 10
  if (newQty <= 0) {
    removeFromCart(item)
  } else {
    item.quantity = newQty
  }
}

function startEditQty(item) {
  editingQtyKey.value = item._uomKey
  editingQtyValue.value = String(item.quantity)
}

function saveEditQty(item) {
  const newQty = parseFloat(editingQtyValue.value)
  if (!isNaN(newQty) && newQty > 0) {
    item.quantity = Math.round(newQty * 10) / 10
  }
  editingQtyKey.value = null
}

function removeFromCart(item) {
  const index = cart.items.indexOf(item)
  if (index > -1) {
    cart.items.splice(index, 1)
  }
}

function startEditPrice(item) {
  editingPriceKey.value = item._uomKey
  editingPriceValue.value = String(item.price)
}

function saveEditPrice(item) {
  const newPrice = parseFloat(editingPriceValue.value)
  if (!isNaN(newPrice) && newPrice >= 0) {
    item.price = newPrice
  }
  editingPriceKey.value = null
}

function cancelEditPrice() {
  editingPriceKey.value = null
}

function selectCustomer(customer) {
  cart.customerId = customer.id
  cart.customerName = customer.name
  showCustomerModal.value = false
}

function openCustomerModal() {
  showCustomerModal.value = true
  loadAllCustomers()
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
      deliveryRegions.value = unwrapList(response)
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
    deliveryVillages.value = unwrapList(response)
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
  discountInitial.value = {
    type: transactionDiscount.type || 'percent',
    value: transactionDiscount.value || 0,
    reason: transactionDiscount.reason || ''
  }
  showDiscountModal.value = true
}

function openItemDiscount(index) {
  const item = cart.items[index]
  discountTarget.value = index
  discountInitial.value = item.discountPercent
    ? { type: 'percent', value: item.discountPercent, reason: item.discountReason || '' }
    : { type: 'amount', value: item.discount || 0, reason: item.discountReason || '' }
  showDiscountModal.value = true
}

function applyDiscountFromModal(form) {
  if (form.value < 0) return

  if (discountTarget.value === 'transaction') {
    transactionDiscount.type = form.type
    transactionDiscount.value = form.value
    transactionDiscount.reason = form.reason
  } else {
    const item = cart.items[discountTarget.value]
    const itemTotal = item.price * item.quantity
    if (form.type === 'percent') {
      item.discountPercent = form.value
      item.discount = itemTotal * (form.value / 100)
    } else {
      item.discountPercent = null
      item.discount = Math.min(form.value, itemTotal)
    }
    item.discountReason = form.reason
  }
  showDiscountModal.value = false
}

function clearTransactionDiscount() {
  transactionDiscount.type = null
  transactionDiscount.value = 0
  transactionDiscount.reason = ''
}

// Coupon functions
async function validateCoupon() {
  if (!couponCode.value.trim()) return
  couponValidating.value = true
  couponError.value = ''
  couponValid.value = null
  couponDiscount.value = null
  try {
    const res = await pricingApi.validateCoupon(couponCode.value.trim(), cart.customerId)
    const data = unwrapData(res)
    couponValid.value = data.valid !== false
    if (!couponValid.value) {
      couponError.value = data.message || t('pos.couponInvalid')
    }
  } catch (error) {
    couponValid.value = false
    couponError.value = error.response?.data?.message || t('pos.couponInvalid')
  } finally {
    couponValidating.value = false
  }
}

async function applyCoupon() {
  if (!couponValid.value || !couponCode.value.trim()) return
  try {
    const res = await pricingApi.applyCoupon({
      couponCode: couponCode.value.trim(),
      customerId: cart.customerId,
      orderTotal: total.value,
      items: cart.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.price
      }))
    })
    const data = unwrapData(res)
    couponDiscount.value = data
    if (data.discountAmount > 0) {
      transactionDiscount.type = 'amount'
      transactionDiscount.value = data.discountAmount
      transactionDiscount.reason = t('pos.couponApplied') + ': ' + couponCode.value
    }
  } catch (error) {
    couponError.value = error.response?.data?.message || t('pos.couponApplyFailed')
  }
}

function clearCoupon() {
  couponCode.value = ''
  couponValid.value = null
  couponDiscount.value = null
  couponError.value = ''
  if (transactionDiscount.reason?.startsWith(t('pos.couponApplied'))) {
    clearTransactionDiscount()
  }
}

// Fetch customer-specific price for a product
async function fetchProductPrice(productId) {
  if (!cart.customerId) return null
  try {
    const res = await pricingApi.getProductPrice(productId, { customerId: cart.customerId })
    return unwrapData(res)
  } catch {
    return null
  }
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
    toast.error(t('pos.customerRequiredForDebt'))
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
    toast.error(t('pos.customerRequiredForDebt'))
    return
  }

  // Add current payment if there's remaining amount
  if (remainingAmount.value > 0 && currentPayment.amount > 0) {
    addPaymentSplit()
  }

  // Verify total payments
  const finalTotal = payments.value.reduce((sum, p) => sum + p.amount, 0)
  if (finalTotal < total.value) {
    toast.error(t('pos.paymentLessThanTotal'))
    return
  }

  // Validate terminal is selected
  if (!selectedTerminalId.value) {
    toast.error(t('pos.noTerminalAvailable'))
    return
  }

  let transactionId = null
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
    const txData = unwrapData(txResponse)
    transactionId = txData.id

    // Apply transaction-level discount if set
    let backendTotal = Number(txData.totalAmount) || 0
    if (transactionDiscount.type && transactionDiscount.value > 0) {
      const discountData = { reason: transactionDiscount.reason || undefined }
      if (transactionDiscount.type === 'percent') {
        discountData.percent = transactionDiscount.value
      } else {
        discountData.amount = transactionDiscountAmount.value
      }
      const discountResponse = await posApi.applyDiscount(transactionId, discountData)
      const discountTxData = unwrapData(discountResponse)
      backendTotal = Number(discountTxData.totalAmount) || backendTotal
    }

    // Use the backend's authoritative totalAmount for payments.
    // The frontend total may differ from the backend (e.g. per-product tax)
    // so adjust the last payment to cover the exact backend balance.
    const frontendTotal = payments.value.reduce((sum, p) => sum + p.amount, 0)
    if (Math.abs(frontendTotal - backendTotal) > 0.001) {
      // Scale all payments proportionally to match backend total
      const ratio = backendTotal / frontendTotal
      for (const p of payments.value) {
        p.amount = Math.round(p.amount * ratio * 100) / 100
      }
      // Fix any rounding remainder on the last payment
      const adjusted = payments.value.reduce((sum, p) => sum + p.amount, 0)
      const diff = backendTotal - adjusted
      if (payments.value.length > 0) {
        payments.value[payments.value.length - 1].amount += diff
      }
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

    // Record coupon redemption if a coupon was applied
    if (couponDiscount.value && couponCode.value) {
      try {
        await pricingApi.recordCouponRedemption(
          couponCode.value,
          cart.customerId,
          transactionId,
          couponDiscount.value.discountAmount || transactionDiscountAmount.value
        )
      } catch (e) {
        console.error('Failed to record coupon redemption:', e)
      }
    }

    // Check before clearing
    const hasDebtPayment = payments.value.some(p => p.method === 'CREDIT')

    // Reset cart
    cart.items = []
    cart.customerId = null
    cart.customerName = ''
    clearDeliveryAddress()
    payments.value = []
    clearTransactionDiscount()
    clearCoupon()
    showPaymentModal.value = false

    toast.error(hasDebtPayment ? t('pos.saleCompletedWithDebt') : t('pos.saleComplete'))
  } catch (error) {
    console.error('Payment failed:', error)

    // Auto-void the orphaned transaction so it doesn't block shift closure
    if (transactionId) {
      try {
        await posApi.voidTransaction(transactionId, 'Auto-voided: payment processing failed')
      } catch (voidError) {
        console.error('Failed to auto-void transaction:', voidError)
      }
    }

    toast.error(t('pos.paymentFailed') + ': ' + (error.response?.data?.message || error.message))
  }
}

// Quick debt sale - sell entire amount as debt
async function sellAsDebt() {
  if (!cart.customerId) {
    toast.error(t('pos.customerRequiredForDebt'))
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

async function createQuickCustomer(form) {
  if (!form.name?.trim()) {
    toast.error(t('customers.form.nameRequired'))
    return
  }

  try {
    const response = await customersApi.create({
      name: form.name,
      phone: form.phone || null,
      email: form.email || null
    })
    const customer = unwrapData(response)
    cart.customerId = customer.id
    cart.customerName = customer.name
    showNewCustomerModal.value = false
  } catch (error) {
    console.error('Failed to create customer:', error)
    toast.error(t('pos.failedToCreateCustomer') + ': ' + (error.response?.data?.message || error.message))
  }
}

// Handle barcode scanner input
let barcodeBuffer = ''
let barcodeTimeout = null

async function handleKeydown(event) {
  // Ignore if focused on input
  if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') return

  // Ignore modifier keys and special keys — only accept single printable characters
  if (event.ctrlKey || event.altKey || event.metaKey) return
  if (event.key.length !== 1) return

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
    terminals.value = unwrapList(response)
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
    currentShift.value = unwrapData(response)
    showOpenShiftModal.value = false
    openingCash.value = 0
  } catch (error) {
    toast.error(t('pos.shifts.openError') + ': ' + (error.response?.data?.message || error.message))
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
    unresolvedTransactions.value = []
  } catch (error) {
    const msg = error.response?.data?.message || error.message
    if (msg && msg.includes('unresolved')) {
      await fetchUnresolvedTransactions()
    } else {
      toast.error(t('pos.shifts.closeError') + ': ' + msg)
    }
  } finally {
    shiftLoading.value = false
  }
}

async function fetchUnresolvedTransactions() {
  if (!currentShift.value) return
  unresolvedLoading.value = true
  try {
    const response = await posApi.getUnresolved(currentShift.value.id)
    const data = unwrapData(response)
    unresolvedTransactions.value = Array.isArray(data) ? data : (data.content || [])
  } catch (e) {
    console.error('Failed to fetch unresolved transactions:', e)
  } finally {
    unresolvedLoading.value = false
  }
}

async function voidShiftTransaction(tx) {
  const reason = shiftVoidReason.value.trim()
  if (!reason) return
  voidingTransactionId.value = tx.id
  try {
    await posApi.voidTransaction(tx.id, reason)
    unresolvedTransactions.value = unresolvedTransactions.value.filter(t => t.id !== tx.id)
    shiftVoidReason.value = ''
  } catch (error) {
    toast.error(t('pos.transactions.voidError') + ': ' + (error.response?.data?.message || error.message))
  } finally {
    voidingTransactionId.value = null
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
    <!-- Left Panel - Product List -->
    <div class="flex-1 flex flex-col">
      <!-- Search -->
      <div class="card mb-3">
        <div class="card-body py-3">
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

      <!-- Search Results (list rows) -->
      <div v-if="searchQuery && products.length > 0" class="card flex-1 overflow-hidden">
        <div class="overflow-y-auto h-full divide-y divide-gray-100">
          <button
            v-for="product in products"
            :key="product.id"
            @click="addToCart(product)"
            class="w-full flex items-center gap-3 px-4 py-3 hover:bg-primary-50 active:bg-primary-100 transition-colors text-left"
          >
            <img
              v-if="product.primaryImageUrl"
              :src="product.primaryImageUrl"
              :alt="product.name"
              class="h-10 w-10 rounded object-cover flex-shrink-0"
            />
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-900 truncate">{{ product.name }}</div>
              <div class="text-xs text-gray-400">{{ product.sku }}</div>
            </div>
            <div
              :class="[
                'text-xs font-medium px-2 py-0.5 rounded-full',
                (product.stockQuantity || 0) > 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'
              ]"
            >
              {{ product.stockQuantity || 0 }}
            </div>
            <PlusIcon class="h-5 w-5 text-primary-500 flex-shrink-0" />
          </button>
        </div>
      </div>

      <!-- Search - No Results -->
      <div v-else-if="searchQuery && products.length === 0 && !loading" class="card flex-1 flex items-center justify-center">
        <div class="text-center text-gray-500">
          <MagnifyingGlassIcon class="h-12 w-12 mx-auto mb-4 text-gray-300" />
          <p>{{ $t('pos.noProductsFound') }}</p>
        </div>
      </div>

      <!-- Quick Product List (list rows) -->
      <div v-else class="card flex-1 overflow-hidden">
        <div v-if="quickProducts.length > 0" class="overflow-y-auto h-full divide-y divide-gray-100">
          <button
            v-for="product in quickProducts"
            :key="product.id"
            @click="addToCart(product)"
            class="w-full flex items-center gap-3 px-4 py-3 hover:bg-primary-50 active:bg-primary-100 transition-colors text-left"
          >
            <img
              v-if="product.primaryImageUrl"
              :src="product.primaryImageUrl"
              :alt="product.name"
              class="h-10 w-10 rounded object-cover flex-shrink-0"
            />
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-900 truncate">{{ product.name }}</div>
              <div class="text-xs text-gray-400">{{ product.sku }}</div>
            </div>
            <div
              :class="[
                'text-xs font-medium px-2 py-0.5 rounded-full',
                (product.stockQuantity || 0) > 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'
              ]"
            >
              {{ product.stockQuantity || 0 }}
            </div>
            <PlusIcon class="h-5 w-5 text-primary-500 flex-shrink-0" />
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
              @click="openCustomerModal"
              class="flex items-center text-primary-600 hover:text-primary-700 text-sm py-1 px-2 rounded-lg hover:bg-primary-50 active:bg-primary-100"
            >
              <UserIcon class="h-4 w-4 mr-1" />
              {{ $t('pos.selectCustomer') }}
            </button>
            <span class="text-gray-300">|</span>
            <button
              @click="showNewCustomerModal = true"
              class="flex items-center text-green-600 hover:text-green-700 text-sm py-1 px-2 rounded-lg hover:bg-green-50 active:bg-green-100"
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
            <li v-for="(item, index) in cart.items" :key="item._uomKey" class="px-3 py-2.5">
              <div class="flex justify-between items-start">
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-gray-900 truncate text-sm">{{ item.name }}</p>
                  <div class="flex items-center gap-1.5">
                    <span v-if="editingPriceKey !== item._uomKey" class="text-xs text-gray-400">
                      <span
                        class="cursor-pointer hover:text-primary-600 hover:underline"
                        @click="startEditPrice(item)"
                        :title="$t('pos.clickToEditPrice', 'Narxni o\'zgartirish')"
                      >{{ formatCurrency(item.price) }}</span> x {{ item.quantity }}
                    </span>
                    <span v-else class="flex items-center gap-1">
                      <input
                        type="number"
                        v-model="editingPriceValue"
                        @keyup.enter="saveEditPrice(item)"
                        @keyup.escape="cancelEditPrice()"
                        @blur="saveEditPrice(item)"
                        class="w-24 text-xs border border-primary-300 rounded px-1.5 py-0.5 focus:outline-none focus:ring-1 focus:ring-primary-500"
                        min="0"
                        step="any"
                        ref="priceInput"
                        autofocus
                      />
                      <span class="text-xs text-gray-400">x {{ item.quantity }}</span>
                    </span>
                    <span v-if="item.uomName" class="inline-flex items-center gap-0.5 text-xs px-1 py-0.5 rounded bg-blue-50 text-blue-700">
                      <ScaleIcon class="h-3 w-3" />
                      {{ item.uomName }}
                    </span>
                  </div>
                </div>
                <div class="text-right flex-shrink-0 ml-2">
                  <span class="font-bold text-sm text-gray-900">{{ formatCurrency(item.price * item.quantity) }}</span>
                </div>
              </div>

              <div class="flex items-center justify-between mt-1.5">
                <div class="flex items-center gap-1">
                  <button
                    @click="updateQuantity(item, -1)"
                    class="h-8 w-8 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-gray-100 active:bg-gray-200"
                  >
                    <MinusIcon class="h-4 w-4" />
                  </button>
                  <button
                    @click="updateQuantity(item, -0.5)"
                    class="h-8 px-1.5 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-gray-100 active:bg-gray-200 text-xs font-medium text-gray-600"
                  >
                    -½
                  </button>
                  <input
                    v-if="editingQtyKey === item._uomKey"
                    v-model="editingQtyValue"
                    type="number"
                    step="0.1"
                    min="0.1"
                    class="w-14 h-8 text-center font-bold text-sm border border-primary-400 rounded-lg outline-none focus:ring-1 focus:ring-primary-500"
                    @blur="saveEditQty(item)"
                    @keyup.enter="saveEditQty(item)"
                    @keyup.escape="editingQtyKey = null"
                    @focus="$event.target.select()"
                    ref="qtyInput"
                    autofocus
                  />
                  <span
                    v-else
                    @click="startEditQty(item)"
                    class="w-10 text-center font-bold text-sm cursor-pointer hover:text-primary-600 hover:bg-primary-50 rounded-lg py-1 transition-colors"
                  >{{ item.quantity }}</span>
                  <button
                    @click="updateQuantity(item, 0.5)"
                    class="h-8 px-1.5 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-gray-100 active:bg-gray-200 text-xs font-medium text-gray-600"
                  >
                    +½
                  </button>
                  <button
                    @click="updateQuantity(item, 1)"
                    class="h-8 w-8 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-gray-100 active:bg-gray-200"
                  >
                    <PlusIcon class="h-4 w-4" />
                  </button>
                </div>
                <div class="flex items-center gap-1">
                  <button
                    @click="openItemDiscount(index)"
                    class="h-8 text-xs px-2 rounded-lg border hover:bg-gray-100 active:bg-gray-200"
                    :class="item.discount ? 'text-green-600 border-green-300 bg-green-50' : 'text-gray-400 border-gray-200'"
                  >
                    {{ item.discount ? ('-' + formatCurrency(item.discount)) : '%' }}
                  </button>
                  <button
                    @click="removeFromCart(item)"
                    class="h-8 w-8 flex items-center justify-center rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 active:bg-red-100"
                  >
                    <TrashIcon class="h-4 w-4" />
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
        <div class="p-3 border-t space-y-2">
          <button
            @click="openPayment"
            :disabled="cart.items.length === 0 || !currentShift"
            class="btn-primary w-full py-4 text-lg rounded-xl active:scale-[0.98] transition-transform"
          >
            <CreditCardIcon class="h-6 w-6 mr-2" />
            {{ $t('pos.checkout') }} {{ formatCurrency(total) }}
          </button>
          <button
            @click="sellAsDebt"
            :disabled="cart.items.length === 0 || !currentShift"
            class="w-full py-3 text-sm border-2 border-orange-500 text-orange-600 rounded-xl hover:bg-orange-50 active:bg-orange-100 transition-colors flex items-center justify-center"
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

          <!-- Coupon Section -->
          <div class="mb-4 p-3 bg-gray-50 rounded-lg">
            <p class="text-sm font-medium text-gray-700 mb-2">{{ $t('pos.couponCode') }}</p>
            <div class="flex gap-2">
              <input
                v-model="couponCode"
                type="text"
                :placeholder="$t('pos.couponPlaceholder')"
                class="input flex-1 text-sm"
                :disabled="couponDiscount !== null"
              />
              <button
                v-if="!couponDiscount"
                @click="validateCoupon"
                :disabled="!couponCode.trim() || couponValidating"
                class="btn-secondary text-sm whitespace-nowrap"
              >
                {{ couponValidating ? '...' : $t('pos.validateCoupon') }}
              </button>
              <button
                v-if="couponValid && !couponDiscount"
                @click="applyCoupon"
                class="btn-primary text-sm whitespace-nowrap"
              >
                {{ $t('pos.applyCouponBtn') }}
              </button>
              <button
                v-if="couponDiscount"
                @click="clearCoupon"
                class="text-red-500 hover:text-red-700"
              >
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>
            <p v-if="couponError" class="text-xs text-red-500 mt-1">{{ couponError }}</p>
            <p v-if="couponValid === true && !couponDiscount" class="text-xs text-green-600 mt-1">{{ $t('pos.couponValidMsg') }}</p>
            <p v-if="couponDiscount" class="text-xs text-green-600 mt-1">
              {{ $t('pos.couponApplied') }}: -{{ formatCurrency(couponDiscount.discountAmount || transactionDiscountAmount) }} {{ $t('sum') }}
            </p>
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

          <!-- Payment Method Selection (always visible while there's remaining) -->
          <div v-if="remainingAmount > 0" class="space-y-4">
            <p class="text-sm font-medium text-gray-700">{{ payments.length > 0 ? $t('pos.addNextPayment') : $t('pos.selectPaymentMethod') }}:</p>

            <div class="grid grid-cols-2 gap-3">
              <button
                v-for="method in paymentMethods"
                :key="method.value"
                @click="selectPaymentMethod(method.value)"
                :class="[
                  'p-4 border-2 rounded-xl flex items-center gap-3 transition-colors active:scale-95',
                  currentPayment.method === method.value ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'
                ]"
              >
                <component :is="method.icon" :class="['h-7 w-7', method.color]" />
                <span class="text-sm font-medium">{{ method.label }}</span>
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
              v-if="currentPayment.amount > 0"
              @click="addPaymentSplit"
              :disabled="currentPayment.method === 'CREDIT' && !cart.customerId"
              class="w-full py-2 border-2 border-primary-500 text-primary-600 rounded-lg hover:bg-primary-50 transition-colors"
            >
              <PlusIcon class="h-5 w-5 inline mr-1" />
              {{ $t('pos.addSplit') }}
            </button>
          </div>

          <!-- Action Buttons -->
          <div class="flex space-x-3 mt-6">
            <button @click="showPaymentModal = false" class="btn-secondary flex-1">
              {{ $t('cancel') }}
            </button>
            <button
              @click="processPayment"
              :disabled="!canCompleteSale || (currentPayment.method === 'CREDIT' && !cart.customerId)"
              class="btn-primary flex-1"
            >
              <CheckIcon class="h-5 w-5 mr-2" />
              {{ $t('pos.completeSale') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Customer selection / quick add -->
    <PosCustomerModal
      :show="showCustomerModal"
      :customers="allCustomers"
      :loading="customersLoading"
      @close="showCustomerModal = false"
      @select="selectCustomer"
      @new-customer="showCustomerModal = false; showNewCustomerModal = true"
    />
    <PosQuickAddCustomerModal
      :show="showNewCustomerModal"
      @close="showNewCustomerModal = false"
      @create="createQuickCustomer"
    />

    <!-- Open Shift -->
    <PosOpenShiftModal
      :show="showOpenShiftModal"
      :terminal-name="terminals.find(t => t.id === selectedTerminalId)?.name || terminals.find(t => t.id === selectedTerminalId)?.terminalCode || ''"
      :loading="shiftLoading"
      v-model:opening-cash="openingCash"
      @close="showOpenShiftModal = false"
      @open="openShift"
    />

    <!-- Close Shift -->
    <PosCloseShiftModal
      :show="showCloseShiftModal"
      :shift="currentShift"
      :unresolved="unresolvedTransactions"
      :unresolved-loading="unresolvedLoading"
      :loading="shiftLoading"
      v-model:closing-cash="closingCash"
      v-model:voiding-id="voidingTransactionId"
      v-model:void-reason="shiftVoidReason"
      @close="showCloseShiftModal = false; unresolvedTransactions.splice(0)"
      @close-shift="closeShift"
      @void="voidShiftTransaction"
    />

    <!-- Delivery Address -->
    <PosDeliveryModal
      :show="showDeliveryModal"
      :regions="deliveryRegions"
      :villages="deliveryVillages"
      v-model:region-id="selectedRegionId"
      v-model:village-id="selectedVillageId"
      @close="showDeliveryModal = false"
      @confirm="confirmDeliveryAddress"
      @region-change="onRegionChange"
    />

    <!-- UOM Selection -->
    <PosUomModal
      :show="showUomModal"
      :product="pendingProduct"
      :uoms="productUoms"
      @close="showUomModal = false"
      @select-base="selectBaseUom"
      @select-uom="puom => addToCartWithUom(pendingProduct, puom)"
    />

    <!-- Discount -->
    <PosDiscountModal
      :show="showDiscountModal"
      :for-transaction="discountTarget === 'transaction'"
      :initial="discountInitial"
      @close="showDiscountModal = false"
      @apply="applyDiscountFromModal"
    />
  </div>
</template>
