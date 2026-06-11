<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  /** true when discounting the whole transaction, false for a single item */
  forTransaction: { type: Boolean, default: true },
  /** initial { type, value, reason } the parent pre-populates on open */
  initial: { type: Object, default: () => ({ type: 'percent', value: 0, reason: '' }) }
})

const emit = defineEmits(['close', 'apply'])

const form = reactive({ type: 'percent', value: 0, reason: '' })

watch(() => props.show, (open) => {
  if (open) {
    form.type = props.initial.type || 'percent'
    form.value = props.initial.value || 0
    form.reason = props.initial.reason || ''
  }
})
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('close')"></div>
      <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
        <h3 class="text-lg font-medium text-gray-900 mb-4">
          {{ forTransaction ? $t('pos.discountTransaction') : $t('pos.discountItem') }}
        </h3>

        <div class="space-y-4">
          <!-- Discount type toggle -->
          <div class="flex rounded-lg border overflow-hidden">
            <button
              @click="form.type = 'percent'"
              :class="['flex-1 py-2 text-sm font-medium transition-colors', form.type === 'percent' ? 'bg-primary-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']"
            >
              {{ $t('pos.percent') }} (%)
            </button>
            <button
              @click="form.type = 'amount'"
              :class="['flex-1 py-2 text-sm font-medium transition-colors', form.type === 'amount' ? 'bg-primary-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']"
            >
              {{ $t('amount') }}
            </button>
          </div>

          <!-- Discount value -->
          <div>
            <label class="label">
              {{ form.type === 'percent' ? $t('pos.percent') + ' (%)' : $t('amount') }}
            </label>
            <input
              v-model.number="form.value"
              type="number"
              step="0.01"
              min="0"
              :max="form.type === 'percent' ? 100 : undefined"
              class="input text-lg text-center"
              autofocus
            />
          </div>

          <!-- Quick percent buttons -->
          <div v-if="form.type === 'percent'" class="flex gap-2">
            <button
              v-for="pct in [5, 10, 15, 20, 25]"
              :key="pct"
              @click="form.value = pct"
              :class="['flex-1 py-2 text-sm border rounded-lg transition-colors', form.value === pct ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 hover:bg-gray-50']"
            >
              {{ pct }}%
            </button>
          </div>

          <!-- Reason -->
          <div>
            <label class="label">{{ $t('pos.discountReason') }}</label>
            <input
              v-model="form.reason"
              type="text"
              class="input"
              :placeholder="$t('pos.discountReason')"
            />
          </div>
        </div>

        <div class="flex space-x-3 mt-6">
          <button @click="emit('close')" class="btn-secondary flex-1">
            {{ $t('cancel') }}
          </button>
          <button
            @click="emit('apply', { ...form })"
            :disabled="form.value <= 0"
            class="btn-primary flex-1"
          >
            {{ $t('pos.apply') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
