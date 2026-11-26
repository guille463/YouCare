# Comprobaciones de la base de datos — YouCare

Comandos para comprobar desde la terminal que la BD local está como esperamos.
Se ejecutan desde la **raíz del repo** (excepto el arranque de la app, que se lanza desde `backend/`).

**Requisito:** Docker Desktop abierto, con *Engine running*.

> Si `psql` muestra `~` o `(END)` al final, es el paginador: pulsa `q` para salir.

---

## 1. Levantar la BD desde cero

```bash
docker compose down -v
docker compose up -d
docker compose ps
```

**Esperado:** `youcare-db` y `youcare-mailpit` en estado `running` (la BD, además, `healthy`).

⚠️ `down -v` borra todos los datos locales. Al arrancar, Flyway reconstruye la BD desde V1.

![Contenedores levantados](imgsReadme/bd-01-docker-ps.png)

---

## 2. Arrancar la app

```bash
cd backend
./mvnw spring-boot:run
```

**Esperado en el log:**
- `Successfully applied 7 migrations to schema "public", now at version v8`
- `Started BackendApplication`

(Son 7 porque no existe V5: Flyway admite huecos en la numeración.)

Si aparece `Application run failed`, el motivo está en `Message:` o en `Description:`. Con `ddl-auto=validate`, si una entidad no coincide con su tabla sale `missing column` o `wrong column type`.

![Arranque correcto](imgsReadme/bd-02-arranque.png)

---

## 3. Migraciones aplicadas por Flyway

En otra terminal, desde la raíz:

```bash
docker compose exec db psql -U youcare -d youcare -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank"
```

**Esperado:** V1, V2, V3, V4, V6, V7 y V8, todas con `success = t`.

![Historial de Flyway](imgsReadme/bd-03-flyway-history.png)

---

## 4. Extensiones

```bash
docker compose exec db psql -U youcare -d youcare -c "\dx"
```

**Esperado:** `postgis` y `btree_gist` en la lista.

![Extensiones](imgsReadme/bd-04-extensiones.png)

---

## 5. Tablas

### 5.1 Listado

```bash
docker compose exec db psql -U youcare -d youcare -c "\dt"
```

**Esperado:** `usuario`, `cliente`, `admin`, `empresa`, `empresa_documento`, `cuidador`, `cuidador_documento`, `cuidador_franja_horaria`, `persona_cuidada`, `flyway_schema_history` y `spatial_ref_sys` (esta última la crea PostGIS).

![Listado de tablas](imgsReadme/bd-05-tablas.png)

### 5.2 `usuario`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d usuario"
```

**Esperado:**
- `telefono` y `cantidad_strikes` con `not null`; `cantidad_strikes` con default `0`.
- Restricciones `uk_usuario_email`, `ck_usuario_email_minusculas`, `ck_usuario_rol` y `ck_usuario_strikes`.
- Referenciada por `fk_admin_usuario`, `fk_cliente_usuario`, `fk_empresa_usuario` y `fk_cuidador_usuario`, con `ON DELETE CASCADE`.

![Tabla usuario](imgsReadme/bd-06-usuario.png)

### 5.3 `cliente`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d cliente"
```

**Esperado:**
- `id` es la PK.
- `fecha_nacimiento` con `not null`.
- `direccion varchar(200)` y `ciudad varchar(100)`.
- FK `fk_cliente_usuario`.
- Referenciada por `fk_persona_cuidada_cliente`.

![Tabla cliente](imgsReadme/bd-07-cliente.png)

### 5.4 `admin`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d admin"
```

**Esperado:** `id` es la PK, `departamento varchar(100)` y FK `fk_admin_usuario`.

![Tabla admin](imgsReadme/bd-08-admin.png)

---

## 6. Las restricciones funcionan

Cada prueba intenta insertar un dato inválido. **El error es el resultado esperado.**

### 6.1 Email en mayúsculas → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('Ana@gmail.com', 'x', 'Ana', 'López', '600000000', 'CLIENTE')"
```

**Esperado:** `violates check constraint "ck_usuario_email_minusculas"`

![Email en mayúsculas](imgsReadme/bd-09-check-email.png)

### 6.2 Email correcto → se inserta

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('ana@gmail.com', 'x', 'Ana', 'López', '600000000', 'CLIENTE')"
```

**Esperado:** `INSERT 0 1`

![Insert correcto](imgsReadme/bd-10-insert-ok.png)

### 6.3 Email duplicado → rechazado

Repite el comando del 6.2.

**Esperado:** `violates unique constraint "uk_usuario_email"`

![Email duplicado](imgsReadme/bd-11-unique-email.png)

### 6.4 Rol inválido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('rol@gmail.com', 'x', 'Rol', 'Malo', '600000001', 'SUPERUSER')"
```

**Esperado:** `violates check constraint "ck_usuario_rol"`

![Rol inválido](imgsReadme/bd-12-check-rol.png)

### 6.5 Strikes negativos → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE usuario SET cantidad_strikes = -1 WHERE email = 'ana@gmail.com'"
```

**Esperado:** `violates check constraint "ck_usuario_strikes"`

![Strikes negativos](imgsReadme/bd-13-check-strikes.png)

### 6.6 Teléfono vacío → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, rol) VALUES ('sintel@gmail.com', 'x', 'Sin', 'Teléfono', 'CLIENTE')"
```

**Esperado:** `null value in column "telefono" ... violates not-null constraint`

![Teléfono obligatorio](imgsReadme/bd-14-not-null-telefono.png)

### 6.7 Cliente sin usuario → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cliente (id, fecha_nacimiento) VALUES (9999, '1990-01-01')"
```

**Esperado:** `violates foreign key constraint "fk_cliente_usuario"`

![FK cliente](imgsReadme/bd-15-fk-cliente.png)

### 6.8 Borrado en cascada

Crear la fila de `cliente` para Ana:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cliente (id, fecha_nacimiento) SELECT id, '1990-01-01' FROM usuario WHERE email = 'ana@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.id, u.email, c.fecha_nacimiento FROM usuario u JOIN cliente c ON c.id = u.id"
```

Borrar el usuario y comprobar que su fila de `cliente` también desaparece:

```bash
docker compose exec db psql -U youcare -d youcare -c "DELETE FROM usuario WHERE email = 'ana@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT count(*) FROM cliente"
```

**Esperado:** antes del borrado, una fila con Ana y su fecha de nacimiento; después, `count = 0`.

![Borrado en cascada](imgsReadme/bd-16-cascade.png)

---

## 7. V6 — Empresa

### 7.1 Estructura de `empresa`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d empresa"
```

**Esperado:**
- `razon_social varchar(150)`, `cif varchar(9)` y `estado_verificacion varchar(20)`, todas `not null`.
- `estado_verificacion` con default `'SIN_SUBIR'`.
- Restricciones `uk_empresa_cif`, `ck_empresa_cif` y `ck_empresa_estado_verificacion`.
- FK `fk_empresa_usuario` (`ON DELETE CASCADE`).
- Referenciada por `fk_cuidador_empresa` y `fk_empresa_documento_empresa`.

![Tabla empresa](imgsReadme/bd-17-empresa.png)

### 7.2 Estructura de `empresa_documento`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d empresa_documento"
```

**Esperado:**
- `estado` con default `'PENDIENTE'` y `fecha_subida` con default `now()`.
- `motivo_rechazo` es la única columna que admite `null`.
- Restricciones `ck_empresa_documento_tipo`, `ck_empresa_documento_estado` y `ck_empresa_documento_motivo`.
- FK `fk_empresa_documento_empresa` (`ON DELETE CASCADE`).

![Tabla empresa_documento](imgsReadme/bd-18-empresa-documento.png)

### 7.3 Empresa correcta → se inserta

Primero el usuario con rol `EMPRESA` y después su fila en `empresa`:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('empresa@gmail.com', 'x', 'Ana', 'López', '600000010', 'EMPRESA')"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO empresa (id, razon_social, cif) SELECT id, 'Cuidados Sur SL', 'B12345678' FROM usuario WHERE email = 'empresa@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email, e.razon_social, e.cif, e.estado_verificacion FROM usuario u JOIN empresa e ON e.id = u.id"
```

**Esperado:** `INSERT 0 1` dos veces, y una fila con `estado_verificacion = SIN_SUBIR` (lo pone el `DEFAULT`).

![Empresa insertada](imgsReadme/bd-19-empresa-insert.png)

### 7.4 CIF con formato inválido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE empresa SET cif = '12345678Z' WHERE cif = 'B12345678'"
```

**Esperado:** `violates check constraint "ck_empresa_cif"`

![CIF inválido](imgsReadme/bd-20-check-cif.png)

### 7.5 CIF duplicado → rechazado

Intenta crear otra empresa con el mismo CIF. Va en una sola sentencia, así que al fallar no queda ningún usuario suelto:

```bash
docker compose exec db psql -U youcare -d youcare -c "WITH u AS (INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('empresa2@gmail.com', 'x', 'Eva', 'Gil', '600000011', 'EMPRESA') RETURNING id) INSERT INTO empresa (id, razon_social, cif) SELECT id, 'Otra SL', 'B12345678' FROM u"
```

**Esperado:** `violates unique constraint "uk_empresa_cif"`

![CIF duplicado](imgsReadme/bd-21-unique-cif.png)

### 7.6 Estado de verificación inválido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE empresa SET estado_verificacion = 'INVENTADO' WHERE cif = 'B12345678'"
```

**Esperado:** `violates check constraint "ck_empresa_estado_verificacion"`

![Estado inválido](imgsReadme/bd-22-check-estado-empresa.png)

### 7.7 Documento de la empresa

Subir el CIF y comprobar que queda `PENDIENTE`:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO empresa_documento (empresa_id, tipo, clave_archivo) SELECT id, 'CIF', 'docs/cif.pdf' FROM usuario WHERE email = 'empresa@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email, d.tipo, d.estado, d.clave_archivo FROM empresa_documento d JOIN usuario u ON u.id = d.empresa_id"
```

**Esperado:** `INSERT 0 1` y una fila con `tipo = CIF` y `estado = PENDIENTE`.

Rechazarlo **sin motivo** no debe dejar:

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE empresa_documento SET estado = 'RECHAZADO' WHERE tipo = 'CIF'"
```

**Esperado:** `violates check constraint "ck_empresa_documento_motivo"`

![Documento sin motivo](imgsReadme/bd-23-empresa-documento-motivo.png)

---

## 8. V7 — Cuidador

Usa la empresa que creaste en el apartado 7.

### 8.1 Estructura de `cuidador`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d cuidador"
```

**Esperado:**
- `dni_nie varchar(9)` con `not null`; `empresa_id` y `estado_en_empresa` admiten `null`.
- `estado_verificacion` con default `'SIN_SUBIR'`.
- Restricciones `uk_cuidador_dni_nie`, `ck_cuidador_dni_nie`, `ck_cuidador_estado_verificacion`, `ck_cuidador_estado_en_empresa` y `ck_cuidador_empresa_y_estado`.
- FK `fk_cuidador_usuario` (`ON DELETE CASCADE`) y `fk_cuidador_empresa` (**sin** `ON DELETE`).
- Referenciada por `cuidador_documento` y `cuidador_franja_horaria`.

![Tabla cuidador](imgsReadme/bd-24-cuidador.png)

### 8.2 Estructura de las tablas de colección

```bash
docker compose exec db psql -U youcare -d youcare -c "\d cuidador_documento"
docker compose exec db psql -U youcare -d youcare -c "\d cuidador_franja_horaria"
```

**Esperado:**
- `cuidador_documento`: mismas columnas y restricciones que `empresa_documento` (con `ck_cuidador_documento_*`), FK `ON DELETE CASCADE`.
- `cuidador_franja_horaria`: `dia_semana varchar(10)`, `hora_inicio` y `hora_fin` de tipo `time`, y restricciones `ck_cuidador_franja_dia` y `ck_cuidador_franja_horas`.

![Tablas de colección](imgsReadme/bd-25-cuidador-colecciones.png)

### 8.3 Cuidador autónomo → se inserta

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('luis@gmail.com', 'x', 'Luis', 'Pérez', '600000020', 'CUIDADOR')"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cuidador (id, dni_nie) SELECT id, '12345678Z' FROM usuario WHERE email = 'luis@gmail.com'"
```

**Esperado:** `INSERT 0 1` dos veces. Sin empresa ni estado en empresa.

### 8.4 Cuidador dentro de una empresa → se inserta

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('marta@gmail.com', 'x', 'Marta', 'Ruiz', '600000021', 'CUIDADOR')"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cuidador (id, dni_nie, empresa_id, estado_en_empresa) SELECT u.id, '87654321X', (SELECT id FROM empresa LIMIT 1), 'PENDIENTE' FROM usuario u WHERE u.email = 'marta@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email, c.dni_nie, c.estado_verificacion, c.empresa_id IS NOT NULL AS tiene_empresa, c.estado_en_empresa FROM cuidador c JOIN usuario u ON u.id = c.id ORDER BY u.email"
```

**Esperado:** dos filas, ambas con `estado_verificacion = SIN_SUBIR`:
- `luis@gmail.com`: `tiene_empresa = f` y `estado_en_empresa` vacío.
- `marta@gmail.com`: `tiene_empresa = t` y `estado_en_empresa = PENDIENTE`.

![Cuidadores insertados](imgsReadme/bd-26-cuidadores-insert.png)

### 8.5 Estado en empresa sin empresa → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE cuidador SET estado_en_empresa = 'ACTIVO' WHERE dni_nie = '12345678Z'"
```

**Esperado:** `violates check constraint "ck_cuidador_empresa_y_estado"`

### 8.6 Empresa sin estado en empresa → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE cuidador SET estado_en_empresa = NULL WHERE dni_nie = '87654321X'"
```

**Esperado:** `violates check constraint "ck_cuidador_empresa_y_estado"` (la misma que en 8.5: obliga a que o estén los dos datos o ninguno).

![Empresa y estado](imgsReadme/bd-27-check-empresa-y-estado.png)

### 8.7 DNI con formato inválido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE cuidador SET dni_nie = '1234' WHERE dni_nie = '12345678Z'"
```

**Esperado:** `violates check constraint "ck_cuidador_dni_nie"`

![DNI inválido](imgsReadme/bd-28-check-dni.png)

### 8.8 DNI duplicado → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE cuidador SET dni_nie = '87654321X' WHERE dni_nie = '12345678Z'"
```

**Esperado:** `violates unique constraint "uk_cuidador_dni_nie"`

![DNI duplicado](imgsReadme/bd-29-unique-dni.png)

### 8.9 Estado de verificación inválido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE cuidador SET estado_verificacion = 'INVENTADO' WHERE dni_nie = '12345678Z'"
```

**Esperado:** `violates check constraint "ck_cuidador_estado_verificacion"`

### 8.10 Documentos del cuidador

Subir un DNI y comprobar que queda `PENDIENTE`:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cuidador_documento (cuidador_id, tipo, clave_archivo) SELECT id, 'DNI_NIE', 'docs/dni.pdf' FROM usuario WHERE email = 'luis@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email, d.tipo, d.estado, d.clave_archivo FROM cuidador_documento d JOIN usuario u ON u.id = d.cuidador_id"
```

**Esperado:** `INSERT 0 1` y una fila con `estado = PENDIENTE`.

Rechazarlo sin motivo → error:

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE cuidador_documento SET estado = 'RECHAZADO' WHERE tipo = 'DNI_NIE'"
```

**Esperado:** `violates check constraint "ck_cuidador_documento_motivo"`

Rechazarlo con motivo → correcto:

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE cuidador_documento SET estado = 'RECHAZADO', motivo_rechazo = 'Foto ilegible' WHERE tipo = 'DNI_NIE'"
```

**Esperado:** `UPDATE 1`

![Motivo de rechazo](imgsReadme/bd-30-documento-motivo.png)

### 8.11 Franjas horarias

Franja válida:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cuidador_franja_horaria (cuidador_id, dia_semana, hora_inicio, hora_fin) SELECT id, 'MONDAY', '09:00', '14:00' FROM usuario WHERE email = 'luis@gmail.com'"
```

**Esperado:** `INSERT 0 1`

Hora de fin anterior a la de inicio → rechazado:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cuidador_franja_horaria (cuidador_id, dia_semana, hora_inicio, hora_fin) SELECT id, 'TUESDAY', '14:00', '09:00' FROM usuario WHERE email = 'luis@gmail.com'"
```

**Esperado:** `violates check constraint "ck_cuidador_franja_horas"`

Día inválido → rechazado:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cuidador_franja_horaria (cuidador_id, dia_semana, hora_inicio, hora_fin) SELECT id, 'LUNES', '09:00', '14:00' FROM usuario WHERE email = 'luis@gmail.com'"
```

**Esperado:** `violates check constraint "ck_cuidador_franja_dia"` (el día se guarda con el nombre de `DayOfWeek` en inglés: `MONDAY`, `TUESDAY`…).

Comprobar lo que quedó:

```bash
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email, f.dia_semana, f.hora_inicio, f.hora_fin FROM cuidador_franja_horaria f JOIN usuario u ON u.id = f.cuidador_id"
```

**Esperado:** una sola fila: Luis, `MONDAY`, `09:00:00`, `14:00:00`.

![Franjas](imgsReadme/bd-31-franjas.png)

### 8.12 No se puede borrar una empresa con cuidadores

```bash
docker compose exec db psql -U youcare -d youcare -c "DELETE FROM usuario WHERE email = 'empresa@gmail.com'"
```

**Esperado:** `violates foreign key constraint "fk_cuidador_empresa" on table "cuidador"`

Es intencionado: la FK a `empresa` no lleva `ON DELETE`, así que hay que desvincular a los cuidadores antes de borrar la empresa.

![FK empresa](imgsReadme/bd-32-fk-cuidador-empresa.png)

### 8.13 Borrado en cascada del cuidador

Luis tiene un documento y una franja. Al borrar su usuario deben desaparecer con él:

```bash
docker compose exec db psql -U youcare -d youcare -c "DELETE FROM usuario WHERE email = 'luis@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT (SELECT count(*) FROM cuidador) AS cuidadores, (SELECT count(*) FROM cuidador_documento) AS documentos, (SELECT count(*) FROM cuidador_franja_horaria) AS franjas"
```

**Esperado:** `DELETE 1` y `cuidadores = 1` (queda Marta), `documentos = 0`, `franjas = 0`.

![Cascada cuidador](imgsReadme/bd-33-cascade-cuidador.png)

---

## 9. V8 — Persona cuidada

### 9.1 Estructura de `persona_cuidada`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d persona_cuidada"
```

**Esperado:**
- `id` generado por identidad (`generated by default as identity`).
- `cliente_id`, `nombre`, `apellidos`, `fecha_nacimiento`, `direccion` y `ciudad` con `not null`.
- `necesidades varchar(1000)` es la única que admite `null`.
- Índice `idx_persona_cuidada_cliente` y FK `fk_persona_cuidada_cliente` hacia `cliente(id)` con `ON DELETE CASCADE`.

![Tabla persona_cuidada](imgsReadme/bd-34-persona-cuidada.png)

### 9.2 Persona cuidada de un cliente → se inserta

Hace falta un cliente (usuario con rol `CLIENTE` + su fila en `cliente`):

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('pedro@gmail.com', 'x', 'Pedro', 'Gil', '600000030', 'CLIENTE')"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cliente (id, fecha_nacimiento) SELECT id, '1980-05-10' FROM usuario WHERE email = 'pedro@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO persona_cuidada (cliente_id, nombre, apellidos, fecha_nacimiento, direccion, ciudad) SELECT id, 'Carmen', 'Gil', '1940-03-02', 'Calle Mayor 1', 'Granada' FROM usuario WHERE email = 'pedro@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email AS cliente, p.nombre, p.apellidos, p.ciudad, p.necesidades FROM persona_cuidada p JOIN usuario u ON u.id = p.cliente_id"
```

**Esperado:** `INSERT 0 1` tres veces y una fila: `pedro@gmail.com`, Carmen Gil, Granada, con `necesidades` vacío.

![Persona cuidada insertada](imgsReadme/bd-35-persona-insert.png)

### 9.3 Persona cuidada sin cliente → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO persona_cuidada (cliente_id, nombre, apellidos, fecha_nacimiento, direccion, ciudad) VALUES (999999, 'X', 'Y', '1940-01-01', 'Calle 1', 'Granada')"
```

**Esperado:** `violates foreign key constraint "fk_persona_cuidada_cliente"`

![FK persona cuidada](imgsReadme/bd-36-fk-persona-cuidada.png)

### 9.4 Dirección obligatoria → rechazada si falta

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO persona_cuidada (cliente_id, nombre, apellidos, fecha_nacimiento, ciudad) SELECT id, 'Sin', 'Direccion', '1940-01-01', 'Granada' FROM usuario WHERE email = 'pedro@gmail.com'"
```

**Esperado:** `null value in column "direccion" ... violates not-null constraint`

![Dirección obligatoria](imgsReadme/bd-37-not-null-direccion.png)

### 9.5 Borrado en cascada

Al borrar el cliente desaparece su persona cuidada (cascada de dos niveles: `usuario` → `cliente` → `persona_cuidada`):

```bash
docker compose exec db psql -U youcare -d youcare -c "DELETE FROM usuario WHERE email = 'pedro@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT count(*) FROM persona_cuidada"
```

**Esperado:** `count = 0`.

![Cascada persona cuidada](imgsReadme/bd-38-cascade-persona.png)

---

## 10. Datos para V12–V15

Un cliente, un cuidador autónomo y una empresa, con datos propios para no depender de los apartados anteriores:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO usuario (email, hash_contrasenia, nombre, apellidos, telefono, rol) VALUES ('rosa@gmail.com', 'x', 'Rosa', 'López', '600000030', 'CLIENTE'), ('pablo@gmail.com', 'x', 'Pablo', 'Gil', '600000031', 'CUIDADOR'), ('info@cuidagranada.com', 'x', 'Elena', 'Mora', '958000030', 'EMPRESA')"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cliente (id, fecha_nacimiento) SELECT id, '1975-03-10' FROM usuario WHERE email = 'rosa@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO cuidador (id, dni_nie) SELECT id, '11111111H' FROM usuario WHERE email = 'pablo@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO empresa (id, razon_social, cif) SELECT id, 'Cuida Granada SL', 'B18000001' FROM usuario WHERE email = 'info@cuidagranada.com'"
```

**Esperado:** `INSERT 0 3` y después `INSERT 0 1` tres veces.

![Datos de prueba](imgsReadme/bd-39-datos-v12-v15.png)

---

## 11. V12 — Chat

### 11.1 Estructura de `chat`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d chat"
```

**Esperado:**
- `cliente_id` con `not null`. `cuidador_id`, `empresa_id` y `fecha_ultimo_mensaje` admiten `null`.
- `fecha_creacion` con default `now()`.
- Restricciones `ck_chat_un_proveedor`, `uk_chat_cliente_cuidador` y `uk_chat_cliente_empresa`.
- Tres FK con `ON DELETE CASCADE`: `fk_chat_cliente`, `fk_chat_cuidador` y `fk_chat_empresa`.
- Referenciada por `mensaje`.

![Tabla chat](imgsReadme/bd-40-chat.png)

### 11.2 Chat con cuidador y chat con empresa → se insertan

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO chat (cliente_id, cuidador_id) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), (SELECT id FROM usuario WHERE email = 'pablo@gmail.com'))"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO chat (cliente_id, empresa_id) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), (SELECT id FROM usuario WHERE email = 'info@cuidagranada.com'))"
docker compose exec db psql -U youcare -d youcare -c "SELECT c.id, u.email AS cliente, c.cuidador_id IS NOT NULL AS con_cuidador, c.empresa_id IS NOT NULL AS con_empresa FROM chat c JOIN usuario u ON u.id = c.cliente_id ORDER BY c.id"
```

**Esperado:** dos filas de `rosa@gmail.com`, una con `con_cuidador = t` y otra con `con_empresa = t`. Nunca las dos a la vez.

![Chats insertados](imgsReadme/bd-41-chat-insert.png)

### 11.3 Chat con los dos proveedores → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO chat (cliente_id, cuidador_id, empresa_id) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), (SELECT id FROM usuario WHERE email = 'pablo@gmail.com'), (SELECT id FROM usuario WHERE email = 'info@cuidagranada.com'))"
```

**Esperado:** `violates check constraint "ck_chat_un_proveedor"`

![Dos proveedores](imgsReadme/bd-42-check-chat-dos-proveedores.png)

### 11.4 Chat sin proveedor → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO chat (cliente_id) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'))"
```

**Esperado:** `violates check constraint "ck_chat_un_proveedor"`

![Sin proveedor](imgsReadme/bd-43-check-chat-sin-proveedor.png)

### 11.5 Chat repetido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO chat (cliente_id, cuidador_id) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), (SELECT id FROM usuario WHERE email = 'pablo@gmail.com'))"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO chat (cliente_id, empresa_id) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), (SELECT id FROM usuario WHERE email = 'info@cuidagranada.com'))"
```

**Esperado:** `uk_chat_cliente_cuidador` en el primero y `uk_chat_cliente_empresa` en el segundo. Un solo chat por pareja.

![Chat repetido](imgsReadme/bd-44-unique-chat.png)

---

## 12. V13 — Mensaje

### 12.1 Estructura de `mensaje`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d mensaje"
```

**Esperado:**
- `contenido varchar(2000)` con `not null`. `fecha_lectura` admite `null`.
- Restricciones `ck_mensaje_contenido` y `ck_mensaje_fecha_lectura`.
- FK `fk_mensaje_chat` y `fk_mensaje_emisor`, las dos con `ON DELETE CASCADE`.
- Índices `idx_mensaje_chat_fecha` (`chat_id, fecha_envio`) e `idx_mensaje_emisor`.
- Sin columna de receptor: es el otro participante del chat.

![Tabla mensaje](imgsReadme/bd-45-mensaje.png)

### 12.2 Conversación entre cliente y cuidador → se inserta

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO mensaje (chat_id, emisor_id, contenido) VALUES ((SELECT id FROM chat WHERE cuidador_id = (SELECT id FROM usuario WHERE email = 'pablo@gmail.com')), (SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), 'Hola, ¿tienes disponibilidad el lunes?'), ((SELECT id FROM chat WHERE cuidador_id = (SELECT id FROM usuario WHERE email = 'pablo@gmail.com')), (SELECT id FROM usuario WHERE email = 'pablo@gmail.com'), 'Sí, por la mañana')"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email AS emisor, m.contenido, m.fecha_lectura FROM mensaje m JOIN usuario u ON u.id = m.emisor_id ORDER BY m.id"
```

**Esperado:** `INSERT 0 2`. Dos filas, una de Rosa y otra de Pablo, con `fecha_lectura` vacía porque todavía no se han leído.

![Mensajes insertados](imgsReadme/bd-46-mensaje-insert.png)

### 12.3 Mensaje marcado como leído

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE mensaje SET fecha_lectura = now() WHERE emisor_id = (SELECT id FROM usuario WHERE email = 'pablo@gmail.com')"
docker compose exec db psql -U youcare -d youcare -c "SELECT u.email AS emisor, m.fecha_lectura IS NOT NULL AS leido FROM mensaje m JOIN usuario u ON u.id = m.emisor_id ORDER BY m.id"
```

**Esperado:** `UPDATE 1`. El mensaje de Pablo aparece con `leido = t` y el de Rosa con `leido = f`.

![Mensaje leído](imgsReadme/bd-47-mensaje-leido.png)

### 12.4 Mensaje vacío → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO mensaje (chat_id, emisor_id, contenido) VALUES ((SELECT id FROM chat WHERE cuidador_id = (SELECT id FROM usuario WHERE email = 'pablo@gmail.com')), (SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), '   ')"
```

**Esperado:** `violates check constraint "ck_mensaje_contenido"`

![Mensaje vacío](imgsReadme/bd-48-check-mensaje-contenido.png)

### 12.5 Leído antes de enviarse → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO mensaje (chat_id, emisor_id, contenido, fecha_envio, fecha_lectura) VALUES ((SELECT id FROM chat WHERE cuidador_id = (SELECT id FROM usuario WHERE email = 'pablo@gmail.com')), (SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), 'x', now(), now() - interval '1 hour')"
```

**Esperado:** `violates check constraint "ck_mensaje_fecha_lectura"`

![Fecha de lectura](imgsReadme/bd-49-check-mensaje-fecha-lectura.png)

### 12.6 Más de 2000 caracteres → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO mensaje (chat_id, emisor_id, contenido) VALUES ((SELECT id FROM chat WHERE cuidador_id = (SELECT id FROM usuario WHERE email = 'pablo@gmail.com')), (SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('x', 2001))"
```

**Esperado:** `value too long for type character varying(2000)`

![Mensaje largo](imgsReadme/bd-50-mensaje-largo.png)

### 12.7 Mensaje en un chat que no existe → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO mensaje (chat_id, emisor_id, contenido) VALUES (999999, (SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), 'Hola')"
```

**Esperado:** `violates foreign key constraint "fk_mensaje_chat"`

![FK mensaje](imgsReadme/bd-51-fk-mensaje-chat.png)

---

## 13. V14 — Refresh token

### 13.1 Estructura de `refresh_token`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d refresh_token"
```

**Esperado:**
- `hash_token varchar(64)` con `not null`. `fecha_revocacion` admite `null`.
- Restricciones `uk_refresh_token_hash`, `ck_refresh_token_hash` y `ck_refresh_token_fechas`.
- FK `fk_refresh_token_usuario` con `ON DELETE CASCADE` e índice `idx_refresh_token_usuario`.
- No hay columna con el token: solo su hash SHA-256.

![Tabla refresh_token](imgsReadme/bd-52-refresh-token.png)

### 13.2 Token válido → se inserta

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO refresh_token (usuario_id, hash_token, fecha_expiracion) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('a', 64), now() + interval '7 days')"
docker compose exec db psql -U youcare -d youcare -c "SELECT fecha_expiracion - fecha_creacion AS validez, fecha_revocacion IS NULL AND fecha_expiracion > now() AS activo FROM refresh_token"
```

**Esperado:** `validez = 7 days` y `activo = t`.

![Refresh token insertado](imgsReadme/bd-53-refresh-insert.png)

### 13.3 Hash repetido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO refresh_token (usuario_id, hash_token, fecha_expiracion) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('a', 64), now() + interval '7 days')"
```

**Esperado:** `violates unique constraint "uk_refresh_token_hash"`

![Hash repetido](imgsReadme/bd-54-unique-refresh-hash.png)

### 13.4 Hash con formato inválido → rechazado

En mayúsculas y con 63 caracteres:

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO refresh_token (usuario_id, hash_token, fecha_expiracion) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('A', 64), now() + interval '7 days')"
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO refresh_token (usuario_id, hash_token, fecha_expiracion) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('b', 63), now() + interval '7 days')"
```

**Esperado:** `violates check constraint "ck_refresh_token_hash"` en los dos.

![Hash inválido](imgsReadme/bd-55-check-refresh-hash.png)

### 13.5 Caduca antes de crearse → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO refresh_token (usuario_id, hash_token, fecha_expiracion) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('b', 64), now() - interval '1 hour')"
```

**Esperado:** `violates check constraint "ck_refresh_token_fechas"`

![Fechas inválidas](imgsReadme/bd-56-check-refresh-fechas.png)

### 13.6 Revocar el token (logout)

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE refresh_token SET fecha_revocacion = now() WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'rosa@gmail.com')"
docker compose exec db psql -U youcare -d youcare -c "SELECT fecha_revocacion IS NULL AND fecha_expiracion > now() AS activo FROM refresh_token"
```

**Esperado:** `UPDATE 1` y `activo = f`.

![Token revocado](imgsReadme/bd-57-refresh-revocado.png)

---

## 14. V15 — Email token

### 14.1 Estructura de `email_token`

```bash
docker compose exec db psql -U youcare -d youcare -c "\d email_token"
```

**Esperado:**
- `hash_token varchar(64)` y `tipo varchar(20)`, los dos con `not null`. `fecha_uso` admite `null`.
- Restricciones `uk_email_token_hash`, `ck_email_token_hash`, `ck_email_token_tipo` y `ck_email_token_fechas`.
- FK `fk_email_token_usuario` con `ON DELETE CASCADE` e índice `idx_email_token_usuario_tipo`.

![Tabla email_token](imgsReadme/bd-58-email-token.png)

### 14.2 Token de verificación y de recuperación → se insertan

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO email_token (usuario_id, hash_token, tipo, fecha_expiracion) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('c', 64), 'VERIFICACION', now() + interval '24 hours'), ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('d', 64), 'RECUPERACION', now() + interval '1 hour')"
docker compose exec db psql -U youcare -d youcare -c "SELECT tipo, fecha_expiracion - fecha_creacion AS validez, fecha_uso IS NULL AS sin_usar FROM email_token ORDER BY id"
```

**Esperado:** `VERIFICACION` con `1 day` y `RECUPERACION` con `01:00:00`, los dos con `sin_usar = t`.

![Email tokens insertados](imgsReadme/bd-59-email-token-insert.png)

### 14.3 Tipo inválido → rechazado

```bash
docker compose exec db psql -U youcare -d youcare -c "INSERT INTO email_token (usuario_id, hash_token, tipo, fecha_expiracion) VALUES ((SELECT id FROM usuario WHERE email = 'rosa@gmail.com'), repeat('e', 64), 'OTRO', now() + interval '1 hour')"
```

**Esperado:** `violates check constraint "ck_email_token_tipo"`

![Tipo inválido](imgsReadme/bd-60-check-email-token-tipo.png)

### 14.4 Usar el token de verificación

```bash
docker compose exec db psql -U youcare -d youcare -c "UPDATE email_token SET fecha_uso = now() WHERE tipo = 'VERIFICACION'"
docker compose exec db psql -U youcare -d youcare -c "SELECT tipo, fecha_uso IS NULL AS sin_usar FROM email_token ORDER BY id"
```

**Esperado:** `UPDATE 1`. `VERIFICACION` aparece con `sin_usar = f` y `RECUPERACION` sigue con `t`. Que no se pueda usar dos veces lo controla `EmailToken.marcarUsado()` en Java: lo comprueban los tests del apartado 15.2.

![Token usado](imgsReadme/bd-61-email-token-usado.png)

### 14.5 Borrado en cascada

Al borrar el cliente desaparecen sus chats, los mensajes de esos chats y sus tokens:

```bash
docker compose exec db psql -U youcare -d youcare -c "DELETE FROM usuario WHERE email = 'rosa@gmail.com'"
docker compose exec db psql -U youcare -d youcare -c "SELECT (SELECT count(*) FROM chat) AS chats, (SELECT count(*) FROM mensaje) AS mensajes, (SELECT count(*) FROM refresh_token) AS refresh, (SELECT count(*) FROM email_token) AS email"
docker compose exec db psql -U youcare -d youcare -c "DELETE FROM usuario WHERE email IN ('pablo@gmail.com', 'info@cuidagranada.com')"
```

**Esperado:** `DELETE 1`, después `0 | 0 | 0 | 0` y por último `DELETE 2`.

![Cascada V12–V15](imgsReadme/bd-62-cascade-chat-tokens.png)

---

## 15. Comprobación rápida y tests

### 15.1 Script con todas las restricciones de V12–V15

Hace en una sola ejecución lo mismo que los apartados 11–14, con ids 9001–9003, y se limpia al final. **Desde `backend/`:**

```bash
docker compose exec -T db psql -U youcare -d youcare < scripts/pruebas/prueba_chat_tokens.sql
```

**Esperado:** cada sentencia da `INSERT 0 1` o un `ERROR` con su restricción, en el orden de los comentarios del archivo. La última fila es `0 | 0 | 0 | 0`.

![Script de pruebas](imgsReadme/bd-63-script-chat-tokens.png)

### 15.2 Tests unitarios

**Desde `backend/`, con la BD levantada** (`BackendApplicationTests` arranca la app contra ella):

```bash
./mvnw test
```

**Esperado:** `Tests run: 98, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`.

| Clase | Tests |
|---|---|
| `CuidadorTest` | 23 |
| `EmpresaTest` | 8 |
| `DocumentoTest` | 6 |
| `FranjaHorariaTest` | 7 |
| `ChatTest` | 16 |
| `MensajeTest` | 15 |
| `RefreshTokenTest` | 11 |
| `EmailTokenTest` | 11 |
| `BackendApplicationTests` | 1 |

Para ejecutar solo los de esta rama:

```bash
./mvnw test -Dtest='ChatTest,MensajeTest,RefreshTokenTest,EmailTokenTest'
```

**Esperado:** `Tests run: 53`.

![Tests unitarios](imgsReadme/bd-64-tests.png)

## 16. Limpiar los datos de prueba

```bash
docker compose exec db psql -U youcare -d youcare -c "DELETE FROM usuario"
```

Por el `ON DELETE CASCADE`, también se vacían `cliente`, `admin`, `empresa`, `cuidador`, `persona_cuidada` y las tablas de documentos y franjas. Funciona aunque haya cuidadores dentro de una empresa, porque la comprobación de `fk_cuidador_empresa` se hace al terminar la sentencia, cuando los cuidadores ya se han borrado.

---

## 17. Consola interactiva (opcional)

```bash
docker compose exec db psql -U youcare -d youcare
```

| Comando | Qué hace |
|---|---|
| `\dt` | Lista las tablas |
| `\d tabla` | Muestra la estructura de una tabla |
| `\dx` | Lista las extensiones |
| `\q` | Sale |
