<script setup>
import { ClockIcon } from '@heroicons/vue/24/outline'

defineProps({
  show: { type: Boolean, default: false },
  terminalName: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const openingCash = defineModel('openingCash', { type: Number, default: 0 })

const emit = defineEmits(['close', 'open'])
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('close')"></div>
      <div class="relative bg-white rounded-lg max-w-md w-full p-6">
        <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('pos.shifts.openShift') }}</h3>

        <div class="space-y-4">
          <div>
            <label class="label">{{ $t('pos.shifts.terminal') }}</label>
            <p class="text-sm text-gray-700 font-medium">{{ terminalName }}</p>
          </div>
          <div>
            <label class="label">{{ $t('pos.shifts.openingBalance') }}</label>
            <input
              v-model.number="openingCash"
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
          <button @click="emit('open')" :disabled="loading" class="btn-primary flex-1">
            <ClockIcon class="h-5 w-5 mr-2" />
            {{ loading ? $t('pos.processing') : $t('pos.shifts.openShift') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
