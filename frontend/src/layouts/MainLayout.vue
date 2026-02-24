<script setup>
import { ref, computed, watchEffect } from 'vue'
import { RouterView, RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
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
  MapIcon
} from '@heroicons/vue/24/outline'

const { t } = useI18n()
const authStore = useAuthStore()
const route = useRoute()

const sidebarOpen = ref(false)
const expandedMenus = ref([])

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
      { name: t('nav.unitsOfMeasure'), href: '/inventory/uom', icon: TagIcon }
    ]
  },
  {
    name: t('nav.pos'),
    icon: ShoppingCartIcon,
    key: 'pos',
    children: [
      { name: t('nav.cashRegister'), href: '/pos', icon: ShoppingCartIcon },
      { name: t('nav.salesHistory'), href: '/pos/transactions', icon: ClockIcon },
      { name: t('nav.shifts'), href: '/pos/shifts', icon: ClipboardDocumentListIcon }
    ]
  },
  {
    name: t('nav.customers'),
    href: '/customers',
    icon: UsersIcon,
    current: route.path.startsWith('/customers')
  },
  {
    name: t('nav.purchases'),
    icon: TruckIcon,
    key: 'purchases',
    children: [
      { name: t('nav.suppliers'), href: '/purchases/suppliers', icon: UserGroupIcon },
      { name: t('nav.purchaseOrders'), href: '/purchases/orders', icon: DocumentTextIcon }
    ]
  },
  {
    name: t('nav.finance'),
    icon: BanknotesIcon,
    key: 'finance',
    children: [
      { name: t('nav.debtors'), href: '/finance/debtors', icon: UsersIcon },
      { name: t('nav.payments'), href: '/finance/payments', icon: BanknotesIcon },
      { name: t('nav.expenses'), href: '/finance/expenses', icon: CurrencyDollarIcon }
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
      { name: t('nav.salary'), href: '/hr/salary', icon: BanknotesIcon }
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
      { name: t('nav.auditLog'), href: '/admin/audit-logs', icon: DocumentTextIcon }
    ]
  }
])

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
  const activeGroup = navigation.value.find(
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
        'fixed inset-y-0 left-0 z-50 w-64 bg-white shadow-lg transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-auto lg:flex-shrink-0',
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      ]"
    >
      <!-- Logo -->
      <div class="flex items-center justify-between h-16 px-6 border-b border-gray-200">
        <RouterLink to="/dashboard" class="flex items-center space-x-2">
          <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
            <span class="text-white font-bold text-lg">H</span>
          </div>
          <span class="text-xl font-bold text-gray-900">{{ $t('appName') }}</span>
        </RouterLink>
        <button
          class="lg:hidden p-2 rounded-md text-gray-400 hover:text-gray-500"
          @click="sidebarOpen = false"
        >
          <XMarkIcon class="h-6 w-6" />
        </button>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
        <template v-for="item in navigation" :key="item.name">
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
          >
            <component
              :is="item.icon"
              :class="[
                'mr-3 h-5 w-5 flex-shrink-0',
                item.current ? 'text-primary-600' : 'text-gray-400 group-hover:text-gray-500'
              ]"
            />
            {{ item.name }}
          </RouterLink>

          <!-- Expandable menu -->
          <div v-else>
            <button
              @click="toggleMenu(item.key)"
              :class="[
                'w-full group flex items-center justify-between px-3 py-2 text-sm font-medium rounded-lg transition-colors',
                isChildActive(item.children)
                  ? 'bg-primary-50 text-primary-700'
                  : 'text-gray-700 hover:bg-gray-100'
              ]"
            >
              <div class="flex items-center">
                <component
                  :is="item.icon"
                  :class="[
                    'mr-3 h-5 w-5 flex-shrink-0',
                    isChildActive(item.children) ? 'text-primary-600' : 'text-gray-400'
                  ]"
                />
                {{ item.name }}
              </div>
              <ChevronDownIcon
                :class="[
                  'h-4 w-4 transition-transform',
                  isMenuExpanded(item.key) ? 'rotate-180' : ''
                ]"
              />
            </button>

            <div
              v-show="isMenuExpanded(item.key)"
              class="mt-1 ml-4 pl-4 border-l border-gray-200 space-y-1"
            >
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
        </template>
      </nav>

      <!-- User section -->
      <div class="border-t border-gray-200 p-4">
        <div class="flex items-center">
          <RouterLink to="/profile" class="flex items-center flex-1 min-w-0 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
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
