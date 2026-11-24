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
3. [Entorno de desarrollo y configuración](#3-entorno-de-desarrollo-y-configuración)
4. [Base de datos y migraciones](#4-base-de-datos-y-migraciones)
5. [Modelo de dominio: `Usuario`, `Cliente` y `Admin`](#5-modelo-de-dominio-usuario-cliente-y-admin)
6. [Seguridad y RGPD aplicados al diseño](#6-seguridad-y-rgpd-aplicados-al-diseño)
7. [Problemas encontrados y cómo se resolvieron](#7-problemas-encontrados-y-cómo-se-resolvieron)

---

## 1. Arquitectura general

### Cliente-servidor con API REST

- **Qué es:** un único backend expone una API REST en JSON. La consumen tres clientes: la app móvil, el panel de administración y la web corporativa.
- **Para qué:** toda la lógica de negocio y todos los datos están en un solo sitio.
- **Por qué así:** si mañana hay que añadir otro cliente (una app de escritorio, una integración con una empresa), le basta con consumir la misma API. Además, las reglas de negocio no se duplican en cada cliente.
- **Pregunta probable:** *¿Por qué no hacer que la app se conecte directamente a la base de datos?*
  Porque las credenciales de la BD quedarían dentro de la app y cualquiera podría extraerlas. La API es la única que habla con la BD, y es ella la que comprueba la identidad y los permisos de cada petición.

### Arquitectura en capas en el backend

```
Controller  →  Service  →  Repository  →  Base de datos
(HTTP/DTO)     (negocio)    (acceso a datos)
```

- **Para qué:** cada capa tiene una sola responsabilidad.
- **Por qué así:** se puede cambiar una capa sin tocar las demás (por ejemplo, pasar del pago simulado a Stripe). Además, cada `Service` se puede probar con JUnit y Mockito sin levantar la BD ni el servidor.
- **DTOs:** las entidades JPA nunca salen por la API. Antes se transforman en DTOs, por dos motivos: no exponer campos internos como `hashContrasenia`, y que un cambio en la BD no rompa la app.

### Stack tecnológico

| Capa | Tecnología | Por qué |
|---|---|---|
| Backend | Java 25 + Spring Boot 4 | Es el lenguaje y el framework que mejor domina el equipo (DAM). Versión con soporte vigente |
| Base de datos | PostgreSQL + PostGIS | El dominio es relacional y transaccional (reservas, pagos), y necesita búsquedas por distancia |
| App móvil | React Native + Expo | Un solo código para Android, iOS y web, aprovechando que el equipo sabe JavaScript |
| Panel admin | React + Vite | Reutiliza la plantilla de un proyecto anterior |
| Archivos | Cloudflare R2 | Capa gratuita, bucket privado y URLs firmadas para el DNI y los certificados |
| Pagos | Simulación con la interfaz `PaymentGateway` | Se puede cambiar a Stripe Connect sin tocar la lógica de negocio |

- **Pregunta probable:** *¿Por qué no MongoDB?*
  Las reservas, los pagos y las valoraciones están muy relacionados entre sí. Necesitan integridad referencial y transacciones (no se puede cobrar una reserva que no existe), y eso es justo lo que da una base de datos relacional.

---

## 2. Organización del trabajo

### Monorepo

- **Qué es:** un solo repositorio con `backend/`, `mobile/`, `admin/` y `web/`.
- **Por qué:** un cambio en la API y su adaptación en la app pueden ir en el mismo PR. Además, hay un único README y un único sitio para las incidencias.

### GitFlow

| Rama | Uso |
|---|---|
| `main` | Lo que está en producción. Cada merge despliega |
| `develop` | Integración de lo terminado. Es la rama por defecto del repo |
| `feature/<area>-<descripcion>` | Una funcionalidad; sale de `develop` y vuelve a `develop` por PR |
| `release/v<versión>` | Preparación de una entrega |
| `hotfix/*` | Correcciones urgentes en producción |

- **Por qué:** `main` siempre funciona. Lo que está a medias no rompe el trabajo de los compañeros, y cada cambio pasa por una revisión en el PR.

### Pull requests

- **Flujo:** se abre un PR de la `feature` a `develop`. El compañero lo descarga, lo prueba en local y lo aprueba. Después se hace el merge y se borra la rama.
- **Merge commit, no *squash*:** se conservan los commits de cada miembro del equipo. Así el historial muestra quién hizo qué, y sirve como evidencia del trabajo en grupo.
- **Ramas cortas por tarea:** dos personas no trabajan a la vez sobre los mismos archivos en la misma rama. Si no, el segundo en hacer `push` tiene que resolver conflictos a mano.

### Conventional Commits

- **Formato:** `tipo(ámbito): descripción`, en minúscula y sin espacio antes del paréntesis. Por ejemplo: `feat(backend): migración V4 con tablas cliente y admin`.
- **Tipos usados:**
  - `feat`: funcionalidad nueva
  - `fix`: corrección
  - `refactor`: reestructurar sin cambiar el comportamiento
  - `chore`: configuración y entorno
  - `docs`: documentación
  - `test`: pruebas
- **Un commit, un tipo de cambio:** una migración no va dentro de un commit de `docs`.
- **Por qué:** el historial se lee de un vistazo, y se puede generar un changelog automáticamente.

---

## 3. Entorno de desarrollo y configuración

### Docker Compose

- **Qué es:** un archivo (`docker-compose.yml`) que define los servicios que necesita la app. Se levantan con un solo comando: `docker compose up -d`.
- **Para qué:** levantar PostgreSQL + PostGIS y Mailpit en local.
- **Por qué así:** los tres miembros del equipo tienen exactamente la misma versión de la BD y la misma configuración, sin instalar nada a mano. Se acaba el "en mi ordenador funciona".
- **Puerto 5433:** el 5432 choca con PostgreSQL si está instalado en Windows.
- **Volumen:** los datos se conservan aunque se pare el contenedor. `docker compose down -v` los borra, y Flyway reconstruye la BD desde V1 al arrancar.
- **Healthcheck:** el contenedor indica cuándo PostgreSQL ya acepta conexiones.
- **Mailpit:** un servidor de correo falso. Los emails de verificación se ven en `http://localhost:8025` en lugar de enviarse de verdad.
- **Pregunta probable:** *¿Por qué las credenciales de la BD están en el repositorio?*
  Porque solo sirven para un contenedor local al que no se puede acceder desde fuera. Las de producción van en variables de entorno del servidor y nunca se suben.

### Perfiles de Spring (`dev` / `prod`)

| Archivo | Contenido |
|---|---|
| `application.properties` | Lo común: perfil por defecto `dev`, `open-in-view=false` y `ddl-auto=validate` |
| `application-dev.properties` | BD del contenedor local, `show-sql` para depurar y Mailpit |
| `application-prod.properties` | Todo con variables de entorno: `${DB_URL}`, `${MAIL_HOST}`… |

- **Para qué:** el mismo código se conecta a la BD local en desarrollo y a la de producción en el servidor, sin tocar código.
- **`prod` sin valores por defecto:** si falta una variable, la app no arranca y dice cuál falta (`Could not resolve placeholder 'MAIL_HOST'`). Es mejor fallar al arrancar que conectarse a una BD equivocada.
- **`server.port=${PORT:8080}`:** Render asigna el puerto en la variable `PORT`. El valor después de los dos puntos es el que se usa por defecto.
- **Actuator solo con `health`:** Render lo usa para comprobar que la app está viva. Los demás endpoints muestran información interna y no se exponen.
- **`open-in-view=false`:** la conexión a la BD no se queda abierta mientras se genera la respuesta. Así no se lanzan consultas ocultas fuera del `Service`.
- **`backend/.env.example`:** plantilla con los nombres de todas las variables de producción y sin ningún valor real. Quien clone el repo sabe qué tiene que configurar sin leer el código.
- **Pregunta probable:** *¿Cómo demostráis que no hay credenciales en el código?*
  Arrancando con `-Dspring-boot.run.profiles=prod` sin variables de entorno: la app se niega a arrancar.

### `.gitignore` en dos niveles

| Archivo | Qué ignora |
|---|---|
| `.gitignore` (raíz) | Secretos (`.env`, `.env.*`, menos `.env.example`), `node_modules/`, Expo, claves de firma (`.jks`, `.keystore`, `.p12`, `.p8`, `.key`), builds, IDEs, sistema operativo, logs |
| `backend/.gitignore` | Lo de Maven (`target/`, `HELP.md`) |

- **Por qué en la raíz:** Git aplica cada `.gitignore` a su carpeta y a todas sus subcarpetas, así que el de la raíz cubre todo el monorepo, incluidos `mobile/` y `admin/`.
- **`!.env.example`:** el `!` es una excepción. Se ignoran todos los `.env.*` menos la plantilla.
- **Claves de firma:** con un `.jks` o un `.p12`, cualquiera podría publicar una versión de la app firmada como si fuera nuestra.
- **Por qué es importante:** el repositorio es público. Una credencial subida a GitHub queda expuesta para siempre, aunque después se borre, porque queda en el historial.

---

## 4. Base de datos y migraciones

### Flyway

- **Qué es:** una herramienta de versionado del esquema de la BD. Cada cambio es un script SQL numerado (`V1__...sql`, `V2__...sql`). Flyway los ejecuta en orden al arrancar la app.
- **Para qué:** crear y modificar las tablas de forma controlada y reproducible.
- **Por qué así:**
  - El esquema queda versionado en Git, igual que el código.
  - Cualquier BD (la local de cada persona o la de producción) llega exactamente al mismo estado: cada una aplica solo lo que le falta.
  - Flyway guarda en la tabla `flyway_schema_history` qué scripts ha aplicado, así que ninguno se ejecuta dos veces.
- **Formato del nombre:** `V` + número + **dos** guiones bajos + descripción. Con un solo guion bajo, Flyway no detecta el archivo.
- **Alternativa descartada:** `ddl-auto=update`, que deja que Hibernate cree o modifique las tablas por su cuenta:
  - No crea extensiones, índices GiST, `EXCLUDE` ni `CHECK` a medida.
  - Solo añade columnas: no las borra, no las renombra y no cambia tipos ni longitudes.
  - No deja historial, y en producción cambiaría la BD real al arrancar, sin revisión.

### Migraciones inmutables

- **Regla:** una migración que ya está en el repo no se edita nunca. Cualquier cambio va en una migración nueva.
- **Por qué:**
  - Flyway guarda el checksum de cada migración aplicada. Si el archivo cambia, la app falla con `Migration checksum mismatch`.
  - En producción, una migración editada no se volvería a ejecutar, porque Flyway ya la tiene marcada como aplicada. El cambio no llegaría nunca.
- **`CREATE TABLE` frente a `ALTER TABLE`:** `CREATE` sirve para empezar de cero. `ALTER` hace evolucionar la tabla sin perder los datos que ya tiene.
- **Coordinación:** antes de crear una migración se avisa al equipo, para que no haya dos `V5` en ramas distintas.
- **Pregunta probable:** *¿No acabaréis con demasiados archivos V?*
  Es normal: cada archivo es un cambio pequeño y explicado, como un commit. Para ver el esquema completo no se leen las migraciones; se usan el diagrama ER o un volcado: `pg_dump --schema-only`.
- **Pregunta probable:** *¿Por qué no juntáis todas las migraciones en una?*
  Solo sería seguro antes del primer despliegue, y la BD resultante sería idéntica. Además, se perdería el historial que demuestra cómo evolucionó el modelo sin perder datos.

### `ddl-auto=validate`

- **Qué es:** Hibernate no toca la BD. Solo comprueba al arrancar que cada entidad coincide con su tabla.
- **Para qué:** detectar enseguida cualquier diferencia entre el código Java y el SQL.
- **Por qué así:** si alguien añade un atributo y olvida la migración, la app no arranca (`missing column`). El error sale en desarrollo, no en producción.
- **Lo que NO comprueba:** longitudes, `nullable` ni `unique`. Por eso las longitudes de `@Column` usan las mismas constantes de `Config` que los `VARCHAR` del SQL. Si no coincidieran, el fallo saldría en el primer `INSERT` largo, como un error 500 de la BD.
- **`@Column(unique = true)` y `@Column(length = ...)`:** con Flyway, estas anotaciones solo documentan. Quien garantiza la unicidad o la longitud es la restricción de la migración.

### Nombres de columna

- **Estrategia de Spring Boot:** el nombre de la columna se genera en snake_case a partir del atributo (`fechaNacimiento` → `fecha_nacimiento`).
- **Criterio:** `@Column(name = ...)` solo es obligatorio cuando la columna se llama distinto al atributo, por ejemplo `strikes` → `cantidad_strikes`.

### Migraciones actuales

| Migración | Contenido | Por qué |
|---|---|---|
| `V1__extensions.sql` | `postgis` y `btree_gist` | Van primero: las tablas posteriores usan sus tipos y operadores |
| `V2__crear_usuario.sql` | Tabla `usuario` | Datos comunes a todos los tipos de usuario |
| `V3__ajustar_usuario.sql` | `cantidad_strikes`, `telefono NOT NULL`, `CHECK` del email en minúsculas | Adaptar `usuario` al modelo final sin editar V2 |
| `V4__crear_cliente_y_admin.sql` | Tablas `cliente` y `admin` | Subclases de la herencia JOINED |

### Extensiones de PostgreSQL

- **PostGIS:** añade el tipo `geography(Point)` y funciones como `ST_DWithin`, para buscar cuidadores en un radio de X km. Con un índice GiST, la búsqueda es rápida aunque haya miles de cuidadores.
- **btree_gist:** permite la restricción `EXCLUDE`, con la que la BD impide que un cuidador tenga dos reservas que se solapan en el tiempo. La regla la garantiza la propia BD, aunque lleguen dos peticiones a la vez.
- **`IF NOT EXISTS`:** el script no falla si la extensión ya estaba activada, como pasa en la imagen de Docker.

### Decisiones en el SQL de `usuario`

| Elemento | Por qué |
|---|---|
| `BIGINT GENERATED BY DEFAULT AS IDENTITY` | Es la forma estándar en SQL de generar el id (sustituye a `SERIAL`) y la que espera `GenerationType.IDENTITY` |
| `VARCHAR(n)` con el mismo `n` que `@Column(length)` | La entidad y la tabla coinciden, y el tamaño queda documentado |
| `TIMESTAMPTZ` | Guarda el instante con zona horaria y corresponde a `Instant` de Java. Evita errores con el cambio de hora |
| `DEFAULT` en `activo`, `email_verificado`, `fecha_registro` y `cantidad_strikes` | Las filas insertadas a mano (datos de prueba) quedan igual que las creadas desde Java. En un `ALTER TABLE ... ADD COLUMN ... NOT NULL`, el `DEFAULT` rellena las filas que ya existen |
| `uk_usuario_email UNIQUE` | No puede haber dos cuentas con el mismo email. El nombre de la restricción permite devolver el mensaje "El email ya está registrado" |
| `ck_usuario_email_minusculas CHECK (email = lower(email))` | Con la normalización en Java, el `UNIQUE` funciona sin distinguir mayúsculas. El `CHECK` impide que se cuele un email en mayúsculas por SQL |
| `ck_usuario_rol CHECK (...)` | El enum solo protege en Java. El `CHECK` también impide valores inválidos que entren por SQL |
| `ck_usuario_strikes CHECK (cantidad_strikes >= 0)` | Nunca habrá strikes negativos. El máximo (3) no va en el `CHECK`: está en `Config.MAX_STRIKES`, y así solo se cambia en un sitio |
| `telefono NOT NULL`, sin `UNIQUE` | Hace falta para contactar, pero hay hogares que comparten teléfono (una pareja de mayores, un padre y su hijo) |

- **Restricciones con nombre (`uk_`, `ck_`, `fk_`):** si una se incumple, el error dice cuál ha sido, y el `GlobalExceptionHandler` podrá traducirlo a un mensaje claro.
- **Pregunta probable:** *¿Por qué validar en la BD si ya se valida en Java?*
  Por defensa en profundidad. La BD es la última barrera: protege los datos aunque alguien los toque desde fuera de la aplicación (scripts, otra herramienta o un error del programador).

### Decisiones en el SQL de `cliente` y `admin`

| Elemento | Por qué |
|---|---|
| `id BIGINT PRIMARY KEY` + `FOREIGN KEY (id) REFERENCES usuario (id)` | En JOINED, la fila hija comparte el id con su fila de `usuario`: el id es a la vez PK y FK |
| El `id` de la hija no es `IDENTITY` | Lo genera `usuario`, y Hibernate lo copia en el `INSERT` de la hija |
| `ON DELETE CASCADE` | Si se borra un usuario, también se borra su fila hija y no quedan huérfanos. En el día a día no se borra: se desactiva con `activo`. El borrado físico es para el derecho de supresión del RGPD |
| `fecha_nacimiento DATE NOT NULL` | `DATE` ↔ `LocalDate`: una fecha de nacimiento no tiene hora ni zona horaria |
| Sin `CHECK` de edad mínima | La edad cambia con el tiempo, y un `CHECK` con `now()` no es fiable. Se valida en Java con `Config.EDAD_MINIMA_REGISTRO` |

### Comprobación

Los comandos para comprobar cada migración, cada tabla y cada restricción desde la terminal están en `Comprobaciones_BD_Terminal.md`.

---

## 5. Modelo de dominio: `Usuario`, `Cliente` y `Admin`

### Qué es una entidad JPA

| Java | Base de datos |
|---|---|
| Clase `@Entity` | Tabla |
| Atributo | Columna |
| Objeto | Fila |

Las anotaciones (`@Column`, `@Id`…) indican cómo se traduce cada atributo a una columna. JPA/Hibernate hace la conversión, y así no hace falta escribir SQL para las operaciones básicas.

### Herencia: `Usuario` y sus subclases

```
              Usuario (abstracta)
     ┌────────┬───────┴──────┬──────────┐
   Admin   Cliente       Cuidador     Empresa
```

- **Por qué herencia:** los cuatro tipos comparten el login, el nombre, el teléfono y el estado de la cuenta. Escribirlo una sola vez evita duplicar código.
- **Por qué `abstract`:** nunca existe un "usuario a secas"; siempre es de uno de los cuatro tipos.
- **Regla de la herencia:** en la clase padre solo va lo que comparten **todos** los hijos.
- **Pregunta probable:** *¿Por qué `Admin` hereda de `Usuario` si no usa todos sus datos?*
  Porque `Usuario` contiene lo necesario para iniciar sesión, y un admin también inicia sesión. Si no heredara, habría dos tablas de login, dos flujos de autenticación y la posibilidad de tener el mismo email en las dos tablas.

### Estrategia `JOINED`

- **Qué es:** la tabla `usuario` guarda los datos comunes, y cada subclase tiene su propia tabla con sus columnas propias, que comparte el `id` con `usuario`. Hibernate las une con un JOIN al consultar.
- **Por qué así:**
  - Las tablas están normalizadas, sin columnas vacías.
  - Las columnas propias de cada tipo pueden ser `NOT NULL`. Ejemplo: `cliente.fecha_nacimiento`; en el futuro, la tarifa del cuidador o el CIF de la empresa.
- **Alternativa descartada:** `SINGLE_TABLE`, con todo en una sola tabla. Es más rápida al no tener JOINs, pero obliga a que las columnas de cuidador y empresa admitan `NULL` en las filas de cliente y admin, y se pierde integridad.
- **Coste asumido:** las consultas polimórficas (por ejemplo, buscar un usuario por email en el login) hacen JOIN con las subtablas. Con el volumen de un piloto en Granada, el impacto es despreciable.

### Toda cuenta pertenece a una persona

- **Decisión:** `nombre` y `apellidos` son siempre los de la persona que inicia sesión. En una empresa, son los de la persona de contacto.
- **Los datos fiscales de la empresa** (razón social, CIF) irán en la tabla `empresa`, no en `usuario`.
- **Por qué:**
  - Cada campo tiene un único significado: `nombre` nunca es una razón social.
  - `apellidos` puede ser `NOT NULL` para todos, sin excepciones por rol.
  - Siempre se sabe qué persona opera una cuenta, que es lo que pide la trazabilidad del DSA (art. 30).
- **Alternativa descartada:** usar `nombre` como razón social y `apellidos` como los del contacto. Mezcla dos significados en el mismo dato y daría resultados como "Cuidados Sur SL García López".

### La ubicación no está en `Usuario`

- **Decisión:** `Usuario` no tiene latitud ni longitud.
- **Por qué:**
  - Solo la necesitan el cuidador y la empresa, para aparecer en las búsquedas por radio. Un admin no la necesita.
  - El cliente tampoco: el servicio se presta en casa de la persona mayor, no en la del cliente que paga. Además, un cliente puede tener a su cargo a varias personas en ciudades distintas.
  - Guardar un dato que no se usa va contra la minimización de datos (RGPD, art. 5.1.c).
  - Dos `Double` no permiten búsquedas espaciales con índice.
- **Dónde irá:**

| Entidad | Ubicación | Para qué |
|---|---|---|
| `Cuidador` / `Empresa` | `geography(Point, 4326)` + índice GiST | Aparecer en las búsquedas por radio (Sprint 3) |
| `PersonaCuidada` / `Reserva` | dirección + `geography(Point, 4326)` | Saber dónde se presta el servicio (Sprint 4) |
| `Cliente`, `Admin` | ninguna | No la necesitan |

- **Pregunta probable:** *Si el cliente no tiene ubicación, ¿cómo busca cuidadores?*
  El punto de búsqueda llega en cada petición (`GET /api/v1/cuidadores?lat=...&lng=...&radioKm=10`). La app lo toma del domicilio de la persona cuidada que se elige, del GPS del móvil o de una dirección convertida con Mapbox. La ubicación que tiene que estar guardada e indexada es la del objetivo de la búsqueda (los cuidadores), no la del origen.
- **`geography` y no `geometry`:** calcula las distancias en metros sobre la esfera terrestre. `4326` es WGS84, el sistema del GPS y de Mapbox. Ojo: PostGIS ordena las coordenadas como (longitud, latitud).

### Decisiones de diseño de la clase

| Decisión | Por qué |
|---|---|
| Constructor vacío `protected` | JPA lo necesita para crear los objetos al leer de la BD, pero así nadie puede crear un usuario incompleto desde fuera |
| Constructor con los campos obligatorios (email, hash, nombre, apellidos, rol, teléfono) | Si falta un dato obligatorio, el error sale al compilar y no al guardar en la BD |
| `id` y `fechaRegistro` sin setter | Los asignan la BD y `@PrePersist`, y no se deben cambiar a mano |
| `rol` sin setter, fijado por cada subclase | Un `Cliente` siempre tiene rol `CLIENTE`: el rol y la clase no pueden dejar de coincidir |
| Campo `rol` guardado en `usuario` | Spring Security conoce el rol sin hacer JOIN con las subtablas |
| `@Enumerated(EnumType.STRING)` | Guarda `"CUIDADOR"` y no la posición del enum (`2`). Si se reordena el enum, los datos no se corrompen |
| `@PrePersist` en `fechaRegistro` | La fecha se asigna sola justo antes del primer guardado |
| `Instant` para fechas con hora | Es un instante exacto en UTC, independiente de la zona horaria del servidor |
| Atributos `private`, métodos de negocio `public` | El objeto protege sus propias reglas. Quién puede ejecutar cada acción se controla en otra capa (`@PreAuthorize`) |

### Constantes en `Config`

- **Qué es:** una clase `final` con constructor privado que agrupa los límites y los patrones de validación (longitudes, regex de email, teléfono, DNI/NIE, CIF, nombre, edad mínima, número máximo de strikes…).
- **Por qué:** sin "números mágicos" repetidos. Cada límite se cambia en un solo sitio, y es el mismo valor que usan la `@Column`, el DTO y el `VARCHAR` del SQL.
- **Dos constantes para la contraseña:**

| Constante | Valor | Para qué |
|---|---|---|
| `MAX_LONGITUD_CONTRASENIA` | 64 | Límite de la contraseña en claro que escribe el usuario. Evita DoS con contraseñas enormes (BCrypt es lento a propósito) |
| `MAX_LONGITUD_HASH_CONTRASENIA` | 100 | Tamaño de la columna. Cabe el hash BCrypt (60) más el prefijo del algoritmo `{bcrypt}` (68 en total), con margen para cambiar de algoritmo |

- **Pregunta probable:** *¿Por qué no usar la misma constante?*
  Porque miden cosas distintas. Con 64 en la columna, el hash con prefijo (68 caracteres) no cabría.

### Strikes (moderación)

- **Qué es:** un contador de advertencias (`strikes`, columna `cantidad_strikes`). Al llegar a `Config.MAX_STRIKES` (3), la cuenta se bloquea.
- **Sin setter público.** Solo hay dos métodos de negocio:
  - `aniadirStrike()`: suma 1, con tope en el máximo. Al llegar a 3, pone `activo = false`.
  - `quitarStrike()`: resta 1, nunca baja de 0 y **no reactiva** la cuenta.
- **Por qué así:**
  - **Encapsulación:** desde fuera no se puede poner `strikes = -5` ni `strikes = 1000`.
  - **Bloquear es automático; desbloquear es una decisión humana.** Lo hace un admin desde el panel, de forma explícita y registrada (trazabilidad, DSA).
- **Alternativa descartada:** un `setStrikes(int)` que reactivaba la cuenta si los strikes bajaban de 3. Una cuenta bloqueada por fraude se habría desbloqueado sola al quitarle un strike.

### Email normalizado

- **Qué es:** `EmailUtil.normalizar(email)` devuelve `email.trim().toLowerCase(Locale.ROOT)`. Se usa en el constructor y en `setEmail` de `Usuario`, y se usará en el login antes de buscar.
- **Por qué:**
  - `Ana@gmail.com` y `ana@gmail.com` son el mismo buzón. Sin normalizar, se podrían crear dos cuentas para la misma persona, y el login fallaría si el usuario escribe el email con otras mayúsculas.
  - `trim()` quita los espacios que se cuelan al copiar y pegar.
  - **`Locale.ROOT`:** `toLowerCase()` sin locale usa el idioma del sistema. En turco, `I` pasa a `ı` (i sin punto) y el email dejaría de coincidir. `Locale.ROOT` da el mismo resultado en cualquier servidor.
- **En la entidad:** da igual por dónde entre el email (registro, cambio de email, datos de prueba): siempre se guarda normalizado.
- **`null` → `null`:** el método solo transforma. Comprobar que el email es obligatorio es trabajo del DTO (`@NotBlank`).
- **Pregunta probable:** *El estándar permite que la parte local del email distinga mayúsculas. ¿Por qué no lo respetáis?*
  Ningún proveedor real (Gmail, Outlook, Yahoo, iCloud) lo aplica. Respetarlo permitiría cuentas duplicadas y haría fallar el login.
- **Lo que no se normaliza:** los puntos y el `+alias` de Gmail. Es un comportamiento propio de Gmail, y aplicarlo a otros dominios juntaría cuentas que son distintas.

### Atributos de `Usuario`

| Atributo | Por qué existe o por qué se llama así |
|---|---|
| `email` (único, normalizado) | Identificador para iniciar sesión |
| `hashContrasenia` | El nombre deja claro que se guarda el hash BCrypt y nunca la contraseña. Evita que alguien guarde la contraseña en claro por error |
| `nombre` + `apellidos` | Siempre de una persona (en una empresa, del contacto). Separados para poder mostrar "María G." (minimización de datos), ordenar y buscar |
| `telefono` | Obligatorio para contactar, pero no único: hay hogares que comparten teléfono |
| `claveImagenPerfil` | Clave del archivo en R2, no la URL. Las URLs firmadas caducan, así que se generan en cada petición |
| `rol` | Tipo de usuario, fijado por la subclase |
| `activo` | Permite al admin bloquear cuentas (moderación, DSA) sin borrar datos |
| `emailVerificado` | Impide usar la plataforma con un email falso |
| `fechaRegistro` | Auditoría y estadísticas |
| `strikes` | Advertencias de moderación; bloqueo automático al llegar al máximo |

### Atributos de `Cliente` y `Admin`

| Clase | Atributo | Por qué |
|---|---|---|
| `Cliente` | `fechaNacimiento` (obligatoria, `LocalDate`) | Comprobar la mayoría de edad (`EDAD_MINIMA_REGISTRO = 18`) |
| `Cliente` | `direccion`, `ciudad` (opcionales) | Datos de contacto y facturación del cliente. No son el lugar del servicio, que va en la persona cuidada |
| `Admin` | `departamento` (opcional) | Soporte, Moderación, Superadmin… para organizar el panel |

### Idioma del código

- **Decisión:** español en entidades, columnas y JSON.
- **Por qué:** es la preferencia del equipo y coincide con la memoria y con el tribunal. Lo importante es no mezclar idiomas: tener `Usuario` relacionado con `Booking` genera confusión.

---

## 6. Seguridad y RGPD aplicados al diseño

| Medida | Dónde | Principio |
|---|---|---|
| Contraseñas con BCrypt (hash + salt) | `hashContrasenia` | Si roban la BD, no pueden recuperar las contraseñas |
| Límite de 64 caracteres en la contraseña en claro | `Config` | Evita DoS con contraseñas enormes |
| Bucket privado + URLs firmadas | `claveImagenPerfil`, documentos | El DNI y los certificados solo los ve quien tiene permiso, y solo durante unos minutos |
| DTOs en lugar de entidades | API | Minimización: solo sale el dato necesario |
| "María G." en los perfiles públicos | `nombre` + `apellidos` | Minimización de datos (RGPD, art. 5.1.c) |
| Ubicación solo donde se usa | Cuidador, empresa, persona cuidada | Minimización: un admin o un cliente no guardan una ubicación que no se usa |
| Toda cuenta pertenece a una persona | `nombre` + `apellidos` obligatorios | Trazabilidad de quién opera cada cuenta (DSA, art. 30) |
| Campo `activo` + strikes | Moderación | Bloquear cuentas sin perder la trazabilidad (DSA). La reactivación es siempre manual |
| `ON DELETE CASCADE` | FK de las subtablas | El borrado físico (derecho de supresión) no deja datos huérfanos |
| Email normalizado + `CHECK` | `usuario.email` | Una persona, una cuenta |
| Secretos en variables de entorno | `application-prod.properties` | Ninguna credencial real en el repositorio; sin variables, `prod` no arranca |
| `.gitignore` en la raíz | Repo | `.env` y claves de firma nunca se suben a un repo público |
| Actuator solo con `health` | `prod` | No se expone información interna de la app |
| `CHECK`, `UNIQUE` y `FK` en la BD | Migraciones | Integridad garantizada por la BD, aunque falle la aplicación |

---

## 7. Problemas encontrados y cómo se resolvieron

Este apartado sirve para la memoria: muestra cómo se depuró cada fallo.

| Fecha | Error | Causa | Solución | Aprendizaje |
|---|---|---|---|---|
| 22/11 | `no configuration file provided` | `docker compose` lanzado desde `backend/` | Ejecutarlo en la raíz | Compose busca el archivo en la carpeta actual |
| 22/11 | `'url' attribute is not specified` | El perfil `dev` no tenía conexión | Añadir `spring.datasource.*` | Spring lee las propiedades del perfil activo |
| 22/11 | `la autentificación password falló` | Respondía el PostgreSQL de Windows, no el contenedor. El mensaje en español era la pista | Contenedor en el puerto 5433 | Con dos servicios en el mismo puerto, el que responde puede no ser el esperado |
| 23/11 | La app no arrancaría con `validate` | Tras el merge de ramas, `Usuario`, `Cliente` y `Admin` tenían campos y tablas que no estaban en V2 | Quitar la ubicación de `Usuario` y crear V3 y V4 | Cada cambio en una entidad va en el mismo commit que su migración |
| 23/11 | Longitudes distintas en Java y en SQL | `@Column(length)` no coincidía con el `VARCHAR`, y `validate` no lo detecta | Constantes de `Config` iguales a la migración | `validate` comprueba columnas y tipos, no longitudes |
| 23/11 | El hash podía no caber en su columna | Se usaba el máximo de la contraseña en claro (64) | Constante propia de 100 | Contraseña en claro y hash son datos distintos |
| 23/11 | `setStrikes` reactivaba cuentas bloqueadas | Mezclaba los strikes con el estado de la cuenta | `aniadirStrike()` / `quitarStrike()` sin setter | Una regla de negocio se protege dentro del objeto |
| 23/11 | Emails duplicados por mayúsculas | `UNIQUE` distingue mayúsculas | `EmailUtil.normalizar` + `CHECK (email = lower(email))` | La unicidad depende de cómo se guarda el dato |
| 24/11 | `syntax error at or near "CHECK"` en V2 | Se editó una migración ya aplicada, con un `CONSTRAINT` sin nombre | Restaurar V2 desde su commit original; los cambios, en V3 | Una migración del repo no se edita nunca |
| 24/11 | `git checkout --` no arreglaba V2 | El V2 roto ya estaba commiteado, y `--` restaura la última versión del commit | `git checkout <commit> -- archivo` | Hay que restaurar desde el commit en que el archivo estaba bien |
| 24/11 | `pathspec ... did not match any file` | El comando se lanzó desde `backend/` con una ruta relativa a la raíz | Ejecutarlo desde la raíz | Las rutas de Git son relativas a la carpeta actual |
| 24/11 | `backend/target/` aparecía como *untracked* | El `.gitignore` de la raíz se pegó encima de `backend/.gitignore` | `git restore backend/.gitignore` y crear el archivo en la raíz | Cada `.gitignore` se aplica a su carpeta |
| 24/11 | `prod`: `Could not resolve placeholder 'MAIL_HOST'` | **Esperado:** en local no hay variables de entorno | — | Demuestra que no hay credenciales en el código. Spring falla con la primera variable que evalúa |