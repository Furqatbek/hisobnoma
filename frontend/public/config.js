// Runtime configuration, injected at container start by docker-entrypoint.sh.
// Served with no-store (see nginx.conf) so it is never cached across redeploys.
//
// defaultTenantId seeds the pre-login user/PIN screen on a fresh device, where
// no prior login has remembered a tenant yet (the backend requires X-Tenant-ID
// on /auth/users/list and fails closed). Set DEFAULT_TENANT_ID for a
// single-tenant deployment; leave it unset for multi-tenant setups, where the
// screen falls back to password login until a tenant is remembered.
window.__APP_CONFIG__ = { defaultTenantId: 'DEFAULT_TENANT_ID_PLACEHOLDER' };
