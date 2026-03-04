-- ==========================================
-- 1. LIMPIEZA DE DATOS (Orden jerárquico)
-- ==========================================
DELETE FROM tenant_debts;
DELETE FROM bills;
DELETE FROM apartment_matches;
DELETE FROM apartment_members;
DELETE FROM reglas_vivienda;
DELETE FROM apartment_photos;
DELETE FROM refresh_tokens;
DELETE FROM apartments;
DELETE FROM users;

-- ==========================================
-- 2. USUARIOS (IDs 1-10)
-- ==========================================
-- Landlords (1-5)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(1, 'landlord1@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Lectura, Cine', 'Mañanas libres', 'Inversor Inmobiliario'),
(2, 'landlord2@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Deportes', 'Horario de oficina', 'Arquitecta'),
(3, 'landlord3@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Golf, Viajes', 'Flexible', 'Abogado'),
(4, 'landlord4@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Cocina, Arte', 'Mañanas', 'Empresaria'),
(5, 'landlord5@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Música, Cine', 'Tarde', 'Músico');

-- Tenants (6-10)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(6, 'tenant1@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Videojuegos, Música', 'Estudiante de tarde', 'Estudiante'),
(7, 'tenant2@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Fotografía, Viajes', 'Mañanas', 'Diseñadora'),
(8, 'tenant3@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Cocina, Yoga', 'Teletrabajo', 'Developer'),
(9, 'tenant4@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Lectura, Cine', 'Rotativo', 'Enfermero'),
(10, 'tenant5@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Running', 'Mañanas', 'Periodista');

-- ==========================================
-- 3. APARTAMENTOS
-- ==========================================
INSERT INTO apartments (id, title, description, price, bills, ubication, state, user_id) VALUES 
(1, 'Piso céntrico luminoso', '3 hab en el centro. Buscamos gente tranquila.', 350.0, 'Agua e Internet inc.', 'Madrid Centro', 'ACTIVE', 1),
(2, 'Estudio Universitario', 'Cerca del campus. Ambiente de estudio.', 250.0, 'Gastos compartir', 'Valencia Benimaclet', 'ACTIVE', 1),
(3, 'Ático con terraza', 'Ático moderno con gran terraza.', 450.0, 'Todo incluido', 'Barcelona Eixample', 'ACTIVE', 2);

-- ==========================================
-- 4. FOTOS
-- ==========================================
INSERT INTO apartment_photos (id, apartment_id, orden, portada, public_id, url) VALUES
(1, 1, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(2, 2, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(3, 3, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg');

-- ==========================================
-- 5. MIEMBROS (Asignación lógica de inquilinos)
-- ==========================================
-- Piso 1: tenant1(6), tenant2(7), tenant3(8)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(1, 1, 6, 'HOMEBODY', '2024-01-01'),
(2, 1, 7, 'RENTER', '2024-01-01'),
(3, 1, 8, 'RENTER', '2024-01-01');

-- Piso 2: tenant4(9)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(4, 2, 9, 'HOMEBODY', '2024-02-01');

-- Piso 3: tenant5(10)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(5, 3, 10, 'HOMEBODY', '2024-02-15');

-- ==========================================
-- 6. FACTURAS Y DEUDAS
-- ==========================================

-- FACTURA 1: Alquiler Julio - Piso 1 (Pendiente)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(1, 'Alquiler Julio', 1050.00, 'PENDING', '2025-07-01', 1, 1);
-- Deuda repartida entre los 3 miembros del Piso 1 (6, 7, 8)
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(1, 350.00, 'PENDING', 6, 1),
(2, 350.00, 'PENDING', 7, 1),
(3, 350.00, 'PENDING', 8, 1);

-- FACTURA 2: Internet Agosto - Piso 1 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(2, 'Internet Agosto', 60.00, 'PAID', '2025-08-01', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(4, 20.00, 'PAID', 6, 2),
(5, 20.00, 'PAID', 7, 2),
(6, 20.00, 'PAID', 8, 2);

-- FACTURA 3: Alquiler Agosto - Piso 2 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(3, 'Alquiler Agosto', 250.00, 'PAID', '2025-08-01', 2, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(7, 250.00, 'PAID', 9, 3);

-- FACTURA 4: Alquiler Agosto - Piso 3 (Pendiente)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(4, 'Alquiler Agosto', 450.00, 'PENDING', '2025-08-01', 3, 2);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(8, 450.00, 'PENDING', 10, 4);