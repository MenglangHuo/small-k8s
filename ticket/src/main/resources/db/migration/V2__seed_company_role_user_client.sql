-- ==============================================================================
-- Flyway Migration: V2__seed_company_role_user_client.sql
-- Description: Idempotent seed data for companies, roles, users, and api_clients
-- ==============================================================================

-- 1. SEED COMPANIES
INSERT INTO companies (id, code, name, legal_name, domain, status, timezone, created_at, updated_at)
VALUES
    (1, 'BRONX', 'Bronx Technology', 'Bronx Technology Solutions Co., Ltd.', 'bronx.internal', 'active', 'Asia/Phnom_Penh', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'UYFC', 'UYFC Central', 'Union of Youth Federations of Cambodia', 'uyfc.internal', 'active', 'Asia/Phnom_Penh', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE 
SET code = EXCLUDED.code,
    name = EXCLUDED.name,
    legal_name = EXCLUDED.legal_name,
    domain = EXCLUDED.domain,
    status = EXCLUDED.status,
    timezone = EXCLUDED.timezone,
    updated_at = CURRENT_TIMESTAMP;

-- Reset sequence for companies
SELECT setval(pg_get_serial_sequence('companies', 'id'), COALESCE(MAX(id), 1)) FROM companies;


-- 2. SEED ROLES
INSERT INTO roles (id, name, code, description, is_system, created_at, updated_at)
VALUES
    (1, 'HQ ADMIN', 'hq_admin', 'Headquarters Administrator with full access to tickets, settings, clients, and user management.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'BRANCH MANAGER', 'branch_manager', 'Branch Manager managing branch operations and tickets.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'BRANCH STAFF', 'branch_staff', 'Branch staff submitting and tracking issue tickets.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'TICKET ADMIN', 'ticket_admin', 'Ticket Administrator managing ticket lifecycle, triage, and assignment.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'PREVIEW', 'preview', 'Read-only viewer account for tickets and reporting.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name,
    code = EXCLUDED.code,
    description = EXCLUDED.description,
    is_system = EXCLUDED.is_system,
    updated_at = CURRENT_TIMESTAMP;

-- Reset sequence for roles
SELECT setval(pg_get_serial_sequence('roles', 'id'), COALESCE(MAX(id), 1)) FROM roles;


-- 3. SEED USERS
-- Default password: password123 (BCrypt hash: $2a$12$6Pq160161434o5R35j96gOBp4o0.4gT9Nq3f0g1sV8wU.pE0J3kOi)
INSERT INTO users (
    id, company_id, role_id, username, password_hash,
    first_name, last_name, first_name_kh, last_name_kh,
    email, phone, personal_phone,
    department_id, department_name, division_name,
    is_expert, accept_card, status,
    created_at, updated_at
)
VALUES
    (
        1, 1, 1, 'admin', '$2a$12$6Pq160161434o5R35j96gOBp4o0.4gT9Nq3f0g1sV8wU.pE0J3kOi',
        'Menglang', 'Huo', 'ម៉េងឡាង', 'ហួ',
        'menglang@bronx.internal', '010 334 556', '010 334 556',
        1, 'Core Technology & Infrastructure', 'Software Engineering',
        true, true, 'active',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        2, 2, 2, 'branch_manager', '$2a$12$6Pq160161434o5R35j96gOBp4o0.4gT9Nq3f0g1sV8wU.pE0J3kOi',
        'Dara', 'Chan', 'ដារា', 'ចាន់',
        'dara.chan@uyfc.internal', '012 889 901', '012 889 901',
        2, 'Central Branch Operations', 'Branch Management',
        false, false, 'active',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        3, 1, 3, 'branch_staff', '$2a$12$6Pq160161434o5R35j96gOBp4o0.4gT9Nq3f0g1sV8wU.pE0J3kOi',
        'Sokha', 'Vann', 'សុខា', 'វ៉ាន់',
        'staff@bronx.internal', '011 223 344', '011 223 344',
        3, 'Support & Services', 'Field Operations',
        false, false, 'active',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        4, 1, 5, 'viewer', '$2a$12$6Pq160161434o5R35j96gOBp4o0.4gT9Nq3f0g1sV8wU.pE0J3kOi',
        'Audit', 'Viewer', 'សវនកម្ម', 'អ្នកមើល',
        'preview@bronx.internal', '015 667 788', '015 667 788',
        4, 'Compliance & Audit', 'Internal Audit',
        false, false, 'active',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO UPDATE 
SET company_id = EXCLUDED.company_id,
    role_id = EXCLUDED.role_id,
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    first_name_kh = EXCLUDED.first_name_kh,
    last_name_kh = EXCLUDED.last_name_kh,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    personal_phone = EXCLUDED.personal_phone,
    department_id = EXCLUDED.department_id,
    department_name = EXCLUDED.department_name,
    division_name = EXCLUDED.division_name,
    is_expert = EXCLUDED.is_expert,
    accept_card = EXCLUDED.accept_card,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

-- Reset sequence for users
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;


-- 4. SEED API CLIENTS
INSERT INTO api_clients (
    id, company_id, client_id, client_secret, name,
    rate_limit, allowed_types, status,
    last_ping, created_at, updated_at
)
VALUES
    (
        1, 1, 'bronx_mobile_app_prod', '4d2aff85e28a4c3395d9082bfceb1784', 'Bronx Mobile Storefront (iOS & Android)',
        '500 req/min', 'bug,enhancement,question', 'active',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        2, 1, 'pos_sync_service_v2', '9e7b2311f4564811b51e44c207d81a93', 'Branch POS Terminal Sync Service',
        '1200 req/min', 'bug,task', 'active',
        CURRENT_TIMESTAMP - INTERVAL '35 seconds', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        3, 1, 'helpdesk_webhook_int', '1a8c9034c56740b28ec4b953a793c2de', 'Customer Helpdesk Ingestion Webhook',
        '300 req/min', 'bug,question', 'active',
        CURRENT_TIMESTAMP - INTERVAL '2 minutes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        4, 1, 'payment_alert_service', '7f44a190bc234a9190db99a071ef68d1', 'E-Commerce Payment Gateway Monitor',
        '600 req/min', 'bug,task', 'active',
        CURRENT_TIMESTAMP - INTERVAL '5 minutes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        5, 2, 'erp_legacy_batch_sync', '3d10ee92cb344078a6311054e9bc3321', 'Legacy Inventory ERP Sync (Archived)',
        '50 req/min', 'task', 'inactive',
        CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO UPDATE 
SET company_id = EXCLUDED.company_id,
    client_id = EXCLUDED.client_id,
    client_secret = EXCLUDED.client_secret,
    name = EXCLUDED.name,
    rate_limit = EXCLUDED.rate_limit,
    allowed_types = EXCLUDED.allowed_types,
    status = EXCLUDED.status,
    last_ping = EXCLUDED.last_ping,
    updated_at = CURRENT_TIMESTAMP;

-- Reset sequence for api_clients
SELECT setval(pg_get_serial_sequence('api_clients', 'id'), COALESCE(MAX(id), 1)) FROM api_clients;
