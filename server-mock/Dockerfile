# ==============================================================================
# RadioAuto ES - Dockerfile del Servidor Mock (Puerto 4112)
# Imagen ligera basada en Node.js Alpine sin dependencias externas pesadas
# ==============================================================================

FROM node:20-alpine

# Directorio de trabajo en el contenedor
WORKDIR /app

# Copiar archivos del servidor, catálogo JSON y frontend web estático
COPY server.js ./
COPY stations.json ./
COPY default-stations.json ./
COPY public ./public

# Exponer estrictamente el puerto 4112
EXPOSE 4112

# Variables de entorno
ENV NODE_ENV=production
ENV PORT=4112

# Comando de inicio del servidor HTTP
CMD ["node", "server.js"]
