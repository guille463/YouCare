# YouCare

Proyecto fin de grado 2ª DAM

# YouCare — Marketplace de cuidado de personas mayores

## Descripción

**YouCare** es una plataforma multiplataforma de intermediación que conecta a familias que necesitan cuidar de una persona mayor con cuidadores verificados, ya sean autónomos o trabajen a través de una empresa. Todo el proceso se hace dentro de la app: buscar, comparar, reservar, pagar y valorar.

Proyecto de Fin de Grado de **DAM** (Desarrollo de Aplicaciones Multiplataforma). Piloto en Granada.

### Flujo

**APP MÓVIL / PANEL ADMIN** --> **CONTROLLER** --> **SERVICE** --> **REPOSITORY** --> **POSTGRESQL + POSTGIS**

---

## Estructura del repositorio

```
youcare/
├── README.md
├── .gitignore
├── docker-compose.yml            # PostgreSQL + PostGIS y Mailpit para desarrollo local
├── pnpm-workspace.yaml           # Workspace de los proyectos JS (mobile y admin)
├── docs/                         # Modelo de dominio, arquitectura, capturas para la memoria
├── imgsReadme/                   # Imágenes del README
│
├── backend/                      # API REST — Java 25 + Spring Boot 4
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .env.example
│   ├── http/                     # Colección de Bruno para probar endpoints
│   └── src/
│       ├── main/
│       │   ├── java/com/youcare/
│       │   │   ├── BackendApplication.java
│       │   │   ├── config/       # Seguridad, CORS, Swagger, almacenamiento
│       │   │   ├── controller/   # Endpoints REST (/api/v1/...)
│       │   │   ├── service/      # Lógica de negocio
│       │   │   │   └── payment/  # PaymentGateway + implementación mock
│       │   │   ├── repository/   # Acceso a datos (Spring Data JPA)
│       │   │   ├── entity/       # User, Admin, Client, Caregiver, Company,
│       │   │   │   │             # CareRecipient, Booking, Payment, Review, Message
│       │   │   │   └── enums/    # Role, BookingStatus, ServiceType...
│       │   │   ├── dto/
│       │   │   │   ├── request/  # Datos que llegan a la API
│       │   │   │   └── response/ # Datos que devuelve la API
│       │   │   ├── mapper/       # Conversión entity <-> DTO
│       │   │   ├── security/     # JWT, filtro de autenticación
│       │   │   └── exception/    # GlobalExceptionHandler y excepciones propias
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       ├── application-prod.properties
│       │       ├── templates/    # Plantillas de email (Thymeleaf)
│       │       └── db/migration/ # Migraciones Flyway (V1__extensions.sql, V2__init.sql...)
│       └── test/java/com/youcare/
│
├── mobile/                       # App de clientes y cuidadores — React Native + Expo
│   ├── app.json
│   ├── package.json
│   ├── .env.example              # EXPO_PUBLIC_API_URL
│   ├── assets/
│   ├── app/                      # Rutas (Expo Router)
│   │   ├── (auth)/               # Login y registro
│   │   ├── (client)/             # Pantallas del cliente
│   │   └── (caregiver)/          # Pantallas del cuidador
│   └── src/
│       ├── api/                  # axiosClient + ApiError
│       ├── services/             # Llamadas a la API por módulo
│       ├── hooks/                # Hooks propios (useCaregivers, useBookings...)
│       ├── components/           # Componentes reutilizables + estilos
│       ├── screens/              # Pantallas
│       ├── context/              # Estado global (sesión)
│       ├── theme/                # Colores y espaciados
│       └── utils/                # Textos centralizados, formateadores
│
├── admin/                        # Panel de administración — React + Vite
│   ├── index.html
│   ├── vite.config.js
│   ├── .env.example              # VITE_API_URL
│   └── src/
│       ├── api/
│       ├── services/
│       ├── hooks/
│       ├── components/
│       ├── pages/                # Verificación, reseñas, usuarios
│       └── context/
│
└── web/                          # Web corporativa — HTML, CSS y JS
    ├── index.html
    ├── privacidad.html
    ├── aviso-legal.html
    ├── css/
    ├── js/
    └── img/
```

---

## Ramas (GitFlow)

```
main ─────●──────────────────────●────────────●──────   producción
          │                      ▲            ▲
          │                      │ release    │ hotfix
develop ──●────●────●────●───────●────────────●──────   integración
               ▲    ▲    ▲
               │    │    │
         feature/... (una por tarea)
```

| Rama                           | Sale de   | Se fusiona en      | Para qué se usa                                                                                                      |
| ------------------------------ | --------- | ------------------ | -------------------------------------------------------------------------------------------------------------------- |
| `main`                         | —         | —                  | Código en producción. Cada merge despliega automáticamente (Render, Netlify). **Nunca se trabaja directamente aquí** |
| `develop`                      | `main`    | —                  | Integración. Aquí se juntan todas las funcionalidades terminadas. Siempre tiene que compilar y arrancar              |
| `feature/<area>-<descripcion>` | `develop` | `develop`          | Una rama por tarea de Trello. Se borra al fusionarla                                                                 |
| `release/v<versión>`           | `develop` | `main` y `develop` | Se crea al cerrar cada fase: últimos ajustes, versión y etiqueta                                                     |
| `hotfix/<descripcion>`         | `main`    | `main` y `develop` | Corrección urgente de un error en producción                                                                         |

### Nombres de las ramas `feature`

El prefijo indica la parte del proyecto:

| Área      | Ejemplo                                |
| --------- | -------------------------------------- |
| `backend` | `feature/backend-auth-jwt`             |
| `mobile`  | `feature/mobile-search-screen`         |
| `admin`   | `feature/admin-caregiver-verification` |
| `web`     | `feature/web-legal-pages`              |
| `db`      | `feature/db-init-schema`               |
| `infra`   | `feature/infra-docker-render`          |
| `docs`    | `feature/docs-domain-model`            |

### Normas

1. **Nadie hace push directo a `main` ni a `develop`.** Todo entra por Pull Request.
2. Cada PR la **revisa y aprueba otra persona** del equipo antes de fusionarla.
3. Las features se fusionan con **Squash and merge**: un commit por tarea en `develop`.
4. Antes de abrir la PR: `develop` actualizado en tu rama, todo compila y los tests pasan.
5. Nunca se suben credenciales. Las variables reales van en `.env` (ignorado por Git); en el repo solo está `.env.example`.

## Flyway: cómo trabajar con la base de datos

### Qué es
Flyway crea y modifica la BD con scripts SQL numerados (`V1__...sql`, `V2__...sql`…) que están en
`backend/src/main/resources/db/migration/`.
Al arrancar la app, Flyway ejecuta los que faltan, en orden, una sola vez.
En la tabla `flyway_schema_history` guarda cuáles ha aplicado.
Hibernate NO crea tablas (`ddl-auto=validate`): solo comprueba que las entidades coinciden con la BD.
Si no coinciden, la app no arranca.

### Reglas
1. **Nunca editar un `V` que ya esté en el repo.** Flyway guarda su checksum.
   Si cambia el archivo, la app falla con `checksum mismatch`.
2. **Cualquier cambio va en una migración nueva** con el siguiente número:
   `V5__descripcion.sql`, `V6__...`
3. **Una migración por propósito:** `V5__crear_cuidador.sql`, no `V5__cosas.sql`.
4. **Avisad antes de crear una `V`**, para no tener dos `V5` en ramas distintas.
5. **Formato del nombre:** `V` + número + **dos** guiones bajos + descripción.
   Con un solo guion bajo, Flyway no lo detecta.

### Si añades un campo a una entidad
1. Añade el atributo en Java, con su `@Column`.
2. Crea `V<n>__....sql` con el `ALTER TABLE` o el `CREATE TABLE`.
3. Arranca la app. Si sale `missing column`, el campo de Java y el SQL no coinciden.
4. Haz commit del Java y del SQL **juntos**.

### Después de hacer `pull`
Arranca normal: Flyway aplica solo las migraciones nuevas.

### Si falla con `checksum mismatch` o quieres la BD limpia
Desde la raíz del repo:
    docker compose down -v
    docker compose up -d
Esto borra la BD local; al arrancar, Flyway la reconstruye desde V1.

### Comprobar qué se ha aplicado
    docker compose exec db psql -U youcare -d youcare -c "SELECT version, description, success FROM flyway_schema_history"

### Mensajes de commit

Formato [Conventional Commits](https://www.conventionalcommits.org/es/):

| Prefijo     | Uso                                         | Ejemplo                                                  |
| ----------- | ------------------------------------------- | -------------------------------------------------------- |
| `feat:`     | Nueva funcionalidad                         | `feat: registro de cuidadores con verificación de email` |
| `fix:`      | Corrección de error                         | `fix: solape de reservas en el mismo horario`            |
| `docs:`     | Documentación                               | `docs: actualizar modelo de dominio`                     |
| `refactor:` | Cambio de código sin cambiar comportamiento | `refactor: extraer mapper de Booking`                    |
| `test:`     | Tests                                       | `test: casos de cancelación con reembolso`               |
| `chore:`    | Configuración, dependencias                 | `chore: añadir Flyway al backend`                        |

### Versiones

Una versión por fase de la hoja de ruta:

| Versión  | Fase                                      |
| -------- | ----------------------------------------- |
| `v0.1.0` | Fase 1 — Análisis y diseño                |
| `v0.2.0` | Fase 2 — Autenticación y usuarios         |
| `v0.3.0` | Fase 3 — Búsqueda y geolocalización       |
| `v0.4.0` | Fase 4 — Reservas y pagos                 |
| `v0.5.0` | Fase 5 — Valoraciones, chat y panel admin |
| `v1.0.0` | Fase 6 — Entrega del TFG |




# Diario de desarrollo — YouCare

## 22/11/2026 

### Resumen
Puesta en marcha de la base de datos local y primera entidad persistida (`Usuario`). La app ya arranca, conecta con PostgreSQL y Flyway crea las tablas automáticamente.

### Qué he hecho

**1. Refactor de `Usuario.java`**
- Se mantiene la herencia `JOINED`: tabla `usuario` con los datos comunes y una tabla por subclase que comparte el `id`.
- Código en español en todo el proyecto (entidades, columnas y JSON).
- `contrasenia` → `hashContrasenia`: el campo guarda el hash BCrypt, nunca la contraseña en claro.
- `imagenPerfilUrl` → `claveImagenPerfil`: se guarda la clave del archivo en R2, porque las URLs firmadas caducan.
- Nuevo campo `apellidos`: permite mostrar "María G." y ordenar por apellido.
- `length` en todas las columnas de texto, para que coincidan con los `VARCHAR` de la migración.

**2. Docker Compose** (`docker-compose.yml`, en la raíz)
- **PostgreSQL 17 + PostGIS** en el puerto **5433**. El 5432 choca con PostgreSQL si lo tienes instalado en Windows.
- **Mailpit** para probar emails en local (`http://localhost:8025`).

**3. Configuración de Spring** (`backend/src/main/resources/`)
- `application.properties`: lo común a todos los perfiles, con `dev` como perfil por defecto.
- `application-dev.properties`: conexión a la BD local, `ddl-auto=validate` y Mailpit.
- Con `validate`, Hibernate no crea tablas: solo comprueba que la entidad coincide con la tabla. El esquema lo gestiona Flyway.

**4. Migraciones Flyway** (`backend/src/main/resources/db/migration/`)
- `V1__extensions.sql`: activa `postgis` (búsqueda por distancia) y `btree_gist` (evitar reservas solapadas).
- `V2__crear_usuario.sql`: tabla `usuario` con email único y `CHECK` de los roles válidos.
- **Regla:** una migración que ya se ha ejecutado no se edita. Los cambios van en `V3`, `V4`…

**5. `.gitignore`**
- Se ignoran los secretos (`.env`, `application-local.properties`), los logs y los archivos del sistema operativo.
- `docker-compose.yml` y `application-dev.properties` sí se suben: solo llevan credenciales locales.

### Problemas resueltos
| Error | Causa | Solución |
|---|---|---|
| `no configuration file provided` | `docker compose` lanzado desde `backend/` | Ejecutarlo desde la raíz del repo |
| `'url' attribute is not specified` | Faltaba la conexión en `application-dev.properties` | Añadir `spring.datasource.*` |
| `la autentificación password falló` | Respondía el PostgreSQL de Windows, no el contenedor | Cambiar el contenedor al puerto 5433 |

### Cómo comprobar que funciona

**Requisito:** Docker Desktop abierto, con *Engine running*.

**1. Levantar la BD** (desde la raíz del repo)
    docker compose up -d
    docker compose ps
✅ `youcare-db` aparece como `healthy`.

**2. Compilar** (desde `backend/`)
    ./mvnw compile
✅ `BUILD SUCCESS`.

**3. Arrancar la app** (desde `backend/`)
    ./mvnw spring-boot:run
✅ En el log aparecen:
- `Successfully applied N migrations`
- `Started BackendApplication`

❌ Si sale `APPLICATION FAILED TO START`, el motivo está en la sección `Description`.

**4. Ver la tabla** (otra terminal, desde la raíz)
    docker compose exec db psql -U youcare -d youcare -c "\d usuario"
✅ Aparecen las 11 columnas y las restricciones `uk_usuario_email` y `ck_usuario_rol`.

**5. Empezar con la BD vacía** (si una migración falla o queréis datos limpios)
    docker compose down -v
    docker compose up -d
⚠️ Borra todos los datos locales.

### Commits
- `refactor(backend): ajustar Usuario al modelo de datos`
- `chore: añadir docker-compose con PostGIS y Mailpit`
- `chore(backend): configurar perfiles y conexión a la BD local`
- `feat(backend): añadir migraciones de extensiones y usuario`

### Siguiente paso
Subclases `Admin` y `Cliente` con su migración `V3`.


## 23/11/2026 

### Resumen
Entidad `Admin`, constantes de validación y revisión completa del backend. Se corrigen las diferencias entre las entidades y la migración `V2`, y `Usuario` queda cerrado. La app **todavía no arranca**: faltan las tablas `cliente` y `admin` y la columna `cantidad_strikes`, que irán en `V3`.

### Qué hemos hecho

**1. Merge de `develop` en `feature/backend-setup`**
- Se integra el trabajo de Jorge (`Cliente`, latitud y longitud en `Usuario`) con el de Guillermo (Docker, perfiles, migraciones).

**2. Entidad `Admin`**
- Hereda de `Usuario` (`JOINED`) y añade `departamento`, que es opcional.
- Sigue heredando de `Usuario` porque comparte el login (email, hash, rol, activo). Si no heredara, habría dos tablas de login y emails duplicados entre ellas.

**3. `Config.java`**
- Patrones de validación: nombre y apellidos, teléfono, DNI/NIE, CIF, ciudad y dirección.
- Constantes en lugar de números mágicos en las `@Column`.

**4. Excepciones**
- `EdadMinimaException` → `ValidacionException(campo, mensaje)`: una sola excepción genérica que indica qué campo ha fallado.

**5. Longitudes iguales en Java y en SQL**
- `MAX_LONGITUD_NOMBRE` 255 → 100 y nueva `MAX_LONGITUD_APELLIDOS = 150`, igual que en `V2`.
- Dos constantes distintas para la contraseña:
  - `MAX_LONGITUD_CONTRASENIA = 64`: contraseña en claro, evita DoS con contraseñas enormes.
  - `MAX_LONGITUD_HASH_CONTRASENIA = 100`: columna del hash. `{bcrypt}` + hash ocupa 68 caracteres y no cabría en 64.
- `validate` no comprueba longitudes. Si no coinciden, el fallo sale en el primer `INSERT` largo.

**6. Ubicación fuera de `Usuario`**
- Se quitan `latitud` y `longitud`. Admin y cliente no las necesitan (minimización de datos, RGPD).
- Irán como `geography(Point, 4326)` con índice GiST en `Cuidador` y `Empresa` (Sprint 3), y en la persona cuidada o la reserva (Sprint 4).
- El cliente no guarda ubicación: el punto de búsqueda llega en cada petición (`?lat=&lng=&radioKm=`) y sale del domicilio de la persona cuidada, del GPS o de una dirección.

**7. Strikes**
- Nuevo campo `strikes` (columna `cantidad_strikes`), con `MAX_STRIKES = 3`.
- Sin setter público, para encapsular la regla:
  - `aniadirStrike()`: suma 1, con tope en 3. Al llegar a 3, bloquea la cuenta (`activo = false`).
  - `quitarStrike()`: resta 1, nunca baja de 0 y no reactiva la cuenta.
- Bloquear es automático; reactivar es una decisión del admin (trazabilidad, DSA).

**8. Teléfono**
- Obligatorio (`nullable = false`), pero no único: hay hogares que comparten teléfono. El identificador único es el email.

**9. Email normalizado**
- Nueva clase `util/EmailUtil`: `normalizar(email)` → `trim()` + `toLowerCase(Locale.ROOT)`.
- Se usa en el constructor y en `setEmail` de `Usuario`.
- Evita cuentas


## 24/11/2026

### Resumen
La app vuelve a arrancar: Flyway aplica V1–V4 y Hibernate valida `Usuario`, `Cliente` y `Admin` sin errores. Por la mañana: configuración de producción, `.gitignore` en la raíz y primer PR (`feature/backend-setup` → `develop`). Por la tarde y la noche: enums de documentos y verificación, y las entidades `Cuidador`, `Empresa` y `PersonaCuidada` con sus migraciones (rama `feature/backend-entidad-cuidador`).

### Qué hemos hecho

**1. V2 restaurada a su versión original**
- V2 estaba editada en el repo, con un `CONSTRAINT CHECK` sin nombre (sintaxis inválida).
- Se recupera la versión del commit en que se creó: `git checkout 74089a4 -- V2__crear_usuario.sql`.
- **Regla:** un archivo `V` que ya está en el repo no se edita. Flyway guarda su checksum, y cualquier cambio da `checksum mismatch`.

**2. `V3__ajustar_usuario.sql`**
- `cantidad_strikes INT NOT NULL DEFAULT 0` + `ck_usuario_strikes CHECK (>= 0)`. El máximo (3) está solo en `Config.MAX_STRIKES`.
- `telefono SET NOT NULL`.
- `ck_usuario_email_minusculas CHECK (email = lower(email))`: refuerza en la BD la normalización de `EmailUtil`.

**3. `V4__crear_cliente_y_admin.sql`**
- Tablas `cliente` y `admin`: el `id` es PK y FK hacia `usuario(id)`, con `ON DELETE CASCADE`.
- El `id` de las hijas no es `IDENTITY`: lo genera `usuario`.
- `fecha_nacimiento DATE NOT NULL`. La edad mínima se valida en Java, porque un `CHECK` con `now()` no es fiable.

**4. Longitudes en `Cliente`**
- `MAX_LONGITUD_DIRECCION = 200` y `MAX_LONGITUD_CIUDAD = 100`, usadas en las `@Column`.
- `name` en `@Column` solo cuando la columna se llama distinto al atributo (`strikes` → `cantidad_strikes`).

**5. `.gitignore` en la raíz**
- Secretos (`.env`, `.env.*`, excepto `.env.example`), `node_modules/`, Expo, claves de firma (`.jks`, `.p12`…), builds, IDEs, sistema operativo y logs.
- `backend/.gitignore` se queda para lo de Maven (`target/`).

**6. Perfiles y variables de entorno**
- `ddl-auto=validate` pasa a `application.properties`, porque vale para todos los perfiles.
- `application-prod.properties`:
  - BD y correo con `${VARIABLE}`, sin valores por defecto: si falta una variable, la app no arranca.
  - `server.port=${PORT:8080}`, porque Render asigna el puerto en `PORT`.
  - Actuator solo con `health`.
- `backend/.env.example`: los nombres de las variables, sin valores reales.

**7. Comprobaciones de la BD por terminal**
- `Comprobaciones_BD_Terminal.md`: comandos y capturas para comprobar migraciones, tablas y restricciones con `psql`.

**8. PR #1: `feature/backend-setup` → `develop`**
- Revisado y fusionado por Jorge.

**9. Enums de documentos y verificación** (Guillermo)
- `TipoDocumento`: `DNI_NIE`, `CERTIFICADO_PROFESIONAL`, `CIF`, `OTRO`.
- `EstadoDocumento`: `PENDIENTE`, `APROBADO`, `RECHAZADO`.
- `EstadoVerificacion`: `SIN_SUBIR`, `PENDIENTE`, `VERIFICADO`, `RECHAZADO`.

**10. Entidades `Cuidador`, `Empresa` y `PersonaCuidada`** (Jorge, de 23:00 a 00:55)
- `EstadoEnEmpresa` (`PENDIENTE`, `ACTIVO`, `BAJA`) y constantes nuevas en `Config`.
- Errata corregida: `TipoDoucmento` → `TipoDocumento`.
- Embebibles:
  - `Documento`: tipo, estado, clave del archivo, motivo de rechazo y fecha de subida.
  - `FranjaHoraria`: día, hora de inicio y hora de fin, con detección de solapamientos.
- `Cuidador` y `Empresa` heredan de `Usuario` (`JOINED`). `PersonaCuidada` es una entidad independiente, a cargo de un `Cliente`.
- Migraciones:
  - `V6__crear_empresa.sql`
  - `V7__crear_cuidador.sql`
  - `V8__crear_persona_cuidada.sql`
  - No hay V5: Flyway admite huecos en la numeración.
- Fix: subir cualquier documento pasaba la cuenta a `PENDIENTE`, no solo un certificado. Las listas de documentos y de franjas pasan a ser de solo lectura.

### Problemas

| Error | Causa | Solución |
|---|---|---|
| `syntax error at or near "CHECK"` en V2 | Se editó una migración ya aplicada | Restaurar V2 desde su commit; los cambios van en V3 |
| `git checkout --` no arreglaba V2 | El V2 roto ya estaba commiteado | `git checkout <commit> -- archivo` |
| `pathspec ... did not match any file` | Comando lanzado desde `backend/` con una ruta relativa a la raíz | Ejecutarlo desde la raíz |
| `backend/target/` aparecía como *untracked* | Se pegó el `.gitignore` raíz encima de `backend/.gitignore` | `git restore` y crear el archivo en la raíz |
| `prod`: `Could not resolve placeholder 'MAIL_HOST'` | **Esperado:** en local no hay variables de entorno | — (demuestra que no hay credenciales en el código) |

---

## 25/11/2026

### Resumen
Revisión de la rama `feature/backend-entidad-cuidador` y corrección de los errores encontrados, paso a paso y con tests. Primeros tests unitarios del proyecto: **45 en verde**, incluido el arranque completo con `validate` contra la BD.

### Qué hemos hecho

**1. Normalizar DNI/NIE y CIF**
- Nueva clase `util/IdentificadorUtil.normalizar()`: quita espacios y guiones y pasa a mayúsculas con `Locale.ROOT`.
- Se usa en el constructor y en el setter de `dniNie` (`Cuidador`) y de `cif` (`Empresa`).
- Antes, `12345678z` o `12345678-Z` chocaban con el `CHECK` de la BD y la API daba un error 500.

**2. Mismas reglas de documentos en `Cuidador` y `Empresa`**
- Solo el documento de identidad reactiva la verificación: `DNI_NIE` en el cuidador y `CIF` en la empresa.
- Tipos prohibidos: un cuidador no puede subir un `CIF`; una empresa no puede subir un `CERTIFICADO_PROFESIONAL`.
- Primero se valida y después se añade: si algo falla, la lista no cambia.
- `Empresa.getDocumentos()` pasa a ser de solo lectura.

**3. `verificar()` y `rechazarVerificacion()`**
- Sustituyen a `setEstadoVerificacion` en `Cuidador` y en `Empresa`.
- Solo actúan si la cuenta está en `PENDIENTE`.
- `verificar()` exige un documento de identidad en estado `APROBADO`. Sin DNI aprobado, el cuidador no aparece en las búsquedas (DSA, art. 30).

**4. `Documento.rechazar()`**
- La longitud del motivo se comprueba después del `trim()`: se mide lo mismo que se guarda.

**5. Transiciones del cuidador en la empresa**
- `setEstadoEnEmpresa` se sustituye por `vincularAEmpresa()`, `aceptarEnEmpresa()`, `darDeBajaEnEmpresa()` y `desvincularDeEmpresa()`.
- Para vincularse, la empresa tiene que estar verificada y el cuidador no puede estar activo en otra.
- `BAJA` conserva la empresa como historial.

**6. `disponibilidad` como `Set`**
- `equals`/`hashCode` en `FranjaHoraria`.
- Dos `List` en `@ElementCollection` son dos *bags*, y cargarlas juntas da `MultipleBagFetchException`. Con una basta para evitarlo.
- `Documento` sigue en `List`: cambia de estado y tiene una columna que admite `NULL`.
- Sin migración: la tabla no cambia.

**7. Tests unitarios** (JUnit, sin Spring)

| Clase | Tests |
|---|---|
| `CuidadorTest` | 23 |
| `EmpresaTest` | 8 |
| `DocumentoTest` | 6 |
| `FranjaHorariaTest` | 7 |

- Patrón *Arrange, Act, Assert* y `@BeforeEach` para que cada test empiece con un objeto nuevo.
- `BackendApplicationTests` (con Docker levantado): Flyway aplica V6–V8 y `validate` confirma que entidades y tablas coinciden.

**8. Commits separados por tipo de cambio**
- `fix`, `refactor` y `test` por separado.
- Con `git add -p` se separaron dos cambios distintos hechos en el mismo archivo (`Cuidador.java`).

### Problemas

| Error | Causa | Solución |
|---|---|---|
| `release version 25 not supported` | `~/.bashrc` fijaba `JAVA_HOME` al JDK 21 y pisaba la variable de Windows | Apuntar `JAVA_HOME` al JDK 25 en `.bashrc` (`./mvnw -v` debe decir 25) |
| `Nothing to compile` sin haber compilado | VS Code compila en la misma carpeta `target/classes`, o el archivo no estaba guardado | `git diff --stat` y `./mvnw clean compile` |
| El `grep` seguía encontrando un método ya borrado | El archivo no se había guardado en disco | Borrarlo con `sed -i` y comprobarlo con `sed -n` |
| `desvincularDeEmpresa() is undefined` | Se borró al sustituir el método de encima | Volver a añadirlo |
| `unmodifiableList` con un `Set` | Se cambió el atributo pero no el getter | `unmodifiableSet` y tipo de retorno `Set` |
| `Connection to localhost:5433 refused` | El contenedor de la BD no estaba levantado | `docker compose up -d` desde la raíz |

### Pendiente
- [ ] Javadoc de `Usuario` y `Cliente`: razón social, ubicación, `@param` que faltan y `contrasenia` → `hashContrasenia`.
- [ ] Borrar `feature/backend-entidad-usuario` sin fusionarla, después de confirmarlo con el equipo.
- [ ] Avisar al equipo de que el proyecto necesita JDK 25 y ponerlo en el README.
- [ ] Abrir el PR de `feature/backend-entidad-cuidador` → `develop`.
- [ ] Sprint 3: ubicación (`geography(Point, 4326)` + GiST), tarifa y descripción en `Cuidador`; dependencia `hibernate-spatial`. La siguiente migración es V9.