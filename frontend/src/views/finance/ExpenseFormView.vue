<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { expensesApi, suppliersApi, productsApi } from '@/services/api'
import { ArrowLeftIcon, PlusIcon, TrashIcon, BanknotesIcon } from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const vendors = ref([])
const products = ref([])
const errors = ref({})

const form = ref({
  vendorId: '',
  vendorInvoiceNumber: '',
  invoiceDate: new Date().toISOString().split('T')[0],
  dueDate: '',
  description: '',
  notes: '',
  currency: 'UZS',
  discountAmount: 0,
  taxAmount: 0,
  shippingAmount: 0,
  lines: [
    {
      description: '',
      productId: '',
      quantity: 1,
      unitOfMeasure: 'dona',
      unitPrice: 0,
      discountPercent: 0,
      taxRate: 0,
      notes: ''
    }
  ]
})

// Calculate totals
const subtotal = computed(() => {
  return form.value.lines.reduce((sum, line) => {
    const lineTotal = (line.quantity || 0) * (line.unitPrice || 0)
    const discount = lineTotal * ((line.discountPercent || 0) / 100)
    return sum + lineTotal - discount
  }, 0)
})

const lineTaxTotal = computed(() => {
  return form.value.lines.reduce((sum, line) => {
    const lineTotal = (line.quantity || 0) * (line.unitPrice || 0)
    const discount = lineTotal * ((line.discountPercent || 0) / 100)
    const taxableAmount = lineTotal - discount
    return sum + (taxableAmount * ((line.taxRate || 0) / 100))
  }, 0)
})

const totalAmount = computed(() => {
  return subtotal.value + lineTaxTotal.value + (form.value.taxAmount || 0) +
         (form.value.shippingAmount || 0) - (form.value.discountAmount || 0)
})

async function fetchExpense() {
  if (!isEdit.value) return

  loading.value = true
  try {
    const response = await expensesApi.getById(route.params.id)
    const expense = response.data.data || response.data
    form.value = {
      vendorId: expense.vendorId || '',
      vendorInvoiceNumber: expense.vendorInvoiceNumber || '',
      invoiceDate: expense.invoiceDate || '',
      dueDate: expense.dueDate || '',
      description: expense.description || '',
      notes: expense.notes || '',
      currency: expense.currency || 'UZS',
      discountAmount: Number(expense.discountAmount) || 0,
      taxAmount: Number(expense.taxAmount) || 0,
      shippingAmount: Number(expense.shippingAmount) || 0,
      lines: expense.lines?.map(line => ({
        id: line.id,
        description: line.description || '',
        productId: line.productId || '',
        quantity: Number(line.quantity) || 1,
        unitOfMeasure: line.unitOfMeasure || 'dona',
        unitPrice: Number(line.unitPrice) || 0,
        discountPercent: Number(line.discountPercent) || 0,
        taxRate: Number(line.taxRate) || 0,
        notes: line.notes || ''
      })) || [{ description: '', quantity: 1, unitOfMeasure: 'dona', unitPrice: 0 }]
    }
  } catch (error) {
    console.error('Xarajatni yuklashda xatolik:', error)
    alert('Xarajatni yuklashda xatolik yuz berdi')
    router.push('/finance/expenses')
  } finally {
    loading.value = false
  }
}

async function fetchVendors() {
  try {
    const response = await suppliersApi.getAll({ size: 100 })
    const data = response.data.data || response.data
    vendors.value = data.content || data || []
  } catch (error) {
    console.error('Yetkazib beruvchilarni yuklashda xatolik:', error)
  }
}

async function fetchProducts() {
  try {
    const response = await productsApi.getAll({ size: 100 })
    const data = response.data.data || response.data
    products.value = data.content || data || []
  } catch (error) {
    console.error('Mahsulotlarni yuklashda xatolik:', error)
  }
}

function addLine() {
  form.value.lines.push({
    description: '',
    productId: '',
    quantity: 1,
    unitOfMeasure: 'dona',
    unitPrice: 0,
    discountPercent: 0,
    taxRate: 0,
    notes: ''
  })
}

function removeLine(index) {
  if (form.value.lines.length > 1) {
    form.value.lines.splice(index, 1)
  }
}

function onProductSelect(line, productId) {
  const product = products.value.find(p => p.id === parseInt(productId))
  if (product) {
    line.description = product.name
    line.unitPrice = product.costPrice || product.sellingPrice || 0
    line.unitOfMeasure = product.uom?.code || 'dona'
  }
}

function validateForm() {
  errors.value = {}

  if (!form.value.vendorId && !form.value.description?.trim()) {
    errors.value.description = 'Yetkazib beruvchi yoki tavsif kiritilishi kerak'
  }

  if (!form.value.invoiceDate) {
    errors.value.invoiceDate = 'Sana majburiy'
  }

  if (!form.value.dueDate) {
    errors.value.dueDate = 'To\'lov muddati majburiy'
  }

  if (form.value.lines.length === 0) {
    errors.value.lines = 'Kamida bitta qator bo\'lishi kerak'
  }

  const hasEmptyLine = form.value.lines.some(line => !line.description?.trim())
  if (hasEmptyLine) {
    errors.value.lines = 'Barcha qatorlarda tavsif bo\'lishi kerak'
  }

  return Object.keys(errors.value).length === 0
}

async function saveExpense() {
  if (!validateForm()) return

  saving.value = true
  try {
    const data = {
      vendorId: form.value.vendorId ? parseInt(form.value.vendorId) : null,
      vendorInvoiceNumber: form.value.vendorInvoiceNumber?.trim() || null,
      invoiceDate: form.value.invoiceDate,
      dueDate: form.value.dueDate,
      description: form.value.description?.trim() || null,
      notes: form.value.notes?.trim() || null,
      currency: form.value.currency,
      discountAmount: parseFloat(form.value.discountAmount) || 0,
      taxAmount: parseFloat(form.value.taxAmount) || 0,
      shippingAmount: parseFloat(form.value.shippingAmount) || 0,
      lines: form.value.lines.map(line => ({
        description: line.description.trim(),
        productId: line.productId ? parseInt(line.productId) : null,
        quantity: parseFloat(line.quantity) || 1,
        unitOfMeasure: line.unitOfMeasure || 'dona',
        unitPrice: parseFloat(line.unitPrice) || 0,
        discountPercent: parseFloat(line.discountPercent) || 0,
        taxRate: parseFloat(line.taxRate) || 0,
        notes: line.notes?.trim() || null
      }))
    }

    if (isEdit.value) {
      await expensesApi.update(route.params.id, data)
    } else {
      await expensesApi.create(data)
    }

    router.push('/finance/expenses')
  } catch (error) {
    console.error('Xarajatni saqlashda xatolik:', error)

    if (error.response?.data?.validationErrors) {
      error.response.data.validationErrors.forEach(err => {
        errors.value[err.field] = err.message
      })
    } else if (error.response?.data?.message) {
      alert(error.response.data.message)
    } else {
      alert('Xarajatni saqlashda xatolik yuz berdi')
    }
  } finally {
    saving.value = false
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(Number(value) || 0)
}

function getLineTotal(line) {
  const total = (line.quantity || 0) * (line.unitPrice || 0)
  const discount = total * ((line.discountPercent || 0) / 100)
  const tax = (total - discount) * ((line.taxRate || 0) / 100)
  return total - discount + tax
}

// Set default due date 30 days from invoice date
watch(() => form.value.invoiceDate, (newDate) => {
  if (newDate && !form.value.dueDate) {
    const date = new Date(newDate)
    date.setDate(date.getDate() + 30)
    form.value.dueDate = date.toISOString().split('T')[0]
  }
})

onMounted(() => {
  fetchVendors()
  fetchProducts()
  fetchExpense()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? 'Xarajatni tahrirlash' : 'Yangi xarajat' }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          {{ isEdit ? 'Xarajat ma\'lumotlarini yangilash' : 'Yangi xarajat/hisob-faktura qo\'shish' }}
        </p>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Main Form -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Basic Info -->
        <div class="card">
          <div class="card-header">
            <div class="flex items-center">
              <BanknotesIcon class="h-6 w-6 text-primary-600 mr-2" />
              <h3 class="text-lg font-medium">Asosiy ma'lumotlar</h3>
            </div>
          </div>

          <div class="card-body space-y-4">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <!-- Vendor -->
              <div>
                <label class="label">Yetkazib beruvchi</label>
                <select
                  v-model="form.vendorId"
                  class="input"
                >
                  <option value="">Tanlanmagan (mustaqil xarajat)</option>
                  <option v-for="vendor in vendors" :key="vendor.id" :value="vendor.id">
                    {{ vendor.name }}
                  </option>
                </select>
                <p class="text-xs text-gray-400 mt-1">Ijara, kommunal va boshqa xarajatlar uchun bo'sh qoldiring</p>
              </div>

              <!-- Vendor Invoice Number -->
              <div>
                <label class="label">Yetkazib beruvchi hisob-faktura №</label>
                <input
                  v-model="form.vendorInvoiceNumber"
                  type="text"
                  class="input"
                  placeholder="INV-001"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <!-- Invoice Date -->
              <div>
                <label class="label">
                  Sana <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.invoiceDate"
                  type="date"
                  class="input"
                  :class="{ 'border-red-500': errors.invoiceDate }"
                />
                <p v-if="errors.invoiceDate" class="text-sm text-red-500 mt-1">{{ errors.invoiceDate }}</p>
              </div>

              <!-- Due Date -->
              <div>
                <label class="label">
                  To'lov muddati <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.dueDate"
                  type="date"
                  class="input"
                  :class="{ 'border-red-500': errors.dueDate }"
                />
                <p v-if="errors.dueDate" class="text-sm text-red-500 mt-1">{{ errors.dueDate }}</p>
              </div>
            </div>

            <!-- Description -->
            <div>
              <label class="label">
                Tavsif
                <span v-if="!form.vendorId" class="text-red-500">*</span>
              </label>
              <textarea
                v-model="form.description"
                rows="2"
                class="input"
                :class="{ 'border-red-500': errors.description }"
                :placeholder="!form.vendorId ? 'Masalan: Ijara to\'lovi, Kommunal xarajat...' : 'Xarajat haqida...'"
              ></textarea>
              <p v-if="errors.description" class="text-sm text-red-500 mt-1">{{ errors.description }}</p>
            </div>
          </div>
        </div>

        <!-- Line Items -->
        <div class="card">
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg font-medium">Xarajat qatorlari</h3>
            <button @click="addLine" class="btn-secondary btn-sm">
              <PlusIcon class="h-4 w-4 mr-1" />
              Qator qo'shish
            </button>
          </div>

          <div class="card-body">
            <p v-if="errors.lines" class="text-sm text-red-500 mb-4">{{ errors.lines }}</p>

            <div class="space-y-4">
              <div
                v-for="(line, index) in form.lines"
                :key="index"
                class="border rounded-lg p-4 bg-gray-50"
              >
                <div class="flex justify-between items-start mb-3">
                  <span class="text-sm font-medium text-gray-500">Qator {{ index + 1 }}</span>
                  <button
                    v-if="form.lines.length > 1"
                    @click="removeLine(index)"
                    class="text-red-500 hover:text-red-700"
                  >
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
                  <!-- Product (optional) -->
                  <div>
                    <label class="text-xs text-gray-500">Mahsulot</label>
                    <select
                      v-model="line.productId"
                      @change="onProductSelect(line, line.productId)"
                      class="input input-sm"
                    >
                      <option value="">Tanlang (ixtiyoriy)</option>
                      <option v-for="product in products" :key="product.id" :value="product.id">
                        {{ product.name }}
                      </option>
                    </select>
                  </div>

                  <!-- Description -->
                  <div class="md:col-span-2 lg:col-span-1">
                    <label class="text-xs text-gray-500">Tavsif <span class="text-red-500">*</span></label>
                    <input
                      v-model="line.description"
                      type="text"
                      class="input input-sm"
                      placeholder="Xarajat tavsifi"
                    />
                  </div>

                  <!-- Quantity -->
                  <div>
                    <label class="text-xs text-gray-500">Miqdor</label>
                    <input
                      v-model.number="line.quantity"
                      type="number"
                      min="0.0001"
                      step="any"
                      class="input input-sm"
                    />
                  </div>

                  <!-- Unit -->
                  <div>
                    <label class="text-xs text-gray-500">Birlik</label>
                    <input
                      v-model="line.unitOfMeasure"
                      type="text"
                      class="input input-sm"
                      placeholder="dona"
                    />
                  </div>

                  <!-- Unit Price -->
                  <div>
                    <label class="text-xs text-gray-500">Narx</label>
                    <input
                      v-model.number="line.unitPrice"
                      type="number"
                      min="0"
                      step="any"
                      class="input input-sm"
                    />
                  </div>

                  <!-- Tax Rate -->
                  <div>
                    <label class="text-xs text-gray-500">Soliq %</label>
                    <input
                      v-model.number="line.taxRate"
                      type="number"
                      min="0"
                      max="100"
                      step="any"
                      class="input input-sm"
                    />
                  </div>

                  <!-- Line Total -->
                  <div>
                    <label class="text-xs text-gray-500">Jami</label>
                    <div class="input input-sm bg-gray-100 font-medium">
                      {{ formatCurrency(getLineTotal(line)) }} so'm
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Sidebar - Summary -->
      <div class="lg:col-span-1">
        <div class="card sticky top-24">
          <div class="card-header">
            <h3 class="text-lg font-medium">Jami</h3>
          </div>

          <div class="card-body space-y-4">
            <!-- Additional Charges -->
            <div class="space-y-3">
              <div>
                <label class="text-xs text-gray-500">Yetkazish narxi</label>
                <input
                  v-model.number="form.shippingAmount"
                  type="number"
                  min="0"
                  class="input input-sm"
                />
              </div>
              <div>
                <label class="text-xs text-gray-500">Qo'shimcha soliq</label>
                <input
                  v-model.number="form.taxAmount"
                  type="number"
                  min="0"
                  class="input input-sm"
                />
              </div>
              <div>
                <label class="text-xs text-gray-500">Chegirma</label>
                <input
                  v-model.number="form.discountAmount"
                  type="number"
                  min="0"
                  class="input input-sm"
                />
              </div>
            </div>

            <div class="border-t pt-4 space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-gray-500">Oraliq jami:</span>
                <span>{{ formatCurrency(subtotal) }} so'm</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-500">Qator soliqlari:</span>
                <span>{{ formatCurrency(lineTaxTotal) }} so'm</span>
              </div>
              <div v-if="form.shippingAmount > 0" class="flex justify-between text-sm">
                <span class="text-gray-500">Yetkazish:</span>
                <span>{{ formatCurrency(form.shippingAmount) }} so'm</span>
              </div>
              <div v-if="form.taxAmount > 0" class="flex justify-between text-sm">
                <span class="text-gray-500">Qo'shimcha soliq:</span>
                <span>{{ formatCurrency(form.taxAmount) }} so'm</span>
              </div>
              <div v-if="form.discountAmount > 0" class="flex justify-between text-sm text-green-600">
                <span>Chegirma:</span>
                <span>-{{ formatCurrency(form.discountAmount) }} so'm</span>
              </div>
              <div class="flex justify-between text-lg font-bold border-t pt-2">
                <span>Jami:</span>
                <span class="text-primary-600">{{ formatCurrency(totalAmount) }} so'm</span>
              </div>
            </div>

            <!-- Notes -->
            <div class="pt-4 border-t">
              <label class="label">Izohlar</label>
              <textarea
                v-model="form.notes"
                rows="3"
                class="input"
                placeholder="Ichki izohlar..."
              ></textarea>
            </div>
          </div>

          <div class="card-footer flex justify-end space-x-3">
            <button @click="router.back()" class="btn-secondary">
              Bekor qilish
            </button>
            <button @click="saveExpense" :disabled="saving" class="btn-primary">
              {{ saving ? 'Saqlanmoqda...' : (isEdit ? 'Yangilash' : 'Saqlash') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.input-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}
.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}
</style>
