<script setup>
import { ref, computed, watchEffect, onMounted, onUnmounted } from 'vue'
import { RouterView, RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { webOrdersApi } from '@/services/api'
import {
  HomeIcon,
  CubeIcon,
  ShoppingCartIcon,
  UsersIcon,
  TruckIcon,
  ChartBarIcon,
  Cog6ToothIcon,
  ArrowRightOnRectangleIcon,
  Bars3Icon,
  XMarkIcon,
  ChevronDownIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  BuildingStorefrontIcon,
  TagIcon,
  ArchiveBoxIcon,
  ClipboardDocumentListIcon,
  UserGroupIcon,
  DocumentTextIcon,
  CurrencyDollarIcon,
  ShieldCheckIcon,
  ComputerDesktopIcon,
  ClockIcon,
  KeyIcon,
  BanknotesIcon,
  BriefcaseIcon,
  PaperAirplaneIcon,
  ChatBubbleLeftRightIcon,
  MapPinIcon,
  MapIcon,
  WrenchScrewdriverIcon,
  AdjustmentsHorizontalIcon,
  CreditCardIcon,
  InboxArrowDownIcon,
  GlobeAltIcon,
  MegaphoneIcon,
  TicketIcon,
  ReceiptPercentIcon,
  GiftIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const sidebarOpen = ref(false)
const sidebarCollapsed = ref(false)
const expandedMenus = ref([])

// New online orders badge (polled every 60s; 403 means no permission - ignore)
const newOrderCount = ref(0)
let orderCountTimer = null
async function refreshNewOrderCount() {
  try {
    const response = await webOrdersApi.getNewCount()
    newOrderCount.value = response.data.data?.newOrders || 0
  } catch (e) {
    newOrderCount.value = 0
  }
}
onMounted(() => {
  refreshNewOrderCount()
  orderCountTimer = setInterval(refreshNewOrderCount, 60000)
})
onUnmounted(() => clearInterval(orderCountTimer))

const navigation = computed(() => [
  {
    name: t('nav.dashboard'),
    href: '/dashboard',
    icon: HomeIcon,
    current: route.path === '/dashboard'
  },
  {
    name: t('nav.inventory'),
    icon: CubeIcon,
    key: 'inventory',
    children: [
      { name: t('nav.products'), href: '/inventory/products', icon: TagIcon },
      { name: t('nav.categories'), href: '/inventory/categories', icon: ArchiveBoxIcon },
      { name: t('nav.brands'), href: '/inventory/brands', icon: BuildingStorefrontIcon },
      { name: t('nav.stock'), href: '/inventory/stock', icon: ClipboardDocumentListIcon },
      { name: t('nav.warehouses'), href: '/inventory/warehouses', icon: BuildingStorefrontIcon },
      { name: t('nav.unitsOfMeasure'), href: '/inventory/uom', icon: TagIcon },
      { name: t('nav.receiving'), href: '/inventory/receiving', icon: InboxArrowDownIcon },
      { name: t('nav.inventoryPlanning'), href: '/inventory/planning', icon: ChartBarIcon }
    ]
  },
  {
    name: t('nav.pos'),
    icon: ShoppingCartIcon,
    key: 'pos',
    children: [
      { name: t('nav.cashRegister'), href: '/pos', icon: ShoppingCartIcon },
      { name: t('nav.salesHistory'), href: '/pos/transactions', icon: ClockIcon },
      { name: t('nav.shifts'), href: '/pos/shifts', icon: ClipboardDocumentListIcon },
      { name: t('nav.promotions'), href: '/pos/promotions', icon: ReceiptPercentIcon },
      { name: t('nav.coupons'), href: '/pos/coupons', icon: TicketIcon },
      { name: t('nav.priceLists'), href: '/pos/price-lists', icon: CurrencyDollarIcon }
    ]
  },
  {
    name: t('nav.webCatalog'),
    href: '/web-catalog',
    icon: GlobeAltIcon,
    current: route.path === '/web-catalog'
  },
  {
    name: t('nav.webOrders'),
    href: '/web-orders',
    icon: InboxArrowDownIcon,
    current: route.path === '/web-orders',
    badge: newOrderCount.value
  },
  {
    name: t('nav.webCustomers'),
    href: '/web-customers',
    icon: UserGroupIcon,
    current: route.path === '/web-customers'
  },
  {
    name: t('nav.customerSegments'),
    href: '/web-customer-segments',
    icon: ChartBarIcon,
    current: route.path === '/web-customer-segments'
  },
  {
    name: t('nav.webCampaigns'),
    href: '/web-campaigns',
    icon: MegaphoneIcon,
    current: route.path === '/web-campaigns'
  },
  {
    name: t('nav.loyaltySettings'),
    href: '/web-loyalty-settings',
    icon: GiftIcon,
    current: route.path === '/web-loyalty-settings'
  },
  {
    name: t('nav.customers'),
    icon: UsersIcon,
    key: 'customers',
    children: [
      { name: t('nav.customerList'), href: '/customers', icon: UsersIcon },
      { name: t('nav.customerHistory'), href: '/customers/history', icon: ClockIcon }
    ]
  },
  {
    name: t('nav.purchases'),
    icon: TruckIcon,
    key: 'purchases',
    children: [
      { name: t('nav.suppliers'), href: '/purchases/suppliers', icon: UserGroupIcon },
      { name: t('nav.purchaseOrders'), href: '/purchases/orders', icon: DocumentTextIcon },
      { name: t('nav.supplierHistory'), href: '/purchases/history', icon: ClockIcon }
    ]
  },
  {
    name: t('nav.finance'),
    icon: BanknotesIcon,
    key: 'finance',
    children: [
      { name: t('nav.debtors'), href: '/finance/debtors', icon: UsersIcon },
      { name: t('nav.payments'), href: '/finance/payments', icon: BanknotesIcon },
      { name: t('nav.arInvoices'), href: '/finance/invoices', icon: DocumentTextIcon },
      { name: t('nav.expenses'), href: '/finance/expenses', icon: CurrencyDollarIcon },
      { name: t('nav.chartOfAccounts'), href: '/finance/accounts', icon: ClipboardDocumentListIcon },
      { name: t('nav.journalEntries'), href: '/finance/journal-entries', icon: DocumentTextIcon },
      { name: t('nav.bankAccounts'), href: '/finance/bank-accounts', icon: BuildingStorefrontIcon },
      { name: t('nav.bankTransactions'), href: '/finance/bank-transactions', icon: BanknotesIcon },
      { name: t('nav.creditNotes'), href: '/finance/credit-notes', icon: DocumentTextIcon },
      { name: t('nav.currencies'), href: '/finance/currencies', icon: CurrencyDollarIcon },
      { name: t('nav.taxCodes'), href: '/finance/tax-codes', icon: ClipboardDocumentListIcon },
      { name: t('nav.fiscalPeriods'), href: '/finance/fiscal-periods', icon: ClockIcon },
      { name: t('nav.recurringJournals'), href: '/finance/recurring-journals', icon: DocumentTextIcon }
    ]
  },
  {
    name: t('nav.hr'),
    icon: BriefcaseIcon,
    key: 'hr',
    children: [
      { name: t('nav.employees'), href: '/hr/employees', icon: UserGroupIcon },
      { name: t('nav.departments'), href: '/hr/departments', icon: BuildingStorefrontIcon },
      { name: t('nav.positions'), href: '/hr/positions', icon: BriefcaseIcon },
      { name: t('nav.salary'), href: '/hr/salary', icon: BanknotesIcon },
      { name: t('nav.employeeHistory'), href: '/hr/history', icon: ClockIcon }
    ]
  },
  {
    name: t('nav.distribution'),
    icon: TruckIcon,
    key: 'distribution',
    children: [
      { name: t('nav.distributionKpi'), href: '/distribution/kpi', icon: ChartBarIcon },
      { name: t('nav.distributionOrders'), href: '/distribution/orders', icon: ClipboardDocumentListIcon },
      { name: t('nav.distributionVanLoadouts'), href: '/distribution/van-loadouts', icon: TruckIcon },
      { name: t('nav.distributionRoutes'), href: '/distribution/routes', icon: MapIcon },
      { name: t('nav.distributionVisits'), href: '/distribution/visits', icon: MapPinIcon },
      { name: t('nav.distributionAgents'), href: '/distribution/agents', icon: UserGroupIcon }
    ]
  },
  {
    name: t('nav.reports'),
    icon: ChartBarIcon,
    key: 'reports',
    children: [
      { name: t('nav.salesReport'), href: '/reports/sales', icon: CurrencyDollarIcon },
      { name: t('nav.inventoryReport'), href: '/reports/inventory', icon: ClipboardDocumentListIcon },
      { name: t('nav.financialReport'), href: '/reports/financial', icon: DocumentTextIcon },
      { name: t('nav.salaryReport'), href: '/reports/salary', icon: BanknotesIcon }
    ]
  },
  {
    name: t('nav.admin'),
    icon: ShieldCheckIcon,
    key: 'admin',
    children: [
      { name: t('nav.users'), href: '/admin/users', icon: UserGroupIcon },
      { name: t('nav.rolesAndPermissions'), href: '/admin/roles', icon: KeyIcon },
      { name: t('nav.terminals'), href: '/admin/terminals', icon: ComputerDesktopIcon },
      { name: t('nav.regions'), href: '/admin/regions', icon: MapIcon },
      { name: t('nav.villages'), href: '/admin/villages', icon: MapPinIcon },
      { name: t('nav.telegramBot'), href: '/admin/telegram', icon: PaperAirplaneIcon },
      { name: t('nav.sms'), href: '/admin/sms', icon: ChatBubbleLeftRightIcon },
      { name: t('nav.settings'), href: '/admin/settings', icon: Cog6ToothIcon },
      { name: t('nav.systemSettings'), href: '/admin/system-settings', icon: WrenchScrewdriverIcon },
      { name: t('nav.tenantSettings'), href: '/admin/tenant-settings', icon: AdjustmentsHorizontalIcon },
      { name: t('nav.subscription'), href: '/admin/subscription', icon: CreditCardIcon },
      { name: t('nav.auditLog'), href: '/admin/audit-logs', icon: DocumentTextIcon }
    ]
  }
])

// A nav link is visible unless the route it points to declares a meta.permission
// the current user lacks. This keeps the sidebar in sync with the router guards
// (single source of truth) — links with no permission requirement always show,
// and SUPER_ADMIN/ADMIN (who hold every permission) see everything.
function canAccessHref(href) {
  if (!href) return true
  let resolved
  try {
    resolved = router.resolve(href)
  } catch (e) {
    return true
  }
  const guarded = [...resolved.matched].reverse().find(r => r.meta && r.meta.permission)
  return !guarded || authStore.hasPermission(guarded.meta.permission)
}

// Navigation with items/groups the user cannot access removed. A group is kept
// only if at least one of its children remains visible.
const visibleNavigation = computed(() =>
  navigation.value
    .map(item => {
      if (!item.children) {
        return canAccessHref(item.href) ? item : null
      }
      const children = item.children.filter(child => canAccessHref(child.href))
      return children.length ? { ...item, children } : null
    })
    .filter(Boolean)
)

function toggleMenu(key) {
  if (expandedMenus.value.includes(key)) {
    expandedMenus.value = []
  } else {
    expandedMenus.value = [key]
  }
}

function isMenuExpanded(key) {
  return expandedMenus.value.includes(key)
}

function isChildActive(children) {
  return children?.some(child => route.path.startsWith(child.href))
}

// Auto-expand the menu containing the active route
watchEffect(() => {
  const activeGroup = visibleNavigation.value.find(
    item => item.children && isChildActive(item.children)
  )
  if (activeGroup) {
    expandedMenus.value = [activeGroup.key]
  }
})
</script>

<template>
  <div class="min-h-screen bg-gray-100 lg:flex">
    <!-- Mobile sidebar overlay -->
    <div
      v-if="sidebarOpen"
      class="fixed inset-0 z-40 bg-gray-600 bg-opacity-75 lg:hidden"
      @click="sidebarOpen = false"
    />

    <!-- Sidebar -->
    <aside
      :class="[
        'fixed inset-y-0 left-0 z-50 bg-white shadow-lg transform transition-all duration-300 ease-in-out lg:translate-x-0 lg:sticky lg:top-0 lg:h-screen lg:inset-auto lg:flex-shrink-0 flex flex-col',
        sidebarCollapsed ? 'lg:w-16 w-64' : 'w-64',
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      ]"
    >
      <!-- Logo -->
      <div class="flex items-center justify-between h-16 px-4 border-b border-gray-200">
        <RouterLink to="/dashboard" class="flex items-center space-x-2 overflow-hidden">
          <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center flex-shrink-0">
            <span class="text-white font-bold text-lg">H</span>
          </div>
          <span v-if="!sidebarCollapsed" class="text-xl font-bold text-gray-900 whitespace-nowrap">{{ $t('appName') }}</span>
        </RouterLink>
        <button
          class="lg:hidden p-2 rounded-md text-gray-400 hover:text-gray-500"
          @click="sidebarOpen = false"
        >
          <XMarkIcon class="h-6 w-6" />
        </button>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-2 py-4 space-y-1 overflow-y-auto overflow-x-hidden">
        <template v-for="item in visibleNavigation" :key="item.name">
          <!-- Simple link -->
          <RouterLink
            v-if="!item.children"
            :to="item.href"
            :class="[
              'group flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors',
              item.current
                ? 'bg-primary-50 text-primary-700'
                : 'text-gray-700 hover:bg-gray-100'
            ]"
            :title="sidebarCollapsed ? item.name : ''"
          >
            <component
              :is="item.icon"
              :class="[
                'h-5 w-5 flex-shrink-0',
                sidebarCollapsed ? '' : 'mr-3',
                item.current ? 'text-primary-600' : 'text-gray-400 group-hover:text-gray-500'
              ]"
            />
            <span v-if="!sidebarCollapsed">{{ item.name }}</span>
            <span
              v-if="!sidebarCollapsed && item.badge > 0"
              class="ml-auto inline-flex items-center justify-center min-w-[20px] px-1.5 py-0.5 text-xs font-bold rounded-full bg-red-500 text-white"
            >{{ item.badge }}</span>
          </RouterLink>

          <!-- Expandable menu -->
          <div v-else>
            <button
              @click="sidebarCollapsed ? (sidebarCollapsed = false, toggleMenu(item.key)) : toggleMenu(item.key)"
              :class="[
                'w-full group flex items-center justify-between px-3 py-2 text-sm font-medium rounded-lg transition-colors',
                isChildActive(item.children)
                  ? 'bg-primary-50 text-primary-700'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
              :title="sidebarCollapsed ? item.name : ''"
            >
              <div class="flex items-center">
                <component
                  :is="item.icon"
                  :class="[
                    'h-5 w-5 flex-shrink-0',
                    sidebarCollapsed ? '' : 'mr-3',
                    isChildActive(item.children) ? 'text-primary-600' : 'text-gray-400'
                  ]"
                />
                <span v-if="!sidebarCollapsed">{{ item.name }}</span>
              </div>
              <ChevronDownIcon
                v-if="!sidebarCollapsed"
                :class="[
                  'h-4 w-4 transition-transform duration-200',
                  isMenuExpanded(item.key) ? 'rotate-180' : ''
                ]"
              />
            </button>

            <div
              class="overflow-hidden transition-all duration-300 ease-in-out"
              :style="{ maxHeight: isMenuExpanded(item.key) && !sidebarCollapsed ? (item.children.length * 80 + 16) + 'px' : '0px' }"
            >
              <div class="mt-1 ml-4 pl-4 border-l border-gray-200 space-y-1">
                <RouterLink
                  v-for="child in item.children"
                  :key="child.name"
                  :to="child.href"
                  :class="[
                    'group flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors',
                    route.path === child.href
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-600 hover:bg-gray-100'
                  ]"
                >
                  <component
                    :is="child.icon"
                    :class="[
                      'mr-3 h-4 w-4 flex-shrink-0',
                      route.path === child.href ? 'text-primary-600' : 'text-gray-400'
                    ]"
                  />
                  {{ child.name }}
                </RouterLink>
              </div>
            </div>
          </div>
        </template>
      </nav>

      <!-- Collapse button (desktop only) -->
      <div class="hidden lg:block border-t border-gray-200 p-2">
        <button
          @click="sidebarCollapsed = !sidebarCollapsed"
          class="w-full flex items-center justify-center p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
        >
          <ChevronLeftIcon v-if="!sidebarCollapsed" class="h-5 w-5" />
          <ChevronRightIcon v-else class="h-5 w-5" />
        </button>
      </div>

      <!-- User section -->
      <div class="border-t border-gray-200 p-3">
        <div class="flex items-center" :class="sidebarCollapsed ? 'justify-center' : ''">
          <RouterLink v-if="!sidebarCollapsed" to="/profile" class="flex items-center flex-1 min-w-0 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
              <span class="text-primary-700 font-medium text-sm">
                {{ authStore.user?.username?.charAt(0).toUpperCase() || 'U' }}
              </span>
            </div>
            <div class="ml-3 flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-900 truncate">
                {{ authStore.user?.username || $t('user') }}
              </p>
              <p class="text-xs text-gray-500 truncate">
                {{ authStore.user?.roles?.[0] || $t('user') }}
              </p>
            </div>
          </RouterLink>
          <button
            v-if="sidebarCollapsed"
            @click="authStore.logout()"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-500 hover:bg-gray-100"
            :title="$t('logout')"
          >
            <ArrowRightOnRectangleIcon class="h-5 w-5" />
          </button>
          <button
            v-else
            @click="authStore.logout()"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-500 hover:bg-gray-100"
            :title="$t('logout')"
          >
            <ArrowRightOnRectangleIcon class="h-5 w-5" />
          </button>
        </div>
      </div>
    </aside>

    <!-- Main content -->
    <div class="flex-1 min-w-0">
      <!-- Top bar -->
      <header class="sticky top-0 z-30 bg-white shadow-sm">
        <div class="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
          <button
            class="lg:hidden p-2 rounded-md text-gray-400 hover:text-gray-500"
            @click="sidebarOpen = true"
          >
            <Bars3Icon class="h-6 w-6" />
          </button>

          <div class="flex-1 flex justify-end items-center space-x-4">
            <span class="text-sm text-gray-500">
              {{ new Date().toLocaleDateString('uz-UZ', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) }}
            </span>
          </div>
        </div>
      </header>

      <!-- Page content -->
      <main class="p-4 sm:p-6 lg:p-8">
        <RouterView />
      </main>
    </div>
  </div>
</template>
