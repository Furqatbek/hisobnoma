<script setup>
import { ref, computed, onMounted } from 'vue'
import { arReportsApi, arInvoicesApi } from '@/services/api'
import { useReceiptStore } from '@/stores/receipt'
import { MagnifyingGlassIcon, ExclamationTriangleIcon, PrinterIcon, XMarkIcon, EyeIcon } from '@heroicons/vue/24/outline'

const receiptStore = useReceiptStore()
const brandConfig = computed(() => receiptStore.config)

const loading = ref(true)
const search = ref('')
const typeFilter = ref('all')
const balanceReport = ref(null)
const agingReport = ref(null)

// Detail modal
const showDetailModal = ref(false)
const selectedCustomer = ref(null)
const selectedAging = ref(null)
const unpaidInvoices = ref([])
const loadingInvoices = ref(false)
const expandedInvoice = ref(null)

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

async function viewCustomer(customer) {
  selectedCustomer.value = customer
  selectedAging.value = getAgingForCustomer(customer.customerId)
  unpaidInvoices.value = []
  expandedInvoice.value = null
  showDetailModal.value = true

  loadingInvoices.value = true
  try {
    const res = await arInvoicesApi.getUnpaidByCustomer(customer.customerId)
    unpaidInvoices.value = res.data.data || res.data || []
  } catch (error) {
    console.error('Fakturalarni yuklashda xatolik:', error)
  } finally {
    loadingInvoices.value = false
  }
}

function toggleInvoice(invoiceId) {
  expandedInvoice.value = expandedInvoice.value === invoiceId ? null : invoiceId
}

function closeModal() {
  showDetailModal.value = false
  selectedCustomer.value = null
  selectedAging.value = null
  unpaidInvoices.value = []
  expandedInvoice.value = null
}

function getStatusLabel(status) {
  const labels = {
    'PENDING': 'Kutilmoqda',
    'SENT': 'Yuborilgan',
    'PARTIAL': 'Qisman',
    'OVERDUE': 'Muddati o\'tgan',
    'DRAFT': 'Qoralama',
    'DISPUTED': 'Bahsli'
  }
  return labels[status] || status
}

function getStatusClass(status) {
  switch (status) {
    case 'OVERDUE': return 'badge-danger'
    case 'PARTIAL': return 'badge-warning'
    case 'SENT': case 'PENDING': return 'badge-info'
    default: return 'badge-info'
  }
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

function printDebtors() {
  const list = debtors.value
  if (!list.length) return

  const rows = list.map((c, i) => {
    const aging = getAgingForCustomer(c.customerId)
    const status = c.onCreditHold ? 'To\'xtatilgan' : c.overCreditLimit ? 'Limitidan oshgan' : 'Faol'
    return `
      <tr>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center;">${i + 1}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db;">
          <strong>${c.customerName || ''}</strong>
          ${c.customerCode ? '<br><span style="color: #6b7280; font-size: 11px;">' + c.customerCode + '</span>' : ''}
        </td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right; font-weight: 600; color: #dc2626;">${formatCurrency(c.netBalance)}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right;">${c.creditLimit ? formatCurrency(c.creditLimit) : '-'}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right;">${aging ? formatCurrency(aging.currentAmount) : '-'}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right; color: ${aging && aging.days1To30 > 0 ? '#ca8a04' : '#111827'};">${aging ? formatCurrency(aging.days1To30) : '-'}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right; color: ${aging && aging.days31To60 > 0 ? '#ea580c' : '#111827'};">${aging ? formatCurrency(aging.days31To60) : '-'}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right; color: ${aging && aging.over90Days > 0 || aging && aging.days61To90 > 0 ? '#dc2626' : '#111827'};">${aging ? formatCurrency((aging.days61To90 || 0) + (aging.over90Days || 0)) : '-'}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center; font-size: 11px;">${status}</td>
        <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center; font-size: 11px;">${formatDate(c.lastInvoiceDate)}</td>
      </tr>`
  }).join('')

  const agingTotals = agingReport.value || {}
  const today = new Date().toLocaleDateString('uz-UZ')

  const printWindow = window.open('', '_blank', 'width=900,height=700')
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>Qarzdorlar ro'yxati</title>
      <style>
        @page { size: A4 landscape; margin: 12mm 15mm; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Arial', sans-serif; font-size: 12px; color: #111827; line-height: 1.4; }
        @media print { body { width: 100%; } }
        table { width: 100%; border-collapse: collapse; }
        th { padding: 8px 10px; background: #111827; color: white; font-weight: 600; font-size: 11px; text-align: left; }
      </style>
    </head>
    <body>
      <!-- Header -->
      <div style="display: flex; justify-content: space-between; align-items: flex-start; padding-bottom: 16px; border-bottom: 3px solid #111827; margin-bottom: 16px;">
        <div>
          <div style="font-size: 20px; font-weight: bold;">${brandConfig.value.brandName || ''}</div>
          ${brandConfig.value.address ? '<div style="color: #6b7280; font-size: 12px;">' + brandConfig.value.address + '</div>' : ''}
          ${brandConfig.value.phone ? '<div style="color: #6b7280; font-size: 12px;">Tel: ' + brandConfig.value.phone + '</div>' : ''}
        </div>
        <div style="text-align: right;">
          <div style="font-size: 22px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px;">Qarzdorlar ro'yxati</div>
          <div style="color: #6b7280; margin-top: 4px;">Sana: ${today}</div>
        </div>
      </div>

      <!-- Summary -->
      <div style="display: flex; gap: 24px; margin-bottom: 16px; font-size: 13px;">
        <div><strong>Jami qarzdorlar:</strong> ${summary.value.count} ta</div>
        <div><strong>Jami qarz:</strong> <span style="color: #dc2626;">${formatCurrency(summary.value.totalDebt)} so'm</span></div>
        <div><strong>Limitidan oshgan:</strong> ${summary.value.overLimit}</div>
        <div><strong>Kredit to'xtatilgan:</strong> ${summary.value.onHold}</div>
      </div>

      <!-- Table -->
      <table>
        <thead>
          <tr>
            <th style="text-align: center; width: 35px;">№</th>
            <th>Mijoz</th>
            <th style="text-align: right;">Qarz summasi</th>
            <th style="text-align: right;">Kredit limiti</th>
            <th style="text-align: right;">Joriy</th>
            <th style="text-align: right;">1-30 kun</th>
            <th style="text-align: right;">31-60 kun</th>
            <th style="text-align: right;">60+ kun</th>
            <th style="text-align: center;">Holat</th>
            <th style="text-align: center;">Oxirgi faktura</th>
          </tr>
        </thead>
        <tbody>
          ${rows}
        </tbody>
        <tfoot>
          <tr style="background: #f3f4f6; font-weight: bold;">
            <td colspan="2" style="padding: 8px 10px; border: 1px solid #d1d5db;">JAMI</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right; color: #dc2626;">${formatCurrency(summary.value.totalDebt)} so'm</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db;"></td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right;">${formatCurrency(agingTotals.totalCurrent)}</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right;">${formatCurrency(agingTotals.total1To30Days)}</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right;">${formatCurrency(agingTotals.total31To60Days)}</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right;">${formatCurrency(((agingTotals.total61To90Days || 0) + (agingTotals.totalOver90Days || 0)))}</td>
            <td colspan="2" style="padding: 8px 10px; border: 1px solid #d1d5db;"></td>
          </tr>
        </tfoot>
      </table>

      <!-- Footer -->
      <div style="margin-top: 40px; display: flex; justify-content: space-between;">
        <div style="width: 40%;">
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">Tuzuvchi:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
        <div style="width: 40%;">
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">Tasdiqladi:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
      </div>
      <div style="margin-top: 24px; text-align: center; font-size: 10px; color: #9ca3af;">
        Chop etilgan: ${new Date().toLocaleString('uz-UZ')}
      </div>
    </body>
    </html>
  `)

  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 250)
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Qarzdorlar</h1>
        <p class="mt-1 text-sm text-gray-500">Nasiyaga olgan mijozlar ro'yxati</p>
      </div>
      <button
        @click="printDebtors"
        :disabled="loading || debtors.length === 0"
        class="btn-primary flex items-center gap-2"
      >
        <PrinterIcon class="h-5 w-5" />
        Chop etish
      </button>
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

          <!-- Unpaid Invoices -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-3">To'lanmagan fakturalar</h3>

            <div v-if="loadingInvoices" class="flex items-center justify-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="unpaidInvoices.length === 0" class="text-center py-6 bg-gray-50 rounded-lg">
              <p class="text-gray-500 text-sm">To'lanmagan fakturalar topilmadi</p>
            </div>

            <div v-else class="space-y-3">
              <div
                v-for="invoice in unpaidInvoices"
                :key="invoice.id"
                class="border border-gray-200 rounded-lg overflow-hidden"
              >
                <!-- Invoice header (clickable) -->
                <div
                  @click="toggleInvoice(invoice.id)"
                  class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                >
                  <div class="flex items-center gap-3">
                    <div>
                      <p class="font-medium text-sm">{{ invoice.invoiceNumber }}</p>
                      <p class="text-xs text-gray-500">{{ formatDate(invoice.invoiceDate) }}</p>
                    </div>
                    <span :class="['badge text-xs', getStatusClass(invoice.status)]">
                      {{ getStatusLabel(invoice.status) }}
                    </span>
                    <span v-if="invoice.overdue" class="text-xs text-red-600 font-medium">
                      {{ invoice.daysOverdue }} kun kechikkan
                    </span>
                  </div>
                  <div class="text-right">
                    <p class="font-semibold text-red-600 text-sm">{{ formatCurrency(invoice.balanceDue) }} so'm</p>
                    <p class="text-xs text-gray-500">Jami: {{ formatCurrency(invoice.totalAmount) }} so'm</p>
                  </div>
                </div>

                <!-- Invoice line items (expandable) -->
                <div v-if="expandedInvoice === invoice.id" class="border-t border-gray-200 bg-gray-50">
                  <!-- Payment info -->
                  <div class="px-4 py-2 flex gap-4 text-xs text-gray-500 border-b border-gray-100">
                    <span>To'lov muddati: <strong>{{ formatDate(invoice.dueDate) }}</strong></span>
                    <span v-if="invoice.paidAmount > 0">To'langan: <strong>{{ formatCurrency(invoice.paidAmount) }} so'm</strong></span>
                    <span v-if="invoice.posTransactionNumber">Tranzaksiya: <strong>{{ invoice.posTransactionNumber }}</strong></span>
                  </div>

                  <!-- Items table -->
                  <table v-if="invoice.lines?.length" class="w-full text-sm">
                    <thead>
                      <tr class="text-xs text-gray-500 border-b border-gray-200">
                        <th class="px-4 py-2 text-left font-medium">Mahsulot</th>
                        <th class="px-4 py-2 text-right font-medium">Soni</th>
                        <th class="px-4 py-2 text-right font-medium">Narxi</th>
                        <th class="px-4 py-2 text-right font-medium">Jami</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100">
                      <tr v-for="line in invoice.lines" :key="line.id">
                        <td class="px-4 py-2">
                          <p class="font-medium">{{ line.productName || line.description }}</p>
                          <p v-if="line.productSku" class="text-xs text-gray-400">{{ line.productSku }}</p>
                        </td>
                        <td class="px-4 py-2 text-right">{{ line.quantity }}{{ line.unitOfMeasure ? ' ' + line.unitOfMeasure : '' }}</td>
                        <td class="px-4 py-2 text-right">{{ formatCurrency(line.unitPrice) }}</td>
                        <td class="px-4 py-2 text-right font-medium">{{ formatCurrency(line.lineTotal) }} so'm</td>
                      </tr>
                    </tbody>
                  </table>

                  <div v-else class="px-4 py-3 text-sm text-gray-500">
                    Mahsulot tafsilotlari mavjud emas
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
