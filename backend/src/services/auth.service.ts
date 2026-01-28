import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import prisma from './prisma.service.js';
import { AppError } from '../utils/errors.js';

const JWT_SECRET = process.env.JWT_SECRET || 'your_jwt_secret_change_me';
const SALT_ROUNDS = 10;

export const registerUser = async (data: any) => {
    const { email, password, fullName } = data;

    const existingUser = await prisma.user.findUnique({ where: { email } });
    if (existingUser) {
        throw new AppError('User already exists', 400);
    }

    const hashedPassword = await bcrypt.hash(password, SALT_ROUNDS);

    const user = await prisma.user.create({
        data: {
            email,
            passwordHash: hashedPassword,
            fullName
        }
    });

    const { passwordHash, ...userWithoutPassword } = user;
    return userWithoutPassword;
};

export const loginUser = async (email: string, password: string) => {
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) {
        throw new AppError('Invalid credentials', 401);
    }

    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);
    if (!isPasswordValid) {
        throw new AppError('Invalid credentials', 401);
    }

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, {
        expiresIn: '24h'
    });

    const { passwordHash, ...userWithoutPassword } = user;
    return { user: userWithoutPassword, token };
};

export const getUserProfile = async (userId: string) => {
    const user = await prisma.user.findUnique({
        where: { id: userId },
        select: {
            id: true,
            email: true,
            fullName: true,
            status: true,
            createdAt: true
        }
    });

    if (!user) {
        throw new AppError('User not found', 404);
    }

    return user;
};
