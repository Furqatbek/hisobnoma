<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deliveryRegionsApi, deliveryVillagesApi, unwrapData, unwrapList } from '@/services/api'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const regions = ref([])
const errors = reactive({})

const form = reactive({
  regionId: null,
  name: '',
  code: '',
  description: '',
  active: true,
  sortOrder: 0
})

async function fetchRegions() {
  try {
    const response = await deliveryRegionsApi.getActive()
    regions.value = unwrapList(response)
  } catch (error) {
    console.error('Failed to fetch regions:', error)
  }
}

async function fetchVillage() {
  loading.value = true
  try {
    const response = await deliveryVillagesApi.getById(route.params.id)
    const village = unwrapData(response)
    Object.assign(form, {
      regionId: village.regionId,
      name: village.name || '',
      code: village.code || '',
      description: village.description || '',
      active: village.active !== false,
      sortOrder: village.sortOrder || 0
    })
  } catch (error) {
    console.error('Failed to fetch village:', error)
    toast.error(t('admin.villageForm.loadError'))
    router.push('/admin/villages')
  } finally {
    loading.value = false
  }
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) errors.name = t('admin.villageForm.nameRequired')
  if (!form.regionId) errors.regionId = t('admin.villageForm.regionRequired')
  return Object.keys(errors).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  saving.value = true
  try {
    if (isEdit.value) {
      await deliveryVillagesApi.update(route.params.id, form)
    } else {
      await deliveryVillagesApi.create(form)
    }
    router.push('/admin/villages')
  } catch (error) {
    const msg = error.response?.data?.message || t('admin.villageForm.saveError')
    toast.error(msg)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fetchRegions()
  if (isEdit.value) {
    await fetchVillage()
  }
})
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <!-- Header -->
    <div class="flex items-center mb-6">
      <router-link to="/admin/villages" class="mr-4 p-2 rounded-lg hover:bg-gray-100">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </router-link>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? $t('admin.villageForm.editVillage') : $t('admin.villageForm.newVillage') }}
        </h1>
      </div>
    </div>

    <div v-if="loading" class="card p-8 text-center text-gray-500">{{ $t('loading') }}</div>

    <form v-else @submit.prevent="handleSubmit" class="card">
      <div class="card-body space-y-6">
        <!-- Region -->
        <div>
          <label class="label">{{ $t('admin.villages.region') }} *</label>
          <select v-model="form.regionId" class="input">
            <option :value="null" disabled>{{ $t('admin.villageForm.selectRegion') }}</option>
            <option v-for="region in regions" :key="region.id" :value="region.id">
              {{ region.name }}
            </option>
          </select>
          <p v-if="errors.regionId" class="text-sm text-red-600 mt-1">{{ errors.regionId }}</p>
          <p v-if="regions.length === 0" class="text-sm text-orange-600 mt-1">
            {{ $t('admin.villageForm.createRegionFirst') }}: <router-link to="/admin/regions/new" class="underline">{{ $t('admin.villageForm.addRegion') }}</router-link>
          </p>
        </div>

        <!-- Name -->
        <div>
          <label class="label">{{ $t('name') }} *</label>
          <input v-model="form.name" type="text" class="input" :placeholder="$t('admin.villageForm.namePlaceholder')" />
          <p v-if="errors.name" class="text-sm text-red-600 mt-1">{{ errors.name }}</p>
        </div>

        <!-- Code -->
        <div>
          <label class="label">{{ $t('code') }}</label>
          <input v-model="form.code" type="text" class="input" :placeholder="$t('admin.villageForm.codePlaceholder')" />
        </div>

        <!-- Description -->
        <div>
          <label class="label">{{ $t('description') }}</label>
          <textarea v-model="form.description" class="input" rows="3" :placeholder="$t('admin.villageForm.descriptionPlaceholder')"></textarea>
        </div>

        <!-- Sort Order -->
        <div>
          <label class="label">{{ $t('admin.villageForm.sortOrder') }}</label>
          <input v-model.number="form.sortOrder" type="number" class="input" min="0" />
        </div>

        <!-- Active -->
        <div class="flex items-center">
          <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 border-gray-300 rounded" />
          <label class="ml-2 text-sm text-gray-700">{{ $t('active') }}</label>
        </div>
      </div>

      <!-- Actions -->
      <div class="card-footer flex justify-end space-x-3">
        <router-link to="/admin/villages" class="btn-secondary">
          {{ $t('cancel') }}
        </router-link>
        <button type="submit" :disabled="saving" class="btn-primary">
          {{ saving ? $t('saving') : (isEdit ? $t('save') : $t('admin.villageForm.create')) }}
        </button>
      </div>
    </form>
  </div>
</template>
