import prisma from './prisma.service.js';
import logger from '../utils/logger.js';

/**
 * Background Workers Service
 * Handles recurring system tasks like subscription expiry and data archival.
 */
class WorkersService {
    private interval: NodeJS.Timeout | null = null;

    start() {
        logger.info('Enterprise Workers started.');

        // Run core loops
        this.interval = setInterval(async () => {
            try {
                await this.checkExpirations();
                await this.cleanupData();
            } catch (error) {
                logger.error(error, 'Worker loop error:');
            }
        }, 1000 * 60 * 60); // Run every hour

        // Run immediately on start
        this.checkExpirations();
    }

    stop() {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
    }

    /**
     * Automatically transition active subscriptions to EXPIRED 
     * and PAUSE devices if their endDate has passed.
     */
    async checkExpirations() {
        logger.info('Running subscription expiry check...');
        const now = new Date();

        // Find all active subscriptions that have passed their end date
        const expiredSubscriptions = await prisma.subscription.findMany({
            where: {
                endDate: { lt: now },
                status: 'ACTIVE'
            },
            include: {
                device: true
            }
        });

        for (const sub of expiredSubscriptions) {
            try {
                await prisma.$transaction([
                    // 1. Update subscription status
                    prisma.subscription.update({
                        where: { id: sub.id },
                        data: { status: 'EXPIRED' }
                    }),
                    // 2. Pause the device
                    prisma.device.update({
                        where: { id: sub.deviceId },
                        data: { status: 'PAUSED' }
                    }),
                    // 3. Notify the user
                    prisma.notification.create({
                        data: {
                            userId: sub.userId,
                            deviceId: sub.deviceId,
                            type: 'SUBSCRIPTION_EXPIRING',
                            title: 'Device Paused',
                            message: `The subscription for "${sub.device.name || sub.deviceId}" has expired. Service has been paused.`
                        }
                    })
                ]);
                logger.info(`Subscription ${sub.id} expired and device ${sub.deviceId} paused.`);
            } catch (err) {
                logger.error(err, `Failed to expire sub ${sub.id}:`);
            }
        }
    }

    /**
     * Purge old telemetry and command logs to keep the database lean.
     * Standard retention policy: 30 days.
     */
    async cleanupData() {
        logger.info('Archiving old telemetry data...');
        const thirtyDaysAgo = new Date();
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

        try {
            const deletedTelemetry = await prisma.deviceTelemetry.deleteMany({
                where: { timestamp: { lt: thirtyDaysAgo } }
            });

            const deletedCommands = await prisma.commandLog.deleteMany({
                where: { executedAt: { lt: thirtyDaysAgo } }
            });

            logger.info(`Cleanup complete: Removed ${deletedTelemetry.count} telemetry rows and ${deletedCommands.count} command logs.`);
        } catch (err) {
            logger.error(err, 'Cleanup worker failed:');
        }
    }
}

export const workersService = new WorkersService();
