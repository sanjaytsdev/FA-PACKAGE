# ── Build stage ─────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

# Install deps first, so this layer stays cached unless the package files change
COPY package.json package-lock.json ./
RUN npm ci

# Build the static site. VITE_API_BASE_URL gets baked in at build time and
# points at the same-origin /api path that nginx serves (see nginx.conf).
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
COPY . .
RUN npm run build

# ── Run stage ───────────────────────────────────────────────────
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
