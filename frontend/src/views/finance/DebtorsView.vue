<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { arReportsApi, arInvoicesApi, arPaymentsApi } from '@/services/api'
import { useReceiptStore } from '@/stores/receipt'
import { MagnifyingGlassIcon, ExclamationTriangleIcon, PrinterIcon, XMarkIcon, EyeIcon, PlusIcon, BanknotesIcon, PencilIcon, CheckIcon } from '@heroicons/vue/24/outline'

const { t } = useI18n()
const router = useRouter()
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
const customerPayments = ref([])
const loadingInvoices = ref(false)
const loadingPayments = ref(false)
const expandedInvoice = ref(null)
const outstandingAmount = ref(null)
const loadingOutstanding = ref(false)

// Selection state for print
const selectedLines = reactive(new Set())

const selectedCount = computed(() => selectedLines.size)

function isLineSelected(lineId) {
  return selectedLines.has(lineId)
}

function toggleLineSelection(lineId) {
  if (selectedLines.has(lineId)) {
    selectedLines.delete(lineId)
  } else {
    selectedLines.add(lineId)
  }
}

function isInvoiceFullySelected(invoice) {
  if (!invoice.lines?.length) return false
  return invoice.lines.every(line => selectedLines.has(line.id))
}

function isInvoicePartiallySelected(invoice) {
  if (!invoice.lines?.length) return false
  const selected = invoice.lines.filter(line => selectedLines.has(line.id)).length
  return selected > 0 && selected < invoice.lines.length
}

function toggleInvoiceSelection(invoice) {
  if (!invoice.lines?.length) return
  const allSelected = isInvoiceFullySelected(invoice)
  if (allSelected) {
    invoice.lines.forEach(line => selectedLines.delete(line.id))
  } else {
    invoice.lines.forEach(line => selectedLines.add(line.id))
    expandedInvoice.value = invoice.id
  }
}

function selectAll() {
  unpaidInvoices.value.forEach(inv => {
    inv.lines?.forEach(line => selectedLines.add(line.id))
  })
}

function deselectAll() {
  selectedLines.clear()
}

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

  let list = balanceReport.value.customerBalances.filter(c => c.netBalance > 0 || c.unpaidInvoiceCount > 0)

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
  customerPayments.value = []
  expandedInvoice.value = null
  outstandingAmount.value = null
  selectedLines.clear()
  showDetailModal.value = true

  loadingInvoices.value = true
  loadingPayments.value = true
  loadingOutstanding.value = true
  try {
    const res = await arInvoicesApi.getUnpaidByCustomer(customer.customerId)
    unpaidInvoices.value = res.data.data || res.data || []
  } catch (error) {
    console.error('Fakturalarni yuklashda xatolik:', error)
  } finally {
    loadingInvoices.value = false
  }
  try {
    const res = await arPaymentsApi.getByCustomer(customer.customerId, { page: 0, size: 50, sort: 'createdAt,desc' })
    const data = res.data.data || res.data
    customerPayments.value = data.content || data || []
  } catch (error) {
    console.error('To\'lovlarni yuklashda xatolik:', error)
  } finally {
    loadingPayments.value = false
  }
  try {
    const res = await arInvoicesApi.getOutstandingByCustomer(customer.customerId)
    const data = res.data.data ?? res.data
    outstandingAmount.value = typeof data === 'number' ? data : data?.totalOutstanding ?? data?.outstanding ?? data?.amount ?? data
  } catch (error) {
    console.error('Outstanding miqdorni yuklashda xatolik:', error)
  } finally {
    loadingOutstanding.value = false
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
  customerPayments.value = []
  expandedInvoice.value = null
  selectedLines.clear()
}

// Payment modal
const showPaymentModal = ref(false)
const paymentInvoice = ref(null)
const paymentForm = reactive({
  amount: 0,
  method: 'CASH',
  date: new Date().toISOString().split('T')[0],
  notes: ''
})
const paymentSubmitting = ref(false)
const paymentError = ref('')

const paymentMethods = computed(() => [
  { value: 'CASH', label: t('enums.paymentMethod.CASH') },
  { value: 'BANK_TRANSFER', label: t('enums.paymentMethod.BANK_TRANSFER') },
  { value: 'CREDIT_CARD', label: t('enums.paymentMethod.CREDIT_CARD') },
  { value: 'MOBILE_PAYMENT', label: t('enums.paymentMethod.MOBILE_PAYMENT') }
])

function openPayment(invoice) {
  paymentInvoice.value = invoice
  paymentForm.amount = invoice.balanceDue || 0
  paymentForm.method = 'CASH'
  paymentForm.date = new Date().toISOString().split('T')[0]
  paymentForm.notes = ''
  paymentError.value = ''
  showPaymentModal.value = true
}

async function submitPayment() {
  if (!paymentInvoice.value || !selectedCustomer.value) return
  if (paymentForm.amount <= 0) {
    paymentError.value = t('finance.expenseDetail.paymentAmountRequired')
    return
  }
  if (paymentForm.amount > paymentInvoice.value.balanceDue) {
    paymentError.value = t('finance.expenseDetail.paymentExceedsBalance')
    return
  }

  paymentSubmitting.value = true
  paymentError.value = ''
  try {
    // Single atomic call: creates payment, posts to GL, applies to invoice, updates customer balance
    await arPaymentsApi.createAndComplete({
      customerId: selectedCustomer.value.customerId,
      paymentDate: paymentForm.date,
      paymentMethod: paymentForm.method,
      paymentAmount: paymentForm.amount,
      notes: paymentForm.notes || null,
      allocations: [{
        arInvoiceId: paymentInvoice.value.id,
        allocatedAmount: paymentForm.amount
      }]
    })

    showPaymentModal.value = false

    // Refresh invoices, payments and main data
    const [invoiceRes, paymentRes] = await Promise.all([
      arInvoicesApi.getUnpaidByCustomer(selectedCustomer.value.customerId),
      arPaymentsApi.getByCustomer(selectedCustomer.value.customerId, { page: 0, size: 50, sort: 'createdAt,desc' })
    ])
    unpaidInvoices.value = invoiceRes.data.data || invoiceRes.data || []
    const payData = paymentRes.data.data || paymentRes.data
    customerPayments.value = payData.content || payData || []
    await fetchData()
  } catch (e) {
    paymentError.value = e.response?.data?.message || t('finance.expenseDetail.paymentError')
  } finally {
    paymentSubmitting.value = false
  }
}

function getStatusLabel(status) {
  return t(`enums.arInvoiceStatus.${status}`, status)
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

function getPaymentStatusLabel(status) {
  return t(`enums.arPaymentStatus.${status}`, status)
}

function getPaymentStatusClass(status) {
  switch (status) {
    case 'COMPLETED': case 'DEPOSITED': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'CANCELLED': case 'REFUNDED': return 'badge-danger'
    default: return 'badge-info'
  }
}

function getMethodLabel(method) {
  return t(`enums.paymentMethod.${method}`, method)
}

function printCustomerDebt() {
  if (!selectedCustomer.value || !unpaidInvoices.value.length) return

  const customer = selectedCustomer.value
  const aging = selectedAging.value
  const invoices = unpaidInvoices.value
  const today = new Date().toLocaleDateString('uz-UZ')

  // Build invoice rows with line items
  let invoiceRows = ''
  let lineCounter = 0
  invoices.forEach((inv) => {
    // Invoice header row
    invoiceRows += `
      <tr style="background: #f3f4f6;">
        <td colspan="6" style="padding: 8px 10px; border: 1px solid #d1d5db; font-weight: 600;">
          <span style="color: #111827;">${t('finance.payments.invoice')}: ${inv.invoiceNumber}</span>
          <span style="margin-left: 16px; color: #6b7280; font-size: 11px;">${t('date')}: ${formatDate(inv.invoiceDate)}</span>
          <span style="margin-left: 16px; color: #6b7280; font-size: 11px;">${t('finance.debtors.dueDate')}: ${formatDate(inv.dueDate)}</span>
          ${inv.overdue ? '<span style="margin-left: 16px; color: #dc2626; font-size: 11px; font-weight: 600;">' + inv.daysOverdue + ' ' + t('finance.debtors.overdue') + '</span>' : ''}
          <span style="float: right; color: #dc2626; font-weight: 700;">${t('balance')}: ${formatCurrency(inv.balanceDue)} ${t('sum')}</span>
        </td>
      </tr>`

    if (inv.lines?.length) {
      inv.lines.forEach((line) => {
        lineCounter++
        invoiceRows += `
          <tr>
            <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center;">${lineCounter}</td>
            <td style="padding: 6px 10px; border: 1px solid #d1d5db;">
              ${line.productName || line.description || ''}
              ${line.productSku ? '<br><span style="color: #6b7280; font-size: 10px;">' + line.productSku + '</span>' : ''}
            </td>
            <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center;">${line.quantity || ''}${line.unitOfMeasure ? ' ' + line.unitOfMeasure : ''}</td>
            <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right;">${formatCurrency(line.unitPrice)}</td>
            <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right;">${line.discountAmount ? formatCurrency(line.discountAmount) : '-'}</td>
            <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right; font-weight: 500;">${formatCurrency(line.lineTotal)}</td>
          </tr>`
      })
    } else {
      invoiceRows += `
        <tr>
          <td colspan="6" style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center; color: #9ca3af; font-size: 11px;">
            ${t('noData')}
          </td>
        </tr>`
    }
  })

  // Totals
  const totalAmount = invoices.reduce((s, inv) => s + (inv.totalAmount || 0), 0)
  const totalPaid = invoices.reduce((s, inv) => s + (inv.paidAmount || 0), 0)
  const totalBalance = invoices.reduce((s, inv) => s + (inv.balanceDue || 0), 0)

  const printWindow = window.open('', '_blank', 'width=900,height=700')
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>${t('finance.debtors.debtor')}: ${customer.customerName}</title>
      <style>
        @page { size: A4 portrait; margin: 12mm 15mm; }
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
          <div style="font-size: 18px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px;">${t('details')}</div>
          <div style="color: #6b7280; margin-top: 4px;">${t('date')}: ${today}</div>
        </div>
      </div>

      <!-- Customer Info -->
      <div style="display: flex; justify-content: space-between; margin-bottom: 16px; padding: 12px 16px; background: #f9fafb; border-radius: 8px; border: 1px solid #e5e7eb;">
        <div>
          <div style="font-size: 16px; font-weight: bold;">${customer.customerName}</div>
          ${customer.customerCode ? '<div style="color: #6b7280; font-size: 12px;">' + t('code') + ': ' + customer.customerCode + '</div>' : ''}
          ${customer.paymentTerms ? '<div style="color: #6b7280; font-size: 12px;">' + t('finance.debtors.dueDate') + ': ' + customer.paymentTerms + '</div>' : ''}
        </div>
        <div style="text-align: right;">
          <div style="font-size: 12px; color: #6b7280;">${t('finance.debtors.totalDebt')}:</div>
          <div style="font-size: 20px; font-weight: bold; color: #dc2626;">${formatCurrency(customer.netBalance)} ${t('sum')}</div>
          ${customer.creditLimit ? '<div style="font-size: 11px; color: #6b7280;">' + t('finance.debtors.creditLimit') + ': ' + formatCurrency(customer.creditLimit) + ' ' + t('sum') + '</div>' : ''}
        </div>
      </div>

      ${aging ? `
      <!-- Aging -->
      <div style="display: flex; gap: 8px; margin-bottom: 16px; font-size: 11px;">
        <div style="flex: 1; padding: 8px; background: #f0fdf4; border-radius: 6px; text-align: center;">
          <div style="color: #6b7280;">${t('finance.debtors.current')}</div>
          <div style="font-weight: 600; color: #15803d;">${formatCurrency(aging.currentAmount)}</div>
        </div>
        <div style="flex: 1; padding: 8px; background: #fefce8; border-radius: 6px; text-align: center;">
          <div style="color: #6b7280;">${t('finance.debtors.days1to30')}</div>
          <div style="font-weight: 600; color: #ca8a04;">${formatCurrency(aging.days1To30)}</div>
        </div>
        <div style="flex: 1; padding: 8px; background: #fff7ed; border-radius: 6px; text-align: center;">
          <div style="color: #6b7280;">${t('finance.debtors.days31to60')}</div>
          <div style="font-weight: 600; color: #ea580c;">${formatCurrency(aging.days31To60)}</div>
        </div>
        <div style="flex: 1; padding: 8px; background: #fef2f2; border-radius: 6px; text-align: center;">
          <div style="color: #6b7280;">${t('finance.debtors.days61to90')}</div>
          <div style="font-weight: 600; color: #dc2626;">${formatCurrency(aging.days61To90)}</div>
        </div>
        <div style="flex: 1; padding: 8px; background: #fef2f2; border-radius: 6px; text-align: center;">
          <div style="color: #6b7280;">${t('finance.debtors.over90days')}</div>
          <div style="font-weight: 600; color: #991b1b;">${formatCurrency(aging.over90Days)}</div>
        </div>
      </div>
      ` : ''}

      <!-- Invoices with Items -->
      <div style="font-size: 14px; font-weight: bold; margin-bottom: 8px;">${t('finance.debtors.unpaidInvoices')}</div>
      <table>
        <thead>
          <tr>
            <th style="text-align: center; width: 35px;">№</th>
            <th>${t('product')}</th>
            <th style="text-align: center; width: 80px;">${t('quantity')}</th>
            <th style="text-align: right; width: 100px;">${t('price')}</th>
            <th style="text-align: right; width: 80px;">${t('receipt.discount')}</th>
            <th style="text-align: right; width: 110px;">${t('total')}</th>
          </tr>
        </thead>
        <tbody>
          ${invoiceRows}
        </tbody>
        <tfoot>
          <tr style="background: #f3f4f6; font-weight: bold;">
            <td colspan="5" style="padding: 8px 10px; border: 1px solid #d1d5db;">${t('total')}</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right;">${formatCurrency(totalAmount)} ${t('sum')}</td>
          </tr>
          <tr style="font-weight: bold;">
            <td colspan="5" style="padding: 6px 10px; border: 1px solid #d1d5db; color: #15803d;">${t('finance.debtors.paidAmount')}</td>
            <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right; color: #15803d;">${formatCurrency(totalPaid)} ${t('sum')}</td>
          </tr>
          <tr style="font-weight: bold; background: #fef2f2;">
            <td colspan="5" style="padding: 8px 10px; border: 1px solid #d1d5db; color: #dc2626; font-size: 13px;">${t('finance.debtors.remainingBalance')}</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right; color: #dc2626; font-size: 13px;">${formatCurrency(totalBalance)} ${t('sum')}</td>
          </tr>
        </tfoot>
      </table>

      <!-- Footer -->
      <div style="margin-top: 40px; display: flex; justify-content: space-between;">
        <div style="width: 40%;">
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">${t('invoice.sellerHandedOver')}:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
        <div style="width: 40%;">
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">${t('invoice.buyerReceived')}:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
      </div>
      <div style="margin-top: 24px; text-align: center; font-size: 10px; color: #9ca3af;">
        ${t('receipt.printedAt')}: ${new Date().toLocaleString('uz-UZ')}
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

function printSelectedItems() {
  if (!selectedCustomer.value || selectedCount.value === 0) return

  const customer = selectedCustomer.value
  const aging = selectedAging.value
  const today = new Date().toLocaleDateString('uz-UZ')

  // Collect selected lines grouped by invoice
  const invoicesWithSelected = unpaidInvoices.value
    .map(inv => {
      const selected = (inv.lines || []).filter(line => selectedLines.has(line.id))
      return selected.length > 0 ? { ...inv, lines: selected } : null
    })
    .filter(Boolean)

  if (invoicesWithSelected.length === 0) return

  let invoiceRows = ''
  let lineCounter = 0
  invoicesWithSelected.forEach((inv) => {
    invoiceRows += `
      <tr style="background: #f3f4f6;">
        <td colspan="6" style="padding: 8px 10px; border: 1px solid #d1d5db; font-weight: 600;">
          <span style="color: #111827;">${t('finance.payments.invoice')}: ${inv.invoiceNumber}</span>
          <span style="margin-left: 16px; color: #6b7280; font-size: 11px;">${t('date')}: ${formatDate(inv.invoiceDate)}</span>
          <span style="margin-left: 16px; color: #6b7280; font-size: 11px;">${t('finance.debtors.dueDate')}: ${formatDate(inv.dueDate)}</span>
          ${inv.overdue ? '<span style="margin-left: 16px; color: #dc2626; font-size: 11px; font-weight: 600;">' + inv.daysOverdue + ' ' + t('finance.debtors.overdue') + '</span>' : ''}
          <span style="float: right; color: #dc2626; font-weight: 700;">${t('balance')}: ${formatCurrency(inv.balanceDue)} ${t('sum')}</span>
        </td>
      </tr>`

    inv.lines.forEach((line) => {
      lineCounter++
      invoiceRows += `
        <tr>
          <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center;">${lineCounter}</td>
          <td style="padding: 6px 10px; border: 1px solid #d1d5db;">
            ${line.productName || line.description || ''}
            ${line.productSku ? '<br><span style="color: #6b7280; font-size: 10px;">' + line.productSku + '</span>' : ''}
          </td>
          <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: center;">${line.quantity || ''}${line.unitOfMeasure ? ' ' + line.unitOfMeasure : ''}</td>
          <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right;">${formatCurrency(line.unitPrice)}</td>
          <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right;">${line.discountAmount ? formatCurrency(line.discountAmount) : '-'}</td>
          <td style="padding: 6px 10px; border: 1px solid #d1d5db; text-align: right; font-weight: 500;">${formatCurrency(line.lineTotal)}</td>
        </tr>`
    })
  })

  const selectedTotal = invoicesWithSelected.reduce((s, inv) =>
    s + inv.lines.reduce((ls, line) => ls + (line.lineTotal || 0), 0), 0)

  const printWindow = window.open('', '_blank', 'width=900,height=700')
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>${t('finance.debtors.debtor')}: ${customer.customerName}</title>
      <style>
        @page { size: A4 portrait; margin: 12mm 15mm; }
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
          <div style="font-size: 18px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px;">${t('details')}</div>
          <div style="color: #6b7280; margin-top: 4px;">${t('date')}: ${today}</div>
        </div>
      </div>

      <!-- Customer Info -->
      <div style="display: flex; justify-content: space-between; margin-bottom: 16px; padding: 12px 16px; background: #f9fafb; border-radius: 8px; border: 1px solid #e5e7eb;">
        <div>
          <div style="font-size: 16px; font-weight: bold;">${customer.customerName}</div>
          ${customer.customerCode ? '<div style="color: #6b7280; font-size: 12px;">' + t('code') + ': ' + customer.customerCode + '</div>' : ''}
        </div>
        <div style="text-align: right;">
          <div style="font-size: 12px; color: #6b7280;">${t('finance.debtors.totalDebt')}:</div>
          <div style="font-size: 20px; font-weight: bold; color: #dc2626;">${formatCurrency(customer.netBalance)} ${t('sum')}</div>
        </div>
      </div>

      <!-- Selected Items -->
      <div style="font-size: 14px; font-weight: bold; margin-bottom: 8px;">${t('finance.debtors.unpaidInvoices')}</div>
      <table>
        <thead>
          <tr>
            <th style="text-align: center; width: 35px;">&#8470;</th>
            <th>${t('product')}</th>
            <th style="text-align: center; width: 80px;">${t('quantity')}</th>
            <th style="text-align: right; width: 100px;">${t('price')}</th>
            <th style="text-align: right; width: 80px;">${t('receipt.discount')}</th>
            <th style="text-align: right; width: 110px;">${t('total')}</th>
          </tr>
        </thead>
        <tbody>
          ${invoiceRows}
        </tbody>
        <tfoot>
          <tr style="background: #fef2f2; font-weight: bold;">
            <td colspan="5" style="padding: 8px 10px; border: 1px solid #d1d5db; color: #dc2626; font-size: 13px;">${t('total')}</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right; color: #dc2626; font-size: 13px;">${formatCurrency(selectedTotal)} ${t('sum')}</td>
          </tr>
        </tfoot>
      </table>

      <!-- Footer -->
      <div style="margin-top: 40px; display: flex; justify-content: space-between;">
        <div style="width: 40%;">
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">${t('invoice.sellerHandedOver')}:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
        <div style="width: 40%;">
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">${t('invoice.buyerReceived')}:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
      </div>
      <div style="margin-top: 24px; text-align: center; font-size: 10px; color: #9ca3af;">
        ${t('receipt.printedAt')}: ${new Date().toLocaleString('uz-UZ')}
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

function printDebtors() {
  const list = debtors.value
  if (!list.length) return

  const rows = list.map((c, i) => {
    const aging = getAgingForCustomer(c.customerId)
    const status = c.onCreditHold ? t('finance.debtors.creditHold') : c.overCreditLimit ? t('finance.debtors.overLimit') : t('active')
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
      <title>${t('finance.debtors.title')}</title>
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
          <div style="font-size: 22px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px;">${t('finance.debtors.title')}</div>
          <div style="color: #6b7280; margin-top: 4px;">${t('date')}: ${today}</div>
        </div>
      </div>

      <!-- Summary -->
      <div style="display: flex; gap: 24px; margin-bottom: 16px; font-size: 13px;">
        <div><strong>${t('finance.debtors.title')}:</strong> ${summary.value.count} ${t('items')}</div>
        <div><strong>${t('finance.debtors.totalDebt')}:</strong> <span style="color: #dc2626;">${formatCurrency(summary.value.totalDebt)} ${t('sum')}</span></div>
        <div><strong>${t('finance.debtors.overLimit')}:</strong> ${summary.value.overLimit}</div>
        <div><strong>${t('finance.debtors.creditHold')}:</strong> ${summary.value.onHold}</div>
      </div>

      <!-- Table -->
      <table>
        <thead>
          <tr>
            <th style="text-align: center; width: 35px;">№</th>
            <th>${t('customer')}</th>
            <th style="text-align: right;">${t('finance.debtors.totalDebt')}</th>
            <th style="text-align: right;">${t('finance.debtors.creditLimit')}</th>
            <th style="text-align: right;">${t('finance.debtors.current')}</th>
            <th style="text-align: right;">${t('finance.debtors.days1to30')}</th>
            <th style="text-align: right;">${t('finance.debtors.days31to60')}</th>
            <th style="text-align: right;">60+</th>
            <th style="text-align: center;">${t('status')}</th>
            <th style="text-align: center;">${t('finance.debtors.lastInvoice')}</th>
          </tr>
        </thead>
        <tbody>
          ${rows}
        </tbody>
        <tfoot>
          <tr style="background: #f3f4f6; font-weight: bold;">
            <td colspan="2" style="padding: 8px 10px; border: 1px solid #d1d5db;">${t('total')}</td>
            <td style="padding: 8px 10px; border: 1px solid #d1d5db; text-align: right; color: #dc2626;">${formatCurrency(summary.value.totalDebt)} ${t('sum')}</td>
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
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">${t('invoice.sellerHandedOver')}:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
        <div style="width: 40%;">
          <div style="font-size: 11px; color: #6b7280; margin-bottom: 4px;">${t('confirm')}:</div>
          <div style="border-bottom: 1px solid #111827; min-height: 30px;"></div>
        </div>
      </div>
      <div style="margin-top: 24px; text-align: center; font-size: 10px; color: #9ca3af;">
        ${t('receipt.printedAt')}: ${new Date().toLocaleString('uz-UZ')}
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
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('finance.debtors.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('finance.debtors.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-3">
        <button
          @click="router.push('/finance/debtors/new')"
          class="btn-primary flex items-center gap-2"
        >
          <PlusIcon class="h-5 w-5" />
          {{ $t('finance.debtors.addDebt') }}
        </button>
        <button
          @click="printDebtors"
          :disabled="loading || debtors.length === 0"
          class="btn-secondary flex items-center gap-2"
        >
          <PrinterIcon class="h-5 w-5" />
          {{ $t('print') }}
        </button>
      </div>
    </div>

    <!-- Summary Cards -->
    <div v-if="!loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="card card-body">
        <p class="text-sm text-gray-500">{{ $t('finance.debtors.title') }}</p>
        <p class="text-2xl font-bold text-gray-900">{{ summary.count }}</p>
      </div>
      <div class="card card-body">
        <p class="text-sm text-gray-500">{{ $t('finance.debtors.totalDebt') }}</p>
        <p class="text-2xl font-bold text-red-600">{{ formatCurrency(summary.totalDebt) }} {{ $t('sum') }}</p>
      </div>
      <div class="card card-body">
        <p class="text-sm text-gray-500">{{ $t('finance.debtors.overLimit') }}</p>
        <p class="text-2xl font-bold text-orange-600">{{ summary.overLimit }}</p>
      </div>
      <div class="card card-body">
        <p class="text-sm text-gray-500">{{ $t('finance.debtors.creditHold') }}</p>
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
            :placeholder="$t('finance.debtors.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
        <select v-model="typeFilter" class="input w-auto">
          <option value="all">{{ $t('all') }}</option>
          <option value="credit_hold">{{ $t('finance.debtors.creditHold') }}</option>
          <option value="over_limit">{{ $t('finance.debtors.overLimit') }}</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="debtors.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('finance.debtors.noDebtors') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('finance.payments.customer') }}</th>
              <th>{{ $t('phone') }} / {{ $t('code') }}</th>
              <th class="text-right">{{ $t('finance.debtors.totalDebt') }}</th>
              <th class="text-right">{{ $t('finance.debtors.creditLimit') }}</th>
              <th class="text-right">{{ $t('finance.debtors.availableCredit') }}</th>
              <th>{{ $t('status') }}</th>
              <th>{{ $t('finance.debtors.lastInvoice') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
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
                <span class="font-semibold text-red-600">{{ formatCurrency(customer.netBalance) }} {{ $t('sum') }}</span>
              </td>
              <td class="text-right text-sm">
                {{ customer.creditLimit ? formatCurrency(customer.creditLimit) + " " + $t('sum') : '-' }}
              </td>
              <td class="text-right text-sm">
                <span :class="(customer.availableCreditLimit || 0) < 0 ? 'text-red-600' : 'text-green-600'">
                  {{ customer.creditLimit ? formatCurrency(customer.availableCreditLimit) + " " + $t('sum') : '-' }}
                </span>
              </td>
              <td>
                <div class="flex flex-col gap-1">
                  <span v-if="customer.overCreditLimit" class="badge badge-danger text-xs">{{ $t('finance.debtors.overLimit') }}</span>
                  <span v-if="customer.onCreditHold" class="badge badge-warning text-xs">{{ $t('finance.debtors.creditHold') }}</span>
                  <span v-if="!customer.overCreditLimit && !customer.onCreditHold" class="badge badge-info text-xs">{{ $t('active') }}</span>
                </div>
              </td>
              <td class="text-sm text-gray-500">
                {{ formatDate(customer.lastInvoiceDate) }}
              </td>
              <td class="text-right">
                <button
                  @click="viewCustomer(customer)"
                  class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 inline-flex"
                  :title="$t('details')"
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
          <div class="flex items-center gap-2">
            <button
              v-if="selectedCount > 0"
              @click="printSelectedItems"
              class="flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-lg"
              :title="$t('print')"
            >
              <PrinterIcon class="h-4 w-4" />
              {{ $t('print') }} ({{ selectedCount }})
            </button>
            <button
              @click="printCustomerDebt"
              :disabled="loadingInvoices || unpaidInvoices.length === 0"
              class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100 disabled:opacity-30 disabled:cursor-not-allowed"
              :title="$t('print')"
            >
              <PrinterIcon class="h-5 w-5" />
            </button>
            <button @click="closeModal" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
              <XMarkIcon class="h-6 w-6" />
            </button>
          </div>

        </div>

        <div class="p-6 space-y-6">
          <!-- Balance Summary -->
          <div class="grid grid-cols-2 gap-4">
            <div class="bg-red-50 rounded-lg p-4">
              <p class="text-sm text-red-600">{{ $t('finance.debtors.totalDebt') }}</p>
              <p class="text-xl font-bold text-red-700">{{ formatCurrency(selectedCustomer.netBalance) }} {{ $t('sum') }}</p>
            </div>
            <div class="bg-orange-50 rounded-lg p-4">
              <p class="text-sm text-orange-600">{{ $t('finance.debtors.outstandingAmount') }}</p>
              <p v-if="loadingOutstanding" class="text-xl font-bold text-orange-700">...</p>
              <p v-else class="text-xl font-bold text-orange-700">{{ outstandingAmount !== null ? formatCurrency(outstandingAmount) + ' ' + $t('sum') : '-' }}</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-sm text-gray-600">{{ $t('finance.debtors.creditLimit') }}</p>
              <p class="text-xl font-bold text-gray-900">{{ selectedCustomer.creditLimit ? formatCurrency(selectedCustomer.creditLimit) + " " + $t('sum') : $t('notSet') }}</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-sm text-gray-600">{{ $t('finance.debtors.openInvoices') }}</p>
              <p class="text-xl font-bold text-gray-900">{{ formatCurrency(selectedCustomer.outstandingInvoices) }} {{ $t('sum') }}</p>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-sm text-gray-600">{{ $t('finance.debtors.availableCredit') }}</p>
              <p class="text-xl font-bold text-gray-900">{{ formatCurrency(selectedCustomer.availableCredits) }} {{ $t('sum') }}</p>
            </div>
          </div>

          <!-- Status Flags -->
          <div v-if="selectedCustomer.overCreditLimit || selectedCustomer.onCreditHold" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div class="flex items-center gap-2 mb-1">
              <ExclamationTriangleIcon class="h-5 w-5 text-yellow-600" />
              <span class="font-medium text-yellow-800">{{ $t('finance.debtors.warning') }}</span>
            </div>
            <p v-if="selectedCustomer.overCreditLimit" class="text-sm text-yellow-700">{{ $t('finance.debtors.overLimit') }}</p>
            <p v-if="selectedCustomer.onCreditHold" class="text-sm text-yellow-700">{{ $t('finance.debtors.creditHold') }}</p>
          </div>

          <!-- Info -->
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span class="text-gray-500">{{ $t('finance.debtors.lastInvoice') }}:</span>
              <span class="ml-2 font-medium">{{ formatDate(selectedCustomer.lastInvoiceDate) }}</span>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.debtors.lastPayment') }}:</span>
              <span class="ml-2 font-medium">{{ formatDate(selectedCustomer.lastPaymentDate) }}</span>
            </div>
            <div>
              <span class="text-gray-500">{{ $t('finance.debtors.dueDate') }}:</span>
              <span class="ml-2 font-medium">{{ selectedCustomer.paymentTerms ? selectedCustomer.paymentTerms + ' ' + $t('finance.debtors.overdue') : '-' }}</span>
            </div>
          </div>

          <!-- Aging Breakdown -->
          <div v-if="selectedAging">
            <h3 class="font-semibold text-gray-900 mb-3">{{ $t('finance.debtors.agingBreakdown') }}</h3>
            <div class="bg-gray-50 rounded-lg overflow-hidden">
              <table class="w-full text-sm">
                <thead class="bg-gray-100">
                  <tr>
                    <th class="px-4 py-2 text-left text-gray-600">{{ $t('finance.debtors.period') }}</th>
                    <th class="px-4 py-2 text-right text-gray-600">{{ $t('amount') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr>
                    <td class="px-4 py-2">{{ $t('finance.debtors.current') }}</td>
                    <td class="px-4 py-2 text-right font-medium">{{ formatCurrency(selectedAging.currentAmount) }} {{ $t('sum') }}</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">{{ $t('finance.debtors.days1to30') }}</td>
                    <td class="px-4 py-2 text-right font-medium" :class="selectedAging.days1To30 > 0 ? 'text-yellow-600' : ''">{{ formatCurrency(selectedAging.days1To30) }} {{ $t('sum') }}</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">{{ $t('finance.debtors.days31to60') }}</td>
                    <td class="px-4 py-2 text-right font-medium" :class="selectedAging.days31To60 > 0 ? 'text-orange-600' : ''">{{ formatCurrency(selectedAging.days31To60) }} {{ $t('sum') }}</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">{{ $t('finance.debtors.days61to90') }}</td>
                    <td class="px-4 py-2 text-right font-medium" :class="selectedAging.days61To90 > 0 ? 'text-red-600' : ''">{{ formatCurrency(selectedAging.days61To90) }} {{ $t('sum') }}</td>
                  </tr>
                  <tr>
                    <td class="px-4 py-2">{{ $t('finance.debtors.over90days') }}</td>
                    <td class="px-4 py-2 text-right font-bold" :class="selectedAging.over90Days > 0 ? 'text-red-700' : ''">{{ formatCurrency(selectedAging.over90Days) }} {{ $t('sum') }}</td>
                  </tr>
                  <tr class="bg-gray-100">
                    <td class="px-4 py-2 font-semibold">{{ $t('total') }}</td>
                    <td class="px-4 py-2 text-right font-bold">{{ formatCurrency(selectedAging.totalBalance) }} {{ $t('sum') }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Payment History -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-3">{{ $t('finance.debtors.paymentHistory') }}</h3>

            <div v-if="loadingPayments" class="flex items-center justify-center py-6">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="customerPayments.length === 0" class="text-center py-4 bg-gray-50 rounded-lg">
              <p class="text-gray-500 text-sm">{{ $t('finance.payments.noArPayments') }}</p>
            </div>

            <div v-else class="bg-gray-50 rounded-lg overflow-hidden">
              <table class="w-full text-sm">
                <thead class="bg-gray-100">
                  <tr>
                    <th class="px-4 py-2 text-left text-gray-600">{{ $t('finance.payments.paymentNumber') }}</th>
                    <th class="px-4 py-2 text-left text-gray-600">{{ $t('date') }}</th>
                    <th class="px-4 py-2 text-left text-gray-600">{{ $t('finance.payments.method') }}</th>
                    <th class="px-4 py-2 text-left text-gray-600">{{ $t('finance.payments.invoice') }}</th>
                    <th class="px-4 py-2 text-right text-gray-600">{{ $t('amount') }}</th>
                    <th class="px-4 py-2 text-center text-gray-600">{{ $t('status') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-200">
                  <tr v-for="payment in customerPayments" :key="payment.id">
                    <td class="px-4 py-2">
                      <p class="font-medium">{{ payment.paymentNumber }}</p>
                    </td>
                    <td class="px-4 py-2 text-gray-500">
                      {{ formatDate(payment.paymentDate) }}
                    </td>
                    <td class="px-4 py-2 text-gray-500">
                      {{ getMethodLabel(payment.paymentMethod) }}
                    </td>
                    <td class="px-4 py-2">
                      <div v-if="payment.allocations?.length">
                        <p v-for="alloc in payment.allocations" :key="alloc.id" class="text-blue-600 text-xs">
                          {{ alloc.invoiceNumber || `#${alloc.arInvoiceId}` }}
                        </p>
                      </div>
                      <span v-else class="text-gray-400">-</span>
                    </td>
                    <td class="px-4 py-2 text-right font-semibold text-green-600">
                      {{ formatCurrency(payment.paymentAmount) }} {{ $t('sum') }}
                    </td>
                    <td class="px-4 py-2 text-center">
                      <span :class="['badge text-xs', getPaymentStatusClass(payment.status)]">
                        {{ getPaymentStatusLabel(payment.status) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Unpaid Invoices -->
          <div>
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-semibold text-gray-900">{{ $t('finance.debtors.unpaidInvoices') }}</h3>
              <div v-if="unpaidInvoices.length > 0" class="flex items-center gap-2">
                <span v-if="selectedCount > 0" class="text-xs text-primary-600 font-medium">
                  {{ selectedCount }} {{ $t('selected') }}
                </span>
                <button
                  v-if="selectedCount === 0"
                  @click="selectAll"
                  class="text-xs text-primary-600 hover:text-primary-700 font-medium"
                >{{ $t('selectAll') }}</button>
                <button
                  v-else
                  @click="deselectAll"
                  class="text-xs text-gray-500 hover:text-gray-700 font-medium"
                >{{ $t('deselectAll') }}</button>
              </div>
            </div>

            <div v-if="loadingInvoices" class="flex items-center justify-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>

            <div v-else-if="unpaidInvoices.length === 0" class="text-center py-6 bg-gray-50 rounded-lg">
              <p class="text-gray-500 text-sm">{{ $t('finance.debtors.noUnpaidInvoices') }}</p>
            </div>

            <div v-else class="space-y-3">
              <div
                v-for="invoice in unpaidInvoices"
                :key="invoice.id"
                class="border rounded-lg overflow-hidden"
                :class="isInvoiceFullySelected(invoice) ? 'border-primary-300 bg-primary-50/30' : isInvoicePartiallySelected(invoice) ? 'border-primary-200' : 'border-gray-200'"
              >
                <!-- Invoice header (clickable) -->
                <div
                  @click="toggleInvoice(invoice.id)"
                  class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                >
                  <div class="flex items-center gap-3">
                    <!-- Invoice-level checkbox -->
                    <button
                      @click.stop="toggleInvoiceSelection(invoice)"
                      class="flex-shrink-0 w-5 h-5 rounded border-2 flex items-center justify-center transition-colors"
                      :class="isInvoiceFullySelected(invoice) ? 'bg-primary-600 border-primary-600' : isInvoicePartiallySelected(invoice) ? 'bg-primary-200 border-primary-400' : 'border-gray-300 hover:border-primary-400'"
                    >
                      <CheckIcon v-if="isInvoiceFullySelected(invoice)" class="h-3.5 w-3.5 text-white" />
                      <div v-else-if="isInvoicePartiallySelected(invoice)" class="w-2 h-0.5 bg-primary-600 rounded"></div>
                    </button>
                    <div>
                      <p class="font-medium text-sm">{{ invoice.invoiceNumber }}</p>
                      <p class="text-xs text-gray-500">{{ formatDate(invoice.invoiceDate) }}</p>
                    </div>
                    <span :class="['badge text-xs', getStatusClass(invoice.status)]">
                      {{ getStatusLabel(invoice.status) }}
                    </span>
                    <span v-if="invoice.overdue" class="text-xs text-red-600 font-medium">
                      {{ invoice.daysOverdue }} {{ $t('finance.debtors.overdue') }}
                    </span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="text-right">
                      <p class="font-semibold text-red-600 text-sm">{{ formatCurrency(invoice.balanceDue) }} {{ $t('sum') }}</p>
                      <p class="text-xs text-gray-500">{{ $t('total') }}: {{ formatCurrency(invoice.totalAmount) }} {{ $t('sum') }}</p>
                    </div>
                    <button
                      @click.stop="router.push(`/finance/debtors/${invoice.id}/edit`)"
                      class="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg flex items-center gap-1"
                    >
                      <PencilIcon class="h-3.5 w-3.5" />
                      {{ $t('edit') }}
                    </button>
                    <button
                      @click.stop="openPayment(invoice)"
                      class="px-3 py-1.5 text-xs font-medium text-white bg-green-600 hover:bg-green-700 rounded-lg flex items-center gap-1"
                    >
                      <BanknotesIcon class="h-3.5 w-3.5" />
                      {{ $t('finance.debtors.makePayment') }}
                    </button>
                  </div>
                </div>

                <!-- Invoice line items (expandable) -->
                <div v-if="expandedInvoice === invoice.id" class="border-t border-gray-200 bg-gray-50">
                  <!-- Payment info -->
                  <div class="px-4 py-2 flex gap-4 text-xs text-gray-500 border-b border-gray-100">
                    <span>{{ $t('finance.debtors.dueDate') }}: <strong>{{ formatDate(invoice.dueDate) }}</strong></span>
                    <span v-if="invoice.paidAmount > 0">{{ $t('finance.debtors.paidAmount') }}: <strong>{{ formatCurrency(invoice.paidAmount) }} {{ $t('sum') }}</strong></span>
                    <span v-if="invoice.posTransactionNumber">{{ $t('finance.payments.reference') }}: <strong>{{ invoice.posTransactionNumber }}</strong></span>
                  </div>

                  <!-- Items table -->
                  <table v-if="invoice.lines?.length" class="w-full text-sm">
                    <thead>
                      <tr class="text-xs text-gray-500 border-b border-gray-200">
                        <th class="w-8 px-2 py-2"></th>
                        <th class="px-4 py-2 text-left font-medium">{{ $t('finance.debtors.products') }}</th>
                        <th class="px-4 py-2 text-right font-medium">{{ $t('quantity') }}</th>
                        <th class="px-4 py-2 text-right font-medium">{{ $t('price') }}</th>
                        <th class="px-4 py-2 text-right font-medium">{{ $t('total') }}</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100">
                      <tr
                        v-for="line in invoice.lines"
                        :key="line.id"
                        @click="toggleLineSelection(line.id)"
                        class="cursor-pointer transition-colors"
                        :class="isLineSelected(line.id) ? 'bg-primary-50' : 'hover:bg-gray-100'"
                      >
                        <td class="px-2 py-2 text-center">
                          <div
                            class="w-4 h-4 rounded border-2 flex items-center justify-center mx-auto transition-colors"
                            :class="isLineSelected(line.id) ? 'bg-primary-600 border-primary-600' : 'border-gray-300'"
                          >
                            <CheckIcon v-if="isLineSelected(line.id)" class="h-3 w-3 text-white" />
                          </div>
                        </td>
                        <td class="px-4 py-2">
                          <p class="font-medium">{{ line.productName || line.description }}</p>
                          <p v-if="line.productSku" class="text-xs text-gray-400">{{ line.productSku }}</p>
                        </td>
                        <td class="px-4 py-2 text-right">{{ line.quantity }}{{ line.unitOfMeasure ? ' ' + line.unitOfMeasure : '' }}</td>
                        <td class="px-4 py-2 text-right">{{ formatCurrency(line.unitPrice) }}</td>
                        <td class="px-4 py-2 text-right font-medium">{{ formatCurrency(line.lineTotal) }} {{ $t('sum') }}</td>
                      </tr>
                    </tbody>
                  </table>

                  <div v-else class="px-4 py-3 text-sm text-gray-500">
                    {{ $t('finance.debtors.noProductDetails') }}
                  </div>

                  <!-- Invoice Summary -->
                  <div class="px-4 py-3 border-t border-gray-200 bg-white">
                    <div class="grid grid-cols-3 gap-3 text-sm">
                      <div>
                        <span class="text-gray-500 text-xs">{{ $t('invoice.grandTotal') }}</span>
                        <p class="font-semibold">{{ formatCurrency(invoice.totalAmount) }} {{ $t('sum') }}</p>
                      </div>
                      <div>
                        <span class="text-gray-500 text-xs">{{ $t('finance.debtors.paidAmount') }}</span>
                        <p class="font-semibold text-green-600">{{ formatCurrency(invoice.paidAmount || 0) }} {{ $t('sum') }}</p>
                      </div>
                      <div>
                        <span class="text-gray-500 text-xs">{{ $t('finance.debtors.remainingBalance') }}</span>
                        <p class="font-bold text-red-600">{{ formatCurrency(invoice.balanceDue) }} {{ $t('sum') }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Payment Modal -->
    <div
      v-if="showPaymentModal && paymentInvoice"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60] p-4"
      @click.self="showPaymentModal = false"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-md w-full">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h3 class="text-lg font-bold text-gray-900">{{ $t('finance.debtors.makePayment') }}</h3>
            <p class="text-sm text-gray-500">{{ paymentInvoice.invoiceNumber }}</p>
          </div>
          <button @click="showPaymentModal = false" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="p-6 space-y-4">
          <div v-if="paymentError" class="p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-600">{{ paymentError }}</p>
          </div>

          <!-- Invoice info -->
          <div class="bg-gray-50 rounded-lg p-3 text-sm">
            <div class="flex justify-between">
              <span class="text-gray-500">{{ $t('finance.debtors.invoiceAmount') }}:</span>
              <span class="font-medium">{{ formatCurrency(paymentInvoice.totalAmount) }} {{ $t('sum') }}</span>
            </div>
            <div v-if="paymentInvoice.paidAmount > 0" class="flex justify-between mt-1">
              <span class="text-gray-500">{{ $t('finance.debtors.paidAmount') }}:</span>
              <span class="font-medium text-green-600">{{ formatCurrency(paymentInvoice.paidAmount) }} {{ $t('sum') }}</span>
            </div>
            <div class="flex justify-between mt-1 pt-1 border-t border-gray-200">
              <span class="text-gray-600 font-medium">{{ $t('finance.debtors.remainingBalance') }}:</span>
              <span class="font-bold text-red-600">{{ formatCurrency(paymentInvoice.balanceDue) }} {{ $t('sum') }}</span>
            </div>
          </div>

          <!-- Amount -->
          <div>
            <label class="label">{{ $t('finance.expenseDetail.paymentAmount') }} <span class="text-red-500">*</span></label>
            <input
              v-model.number="paymentForm.amount"
              type="number"
              :max="paymentInvoice.balanceDue"
              min="1"
              step="1"
              class="input"
            />
          </div>

          <!-- Method -->
          <div>
            <label class="label">{{ $t('finance.payments.paymentMethod') }}</label>
            <select v-model="paymentForm.method" class="input">
              <option v-for="m in paymentMethods" :key="m.value" :value="m.value">{{ m.label }}</option>
            </select>
          </div>

          <!-- Date -->
          <div>
            <label class="label">{{ $t('date') }}</label>
            <input v-model="paymentForm.date" type="date" class="input" />
          </div>

          <!-- Notes -->
          <div>
            <label class="label">{{ $t('notes') }}</label>
            <input v-model="paymentForm.notes" type="text" class="input" :placeholder="$t('finance.expenseDetail.paymentNote')" />
          </div>

          <div class="flex justify-end gap-3 pt-2">
            <button @click="showPaymentModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button
              @click="submitPayment"
              :disabled="paymentSubmitting || paymentForm.amount <= 0"
              class="btn-primary flex items-center gap-2"
            >
              <BanknotesIcon class="h-4 w-4" />
              {{ paymentSubmitting ? $t('saving') : $t('save') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
