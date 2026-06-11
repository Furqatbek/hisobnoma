<script setup>
import { useToastStore } from '@/stores/toast'
import { ref, computed, onMounted } from 'vue'
import { systemSettingsApi } from '@/services/api'
import {
  PlusIcon, PencilSquareIcon, TrashIcon, XMarkIcon,
  EyeSlashIcon, LockClosedIcon, CheckIcon, MagnifyingGlassIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const toast = useToastStore()

const { t } = useI18n()

const allSettings = ref([])
const categories = ref([])
const activeCategory = ref('')
const loading = ref(true)
const saving = ref(false)

// Edit modal
const showModal = ref(false)
const editMode = ref(false)
const modalForm = ref(createEmptyForm())
const modalErrors = ref({})

// Key lookup
const keyLookup = ref('')
const keyLookupLoading = ref(false)
const keyLookupResult = ref(null)
const keyLookupError = ref('')

// Batch update
const modifiedSettings = ref(new Map())
const batchSaving = ref(false)
const batchSuccess = ref(false)

// Inline edit
const editingKey = ref(null)
const editingValue = ref('')

function createEmptyForm() {
  return {
    settingKey: '',
    settingValue: '',
    defaultValue: '',
    description: '',
    category: '',
    valueType: 'STRING',
    sensitive: false,
    readonly: false,
    active: true,
    validationRegex: '',
    minValue: null,
    maxValue: null,
    allowedValues: '',
    sortOrder: 0
  }
}

async function fetchAll() {
  loading.value = true
  try {
    const [settingsRes, catsRes] = await Promise.all([
      systemSettingsApi.getAll(),
      systemSettingsApi.getCategories()
    ])
    allSettings.value = settingsRes.data.data || settingsRes.data || []
    categories.value = catsRes.data.data || catsRes.data || []
    if (!activeCategory.value && categories.value.length > 0) {
      activeCategory.value = categories.value[0]
    }
  } catch (error) {
    console.error('Failed to fetch settings:', error)
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
    const response = await systemSettingsApi.getByCategory(category)
    const data = response.data.data || response.data || []
    // Replace only the settings for this category in the full list
    allSettings.value = allSettings.value.filter(s => s.category !== category).concat(data)
    activeCategory.value = category
  } catch (error) {
    console.error('Failed to fetch category settings:', error)
  } finally {
    loading.value = false
  }
}

async function lookupByKey() {
  if (!keyLookup.value.trim()) return
  keyLookupLoading.value = true
  keyLookupError.value = ''
  keyLookupResult.value = null
  try {
    const response = await systemSettingsApi.getByKey(keyLookup.value.trim())
    keyLookupResult.value = response.data.data || response.data
  } catch (error) {
    keyLookupError.value = error.response?.status === 404
      ? t('admin.systemSettings.keyNotFound')
      : (error.response?.data?.message || t('admin.systemSettings.keyLookupError'))
  } finally {
    keyLookupLoading.value = false
  }
}

function clearKeyLookup() {
  keyLookup.value = ''
  keyLookupResult.value = null
  keyLookupError.value = ''
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
    await systemSettingsApi.batchUpdate(settings)
    modifiedSettings.value.clear()
    batchSuccess.value = true
    await fetchAll()
    setTimeout(() => { batchSuccess.value = false }, 3000)
  } catch (error) {
    console.error('Batch update failed:', error)
    toast.error(error.response?.data?.message || t('admin.systemSettings.batchSaveError'))
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

// Create / Edit modal
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
    modalErrors.value.settingKey = t('admin.systemSettings.keyRequired')
    return
  }

  saving.value = true
  try {
    if (editMode.value) {
      await systemSettingsApi.update(modalForm.value.settingKey, modalForm.value)
    } else {
      await systemSettingsApi.create(modalForm.value)
    }
    closeModal()
    fetchAll()
  } catch (error) {
    modalErrors.value.api = error.response?.data?.message || t('admin.systemSettings.saveError')
  } finally {
    saving.value = false
  }
}

// Inline value edit
function startInlineEdit(setting) {
  if (setting.readonly) return
  editingKey.value = setting.settingKey
  editingValue.value = setting.sensitive ? '' : (setting.settingValue ?? setting.defaultValue ?? '')
}

function cancelInlineEdit() {
  editingKey.value = null
  editingValue.value = ''
}

async function saveInlineEdit(setting) {
  try {
    await systemSettingsApi.updateValue(setting.settingKey, editingValue.value)
    editingKey.value = null
    modifiedSettings.value.delete(setting.settingKey)
    fetchAll()
  } catch (error) {
    console.error('Failed to update value:', error)
  }
}

async function deleteSetting(setting) {
  if (setting.readonly) return
  if (!confirm(t('admin.systemSettings.confirmDelete', { key: setting.settingKey }))) return
  try {
    await systemSettingsApi.delete(setting.settingKey)
    fetchAll()
  } catch (error) {
    console.error('Failed to delete setting:', error)
  }
}

function valueTypeLabel(type) {
  const labels = {
    STRING: 'String',
    INTEGER: 'Integer',
    DECIMAL: 'Decimal',
    BOOLEAN: 'Boolean',
    JSON: 'JSON',
    ENUM: 'Enum',
    DATE: 'Date',
    DATETIME: 'DateTime'
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
  return setting.settingValue ?? setting.defaultValue ?? ''
}

const valueTypes = ['STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'JSON', 'ENUM', 'DATE', 'DATETIME']
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('admin.systemSettings.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('admin.systemSettings.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <button v-if="modifiedSettings.size > 0 || batchSuccess" @click="batchSaveAll" :disabled="batchSaving || modifiedSettings.size === 0" class="btn btn-primary">
          <span v-if="batchSaving" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
          <ArrowPathIcon v-else class="h-5 w-5 mr-2" />
          <template v-if="batchSuccess">{{ $t('admin.systemSettings.batchSaved') }}</template>
          <template v-else>{{ $t('admin.systemSettings.batchSave') }} ({{ modifiedSettings.size }})</template>
        </button>
        <button @click="openCreateModal" class="btn btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" />
          {{ $t('admin.systemSettings.addSetting') }}
        </button>
      </div>
    </div>

    <!-- Key Lookup -->
    <div class="card">
      <div class="card-body">
        <div class="flex items-center gap-3">
          <div class="flex-1 relative">
            <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              v-model="keyLookup"
              type="text"
              :placeholder="$t('admin.systemSettings.keyLookupPlaceholder')"
              class="input pl-10 font-mono"
              @keyup.enter="lookupByKey"
            />
          </div>
          <button @click="lookupByKey" :disabled="keyLookupLoading || !keyLookup.trim()" class="btn btn-secondary">
            {{ keyLookupLoading ? '...' : $t('admin.systemSettings.lookupKey') }}
          </button>
          <button v-if="keyLookupResult || keyLookupError" @click="clearKeyLookup" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>
        <!-- Key Lookup Result -->
        <div v-if="keyLookupResult" class="mt-3 p-4 bg-primary-50 border border-primary-200 rounded-lg">
          <div class="flex items-center gap-2 flex-wrap">
            <code class="text-sm font-semibold text-gray-900 bg-white px-2 py-0.5 rounded">{{ keyLookupResult.settingKey }}</code>
            <span :class="['text-xs px-1.5 py-0.5 rounded-full', valueTypeClass(keyLookupResult.valueType)]">{{ valueTypeLabel(keyLookupResult.valueType) }}</span>
            <span v-if="keyLookupResult.category" class="text-xs text-gray-500">{{ keyLookupResult.category }}</span>
          </div>
          <p v-if="keyLookupResult.description" class="text-sm text-gray-500 mt-1">{{ keyLookupResult.description }}</p>
          <div class="mt-2 text-sm font-mono text-gray-700">
            {{ keyLookupResult.sensitive ? '••••••••' : (keyLookupResult.settingValue ?? keyLookupResult.defaultValue ?? '-') }}
          </div>
          <div class="mt-2 flex gap-2">
            <button @click="openEditModal(keyLookupResult)" class="text-sm text-primary-600 hover:text-primary-800">{{ $t('edit') }}</button>
          </div>
        </div>
        <div v-if="keyLookupError" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
          {{ keyLookupError }}
        </div>
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
          {{ $t('admin.systemSettings.uncategorized') }}
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
        <p class="text-gray-500">{{ $t('admin.systemSettings.noSettings') }}</p>
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
                <span v-if="setting.readonly" class="flex items-center gap-0.5 text-xs text-gray-400">
                  <LockClosedIcon class="h-3 w-3" /> {{ $t('admin.systemSettings.readonlyBadge') }}
                </span>
                <span v-if="setting.sensitive" class="flex items-center gap-0.5 text-xs text-amber-500">
                  <EyeSlashIcon class="h-3 w-3" /> {{ $t('admin.systemSettings.sensitiveBadge') }}
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
                  <template v-else-if="setting.valueType === 'ENUM' && setting.allowedValues">
                    <select v-model="editingValue" class="input input-sm w-48">
                      <option v-for="v in setting.allowedValues.split(',')" :key="v" :value="v.trim()">{{ v.trim() }}</option>
                    </select>
                  </template>
                  <template v-else-if="setting.valueType === 'INTEGER' || setting.valueType === 'DECIMAL'">
                    <input
                      v-model="editingValue"
                      type="number"
                      :step="setting.valueType === 'DECIMAL' ? '0.01' : '1'"
                      :min="setting.minValue"
                      :max="setting.maxValue"
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
                      :placeholder="setting.defaultValue || ''"
                      @keyup.enter="saveInlineEdit(setting)"
                      @keyup.escape="cancelInlineEdit"
                    />
                  </template>
                  <button @click="saveInlineEdit(setting)" class="p-1 text-green-600 hover:bg-green-50 rounded" :title="$t('save')">
                    <CheckIcon class="h-5 w-5" />
                  </button>
                  <button @click="trackModification(setting.settingKey, editingValue); cancelInlineEdit()" class="p-1 text-primary-500 hover:bg-primary-50 rounded" :title="$t('admin.systemSettings.batchTrack')">
                    <ArrowPathIcon class="h-5 w-5" />
                  </button>
                  <button @click="cancelInlineEdit" class="p-1 text-gray-400 hover:bg-gray-100 rounded">
                    <XMarkIcon class="h-5 w-5" />
                  </button>
                </div>
                <!-- Display value -->
                <div v-else class="flex items-center gap-2">
                  <button
                    v-if="!setting.readonly"
                    @click="startInlineEdit(setting)"
                    class="text-sm font-mono px-2 py-0.5 rounded border border-transparent hover:border-gray-300 hover:bg-white transition-colors text-left"
                    :title="$t('admin.systemSettings.clickToEdit')"
                  >
                    <span :class="[setting.sensitive ? 'text-gray-400 tracking-wider' : 'text-gray-700']">
                      {{ displayValue(setting) || '-' }}
                    </span>
                  </button>
                  <span v-else class="text-sm font-mono text-gray-500 px-2 py-0.5">
                    {{ displayValue(setting) || '-' }}
                  </span>
                  <span v-if="setting.defaultValue && setting.settingValue !== setting.defaultValue" class="text-xs text-gray-400">
                    ({{ $t('admin.systemSettings.default') }}: {{ setting.sensitive ? '••••' : setting.defaultValue }})
                  </span>
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
              <button
                v-if="!setting.readonly"
                @click="deleteSetting(setting)"
                class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-gray-100 rounded-lg transition-colors"
                :title="$t('delete')"
              >
                <TrashIcon class="h-5 w-5" />
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
                {{ editMode ? $t('admin.systemSettings.editSetting') : $t('admin.systemSettings.newSetting') }}
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
                <label class="label">{{ $t('admin.systemSettings.key') }} <span class="text-red-500">*</span></label>
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
                  <label class="label">{{ $t('admin.systemSettings.category') }}</label>
                  <input
                    v-model="modalForm.category"
                    type="text"
                    class="input"
                    :placeholder="$t('admin.systemSettings.categoryPlaceholder')"
                    list="category-suggestions"
                  />
                  <datalist id="category-suggestions">
                    <option v-for="cat in categories" :key="cat" :value="cat" />
                  </datalist>
                </div>
                <div>
                  <label class="label">{{ $t('admin.systemSettings.valueType') }}</label>
                  <select v-model="modalForm.valueType" class="input">
                    <option v-for="vt in valueTypes" :key="vt" :value="vt">{{ valueTypeLabel(vt) }}</option>
                  </select>
                </div>
              </div>

              <!-- Value + Default -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('admin.systemSettings.value') }}</label>
                  <input v-model="modalForm.settingValue" type="text" class="input font-mono" />
                </div>
                <div>
                  <label class="label">{{ $t('admin.systemSettings.defaultValue') }}</label>
                  <input v-model="modalForm.defaultValue" type="text" class="input font-mono" />
                </div>
              </div>

              <!-- Description -->
              <div>
                <label class="label">{{ $t('description') }}</label>
                <textarea
                  v-model="modalForm.description"
                  rows="2"
                  class="input"
                  :placeholder="$t('admin.systemSettings.descriptionPlaceholder')"
                ></textarea>
              </div>

              <!-- Allowed values (for ENUM) -->
              <div v-if="modalForm.valueType === 'ENUM'">
                <label class="label">{{ $t('admin.systemSettings.allowedValues') }}</label>
                <input
                  v-model="modalForm.allowedValues"
                  type="text"
                  class="input font-mono"
                  placeholder="value1, value2, value3"
                />
              </div>

              <!-- Min/Max (for INTEGER/DECIMAL) -->
              <div v-if="modalForm.valueType === 'INTEGER' || modalForm.valueType === 'DECIMAL'" class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('admin.systemSettings.minValue') }}</label>
                  <input v-model.number="modalForm.minValue" type="number" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('admin.systemSettings.maxValue') }}</label>
                  <input v-model.number="modalForm.maxValue" type="number" class="input" />
                </div>
              </div>

              <!-- Validation regex -->
              <div>
                <label class="label">{{ $t('admin.systemSettings.validationRegex') }}</label>
                <input v-model="modalForm.validationRegex" type="text" class="input font-mono text-sm" placeholder="^[a-zA-Z0-9]+$" />
              </div>

              <!-- Flags -->
              <div class="flex flex-wrap gap-6">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="modalForm.sensitive" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('admin.systemSettings.sensitive') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="modalForm.readonly" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('admin.systemSettings.readonlyFlag') }}</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input v-model="modalForm.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded border-gray-300" />
                  <span class="text-sm text-gray-700">{{ $t('active') }}</span>
                </label>
              </div>

              <!-- Sort order -->
              <div class="w-32">
                <label class="label">{{ $t('admin.systemSettings.sortOrder') }}</label>
                <input v-model.number="modalForm.sortOrder" type="number" class="input" min="0" />
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button @click="closeModal" class="btn btn-secondary">{{ $t('cancel') }}</button>
              <button @click="submitModal" :disabled="saving" class="btn btn-primary">
                <span v-if="saving" class="animate-spin inline-block h-4 w-4 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                {{ editMode ? $t('save') : $t('admin.systemSettings.createSetting') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
