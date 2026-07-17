# Master Gateway

Sistema de Autenticación y Autorización Centralizado (microservicio maestro) para un ecosistema de microservicios.

- **Backend**: Spring Boot + Hibernate/JPA + PostgreSQL (`/backend`)
- **Frontend**: React + Vite + React Router + Tailwind CSS (`/frontend`)

## Correr el frontend en desarrollo

```
cd frontend
cp .env.example .env   # ajusta VITE_API_BASE_URL si el backend no corre en localhost:8080
npm install
npm run dev
```

Requiere el backend corriendo en paralelo (con `CORS_ALLOWED_ORIGIN=http://localhost:5173`, que ya es el valor por defecto). El flujo implementado: `/login` → `/select-role` (Workspace Selector, obligatorio) → `/app` con sidebar y rutas construidas dinámicamente desde `GET /api/menus/tree` (sin rutas hardcodeadas).

**Nota:** no existe todavía un usuario sembrado en la base de datos ni un endpoint público de registro (`POST /api/users` está protegido), así que para probar el login hace falta insertar manualmente un usuario con contraseña hasheada en BCrypt directamente en Postgres.

## Estrategia de ramas

- `main`: producción. Inmutable salvo PRs desde `test`. Un merge aquí dispara el despliegue automático.
- `test`: QA. Los PRs hacia `main` nacen de aquí.
- `dev`: integración de features. Ramas `feature/*` se crean desde `dev` y regresan a `dev` via PR.

## Pipeline CI/CD (`.github/workflows/ci-cd.yml`)

Push a `dev` → PR automático a `test` → (seguridad ML → SonarCloud + JUnit/JaCoCo en paralelo) → merge automático a `test` → merge automático a `main` → build Docker + deploy a Render, con notificaciones a Telegram en cada nodo.

El análisis de seguridad ML (carpeta [`/seguridad`](seguridad)) reutiliza un modelo Random Forest pre-entrenado sobre Java (CWE/Juliet) del proyecto P2; expone un microservicio FastAPI (`api_modelo.py`) que debe estar desplegado aparte, y un script de CI (`evaluar_pr.py`) que lo consume por cada PR.

### Secrets requeridos en GitHub (Settings → Secrets and variables → Actions)

| Secret | Uso |
|---|---|
| `PAT_AUTOMATION` | Token con permisos de repo para que el PR automático `dev → test` dispare el resto del pipeline. |
| `GITHUB_TOKEN` | Provisto automáticamente por GitHub Actions. |
| `TELEGRAM_TOKEN` / `TELEGRAM_CHAT_ID` | Bot de notificaciones (BotFather). |
| `MODELO_API_URL` | URL pública del microservicio FastAPI del modelo ML (`/seguridad/api_modelo.py` desplegado en Railway/Render). |
| `SONAR_TOKEN` / `SONAR_ORGANIZATION` / `SONAR_PROJECT_KEY` | Credenciales de SonarCloud para el Quality Gate. |
| `JWT_SECRET_TEST` | Secret JWT dummy usado solo para que el `ApplicationContext` levante en los tests (no es el secret de producción). |
| `DOCKER_USERNAME` / `DOCKER_PASSWORD` | Credenciales de Docker Hub para publicar la imagen `master-gateway-backend`. |
| `RENDER_DEPLOY_HOOK_URL` | Deploy Hook del servicio en Render (dispara el redeploy desde el pipeline, no por webhook automático). |
| `RENDER_SERVICE_URL` | URL pública del servicio en Render, usada para el health-check post-deploy. |

Las credenciales reales de base de datos y el `JWT_SECRET` de producción se inyectan como variables de entorno directamente en Render (nunca en el repo).
