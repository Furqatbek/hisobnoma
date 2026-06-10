<script setup>
import { ref, onMounted } from 'vue'
import { webCustomersApi, customersApi } from '@/services/api'
import {
  UserGroupIcon,
  MagnifyingGlassIcon,
  LinkIcon,
  XMarkIcon
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
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.lastLogin') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">{{ $t('webCustomers.linkedCustomer') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200 bg-white">
            <tr v-for="customer in customers" :key="customer.id" class="hover:bg-gray-50">
              <td class="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">
                +{{ customer.phone }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-700">{{ customer.name || '—' }}</td>
              <td class="px-4 py-3 text-center text-sm">{{ customer.orderCount }}</td>
              <td class="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                {{ formatDate(customer.lastLoginAt) }}
              </td>
              <td class="px-4 py-3 text-sm">
                <span v-if="customer.customerId" class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                  {{ customer.customerCode }} · {{ customer.customerName }}
                </span>
                <span v-else class="text-gray-400 text-xs">{{ $t('webCustomers.notLinked') }}</span>
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap">
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
  </div>
</template>
