<script setup>
import { ref, computed, watch } from 'vue'
import { XMarkIcon, MagnifyingGlassIcon, PlusIcon } from '@heroicons/vue/24/outline'

const props = defineProps({
  show: { type: Boolean, default: false },
  customers: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'select', 'new-customer'])

const filter = ref('')

watch(() => props.show, (open) => {
  if (open) filter.value = ''
})

const filteredCustomers = computed(() => {
  const q = filter.value.toLowerCase().trim()
  if (!q) return props.customers
  return props.customers.filter(c => {
    const name = (c.name || '').toLowerCase()
    const code = (c.code || '').toLowerCase()
    const phone = (c.phone || c.mobilePhone || '').toLowerCase()
    return name.includes(q) || code.includes(q) || phone.includes(q)
  })
})

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
      <div class="relative bg-white rounded-xl max-w-md w-full p-5">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-lg font-bold text-gray-900">{{ $t('pos.selectCustomer') }}</h3>
          <button @click="emit('close')" class="text-gray-400 hover:text-gray-600">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <!-- Filter -->
        <div class="relative mb-3">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            v-model="filter"
            type="text"
            :placeholder="$t('pos.filterCustomerPlaceholder')"
            class="input pl-9 text-sm"
          />
        </div>

        <!-- Customer List -->
        <div v-if="loading" class="flex items-center justify-center py-8">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="filteredCustomers.length === 0" class="text-center py-6 text-sm text-gray-400">
          {{ $t('pos.noCustomersFound') }}
        </div>
        <div v-else class="max-h-72 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100">
          <button
            v-for="customer in filteredCustomers"
            :key="customer.id"
            @click="emit('select', customer)"
            class="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-primary-50 active:bg-primary-100 transition-colors"
          >
            <div class="h-9 w-9 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
              <span class="text-sm font-bold text-primary-700">{{ (customer.name || '?')[0] }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-sm text-gray-900 truncate">{{ customer.name }}</p>
              <p class="text-xs text-gray-400">{{ customer.code }} {{ customer.phone ? '· ' + customer.phone : '' }}</p>
            </div>
            <div v-if="customer.currentBalance > 0" class="text-xs font-medium text-red-600 flex-shrink-0">
              {{ formatCurrency(customer.currentBalance) }}
            </div>
          </button>
        </div>

        <div class="flex gap-2 mt-4">
          <button @click="emit('close')" class="btn-secondary flex-1">
            {{ $t('cancel') }}
          </button>
          <button @click="emit('new-customer')" class="btn-primary flex-1">
            <PlusIcon class="h-4 w-4 mr-1" />
            {{ $t('pos.quickAdd') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
