import { Request, Response, NextFunction } from 'express';
import prisma from '../services/prisma.service.js';
import { AppError } from '../utils/errors.js';

/**
 * Subscription Controller
 * Handles user-facing subscription management and billing.
 */

export const getMySubscriptions = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userId = (req as any).user.id;
        const subscriptions = await prisma.subscription.findMany({
            where: { userId },
            include: {
                device: { select: { name: true, model: true } }
            },
            orderBy: { endDate: 'desc' }
        });
        res.json({ success: true, data: subscriptions });
    } catch (error) {
        next(error);
    }
};

export const createSubscriptionSlot = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userId = (req as any).user.id;
        const { deviceId } = req.body;

        // Check if device belongs to user
        const device = await prisma.device.findUnique({
            where: { id: deviceId }
        });

        if (!device || device.userId !== userId) {
            return next(new AppError('Device not found or access denied', 404));
        }

        // Check if subscription already exists for this device
        const existing = await prisma.subscription.findUnique({
            where: { deviceId }
        });

        if (existing) {
            return res.status(400).json({ success: false, message: 'Subscription already exists for this device' });
        }

        // Create a new subscription starting now, expiring in 30 days
        // status is PAUSED until payment is verified
        const subscription = await prisma.subscription.create({
            data: {
                userId,
                deviceId,
                startDate: new Date(),
                endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
                status: 'PAUSED',
                paymentStatus: 'PENDING',
                price: 25.00
            }
        });

        res.status(201).json({ success: true, data: subscription });
    } catch (error) {
        next(error);
    }
};

export const uploadPaymentProof = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userId = (req as any).user.id;
        const { subscriptionId, amount, paymentMethod, proofUrl, notes } = req.body;

        const subscription = await prisma.subscription.findUnique({
            where: { id: subscriptionId }
        });

        if (!subscription || subscription.userId !== userId) {
            return next(new AppError('Subscription not found', 404));
        }

        const payment = await prisma.payment.create({
            data: {
                userId,
                subscriptionId,
                amount: amount || 25.00,
                paymentMethod,
                proofUrl,
                notes,
                status: 'PENDING'
            }
        });

        res.status(201).json({ success: true, data: payment });
    } catch (error) {
        next(error);
    }
};

export const getPaymentHistory = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const userId = (req as any).user.id;
        const payments = await prisma.payment.findMany({
            where: { userId },
            include: {
                subscription: { include: { device: { select: { name: true } } } }
            },
            orderBy: { paymentDate: 'desc' }
        });
        res.json({ success: true, data: payments });
    } catch (error) {
        next(error);
    }
};
