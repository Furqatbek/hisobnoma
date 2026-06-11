<script setup>
import { useToastStore } from '@/stores/toast'
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'

const toast = useToastStore()

const styles = {
  error: 'bg-red-50 border-red-200 text-red-800',
  success: 'bg-green-50 border-green-200 text-green-800',
  info: 'bg-blue-50 border-blue-200 text-blue-800'
}

const icons = {
  error: ExclamationTriangleIcon,
  success: CheckCircleIcon,
  info: InformationCircleIcon
}
</script>

<template>
  <div class="fixed top-4 right-4 z-[100] flex flex-col gap-2 w-80 max-w-[calc(100vw-2rem)]" aria-live="polite">
    <transition-group name="toast">
      <div
        v-for="item in toast.toasts"
        :key="item.id"
        :class="styles[item.type] || styles.info"
        class="flex items-start gap-2 rounded-lg border px-4 py-3 shadow-lg text-sm"
        role="alert"
      >
        <component :is="icons[item.type] || icons.info" class="h-5 w-5 flex-shrink-0 mt-0.5" />
        <span class="flex-1 break-words">{{ item.message }}</span>
        <button class="flex-shrink-0 opacity-60 hover:opacity-100" @click="toast.dismiss(item.id)">
          <XMarkIcon class="h-4 w-4" />
        </button>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(1rem);
}
.toast-leave-to {
  opacity: 0;
}
</style>
