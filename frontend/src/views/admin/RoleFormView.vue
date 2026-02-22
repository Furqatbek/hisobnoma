<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { rolesApi } from '@/services/api'
import { ArrowLeftIcon, ShieldCheckIcon, CheckIcon } from '@heroicons/vue/24/outline'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const errors = ref({})
const allPermissions = ref([])
const isSystemRole = ref(false)

const form = ref({
  name: '',
  code: '',
  description: '',
  permissionCodes: []
})

// Group permissions by category
const permissionGroups = computed(() => {
  const groups = {}

  allPermissions.value.forEach(permission => {
    // Extract category from permission code (e.g., INVENTORY_PRODUCT_READ -> INVENTORY)
    const parts = permission.split('_')
    let category = parts[0]

    // Handle special cases
    if (parts.length >= 2) {
      if (parts[0] === 'POS' || parts[0] === 'ADMIN') {
        category = parts[0] + '_' + parts[1]
      }
    }

    if (!groups[category]) {
      groups[category] = {
        name: getCategoryName(category),
        permissions: []
      }
    }

    groups[category].permissions.push({
      code: permission,
      label: getPermissionLabel(permission)
    })
  })

  return groups
})

function getCategoryName(category) {
  const key = `admin.roleForm.categories.${category}`
  const translated = t(key)
  return translated !== key ? translated : category
}

function getPermissionLabel(permission) {
  // Get the action part (READ, CREATE, UPDATE, DELETE, MANAGE, etc.)
  const parts = permission.split('_')
  const action = parts[parts.length - 1]

  const key = `admin.roleForm.actions.${action}`
  const translated = t(key)
  return translated !== key ? translated : action
}

async function fetchRole() {
  if (!isEdit.value) return

  loading.value = true
  try {
    const response = await rolesApi.getById(route.params.id)
    const role = response.data.data || response.data
    form.value = {
      name: role.name || '',
      code: role.code || '',
      description: role.description || '',
      permissionCodes: role.permissions || []
    }
    isSystemRole.value = role.systemRole || false
  } catch (error) {
    console.error('Rolni yuklashda xatolik:', error)
    alert(t('admin.roleForm.loadError'))
    router.push('/admin/roles')
  } finally {
    loading.value = false
  }
}

async function fetchPermissions() {
  try {
    const response = await rolesApi.getAllPermissions()
    allPermissions.value = response.data.data || response.data || []
  } catch (error) {
    console.error('Ruxsatlarni yuklashda xatolik:', error)
  }
}

function validateForm() {
  errors.value = {}

  if (!form.value.name?.trim()) {
    errors.value.name = t('admin.roleForm.nameRequired')
  }

  if (!form.value.code?.trim()) {
    errors.value.code = t('admin.roleForm.codeRequired')
  } else if (!/^[A-Z][A-Z0-9_]*$/.test(form.value.code)) {
    errors.value.code = t('admin.roleForm.codeFormat')
  }

  return Object.keys(errors.value).length === 0
}

async function saveRole() {
  if (!validateForm()) return

  saving.value = true
  try {
    const data = {
      name: form.value.name.trim(),
      code: form.value.code.trim().toUpperCase(),
      description: form.value.description?.trim() || null,
      permissionCodes: form.value.permissionCodes
    }

    if (isEdit.value) {
      await rolesApi.update(route.params.id, data)
    } else {
      await rolesApi.create(data)
    }

    router.push('/admin/roles')
  } catch (error) {
    console.error('Rolni saqlashda xatolik:', error)

    if (error.response?.data?.validationErrors) {
      error.response.data.validationErrors.forEach(err => {
        errors.value[err.field] = err.message
      })
    } else if (error.response?.data?.message) {
      alert(error.response.data.message)
    } else {
      alert(t('admin.roleForm.saveError'))
    }
  } finally {
    saving.value = false
  }
}

function togglePermission(permissionCode) {
  const index = form.value.permissionCodes.indexOf(permissionCode)
  if (index > -1) {
    form.value.permissionCodes.splice(index, 1)
  } else {
    form.value.permissionCodes.push(permissionCode)
  }
}

function isPermissionSelected(permissionCode) {
  return form.value.permissionCodes.includes(permissionCode)
}

function selectAllInGroup(groupPermissions) {
  groupPermissions.forEach(p => {
    if (!form.value.permissionCodes.includes(p.code)) {
      form.value.permissionCodes.push(p.code)
    }
  })
}

function deselectAllInGroup(groupPermissions) {
  groupPermissions.forEach(p => {
    const index = form.value.permissionCodes.indexOf(p.code)
    if (index > -1) {
      form.value.permissionCodes.splice(index, 1)
    }
  })
}

function isGroupFullySelected(groupPermissions) {
  return groupPermissions.every(p => form.value.permissionCodes.includes(p.code))
}

function isGroupPartiallySelected(groupPermissions) {
  const selectedCount = groupPermissions.filter(p => form.value.permissionCodes.includes(p.code)).length
  return selectedCount > 0 && selectedCount < groupPermissions.length
}

function toggleGroup(groupPermissions) {
  if (isGroupFullySelected(groupPermissions)) {
    deselectAllInGroup(groupPermissions)
  } else {
    selectAllInGroup(groupPermissions)
  }
}

function selectAll() {
  form.value.permissionCodes = [...allPermissions.value]
}

function deselectAll() {
  form.value.permissionCodes = []
}

onMounted(() => {
  fetchPermissions()
  fetchRole()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center space-x-4">
      <button @click="router.back()" class="p-2 hover:bg-gray-100 rounded-lg">
        <ArrowLeftIcon class="h-5 w-5 text-gray-500" />
      </button>
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          {{ isEdit ? $t('admin.roleForm.editRole') : $t('admin.roleForm.newRole') }}
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          {{ isEdit ? $t('admin.roleForm.editSubtitle') : $t('admin.roleForm.newSubtitle') }}
        </p>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Role Info -->
      <div class="lg:col-span-1">
        <div class="card sticky top-24">
          <div class="card-header">
            <div class="flex items-center">
              <ShieldCheckIcon class="h-6 w-6 text-primary-600 mr-2" />
              <h3 class="text-lg font-medium">{{ $t('admin.roleForm.roleInfo') }}</h3>
            </div>
          </div>

          <div class="card-body space-y-4">
            <!-- Name -->
            <div>
              <label class="label">
                {{ $t('admin.roles.roleName') }} <span class="text-red-500">*</span>
              </label>
              <input
                v-model="form.name"
                type="text"
                class="input"
                :class="{ 'border-red-500': errors.name }"
                :placeholder="$t('admin.roleForm.namePlaceholder')"
                :disabled="isSystemRole"
              />
              <p v-if="errors.name" class="text-sm text-red-500 mt-1">{{ errors.name }}</p>
            </div>

            <!-- Code -->
            <div>
              <label class="label">
                {{ $t('admin.roleForm.roleCode') }} <span class="text-red-500">*</span>
              </label>
              <input
                v-model="form.code"
                type="text"
                class="input uppercase"
                :class="{ 'border-red-500': errors.code }"
                :placeholder="$t('admin.roleForm.codePlaceholder')"
                :disabled="isEdit"
              />
              <p v-if="errors.code" class="text-sm text-red-500 mt-1">{{ errors.code }}</p>
              <p v-else class="text-xs text-gray-500 mt-1">{{ $t('admin.roleForm.codeHint') }}</p>
            </div>

            <!-- Description -->
            <div>
              <label class="label">{{ $t('description') }}</label>
              <textarea
                v-model="form.description"
                rows="3"
                class="input"
                :placeholder="$t('admin.roleForm.descriptionPlaceholder')"
                :disabled="isSystemRole"
              ></textarea>
            </div>

            <!-- Selected count -->
            <div class="pt-4 border-t">
              <div class="flex items-center justify-between text-sm">
                <span class="text-gray-500">{{ $t('admin.roleForm.selectedPermissions') }}:</span>
                <span class="font-bold text-primary-600">{{ form.permissionCodes.length }} / {{ allPermissions.length }}</span>
              </div>
            </div>

            <!-- System role warning -->
            <div v-if="isSystemRole" class="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
              <p class="text-sm text-yellow-800">
                {{ $t('admin.roleForm.systemRoleWarning') }}
              </p>
            </div>
          </div>

          <div class="card-footer flex justify-end space-x-3">
            <button @click="router.back()" class="btn-secondary">
              {{ $t('cancel') }}
            </button>
            <button @click="saveRole" :disabled="saving" class="btn-primary">
              {{ saving ? $t('saving') : (isEdit ? $t('admin.roleForm.update') : $t('save')) }}
            </button>
          </div>
        </div>
      </div>

      <!-- Permissions -->
      <div class="lg:col-span-2">
        <div class="card">
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg font-medium">{{ $t('admin.roles.permissions') }}</h3>
            <div class="flex space-x-2">
              <button @click="selectAll" class="text-sm text-primary-600 hover:text-primary-800">
                {{ $t('admin.roleForm.selectAll') }}
              </button>
              <span class="text-gray-300">|</span>
              <button @click="deselectAll" class="text-sm text-gray-500 hover:text-gray-700">
                {{ $t('admin.roleForm.deselectAll') }}
              </button>
            </div>
          </div>

          <div class="card-body">
            <div class="space-y-6">
              <div v-for="(group, key) in permissionGroups" :key="key" class="border rounded-lg">
                <!-- Group Header -->
                <div
                  class="flex items-center justify-between px-4 py-3 bg-gray-50 border-b cursor-pointer hover:bg-gray-100"
                  @click="toggleGroup(group.permissions)"
                >
                  <div class="flex items-center">
                    <div
                      :class="[
                        'w-5 h-5 rounded border-2 flex items-center justify-center mr-3 transition-colors',
                        isGroupFullySelected(group.permissions)
                          ? 'bg-primary-600 border-primary-600'
                          : isGroupPartiallySelected(group.permissions)
                            ? 'bg-primary-200 border-primary-400'
                            : 'border-gray-300'
                      ]"
                    >
                      <CheckIcon v-if="isGroupFullySelected(group.permissions)" class="h-3 w-3 text-white" />
                      <div v-else-if="isGroupPartiallySelected(group.permissions)" class="w-2 h-2 bg-primary-600 rounded-sm"></div>
                    </div>
                    <span class="font-medium text-gray-900">{{ group.name }}</span>
                    <span class="ml-2 text-sm text-gray-500">({{ group.permissions.length }})</span>
                  </div>
                  <span class="text-sm text-gray-500">
                    {{ group.permissions.filter(p => isPermissionSelected(p.code)).length }} {{ $t('admin.roleForm.selected') }}
                  </span>
                </div>

                <!-- Group Permissions -->
                <div class="px-4 py-3 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                  <label
                    v-for="permission in group.permissions"
                    :key="permission.code"
                    class="flex items-center cursor-pointer hover:bg-gray-50 p-2 rounded-lg transition-colors"
                  >
                    <input
                      type="checkbox"
                      :checked="isPermissionSelected(permission.code)"
                      @change="togglePermission(permission.code)"
                      class="h-4 w-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
                    />
                    <span class="ml-2 text-sm text-gray-700">{{ permission.label }}</span>
                  </label>
                </div>
              </div>

              <div v-if="Object.keys(permissionGroups).length === 0" class="text-center py-8">
                <p class="text-gray-500">{{ $t('admin.roleForm.permissionsLoading') }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
