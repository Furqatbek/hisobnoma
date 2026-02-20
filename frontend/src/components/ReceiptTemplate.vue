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
    default: 'sale' // 'sale' or 'purchase'
  }
})

const receiptStore = useReceiptStore()
const config = computed(() => receiptStore.config)
const receiptRef = ref(null)
const invoiceA4Ref = ref(null)

// Format currency in UZS
function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value || 0)
}

// Format date in Uzbek locale
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

// Payment type labels in Uzbek
const paymentTypeLabels = {
  CASH: 'Naqd',
  CARD: 'Karta',
  CREDIT: 'Nasiya',
  MOBILE_PAYMENT: 'Mobil to\'lov',
  TRANSFER: 'O\'tkazma'
}

function getPaymentLabel(type) {
  return paymentTypeLabels[type] || type
}

// Print receipt
function printReceipt() {
  // For A4, delegate to the dedicated invoice template
  if (config.value.paperWidth === 'A4') {
    if (invoiceA4Ref.value) {
      invoiceA4Ref.value.printInvoice()
    }
    return
  }

  const printContent = receiptRef.value.innerHTML
  const paperWidth = config.value.paperWidth === '58' ? '58mm' : '80mm'
  const printWindow = window.open('', '_blank', 'width=400,height=600')

  printWindow.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>Chek</title>
        <style>
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
            font-family: 'Arial', 'Helvetica Neue', sans-serif;
            font-size: 11px;
            line-height: 1.3;
            width: ${paperWidth};
            padding: 3mm;
            color: #000;
          }
          .receipt-header {
            text-align: center;
            margin-bottom: 6px;
          }
          .brand-name {
            font-size: 14px;
            font-weight: bold;
            margin-bottom: 2px;
          }
          .header-info {
            font-size: 9px;
          }
          .meta-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 6px;
            font-size: 10px;
          }
          .meta-table td {
            padding: 2px 4px;
            border: 1px solid #000;
          }
          .meta-table .label {
            font-weight: bold;
            background: #f0f0f0;
            width: 35%;
          }
          .items-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 6px;
            font-size: 10px;
          }
          .items-table th {
            padding: 3px 4px;
            border: 1px solid #000;
            background: #000;
            color: #fff;
            font-weight: bold;
            text-align: center;
            font-size: 9px;
          }
          .items-table td {
            padding: 2px 4px;
            border: 1px solid #000;
          }
          .items-table .num { text-align: center; }
          .items-table .qty { text-align: center; }
          .items-table .price { text-align: right; }
          .items-table .total { text-align: right; font-weight: bold; }
          .totals-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 6px;
            font-size: 10px;
          }
          .totals-table td {
            padding: 2px 4px;
            border: 1px solid #000;
          }
          .totals-table .label {
            font-weight: bold;
            background: #f0f0f0;
          }
          .totals-table .value {
            text-align: right;
            font-weight: bold;
          }
          .grand-total td {
            background: #000 !important;
            color: #fff;
            font-size: 12px;
            font-weight: bold;
            padding: 4px;
          }
          .receipt-footer {
            text-align: center;
            font-size: 9px;
            margin-top: 6px;
          }
          .footer-thanks {
            font-size: 11px;
            font-weight: bold;
            margin-top: 4px;
          }
          @media print {
            body {
              width: ${paperWidth};
            }
          }
        </style>
      </head>
      <body>
        ${printContent}
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

// Expose print function
defineExpose({ printReceipt })
</script>

<template>
  <div ref="receiptRef" class="receipt-content">
    <!-- Header -->
    <div class="receipt-header">
      <div class="brand-name">{{ config.brandName }}</div>
      <div class="header-info" v-if="config.address">{{ config.address }}</div>
      <div class="header-info" v-if="config.phone">Tel: {{ config.phone }}</div>
      <div class="header-info" v-if="config.taxId">STIR: {{ config.taxId }}</div>
    </div>

    <!-- Transaction Meta -->
    <table class="meta-table">
      <tr>
        <td class="label">Chek №</td>
        <td>{{ transaction.transactionNumber || transaction.orderNumber || `#${transaction.id}` }}</td>
      </tr>
      <tr>
        <td class="label">Sana</td>
        <td>{{ formatDate(transaction.createdAt || transaction.orderDate) }}</td>
      </tr>
      <tr v-if="transaction.terminalName">
        <td class="label">Kassa</td>
        <td>{{ transaction.terminalName }}</td>
      </tr>
      <tr v-if="transaction.cashierName">
        <td class="label">Kassir</td>
        <td>{{ transaction.cashierName }}</td>
      </tr>
      <tr v-if="transaction.customerName">
        <td class="label">Mijoz</td>
        <td>
          {{ transaction.customerName }}
          <span v-if="transaction.customerPhone" style="font-size: 9px; color: #666;"> ({{ transaction.customerPhone }})</span>
        </td>
      </tr>
    </table>

    <!-- Items Table -->
    <table class="items-table">
      <thead>
        <tr>
          <th style="width: 24px;">№</th>
          <th>Mahsulot</th>
          <th style="width: 36px;">Soni</th>
          <th style="width: 56px;">Narx</th>
          <th style="width: 64px;">Summa</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, index) in transaction.lines" :key="index">
          <td class="num">{{ index + 1 }}</td>
          <td>{{ item.productName }}</td>
          <td class="qty">{{ item.quantity }}</td>
          <td class="price">{{ formatCurrency(item.unitPrice) }}</td>
          <td class="total">{{ formatCurrency(item.lineTotal) }}</td>
        </tr>
      </tbody>
    </table>

    <!-- Totals -->
    <table class="totals-table">
      <tr v-if="transaction.subtotal && transaction.subtotal !== transaction.totalAmount">
        <td class="label">Oraliq summa</td>
        <td class="value">{{ formatCurrency(transaction.subtotal) }}</td>
      </tr>
      <tr v-if="transaction.discountAmount > 0">
        <td class="label">Chegirma</td>
        <td class="value" style="color: #dc2626;">-{{ formatCurrency(transaction.discountAmount) }}</td>
      </tr>
      <tr v-if="transaction.taxAmount > 0">
        <td class="label">Soliq</td>
        <td class="value">{{ formatCurrency(transaction.taxAmount) }}</td>
      </tr>
      <tr class="grand-total">
        <td>JAMI</td>
        <td style="text-align: right;">{{ formatCurrency(transaction.totalAmount || transaction.total) }} so'm</td>
      </tr>
    </table>

    <!-- Payments -->
    <table v-if="transaction.payments?.length" class="totals-table">
      <tr v-for="(payment, index) in transaction.payments" :key="index">
        <td class="label">{{ getPaymentLabel(payment.paymentType || payment.type) }}</td>
        <td class="value">{{ formatCurrency(payment.amount) }} so'm</td>
      </tr>
      <tr v-if="transaction.changeAmount > 0">
        <td class="label">Qaytim</td>
        <td class="value">{{ formatCurrency(transaction.changeAmount) }} so'm</td>
      </tr>
    </table>

    <!-- Footer -->
    <div class="receipt-footer">
      <div v-if="config.website">{{ config.website }}</div>
      <div class="footer-thanks">{{ config.footerText }}</div>
      <div style="margin-top: 4px; font-size: 8px;">
        {{ formatDate(new Date().toISOString()) }}
      </div>
    </div>
  </div>

  <!-- A4 Invoice Template (hidden, used for A4 printing) -->
  <div v-show="config.paperWidth === 'A4'" style="position: absolute; left: -9999px;">
    <InvoiceA4Template ref="invoiceA4Ref" :transaction="transaction" />
  </div>
</template>

<style scoped>
.receipt-content {
  font-family: 'Arial', 'Helvetica Neue', sans-serif;
  font-size: 11px;
  line-height: 1.3;
  max-width: 300px;
  padding: 12px;
  background: white;
  color: #000;
}

.receipt-header {
  text-align: center;
  margin-bottom: 8px;
}

.brand-name {
  font-size: 15px;
  font-weight: bold;
  margin-bottom: 2px;
}

.header-info {
  font-size: 10px;
  color: #444;
}

.meta-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
  font-size: 10px;
}

.meta-table td {
  padding: 3px 6px;
  border: 1px solid #999;
}

.meta-table .label {
  font-weight: bold;
  background: #f3f4f6;
  width: 35%;
  color: #374151;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
  font-size: 10px;
}

.items-table th {
  padding: 4px 5px;
  border: 1px solid #374151;
  background: #374151;
  color: #fff;
  font-weight: 600;
  text-align: center;
  font-size: 9px;
  text-transform: uppercase;
}

.items-table td {
  padding: 3px 5px;
  border: 1px solid #999;
}

.items-table .num { text-align: center; color: #666; }
.items-table .qty { text-align: center; }
.items-table .price { text-align: right; }
.items-table .total { text-align: right; font-weight: 600; }

.totals-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
  font-size: 10px;
}

.totals-table td {
  padding: 3px 6px;
  border: 1px solid #999;
}

.totals-table .label {
  font-weight: 600;
  background: #f3f4f6;
  color: #374151;
}

.totals-table .value {
  text-align: right;
  font-weight: 600;
}

.grand-total td {
  background: #374151 !important;
  color: #fff !important;
  font-size: 12px;
  font-weight: bold;
  padding: 5px 6px;
}

.receipt-footer {
  text-align: center;
  font-size: 9px;
  color: #666;
  padding-top: 6px;
}

.footer-thanks {
  font-size: 11px;
  font-weight: bold;
  margin-top: 4px;
  color: #000;
}
</style>
