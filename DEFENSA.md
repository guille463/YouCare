# YouCare — Guía para la defensa

Documento vivo. Cada decisión técnica se explica con el mismo esquema:

- **Qué es:** la herramienta o técnica.
- **Para qué la usamos:** su función en YouCare.
- **Por qué así:** el motivo de la decisión.
- **Alternativas descartadas:** qué más se valoró y por qué no se eligió.
- **Pregunta probable del tribunal:** con su respuesta.

---

## Índice

1. [Arquitectura general](#1-arquitectura-general)
2. [Organización del trabajo](#2-organización-del-trabajo)
3. [Entorno de desarrollo](#3-entorno-de-desarrollo)
4. [Base de datos y migraciones](#4-base-de-datos-y-migraciones)
5. [Modelo de dominio: la entidad `Usuario`](#5-modelo-de-dominio-la-entidad-usuario)
6. [Seguridad y RGPD aplicados al diseño](#6-seguridad-y-rgpd-aplicados-al-diseño)
7. [Problemas encontrados y cómo se resolvieron](#7-problemas-encontrados-y-cómo-se-resolvieron)

---

## 1. Arquitectura general

### Cliente-servidor con API REST

- **Qué es:** un único backend expone una API REST en JSON, y la consumen tres clientes: la app móvil, el panel de administración y la web corporativa.
- **Para qué:** toda la lógica de negocio y los datos están en un solo sitio.
- **Por qué así:** si mañana hay que añadir otro cliente (una app de escritorio, una integración con una empresa), solo tiene que consumir la misma API. Además, las reglas de negocio no se duplican en cada cliente.
- **Pregunta probable:** *¿Por qué no hacer la app conectando directamente a la base de datos?*
  Porque las credenciales de la BD quedarían dentro de la app y cualquiera podría extraerlas. La API es la única que habla con la BD, y es ella la que comprueba la identidad y los permisos de cada petición.

### Arquitectura en capas en el backend

```
Controller  →  Service  →  Repository  →  Base de datos
(HTTP/DTO)     (negocio)    (acceso a datos)
```

- **Para qué:** cada capa tiene una sola responsabilidad.
- **Por qué así:** se puede cambiar una capa sin tocar las demás (por ejemplo, el pago simulado por Stripe), y cada `Service` se puede probar con JUnit y Mockito sin levantar ni la BD ni el servidor.
- **DTOs:** las entidades JPA nunca salen por la API. Se transforman antes en DTOs, para no exponer campos internos como `hashContrasenia` y para que cambiar la BD no rompa la app.

### Stack tecnológico

| Capa | Tecnología | Por qué |
|---|---|---|
| Backend | Java 25 + Spring Boot 4 | Es el lenguaje y el framework que mejor domina el equipo (DAM). Versión con soporte vigente |
| Base de datos | PostgreSQL + PostGIS | Dominio relacional y transaccional (reservas, pagos), y búsquedas por distancia |
| App móvil | React Native + Expo | Un solo código para Android, iOS y web, aprovechando que el equipo sabe JavaScript |
| Panel admin | React + Vite | Reutiliza la plantilla de un proyecto anterior |
| Archivos | Cloudflare R2 | Capa gratuita, bucket privado y URLs firmadas para el DNI y los certificados |
| Pagos | Simulación con la interfaz `PaymentGateway` | Se puede cambiar a Stripe Connect sin tocar la lógica de negocio |

- **Pregunta probable:** *¿Por qué no MongoDB?*
  Las reservas, los pagos y las valoraciones están muy relacionados entre sí, y necesitan integridad referencial y transacciones (no se puede cobrar una reserva que no existe). Eso es justo lo que da una base de datos relacional.

---

## 2. Organización del trabajo

### Monorepo

- **Qué es:** un solo repositorio con `backend/`, `mobile/`, `admin/` y `web/`.
- **Por qué:** un cambio en la API y su adaptación en la app pueden ir en la misma PR. Además, hay un único README y un único sitio para las incidencias.

### GitFlow

| Rama | Uso |
|---|---|
| `main` | Lo que está en producción. Cada merge despliega |
| `develop` | Integración de lo terminado |
| `feature/<area>-<descripcion>` | Una funcionalidad |
| `release/v<versión>` | Preparación de una entrega |
| `hotfix/*` | Correcciones urgentes en producción |

- **Por qué:** `main` siempre funciona. Lo que está a medias no rompe el trabajo de los compañeros, y cada cambio pasa por una revisión en la PR.

### Conventional Commits

- **Formato:** `tipo(ámbito): descripción`, por ejemplo `feat(backend): añadir migraciones de extensiones y usuario`.
- **Tipos usados:** `feat` (funcionalidad nueva), `fix` (corrección), `refactor` (reestructurar sin cambiar el comportamiento), `chore` (configuración y entorno), `docs`, `test`.
- **Por qué:** el historial se lee de un vistazo y se puede generar un changelog automáticamente.

---

## 3. Entorno de desarrollo

### Docker Compose

- **Qué es:** un archivo (`docker-compose.yml`) que define los servicios que necesita la app, y que se levantan con un solo comando: `docker compose up -d`.
- **Para qué:** sirve para levantar PostgreSQL + PostGIS y Mailpit en local.
- **Por qué así:** los tres miembros del equipo tienen exactamente la misma versión de la BD y la misma configuración, sin instalar nada a mano. Se acaba el "en mi ordenador funciona".
- **Volumen:** los datos se conservan aunque se pare el contenedor. `docker compose down -v` los borra.
- **Healthcheck:** el contenedor indica cuándo PostgreSQL ya acepta conexiones.
- **Mailpit:** servidor de correo falso. Los emails de verificación se ven en `http://localhost:8025` en lugar de enviarse de verdad.
- **Pregunta probable:** *¿Por qué las credenciales de la BD están en el repositorio?*
  Porque solo sirven para un contenedor local al que no se puede acceder desde fuera. Las de producción van en variables de entorno del servidor y nunca se suben.

### Perfiles de Spring (`dev` / `prod`)

- **Qué es:** archivos de configuración distintos según el entorno: `application.properties` (común), `application-dev.properties` y `application-prod.properties`.
- **Para qué:** el mismo código se conecta a la BD local en desarrollo y a Neon en producción.
- **Por qué así:** se cambia de entorno sin tocar código. En producción, todos los valores se leen de variables de entorno (`${DB_URL}`).

### `.gitignore`

- **Regla:** se ignoran los secretos (`.env`) y lo que se genera solo (`target/`, logs). Se sube la configuración que necesita todo el equipo.
- **Por qué:** subir una credencial a GitHub la expone para siempre, aunque después se borre, porque queda en el historial.

---

## 4. Base de datos y migraciones

### Flyway

- **Qué es:** una herramienta de versionado del esquema de la BD. Cada cambio es un script SQL numerado (`V1__...sql`, `V2__...sql`), y Flyway los ejecuta en orden al arrancar la app.
- **Para qué:** crear y modificar las tablas de forma controlada y reproducible.
- **Por qué así:**
  - El esquema queda versionado en Git, igual que el código.
  - Cualquier BD (la local de cada persona o la de producción) llega exactamente al mismo estado.
  - Flyway guarda en la tabla `flyway_schema_history` qué scripts se han aplicado, así que ninguno se ejecuta dos veces.
- **Regla clave:** una migración ya aplicada no se edita nunca. Flyway guarda su checksum y falla si cambia. Cualquier corrección va en una migración nueva.
- **Alternativa descartada:** `ddl-auto=update`, que deja a Hibernate crear o modificar las tablas solo. No es predecible, no borra columnas, no deja historial y es peligroso en producción.

### `ddl-auto=validate`

- **Qué es:** Hibernate no toca la BD, solo comprueba al arrancar que cada entidad coincide con su tabla.
- **Para qué:** detectar enseguida cualquier diferencia entre el código Java y el SQL.
- **Por qué así:** si alguien añade un atributo y olvida la migración, la app no arranca. El error sale en desarrollo y no en producción.

### Migraciones actuales

| Migración | Contenido | Por qué |
|---|---|---|
| `V1__extensions.sql` | `postgis` y `btree_gist` | Van primero: las tablas posteriores usan sus tipos y operadores |
| `V2__crear_usuario.sql` | Tabla `usuario` | Datos comunes a todos los tipos de usuario |

### Extensiones de PostgreSQL

- **PostGIS:** añade el tipo `geography(Point)` y funciones como `ST_DWithin`, para buscar cuidadores en un radio de X km. Con un índice GiST, la búsqueda es rápida aunque haya miles de cuidadores.
- **btree_gist:** permite la restricción `EXCLUDE`, con la que la BD impide que un cuidador tenga dos reservas que se solapan en el tiempo. La regla la garantiza la propia BD, aunque haya dos peticiones simultáneas.
- **`IF NOT EXISTS`:** el script no falla si la extensión ya estaba activada, como pasa en la imagen de Docker y puede pasar en Neon.

### Decisiones en el SQL de `usuario`

| Elemento | Por qué |
|---|---|
| `BIGINT GENERATED BY DEFAULT AS IDENTITY` | Es la forma estándar en SQL de generar el id (sustituye a `SERIAL`) y la que espera `GenerationType.IDENTITY` |
| `VARCHAR(n)` con el mismo `n` que `@Column(length)` | La entidad y la tabla coinciden y el tamaño queda documentado |
| `TIMESTAMPTZ` | Guarda el instante con zona horaria y corresponde a `Instant` de Java. Evita errores con el cambio de hora |
| `DEFAULT` en `activo`, `email_verificado` y `fecha_registro` | Las filas insertadas a mano (datos de prueba) quedan igual que las creadas desde Java |
| `CONSTRAINT uk_usuario_email UNIQUE` | No puede haber dos cuentas con el mismo email. El nombre de la restricción permite devolver el mensaje "El email ya está registrado" |
| `CONSTRAINT ck_usuario_rol CHECK (...)` | El enum solo protege en Java. El `CHECK` también impide valores inválidos que entren por SQL |

- **Pregunta probable:** *¿Por qué validar en la BD si ya se valida en Java?*
  Por defensa en profundidad. La BD es la última barrera, y protege los datos aunque alguien los toque desde fuera de la aplicación (scripts, otra herramienta o un error del programador).

---

## 5. Modelo de dominio: la entidad `Usuario`

### Qué es una entidad JPA

| Java | Base de datos |
|---|---|
| Clase `@Entity` | Tabla |
| Atributo | Columna |
| Objeto | Fila |

Las anotaciones (`@Column`, `@Id`…) indican cómo se traduce cada atributo a una columna. JPA/Hibernate hace la conversión y evita escribir SQL para las operaciones básicas.

### Herencia: `Usuario` y sus subclases

```
              Usuario (abstracta)
     ┌────────┬───────┴──────┬──────────┐
   Admin   Cliente       Cuidador     Empresa
```

- **Por qué herencia:** los cuatro tipos comparten el login, el nombre, el teléfono, el estado de la cuenta… Escribirlos una sola vez evita duplicar código.
- **Por qué `abstract`:** nunca existe un "usuario a secas"; siempre es de uno de los cuatro tipos.

### Estrategia `JOINED`

- **Qué es:** la tabla `usuario` guarda los datos comunes y cada subclase tiene su propia tabla con sus columnas propias, que comparte el `id` con `usuario`. Hibernate las une con un JOIN al consultar.
- **Por qué así:**
  - Las tablas están normalizadas, sin columnas vacías.
  - Las columnas propias de cada tipo (tarifa del cuidador, CIF de la empresa) pueden ser `NOT NULL`, así que la BD garantiza que un cuidador siempre tiene tarifa.
- **Alternativa descartada:** `SINGLE_TABLE`, con todo en una sola tabla. Es más rápida al no tener JOINs, pero obliga a que las columnas de cuidador y empresa admitan `NULL` en las filas de cliente y admin, y se pierde integridad.
- **Coste asumido:** las consultas polimórficas (por ejemplo, buscar un usuario por email en el login) hacen JOIN con las subtablas. Con el volumen de un piloto en Granada, el impacto es despreciable.

### Decisiones de diseño de la clase

| Decisión | Por qué |
|---|---|
| Constructor vacío `protected` | JPA lo necesita para crear los objetos al leer de la BD, pero así nadie puede crear un usuario incompleto desde fuera |
| Constructor con los campos obligatorios | Si falta un dato obligatorio, el error sale al compilar y no al guardar en la BD |
| `id` y `fechaRegistro` sin setter | Los asignan la BD y `@PrePersist`, y no se deben cambiar a mano |
| `rol` sin setter, fijado por cada subclase | Un `Cliente` siempre tiene rol `CLIENTE`: el rol y la clase no pueden dejar de coincidir |
| Campo `rol` guardado en `usuario` | Spring Security conoce el rol sin hacer JOIN con las subtablas |
| `@Enumerated(EnumType.STRING)` | Guarda `"CUIDADOR"` y no la posición del enum (`2`). Si se reordena el enum, los datos no se corrompen |
| `@PrePersist` en `fechaRegistro` | La fecha se asigna sola justo antes del primer guardado |
| `Instant` para fechas | Es un instante exacto en UTC, independiente de la zona horaria del servidor |

### Atributos y por qué

| Atributo | Por qué existe o por qué se llama así |
|---|---|
| `email` (único) | Identificador para iniciar sesión |
| `hashContrasenia` | El nombre deja claro que se guarda el hash BCrypt y nunca la contraseña. Evita que alguien guarde la contraseña en claro por error |
| `nombre` + `apellidos` | Separados para poder mostrar "María G." (minimización de datos), ordenar y buscar |
| `telefono` | Opcional |
| `claveImagenPerfil` | Clave del archivo en R2, no la URL. Las URLs firmadas caducan, así que se generan en cada petición |
| `activo` | Permite al admin bloquear cuentas (moderación, DSA) sin borrar datos |
| `emailVerificado` | Impide usar la plataforma con un email falso |
| `fechaRegistro` | Auditoría y estadísticas |

### Idioma del código

- **Decisión:** español en entidades, columnas y JSON.
- **Por qué:** es la preferencia del equipo y coincide con la memoria y con el tribunal. Lo importante es no mezclar idiomas: tener `Usuario` relacionado con `Booking` genera confusión.

---

## 6. Seguridad y RGPD aplicados al diseño

| Medida | Dónde | Principio |
|---|---|---|
| Contraseñas con BCrypt (hash + salt) | `hashContrasenia` | Si roban la BD, no pueden recuperar las contraseñas |
| Bucket privado + URLs firmadas | `claveImagenPerfil`, documentos | El DNI y los certificados solo los ve quien tiene permiso, y solo durante unos minutos |
| DTOs en lugar de entidades | API | Minimización: solo sale el dato necesario |
| "María G." en los perfiles públicos | `nombre` + `apellidos` | Minimización de datos (RGPD, art. 5.1.c) |
| Campo `activo` | Moderación | Cumplimiento del DSA: bloquear cuentas sin perder la trazabilidad |
| Secretos en variables de entorno | Configuración | Ninguna credencial real en el repositorio |
| `CHECK` y `UNIQUE` en la BD | Migraciones | Integridad garantizada por la BD, aunque falle la aplicación |

---

## 7. Problemas encontrados y cómo se resolvieron

Este apartado sirve para la memoria: muestra cómo se depuró cada fallo.

| Fecha | Error | Causa | Solución | Aprendizaje |
|---|---|---|---|---|
| 22/11 | `no configuration file provided` | `docker compose` lanzado desde `backend/` | Ejecutarlo en la raíz | Compose busca el archivo en la carpeta actual |
| 22/11 | `'url' attribute is not specified` | El perfil `dev` no tenía conexión | Añadir `spring.datasource.*` | Spring lee las propiedades del perfil activo |
| 22/11 | `la autentificación password falló` | Respondía el PostgreSQL de Windows, no el contenedor; el mensaje en español era la pista | Contenedor en el puerto 5433 | Dos servicios en el mismo puerto: el que responde puede no ser el esperado |