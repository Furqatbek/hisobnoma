<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  distributionRoutesApi, distributionAgentsApi, customersApi, deliveryRegionsApi,
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
const errors = reactive({})

const agents = ref([])
const customers = ref([])
const regions = ref([])

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const STATUSES = ['DRAFT', 'ACTIVE', 'ARCHIVED']

const form = reactive({
  code: '', name: '', agentId: null, territoryRegionId: null,
  dayOfWeek: null, estimatedDurationMinutes: null, distanceKm: null,
  status: 'DRAFT', notes: '', stops: []
})

function addStop() {
  form.stops.push({ customerId: null, sortOrder: form.stops.length + 1, address: '', visitWindowStart: '', visitWindowEnd: '' })
}
function removeStop(i) {
  form.stops.splice(i, 1)
}

async function fetchLookups() {
  const [agentRes, custRes, regionRes] = await Promise.all([
    distributionAgentsApi.getActive(),
    customersApi.getActive(),
    deliveryRegionsApi.getActive()
  ])
  agents.value = unwrapList(agentRes)
  customers.value = unwrapList(custRes)
  regions.value = unwrapList(regionRes)
}

async function fetchRoute() {
  loading.value = true
  try {
    const r = unwrapData(await distributionRoutesApi.getById(route.params.id))
    Object.assign(form, {
      code: r.code || '', name: r.name || '', agentId: r.agentId, territoryRegionId: r.territoryRegionId,
      dayOfWeek: r.dayOfWeek, estimatedDurationMinutes: r.estimatedDurationMinutes, distanceKm: r.distanceKm,
      status: r.status || 'DRAFT', notes: r.notes || '',
      stops: (r.stops || []).map(s => ({
        customerId: s.customerId, sortOrder: s.sortOrder ?? 0, address: s.address || '',
        visitWindowStart: s.visitWindowStart || '', visitWindowEnd: s.visitWindowEnd || ''
      }))
    })
  } catch (error) {
    console.error('Failed to load route:', error)
    toast.error(t('distribution.routeForm.loadError'))
    router.push('/distribution/routes')
  } finally {
    loading.value = false
  }
}

function validate() {
  Object.keys(errors).forEach(k => delete errors[k])
  if (!form.code?.trim()) errors.code = t('distribution.routeForm.codeRequired')
  if (!form.name?.trim()) errors.name = t('distribution.routeForm.nameRequired')
  if (form.stops.some(s => !s.customerId)) errors.stops = t('distribution.routeForm.stopCustomerRequired')
  return Object.keys(errors).length === 0
}

function payload() {
  const p = {
    name: form.name.trim(),
    agentId: form.agentId || null,
    territoryRegionId: form.territoryRegionId || null,
    dayOfWeek: form.dayOfWeek || null,
    estimatedDurationMinutes: form.estimatedDurationMinutes === '' ? null : form.estimatedDurationMinutes,
    distanceKm: form.distanceKm === '' ? null : form.distanceKm,
    status: form.status,
    notes: form.notes || null,
    stops: form.stops.map(s => ({
      customerId: s.customerId,
      sortOrder: Number(s.sortOrder || 0),
      address: s.address || null,
      visitWindowStart: s.visitWindowStart || null,
      visitWindowEnd: s.visitWindowEnd || null
    }))
  }
  if (!isEdit.value) p.code = form.code.trim()
  return p
}

async function handleSubmit() {
  if (!validate()) return
  saving.value = true
  try {
    if (isEdit.value) await distributionRoutesApi.update(route.params.id, payload())
    else await distributionRoutesApi.create(payload())
    router.push('/distribution/routes')
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.routeForm.saveError'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fetchLookups()
  if (isEdit.value) await fetchRoute()
  else addStop()
})
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center mb-6">
      <router-link to="/distribution/routes" class="mr-4 p-2 rounded-lg hover:bg-gray-100"><ArrowLeftIcon class="h-5 w-5 text-gray-500" /></router-link>
      <h1 class="text-2xl font-bold text-gray-900">{{ isEdit ? $t('distribution.routeForm.editRoute') : $t('distribution.routeForm.newRoute') }}</h1>
    </div>

    <div v-if="loading" class="card p-8 text-center text-gray-500">{{ $t('loading') }}</div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <div class="card">
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('code') }} *</label>
            <input v-model="form.code" type="text" class="input" :disabled="isEdit" />
            <p v-if="errors.code" class="text-sm text-red-600 mt-1">{{ errors.code }}</p>
          </div>
          <div>
            <label class="label">{{ $t('name') }} *</label>
            <input v-model="form.name" type="text" class="input" />
            <p v-if="errors.name" class="text-sm text-red-600 mt-1">{{ errors.name }}</p>
          </div>
          <div>
            <label class="label">{{ $t('distribution.routeForm.agent') }}</label>
            <select v-model.number="form.agentId" class="input"><option :value="null">—</option>
              <option v-for="a in agents" :key="a.id" :value="a.id">{{ a.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('distribution.routeForm.region') }}</label>
            <select v-model.number="form.territoryRegionId" class="input"><option :value="null">—</option>
              <option v-for="r in regions" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('distribution.routeForm.dayOfWeek') }}</label>
            <select v-model="form.dayOfWeek" class="input"><option :value="null">—</option>
              <option v-for="d in DAYS" :key="d" :value="d">{{ $t('distribution.days.' + d) }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('distribution.routeForm.status') }}</label>
            <select v-model="form.status" class="input">
              <option v-for="s in STATUSES" :key="s" :value="s">{{ $t('distribution.routeStatus.' + s) }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('distribution.routeForm.duration') }}</label>
            <input v-model.number="form.estimatedDurationMinutes" type="number" min="0" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('distribution.routeForm.distance') }}</label>
            <input v-model.number="form.distanceKm" type="number" min="0" step="0.1" class="input" />
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-body space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-sm font-semibold text-gray-900">{{ $t('distribution.routeForm.stops') }}</h2>
            <button type="button" @click="addStop" class="btn-secondary text-sm py-1 px-3"><PlusIcon class="h-4 w-4 mr-1" />{{ $t('distribution.routeForm.addStop') }}</button>
          </div>
          <p v-if="errors.stops" class="text-sm text-red-600">{{ errors.stops }}</p>
          <div v-for="(stop, i) in form.stops" :key="i" class="grid grid-cols-12 gap-3 items-end border-b border-gray-100 pb-4">
            <div class="col-span-1">
              <label class="label">#</label>
              <input v-model.number="stop.sortOrder" type="number" min="0" class="input" />
            </div>
            <div class="col-span-4">
              <label class="label">{{ $t('distribution.routeForm.customer') }} *</label>
              <select v-model.number="stop.customerId" class="input"><option :value="null">—</option>
                <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="col-span-4">
              <label class="label">{{ $t('distribution.routeForm.address') }}</label>
              <input v-model="stop.address" type="text" class="input" />
            </div>
            <div class="col-span-2">
              <label class="label">{{ $t('distribution.routeForm.window') }}</label>
              <input v-model="stop.visitWindowStart" type="time" class="input" />
            </div>
            <div class="col-span-1 flex justify-end pb-1">
              <button type="button" @click="removeStop(i)" class="text-red-600 hover:text-red-700"><TrashIcon class="h-4 w-4" /></button>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-body">
          <label class="label">{{ $t('distribution.routeForm.notes') }}</label>
          <textarea v-model="form.notes" class="input" rows="2"></textarea>
        </div>
      </div>

      <div class="flex justify-end space-x-3">
        <router-link to="/distribution/routes" class="btn-secondary">{{ $t('cancel') }}</router-link>
        <button type="submit" :disabled="saving" class="btn-primary">{{ saving ? $t('saving') : $t('save') }}</button>
      </div>
    </form>
  </div>
</template>
