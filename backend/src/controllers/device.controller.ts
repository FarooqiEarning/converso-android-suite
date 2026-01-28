import { Request, Response, NextFunction } from 'express';
import * as deviceService from '../services/device.service.js';
import prisma from '../services/prisma.service.js';
import jwt from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || 'your_jwt_secret_change_me';

export const getDevices = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userId = (req as any).user.id;
        const devices = await deviceService.listDevices(userId);
        res.json({ success: true, data: devices });
    } catch (error) {
        next(error);
    }
};

export const registerDevice = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const { registrationToken, deviceId, ...deviceDetails } = req.body;

        if (!registrationToken) {
            return res.status(400).json({ success: false, message: 'Registration token required' });
        }

        const decoded = jwt.verify(registrationToken, JWT_SECRET) as any;
        if (decoded.type !== 'REGISTRATION') {
            return res.status(401).json({ success: false, message: 'Invalid token type' });
        }

        const device = await deviceService.registerDevice(decoded.userId, {
            deviceId,
            ...deviceDetails
        });

        res.status(201).json({ success: true, data: device });
    } catch (error) {
        if (error instanceof jwt.JsonWebTokenError) {
            return res.status(401).json({ success: false, message: 'Invalid or expired registration token' });
        }
        next(error);
    }
};

export const getRegistrationPayload = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const user = (req as any).user;

        // Dynamic IP detection: DB -> ENV -> Request
        const vpsConfig = await prisma.globalConfig.findUnique({ where: { key: 'VPS_IP' } });
        const masterIp = vpsConfig?.value || process.env.PUBLIC_URL || `http://${req.hostname}:${process.env.PORT || 9081}`;

        const payload = {
            serverUrl: masterIp.startsWith('http') ? masterIp : `http://${masterIp}:9081`,
            userId: user.id,
            userName: user.fullName || user.email,
            registrationToken: jwt.sign({ userId: user.id, type: 'REGISTRATION' }, JWT_SECRET, { expiresIn: '1h' }),
            timestamp: Date.now()
        };
        res.json({ success: true, data: payload });
    } catch (error) {
        next(error);
    }
};

export const postTelemetry = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const { deviceId, batteryLevel, cpuUsage, ramUsedMb, ramTotalMb, networkType, ipAddress } = req.body;

        const telemetry = await prisma.deviceTelemetry.create({
            data: {
                deviceId,
                batteryLevel,
                cpuUsage,
                ramUsedMb,
                ramTotalMb,
                networkType,
                ipAddress,
                timestamp: new Date()
            }
        });

        // Also update the device's lastOnline status
        await prisma.device.update({
            where: { deviceId },
            data: {
                lastOnline: new Date(),
                isOnline: true
            }
        });

        res.json({ success: true, data: telemetry });
    } catch (error) {
        next(error);
    }
};
