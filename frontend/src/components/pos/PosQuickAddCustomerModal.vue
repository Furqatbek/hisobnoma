<script setup>
import { reactive, watch } from 'vue'
import { PlusIcon } from '@heroicons/vue/24/outline'

const props = defineProps({
  show: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'create'])

const form = reactive({ name: '', phone: '', email: '' })

watch(() => props.show, (open) => {
  if (open) {
    form.name = ''
    form.phone = ''
    form.email = ''
  }
})
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('close')"></div>
      <div class="relative bg-white rounded-lg max-w-md w-full p-6">
        <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('pos.quickAddCustomer') }}</h3>

        <div class="space-y-4">
          <div>
            <label class="label">{{ $t('name') }} *</label>
            <input v-model="form.name" type="text" class="input" :placeholder="$t('name')" />
          </div>
          <div>
            <label class="label">{{ $t('phone') }}</label>
            <input v-model="form.phone" type="text" class="input" :placeholder="$t('phone')" />
          </div>
          <div>
            <label class="label">{{ $t('customers.form.email') }}</label>
            <input v-model="form.email" type="email" class="input" :placeholder="$t('customers.form.email')" />
          </div>
        </div>

        <div class="flex space-x-3 mt-6">
          <button @click="emit('close')" class="btn-secondary flex-1">
            {{ $t('cancel') }}
          </button>
          <button @click="emit('create', { ...form })" class="btn-primary flex-1">
            <PlusIcon class="h-5 w-5 mr-2" />
            {{ $t('pos.createAndSelect') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
