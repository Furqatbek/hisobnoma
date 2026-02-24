<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { smsApi } from '@/services/api'
import {
  ChatBubbleLeftRightIcon,
  Cog6ToothIcon,
  CheckCircleIcon,
  XMarkIcon,
  PaperAirplaneIcon,
  CurrencyDollarIcon,
  ClockIcon,
  SignalIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loading = ref(true)
const error = ref('')
const successMsg = ref('')

// Settings
const settingsForm = reactive({ enabled: false, apiToken: '', senderId: '4546' })
const savingSettings = ref(false)

// Balance
const balance = ref(null)

// History
const history = ref([])
const historyLoading = ref(false)
const historyFilter = ref('')
const historyOffset = ref(0)
const historyLimit = 20

// Send SMS
const showSendModal = ref(false)
const sendForm = reactive({ phone: '', message: '', from: '' })
const sending = ref(false)
const sendResult = ref(null)

const isConfigured = computed(() => settingsForm.enabled && settingsForm.apiToken)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const settingsRes = await smsApi.getSettings()
    const data = settingsRes.data?.data || settingsRes.data
    settingsForm.enabled = data.enabled || false
    settingsForm.apiToken = data.apiToken || ''
    settingsForm.senderId = data.senderId || '4546'

    if (data.configured) {
      await Promise.all([loadBalance(), loadHistory()])
    }
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.sms.loadError')
  } finally {
    loading.value = false
  }
}

async function loadBalance() {
  try {
    const res = await smsApi.getBalance()
    balance.value = res.data?.data || res.data
  } catch (e) {
    // Silently fail
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await smsApi.getHistory({
      limit: historyLimit,
      offset: historyOffset.value,
      status: historyFilter.value || undefined
    })
    const data = res.data?.data || res.data
    history.value = data?.data || data?.history || []
  } catch (e) {
    // Silently fail
  } finally {
    historyLoading.value = false
  }
}

async function saveSettings() {
  savingSettings.value = true
  error.value = ''
  successMsg.value = ''
  try {
    const res = await smsApi.saveSettings({
      enabled: settingsForm.enabled,
      apiToken: settingsForm.apiToken,
      senderId: settingsForm.senderId
    })
    const data = res.data?.data || res.data
    if (data.saved) {
      if (data.valid === false) {
        error.value = t('admin.sms.invalidToken')
      } else {
        successMsg.value = t('admin.sms.settingsSaved')
        await loadData()
      }
    }
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.sms.saveError')
  } finally {
    savingSettings.value = false
  }
}

function openSendModal() {
  sendForm.phone = ''
  sendForm.message = ''
  sendForm.from = ''
  sendResult.value = null
  showSendModal.value = true
}

async function handleSend() {
  if (!sendForm.phone.trim() || !sendForm.message.trim()) return
  sending.value = true
  error.value = ''
  sendResult.value = null
  try {
    const res = await smsApi.send({
      phone: sendForm.phone,
      message: sendForm.message,
      from: sendForm.from || undefined
    })
    sendResult.value = res.data?.data || res.data
    successMsg.value = t('admin.sms.smsSent')
    await Promise.all([loadBalance(), loadHistory()])
    showSendModal.value = false
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.sms.sendError')
  } finally {
    sending.value = false
  }
}

function filterHistory(status) {
  historyFilter.value = status
  historyOffset.value = 0
  loadHistory()
}

function nextPage() {
  historyOffset.value += historyLimit
  loadHistory()
}

function prevPage() {
  historyOffset.value = Math.max(0, historyOffset.value - historyLimit)
  loadHistory()
}

function getStatusColor(status) {
  switch (status?.toUpperCase()) {
    case 'DELIVERED': return 'bg-green-100 text-green-800'
    case 'WAITING': return 'bg-yellow-100 text-yellow-800'
    case 'ACCEPTED': return 'bg-blue-100 text-blue-800'
    case 'REJECTED': return 'bg-red-100 text-red-800'
    case 'UNDELIVERABLE': return 'bg-gray-100 text-gray-800'
    default: return 'bg-gray-100 text-gray-600'
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('uz-UZ', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.sms.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.sms.subtitle') }}</p>
      </div>
      <button
        v-if="!loading && isConfigured"
        @click="openSendModal"
        class="btn-primary inline-flex items-center gap-2"
      >
        <PaperAirplaneIcon class="h-4 w-4" />
        {{ $t('admin.sms.sendSms') }}
      </button>
    </div>

    <!-- Error / Success -->
    <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg">
      <p class="text-sm text-red-600">{{ error }}</p>
    </div>
    <div v-if="successMsg" class="p-4 bg-green-50 border border-green-200 rounded-lg flex items-center justify-between">
      <p class="text-sm text-green-600">{{ successMsg }}</p>
      <button @click="successMsg = ''" class="text-green-400 hover:text-green-600">
        <XMarkIcon class="h-4 w-4" />
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <template v-else>
      <!-- Settings Card -->
      <div class="card">
        <div class="card-header flex items-center gap-2">
          <Cog6ToothIcon class="h-5 w-5 text-gray-500" />
          <h3 class="text-lg font-medium">{{ $t('admin.sms.smsSettings') }}</h3>
        </div>
        <div class="card-body space-y-4">
          <!-- Enable toggle -->
          <div class="flex items-center justify-between">
            <div>
              <p class="font-medium text-gray-900">{{ $t('admin.sms.enableSms') }}</p>
              <p class="text-sm text-gray-500">{{ $t('admin.sms.enableSmsDescription') }}</p>
            </div>
            <button
              @click="settingsForm.enabled = !settingsForm.enabled"
              :class="[
                'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out',
                settingsForm.enabled ? 'bg-primary-600' : 'bg-gray-200'
              ]"
            >
              <span
                :class="[
                  'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                  settingsForm.enabled ? 'translate-x-5' : 'translate-x-0'
                ]"
              />
            </button>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="label">{{ $t('admin.sms.apiToken') }}</label>
              <input
                v-model="settingsForm.apiToken"
                type="text"
                class="input font-mono text-sm"
                :placeholder="$t('admin.sms.apiTokenPlaceholder')"
              />
              <p class="mt-1 text-xs text-gray-400">{{ $t('admin.sms.apiTokenHint') }}</p>
            </div>
            <div>
              <label class="label">{{ $t('admin.sms.senderId') }}</label>
              <input
                v-model="settingsForm.senderId"
                type="text"
                class="input"
                placeholder="4546"
              />
              <p class="mt-1 text-xs text-gray-400">{{ $t('admin.sms.senderIdHint') }}</p>
            </div>
          </div>

          <div class="flex justify-end">
            <button
              @click="saveSettings"
              :disabled="savingSettings"
              class="btn-primary inline-flex items-center gap-2"
            >
              <CheckCircleIcon class="h-4 w-4" />
              {{ savingSettings ? $t('saving') : $t('save') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Stats Cards (when configured) -->
      <template v-if="isConfigured && balance">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="card">
            <div class="card-body flex items-center gap-4">
              <div class="p-3 bg-green-100 rounded-full">
                <CurrencyDollarIcon class="h-6 w-6 text-green-600" />
              </div>
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.sms.balance') }}</p>
                <p class="text-lg font-bold text-gray-900">{{ balance.balance ?? '-' }}</p>
                <p class="text-xs text-gray-400">{{ $t('admin.sms.sumUnit') }}</p>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-body flex items-center gap-4">
              <div class="p-3 bg-blue-100 rounded-full">
                <ChatBubbleLeftRightIcon class="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.sms.smsCost') }}</p>
                <p class="text-lg font-bold text-gray-900">{{ balance.sms_price ?? '-' }}</p>
                <p class="text-xs text-gray-400">{{ $t('admin.sms.perSms') }}</p>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-body flex items-center gap-4">
              <div class="p-3 bg-purple-100 rounded-full">
                <SignalIcon class="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.sms.status') }}</p>
                <p class="text-lg font-bold text-green-600">{{ $t('admin.sms.active') }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- SMS History -->
      <template v-if="isConfigured">
        <div class="card">
          <div class="card-header flex items-center justify-between">
            <div class="flex items-center gap-2">
              <ClockIcon class="h-5 w-5 text-gray-500" />
              <h3 class="text-lg font-medium">{{ $t('admin.sms.history') }}</h3>
            </div>
            <div class="flex gap-2">
              <button
                @click="filterHistory('')"
                :class="['px-3 py-1 text-xs rounded-full', !historyFilter ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
              >{{ $t('admin.sms.all') }}</button>
              <button
                @click="filterHistory('DELIVERED')"
                :class="['px-3 py-1 text-xs rounded-full', historyFilter === 'DELIVERED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
              >{{ $t('admin.sms.delivered') }}</button>
              <button
                @click="filterHistory('waiting')"
                :class="['px-3 py-1 text-xs rounded-full', historyFilter === 'waiting' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
              >{{ $t('admin.sms.waiting') }}</button>
              <button
                @click="filterHistory('REJECTED')"
                :class="['px-3 py-1 text-xs rounded-full', historyFilter === 'REJECTED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
              >{{ $t('admin.sms.rejected') }}</button>
            </div>
          </div>

          <div v-if="historyLoading" class="card-body flex items-center justify-center py-8">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>

          <div v-else-if="history.length === 0" class="card-body">
            <div class="text-center py-8 text-gray-500">
              <ChatBubbleLeftRightIcon class="h-12 w-12 mx-auto text-gray-300 mb-3" />
              <p>{{ $t('admin.sms.noHistory') }}</p>
            </div>
          </div>

          <div v-else class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.sms.phone') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.sms.messageText') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.sms.smsStatus') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.sms.cost') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.sms.date') }}</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="(item, idx) in history" :key="idx">
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900">{{ item.phone }}</td>
                  <td class="px-6 py-4 text-sm text-gray-500 max-w-xs truncate">{{ item.message }}</td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span :class="['px-2 py-1 text-xs font-medium rounded-full', getStatusColor(item.status)]">
                      {{ item.status }}
                    </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ item.total_cost ?? '-' }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDate(item.created_at) }}</td>
                </tr>
              </tbody>
            </table>

            <!-- Pagination -->
            <div class="flex items-center justify-between px-6 py-3 border-t border-gray-200">
              <button
                @click="prevPage"
                :disabled="historyOffset === 0"
                class="px-3 py-1 text-sm text-gray-600 bg-gray-100 rounded hover:bg-gray-200 disabled:opacity-50"
              >{{ $t('admin.sms.prev') }}</button>
              <span class="text-sm text-gray-500">{{ historyOffset + 1 }} - {{ historyOffset + history.length }}</span>
              <button
                @click="nextPage"
                :disabled="history.length < historyLimit"
                class="px-3 py-1 text-sm text-gray-600 bg-gray-100 rounded hover:bg-gray-200 disabled:opacity-50"
              >{{ $t('admin.sms.next') }}</button>
            </div>
          </div>
        </div>
      </template>
    </template>

    <!-- Send SMS Modal -->
    <Teleport to="body">
      <div v-if="showSendModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showSendModal = false"></div>
        <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold text-gray-900">{{ $t('admin.sms.sendSms') }}</h3>
            <button @click="showSendModal = false" class="text-gray-400 hover:text-gray-600">
              <XMarkIcon class="h-5 w-5" />
            </button>
          </div>

          <div>
            <label class="label">{{ $t('admin.sms.phone') }}</label>
            <input
              v-model="sendForm.phone"
              type="text"
              class="input font-mono"
              placeholder="998901234567"
            />
            <p class="mt-1 text-xs text-gray-400">{{ $t('admin.sms.phoneHint') }}</p>
          </div>

          <div>
            <label class="label">{{ $t('admin.sms.messageText') }}</label>
            <textarea
              v-model="sendForm.message"
              rows="4"
              class="input"
              :placeholder="$t('admin.sms.messagePlaceholder')"
            ></textarea>
            <p class="mt-1 text-xs text-gray-400">
              {{ sendForm.message.length }} {{ $t('admin.sms.characters') }}
            </p>
          </div>

          <div v-if="sendResult" class="p-3 bg-green-50 border border-green-200 rounded-lg">
            <p class="text-sm text-green-700">
              SMS ID: {{ sendResult.sms_id }} | {{ $t('admin.sms.cost') }}: {{ sendResult.total_cost }}
            </p>
          </div>

          <div class="flex justify-end gap-3 pt-2">
            <button @click="showSendModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button
              @click="handleSend"
              :disabled="sending || !sendForm.phone.trim() || !sendForm.message.trim()"
              class="btn-primary inline-flex items-center gap-2"
            >
              <PaperAirplaneIcon class="h-4 w-4" />
              {{ sending ? $t('admin.sms.sending') : $t('admin.sms.send') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
