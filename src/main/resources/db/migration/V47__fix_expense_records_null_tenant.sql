-- V47__fix_expense_records_null_tenant.sql
-- Fix expense records with null tenant_id by assigning them to the default tenant

UPDATE expense_records SET tenant_id = 1 WHERE tenant_id IS NULL;
