# Production Dockerfile for Converso Dashboard
FROM node:20-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Servable via Nginx or Serve
FROM node:20-alpine
WORKDIR /app
RUN npm install -g serve
COPY --from=builder /app/dist ./dist

ENV PORT=9082
EXPOSE 9082

CMD ["serve", "-s", "dist", "-l", "9082"]
