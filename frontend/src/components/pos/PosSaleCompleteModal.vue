<script setup>
import { ref } from 'vue'
import { CheckCircleIcon, PrinterIcon } from '@heroicons/vue/24/outline'
import ReceiptTemplate from '@/components/ReceiptTemplate.vue'
import { formatCurrency } from '@/utils/format'

defineProps({
  // The completed sale as returned by the server, or null when closed.
  sale: { type: Object, default: null }
})
const emit = defineEmits(['close'])

const receiptRef = ref(null)

function print() {
  receiptRef.value?.printReceipt()
}
</script>

<template>
  <div v-if="sale" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4 py-6">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75"></div>
      <div class="relative bg-white rounded-xl max-w-md w-full p-6">
        <div class="text-center">
          <CheckCircleIcon class="h-16 w-16 text-green-500 mx-auto" />
          <h3 class="mt-2 text-xl font-bold text-gray-900">{{ $t('pos.saleComplete') }}</h3>
          <p class="text-sm text-gray-500">{{ sale.transactionNumber }}</p>
        </div>

        <div class="mt-4 grid gap-3" :class="sale.changeAmount > 0 ? 'grid-cols-2' : 'grid-cols-1'">
          <div class="text-center p-3 bg-gray-100 rounded-lg">
            <p class="text-sm text-gray-500">{{ $t('total') }}</p>
            <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(sale.totalAmount) }}</p>
          </div>
          <div v-if="sale.changeAmount > 0" class="text-center p-3 bg-green-100 rounded-lg">
            <p class="text-sm text-green-700">{{ $t('pos.change') }}</p>
            <p class="text-2xl font-bold text-green-700">{{ formatCurrency(sale.changeAmount) }}</p>
          </div>
        </div>

        <div class="mt-4 max-h-64 overflow-y-auto border border-gray-200 rounded-lg bg-white">
          <ReceiptTemplate ref="receiptRef" :transaction="sale" type="sale" />
        </div>

        <div class="flex gap-3 mt-5">
          <button @click="print" class="btn-secondary flex-1 py-3">
            <PrinterIcon class="h-5 w-5 mr-2" />
            {{ $t('pos.printReceipt') }}
          </button>
          <button @click="emit('close')" class="btn-primary flex-1 py-3">
            {{ $t('pos.newSale') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
