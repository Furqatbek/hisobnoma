-- V79: Flag POS payments that were approved without a gateway authorization.
--
-- CARD/MOBILE_PAYMENT tenders have no gateway integration yet; they are approved on staff-entered
-- details (standalone terminal). This column marks such approvals as manual so they are auditable
-- and can be reconciled against the card terminal's settlement report, instead of being
-- indistinguishable from a real gateway-authorized payment.

ALTER TABLE pos_payments
    ADD COLUMN IF NOT EXISTS manually_approved BOOLEAN NOT NULL DEFAULT FALSE;
