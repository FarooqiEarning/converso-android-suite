import { Router } from 'express';
import * as adminController from '../controllers/admin.controller.js';
import { authMiddleware, adminMiddleware } from '../middlewares/auth.middleware.js';

const router = Router();

// Protect all admin routes
router.use(authMiddleware);
router.use(adminMiddleware);

// User Management
router.get('/users', adminController.getAllUsers);
router.patch('/users/:userId/status', adminController.updateUserStatus);

// Device Management
router.get('/devices', adminController.getAllDevices);
router.patch('/devices/:deviceId/status', adminController.updateDeviceStatus);

// Billing & Payments
router.get('/payments/pending', adminController.getPendingPayments);
router.post('/payments/:paymentId/verify', adminController.verifyPayment);

// Analytics
router.get('/stats', adminController.getSystemStats);

export default router;
