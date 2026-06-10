<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { webCampaignsApi, smsApi, promotionsApi } from '@/services/api'
import { MegaphoneIcon, PlusIcon, PaperAirplaneIcon, EyeIcon, PencilIcon, TrashIcon, XMarkIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()

const campaigns = ref([])
const templates = ref([])
const promotions = ref([])
const loading = ref(false)
const error = ref('')
const success = ref('')

// Segments that need a numeric parameter, and whether it's days or money
const PARAM_DAYS = ['ORDERED_LAST_N_DAYS', 'NO_ORDER_IN_N_DAYS']
const PARAM_AMOUNT = ['MIN_TOTAL_SPENT']
const SEGMENTS = ['ALL_CUSTOMERS', 'ORDERED_LAST_N_DAYS', 'NO_ORDER_IN_N_DAYS', 'MIN_TOTAL_SPENT', 'NEVER_ORDERED']

const statusClasses = {
  DRAFT: 'bg-gray-100 text-gray-700',
  SENDING: 'bg-blue-100 text-blue-800',
  SENT: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800'
}

// Create/edit modal
const showForm = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', segmentType: 'ALL_CUSTOMERS', segmentParam: null, smsTemplateId: '', promotionId: '' })

const needsParam = computed(() => PARAM_DAYS.includes(form.segmentType) || PARAM_AMOUNT.includes(form.segmentType))
const paramIsDays = computed(() => PARAM_DAYS.includes(form.segmentType))

// Preview / send modal
const showPreview = ref(false)
const previewData = ref(null)
const previewCampaignId = ref(null)
const sending = ref(false)

async function fetchAll() {
  loading.value = true
  try {
    const [campRes, tmplRes, promoRes] = await Promise.all([
      webCampaignsApi.getAll({ size: 50 }),
      smsApi.getTemplates(),
      promotionsApi.getActive()
    ])
    campaigns.value = campRes.data.content || []
    const tmplData = tmplRes.data.data || tmplRes.data
    templates.value = (Array.isArray(tmplData) ? tmplData : tmplData.content || []).filter(t => t.active)
    const promoData = promoRes.data.data || promoRes.data
    promotions.value = Array.isArray(promoData) ? promoData : promoData.content || []
  } catch (e) {
    if (e.response?.status !== 403) error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    loading.value = false
  }
}

onMounted(fetchAll)

function openCreate() {
  editingId.value = null
  form.name = ''
  form.segmentType = 'ALL_CUSTOMERS'
  form.segmentParam = null
  form.smsTemplateId = ''
  form.promotionId = ''
  showForm.value = true
}

function openEdit(c) {
  editingId.value = c.id
  form.name = c.name
  form.segmentType = c.segmentType
  form.segmentParam = c.segmentParam
  form.smsTemplateId = c.smsTemplateId
  form.promotionId = c.promotionId || ''
  showForm.value = true
}

async function save() {
  if (!form.name?.trim() || !form.smsTemplateId) return
  error.value = ''
  const payload = {
    name: form.name.trim(),
    segmentType: form.segmentType,
    segmentParam: needsParam.value ? form.segmentParam : null,
    smsTemplateId: form.smsTemplateId,
    promotionId: form.promotionId || null
  }
  try {
    if (editingId.value) {
      await webCampaignsApi.update(editingId.value, payload)
    } else {
      await webCampaignsApi.create(payload)
    }
    success.value = t('webCampaigns.saveSuccess')
    showForm.value = false
    fetchAll()
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

async function remove(c) {
  if (!confirm(t('webCampaigns.deleteConfirm'))) return
  try {
    await webCampaignsApi.remove(c.id)
    fetchAll()
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  }
}

// Mandatory preview before sending
async function openPreview(c) {
  previewCampaignId.value = c.id
  previewData.value = null
  showPreview.value = true
  try {
    const res = await webCampaignsApi.preview(c.id)
    previewData.value = res.data.data || res.data
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
    showPreview.value = false
  }
}

async function confirmSend() {
  if (!previewCampaignId.value) return
  sending.value = true
  try {
    await webCampaignsApi.send(previewCampaignId.value)
    success.value = t('webCampaigns.sendStarted')
    showPreview.value = false
    fetchAll()
  } catch (e) {
    error.value = e.response?.data?.message || t('errorOccurred')
  } finally {
    sending.value = false
  }
}

function formatPrice(v) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(v)
}
function formatDate(v) {
  if (!v) return '—'
  return new Date(v).toLocaleDateString('uz-UZ')
}
function segmentLabel(c) {
  let label = t('webCampaigns.seg' + c.segmentType)
  if (c.segmentParam != null) label += ` (${formatPrice(c.segmentParam)})`
  return label
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <MegaphoneIcon class="h-7 w-7 text-teal-600" />
          {{ $t('webCampaigns.title') }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('webCampaigns.subtitle') }}</p>
      </div>
      <button class="btn-primary" @click="openCreate">
        <PlusIcon class="h-5 w-5 mr-1 inline" /> {{ $t('webCampaigns.create') }}
      </button>
    </div>

    <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg flex justify-between">
      <p class="text-sm text-red-600">{{ error }}</p>
      <button @click="error = ''"><XMarkIcon class="h-4 w-4 text-red-400" /></button>
    </div>
    <div v-if="success" class="p-4 bg-green-50 border border-green-200 rounded-lg flex justify-between">
      <p class="text-sm text-green-600">{{ success }}</p>
      <button @click="success = ''"><XMarkIcon class="h-4 w-4 text-green-400" /></button>
    </div>

    <div class="card">
      <div v-if="loading" class="p-8 text-center text-gray-500">{{ $t('loading') }}</div>
      <div v-else-if="campaigns.length === 0" class="p-8 text-center text-gray-500">
        {{ $t('webCampaigns.noCampaigns') }}
      </div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCampaigns.name') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCampaigns.segment') }}</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">{{ $t('webCampaigns.status') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('webCampaigns.recipients') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('webCampaigns.cost') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCampaigns.createdAt') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('webCampaigns.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200 bg-white">
            <tr v-for="c in campaigns" :key="c.id">
              <td class="px-4 py-3 text-sm font-medium text-gray-900">
                {{ c.name }}
                <div v-if="c.promotionCode" class="text-xs text-gray-400 font-mono">{{ c.promotionCode }}</div>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600">{{ segmentLabel(c) }}</td>
              <td class="px-4 py-3 text-center">
                <span :class="statusClasses[c.status]" class="inline-flex px-2.5 py-1 rounded-full text-xs font-medium">
                  {{ $t('webCampaigns.status' + c.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-right">
                {{ c.recipientCount || 0 }}
                <span v-if="c.status === 'SENT' || c.status === 'SENDING'" class="text-xs text-gray-400">
                  ({{ c.sentCount }}✓<span v-if="c.failedCount"> {{ c.failedCount }}✗</span>)
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-right">{{ formatPrice(c.costEstimate) }}</td>
              <td class="px-4 py-3 text-sm text-gray-500">{{ formatDate(c.createdAt) }}</td>
              <td class="px-4 py-3 text-right">
                <div v-if="c.status === 'DRAFT'" class="flex items-center justify-end gap-1">
                  <button class="p-1.5 text-gray-400 hover:text-primary-600" :title="$t('webCampaigns.preview')" @click="openPreview(c)">
                    <PaperAirplaneIcon class="h-5 w-5" />
                  </button>
                  <button class="p-1.5 text-gray-400 hover:text-primary-600" :title="$t('edit')" @click="openEdit(c)">
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button class="p-1.5 text-gray-400 hover:text-red-600" :title="$t('delete')" @click="remove(c)">
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
                <span v-else-if="c.failureReason" class="text-xs text-red-500">{{ c.failureReason }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create / edit modal -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40" @click="showForm = false"></div>
        <div class="relative bg-white rounded-lg shadow-xl w-full max-w-lg p-6 max-h-[90vh] overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-medium">{{ editingId ? $t('webCampaigns.edit') : $t('webCampaigns.create') }}</h3>
            <button @click="showForm = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
          </div>
          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('webCampaigns.name') }} <span class="text-red-500">*</span></label>
              <input v-model="form.name" type="text" class="input" :placeholder="$t('webCampaigns.namePlaceholder')" />
            </div>
            <div>
              <label class="label">{{ $t('webCampaigns.segment') }} <span class="text-red-500">*</span></label>
              <select v-model="form.segmentType" class="input">
                <option v-for="s in SEGMENTS" :key="s" :value="s">{{ $t('webCampaigns.seg' + s) }}</option>
              </select>
            </div>
            <div v-if="needsParam">
              <label class="label">{{ $t('webCampaigns.segmentParam') }} ({{ paramIsDays ? $t('webCampaigns.days') : $t('webCampaigns.amount') }})</label>
              <input v-model.number="form.segmentParam" type="number" min="1" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('webCampaigns.template') }} <span class="text-red-500">*</span></label>
              <select v-model="form.smsTemplateId" class="input">
                <option value="" disabled>{{ $t('webCampaigns.selectTemplate') }}</option>
                <option v-for="tmpl in templates" :key="tmpl.id" :value="tmpl.id">{{ tmpl.name }}</option>
              </select>
            </div>
            <div>
              <label class="label">{{ $t('webCampaigns.promotion') }}</label>
              <select v-model="form.promotionId" class="input">
                <option value="">{{ $t('webCampaigns.noPromotion') }}</option>
                <option v-for="p in promotions" :key="p.id" :value="p.id">{{ p.code }} — {{ p.name }}</option>
              </select>
              <p class="mt-1 text-xs text-gray-400">{{ $t('webCampaigns.promotionHint') }}</p>
            </div>
          </div>
          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" @click="showForm = false">{{ $t('cancel') }}</button>
            <button class="btn-primary" @click="save">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Preview / send modal -->
    <Teleport to="body">
      <div v-if="showPreview" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40" @click="showPreview = false"></div>
        <div class="relative bg-white rounded-lg shadow-xl w-full max-w-md p-6">
          <h3 class="text-lg font-medium mb-4">{{ $t('webCampaigns.previewTitle') }}</h3>
          <div v-if="!previewData" class="py-8 text-center text-gray-500">{{ $t('loading') }}</div>
          <div v-else class="space-y-3">
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('webCampaigns.previewRecipients') }}</span>
              <span class="font-semibold">{{ previewData.recipientCount }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('webCampaigns.previewCost') }}</span>
              <span class="font-semibold">{{ formatPrice(previewData.estimatedCost) }} сўм</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('webCampaigns.previewBalance') }}</span>
              <span>{{ previewData.smsBalance != null ? formatPrice(previewData.smsBalance) + ' сўм' : $t('webCampaigns.balanceUnknown') }}</span>
            </div>
            <div v-if="!previewData.sufficientBalance" class="p-3 bg-red-50 border border-red-200 rounded text-sm text-red-600">
              {{ $t('webCampaigns.insufficientBalance') }}
            </div>
            <p class="text-xs text-gray-400">{{ $t('webCampaigns.sendOnce') }}</p>
          </div>
          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-secondary" @click="showPreview = false">{{ $t('cancel') }}</button>
            <button class="btn-primary"
                    :disabled="!previewData || previewData.recipientCount === 0 || !previewData.sufficientBalance || sending"
                    @click="confirmSend">
              <PaperAirplaneIcon class="h-4 w-4 mr-1 inline" /> {{ $t('webCampaigns.send') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
