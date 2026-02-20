<script setup>
import { computed, ref } from 'vue'
import { useReceiptStore } from '@/stores/receipt'
import InvoiceA4Template from '@/components/InvoiceA4Template.vue'

const props = defineProps({
  transaction: {
    type: Object,
    required: true
  },
  type: {
    type: String,
    default: 'sale'
  }
})

const receiptStore = useReceiptStore()
const config = computed(() => receiptStore.config)
const receiptRef = ref(null)
const invoiceA4Ref = ref(null)

const is80 = computed(() => config.value.paperWidth === '80')

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

function formatDate(dateString) {
  const date = new Date(dateString)
  return date.toLocaleDateString('uz-UZ', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const paymentTypeLabels = {
  CASH: 'Naqd',
  CARD: 'Karta',
  CREDIT: 'Nasiya',
  MOBILE_PAYMENT: 'Mobil',
  TRANSFER: "O'tkazma"
}

function getPaymentLabel(type) {
  return paymentTypeLabels[type] || type
}

function getPrintCSS(paperWidth) {
  return `
    @page {
      size: ${paperWidth} auto;
      margin: 0;
    }
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: 'Arial', sans-serif;
      font-size: ${paperWidth === '80mm' ? '12px' : '10px'};
      line-height: 1.35;
      width: ${paperWidth};
      padding: 2.5mm;
      color: #000;
      -webkit-print-color-adjust: exact;
    }

    /* Header */
    .rc-header {
      text-align: center;
      padding-bottom: 6px;
      border-bottom: 1px dashed #000;
      margin-bottom: 6px;
    }
    .rc-brand {
      font-size: ${paperWidth === '80mm' ? '16px' : '13px'};
      font-weight: 700;
      letter-spacing: 0.5px;
      margin-bottom: 2px;
    }
    .rc-header-line {
      font-size: ${paperWidth === '80mm' ? '10px' : '9px'};
      line-height: 1.4;
    }

    /* Meta info */
    .rc-meta {
      padding-bottom: 6px;
      border-bottom: 1.5px solid #000;
      margin-bottom: 6px;
    }
    .rc-meta-row {
      display: flex;
      justify-content: space-between;
      font-size: ${paperWidth === '80mm' ? '11px' : '9.5px'};
      padding: 1px 0;
    }
    .rc-meta-label {
      font-weight: 600;
    }
    .rc-meta-value {
      text-align: right;
    }

    /* Items table */
    .rc-items {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 6px;
    }
    .rc-items thead th {
      font-size: ${paperWidth === '80mm' ? '10px' : '8.5px'};
      font-weight: 700;
      text-transform: uppercase;
      padding: 3px 2px;
      border-bottom: 1.5px solid #000;
      border-top: 1.5px solid #000;
    }
    .rc-items th.col-num { text-align: center; width: 20px; }
    .rc-items th.col-name { text-align: left; }
    .rc-items th.col-qty { text-align: center; width: ${paperWidth === '80mm' ? '36px' : '28px'}; }
    .rc-items th.col-price { text-align: right; width: ${paperWidth === '80mm' ? '70px' : '52px'}; }
    .rc-items th.col-total { text-align: right; width: ${paperWidth === '80mm' ? '76px' : '58px'}; }

    .rc-items tbody td {
      font-size: ${paperWidth === '80mm' ? '11px' : '9.5px'};
      padding: 3px 2px;
      vertical-align: top;
      border-bottom: 1px dotted #ccc;
    }
    .rc-items td.col-num { text-align: center; }
    .rc-items td.col-name {
      text-align: left;
      word-break: break-word;
    }
    .rc-items td.col-qty { text-align: center; }
    .rc-items td.col-price { text-align: right; white-space: nowrap; }
    .rc-items td.col-total { text-align: right; font-weight: 700; white-space: nowrap; }

    .rc-items tbody tr:last-child td {
      border-bottom: none;
    }

    /* Totals */
    .rc-totals {
      border-top: 1px dashed #000;
      padding-top: 4px;
      margin-bottom: 4px;
    }
    .rc-total-row {
      display: flex;
      justify-content: space-between;
      font-size: ${paperWidth === '80mm' ? '11px' : '9.5px'};
      padding: 1.5px 0;
    }
    .rc-total-row .label {
      font-weight: 600;
    }
    .rc-total-row .value {
      font-weight: 600;
      text-align: right;
    }

    /* Grand total */
    .rc-grand {
      border-top: 2px solid #000;
      border-bottom: 2px solid #000;
      padding: 5px 0;
      margin: 4px 0;
      text-align: center;
    }
    .rc-grand-label {
      font-size: ${paperWidth === '80mm' ? '11px' : '9.5px'};
      font-weight: 600;
    }
    .rc-grand-amount {
      font-size: ${paperWidth === '80mm' ? '18px' : '14px'};
      font-weight: 700;
      letter-spacing: 0.5px;
    }

    /* Payments */
    .rc-payments {
      padding: 4px 0;
      border-bottom: 1px dashed #000;
      margin-bottom: 6px;
    }
    .rc-pay-row {
      display: flex;
      justify-content: space-between;
      font-size: ${paperWidth === '80mm' ? '11px' : '9.5px'};
      padding: 1.5px 0;
    }

    /* Footer */
    .rc-footer {
      text-align: center;
      padding-top: 6px;
    }
    .rc-footer-site {
      font-size: ${paperWidth === '80mm' ? '10px' : '9px'};
    }
    .rc-footer-thanks {
      font-size: ${paperWidth === '80mm' ? '12px' : '10px'};
      font-weight: 700;
      margin: 4px 0;
    }
    .rc-footer-time {
      font-size: ${paperWidth === '80mm' ? '9px' : '8px'};
      color: #555;
    }

    @media print {
      body { width: ${paperWidth}; }
    }
  `
}

function printReceipt() {
  if (config.value.paperWidth === 'A4') {
    if (invoiceA4Ref.value) {
      invoiceA4Ref.value.printInvoice()
    }
    return
  }

  const printContent = receiptRef.value.innerHTML
  const paperWidth = is80.value ? '80mm' : '58mm'

  const printWindow = window.open('', '_blank', 'width=400,height=600')
  printWindow.document.write(`<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Chek</title>
  <style>${getPrintCSS(paperWidth)}</style>
</head>
<body>${printContent}</body>
</html>`)

  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 250)
}

defineExpose({ printReceipt })
</script>

<template>
  <div
    ref="receiptRef"
    class="receipt-preview"
    :class="is80 ? 'receipt-80' : 'receipt-58'"
  >
    <!-- Header -->
    <div class="rc-header">
      <div class="rc-brand">{{ config.brandName }}</div>
      <div class="rc-header-line" v-if="config.address">{{ config.address }}</div>
      <div class="rc-header-line" v-if="config.phone">Tel: {{ config.phone }}</div>
      <div class="rc-header-line" v-if="config.taxId">STIR: {{ config.taxId }}</div>
    </div>

    <!-- Transaction Meta -->
    <div class="rc-meta">
      <div class="rc-meta-row">
        <span class="rc-meta-label">Chek №:</span>
        <span class="rc-meta-value">{{ transaction.transactionNumber || transaction.orderNumber || `#${transaction.id}` }}</span>
      </div>
      <div class="rc-meta-row">
        <span class="rc-meta-label">Sana:</span>
        <span class="rc-meta-value">{{ formatDate(transaction.createdAt || transaction.orderDate) }}</span>
      </div>
      <div class="rc-meta-row" v-if="transaction.terminalName">
        <span class="rc-meta-label">Kassa:</span>
        <span class="rc-meta-value">{{ transaction.terminalName }}</span>
      </div>
      <div class="rc-meta-row" v-if="transaction.cashierName">
        <span class="rc-meta-label">Kassir:</span>
        <span class="rc-meta-value">{{ transaction.cashierName }}</span>
      </div>
      <div class="rc-meta-row" v-if="transaction.customerName">
        <span class="rc-meta-label">Mijoz:</span>
        <span class="rc-meta-value">
          {{ transaction.customerName }}
          <span v-if="transaction.customerPhone" style="font-size: 9px;"> ({{ transaction.customerPhone }})</span>
        </span>
      </div>
    </div>

    <!-- Items Table -->
    <table class="rc-items">
      <thead>
        <tr>
          <th class="col-num">№</th>
          <th class="col-name">Mahsulot</th>
          <th class="col-qty">Soni</th>
          <th class="col-price">Narx</th>
          <th class="col-total">Summa</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, index) in transaction.lines" :key="index">
          <td class="col-num">{{ index + 1 }}</td>
          <td class="col-name">{{ item.productName }}</td>
          <td class="col-qty">{{ item.quantity }}</td>
          <td class="col-price">{{ formatCurrency(item.unitPrice) }}</td>
          <td class="col-total">{{ formatCurrency(item.lineTotal) }}</td>
        </tr>
      </tbody>
    </table>

    <!-- Totals -->
    <div class="rc-totals" v-if="(transaction.subtotal && transaction.subtotal !== transaction.totalAmount) || transaction.discountAmount > 0 || transaction.taxAmount > 0">
      <div class="rc-total-row" v-if="transaction.subtotal && transaction.subtotal !== transaction.totalAmount">
        <span class="label">Oraliq summa:</span>
        <span class="value">{{ formatCurrency(transaction.subtotal) }}</span>
      </div>
      <div class="rc-total-row" v-if="transaction.discountAmount > 0">
        <span class="label">Chegirma:</span>
        <span class="value">-{{ formatCurrency(transaction.discountAmount) }}</span>
      </div>
      <div class="rc-total-row" v-if="transaction.taxAmount > 0">
        <span class="label">Soliq:</span>
        <span class="value">{{ formatCurrency(transaction.taxAmount) }}</span>
      </div>
    </div>

    <!-- Grand Total -->
    <div class="rc-grand">
      <div class="rc-grand-label">JAMI</div>
      <div class="rc-grand-amount">{{ formatCurrency(transaction.totalAmount || transaction.total) }} so'm</div>
    </div>

    <!-- Payments -->
    <div v-if="transaction.payments?.length" class="rc-payments">
      <div class="rc-pay-row" v-for="(payment, index) in transaction.payments" :key="index">
        <span>{{ getPaymentLabel(payment.paymentType || payment.type) }}:</span>
        <span>{{ formatCurrency(payment.amount) }} so'm</span>
      </div>
      <div class="rc-pay-row" v-if="transaction.changeAmount > 0">
        <span>Qaytim:</span>
        <span>{{ formatCurrency(transaction.changeAmount) }} so'm</span>
      </div>
    </div>

    <!-- Footer -->
    <div class="rc-footer">
      <div class="rc-footer-site" v-if="config.website">{{ config.website }}</div>
      <div class="rc-footer-thanks">{{ config.footerText }}</div>
      <div class="rc-footer-time">{{ formatDate(new Date().toISOString()) }}</div>
    </div>
  </div>

  <!-- A4 Invoice Template (hidden, used for A4 printing) -->
  <div v-show="config.paperWidth === 'A4'" style="position: absolute; left: -9999px;">
    <InvoiceA4Template ref="invoiceA4Ref" :transaction="transaction" />
  </div>
</template>

<style scoped>
/* ── Preview container ── */
.receipt-preview {
  font-family: 'Arial', sans-serif;
  line-height: 1.35;
  padding: 10px;
  background: #fff;
  color: #000;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.receipt-80 {
  width: 302px;
  font-size: 12px;
}
.receipt-58 {
  width: 220px;
  font-size: 10px;
}

/* ── Header ── */
.rc-header {
  text-align: center;
  padding-bottom: 6px;
  border-bottom: 1px dashed #000;
  margin-bottom: 6px;
}
.rc-brand {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 2px;
}
.receipt-58 .rc-brand {
  font-size: 13px;
}
.rc-header-line {
  font-size: 10px;
  line-height: 1.4;
  color: #333;
}
.receipt-58 .rc-header-line {
  font-size: 9px;
}

/* ── Meta ── */
.rc-meta {
  padding-bottom: 6px;
  border-bottom: 1.5px solid #000;
  margin-bottom: 6px;
}
.rc-meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  padding: 1px 0;
}
.receipt-58 .rc-meta-row {
  font-size: 9.5px;
}
.rc-meta-label {
  font-weight: 600;
}
.rc-meta-value {
  text-align: right;
}

/* ── Items table ── */
.rc-items {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 6px;
}
.rc-items thead th {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  padding: 3px 2px;
  border-bottom: 1.5px solid #000;
  border-top: 1.5px solid #000;
}
.receipt-58 .rc-items thead th {
  font-size: 8.5px;
}
.rc-items th.col-num { text-align: center; width: 20px; }
.rc-items th.col-name { text-align: left; }
.rc-items th.col-qty { text-align: center; width: 36px; }
.rc-items th.col-price { text-align: right; width: 70px; }
.rc-items th.col-total { text-align: right; width: 76px; }

.receipt-58 .rc-items th.col-qty { width: 28px; }
.receipt-58 .rc-items th.col-price { width: 52px; }
.receipt-58 .rc-items th.col-total { width: 58px; }

.rc-items tbody td {
  font-size: 11px;
  padding: 3px 2px;
  vertical-align: top;
  border-bottom: 1px dotted #ccc;
}
.receipt-58 .rc-items tbody td {
  font-size: 9.5px;
}
.rc-items td.col-num { text-align: center; }
.rc-items td.col-name {
  text-align: left;
  word-break: break-word;
}
.rc-items td.col-qty { text-align: center; }
.rc-items td.col-price { text-align: right; white-space: nowrap; }
.rc-items td.col-total { text-align: right; font-weight: 700; white-space: nowrap; }

.rc-items tbody tr:last-child td {
  border-bottom: none;
}

/* ── Totals ── */
.rc-totals {
  border-top: 1px dashed #000;
  padding-top: 4px;
  margin-bottom: 4px;
}
.rc-total-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  padding: 1.5px 0;
}
.receipt-58 .rc-total-row {
  font-size: 9.5px;
}
.rc-total-row .label {
  font-weight: 600;
}
.rc-total-row .value {
  font-weight: 600;
  text-align: right;
}

/* ── Grand total ── */
.rc-grand {
  border-top: 2px solid #000;
  border-bottom: 2px solid #000;
  padding: 5px 0;
  margin: 4px 0;
  text-align: center;
}
.rc-grand-label {
  font-size: 11px;
  font-weight: 600;
}
.receipt-58 .rc-grand-label {
  font-size: 9.5px;
}
.rc-grand-amount {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.receipt-58 .rc-grand-amount {
  font-size: 14px;
}

/* ── Payments ── */
.rc-payments {
  padding: 4px 0;
  border-bottom: 1px dashed #000;
  margin-bottom: 6px;
}
.rc-pay-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  padding: 1.5px 0;
}
.receipt-58 .rc-pay-row {
  font-size: 9.5px;
}

/* ── Footer ── */
.rc-footer {
  text-align: center;
  padding-top: 6px;
}
.rc-footer-site {
  font-size: 10px;
  color: #333;
}
.receipt-58 .rc-footer-site {
  font-size: 9px;
}
.rc-footer-thanks {
  font-size: 12px;
  font-weight: 700;
  margin: 4px 0;
}
.receipt-58 .rc-footer-thanks {
  font-size: 10px;
}
.rc-footer-time {
  font-size: 9px;
  color: #777;
}
.receipt-58 .rc-footer-time {
  font-size: 8px;
}
</style>
