-- Active application users.
SELECT id, username, email, role, enabled, created_at
FROM app_users
WHERE deleted = FALSE
ORDER BY username;

-- Procurement tickets waiting for approval, newest first.
SELECT id, ticket_code, title, department, total_amount, currency,
       priority, maker_username, created_at
FROM procurement_tickets
WHERE status = 'PENDING_APPROVAL'
ORDER BY created_at DESC;

-- Procurement ticket totals compared with totals calculated from item rows.
SELECT t.id,
       t.ticket_code,
       t.total_amount AS stored_total,
       COALESCE(SUM(i.total_price), 0) AS calculated_total
FROM procurement_tickets t
LEFT JOIN procurement_items i ON i.ticket_id = t.id
GROUP BY t.id, t.ticket_code, t.total_amount
HAVING t.total_amount <> COALESCE(SUM(i.total_price), 0)
ORDER BY t.ticket_code;

-- Pending supplier proposals.
SELECT id, name, tax_code, submitted_by, submitted_at
FROM supplier_proposals
WHERE status = 'PENDING'
ORDER BY submitted_at DESC;

-- Suppliers currently available for procurement.
SELECT id, code, name, contact_person, email, phone, city, country, tax_code
FROM suppliers
WHERE status = 'ACTIVE'
ORDER BY name;

-- Duplicate non-empty tax codes. Normally returns no rows because of the unique index.
SELECT tax_code, COUNT(*) AS duplicate_count
FROM suppliers
WHERE tax_code IS NOT NULL AND BTRIM(tax_code) <> ''
GROUP BY tax_code
HAVING COUNT(*) > 1
ORDER BY tax_code;

-- Proposals whose approved supplier record is missing.
SELECT p.id, p.name, p.tax_code, p.supplier_id, p.reviewed_at
FROM supplier_proposals p
LEFT JOIN suppliers s ON s.id = p.supplier_id
WHERE p.status = 'APPROVED'
  AND (p.supplier_id IS NULL OR s.id IS NULL)
ORDER BY p.reviewed_at DESC;
