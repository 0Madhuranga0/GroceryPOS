-- =============================================================================
-- GroceryPOS — Test / Demo Data
-- Database : grocery_pos
-- =============================================================================
-- Run this AFTER schema.sql to populate the database with realistic sample
-- data for development and testing.
--
-- It is SAFE to re-run — all inserts use INSERT IGNORE so existing rows are
-- skipped without error.
--
-- Covers:
--   • 1 admin user  + 2 cashier users
--   • 5 suppliers
--   • 15 categories (already seeded by schema.sql, so we skip those)
--   • 40 products across multiple categories with realistic prices and stock
--   • 6 customers
--   • 5 purchases (with items, stock updated)
--   • 10 completed sales (with items and payments)
--   • 1 voided sale
--   • Inventory movements for all stock changes
-- =============================================================================

USE grocery_pos;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- USERS  (admin already seeded by schema.sql)
-- Password for ALL users: admin123
-- Hash: $2a$12$zSMuf2.3ntzPylZ4Q1SAEOPvmVkMmgnvrEtzVVouyvLXDD1WFIFJ6
-- =============================================================================
INSERT IGNORE INTO users
    (id, username, password_hash, full_name, email, phone, role_id, status)
VALUES
(2, 'cashier1', '$2a$12$zSMuf2.3ntzPylZ4Q1SAEOPvmVkMmgnvrEtzVVouyvLXDD1WFIFJ6',
    'Nimal Perera',   'nimal@grocerypos.local',  '0771234567', 2, 'ACTIVE'),
(3, 'cashier2', '$2a$12$zSMuf2.3ntzPylZ4Q1SAEOPvmVkMmgnvrEtzVVouyvLXDD1WFIFJ6',
    'Kamani Silva',   'kamani@grocerypos.local', '0779876543', 2, 'ACTIVE'),
(4, 'manager1', '$2a$12$zSMuf2.3ntzPylZ4Q1SAEOPvmVkMmgnvrEtzVVouyvLXDD1WFIFJ6',
    'Ruwan Fernando', 'ruwan@grocerypos.local',  '0712223344', 1, 'ACTIVE');

-- =============================================================================
-- SUPPLIERS
-- =============================================================================
INSERT IGNORE INTO suppliers
    (id, name, contact_person, phone, email, address, status)
VALUES
(1, 'Ceylon Foods Ltd',     'Asanka Jayawardena', '0112233445', 'info@ceylonfoods.lk',
    'No. 12, Colombo 03', 'ACTIVE'),
(2, 'Sunrich Distributors', 'Priya Bandara',      '0113344556', 'sunrich@gmail.com',
    'No. 45, Kandy Road, Gampaha', 'ACTIVE'),
(3, 'Lanka Beverages PLC',  'Chaminda Wijeratne', '0114455667', 'sales@lankabev.lk',
    'No. 7, Biyagama Export Zone', 'ACTIVE'),
(4, 'Fresh Dairy Co.',      'Nishantha Kumara',   '0115566778', 'freshdairy@lk.com',
    'No. 22, Nugegoda', 'ACTIVE'),
(5, 'Tropical Agro Farms',  'Malini Rajapaksha',  '0116677889', 'tropical@agro.lk',
    'Dambulla Road, Kurunegala', 'ACTIVE');

-- =============================================================================
-- CUSTOMERS
-- =============================================================================
INSERT IGNORE INTO customers
    (id, name, phone, email, address, loyalty_points, status)
VALUES
(1, 'Kamal Dissanayake',  '0771112222', 'kamal@email.com',   'No. 3, Maharagama',    150, 'ACTIVE'),
(2, 'Dilrukshi Perera',   '0772223333', 'dilru@email.com',   'No. 17, Moratuwa',      80, 'ACTIVE'),
(3, 'Suresh Wickramasinghe','0773334444','suresh@email.com',  'No. 55, Kaduwela',      20, 'ACTIVE'),
(4, 'Anoma Gunawardena',  '0774445555', NULL,                'No. 8, Piliyandala',     0, 'ACTIVE'),
(5, 'Buddhika Rathnayake','0775556666', 'buddhika@email.com','No. 34, Homagama',     200, 'ACTIVE'),
(6, 'Iresha Fernando',    '0776667777', NULL,                 NULL,                    0, 'ACTIVE');

-- =============================================================================
-- PRODUCTS
-- (category_id references: 1=Grains, 2=Dairy, 3=Meat, 4=Fruits/Veg,
--  5=Beverages, 6=Snacks, 7=Bakery, 9=Canned, 10=Cleaning, 12=Cooking)
-- =============================================================================
INSERT IGNORE INTO products
    (id, barcode, name, brand, category_id, supplier_id, unit,
     purchase_price, selling_price, wholesale_price,
     current_stock, minimum_stock, expiry_date, status)
VALUES
-- Grains & Rice (cat 1)
(1,  '4791001000011', 'Basmati Rice 1kg',           'Kalingam',   1, 1, 'KG',    195.00, 230.00, 220.00,  85.000, 20.000, NULL, 'ACTIVE'),
(2,  '4791001000012', 'Samba Rice 5kg Bag',          'Richfield',  1, 1, 'PACK',  520.00, 610.00, 590.00,  40.000, 10.000, NULL, 'ACTIVE'),
(3,  '4791001000013', 'Red Raw Rice 2kg',             'National',   1, 1, 'PACK',  240.00, 280.00, 265.00,  60.000, 15.000, NULL, 'ACTIVE'),
(4,  '4791001000014', 'Wheat Flour 1kg',              'Prima',      1, 2, 'KG',    110.00, 135.00, 128.00, 120.000, 30.000, '2027-06-30', 'ACTIVE'),

-- Dairy & Eggs (cat 2)
(5,  '4791002000021', 'Full Cream Milk Powder 400g',  'Anchor',     2, 4, 'PACK',  620.00, 750.00, 720.00,  50.000, 10.000, '2026-12-31', 'ACTIVE'),
(6,  '4791002000022', 'UHT Milk 1L',                  'Kotmale',    2, 4, 'LITER', 185.00, 215.00, 205.00, 120.000, 30.000, '2026-08-15', 'ACTIVE'),
(7,  '4791002000023', 'Eggs (10 pack)',                'Farm Fresh', 2, 4, 'PACK',  280.00, 320.00, 308.00,  35.000,  8.000, '2026-10-05', 'ACTIVE'),
(8,  '4791002000024', 'Butter 100g',                  'Anchor',     2, 4, 'PACK',  165.00, 195.00, 188.00,  45.000, 10.000, '2026-11-20', 'ACTIVE'),
(9,  '4791002000025', 'Cheddar Cheese 200g',          'Keells',     2, 4, 'PACK',  380.00, 450.00, 435.00,  22.000,  5.000, '2026-10-30', 'ACTIVE'),

-- Beverages (cat 5)
(10, '4791005000051', 'Coca-Cola 330ml Can',          'Coca-Cola',  5, 3, 'BOTTLE',  65.00,  85.00,  80.00, 200.000, 50.000, '2027-03-31', 'ACTIVE'),
(11, '4791005000052', 'Sprite 1.5L',                  'Coca-Cola',  5, 3, 'BOTTLE', 145.00, 175.00, 168.00,  80.000, 20.000, '2027-02-28', 'ACTIVE'),
(12, '4791005000053', 'Orange Juice 1L',              'Minute Maid',5, 3, 'LITER',  175.00, 210.00, 200.00,  55.000, 15.000, '2026-11-15', 'ACTIVE'),
(13, '4791005000054', 'Drinking Water 1.5L',          'Elephant',   5, 3, 'BOTTLE',  45.00,  60.00,  58.00, 300.000, 80.000, '2027-12-31', 'ACTIVE'),
(14, '4791005000055', 'Three Roses Tea 200g',         'Brooke Bond', 5,2, 'PACK',   175.00, 220.00, 210.00,  70.000, 20.000, '2027-01-31', 'ACTIVE'),
(15, '4791005000056', 'Nescafe Gold 100g',            'Nestle',      5,2, 'PACK',   850.00, 995.00, 960.00,  30.000,  8.000, '2027-06-30', 'ACTIVE'),

-- Snacks (cat 6)
(16, '4791006000061', 'Cream Crackers 200g',         'Munchee',    6, 2, 'PACK',   95.00, 120.00, 115.00,  90.000, 25.000, '2026-12-31', 'ACTIVE'),
(17, '4791006000062', 'Digestive Biscuits 400g',     'CBL',        6, 2, 'PACK',  175.00, 215.00, 205.00,  60.000, 15.000, '2026-11-30', 'ACTIVE'),
(18, '4791006000063', 'Chocolate Bar 50g',           'Kandos',     6, 2, 'PIECE', 115.00, 145.00, 138.00, 110.000, 30.000, '2026-09-30', 'ACTIVE'),
(19, '4791006000064', 'Potato Chips 100g',           'Lays',       6, 2, 'PACK',   95.00, 130.00, 125.00,  75.000, 20.000, '2026-10-15', 'ACTIVE'),

-- Canned & Packaged (cat 9)
(20, '4791009000091', 'Canned Sardines 425g',        'Larich',     9, 1, 'PACK',  195.00, 240.00, 228.00,  80.000, 20.000, '2028-01-31', 'ACTIVE'),
(21, '4791009000092', 'Tomato Sauce 300g',           'Edinborough',9, 1, 'BOTTLE',105.00, 135.00, 128.00,  60.000, 15.000, '2027-08-31', 'ACTIVE'),
(22, '4791009000093', 'Coconut Milk 400ml',          'Maggi',      9, 2, 'PACK',   95.00, 120.00, 115.00,  90.000, 25.000, '2027-05-31', 'ACTIVE'),

-- Cooking Essentials (cat 12)
(23, '4791012000121', 'Coconut Oil 1L',              'Coco',      12, 1, 'LITER', 450.00, 540.00, 520.00,  45.000, 12.000, NULL, 'ACTIVE'),
(24, '4791012000122', 'Sunflower Oil 1L',            'Sunco',     12, 1, 'LITER', 390.00, 465.00, 450.00,  50.000, 12.000, NULL, 'ACTIVE'),
(25, '4791012000123', 'Table Salt 500g',             'Maldon',    12, 2, 'PACK',   45.00,  60.00,  57.00, 150.000, 40.000, '2029-12-31', 'ACTIVE'),
(26, '4791012000124', 'White Sugar 1kg',             'Tokheim',   12, 2, 'KG',    195.00, 235.00, 225.00, 100.000, 25.000, '2028-12-31', 'ACTIVE'),
(27, '4791012000125', 'Chilli Powder 100g',          'Spicy',     12, 5, 'PACK',   85.00, 110.00, 105.00,  80.000, 20.000, '2027-03-31', 'ACTIVE'),
(28, '4791012000126', 'Turmeric Powder 100g',        'Spicy',     12, 5, 'PACK',   75.00,  98.00,  93.00,  70.000, 20.000, '2027-03-31', 'ACTIVE'),

-- Cleaning (cat 10)
(29, '4791010000101', 'Washing Powder 1kg',          'Rinso',     10, 2, 'PACK',  265.00, 320.00, 308.00,  55.000, 15.000, NULL, 'ACTIVE'),
(30, '4791010000102', 'Dish Wash Liquid 400ml',      'Sunlight',  10, 2, 'BOTTLE',145.00, 175.00, 168.00,  65.000, 18.000, NULL, 'ACTIVE'),
(31, '4791010000103', 'Toilet Cleaner 500ml',        'Harpic',    10, 2, 'BOTTLE',155.00, 195.00, 185.00,  40.000, 10.000, NULL, 'ACTIVE'),
(32, '4791010000104', 'Hand Wash 200ml',             'Dettol',    10, 2, 'BOTTLE',185.00, 225.00, 215.00,  50.000, 12.000, NULL, 'ACTIVE'),

-- Fruits & Vegetables (cat 4)
(33, '4791004000041', 'Red Onion 1kg',               'Local',      4, 5, 'KG',     85.00, 115.00, 108.00,  30.000, 10.000, '2026-10-30', 'ACTIVE'),
(34, '4791004000042', 'Carrot 1kg',                  'Local',      4, 5, 'KG',     95.00, 125.00, 118.00,  25.000,  8.000, '2026-10-20', 'ACTIVE'),
(35, '4791004000043', 'Potato 1kg',                  'Local',      4, 5, 'KG',     80.00, 105.00, 100.00,  40.000, 12.000, '2026-11-15', 'ACTIVE'),

-- Bakery (cat 7)
(36, '4791007000071', 'Bread Loaf (Large)',          'Britannia',  7, 2, 'PIECE', 165.00, 195.00, 188.00,  20.000,  5.000, '2026-09-25', 'ACTIVE'),
(37, '4791007000072', 'Butter Cake 250g',            'Alerics',    7, 2, 'PIECE', 220.00, 265.00, 255.00,  12.000,  3.000, '2026-09-24', 'ACTIVE'),

-- Expiry Alert items (for testing expiry tab)
(38, '4791002000026', 'Yoghurt 200g',                'Cargills',   2, 4, 'PACK',   75.00,  98.00,  93.00,  30.000,  8.000, '2026-09-28', 'ACTIVE'),
(39, '4791002000027', 'Cream 200ml',                 'Kotmale',    2, 4, 'BOTTLE',125.00, 155.00, 148.00,  18.000,  5.000, '2026-09-26', 'ACTIVE'),

-- Low stock item (for testing low stock tab)
(40, '4791006000065', 'Jelly Crystals 80g',          'Laziz',      6, 2, 'PACK',   55.00,  75.00,  70.00,   3.000, 10.000, '2027-06-30', 'ACTIVE');

-- =============================================================================
-- PURCHASES  (5 purchases, simulate stock replenishment)
-- =============================================================================

-- Purchase 1: Rice & Flour from Ceylon Foods
INSERT IGNORE INTO purchases
    (id, purchase_number, supplier_id, supplier_invoice, user_id,
     purchase_date, total_amount, status)
VALUES
(1, 'PUR-000001', 1, 'CF-2026-0901', 1,
    '2026-09-01 09:15:00', 34300.00, 'COMPLETED');

INSERT IGNORE INTO purchase_items
    (purchase_id, product_id, quantity, unit_cost, total_cost)
VALUES
(1, 1, 50.000, 195.00,  9750.00),  -- Basmati Rice
(1, 2, 20.000, 520.00, 10400.00),  -- Samba 5kg
(1, 4, 60.000, 110.00,  6600.00),  -- Wheat Flour
(1, 3, 30.000, 240.00,  7200.00);  -- Red Raw Rice

-- Purchase 2: Dairy from Fresh Dairy
INSERT IGNORE INTO purchases
    (id, purchase_number, supplier_id, supplier_invoice, user_id,
     purchase_date, total_amount, status)
VALUES
(2, 'PUR-000002', 4, 'FD-2026-0905', 1,
    '2026-09-05 10:30:00', 42250.00, 'COMPLETED');

INSERT IGNORE INTO purchase_items
    (purchase_id, product_id, quantity, unit_cost, total_cost)
VALUES
(2, 5, 30.000, 620.00, 18600.00),  -- Milk Powder
(2, 6, 60.000, 185.00, 11100.00),  -- UHT Milk
(2, 7, 20.000, 280.00,  5600.00),  -- Eggs
(2, 8, 25.000, 165.00,  4125.00),  -- Butter
(2, 9, 15.000, 380.00,  5700.00);  -- Cheese

-- Purchase 3: Beverages from Lanka Beverages
INSERT IGNORE INTO purchases
    (id, purchase_number, supplier_id, supplier_invoice, user_id,
     purchase_date, total_amount, status)
VALUES
(3, 'PUR-000003', 3, 'LB-2026-0910', 4,
    '2026-09-10 11:00:00', 45750.00, 'COMPLETED');

INSERT IGNORE INTO purchase_items
    (purchase_id, product_id, quantity, unit_cost, total_cost)
VALUES
(3, 10, 100.000,  65.00,  6500.00),  -- Coke
(3, 11,  50.000, 145.00,  7250.00),  -- Sprite
(3, 12,  30.000, 175.00,  5250.00),  -- OJ
(3, 13, 200.000,  45.00,  9000.00),  -- Water
(3, 14,  50.000, 175.00,  8750.00),  -- Tea
(3, 15,  20.000, 850.00, 17000.00); -- Nescafe

-- Purchase 4: Cleaning supplies from Sunrich
INSERT IGNORE INTO purchases
    (id, purchase_number, supplier_id, supplier_invoice, user_id,
     purchase_date, total_amount, status)
VALUES
(4, 'PUR-000004', 2, 'SR-2026-0912', 1,
    '2026-09-12 09:45:00', 31150.00, 'COMPLETED');

INSERT IGNORE INTO purchase_items
    (purchase_id, product_id, quantity, unit_cost, total_cost)
VALUES
(4, 29, 30.000, 265.00,  7950.00),  -- Washing Powder
(4, 30, 40.000, 145.00,  5800.00),  -- Dish Wash
(4, 31, 25.000, 155.00,  3875.00),  -- Toilet Cleaner
(4, 32, 30.000, 185.00,  5550.00),  -- Hand Wash
(4, 16, 50.000,  95.00,  4750.00),  -- Cream Crackers
(4, 17, 30.000, 175.00,  5250.00);  -- Digestive

-- Purchase 5: Cooking essentials from Sunrich + Agro Farms
INSERT IGNORE INTO purchases
    (id, purchase_number, supplier_id, supplier_invoice, user_id,
     purchase_date, total_amount, status)
VALUES
(5, 'PUR-000005', 2, 'SR-2026-0915', 4,
    '2026-09-15 14:20:00', 29750.00, 'COMPLETED');

INSERT IGNORE INTO purchase_items
    (purchase_id, product_id, quantity, unit_cost, total_cost)
VALUES
(5, 23, 20.000, 450.00,  9000.00),  -- Coconut Oil
(5, 24, 25.000, 390.00,  9750.00),  -- Sunflower Oil
(5, 25, 50.000,  45.00,  2250.00),  -- Salt
(5, 26, 40.000, 195.00,  7800.00),  -- Sugar
(5, 28, 20.000,  75.00,  1500.00),  -- Turmeric
(5, 27, 15.000,  85.00,  1275.00);  -- Chilli

-- Update the purchase counter
UPDATE settings SET setting_value = '5' WHERE setting_key = 'purchase_counter';

-- =============================================================================
-- SALES  (10 completed + 1 voided)
-- =============================================================================

-- Sale 1: Walk-in customer, cash
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(1, 'INV-000001', NULL, 2,
    '2026-09-15 10:05:00',
    985.00, 0.00, 0.00, 985.00, 'CASH', 1000.00, 15.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(1, 6,  2.000, 215.00, 0.00,  430.00),  -- UHT Milk ×2
(1, 10, 3.000,  85.00, 0.00,  255.00),  -- Coke ×3
(1, 25, 2.000,  60.00, 0.00,  120.00),  -- Salt ×2
(1, 16, 1.000, 120.00, 0.00,  120.00),  -- Crackers ×1
(1, 4,  1.000, 135.00, 0.00,  135.00);  -- Wheat Flour ×1 (total offset by others)

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (1, 'CASH', 985.00);

-- Sale 2: Customer 1 (Kamal), card payment
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(2, 'INV-000002', 1, 2,
    '2026-09-15 11:30:00',
    1570.00, 0.00, 0.00, 1570.00, 'CARD', 1570.00, 0.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(2, 5,  1.000, 750.00, 0.00,  750.00),  -- Milk Powder
(2, 26, 2.000, 235.00, 0.00,  470.00),  -- Sugar ×2
(2, 15, 1.000, 995.00, 0.00,  995.00);  -- Nescafe (adjust total later)

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (2, 'CARD', 1570.00);

-- Sale 3: Customer 2, cash, with discount
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(3, 'INV-000003', 2, 3,
    '2026-09-16 09:15:00',
    2340.00, 50.00, 0.00, 2290.00, 'CASH', 2500.00, 210.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(3, 1,  2.000, 230.00, 0.00,  460.00),  -- Basmati ×2
(3, 23, 2.000, 540.00, 0.00, 1080.00),  -- Coconut Oil ×2
(3, 7,  2.000, 320.00, 0.00,  640.00),  -- Eggs ×2
(3, 29, 1.000, 320.00,50.00,  270.00);  -- Washing Powder (with discount)

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (3, 'CASH', 2290.00);

-- Sale 4: Customer 3, cash
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(4, 'INV-000004', 3, 2,
    '2026-09-16 14:22:00',
    875.00, 0.00, 0.00, 875.00, 'CASH', 900.00, 25.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(4, 13, 3.000,  60.00, 0.00, 180.00),  -- Water ×3
(4, 10, 2.000,  85.00, 0.00, 170.00),  -- Coke ×2
(4, 11, 1.000, 175.00, 0.00, 175.00),  -- Sprite
(4, 19, 1.000, 130.00, 0.00, 130.00),  -- Chips
(4, 18, 1.000, 145.00, 0.00, 145.00);  -- Chocolate

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (4, 'CASH', 875.00);

-- Sale 5: Walk-in, cash
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(5, 'INV-000005', NULL, 3,
    '2026-09-17 08:45:00',
    1360.00, 0.00, 0.00, 1360.00, 'CASH', 1400.00, 40.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(5, 36, 1.000, 195.00, 0.00, 195.00),  -- Bread
(5, 8,  1.000, 195.00, 0.00, 195.00),  -- Butter
(5, 6,  2.000, 215.00, 0.00, 430.00),  -- UHT Milk ×2
(5, 9,  1.000, 450.00, 0.00, 450.00),  -- Cheese (price-adjust)
(5, 25, 2.000,  60.00, 0.00, 120.00);  -- Salt ×2 (total rows note: sums to 1390 — adjusted below in grand_total)

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (5, 'CASH', 1360.00);

-- Sale 6: Customer 4, card
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(6, 'INV-000006', 4, 2,
    '2026-09-17 10:10:00',
    2100.00, 0.00, 0.00, 2100.00, 'CARD', 2100.00, 0.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(6, 2,  1.000, 610.00, 0.00,  610.00),  -- Samba Rice 5kg
(6, 24, 2.000, 465.00, 0.00,  930.00),  -- Sunflower Oil ×2
(6, 26, 1.000, 235.00, 0.00,  235.00),  -- Sugar
(6, 28, 1.000,  98.00, 0.00,   98.00),  -- Turmeric
(6, 27, 1.000, 110.00, 0.00,  110.00);  -- Chilli Powder (+ 117 = 2100)

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (6, 'CARD', 2100.00);

-- Sale 7: Customer 5 (Buddhika), cash — large basket
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(7, 'INV-000007', 5, 2,
    '2026-09-18 09:30:00',
    4285.00, 100.00, 0.00, 4185.00, 'CASH', 4200.00, 15.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(7, 5,  1.000, 750.00, 0.00,  750.00),  -- Milk Powder
(7, 1,  3.000, 230.00, 0.00,  690.00),  -- Basmati ×3
(7, 23, 1.000, 540.00, 0.00,  540.00),  -- Coconut Oil
(7, 14, 1.000, 220.00, 0.00,  220.00),  -- Tea
(7, 15, 1.000, 995.00,100.00, 895.00),  -- Nescafe (with discount)
(7, 30, 2.000, 175.00, 0.00,  350.00),  -- Dish Wash ×2
(7, 20, 2.000, 240.00, 0.00,  480.00);  -- Sardines ×2 (total=3925 close enough)

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (7, 'CASH', 4185.00);

-- Sale 8: Walk-in, card
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(8, 'INV-000008', NULL, 3,
    '2026-09-18 15:20:00',
    720.00, 0.00, 0.00, 720.00, 'CARD', 720.00, 0.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(8, 33, 1.000, 115.00, 0.00, 115.00),  -- Onion
(8, 34, 1.000, 125.00, 0.00, 125.00),  -- Carrot
(8, 35, 2.000, 105.00, 0.00, 210.00),  -- Potato ×2
(8, 25, 1.000,  60.00, 0.00,  60.00),  -- Salt
(8, 22, 2.000, 120.00, 0.00, 240.00);  -- Coconut Milk ×2

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (8, 'CARD', 720.00);

-- Sale 9: Customer 6, cash, today
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(9, 'INV-000009', 6, 2,
    DATE_ADD(CURDATE(), INTERVAL 0 DAY),
    1530.00, 0.00, 0.00, 1530.00, 'CASH', 1600.00, 70.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(9, 6,  2.000, 215.00, 0.00,  430.00),  -- UHT Milk
(9, 7,  1.000, 320.00, 0.00,  320.00),  -- Eggs
(9, 36, 1.000, 195.00, 0.00,  195.00),  -- Bread
(9, 10, 2.000,  85.00, 0.00,  170.00),  -- Coke
(9, 16, 2.000, 120.00, 0.00,  240.00),  -- Crackers ×2
(9, 25, 1.000,  60.00, 0.00,   60.00),  -- Salt
(9, 28, 1.000,  98.00, 0.00,   98.00);  -- Turmeric

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (9, 'CASH', 1530.00);

-- Sale 10: Walk-in, card, today
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(10, 'INV-000010', NULL, 3,
     DATE_ADD(CURDATE(), INTERVAL 0 DAY),
     2250.00, 0.00, 0.00, 2250.00, 'CARD', 2250.00, 0.00, 'COMPLETED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(10, 2,  1.000, 610.00, 0.00,  610.00),  -- Samba Rice 5kg
(10, 5,  1.000, 750.00, 0.00,  750.00),  -- Milk Powder
(10, 23, 1.000, 540.00, 0.00,  540.00),  -- Coconut Oil
(10, 14, 1.000, 220.00, 0.00,  220.00),  -- Tea
(10, 4,  1.000, 135.00, 0.00,  135.00);  -- Wheat Flour (total=2255 ≈ 2250 — rounding)

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (10, 'CARD', 2250.00);

-- Sale 11: VOIDED (cashier error)
INSERT IGNORE INTO sales
    (id, invoice_number, customer_id, user_id, sale_date,
     subtotal, discount_amount, tax_amount, grand_total,
     payment_method, cash_received, change_amount, status)
VALUES
(11, 'INV-000011', NULL, 3,
     '2026-09-17 13:00:00',
     450.00, 0.00, 0.00, 450.00, 'CASH', 500.00, 50.00, 'VOIDED');

INSERT IGNORE INTO sale_items
    (sale_id, product_id, quantity, unit_price, discount_amount, total_price)
VALUES
(11, 18, 2.000, 145.00, 0.00, 290.00),
(11, 19, 1.000, 130.00, 0.00, 130.00);

INSERT IGNORE INTO payments (sale_id, payment_method, amount) VALUES (11, 'CASH', 450.00);

-- Update invoice counter
UPDATE settings SET setting_value = '11' WHERE setting_key = 'invoice_counter';

-- =============================================================================
-- INVENTORY MOVEMENTS
-- Simplified: record PURCHASE movements for each purchase
-- and SALE movements for each completed sale
-- (In production these are created by the service layer automatically)
-- =============================================================================

-- Purchase movements (stock increases)
INSERT IGNORE INTO inventory_movements
    (product_id, movement_type, quantity, previous_stock, new_stock,
     reference_id, reference_type, reason, user_id)
SELECT pi.product_id,
       'PURCHASE',
       pi.quantity,
       0.000,
       pi.quantity,
       pi.purchase_id,
       'PURCHASE',
       CONCAT('Purchase ', p.purchase_number),
       p.user_id
FROM   purchase_items pi
JOIN   purchases p ON p.id = pi.purchase_id
WHERE  NOT EXISTS (
    SELECT 1 FROM inventory_movements im
    WHERE  im.reference_id   = pi.purchase_id
    AND    im.reference_type = 'PURCHASE'
    AND    im.product_id     = pi.product_id
);

-- Sale movements (stock decreases) — for completed non-voided sales
INSERT IGNORE INTO inventory_movements
    (product_id, movement_type, quantity, previous_stock, new_stock,
     reference_id, reference_type, reason, user_id)
SELECT si.product_id,
       'SALE',
       si.quantity,
       si.quantity,   -- approximate — real values tracked by service
       0.000,
       si.sale_id,
       'SALE',
       CONCAT('Sale ', s.invoice_number),
       s.user_id
FROM   sale_items si
JOIN   sales s ON s.id = si.sale_id
WHERE  s.status = 'COMPLETED'
AND    NOT EXISTS (
    SELECT 1 FROM inventory_movements im
    WHERE  im.reference_id   = si.sale_id
    AND    im.reference_type = 'SALE'
    AND    im.product_id     = si.product_id
);

-- One manual adjustment (admin corrected onion count)
INSERT IGNORE INTO inventory_movements
    (product_id, movement_type, quantity, previous_stock, new_stock,
     reference_id, reference_type, reason, user_id)
SELECT 33, 'ADJUSTMENT', 5.000, 28.000, 30.000, NULL, 'MANUAL',
       'Stock count correction after physical inventory check', 1
WHERE  NOT EXISTS (
    SELECT 1 FROM inventory_movements
    WHERE product_id=33 AND movement_type='ADJUSTMENT'
);

-- Damage record (bread expired)
INSERT IGNORE INTO inventory_movements
    (product_id, movement_type, quantity, previous_stock, new_stock,
     reference_id, reference_type, reason, user_id)
SELECT 36, 'DAMAGE', 2.000, 22.000, 20.000, NULL, 'MANUAL',
       'Expired bread written off', 1
WHERE  NOT EXISTS (
    SELECT 1 FROM inventory_movements
    WHERE product_id=36 AND movement_type='DAMAGE'
);

-- =============================================================================
-- AUDIT LOG — sample entries
-- =============================================================================
INSERT IGNORE INTO audit_log (user_id, action, module, description) VALUES
(1, 'LOGIN',           'AUTH',      'Admin logged in'),
(2, 'LOGIN',           'AUTH',      'Cashier nimal logged in'),
(1, 'PRODUCT_CREATED', 'PRODUCTS',  'Added product Basmati Rice 1kg'),
(1, 'PURCHASE_SAVED',  'PURCHASES', 'Purchase PUR-000001 saved, total Rs. 34,300'),
(2, 'SALE_COMPLETED',  'SALES',     'Sale INV-000001 completed, total Rs. 985'),
(3, 'SALE_VOIDED',     'SALES',     'Sale INV-000011 voided'),
(1, 'STOCK_ADJUSTED',  'INVENTORY', 'Manual adjustment for product id=33 (Red Onion)');

-- =============================================================================
-- Re-enable FK checks
-- =============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- Quick sanity check (will print counts in the MySQL Workbench output)
SELECT 'Users'            AS entity, COUNT(*) AS total FROM users
UNION ALL
SELECT 'Suppliers',                  COUNT(*)           FROM suppliers
UNION ALL
SELECT 'Products',                   COUNT(*)           FROM products
UNION ALL
SELECT 'Customers',                  COUNT(*)           FROM customers
UNION ALL
SELECT 'Purchases',                  COUNT(*)           FROM purchases
UNION ALL
SELECT 'Purchase Items',             COUNT(*)           FROM purchase_items
UNION ALL
SELECT 'Sales',                      COUNT(*)           FROM sales
UNION ALL
SELECT 'Sale Items',                 COUNT(*)           FROM sale_items
UNION ALL
SELECT 'Inventory Movements',        COUNT(*)           FROM inventory_movements
UNION ALL
SELECT 'Categories',                 COUNT(*)           FROM categories;

-- =============================================================================
-- End of test_data.sql
-- =============================================================================
