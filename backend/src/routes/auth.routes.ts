import { Router } from 'express';
import { login, register, me } from '../controllers/auth.controller.js';
import { authMiddleware } from '../middlewares/auth.middleware.js';
import { validateRequest } from '../middlewares/validation.middleware.js';
import { loginSchema, registerSchema } from '../schemas/auth.schema.js';

const router = Router();

router.post('/register', validateRequest(registerSchema), register);
router.post('/login', validateRequest(loginSchema), login);
router.get('/me', authMiddleware, me);

export default router;
