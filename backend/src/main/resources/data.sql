
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
(12, 'tenant7@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Pintura, Música', 'Tarde', 'Artista'),
(13, 'tenant8@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Lectura, Cine', 'Flexible', 'Estudiante')
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
(3, 'Ático con terraza', 'Ático moderno con gran terraza.', 450.0, 'Todo incluido', 'Barcelona Eixample', 'ACTIVE', 1),
(4, 'Loft Estilo Industrial', 'Techos altos, paredes de ladrillo visto y grandes ventanales. Ideal para artistas.', 550.0, 'No incluido', 'Bilbao Abando', 'ACTIVE', 1),
(5, 'Suite Rústica en las Afueras', 'Vigas de madera y ambiente acogedor. Perfecta para amantes de la naturaleza.', 300.0, 'Leña e Internet inc.', 'Granada Alpujarra', 'ACTIVE', 1),
(6, 'Habitación Boho Chic', 'Decoración natural con mimbre y plantas. Muy luminosa y con balcón privado.', 320.0, 'Gastos fijos 50€', 'Sevilla Alameda', 'ACTIVE', 1),
(7, 'Piso Clásico Señorial', 'Mobiliario elegante, molduras en el techo y lámparas de cristal. Muy espacioso.', 600.0, 'Calefacción central inc.', 'Madrid Salamanca', 'ACTIVE', 1),
(8, 'Apartamento Minimalista', 'Espacios diáfanos, tonos blancos y máximo orden. Muy cerca del metro.', 400.0, 'Todo incluido', 'Valencia Ruzafa', 'ACTIVE', 1),
(9, 'Vivienda Mediterránea', 'Arcos de medio punto, tonos tierra y vistas despejadas. Muy fresca en verano.', 380.0, 'Agua incluida', 'Málaga Pedregalejo', 'ACTIVE', 1),
(10, 'Estudio Nórdico Moderno', 'Funcionalidad escandinava con madera clara. Recién reformado y equipado.', 370.0, 'Luz e Internet inc.', 'Barcelona Gràcia', 'ACTIVE', 1)
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
(9, 3, 3, false, 'rooma/apartments/iu4rhbdbaykkgfln46wb', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772642861/rooma/apartments/iu4rhbdbaykkgfln46wb.jpg'),
-- Apartamento 4
(10, 4, 1, true, 'rooma/apartments/Imagen13_ebuzwi', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079032/Imagen13_ebuzwi.jpg'),
(11, 4, 2, false, 'rooma/apartments/Imagen12_ezal91', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079032/Imagen12_ezal91.jpg'),
(12, 4, 3, false, 'rooma/apartments/Imagen11_spun2x', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079032/Imagen11_spun2x.jpg'),
(13, 4, 4, false, 'rooma/apartments/Imagen10_avhils', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079031/Imagen10_avhils.png'),
(14, 4, 5, false, 'rooma/apartments/Imagen9_jltbr5', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079031/Imagen9_jltbr5.png'),
(15, 4, 6, false, 'rooma/apartments/Imagen7_je1vfk', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079030/Imagen7_je1vfk.jpg'),
(16, 4, 7, false, 'rooma/apartments/Imagen8_m1mqgp', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079030/Imagen8_m1mqgp.jpg'),
-- Apartamento 5
(17, 5, 1, true, 'rooma/apartments/Imagen14_czuv5g', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079078/Imagen14_czuv5g.jpg'),
(18, 5, 2, false, 'rooma/apartments/Imagen15_cpmvqo', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079079/Imagen15_cpmvqo.jpg'),
(19, 5, 3, false, 'rooma/apartments/Imagen17_o4lnqv', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079080/Imagen17_o4lnqv.png'),
(20, 5, 4, false, 'rooma/apartments/Imagen16_g3zap8', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079080/Imagen16_g3zap8.jpg'),
(21, 5, 5, false, 'rooma/apartments/Imagen18_mtfal6', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079081/Imagen18_mtfal6.jpg'),
(22, 5, 6, false, 'rooma/apartments/Imagen19_tl65vp', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079081/Imagen19_tl65vp.jpg'),
(23, 5, 7, false, 'rooma/apartments/Imagen21_y3gbwx', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079082/Imagen21_y3gbwx.png'),
(24, 5, 8, false, 'rooma/apartments/Imagen20_khmaik', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079082/Imagen20_khmaik.png'),
-- Apartamento 6
(25, 6, 1, true, 'rooma/apartments/Imagen28_imojqj', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776078991/Imagen28_imojqj.png'),
(26, 6, 2, false, 'rooma/apartments/Imagen26_fh0ceq', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776078991/Imagen26_fh0ceq.png'),
(27, 6, 3, false, 'rooma/apartments/Imagen25_niekrm', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776078991/Imagen25_niekrm.png'),
(28, 6, 4, false, 'rooma/apartments/Imagen22_bpsvto', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776078990/Imagen22_bpsvto.png'),
(29, 6, 5, false, 'rooma/apartments/Imagen27_aobsut', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776078991/Imagen27_aobsut.png'),
(30, 6, 6, false, 'rooma/apartments/Imagen24_xltprx', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776078991/Imagen24_xltprx.png'),
(31, 6, 7, false, 'rooma/apartments/Imagen23_ovi3fa', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776078991/Imagen23_ovi3fa.png'),
-- Apartamento 7
(32, 7, 1, true, 'rooma/apartments/Imagen29_ryoqlc', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079006/Imagen29_ryoqlc.png'),
(33, 7, 2, false, 'rooma/apartments/Imagen30_j9bc3p', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079006/Imagen30_j9bc3p.png'),
(34, 7, 3, false, 'rooma/apartments/Imagen31_nfc05m', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079006/Imagen31_nfc05m.png'),
(35, 7, 4, false, 'rooma/apartments/Imagen32_ioxfcg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079006/Imagen32_ioxfcg.png'),
(36, 7, 5, false, 'rooma/apartments/Imagen33_o1iqts', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079007/Imagen33_o1iqts.png'),
(37, 7, 6, false, 'rooma/apartments/Imagen34_vl8rdb', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079007/Imagen34_vl8rdb.png'),
(38, 7, 7, false, 'rooma/apartments/Imagen35_pepwn6', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079007/Imagen35_pepwn6.png'),
-- Apartamento 8
(39, 8, 1, true, 'rooma/apartments/Minimalista_dtxpxh', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079051/Minimalista_dtxpxh.png'),
(40, 8, 2, false, 'rooma/apartments/Imagen51_g4o2te', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079048/Imagen51_g4o2te.png'),
(41, 8, 3, false, 'rooma/apartments/Imagen52_utjdyf', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079048/Imagen52_utjdyf.png'),
(42, 8, 4, false, 'rooma/apartments/Imagen54_j2fg3o', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079049/Imagen54_j2fg3o.png'),
(43, 8, 5, false, 'rooma/apartments/Imagen53_xkudw8', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079049/Imagen53_xkudw8.png'),
(44, 8, 6, false, 'rooma/apartments/Imagen56_x62aku', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079050/Imagen56_x62aku.png'),
(45, 8, 7, false, 'rooma/apartments/Imagen55_zmvgih', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079050/Imagen55_zmvgih.png'),
-- Apartamento 9
(46, 9, 1, true, 'rooma/apartments/Imagen43_hux4hs', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079041/Imagen43_hux4hs.png'),
(47, 9, 2, false, 'rooma/apartments/Imagen44_vgompl', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079042/Imagen44_vgompl.png'),
(48, 9, 3, false, 'rooma/apartments/Imagen45_ovzkfz', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079041/Imagen45_ovzkfz.png'),
(49, 9, 4, false, 'rooma/apartments/Imagen46_gtyyh8', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079042/Imagen46_gtyyh8.png'),
(50, 9, 5, false, 'rooma/apartments/Imagen47_xiju4b', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079042/Imagen47_xiju4b.png'),
(51, 9, 6, false, 'rooma/apartments/Imagen49_sn2sk9', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079043/Imagen49_sn2sk9.png'),
(52, 9, 7, false, 'rooma/apartments/Imagen48_fq20ie', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079043/Imagen48_fq20ie.png'),

-- Apartamento 10
(53, 10, 1, true, 'rooma/apartments/Imagen1_kxgxo5', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079071/Imagen1_kxgxo5.jpg'),
(54, 10, 2, false, 'rooma/apartments/Imagen2_bx1xws', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079071/Imagen2_bx1xws.jpg'),
(55, 10, 3, false, 'rooma/apartments/Imagen3_ezt1nd', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079072/Imagen3_ezt1nd.jpg'),
(56, 10, 4, false, 'rooma/apartments/Imagen4_h7eiee', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079072/Imagen4_h7eiee.jpg'),
(57, 10, 5, false, 'rooma/apartments/Imagen5_l8ujvt', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079073/Imagen5_l8ujvt.jpg'),
(58, 10, 6, false, 'rooma/apartments/Imagen6_b58bjd', 'https://res.cloudinary.com/djuqshdey/image/upload/v1776079073/Imagen6_b58bjd.jpg')

ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 5. MIEMBROS (Asignación lógica de inquilinos)
-- ==========================================
-- Piso 1: tenant1(6), tenant2(7), tenant3(8)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date,end_date) VALUES 
(1, 1, 6, 'HOMEBODY', '2024-01-01', NULL),
(2, 1, 7, 'RENTER', '2024-01-01', NULL),
(3, 1, 8, 'RENTER', '2024-01-01', NULL),
(6, 1, 11, 'RENTER', '2024-01-15', NULL),
(7, 1, 12, 'RENTER', '2024-01-20', NULL)
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
-- Deuda repartida entre los 5 miembros del Piso 1 (6, 7, 8, 11, 12)
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(1, 210.00, 'PENDING', 6, 1),
(2, 210.00, 'PENDING', 7, 1),
(3, 210.00, 'PENDING', 8, 1),
(21, 210.00, 'PENDING', 11, 1),
(22, 210.00, 'PENDING', 12, 1)
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

-- FACTURA 5: Alquiler Junio - Piso 1 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(5, 'Alquiler Junio', 1050.00, 'PAID', '2025-06-01', 1, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(9, 210.00, 'PAID', 6, 5),
(10, 210.00, 'PAID', 7, 5),
(11, 210.00, 'PAID', 8, 5),
(23, 210.00, 'PAID', 11, 5),
(24, 210.00, 'PAID', 12, 5)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 6: Luz Junio - Piso 1 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(6, 'Luz Junio', 75.00, 'PAID', '2025-06-10', 1, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(12, 25.00, 'PAID', 6, 6),
(13, 25.00, 'PAID', 7, 6),
(14, 25.00, 'PAID', 8, 6)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 7: Agua Mayo - Piso 1 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(7, 'Agua Mayo', 45.00, 'PAID', '2025-05-15', 1, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(15, 15.00, 'PAID', 6, 7),
(16, 15.00, 'PAID', 7, 7),
(17, 15.00, 'PAID', 8, 7)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 8: Alquiler Julio - Piso 2 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(8, 'Alquiler Julio', 250.00, 'PAID', '2025-07-01', 2, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(18, 250.00, 'PAID', 9, 8)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 9: Internet Julio - Piso 3 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(9, 'Internet Julio', 40.00, 'PAID', '2025-07-10', 3, 2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(19, 40.00, 'PAID', 10, 9)
ON CONFLICT (id) DO NOTHING;

-- FACTURA 10: Alquiler Junio - Piso 3 (Pagada)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(10, 'Alquiler Junio', 450.00, 'PAID', '2025-06-01', 3, 2)
ON CONFLICT (id) DO NOTHING;
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(20, 450.00, 'PAID', 10, 10)
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
(1, 'https://res.cloudinary.com/djuqshdey/image/upload/v1776090142/fuga_fregadero_suv1p2.jpg'),
(2, 'https://res.cloudinary.com/djuqshdey/image/upload/v1776090317/no_calienta_tcacl9.jpg'),
(3, 'https://res.cloudinary.com/djuqshdey/image/upload/v1776090538/persiana_rota_yb8tlv.jpg');

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

-- ==========================================
-- 9. PATCH: Añadir APPOINTMENT al constraint de notifications
-- ==========================================
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_event_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_event_type_check CHECK (event_type::text = ANY (ARRAY['MATCH','NEW_MATCH','INVITATION_ACCEPTED','INVITATION_SENT','INVITATION_REJECTED','REVIEW','NEW_BILL','BILL_PAID','APPOINTMENT']::text[]));
