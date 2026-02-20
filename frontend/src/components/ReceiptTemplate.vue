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
const invoiceA4Ref = ref(null)

const is80 = computed(() => config.value.paperWidth === '80')
const charWidth = computed(() => is80.value ? 42 : 32)

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

function printReceipt() {
  if (config.value.paperWidth === 'A4') {
    if (invoiceA4Ref.value) {
      invoiceA4Ref.value.printInvoice()
    }
    return
  }

  const t = props.transaction
  const w = charWidth.value
  const pw = is80.value ? '80mm' : '58mm'
  const dash = '─'.repeat(w)
  const dblDash = '═'.repeat(w)

  function center(text) {
    const pad = Math.max(0, Math.floor((w - text.length) / 2))
    return ' '.repeat(pad) + text
  }

  function row(label, value) {
    const dots = w - label.length - value.length - 1
    if (dots < 1) return label + ' ' + value
    return label + ' ' + '.'.repeat(dots) + value
  }

  function rightAlign(text) {
    const pad = Math.max(0, w - text.length)
    return ' '.repeat(pad) + text
  }

  let lines = []

  // Header
  lines.push(center(config.value.brandName.toUpperCase()))
  if (config.value.address) lines.push(center(config.value.address))
  if (config.value.phone) lines.push(center('Tel: ' + config.value.phone))
  if (config.value.taxId) lines.push(center('STIR: ' + config.value.taxId))
  lines.push(dash)

  // Transaction meta
  const txNum = t.transactionNumber || t.orderNumber || `#${t.id}`
  lines.push(row('Chek', txNum))
  lines.push(row('Sana', formatDate(t.createdAt || t.orderDate)))
  if (t.terminalName) lines.push(row('Kassa', t.terminalName))
  if (t.cashierName) lines.push(row('Kassir', t.cashierName))
  if (t.customerName) {
    let custLine = t.customerName
    if (t.customerPhone) custLine += ' (' + t.customerPhone + ')'
    lines.push(row('Mijoz', custLine))
  }
  lines.push(dblDash)

  // Items header
  if (is80.value) {
    // 80mm: №  Mahsulot          Soni   Narx     Summa
    lines.push(
      '#  ' +
      'Mahsulot'.padEnd(16) +
      'Soni'.padStart(5) +
      'Narx'.padStart(9) +
      'Summa'.padStart(10)
    )
  } else {
    lines.push(
      '#  ' +
      'Mahsulot'.padEnd(10) +
      'Son'.padStart(4) +
      'Narx'.padStart(7) +
      'Summa'.padStart(9)
    )
  }
  lines.push(dash)

  // Items
  const items = t.lines || []
  items.forEach((item, i) => {
    const num = String(i + 1).padEnd(3)
    const qty = String(item.quantity)
    const price = formatCurrency(item.unitPrice)
    const total = formatCurrency(item.lineTotal)

    if (is80.value) {
      const nameWidth = 16
      const name = item.productName.length > nameWidth
        ? item.productName.substring(0, nameWidth - 1) + '…'
        : item.productName.padEnd(nameWidth)
      lines.push(
        num + name + qty.padStart(5) + price.padStart(9) + total.padStart(10)
      )
    } else {
      const nameWidth = 10
      const name = item.productName.length > nameWidth
        ? item.productName.substring(0, nameWidth - 1) + '…'
        : item.productName.padEnd(nameWidth)
      lines.push(
        num + name + qty.padStart(4) + price.padStart(7) + total.padStart(9)
      )
    }

    // If name was truncated, show full name on next line
    const maxNameWidth = is80.value ? 16 : 10
    if (item.productName.length > maxNameWidth) {
      lines.push('   ' + item.productName)
    }
  })
  lines.push(dash)

  // Totals
  if (t.subtotal && t.subtotal !== t.totalAmount) {
    lines.push(row('Oraliq summa', formatCurrency(t.subtotal)))
  }
  if (t.discountAmount > 0) {
    lines.push(row('Chegirma', '-' + formatCurrency(t.discountAmount)))
  }
  if (t.taxAmount > 0) {
    lines.push(row('Soliq', formatCurrency(t.taxAmount)))
  }
  lines.push(dblDash)
  const totalStr = formatCurrency(t.totalAmount || t.total) + " so'm"
  lines.push(center('JAMI: ' + totalStr))
  lines.push(dblDash)

  // Payments
  if (t.payments?.length) {
    t.payments.forEach(p => {
      const label = getPaymentLabel(p.paymentType || p.type)
      lines.push(row(label, formatCurrency(p.amount) + " so'm"))
    })
    if (t.changeAmount > 0) {
      lines.push(row('Qaytim', formatCurrency(t.changeAmount) + " so'm"))
    }
    lines.push(dash)
  }

  // Footer
  lines.push('')
  if (config.value.website) lines.push(center(config.value.website))
  lines.push(center(config.value.footerText))
  lines.push('')
  lines.push(center(formatDate(new Date().toISOString())))
  lines.push('')

  const content = lines.join('\n')

  const printWindow = window.open('', '_blank', 'width=400,height=600')
  printWindow.document.write(`<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Chek</title>
<style>
@page {
  size: ${pw} auto;
  margin: 0;
}
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: ${is80.value ? '12px' : '11px'};
  line-height: 1.4;
  width: ${pw};
  padding: 2mm;
  color: #000;
  white-space: pre;
}
@media print {
  body { width: ${pw}; }
}
</style>
</head>
<body>${content}</body>
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
  <!-- On-screen preview -->
  <div
    class="receipt-preview"
    :class="is80 ? 'receipt-80' : 'receipt-58'"
  >
    <!-- Header -->
    <div class="r-header">
      <div class="r-brand">{{ config.brandName }}</div>
      <div class="r-info" v-if="config.address">{{ config.address }}</div>
      <div class="r-info" v-if="config.phone">Tel: {{ config.phone }}</div>
      <div class="r-info" v-if="config.taxId">STIR: {{ config.taxId }}</div>
    </div>

    <div class="r-sep"></div>

    <!-- Transaction meta -->
    <div class="r-meta">
      <div class="r-row">
        <span>Chek</span>
        <span>{{ transaction.transactionNumber || transaction.orderNumber || `#${transaction.id}` }}</span>
      </div>
      <div class="r-row">
        <span>Sana</span>
        <span>{{ formatDate(transaction.createdAt || transaction.orderDate) }}</span>
      </div>
      <div class="r-row" v-if="transaction.terminalName">
        <span>Kassa</span>
        <span>{{ transaction.terminalName }}</span>
      </div>
      <div class="r-row" v-if="transaction.cashierName">
        <span>Kassir</span>
        <span>{{ transaction.cashierName }}</span>
      </div>
      <div class="r-row" v-if="transaction.customerName">
        <span>Mijoz</span>
        <span>{{ transaction.customerName }}</span>
      </div>
    </div>

    <div class="r-sep-bold"></div>

    <!-- Items -->
    <div class="r-items-header">
      <span class="r-col-num">#</span>
      <span class="r-col-name">Mahsulot</span>
      <span class="r-col-qty">Soni</span>
      <span class="r-col-price">Narx</span>
      <span class="r-col-total">Summa</span>
    </div>
    <div class="r-sep"></div>

    <div class="r-items">
      <div v-for="(item, index) in transaction.lines" :key="index" class="r-item">
        <span class="r-col-num">{{ index + 1 }}</span>
        <span class="r-col-name">{{ item.productName }}</span>
        <span class="r-col-qty">{{ item.quantity }}</span>
        <span class="r-col-price">{{ formatCurrency(item.unitPrice) }}</span>
        <span class="r-col-total">{{ formatCurrency(item.lineTotal) }}</span>
      </div>
    </div>

    <div class="r-sep"></div>

    <!-- Totals -->
    <div class="r-totals">
      <div class="r-row" v-if="transaction.subtotal && transaction.subtotal !== transaction.totalAmount">
        <span>Oraliq summa</span>
        <span>{{ formatCurrency(transaction.subtotal) }}</span>
      </div>
      <div class="r-row" v-if="transaction.discountAmount > 0">
        <span>Chegirma</span>
        <span>-{{ formatCurrency(transaction.discountAmount) }}</span>
      </div>
      <div class="r-row" v-if="transaction.taxAmount > 0">
        <span>Soliq</span>
        <span>{{ formatCurrency(transaction.taxAmount) }}</span>
      </div>
    </div>

    <div class="r-sep-bold"></div>
    <div class="r-grand-total">
      JAMI: {{ formatCurrency(transaction.totalAmount || transaction.total) }} so'm
    </div>
    <div class="r-sep-bold"></div>

    <!-- Payments -->
    <div v-if="transaction.payments?.length" class="r-payments">
      <div class="r-row" v-for="(payment, index) in transaction.payments" :key="index">
        <span>{{ getPaymentLabel(payment.paymentType || payment.type) }}</span>
        <span>{{ formatCurrency(payment.amount) }} so'm</span>
      </div>
      <div class="r-row" v-if="transaction.changeAmount > 0">
        <span>Qaytim</span>
        <span>{{ formatCurrency(transaction.changeAmount) }} so'm</span>
      </div>
      <div class="r-sep"></div>
    </div>

    <!-- Footer -->
    <div class="r-footer">
      <div v-if="config.website">{{ config.website }}</div>
      <div class="r-thanks">{{ config.footerText }}</div>
      <div class="r-timestamp">{{ formatDate(new Date().toISOString()) }}</div>
    </div>
  </div>

  <!-- A4 Invoice Template (hidden, used for A4 printing) -->
  <div v-show="config.paperWidth === 'A4'" style="position: absolute; left: -9999px;">
    <InvoiceA4Template ref="invoiceA4Ref" :transaction="transaction" />
  </div>
</template>

<style scoped>
.receipt-preview {
  font-family: 'Courier New', 'Consolas', monospace;
  line-height: 1.4;
  padding: 12px;
  background: #fff;
  color: #000;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.receipt-80 {
  width: 302px; /* ~80mm at 96dpi */
  font-size: 12px;
}
.receipt-58 {
  width: 220px; /* ~58mm at 96dpi */
  font-size: 10.5px;
}

/* Header */
.r-header {
  text-align: center;
  margin-bottom: 4px;
}
.r-brand {
  font-size: 1.3em;
  font-weight: bold;
  letter-spacing: 0.5px;
}
.r-info {
  font-size: 0.85em;
  color: #555;
}

/* Separators */
.r-sep {
  border-bottom: 1px dashed #000;
  margin: 6px 0;
}
.r-sep-bold {
  border-bottom: 2px solid #000;
  margin: 6px 0;
}

/* Meta rows */
.r-meta {
  margin-bottom: 2px;
}
.r-row {
  display: flex;
  justify-content: space-between;
  padding: 1px 0;
  font-size: 0.92em;
}

/* Items */
.r-items-header {
  display: flex;
  font-weight: bold;
  font-size: 0.85em;
  text-transform: uppercase;
  padding: 2px 0;
}
.r-items .r-item {
  display: flex;
  padding: 2px 0;
  font-size: 0.92em;
}
.r-col-num {
  width: 18px;
  text-align: center;
  flex-shrink: 0;
}
.r-col-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 4px;
}
.r-col-qty {
  width: 32px;
  text-align: center;
  flex-shrink: 0;
}
.r-col-price {
  width: 60px;
  text-align: right;
  flex-shrink: 0;
}
.r-col-total {
  width: 68px;
  text-align: right;
  flex-shrink: 0;
  font-weight: 600;
}

/* 58mm adjustments */
.receipt-58 .r-col-price { width: 50px; }
.receipt-58 .r-col-total { width: 56px; }
.receipt-58 .r-col-qty { width: 28px; }

/* Totals */
.r-totals .r-row {
  font-size: 0.92em;
}

/* Grand total */
.r-grand-total {
  text-align: center;
  font-size: 1.2em;
  font-weight: bold;
  padding: 4px 0;
  letter-spacing: 0.3px;
}

/* Payments */
.r-payments .r-row {
  font-size: 0.92em;
}

/* Footer */
.r-footer {
  text-align: center;
  font-size: 0.85em;
  color: #555;
  padding-top: 4px;
}
.r-thanks {
  font-weight: bold;
  font-size: 1em;
  color: #000;
  margin-top: 4px;
}
.r-timestamp {
  font-size: 0.8em;
  margin-top: 4px;
  color: #888;
}
</style>
