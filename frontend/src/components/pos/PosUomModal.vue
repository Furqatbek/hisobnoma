<script setup>
const props = defineProps({
  show: { type: Boolean, default: false },
  product: { type: Object, default: null },
  uoms: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'select-base', 'select-uom'])

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value || 0)
}
</script>

<template>
  <div v-if="show && product" class="fixed inset-0 z-50 overflow-y-auto">
    <div class="flex items-center justify-center min-h-screen px-4">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="emit('close')"></div>
      <div class="relative bg-white rounded-lg max-w-sm w-full p-6">
        <h3 class="text-lg font-medium text-gray-900 mb-1">{{ $t('pos.selectUom') }}</h3>
        <p class="text-sm text-gray-500 mb-4">{{ product.name }}</p>

        <div class="space-y-2">
          <!-- Base UOM option -->
          <button
            @click="emit('select-base')"
            class="w-full p-3 text-left rounded-lg border-2 border-gray-200 hover:border-primary-500 hover:bg-primary-50 transition-colors"
          >
            <div class="flex justify-between items-center">
              <div>
                <p class="font-medium text-gray-900">{{ $t('pos.baseUnit') }}</p>
                <p class="text-xs text-gray-500">{{ $t('pos.baseUnitDescription') }}</p>
              </div>
              <span class="font-bold text-primary-600">{{ formatCurrency(product.sellingPrice) }}</span>
            </div>
          </button>

          <!-- Alternate UOM options -->
          <button
            v-for="puom in uoms"
            :key="puom.id"
            @click="emit('select-uom', puom)"
            class="w-full p-3 text-left rounded-lg border-2 transition-colors"
            :class="puom.defaultSale ? 'border-primary-300 bg-primary-50 hover:border-primary-500' : 'border-gray-200 hover:border-primary-500 hover:bg-primary-50'"
          >
            <div class="flex justify-between items-center">
              <div>
                <p class="font-medium text-gray-900">
                  {{ puom.uomName }}
                  <span v-if="puom.defaultSale" class="text-xs text-primary-600">({{ $t('pos.standard') }})</span>
                </p>
                <p class="text-xs text-gray-500">1 {{ puom.uomCode }} = {{ puom.conversionFactor }} {{ $t('pos.baseUnitSuffix') }}</p>
              </div>
              <span class="font-bold text-primary-600">{{ formatCurrency(puom.effectiveSellingPrice) }}</span>
            </div>
          </button>
        </div>

        <button @click="emit('close')" class="btn-secondary w-full mt-4">
          {{ $t('cancel') }}
        </button>
      </div>
    </div>
  </div>
</template>
