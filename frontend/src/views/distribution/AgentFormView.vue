<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { distributionAgentsApi, deliveryRegionsApi, deliveryVillagesApi, unwrapData, unwrapList } from '@/services/api'
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

const regions = ref([])
const villages = ref([])

const STATUSES = ['ACTIVE', 'SUSPENDED', 'TERMINATED']

const form = reactive({
  code: '',
  name: '',
  phone: '',
  email: '',
  vehicleName: '',
  vehiclePlate: '',
  commissionPercent: null,
  status: 'ACTIVE',
  hiredAt: '',
  notes: '',
  territories: []
})

function villagesForRegion(regionId) {
  return villages.value.filter(v => v.region?.id === regionId || v.regionId === regionId)
}

function addTerritory() {
  form.territories.push({ regionId: null, villageId: null, priority: 0, exclusive: false, active: true })
}

function removeTerritory(index) {
  form.territories.splice(index, 1)
}

async function fetchLookups() {
  try {
    const [regionRes, villageRes] = await Promise.all([
      deliveryRegionsApi.getActive(),
      deliveryVillagesApi.getActive()
    ])
    regions.value = unwrapList(regionRes)
    villages.value = unwrapList(villageRes)
  } catch (error) {
    console.error('Failed to load territory lookups:', error)
  }
}

async function fetchAgent() {
  loading.value = true
  try {
    const agent = unwrapData(await distributionAgentsApi.getById(route.params.id))
    Object.assign(form, {
      code: agent.code || '',
      name: agent.name || '',
      phone: agent.phone || '',
      email: agent.email || '',
      vehicleName: agent.vehicleName || '',
      vehiclePlate: agent.vehiclePlate || '',
      commissionPercent: agent.commissionPercent ?? null,
      status: agent.status || 'ACTIVE',
      hiredAt: agent.hiredAt || '',
      notes: agent.notes || '',
      territories: (agent.territories || []).map(tr => ({
        regionId: tr.regionId,
        villageId: tr.villageId,
        priority: tr.priority ?? 0,
        exclusive: !!tr.exclusive,
        active: tr.active !== false
      }))
    })
  } catch (error) {
    console.error('Failed to fetch agent:', error)
    toast.error(t('distribution.agentForm.loadError'))
    router.push('/distribution/agents')
  } finally {
    loading.value = false
  }
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.code?.trim()) errors.code = t('distribution.agentForm.codeRequired')
  if (!form.name?.trim()) errors.name = t('distribution.agentForm.nameRequired')
  if (form.territories.some(tr => !tr.regionId)) errors.territories = t('distribution.agentForm.regionRequired')
  return Object.keys(errors).length === 0
}

function buildPayload() {
  const payload = {
    name: form.name.trim(),
    phone: form.phone || null,
    email: form.email || null,
    vehicleName: form.vehicleName || null,
    vehiclePlate: form.vehiclePlate || null,
    commissionPercent: form.commissionPercent === '' ? null : form.commissionPercent,
    status: form.status,
    hiredAt: form.hiredAt || null,
    notes: form.notes || null,
    territories: form.territories.map(tr => ({
      regionId: tr.regionId,
      villageId: tr.villageId || null,
      priority: tr.priority ?? 0,
      exclusive: !!tr.exclusive,
      active: tr.active !== false
    }))
  }
  if (!isEdit.value) payload.code = form.code.trim()
  return payload
}

async function handleSubmit() {
  if (!validate()) return
  saving.value = true
  try {
    if (isEdit.value) {
      await distributionAgentsApi.update(route.params.id, buildPayload())
    } else {
      await distributionAgentsApi.create(buildPayload())
    }
    router.push('/distribution/agents')
  } catch (error) {
    toast.error(error.response?.data?.message || t('distribution.agentForm.saveError'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fetchLookups()
  if (isEdit.value) await fetchAgent()
})
</script>

<template>
  <div class="max-w-3xl mx-auto">
    <!-- Header -->
    <div class="flex items-center mb-6">
      <router-link to="/distribution/agents" class="mr-4 p-2 rounded-lg hover:bg-gray-100">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </router-link>
      <h1 class="text-2xl font-bold text-gray-900">
        {{ isEdit ? $t('distribution.agentForm.editAgent') : $t('distribution.agentForm.newAgent') }}
      </h1>
    </div>

    <div v-if="loading" class="card p-8 text-center text-gray-500">{{ $t('loading') }}</div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <!-- Identity -->
      <div class="card">
        <div class="card-body grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="label">{{ $t('code') }} *</label>
            <input v-model="form.code" type="text" class="input" :disabled="isEdit" />
            <p v-if="errors.code" class="text-sm text-red-600 mt-1">{{ errors.code }}</p>
          </div>
          <div>
            <label class="label">{{ $t('distribution.agentForm.status') }}</label>
            <select v-model="form.status" class="input">
              <option v-for="s in STATUSES" :key="s" :value="s">{{ $t('distribution.status.' + s) }}</option>
            </select>
          </div>
          <div>
            <label class="label">{{ $t('name') }} *</label>
            <input v-model="form.name" type="text" class="input" />
            <p v-if="errors.name" class="text-sm text-red-600 mt-1">{{ errors.name }}</p>
          </div>
          <div>
            <label class="label">{{ $t('distribution.agents.phone') }}</label>
            <input v-model="form.phone" type="text" class="input" placeholder="+998..." />
          </div>
          <div>
            <label class="label">{{ $t('distribution.agentForm.email') }}</label>
            <input v-model="form.email" type="email" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('distribution.agentForm.hiredAt') }}</label>
            <input v-model="form.hiredAt" type="date" class="input" />
          </div>
        </div>
      </div>

      <!-- Vehicle & commission -->
      <div class="card">
        <div class="card-body grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label class="label">{{ $t('distribution.agentForm.vehicleName') }}</label>
            <input v-model="form.vehicleName" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('distribution.agentForm.vehiclePlate') }}</label>
            <input v-model="form.vehiclePlate" type="text" class="input" />
          </div>
          <div>
            <label class="label">{{ $t('distribution.agentForm.commissionPercent') }}</label>
            <input v-model.number="form.commissionPercent" type="number" step="0.01" min="0" max="100" class="input" />
          </div>
        </div>
      </div>

      <!-- Territories -->
      <div class="card">
        <div class="card-body space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-sm font-semibold text-gray-900">{{ $t('distribution.agentForm.territoriesTitle') }}</h2>
              <p class="text-xs text-gray-500">{{ $t('distribution.agentForm.territoriesHint') }}</p>
            </div>
            <button type="button" @click="addTerritory" class="btn-secondary text-sm py-1 px-3">
              <PlusIcon class="h-4 w-4 mr-1" />
              {{ $t('distribution.agentForm.addTerritory') }}
            </button>
          </div>

          <p v-if="errors.territories" class="text-sm text-red-600">{{ errors.territories }}</p>

          <p v-if="form.territories.length === 0" class="text-sm text-gray-400 py-2">
            {{ $t('distribution.agentForm.noTerritories') }}
          </p>

          <div
            v-for="(tr, index) in form.territories"
            :key="index"
            class="grid grid-cols-1 md:grid-cols-12 gap-3 items-end border-b border-gray-100 pb-4"
          >
            <div class="md:col-span-4">
              <label class="label">{{ $t('distribution.agentForm.region') }} *</label>
              <select v-model.number="tr.regionId" class="input" @change="tr.villageId = null">
                <option :value="null">—</option>
                <option v-for="r in regions" :key="r.id" :value="r.id">{{ r.name }}</option>
              </select>
            </div>
            <div class="md:col-span-4">
              <label class="label">{{ $t('distribution.agentForm.village') }}</label>
              <select v-model.number="tr.villageId" class="input" :disabled="!tr.regionId">
                <option :value="null">{{ $t('distribution.agentForm.wholeRegion') }}</option>
                <option v-for="v in villagesForRegion(tr.regionId)" :key="v.id" :value="v.id">{{ v.name }}</option>
              </select>
            </div>
            <div class="md:col-span-1">
              <label class="label">{{ $t('distribution.agentForm.priority') }}</label>
              <input v-model.number="tr.priority" type="number" min="0" class="input" />
            </div>
            <div class="md:col-span-2 flex items-center gap-2 pb-2">
              <input :id="`excl-${index}`" v-model="tr.exclusive" type="checkbox" class="h-4 w-4 text-primary-600 border-gray-300 rounded" />
              <label :for="`excl-${index}`" class="text-sm text-gray-700">{{ $t('distribution.agentForm.exclusive') }}</label>
            </div>
            <div class="md:col-span-1 flex justify-end pb-1">
              <button type="button" @click="removeTerritory(index)" class="text-red-600 hover:text-red-700">
                <TrashIcon class="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div class="card">
        <div class="card-body">
          <label class="label">{{ $t('distribution.agentForm.notes') }}</label>
          <textarea v-model="form.notes" class="input" rows="3"></textarea>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex justify-end space-x-3">
        <router-link to="/distribution/agents" class="btn-secondary">{{ $t('cancel') }}</router-link>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : $t('save') }}
        </button>
      </div>
    </form>
  </div>
</template>
