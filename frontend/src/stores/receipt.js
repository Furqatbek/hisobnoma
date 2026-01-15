import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useReceiptStore = defineStore('receipt', () => {
  // Receipt configuration - saved to localStorage
  const config = ref({
    brandName: localStorage.getItem('receipt_brandName') || 'Do\'koningiz nomi',
    phone: localStorage.getItem('receipt_phone') || '+998 XX XXX XX XX',
    address: localStorage.getItem('receipt_address') || 'Manzil: Toshkent sh.',
    website: localStorage.getItem('receipt_website') || '',
    taxId: localStorage.getItem('receipt_taxId') || '',
    footerText: localStorage.getItem('receipt_footerText') || 'Xaridingiz uchun rahmat!',
    showLogo: localStorage.getItem('receipt_showLogo') === 'true',
    paperWidth: localStorage.getItem('receipt_paperWidth') || '80' // 58mm or 80mm
  })

  // Update config and save to localStorage
  function updateConfig(newConfig) {
    Object.keys(newConfig).forEach(key => {
      if (config.value.hasOwnProperty(key)) {
        config.value[key] = newConfig[key]
        localStorage.setItem(`receipt_${key}`, newConfig[key])
      }
    })
  }

  // Reset to defaults
  function resetConfig() {
    const defaults = {
      brandName: 'Do\'koningiz nomi',
      phone: '+998 XX XXX XX XX',
      address: 'Manzil: Toshkent sh.',
      website: '',
      taxId: '',
      footerText: 'Xaridingiz uchun rahmat!',
      showLogo: false,
      paperWidth: '80'
    }
    updateConfig(defaults)
  }

  return {
    config,
    updateConfig,
    resetConfig
  }
})
