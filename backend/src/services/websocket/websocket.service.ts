import { Server, Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import prisma from '../prisma.service.js';
import logger from '../../utils/logger.js';
import { ConnectionManager } from './connection-manager.js';
import { CommandRelay } from './command-relay.js';

const JWT_SECRET = process.env.JWT_SECRET || 'your_jwt_secret_change_me';

export class WebSocketService {
    private connectionManager: ConnectionManager;
    private commandRelay: CommandRelay;

    constructor(private io: Server) {
        this.connectionManager = new ConnectionManager();
        this.commandRelay = new CommandRelay(this.connectionManager);
        this.setupMiddleware();
        this.setupHandlers();
    }

    private setupMiddleware() {
        this.io.use(async (socket, next) => {
            const { token, type, deviceId } = socket.handshake.auth;

            if (type === 'DASHBOARD') {
                try {
                    const decoded = jwt.verify(token, JWT_SECRET) as any;
                    socket.data.userId = decoded.id;
                    socket.data.type = 'DASHBOARD';
                    return next();
                } catch (err) {
                    return next(new Error('Authentication failed'));
                }
            }

            if (type === 'DEVICE') {
                if (!deviceId) return next(new Error('Device ID required'));
                socket.data.deviceId = deviceId;
                socket.data.type = 'DEVICE';
                return next();
            }

            next(new Error('Invalid connection type'));
        });
    }

    private setupHandlers() {
        this.io.on('connection', async (socket: Socket) => {
            const { type } = socket.data;
            logger.info({ socketId: socket.id, type }, 'New WebSocket connection');

            if (type === 'DASHBOARD') {
                const userId = socket.data.userId;
                this.connectionManager.registerUserSocket(userId, socket.id);

                // Admin-specific: join global room or specific device rooms
                const admin = await prisma.adminUser.findUnique({ where: { userId } });
                if (admin) {
                    socket.data.isAdmin = true;
                    socket.join('ADMIN_GLOBAL');
                    logger.info({ userId }, 'Admin joined WebSocket God-mode');
                }

                socket.on('JOIN_DEVICE', (deviceId) => {
                    socket.join(`DEVICE_ROOM_${deviceId}`);
                });

                socket.on('SEND_COMMAND', async (data) => {
                    // Check ownership or admin status
                    if (!socket.data.isAdmin) {
                        const device = await prisma.device.findUnique({ where: { deviceId: data.deviceId } });
                        if (!device || device.userId !== userId) {
                            return socket.emit('ERROR', { message: 'Unauthorized command' });
                        }
                    }
                    this.commandRelay.dispatchCommand(data.deviceId, data.command);
                });

                socket.on('disconnect', () => {
                    this.connectionManager.removeUserSocket(userId, socket.id);
                });

            } else if (type === 'DEVICE') {
                const deviceId = socket.data.deviceId;
                this.connectionManager.registerDeviceSocket(deviceId, socket.id);

                socket.on('COMMAND_RESULT', async (result) => {
                    const device = await prisma.device.findUnique({ where: { deviceId } });
                    if (device) {
                        // Notify owner
                        this.connectionManager.notifyUser(device.userId, 'COMMAND_RESULT', {
                            deviceId,
                            ...result
                        });
                        // Notify Admins viewing this device
                        this.io.to(`DEVICE_ROOM_${deviceId}`).emit('COMMAND_RESULT', { deviceId, ...result });
                    }
                });

                socket.on('SCREEN_FRAME', async (frameData) => {
                    const device = await prisma.device.findUnique({ where: { deviceId } });
                    if (device) {
                        // Notify owner
                        this.connectionManager.notifyUser(device.userId, 'SCREEN_FRAME', {
                            deviceId,
                            frame: frameData.frame,
                            timestamp: Date.now()
                        });
                        // Notify Admins/Users in the device room
                        this.io.to(`DEVICE_ROOM_${deviceId}`).emit('SCREEN_FRAME', {
                            deviceId,
                            frame: frameData.frame,
                            timestamp: Date.now()
                        });
                    }
                });

                socket.on('telemetry', async (data) => {
                    // Update DB and broadcast
                    this.io.to(`DEVICE_ROOM_${deviceId}`).emit('TELEMETRY_UPDATE', data);
                });

                socket.on('disconnect', () => {
                    this.connectionManager.removeDeviceSocket(deviceId);
                });
            }
        });
    }
}
