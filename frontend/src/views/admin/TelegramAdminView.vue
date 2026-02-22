<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { telegramApi } from '@/services/api'
import { PaperAirplaneIcon, UserGroupIcon, SignalIcon, XMarkIcon, Cog6ToothIcon, CheckCircleIcon, LinkIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loading = ref(true)
const botInfo = ref(null)
const users = ref([])
const error = ref('')
const successMsg = ref('')

// Settings
const settingsForm = reactive({ enabled: false, botToken: '', botUsername: 'hisobnoma_bot' })
const savingSettings = ref(false)
const settingsLoaded = ref(false)

// My Telegram link state
const myTelegram = reactive({
  linked: false,
  linkedAt: '',
  botUsername: '',
  linkCode: '',
  loading: false,
  generatingCode: false,
  unlinking: false
})

// Send message modal
const showSendModal = ref(false)
const sendTarget = ref(null)
const sendForm = reactive({ title: '', message: '' })
const sending = ref(false)

const botConnected = computed(() => botInfo.value && botInfo.value.botName)

onMounted(async () => {
  await Promise.all([loadData(), loadMyTelegramStatus()])
})

async function loadMyTelegramStatus() {
  myTelegram.loading = true
  try {
    const res = await telegramApi.getStatus()
    myTelegram.linked = res.data.linked
    myTelegram.botUsername = res.data.botUsername
    myTelegram.linkedAt = res.data.linkedAt
  } catch (e) {
    // Silently fail — user-facing status endpoint may not be available
  } finally {
    myTelegram.loading = false
  }
}

async function generateMyCode() {
  myTelegram.generatingCode = true
  error.value = ''
  try {
    const res = await telegramApi.generateLinkCode()
    myTelegram.linkCode = res.data.code
    myTelegram.botUsername = res.data.botUsername
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.telegram.codeGenerateError')
  } finally {
    myTelegram.generatingCode = false
  }
}

async function unlinkMyTelegram() {
  if (!confirm(t('admin.telegram.confirmUnlink'))) return
  myTelegram.unlinking = true
  try {
    await telegramApi.unlink()
    myTelegram.linked = false
    myTelegram.linkedAt = ''
    myTelegram.linkCode = ''
    await loadData() // refresh connected users list
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.telegram.unlinkError')
  } finally {
    myTelegram.unlinking = false
  }
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [infoRes, usersRes, settingsRes] = await Promise.all([
      telegramApi.getBotInfo(),
      telegramApi.getConnectedUsers(),
      telegramApi.getSettings()
    ])
    botInfo.value = infoRes.data
    users.value = usersRes.data
    settingsForm.enabled = settingsRes.data.enabled || false
    settingsForm.botToken = settingsRes.data.botToken || ''
    settingsForm.botUsername = settingsRes.data.botUsername || 'hisobnoma_bot'
    settingsLoaded.value = true
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.telegram.dataLoadError')
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  savingSettings.value = true
  error.value = ''
  successMsg.value = ''
  try {
    const res = await telegramApi.saveSettings({
      enabled: settingsForm.enabled,
      botToken: settingsForm.botToken,
      botUsername: settingsForm.botUsername
    })
    if (res.data.saved) {
      if (res.data.valid === false) {
        error.value = res.data.error || t('admin.telegram.invalidToken')
      } else {
        successMsg.value = t('admin.telegram.settingsSaved') + (res.data.botName ? ` — Bot: ${res.data.botName}` : '')
      }
    }
    await loadData()
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.telegram.saveError')
  } finally {
    savingSettings.value = false
  }
}

function openBroadcast() {
  sendTarget.value = null
  sendForm.title = ''
  sendForm.message = ''
  showSendModal.value = true
}

function openSendToUser(user) {
  sendTarget.value = user
  sendForm.title = ''
  sendForm.message = ''
  showSendModal.value = true
}

async function handleSend() {
  if (!sendForm.title.trim() || !sendForm.message.trim()) return
  sending.value = true
  error.value = ''
  successMsg.value = ''
  try {
    if (sendTarget.value) {
      await telegramApi.sendMessage({
        userId: sendTarget.value.id,
        title: sendForm.title,
        message: sendForm.message
      })
      successMsg.value = t('admin.telegram.messageSentTo', { name: sendTarget.value.fullName })
    } else {
      const res = await telegramApi.broadcast({
        title: sendForm.title,
        message: sendForm.message
      })
      successMsg.value = t('admin.telegram.messageBroadcast', { count: res.data.recipientCount })
    }
    showSendModal.value = false
  } catch (e) {
    error.value = e.response?.data?.error || e.response?.data?.message || t('admin.telegram.sendError')
  } finally {
    sending.value = false
  }
}

async function unlinkUser(user) {
  if (!confirm(t('admin.telegram.confirmUnlinkUser', { name: user.fullName }))) return
  try {
    await telegramApi.adminUnlinkUser(user.id)
    users.value = users.value.filter(u => u.id !== user.id)
    successMsg.value = t('admin.telegram.userUnlinked', { name: user.fullName })
  } catch (e) {
    error.value = e.response?.data?.message || t('admin.telegram.unlinkError')
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('uz-UZ', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.telegram.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.telegram.subtitle') }}</p>
      </div>
      <button
        v-if="!loading && botConnected && users.length > 0"
        @click="openBroadcast"
        class="btn-primary inline-flex items-center gap-2"
      >
        <PaperAirplaneIcon class="h-4 w-4" />
        {{ $t('admin.telegram.broadcastAll') }}
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
          <h3 class="text-lg font-medium">{{ $t('admin.telegram.botSettings') }}</h3>
        </div>
        <div class="card-body space-y-4">
          <!-- Enable toggle -->
          <div class="flex items-center justify-between">
            <div>
              <p class="font-medium text-gray-900">{{ $t('admin.telegram.enableBot') }}</p>
              <p class="text-sm text-gray-500">{{ $t('admin.telegram.enableBotDescription') }}</p>
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
              <label class="label">{{ $t('admin.telegram.botToken') }}</label>
              <input
                v-model="settingsForm.botToken"
                type="text"
                class="input font-mono text-sm"
                placeholder="123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
              />
              <p class="mt-1 text-xs text-gray-400">{{ $t('admin.telegram.botTokenHint') }}</p>
            </div>
            <div>
              <label class="label">{{ $t('admin.telegram.botUsername') }}</label>
              <div class="flex">
                <span class="inline-flex items-center px-3 bg-gray-50 border border-r-0 border-gray-300 rounded-l-lg text-gray-500 text-sm">@</span>
                <input
                  v-model="settingsForm.botUsername"
                  type="text"
                  class="input rounded-l-none"
                  placeholder="hisobnoma_bot"
                />
              </div>
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

      <!-- My Telegram Account Link -->
      <div class="card">
        <div class="card-header flex items-center gap-2">
          <LinkIcon class="h-5 w-5 text-gray-500" />
          <h3 class="text-lg font-medium">{{ $t('admin.telegram.myAccount') }}</h3>
        </div>
        <div class="card-body">
          <div v-if="myTelegram.loading" class="flex items-center justify-center py-4">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
          </div>

          <!-- Linked state -->
          <div v-else-if="myTelegram.linked" class="space-y-3">
            <div class="flex items-center gap-3 p-3 bg-green-50 border border-green-200 rounded-lg">
              <span class="inline-block w-3 h-3 rounded-full bg-green-500"></span>
              <div>
                <p class="font-medium text-green-800">{{ $t('admin.telegram.linked') }}</p>
                <p v-if="myTelegram.linkedAt" class="text-sm text-green-600">
                  {{ $t('admin.telegram.linkedDate') }}: {{ formatDate(myTelegram.linkedAt) }}
                </p>
              </div>
            </div>
            <button
              @click="unlinkMyTelegram"
              :disabled="myTelegram.unlinking"
              class="px-4 py-2 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100 disabled:opacity-50"
            >
              {{ myTelegram.unlinking ? $t('admin.telegram.unlinking') : $t('admin.telegram.unlinkTelegram') }}
            </button>
          </div>

          <!-- Not linked state -->
          <div v-else class="space-y-4">
            <div class="p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <p class="font-medium text-blue-800 mb-2">{{ $t('admin.telegram.connectToBot') }}</p>
              <ol class="text-sm text-blue-700 space-y-1 list-decimal list-inside">
                <li>{{ $t('admin.telegram.step1') }}</li>
                <li>{{ $t('admin.telegram.step2') }} <b>@{{ myTelegram.botUsername || settingsForm.botUsername || 'hisobnoma_bot' }}</b></li>
                <li>{{ $t('admin.telegram.step3') }}</li>
              </ol>
            </div>

            <!-- Link code display -->
            <div v-if="myTelegram.linkCode" class="text-center space-y-3">
              <p class="text-sm text-gray-600">{{ $t('admin.telegram.sendCodeToBot') }}</p>
              <div class="inline-block px-8 py-4 bg-gray-900 rounded-xl">
                <span class="text-3xl font-mono font-bold text-white tracking-widest">{{ myTelegram.linkCode }}</span>
              </div>
              <p class="text-xs text-gray-400">{{ $t('admin.telegram.codeExpiry') }}</p>
              <div>
                <a
                  :href="'https://t.me/' + (myTelegram.botUsername || settingsForm.botUsername || 'hisobnoma_bot')"
                  target="_blank"
                  class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 rounded-lg"
                >
                  <PaperAirplaneIcon class="h-4 w-4" />
                  {{ $t('admin.telegram.openBot') }}
                </a>
              </div>
            </div>

            <button
              @click="generateMyCode"
              :disabled="myTelegram.generatingCode"
              class="btn-primary"
            >
              {{ myTelegram.generatingCode ? $t('admin.telegram.generating') : (myTelegram.linkCode ? $t('admin.telegram.getNewCode') : $t('admin.telegram.getCode')) }}
            </button>
          </div>
        </div>
      </div>

      <!-- Bot Info Cards (only when configured) -->
      <template v-if="botInfo">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="card">
            <div class="card-body flex items-center gap-4">
              <div class="p-3 rounded-full" :class="botConnected ? 'bg-green-100' : 'bg-red-100'">
                <SignalIcon class="h-6 w-6" :class="botConnected ? 'text-green-600' : 'text-red-600'" />
              </div>
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.telegram.botStatus') }}</p>
                <p class="text-lg font-bold" :class="botConnected ? 'text-green-600' : 'text-red-600'">
                  {{ botConnected ? $t('admin.telegram.connected') : $t('admin.telegram.disconnected') }}
                </p>
                <p v-if="botInfo.botName" class="text-xs text-gray-400">{{ botInfo.botName }}</p>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-body flex items-center gap-4">
              <div class="p-3 bg-blue-100 rounded-full">
                <PaperAirplaneIcon class="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.telegram.botUsername') }}</p>
                <p class="text-lg font-bold text-gray-900">@{{ botInfo.botUsername }}</p>
                <a
                  :href="'https://t.me/' + botInfo.botUsername"
                  target="_blank"
                  class="text-xs text-blue-600 hover:underline"
                >{{ $t('admin.telegram.openInTelegram') }}</a>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-body flex items-center gap-4">
              <div class="p-3 bg-purple-100 rounded-full">
                <UserGroupIcon class="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <p class="text-sm text-gray-500">{{ $t('admin.telegram.connectedUsers') }}</p>
                <p class="text-lg font-bold text-gray-900">
                  {{ botInfo.connectedUsers }} / {{ botInfo.totalUsers }}
                </p>
                <p class="text-xs text-gray-400">{{ $t('admin.telegram.ofTotalUsers') }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Connected Users Table -->
        <div class="card">
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg font-medium">{{ $t('admin.telegram.connectedUsers') }}</h3>
            <span class="text-sm text-gray-500">{{ users.length }} ta</span>
          </div>
          <div v-if="users.length === 0" class="card-body">
            <div class="text-center py-8 text-gray-500">
              <UserGroupIcon class="h-12 w-12 mx-auto text-gray-300 mb-3" />
              <p>{{ $t('admin.telegram.noConnectedUsers') }}</p>
              <p class="text-sm mt-1">{{ $t('admin.telegram.usersCanConnect') }}</p>
            </div>
          </div>
          <div v-else class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.telegram.user') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.telegram.username') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('phone') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('admin.telegram.linkedDate') }}</th>
                  <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="user in users" :key="user.id">
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center gap-3">
                      <div class="h-8 w-8 bg-blue-100 rounded-full flex items-center justify-center">
                        <span class="text-sm font-medium text-blue-700">{{ user.fullName?.charAt(0) || '?' }}</span>
                      </div>
                      <span class="text-sm font-medium text-gray-900">{{ user.fullName }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.username }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.phone || '-' }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ formatDate(user.linkedAt) }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-right space-x-2">
                    <button
                      @click="openSendToUser(user)"
                      class="text-sm text-blue-600 hover:text-blue-800"
                    >{{ $t('admin.telegram.message') }}</button>
                    <button
                      @click="unlinkUser(user)"
                      class="text-sm text-red-600 hover:text-red-800"
                    >{{ $t('admin.telegram.unlink') }}</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </template>

    <!-- Send Message Modal -->
    <Teleport to="body">
      <div v-if="showSendModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showSendModal = false"></div>
        <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold text-gray-900">
              {{ sendTarget ? $t('admin.telegram.messageTo', { name: sendTarget.fullName }) : $t('admin.telegram.messageToAll') }}
            </h3>
            <button @click="showSendModal = false" class="text-gray-400 hover:text-gray-600">
              <XMarkIcon class="h-5 w-5" />
            </button>
          </div>

          <div v-if="!sendTarget" class="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p class="text-sm text-yellow-700">
              {{ $t('admin.telegram.broadcastWarning', { count: users.length }) }}
            </p>
          </div>

          <div>
            <label class="label">{{ $t('admin.telegram.messageTitle') }}</label>
            <input v-model="sendForm.title" type="text" class="input" :placeholder="$t('admin.telegram.messageTitlePlaceholder')" />
          </div>
          <div>
            <label class="label">{{ $t('admin.telegram.messageBody') }}</label>
            <textarea v-model="sendForm.message" rows="4" class="input" :placeholder="$t('admin.telegram.messageBodyPlaceholder')"></textarea>
          </div>

          <div class="flex justify-end gap-3 pt-2">
            <button @click="showSendModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button
              @click="handleSend"
              :disabled="sending || !sendForm.title.trim() || !sendForm.message.trim()"
              class="btn-primary inline-flex items-center gap-2"
            >
              <PaperAirplaneIcon class="h-4 w-4" />
              {{ sending ? $t('admin.telegram.sending') : $t('admin.telegram.send') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
