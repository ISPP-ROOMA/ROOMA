-- Limpiar datos existentes (opcional pero recomendado en desarrollo)
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
(1, 'landlord1@test.com', '$2a$10$vI8aWNk0/./1cE/gY9c/..c.00gTzB.P2w.0.0.0.0.0.0.0.0.0', 'LANDLORD', 'Lectura, Cine', 'Mañanas libres', 'Inversor Inmobiliario'),
(2, 'landlord2@test.com', '$2a$10$vI8aWNk0/./1cE/gY9c/..c.00gTzB.P2w.0.0.0.0.0.0.0.0.0', 'LANDLORD', 'Deportes', 'Horario de oficina', 'Arquitecta');

-- Inquilinos (TENANT)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(3, 'tenant1@test.com', '$2a$10$vI8aWNk0/./1cE/gY9c/..c.00gTzB.P2w.0.0.0.0.0.0.0.0.0', 'TENANT', 'Videojuegos, Música', 'Estudiante de tarde', 'Estudiante Universitario'),
(4, 'tenant2@test.com', '$2a$10$vI8aWNk0/./1cE/gY9c/..c.00gTzB.P2w.0.0.0.0.0.0.0.0.0', 'TENANT', 'Fotografía, Viajes', 'Horario intensivo mañana', 'Diseñadora Gráfica'),
(5, 'tenant3@test.com', '$2a$10$vI8aWNk0/./1cE/gY9c/..c.00gTzB.P2w.0.0.0.0.0.0.0.0.0', 'TENANT', 'Cocina, Yoga', 'Teletrabajo', 'Desarrollador Software'),
(6, 'tenant4@test.com', '$2a$10$vI8aWNk0/./1cE/gY9c/..c.00gTzB.P2w.0.0.0.0.0.0.0.0.0', 'TENANT', 'Lectura, Cine', 'Horario rotativo', 'Enfermero');


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
