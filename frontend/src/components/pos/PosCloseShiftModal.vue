<script setup>
import { ArrowRightStartOnRectangleIcon } from '@heroicons/vue/24/outline'

defineProps({
  show: { type: Boolean, default: false },
  shift: { type: Object, default: null },
  unresolved: { type: Array, default: () => [] },
  unresolvedLoading: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

const closingCash = defineModel('closingCash', { type: Number, default: 0 })
// Inline void-state machine shared with the parent ('__loading__' sentinel
// is set by the parent while the void API call runs)
const voidingId = defineModel('voidingId', { type: [Number, String], default: null })
const voidReason = defineModel('voidReason', { type: String, default: '' })

const emit = defineEmits(['close', 'close-shift', 'void'])

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value || 0)
}
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('close')"></div>
      <div class="relative bg-white rounded-lg w-full p-6" :class="unresolved.length ? 'max-w-2xl' : 'max-w-md'">
        <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('pos.shifts.closeShift') }}</h3>

        <div class="space-y-4">
          <div class="p-3 bg-gray-50 rounded-lg space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('pos.shifts.shiftNumber') }}:</span>
              <span class="font-medium">{{ shift?.shiftNumber }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('pos.shifts.openingBalance') }}:</span>
              <span class="font-medium">{{ formatCurrency(shift?.openingCash || 0) }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('pos.shifts.salesCount') }}:</span>
              <span class="font-medium">{{ shift?.transactionCount || 0 }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-gray-500">{{ $t('pos.shifts.totalSales') }}:</span>
              <span class="font-medium">{{ formatCurrency(shift?.totalSales || 0) }}</span>
            </div>
          </div>

          <!-- Unresolved Transactions Warning -->
          <div v-if="unresolved.length > 0" class="border border-red-200 bg-red-50 rounded-lg p-4 space-y-3">
            <div class="flex items-start gap-2">
              <svg class="h-5 w-5 text-red-500 mt-0.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 6a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 6zm0 9a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
              </svg>
              <div>
                <p class="text-sm font-medium text-red-800">{{ $t('pos.shifts.unresolvedWarning', { count: unresolved.length }) }}</p>
                <p class="text-xs text-red-600 mt-1">{{ $t('pos.shifts.unresolvedHint') }}</p>
              </div>
            </div>

            <div class="space-y-2 max-h-60 overflow-y-auto">
              <div
                v-for="tx in unresolved"
                :key="tx.id"
                class="bg-white rounded-lg border border-red-100 p-3"
              >
                <div class="flex items-center justify-between gap-4">
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-gray-900 truncate">
                      {{ tx.transactionNumber || `#${tx.id}` }}
                      <span class="inline-block ml-2 px-1.5 py-0.5 rounded text-xs font-medium"
                        :class="tx.status === 'HELD' ? 'bg-blue-100 text-blue-700' : 'bg-yellow-100 text-yellow-700'">
                        {{ tx.status === 'HELD' ? $t('pos.transactions.held') : $t('pos.transactions.pending') }}
                      </span>
                    </p>
                    <p class="text-xs text-gray-500 mt-0.5">
                      {{ tx.customerName || $t('pos.walkInCustomer') }} &middot; {{ formatCurrency(tx.totalAmount) }} {{ $t('sum') }}
                    </p>
                  </div>
                  <button
                    v-if="voidingId !== tx.id"
                    @click="voidingId = tx.id; voidReason = ''"
                    class="shrink-0 text-xs px-2 py-1 rounded bg-red-100 text-red-700 hover:bg-red-200 font-medium"
                  >
                    {{ $t('pos.transactions.voidTransaction') }}
                  </button>
                  <div v-if="voidingId === tx.id" class="animate-spin h-4 w-4 border-2 border-red-500 border-t-transparent rounded-full shrink-0" v-show="voidReason === '__loading__'"></div>
                </div>
                <!-- Inline void reason input -->
                <div v-if="voidingId === tx.id && voidReason !== '__loading__'" class="mt-2 flex gap-2">
                  <input
                    v-model="voidReason"
                    type="text"
                    class="input text-sm flex-1"
                    :placeholder="$t('pos.transactions.voidReasonPlaceholder')"
                    @keyup.enter="voidReason.trim() && emit('void', tx)"
                  />
                  <button
                    @click="emit('void', tx)"
                    :disabled="!voidReason.trim()"
                    class="px-3 py-1 text-sm rounded bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300"
                  >
                    {{ $t('confirm') }}
                  </button>
                  <button
                    @click="voidingId = null"
                    class="px-2 py-1 text-sm rounded border border-gray-300 text-gray-600 hover:bg-gray-100"
                  >
                    {{ $t('cancel') }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="unresolvedLoading" class="flex items-center justify-center py-4">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
          </div>

          <div>
            <label class="label">{{ $t('pos.shifts.closingBalance') }}</label>
            <input
              v-model.number="closingCash"
              type="number"
              min="0"
              step="1000"
              class="input text-lg"
              placeholder="0"
            />
          </div>
        </div>

        <div class="flex space-x-3 mt-6">
          <button @click="emit('close')" class="btn-secondary flex-1">
            {{ $t('cancel') }}
          </button>
          <button @click="emit('close-shift')" :disabled="loading" class="btn-primary flex-1 !bg-red-600 hover:!bg-red-700">
            <ArrowRightStartOnRectangleIcon class="h-5 w-5 mr-2" />
            {{ loading ? $t('pos.processing') : $t('pos.shifts.closeShift') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
