<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, computed, onMounted } from 'vue'
import { distributionKpiApi, distributionAgentTargetsApi, unwrapData } from '@/services/api'
import { TrophyIcon, FlagIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()
const { t } = useI18n()

function isoDate(d) {
  return d.toISOString().slice(0, 10)
}
const now = new Date()
const from = ref(isoDate(new Date(now.getFullYear(), now.getMonth(), 1)))
const to = ref(isoDate(new Date(now.getFullYear(), now.getMonth() + 1, 0)))

const rows = ref([])
const loading = ref(false)

function money(v) {
  return new Intl.NumberFormat('uz-UZ').format(Math.round(v || 0))
}
function pct(actual, target) {
  if (!target || Number(target) <= 0) return null
  return Math.round((Number(actual) / Number(target)) * 100)
}
function barClass(p) {
  if (p == null) return 'bg-gray-300'
  if (p >= 100) return 'bg-green-500'
  if (p >= 60) return 'bg-amber-500'
  return 'bg-red-500'
}

const trend = ref([])
const maxTrendRevenue = computed(() =>
  Math.max(1, ...trend.value.map(d => Number(d.revenue) || 0)))

function trendTooltip(d) {
  return `${d.date}: ${money(d.revenue)} · ${d.orders} ${t('distribution.kpi.orders')} · ${d.visits} ${t('distribution.kpi.visits')}`
}

async function fetchDashboard() {
  loading.value = true
  try {
    rows.value = unwrapData(await distributionKpiApi.dashboard(from.value, to.value)) || []
    trend.value = unwrapData(await distributionKpiApi.trend(from.value, to.value)) || []
  } catch (error) {
    console.error('Failed to load KPI dashboard:', error)
  } finally {
    loading.value = false
  }
}

// Target editor
const showTarget = ref(false)
const targetForm = ref({ agentId: null, agentName: '', targetRevenue: 0, targetOrders: 0, targetVisits: 0, targetCollection: 0 })

function openTarget(row) {
  targetForm.value = {
    agentId: row.agentId, agentName: row.agentName,
    targetRevenue: row.targetRevenue || 0, targetOrders: row.targetOrders || 0,
    targetVisits: row.targetVisits || 0, targetCollection: row.targetCollection || 0
  }
  showTarget.value = true
}

async function saveTarget() {
  try {
    // Find an existing target for this agent+period, else create.
    const existing = (unwrapData(await distributionAgentTargetsApi.byAgent(targetForm.value.agentId)) || [])
      .find(x => x.periodStart === from.value && x.periodEnd === to.value)
    const body = {
      targetRevenue: Number(targetForm.value.targetRevenue || 0),
      targetOrders: Number(targetForm.value.targetOrders || 0),
      targetVisits: Number(targetForm.value.targetVisits || 0),
      targetCollection: Number(targetForm.value.targetCollection || 0)
    }
    if (existing) {
      await distributionAgentTargetsApi.update(existing.id, body)
    } else {
      await distributionAgentTargetsApi.create({
        agentId: targetForm.value.agentId, periodType: 'MONTHLY',
        periodStart: from.value, periodEnd: to.value, ...body
      })
    }
    showTarget.value = false
    fetchDashboard()
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.kpi.saveTargetError'))
  }
}

const totals = computed(() => rows.value.reduce((acc, r) => ({
  revenue: acc.revenue + Number(r.revenue || 0),
  orders: acc.orders + Number(r.orders || 0),
  visits: acc.visits + Number(r.visits || 0)
}), { revenue: 0, orders: 0, visits: 0 }))

onMounted(fetchDashboard)
</script>

<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('distribution.kpi.title') }}</h1>
      <p class="text-sm text-gray-500 mt-1">{{ $t('distribution.kpi.subtitle') }}</p>
    </div>

    <div class="card mb-6">
      <div class="card-body flex flex-col md:flex-row gap-4 items-end">
        <div>
          <label class="label">{{ $t('distribution.kpi.from') }}</label>
          <input v-model="from" type="date" class="input" />
        </div>
        <div>
          <label class="label">{{ $t('distribution.kpi.to') }}</label>
          <input v-model="to" type="date" class="input" />
        </div>
        <button @click="fetchDashboard" class="btn-primary">{{ $t('distribution.kpi.apply') }}</button>
      </div>
    </div>

    <!-- Summary tiles -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
      <div class="card"><div class="card-body">
        <p class="text-xs text-gray-500 uppercase">{{ $t('distribution.kpi.totalRevenue') }}</p>
        <p class="text-2xl font-bold text-gray-900">{{ money(totals.revenue) }}</p>
      </div></div>
      <div class="card"><div class="card-body">
        <p class="text-xs text-gray-500 uppercase">{{ $t('distribution.kpi.totalOrders') }}</p>
        <p class="text-2xl font-bold text-gray-900">{{ totals.orders }}</p>
      </div></div>
      <div class="card"><div class="card-body">
        <p class="text-xs text-gray-500 uppercase">{{ $t('distribution.kpi.totalVisits') }}</p>
        <p class="text-2xl font-bold text-gray-900">{{ totals.visits }}</p>
      </div></div>
    </div>

    <!-- Revenue trend -->
    <div v-if="trend.length" class="card mb-6"><div class="card-body">
      <p class="text-xs text-gray-500 uppercase mb-3">{{ $t('distribution.kpi.revenueTrend') }}</p>
      <div class="flex items-end gap-1 h-32">
        <div v-for="d in trend" :key="d.date" class="flex-1 flex flex-col items-center justify-end group">
          <div class="w-full bg-primary-500 hover:bg-primary-600 rounded-t transition-colors"
               :style="{ height: Math.max(2, (Number(d.revenue) / maxTrendRevenue) * 100) + '%' }"
               :title="trendTooltip(d)"></div>
        </div>
      </div>
      <div class="flex justify-between text-[10px] text-gray-400 mt-1">
        <span>{{ trend[0]?.date }}</span>
        <span>{{ trend[trend.length - 1]?.date }}</span>
      </div>
    </div></div>

    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">#</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.kpi.agent') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase w-64">{{ $t('distribution.kpi.revenue') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.kpi.orders') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.kpi.visits') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.kpi.customers') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase" :title="$t('distribution.kpi.strikeRateHint')">{{ $t('distribution.kpi.strikeRate') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase" :title="$t('distribution.kpi.avgDropHint')">{{ $t('distribution.kpi.avgDrop') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('distribution.kpi.cash') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-if="loading"><td colspan="8" class="px-6 py-8 text-center text-gray-500">{{ $t('loading') }}</td></tr>
            <tr v-else-if="rows.length === 0"><td colspan="8" class="px-6 py-8 text-center text-gray-500">{{ $t('distribution.kpi.noData') }}</td></tr>
            <tr v-for="(row, i) in rows" :key="row.agentId" class="hover:bg-gray-50">
              <td class="px-4 py-4">
                <TrophyIcon v-if="i === 0 && Number(row.revenue) > 0" class="h-5 w-5 text-amber-500" />
                <span v-else class="text-sm text-gray-500">{{ i + 1 }}</span>
              </td>
              <td class="px-4 py-4 font-medium text-gray-900">{{ row.agentName }}</td>
              <td class="px-4 py-4">
                <div class="text-sm text-gray-900">{{ money(row.revenue) }}<span v-if="row.targetRevenue" class="text-gray-400"> / {{ money(row.targetRevenue) }}</span></div>
                <div v-if="row.targetRevenue" class="mt-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div :class="['h-full rounded-full', barClass(pct(row.revenue, row.targetRevenue))]" :style="{ width: Math.min(100, pct(row.revenue, row.targetRevenue) || 0) + '%' }"></div>
                </div>
              </td>
              <td class="px-4 py-4 text-right text-sm text-gray-700">{{ row.orders }}<span v-if="row.targetOrders" class="text-gray-400"> / {{ row.targetOrders }}</span></td>
              <td class="px-4 py-4 text-right text-sm text-gray-700">{{ row.visits }}<span v-if="row.targetVisits" class="text-gray-400"> / {{ row.targetVisits }}</span></td>
              <td class="px-4 py-4 text-right text-sm text-gray-700">{{ row.customersReached }}</td>
              <td class="px-4 py-4 text-right text-sm text-gray-700">{{ row.strikeRatePercent != null ? row.strikeRatePercent + '%' : '—' }}</td>
              <td class="px-4 py-4 text-right text-sm text-gray-700">{{ row.avgDropSize != null ? money(row.avgDropSize) : '—' }}</td>
              <td class="px-4 py-4 text-right text-sm text-gray-700">{{ money(row.cashCollected) }}</td>
              <td class="px-4 py-4 text-right">
                <button @click="openTarget(row)" class="inline-flex items-center text-primary-600 hover:text-primary-700 text-sm">
                  <FlagIcon class="h-4 w-4 mr-1" />{{ $t('distribution.kpi.setTarget') }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Target modal -->
    <div v-if="showTarget" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50" @click.self="showTarget = false">
      <div class="bg-white rounded-lg shadow-xl w-full max-w-md p-6 space-y-4">
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('distribution.kpi.setTarget') }} — {{ targetForm.agentName }}</h3>
        <p class="text-xs text-gray-500">{{ from }} … {{ to }}</p>
        <div class="grid grid-cols-2 gap-4">
          <div><label class="label">{{ $t('distribution.kpi.targetRevenue') }}</label><input v-model.number="targetForm.targetRevenue" type="number" min="0" class="input" /></div>
          <div><label class="label">{{ $t('distribution.kpi.targetOrders') }}</label><input v-model.number="targetForm.targetOrders" type="number" min="0" class="input" /></div>
          <div><label class="label">{{ $t('distribution.kpi.targetVisits') }}</label><input v-model.number="targetForm.targetVisits" type="number" min="0" class="input" /></div>
          <div><label class="label">{{ $t('distribution.kpi.targetCollection') }}</label><input v-model.number="targetForm.targetCollection" type="number" min="0" class="input" /></div>
        </div>
        <div class="flex justify-end gap-3">
          <button @click="showTarget = false" class="btn-secondary">{{ $t('cancel') }}</button>
          <button @click="saveTarget" class="btn-primary">{{ $t('save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
