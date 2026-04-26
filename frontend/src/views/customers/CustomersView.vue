<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { customersApi } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  PhoneIcon, EnvelopeIcon, ExclamationTriangleIcon, NoSymbolIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const customers = ref([])
const loading = ref(true)
const search = ref('')
const activeFilter = ref('all')
const pagination = ref({ page: 0, size: 20, totalPages: 0, totalElements: 0 })

const creditHoldCustomers = ref([])
const overCreditCustomers = ref([])
const creditHoldCount = ref(0)
const overCreditCount = ref(0)

const filters = computed(() => [
  { key: 'all', label: t('customers.filterAll') },
  { key: 'active', label: t('active') },
  { key: 'creditHold', label: t('customers.creditHold'), count: creditHoldCount.value },
  { key: 'overCredit', label: t('customers.overCreditLimit'), count: overCreditCount.value }
])

async function fetchCustomers() {
  loading.value = true
  try {
    if (activeFilter.value === 'creditHold') {
      const res = await customersApi.getCreditHold()
      customers.value = res.data || []
      pagination.value.totalElements = customers.value.length
      pagination.value.totalPages = 1
    } else if (activeFilter.value === 'overCredit') {
      const res = await customersApi.getOverCreditLimit()
      customers.value = res.data || []
      pagination.value.totalElements = customers.value.length
      pagination.value.totalPages = 1
    } else if (activeFilter.value === 'active') {
      const res = await customersApi.getActive()
      customers.value = res.data || []
      pagination.value.totalElements = customers.value.length
      pagination.value.totalPages = 1
    } else {
      const response = await customersApi.getAll({
        page: pagination.value.page,
        size: pagination.value.size,
        search: search.value || undefined
      })
      customers.value = response.data.content || []
      pagination.value.totalPages = response.data.page?.totalPages || 0
      pagination.value.totalElements = response.data.page?.totalElements || 0
    }
  } catch (error) {
    console.error('Failed to fetch customers:', error)
  } finally {
    loading.value = false
  }
}

async function loadCreditCounts() {
  try {
    const [holdRes, overRes] = await Promise.all([
      customersApi.getCreditHold(),
      customersApi.getOverCreditLimit()
    ])
    creditHoldCount.value = (holdRes.data || []).length
    overCreditCount.value = (overRes.data || []).length
  } catch (error) {
    console.error('Failed to load credit counts:', error)
  }
}

onMounted(() => {
  fetchCustomers()
  loadCreditCounts()
})

function switchFilter(key) {
  activeFilter.value = key
  pagination.value.page = 0
  fetchCustomers()
}

async function toggleActive(customer) {
  try {
    await customersApi.activate(customer.id)
    fetchCustomers()
    loadCreditCounts()
  } catch (error) {
    console.error('Failed to toggle customer status:', error)
  }
}

async function deleteCustomer(customer) {
  if (!confirm(t('customers.confirmDelete'))) return
  try {
    await customersApi.delete(customer.id)
    fetchCustomers()
    loadCreditCounts()
  } catch (error) {
    console.error('Failed to delete customer:', error)
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('customers.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('customers.subtitle') }}</p>
      </div>
      <RouterLink to="/customers/new" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('customers.addCustomer') }}
      </RouterLink>
    </div>

    <!-- Filter Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex space-x-4 overflow-x-auto">
        <button
          v-for="f in filters"
          :key="f.key"
          @click="switchFilter(f.key)"
          :class="[
            'px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors',
            activeFilter === f.key
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ f.label }}
          <span
            v-if="f.count"
            class="ml-1.5 inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800"
          >
            {{ f.count }}
          </span>
        </button>
      </nav>
    </div>

    <div class="card" v-if="activeFilter === 'all'">
      <div class="card-body">
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @input="fetchCustomers"
            type="text"
            :placeholder="$t('customers.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="customers.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('customers.noCustomers') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('customers.customer') }}</th>
              <th>{{ $t('pos.transactions.customer') }}</th>
              <th>{{ $t('customers.form.basicInfo') }}</th>
              <th class="text-right">{{ $t('balance') }}</th>
              <th>{{ $t('customers.creditInfo') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="customer in customers" :key="customer.id">
              <td>
                <div class="flex items-center">
                  <div class="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                    <span class="text-primary-700 font-medium">{{ customer.name?.charAt(0) }}</span>
                  </div>
                  <div class="ml-3">
                    <p class="font-medium">{{ customer.name }}</p>
                    <p class="text-sm text-gray-500">{{ customer.code || `#${customer.id}` }}</p>
                  </div>
                </div>
              </td>
              <td>
                <div class="flex flex-col text-sm">
                  <span v-if="customer.phone" class="flex items-center text-gray-500">
                    <PhoneIcon class="h-4 w-4 mr-1" />
                    {{ customer.phone }}
                  </span>
                  <span v-if="customer.email" class="flex items-center text-gray-500">
                    <EnvelopeIcon class="h-4 w-4 mr-1" />
                    {{ customer.email }}
                  </span>
                </div>
              </td>
              <td>
                <span class="badge badge-info">{{ customer.customerType || 'RETAIL' }}</span>
              </td>
              <td class="text-right">
                <span :class="(customer.balance || 0) > 0 ? 'text-red-600' : 'text-green-600'">
                  {{ formatCurrency(customer.balance) }}
                </span>
              </td>
              <td>
                <div class="flex flex-col gap-1">
                  <span v-if="customer.creditHold" class="inline-flex items-center text-xs text-red-600">
                    <NoSymbolIcon class="h-3.5 w-3.5 mr-1" />
                    {{ $t('customers.creditHold') }}
                  </span>
                  <span v-if="customer.creditLimit" class="text-xs text-gray-500">
                    {{ $t('customers.form.creditLimit') }}: {{ formatCurrency(customer.creditLimit) }}
                  </span>
                  <span v-if="customer.balance > customer.creditLimit && customer.creditLimit > 0" class="inline-flex items-center text-xs text-orange-600">
                    <ExclamationTriangleIcon class="h-3.5 w-3.5 mr-1" />
                    {{ $t('customers.overCreditLimit') }}
                  </span>
                </div>
              </td>
              <td>
                <button
                  @click="toggleActive(customer)"
                  :class="['badge cursor-pointer', customer.active !== false ? 'badge-success' : 'badge-danger']"
                >
                  {{ customer.active !== false ? $t('active') : $t('inactive') }}
                </button>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <RouterLink :to="`/customers/${customer.id}/edit`" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </RouterLink>
                  <button @click="deleteCustomer(customer)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1 && activeFilter === 'all'" class="px-6 py-4 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <p class="text-sm text-gray-500">
            {{ pagination.totalElements }} {{ $t('customers.totalRecords') }}
          </p>
          <div class="flex space-x-2">
            <button
              @click="pagination.page--; fetchCustomers()"
              :disabled="pagination.page === 0"
              class="btn-secondary px-3 py-1"
            >
              {{ $t('previous') }}
            </button>
            <button
              @click="pagination.page++; fetchCustomers()"
              :disabled="pagination.page >= pagination.totalPages - 1"
              class="btn-secondary px-3 py-1"
            >
              {{ $t('next') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
