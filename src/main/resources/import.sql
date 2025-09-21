-- ==============================
-- IMPORT.SQL - FCF Motors Database Population 
-- ==============================

-- UTENTI con password che rispettano la validazione (maiuscola + minuscola + numero)
-- Password: Password123
INSERT INTO users (username, email, password, roles_string) VALUES ('admin', 'admin@fcfmotors.com', '$2a$10$DrxNtdyJH2qWjGln7Kmcqe.3xZRsCaC6ye5bYcYMx2wAzCwEB6jZC', 'ADMIN');
INSERT INTO users (username, email, password, roles_string) VALUES ('dealer_roma', 'dealer@fcfmotors.com', '$2a$10$DrxNtdyJH2qWjGln7Kmcqe.3xZRsCaC6ye5bYcYMx2wAzCwEB6jZC', 'DEALER');
INSERT INTO users (username, email, password, roles_string) VALUES ('mario_rossi', 'mario@email.com', '$2a$10$DrxNtdyJH2qWjGln7Kmcqe.3xZRsCaC6ye5bYcYMx2wAzCwEB6jZC', 'PRIVATE');
INSERT INTO users (username, email, password, roles_string) VALUES ('giovanni_bianchi', 'giovanni@email.com', '$2a$10$DrxNtdyJH2qWjGln7Kmcqe.3xZRsCaC6ye5bYcYMx2wAzCwEB6jZC', 'USER');
INSERT INTO users (username, email, password, roles_string) VALUES ('dealer_milano', 'milano@fcfmotors.com', '$2a$10$DrxNtdyJH2qWjGln7Kmcqe.3xZRsCaC6ye5bYcYMx2wAzCwEB6jZC', 'DEALER');

-- INFORMAZIONI ACCOUNT
INSERT INTO account_information (user_id, first_name, last_name, birth_date, address, phone_number) VALUES (1, 'Admin', 'FCF', '1980-01-01', 'Via Roma 1, Roma', '+39 06 12345');
INSERT INTO account_information (user_id, first_name, last_name, birth_date, address, phone_number) VALUES (2, 'Marco', 'Ferrari', '1975-05-15', 'Via del Corso 100, Roma', '+39 06 99887');
INSERT INTO account_information (user_id, first_name, last_name, birth_date, address, phone_number) VALUES (3, 'Mario', 'Rossi', '1985-08-22', 'Via Milano 45, Roma', '+39 333 456789');
INSERT INTO account_information (user_id, first_name, last_name, birth_date, address, phone_number) VALUES (4, 'Giovanni', 'Bianchi', '1990-12-10', 'Via Napoli 23, Roma', '+39 334 567890');
INSERT INTO account_information (user_id, first_name, last_name, birth_date, address, phone_number) VALUES (5, 'Luca', 'Verdi', '1978-03-18', 'Corso Buenos Aires 50, Milano', '+39 02 123456');

-- CONCESSIONARI (Dealer)
INSERT INTO dealer (name, description, address, phone, email, owner_id) VALUES ('Ferrari Roma Center', 'Concessionario ufficiale Ferrari nel cuore di Roma. Vendita auto nuove e usate, assistenza specializzata e ricambi originali.', 'Via del Corso 100, 00186 Roma', '+39 06 99887766', 'info@ferrariroma.com', 2);
INSERT INTO dealer (name, description, address, phone, email, owner_id) VALUES ('Auto Milano Premium', 'Il punto di riferimento per auto di lusso a Milano. Marchi premium, servizi esclusivi e finanziamenti personalizzati.', 'Corso Buenos Aires 50, 20124 Milano', '+39 02 123456789', 'premium@automilanopremium.it', 5);

-- PRODOTTI (AUTO)
INSERT INTO product (brand, model, category, price, year, mileage, fuel_type, transmission, description, seller_id, seller_type, is_featured, featured_until) VALUES ('Ferrari', '488 GTB', 'SPORTIVA', 280000.00, 2020, 15000, 'BENZINA', 'AUTOMATICO', 'Supercar italiana con motore V8 biturbo da 670 CV. Condizioni eccellenti, tagliandi regolari presso officina autorizzata.', 2, 'DEALER', true, '2025-12-31 23:59:59');
INSERT INTO product (brand, model, category, price, year, mileage, fuel_type, transmission, description, seller_id, seller_type, is_featured, featured_until) VALUES ('Lamborghini', 'Huracan', 'SPORTIVA', 250000.00, 2019, 20000, 'BENZINA', 'AUTOMATICO', 'Supersportiva con motore V10 aspirato da 610 CV. Prestazioni mozzafiato e design iconico del toro.', 2, 'DEALER', false, null);
INSERT INTO product (brand, model, category, price, year, mileage, fuel_type, transmission, description, seller_id, seller_type, is_featured, featured_until) VALUES ('Mercedes-Benz', 'S-Class', 'BERLINA', 95000.00, 2021, 35000, 'IBRIDA', 'AUTOMATICO', 'Ammiraglia di lusso con tecnologie all avanguardia. Comfort e prestazioni al massimo livello.', 2, 'DEALER', true, '2025-10-15 23:59:59');
INSERT INTO product (brand, model, category, price, year, mileage, fuel_type, transmission, description, seller_id, seller_type, is_featured, featured_until) VALUES ('BMW', 'X5 M', 'SUV', 120000.00, 2022, 25000, 'BENZINA', 'AUTOMATICO', 'SUV sportivo con motore V8 da 625 CV. Perfetta combinazione di lusso, prestazioni e praticità.', 5, 'DEALER', true, '2025-11-30 23:59:59');
INSERT INTO product (brand, model, category, price, year, mileage, fuel_type, transmission, description, seller_id, seller_type, is_featured, featured_until) VALUES ('Audi', 'RS6 Avant', 'FAMILIARE', 145000.00, 2021, 18000, 'BENZINA', 'AUTOMATICO', 'Wagon sportiva con motore V8 biturbo da 600 CV. Spazio e prestazioni in un unico veicolo.', 5, 'DEALER', false, null);
INSERT INTO product (brand, model, category, price, year, mileage, fuel_type, transmission, description, seller_id, seller_type, is_featured, featured_until) VALUES ('Porsche', '911 Carrera S', 'SPORTIVA', 165000.00, 2020, 12000, 'BENZINA', 'MANUALE', 'Icona sportiva tedesca. Motore boxer 6 cilindri da 450 CV, cambio manuale per i puristi.', 5, 'DEALER', true, '2025-09-20 23:59:59');
INSERT INTO product (brand, model, category, price, year, mileage, fuel_type, transmission, description, seller_id, seller_type, is_featured, featured_until) VALUES ('Volkswagen', 'Golf GTI', 'BERLINA', 35000.00, 2019, 45000, 'BENZINA', 'MANUALE', 'Hot hatch tedesca in ottime condizioni. Motore turbo da 245 CV, sempre in garage, non fumatori.', 3, 'PRIVATE', false, null);

-- ABBONAMENTI (Subscription)
INSERT INTO subscription (name, description, price, duration_days, max_featured_cars ) VALUES ('Basic Dealer', 'Piano base per concessionari. Include visibilità standard e gestione inventario base.', 99.99, 30, 3);
INSERT INTO subscription (name, description, price, duration_days, max_featured_cars) VALUES ('Premium Dealer', 'Piano premium per concessionari. Maggiore visibilità, statistiche avanzate e supporto prioritario.', 199.99, 30, 10);
INSERT INTO subscription (name, description, price, duration_days, max_featured_cars) VALUES ('Pro Dealer', 'Piano professionale per grandi concessionari. Massima visibilità e strumenti marketing completi.', 399.99, 30, 25);
INSERT INTO subscription (name, description, price, duration_days, max_featured_cars) VALUES ('Private Seller', 'Piano per venditori privati. Promuovi la tua auto con visibilità premium per vendite rapide.', 29.99, 15, 1);

-- ABBONAMENTI UTENTI ATTIVI (UserSubscription)
INSERT INTO user_subscription (user_id, subscription_id, start_date, expiry_date, active, auto_renew) VALUES (2, 2, '2024-08-01 00:00:00', '2024-08-31 23:59:59', true, true);
INSERT INTO user_subscription (user_id, subscription_id, start_date, expiry_date, active, auto_renew) VALUES (5, 3, '2024-08-01 00:00:00', '2024-08-31 23:59:59', true, false);
INSERT INTO user_subscription (user_id, subscription_id, start_date, expiry_date, active, auto_renew) VALUES (3, 4, '2024-08-10 00:00:00', '2024-08-25 23:59:59', true, true);

-- IMMAGINI (Image) - Per prodotti e dealer (CORRETTE PER POSTGRESQL OID)
-- Immagini Ferrari 488 GTB (product_id = 1)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 1, null);
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 1, null);

-- Immagini Lamborghini Huracan (product_id = 2)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 2, null);

-- Immagini Mercedes S-Class (product_id = 3)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 3, null);

-- Immagini BMW X5 M (product_id = 4)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 4, null);

-- Immagini Audi RS6 (product_id = 5)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 5, null);

-- Immagini Porsche 911 (product_id = 6)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 6, null);

-- Immagini Golf GTI (product_id = 7)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', 7, null);

-- Immagini concessionari (dealer_id)
-- Immagine Ferrari Roma Center (dealer_id = 1)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', null, 1);

-- Immagine Auto Milano Premium (dealer_id = 2)
INSERT INTO image (data, content_type, product_id, dealer_id) VALUES (lo_from_bytea(0, decode('FFD8FFE000104A46494600010101006000600000FFDB004300080606070605080707070909080A0C140D0C0B0B0C1912130F141D1A1F1E1D1A1C1C20242E2720222C231C1C2837292C30313434341F27393D38323C2E333432FFDB0043010909090C0B0C180D0D1832211C213232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232FFC00011080064006403012200021101031101FFC4001F0000010501010101010100000000000000000102030405060708090A0B', 'hex')), 'image/jpeg', null, 2);