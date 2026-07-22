<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { tenantSettingsApi, unwrapData } from '@/services/api'

const { t } = useI18n()

const loading = ref(true)
const saving = ref(false)
const error = ref(null)
const saved = ref(false)

// Typed model over the raw tenant-setting keys (single source of truth for key names).
const KEYS = {
  loyaltyEnabled: 'loyalty.enabled',
  earnPercent: 'loyalty.earn_percent',
  expiryDays: 'loyalty.expiry_days',
  minRedeem: 'loyalty.min_redeem',
  maxRedeemPercent: 'loyalty.max_redeem_percent_of_order',
  referralEnabled: 'referral.enabled',
  rewardReferrer: 'referral.reward_referrer',
  rewardReferred: 'referral.reward_referred',
  monthlyCap: 'referral.monthly_cap'
}

const form = ref({
  loyaltyEnabled: false,
  earnPercent: 1,
  expiryDays: 180,
  minRedeem: 5000,
  maxRedeemPercent: 50,
  referralEnabled: false,
  rewardReferrer: 10000,
  rewardReferred: 5000,
  monthlyCap: 0
})

// Keys that already exist server-side (existing → updateValue, missing → create).
const existing = ref(new Set())

async function load() {
  loading.value = true
  error.value = null
  try {
    const map = unwrapData(await tenantSettingsApi.getMap()) || {}
    existing.value = new Set(Object.keys(map))
    const num = (k, d) => {
      const v = parseFloat(map[KEYS[k]])
      return Number.isFinite(v) ? v : d
    }
    form.value = {
      loyaltyEnabled: map[KEYS.loyaltyEnabled] === 'true',
      earnPercent: num('earnPercent', 1),
      expiryDays: num('expiryDays', 180),
      minRedeem: num('minRedeem', 5000),
      maxRedeemPercent: num('maxRedeemPercent', 50),
      referralEnabled: map[KEYS.referralEnabled] === 'true',
      rewardReferrer: num('rewardReferrer', 10000),
      rewardReferred: num('rewardReferred', 5000),
      monthlyCap: num('monthlyCap', 0)
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    loading.value = false
  }
}

function clamp(v, min, max) {
  const n = Number(v)
  if (!Number.isFinite(n)) return min
  return Math.min(max, Math.max(min, n))
}

async function save() {
  saving.value = true
  error.value = null
  saved.value = false
  // Client-side sanity clamps mirror the backend's server-side caps.
  form.value.earnPercent = clamp(form.value.earnPercent, 0, 100)
  form.value.maxRedeemPercent = clamp(form.value.maxRedeemPercent, 0, 100)
  form.value.expiryDays = clamp(form.value.expiryDays, 0, 3650)
  form.value.minRedeem = clamp(form.value.minRedeem, 0, 100000000)
  form.value.rewardReferrer = clamp(form.value.rewardReferrer, 0, 100000000)
  form.value.rewardReferred = clamp(form.value.rewardReferred, 0, 100000000)
  form.value.monthlyCap = clamp(form.value.monthlyCap, 0, 100000)

  const values = {
    [KEYS.loyaltyEnabled]: String(form.value.loyaltyEnabled),
    [KEYS.earnPercent]: String(form.value.earnPercent),
    [KEYS.expiryDays]: String(form.value.expiryDays),
    [KEYS.minRedeem]: String(form.value.minRedeem),
    [KEYS.maxRedeemPercent]: String(form.value.maxRedeemPercent),
    [KEYS.referralEnabled]: String(form.value.referralEnabled),
    [KEYS.rewardReferrer]: String(form.value.rewardReferrer),
    [KEYS.rewardReferred]: String(form.value.rewardReferred),
    [KEYS.monthlyCap]: String(form.value.monthlyCap)
  }
  try {
    for (const [key, value] of Object.entries(values)) {
      if (existing.value.has(key)) {
        await tenantSettingsApi.updateValue(key, value)
      } else {
        await tenantSettingsApi.create({ settingKey: key, settingValue: value, category: 'loyalty' })
        existing.value.add(key)
      }
    }
    saved.value = true
    setTimeout(() => (saved.value = false), 3000)
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="max-w-3xl">
    <div class="mb-6">
      <h1 class="text-2xl font-semibold text-gray-900">{{ t('loyalty.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ t('loyalty.subtitle') }}</p>
    </div>

    <div v-if="loading" class="text-gray-500">{{ t('loading') }}</div>

    <form v-else class="space-y-8" @submit.prevent="save">
      <!-- Cashback program -->
      <section class="bg-white shadow rounded-lg p-6 space-y-5">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-lg font-medium text-gray-900">{{ t('loyalty.cashbackSection') }}</h2>
            <p class="text-sm text-gray-500">{{ t('loyalty.cashbackHint') }}</p>
          </div>
          <label class="inline-flex items-center cursor-pointer">
            <input v-model="form.loyaltyEnabled" type="checkbox" class="sr-only peer" />
            <div class="relative w-11 h-6 bg-gray-200 peer-checked:bg-primary-600 rounded-full
                        after:content-[''] after:absolute after:top-0.5 after:start-0.5 after:bg-white
                        after:rounded-full after:h-5 after:w-5 after:transition-all
                        peer-checked:after:translate-x-full"></div>
          </label>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-5" :class="{ 'opacity-50': !form.loyaltyEnabled }">
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('loyalty.earnPercent') }}</label>
            <input v-model.number="form.earnPercent" type="number" min="0" max="100" step="1"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
            <p class="mt-1 text-xs text-gray-500">{{ t('loyalty.earnPercentHint') }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('loyalty.expiryDays') }}</label>
            <input v-model.number="form.expiryDays" type="number" min="0" max="3650" step="1"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
            <p class="mt-1 text-xs text-gray-500">{{ t('loyalty.expiryDaysHint') }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('loyalty.minRedeem') }}</label>
            <input v-model.number="form.minRedeem" type="number" min="0" step="500"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('loyalty.maxRedeemPercent') }}</label>
            <input v-model.number="form.maxRedeemPercent" type="number" min="0" max="100" step="5"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
            <p class="mt-1 text-xs text-gray-500">{{ t('loyalty.maxRedeemPercentHint') }}</p>
          </div>
        </div>
      </section>

      <!-- Referral program -->
      <section class="bg-white shadow rounded-lg p-6 space-y-5">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-lg font-medium text-gray-900">{{ t('loyalty.referralSection') }}</h2>
            <p class="text-sm text-gray-500">{{ t('loyalty.referralHint') }}</p>
          </div>
          <label class="inline-flex items-center cursor-pointer">
            <input v-model="form.referralEnabled" type="checkbox" class="sr-only peer" />
            <div class="relative w-11 h-6 bg-gray-200 peer-checked:bg-primary-600 rounded-full
                        after:content-[''] after:absolute after:top-0.5 after:start-0.5 after:bg-white
                        after:rounded-full after:h-5 after:w-5 after:transition-all
                        peer-checked:after:translate-x-full"></div>
          </label>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-5" :class="{ 'opacity-50': !form.referralEnabled }">
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('loyalty.rewardReferrer') }}</label>
            <input v-model.number="form.rewardReferrer" type="number" min="0" step="1000"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('loyalty.rewardReferred') }}</label>
            <input v-model.number="form.rewardReferred" type="number" min="0" step="1000"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">{{ t('loyalty.monthlyCap') }}</label>
            <input v-model.number="form.monthlyCap" type="number" min="0" step="1"
                   class="mt-1 block w-full rounded-md border-gray-300 shadow-sm" />
            <p class="mt-1 text-xs text-gray-500">{{ t('loyalty.monthlyCapHint') }}</p>
          </div>
        </div>
      </section>

      <div class="flex items-center gap-4">
        <button type="submit" :disabled="saving"
                class="inline-flex justify-center rounded-md bg-primary-600 px-4 py-2 text-sm
                       font-semibold text-white shadow-sm hover:bg-primary-500 disabled:opacity-50">
          {{ saving ? t('saving') : t('save') }}
        </button>
        <span v-if="saved" class="text-sm text-green-600">{{ t('loyalty.savedMessage') }}</span>
        <span v-if="error" class="text-sm text-red-600">{{ error }}</span>
      </div>
    </form>
  </div>
</template>
