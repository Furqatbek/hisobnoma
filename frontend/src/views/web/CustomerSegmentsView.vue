<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { webCustomersApi, promotionsApi, unwrapData } from '@/services/api'

const { t } = useI18n()
const router = useRouter()

const activeDays = ref(30)
const lostDays = ref(90)
const counts = ref(null)
const loading = ref(true)
const error = ref(null)

// Segment definitions: key → backend segment + param resolution
const segmentDefs = () => ([
  { key: 'active', segment: 'ORDERED_LAST_N_DAYS', param: activeDays.value, tone: 'green' },
  { key: 'atRisk', segment: 'NO_ORDER_IN_N_DAYS', param: activeDays.value, tone: 'yellow' },
  { key: 'lost', segment: 'NO_ORDER_IN_N_DAYS', param: lostDays.value, tone: 'red' },
  { key: 'neverOrdered', segment: 'NEVER_ORDERED', param: null, tone: 'gray' }
])

const selected = ref(null)          // { key, segment, param }
const customers = ref([])
const customersLoading = ref(false)

// Issue-coupon modal state
const showIssue = ref(false)
const promotions = ref([])
const issueForm = ref({ promotionId: null, validityDays: 14, sendSms: true, note: '' })
const issuing = ref(false)
const issueResult = ref(null)

async function loadCounts() {
  loading.value = true
  error.value = null
  try {
    counts.value = unwrapData(await webCustomersApi.getSegments({
      activeDays: activeDays.value, lostDays: lostDays.value
    }))
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    loading.value = false
  }
}

async function openSegment(def) {
  selected.value = def
  customersLoading.value = true
  customers.value = []
  try {
    customers.value = unwrapData(await webCustomersApi.getSegmentCustomers(
      def.segment, def.param != null ? { param: def.param } : {})) || []
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    customersLoading.value = false
  }
}

function makeCampaign(def) {
  // Prefill the campaign form: win-back SMS with per-recipient coupon codes.
  router.push({ path: '/web-campaigns', query: { segment: def.segment, param: def.param ?? '' } })
}

async function openIssue() {
  showIssue.value = true
  issueResult.value = null
  if (!promotions.value.length) {
    try {
      const data = unwrapData(await promotionsApi.getAll({ size: 100 }))
      promotions.value = data?.content || data || []
    } catch (e) { /* promotion list optional */ }
  }
}

async function submitIssue() {
  if (!issueForm.value.promotionId || !selected.value) return
  issuing.value = true
  issueResult.value = null
  try {
    const data = unwrapData(await webCustomersApi.issueSegmentCoupons(
      selected.value.segment, selected.value.param, {
        promotionId: issueForm.value.promotionId,
        validityDays: issueForm.value.validityDays,
        sendSms: issueForm.value.sendSms,
        note: issueForm.value.note || null
      }))
    issueResult.value = t('segments.issuedResult', { count: data?.issued ?? 0 })
  } catch (e) {
    issueResult.value = e.response?.data?.message || e.message
  } finally {
    issuing.value = false
  }
}

// Push broadcast modal
const showPush = ref(false)
const pushForm = ref({ title: '', body: '' })
const pushing = ref(false)
const pushResult = ref(null)

function openPush() {
  pushForm.value = { title: '', body: '' }
  pushResult.value = null
  showPush.value = true
}

async function submitPush() {
  if (!pushForm.value.title.trim() || !pushForm.value.body.trim() || !selected.value) return
  pushing.value = true
  pushResult.value = null
  try {
    const data = unwrapData(await webCustomersApi.sendSegmentPush(
      selected.value.segment, selected.value.param, {
        title: pushForm.value.title.trim(),
        body: pushForm.value.body.trim()
      }))
    pushResult.value = t('segments.pushResult', {
      customers: data?.customers ?? 0,
      devices: data?.devices ?? 0
    })
  } catch (e) {
    pushResult.value = e.response?.data?.message || e.message
  } finally {
    pushing.value = false
  }
}

onMounted(loadCounts)
</script>

<template>
  <div>
    <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-gray-900">{{ t('segments.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ t('segments.subtitle') }}</p>
      </div>
      <div class="flex items-end gap-3">
        <div>
          <label class="block text-xs text-gray-500">{{ t('segments.activeDays') }}</label>
          <input v-model.number="activeDays" type="number" min="7" max="365"
                 class="mt-1 w-24 rounded-md border-gray-300 shadow-sm text-sm" />
        </div>
        <div>
          <label class="block text-xs text-gray-500">{{ t('segments.lostDays') }}</label>
          <input v-model.number="lostDays" type="number" min="30" max="730"
                 class="mt-1 w-24 rounded-md border-gray-300 shadow-sm text-sm" />
        </div>
        <button class="rounded-md bg-gray-100 px-3 py-2 text-sm hover:bg-gray-200" @click="loadCounts">
          {{ t('segments.refresh') }}
        </button>
      </div>
    </div>

    <p v-if="error" class="mb-4 text-sm text-red-600">{{ error }}</p>
    <div v-if="loading" class="text-gray-500">{{ t('loading') }}</div>

    <div v-else-if="counts" class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      <button v-for="def in segmentDefs()" :key="def.key"
              class="text-left bg-white shadow rounded-lg p-5 border-2 transition"
              :class="selected?.key === def.key ? 'border-primary-500' : 'border-transparent hover:border-gray-200'"
              @click="openSegment(def)">
        <div class="text-3xl font-bold"
             :class="{ 'text-green-600': def.tone==='green', 'text-yellow-600': def.tone==='yellow',
                       'text-red-600': def.tone==='red', 'text-gray-600': def.tone==='gray' }">
          {{ counts[def.key] ?? 0 }}
        </div>
        <div class="mt-1 font-medium text-gray-900">{{ t(`segments.${def.key}`) }}</div>
        <div class="text-xs text-gray-500">{{ t(`segments.${def.key}Hint`, { active: activeDays, lost: lostDays }) }}</div>
      </button>
    </div>

    <!-- Selected segment: actions + customer list -->
    <div v-if="selected" class="bg-white shadow rounded-lg">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b px-5 py-4">
        <h2 class="font-medium text-gray-900">
          {{ t(`segments.${selected.key}`) }} — {{ customers.length }}
        </h2>
        <div class="flex gap-2">
          <button class="rounded-md bg-primary-600 px-3 py-2 text-sm font-semibold text-white hover:bg-primary-500"
                  @click="openIssue">
            {{ t('segments.issueCoupons') }}
          </button>
          <button class="rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-500"
                  @click="openPush">
            {{ t('segments.sendPush') }}
          </button>
          <button class="rounded-md bg-gray-100 px-3 py-2 text-sm hover:bg-gray-200"
                  @click="makeCampaign(selected)">
            {{ t('segments.makeCampaign') }}
          </button>
        </div>
      </div>
      <div v-if="customersLoading" class="p-5 text-gray-500">{{ t('loading') }}</div>
      <table v-else class="min-w-full divide-y divide-gray-200 text-sm">
        <thead class="bg-gray-50 text-left text-xs uppercase text-gray-500">
          <tr>
            <th class="px-5 py-3">{{ t('segments.colName') }}</th>
            <th class="px-5 py-3">{{ t('segments.colPhone') }}</th>
            <th class="px-5 py-3">{{ t('segments.colOrders') }}</th>
            <th class="px-5 py-3">{{ t('segments.colLastOrder') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="c in customers" :key="c.id">
            <td class="px-5 py-3">{{ c.name || '—' }}</td>
            <td class="px-5 py-3">+{{ c.phone }}</td>
            <td class="px-5 py-3">{{ c.orderCount ?? 0 }}</td>
            <td class="px-5 py-3">{{ c.lastOrderAt ? new Date(c.lastOrderAt).toLocaleDateString() : '—' }}</td>
          </tr>
          <tr v-if="!customers.length">
            <td colspan="4" class="px-5 py-6 text-center text-gray-400">{{ t('segments.empty') }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Issue coupons modal -->
    <div v-if="showIssue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <h3 class="text-lg font-medium text-gray-900">{{ t('segments.issueCoupons') }}</h3>
        <p class="mt-1 text-sm text-gray-500">
          {{ t('segments.issueHint', { count: customers.length }) }}
        </p>
        <div class="mt-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('segments.promotion') }}</label>
            <select v-model="issueForm.promotionId" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm">
              <option :value="null" disabled>{{ t('segments.pickPromotion') }}</option>
              <option v-for="p in promotions" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700">{{ t('segments.validityDays') }}</label>
              <input v-model.number="issueForm.validityDays" type="number" min="1" max="365"
                     class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
            </div>
            <label class="mt-6 inline-flex items-center gap-2 text-sm text-gray-700">
              <input v-model="issueForm.sendSms" type="checkbox" class="rounded border-gray-300" />
              {{ t('segments.sendSms') }}
            </label>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('segments.note') }}</label>
            <input v-model="issueForm.note" type="text" maxlength="200"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
          </div>
          <p v-if="issueResult" class="text-sm" :class="issueResult.includes('✓') ? 'text-green-600' : 'text-gray-700'">
            {{ issueResult }}
          </p>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button class="rounded-md bg-gray-100 px-4 py-2 text-sm hover:bg-gray-200" @click="showIssue = false">
            {{ t('close') }}
          </button>
          <button class="rounded-md bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-500 disabled:opacity-50"
                  :disabled="issuing || !issueForm.promotionId" @click="submitIssue">
            {{ issuing ? t('saving') : t('segments.issue') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Push broadcast modal -->
    <div v-if="showPush" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="showPush = false"></div>
      <div class="relative bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h3 class="text-lg font-medium text-gray-900">{{ t('segments.sendPush') }}</h3>
        <p class="mt-1 text-sm text-gray-500">
          {{ t(`segments.${selected.key}`) }} — {{ customers.length }}
        </p>
        <div class="mt-4 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('segments.pushTitle') }}</label>
            <input v-model="pushForm.title" type="text" maxlength="100"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('segments.pushBody') }}</label>
            <textarea v-model="pushForm.body" rows="3" maxlength="500"
                      class="mt-1 block w-full rounded-md border-gray-300 shadow-sm"></textarea>
          </div>
          <p v-if="pushResult" class="text-sm text-gray-700">{{ pushResult }}</p>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button class="rounded-md bg-gray-100 px-4 py-2 text-sm hover:bg-gray-200" @click="showPush = false">
            {{ t('close') }}
          </button>
          <button class="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500 disabled:opacity-50"
                  :disabled="pushing || !pushForm.title.trim() || !pushForm.body.trim()" @click="submitPush">
            {{ pushing ? t('saving') : t('segments.pushSend') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
