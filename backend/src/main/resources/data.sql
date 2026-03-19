
-- ==========================================
-- 2. USUARIOS (IDs 1-10)
-- ==========================================
-- Landlords (1-5)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(1, 'landlord1@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Lectura, Cine', 'Mañanas libres', 'Inversor Inmobiliario'),
(2, 'landlord2@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Deportes', 'Horario de oficina', 'Arquitecta'),
(3, 'landlord3@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Golf, Viajes', 'Flexible', 'Abogado'),
(4, 'landlord4@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Cocina, Arte', 'Mañanas', 'Empresaria'),
(5, 'landlord5@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Música, Cine', 'Tarde', 'Músico')
ON CONFLICT (id) DO NOTHING;

-- Tenants (6-12)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(6, 'tenant1@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Videojuegos, Música', 'Estudiante de tarde', 'Estudiante'),
(7, 'tenant2@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Fotografía, Viajes', 'Mañanas', 'Diseñadora'),
(8, 'tenant3@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Cocina, Yoga', 'Teletrabajo', 'Developer'),
(9, 'tenant4@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Lectura, Cine', 'Rotativo', 'Enfermero'),
(10, 'tenant5@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Running', 'Mañanas', 'Periodista'),
(11, 'tenant6@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Leer, Cocinar', 'Flexible', 'Docente'),
(12, 'tenant7@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Pintura, Música', 'Tarde', 'Artista')
ON CONFLICT (id) DO NOTHING;

-- Test User (13)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES
(13, 'testuser@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Prueba, Testing', 'Flexible', 'Tester')
ON CONFLICT (id) DO NOTHING;
-- ==========================================
-- 3. APARTAMENTOS
-- ==========================================
INSERT INTO apartments (id, title, description, price, bills, ubication, state, user_id) VALUES 
(1, 'Piso céntrico luminoso', '3 hab en el centro. Buscamos gente tranquila.', 350.0, 'Agua e Internet inc.', 'Madrid Centro', 'ACTIVE', 1),
(2, 'Estudio Universitario', 'Cerca del campus. Ambiente de estudio.', 250.0, 'Gastos compartir', 'Valencia Benimaclet', 'ACTIVE', 1),
(3, 'Ático con terraza', 'Ático moderno con gran terraza.', 450.0, 'Todo incluido', 'Barcelona Eixample', 'ACTIVE', 2)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 4. FOTOS
-- ==========================================
INSERT INTO apartment_photos (id, apartment_id, orden, portada, public_id, url) VALUES
-- Apartamento 1
(1, 1, 1, true,  'rooma/apartments/dgfh1070gkygrbgmrd0w', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773686395/rooma/apartments/dgfh1070gkygrbgmrd0w.jpg'),
(2, 1, 2, false, 'rooma/apartments/l7zljsfbijp0c3vbgzmc', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773740478/rooma/apartments/l7zljsfbijp0c3vbgzmc.png'),
(3, 1, 3, false, 'rooma/apartments/fobzzwpkelkznmh9a9wk', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773607514/rooma/apartments/fobzzwpkelkznmh9a9wk.jpg'),
-- Apartamento 2
(4, 2, 1, true,  'rooma/apartments/vqihs5t9b0l7uisgz164', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773689916/rooma/apartments/vqihs5t9b0l7uisgz164.jpg'),
(5, 2, 2, false, 'rooma/apartments/hac2vyrfpeagwsbvomjj', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773600606/rooma/apartments/hac2vyrfpeagwsbvomjj.jpg'),
(6, 2, 3, false, 'rooma/apartments/fqbfxycztrgz5zrjbz4b', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773600609/rooma/apartments/fqbfxycztrgz5zrjbz4b.jpg'),
-- Apartamento 3
(7, 3, 1, true,  'rooma/apartments/a6edjyorif702tyu6886', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773607522/rooma/apartments/a6edjyorif702tyu6886.jpg'),
(8, 3, 2, false, 'rooma/apartments/l1borh6of0igs4cofpmf', 'https://res.cloudinary.com/djuqshdey/image/upload/v1773607520/rooma/apartments/l1borh6of0igs4cofpmf.jpg'),
(9, 3, 3, false, 'rooma/apartments/iu4rhbdbaykkgfln46wb', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772642861/rooma/apartments/iu4rhbdbaykkgfln46wb.jpg')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 5. MIEMBROS (Asignación lógica de inquilinos)
-- ==========================================
-- Piso 1: tenant1(6), tenant2(7), tenant3(8)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date,end_date) VALUES 
(1, 1, 6, 'HOMEBODY', '2024-01-01', NULL),
(2, 1, 7, 'RENTER', '2024-01-01', NULL),
(3, 1, 8, 'RENTER', '2024-01-01', NULL),
(6, 1, 11, 'RENTER', '2024-01-15', CURRENT_DATE - INTERVAL '7 days'),
(7, 1, 12, 'RENTER', '2024-01-20', CURRENT_DATE - INTERVAL '3 days')
ON CONFLICT (id) DO NOTHING;;

-- Piso 2: tenant4(9)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(4, 2, 9, 'HOMEBODY', '2024-02-01')
ON CONFLICT (id) DO NOTHING;

-- Piso 3: tenant5(10)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(5, 3, 10, 'HOMEBODY', '2024-02-15')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 6. FACTURAS Y DEUDAS
-- ==========================================

-- FACTURA 1: Alquiler Julio - Piso 1 (Pendiente)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(1, 'Alquiler Julio', 1050.00, 'PENDING', '2025-07-01', 1, 1)
ON CONFLICT (id) DO NOTHING;
-- Deuda repartida entre los 3 miembros del Piso 1 (6, 7, 8)
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(1, 350.00, 'PENDING', 6, 1),
(2, 350.00, 'PENDING', 7, 1),
(3, 350.00, 'PENDING', 8, 1)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 2: Internet Agosto - Piso 1 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(2, 'Internet Agosto', 60.00, 'PAID', '2025-08-01', 1, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(4, 20.00, 'PAID', 6, 2),
(5, 20.00, 'PAID', 7, 2),
(6, 20.00, 'PAID', 8, 2)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 3: Alquiler Agosto - Piso 2 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(3, 'Alquiler Agosto', 250.00, 'PAID', '2025-08-01', 2, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(7, 250.00, 'PAID', 9, 3)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 4: Alquiler Agosto - Piso 3 (Pendiente)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(4, 'Alquiler Agosto', 450.00, 'PENDING', '2025-08-01', 3, 2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(8, 450.00, 'PENDING', 10, 4)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 7. RESEÑAS DE MIEMBROS
-- ==========================================

INSERT INTO reviews (id, rating, comment, review_member_id, reviewed_member_id, apartment_id,published,review_date) VALUES
(1, 5, 'Excelente inquilino, muy responsable y limpio.', 1, 11, 1, true, CURRENT_DATE - INTERVAL '6 days'),
(2, 4, 'Buen compañero de piso, aunque a veces un poco desordenado.', 6, 11, 1, true, CURRENT_DATE - INTERVAL '6 days'),
(3, 5, 'El casero es muy atento y siempre dispuesto a ayudar.', 11, 1, 1, true, CURRENT_DATE - INTERVAL '6 days'),
(4, 4, 'Buen compañero de piso, aunque hace mucho ruido.', 11, 6, 1, true, CURRENT_DATE - INTERVAL '6 days'),
(5, 3, 'Inquilino razonable, pero necesita mejorar en la limpieza.', 1, 12, 1, false, CURRENT_DATE - INTERVAL '1 days'),
(6, 3, 'Compañero de piso razonable, pero necesita mejorar en la limpieza.', 6, 12, 1, false, CURRENT_DATE - INTERVAL '1 days')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 8. INCIDENCIAS
-- ==========================================
INSERT INTO incidents (id, title, description, category, zone, urgency, status, created_at, updated_at, resolved_at, closed_at, rejection_reason, apartment_id, tenant_id, landlord_id)
VALUES
(1, 'Fuga debajo del fregadero', 'Pierde agua cada vez que abrimos el grifo de la cocina. Hemos puesto un cubo temporal.', 'PLUMBING', 'KITCHEN', 'HIGH', 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '4 hours', NULL, NULL, NULL, 1, 6, 1),
(2, 'Microondas no calienta', 'Enciende pero no calienta la comida desde ayer por la noche.', 'APPLIANCES', 'KITCHEN', 'MEDIUM', 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '12 hours', CURRENT_TIMESTAMP - INTERVAL '12 hours', NULL, NULL, 1, 7, 1),
(3, 'Persiana rota en dormitorio', 'La persiana no sube y entra demasiada luz por la mañana.', 'OTHER', 'BEDROOM', 'LOW', 'CLOSED', CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '3 days', NULL, 1, 8, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO incident_attachments (incident_id, photo_url) VALUES
(1, 'https://images.unsplash.com/photo-1621905252507-b35492cc74b4'),
(2, 'https://images.unsplash.com/photo-1616628182509-6c6c1219f8ff');

INSERT INTO incident_status_history (id, incident_id, status, changed_at, changed_by_user_id, changed_by_email) VALUES
(1, 1, 'OPEN', CURRENT_TIMESTAMP - INTERVAL '2 days', 6, 'tenant1@test.com'),
(2, 1, 'RECEIVED', CURRENT_TIMESTAMP - INTERVAL '1 day 20 hours', 1, 'landlord1@test.com'),
(3, 1, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '4 hours', 1, 'landlord1@test.com'),
(4, 2, 'OPEN', CURRENT_TIMESTAMP - INTERVAL '5 days', 7, 'tenant2@test.com'),
(5, 2, 'RECEIVED', CURRENT_TIMESTAMP - INTERVAL '4 days 20 hours', 1, 'landlord1@test.com'),
(6, 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '2 days', 1, 'landlord1@test.com'),
(7, 2, 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '12 hours', 1, 'landlord1@test.com'),
(8, 3, 'OPEN', CURRENT_TIMESTAMP - INTERVAL '14 days', 8, 'tenant3@test.com'),
(9, 3, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '12 days', 1, 'landlord1@test.com'),
(10, 3, 'RESOLVED', CURRENT_TIMESTAMP - INTERVAL '4 days', 1, 'landlord1@test.com'),
(11, 3, 'CLOSED', CURRENT_TIMESTAMP - INTERVAL '3 days', 8, 'tenant3@test.com')
ON CONFLICT (id) DO NOTHING;
