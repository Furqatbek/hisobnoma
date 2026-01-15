<script setup>
import { ref, reactive } from 'vue'
import { Cog6ToothIcon, BuildingStorefrontIcon, CurrencyDollarIcon, BellIcon } from '@heroicons/vue/24/outline'

const activeTab = ref('general')

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

const saving = ref(false)

async function saveSettings() {
  saving.value = true
  try {
    // API call would go here
    await new Promise(resolve => setTimeout(resolve, 1000))
    alert('Settings saved successfully!')
  } catch (error) {
    alert('Failed to save settings')
  } finally {
    saving.value = false
  }
}

const tabs = [
  { key: 'general', name: 'General', icon: BuildingStorefrontIcon },
  { key: 'pos', name: 'Point of Sale', icon: CurrencyDollarIcon },
  { key: 'notifications', name: 'Notifications', icon: BellIcon }
]
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Settings</h1>
      <p class="mt-1 text-sm text-gray-500">Manage system configuration</p>
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
            <h3 class="text-lg font-medium">Company Information</h3>
          </div>
          <div class="card-body space-y-4">
            <div>
              <label class="label">Company Name</label>
              <input v-model="settings.company.name" type="text" class="input" />
            </div>
            <div>
              <label class="label">Address</label>
              <textarea v-model="settings.company.address" rows="2" class="input"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">Phone</label>
                <input v-model="settings.company.phone" type="tel" class="input" />
              </div>
              <div>
                <label class="label">Email</label>
                <input v-model="settings.company.email" type="email" class="input" />
              </div>
            </div>
            <div>
              <label class="label">Tax ID</label>
              <input v-model="settings.company.taxId" type="text" class="input" />
            </div>
          </div>
        </div>

        <!-- POS Settings -->
        <div v-show="activeTab === 'pos'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Point of Sale Settings</h3>
          </div>
          <div class="card-body space-y-4">
            <div>
              <label class="label">Default Tax Rate (%)</label>
              <input v-model.number="settings.pos.defaultTaxRate" type="number" min="0" max="100" step="0.1" class="input w-32" />
            </div>
            <div class="space-y-3">
              <label class="flex items-center">
                <input v-model="settings.pos.allowNegativeStock" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Allow selling items with negative stock</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.pos.requireCustomer" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Require customer for each sale</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.pos.printReceipt" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Auto-print receipt after sale</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Notification Settings -->
        <div v-show="activeTab === 'notifications'" class="card">
          <div class="card-header">
            <h3 class="text-lg font-medium">Notification Settings</h3>
          </div>
          <div class="card-body space-y-4">
            <div class="space-y-3">
              <label class="flex items-center">
                <input v-model="settings.notifications.lowStockAlert" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Enable low stock alerts</span>
              </label>
              <div v-if="settings.notifications.lowStockAlert" class="ml-6">
                <label class="label">Alert when stock falls below</label>
                <input v-model.number="settings.notifications.lowStockThreshold" type="number" min="1" class="input w-24" />
              </div>
            </div>
            <div class="space-y-3 pt-4 border-t">
              <label class="flex items-center">
                <input v-model="settings.notifications.emailNotifications" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">Email notifications</span>
              </label>
              <label class="flex items-center">
                <input v-model="settings.notifications.smsNotifications" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">SMS notifications</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Save Button -->
        <div class="mt-6 flex justify-end">
          <button @click="saveSettings" :disabled="saving" class="btn-primary">
            {{ saving ? 'Saving...' : 'Save Settings' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
