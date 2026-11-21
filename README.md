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

| Rama | Sale de | Se fusiona en | Para qué se usa |
|---|---|---|---|
| `main` | — | — | Código en producción. Cada merge despliega automáticamente (Render, Netlify). **Nunca se trabaja directamente aquí** |
| `develop` | `main` | — | Integración. Aquí se juntan todas las funcionalidades terminadas. Siempre tiene que compilar y arrancar |
| `feature/<area>-<descripcion>` | `develop` | `develop` | Una rama por tarea de Trello. Se borra al fusionarla |
| `release/v<versión>` | `develop` | `main` y `develop` | Se crea al cerrar cada fase: últimos ajustes, versión y etiqueta |
| `hotfix/<descripcion>` | `main` | `main` y `develop` | Corrección urgente de un error en producción |

### Nombres de las ramas `feature`

El prefijo indica la parte del proyecto:

| Área | Ejemplo |
|---|---|
| `backend` | `feature/backend-auth-jwt` |
| `mobile` | `feature/mobile-search-screen` |
| `admin` | `feature/admin-caregiver-verification` |
| `web` | `feature/web-legal-pages` |
| `db` | `feature/db-init-schema` |
| `infra` | `feature/infra-docker-render` |
| `docs` | `feature/docs-domain-model` |

### Normas

1. **Nadie hace push directo a `main` ni a `develop`.** Todo entra por Pull Request.
2. Cada PR la **revisa y aprueba otra persona** del equipo antes de fusionarla.
3. Las features se fusionan con **Squash and merge**: un commit por tarea en `develop`.
4. Antes de abrir la PR: `develop` actualizado en tu rama, todo compila y los tests pasan.
5. Nunca se suben credenciales. Las variables reales van en `.env` (ignorado por Git); en el repo solo está `.env.example`.

### Mensajes de commit

Formato [Conventional Commits](https://www.conventionalcommits.org/es/):

| Prefijo | Uso | Ejemplo |
|---|---|---|
| `feat:` | Nueva funcionalidad | `feat: registro de cuidadores con verificación de email` |
| `fix:` | Corrección de error | `fix: solape de reservas en el mismo horario` |
| `docs:` | Documentación | `docs: actualizar modelo de dominio` |
| `refactor:` | Cambio de código sin cambiar comportamiento | `refactor: extraer mapper de Booking` |
| `test:` | Tests | `test: casos de cancelación con reembolso` |
| `chore:` | Configuración, dependencias | `chore: añadir Flyway al backend` |

### Versiones

Una versión por fase de la hoja de ruta:

| Versión | Fase |
|---|---|
| `v0.1.0` | Fase 1 — Análisis y diseño |
| `v0.2.0` | Fase 2 — Autenticación y usuarios |
| `v0.3.0` | Fase 3 — Búsqueda y geolocalización |
| `v0.4.0` | Fase 4 — Reservas y pagos |
| `v0.5.0` | Fase 5 — Valoraciones, chat y panel admin |
| `v1.0.0` | Fase 6 — Entrega del TFG |