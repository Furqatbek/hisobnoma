import { createI18n } from 'vue-i18n'
import common from './locales/uz-Cyrl/common.json'
import nav from './locales/uz-Cyrl/nav.json'
import auth from './locales/uz-Cyrl/auth.json'
import dashboard from './locales/uz-Cyrl/dashboard.json'
import profile from './locales/uz-Cyrl/profile.json'
import admin from './locales/uz-Cyrl/admin.json'
import inventory from './locales/uz-Cyrl/inventory.json'
import customers from './locales/uz-Cyrl/customers.json'
import pos from './locales/uz-Cyrl/pos.json'
import purchases from './locales/uz-Cyrl/purchases.json'
import hr from './locales/uz-Cyrl/hr.json'
import finance from './locales/uz-Cyrl/finance.json'
import reports from './locales/uz-Cyrl/reports.json'
import enums from './locales/uz-Cyrl/enums.json'
import customerHistory from './locales/uz-Cyrl/customerHistory.json'

const i18n = createI18n({
  legacy: false,
  locale: 'uz-Cyrl',
  fallbackLocale: 'uz-Cyrl',
  messages: {
    'uz-Cyrl': {
      ...common,
      nav,
      auth,
      dashboard,
      profile,
      admin,
      inventory,
      customers,
      pos,
      purchases,
      hr,
      finance,
      reports,
      enums,
      customerHistory
    }
  }
})

export default i18n
