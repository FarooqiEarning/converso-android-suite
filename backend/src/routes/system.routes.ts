import { Router } from 'express';
import * as systemController from '../controllers/system.controller.js';
import { authMiddleware } from '../middlewares/auth.middleware.js';
// Note: In production, there should be an adminMiddleware here
// For now, using authMiddleware for simplicity

const router = Router();

router.use(authMiddleware);

router.get('/config', systemController.getGlobalConfig);
router.post('/config', systemController.updateGlobalConfig);
router.get('/stats', systemController.getSystemStats);

export default router;
