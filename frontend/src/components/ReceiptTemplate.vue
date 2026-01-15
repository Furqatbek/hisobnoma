<script setup>
import { computed, ref } from 'vue'
import { useReceiptStore } from '@/stores/receipt'

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
  const printContent = receiptRef.value.innerHTML
  const printWindow = window.open('', '_blank', 'width=400,height=600')

  const paperWidth = config.value.paperWidth === '58' ? '58mm' : '80mm'

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
          font-family: 'Courier New', monospace;
          font-size: 12px;
          line-height: 1.3;
          width: ${paperWidth};
          padding: 4mm;
          color: #000;
        }
        .receipt-header {
          text-align: center;
          border-bottom: 1px dashed #000;
          padding-bottom: 8px;
          margin-bottom: 8px;
        }
        .brand-name {
          font-size: 16px;
          font-weight: bold;
          margin-bottom: 4px;
        }
        .header-info {
          font-size: 10px;
        }
        .receipt-meta {
          display: flex;
          justify-content: space-between;
          font-size: 10px;
          margin-bottom: 8px;
          border-bottom: 1px dashed #000;
          padding-bottom: 8px;
        }
        .receipt-items {
          border-bottom: 1px dashed #000;
          padding-bottom: 8px;
          margin-bottom: 8px;
        }
        .item-row {
          margin-bottom: 4px;
        }
        .item-name {
          font-weight: bold;
        }
        .item-details {
          display: flex;
          justify-content: space-between;
          font-size: 11px;
        }
        .totals {
          border-bottom: 1px dashed #000;
          padding-bottom: 8px;
          margin-bottom: 8px;
        }
        .total-row {
          display: flex;
          justify-content: space-between;
          margin-bottom: 2px;
        }
        .grand-total {
          font-size: 14px;
          font-weight: bold;
          border-top: 1px solid #000;
          padding-top: 4px;
          margin-top: 4px;
        }
        .payments {
          margin-bottom: 8px;
          font-size: 11px;
        }
        .payment-row {
          display: flex;
          justify-content: space-between;
        }
        .receipt-footer {
          text-align: center;
          font-size: 10px;
          padding-top: 8px;
        }
        .footer-thanks {
          font-size: 12px;
          font-weight: bold;
          margin-top: 8px;
        }
        .customer-info {
          font-size: 10px;
          margin-bottom: 8px;
          padding-bottom: 8px;
          border-bottom: 1px dashed #000;
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
    <div class="receipt-meta">
      <div>
        <div><strong>Chek №:</strong> {{ transaction.transactionNumber || transaction.orderNumber || `#${transaction.id}` }}</div>
        <div><strong>Sana:</strong> {{ formatDate(transaction.createdAt || transaction.orderDate) }}</div>
      </div>
      <div style="text-align: right;">
        <div v-if="transaction.terminal">{{ transaction.terminal.name }}</div>
        <div v-if="transaction.cashier">Kassir: {{ transaction.cashier.name }}</div>
      </div>
    </div>

    <!-- Customer Info -->
    <div v-if="transaction.customer" class="customer-info">
      <div><strong>Mijoz:</strong> {{ transaction.customer.name }}</div>
      <div v-if="transaction.customer.phone">Tel: {{ transaction.customer.phone }}</div>
    </div>

    <!-- Items -->
    <div class="receipt-items">
      <div class="item-row" v-for="(item, index) in transaction.items" :key="index">
        <div class="item-name">{{ item.product?.name || item.productName || item.name }}</div>
        <div class="item-details">
          <span>{{ item.quantity }} x {{ formatCurrency(item.unitPrice || item.price) }}</span>
          <span>{{ formatCurrency((item.quantity || 1) * (item.unitPrice || item.price || 0)) }} so'm</span>
        </div>
      </div>
    </div>

    <!-- Totals -->
    <div class="totals">
      <div class="total-row">
        <span>Jami mahsulotlar:</span>
        <span>{{ transaction.items?.length || 0 }} ta</span>
      </div>
      <div class="total-row" v-if="transaction.subtotal">
        <span>Oraliq summa:</span>
        <span>{{ formatCurrency(transaction.subtotal) }} so'm</span>
      </div>
      <div class="total-row" v-if="transaction.discount">
        <span>Chegirma:</span>
        <span>-{{ formatCurrency(transaction.discount) }} so'm</span>
      </div>
      <div class="total-row" v-if="transaction.tax">
        <span>Soliq:</span>
        <span>{{ formatCurrency(transaction.tax) }} so'm</span>
      </div>
      <div class="total-row grand-total">
        <span>JAMI:</span>
        <span>{{ formatCurrency(transaction.totalAmount || transaction.total) }} so'm</span>
      </div>
    </div>

    <!-- Payments -->
    <div class="payments" v-if="transaction.payments?.length">
      <div><strong>To'lov:</strong></div>
      <div class="payment-row" v-for="(payment, index) in transaction.payments" :key="index">
        <span>{{ getPaymentLabel(payment.paymentType || payment.type) }}:</span>
        <span>{{ formatCurrency(payment.amount) }} so'm</span>
      </div>
      <div class="payment-row" v-if="transaction.change > 0">
        <span>Qaytim:</span>
        <span>{{ formatCurrency(transaction.change) }} so'm</span>
      </div>
    </div>

    <!-- Footer -->
    <div class="receipt-footer">
      <div v-if="config.website">{{ config.website }}</div>
      <div class="footer-thanks">{{ config.footerText }}</div>
      <div style="margin-top: 8px; font-size: 9px;">
        {{ formatDate(new Date().toISOString()) }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.receipt-content {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  max-width: 300px;
  padding: 16px;
  background: white;
  color: #000;
}

.receipt-header {
  text-align: center;
  border-bottom: 1px dashed #ccc;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.brand-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}

.header-info {
  font-size: 11px;
  color: #333;
}

.receipt-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  margin-bottom: 12px;
  border-bottom: 1px dashed #ccc;
  padding-bottom: 12px;
}

.customer-info {
  font-size: 11px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #ccc;
}

.receipt-items {
  border-bottom: 1px dashed #ccc;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.item-row {
  margin-bottom: 8px;
}

.item-name {
  font-weight: bold;
  font-size: 11px;
}

.item-details {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #333;
}

.totals {
  border-bottom: 1px dashed #ccc;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 11px;
}

.grand-total {
  font-size: 14px;
  font-weight: bold;
  border-top: 1px solid #000;
  padding-top: 8px;
  margin-top: 8px;
}

.payments {
  margin-bottom: 12px;
  font-size: 11px;
}

.payment-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
}

.receipt-footer {
  text-align: center;
  font-size: 10px;
  color: #333;
  padding-top: 8px;
}

.footer-thanks {
  font-size: 12px;
  font-weight: bold;
  margin-top: 8px;
  color: #000;
}
</style>
