<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  distributionVanLoadoutsApi, distributionAgentsApi, warehousesApi, productsApi,
  unwrapData, unwrapList
} from '@/services/api'
import { ArrowLeftIcon, PlusIcon, TrashIcon, PrinterIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'
import { printLoadSheet } from '@/utils/printDocument'

const toast = useToastStore()
const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const isDetail = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const acting = ref(false)
const errors = reactive({})

const agents = ref([])
const locations = ref([])
const products = ref([])
const loadout = ref(null)

const vehicleLocations = computed(() => locations.value.filter(l => l.locationType === 'VEHICLE'))
const warehouseLocations = computed(() => locations.value.filter(l => l.locationType !== 'VEHICLE'))

const form = reactive({
  agentId: null,
  vehicleLocationId: null,
  sourceLocationId: null,
  loadoutDate: '',
  notes: '',
  lines: []
})

// Reconcile inputs (LOADED loadouts): keyed by loadout line id
const recon = reactive({ actualCash: 0, notes: '', lines: {} })

function productName(id) {
  return products.value.find(p => p.id === id)?.name || ''
}
function formatMoney(v) {
  return new Intl.NumberFormat('uz-UZ').format(v || 0)
}

function addLine() {
  form.lines.push({ productId: null, quantityLoaded: 1 })
}
function removeLine(i) {
  form.lines.splice(i, 1)
}

async function fetchLookups() {
  const [agentRes, locRes, prodRes] = await Promise.all([
    distributionAgentsApi.getActive(),
    warehousesApi.getActive(),
    productsApi.getActive({ size: 500 })
  ])
  agents.value = unwrapList(agentRes)
  locations.value = unwrapList(locRes)
  products.value = unwrapList(prodRes)
}

async function fetchLoadout() {
  loading.value = true
  try {
    const lo = unwrapData(await distributionVanLoadoutsApi.getById(route.params.id))
    loadout.value = lo
    recon.actualCash = lo.expectedCash || 0
    ;(lo.lines || []).forEach(l => {
      recon.lines[l.id] = { quantityReturned: l.quantityReturned || 0, quantityDamaged: l.quantityDamaged || 0 }
    })
  } catch (error) {
    console.error('Failed to load loadout:', error)
    toast.error(t('distribution.vanForm.loadError'))
    router.push('/distribution/van-loadouts')
  } finally {
    loading.value = false
  }
}

function validate() {
  Object.keys(errors).forEach(k => delete errors[k])
  if (!form.agentId) errors.agentId = t('distribution.vanForm.agentRequired')
  if (!form.vehicleLocationId) errors.vehicleLocationId = t('distribution.vanForm.vehicleRequired')
  if (!form.sourceLocationId) errors.sourceLocationId = t('distribution.vanForm.sourceRequired')
  if (form.lines.length === 0 || form.lines.some(l => !l.productId || !(Number(l.quantityLoaded) > 0))) {
    errors.lines = t('distribution.vanForm.linesRequired')
  }
  return Object.keys(errors).length === 0
}

async function handleCreate() {
  if (!validate()) return
  saving.value = true
  try {
    const created = unwrapData(await distributionVanLoadoutsApi.create({
      agentId: form.agentId,
      vehicleLocationId: form.vehicleLocationId,
      sourceLocationId: form.sourceLocationId,
      loadoutDate: form.loadoutDate || null,
      notes: form.notes || null,
      lines: form.lines.map(l => ({ productId: l.productId, quantityLoaded: Number(l.quantityLoaded) }))
    }))
    router.push(`/distribution/van-loadouts/${created.id}`)
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.vanForm.saveError'))
  } finally {
    saving.value = false
  }
}

async function runAction(action) {
  acting.value = true
  try {
    if (action === 'load') await distributionVanLoadoutsApi.load(route.params.id)
    else if (action === 'cancel') await distributionVanLoadoutsApi.cancel(route.params.id)
    else if (action === 'reconcile') {
      await distributionVanLoadoutsApi.reconcile(route.params.id, {
        actualCash: Number(recon.actualCash || 0),
        notes: recon.notes || null,
        lines: Object.entries(recon.lines).map(([lineId, v]) => ({
          lineId: Number(lineId),
          quantityReturned: Number(v.quantityReturned || 0),
          quantityDamaged: Number(v.quantityDamaged || 0)
        }))
      })
    }
    await fetchLoadout()
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.vanForm.actionError'))
  } finally {
    acting.value = false
  }
}

function soldPreview(line) {
  const r = recon.lines[line.id] || {}
  return Math.max(0, Number(line.quantityLoaded) - Number(r.quantityReturned || 0) - Number(r.quantityDamaged || 0))
}

function doPrint() {
  if (!loadout.value) return
  const agent = agents.value.find(a => a.id === loadout.value.agentId)
  printLoadSheet(loadout.value, agent?.name, t)
}

onMounted(async () => {
  await fetchLookups()
  if (isDetail.value) await fetchLoadout()
  else addLine()
})
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center">
        <router-link to="/distribution/van-loadouts" class="mr-4 p-2 rounded-lg hover:bg-gray-100">
          <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
        </router-link>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">
            {{ isDetail ? (loadout?.loadoutNumber || $t('distribution.van.title')) : $t('distribution.van.newLoadout') }}
          </h1>
          <p v-if="loadout" class="text-sm text-gray-500">{{ $t('distribution.vanStatus.' + loadout.status) }}</p>
        </div>
      </div>
      <div class="flex gap-2" v-if="loadout">
        <button @click="doPrint" class="btn-secondary"><PrinterIcon class="h-4 w-4 mr-1 inline" />{{ $t('distribution.print.print') }}</button>
        <button v-if="loadout.status === 'DRAFT'" @click="runAction('load')" :disabled="acting" class="btn-primary">{{ $t('distribution.vanForm.load') }}</button>
        <button v-if="loadout.status === 'DRAFT' || loadout.status === 'LOADED'" @click="runAction('cancel')" :disabled="acting" class="btn-secondary text-red-600">{{ $t('distribution.vanForm.cancel') }}</button>
      </div>
    </div>

    <div v-if="loading" class="card p-8 text-center text-gray-500">{{ $t('loading') }}</div>

    <!-- CREATE -->
    <form v-else-if="!isDetail" @submit.prevent="handleCreate" class="space-y-6">
      <div class="card">
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('distribution.vanForm.agent') }} *</label>
            <select v-model.number="form.agentId" class="input"><option :value="null">—</option>
              <option v-for="a in agents" :key="a.id" :value="a.id">{{ a.name }}</option>
            </select>
            <p v-if="errors.agentId" class="text-sm text-red-600 mt-1">{{ errors.agentId }}</p>
          </div>
          <div>
            <label class="label">{{ $t('distribution.vanForm.loadoutDate') }}</label>
            <input v-model="form.loadoutDate" type="date" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('distribution.vanForm.vehicle') }} *</label>
            <select v-model.number="form.vehicleLocationId" class="input"><option :value="null">—</option>
              <option v-for="l in vehicleLocations" :key="l.id" :value="l.id">{{ l.name }}</option>
            </select>
            <p v-if="errors.vehicleLocationId" class="text-sm text-red-600 mt-1">{{ errors.vehicleLocationId }}</p>
            <p v-if="vehicleLocations.length === 0" class="text-xs text-amber-600 mt-1">{{ $t('distribution.vanForm.noVehicleHint') }}</p>
          </div>
          <div>
            <label class="label">{{ $t('distribution.vanForm.source') }} *</label>
            <select v-model.number="form.sourceLocationId" class="input"><option :value="null">—</option>
              <option v-for="l in warehouseLocations" :key="l.id" :value="l.id">{{ l.name }}</option>
            </select>
            <p v-if="errors.sourceLocationId" class="text-sm text-red-600 mt-1">{{ errors.sourceLocationId }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-body space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-sm font-semibold text-gray-900">{{ $t('distribution.vanForm.lines') }}</h2>
            <button type="button" @click="addLine" class="btn-secondary text-sm py-1 px-3"><PlusIcon class="h-4 w-4 mr-1" />{{ $t('distribution.vanForm.addLine') }}</button>
          </div>
          <p v-if="errors.lines" class="text-sm text-red-600">{{ errors.lines }}</p>
          <div v-for="(line, i) in form.lines" :key="i" class="grid grid-cols-12 gap-3 items-end">
            <div class="col-span-8">
              <label class="label">{{ $t('distribution.vanForm.product') }}</label>
              <select v-model.number="line.productId" class="input"><option :value="null">—</option>
                <option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </div>
            <div class="col-span-3">
              <label class="label">{{ $t('distribution.vanForm.quantityLoaded') }}</label>
              <input v-model.number="line.quantityLoaded" type="number" min="0" step="0.001" class="input" />
            </div>
            <div class="col-span-1 flex justify-end pb-1">
              <button type="button" @click="removeLine(i)" class="text-red-600 hover:text-red-700"><TrashIcon class="h-4 w-4" /></button>
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <router-link to="/distribution/van-loadouts" class="btn-secondary">{{ $t('cancel') }}</router-link>
        <button type="submit" :disabled="saving" class="btn-primary">{{ saving ? $t('saving') : $t('save') }}</button>
      </div>
    </form>

    <!-- DETAIL / RECONCILE -->
    <div v-else class="space-y-6">
      <div class="card">
        <div class="card-body">
          <table class="min-w-full text-sm">
            <thead>
              <tr class="text-left text-xs text-gray-500 uppercase">
                <th class="py-2">{{ $t('distribution.vanForm.product') }}</th>
                <th class="py-2 text-right w-24">{{ $t('distribution.vanForm.loaded') }}</th>
                <th v-if="loadout.status === 'LOADED'" class="py-2 w-24">{{ $t('distribution.vanForm.returned') }}</th>
                <th v-if="loadout.status === 'LOADED'" class="py-2 w-24">{{ $t('distribution.vanForm.damaged') }}</th>
                <th v-else class="py-2 text-right w-20">{{ $t('distribution.vanForm.returned') }}</th>
                <th v-if="loadout.status !== 'LOADED'" class="py-2 text-right w-20">{{ $t('distribution.vanForm.damaged') }}</th>
                <th class="py-2 text-right w-20">{{ $t('distribution.vanForm.sold') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="line in loadout.lines" :key="line.id" class="border-t border-gray-100">
                <td class="py-2">{{ line.productName || productName(line.productId) }}</td>
                <td class="py-2 text-right">{{ line.quantityLoaded }}</td>
                <template v-if="loadout.status === 'LOADED'">
                  <td class="py-2 pr-2"><input v-model.number="recon.lines[line.id].quantityReturned" type="number" min="0" step="0.001" class="input" /></td>
                  <td class="py-2 pr-2"><input v-model.number="recon.lines[line.id].quantityDamaged" type="number" min="0" step="0.001" class="input" /></td>
                  <td class="py-2 text-right font-medium">{{ soldPreview(line) }}</td>
                </template>
                <template v-else>
                  <td class="py-2 text-right">{{ line.quantityReturned }}</td>
                  <td class="py-2 text-right">{{ line.quantityDamaged }}</td>
                  <td class="py-2 text-right font-medium">{{ line.quantitySold }}</td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Reconcile form (LOADED) -->
      <div v-if="loadout.status === 'LOADED'" class="card">
        <div class="card-body space-y-4">
          <h2 class="text-sm font-semibold text-gray-900">{{ $t('distribution.vanForm.reconcile') }}</h2>
          <div class="max-w-xs">
            <label class="label">{{ $t('distribution.vanForm.actualCash') }}</label>
            <input v-model.number="recon.actualCash" type="number" min="0" class="input" />
          </div>
          <div class="flex justify-end">
            <button @click="runAction('reconcile')" :disabled="acting" class="btn-primary">{{ $t('distribution.vanForm.doReconcile') }}</button>
          </div>
        </div>
      </div>

      <!-- Reconciled summary -->
      <div v-if="loadout.status === 'RECONCILED'" class="card">
        <div class="card-body grid grid-cols-2 gap-3 text-sm">
          <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.van.loadedValue') }}</span><span>{{ formatMoney(loadout.totalLoadedValue) }}</span></div>
          <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.vanForm.soldValue') }}</span><span>{{ formatMoney(loadout.totalSoldValue) }}</span></div>
          <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.vanForm.expectedCash') }}</span><span>{{ formatMoney(loadout.expectedCash) }}</span></div>
          <div class="flex justify-between"><span class="text-gray-500">{{ $t('distribution.vanForm.actualCash') }}</span><span>{{ formatMoney(loadout.actualCash) }}</span></div>
          <div class="flex justify-between font-semibold col-span-2 border-t pt-2">
            <span>{{ $t('distribution.van.cashDiff') }}</span>
            <span :class="Number(loadout.cashDifference) < 0 ? 'text-red-600' : 'text-green-700'">{{ formatMoney(loadout.cashDifference) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
