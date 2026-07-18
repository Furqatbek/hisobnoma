import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getAccessToken } from '@/services/tokenStorage'

// Layouts
import MainLayout from '@/layouts/MainLayout.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'

// Auth Views
import LoginView from '@/views/auth/LoginView.vue'

// Main Views
import DashboardView from '@/views/dashboard/DashboardView.vue'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'login',
        component: LoginView,
        meta: { guest: true }
      }
    ]
  },
  {
    path: '/forgot-password',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'forgot-password',
        component: () => import('@/views/auth/ForgotPasswordView.vue'),
        meta: { guest: true }
      }
    ]
  },
  {
    path: '/reset-password',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'reset-password',
        component: () => import('@/views/auth/ResetPasswordView.vue'),
        meta: { guest: true }
      }
    ]
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileView.vue')
      },
      // Inventory
      {
        path: 'inventory',
        children: [
          {
            path: 'products',
            name: 'products',
            component: () => import('@/views/inventory/ProductsView.vue')
          },
          {
            path: 'products/new',
            name: 'product-create',
            component: () => import('@/views/inventory/ProductFormView.vue')
          },
          {
            path: 'products/:id/edit',
            name: 'product-edit',
            component: () => import('@/views/inventory/ProductFormView.vue')
          },
          {
            path: 'categories',
            name: 'categories',
            component: () => import('@/views/inventory/CategoriesView.vue')
          },
          {
            path: 'brands',
            name: 'brands',
            component: () => import('@/views/inventory/BrandsView.vue')
          },
          {
            path: 'stock',
            name: 'stock',
            component: () => import('@/views/inventory/StockView.vue')
          },
          {
            path: 'warehouses',
            name: 'warehouses',
            component: () => import('@/views/inventory/WarehousesView.vue')
          },
          {
            path: 'uom',
            name: 'uom',
            component: () => import('@/views/inventory/UOMView.vue')
          },
          {
            path: 'receiving',
            name: 'receiving',
            component: () => import('@/views/inventory/ReceivingView.vue')
          },
          {
            path: 'receiving/new',
            name: 'receiving-create',
            component: () => import('@/views/inventory/ReceivingFormView.vue')
          },
          {
            path: 'planning',
            name: 'inventory-planning',
            component: () => import('@/views/inventory/InventoryPlanningView.vue')
          }
        ]
      },
      // POS
      {
        path: 'pos',
        name: 'pos',
        component: () => import('@/views/pos/POSView.vue')
      },
      {
        path: 'pos/transactions',
        name: 'transactions',
        component: () => import('@/views/pos/TransactionsView.vue')
      },
      {
        path: 'pos/shifts',
        name: 'shifts',
        component: () => import('@/views/pos/ShiftsView.vue')
      },
      {
        path: 'pos/promotions',
        name: 'promotions',
        component: () => import('@/views/pos/PromotionsView.vue')
      },
      {
        path: 'pos/coupons',
        name: 'coupons',
        component: () => import('@/views/pos/CouponsView.vue')
      },
      {
        path: 'pos/price-lists',
        name: 'price-lists',
        component: () => import('@/views/pos/PriceListsView.vue')
      },
      // Web catalog (online shop)
      {
        path: 'web-catalog',
        name: 'web-catalog',
        component: () => import('@/views/web/WebCatalogView.vue')
      },
      {
        path: 'web-orders',
        name: 'web-orders',
        component: () => import('@/views/web/WebOrdersView.vue')
      },
      {
        path: 'web-customers',
        name: 'web-customers',
        component: () => import('@/views/web/WebCustomersView.vue')
      },
      {
        path: 'web-campaigns',
        name: 'web-campaigns',
        component: () => import('@/views/web/WebCampaignsView.vue')
      },
      // Customers
      {
        path: 'customers',
        name: 'customers',
        component: () => import('@/views/customers/CustomersView.vue')
      },
      {
        path: 'customers/new',
        name: 'customer-create',
        component: () => import('@/views/customers/CustomerFormView.vue')
      },
      {
        path: 'customers/:id/edit',
        name: 'customer-edit',
        component: () => import('@/views/customers/CustomerFormView.vue')
      },
      {
        path: 'customers/history',
        name: 'customer-history',
        component: () => import('@/views/customers/CustomerHistoryView.vue')
      },
      {
        path: 'customers/history/:id',
        name: 'customer-history-detail',
        component: () => import('@/views/customers/CustomerHistoryView.vue')
      },
      // Purchases
      {
        path: 'purchases',
        children: [
          {
            path: 'suppliers',
            name: 'suppliers',
            component: () => import('@/views/purchases/SuppliersView.vue')
          },
          {
            path: 'orders',
            name: 'purchase-orders',
            component: () => import('@/views/purchases/PurchaseOrdersView.vue')
          },
          {
            path: 'orders/new',
            name: 'purchase-order-create',
            component: () => import('@/views/purchases/PurchaseOrderFormView.vue')
          },
          {
            path: 'orders/:id',
            name: 'purchase-order-detail',
            component: () => import('@/views/purchases/PurchaseOrderDetailView.vue')
          },
          {
            path: 'history',
            name: 'supplier-history',
            component: () => import('@/views/purchases/SupplierHistoryView.vue')
          },
          {
            path: 'history/:id',
            name: 'supplier-history-detail',
            component: () => import('@/views/purchases/SupplierHistoryView.vue')
          }
        ]
      },
      // Reports
      {
        path: 'reports',
        children: [
          {
            path: 'sales',
            name: 'sales-report',
            component: () => import('@/views/reports/SalesReportView.vue')
          },
          {
            path: 'inventory',
            name: 'inventory-report',
            component: () => import('@/views/reports/InventoryReportView.vue')
          },
          {
            path: 'financial',
            name: 'financial-report',
            component: () => import('@/views/reports/FinancialReportView.vue')
          },
          {
            path: 'salary',
            name: 'salary-report',
            component: () => import('@/views/reports/SalaryReportView.vue')
          }
        ]
      },
      // Finance
      {
        path: 'finance',
        children: [
          {
            path: 'debtors',
            name: 'debtors',
            component: () => import('@/views/finance/DebtorsView.vue')
          },
          {
            path: 'debtors/new',
            name: 'debtor-create',
            component: () => import('@/views/finance/DebtorFormView.vue')
          },
          {
            path: 'debtors/:id/edit',
            name: 'debtor-edit',
            component: () => import('@/views/finance/DebtorFormView.vue')
          },
          {
            path: 'expenses',
            name: 'expenses',
            component: () => import('@/views/finance/ExpensesView.vue')
          },
          {
            path: 'expenses/new',
            name: 'expense-create',
            component: () => import('@/views/finance/ExpenseFormView.vue')
          },
          {
            path: 'expenses/:id',
            name: 'expense-detail',
            component: () => import('@/views/finance/ExpenseDetailView.vue')
          },
          {
            path: 'expenses/:id/edit',
            name: 'expense-edit',
            component: () => import('@/views/finance/ExpenseFormView.vue')
          },
          {
            path: 'payments',
            name: 'payments',
            component: () => import('@/views/finance/PaymentsView.vue')
          },
          {
            path: 'invoices',
            name: 'ar-invoices',
            component: () => import('@/views/finance/ARInvoicesView.vue')
          },
          {
            path: 'accounts',
            name: 'chart-of-accounts',
            component: () => import('@/views/finance/ChartOfAccountsView.vue')
          },
          {
            path: 'journal-entries',
            name: 'journal-entries',
            component: () => import('@/views/finance/JournalEntriesView.vue')
          },
          {
            path: 'bank-accounts',
            name: 'bank-accounts',
            component: () => import('@/views/finance/BankAccountsView.vue')
          },
          {
            path: 'bank-transactions',
            name: 'bank-transactions',
            component: () => import('@/views/finance/BankTransactionsView.vue')
          },
          {
            path: 'credit-notes',
            name: 'credit-notes',
            component: () => import('@/views/finance/CreditNotesView.vue')
          },
          {
            path: 'currencies',
            name: 'currencies',
            component: () => import('@/views/finance/CurrenciesView.vue')
          },
          {
            path: 'tax-codes',
            name: 'tax-codes',
            component: () => import('@/views/finance/TaxCodesView.vue')
          },
          {
            path: 'fiscal-periods',
            name: 'fiscal-periods',
            component: () => import('@/views/finance/FiscalPeriodsView.vue')
          },
          {
            path: 'recurring-journals',
            name: 'recurring-journals',
            component: () => import('@/views/finance/RecurringJournalsView.vue')
          }
        ]
      },
      // HR
      {
        path: 'hr',
        children: [
          {
            path: 'employees',
            name: 'employees',
            component: () => import('@/views/hr/EmployeesView.vue')
          },
          {
            path: 'employees/new',
            name: 'employee-create',
            component: () => import('@/views/hr/EmployeeFormView.vue')
          },
          {
            path: 'employees/:id/edit',
            name: 'employee-edit',
            component: () => import('@/views/hr/EmployeeFormView.vue')
          },
          {
            path: 'departments',
            name: 'departments',
            component: () => import('@/views/hr/DepartmentsView.vue')
          },
          {
            path: 'positions',
            name: 'positions',
            component: () => import('@/views/hr/PositionsView.vue')
          },
          {
            path: 'salary',
            name: 'salary',
            component: () => import('@/views/hr/SalaryView.vue')
          },
          {
            path: 'history',
            name: 'employee-history',
            component: () => import('@/views/hr/EmployeeHistoryView.vue')
          },
          {
            path: 'history/:id',
            name: 'employee-history-detail',
            component: () => import('@/views/hr/EmployeeHistoryView.vue')
          }
        ]
      },
      // Distribution
      {
        path: 'distribution',
        children: [
          {
            path: 'agents',
            name: 'distribution-agents',
            component: () => import('@/views/distribution/AgentsView.vue'),
            meta: { permission: 'DISTRIBUTION_AGENT_VIEW' }
          },
          {
            path: 'agents/new',
            name: 'distribution-agent-create',
            component: () => import('@/views/distribution/AgentFormView.vue'),
            meta: { permission: 'DISTRIBUTION_AGENT_MANAGE' }
          },
          {
            path: 'agents/:id/edit',
            name: 'distribution-agent-edit',
            component: () => import('@/views/distribution/AgentFormView.vue'),
            meta: { permission: 'DISTRIBUTION_AGENT_MANAGE' }
          },
          {
            path: 'orders',
            name: 'distribution-orders',
            component: () => import('@/views/distribution/OrdersView.vue'),
            meta: { permission: 'DISTRIBUTION_ORDER_VIEW' }
          },
          {
            path: 'orders/new',
            name: 'distribution-order-create',
            component: () => import('@/views/distribution/OrderFormView.vue'),
            meta: { permission: 'DISTRIBUTION_ORDER_CREATE' }
          },
          {
            path: 'orders/:id/edit',
            name: 'distribution-order-edit',
            component: () => import('@/views/distribution/OrderFormView.vue'),
            meta: { permission: 'DISTRIBUTION_ORDER_MANAGE' }
          },
          {
            path: 'van-loadouts',
            name: 'distribution-van-loadouts',
            component: () => import('@/views/distribution/VanLoadoutsView.vue'),
            meta: { permission: 'DISTRIBUTION_VAN_VIEW' }
          },
          {
            path: 'van-loadouts/new',
            name: 'distribution-van-loadout-create',
            component: () => import('@/views/distribution/VanLoadoutFormView.vue'),
            meta: { permission: 'DISTRIBUTION_VAN_MANAGE' }
          },
          {
            path: 'van-loadouts/:id',
            name: 'distribution-van-loadout-detail',
            component: () => import('@/views/distribution/VanLoadoutFormView.vue'),
            meta: { permission: 'DISTRIBUTION_VAN_VIEW' }
          },
          {
            path: 'routes',
            name: 'distribution-routes',
            component: () => import('@/views/distribution/RoutesView.vue'),
            meta: { permission: 'DISTRIBUTION_ROUTE_VIEW' }
          },
          {
            path: 'routes/new',
            name: 'distribution-route-create',
            component: () => import('@/views/distribution/RouteFormView.vue'),
            meta: { permission: 'DISTRIBUTION_ROUTE_MANAGE' }
          },
          {
            path: 'routes/:id/edit',
            name: 'distribution-route-edit',
            component: () => import('@/views/distribution/RouteFormView.vue'),
            meta: { permission: 'DISTRIBUTION_ROUTE_MANAGE' }
          },
          {
            path: 'visits',
            name: 'distribution-visits',
            component: () => import('@/views/distribution/VisitsView.vue'),
            meta: { permission: 'DISTRIBUTION_VISIT_VIEW' }
          },
          {
            path: 'kpi',
            name: 'distribution-kpi',
            component: () => import('@/views/distribution/KpiDashboardView.vue'),
            meta: { permission: 'DISTRIBUTION_KPI_VIEW' }
          }
        ]
      },
      // Admin
      {
        path: 'admin',
        children: [
          {
            path: 'users',
            name: 'users',
            component: () => import('@/views/admin/UsersView.vue'),
            meta: { permission: 'USER_MANAGE' }
          },
          {
            path: 'users/new',
            name: 'user-create',
            component: () => import('@/views/admin/UserFormView.vue'),
            meta: { permission: 'USER_MANAGE' }
          },
          {
            path: 'users/:id/edit',
            name: 'user-edit',
            component: () => import('@/views/admin/UserFormView.vue'),
            meta: { permission: 'USER_MANAGE' }
          },
          {
            path: 'settings',
            name: 'settings',
            component: () => import('@/views/admin/SettingsView.vue'),
            meta: { permission: 'ADMIN_SETTINGS_MANAGE' }
          },
          {
            path: 'system-settings',
            name: 'system-settings',
            component: () => import('@/views/admin/SystemSettingsView.vue'),
            meta: { permission: 'ADMIN_SETTINGS_MANAGE' }
          },
          {
            path: 'tenant-settings',
            name: 'tenant-settings',
            component: () => import('@/views/admin/TenantSettingsView.vue'),
            meta: { permission: 'TENANT_SETTINGS_MANAGE' }
          },
          {
            path: 'audit-logs',
            name: 'audit-logs',
            component: () => import('@/views/admin/AuditLogsView.vue'),
            meta: { permission: 'AUDIT_VIEW' }
          },
          {
            path: 'terminals',
            name: 'terminals',
            component: () => import('@/views/admin/TerminalsView.vue'),
            meta: { permission: 'POS_TERMINAL_READ' }
          },
          {
            path: 'terminals/new',
            name: 'terminal-create',
            component: () => import('@/views/admin/TerminalFormView.vue'),
            meta: { permission: 'POS_TERMINAL_CREATE' }
          },
          {
            path: 'terminals/:id/edit',
            name: 'terminal-edit',
            component: () => import('@/views/admin/TerminalFormView.vue'),
            meta: { permission: 'POS_TERMINAL_UPDATE' }
          },
          {
            path: 'roles',
            name: 'roles',
            component: () => import('@/views/admin/RolesView.vue'),
            meta: { permission: 'ADMIN_ROLE_MANAGE' }
          },
          {
            path: 'roles/new',
            name: 'role-create',
            component: () => import('@/views/admin/RoleFormView.vue'),
            meta: { permission: 'ADMIN_ROLE_MANAGE' }
          },
          {
            path: 'roles/:id/edit',
            name: 'role-edit',
            component: () => import('@/views/admin/RoleFormView.vue'),
            meta: { permission: 'ADMIN_ROLE_MANAGE' }
          },
          {
            path: 'telegram',
            name: 'telegram-admin',
            component: () => import('@/views/admin/TelegramAdminView.vue'),
            meta: { permission: 'ADMIN_SETTINGS_MANAGE' }
          },
          {
            path: 'sms',
            name: 'sms-admin',
            component: () => import('@/views/admin/SmsAdminView.vue'),
            meta: { permission: 'ADMIN_SETTINGS_MANAGE' }
          },
          {
            path: 'regions',
            name: 'regions',
            component: () => import('@/views/admin/RegionsView.vue')
          },
          {
            path: 'regions/new',
            name: 'region-create',
            component: () => import('@/views/admin/RegionFormView.vue')
          },
          {
            path: 'regions/:id/edit',
            name: 'region-edit',
            component: () => import('@/views/admin/RegionFormView.vue')
          },
          {
            path: 'villages',
            name: 'villages',
            component: () => import('@/views/admin/VillagesView.vue')
          },
          {
            path: 'villages/new',
            name: 'village-create',
            component: () => import('@/views/admin/VillageFormView.vue')
          },
          {
            path: 'villages/:id/edit',
            name: 'village-edit',
            component: () => import('@/views/admin/VillageFormView.vue')
          }
        ]
      }
    ]
  },
  {
    path: '/no-access',
    name: 'no-access',
    component: () => import('@/views/NoAccessView.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Wait for auth to initialize
  if (!authStore.isAuthenticated && getAccessToken()) {
    await authStore.initializeAuth()
  }

  // Check if route requires authentication
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!authStore.isAuthenticated) {
      next({ name: 'login', query: { redirect: to.fullPath } })
      return
    }

    // Check permissions
    const permission = to.meta.permission
    if (permission && !authStore.hasPermission(permission)) {
      next({ name: 'no-access' })
      return
    }
  }

  // Redirect authenticated users away from guest pages
  if (to.matched.some(record => record.meta.guest) && authStore.isAuthenticated) {
    next({ name: 'dashboard' })
    return
  }

  next()
})

export default router
