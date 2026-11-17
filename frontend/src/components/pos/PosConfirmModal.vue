<script setup>
defineProps({
  show: { type: Boolean, default: false },
  message: { type: String, default: '' },
  confirmLabel: { type: String, default: '' },
  danger: { type: Boolean, default: false }
})
const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-[60] overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('cancel')"></div>
      <div class="relative bg-white rounded-xl max-w-sm w-full p-6">
        <p class="text-base text-gray-900 text-center mb-6">{{ message }}</p>
        <div class="flex gap-3">
          <button @click="emit('cancel')" class="btn-secondary flex-1 py-3">{{ $t('cancel') }}</button>
          <button
            @click="emit('confirm')"
            class="flex-1 py-3"
            :class="danger ? 'btn-danger' : 'btn-primary'"
          >
            {{ confirmLabel || $t('pos.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
