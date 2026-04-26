<script setup>
import { ref, computed, onMounted } from 'vue'
import { tenantSettingsApi } from '@/services/api'
import {
  PlusIcon, PencilSquareIcon, XMarkIcon,
  EyeSlashIcon, CheckIcon, ArrowPathIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const allSettings = ref([])
const settingsMap = ref({})
const categories = ref([])
const activeCategory = ref('')
const loading = ref(true)
const saving = ref(false)

// Batch update
const modifiedSettings = ref(new Map())
const batchSaving = ref(false)
const batchSuccess = ref(false)

const showModal = ref(false)
const editMode = ref(false)
const modalForm = ref(createEmptyForm())
const modalErrors = ref({})

const editingKey = ref(null)
const editingValue = ref('')

function createEmptyForm() {
  return {
    settingKey: '',
    settingValue: '',
    description: '',
    category: '',
    valueType: 'STRING',
    sensitive: false,
    active: true
  }
}

async function fetchAll() {
  loading.value = true
  try {
    const [settingsRes, catsRes, mapRes] = await Promise.all([
      tenantSettingsApi.getAll(),
      tenantSettingsApi.getCategories(),
      tenantSettingsApi.getMap()
    ])
    allSettings.value = settingsRes.data.data || settingsRes.data || []
    categories.value = catsRes.data.data || catsRes.data || []
    settingsMap.value = mapRes.data.data || mapRes.data || {}
    if (!activeCategory.value && categories.value.length > 0) {
      activeCategory.value = categories.value[0]
    }
  } catch (error) {
    console.error('Failed to fetch tenant settings:', error)
  } finally {
    loading.value = false
  }
}

async function fetchByCategory(category) {
  if (!category) {
    await fetchAll()
    return
  }
  loading.value = true
  try {
    const response = await tenantSettingsApi.getByCategory(category)
    const data = response.data.data || response.data || []
    allSettings.value = allSettings.value.filter(s => s.category !== category).concat(data)
    activeCategory.value = category
  } catch (error) {
    console.error('Failed to fetch category settings:', error)
  } finally {
    loading.value = false
  }
}

function trackModification(settingKey, newValue) {
  modifiedSettings.value.set(settingKey, newValue)
  batchSuccess.value = false
}

async function batchSaveAll() {
  if (modifiedSettings.value.size === 0) return
  batchSaving.value = true
  batchSuccess.value = false
  try {
    const settings = {}
    modifiedSettings.value.forEach((value, key) => {
      settings[key] = value
    })
    await tenantSettingsApi.batchUpdate(settings)
    modifiedSettings.value.clear()
    batchSuccess.value = true
    await fetchAll()
    setTimeout(() => { batchSuccess.value = false }, 3000)
  } catch (error) {
    console.error('Batch update failed:', error)
    alert(error.response?.data?.message || t('admin.tenantSettings.batchSaveError'))
  } finally {
    batchSaving.value = false
  }
}

onMounted(fetchAll)

const filteredSettings = computed(() => {
  if (!activeCategory.value) return allSettings.value
  return allSettings.value.filter(s => s.category === activeCategory.value)
})

const uncategorized = computed(() => allSettings.value.filter(s => !s.category))

function openCreateModal() {
  editMode.value = false
  modalForm.value = createEmptyForm()
  modalForm.value.category = activeCategory.value || ''
  modalErrors.value = {}
  showModal.value = true
}

function openEditModal(setting) {
  editMode.value = true
  modalForm.value = { ...setting }
  modalErrors.value = {}
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function submitModal() {
  modalErrors.value = {}
  if (!modalForm.value.settingKey.trim()) {
    modalErrors.value.settingKey = t('admin.tenantSettings.keyRequired')
    return
  }

  saving.value = true
  try {
    if (editMode.value) {
      await tenantSettingsApi.update(modalForm.value.settingKey, modalForm.value)
    } else {
      await tenantSettingsApi.create(modalForm.value)
    }
    closeModal()
    fetchAll()
  } catch (error) {
    modalErrors.value.api = error.response?.data?.message || t('admin.tenantSettings.saveError')
  } finally {
    saving.value = false
  }
}

function startInlineEdit(setting) {
  editingKey.value = setting.settingKey
  editingValue.value = setting.sensitive ? '' : (setting.settingValue ?? '')
}

function cancelInlineEdit() {
  editingKey.value = null
  editingValue.value = ''
}

async function saveInlineEdit(setting) {
  try {
    await tenantSettingsApi.updateValue(setting.settingKey, editingValue.value)
    editingKey.value = null
    modifiedSettings.value.delete(setting.settingKey)
    fetchAll()
  } catch (error) {
    console.error('Failed to update value:', error)
  }
}

function valueTypeLabel(type) {
  const labels = {
    STRING: 'String', INTEGER: 'Integer', DECIMAL: 'Decimal',
    BOOLEAN: 'Boolean', JSON: 'JSON', ENUM: 'Enum',
    DATE: 'Date', DATETIME: 'DateTime'
  }
  return labels[type] || type
}

function valueTypeClass(type) {
  const cls = {
    STRING: 'bg-gray-100 text-gray-600',
    INTEGER: 'bg-blue-100 text-blue-700',
    DECIMAL: 'bg-blue-100 text-blue-700',
    BOOLEAN: 'bg-green-100 text-green-700',
    JSON: 'bg-purple-100 text-purple-700',
    ENUM: 'bg-amber-100 text-amber-700',
    DATE: 'bg-indigo-100 text-indigo-700',
    DATETIME: 'bg-indigo-100 text-indigo-700'
  }
  return cls[type] || 'bg-gray-100 text-gray-600'
}

function displayValue(setting) {
  if (setting.sensitive) return '••••••••'
  return setting.settingValue ?? ''
}

const valueTypes = ['STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON', 'ENUM', 'DATE', 'DATETIME']
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.tenantSettings.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.tenantSettings.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <button v-if="modifiedSettings.size > 0 || batchSuccess" @click="batchSaveAll" :disabled="batchSaving || modifiedSettings.size === 0" class="btn btn-primary">
          <span v-if="batchSaving" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
          <ArrowPathIcon v-else class="h-5 w-5 mr-2" />
          <template v-if="batchSuccess">{{ $t('admin.tenantSettings.batchSaved') }}</template>
          <template v-else>{{ $t('admin.tenantSettings.batchSave') }} ({{ modifiedSettings.size }})</template>
        </button>
        <button @click="openCreateModal" class="btn btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('admin.tenantSettings.addSetting') }}
        </button>
      </div>
    </div>

    <!-- Category Tabs -->
    <div v-if="categories.length > 0" class="border-b border-gray-200">
      <nav class="flex gap-1 overflow-x-auto pb-px">
        <button
          v-for="cat in categories" :key="cat"
          @click="fetchByCategory(cat)"
          :class="['px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
            activeCategory === cat
              ? 'border-primary-500 text-primary-600 bg-primary-50/50'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
        >
          {{ cat }}
          <span class="ml-1.5 text-xs px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-500">
            {{ allSettings.filter(s => s.category === cat).length }}
          </span>
        </button>
        <button
          v-if="uncategorized.length > 0"
          @click="activeCategory = ''"
          :class="['px-4 py-2.5 text-sm font-medium rounded-t-lg border-b-2 transition-colors whitespace-nowrap',
            activeCategory === ''
              ? 'border-primary-500 text-primary-600 bg-primary-50/50'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50']"
        >
          {{ $t('admin.tenantSettings.uncategorized') }}
          <span class="ml-1.5 text-xs px-1.5 py-0.5 rounded-full bg-gray-100 text-gray-500">
            {{ uncategorized.length }}
          </span>
        </button>
      </nav>
    </div>

    <!-- Settings List -->
    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="filteredSettings.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('admin.tenantSettings.noSettings') }}</p>
      </div>

      <div v-else class="divide-y divide-gray-100">
        <div
          v-for="setting in filteredSettings" :key="setting.settingKey"
          :class="['px-6 py-4 hover:bg-gray-50/50 transition-colors', { 'opacity-60': !setting.active }]"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <!-- Key + badges -->
              <div class="flex items-center gap-2 flex-wrap">
                <code class="text-sm font-semibold text-gray-900 bg-gray-100 px-2 py-0.5 rounded">
                  {{ setting.settingKey }}
                </code>
                <span :class="['text-xs px-1.5 py-0.5 rounded-full', valueTypeClass(setting.valueType)]">
                  {{ valueTypeLabel(setting.valueType) }}
                </span>
                <span v-if="setting.sensitive" class="flex items-center gap-0.5 text-xs text-amber-500">
                  <EyeSlashIcon class="h-3 w-3" /> {{ $t('admin.tenantSettings.sensitiveBadge') }}
                </span>
              </div>

              <!-- Description -->
              <p v-if="setting.description" class="text-sm text-gray-500 mt-1">{{ setting.description }}</p>

              <!-- Value -->
              <div class="mt-2">
                <!-- Inline editing -->
                <div v-if="editingKey === setting.settingKey" class="flex items-center gap-2">
                  <template v-if="setting.valueType === 'BOOLEAN'">
                    <select v-model="editingValue" class="input input-sm w-32">
                      <option value="true">true</option>
                      <option value="false">false</option>
                    </select>
                  </template>
                  <template v-else-if="setting.valueType === 'INTEGER' || setting.valueType === 'DECIMAL'">
                    <input
                      v-model="editingValue"
                      type="number"
                      :step="setting.valueType === 'DECIMAL' ? '0.01' : '1'"
                      class="input input-sm w-48"
                      @keyup.enter="saveInlineEdit(setting)"
                      @keyup.escape="cancelInlineEdit"
                    />
                  </template>
                  <template v-else>
                    <input
                      v-model="editingValue"
                      type="text"
                      class="input input-sm flex-1 max-w-md"
                      @keyup.enter="saveInlineEdit(setting)"
                      @keyup.escape="cancelInlineEdit"
                    />
                  </template>
                  <button @click="saveInlineEdit(setting)" class="p-1 text-green-600 hover:bg-green-50 rounded">
                    <CheckIcon class="h-5 w-5" />
                  </button>
                  <button @click="cancelInlineEdit" class="p-1 text-gray-400 hover:bg-gray-100 rounded">
                    <XMarkIcon class="h-5 w-5" />
                  </button>
                </div>
                <!-- Display value -->
                <div v-else>
                  <button
                    @click="startInlineEdit(setting)"
                    class="text-sm font-mono px-2 py-0.5 rounded border border-transparent hover:border-gray-300 hover:bg-white transition-colors text-left"
                    :title="$t('admin.tenantSettings.clickToEdit')"
                  >
                    <span :class="[setting.sensitive ? 'text-gray-400 tracking-wider' : 'text-gray-700']">
                      {{ displayValue(setting) || '-' }}
                    </span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center gap-1 flex-shrink-0">
              <button
                @click="openEditModal(setting)"
                class="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded-lg transition-colors"
                :title="$t('edit')"
              >
                <PencilSquareIcon class="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-black/30" @click="closeModal"></div>
          <div class="relative bg-white rounded-xl shadow-xl w-full max-w-lg p-6 space-y-5 max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold text-gray-900">
                {{ editMode ? $t('admin.tenantSettings.editSetting') : $t('admin.tenantSettings.newSetting') }}
              </h3>
              <button @click="closeModal" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg">
                <XMarkIcon class="h-5 w-5" />
              </button>
            </div>

            <div v-if="modalErrors.api" class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
              {{ modalErrors.api }}
            </div>

            <div class="space-y-4">
              <!-- Key -->
              <div>
                <label class="label">{{ $t('admin.tenantSettings.key') }} <span class="text-red-500">*</span></label>
                <input
                  v-model="modalForm.settingKey"
                  type="text"
                  class="input font-mono"
                  :disabled="editMode"
                  :class="{ 'bg-gray-50': editMode, 'border-red-500': modalErrors.settingKey }"
                  placeholder="app.feature.enabled"
                />
                <p v-if="modalErrors.settingKey" class="text-sm text-red-500 mt-1">{{ modalErrors.settingKey }}</p>
              </div>

              <!-- Category + Value Type -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('admin.tenantSettings.category') }}</label>
                  <input
                    v-model="modalForm.category"
                    type="text"
                    class="input"
                    :placeholder="$t('admin.tenantSettings.categoryPlaceholder')"
                    list="tenant-category-suggestions"
                  />
                  <datalist id="tenant-category-suggestions">
                    <option v-for="cat in categories" :key="cat" :value="cat" />
                  </datalist>
                </div>
                <div>
                  <label class="label">{{ $t('admin.tenantSettings.valueType') }}</label>
                  <select v-model="modalForm.valueType" class="input">
                    <option v-for="vt in valueTypes" :key="vt" :value="vt">{{ valueTypeLabel(vt) }}</option>
                  </select>
                </div>
              </div>

              <!-- Value -->
              <div>
                <label class="label">{{ $t('admin.tenantSettings.value') }}</label>
                <input v-model="modalForm.settingValue" type="text" class="input font-mono" />
              </div>

              <!-- Description -->
              <div>
                <label class="label">{{ $t('description') }}</label>
                <textarea
                  v-model="modalForm.description"
                  rows="2"
                  class="input"
                  :placeholder="$t('admin.tenantSettings.descriptionPlaceholder')"
                ></textarea>
              </div>

              <!-- Flags -->
              <div class="flex flex-wrap gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="modalForm.sensitive" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('admin.tenantSettings.sensitive') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="modalForm.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('active') }}</span>
                </label>
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button @click="closeModal" class="btn btn-secondary">{{ $t('cancel') }}</button>
              <button @click="submitModal" :disabled="saving" class="btn btn-primary">
                <span v-if="saving" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ editMode ? $t('save') : $t('admin.tenantSettings.createSetting') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
