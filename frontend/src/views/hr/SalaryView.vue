<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { salaryApi, employeesApi } from '@/services/api'
import { PlusIcon, CheckCircleIcon, XCircleIcon, XMarkIcon } from '@heroicons/vue/24/outline'

const records = ref([])
const employees = ref([])
const loading = ref(true)
const showModal = ref(false)
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

const netAmount = computed(() => {
  return (form.baseAmount || 0) + (form.bonusAmount || 0) - (form.deductionAmount || 0)
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
    default: return 'badge-info'
  }
}

function statusLabel(status) {
  switch (status) {
    case 'PAID': return "To'langan"
    case 'PENDING': return 'Kutilmoqda'
    case 'CANCELLED': return 'Bekor qilingan'
    default: return status
  }
}

async function loadData() {
  loading.value = true
  try {
    const [salaryRes, empRes] = await Promise.all([
      salaryApi.getByPeriod(filterYear.value, filterMonth.value, { size: 200 }),
      employeesApi.getActive()
    ])
    records.value = salaryRes.data.content || salaryRes.data.data?.content || []
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

async function handlePay(id) {
  if (!confirm("To'lovni tasdiqlaysizmi?")) return
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

onMounted(loadData)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Ish haqi</h1>
        <p class="mt-1 text-sm text-gray-500">Oylik maosh hisoblash va to'lash</p>
      </div>
      <button @click="openCreate" class="btn-primary">
        <PlusIcon class="h-5 w-5 mr-2" /> Maosh hisoblash
      </button>
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

    <div class="card">
      <div class="card-body">
        <div v-if="loading" class="flex items-center justify-center h-32">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
        <div v-else-if="records.length === 0" class="text-center py-12 text-gray-500">
          Bu davr uchun yozuvlar topilmadi
        </div>
        <div v-else class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Xodim</th>
                <th>Kod</th>
                <th>Asosiy</th>
                <th>Bonus</th>
                <th>Ushlanma</th>
                <th>Jami</th>
                <th>Holat</th>
                <th>Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rec in records" :key="rec.id">
                <td>{{ rec.employeeName }}</td>
                <td class="font-medium">{{ rec.employeeCode }}</td>
                <td>{{ formatCurrency(rec.baseAmount) }}</td>
                <td class="text-green-600">+{{ formatCurrency(rec.bonusAmount) }}</td>
                <td class="text-red-600">-{{ formatCurrency(rec.deductionAmount) }}</td>
                <td class="font-semibold">{{ formatCurrency(rec.netAmount) }}</td>
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

    <!-- Modal -->
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
  </div>
</template>
