/**
 * Opens a print-friendly window and prints the given HTML body inside a minimal
 * A4 document shell. Self-contained (inline CSS) so it works from any view.
 */
function openPrint(title, bodyHtml) {
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) return
  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>${title}</title>
<style>
  * { box-sizing: border-box; }
  body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; color: #111; margin: 24px; }
  h1 { font-size: 20px; margin: 0 0 4px; }
  .muted { color: #666; font-size: 12px; }
  .meta { margin: 12px 0 20px; font-size: 13px; }
  .meta div { margin: 2px 0; }
  table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 13px; }
  th, td { border: 1px solid #ccc; padding: 6px 8px; text-align: left; }
  th { background: #f3f4f6; }
  td.num, th.num { text-align: right; }
  tfoot td { font-weight: bold; }
  .totals { margin-top: 16px; width: 320px; margin-left: auto; font-size: 13px; }
  .totals div { display: flex; justify-content: space-between; padding: 3px 0; }
  .totals .grand { border-top: 2px solid #111; font-weight: bold; font-size: 15px; padding-top: 6px; }
  .sign { margin-top: 48px; display: flex; justify-content: space-between; font-size: 13px; }
  .sign div { border-top: 1px solid #111; padding-top: 6px; width: 40%; text-align: center; }
  @media print { body { margin: 0; } @page { margin: 16mm; } }
</style></head><body>${bodyHtml}
<script>window.onload = function(){ window.print(); }<\/script>
</body></html>`)
  win.document.close()
}

function money(v) {
  return (Number(v) || 0).toLocaleString('uz-UZ')
}

function esc(s) {
  return String(s ?? '').replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]))
}

/**
 * Van load sheet — what physically leaves the warehouse on the vehicle.
 * @param loadout VanLoadoutDto  @param agentName resolved agent name  @param t i18n translator
 */
export function printLoadSheet(loadout, agentName, t) {
  const rows = (loadout.lines || []).map((l, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${esc(l.productName)}</td>
      <td>${esc(l.productSku || '')}</td>
      <td class="num">${money(l.quantityLoaded)}</td>
      <td>${esc(l.unitName || '')}</td>
    </tr>`).join('')

  openPrint(t('distribution.print.loadSheet') + ' ' + esc(loadout.loadoutNumber), `
    <h1>${t('distribution.print.loadSheet')}</h1>
    <div class="muted">${esc(loadout.loadoutNumber)}</div>
    <div class="meta">
      <div><b>${t('distribution.print.agent')}:</b> ${esc(agentName || ('#' + loadout.agentId))}</div>
      <div><b>${t('distribution.print.date')}:</b> ${esc(loadout.loadoutDate || '')}</div>
      <div><b>${t('distribution.print.loadedValue')}:</b> ${money(loadout.totalLoadedValue)} ${esc(loadout.currency || 'UZS')}</div>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>${t('distribution.print.product')}</th><th>${t('distribution.print.sku')}</th>
        <th class="num">${t('distribution.print.qty')}</th><th>${t('distribution.print.unit')}</th>
      </tr></thead>
      <tbody>${rows || `<tr><td colspan="5" class="muted">—</td></tr>`}</tbody>
    </table>
    <div class="sign">
      <div>${t('distribution.print.issuedBy')}</div>
      <div>${t('distribution.print.receivedBy')}</div>
    </div>`)
}

/**
 * Delivery note for one distribution order (goods + amounts for the customer).
 */
export function printDeliveryNote(order, agentName, t) {
  const rows = (order.lines || []).map((l, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${esc(l.productName)}</td>
      <td class="num">${money(l.quantity)}</td>
      <td>${esc(l.unitName || '')}</td>
      <td class="num">${money(l.unitPrice)}</td>
      <td class="num">${money(l.lineTotal)}</td>
    </tr>`).join('')

  openPrint(t('distribution.print.deliveryNote') + ' ' + esc(order.orderNumber), `
    <h1>${t('distribution.print.deliveryNote')}</h1>
    <div class="muted">${esc(order.orderNumber)}</div>
    <div class="meta">
      <div><b>${t('distribution.print.customer')}:</b> ${esc(order.customerName || ('#' + order.customerId))}</div>
      <div><b>${t('distribution.print.agent')}:</b> ${esc(agentName || ('#' + order.agentId))}</div>
      <div><b>${t('distribution.print.date')}:</b> ${esc(order.orderDate || '')}</div>
      <div v-if="order.deliveryAddress"><b>${t('distribution.print.address')}:</b> ${esc(order.deliveryAddress || '')}</div>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>${t('distribution.print.product')}</th>
        <th class="num">${t('distribution.print.qty')}</th><th>${t('distribution.print.unit')}</th>
        <th class="num">${t('distribution.print.price')}</th><th class="num">${t('distribution.print.total')}</th>
      </tr></thead>
      <tbody>${rows || `<tr><td colspan="6" class="muted">—</td></tr>`}</tbody>
    </table>
    <div class="totals">
      <div><span>${t('distribution.print.subtotal')}</span><span>${money(order.subtotal)}</span></div>
      ${Number(order.discountAmount) ? `<div><span>${t('distribution.print.discount')}</span><span>-${money(order.discountAmount)}</span></div>` : ''}
      ${Number(order.taxAmount) ? `<div><span>${t('distribution.print.tax')}</span><span>${money(order.taxAmount)}</span></div>` : ''}
      ${Number(order.deliveryFee) ? `<div><span>${t('distribution.print.deliveryFee')}</span><span>${money(order.deliveryFee)}</span></div>` : ''}
      <div class="grand"><span>${t('distribution.print.grandTotal')}</span><span>${money(order.totalAmount)} ${esc(order.currency || 'UZS')}</span></div>
    </div>
    <div class="sign">
      <div>${t('distribution.print.deliveredBy')}</div>
      <div>${t('distribution.print.receivedBy')}</div>
    </div>`)
}
