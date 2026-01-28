import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { AppError } from '../utils/errors.js';
import prisma from '../services/prisma.service.js';

const JWT_SECRET = process.env.JWT_SECRET || 'your_jwt_secret_change_me';

export const authMiddleware = (req: Request, res: Response, next: NextFunction) => {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return next(new AppError('No token provided', 401));
    }

    const token = authHeader.split(' ')[1];

    try {
        const decoded = jwt.verify(token, JWT_SECRET) as any;
        (req as any).user = decoded;
        next();
    } catch (error) {
        next(new AppError('Unauthorized access', 401));
    }
};

export const adminMiddleware = async (req: Request, res: Response, next: NextFunction) => {
    const user = (req as any).user;
    if (!user) return next(new AppError('Unauthorized', 401));

    try {
        const admin = await prisma.adminUser.findUnique({
            where: { userId: user.id }
        });

        if (!admin) {
            return next(new AppError('Forbidden: Admin access only', 403));
        }

        (req as any).admin = admin;
        next();
    } catch (error) {
        next(error);
    }
};
