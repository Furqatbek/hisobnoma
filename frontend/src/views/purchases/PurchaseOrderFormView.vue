<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { purchaseOrdersApi, suppliersApi, productsApi } from '@/services/api'
import { ArrowLeftIcon, PlusIcon, TrashIcon, MagnifyingGlassIcon } from '@heroicons/vue/24/outline'

const router = useRouter()
const saving = ref(false)
const suppliers = ref([])
const products = ref([])
const productSearch = ref('')

const form = reactive({
  supplierId: null,
  expectedDate: '',
  notes: '',
  items: []
})

const total = computed(() => {
  return form.items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0)
})

onMounted(async () => {
  try {
    const response = await suppliersApi.getAll({ size: 100 })
    suppliers.value = response.data.content || []
  } catch (error) {
    console.error('Failed to load suppliers:', error)
  }
})

async function searchProducts() {
  if (!productSearch.value.trim()) {
    products.value = []
    return
  }
  try {
    const response = await productsApi.search(productSearch.value)
    products.value = response.data.content || response.data || []
  } catch (error) {
    console.error('Search failed:', error)
  }
}

function addProduct(product) {
  const existing = form.items.find(i => i.productId === product.id)
  if (existing) {
    existing.quantity++
  } else {
    form.items.push({
      productId: product.id,
      productName: product.name,
      sku: product.sku,
      quantity: 1,
      unitPrice: product.costPrice || 0
    })
  }
  productSearch.value = ''
  products.value = []
}

function removeItem(index) {
  form.items.splice(index, 1)
}

async function handleSubmit() {
  if (!form.supplierId || form.items.length === 0) {
    alert('Please select a supplier and add at least one item')
    return
  }

  saving.value = true
  try {
    await purchaseOrdersApi.create({
      supplierId: form.supplierId,
      expectedDate: form.expectedDate || null,
      notes: form.notes,
      items: form.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice
      }))
    })
    router.push('/purchases/orders')
  } catch (error) {
    console.error('Failed to create order:', error)
    alert('Failed to create order')
  } finally {
    saving.value = false
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <h1 class="text-2xl font-bold text-gray-900">New Purchase Order</h1>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-6">
      <!-- Order Info -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">Order Information</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label class="label">Supplier *</label>
            <select v-model="form.supplierId" class="input">
              <option :value="null">Select supplier</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">Expected Date</label>
            <input v-model="form.expectedDate" type="date" class="input" />
          </div>
          <div>
            <label class="label">Notes</label>
            <input v-model="form.notes" type="text" class="input" />
          </div>
        </div>
      </div>

      <!-- Products -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">Products</h3></div>
        <div class="card-body">
          <!-- Search -->
          <div class="relative mb-4">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="productSearch"
              @input="searchProducts"
              type="text"
              placeholder="Search products..."
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
                <div class="text-sm text-gray-500">{{ product.sku }}</div>
              </div>
              <div class="text-right">
                <div class="font-medium">{{ formatCurrency(product.costPrice) }}</div>
              </div>
            </button>
          </div>

          <!-- Items Table -->
          <div v-if="form.items.length > 0" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th class="text-right">Quantity</th>
                  <th class="text-right">Unit Price</th>
                  <th class="text-right">Subtotal</th>
                  <th></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="(item, index) in form.items" :key="item.productId">
                  <td>
                    <div class="font-medium">{{ item.productName }}</div>
                    <div class="text-sm text-gray-500">{{ item.sku }}</div>
                  </td>
                  <td class="text-right">
                    <input
                      v-model.number="item.quantity"
                      type="number"
                      min="1"
                      class="input w-20 text-right"
                    />
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
                  <td colspan="3" class="text-right font-medium">Total:</td>
                  <td class="text-right text-lg font-bold text-primary-600">{{ formatCurrency(total) }}</td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>

          <div v-else class="text-center py-8 text-gray-500">
            Search and add products to this order
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">Cancel</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? 'Creating...' : 'Create Order' }}
        </button>
      </div>
    </form>
  </div>
</template>
