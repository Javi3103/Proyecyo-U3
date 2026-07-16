# Secret del backend

```bash
kubectl create secret generic backend-secret \
  --namespace master-gateway-ns \
  --from-literal=JWT_SECRET=$(openssl rand -base64 32)
```
