# Secret de Postgres

No se versiona un YAML con la contraseña real (mismo criterio que `.env.example`
en el backend). Crear el Secret con un comando antes de aplicar los manifiestos:

```bash
kubectl create secret generic postgres-secret \
  --namespace master-gateway-ns \
  --from-literal=POSTGRES_DB=master_gateway \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=<elige-una-contrasena>
```

El backend reutiliza la misma clave `POSTGRES_PASSWORD` de este Secret para su
propia variable `DB_PASSWORD` (ver `3-backend-deployment.yaml`), para no
duplicar la contraseña en dos Secrets distintos.
