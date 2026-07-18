# Master Gateway

Sistema de Autenticación y Autorización Centralizado (microservicio maestro) para un ecosistema de microservicios — Proyecto Integrador Parcial III, Desarrollo de Software Seguro (ESPE).

Centraliza identidad, roles y navegación para que los microservicios hijos (ej. un futuro módulo de Ventas) no necesiten su propia base de usuarios: delegan en este servicio la autenticación y la validación de cada request (Zero Trust).

- **Backend**: Spring Boot + Hibernate/JPA + PostgreSQL (`/backend`)
- **Frontend**: React + Vite + React Router + Tailwind CSS (`/frontend`)
- **Análisis de seguridad (SAST propio)**: FastAPI + modelo Random Forest (`/seguridad`)

## Arquitectura y objetivos cumplidos

| Objetivo | Requisito | Implementación |
|---|---|---|
| OE1 | Relación M:N Usuario-Rol | Tabla pivote `usuario_rol` auditada (no es una tabla "tonta"): [`UsuarioRol.java`](backend/src/main/java/ec/edu/espe/mastergateway/acceso/UsuarioRol.java) hereda `estado`/`fecha_creacion` para trazar cuándo se otorgó o revocó un rol |
| OE2 | Menú recursivo en una sola tabla | [`Menu.java`](backend/src/main/java/ec/edu/espe/mastergateway/menu/Menu.java) (patrón Adjacency List vía `parent_id`), recorrido con `WITH RECURSIVE` nativo en [`MenuRepository.java`](backend/src/main/java/ec/edu/espe/mastergateway/menu/MenuRepository.java) — evita el problema N+1 |
| OE3 | Selección activa de rol tras el login | Flujo en dos pasos: `POST /api/auth/login` (TempToken + lista de roles) → `POST /api/auth/select-role` (JWT definitivo, Least Privilege) en [`AuthService.java`](backend/src/main/java/ec/edu/espe/mastergateway/auth/AuthService.java) |
| OE4 | Arquitectura Zero Trust para microservicios hijos | `POST /api/internals/validate-token` en [`InternalTokenController.java`](backend/src/main/java/ec/edu/espe/mastergateway/auth/InternalTokenController.java); todo endpoint exige JWT salvo 4 rutas públicas explícitas en [`SecurityConfig.java`](backend/src/main/java/ec/edu/espe/mastergateway/security/SecurityConfig.java) |
| OE5 | Shift-Left: pruebas, validación, anti-SQLi, hash | 56 pruebas unitarias de seguridad (`backend/src/test/java`), validación de entradas con Bean Validation, ORM parametrizado (incluso la consulta nativa recursiva usa `@Param`), contraseñas con BCrypt costo 12 |

### Decisiones de seguridad clave

- **Least Privilege real**: el JWT emitido en `/select-role` solo lleva los permisos del rol elegido, nunca los de otros roles que el usuario tenga asignados.
- **Detección de reuso de Refresh Token**: si un token ya canjeado se reenvía (señal de robo), se revoca de inmediato toda la cadena de sesión de ese usuario+rol.
- **Login timing-safe**: no revela si falló el email o la contraseña (usa un hash "dummy" cuando el email no existe, para igualar el tiempo de respuesta), con rate limiting de 5 intentos / 15 min.
- **Soft delete en todas las entidades** vía `@SQLDelete`/`@SQLRestriction` de Hibernate — nunca hay un `DELETE` físico, salvo en la desasignación de la tabla pivote rol-usuario (que el propio requisito pide como eliminación física).
- **Contraseñas nunca en texto plano**: verificado en pruebas unitarias que el repositorio solo persiste el hash de BCrypt.

## Correr el frontend en desarrollo

```
cd frontend
cp .env.example .env   # ajusta VITE_API_BASE_URL si el backend no corre en localhost:8080
npm install
npm run dev
```

Requiere el backend corriendo en paralelo (con `CORS_ALLOWED_ORIGIN=http://localhost:5173`, que ya es el valor por defecto). El flujo implementado: `/login` → `/select-role` (Workspace Selector, obligatorio) → `/app` con sidebar y rutas construidas dinámicamente desde `GET /api/menus/tree` (sin rutas hardcodeadas).

**Nota:** no existe todavía un usuario sembrado en la base de datos ni un endpoint público de registro (`POST /api/users` está protegido), así que para probar el login hace falta insertar manualmente un usuario con contraseña hasheada en BCrypt directamente en Postgres.

## Pruebas

```
cd backend
./mvnw clean test
```

56 pruebas de seguridad cubren el flujo de autenticación completo (`AuthServiceTest`), el filtro Zero Trust (`JwtAuthenticationFilterTest`), el hash de refresh tokens (`TokenHasherTest`), reglas de negocio de roles/usuarios (`RolServiceTest`, `UsuarioServiceTest`) y la política de contraseñas (`CreateUsuarioRequestValidationTest`).

## Estrategia de ramas

- `main`: producción. Inmutable salvo Pull Requests desde `test` — protegida en GitHub (Settings → Rules), sin excepciones ni para administradores. Un merge aquí dispara la publicación de la imagen Docker.
- `test`: QA. Los PRs hacia `main` nacen de aquí (creados y mergeados automáticamente por el pipeline).
- `dev`: integración de features. Ramas `feature/*` se crean desde `dev` y regresan a `dev` vía PR.

## Pipeline CI/CD (`.github/workflows/ci-cd.yml`)

Push a `dev` → PR automático a `test` → (seguridad ML → SonarCloud + JUnit/JaCoCo en paralelo) → merge automático a `test` → merge automático a `main` (vía Pull Request, nunca push directo) → build y publicación de la imagen Docker en Docker Hub, con notificaciones a Telegram en cada nodo. El despliegue final a producción es manual, vía Kubernetes/Minikube (ver [`k8s/README.md`](k8s/README.md)) — no hay servicio PaaS público al que el pipeline despliegue automáticamente.

SonarCloud corre en modo informativo (`sonar.qualitygate.wait=false`): el plan Free no incluye evaluación de Quality Gate sobre PRs/branches (solo la rama `main`), así que el gate de seguridad real que bloquea el merge es el Nodo 1 (modelo ML propio).

El análisis de seguridad ML (carpeta [`/seguridad`](seguridad)) reutiliza un modelo Random Forest pre-entrenado sobre Java (CWE/Juliet); expone un microservicio FastAPI (`api_modelo.py`) desplegado en Railway, y un script de CI (`evaluar_pr.py`) que lo consume por cada PR, comentando el PR y creando un Issue automático si detecta código vulnerable.

### Secrets requeridos en GitHub (Settings → Secrets and variables → Actions)

| Secret | Uso |
|---|---|
| `PAT_AUTOMATION` | Token con permisos de repo para que el PR automático `dev → test` dispare el resto del pipeline. |
| `GITHUB_TOKEN` | Provisto automáticamente por GitHub Actions. |
| `TELEGRAM_TOKEN` / `TELEGRAM_CHAT_ID` | Bot de notificaciones (BotFather). |
| `MODELO_API_URL` | URL pública del microservicio FastAPI del modelo ML (`/seguridad/api_modelo.py` desplegado en Railway/Render). |
| `SONAR_TOKEN` / `SONAR_ORGANIZATION` / `SONAR_PROJECT_KEY` | Credenciales de SonarCloud para el análisis estático. |
| `JWT_SECRET_TEST` | Secret JWT dummy usado solo para que el `ApplicationContext` levante en los tests (no es el secret de producción). |
| `DOCKER_USERNAME` / `DOCKER_PASSWORD` | Credenciales de Docker Hub para publicar la imagen `master-gateway-backend`. |

Las credenciales reales de base de datos y el `JWT_SECRET` de producción se crean como Kubernetes Secrets en el clúster de Minikube (nunca en el repo) — ver [`k8s/README.md`](k8s/README.md).

## Despliegue en Kubernetes

Backend, frontend, modelo ML y PostgreSQL corren como Deployments independientes en un clúster de Minikube, expuestos por un único Ingress (`mastergateway.local`). Instrucciones completas, incluyendo las particularidades de Windows (`kubectl port-forward` en vez de `minikube tunnel`), en [`k8s/README.md`](k8s/README.md).
