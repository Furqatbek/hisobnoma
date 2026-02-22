import { createI18n } from 'vue-i18n'
import uzCyrl from './locales/uz-Cyrl.json'

const i18n = createI18n({
  legacy: false,
  locale: 'uz-Cyrl',
  fallbackLocale: 'uz-Cyrl',
  messages: {
    'uz-Cyrl': uzCyrl
  }
})

export default i18n
