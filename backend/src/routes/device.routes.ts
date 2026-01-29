import { Router } from 'express';
import { getRegistrationPayload, postTelemetry, registerDevice } from '../controllers/device.controller.js';
import { authMiddleware } from '../middlewares/auth.middleware.js';

const router = Router();

router.use(authMiddleware);

router.post('/register', registerDevice);
router.get('/registration-payload', getRegistrationPayload);
router.post('/telemetry', postTelemetry);

export default router;
