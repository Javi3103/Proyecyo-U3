# Despliegue en Kubernetes (Minikube)

## Resumen para la exposición

**Por qué está esto aquí:** el PDF de referencia del proyecto pide un PaaS gratuito (Railway/Render) para el despliegue automático. Kubernetes fue un requisito **adicional**, pedido aparte por el profesor (guía "Instalación de Kubernetes en Windows 11 con Minikube"). No reemplaza nada del PDF — demuestra una capacidad extra: que el sistema puede operar como un ecosistema real de microservicios orquestados, no solo como una app monolítica en un solo contenedor.

**Arquitectura desplegada** — 4 Deployments independientes en el namespace `master-gateway-ns`, cada uno con su propio Service, detrás de un único Ingress (`mastergateway.local`):

| Componente | Rol |
|---|---|
| `postgres` | Base de datos, con PVC para persistencia |
| `master-gateway-backend` | El microservicio maestro (Spring Boot) |
| `master-gateway-frontend` | SPA de React servida por nginx |
| `master-gateway-ml` | Modelo de seguridad (uso interno del pipeline, no expuesto por el Ingress) |

**Cómo esto refuerza los conceptos del PDF que ya defendiste:**

- **OE4 (arquitectura lista para futuros microservicios):** cada componente corre aislado en su propio Pod/Deployment/Service — si mañana se agrega un "Módulo de Ventas" real, sería literalmente un Deployment + Service más en el mismo clúster, consumiendo `/api/internals/validate-token` del backend ya desplegado. Kubernetes es la prueba física de que la arquitectura de microservicios que pide el PDF funciona más allá de la teoría.
- **Zero Trust no cambia con la infraestructura:** el backend sigue validando el JWT en cada request exactamente igual que corriendo localmente — Kubernetes no reemplaza ni debilita esa capa, solo cambia dónde corre el proceso.
- **Gestión segura de Secrets (6.3 del PDF):** `JWT_SECRET` y las credenciales de Postgres se crean como Kubernetes Secrets (`kubectl create secret`), nunca están en los manifiestos versionados en git ni en las imágenes Docker.
- **Decisión de diseño clave, útil si preguntan:** el `Ingress` (`6-ingress.yaml`) se hizo **sin** la anotación `rewrite-target` que usa el ejemplo del profesor — porque el backend espera rutas literales (`/api/auth/login`), y reescribirlas a `/` rompería toda la API con 404. Es una adaptación consciente, no un descuido.

**Qué mostrar en vivo:**
1. `kubectl get pods -n master-gateway-ns` — los 4 pods `Running`.
2. La app funcionando en `http://mastergateway.local` (login → selección de rol → dashboard), igual que en local, pero corriendo desde contenedores orquestados.
3. (Opcional, si preguntan) `kubectl describe secret backend-secret -n master-gateway-ns` — muestra que el valor no es legible en texto plano desde `describe`, reforzando la gestión segura de secrets.

---

Sigue el mismo flujo enseñado en la guía del curso (`minikube start --driver=docker` → habilitar Ingress → manifiestos `.yaml` separados → `kubectl apply` → dominio `.local`).

## 0. Construir y subir las imágenes a Docker Hub

```bash
# Backend
cd backend
./mvnw package -DskipTests
docker build -t javi3103/master-gateway-backend:latest .
docker push javi3103/master-gateway-backend:latest

# Frontend
cd ../frontend
docker build -t javi3103/master-gateway-frontend:latest .
docker push javi3103/master-gateway-frontend:latest

# Modelo ML de seguridad
cd ../seguridad
docker build -t javi3103/master-gateway-ml:latest .
docker push javi3103/master-gateway-ml:latest
```

## 1. Levantar el clúster

```bash
minikube start --driver=docker
minikube addons enable ingress

# Verificar que el Ingress Controller esté corriendo
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx
```

## 2. Crear los Secrets (no versionados en git)

Ver `2-postgres-secret-instrucciones.md` y `3-backend-secret-instrucciones.md`.

```bash
kubectl create secret generic postgres-secret \
  --namespace master-gateway-ns \
  --from-literal=POSTGRES_DB=master_gateway \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=<elige-una-contrasena>

kubectl create namespace master-gateway-ns --dry-run=client -o yaml | kubectl apply -f -
# (el namespace también se crea al aplicar 1-namespace.yaml; si el Secret falla
# por "namespace not found", aplica primero 1-namespace.yaml)

kubectl create secret generic backend-secret \
  --namespace master-gateway-ns \
  --from-literal=JWT_SECRET=$(openssl rand -base64 32)
```

## 3. Aplicar los manifiestos

```bash
kubectl apply -f 1-namespace.yaml
kubectl apply -f 2-postgres-pvc.yaml
kubectl apply -f 2-postgres-deployment.yaml
kubectl apply -f 2-postgres-service.yaml
kubectl apply -f 3-backend-configmap.yaml
kubectl apply -f 3-backend-deployment.yaml
kubectl apply -f 3-backend-service.yaml
kubectl apply -f 4-frontend-deployment.yaml
kubectl apply -f 4-frontend-service.yaml
kubectl apply -f 5-ml-deployment.yaml
kubectl apply -f 5-ml-service.yaml
kubectl apply -f 6-ingress.yaml

# Verificar que todo quede Running
kubectl get pods -n master-gateway-ns -w
```

## 4. Acceder desde el navegador

**Importante en Windows con `--driver=docker`:** ni `minikube ip` ni `minikube tunnel` funcionan de forma directa aquí — `minikube ip` no es alcanzable desde el host (el driver Docker en Windows no expone la IP del nodo directamente), y `minikube tunnel` solo sirve para Services tipo `LoadBalancer`, pero el Service del Ingress Controller que crea el addon es tipo `NodePort`. La forma que sí funciona:

```powershell
# En una terminal de PowerShell COMO ADMINISTRADOR (el puerto 80 requiere privilegios
# elevados en Windows) — déjala abierta mientras usas la app:
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 80:80
```

Luego, en **otra** terminal como administrador, agrega el dominio local (apuntando a tu propia máquina, porque el `port-forward` ya reenvía el tráfico ahí):

```powershell
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "`n127.0.0.1`tmastergateway.local"
```

Abre `http://mastergateway.local`. El frontend fue construido con `VITE_API_BASE_URL=http://mastergateway.local` (sin puerto) horneado en el bundle, así que **tiene que ser el puerto 80** — si usas otro puerto para el port-forward, las llamadas del frontend a `/api` fallarán porque irán a un origen distinto.

**Si el Ingress/DNS no responde de ningún modo**, usa `port-forward` directo a un servicio como último respaldo (igual que enseña la guía):

```bash
kubectl port-forward -n master-gateway-ns svc/master-gateway-frontend 8000:80
kubectl port-forward -n master-gateway-ns svc/master-gateway-backend 8080:8080
```
Y accede a `http://localhost:8000` (nota: con port-forward puro, sin Ingress, las llamadas del frontend a `/api` necesitarían apuntar a `http://localhost:8080` en vez de mismo-origen — es solo para diagnóstico, no para uso normal).

## 5. Sembrar un usuario de prueba

Como no hay endpoint público de registro, el primer usuario se siembra a mano en el Postgres del clúster (`../seed.sql`, en la raíz del repo — crea el rol "Vendedor", el módulo "Ventas" con un ítem de menú, y un usuario `vendedor@mastergateway.local` asignado a ese rol; es aditivo, no toca ningún usuario admin que ya exista). En Windows, `kubectl cp` falla con rutas tipo `C:\...` (el `:` del disco confunde el parser de `kubectl cp`, que espera `namespace/pod:ruta`) — usa `kubectl exec` con la entrada estándar en su lugar:

```powershell
kubectl exec -i -n master-gateway-ns deployment/postgres -- psql -U postgres -d master_gateway < ..\seed.sql
```

## Comandos útiles de diagnóstico

```bash
kubectl get ns master-gateway-ns
kubectl get pods -n master-gateway-ns
kubectl get deploy -n master-gateway-ns
kubectl get svc -n master-gateway-ns
kubectl get ingress -n master-gateway-ns
kubectl get all -n master-gateway-ns
kubectl logs -n master-gateway-ns deployment/master-gateway-backend
```

## Después de reconstruir una imagen

`imagePullPolicy: Always` solo vuelve a jalar la imagen cuando el Pod se recrea, no automáticamente. Tras un nuevo `docker push`:

```bash
kubectl rollout restart deployment/master-gateway-backend -n master-gateway-ns
```
