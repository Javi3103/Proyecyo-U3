-- seed.sql
-- Datos de prueba para Master Gateway: los dos usuarios necesarios para
-- probar el proyecto completo desde cero en otra máquina (ej. la de un
-- compañero de equipo), con sus roles, módulos y menús.
--
-- Seguro de correr mas de una vez: cada INSERT valida que el registro no
-- exista todavia (WHERE NOT EXISTS), asi que no falla ni duplica datos si
-- se ejecuta dos veces contra la misma base.
--
-- Usuarios creados (los hashes son BCrypt reales, costo 12, generados con
-- el mismo BCryptPasswordEncoder que usa el backend en SecurityConfig.java):
--   admin@mastergateway.local    / Test1234     (rol ADMIN)
--   vendedor@mastergateway.local / Vendedor123  (rol Vendedor)

-- ============================================================================
-- USUARIO ADMIN — rol ADMIN, modulo "Administracion"
-- ============================================================================

-- 1. Rol "ADMIN"
INSERT INTO rol (id, nombre, descripcion, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'ADMIN', 'Administrador del sistema', 'ACTIVO', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'ADMIN');

-- 2. Modulo "Administracion"
INSERT INTO modulo (id, nombre, descripcion, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'Administracion', 'Modulo de administracion del sistema', 'ACTIVO', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM modulo WHERE nombre = 'Administracion');

-- 3. Menu principal "Administracion" (parent_id NULL = Menu Principal, sin url propia)
INSERT INTO menu (id, nombre, url, modulo_id, parent_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'Administracion', NULL, m.id, NULL, 'ACTIVO', now(), now()
FROM modulo m
WHERE m.nombre = 'Administracion'
  AND NOT EXISTS (SELECT 1 FROM menu WHERE nombre = 'Administracion' AND parent_id IS NULL);

-- 4. Item hoja "Usuarios" (nodo final, con url real) bajo el menu "Administracion"
INSERT INTO menu (id, nombre, url, modulo_id, parent_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'Usuarios', '/admin/usuarios', padre.modulo_id, padre.id, 'ACTIVO', now(), now()
FROM menu padre
WHERE padre.nombre = 'Administracion' AND padre.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM menu WHERE nombre = 'Usuarios' AND url = '/admin/usuarios');

-- 5. Asignar el modulo y sus menus al rol "ADMIN"
INSERT INTO rol_modulo (id, rol_id, modulo_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), r.id, m.id, 'ACTIVO', now(), now()
FROM rol r, modulo m
WHERE r.nombre = 'ADMIN' AND m.nombre = 'Administracion'
  AND NOT EXISTS (SELECT 1 FROM rol_modulo WHERE rol_id = r.id AND modulo_id = m.id);

INSERT INTO rol_menu (id, rol_id, menu_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), r.id, mn.id, 'ACTIVO', now(), now()
FROM rol r, menu mn
WHERE r.nombre = 'ADMIN' AND mn.nombre IN ('Administracion', 'Usuarios')
  AND NOT EXISTS (SELECT 1 FROM rol_menu WHERE rol_id = r.id AND menu_id = mn.id);

-- 6. Usuario "admin@mastergateway.local" / contrasena "Test1234"
INSERT INTO usuario (id, email, password_hash, nombre_completo, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'admin@mastergateway.local',
       '$2a$12$wnRRRI5ni4I6BYlsNbCCh.L0OCV4krIg9m2zN9hqTU5I2Cb9bd8By',
       'Administrador', 'ACTIVO', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'admin@mastergateway.local');

-- 7. Asignar el rol "ADMIN" a ese usuario
INSERT INTO usuario_rol (id, usuario_id, rol_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), u.id, r.id, 'ACTIVO', now(), now()
FROM usuario u, rol r
WHERE u.email = 'admin@mastergateway.local' AND r.nombre = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM usuario_rol WHERE usuario_id = u.id AND rol_id = r.id);

-- ============================================================================
-- USUARIO VENDEDOR — rol Vendedor, modulo "Ventas"
-- ============================================================================

-- 8. Rol "Vendedor"
INSERT INTO rol (id, nombre, descripcion, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'Vendedor', 'Rol de ventas', 'ACTIVO', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre = 'Vendedor');

-- 9. Modulo "Ventas"
INSERT INTO modulo (id, nombre, descripcion, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'Ventas', 'Modulo de ventas', 'ACTIVO', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM modulo WHERE nombre = 'Ventas');

-- 10. Menu principal "Ventas" (parent_id NULL = Menu Principal, sin url propia)
INSERT INTO menu (id, nombre, url, modulo_id, parent_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'Ventas', NULL, m.id, NULL, 'ACTIVO', now(), now()
FROM modulo m
WHERE m.nombre = 'Ventas'
  AND NOT EXISTS (SELECT 1 FROM menu WHERE nombre = 'Ventas' AND parent_id IS NULL);

-- 11. Item hoja "Ordenes" (nodo final, con url real) bajo el menu "Ventas"
INSERT INTO menu (id, nombre, url, modulo_id, parent_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'Ordenes', '/ventas/ordenes', padre.modulo_id, padre.id, 'ACTIVO', now(), now()
FROM menu padre
WHERE padre.nombre = 'Ventas' AND padre.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM menu WHERE nombre = 'Ordenes' AND url = '/ventas/ordenes');

-- 12. Asignar el modulo y sus menus al rol "Vendedor"
INSERT INTO rol_modulo (id, rol_id, modulo_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), r.id, m.id, 'ACTIVO', now(), now()
FROM rol r, modulo m
WHERE r.nombre = 'Vendedor' AND m.nombre = 'Ventas'
  AND NOT EXISTS (SELECT 1 FROM rol_modulo WHERE rol_id = r.id AND modulo_id = m.id);

INSERT INTO rol_menu (id, rol_id, menu_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), r.id, mn.id, 'ACTIVO', now(), now()
FROM rol r, menu mn
WHERE r.nombre = 'Vendedor' AND mn.nombre IN ('Ventas', 'Ordenes')
  AND NOT EXISTS (SELECT 1 FROM rol_menu WHERE rol_id = r.id AND menu_id = mn.id);

-- 13. Usuario "vendedor@mastergateway.local" / contrasena "Vendedor123"
INSERT INTO usuario (id, email, password_hash, nombre_completo, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), 'vendedor@mastergateway.local',
       '$2a$12$YmwJuLkqL7MoOidLU6eXPOY6l25GZYD27.m2P6AlRhfEvRtDODypG',
       'Usuario Vendedor', 'ACTIVO', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'vendedor@mastergateway.local');

-- 14. Asignar el rol "Vendedor" a ese usuario
INSERT INTO usuario_rol (id, usuario_id, rol_id, estado, fecha_creacion, fecha_actualizacion)
SELECT gen_random_uuid(), u.id, r.id, 'ACTIVO', now(), now()
FROM usuario u, rol r
WHERE u.email = 'vendedor@mastergateway.local' AND r.nombre = 'Vendedor'
  AND NOT EXISTS (SELECT 1 FROM usuario_rol WHERE usuario_id = u.id AND rol_id = r.id);
