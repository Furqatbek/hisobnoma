<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { salaryApi, advancesApi, employeesApi } from '@/services/api'
import { PlusIcon, CheckCircleIcon, XCircleIcon, XMarkIcon, BanknotesIcon } from '@heroicons/vue/24/outline'

const records = ref([])
const advances = ref([])
const employees = ref([])
const loading = ref(true)
const showModal = ref(false)
const showAdvanceModal = ref(false)
const saving = ref(false)

const now = new Date()
const filterYear = ref(now.getFullYear())
const filterMonth = ref(now.getMonth() + 1)

const form = reactive({
  employeeId: null,
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
  baseAmount: 0,
  bonusAmount: 0,
  deductionAmount: 0,
  notes: ''
})

const advanceForm = reactive({
  employeeId: null,
  amount: 0,
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
  notes: ''
})

const netAmount = computed(() => {
  return (form.baseAmount || 0) + (form.bonusAmount || 0) - (form.deductionAmount || 0)
})

const totalAdvances = computed(() => {
  return advances.value
    .filter(a => a.status === 'GIVEN')
    .reduce((sum, a) => sum + (a.amount || 0), 0)
})

const months = [
  'Yanvar', 'Fevral', 'Mart', 'Aprel', 'May', 'Iyun',
  'Iyul', 'Avgust', 'Sentabr', 'Oktabr', 'Noyabr', 'Dekabr'
]

function formatCurrency(value) {
  return new Intl.NumberFormat('uz-UZ').format(value || 0)
}

function statusBadge(status) {
  switch (status) {
    case 'PAID': return 'badge-success'
    case 'PENDING': return 'badge-warning'
    case 'CANCELLED': return 'badge-danger'
    case 'GIVEN': return 'badge-info'
    case 'DEDUCTED': return 'badge-success'
    default: return 'badge-info'
  }
}

function statusLabel(status) {
  switch (status) {
    case 'PAID': return "To'langan"
    case 'PENDING': return 'Kutilmoqda'
    case 'CANCELLED': return 'Bekor qilingan'
    case 'GIVEN': return 'Berilgan'
    case 'DEDUCTED': return 'Hisobdan chiqarilgan'
    default: return status
  }
}

async function loadData() {
  loading.value = true
  try {
    const [salaryRes, advRes, empRes] = await Promise.all([
      salaryApi.getByPeriod(filterYear.value, filterMonth.value, { size: 200 }),
      advancesApi.getByPeriod(filterYear.value, filterMonth.value).catch(() => ({ data: [] })),
      employeesApi.getActive()
    ])
    records.value = salaryRes.data.content || salaryRes.data.data?.content || []
    advances.value = advRes.data.data || advRes.data || []
    employees.value = empRes.data.data || empRes.data || []
  } catch (error) {
    console.error('Failed to load:', error)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    employeeId: null,
    periodYear: filterYear.value,
    periodMonth: filterMonth.value,
    baseAmount: 0,
    bonusAmount: 0,
    deductionAmount: 0,
    notes: ''
  })
  showModal.value = true
}

function openAdvanceCreate() {
  Object.assign(advanceForm, {
    employeeId: null,
    amount: 0,
    periodYear: filterYear.value,
    periodMonth: filterMonth.value,
    notes: ''
  })
  showAdvanceModal.value = true
}

function onEmployeeSelect() {
  const emp = employees.value.find(e => e.id === form.employeeId)
  if (emp && emp.currentSalary) {
    form.baseAmount = emp.currentSalary
  }
}

async function handleSave() {
  if (!form.employeeId || !form.baseAmount) return
  saving.value = true
  try {
    await salaryApi.create(form)
    showModal.value = false
    await loadData()
  } catch (error) {
    console.error('Failed to save:', error)
    alert(error.response?.data?.message || 'Xatolik yuz berdi')
  } finally {
    saving.value = false
  }
}

async function handleAdvanceSave() {
  if (!advanceForm.employeeId || !advanceForm.amount) return
  saving.value = true
  try {
    await advancesApi.create(advanceForm)
    showAdvanceModal.value = false
    await loadData()
  } catch (error) {
    console.error('Failed to save advance:', error)
    alert(error.response?.data?.message || 'Xatolik yuz berdi')
  } finally {
    saving.value = false
  }
}

async function handlePay(id) {
  if (!confirm("To'lovni tasdiqlaysizmi? Avanslar avtomatik hisobdan chiqariladi.")) return
  try {
    await salaryApi.markPaid(id)
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || 'Xatolik')
  }
}

async function handleCancel(id) {
  if (!confirm('Bekor qilmoqchimisiz?')) return
  try {
    await salaryApi.cancel(id)
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || 'Xatolik')
  }
}

async function handleCancelAdvance(id) {
  if (!confirm('Avansni bekor qilmoqchimisiz?')) return
  try {
    await advancesApi.cancel(id)
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || 'Xatolik')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Ish haqi</h1>
        <p class="mt-1 text-sm text-gray-500">Oylik maosh hisoblash, avans va to'lash</p>
      </div>
      <div class="flex gap-2">
        <button @click="openAdvanceCreate" class="btn-secondary">
          <BanknotesIcon class="h-5 w-5 mr-2" /> Avans berish
        </button>
        <button @click="openCreate" class="btn-primary">
          <PlusIcon class="h-5 w-5 mr-2" /> Maosh hisoblash
        </button>
      </div>
    </div>

    <!-- Filter -->
    <div class="card">
      <div class="card-body">
        <div class="flex items-end gap-4">
          <div>
            <label class="label">Yil</label>
            <input v-model.number="filterYear" type="number" min="2020" max="2030" class="input w-28" />
          </div>
          <div>
            <label class="label">Oy</label>
            <select v-model.number="filterMonth" class="input w-40">
              <option v-for="(m, i) in months" :key="i" :value="i + 1">{{ m }}</option>
            </select>
          </div>
          <button @click="loadData" class="btn-primary">Ko'rsatish</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-32">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
    </div>

    <template v-else>
      <!-- Advances section -->
      <div v-if="advances.length > 0" class="card">
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-medium">Avanslar</h3>
          <div class="text-sm">
            Berilgan avanslar jami:
            <span class="font-semibold text-blue-600">{{ formatCurrency(totalAdvances) }}</span>
          </div>
        </div>
        <div class="card-body">
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Xodim</th>
                  <th>Kod</th>
                  <th>Sana</th>
                  <th class="text-right">Summa</th>
                  <th>Holat</th>
                  <th>Amallar</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="adv in advances" :key="adv.id">
                  <td>{{ adv.employeeName }}</td>
                  <td class="font-medium">{{ adv.employeeCode }}</td>
                  <td>{{ adv.advanceDate }}</td>
                  <td class="text-right font-semibold text-blue-600">{{ formatCurrency(adv.amount) }}</td>
                  <td>
                    <span :class="['badge', statusBadge(adv.status)]">{{ statusLabel(adv.status) }}</span>
                  </td>
                  <td>
                    <button v-if="adv.status === 'GIVEN'" @click="handleCancelAdvance(adv.id)"
                      class="text-red-600 hover:text-red-800" title="Bekor qilish">
                      <XCircleIcon class="h-5 w-5" />
                    </button>
                    <span v-else class="text-gray-400 text-sm">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Salary records -->
      <div class="card">
        <div class="card-header">
          <h3 class="text-lg font-medium">Ish haqi yozuvlari</h3>
        </div>
        <div class="card-body">
          <div v-if="records.length === 0" class="text-center py-12 text-gray-500">
            Bu davr uchun yozuvlar topilmadi
          </div>
          <div v-else class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Xodim</th>
                  <th>Kod</th>
                  <th class="text-right">Asosiy</th>
                  <th class="text-right">Bonus</th>
                  <th class="text-right">Ushlanma</th>
                  <th class="text-right">Jami</th>
                  <th class="text-right">Avans</th>
                  <th class="text-right">To'lash</th>
                  <th>Holat</th>
                  <th>Amallar</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="rec in records" :key="rec.id">
                  <td>{{ rec.employeeName }}</td>
                  <td class="font-medium">{{ rec.employeeCode }}</td>
                  <td class="text-right">{{ formatCurrency(rec.baseAmount) }}</td>
                  <td class="text-right text-green-600">+{{ formatCurrency(rec.bonusAmount) }}</td>
                  <td class="text-right text-red-600">-{{ formatCurrency(rec.deductionAmount) }}</td>
                  <td class="text-right font-semibold">{{ formatCurrency(rec.netAmount) }}</td>
                  <td class="text-right text-blue-600">
                    <template v-if="rec.advanceAmount > 0">-{{ formatCurrency(rec.advanceAmount) }}</template>
                    <template v-else>-</template>
                  </td>
                  <td class="text-right font-semibold">
                    <template v-if="rec.payAmount != null">{{ formatCurrency(rec.payAmount) }}</template>
                    <template v-else>{{ formatCurrency(rec.netAmount) }}</template>
                  </td>
                  <td>
                    <span :class="['badge', statusBadge(rec.status)]">{{ statusLabel(rec.status) }}</span>
                  </td>
                  <td>
                    <div class="flex items-center gap-2" v-if="rec.status === 'PENDING'">
                      <button @click="handlePay(rec.id)" class="text-green-600 hover:text-green-800" title="To'lash">
                        <CheckCircleIcon class="h-5 w-5" />
                      </button>
                      <button @click="handleCancel(rec.id)" class="text-red-600 hover:text-red-800" title="Bekor qilish">
                        <XCircleIcon class="h-5 w-5" />
                      </button>
                    </div>
                    <span v-else class="text-gray-400 text-sm">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </template>

    <!-- Salary Modal -->
    <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-lg font-medium">Maosh hisoblash</h3>
          <button @click="showModal = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
        </div>
        <form @submit.prevent="handleSave" class="p-6 space-y-4">
          <div>
            <label class="label">Xodim *</label>
            <select v-model="form.employeeId" @change="onEmployeeSelect" class="input">
              <option :value="null">Tanlang</option>
              <option v-for="e in employees" :key="e.id" :value="e.id">
                {{ e.fullName }} ({{ e.employeeCode }})
              </option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Yil</label>
              <input v-model.number="form.periodYear" type="number" class="input" />
            </div>
            <div>
              <label class="label">Oy</label>
              <select v-model.number="form.periodMonth" class="input">
                <option v-for="(m, i) in months" :key="i" :value="i + 1">{{ m }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="label">Asosiy maosh *</label>
            <input v-model.number="form.baseAmount" type="number" step="1000" min="0" class="input" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Bonus</label>
              <input v-model.number="form.bonusAmount" type="number" step="1000" min="0" class="input" />
            </div>
            <div>
              <label class="label">Ushlanma</label>
              <input v-model.number="form.deductionAmount" type="number" step="1000" min="0" class="input" />
            </div>
          </div>
          <div class="p-3 bg-gray-50 rounded-lg text-center">
            <span class="text-sm text-gray-500">Jami:</span>
            <span class="ml-2 text-lg font-semibold">{{ formatCurrency(netAmount) }}</span>
          </div>
          <div>
            <label class="label">Izoh</label>
            <textarea v-model="form.notes" rows="2" class="input"></textarea>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showModal = false" class="btn-secondary">Bekor qilish</button>
            <button type="submit" :disabled="saving" class="btn-primary">
              {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Advance Modal -->
    <div v-if="showAdvanceModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-lg font-medium">Avans berish</h3>
          <button @click="showAdvanceModal = false"><XMarkIcon class="h-5 w-5 text-gray-400" /></button>
        </div>
        <form @submit.prevent="handleAdvanceSave" class="p-6 space-y-4">
          <div>
            <label class="label">Xodim *</label>
            <select v-model="advanceForm.employeeId" class="input">
              <option :value="null">Tanlang</option>
              <option v-for="e in employees" :key="e.id" :value="e.id">
                {{ e.fullName }} ({{ e.employeeCode }})
              </option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Yil</label>
              <input v-model.number="advanceForm.periodYear" type="number" class="input" />
            </div>
            <div>
              <label class="label">Oy</label>
              <select v-model.number="advanceForm.periodMonth" class="input">
                <option v-for="(m, i) in months" :key="i" :value="i + 1">{{ m }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="label">Avans summasi *</label>
            <input v-model.number="advanceForm.amount" type="number" step="1000" min="0" class="input" />
          </div>
          <div>
            <label class="label">Izoh</label>
            <textarea v-model="advanceForm.notes" rows="2" class="input"></textarea>
          </div>
          <div class="flex justify-end gap-3 pt-4">
            <button type="button" @click="showAdvanceModal = false" class="btn-secondary">Bekor qilish</button>
            <button type="submit" :disabled="saving" class="btn-primary">
              {{ saving ? 'Saqlanmoqda...' : 'Avans berish' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
