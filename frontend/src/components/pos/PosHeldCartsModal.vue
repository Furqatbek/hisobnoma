<script setup>
import { XMarkIcon, TrashIcon } from '@heroicons/vue/24/outline'
import { formatCurrency, formatDateTime } from '@/utils/format'

defineProps({
  show: { type: Boolean, default: false },
  // [{ id, heldAt, customerName, itemCount, total }]
  carts: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'resume', 'remove'])
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('close')"></div>
      <div class="relative bg-white rounded-xl max-w-md w-full p-5">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-lg font-bold text-gray-900">{{ $t('pos.heldSales') }}</h3>
          <button @click="emit('close')" class="text-gray-400 hover:text-gray-600">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div v-if="carts.length === 0" class="text-center py-6 text-sm text-gray-400">
          {{ $t('pos.noHeldSales') }}
        </div>
        <div v-else class="max-h-80 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100">
          <div v-for="held in carts" :key="held.id" class="flex items-center gap-2 px-3 py-2">
            <button
              @click="emit('resume', held)"
              class="flex-1 min-w-0 text-left py-1 rounded-lg hover:bg-primary-50 active:bg-primary-100 px-2"
            >
              <p class="font-medium text-sm text-gray-900 truncate">
                {{ held.customerName || $t('pos.noCustomer') }}
              </p>
              <p class="text-xs text-gray-400">
                {{ formatDateTime(held.heldAt) }} · {{ held.itemCount }} · {{ formatCurrency(held.total) }}
              </p>
            </button>
            <button
              @click="emit('remove', held)"
              class="h-10 w-10 flex items-center justify-center rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50"
            >
              <TrashIcon class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
