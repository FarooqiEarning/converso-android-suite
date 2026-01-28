import { Request, Response, NextFunction } from 'express';
import prisma from '../services/prisma.service.js';
import logger from '../utils/logger.js';

/**
 * Admin Controller
 * Handles platform-wide oversight, user/device management, and billing verification.
 */

// --- USER MANAGEMENT ---

export const getAllUsers = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const users = await prisma.user.findMany({
            include: {
                _count: { select: { devices: true } }
            },
            orderBy: { createdAt: 'desc' }
        });
        res.json({ success: true, data: users });
    } catch (error) {
        next(error);
    }
};

export const updateUserStatus = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const { userId } = req.params;
        const { status } = req.body; // ACTIVE, SUSPENDED, DELETED

        const user = await prisma.user.update({
            where: { id: userId },
            data: { status }
        });

        logger.info(`Admin updated user ${userId} status to ${status}`);
        res.json({ success: true, data: user });
    } catch (error) {
        next(error);
    }
};

// --- DEVICE MANAGEMENT ---

export const getAllDevices = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const devices = await prisma.device.findMany({
            include: {
                user: { select: { email: true, fullName: true } },
                subscriptions: { where: { status: 'ACTIVE' }, take: 1 }
            },
            orderBy: { registeredAt: 'desc' }
        });
        res.json({ success: true, data: devices });
    } catch (error) {
        next(error);
    }
};

export const updateDeviceStatus = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const { deviceId } = req.params;
        const { status } = req.body; // ACTIVE, PAUSED, BLOCKED, DELETED

        const device = await prisma.device.update({
            where: { id: deviceId },
            data: { status }
        });

        logger.info(`Admin updated device ${deviceId} status to ${status}`);
        res.json({ success: true, data: device });
    } catch (error) {
        next(error);
    }
};

// --- BILLING / PAYMENTS ---

export const getPendingPayments = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const payments = await prisma.payment.findMany({
            where: { status: 'PENDING' },
            include: {
                user: { select: { email: true } },
                subscription: true
            },
            orderBy: { paymentDate: 'asc' }
        });
        res.json({ success: true, data: payments });
    } catch (error) {
        next(error);
    }
};

export const verifyPayment = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const { paymentId } = req.params;
        const { approved, notes } = req.body;
        const adminId = (req as any).user.id;

        const payment = await prisma.payment.findUnique({
            where: { id: paymentId },
            include: { subscription: true }
        });

        if (!payment) {
            return res.status(404).json({ success: false, message: 'Payment not found' });
        }

        const updatedPayment = await prisma.$transaction(async (tx: any) => {
            const p = await tx.payment.update({
                where: { id: paymentId },
                data: {
                    status: approved ? 'VERIFIED' : 'REJECTED',
                    verifiedBy: adminId,
                    verifiedAt: new Date(),
                    notes
                }
            });

            if (approved) {
                // Extend subscription by 30 days
                const currentEnd = payment.subscription.endDate > new Date()
                    ? payment.subscription.endDate
                    : new Date();

                const newEndDate = new Date(currentEnd);
                newEndDate.setDate(newEndDate.getDate() + 30);

                await tx.subscription.update({
                    where: { id: payment.subscriptionId },
                    data: {
                        endDate: newEndDate,
                        status: 'ACTIVE',
                        paymentStatus: 'PAID'
                    }
                });

                // Ensure device is active
                await tx.device.update({
                    where: { id: payment.subscription.deviceId },
                    data: { status: 'ACTIVE' }
                });

                // Notify user
                await tx.notification.create({
                    data: {
                        userId: payment.userId,
                        type: 'PAYMENT_REMINDER',
                        title: 'Payment Verified',
                        message: `Your payment was verified. Subscription extended to ${newEndDate.toLocaleDateString()}.`
                    }
                });
            }

            return p;
        });

        res.json({ success: true, data: updatedPayment });
    } catch (error) {
        next(error);
    }
};

// --- ANALYTICS ---

export const getSystemStats = async (req: Request, res: Response, next: NextFunction) => {
    try {
        const [userCount, deviceCount, activeSubscriptions, pendingPayments] = await Promise.all([
            prisma.user.count(),
            prisma.device.count(),
            prisma.subscription.count({ where: { status: 'ACTIVE' } }),
            prisma.payment.count({ where: { status: 'PENDING' } })
        ]);

        res.json({
            success: true,
            data: {
                totalUsers: userCount,
                totalDevices: deviceCount,
                activeSubscriptions,
                pendingPayments,
                revenuePotential: activeSubscriptions * 25
            }
        });
    } catch (error) {
        next(error);
    }
};
