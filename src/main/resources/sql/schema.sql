-- =============================================================================
-- GroceryPOS Database Schema
-- Database : grocery_pos
-- Version  : 1.0.1
-- =============================================================================
-- Compatible with MySQL 5.7 and MySQL 8.x.
-- All indexes are defined inline inside CREATE TABLE statements to avoid
-- the CREATE INDEX IF NOT EXISTS syntax that requires MySQL 8.0.29+.
-- Safe to re-run: tables use CREATE TABLE IF NOT EXISTS,
-- seed data uses INSERT IGNORE.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Create and select database
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS grocery_pos
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE grocery_pos;

-- Disable FK checks during setup so tables can be created in any order
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- TABLE: roles
-- =============================================================================
CREATE TABLE IF NOT EXISTS roles (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_roles      PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: permissions
-- =============================================================================
CREATE TABLE IF NOT EXISTS permissions (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    module      VARCHAR(50)  NOT NULL,

    CONSTRAINT pk_permissions      PRIMARY KEY (id),
    CONSTRAINT uq_permissions_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: role_permissions
-- =============================================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role
        FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: users
-- =============================================================================
CREATE TABLE IF NOT EXISTS users (
    id            INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) DEFAULT NULL,
    phone         VARCHAR(20)  DEFAULT NULL,
    role_id       INT          NOT NULL,
    status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    last_login    DATETIME     DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users       PRIMARY KEY (id),
    CONSTRAINT uq_users_name  UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles(id),

    INDEX idx_users_role   (role_id),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: categories
-- =============================================================================
CREATE TABLE IF NOT EXISTS categories (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_categories      PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name),

    INDEX idx_categories_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: suppliers
-- =============================================================================
CREATE TABLE IF NOT EXISTS suppliers (
    id             INT          NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100) DEFAULT NULL,
    phone          VARCHAR(20)  DEFAULT NULL,
    email          VARCHAR(100) DEFAULT NULL,
    address        TEXT         DEFAULT NULL,
    notes          TEXT         DEFAULT NULL,
    status         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_suppliers PRIMARY KEY (id),

    INDEX idx_suppliers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: customers
-- =============================================================================
CREATE TABLE IF NOT EXISTS customers (
    id             INT          NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  DEFAULT NULL,
    email          VARCHAR(100) DEFAULT NULL,
    address        TEXT         DEFAULT NULL,
    loyalty_points INT          NOT NULL DEFAULT 0,
    status         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_customers       PRIMARY KEY (id),
    CONSTRAINT uq_customers_phone UNIQUE (phone),

    INDEX idx_customers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: products
-- =============================================================================
CREATE TABLE IF NOT EXISTS products (
    id              INT           NOT NULL AUTO_INCREMENT,
    barcode         VARCHAR(50)   DEFAULT NULL,
    name            VARCHAR(150)  NOT NULL,
    description     TEXT          DEFAULT NULL,
    category_id     INT           DEFAULT NULL,
    supplier_id     INT           DEFAULT NULL,
    brand           VARCHAR(100)  DEFAULT NULL,
    unit            ENUM('PIECE','KG','GRAM','LITER','ML','PACK','BOX','BOTTLE')
                                  NOT NULL DEFAULT 'PIECE',
    purchase_price  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    selling_price   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    wholesale_price DECIMAL(12,2) DEFAULT 0.00,
    current_stock   DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    minimum_stock   DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    expiry_date     DATE          DEFAULT NULL,
    status          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_products         PRIMARY KEY (id),
    CONSTRAINT uq_products_barcode UNIQUE (barcode),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_products_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id)  ON DELETE SET NULL,

    INDEX idx_products_name     (name),
    INDEX idx_products_category (category_id),
    INDEX idx_products_supplier (supplier_id),
    INDEX idx_products_status   (status),
    INDEX idx_products_expiry   (expiry_date),
    INDEX idx_products_stock    (current_stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: sales
-- =============================================================================
CREATE TABLE IF NOT EXISTS sales (
    id              INT           NOT NULL AUTO_INCREMENT,
    invoice_number  VARCHAR(20)   NOT NULL,
    customer_id     INT           DEFAULT NULL,
    user_id         INT           NOT NULL,
    sale_date       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal        DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    grand_total     DECIMAL(12,2) NOT NULL,
    payment_method  VARCHAR(50)   NOT NULL,
    cash_received   DECIMAL(12,2) DEFAULT 0.00,
    change_amount   DECIMAL(12,2) DEFAULT 0.00,
    status          ENUM('COMPLETED','VOIDED','RETURNED') NOT NULL DEFAULT 'COMPLETED',
    notes           TEXT          DEFAULT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_sales         PRIMARY KEY (id),
    CONSTRAINT uq_sales_invoice UNIQUE (invoice_number),
    CONSTRAINT fk_sales_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_sales_user
        FOREIGN KEY (user_id)     REFERENCES users(id),

    INDEX idx_sales_date     (sale_date),
    INDEX idx_sales_user     (user_id),
    INDEX idx_sales_customer (customer_id),
    INDEX idx_sales_status   (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: sale_items
-- =============================================================================
CREATE TABLE IF NOT EXISTS sale_items (
    id              INT           NOT NULL AUTO_INCREMENT,
    sale_id         INT           NOT NULL,
    product_id      INT           NOT NULL,
    quantity        DECIMAL(12,3) NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_price     DECIMAL(12,2) NOT NULL,

    CONSTRAINT pk_sale_items PRIMARY KEY (id),
    CONSTRAINT fk_si_sale
        FOREIGN KEY (sale_id)    REFERENCES sales(id)    ON DELETE CASCADE,
    CONSTRAINT fk_si_product
        FOREIGN KEY (product_id) REFERENCES products(id),

    INDEX idx_si_sale    (sale_id),
    INDEX idx_si_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: payments
-- =============================================================================
CREATE TABLE IF NOT EXISTS payments (
    id             INT           NOT NULL AUTO_INCREMENT,
    sale_id        INT           NOT NULL,
    payment_method VARCHAR(50)   NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    reference      VARCHAR(100)  DEFAULT NULL,
    paid_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_sale
        FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,

    INDEX idx_payments_sale (sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: purchases
-- =============================================================================
CREATE TABLE IF NOT EXISTS purchases (
    id               INT           NOT NULL AUTO_INCREMENT,
    purchase_number  VARCHAR(20)   NOT NULL,
    supplier_id      INT           NOT NULL,
    supplier_invoice VARCHAR(100)  DEFAULT NULL,
    user_id          INT           NOT NULL,
    purchase_date    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount     DECIMAL(12,2) NOT NULL,
    status           ENUM('COMPLETED','CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    notes            TEXT          DEFAULT NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_purchases        PRIMARY KEY (id),
    CONSTRAINT uq_purchases_number UNIQUE (purchase_number),
    CONSTRAINT fk_purchases_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_purchases_user
        FOREIGN KEY (user_id)     REFERENCES users(id),

    INDEX idx_purchases_supplier (supplier_id),
    INDEX idx_purchases_date     (purchase_date),
    INDEX idx_purchases_user     (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: purchase_items
-- =============================================================================
CREATE TABLE IF NOT EXISTS purchase_items (
    id          INT           NOT NULL AUTO_INCREMENT,
    purchase_id INT           NOT NULL,
    product_id  INT           NOT NULL,
    quantity    DECIMAL(12,3) NOT NULL,
    unit_cost   DECIMAL(12,2) NOT NULL,
    total_cost  DECIMAL(12,2) NOT NULL,

    CONSTRAINT pk_purchase_items PRIMARY KEY (id),
    CONSTRAINT fk_pi_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_pi_product
        FOREIGN KEY (product_id)  REFERENCES products(id),

    INDEX idx_pi_purchase (purchase_id),
    INDEX idx_pi_product  (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: inventory_movements
-- =============================================================================
CREATE TABLE IF NOT EXISTS inventory_movements (
    id             INT           NOT NULL AUTO_INCREMENT,
    product_id     INT           NOT NULL,
    movement_type  ENUM(
                       'PURCHASE',
                       'SALE',
                       'RETURN_IN',
                       'RETURN_OUT',
                       'ADJUSTMENT',
                       'DAMAGE',
                       'TRANSFER_IN',
                       'TRANSFER_OUT'
                   ) NOT NULL,
    quantity       DECIMAL(12,3) NOT NULL,
    previous_stock DECIMAL(12,3) NOT NULL,
    new_stock      DECIMAL(12,3) NOT NULL,
    reference_id   INT           DEFAULT NULL,
    reference_type VARCHAR(20)   DEFAULT NULL,
    reason         TEXT          DEFAULT NULL,
    user_id        INT           NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT fk_im_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_im_user
        FOREIGN KEY (user_id)    REFERENCES users(id),

    INDEX idx_im_product (product_id),
    INDEX idx_im_type    (movement_type),
    INDEX idx_im_date    (created_at),
    INDEX idx_im_user    (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: returns
-- =============================================================================
CREATE TABLE IF NOT EXISTS returns (
    id            INT           NOT NULL AUTO_INCREMENT,
    return_number VARCHAR(20)   NOT NULL,
    sale_id       INT           NOT NULL,
    user_id       INT           NOT NULL,
    return_date   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_refund  DECIMAL(12,2) NOT NULL,
    reason        TEXT          DEFAULT NULL,
    status        ENUM('COMPLETED','CANCELLED') NOT NULL DEFAULT 'COMPLETED',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_returns        PRIMARY KEY (id),
    CONSTRAINT uq_returns_number UNIQUE (return_number),
    CONSTRAINT fk_returns_sale
        FOREIGN KEY (sale_id)  REFERENCES sales(id),
    CONSTRAINT fk_returns_user
        FOREIGN KEY (user_id)  REFERENCES users(id),

    INDEX idx_returns_sale (sale_id),
    INDEX idx_returns_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: return_items
-- =============================================================================
CREATE TABLE IF NOT EXISTS return_items (
    id            INT           NOT NULL AUTO_INCREMENT,
    return_id     INT           NOT NULL,
    sale_item_id  INT           NOT NULL,
    product_id    INT           NOT NULL,
    quantity      DECIMAL(12,3) NOT NULL,
    unit_price    DECIMAL(12,2) NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    restock       TINYINT(1)    NOT NULL DEFAULT 1,

    CONSTRAINT pk_return_items PRIMARY KEY (id),
    CONSTRAINT fk_ri_return
        FOREIGN KEY (return_id)    REFERENCES returns(id)    ON DELETE CASCADE,
    CONSTRAINT fk_ri_sale_item
        FOREIGN KEY (sale_item_id) REFERENCES sale_items(id),
    CONSTRAINT fk_ri_product
        FOREIGN KEY (product_id)   REFERENCES products(id),

    INDEX idx_ri_return  (return_id),
    INDEX idx_ri_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: settings
-- =============================================================================
CREATE TABLE IF NOT EXISTS settings (
    id            INT          NOT NULL AUTO_INCREMENT,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT         DEFAULT NULL,
    setting_group VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    description   VARCHAR(255) DEFAULT NULL,

    CONSTRAINT pk_settings     PRIMARY KEY (id),
    CONSTRAINT uq_settings_key UNIQUE (setting_key),

    INDEX idx_settings_group (setting_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- TABLE: audit_log
-- =============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id          INT          NOT NULL AUTO_INCREMENT,
    user_id     INT          DEFAULT NULL,
    action      VARCHAR(100) NOT NULL,
    module      VARCHAR(50)  NOT NULL,
    description TEXT         DEFAULT NULL,
    ip_address  VARCHAR(45)  DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,

    INDEX idx_audit_user   (user_id),
    INDEX idx_audit_module (module),
    INDEX idx_audit_date   (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Re-enable FK checks
-- =============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- SEED DATA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Roles
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO roles (id, name, description) VALUES
(1, 'ADMIN',   'Administrator / Owner — full system access'),
(2, 'CASHIER', 'Cashier — POS billing access only');

-- -----------------------------------------------------------------------------
-- Permissions
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO permissions (id, name, description, module) VALUES
-- Sales
( 1, 'SALE_CREATE',          'Create a new sale',               'SALES'),
( 2, 'SALE_VIEW',            'View sales history',              'SALES'),
( 3, 'SALE_VOID',            'Void / cancel a sale',            'SALES'),
-- Products
( 4, 'PRODUCT_CREATE',       'Add new products',                'PRODUCTS'),
( 5, 'PRODUCT_EDIT',         'Edit existing products',          'PRODUCTS'),
( 6, 'PRODUCT_DELETE',       'Delete products',                 'PRODUCTS'),
( 7, 'PRODUCT_PRICE_CHANGE', 'Change product prices',           'PRODUCTS'),
-- Inventory
( 8, 'INVENTORY_VIEW',       'View inventory',                  'INVENTORY'),
( 9, 'INVENTORY_ADJUST',     'Manually adjust stock',           'INVENTORY'),
-- Purchases
(10, 'PURCHASE_CREATE',      'Record new purchases',            'PURCHASES'),
(11, 'PURCHASE_VIEW',        'View purchase history',           'PURCHASES'),
-- Suppliers
(12, 'SUPPLIER_MANAGE',      'Add/edit/delete suppliers',       'SUPPLIERS'),
-- Customers
(13, 'CUSTOMER_VIEW',        'View customers',                  'CUSTOMERS'),
(14, 'CUSTOMER_MANAGE',      'Add/edit/delete customers',       'CUSTOMERS'),
-- Returns
(15, 'RETURN_PROCESS',       'Process sales returns/refunds',   'RETURNS'),
-- Discounts
(16, 'DISCOUNT_APPLY',       'Apply discounts during POS sale', 'SALES'),
-- Reports
(17, 'REPORT_VIEW',          'View and generate reports',       'REPORTS'),
-- Users
(18, 'USER_MANAGE',          'Add/edit/delete system users',    'USERS'),
-- Settings & Backup
(19, 'SETTINGS_MANAGE',      'Manage application settings',     'SETTINGS'),
(20, 'BACKUP_MANAGE',        'Backup and restore database',     'SETTINGS'),
-- Dashboard
(21, 'DASHBOARD_VIEW',       'View the main dashboard',         'DASHBOARD');

-- -----------------------------------------------------------------------------
-- Role → Permission mapping
-- ADMIN gets everything; CASHIER gets a limited set
-- -----------------------------------------------------------------------------

-- Admin: all permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- Cashier: limited permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
(2,  1),   -- SALE_CREATE
(2,  2),   -- SALE_VIEW
(2, 13),   -- CUSTOMER_VIEW
(2, 16),   -- DISCOUNT_APPLY
(2, 21);   -- DASHBOARD_VIEW

-- -----------------------------------------------------------------------------
-- Default admin user
-- Username : admin
-- Password : admin123   (BCrypt hash — change on first login)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO users (id, username, password_hash, full_name, email, role_id, status)
VALUES (
    1,
    'admin',
    '$2a$12$zSMuf2.3ntzPylZ4Q1SAEOPvmVkMmgnvrEtzVVouyvLXDD1WFIFJ6',
    'System Administrator',
    'admin@grocerypos.local',
    1,
    'ACTIVE'
);

-- -----------------------------------------------------------------------------
-- Default settings
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO settings (setting_key, setting_value, setting_group, description) VALUES
-- Store info
('store_name',            'My Grocery Shop',         'STORE',     'Business name printed on receipts'),
('store_address',         'No. 1, Main Street',      'STORE',     'Business address'),
('store_phone',           '+94 11 000 0000',          'STORE',     'Contact phone'),
('store_email',           'info@mygrocery.lk',       'STORE',     'Contact email'),
('store_receipt_footer',  'Thank you for shopping!', 'STORE',     'Footer text on receipts'),
-- Currency
('currency_symbol',       'Rs.',                     'GENERAL',   'Currency symbol'),
('currency_code',         'LKR',                     'GENERAL',   'ISO currency code'),
-- Invoice numbering
('invoice_prefix',        'INV-',                    'INVOICING', 'Prefix for sale invoice numbers'),
('invoice_counter',       '0',                       'INVOICING', 'Last used invoice sequence number'),
('invoice_number_length', '6',                       'INVOICING', 'Zero-padded length of invoice number'),
('purchase_prefix',       'PUR-',                    'INVOICING', 'Prefix for purchase numbers'),
('purchase_counter',      '0',                       'INVOICING', 'Last used purchase sequence number'),
('return_prefix',         'RET-',                    'INVOICING', 'Prefix for return numbers'),
('return_counter',        '0',                       'INVOICING', 'Last used return sequence number'),
-- Tax
('tax_enabled',           'false',                   'TAX',       'Enable tax on sales'),
('tax_rate',              '0.00',                    'TAX',       'Tax percentage (e.g. 15.00 for 15%)'),
-- Inventory
('low_stock_warning',     'true',                    'INVENTORY', 'Warn when stock falls to minimum'),
('allow_negative_stock',  'false',                   'INVENTORY', 'Allow selling when stock is zero'),
('expiry_warning_days',   '30',                      'INVENTORY', 'Days before expiry to show warning'),
-- Hardware
('printer_type',          'NONE',                    'HARDWARE',  'Printer type: ESCPOS | NONE'),
('printer_name',          '',                        'HARDWARE',  'System printer name for ESC/POS'),
('display_type',          'NONE',                    'HARDWARE',  'Customer display: SERIAL | NETWORK | SECONDARY_MONITOR | NONE'),
('display_port',          'COM1',                    'HARDWARE',  'Serial port for customer display'),
('display_baud_rate',     '9600',                    'HARDWARE',  'Baud rate for serial display'),
('display_host',          '',                        'HARDWARE',  'Host/IP for network display'),
('display_tcp_port',      '3000',                    'HARDWARE',  'TCP port for network display'),
-- Backup
('backup_dir',            'backups',                 'BACKUP',    'Directory to store database backups'),
('mysqldump_path',        'mysqldump',               'BACKUP',    'Full path to mysqldump executable');

-- -----------------------------------------------------------------------------
-- Default product categories
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO categories (id, name, description, status) VALUES
( 1, 'Grains & Rice',         'Rice, flour, cereals, pulses',     'ACTIVE'),
( 2, 'Dairy & Eggs',          'Milk, cheese, butter, eggs',       'ACTIVE'),
( 3, 'Meat & Seafood',        'Fresh and frozen meat, fish',      'ACTIVE'),
( 4, 'Fruits & Vegetables',   'Fresh produce',                    'ACTIVE'),
( 5, 'Beverages',             'Water, juice, soft drinks, tea',   'ACTIVE'),
( 6, 'Snacks & Confectionery','Chips, biscuits, chocolates',      'ACTIVE'),
( 7, 'Bakery',                'Bread, cakes, pastries',           'ACTIVE'),
( 8, 'Frozen Foods',          'Frozen meals and ingredients',     'ACTIVE'),
( 9, 'Canned & Packaged',     'Canned goods, sauces, pastes',     'ACTIVE'),
(10, 'Cleaning & Household',  'Detergent, soap, household items', 'ACTIVE'),
(11, 'Personal Care',         'Shampoo, toothpaste, hygiene',     'ACTIVE'),
(12, 'Cooking Essentials',    'Oils, spices, salt, sugar',        'ACTIVE'),
(13, 'Baby Products',         'Baby food, diapers, formula',      'ACTIVE'),
(14, 'Tobacco',               'Cigarettes, tobacco products',     'ACTIVE'),
(15, 'Other',                 'Miscellaneous items',              'ACTIVE');

-- =============================================================================
-- End of schema.sql
-- =============================================================================
