<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { Cog6ToothIcon, BuildingStorefrontIcon, CurrencyDollarIcon, BellIcon, PrinterIcon, PaperAirplaneIcon } from '@heroicons/vue/24/outline'
import { useReceiptStore } from '@/stores/receipt'
import { telegramApi } from '@/services/api'
import ReceiptTemplate from '@/components/ReceiptTemplate.vue'
import InvoiceA4Template from '@/components/InvoiceA4Template.vue'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

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
    toast.success(t('admin.settings.saved'))
  } catch (error) {
    toast.error(t('admin.settings.saveError'))
  } finally {
    saving.value = false
  }
}

function saveReceiptSettings() {
  receiptStore.updateConfig(receiptSettings)
  toast.success(t('admin.settings.receiptSettingsSaved'))
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
    telegram.error = e.response?.status === 404 ? '' : t('admin.telegram.statusLoadError')
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
    telegram.error = e.response?.data?.message || t('admin.telegram.codeGenerateError')
  } finally {
    telegram.generatingCode = false
  }
}

async function unlinkTelegram() {
  if (!confirm(t('admin.telegram.confirmUnlink'))) return
  telegram.unlinking = true
  try {
    await telegramApi.unlink()
    telegram.linked = false
    telegram.linkedAt = ''
    telegram.linkCode = ''
  } catch (e) {
    telegram.error = e.response?.data?.message || t('admin.telegram.unlinkError')
  } finally {
    telegram.unlinking = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'telegram') loadTelegramStatus()
})

const tabs = computed(() => [
  { key: 'general', name: t('admin.settings.tabGeneral'), icon: BuildingStorefrontIcon },
  { key: 'pos', name: t('admin.settings.tabPos'), icon: CurrencyDollarIcon },
  { key: 'receipt', name: t('admin.settings.tabReceipt'), icon: PrinterIcon },
  { key: 'notifications', name: t('admin.settings.tabNotifications'), icon: BellIcon },
  { key: 'telegram', name: t('admin.settings.tabTelegram'), icon: PaperAirplaneIcon }
])
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.settings.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ $t('admin.settings.subtitle') }}</p>
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
            <h3 class="text-lg font-medium">{{ $t('admin.settings.companyInfo') }}</h3>
          </div>
          <div class="card-body space-y-4">
            <div>
              <label class="label">{{ $t('admin.settings.companyName') }}</label>
              <input v-model="settings.company.name" type="text" class="input" />
            </div>
            <div>
              <label class="label">{{ $t('address') }}</label>
              <textarea v-model="settings.company.address" rows="2" class="input"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">{{ $t('phone') }}</label>
                <input v-model="settings.company.phone" type="tel" class="input" />
              </div>
              <div>
                <label class="label">{{ $t('email') }}</label>
                <input v-model="settings.company.email" type="email" class="input" />
              </div>
            </div>
            <div>
              <label class="label">{{ $t('admin.settings.taxId') }}</label>
              <input v-model="settings.company.taxId" type="text" class="input" />
            </div>
          </div>
        </div>

        <!-- POS Settings -->
        <div v-show="activeTab === 'pos'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">{{ $t('admin.settings.posSettings') }}</h3>
          </div>
          <div class="card-body space-y-4">
            <div>
              <label class="label">{{ $t('admin.settings.defaultTaxRate') }}</label>
              <input v-model.number="settings.pos.defaultTaxRate" type="number" min="0" max="100" step="0.1" class="input w-32" />
            </div>
            <div class="space-y-3">
              <label class="flex items-center">
                <input v-model="settings.pos.allowNegativeStock" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('admin.settings.allowNegativeStock') }}</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.pos.requireCustomer" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('admin.settings.requireCustomer') }}</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.pos.printReceipt" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('admin.settings.autoPrintReceipt') }}</span>
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
                <h3 class="text-lg font-medium">{{ $t('admin.settings.receiptInfo') }}</h3>
                <p class="text-sm text-gray-500 mt-1">{{ receiptSettings.paperWidth === 'A4' ? $t('admin.settings.a4Template') : $t('admin.settings.thermalTemplate') }}</p>
              </div>
              <div class="card-body space-y-4">
                <div>
                  <label class="label">{{ $t('admin.settings.brandName') }} <span class="text-red-500">*</span></label>
                  <input
                    v-model="receiptSettings.brandName"
                    type="text"
                    class="input"
                    :placeholder="$t('admin.settings.brandNamePlaceholder')"
                  />
                </div>
                <div>
                  <label class="label">{{ $t('address') }}</label>
                  <input
                    v-model="receiptSettings.address"
                    type="text"
                    class="input"
                    :placeholder="$t('admin.settings.addressPlaceholder')"
                  />
                </div>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="label">{{ $t('phone') }}</label>
                    <input
                      v-model="receiptSettings.phone"
                      type="tel"
                      class="input"
                      :placeholder="$t('admin.settings.phonePlaceholder')"
                    />
                  </div>
                  <div>
                    <label class="label">{{ $t('admin.settings.website') }}</label>
                    <input
                      v-model="receiptSettings.website"
                      type="text"
                      class="input"
                      :placeholder="$t('admin.settings.websitePlaceholder')"
                    />
                  </div>
                </div>
                <div>
                  <label class="label">{{ $t('admin.settings.taxIdShort') }}</label>
                  <input
                    v-model="receiptSettings.taxId"
                    type="text"
                    class="input"
                    :placeholder="$t('admin.settings.taxIdPlaceholder')"
                  />
                </div>
                <div>
                  <label class="label">{{ $t('admin.settings.footerText') }}</label>
                  <input
                    v-model="receiptSettings.footerText"
                    type="text"
                    class="input"
                    :placeholder="$t('admin.settings.receiptFooterPlaceholder')"
                  />
                </div>
                <div>
                  <label class="label">{{ $t('admin.settings.paperWidth') }}</label>
                  <select v-model="receiptSettings.paperWidth" class="input w-40">
                    <option value="58">58mm</option>
                    <option value="80">80mm</option>
                    <option value="A4">A4</option>
                  </select>
                  <p class="text-xs text-gray-500 mt-1">
                    {{ receiptSettings.paperWidth === 'A4' ? $t('admin.settings.a4Format') : $t('admin.settings.thermalPaperSize') }}
                  </p>
                </div>

                <div class="flex gap-3 pt-4 border-t">
                  <button @click="saveReceiptSettings" class="btn-primary">
                    {{ $t('save') }}
                  </button>
                  <button @click="printTestReceipt" class="btn-secondary">
                    {{ $t('admin.settings.printTestReceipt') }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Receipt Preview (80mm / 58mm) -->
            <div v-if="receiptSettings.paperWidth !== 'A4'" class="w-80">
              <div class="card">
                <div class="card-header">
                  <h3 class="text-lg font-medium text-center">{{ $t('admin.settings.preview') }} ({{ receiptSettings.paperWidth }}mm)</h3>
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
                  <h3 class="text-lg font-medium text-center">{{ $t('admin.settings.preview') }} (A4)</h3>
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
            <h3 class="text-lg font-medium">{{ $t('admin.settings.notificationSettings') }}</h3>
          </div>
          <div class="card-body space-y-4">
            <div class="space-y-3">
              <label class="flex items-center">
                <input v-model="settings.notifications.lowStockAlert" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('admin.settings.enableLowStockAlert') }}</span>
              </label>
              <div v-if="settings.notifications.lowStockAlert" class="ml-6">
                <label class="label">{{ $t('admin.settings.lowStockThreshold') }}</label>
                <input v-model.number="settings.notifications.lowStockThreshold" type="number" min="1" class="input w-24" />
              </div>
            </div>
            <div class="space-y-3 pt-4 border-t">
              <label class="flex items-center">
                <input v-model="settings.notifications.emailNotifications" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('admin.settings.emailNotifications') }}</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.notifications.smsNotifications" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('admin.settings.smsNotifications') }}</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Telegram Settings -->
        <div v-show="activeTab === 'telegram'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">{{ $t('admin.telegram.botTitle') }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ $t('admin.telegram.botSubtitle') }}</p>
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
                    <p class="font-medium text-green-800">{{ $t('admin.telegram.linked') }}</p>
                    <p v-if="telegram.linkedAt" class="text-sm text-green-600">
                      {{ $t('admin.telegram.linkedDate') }}: {{ new Date(telegram.linkedAt).toLocaleDateString('uz-UZ') }}
                    </p>
                  </div>
                </div>
                <p class="text-sm text-gray-600">
                  {{ $t('admin.telegram.linkedDescription') }}
                </p>
                <button
                  @click="unlinkTelegram"
                  :disabled="telegram.unlinking"
                  class="px-4 py-2 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100 disabled:opacity-50"
                >
                  {{ telegram.unlinking ? $t('admin.telegram.unlinking') : $t('admin.telegram.unlinkTelegram') }}
                </button>
              </div>

              <!-- Not linked state -->
              <div v-else class="space-y-6">
                <div class="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <p class="font-medium text-blue-800 mb-2">{{ $t('admin.telegram.connectToBot') }}</p>
                  <ol class="text-sm text-blue-700 space-y-1 list-decimal list-inside">
                    <li>{{ $t('admin.telegram.step1') }}</li>
                    <li>{{ $t('admin.telegram.step2') }} <b>@{{ telegram.botUsername || 'hisobnoma_bot' }}</b></li>
                    <li>{{ $t('admin.telegram.step3') }}</li>
                  </ol>
                </div>

                <!-- Link code display -->
                <div v-if="telegram.linkCode" class="text-center space-y-3">
                  <p class="text-sm text-gray-600">{{ $t('admin.telegram.sendCodeToBot') }}</p>
                  <div class="inline-block px-8 py-4 bg-gray-900 rounded-xl">
                    <span class="text-3xl font-mono font-bold text-white tracking-widest">{{ telegram.linkCode }}</span>
                  </div>
                  <p class="text-xs text-gray-400">{{ $t('admin.telegram.codeExpiry') }}</p>
                  <div>
                    <a
                      :href="'https://t.me/' + (telegram.botUsername || 'hisobnoma_bot')"
                      target="_blank"
                      class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-500 hover:bg-blue-600 rounded-lg"
                    >
                      {{ $t('admin.telegram.openBot') }}
                    </a>
                  </div>
                </div>

                <button
                  @click="generateTelegramCode"
                  :disabled="telegram.generatingCode"
                  class="btn-primary"
                >
                  {{ telegram.generatingCode ? $t('admin.telegram.generating') : (telegram.linkCode ? $t('admin.telegram.getNewCode') : $t('admin.telegram.getCode')) }}
                </button>
              </div>
            </template>
          </div>
        </div>

        <!-- Save Button for other tabs -->
        <div v-if="activeTab !== 'receipt' && activeTab !== 'telegram'" class="mt-6 flex justify-end">
          <button @click="saveSettings" :disabled="saving" class="btn-primary">
            {{ saving ? $t('saving') : $t('save') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
