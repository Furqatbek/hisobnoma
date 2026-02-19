<script setup>
import { computed } from 'vue'
import { useReceiptStore } from '@/stores/receipt'

const props = defineProps({
  transaction: {
    type: Object,
    required: true
  }
})

const receiptStore = useReceiptStore()
const config = computed(() => receiptStore.config)

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
  MOBILE_PAYMENT: 'Mobil to\'lov',
  TRANSFER: 'O\'tkazma'
}

function getPaymentLabel(type) {
  return paymentTypeLabels[type] || type
}

function printInvoice() {
  const printWindow = window.open('', '_blank', 'width=800,height=900')

  const items = props.transaction.items || []
  const itemRows = items.map((item, i) => `
    <tr>
      <td style="padding: 8px 12px; border-bottom: 1px solid #e5e7eb; text-align: center;">${i + 1}</td>
      <td style="padding: 8px 12px; border-bottom: 1px solid #e5e7eb;">
        <strong>${item.product?.name || item.productName || item.name || ''}</strong>
        ${item.sku ? '<br><span style="color: #6b7280; font-size: 12px;">' + item.sku + '</span>' : ''}
      </td>
      <td style="padding: 8px 12px; border-bottom: 1px solid #e5e7eb; text-align: center;">${item.quantity || 1}</td>
      <td style="padding: 8px 12px; border-bottom: 1px solid #e5e7eb; text-align: right;">${formatCurrency(item.unitPrice || item.price || 0)}</td>
      <td style="padding: 8px 12px; border-bottom: 1px solid #e5e7eb; text-align: right; font-weight: 600;">${formatCurrency((item.quantity || 1) * (item.unitPrice || item.price || 0))}</td>
    </tr>
  `).join('')

  const payments = props.transaction.payments || []
  const paymentRows = payments.map(p => `
    <tr>
      <td style="padding: 4px 0;">${getPaymentLabel(p.paymentType || p.type)}</td>
      <td style="padding: 4px 0; text-align: right;">${formatCurrency(p.amount)} so'm</td>
    </tr>
  `).join('')

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>Hisob-faktura</title>
      <style>
        @page {
          size: A4;
          margin: 15mm 20mm;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
          font-family: 'Arial', 'Helvetica Neue', sans-serif;
          font-size: 13px;
          line-height: 1.5;
          color: #1f2937;
        }
        @media print {
          body { width: 100%; }
        }
      </style>
    </head>
    <body>
      <!-- Header -->
      <div style="display: flex; justify-content: space-between; align-items: flex-start; padding-bottom: 20px; border-bottom: 3px solid #111827; margin-bottom: 24px;">
        <div>
          <div style="font-size: 24px; font-weight: bold; color: #111827; margin-bottom: 4px;">${config.value.brandName || ''}</div>
          ${config.value.address ? '<div style="color: #6b7280;">' + config.value.address + '</div>' : ''}
          ${config.value.phone ? '<div style="color: #6b7280;">Tel: ' + config.value.phone + '</div>' : ''}
          ${config.value.website ? '<div style="color: #6b7280;">' + config.value.website + '</div>' : ''}
          ${config.value.taxId ? '<div style="color: #6b7280;">STIR: ' + config.value.taxId + '</div>' : ''}
        </div>
        <div style="text-align: right;">
          <div style="font-size: 28px; font-weight: bold; color: #111827; text-transform: uppercase; letter-spacing: 2px;">Hisob-faktura</div>
          <div style="font-size: 16px; color: #4b5563; margin-top: 4px;">${props.transaction.transactionNumber || props.transaction.orderNumber || '#' + props.transaction.id}</div>
          <div style="color: #6b7280; margin-top: 4px;">Sana: ${formatDate(props.transaction.createdAt || props.transaction.orderDate)}</div>
        </div>
      </div>

      <!-- Customer & Meta Info -->
      <div style="display: flex; justify-content: space-between; margin-bottom: 24px;">
        <div style="flex: 1;">
          ${props.transaction.customer ? `
            <div style="font-size: 12px; text-transform: uppercase; color: #9ca3af; font-weight: 600; letter-spacing: 1px; margin-bottom: 6px;">Mijoz</div>
            <div style="font-weight: 600; font-size: 15px;">${props.transaction.customer.name}</div>
            ${props.transaction.customer.phone ? '<div style="color: #6b7280;">Tel: ' + props.transaction.customer.phone + '</div>' : ''}
            ${props.transaction.customer.email ? '<div style="color: #6b7280;">' + props.transaction.customer.email + '</div>' : ''}
            ${props.transaction.customer.address ? '<div style="color: #6b7280;">' + props.transaction.customer.address + '</div>' : ''}
          ` : '<div style="color: #9ca3af;">Mijoz ko\'rsatilmagan</div>'}
        </div>
        <div style="text-align: right;">
          ${props.transaction.terminal ? '<div style="color: #6b7280;">Terminal: ' + (props.transaction.terminal.name || '') + '</div>' : ''}
          ${props.transaction.cashier ? '<div style="color: #6b7280;">Kassir: ' + props.transaction.cashier.name + '</div>' : ''}
        </div>
      </div>

      <!-- Items Table -->
      <table style="width: 100%; border-collapse: collapse; margin-bottom: 24px;">
        <thead>
          <tr style="background-color: #111827; color: white;">
            <th style="padding: 10px 12px; text-align: center; font-weight: 600; width: 50px;">№</th>
            <th style="padding: 10px 12px; text-align: left; font-weight: 600;">Mahsulot</th>
            <th style="padding: 10px 12px; text-align: center; font-weight: 600; width: 80px;">Miqdor</th>
            <th style="padding: 10px 12px; text-align: right; font-weight: 600; width: 130px;">Narx</th>
            <th style="padding: 10px 12px; text-align: right; font-weight: 600; width: 140px;">Summa</th>
          </tr>
        </thead>
        <tbody>
          ${itemRows}
        </tbody>
      </table>

      <!-- Totals -->
      <div style="display: flex; justify-content: flex-end; margin-bottom: 24px;">
        <table style="width: 300px; border-collapse: collapse;">
          ${props.transaction.subtotal ? `
            <tr>
              <td style="padding: 6px 0; color: #6b7280;">Oraliq summa:</td>
              <td style="padding: 6px 0; text-align: right;">${formatCurrency(props.transaction.subtotal)} so'm</td>
            </tr>
          ` : ''}
          ${props.transaction.discount ? `
            <tr>
              <td style="padding: 6px 0; color: #6b7280;">Chegirma:</td>
              <td style="padding: 6px 0; text-align: right; color: #dc2626;">-${formatCurrency(props.transaction.discount)} so'm</td>
            </tr>
          ` : ''}
          ${props.transaction.tax ? `
            <tr>
              <td style="padding: 6px 0; color: #6b7280;">Soliq:</td>
              <td style="padding: 6px 0; text-align: right;">${formatCurrency(props.transaction.tax)} so'm</td>
            </tr>
          ` : ''}
          <tr style="border-top: 2px solid #111827;">
            <td style="padding: 12px 0; font-size: 18px; font-weight: bold;">JAMI:</td>
            <td style="padding: 12px 0; text-align: right; font-size: 18px; font-weight: bold;">${formatCurrency(props.transaction.totalAmount || props.transaction.total)} so'm</td>
          </tr>
        </table>
      </div>

      <!-- Payments -->
      ${payments.length > 0 ? `
        <div style="margin-bottom: 24px; padding: 16px; background: #f9fafb; border-radius: 8px;">
          <div style="font-size: 12px; text-transform: uppercase; color: #9ca3af; font-weight: 600; letter-spacing: 1px; margin-bottom: 8px;">To'lov ma'lumotlari</div>
          <table style="width: 300px; border-collapse: collapse;">
            ${paymentRows}
            ${props.transaction.change > 0 ? `
              <tr>
                <td style="padding: 4px 0; color: #6b7280;">Qaytim:</td>
                <td style="padding: 4px 0; text-align: right;">${formatCurrency(props.transaction.change)} so'm</td>
              </tr>
            ` : ''}
          </table>
        </div>
      ` : ''}

      <!-- Signatures -->
      <div style="display: flex; justify-content: space-between; margin-top: 48px; padding-top: 24px;">
        <!-- Seller -->
        <div style="width: 45%;">
          <div style="font-size: 12px; text-transform: uppercase; color: #6b7280; font-weight: 600; letter-spacing: 1px; margin-bottom: 16px;">Topshirdi (Sotuvchi)</div>
          <div style="margin-bottom: 20px;">
            <div style="font-size: 13px; color: #6b7280; margin-bottom: 4px;">F.I.O.</div>
            <div style="border-bottom: 1px solid #111827; min-height: 24px; padding-bottom: 2px; font-size: 14px;">
              ${props.transaction.cashier?.name || ''}
            </div>
          </div>
          <div>
            <div style="font-size: 13px; color: #6b7280; margin-bottom: 4px;">Imzo</div>
            <div style="border-bottom: 1px solid #111827; min-height: 40px;"></div>
          </div>
        </div>

        <!-- Receiver -->
        <div style="width: 45%;">
          <div style="font-size: 12px; text-transform: uppercase; color: #6b7280; font-weight: 600; letter-spacing: 1px; margin-bottom: 16px;">Qabul qildi (Xaridor)</div>
          <div style="margin-bottom: 20px;">
            <div style="font-size: 13px; color: #6b7280; margin-bottom: 4px;">F.I.O.</div>
            <div style="border-bottom: 1px solid #111827; min-height: 24px; padding-bottom: 2px; font-size: 14px;">
              ${props.transaction.customer?.name || ''}
            </div>
          </div>
          <div>
            <div style="font-size: 13px; color: #6b7280; margin-bottom: 4px;">Imzo</div>
            <div style="border-bottom: 1px solid #111827; min-height: 40px;"></div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div style="margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; text-align: center; color: #9ca3af; font-size: 12px;">
        <div style="font-size: 14px; font-weight: 600; color: #374151; margin-bottom: 4px;">${config.value.footerText || ''}</div>
        ${config.value.website ? '<div>' + config.value.website + '</div>' : ''}
        <div style="margin-top: 8px;">Chop etilgan: ${formatDate(new Date().toISOString())}</div>
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

defineExpose({ printInvoice })
</script>

<template>
  <div class="invoice-preview">
    <!-- Header -->
    <div class="invoice-header">
      <div>
        <div class="company-name">{{ config.brandName }}</div>
        <div class="company-info" v-if="config.address">{{ config.address }}</div>
        <div class="company-info" v-if="config.phone">Tel: {{ config.phone }}</div>
        <div class="company-info" v-if="config.taxId">STIR: {{ config.taxId }}</div>
      </div>
      <div class="invoice-title-block">
        <div class="invoice-title">Hisob-faktura</div>
        <div class="invoice-number">{{ transaction.transactionNumber || transaction.orderNumber || `#${transaction.id}` }}</div>
        <div class="invoice-date">{{ formatDate(transaction.createdAt || transaction.orderDate) }}</div>
      </div>
    </div>

    <!-- Customer -->
    <div class="customer-section" v-if="transaction.customer">
      <div class="section-label">Mijoz</div>
      <div class="customer-name">{{ transaction.customer.name }}</div>
      <div class="customer-detail" v-if="transaction.customer.phone">Tel: {{ transaction.customer.phone }}</div>
    </div>

    <!-- Items Table -->
    <table class="items-table">
      <thead>
        <tr>
          <th class="th-num">№</th>
          <th class="th-name">Mahsulot</th>
          <th class="th-qty">Miqdor</th>
          <th class="th-price">Narx</th>
          <th class="th-total">Summa</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, index) in transaction.items" :key="index">
          <td class="td-num">{{ index + 1 }}</td>
          <td>{{ item.product?.name || item.productName || item.name }}</td>
          <td class="td-qty">{{ item.quantity }}</td>
          <td class="td-price">{{ formatCurrency(item.unitPrice || item.price) }}</td>
          <td class="td-total">{{ formatCurrency((item.quantity || 1) * (item.unitPrice || item.price || 0)) }}</td>
        </tr>
      </tbody>
    </table>

    <!-- Totals -->
    <div class="totals-section">
      <div class="total-row grand-total">
        <span>JAMI:</span>
        <span>{{ formatCurrency(transaction.totalAmount || transaction.total) }} so'm</span>
      </div>
    </div>

    <!-- Payments -->
    <div class="payments-section" v-if="transaction.payments?.length">
      <div class="section-label">To'lov</div>
      <div class="payment-row" v-for="(payment, index) in transaction.payments" :key="index">
        <span>{{ getPaymentLabel(payment.paymentType || payment.type) }}:</span>
        <span>{{ formatCurrency(payment.amount) }} so'm</span>
      </div>
    </div>

    <!-- Signatures -->
    <div class="signatures-section">
      <div class="signature-block">
        <div class="signature-title">Topshirdi (Sotuvchi)</div>
        <div class="signature-field">
          <div class="signature-label">F.I.O.</div>
          <div class="signature-line">{{ transaction.cashier?.name || '' }}</div>
        </div>
        <div class="signature-field">
          <div class="signature-label">Imzo</div>
          <div class="signature-line signature-line-tall"></div>
        </div>
      </div>
      <div class="signature-block">
        <div class="signature-title">Qabul qildi (Xaridor)</div>
        <div class="signature-field">
          <div class="signature-label">F.I.O.</div>
          <div class="signature-line">{{ transaction.customer?.name || '' }}</div>
        </div>
        <div class="signature-field">
          <div class="signature-label">Imzo</div>
          <div class="signature-line signature-line-tall"></div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="invoice-footer">
      {{ config.footerText }}
    </div>
  </div>
</template>

<style scoped>
.invoice-preview {
  font-family: 'Arial', sans-serif;
  font-size: 11px;
  line-height: 1.4;
  padding: 16px;
  background: white;
  color: #1f2937;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 12px;
  border-bottom: 2px solid #111827;
  margin-bottom: 12px;
}

.company-name {
  font-size: 16px;
  font-weight: bold;
  color: #111827;
}

.company-info {
  font-size: 10px;
  color: #6b7280;
}

.invoice-title-block {
  text-align: right;
}

.invoice-title {
  font-size: 14px;
  font-weight: bold;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #111827;
}

.invoice-number {
  font-size: 12px;
  color: #4b5563;
}

.invoice-date {
  font-size: 10px;
  color: #6b7280;
}

.section-label {
  font-size: 9px;
  text-transform: uppercase;
  color: #9ca3af;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-bottom: 2px;
}

.customer-section {
  margin-bottom: 12px;
}

.customer-name {
  font-weight: 600;
  font-size: 12px;
}

.customer-detail {
  font-size: 10px;
  color: #6b7280;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 12px;
  font-size: 10px;
}

.items-table thead tr {
  background-color: #111827;
  color: white;
}

.items-table th {
  padding: 6px 8px;
  font-weight: 600;
  text-align: left;
}

.th-num { width: 30px; text-align: center; }
.th-qty { width: 50px; text-align: center; }
.th-price { width: 70px; text-align: right; }
.th-total { width: 80px; text-align: right; }

.items-table td {
  padding: 5px 8px;
  border-bottom: 1px solid #e5e7eb;
}

.td-num { text-align: center; }
.td-qty { text-align: center; }
.td-price { text-align: right; }
.td-total { text-align: right; font-weight: 600; }

.totals-section {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  width: 180px;
  margin-bottom: 2px;
  font-size: 10px;
}

.grand-total {
  font-size: 13px;
  font-weight: bold;
  border-top: 2px solid #111827;
  padding-top: 6px;
}

.payments-section {
  padding: 8px;
  background: #f9fafb;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 10px;
}

.payment-row {
  display: flex;
  justify-content: space-between;
  width: 180px;
  margin-bottom: 2px;
}

.signatures-section {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 12px;
}

.signature-block {
  width: 45%;
}

.signature-title {
  font-size: 9px;
  text-transform: uppercase;
  color: #6b7280;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.signature-field {
  margin-bottom: 10px;
}

.signature-label {
  font-size: 9px;
  color: #6b7280;
  margin-bottom: 2px;
}

.signature-line {
  border-bottom: 1px solid #111827;
  min-height: 16px;
  font-size: 10px;
  padding-bottom: 1px;
}

.signature-line-tall {
  min-height: 24px;
}

.invoice-footer {
  text-align: center;
  font-size: 10px;
  color: #6b7280;
  padding-top: 8px;
  margin-top: 16px;
  border-top: 1px solid #e5e7eb;
  font-weight: 600;
}
</style>
