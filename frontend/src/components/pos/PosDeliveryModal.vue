<script setup>
import { MapPinIcon, CheckIcon } from '@heroicons/vue/24/outline'

defineProps({
  show: { type: Boolean, default: false },
  regions: { type: Array, default: () => [] },
  villages: { type: Array, default: () => [] }
})

const regionId = defineModel('regionId', { type: [Number, String], default: null })
const villageId = defineModel('villageId', { type: [Number, String], default: null })

const emit = defineEmits(['close', 'confirm', 'region-change'])
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('close')"></div>
      <div class="relative bg-white rounded-lg max-w-md w-full p-6">
        <h3 class="text-lg font-medium text-gray-900 mb-4">
          <MapPinIcon class="h-5 w-5 inline mr-2 text-orange-500" />
          {{ $t('pos.deliveryAddress') }}
        </h3>

        <div class="space-y-4">
          <!-- Region -->
          <div>
            <label class="label">{{ $t('customers.form.region') }} *</label>
            <select v-model="regionId" @change="emit('region-change')" class="input">
              <option :value="null">{{ $t('customers.form.selectRegion') }}</option>
              <option v-for="region in regions" :key="region.id" :value="region.id">
                {{ region.name }}
              </option>
            </select>
          </div>

          <!-- Village -->
          <div>
            <label class="label">{{ $t('customers.form.village') }}</label>
            <select v-model="villageId" class="input" :disabled="!regionId">
              <option :value="null">{{ $t('customers.form.selectVillage') }}</option>
              <option v-for="village in villages" :key="village.id" :value="village.id">
                {{ village.name }}
              </option>
            </select>
            <p v-if="regionId && villages.length === 0" class="text-xs text-gray-400 mt-1">
              {{ $t('pos.noVillagesFound') }}
            </p>
          </div>
        </div>

        <div class="flex space-x-3 mt-6">
          <button @click="emit('close')" class="btn-secondary flex-1">
            {{ $t('cancel') }}
          </button>
          <button
            @click="emit('confirm')"
            :disabled="!regionId"
            class="btn-primary flex-1"
          >
            <CheckIcon class="h-5 w-5 mr-2" />
            {{ $t('pos.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
