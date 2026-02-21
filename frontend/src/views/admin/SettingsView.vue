<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Cog6ToothIcon, BuildingStorefrontIcon, CurrencyDollarIcon, BellIcon, PrinterIcon, PaperAirplaneIcon } from '@heroicons/vue/24/outline'
import { useReceiptStore } from '@/stores/receipt'
import { telegramApi } from '@/services/api'
import ReceiptTemplate from '@/components/ReceiptTemplate.vue'
import InvoiceA4Template from '@/components/InvoiceA4Template.vue'

const receiptStore = useReceiptStore()
const activeTab = ref('general')
const receiptRef = ref(null)
const invoiceA4Ref = ref(null)

// Telegram state
const telegram = reactive({
  linked: false,
  botUsername: '',
  linkedAt: '',
  linkCode: '',
  loading: false,
  generatingCode: false,
  unlinking: false,
  error: ''
})

const settings = reactive({
  company: {
    name: 'My Company',
    address: '',
    phone: '',
    email: '',
    taxId: ''
  },
  pos: {
    defaultTaxRate: 12,
    allowNegativeStock: false,
    requireCustomer: false,
    printReceipt: true
  },
  notifications: {
    lowStockAlert: true,
    lowStockThreshold: 10,
    emailNotifications: true,
    smsNotifications: false
  }
})

// Receipt settings - synced with store
const receiptSettings = reactive({
  brandName: '',
  phone: '',
  address: '',
  website: '',
  taxId: '',
  footerText: '',
  paperWidth: '80'
})

// Sample transaction for preview
const sampleTransaction = {
  id: 12345,
  transactionNumber: '#12345',
  createdAt: new Date().toISOString(),
  customer: { name: 'Namuna mijoz', phone: '+998 90 123 45 67' },
  items: [
    { id: 1, productName: 'Mahsulot 1', quantity: 2, unitPrice: 50000 },
    { id: 2, productName: 'Mahsulot 2', quantity: 1, unitPrice: 75000 }
  ],
  totalAmount: 175000,
  payments: [{ paymentType: 'CASH', amount: 175000 }]
}

onMounted(() => {
  // Load receipt settings from store
  receiptSettings.brandName = receiptStore.config.brandName
  receiptSettings.phone = receiptStore.config.phone
  receiptSettings.address = receiptStore.config.address
  receiptSettings.website = receiptStore.config.website
  receiptSettings.taxId = receiptStore.config.taxId
  receiptSettings.footerText = receiptStore.config.footerText
  receiptSettings.paperWidth = receiptStore.config.paperWidth
})

const saving = ref(false)

async function saveSettings() {
  saving.value = true
  try {
    // API call would go here
    await new Promise(resolve => setTimeout(resolve, 1000))
    alert('Sozlamalar saqlandi!')
  } catch (error) {
    alert('Sozlamalarni saqlashda xatolik')
  } finally {
    saving.value = false
  }
}

function saveReceiptSettings() {
  receiptStore.updateConfig(receiptSettings)
  alert('Chek sozlamalari saqlandi!')
}

function printTestReceipt() {
  if (receiptSettings.paperWidth === 'A4') {
    if (invoiceA4Ref.value) {
      invoiceA4Ref.value.printInvoice()
    }
  } else if (receiptRef.value) {
    receiptRef.value.printReceipt()
  }
}

// Telegram functions
async function loadTelegramStatus() {
  telegram.loading = true
  telegram.error = ''
  try {
    const res = await telegramApi.getStatus()
    telegram.linked = res.data.linked
    telegram.botUsername = res.data.botUsername
    telegram.linkedAt = res.data.linkedAt
  } catch (e) {
    // Telegram not enabled on backend — hide the tab silently
    telegram.error = e.response?.status === 404 ? '' : 'Telegram holatini yuklashda xatolik'
  } finally {
    telegram.loading = false
  }
}

async function generateTelegramCode() {
  telegram.generatingCode = true
  telegram.error = ''
  try {
    const res = await telegramApi.generateLinkCode()
    telegram.linkCode = res.data.code
    telegram.botUsername = res.data.botUsername
  } catch (e) {
    telegram.error = e.response?.data?.message || 'Kod yaratishda xatolik'
  } finally {
    telegram.generatingCode = false
  }
}

async function unlinkTelegram() {
  if (!confirm('Telegram akkauntni uzmoqchimisiz?')) return
  telegram.unlinking = true
  try {
    await telegramApi.unlink()
    telegram.linked = false
    telegram.linkedAt = ''
    telegram.linkCode = ''
  } catch (e) {
    telegram.error = e.response?.data?.message || 'Uzishda xatolik'
  } finally {
    telegram.unlinking = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'telegram') loadTelegramStatus()
})

const tabs = [
  { key: 'general', name: 'Umumiy', icon: BuildingStorefrontIcon },
  { key: 'pos', name: 'Kassa (POS)', icon: CurrencyDollarIcon },
  { key: 'receipt', name: 'Chek sozlamalari', icon: PrinterIcon },
  { key: 'notifications', name: 'Bildirishnomalar', icon: BellIcon },
  { key: 'telegram', name: 'Telegram', icon: PaperAirplaneIcon }
]
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Sozlamalar</h1>
      <p class="mt-1 text-sm text-gray-500">Tizim konfiguratsiyasi</p>
    </div>

    <div class="flex flex-col lg:flex-row gap-6">
      <!-- Tabs -->
      <div class="lg:w-64">
        <nav class="space-y-1">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            @click="activeTab = tab.key"
            :class="[
              'w-full flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors',
              activeTab === tab.key
                ? 'bg-primary-50 text-primary-700'
                : 'text-gray-700 hover:bg-gray-100'
            ]"
          >
            <component :is="tab.icon" class="h-5 w-5 mr-3" />
            {{ tab.name }}
          </button>
        </nav>
      </div>

      <!-- Content -->
      <div class="flex-1">
        <!-- General Settings -->
        <div v-show="activeTab === 'general'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Kompaniya ma'lumotlari</h3>
          </div>
          <div class="card-body space-y-4">
            <div>
              <label class="label">Kompaniya nomi</label>
              <input v-model="settings.company.name" type="text" class="input" />
            </div>
            <div>
              <label class="label">Manzil</label>
              <textarea v-model="settings.company.address" rows="2" class="input"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">Telefon</label>
                <input v-model="settings.company.phone" type="tel" class="input" />
              </div>
              <div>
                <label class="label">Email</label>
                <input v-model="settings.company.email" type="email" class="input" />
              </div>
            </div>
            <div>
              <label class="label">STIR (Soliq to'lovchi identifikatsiya raqami)</label>
              <input v-model="settings.company.taxId" type="text" class="input" />
            </div>
          </div>
        </div>

        <!-- POS Settings -->
        <div v-show="activeTab === 'pos'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Kassa (POS) sozlamalari</h3>
          </div>
          <div class="card-body space-y-4">
            <div>
              <label class="label">Standart soliq stavkasi (%)</label>
              <input v-model.number="settings.pos.defaultTaxRate" type="number" min="0" max="100" step="0.1" class="input w-32" />
            </div>
            <div class="space-y-3">
              <label class="flex items-center">
                <input v-model="settings.pos.allowNegativeStock" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Salbiy zaxirada sotishga ruxsat berish</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.pos.requireCustomer" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Har bir sotuvda mijozni talab qilish</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.pos.printReceipt" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Sotuvdan keyin chekni avtomatik chop etish</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Receipt Settings -->
        <div v-show="activeTab === 'receipt'" class="space-y-6">
          <div class="flex gap-6">
            <!-- Receipt Form -->
            <div class="flex-1 card">
              <div class="card-header">
                <h3 class="text-lg font-medium">Chek ma'lumotlari</h3>
                <p class="text-sm text-gray-500 mt-1">{{ receiptSettings.paperWidth === 'A4' ? 'A4 formatdagi hisob-faktura shabloni' : 'Termal printer uchun chek shabloni' }}</p>
              </div>
              <div class="card-body space-y-4">
                <div>
                  <label class="label">Do'kon/Brend nomi <span class="text-red-500">*</span></label>
                  <input
                    v-model="receiptSettings.brandName"
                    type="text"
                    class="input"
                    placeholder="Do'koningiz nomi"
                  />
                </div>
                <div>
                  <label class="label">Manzil</label>
                  <input
                    v-model="receiptSettings.address"
                    type="text"
                    class="input"
                    placeholder="Manzil: Toshkent sh., Chilonzor t."
                  />
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="label">Telefon raqami</label>
                    <input
                      v-model="receiptSettings.phone"
                      type="tel"
                      class="input"
                      placeholder="+998 XX XXX XX XX"
                    />
                  </div>
                  <div>
                    <label class="label">Veb-sayt</label>
                    <input
                      v-model="receiptSettings.website"
                      type="text"
                      class="input"
                      placeholder="www.example.uz"
                    />
                  </div>
                </div>
                <div>
                  <label class="label">STIR</label>
                  <input
                    v-model="receiptSettings.taxId"
                    type="text"
                    class="input"
                    placeholder="123456789"
                  />
                </div>
                <div>
                  <label class="label">Chek pastki qismi matni</label>
                  <input
                    v-model="receiptSettings.footerText"
                    type="text"
                    class="input"
                    placeholder="Xaridingiz uchun rahmat!"
                  />
                </div>
                <div>
                  <label class="label">Qog'oz kengligi</label>
                  <select v-model="receiptSettings.paperWidth" class="input w-40">
                    <option value="58">58mm</option>
                    <option value="80">80mm</option>
                    <option value="A4">A4</option>
                  </select>
                  <p class="text-xs text-gray-500 mt-1">
                    {{ receiptSettings.paperWidth === 'A4' ? 'A4 formatdagi chek (210mm x 297mm)' : 'Termal printeringiz qog\'oz o\'lchami' }}
                  </p>
                </div>

                <div class="flex gap-3 pt-4 border-t">
                  <button @click="saveReceiptSettings" class="btn-primary">
                    Saqlash
                  </button>
                  <button @click="printTestReceipt" class="btn-secondary">
                    Test chek chop etish
                  </button>
                </div>
              </div>
            </div>

            <!-- Receipt Preview (80mm / 58mm) -->
            <div v-if="receiptSettings.paperWidth !== 'A4'" class="w-80">
              <div class="card">
                <div class="card-header">
                  <h3 class="text-lg font-medium text-center">Ko'rinishi ({{ receiptSettings.paperWidth }}mm)</h3>
                </div>
                <div class="card-body p-2">
                  <div class="bg-white border rounded-lg overflow-hidden">
                    <ReceiptTemplate
                      ref="receiptRef"
                      :transaction="sampleTransaction"
                      type="sale"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- Invoice Preview (A4) -->
            <div v-else class="flex-1 max-w-lg">
              <div class="card">
                <div class="card-header">
                  <h3 class="text-lg font-medium text-center">Ko'rinishi (A4)</h3>
                </div>
                <div class="card-body p-2">
                  <div class="bg-white border rounded-lg overflow-hidden">
                    <InvoiceA4Template
                      ref="invoiceA4Ref"
                      :transaction="sampleTransaction"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Notification Settings -->
        <div v-show="activeTab === 'notifications'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Bildirishnoma sozlamalari</h3>
          </div>
          <div class="card-body space-y-4">
            <div class="space-y-3">
              <label class="flex items-center">
                <input v-model="settings.notifications.lowStockAlert" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Kam zaxira ogohlantirishlarini yoqish</span>
              </label>
              <div v-if="settings.notifications.lowStockAlert" class="ml-6">
                <label class="label">Zaxira quyidagi miqdordan kam bo'lganda ogohlantirish</label>
                <input v-model.number="settings.notifications.lowStockThreshold" type="number" min="1" class="input w-24" />
              </div>
            </div>
            <div class="space-y-3 pt-4 border-t">
              <label class="flex items-center">
                <input v-model="settings.notifications.emailNotifications" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Email bildirishnomalari</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.notifications.smsNotifications" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">SMS bildirishnomalari</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Telegram Settings -->
        <div v-show="activeTab === 'telegram'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Telegram bot</h3>
            <p class="text-sm text-gray-500 mt-1">Telegram orqali bildirishnomalar olish</p>
          </div>
          <div class="card-body space-y-6">
            <div v-if="telegram.error" class="p-3 bg-red-50 border border-red-200 rounded-lg">
              <p class="text-sm text-red-600">{{ telegram.error }}</p>
            </div>

            <div v-if="telegram.loading" class="flex items-center justify-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>

            <template v-else>
              <!-- Linked state -->
              <div v-if="telegram.linked" class="space-y-4">
                <div class="flex items-center gap-3 p-4 bg-green-50 border border-green-200 rounded-lg">
                  <span class="inline-block w-3 h-3 rounded-full bg-green-500"></span>
                  <div>
                    <p class="font-medium text-green-800">Telegram ulangan</p>
                    <p v-if="telegram.linkedAt" class="text-sm text-green-600">
                      Ulangan sana: {{ new Date(telegram.linkedAt).toLocaleDateString('uz-UZ') }}
                    </p>
                  </div>
                </div>
                <p class="text-sm text-gray-600">
                  Siz bildirishnomalarni Telegram orqali olasiz. Bildirishnoma turlarini
                  "Bildirishnomalar" bo'limida sozlashingiz mumkin.
                </p>
                <button
                  @click="unlinkTelegram"
                  :disabled="telegram.unlinking"
                  class="px-4 py-2 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100 disabled:opacity-50"
                >
                  {{ telegram.unlinking ? 'Uzilmoqda...' : 'Telegramni uzish' }}
                </button>
              </div>

              <!-- Not linked state -->
              <div v-else class="space-y-6">
                <div class="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <p class="font-medium text-blue-800 mb-2">Telegram botga ulanish</p>
                  <ol class="text-sm text-blue-700 space-y-1 list-decimal list-inside">
                    <li>"Kod olish" tugmasini bosing</li>
                    <li>Telegramda <b>@{{ telegram.botUsername || 'hisobnoma_bot' }}</b> botni oching</li>
                    <li>Olingan 6 raqamli kodni botga yuboring</li>
                  </ol>
                </div>

                <!-- Link code display -->
                <div v-if="telegram.linkCode" class="text-center space-y-3">
                  <p class="text-sm text-gray-600">Ushbu kodni Telegram botga yuboring:</p>
                  <div class="inline-block px-8 py-4 bg-gray-900 rounded-xl">
                    <span class="text-3xl font-mono font-bold text-white tracking-widest">{{ telegram.linkCode }}</span>
                  </div>
                  <p class="text-xs text-gray-400">Kod 10 daqiqa amal qiladi</p>
                  <div>
                    <a
                      :href="'https://t.me/' + (telegram.botUsername || 'hisobnoma_bot')"
                      target="_blank"
                      class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 rounded-lg"
                    >
                      Telegram botni ochish
                    </a>
                  </div>
                </div>

                <button
                  @click="generateTelegramCode"
                  :disabled="telegram.generatingCode"
                  class="btn-primary"
                >
                  {{ telegram.generatingCode ? 'Yaratilmoqda...' : (telegram.linkCode ? 'Yangi kod olish' : 'Kod olish') }}
                </button>
              </div>
            </template>
          </div>
        </div>

        <!-- Save Button for other tabs -->
        <div v-if="activeTab !== 'receipt' && activeTab !== 'telegram'" class="mt-6 flex justify-end">
          <button @click="saveSettings" :disabled="saving" class="btn-primary">
            {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
