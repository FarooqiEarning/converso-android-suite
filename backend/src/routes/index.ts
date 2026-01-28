import { Router } from 'express';
import authRoutes from './auth.routes.js';
import deviceRoutes from './device.routes.js';
import subscriptionRoutes from './subscription.routes.js';
import adminRoutes from './admin.routes.js';
import systemRoutes from './system.routes.js';

const router = Router();

router.use('/auth', authRoutes);
router.use('/devices', deviceRoutes);
router.use('/subscriptions', subscriptionRoutes);
router.use('/admin', adminRoutes);
router.use('/system', systemRoutes);

// Health check
router.get('/health', (req, res) => {
    res.json({ status: 'up', timestamp: new Date() });
});

export default router;
