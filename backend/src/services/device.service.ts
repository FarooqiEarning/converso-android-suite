import prisma from './prisma.service.js';
import { AppError } from '../utils/errors.js';

export const listDevices = async (userId: string) => {
    return await prisma.device.findMany({
        where: { userId },
        orderBy: { lastOnline: 'desc' }
    });
};

export const registerDevice = async (userId: string, data: any) => {
    const { deviceId, manufacturer, model, androidVersion } = data;

    const device = await prisma.device.upsert({
        where: { deviceId },
        update: {
            userId,
            manufacturer,
            model,
            androidVersion,
            lastOnline: new Date(),
            isOnline: true
        },
        create: {
            deviceId,
            userId,
            manufacturer,
            model,
            androidVersion,
            isOnline: true
        },
        include: {
            subscriptions: {
                where: { status: 'ACTIVE' },
                take: 1
            }
        }
    });

    return device;
};

export const getDeviceById = async (deviceId: string, userId: string) => {
    const device = await prisma.device.findFirst({
        where: { deviceId, userId }
    });

    if (!device) {
        throw new AppError('Device not found', 404);
    }

    return device;
};
