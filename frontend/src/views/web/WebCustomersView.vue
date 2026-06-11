<script setup>
import { ref, onMounted } from 'vue'
import { webCustomersApi, customersApi } from '@/services/api'
import {
  UserGroupIcon,
  MagnifyingGlassIcon,
  LinkIcon,
  XMarkIcon,
  GiftIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const customers = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(0)
const totalPages = ref(0)

// Link modal state
const linkTarget = ref(null)
const customerSearch = ref('')
const customerResults = ref([])
const searching = ref(false)
let searchTimeout = null

// Loyalty modal state
const loyaltyTarget = ref(null)
const loyaltyData = ref(null)
const loyaltyLoading = ref(false)
const adjustAmount = ref('')
const adjustReason = ref('')
const adjustBusy = ref(false)

async function fetchCustomers(page = 0) {
  loading.value = true
  try {
    const params = { page, size: 20 }
    if (searchQuery.value.trim()) params.search = searchQuery.value.trim()
    const response = await webCustomersApi.getAll(params)
    const data = response.data
    customers.value = data.content || []
    currentPage.value = data.page?.number || 0
    totalPages.value = data.page?.totalPages || 0
  } catch (error) {
    console.error('Failed to fetch web customers:', error)
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('uz-UZ', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

function formatPrice(value) {
  if (value == null) return '0'
  return new Intl.NumberFormat('uz-UZ').format(value)
}

function openLinkModal(webCustomer) {
  linkTarget.value = webCustomer
  customerSearch.value = webCustomer.name || ''
  customerResults.value = []
  if (customerSearch.value) searchCustomers()
}

function handleCustomerSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(searchCustomers, 300)
}

async function searchCustomers() {
  const query = customerSearch.value.trim()
  if (!query) {
    customerResults.value = []
    return
  }
  searching.value = true
  try {
    const response = await customersApi.search(query)
    const data = response.data
    customerResults.value = data.content || data.data || []
  } catch (error) {
    console.error('Customer search failed:', error)
    customerResults.value = []
  } finally {
    searching.value = false
  }
}

async function linkTo(customer) {
  try {
    await webCustomersApi.linkCustomer(linkTarget.value.id, customer.id)
    linkTarget.value = null
    fetchCustomers(currentPage.value)
  } catch (error) {
    alert(error.response?.data?.message || t('webCustomers.actionError'))
  }
}

async function unlink(webCustomer) {
  if (!confirm(t('webCustomers.confirmUnlink'))) return
  try {
    await webCustomersApi.unlinkCustomer(webCustomer.id)
    fetchCustomers(currentPage.value)
  } catch (error) {
    alert(error.response?.data?.message || t('webCustomers.actionError'))
  }
}

async function toggleSmsOptOut(webCustomer) {
  try {
    await webCustomersApi.setSmsOptOut(webCustomer.id, !webCustomer.smsOptOut)
    webCustomer.smsOptOut = !webCustomer.smsOptOut
  } catch (error) {
    alert(error.response?.data?.message || t('webCustomers.actionError'))
  }
}

async function openLoyaltyModal(webCustomer) {
  loyaltyTarget.value = webCustomer
  loyaltyData.value = null
  loyaltyLoading.value = true
  adjustAmount.value = ''
  adjustReason.value = ''
  try {
    const response = await webCustomersApi.getLoyalty(webCustomer.id)
    loyaltyData.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch loyalty:', error)
  } finally {
    loyaltyLoading.value = false
  }
}

async function submitAdjust() {
  if (!adjustAmount.value || !adjustReason.value.trim()) return
  adjustBusy.value = true
  try {
    const response = await webCustomersApi.adjustLoyalty(
      loyaltyTarget.value.id,
      parseFloat(adjustAmount.value),
      adjustReason.value.trim()
    )
    loyaltyData.value = response.data.data
    adjustAmount.value = ''
    adjustReason.value = ''
  } catch (error) {
    alert(error.response?.data?.message || t('webCustomers.actionError'))
  } finally {
    adjustBusy.value = false
  }
}

const typeLabels = {
  EARN: { label: 'webCustomers.loyaltyEarn', cls: 'text-green-700' },
  SPEND: { label: 'webCustomers.loyaltySpend', cls: 'text-red-700' },
  EXPIRE: { label: 'webCustomers.loyaltyExpire', cls: 'text-gray-500' },
  ADJUST: { label: 'webCustomers.loyaltyAdjust', cls: 'text-blue-700' }
}

onMounted(() => fetchCustomers())
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">
        <UserGroupIcon class="h-7 w-7 text-teal-600" />
        {{ $t('webCustomers.title') }}
      </h1>
      <p class="mt-1 text-sm text-gray-500">{{ $t('webCustomers.subtitle') }}</p>
    </div>

    <!-- Search -->
    <div class="relative max-w-md">
      <MagnifyingGlassIcon class="h-5 w-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
      <input
        v-model="searchQuery"
        @input="fetchCustomers(0)"
        type="text"
        :placeholder="$t('webCustomers.searchPlaceholder')"
        class="input pl-10"
      />
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="p-8 text-center text-gray-500">{{ $t('loading') }}</div>
      <div v-else-if="customers.length === 0" class="p-8 text-center text-gray-500">
        {{ $t('webCustomers.empty') }}
      </div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.phone') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.name') }}</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.orders') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.lastOrder') }}</th>
              <th class="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.sms') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.linkedCustomer') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200 bg-white">
            <tr v-for="customer in customers" :key="customer.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">
                +{{ customer.phone }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-700">
                {{ customer.name || '—' }}
                <span v-if="customer.pushReachable" class="ml-1 inline-flex px-1.5 py-0.5 rounded text-[10px] font-medium bg-blue-100 text-blue-700" :title="$t('webCustomers.pushReachable')">Push</span>
                <div v-if="customer.referralCode" class="text-xs text-gray-400 font-mono mt-0.5">{{ customer.referralCode }}
                  <span v-if="customer.referredCount > 0" class="text-purple-600">({{ customer.referredCount }})</span>
                </div>
                <div v-if="customer.referredByName" class="text-xs text-gray-400 mt-0.5">{{ $t('webCustomers.referredBy') }}: {{ customer.referredByName }}</div>
              </td>
              <td class="px-4 py-3 text-center text-sm">{{ customer.orderCount }}</td>
              <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                {{ formatDate(customer.lastOrderAt) }}
              </td>
              <td class="px-4 py-3 text-center">
                <button
                  @click="toggleSmsOptOut(customer)"
                  :class="customer.smsOptOut ? 'bg-red-100 text-red-700 hover:bg-red-200' : 'bg-green-100 text-green-700 hover:bg-green-200'"
                  class="inline-flex px-2.5 py-1 rounded-full text-xs font-medium transition-colors"
                  :title="customer.smsOptOut ? $t('webCustomers.smsOptedOut') : $t('webCustomers.smsAllowed')"
                >
                  {{ customer.smsOptOut ? $t('webCustomers.smsOff') : $t('webCustomers.smsOn') }}
                </button>
              </td>
              <td class="px-4 py-3 text-sm">
                <span v-if="customer.customerId" class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                  {{ customer.customerCode }} · {{ customer.customerName }}
                </span>
                <span v-else class="text-gray-400 text-xs">{{ $t('webCustomers.notLinked') }}</span>
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap space-x-1">
                <button
                  class="btn-secondary text-sm py-1 px-3"
                  @click="openLoyaltyModal(customer)"
                  :title="$t('webCustomers.loyalty')"
                >
                  <GiftIcon class="h-4 w-4 inline" />
                </button>
                <button
                  v-if="!customer.customerId"
                  class="btn-secondary text-sm py-1 px-3"
                  @click="openLinkModal(customer)"
                >
                  <LinkIcon class="h-4 w-4 mr-1 inline" />
                  {{ $t('webCustomers.link') }}
                </button>
                <button
                  v-else
                  class="btn-secondary text-sm py-1 px-3 text-red-600"
                  @click="unlink(customer)"
                >
                  <XMarkIcon class="h-4 w-4 mr-1 inline" />
                  {{ $t('webCustomers.unlink') }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-between px-4 py-3 border-t border-gray-200">
        <button class="btn-secondary text-sm py-1 px-3" :disabled="currentPage === 0" @click="fetchCustomers(currentPage - 1)">
          {{ $t('previous') }}
        </button>
        <span class="text-sm text-gray-500">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="btn-secondary text-sm py-1 px-3" :disabled="currentPage >= totalPages - 1" @click="fetchCustomers(currentPage + 1)">
          {{ $t('next') }}
        </button>
      </div>
    </div>

    <!-- Link modal -->
    <div v-if="linkTarget" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="linkTarget = null"></div>
      <div class="relative bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[70vh] flex flex-col">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900">{{ $t('webCustomers.selectCustomer') }}</h3>
          <p class="text-sm text-gray-500 mt-1">+{{ linkTarget.phone }} · {{ linkTarget.name || '' }}</p>
        </div>
        <div class="px-6 py-4 flex-1 overflow-y-auto space-y-4">
          <div class="relative">
            <MagnifyingGlassIcon class="h-5 w-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              v-model="customerSearch"
              @input="handleCustomerSearch"
              type="text"
              :placeholder="$t('webCustomers.customerSearchPlaceholder')"
              class="input pl-10"
              autofocus
            />
          </div>
          <div v-if="searching" class="text-center text-sm text-gray-500 py-4">{{ $t('searching') }}</div>
          <div v-else-if="customerResults.length === 0 && customerSearch.trim()" class="text-center text-sm text-gray-500 py-4">
            {{ $t('webCustomers.noResults') }}
          </div>
          <ul v-else class="divide-y divide-gray-100">
            <li
              v-for="customer in customerResults"
              :key="customer.id"
              class="py-2 px-2 cursor-pointer hover:bg-gray-50 rounded flex items-center justify-between"
              @click="linkTo(customer)"
            >
              <div>
                <div class="text-sm font-medium text-gray-900">{{ customer.name }}</div>
                <div class="text-xs text-gray-500">{{ customer.code }} <span v-if="customer.phone">· {{ customer.phone }}</span></div>
              </div>
              <LinkIcon class="h-4 w-4 text-gray-400" />
            </li>
          </ul>
        </div>
        <div class="px-6 py-4 border-t border-gray-200 flex justify-end">
          <button class="btn-secondary" @click="linkTarget = null">{{ $t('cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- Loyalty modal -->
    <div v-if="loyaltyTarget" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="loyaltyTarget = null"></div>
      <div class="relative bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[80vh] flex flex-col">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
            <GiftIcon class="h-5 w-5 text-purple-600" />
            {{ $t('webCustomers.loyalty') }}
          </h3>
          <p class="text-sm text-gray-500 mt-1">+{{ loyaltyTarget.phone }} · {{ loyaltyTarget.name || '' }}</p>
        </div>

        <div class="px-6 py-4 flex-1 overflow-y-auto space-y-4">
          <div v-if="loyaltyLoading" class="text-center text-sm text-gray-500 py-4">{{ $t('loading') }}</div>
          <template v-else-if="loyaltyData">
            <!-- Balance -->
            <div class="bg-purple-50 rounded-lg p-4 text-center">
              <div class="text-sm text-purple-600">{{ $t('webCustomers.loyaltyBalance') }}</div>
              <div class="text-3xl font-bold text-purple-800 mt-1">{{ formatPrice(loyaltyData.balance) }}</div>
              <div v-if="!loyaltyData.enabled" class="text-xs text-gray-500 mt-1">{{ $t('webCustomers.loyaltyDisabled') }}</div>
            </div>

            <!-- Manual adjust -->
            <div class="bg-gray-50 rounded-lg p-4 space-y-3">
              <h4 class="text-sm font-medium text-gray-700">{{ $t('webCustomers.loyaltyAdjust') }}</h4>
              <div class="flex gap-2">
                <input
                  v-model="adjustAmount"
                  type="number"
                  :placeholder="$t('webCustomers.loyaltyAmount')"
                  class="input flex-1"
                />
                <input
                  v-model="adjustReason"
                  type="text"
                  :placeholder="$t('webCustomers.loyaltyReason')"
                  class="input flex-[2]"
                />
                <button
                  class="btn-primary text-sm px-4"
                  :disabled="adjustBusy || !adjustAmount || !adjustReason.trim()"
                  @click="submitAdjust"
                >
                  {{ $t('save') }}
                </button>
              </div>
            </div>

            <!-- Ledger -->
            <div>
              <h4 class="text-sm font-medium text-gray-700 mb-2">{{ $t('webCustomers.loyaltyLedger') }}</h4>
              <div v-if="loyaltyData.entries.length === 0" class="text-center text-sm text-gray-400 py-4">
                {{ $t('webCustomers.loyaltyEmpty') }}
              </div>
              <ul v-else class="divide-y divide-gray-100 text-sm">
                <li v-for="entry in loyaltyData.entries" :key="entry.id" class="py-2 flex items-center justify-between">
                  <div>
                    <span :class="typeLabels[entry.type]?.cls || 'text-gray-700'" class="font-medium">
                      {{ $t(typeLabels[entry.type]?.label || entry.type) }}
                    </span>
                    <span v-if="entry.orderNumber" class="text-xs text-gray-500 ml-1">({{ entry.orderNumber }})</span>
                    <div v-if="entry.note" class="text-xs text-gray-500">{{ entry.note }}</div>
                  </div>
                  <div :class="entry.amount >= 0 ? 'text-green-700' : 'text-red-700'" class="font-medium whitespace-nowrap">
                    {{ entry.amount >= 0 ? '+' : '' }}{{ formatPrice(entry.amount) }}
                  </div>
                </li>
              </ul>
            </div>
          </template>
        </div>

        <div class="px-6 py-4 border-t border-gray-200 flex justify-end">
          <button class="btn-secondary" @click="loyaltyTarget = null">{{ $t('close') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
