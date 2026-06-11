import { defineStore } from 'pinia'
import { ref } from 'vue'

let nextId = 1

/**
 * Global toast notifications — the replacement for blocking alert() calls.
 *
 *   const toast = useToastStore()
 *   toast.error(t('...saveError'))
 *   toast.success(t('saved'))
 */
export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

  function show(message, type = 'info', duration = 4000) {
    if (!message) return
    const id = nextId++
    toasts.value.push({ id, message: String(message), type })
    if (duration > 0) {
      setTimeout(() => dismiss(id), duration)
    }
  }

  function dismiss(id) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  const error = (message) => show(message, 'error', 6000)
  const success = (message) => show(message, 'success')
  const info = (message) => show(message, 'info')

  return { toasts, show, dismiss, error, success, info }
})
