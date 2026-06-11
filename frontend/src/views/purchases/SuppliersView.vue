<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { suppliersApi, unwrapData, unwrapList } from '@/services/api'
import {
  PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon,
  UserCircleIcon, StarIcon, PhoneIcon, EnvelopeIcon, XMarkIcon
} from '@heroicons/vue/24/outline'
import { StarIcon as StarIconSolid } from '@heroicons/vue/24/solid'

const { t } = useI18n()

const suppliers = ref([])
const loading = ref(true)
const showModal = ref(false)
const editingSupplier = ref(null)
const search = ref('')
const activeFilter = ref('all')

const form = reactive({
  name: '',
  code: '',
  phone: '',
  email: '',
  address: '',
  contactPerson: '',
  active: true
})

// Supplier code lookup
const codeLookup = ref('')
const codeLookupResult = ref(null)
const codeLookupError = ref('')
const codeLookupLoading = ref(false)

// Primary contacts map: vendorId -> contact
const primaryContacts = ref({})

const filters = computed(() => [
  { key: 'all', label: t('purchases.suppliers.filterAll') },
  { key: 'active', label: t('active') },
  { key: 'preferred', label: t('purchases.suppliers.preferred') }
])

async function fetchSuppliers() {
  loading.value = true
  try {
    if (activeFilter.value === 'active') {
      const res = await suppliersApi.getActive()
      suppliers.value = res.data || []
    } else if (activeFilter.value === 'preferred') {
      const res = await suppliersApi.getPreferred()
      suppliers.value = res.data || []
    } else if (search.value) {
      const res = await suppliersApi.search(search.value, { size: 100 })
      suppliers.value = unwrapList(res)
    } else {
      const response = await suppliersApi.getAll({ size: 100 })
      suppliers.value = unwrapList(response)
    }
  } catch (error) {
    console.error('Failed to fetch suppliers:', error)
  } finally {
    loading.value = false
  }
}

async function lookupByCode() {
  if (!codeLookup.value.trim()) return
  codeLookupLoading.value = true
  codeLookupError.value = ''
  codeLookupResult.value = null
  try {
    const res = await suppliersApi.getByCode(codeLookup.value.trim())
    codeLookupResult.value = unwrapData(res)
  } catch (error) {
    if (error.response?.status === 404) {
      codeLookupError.value = t('purchases.suppliers.codeNotFound')
    } else {
      codeLookupError.value = error.response?.data?.message || t('errorOccurred')
    }
  } finally {
    codeLookupLoading.value = false
  }
}

async function loadPrimaryContacts() {
  for (const supplier of suppliers.value) {
    if (!supplier.id) continue
    try {
      const res = await suppliersApi.getPrimaryContact(supplier.id)
      const contact = unwrapData(res)
      if (contact && contact.name) {
        primaryContacts.value[supplier.id] = contact
      }
    } catch {
      // No primary contact or error - skip
    }
  }
}

onMounted(async () => {
  await fetchSuppliers()
  loadPrimaryContacts()
})

async function switchFilter(key) {
  activeFilter.value = key
  await fetchSuppliers()
  loadPrimaryContacts()
}

function openModal(supplier = null) {
  editingSupplier.value = supplier
  if (supplier) {
    Object.assign(form, supplier)
  } else {
    form.name = ''
    form.code = ''
    form.phone = ''
    form.email = ''
    form.address = ''
    form.contactPerson = ''
    form.active = true
  }
  showModal.value = true
}

async function saveSupplier() {
  if (!form.name?.trim()) return

  try {
    if (editingSupplier.value) {
      await suppliersApi.update(editingSupplier.value.id, form)
    } else {
      await suppliersApi.create(form)
    }
    showModal.value = false
    fetchSuppliers()
  } catch (error) {
    console.error('Failed to save supplier:', error)
  }
}

async function toggleActive(supplier) {
  try {
    await suppliersApi.activate(supplier.id)
    fetchSuppliers()
  } catch (error) {
    console.error('Failed to toggle supplier status:', error)
  }
}

async function deleteSupplier(supplier) {
  if (!confirm(t('purchases.suppliers.confirmDelete', { name: supplier.name }))) return
  try {
    await suppliersApi.delete(supplier.id)
    fetchSuppliers()
  } catch (error) {
    console.error('Failed to delete supplier:', error)
  }
}

// Contacts management
const showContactsModal = ref(false)
const contactsVendor = ref(null)
const contacts = ref([])
const loadingContacts = ref(false)
const showContactForm = ref(false)
const editingContact = ref(null)
const contactForm = reactive({
  name: '',
  title: '',
  phone: '',
  email: '',
  contactType: 'GENERAL',
  primary: false,
  notes: ''
})

const contactTypes = ['GENERAL', 'BILLING', 'ORDERING', 'SUPPORT']

async function openContactsModal(supplier) {
  contactsVendor.value = supplier
  showContactsModal.value = true
  showContactForm.value = false
  await loadContacts(supplier.id)
}

async function loadContacts(vendorId) {
  loadingContacts.value = true
  try {
    const res = await suppliersApi.getContacts(vendorId)
    contacts.value = unwrapList(res)
  } catch (error) {
    console.error('Failed to load contacts:', error)
    contacts.value = []
  } finally {
    loadingContacts.value = false
  }
}

function openContactForm(contact = null) {
  editingContact.value = contact
  if (contact) {
    Object.assign(contactForm, {
      name: contact.name || '',
      title: contact.title || '',
      phone: contact.phone || '',
      email: contact.email || '',
      contactType: contact.contactType || 'GENERAL',
      primary: contact.primary || false,
      notes: contact.notes || ''
    })
  } else {
    Object.assign(contactForm, {
      name: '', title: '', phone: '', email: '',
      contactType: 'GENERAL', primary: false, notes: ''
    })
  }
  showContactForm.value = true
}

async function saveContact() {
  if (!contactForm.name?.trim()) return
  try {
    if (editingContact.value) {
      await suppliersApi.updateContact(editingContact.value.id, contactForm)
    } else {
      await suppliersApi.createContact(contactsVendor.value.id, contactForm)
    }
    showContactForm.value = false
    await loadContacts(contactsVendor.value.id)
  } catch (error) {
    console.error('Failed to save contact:', error)
  }
}

async function deleteContact(contact) {
  if (!confirm(t('purchases.suppliers.confirmDeleteContact', { name: contact.name }))) return
  try {
    await suppliersApi.deleteContact(contact.id)
    await loadContacts(contactsVendor.value.id)
  } catch (error) {
    console.error('Failed to delete contact:', error)
  }
}

async function setPrimary(contact) {
  try {
    await suppliersApi.setPrimaryContact(contact.id)
    await loadContacts(contactsVendor.value.id)
  } catch (error) {
    console.error('Failed to set primary contact:', error)
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('purchases.suppliers.title') }}</h1>
        <p class="mt-1 text-sm text-gray-500">{{ $t('purchases.suppliers.subtitle') }}</p>
      </div>
      <button @click="openModal()" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" />
        {{ $t('purchases.suppliers.addSupplier') }}
      </button>
    </div>

    <!-- Filter Tabs -->
    <div class="border-b border-gray-200">
      <nav class="flex space-x-4">
        <button
          v-for="f in filters"
          :key="f.key"
          @click="switchFilter(f.key)"
          :class="[
            'px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 transition-colors',
            activeFilter === f.key
              ? 'border-primary-500 text-primary-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          {{ f.label }}
        </button>
      </nav>
    </div>

    <!-- Search -->
    <div v-if="activeFilter === 'all'" class="card">
      <div class="card-body space-y-3">
        <div class="relative">
          <MagnifyingGlassIcon class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            v-model="search"
            @input="fetchSuppliers"
            type="text"
            :placeholder="$t('purchases.suppliers.searchPlaceholder')"
            class="input pl-10"
          />
        </div>
        <!-- Code lookup -->
        <div>
          <label class="label text-xs">{{ $t('purchases.suppliers.codeLookup') }}</label>
          <div class="flex gap-2">
            <input
              v-model="codeLookup"
              @keyup.enter="lookupByCode"
              type="text"
              :placeholder="$t('purchases.suppliers.codeLookupPlaceholder')"
              class="input flex-1"
            />
            <button @click="lookupByCode" :disabled="codeLookupLoading" class="btn-secondary">
              {{ codeLookupLoading ? $t('searching') : $t('search') }}
            </button>
          </div>
          <div v-if="codeLookupError" class="mt-2 text-sm text-red-600">{{ codeLookupError }}</div>
          <div v-if="codeLookupResult" class="mt-2 p-3 bg-gray-50 rounded-lg border border-gray-200">
            <div class="flex items-center justify-between">
              <div>
                <p class="font-medium">{{ codeLookupResult.name }}</p>
                <p class="text-sm text-gray-500">{{ codeLookupResult.code }} &middot; {{ codeLookupResult.phone || '-' }}</p>
              </div>
              <button @click="openModal(codeLookupResult)" class="btn-secondary text-sm">{{ $t('edit') }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <div v-else-if="suppliers.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ $t('purchases.suppliers.noSuppliers') }}</p>
      </div>

      <div v-else class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('purchases.suppliers.supplierName') }}</th>
              <th>{{ $t('purchases.suppliers.contact') }}</th>
              <th>{{ $t('purchases.suppliers.contactPerson') }}</th>
              <th>{{ $t('purchases.suppliers.primaryContact') }}</th>
              <th>{{ $t('purchases.suppliers.contacts') }}</th>
              <th>{{ $t('status') }}</th>
              <th class="text-right">{{ $t('actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="supplier in suppliers" :key="supplier.id">
              <td>
                <div class="font-medium">{{ supplier.name }}</div>
                <div class="text-sm text-gray-500">{{ supplier.code || `#${supplier.id}` }}</div>
              </td>
              <td>
                <div class="text-sm">{{ supplier.phone || '-' }}</div>
                <div class="text-sm text-gray-500">{{ supplier.email || '-' }}</div>
              </td>
              <td>{{ supplier.contactPerson || '-' }}</td>
              <td>
                <template v-if="primaryContacts[supplier.id]">
                  <div class="text-sm font-medium">{{ primaryContacts[supplier.id].name }}</div>
                  <div v-if="primaryContacts[supplier.id].phone" class="text-xs text-gray-500 flex items-center gap-0.5">
                    <PhoneIcon class="h-3 w-3" /> {{ primaryContacts[supplier.id].phone }}
                  </div>
                </template>
                <span v-else class="text-gray-400 text-sm">-</span>
              </td>
              <td>
                <button
                  @click="openContactsModal(supplier)"
                  class="text-sm text-primary-600 hover:text-primary-800 flex items-center gap-1"
                >
                  <UserCircleIcon class="h-4 w-4" />
                  {{ $t('purchases.suppliers.manageContacts') }}
                </button>
              </td>
              <td>
                <button
                  @click="toggleActive(supplier)"
                  :class="['badge cursor-pointer', supplier.active !== false ? 'badge-success' : 'badge-danger']"
                >
                  {{ supplier.active !== false ? $t('active') : $t('inactive') }}
                </button>
              </td>
              <td class="text-right">
                <div class="flex items-center justify-end space-x-2">
                  <button @click="openModal(supplier)" class="p-2 text-gray-400 hover:text-primary-600 rounded-lg hover:bg-gray-100">
                    <PencilIcon class="h-5 w-5" />
                  </button>
                  <button @click="deleteSupplier(supplier)" class="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-gray-100">
                    <TrashIcon class="h-5 w-5" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Supplier Create/Edit Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-lg w-full p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">
              {{ editingSupplier ? $t('purchases.suppliers.editSupplier') : $t('purchases.suppliers.newSupplier') }}
            </h3>

            <div class="space-y-4">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('name') }} *</label>
                  <input v-model="form.name" type="text" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('code') }}</label>
                  <input v-model="form.code" type="text" class="input" />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">{{ $t('phone') }}</label>
                  <input v-model="form.phone" type="tel" class="input" />
                </div>
                <div>
                  <label class="label">{{ $t('email') }}</label>
                  <input v-model="form.email" type="email" class="input" />
                </div>
              </div>
              <div>
                <label class="label">{{ $t('purchases.suppliers.contactPerson') }}</label>
                <input v-model="form.contactPerson" type="text" class="input" />
              </div>
              <div>
                <label class="label">{{ $t('address') }}</label>
                <textarea v-model="form.address" rows="2" class="input"></textarea>
              </div>
              <label class="flex items-center">
                <input v-model="form.active" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                <span class="ml-2 text-sm">{{ $t('active') }}</span>
              </label>
            </div>

            <div class="mt-6 flex justify-end space-x-3">
              <button @click="showModal = false" class="btn-secondary">{{ $t('cancel') }}</button>
              <button @click="saveSupplier" class="btn-primary">{{ $t('save') }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Contacts Modal -->
    <Teleport to="body">
      <div v-if="showContactsModal" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75" @click="showContactsModal = false"></div>
          <div class="relative bg-white rounded-lg max-w-2xl w-full max-h-[80vh] flex flex-col">
            <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div>
                <h3 class="text-lg font-medium text-gray-900">
                  {{ $t('purchases.suppliers.contactsTitle', { name: contactsVendor?.name }) }}
                </h3>
              </div>
              <div class="flex items-center gap-2">
                <button @click="openContactForm()" class="btn-primary text-sm">
                  <PlusIcon class="h-4 w-4 mr-1" />
                  {{ $t('purchases.suppliers.addContact') }}
                </button>
                <button @click="showContactsModal = false" class="p-1 text-gray-400 hover:text-gray-600">
                  <XMarkIcon class="h-5 w-5" />
                </button>
              </div>
            </div>

            <div class="flex-1 overflow-y-auto p-6">
              <!-- Contact Form (inline) -->
              <div v-if="showContactForm" class="mb-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
                <h4 class="text-sm font-medium text-gray-900 mb-3">
                  {{ editingContact ? $t('purchases.suppliers.editContact') : $t('purchases.suppliers.newContact') }}
                </h4>
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="label text-xs">{{ $t('name') }} *</label>
                    <input v-model="contactForm.name" type="text" class="input text-sm" />
                  </div>
                  <div>
                    <label class="label text-xs">{{ $t('purchases.suppliers.jobTitle') }}</label>
                    <input v-model="contactForm.title" type="text" class="input text-sm" />
                  </div>
                  <div>
                    <label class="label text-xs">{{ $t('phone') }}</label>
                    <input v-model="contactForm.phone" type="tel" class="input text-sm" />
                  </div>
                  <div>
                    <label class="label text-xs">{{ $t('email') }}</label>
                    <input v-model="contactForm.email" type="email" class="input text-sm" />
                  </div>
                  <div>
                    <label class="label text-xs">{{ $t('purchases.suppliers.contactType') }}</label>
                    <select v-model="contactForm.contactType" class="input text-sm">
                      <option v-for="ct in contactTypes" :key="ct" :value="ct">
                        {{ $t(`purchases.suppliers.contactTypes.${ct}`) }}
                      </option>
                    </select>
                  </div>
                  <div class="flex items-end">
                    <label class="flex items-center">
                      <input v-model="contactForm.primary" type="checkbox" class="h-4 w-4 text-primary-600 rounded" />
                      <span class="ml-2 text-sm">{{ $t('purchases.suppliers.primaryContact') }}</span>
                    </label>
                  </div>
                </div>
                <div class="mt-3">
                  <label class="label text-xs">{{ $t('notes') }}</label>
                  <textarea v-model="contactForm.notes" rows="2" class="input text-sm"></textarea>
                </div>
                <div class="mt-3 flex justify-end gap-2">
                  <button @click="showContactForm = false" class="btn-secondary text-sm">{{ $t('cancel') }}</button>
                  <button @click="saveContact" class="btn-primary text-sm">{{ $t('save') }}</button>
                </div>
              </div>

              <!-- Contacts List -->
              <div v-if="loadingContacts" class="flex justify-center py-8">
                <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
              </div>
              <div v-else-if="contacts.length === 0" class="text-center py-8 text-gray-500 text-sm">
                {{ $t('purchases.suppliers.noContacts') }}
              </div>
              <div v-else class="space-y-3">
                <div
                  v-for="contact in contacts"
                  :key="contact.id"
                  class="flex items-start justify-between p-3 border border-gray-200 rounded-lg"
                >
                  <div class="flex items-start gap-3">
                    <div class="w-9 h-9 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
                      <span class="text-primary-700 font-medium text-sm">{{ contact.name?.charAt(0) }}</span>
                    </div>
                    <div>
                      <div class="flex items-center gap-2">
                        <span class="font-medium text-sm">{{ contact.name }}</span>
                        <span v-if="contact.primary" class="bg-yellow-100 text-yellow-800 text-[10px] font-medium px-1.5 py-0.5 rounded">
                          {{ $t('purchases.suppliers.primaryContact') }}
                        </span>
                        <span class="badge badge-info text-[10px]">
                          {{ $t(`purchases.suppliers.contactTypes.${contact.contactType || 'GENERAL'}`) }}
                        </span>
                      </div>
                      <div v-if="contact.title" class="text-xs text-gray-500">{{ contact.title }}</div>
                      <div class="flex gap-3 mt-1 text-xs text-gray-500">
                        <span v-if="contact.phone" class="flex items-center gap-0.5">
                          <PhoneIcon class="h-3 w-3" /> {{ contact.phone }}
                        </span>
                        <span v-if="contact.email" class="flex items-center gap-0.5">
                          <EnvelopeIcon class="h-3 w-3" /> {{ contact.email }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div class="flex items-center gap-1">
                    <button
                      v-if="!contact.primary"
                      @click="setPrimary(contact)"
                      class="p-1.5 text-gray-400 hover:text-yellow-500 rounded hover:bg-gray-100"
                      :title="$t('purchases.suppliers.setPrimary')"
                    >
                      <StarIcon class="h-4 w-4" />
                    </button>
                    <StarIconSolid v-else class="h-4 w-4 text-yellow-500 mx-1.5" />
                    <button
                      @click="openContactForm(contact)"
                      class="p-1.5 text-gray-400 hover:text-primary-600 rounded hover:bg-gray-100"
                    >
                      <PencilIcon class="h-4 w-4" />
                    </button>
                    <button
                      @click="deleteContact(contact)"
                      class="p-1.5 text-gray-400 hover:text-red-600 rounded hover:bg-gray-100"
                    >
                      <TrashIcon class="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
