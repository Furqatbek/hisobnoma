<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deliveryRegionsApi, unwrapData } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const errors = reactive({})

const form = reactive({
  name: '',
  code: '',
  description: '',
  active: true,
  sortOrder: 0,
  deliveryFee: 0
})

async function fetchRegion() {
  loading.value = true
  try {
    const response = await deliveryRegionsApi.getById(route.params.id)
    const region = unwrapData(response)
    Object.assign(form, {
      name: region.name || '',
      code: region.code || '',
      description: region.description || '',
      active: region.active !== false,
      sortOrder: region.sortOrder || 0,
      deliveryFee: region.deliveryFee || 0
    })
  } catch (error) {
    console.error('Failed to fetch region:', error)
    toast.error(t('admin.regions.loadError'))
    router.push('/admin/regions')
  } finally {
    loading.value = false
  }
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) errors.name = t('admin.regionForm.nameRequired')
  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  try {
    if (isEdit.value) {
      await deliveryRegionsApi.update(route.params.id, form)
    } else {
      await deliveryRegionsApi.create(form)
    }
    router.push('/admin/regions')
  } catch (error) {
    const msg = error.response?.data?.message || t('admin.regionForm.saveError')
    toast.error(msg)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    fetchRegion()
  }
})
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <!-- Header -->
    <div class="flex items-center mb-6">
      <router-link to="/admin/regions" class="mr-4 p-2 rounded-lg hover:bg-gray-100">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </router-link>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? $t('admin.regionForm.editRegion') : $t('admin.regionForm.newRegion') }}
        </h1>
      </div>
    </div>

    <div v-if="loading" class="card p-8 text-center text-gray-500">{{ $t('loading') }}</div>

    <form v-else @submit.prevent="handleSubmit" class="card">
      <div class="card-body space-y-6">
        <!-- Name -->
        <div>
          <label class="label">{{ $t('admin.regionForm.regionName') }} *</label>
          <input v-model="form.name" type="text" class="input" :placeholder="$t('admin.regionForm.regionNamePlaceholder')" />
          <p v-if="errors.name" class="text-sm text-red-600 mt-1">{{ errors.name }}</p>
        </div>

        <!-- Code -->
        <div>
          <label class="label">{{ $t('admin.regionForm.regionCode') }}</label>
          <input v-model="form.code" type="text" class="input" :placeholder="$t('admin.regionForm.regionCodePlaceholder')" />
        </div>

        <!-- Description -->
        <div>
          <label class="label">{{ $t('admin.regionForm.regionDescription') }}</label>
          <textarea v-model="form.description" class="input" rows="3" :placeholder="$t('admin.regionForm.regionDescriptionPlaceholder')"></textarea>
        </div>

        <!-- Sort Order -->
        <div>
          <label class="label">{{ $t('admin.regionForm.sortOrder') }}</label>
          <input v-model.number="form.sortOrder" type="number" class="input" min="0" />
        </div>

        <div>
          <label class="label">{{ $t('admin.regionForm.deliveryFee') }}</label>
          <input v-model.number="form.deliveryFee" type="number" class="input" min="0" :placeholder="'0'" />
          <p class="mt-1 text-xs text-gray-500">{{ $t('admin.regionForm.deliveryFeeHint') }}</p>
        </div>

        <!-- Active -->
        <div class="flex items-center">
          <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 border-gray-300 rounded" />
          <label class="ml-2 text-sm text-gray-700">{{ $t('active') }}</label>
        </div>
      </div>

      <!-- Actions -->
      <div class="card-footer flex justify-end space-x-3">
        <router-link to="/admin/regions" class="btn-secondary">
          {{ $t('cancel') }}
        </router-link>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : (isEdit ? $t('save') : $t('admin.regionForm.create')) }}
        </button>
      </div>
    </form>
  </div>
</template>
