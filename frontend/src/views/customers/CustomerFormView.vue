<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { customersApi } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const togglingHold = ref(false)

// Full customer data from API (for displaying balance info in edit mode)
const customerData = ref(null)

const form = reactive({
  name: '',
  code: '',
  customerType: 'RETAIL',
  phone: '',
  email: '',
  address: '',
  city: '',
  taxId: '',
  creditLimit: null,
  creditHold: false,
  paymentTermsDays: 30,
  active: true
})

const errors = reactive({})

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    try {
      const response = await customersApi.getById(route.params.id)
      const data = response.data
      customerData.value = data
      Object.assign(form, data)
      // Ensure creditLimit is null if 0 for display purposes
      if (!form.creditLimit) form.creditLimit = null
    } catch (error) {
      console.error('Failed to load customer:', error)
    } finally {
      loading.value = false
    }
  }
})

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) errors.name = t('customers.form.nameRequired')
  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  try {
    const payload = { ...form, creditLimit: form.creditLimit || null }
    if (isEdit.value) {
      await customersApi.update(route.params.id, payload)
    } else {
      await customersApi.create(payload)
    }
    router.push('/customers')
  } catch (error) {
    errors.general = error.response?.data?.message || t('customers.form.saveFailed')
  } finally {
    saving.value = false
  }
}

async function toggleCreditHold() {
  if (!isEdit.value) return
  togglingHold.value = true
  try {
    const newHold = !form.creditHold
    const res = await customersApi.setCreditHold(route.params.id, newHold)
    form.creditHold = res.data.creditHold
    customerData.value = res.data
  } catch (error) {
    errors.general = error.response?.data?.message || t('customers.form.creditHoldError')
  } finally {
    togglingHold.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ isEdit ? $t('customers.form.editCustomer') : $t('customers.form.newCustomer') }}</h1>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <div v-if="errors.general" class="p-4 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600">{{ errors.general }}</p>
      </div>

      <!-- Credit hold warning banner -->
      <div v-if="isEdit && form.creditHold" class="p-4 bg-red-50 border border-red-300 rounded-lg flex items-center justify-between">
        <div>
          <p class="font-semibold text-red-700">{{ $t('customers.form.creditHoldActive') }}</p>
          <p class="text-sm text-red-600">{{ $t('customers.form.creditHoldWarning') }}</p>
        </div>
        <button
          type="button"
          @click="toggleCreditHold"
          :disabled="togglingHold"
          class="px-4 py-2 text-sm font-medium text-white bg-green-600 hover:bg-green-700 rounded-lg disabled:opacity-50"
        >
          {{ togglingHold ? $t('customers.form.toggling') : $t('customers.form.enableCredit') }}
        </button>
      </div>

      <!-- Balance info (edit mode only) -->
      <div v-if="isEdit && customerData" class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('customers.form.creditStatus') }}</h3></div>
        <div class="card-body">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-500">{{ $t('customers.form.currentDebt') }}</p>
              <p class="text-lg font-bold" :class="(customerData.currentBalance || 0) > 0 ? 'text-red-600' : 'text-gray-900'">
                {{ formatCurrency(customerData.currentBalance) }} so'm
              </p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-500">{{ $t('customers.form.creditLimit') }}</p>
              <p class="text-lg font-bold text-gray-900">
                {{ customerData.creditLimit ? formatCurrency(customerData.creditLimit) + " " + $t('customers.form.currency') : $t('customers.form.unlimited') }}
              </p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-500">{{ $t('customers.form.availableCredit') }}</p>
              <p class="text-lg font-bold" :class="customerData.overCreditLimit ? 'text-red-600' : 'text-green-600'">
                {{ customerData.creditLimit ? formatCurrency(customerData.availableCredit) + " " + $t('customers.form.currency') : $t('customers.form.unlimited') }}
              </p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-500">{{ $t('customers.form.creditStatus') }}</p>
              <div class="flex items-center gap-2 mt-1">
                <span
                  class="inline-block w-2.5 h-2.5 rounded-full"
                  :class="form.creditHold ? 'bg-red-500' : 'bg-green-500'"
                ></span>
                <span class="text-sm font-medium" :class="form.creditHold ? 'text-red-600' : 'text-green-600'">
                  {{ form.creditHold ? $t('customers.form.suspended') : $t('active') }}
                </span>
              </div>
            </div>
          </div>

          <div class="mt-4 flex items-center gap-4 pt-4 border-t border-gray-100">
            <div class="flex-1">
              <label class="label">{{ $t('customers.form.creditLimit') }} ({{ $t('customers.form.currency') }})</label>
              <input
                v-model.number="form.creditLimit"
                type="number"
                min="0"
                step="1000"
                class="input"
                :placeholder="$t('customers.form.emptyUnlimited')"
              />
              <p class="text-xs text-gray-400 mt-1">{{ $t('customers.form.zeroOrEmptyUnlimited') }}</p>
            </div>
            <div class="flex-1">
              <label class="label">{{ $t('customers.form.paymentTermsDays') }}</label>
              <input
                v-model.number="form.paymentTermsDays"
                type="number"
                min="0"
                step="1"
                class="input"
                placeholder="30"
              />
            </div>
            <div class="pt-5">
              <button
                type="button"
                @click="toggleCreditHold"
                :disabled="togglingHold"
                class="px-4 py-2 text-sm font-medium rounded-lg border disabled:opacity-50"
                :class="form.creditHold
                  ? 'text-green-700 bg-green-50 border-green-200 hover:bg-green-100'
                  : 'text-red-700 bg-red-50 border-red-200 hover:bg-red-100'"
              >
                {{ togglingHold ? '...' : (form.creditHold ? $t('customers.form.enableCredit') : $t('customers.form.disableCredit')) }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Customer info -->
      <div class="card">
        <div class="card-header"><h3 class="text-lg font-medium">{{ $t('customers.form.basicInfo') }}</h3></div>
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('customers.form.fullName') }} *</label>
            <input v-model="form.name" type="text" :class="[errors.name ? 'input-error' : 'input']" />
            <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
          </div>
          <div>
            <label class="label">{{ $t('code') }}</label>
            <input v-model="form.code" type="text" class="input" :placeholder="$t('customers.form.autoGenerated')" :disabled="isEdit" />
          </div>
          <div>
            <label class="label">{{ $t('customers.form.type') }}</label>
            <select v-model="form.customerType" class="input">
              <option value="RETAIL">{{ $t('customers.form.retail') }}</option>
              <option value="WHOLESALE">{{ $t('customers.form.wholesale') }}</option>
              <option value="CORPORATE">{{ $t('customers.form.corporate') }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('customers.form.taxId') }}</label>
            <input v-model="form.taxId" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('phone') }}</label>
            <input v-model="form.phone" type="tel" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('customers.form.email') }}</label>
            <input v-model="form.email" type="email" class="input" />
          </div>
          <div class="md:col-span-2">
            <label class="label">{{ $t('address') }}</label>
            <textarea v-model="form.address" rows="2" class="input"></textarea>
          </div>
          <div>
            <label class="label">{{ $t('customers.form.city') }}</label>
            <input v-model="form.city" type="text" class="input" />
          </div>
          <!-- Credit limit for new customers (edit mode shows it in credit section above) -->
          <div v-if="!isEdit">
            <label class="label">{{ $t('customers.form.creditLimit') }}</label>
            <input v-model.number="form.creditLimit" type="number" min="0" step="1000" class="input" :placeholder="$t('customers.form.emptyUnlimited')" />
            <p class="text-xs text-gray-400 mt-1">{{ $t('customers.form.zeroOrEmptyUnlimited') }}</p>
          </div>
          <div>
            <label class="flex items-center">
              <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
              <span class="ml-2 text-sm">{{ $t('active') }}</span>
            </label>
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <button type="button" @click="router.back()" class="btn-secondary">{{ $t('cancel') }}</button>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : (isEdit ? $t('customers.form.update') : $t('customers.form.create')) }}
        </button>
      </div>
    </form>
  </div>
</template>
