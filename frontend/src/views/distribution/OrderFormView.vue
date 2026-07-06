<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  distributionOrdersApi, distributionAgentsApi, customersApi, productsApi,
  unwrapData, unwrapList
} from '@/services/api'
import { ArrowLeftIcon, PlusIcon, TrashIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const acting = ref(false)
const errors = reactive({})

const customers = ref([])
const agents = ref([])
const products = ref([])
const order = ref(null) // loaded order (for status/lifecycle)

const PAYMENT_METHODS = ['CREDIT', 'CASH', 'MIXED']

const form = reactive({
  customerId: null,
  agentId: null,
  paymentMethod: 'CREDIT',
  paymentTermsDays: null,
  discountAmount: 0,
  taxAmount: 0,
  deliveryFee: 0,
  expectedDeliveryDate: '',
  deliveryAddress: '',
  notes: '',
  lines: []
})

// Only DRAFT orders (or brand-new) are editable; others are read-only + lifecycle actions.
const editable = computed(() => !isEdit.value || order.value?.status === 'DRAFT')

function productPrice(productId) {
  const p = products.value.find(pr => pr.id === productId)
  return p ? Number(p.sellingPrice || 0) : 0
}
function productName(productId) {
  return products.value.find(pr => pr.id === productId)?.name || ''
}

function addLine() {
  form.lines.push({ productId: null, quantity: 1, discountPercent: 0 })
}
function removeLine(i) {
  form.lines.splice(i, 1)
}

// Client-side preview only; the server re-prices authoritatively.
function lineTotal(line) {
  const gross = productPrice(line.productId) * Number(line.quantity || 0)
  const disc = gross * (Number(line.discountPercent || 0) / 100)
  return Math.max(0, gross - disc)
}
const subtotal = computed(() => form.lines.reduce((s, l) => s + lineTotal(l), 0))
const grandTotal = computed(() =>
  Math.max(0, subtotal.value - Number(form.discountAmount || 0)) + Number(form.taxAmount || 0) + Number(form.deliveryFee || 0))

function formatMoney(v) {
  return new Intl.NumberFormat('uz-UZ').format(v || 0)
}

async function fetchLookups() {
  const [custRes, agentRes, prodRes] = await Promise.all([
    customersApi.getActive(),
    distributionAgentsApi.getActive(),
    productsApi.getActive({ size: 500 })
  ])
  customers.value = unwrapList(custRes)
  agents.value = unwrapList(agentRes)
  products.value = unwrapList(prodRes)
}

async function fetchOrder() {
  loading.value = true
  try {
    const o = unwrapData(await distributionOrdersApi.getById(route.params.id))
    order.value = o
    Object.assign(form, {
      customerId: o.customerId,
      agentId: o.agentId,
      paymentMethod: o.paymentMethod || 'CREDIT',
      paymentTermsDays: o.paymentTermsDays,
      discountAmount: o.discountAmount || 0,
      taxAmount: o.taxAmount || 0,
      deliveryFee: o.deliveryFee || 0,
      expectedDeliveryDate: o.expectedDeliveryDate || '',
      deliveryAddress: o.deliveryAddress || '',
      notes: o.notes || '',
      lines: (o.lines || []).map(l => ({
        productId: l.productId, quantity: l.quantity, discountPercent: l.discountPercent || 0
      }))
    })
  } catch (error) {
    console.error('Failed to load order:', error)
    toast.error(t('distribution.orderForm.loadError'))
    router.push('/distribution/orders')
  } finally {
    loading.value = false
  }
}

function validate() {
  Object.keys(errors).forEach(k => delete errors[k])
  if (!form.customerId) errors.customerId = t('distribution.orderForm.customerRequired')
  if (form.lines.length === 0) errors.lines = t('distribution.orderForm.linesRequired')
  if (form.lines.some(l => !l.productId || !(Number(l.quantity) > 0))) errors.lines = t('distribution.orderForm.lineInvalid')
  return Object.keys(errors).length === 0
}

function payload() {
  return {
    customerId: form.customerId,
    agentId: form.agentId || null,
    paymentMethod: form.paymentMethod,
    paymentTermsDays: form.paymentTermsDays === '' ? null : form.paymentTermsDays,
    discountAmount: Number(form.discountAmount || 0),
    taxAmount: Number(form.taxAmount || 0),
    deliveryFee: Number(form.deliveryFee || 0),
    expectedDeliveryDate: form.expectedDeliveryDate || null,
    deliveryAddress: form.deliveryAddress || null,
    notes: form.notes || null,
    lines: form.lines.map(l => ({
      productId: l.productId,
      quantity: Number(l.quantity),
      discountPercent: Number(l.discountPercent || 0)
    }))
  }
}

async function handleSubmit() {
  if (!validate()) return
  saving.value = true
  try {
    if (isEdit.value) {
      await distributionOrdersApi.update(route.params.id, payload())
      await fetchOrder()
      toast.success(t('saved'))
    } else {
      const created = unwrapData(await distributionOrdersApi.create(payload()))
      router.push(`/distribution/orders/${created.id}/edit`)
    }
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.orderForm.saveError'))
  } finally {
    saving.value = false
  }
}

// Which lifecycle action buttons to show for the current status.
const nextActions = computed(() => {
  switch (order.value?.status) {
    case 'CONFIRMED': return ['pick', 'cancel']
    case 'PICKING': return ['load', 'cancel']
    case 'LOADED': return ['transit', 'cancel']
    case 'IN_TRANSIT': return ['deliver', 'cancel']
    case 'DELIVERED': return ['invoice']
    default: return []
  }
})

async function runAction(action) {
  acting.value = true
  try {
    if (action === 'confirm') await distributionOrdersApi.confirm(route.params.id)
    else if (action === 'pick') await distributionOrdersApi.pick(route.params.id)
    else if (action === 'load') await distributionOrdersApi.load(route.params.id)
    else if (action === 'transit') await distributionOrdersApi.transit(route.params.id)
    else if (action === 'deliver') {
      let cash = null
      if (form.paymentMethod !== 'CREDIT') {
        const entered = prompt(t('distribution.orderForm.cashCollectedPrompt'), String(order.value?.totalAmount || 0))
        if (entered === null) { acting.value = false; return }
        cash = Number(entered)
      }
      await distributionOrdersApi.deliver(route.params.id, { cashCollected: cash })
    }
    else if (action === 'invoice') await distributionOrdersApi.invoice(route.params.id)
    else if (action === 'cancel') {
      const reason = prompt(t('distribution.orderForm.cancelPrompt'))
      if (reason === null) { acting.value = false; return }
      await distributionOrdersApi.cancel(route.params.id, { reason })
    }
    await fetchOrder()
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.orderForm.actionError'))
  } finally {
    acting.value = false
  }
}

async function confirmOrder() {
  await runAction('confirm')
}

onMounted(async () => {
  await fetchLookups()
  if (isEdit.value) await fetchOrder()
  else addLine()
})
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center">
        <router-link to="/distribution/orders" class="mr-4 p-2 rounded-lg hover:bg-gray-100">
          <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
        </router-link>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">
            {{ isEdit ? (order?.orderNumber || $t('distribution.orders.title')) : $t('distribution.orders.newOrder') }}
          </h1>
          <p v-if="order" class="text-sm text-gray-500">
            {{ $t('distribution.status.' + order.status) }}
            <span v-if="order.arInvoiceNumber"> · {{ order.arInvoiceNumber }}</span>
          </p>
        </div>
      </div>
      <div class="flex gap-2">
        <button
          v-if="order?.status === 'DRAFT'"
          @click="confirmOrder"
          :disabled="acting"
          class="btn-primary"
        >{{ $t('distribution.orderForm.confirm') }}</button>
        <button
          v-for="a in nextActions"
          :key="a"
          @click="runAction(a)"
          :disabled="acting"
          :class="a === 'cancel' ? 'btn-secondary text-red-600' : 'btn-primary'"
        >{{ $t('distribution.orderForm.action_' + a) }}</button>
      </div>
    </div>

    <div v-if="loading" class="card p-8 text-center text-gray-500">{{ $t('loading') }}</div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <!-- Header -->
      <div class="card">
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('distribution.orders.customer') }} *</label>
            <select v-model.number="form.customerId" class="input" :disabled="!editable">
              <option :value="null">—</option>
              <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }} ({{ c.code }})</option>
            </select>
            <p v-if="errors.customerId" class="text-sm text-red-600 mt-1">{{ errors.customerId }}</p>
          </div>
          <div>
            <label class="label">{{ $t('distribution.orderForm.agent') }}</label>
            <select v-model.number="form.agentId" class="input" :disabled="!editable">
              <option :value="null">—</option>
              <option v-for="a in agents" :key="a.id" :value="a.id">{{ a.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('distribution.orderForm.paymentMethod') }}</label>
            <select v-model="form.paymentMethod" class="input" :disabled="!editable">
              <option v-for="m in PAYMENT_METHODS" :key="m" :value="m">{{ $t('distribution.paymentMethod.' + m) }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('distribution.orderForm.paymentTermsDays') }}</label>
            <input v-model.number="form.paymentTermsDays" type="number" min="0" class="input" :disabled="!editable" />
          </div>
          <div>
            <label class="label">{{ $t('distribution.orderForm.expectedDeliveryDate') }}</label>
            <input v-model="form.expectedDeliveryDate" type="date" class="input" :disabled="!editable" />
          </div>
          <div>
            <label class="label">{{ $t('distribution.orderForm.deliveryAddress') }}</label>
            <input v-model="form.deliveryAddress" type="text" class="input" :disabled="!editable" />
          </div>
        </div>
      </div>

      <!-- Lines -->
      <div class="card">
        <div class="card-body space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-sm font-semibold text-gray-900">{{ $t('distribution.orderForm.lines') }}</h2>
            <button v-if="editable" type="button" @click="addLine" class="btn-secondary text-sm py-1 px-3">
              <PlusIcon class="h-4 w-4 mr-1" />{{ $t('distribution.orderForm.addLine') }}
            </button>
          </div>
          <p v-if="errors.lines" class="text-sm text-red-600">{{ errors.lines }}</p>

          <table class="min-w-full text-sm">
            <thead>
              <tr class="text-left text-xs text-gray-500 uppercase">
                <th class="py-2">{{ $t('distribution.orderForm.product') }}</th>
                <th class="py-2 w-24">{{ $t('distribution.orderForm.quantity') }}</th>
                <th class="py-2 w-24">{{ $t('distribution.orderForm.discountPercent') }}</th>
                <th class="py-2 w-32 text-right">{{ $t('distribution.orderForm.lineTotal') }}</th>
                <th v-if="editable" class="py-2 w-10"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(line, i) in form.lines" :key="i" class="border-t border-gray-100">
                <td class="py-2 pr-2">
                  <select v-model.number="line.productId" class="input" :disabled="!editable">
                    <option :value="null">—</option>
                    <option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option>
                  </select>
                </td>
                <td class="py-2 pr-2">
                  <input v-model.number="line.quantity" type="number" min="0" step="0.001" class="input" :disabled="!editable" />
                </td>
                <td class="py-2 pr-2">
                  <input v-model.number="line.discountPercent" type="number" min="0" max="100" class="input" :disabled="!editable" />
                </td>
                <td class="py-2 text-right text-gray-900">{{ formatMoney(lineTotal(line)) }}</td>
                <td v-if="editable" class="py-2 text-right">
                  <button type="button" @click="removeLine(i)" class="text-red-600 hover:text-red-700">
                    <TrashIcon class="h-4 w-4" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Totals + adjustments -->
      <div class="card">
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-3">
            <div>
              <label class="label">{{ $t('distribution.orderForm.discountAmount') }}</label>
              <input v-model.number="form.discountAmount" type="number" min="0" class="input" :disabled="!editable" />
            </div>
            <div>
              <label class="label">{{ $t('distribution.orderForm.taxAmount') }}</label>
              <input v-model.number="form.taxAmount" type="number" min="0" class="input" :disabled="!editable" />
            </div>
            <div>
              <label class="label">{{ $t('distribution.orderForm.deliveryFee') }}</label>
              <input v-model.number="form.deliveryFee" type="number" min="0" class="input" :disabled="!editable" />
            </div>
          </div>
          <div class="flex flex-col justify-end text-sm space-y-2">
            <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.orderForm.subtotal') }}</span><span>{{ formatMoney(subtotal) }}</span></div>
            <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.orderForm.discountAmount') }}</span><span>-{{ formatMoney(form.discountAmount) }}</span></div>
            <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.orderForm.taxAmount') }}</span><span>{{ formatMoney(form.taxAmount) }}</span></div>
            <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.orderForm.deliveryFee') }}</span><span>{{ formatMoney(form.deliveryFee) }}</span></div>
            <div class="flex justify-between font-semibold text-base border-t pt-2"><span>{{ $t('distribution.orderForm.total') }}</span><span>{{ formatMoney(grandTotal) }}</span></div>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div class="card">
        <div class="card-body">
          <label class="label">{{ $t('distribution.orderForm.notes') }}</label>
          <textarea v-model="form.notes" class="input" rows="2" :disabled="!editable"></textarea>
        </div>
      </div>

      <div v-if="editable" class="flex justify-end space-x-3">
        <router-link to="/distribution/orders" class="btn-secondary">{{ $t('cancel') }}</router-link>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : $t('save') }}
        </button>
      </div>
    </form>
  </div>
</template>
