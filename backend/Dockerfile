# Production Dockerfile for Converso Backend
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests
COPY package*.json ./
COPY prisma ./prisma/

# Install all dependencies (including dev)
RUN npm install

# Copy source
COPY . .

# Build TypeScript
RUN npm run build

# Production Image
FROM node:20-alpine

WORKDIR /app

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/prisma ./prisma
COPY --from=builder /app/ecosystem.config.cjs ./

ENV NODE_ENV=production
ENV PORT=9081

EXPOSE 9081

# Start with PM2
RUN npm install -g pm2
CMD ["pm2-runtime", "ecosystem.config.cjs"]
