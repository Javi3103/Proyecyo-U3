# ── Stage 1: build de la SPA con Vite ──────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Vite hornea las variables VITE_* en el bundle en tiempo de build, no en
# tiempo de ejecucion -- por eso se pasa como build-arg, no como env var del
# contenedor final.
ARG VITE_API_BASE_URL=http://mastergateway.local
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ── Stage 2: servir el build estatico con nginx ────────────────────────────
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
