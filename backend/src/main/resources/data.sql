-- Limpiar datos existentes (opcional pero recomendado en desarrollo)
DELETE FROM tenant_debts;
DELETE FROM bills;
DELETE FROM apartment_members;
DELETE FROM apartment_photos;
DELETE FROM apartments;
DELETE FROM users;

-- ==========================================
-- USERS
-- Todas las contraseñas están en texto plano o deberás actualizarlas según el encoder.
-- Por defecto Spring Security suele usar BCrypt. Aquí se asume texto plano '123456' si el encoder lo permite,
-- o tendrías que registrar uno nuevo usando la UI.
-- ==========================================

-- Arrendadores (LANDLORD)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(1, 'landlord1@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'LANDLORD', 'Lectura, Cine', 'Mañanas libres', 'Inversor Inmobiliario'),
(2, 'landlord2@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'LANDLORD', 'Deportes', 'Horario de oficina', 'Arquitecta');

-- Inquilinos (TENANT)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(3, 'tenant1@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Videojuegos, Música', 'Estudiante de tarde', 'Estudiante Universitario'),
(4, 'tenant2@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Fotografía, Viajes', 'Horario intensivo mañana', 'Diseñadora Gráfica'),
(5, 'tenant3@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Cocina, Yoga', 'Teletrabajo', 'Desarrollador Software'),
(6, 'tenant4@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Lectura, Cine', 'Horario rotativo', 'Enfermero');


-- ==========================================
-- APARTMENTS
-- ==========================================

INSERT INTO apartments (id, title, description, price, bills, ubication, state, user_id) VALUES 
(1, 'Piso céntrico luminoso', 'Amplio piso de 3 habitaciones en el centro de la ciudad. Muy luminoso y con buena ventilación. Buscamos a alguien tranquilo.', 350.0, 'Agua e Internet incluidos', 'Madrid Centro', 'ACTIVE', 1),
(2, 'Habitación en barrio universitario', 'Se alquila habitación para estudiantes, cerca del campus universitario. Ambiente de estudio y respeto.', 250.0, 'Gastos a compartir (aprox 30€)', 'Valencia Benimaclet', 'ACTIVE', 1),
(3, 'Ático con terraza', 'Precioso ático moderno con gran terraza para hacer barbacoas. Preferible gente trabajadora.', 450.0, 'Todo incluido', 'Barcelona Eixample', 'ACTIVE', 2);

-- ==========================================
-- APARTMENT PHOTOS
-- Rellena url/public_id con tus valores
-- ==========================================

INSERT INTO apartment_photos (id, apartment_id, orden, portada, public_id, url) VALUES
(1, 1, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(2, 2, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(3, 3, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg');

-- ==========================================
-- APARTMENT MEMBERS
-- role: HOMEBODY, RENTER
-- ==========================================

-- Piso 1 (Id: 1)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(1, 1, 3, 'HOMEBODY', '2023-09-01'), -- tenant1 es HOMEBODY del piso 1
(2, 1, 4, 'RENTER', '2023-09-15'); -- tenant2 es RENTER del piso 1

-- Piso 2 (Id: 2)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(3, 2, 5, 'HOMEBODY', '2024-01-10'); -- tenant3 es solitario/HOMEBODY del piso 2

-- El piso 3 actualmente no tiene inquilinos en esta base de datos de ejemplo.

-- ==========================================
-- Más usuarios de prueba
-- ==========================================
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES
(7, 'tenant5@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Cine, Viajes', 'Flexible', 'Marketing'),
(8, 'tenant6@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Deportes', 'Mañanas', 'Profesor'),
(9, 'landlord3@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'LANDLORD', 'Jardinería', 'Tardes', 'Propietario');

-- ==========================================
-- Más APARTMENTS
-- ==========================================
INSERT INTO apartments (id, title, description, price, bills, ubication, state, user_id) VALUES
(4, 'Estudio acogedor cerca del metro', 'Estudio ideal para persona sola, listo para entrar a vivir.', 300.0, 'Agua incluida', 'Madrid Lavapiés', 'ACTIVE', 9),
(5, 'Habitación doble en piso compartido', 'Habitación doble en piso con dos baños y amplio salón.', 280.0, 'Gastos a compartir (45€)', 'Sevilla Triana', 'ACTIVE', 1);

-- ==========================================
-- MÁS APARTMENT PHOTOS
-- ==========================================
INSERT INTO apartment_photos (id, apartment_id, orden, portada, public_id, url) VALUES
(4, 1, 2, false, '/rooma/apartments/oaazdrxh77fvjcm1t1dz_2.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099622/rooma/apartments/oaazdrxh77fvjcm1t1dz_2.jpg'),
(5, 1, 3, false, '/rooma/apartments/oaazdrxh77fvjcm1t1dz_3.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099623/rooma/apartments/oaazdrxh77fvjcm1t1dz_3.jpg'),
(6, 4, 1, true, '/rooma/apartments/studio1.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099624/rooma/apartments/studio1.jpg'),
(7, 5, 1, true, '/rooma/apartments/room1.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099625/rooma/apartments/room1.jpg');

-- ==========================================
-- MÁS APARTMENT MEMBERS (ampliar compañeros)
-- ==========================================
-- Añadir un tercer miembro al piso 1 y ocupar el piso 3 con un inquilino (tenant4)
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES
(4, 1, 5, 'RENTER', '2024-03-01'),
(5, 3, 6, 'HOMEBODY', '2024-02-15'),
(6, 4, 7, 'HOMEBODY', '2024-11-01'),
(7, 5, 8, 'RENTER', '2025-01-20');

-- ==========================================
-- Más compañeros para el piso del tenant1 (apartment 1)
-- ==========================================
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES
(10, 'tenant7@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Música, Cocina', 'Tardes', 'Estudiante'),
(11, 'tenant8@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Deportes, Running', 'Mañanas', 'Ingeniero'),
(12, 'tenant9@test.com', '$2a$10$n13oA7FdkI4.H2W/6JG0TufyC2U91I1t7jZ9pKdsqukvkDsoRYUOO', 'TENANT', 'Lectura', 'Flexible', 'Diseñador');

INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES
(8, 1, 10, 'RENTER', '2025-06-01'),
(9, 1, 11, 'RENTER', '2025-07-01'),
(10, 1, 12, 'RENTER', '2025-08-01');

-- ==========================================
-- FACTURAS (bills) y deudas por inquilino (tenant_debts)
-- ==========================================

-- === Piso 1 (apartment_id=1, landlord user_id=1) ===
-- Miembros activos: tenant1(3), tenant2(4), tenant3(5), tenant7(10), tenant8(11), tenant9(12)

-- Bill 1: Alquiler Julio — vencida (urgente)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(1, 'Alquiler', 2100.00, 'PENDING', '2025-07-01', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(1, 350.00, 'PENDING', 3, 1),
(2, 350.00, 'PENDING', 4, 1),
(3, 350.00, 'PENDING', 5, 1),
(4, 350.00, 'PENDING', 10, 1),
(5, 350.00, 'PENDING', 11, 1),
(6, 350.00, 'PENDING', 12, 1);

-- Bill 2: Electricidad Mayo — ya pagada
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(2, 'Electricidad', 132.00, 'PAID', '2025-05-15', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(7, 22.00, 'PAID', 3, 2),
(8, 22.00, 'PAID', 4, 2),
(9, 22.00, 'PAID', 5, 2),
(10, 22.00, 'PAID', 10, 2),
(11, 22.00, 'PAID', 11, 2),
(12, 22.00, 'PAID', 12, 2);

-- Bill 3: Agua Junio — vence pronto
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(3, 'Agua', 78.00, 'PENDING', '2025-08-05', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(13, 13.00, 'PENDING', 3, 3),
(14, 13.00, 'PENDING', 4, 3),
(15, 13.00, 'PENDING', 5, 3),
(16, 13.00, 'PENDING', 10, 3),
(17, 13.00, 'PENDING', 11, 3),
(18, 13.00, 'PENDING', 12, 3);

-- Bill 4: Internet Julio — vence en el futuro
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(4, 'Internet', 54.00, 'PENDING', '2025-09-01', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(19, 9.00, 'PENDING', 3, 4),
(20, 9.00, 'PENDING', 4, 4),
(21, 9.00, 'PENDING', 5, 4),
(22, 9.00, 'PENDING', 10, 4),
(23, 9.00, 'PENDING', 11, 4),
(24, 9.00, 'PENDING', 12, 4);

-- Bill 5: Gas Abril — pagada
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(5, 'Gas', 96.00, 'PAID', '2025-04-20', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(25, 16.00, 'PAID', 3, 5),
(26, 16.00, 'PAID', 4, 5),
(27, 16.00, 'PAID', 5, 5),
(28, 16.00, 'PAID', 10, 5),
(29, 16.00, 'PAID', 11, 5),
(30, 16.00, 'PAID', 12, 5);

-- Bill 6: Comunidad Julio — vencida (urgente)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(6, 'Comunidad', 300.00, 'PENDING', '2025-07-05', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(31, 50.00, 'PENDING', 3, 6),
(32, 50.00, 'PENDING', 4, 6),
(33, 50.00, 'PENDING', 5, 6),
(34, 50.00, 'PENDING', 10, 6),
(35, 50.00, 'PENDING', 11, 6),
(36, 50.00, 'PENDING', 12, 6);

-- Bill 7: Alquiler Junio — parcialmente pagada (tenant1 ya pagó)
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(7, 'Alquiler', 2100.00, 'PENDING', '2025-06-01', 1, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(37, 350.00, 'PAID', 3, 7),
(38, 350.00, 'PENDING', 4, 7),
(39, 350.00, 'PENDING', 5, 7),
(40, 350.00, 'PENDING', 10, 7),
(41, 350.00, 'PENDING', 11, 7),
(42, 350.00, 'PENDING', 12, 7);

-- === Piso 2 (apartment_id=2, landlord user_id=1) ===
-- Miembro: tenant3 (id 5)
-- Bill 8: Alquiler piso 2
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(8, 'Alquiler', 250.00, 'PAID', '2025-06-05', 2, 1);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(43, 250.00, 'PAID', 5, 8);

-- === Piso 3 (apartment_id=3, landlord user_id=2) ===
-- Miembro: tenant4 (id 6)
-- Bill 9: Alquiler piso 3
INSERT INTO bills (id, reference, total_amount, status, du_date, apartment_id, user_id) VALUES
(9, 'Alquiler', 450.00, 'PENDING', '2025-08-01', 3, 2);
INSERT INTO tenant_debts (id, amount, status, user_id, bill_id) VALUES
(44, 450.00, 'PENDING', 6, 9);

-- Notas: los ids aquí son de ejemplo y deben ajustarse si tu base de datos aplica secuencias automáticas.
