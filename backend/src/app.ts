import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import http from 'http';
import { Server } from 'socket.io';
import { createAdapter } from '@socket.io/redis-adapter';
import { createClient } from 'redis';
import apiRoutes from './routes/index.js';
import { errorHandler } from './middlewares/error.middleware.js';
import logger from './utils/logger.js';

const app = express();
const server = http.createServer(app);

// Security Middleware
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

// Rate Limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_MAX || '100')
});
app.use('/api/', limiter);

app.use(express.json());

// Request logging
app.use((req, res, next) => {
  logger.info({ method: req.method, url: req.url, ip: req.ip }, 'Incoming request');
  next();
});

// Routes
app.use('/api', apiRoutes);

// WebSocket Setup with Redis Scaling
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

if (process.env.REDIS_URL) {
  const pubClient = createClient({ url: process.env.REDIS_URL });
  const subClient = pubClient.duplicate();

  Promise.all([pubClient.connect(), subClient.connect()]).then(() => {
    io.adapter(createAdapter(pubClient, subClient));
    logger.info('Socket.io Redis adapter initialized');
  }).catch(err => {
    logger.error({ err }, 'Failed to initialize Redis adapter');
  });
}

app.set('io', io);

// Enterprise WebSocket Service
import { WebSocketService } from './services/websocket/websocket.service.js';
const wsService = new WebSocketService(io);
app.set('wsService', wsService);

// 404 handler
app.use((req, res) => {
  res.status(404).json({ success: false, message: `Route ${req.originalUrl} not found` });
});

// Global Error Handler
app.use(errorHandler);

export default server;
