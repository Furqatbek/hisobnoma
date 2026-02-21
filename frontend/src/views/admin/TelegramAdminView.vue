<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { telegramApi } from '@/services/api'
import { PaperAirplaneIcon, UserGroupIcon, SignalIcon, XMarkIcon } from '@heroicons/vue/24/outline'

const loading = ref(true)
const botInfo = ref(null)
const users = ref([])
const error = ref('')
const successMsg = ref('')

// Send message modal
const showSendModal = ref(false)
const sendTarget = ref(null) // null = broadcast, user object = direct
const sendForm = reactive({ title: '', message: '' })
const sending = ref(false)

// Computed
const botConnected = computed(() => botInfo.value && botInfo.value.botName)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [infoRes, usersRes] = await Promise.all([
      telegramApi.getBotInfo(),
      telegramApi.getConnectedUsers()
    ])
    botInfo.value = infoRes.data
    users.value = usersRes.data
  } catch (e) {
    if (e.response?.status === 404) {
      error.value = 'Telegram bot yoqilmagan. TELEGRAM_BOT_ENABLED=true sozlamasini belgilang.'
    } else {
      error.value = e.response?.data?.message || 'Ma\'lumotlarni yuklashda xatolik'
    }
  } finally {
    loading.value = false
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
      successMsg.value = `Xabar ${sendTarget.value.fullName} ga yuborildi`
    } else {
      const res = await telegramApi.broadcast({
        title: sendForm.title,
        message: sendForm.message
      })
      successMsg.value = `Xabar ${res.data.recipientCount} foydalanuvchiga yuborildi`
    }
    showSendModal.value = false
  } catch (e) {
    error.value = e.response?.data?.error || e.response?.data?.message || 'Xabar yuborishda xatolik'
  } finally {
    sending.value = false
  }
}

async function unlinkUser(user) {
  if (!confirm(`${user.fullName} ning Telegram ulanishini uzmoqchimisiz?`)) return
  try {
    await telegramApi.adminUnlinkUser(user.id)
    users.value = users.value.filter(u => u.id !== user.id)
    successMsg.value = `${user.fullName} uzildi`
  } catch (e) {
    error.value = e.response?.data?.message || 'Uzishda xatolik'
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
        <h1 class="text-2xl font-bold text-gray-900">Telegram bot boshqaruvi</h1>
        <p class="mt-1 text-sm text-gray-500">Telegram bot holati, ulangan foydalanuvchilar va xabar yuborish</p>
      </div>
      <button
        v-if="!loading && users.length > 0"
        @click="openBroadcast"
        class="btn-primary inline-flex items-center gap-2"
      >
        <PaperAirplaneIcon class="h-4 w-4" />
        Hammaga xabar
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

    <template v-else-if="botInfo">
      <!-- Bot Info Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="card">
          <div class="card-body flex items-center gap-4">
            <div class="p-3 rounded-full" :class="botConnected ? 'bg-green-100' : 'bg-red-100'">
              <SignalIcon class="h-6 w-6" :class="botConnected ? 'text-green-600' : 'text-red-600'" />
            </div>
            <div>
              <p class="text-sm text-gray-500">Bot holati</p>
              <p class="text-lg font-bold" :class="botConnected ? 'text-green-600' : 'text-red-600'">
                {{ botConnected ? 'Ulangan' : 'Ulanmagan' }}
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
              <p class="text-sm text-gray-500">Bot username</p>
              <p class="text-lg font-bold text-gray-900">@{{ botInfo.botUsername }}</p>
              <a
                :href="'https://t.me/' + botInfo.botUsername"
                target="_blank"
                class="text-xs text-blue-600 hover:underline"
              >Telegramda ochish</a>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-body flex items-center gap-4">
            <div class="p-3 bg-purple-100 rounded-full">
              <UserGroupIcon class="h-6 w-6 text-purple-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">Ulangan foydalanuvchilar</p>
              <p class="text-lg font-bold text-gray-900">
                {{ botInfo.connectedUsers }} / {{ botInfo.totalUsers }}
              </p>
              <p class="text-xs text-gray-400">jami foydalanuvchilardan</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Connected Users Table -->
      <div class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">Ulangan foydalanuvchilar</h3>
          <span class="text-sm text-gray-500">{{ users.length }} ta</span>
        </div>
        <div v-if="users.length === 0" class="card-body">
          <div class="text-center py-8 text-gray-500">
            <UserGroupIcon class="h-12 w-12 mx-auto text-gray-300 mb-3" />
            <p>Hali hech kim Telegramga ulanmagan</p>
            <p class="text-sm mt-1">Foydalanuvchilar Sozlamalar > Telegram orqali ulana oladi</p>
          </div>
        </div>
        <div v-else class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Foydalanuvchi</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Username</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Telefon</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ulangan sana</th>
                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amallar</th>
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
                  >Xabar</button>
                  <button
                    @click="unlinkUser(user)"
                    class="text-sm text-red-600 hover:text-red-800"
                  >Uzish</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Send Message Modal -->
    <Teleport to="body">
      <div v-if="showSendModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="showSendModal = false"></div>
        <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-bold text-gray-900">
              {{ sendTarget ? `Xabar: ${sendTarget.fullName}` : 'Barcha foydalanuvchilarga xabar' }}
            </h3>
            <button @click="showSendModal = false" class="text-gray-400 hover:text-gray-600">
              <XMarkIcon class="h-5 w-5" />
            </button>
          </div>

          <div v-if="!sendTarget" class="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p class="text-sm text-yellow-700">
              Bu xabar Telegramga ulangan barcha <b>{{ users.length }}</b> foydalanuvchiga yuboriladi.
            </p>
          </div>

          <div>
            <label class="label">Sarlavha</label>
            <input v-model="sendForm.title" type="text" class="input" placeholder="Xabar sarlavhasi" />
          </div>
          <div>
            <label class="label">Xabar matni</label>
            <textarea v-model="sendForm.message" rows="4" class="input" placeholder="Xabar matni..."></textarea>
          </div>

          <div class="flex justify-end gap-3 pt-2">
            <button @click="showSendModal = false" class="btn-secondary">Bekor qilish</button>
            <button
              @click="handleSend"
              :disabled="sending || !sendForm.title.trim() || !sendForm.message.trim()"
              class="btn-primary inline-flex items-center gap-2"
            >
              <PaperAirplaneIcon class="h-4 w-4" />
              {{ sending ? 'Yuborilmoqda...' : 'Yuborish' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
