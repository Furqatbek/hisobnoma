<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { subscriptionApi, unwrapData } from '@/services/api'
import { CheckCircleIcon } from '@heroicons/vue/24/solid'

const { t } = useI18n()

const loading = ref(true)
const error = ref(null)
const changed = ref(false)
const sub = ref(null)

const confirmPlan = ref(null)
const changing = ref(false)

async function load() {
  loading.value = true
  error.value = null
  try {
    sub.value = unwrapData(await subscriptionApi.get())
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    loading.value = false
  }
}

async function changePlan() {
  if (!confirmPlan.value) return
  changing.value = true
  error.value = null
  try {
    sub.value = unwrapData(await subscriptionApi.changePlan(confirmPlan.value.code))
    changed.value = true
    setTimeout(() => (changed.value = false), 4000)
    confirmPlan.value = null
  } catch (e) {
    error.value = e.response?.data?.message || e.message
    confirmPlan.value = null
  } finally {
    changing.value = false
  }
}

function formatPrice(price) {
  const n = Number(price)
  if (!n) return t('subscription.free')
  return n.toLocaleString('uz-UZ') + ' ' + t('subscription.perMonth')
}

function usagePercent(used, max) {
  if (!max) return 0
  return Math.min(100, Math.round((used / max) * 100))
}

onMounted(load)
</script>

<template>
  <div class="max-w-5xl">
    <div class="mb-6">
      <h1 class="text-2xl font-semibold text-gray-900">{{ t('subscription.title') }}</h1>
      <p class="mt-1 text-sm text-gray-500">{{ t('subscription.subtitle') }}</p>
    </div>

    <div v-if="loading" class="text-gray-500">{{ t('loading') }}</div>

    <template v-else-if="sub">
      <!-- Usage -->
      <section class="bg-white shadow rounded-lg p-6 mb-8">
        <h2 class="text-lg font-medium text-gray-900 mb-4">{{ t('subscription.usage') }}</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div>
            <div class="flex justify-between text-sm mb-1">
              <span class="text-gray-700">{{ t('subscription.users') }}</span>
              <span class="text-gray-500">{{ sub.usedUsers }} / {{ sub.maxUsers }}</span>
            </div>
            <div class="w-full bg-gray-200 rounded-full h-2">
              <div class="bg-primary-600 h-2 rounded-full"
                   :style="{ width: usagePercent(sub.usedUsers, sub.maxUsers) + '%' }"></div>
            </div>
          </div>
          <div>
            <div class="flex justify-between text-sm mb-1">
              <span class="text-gray-700">{{ t('subscription.locations') }}</span>
              <span class="text-gray-500">{{ sub.usedLocations }} / {{ sub.maxLocations }}</span>
            </div>
            <div class="w-full bg-gray-200 rounded-full h-2">
              <div class="bg-primary-600 h-2 rounded-full"
                   :style="{ width: usagePercent(sub.usedLocations, sub.maxLocations) + '%' }"></div>
            </div>
          </div>
        </div>
      </section>

      <!-- Plans -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div v-for="plan in sub.plans" :key="plan.code"
             class="bg-white shadow rounded-lg p-5 flex flex-col border-2"
             :class="plan.current ? 'border-primary-600' : 'border-transparent'">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-lg font-semibold text-gray-900">{{ plan.name }}</h3>
            <CheckCircleIcon v-if="plan.current" class="h-6 w-6 text-primary-600" />
          </div>
          <p class="text-xl font-bold text-gray-900 mb-4">{{ formatPrice(plan.monthlyPrice) }}</p>
          <ul class="text-sm text-gray-600 space-y-1 mb-5 flex-1">
            <li>{{ t('subscription.users') }}: {{ plan.maxUsers }}</li>
            <li>{{ t('subscription.locations') }}: {{ plan.maxLocations }}</li>
          </ul>
          <button v-if="plan.current" disabled
                  class="w-full rounded-md bg-gray-100 px-3 py-2 text-sm font-semibold text-gray-500 cursor-default">
            {{ t('subscription.current') }}
          </button>
          <button v-else-if="plan.switchable" @click="confirmPlan = plan"
                  class="w-full rounded-md bg-primary-600 px-3 py-2 text-sm font-semibold text-white hover:bg-primary-500">
            {{ t('subscription.upgrade') }}
          </button>
          <button v-else disabled :title="plan.blockedReason"
                  class="w-full rounded-md bg-gray-100 px-3 py-2 text-sm font-semibold text-gray-400 cursor-not-allowed">
            {{ t('subscription.blocked') }}
          </button>
          <p v-if="!plan.current && !plan.switchable" class="mt-2 text-xs text-red-500">
            {{ plan.blockedReason }}
          </p>
        </div>
      </div>

      <p class="text-xs text-gray-500 mb-4">{{ t('subscription.billingNote') }}</p>
      <p v-if="changed" class="text-sm text-green-600">{{ t('subscription.changed') }}</p>
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
    </template>
    <p v-else-if="error" class="text-sm text-red-600">{{ error }}</p>

    <!-- Confirm dialog -->
    <div v-if="confirmPlan" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="fixed inset-0 bg-gray-600 bg-opacity-50" @click="confirmPlan = null"></div>
      <div class="relative bg-white rounded-lg shadow-xl p-6 w-full max-w-md mx-4">
        <h3 class="text-lg font-medium text-gray-900 mb-2">{{ t('subscription.confirmTitle') }}</h3>
        <p class="text-sm text-gray-600 mb-6">
          {{ t('subscription.confirmText', { plan: confirmPlan.name }) }}
        </p>
        <div class="flex justify-end gap-3">
          <button @click="confirmPlan = null"
                  class="rounded-md bg-white px-4 py-2 text-sm font-semibold text-gray-700 ring-1 ring-gray-300 hover:bg-gray-50">
            {{ t('subscription.cancel') }}
          </button>
          <button @click="changePlan" :disabled="changing"
                  class="rounded-md bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-500 disabled:opacity-50">
            {{ changing ? t('saving') : t('subscription.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
