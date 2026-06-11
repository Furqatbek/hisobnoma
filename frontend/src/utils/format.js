/**
 * Shared display formatters. These are the canonical implementations of the
 * helpers that used to be copy-pasted per view — import instead of redefining:
 *
 *   import { formatCurrency, formatDate, formatPrice } from '@/utils/format'
 */

/** Whole-sum money, uz-UZ digit grouping: 1234567 -> "1 234 567". */
export function formatCurrency(value, digits = 0) {
  return new Intl.NumberFormat('uz-UZ', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(Number(value) || 0)
}

/** Price with an explicit "no value" fallback instead of 0. */
export function formatPrice(value, fallback = '—') {
  if (value == null) return fallback
  return new Intl.NumberFormat('uz-UZ').format(value)
}

/** Date only (uz-UZ locale). */
export function formatDate(value, fallback = '-') {
  if (!value) return fallback
  return new Date(value).toLocaleDateString('uz-UZ')
}

/** Date + time (uz-UZ locale). */
export function formatDateTime(value, fallback = '—') {
  if (!value) return fallback
  return new Date(value).toLocaleString('uz-UZ', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

/** Quantity: whole numbers without decimals, fractions with two. */
export function formatQty(value) {
  if (value == null) return '0'
  const n = Number(value)
  return n % 1 === 0 ? n.toFixed(0) : n.toFixed(2)
}
