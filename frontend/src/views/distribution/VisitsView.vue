<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, onMounted } from 'vue'
import { distributionVisitsApi, distributionAgentsApi, unwrapPage, unwrapList } from '@/services/api'
import { MapPinIcon, ArrowRightOnRectangleIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()

const visits = ref([])
const agents = ref([])
const loading = ref(false)
const dateFilter = ref('')
const agentFilter = ref('')
const currentPage = ref(0)
const totalPages = ref(0)

const OUTCOMES = ['ORDER_PLACED', 'NO_ORDER', 'PAYMENT_COLLECTED', 'RESCHEDULED', 'CLOSED']
const outcomeClass = {
  PENDING: 'bg-amber-100 text-amber-800',
  ORDER_PLACED: 'bg-green-100 text-green-800',
  PAYMENT_COLLECTED: 'bg-teal-100 text-teal-800',
  NO_ORDER: 'bg-gray-100 text-gray-800',
  RESCHEDULED: 'bg-blue-100 text-blue-800',
  CLOSED: 'bg-gray-100 text-gray-800'
}

const showCheckout = ref(false)
const checkoutVisit = ref(null)
const checkoutForm = ref({ outcome: 'ORDER_PLACED', notes: '' })

function fmt(instant) {
  return instant ? new Date(instant).toLocaleString('uz-UZ') : '-'
}
function mapsLink(lat, lng) {
  return lat && lng ? `https://maps.google.com/?q=${lat},${lng}` : null
}

async function fetchAgents() {
  agents.value = unwrapList(await distributionAgentsApi.getActive())
}

async function fetchVisits(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 20 }
    if (dateFilter.value) params.date = dateFilter.value
    if (agentFilter.value) params.agentId = agentFilter.value
    const { content, page: meta } = unwrapPage(await distributionVisitsApi.getAll(params))
    visits.value = content
    currentPage.value = meta.number || 0
    totalPages.value = meta.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch visits:', error)
  } finally {
    loading.value = false
  }
}

function openCheckout(visit) {
  checkoutVisit.value = visit
  checkoutForm.value = { outcome: 'ORDER_PLACED', notes: '' }
  showCheckout.value = true
}

async function submitCheckout() {
  try {
    await distributionVisitsApi.checkOut(checkoutVisit.value.id, {
      outcome: checkoutForm.value.outcome,
      notes: checkoutForm.value.notes || null
    })
    showCheckout.value = false
    fetchVisits(currentPage.value)
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.visits.checkoutError'))
  }
}

onMounted(() => { fetchAgents(); fetchVisits() })
</script>

<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('distribution.visits.title') }}</h1>
      <p class="text-sm text-gray-500 mt-1">{{ $t('distribution.visits.subtitle') }}</p>
    </div>

    <div class="card mb-6">
      <div class="card-body flex flex-col md:flex-row gap-4">
        <input v-model="dateFilter" @change="fetchVisits(0)" type="date" class="input md:w-48" />
        <select v-model="agentFilter" @change="fetchVisits(0)" class="input md:w-56">
          <option value="">{{ $t('distribution.visits.allAgents') }}</option>
          <option v-for="a in agents" :key="a.id" :value="a.id">{{ a.name }}</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.visits.customer') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.visits.checkIn') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.visits.checkOut') }}</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.visits.outcome') }}</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading"><td colspan="5" class="px-6 py-8 text-center text-gray-500">{{ $t('loading') }}</td></tr>
            <tr v-else-if="visits.length === 0">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">
                <MapPinIcon class="h-12 w-12 mx-auto mb-2 text-gray-300" />
                <p>{{ $t('distribution.visits.noVisits') }}</p>
              </td>
            </tr>
            <tr v-for="v in visits" :key="v.id" class="hover:bg-gray-50">
              <td class="px-6 py-4">
                <div class="font-medium text-gray-900">{{ v.customerName }}</div>
                <a v-if="mapsLink(v.checkInLat, v.checkInLng)" :href="mapsLink(v.checkInLat, v.checkInLng)" target="_blank" rel="noopener" class="text-xs text-primary-600 inline-flex items-center">
                  <MapPinIcon class="h-3 w-3 mr-1" />{{ $t('distribution.visits.openMap') }}
                </a>
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ fmt(v.checkInAt) }}</td>
              <td class="px-6 py-4 text-sm text-gray-500">{{ fmt(v.checkOutAt) }}</td>
              <td class="px-6 py-4">
                <span :class="['inline-flex px-2 py-1 text-xs font-semibold rounded-full', outcomeClass[v.outcome] || 'bg-gray-100 text-gray-800']">
                  {{ $t('distribution.visitOutcome.' + v.outcome) }}
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                <button v-if="v.outcome === 'PENDING'" @click="openCheckout(v)" class="inline-flex items-center text-primary-600 hover:text-primary-700 text-sm">
                  <ArrowRightOnRectangleIcon class="h-4 w-4 mr-1" />{{ $t('distribution.visits.checkOutAction') }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="px-6 py-3 border-t flex justify-end gap-2">
        <button @click="fetchVisits(currentPage - 1)" :disabled="currentPage === 0" class="btn-secondary text-sm py-1 px-3">{{ $t('previous') }}</button>
        <button @click="fetchVisits(currentPage + 1)" :disabled="currentPage >= totalPages - 1" class="btn-secondary text-sm py-1 px-3">{{ $t('next') }}</button>
      </div>
    </div>

    <!-- Check-out modal -->
    <div v-if="showCheckout" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50" @click.self="showCheckout = false">
      <div class="bg-white rounded-lg shadow-xl w-full max-w-md p-6 space-y-4">
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('distribution.visits.checkOutAction') }} — {{ checkoutVisit?.customerName }}</h3>
        <div>
          <label class="label">{{ $t('distribution.visits.outcome') }}</label>
          <select v-model="checkoutForm.outcome" class="input">
            <option v-for="o in OUTCOMES" :key="o" :value="o">{{ $t('distribution.visitOutcome.' + o) }}</option>
          </select>
        </div>
        <div>
          <label class="label">{{ $t('distribution.visits.notes') }}</label>
          <textarea v-model="checkoutForm.notes" class="input" rows="2"></textarea>
        </div>
        <div class="flex justify-end gap-3">
          <button @click="showCheckout = false" class="btn-secondary">{{ $t('cancel') }}</button>
          <button @click="submitCheckout" class="btn-primary">{{ $t('save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
