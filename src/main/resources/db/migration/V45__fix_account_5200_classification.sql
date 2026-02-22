-- Fix account 5200 classification conflict:
-- V11 created 5200 as "Purchase Discounts" (REVENUE, CREDIT)
-- GLIntegrationService uses 5200 as PURCHASE_EXPENSE and debits it
-- This causes negative revenue and trial balance imbalance
--
-- Fix: Reclassify 5200 as EXPENSE (its actual usage), and create
-- a new account 4300 "Purchase Discounts" (REVENUE) for vendor discounts

-- Step 1: Reclassify 5200 to EXPENSE with DEBIT normal balance
UPDATE accounts
SET account_type = 'EXPENSE',
    normal_balance = 'DEBIT',
    name = 'Purchase Expenses',
    description = 'Operating and purchase expenses',
    updated_at = CURRENT_TIMESTAMP
WHERE code = '5200';

-- Step 2: Create account 4300 Purchase Discounts (REVENUE, CREDIT)
-- This is where vendor discounts should be credited
INSERT INTO accounts (tenant_id, code, name, description, account_type, normal_balance,
                     account_level, full_path, is_system_account, allows_direct_posting,
                     active, currency, created_at, updated_at)
SELECT t.id, '4300', 'Purchase Discounts', 'Discounts received from vendors',
       'REVENUE', 'CREDIT', 1, '4300', TRUE, TRUE, TRUE, 'UZS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.tenant_id = t.id AND a.code = '4300');

-- Step 3: Recalculate ALL account balances from journal entries to fix any accumulated errors.
-- First reset ytd values
UPDATE accounts SET ytd_debit = 0, ytd_credit = 0;

-- Recalculate from actual posted journal lines
UPDATE accounts a
SET ytd_debit = COALESCE(sub.total_debit, 0),
    ytd_credit = COALESCE(sub.total_credit, 0)
FROM (
    SELECT jl.account_id,
           SUM(COALESCE(jl.base_debit_amount, jl.debit_amount, 0)) AS total_debit,
           SUM(COALESCE(jl.base_credit_amount, jl.credit_amount, 0)) AS total_credit
    FROM journal_lines jl
    JOIN journal_entries je ON jl.journal_entry_id = je.id
    WHERE je.status = 'POSTED'
    GROUP BY jl.account_id
) sub
WHERE a.id = sub.account_id;

-- Recalculate current_balance for all accounts based on correct normal balance
UPDATE accounts
SET current_balance = CASE
    WHEN normal_balance = 'DEBIT' THEN COALESCE(opening_balance, 0) + COALESCE(ytd_debit, 0) - COALESCE(ytd_credit, 0)
    ELSE COALESCE(opening_balance, 0) + COALESCE(ytd_credit, 0) - COALESCE(ytd_debit, 0)
    END,
    updated_at = CURRENT_TIMESTAMP;
