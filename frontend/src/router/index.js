import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView
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
            meta: { permission: 'SETTINGS_MANAGE' }
          },
          {
            path: 'audit-logs',
            name: 'audit-logs',
            component: () => import('@/views/admin/AuditLogsView.vue'),
            meta: { permission: 'AUDIT_VIEW' }
          }
        ]
      }
    ]
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
  if (!authStore.isAuthenticated && localStorage.getItem('accessToken')) {
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
      next({ name: 'dashboard' })
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
