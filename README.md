# Master Gateway

Sistema de Autenticación y Autorización Centralizado (microservicio maestro) para un ecosistema de microservicios.

- **Backend**: Spring Boot + Hibernate/JPA + PostgreSQL (`/backend`)
- **Frontend**: React + React Router (`/frontend`)

## Estrategia de ramas

- `main`: producción. Inmutable salvo PRs desde `test`. Un merge aquí dispara el despliegue automático.
- `test`: QA. Los PRs hacia `main` nacen de aquí.
- `dev`: integración de features. Ramas `feature/*` se crean desde `dev` y regresan a `dev` via PR.
