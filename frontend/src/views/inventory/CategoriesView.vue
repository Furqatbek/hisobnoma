<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { categoriesApi, unwrapData, unwrapList } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  ChevronDownIcon, ChevronRightIcon, ListBulletIcon, QueueListIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()

const categories = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingCategory = ref(null)

const form = reactive({
  name: '',
  code: '',
  description: '',
  parentId: null
})

const errors = reactive({})

// Search
const searchQuery = ref('')
const searchLoading = ref(false)

// View mode: 'flat' or 'tree'
const viewMode = ref('flat')
const treeData = ref([])
const treeLoading = ref(false)

// Expanded children
const expandedCategoryId = ref(null)
const expandedChildren = ref([])
const expandedChildrenLoading = ref(false)

// --- Search (search) ---
let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => performSearch(), 400)
}

async function performSearch() {
  const query = searchQuery.value.trim()
  if (!query) {
    fetchCategories()
    return
  }
  searchLoading.value = true
  loading.value = true
  try {
    const response = await categoriesApi.search(query)
    const data = unwrapData(response)
    categories.value = data.content || data || []
  } catch (error) {
    console.error('Failed to search categories:', error)
  } finally {
    searchLoading.value = false
    loading.value = false
  }
}

// --- Tree view (getTree, getRoots) ---
async function fetchTree() {
  treeLoading.value = true
  try {
    const response = await categoriesApi.getTree()
    const data = unwrapData(response)
    treeData.value = data || []
  } catch (error) {
    console.error('Failed to fetch category tree:', error)
    // Fallback to roots
    try {
      const response = await categoriesApi.getRoots()
      const data = unwrapData(response)
      treeData.value = (data.content || data || []).map(item => ({ ...item, children: [] }))
    } catch (e) {
      console.error('Fallback getRoots also failed:', e)
    }
  } finally {
    treeLoading.value = false
  }
}

function switchViewMode(mode) {
  viewMode.value = mode
  if (mode === 'tree') {
    fetchTree()
  } else {
    fetchCategories()
  }
}

// --- Expand children (getChildren) ---
async function toggleExpandCategory(categoryId) {
  if (expandedCategoryId.value === categoryId) {
    expandedCategoryId.value = null
    expandedChildren.value = []
    return
  }
  expandedCategoryId.value = categoryId
  expandedChildrenLoading.value = true
  try {
    const response = await categoriesApi.getChildren(categoryId)
    const data = unwrapData(response)
    expandedChildren.value = data.content || data || []
  } catch (error) {
    console.error('Failed to fetch children:', error)
    expandedChildren.value = []
  } finally {
    expandedChildrenLoading.value = false
  }
}

// Auto-generate code from name
function generateCode(name) {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').substring(0, 50)
}

function onNameChange() {
  if (!editingCategory.value && form.name) {
    form.code = generateCode(form.name)
  }
}

async function fetchCategories() {
  loading.value = true
  try {
    const response = await categoriesApi.getAll()
    // Backend returns a list directly (not paginated)
    categories.value = unwrapList(response)
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  } finally {
    loading.value = false
  }
}

onMounted(fetchCategories)

async function openModal(category = null) {
  editingCategory.value = category
  if (category) {
    // Use getById to load fresh details for editing
    try {
      const response = await categoriesApi.getById(category.id)
      const data = unwrapData(response)
      const fresh = data || category
      form.name = fresh.name
      form.code = fresh.code || ''
      form.description = fresh.description || ''
      form.parentId = fresh.parent?.id || fresh.parentId || null
      editingCategory.value = fresh
    } catch (error) {
      console.error('Failed to fetch category details:', error)
      form.name = category.name
      form.code = category.code || ''
      form.description = category.description || ''
      form.parentId = category.parent?.id || null
    }
  } else {
    form.name = ''
    form.code = ''
    form.description = ''
    form.parentId = null
  }
  Object.keys(errors).forEach(key => delete errors[key])
  showModal.value = true
}

function validate() {
  Object.keys(errors).forEach(key => delete errors[key])
  if (!form.name?.trim()) errors.name = t('inventory.categories.nameRequired')
  if (!form.code?.trim()) errors.code = t('required')
  return Object.keys(errors).length === 0
}

async function saveCategory() {
  if (!validate()) return

  try {
    if (editingCategory.value) {
      await categoriesApi.update(editingCategory.value.id, form)
    } else {
      await categoriesApi.create(form)
    }
    showModal.value = false
    fetchCategories()
  } catch (error) {
    console.error('Failed to save category:', error)
    errors.general = error.response?.data?.message || t('failedToSave')
  }
}

async function deleteCategory(category) {
  if (!confirm(t('inventory.categories.confirmDelete', { name: category.name }))) return

  try {
    await categoriesApi.delete(category.id)
    fetchCategories()
  } catch (error) {
    console.error('Failed to delete category:', error)
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('inventory.categories.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('inventory.categories.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('inventory.categories.addCategory') }}
      </button>
    </div>

    <!-- Search + view mode toggle -->
    <div class="card">
      <div class="card-body space-y-4">
        <div class="flex flex-col sm:flex-row gap-3">
          <div class="flex-1 relative">
            <MagnifyingGlassIcon class="h-5 w-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              v-model="searchQuery"
              @input="onSearchInput"
              type="text"
              class="input pl-10"
              :placeholder="$t('inventory.categories.searchPlaceholder')"
            />
          </div>
          <div class="flex items-center space-x-2">
            <button
              @click="switchViewMode('flat')"
              :class="['btn-secondary flex items-center space-x-1', viewMode === 'flat' ? 'bg-primary-100 text-primary-700 border-primary-300' : '']"
            >
              <ListBulletIcon class="h-4 w-4" />
              <span>{{ $t('inventory.categories.flatView') }}</span>
            </button>
            <button
              @click="switchViewMode('tree')"
              :class="['btn-secondary flex items-center space-x-1', viewMode === 'tree' ? 'bg-primary-100 text-primary-700 border-primary-300' : '']"
            >
              <QueueListIcon class="h-4 w-4" />
              <span>{{ $t('inventory.categories.treeView') }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Flat view -->
    <div v-if="viewMode === 'flat'" class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="categories.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ searchQuery ? $t('inventory.categories.noSearchResults') : $t('inventory.categories.noCategories') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th></th>
              <th>{{ $t('name') }}</th>
              <th>{{ $t('code') }}</th>
              <th>{{ $t('description') }}</th>
              <th>{{ $t('inventory.brands.productsCount') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <template v-for="category in categories" :key="category.id">
              <tr class="hover:bg-gray-50">
                <td class="w-8">
                  <button @click="toggleExpandCategory(category.id)" class="p-1 text-gray-400 hover:text-gray-600">
                    <component :is="expandedCategoryId === category.id ? ChevronDownIcon : ChevronRightIcon" class="h-4 w-4" />
                  </button>
                </td>
                <td class="font-medium">{{ category.name }}</td>
                <td class="font-mono text-sm text-gray-500">{{ category.code }}</td>
                <td class="text-gray-500">{{ category.description || '-' }}</td>
                <td>{{ category.productCount || 0 }}</td>
                <td class="text-right">
                  <div class="flex items-center justify-end space-x-2">
                    <button @click="openModal(category)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                      <PencilIcon class="h-5 w-5" />
                    </button>
                    <button @click="deleteCategory(category)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
                      <TrashIcon class="h-5 w-5" />
                    </button>
                  </div>
                </td>
              </tr>
              <!-- Expanded children row -->
              <tr v-if="expandedCategoryId === category.id">
                <td colspan="6" class="bg-gray-50 px-8 py-3">
                  <div v-if="expandedChildrenLoading" class="text-sm text-gray-400">
                    {{ $t('inventory.categories.loadingChildren') }}
                  </div>
                  <div v-else-if="expandedChildren.length === 0" class="text-sm text-gray-400 italic">
                    {{ $t('inventory.categories.noChildren') }}
                  </div>
                  <div v-else>
                    <h4 class="text-xs font-medium text-gray-500 uppercase mb-2">{{ $t('inventory.categories.children') }}</h4>
                    <ul class="space-y-1">
                      <li v-for="child in expandedChildren" :key="child.id" class="flex items-center justify-between text-sm py-1">
                        <div class="flex items-center space-x-3">
                          <span class="font-medium text-gray-800">{{ child.name }}</span>
                          <span class="font-mono text-xs text-gray-500">{{ child.code }}</span>
                        </div>
                        <div class="flex items-center space-x-2">
                          <button @click="openModal(child)" class="p-1 text-gray-400 hover:text-primary-600">
                            <PencilIcon class="h-4 w-4" />
                          </button>
                          <button @click="deleteCategory(child)" class="p-1 text-gray-400 hover:text-red-600">
                            <TrashIcon class="h-4 w-4" />
                          </button>
                        </div>
                      </li>
                    </ul>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tree view -->
    <div v-if="viewMode === 'tree'" class="card">
      <div v-if="treeLoading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="treeData.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('inventory.categories.noCategories') }}</p>
      </div>

      <div v-else class="p-6">
        <h3 class="text-sm font-medium text-gray-500 mb-4">{{ $t('inventory.categories.rootCategories') }}</h3>
        <ul class="space-y-2">
          <li v-for="node in treeData" :key="node.id">
            <div class="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50">
              <div class="flex items-center">
                <ChevronRightIcon v-if="!node.children || node.children.length === 0" class="h-4 w-4 text-gray-300 mr-2" />
                <ChevronDownIcon v-else class="h-4 w-4 text-gray-400 mr-2" />
                <span class="font-medium text-gray-900">{{ node.name }}</span>
                <span class="ml-2 font-mono text-sm text-gray-500">({{ node.code }})</span>
              </div>
              <div class="flex items-center space-x-2">
                <button @click="openModal(node)" class="p-1 text-gray-400 hover:text-primary-600">
                  <PencilIcon class="h-4 w-4" />
                </button>
                <button @click="deleteCategory(node)" class="p-1 text-gray-400 hover:text-red-600">
                  <TrashIcon class="h-4 w-4" />
                </button>
              </div>
            </div>
            <!-- Tree children -->
            <ul v-if="node.children && node.children.length > 0" class="ml-8 mt-1 space-y-1">
              <li v-for="child in node.children" :key="child.id">
                <div class="flex items-center justify-between p-2 border rounded-lg hover:bg-gray-50">
                  <div class="flex items-center">
                    <span class="text-sm text-gray-800">{{ child.name }}</span>
                    <span class="ml-2 font-mono text-xs text-gray-500">({{ child.code }})</span>
                  </div>
                  <div class="flex items-center space-x-2">
                    <button @click="openModal(child)" class="p-1 text-gray-400 hover:text-primary-600">
                      <PencilIcon class="h-4 w-4" />
                    </button>
                    <button @click="deleteCategory(child)" class="p-1 text-gray-400 hover:text-red-600">
                      <TrashIcon class="h-4 w-4" />
                    </button>
                  </div>
                </div>
                <!-- Nested grandchildren -->
                <ul v-if="child.children && child.children.length > 0" class="ml-8 mt-1 space-y-1">
                  <li v-for="grandchild in child.children" :key="grandchild.id">
                    <div class="flex items-center justify-between p-2 border rounded-lg hover:bg-gray-50">
                      <div class="flex items-center">
                        <span class="text-xs text-gray-700">{{ grandchild.name }}</span>
                        <span class="ml-2 font-mono text-xs text-gray-400">({{ grandchild.code }})</span>
                      </div>
                      <div class="flex items-center space-x-2">
                        <button @click="openModal(grandchild)" class="p-1 text-gray-400 hover:text-primary-600">
                          <PencilIcon class="h-3 w-3" />
                        </button>
                      </div>
                    </div>
                  </li>
                </ul>
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex items-center justify-center min-h-screen px-4">
        <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
        <div class="relative bg-white rounded-lg max-w-md w-full p-6">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ editingCategory ? $t('inventory.categories.editCategory') : $t('inventory.categories.newCategory') }}
          </h3>

          <div v-if="errors.general" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-600">{{ errors.general }}</p>
          </div>

          <div class="space-y-4">
            <div>
              <label class="label">{{ $t('name') }} *</label>
              <input v-model="form.name" @input="onNameChange" type="text" :class="[errors.name ? 'input-error' : 'input']" />
              <p v-if="errors.name" class="mt-1 text-sm text-red-600">{{ errors.name }}</p>
            </div>

            <div>
              <label class="label">{{ $t('code') }} *</label>
              <input v-model="form.code" type="text" :class="[errors.code ? 'input-error' : 'input']" :placeholder="$t('inventory.categories.autoGenerated')" />
              <p v-if="errors.code" class="mt-1 text-sm text-red-600">{{ errors.code }}</p>
            </div>

            <div>
              <label class="label">{{ $t('description') }}</label>
              <textarea v-model="form.description" rows="3" class="input"></textarea>
            </div>

            <div>
              <label class="label">{{ $t('inventory.categories.parentCategory') }}</label>
              <select v-model="form.parentId" class="input">
                <option :value="null">{{ $t('inventory.categories.noParent') }}</option>
                <option
                  v-for="cat in categories.filter(c => c.id !== editingCategory?.id)"
                  :key="cat.id"
                  :value="cat.id"
                >
                  {{ cat.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="mt-6 flex justify-end space-x-3">
            <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
            <button @click="saveCategory" class="btn-primary">{{ $t('save') }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
