<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { MagnifyingGlassIcon, XMarkIcon, CheckIcon } from '@heroicons/vue/24/outline'

/**
 * Searchable single- or multi-select for entities (products, categories, brands).
 *
 * - multiple=true  → modelValue is a comma-separated id string ("1,2,3"),
 *   matching how the promotion backend stores productIds/categoryIds/brandIds.
 * - multiple=false → modelValue is a single id (Number) or null.
 */
const props = defineProps({
  modelValue: { type: [String, Number, null], default: '' },
  options: { type: Array, default: () => [] }, // [{ id, label, sublabel }]
  multiple: { type: Boolean, default: true },
  placeholder: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()
const open = ref(false)
const query = ref('')

const selectedIds = computed(() => {
  if (props.multiple) {
    return String(props.modelValue || '').split(',').map(s => s.trim()).filter(Boolean)
  }
  return props.modelValue != null && props.modelValue !== '' ? [String(props.modelValue)] : []
})

const selectedOptions = computed(() =>
  selectedIds.value.map(id => props.options.find(o => String(o.id) === id) || { id, label: `#${id}` })
)

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return props.options.slice(0, 50)
  return props.options.filter(o =>
    String(o.label).toLowerCase().includes(q) ||
    String(o.sublabel || '').toLowerCase().includes(q) ||
    String(o.id) === q
  ).slice(0, 50)
})

function isSelected(id) {
  return selectedIds.value.includes(String(id))
}

function toggle(option) {
  const id = String(option.id)
  if (props.multiple) {
    const set = new Set(selectedIds.value)
    if (set.has(id)) set.delete(id); else set.add(id)
    emit('update:modelValue', Array.from(set).join(','))
  } else {
    emit('update:modelValue', isSelected(id) ? null : option.id)
    open.value = false
    query.value = ''
  }
}

function removeChip(id) {
  if (props.multiple) {
    emit('update:modelValue', selectedIds.value.filter(x => x !== String(id)).join(','))
  } else {
    emit('update:modelValue', null)
  }
}
</script>

<template>
  <div class="relative">
    <!-- Selected chips -->
    <div v-if="selectedOptions.length" class="flex flex-wrap gap-1.5 mb-2">
      <span v-for="opt in selectedOptions" :key="opt.id"
        class="inline-flex items-center gap-1 px-2 py-0.5 bg-primary-50 text-primary-700 rounded-md text-xs border border-primary-200">
        {{ opt.label }}
        <button type="button" @click="removeChip(opt.id)" class="hover:text-primary-900">
          <XMarkIcon class="h-3 w-3" />
        </button>
      </span>
    </div>

    <!-- Search trigger -->
    <div class="relative">
      <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
      <input
        v-model="query"
        @focus="open = true"
        type="text"
        :placeholder="placeholder || t('search')"
        class="input pl-9 text-sm"
      />
    </div>

    <!-- Dropdown -->
    <div v-if="open" class="absolute z-20 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-56 overflow-y-auto">
      <div v-if="loading" class="px-3 py-4 text-center text-sm text-gray-400">{{ t('loading') }}</div>
      <div v-else-if="filtered.length === 0" class="px-3 py-4 text-center text-sm text-gray-400">{{ t('noResults') }}</div>
      <button
        v-for="opt in filtered"
        :key="opt.id"
        type="button"
        @click="toggle(opt)"
        :class="['w-full flex items-center justify-between px-3 py-2 text-left text-sm hover:bg-gray-50',
          isSelected(opt.id) ? 'bg-primary-50' : '']"
      >
        <span>
          <span class="font-medium text-gray-800">{{ opt.label }}</span>
          <span v-if="opt.sublabel" class="text-gray-400 ml-2 text-xs">{{ opt.sublabel }}</span>
        </span>
        <CheckIcon v-if="isSelected(opt.id)" class="h-4 w-4 text-primary-600" />
      </button>
    </div>

    <!-- Click-away backdrop -->
    <div v-if="open" class="fixed inset-0 z-10" @click="open = false; query = ''"></div>
  </div>
</template>
