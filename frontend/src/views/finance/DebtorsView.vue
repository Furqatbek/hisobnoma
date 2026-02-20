<script setup>
import { ref, computed, onMounted } from 'vue'
import { arReportsApi } from '@/services/api'
import { MagnifyingGlassIcon, ExclamationTriangleIcon, PhoneIcon, XMarkIcon, EyeIcon } from '@heroicons/vue/24/outline'

const loading = ref(true)
const search = ref('')
const typeFilter = ref('all')
const balanceReport = ref(null)
const agingReport = ref(null)

// Detail modal
const showDetailModal = ref(false)
const selectedCustomer = ref(null)
const selectedAging = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const [balanceRes, agingRes] = await Promise.all([
      arReportsApi.getCustomerBalanceReport(),
      arReportsApi.getAgingReport()
    ])
    balanceReport.value = balanceRes.data.data || balanceRes.data
    agingReport.value = agingRes.data.data || agingRes.data
  } catch (error) {
    console.error('Ma\'lumotlarni yuklashda xatolik:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const debtors = computed(() => {
  if (!balanceReport.value?.customerBalances) return []

  let list = balanceReport.value.customerBalances.filter(c => c.netBalance > 0)

  if (typeFilter.value !== 'all') {
    // Match against aging report's customer data or the balance data
    // Since customerType is not in the balance report, we filter by known categories
    if (typeFilter.value === 'credit_hold') {
      list = list.filter(c => c.onCreditHold)
    } else if (typeFilter.value === 'over_limit') {
      list = list.filter(c => c.overCreditLimit)
    }
  }

  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(c =>
      c.customerName?.toLowerCase().includes(q) ||
      c.customerCode?.toLowerCase().includes(q)
    )
  }

  return list.sort((a, b) => b.netBalance - a.netBalance)
})

const summary = computed(() => {
  const list = debtors.value
  return {
    count: list.length,
    totalDebt: list.reduce((sum, c) => sum + (c.netBalance || 0), 0),
    overLimit: list.filter(c => c.overCreditLimit).length,
    onHold: list.filter(c => c.onCreditHold).length
  }
})

function getAgingForCustomer(customerId) {
  if (!agingReport.value?.customerAgingList) return null
  return agingReport.value.customerAgingList.find(a => a.customerId === customerId)
}

function viewCustomer(customer) {
  selectedCustomer.value = customer
  selectedAging.value = getAgingForCustomer(customer.customerId)
  showDetailModal.value = true
}

function closeModal() {
  showDetailModal.value = false
  selectedCustomer.value = null
  selectedAging.value = null
}

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(dateString) {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString('uz-UZ')
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Qarzdorlar</h1>
      <p class="mt-1 text-sm text-gray-500">Nasiyaga olgan mijozlar ro'yxati</p>
    </div>

    <!-- Summary Cards -->
    <div v-if="!loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="card card-body">
        <p class="text-sm text-gray-500">Jami qarzdorlar</p>
        <p class="text-2xl font-bold text-gray-900">{{ summary.count }}</p>
      </div>
      <div class="card card-body">
        <p class="text-sm text-gray-500">Jami qarz summasi</p>
        <p class="text-2xl font-bold text-red-600">{{ formatCurrency(summary.totalDebt) }} so'm</p>
      </div>
      <div class="card card-body">
        <p class="text-sm text-gray-500">Kredit limitidan oshgan</p>
        <p class="text-2xl font-bold text-orange-600">{{ summary.overLimit }}</p>
      </div>
      <div class="card card-body">
        <p class="text-sm text-gray-500">Kredit to'xtatilgan</p>
        <p class="text-2xl font-bold text-red-600">{{ summary.onHold }}</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body flex flex-col sm:flex-row gap-4">
        <div class="flex-1 relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            type="text"
            placeholder="Qidiruv (nomi, kodi)..."
            class="input pl-10"
          />
        </div>
        <select v-model="typeFilter" class="input w-auto">
          <option value="all">Barchasi</option>
          <option value="credit_hold">Kredit to'xtatilgan</option>
          <option value="over_limit">Limitidan oshgan</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="debtors.length === 0" class="text-center py-12">
        <p class="text-gray-500">Qarzdorlar topilmadi</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>Mijoz</th>
              <th>Telefon / Kod</th>
              <th class="text-right">Qarz summasi</th>
              <th class="text-right">Kredit limiti</th>
              <th class="text-right">Mavjud kredit</th>
              <th>Holat</th>
              <th>Oxirgi faktura</th>
              <th class="text-right">Amallar</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="customer in debtors" :key="customer.customerId">
              <td>
                <div class="flex items-center">
                  <div class="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                    <span class="text-red-700 font-medium">{{ customer.customerName?.charAt(0) }}</span>
                  </div>
                  <div class="ml-3">
                    <p class="font-medium">{{ customer.customerName }}</p>
                  </div>
                </div>
              </td>
              <td class="text-sm text-gray-500">
                {{ customer.customerCode || '-' }}
              </td>
              <td class="text-right">
                <span class="font-semibold text-red-600">{{ formatCurrency(customer.netBalance) }} so'm</span>
              </td>
              <td class="text-right text-sm">
                {{ customer.creditLimit ? formatCurrency(customer.creditLimit) + " so'm" : '-' }}
              </td>
              <td class="text-right text-sm">
                <span :class="(customer.availableCreditLimit || 0) < 0 ? 'text-red-600' : 'text-green-600'">
                  {{ customer.creditLimit ? formatCurrency(customer.availableCreditLimit) + " so'm" : '-' }}
                </span>
              </td>
              <td>
                <div class="flex flex-col gap-1">
                  <span v-if="customer.overCreditLimit" class="badge badge-danger text-xs">Limitidan oshgan</span>
                  <span v-if="customer.onCreditHold" class="badge badge-warning text-xs">To'xtatilgan</span>
                  <span v-if="!customer.overCreditLimit && !customer.onCreditHold" class="badge badge-info text-xs">Faol</span>
                </div>
              </td>
              <td class="text-sm text-gray-500">
                {{ formatDate(customer.lastInvoiceDate) }}
              </td>
              <td class="text-right">
                <button
                  @click="viewCustomer(customer)"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  title="Batafsil"
                >
                  <EyeIcon class="h-5 w-5" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Detail Modal -->
    <div
      v-if="showDetailModal && selectedCustomer"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closeModal"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <!-- Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 class="text-xl font-bold text-gray-900">{{ selectedCustomer.customerName }}</h2>
            <p class="text-sm text-gray-500">{{ selectedCustomer.customerCode }}</p>
          </div>
          <button @click="closeModal" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
            <XMarkIcon class="h-6 w-6" />
          </button>
        </div>

        <div class="p-6 space-y-6">
          <!-- Balance Summary -->
          <div class="grid grid-cols-2 gap-4">
            <div class="bg-red-50 rounded-lg p-4">
              <p class="text-sm text-red-600">Jami qarz</p>
              <p class="text-xl font-bold text-red-700">{{ formatCurrency(selectedCustomer.netBalance) }} so'm</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-sm text-gray-600">Kredit limiti</p>
              <p class="text-xl font-bold text-gray-900">{{ selectedCustomer.creditLimit ? formatCurrency(selectedCustomer.creditLimit) + " so'm" : "Belgilanmagan" }}</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-sm text-gray-600">Ochiq fakturalar</p>
              <p class="text-xl font-bold text-gray-900">{{ formatCurrency(selectedCustomer.outstandingInvoices) }} so'm</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-sm text-gray-600">Mavjud kreditlar</p>
              <p class="text-xl font-bold text-gray-900">{{ formatCurrency(selectedCustomer.availableCredits) }} so'm</p>
            </div>
          </div>

          <!-- Status Flags -->
          <div v-if="selectedCustomer.overCreditLimit || selectedCustomer.onCreditHold" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div class="flex items-center gap-2 mb-1">
              <ExclamationTriangleIcon class="h-5 w-5 text-yellow-600" />
              <span class="font-medium text-yellow-800">Ogohlantirish</span>
            </div>
            <p v-if="selectedCustomer.overCreditLimit" class="text-sm text-yellow-700">Kredit limitidan oshgan</p>
            <p v-if="selectedCustomer.onCreditHold" class="text-sm text-yellow-700">Kredit to'xtatilgan</p>
          </div>

          <!-- Info -->
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span class="text-gray-500">Oxirgi faktura:</span>
              <span class="ml-2 font-medium">{{ formatDate(selectedCustomer.lastInvoiceDate) }}</span>
            </div>
            <div>
              <span class="text-gray-500">Oxirgi to'lov:</span>
              <span class="ml-2 font-medium">{{ formatDate(selectedCustomer.lastPaymentDate) }}</span>
            </div>
            <div>
              <span class="text-gray-500">To'lov muddati:</span>
              <span class="ml-2 font-medium">{{ selectedCustomer.paymentTerms ? selectedCustomer.paymentTerms + ' kun' : '-' }}</span>
            </div>
          </div>

          <!-- Aging Breakdown -->
          <div v-if="selectedAging">
            <h3 class="font-semibold text-gray-900 mb-3">Qarz muddati bo'yicha taqsimot</h3>
            <div class="bg-gray-50 rounded-lg overflow-hidden">
              <table class="w-full text-sm">
                <thead class="bg-gray-100">
                  <tr>
                    <th class="px-4 py-2 text-left text-gray-600">Davr</th>
                    <th class="px-4 py-2 text-right text-gray-600">Summa</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr>
                    <td class="px-4 py-2">Joriy (muddatida)</td>
                    <td class="px-4 py-2 text-right font-medium">{{ formatCurrency(selectedAging.currentAmount) }} so'm</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">1–30 kun kechikkan</td>
                    <td class="px-4 py-2 text-right font-medium" :class="selectedAging.days1To30 > 0 ? 'text-yellow-600' : ''">{{ formatCurrency(selectedAging.days1To30) }} so'm</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">31–60 kun kechikkan</td>
                    <td class="px-4 py-2 text-right font-medium" :class="selectedAging.days31To60 > 0 ? 'text-orange-600' : ''">{{ formatCurrency(selectedAging.days31To60) }} so'm</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">61–90 kun kechikkan</td>
                    <td class="px-4 py-2 text-right font-medium" :class="selectedAging.days61To90 > 0 ? 'text-red-600' : ''">{{ formatCurrency(selectedAging.days61To90) }} so'm</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">90+ kun kechikkan</td>
                    <td class="px-4 py-2 text-right font-bold" :class="selectedAging.over90Days > 0 ? 'text-red-700' : ''">{{ formatCurrency(selectedAging.over90Days) }} so'm</td>
                  </tr>
                  <tr class="bg-gray-100">
                    <td class="px-4 py-2 font-semibold">Jami</td>
                    <td class="px-4 py-2 text-right font-bold">{{ formatCurrency(selectedAging.totalBalance) }} so'm</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
