<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { productsApi, customersApi, posApi } from '@/services/api'
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
  DocumentTextIcon
} from '@heroicons/vue/24/outline'

// State
const products = ref([])
const customers = ref([])
const searchQuery = ref('')
const loading = ref(false)

const cart = reactive({
  items: [],
  customerId: null,
  customerName: ''
})

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
const showNewCustomerModal = ref(false)
const newCustomer = reactive({
  name: '',
  phone: '',
  email: ''
})

// Payment methods available
const paymentMethods = [
  { value: 'CASH', label: 'Cash', icon: BanknotesIcon, color: 'text-green-600' },
  { value: 'CARD', label: 'Card', icon: CreditCardIcon, color: 'text-blue-600' },
  { value: 'CREDIT', label: 'Debt', icon: DocumentTextIcon, color: 'text-orange-600' },
  { value: 'MOBILE_PAYMENT', label: 'Mobile', icon: CreditCardIcon, color: 'text-purple-600' }
]

// Computed
const subtotal = computed(() => {
  return cart.items.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

const tax = computed(() => subtotal.value * (taxRate.value / 100))
const total = computed(() => subtotal.value + tax.value)

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

function addToCart(product) {
  const existingItem = cart.items.find(item => item.productId === product.id)

  if (existingItem) {
    existingItem.quantity++
  } else {
    cart.items.push({
      productId: product.id,
      name: product.name,
      sku: product.sku,
      price: product.sellingPrice,
      quantity: 1
    })
  }

  searchQuery.value = ''
  products.value = []
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
    alert('Customer is required for debt sales')
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
    alert('Customer is required for debt sales. Please select a customer first.')
    return
  }

  // Add current payment if there's remaining amount
  if (remainingAmount.value > 0 && currentPayment.amount > 0) {
    addPaymentSplit()
  }

  // Verify total payments
  const finalTotal = payments.value.reduce((sum, p) => sum + p.amount, 0)
  if (finalTotal < total.value) {
    alert('Payment amount is less than total. Please add more payment or sell as debt.')
    return
  }

  try {
    // Create transaction
    const transactionData = {
      type: 'SALE',
      customerId: cart.customerId,
      items: cart.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.price
      }))
    }

    const txResponse = await posApi.createTransaction(transactionData)
    const transactionId = txResponse.data.data?.id || txResponse.data.id

    // Add all payments
    for (const payment of payments.value) {
      await posApi.addPayment(transactionId, {
        paymentType: payment.method,
        amount: payment.amount
      })
    }

    // Complete transaction
    await posApi.completeTransaction(transactionId)

    // Reset cart
    cart.items = []
    cart.customerId = null
    cart.customerName = ''
    payments.value = []
    showPaymentModal.value = false

    const hasDebtPayment = payments.value.some(p => p.method === 'CREDIT')
    alert(hasDebtPayment ? 'Sale completed with debt!' : 'Sale completed successfully!')
  } catch (error) {
    console.error('Payment failed:', error)
    alert('Payment failed: ' + (error.response?.data?.message || error.message))
  }
}

// Quick debt sale - sell entire amount as debt
function sellAsDebt() {
  if (!cart.customerId) {
    alert('Customer is required for debt sales. Please select a customer first.')
    showCustomerModal.value = true
    return
  }

  payments.value = [{
    method: 'CREDIT',
    amount: total.value,
    received: 0
  }]

  currentPayment.amount = 0
  processPayment()
}

function clearCart() {
  if (confirm('Clear all items from cart?')) {
    cart.items = []
    cart.customerId = null
    cart.customerName = ''
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
    alert('Customer name is required')
    return
  }

  try {
    const response = await customersApi.create({
      name: newCustomer.name,
      phone: newCustomer.phone || null,
      email: newCustomer.email || null,
      code: 'CUST-' + Date.now()
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
    alert('Failed to create customer: ' + (error.response?.data?.message || error.message))
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

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
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
              placeholder="Search products by name, SKU, or scan barcode..."
              class="input pl-10 text-lg"
              autofocus
            />
          </div>
        </div>
      </div>

      <!-- Search Results -->
      <div v-if="products.length > 0" class="card flex-1 overflow-hidden">
        <div class="card-body overflow-y-auto h-full">
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <button
              v-for="product in products"
              :key="product.id"
              @click="addToCart(product)"
              class="p-4 border rounded-lg hover:border-primary-500 hover:bg-primary-50 transition-colors text-left"
            >
              <div class="font-medium text-gray-900 truncate">{{ product.name }}</div>
              <div class="text-sm text-gray-500 mt-1">{{ product.sku }}</div>
              <div class="text-lg font-bold text-primary-600 mt-2">
                {{ formatCurrency(product.sellingPrice) }}
              </div>
              <div
                :class="[
                  'text-xs mt-1',
                  (product.stockQuantity || 0) > 0 ? 'text-green-600' : 'text-red-600'
                ]"
              >
                Stock: {{ product.stockQuantity || 0 }}
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="card flex-1 flex items-center justify-center">
        <div class="text-center text-gray-500">
          <MagnifyingGlassIcon class="h-12 w-12 mx-auto mb-4 text-gray-300" />
          <p>Search for products or scan a barcode to add items</p>
        </div>
      </div>
    </div>

    <!-- Right Panel - Cart -->
    <div class="w-96 flex flex-col">
      <div class="card flex-1 flex flex-col overflow-hidden">
        <!-- Cart Header -->
        <div class="card-header flex items-center justify-between">
          <h2 class="text-lg font-medium">Current Sale</h2>
          <button
            v-if="cart.items.length > 0"
            @click="clearCart"
            class="text-sm text-red-600 hover:text-red-700"
          >
            Clear All
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
              Find Customer
            </button>
            <span class="text-gray-300">|</span>
            <button
              @click="showNewCustomerModal = true"
              class="flex items-center text-green-600 hover:text-green-700 text-sm"
            >
              <PlusIcon class="h-4 w-4 mr-1" />
              Quick Add
            </button>
          </div>
        </div>

        <!-- Cart Items -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="cart.items.length === 0" class="flex items-center justify-center h-full text-gray-500">
            <p>Cart is empty</p>
          </div>

          <ul v-else class="divide-y">
            <li v-for="item in cart.items" :key="item.productId" class="p-4">
              <div class="flex justify-between">
                <div class="flex-1 min-w-0">
                  <p class="font-medium text-gray-900 truncate">{{ item.name }}</p>
                  <p class="text-sm text-gray-500">{{ item.sku }}</p>
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
                <span class="font-medium">{{ formatCurrency(item.price * item.quantity) }}</span>
              </div>
            </li>
          </ul>
        </div>

        <!-- Cart Summary -->
        <div class="border-t bg-gray-50 p-4 space-y-2">
          <div class="flex justify-between text-sm">
            <span class="text-gray-500">Subtotal</span>
            <span>{{ formatCurrency(subtotal) }}</span>
          </div>
          <div class="flex justify-between items-center text-sm">
            <div class="flex items-center gap-2">
              <span class="text-gray-500">Tax</span>
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
            <span>Total</span>
            <span class="text-primary-600">{{ formatCurrency(total) }}</span>
          </div>
        </div>

        <!-- Payment Buttons -->
        <div class="p-4 border-t space-y-2">
          <button
            @click="openPayment"
            :disabled="cart.items.length === 0"
            class="btn-primary w-full py-3 text-lg"
          >
            <CreditCardIcon class="h-6 w-6 mr-2" />
            Pay {{ formatCurrency(total) }}
          </button>
          <button
            @click="sellAsDebt"
            :disabled="cart.items.length === 0"
            class="w-full py-2 text-sm border-2 border-orange-500 text-orange-600 rounded-lg hover:bg-orange-50 transition-colors flex items-center justify-center"
          >
            <DocumentTextIcon class="h-5 w-5 mr-2" />
            Sell as Debt
          </button>
        </div>
      </div>
    </div>

    <!-- Payment Modal -->
    <div v-if="showPaymentModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showPaymentModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-lg w-full p-6">
          <h3 class="text-xl font-bold text-gray-900 mb-4">Payment</h3>

          <!-- Total and Remaining -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div class="text-center p-3 bg-gray-100 rounded-lg">
              <p class="text-sm text-gray-500">Total</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(total) }}</p>
            </div>
            <div class="text-center p-3 rounded-lg" :class="remainingAmount > 0 ? 'bg-orange-100' : 'bg-green-100'">
              <p class="text-sm" :class="remainingAmount > 0 ? 'text-orange-600' : 'text-green-600'">Remaining</p>
              <p class="text-2xl font-bold" :class="remainingAmount > 0 ? 'text-orange-700' : 'text-green-700'">
                {{ formatCurrency(remainingAmount) }}
              </p>
            </div>
          </div>

          <!-- Added Payments (Split) -->
          <div v-if="payments.length > 0" class="mb-4">
            <p class="text-sm font-medium text-gray-700 mb-2">Split Payments:</p>
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
            <p class="text-sm font-medium text-gray-700">Add Payment:</p>

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
              <p class="text-sm text-orange-700">Customer is required for debt sales. Please select a customer first.</p>
            </div>

            <!-- Payment Amount -->
            <div>
              <label class="label">Amount</label>
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
              <label class="label">Amount Received</label>
              <input
                v-model.number="currentPayment.received"
                type="number"
                step="0.01"
                min="0"
                class="input text-lg text-center"
              />
              <div v-if="change > 0" class="mt-2 p-3 bg-green-50 rounded-lg text-center">
                <p class="text-sm text-green-600">Change</p>
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
              Add Payment
            </button>
          </div>

          <!-- Action Buttons -->
          <div class="flex space-x-3 mt-6">
            <button @click="showPaymentModal = false" class="btn-secondary flex-1">
              Cancel
            </button>
            <button
              @click="processPayment"
              :disabled="totalPaid < total && remainingAmount > 0"
              class="btn-primary flex-1"
            >
              <CheckIcon class="h-5 w-5 mr-2" />
              Complete Sale
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
          <h3 class="text-lg font-medium text-gray-900 mb-4">Select Customer</h3>

          <div class="relative mb-4">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="customerSearch"
              @input="searchCustomers"
              type="text"
              placeholder="Search customers..."
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
            Cancel
          </button>
        </div>
      </div>
    </div>

    <!-- Quick Add Customer Modal -->
    <div v-if="showNewCustomerModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showNewCustomerModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">Quick Add Customer</h3>

          <div class="space-y-4">
            <div>
              <label class="label">Name *</label>
              <input v-model="newCustomer.name" type="text" class="input" placeholder="Customer name" />
            </div>
            <div>
              <label class="label">Phone</label>
              <input v-model="newCustomer.phone" type="text" class="input" placeholder="Phone number" />
            </div>
            <div>
              <label class="label">Email</label>
              <input v-model="newCustomer.email" type="email" class="input" placeholder="Email address" />
            </div>
          </div>

          <div class="flex space-x-3 mt-6">
            <button @click="showNewCustomerModal = false" class="btn-secondary flex-1">
              Cancel
            </button>
            <button @click="createQuickCustomer" class="btn-primary flex-1">
              <PlusIcon class="h-5 w-5 mr-2" />
              Create & Select
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
