import { Router } from 'express';
import * as subController from '../controllers/subscription.controller.js';
import { authMiddleware } from '../middlewares/auth.middleware.js';

const router = Router();

router.use(authMiddleware);

router.get('/my', subController.getMySubscriptions);
router.post('/add-slot', subController.createSubscriptionSlot);
router.post('/upload-proof', subController.uploadPaymentProof);
router.get('/payments', subController.getPaymentHistory);

export default router;
