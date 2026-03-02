-- Limpiar datos existentes
DELETE FROM apartment_members;
DELETE FROM apartment_photos;
DELETE FROM apartments;
DELETE FROM users;

-- ==========================================
-- USERS
-- ==========================================

-- Arrendadores (LANDLORD)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(1, 'landlord1@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Lectura, Cine', N'Mañanas libres', 'Inversor Inmobiliario'),
(2, 'landlord2@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Deportes', 'Horario de oficina', 'Arquitecta'),
(3, 'landlord3@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Golf, Viajes', 'Flexible', 'Abogado'),
(4, 'landlord4@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Cocina, Arte', N'Mañanas', 'Empresaria'),
(5, 'landlord5@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'LANDLORD', 'Música, Cine', 'Tarde', 'Músico');

-- Inquilinos (TENANT)
INSERT INTO users (id, email, password, role, hobbies, schedule, profession) VALUES 
(6,  'tenant1@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Videojuegos, Música', 'Estudiante de tarde', 'Estudiante Universitario'),
(7,  'tenant2@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Fotografía, Viajes', 'Horario intensivo mañana', 'Diseñadora Gráfica'),
(8,  'tenant3@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Cocina, Yoga', 'Teletrabajo', 'Desarrollador Software'),
(9,  'tenant4@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Lectura, Cine', 'Horario rotativo', 'Enfermero'),
(10, 'tenant5@test.com', '$2a$10$xSV.G4QXraYvYw7KTjt8eObjK8BFvYegnEXXr0yB0axtbqEayYgxK', 'TENANT', 'Running, Senderismo', N'Mañanas', 'Periodista');


-- ==========================================
-- APARTMENTS (18 pisos en 10 ciudades)
-- ==========================================

INSERT INTO apartments (id, title, description, price, bills, ubication, state, user_id) VALUES 
-- Landlord 1 — Madrid
(1,  'Piso céntrico luminoso',            'Amplio piso de 3 habitaciones en el centro de Madrid. Muy luminoso y con buena ventilación. Salón grande y cocina equipada. Buscamos persona tranquila y responsable.',        350.0, 'Agua e Internet incluidos',       'Madrid Centro',           'ACTIVE', 1),
(2,  'Habitación barrio universitario',   'Se alquila habitación para estudiantes, cerca del campus universitario. Ambiente de estudio y respeto. Piso de 4 habitaciones con sala de estudio.',                           250.0, 'Gastos a compartir (aprox 30€)', 'Valencia Benimaclet',     'ACTIVE', 1),
(3,  N'Estudio moderno Malasaña',          N'Estudio completamente reformado con cocina integrada y baño en suite. Ideal para profesional joven. A 5 min del metro Tribunal.',                                              620.0, 'Todos los gastos incluidos',     N'Madrid Malasaña',         'ACTIVE', 1),
(4,  'Habitación doble Lavapiés',         'Piso compartido de 4 habitaciones en Lavapiés. Muy buen ambiente multicultural. Sala común amplia con sofá, TV 55" y zona de trabajo.',                                       290.0, 'Agua y luz a partes iguales',   'Madrid Lavapiés',         'ACTIVE', 1),
-- Landlord 2 — Barcelona
(5,  'Ático con terraza Barcelona',       'Precioso ático moderno con gran terraza para hacer barbacoas y disfrutar del sol. Vistas a la ciudad. Preferible gente trabajadora y ordenada.',                               450.0, 'Todo incluido',                 'Barcelona Eixample',      'ACTIVE', 2),
(6,  'Piso con vistas al mar',            'Amplia habitación en piso con vistas parciales al Mediterráneo. Salón luminoso y cocina completa. A 10 min caminando de la Barceloneta.',                                     480.0, 'Gastos no incluidos',           'Barcelona Barceloneta',   'ACTIVE', 2),
(7,  'Habitación Gràcia con carácter',    'Habitación en piso de artistas en el barrio de Gràcia. Ambiente creativo y tranquilo. Terraza comunitaria con plantas. Bienvenidos todos los perfiles.',                      380.0, 'Internet incluido',             'Barcelona Gràcia',        'ACTIVE', 2),
-- Landlord 3 — Sevilla y alrededores
(8,  'Piso compartido centro Sevilla',    'Habitación en piso de 3 personas en el corazón de Sevilla. A pasos de la Catedral y el Barrio de Santa Cruz. Perfecto para disfrutar la ciudad.',                            280.0, 'Agua e Internet incluidos',     'Sevilla Centro',          'ACTIVE', 3),
(9,  'Estudio Triana',                    N'Encantador estudio en el clásico barrio de Triana. Cocina americana, baño reformado y mucha luz natural. Muy tranquilo a pesar de estar bien ubicado.',                       520.0, 'Gastos incluidos excepto luz',  'Sevilla Triana',          'ACTIVE', 3),
(10, 'Habitación en villa con piscina',   'Habitación en chalet adosado con piscina comunitaria y jardín. Perfecto para personas que valoran el espacio y el aire libre. Zona muy tranquila.',                          350.0, 'Gastos no incluidos',           'Sevilla Palomares',       'ACTIVE', 3),
-- Landlord 4 — Norte y Granada
(11, 'Loft industrial Bilbao',            N'Espectacular loft de diseño industrial reformado. Techos altos de 4m, ventanales enormes y mobiliario de diseño. A 5 min del Museo Guggenheim.',                             700.0, 'Todo incluido',                 'Bilbao Abando',           'ACTIVE', 4),
(12, 'Piso zona universitaria Granada',   'Habitación cómoda y soleada en piso de 5 personas cerca de la UGR. Ambiente estudiantil y buen rollo garantizado. Terraza con vistas a Sierra Nevada.',                     210.0, 'Gastos a compartir',            'Granada Universidad',     'ACTIVE', 4),
(13, 'Apartamento moderno Málaga',        'Apartamento completamente equipado a 5 minutos de la Costa del Sol. Piscina comunitaria, parking disponible y zona de barbacoa.',                                            550.0, 'Agua e Internet incluidos',     'Málaga Centro',           'ACTIVE', 4),
-- Landlord 5 — Levante y resto
(14, 'Habitación luminosa Alicante',      'Habitación exterior con mucha luz en piso reformado. A 15 min de la playa y cerca de todos los servicios. Buscamos persona ordenada y tranquila.',                          300.0, 'Internet incluido',             'Alicante Centro',         'ACTIVE', 5),
(15, 'Piso compartido Zaragoza',          'Bonito piso de 3 habitaciones en el centro de Zaragoza, cerca de la Plaza del Pilar. Muy bien comunicado con bus y tranvía. Ambiente familiar.',                             270.0, 'Agua incluida',                 'Zaragoza Centro',         'ACTIVE', 5),
(16, 'Estudio amueblado San Sebastián',   'Acogedor estudio totalmente amueblado a 5 minutos de La Concha. Ideal para profesional. Edificio con ascensor y portero físico. Recién reformado.',                         750.0, 'Todo incluido',                 'San Sebastián Centro',    'ACTIVE', 5),
(17, 'Habitación piso moderno Murcia',    'Piso reformado de 4 habitaciones, cocina americana y salón con Smart TV. Cerca de la Universidad de Murcia y el centro comercial.',                                          220.0, 'Gastos a partes iguales',       'Murcia El Carmen',        'ACTIVE', 5),
(18, 'Duplex con jardín Valencia',        'Amplio dúplex con jardín privado en zona residencial tranquila del barrio de Ruzafa. Perfecto para quien valora el espacio exterior y la naturaleza.',                       580.0, 'Gastos no incluidos',           'Valencia Ruzafa',         'ACTIVE', 5);


-- ==========================================
-- APARTMENT PHOTOS (portada — orden=1)
-- Imágenes de Unsplash (reales, no requieren autenticación)
-- ==========================================

INSERT INTO apartment_photos (id, apartment_id, orden, portada, public_id, url) VALUES
(1,  1,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(2,  2,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(3,  3,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(4,  4,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(5,  5,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(6,  6,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(7,  7,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(8,  8,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(9,  9,  1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(10, 10, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(11, 11, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(12, 12, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(13, 13, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(14, 14, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(15, 15, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(16, 16, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(17, 17, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg'),
(18, 18, 1, true, '/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg', 'https://res.cloudinary.com/djuqshdey/image/upload/v1772099621/rooma/apartments/oaazdrxh77fvjcm1t1dz.jpg');


-- ==========================================
-- APARTMENT MEMBERS
-- ==========================================

-- Piso 1 — tenant1 y tenant2
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(1, 1, 6, 'HOMEBODY', '2023-09-01'),
(2, 1, 7, 'RENTER',   '2023-09-15');

-- Piso 2 — tenant3 solo
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(3, 2, 8, 'HOMEBODY', '2024-01-10');

-- Piso 5 — tenant4 solo
INSERT INTO apartment_members (id, apartment_id, user_id, role, join_date) VALUES 
(4, 5, 9, 'HOMEBODY', '2024-03-01');

-- Resto de pisos disponibles para swipe (sin inquilinos aún)
