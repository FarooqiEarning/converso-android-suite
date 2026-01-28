import { Request, Response, NextFunction } from 'express';
import prisma from '../services/prisma.service.js';

export const getGlobalConfig = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const configs = await prisma.globalConfig.findMany();
        const configMap = configs.reduce((acc, curr) => ({ ...acc, [curr.key]: curr.value }), {});
        res.json({ success: true, data: configMap });
    } catch (error) {
        next(error);
    }
};

export const updateGlobalConfig = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const { key, value } = req.body;
        const config = await prisma.globalConfig.upsert({
            where: { key },
            update: { value },
            create: { key, value }
        });
        res.json({ success: true, data: config });
    } catch (error) {
        next(error);
    }
};

export const getSystemStats = async (req: Request, res: Response, next: NextFunction) => {
    try {
        // Mocking system stats for the dashboard
        const stats = {
            cpu: Math.floor(Math.random() * 20) + 5,
            memory: Math.floor(Math.random() * 30) + 10,
            dbConnections: await prisma.user.count(),
            activeDevices: await prisma.device.count({ where: { isOnline: true } })
        };
        res.json({ success: true, data: stats });
    } catch (error) {
        next(error);
    }
};
