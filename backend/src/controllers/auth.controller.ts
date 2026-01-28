import { Request, Response, NextFunction } from 'express';
import * as authService from '../services/auth.service.js';
import { AppError } from '../utils/errors.js';

export const register = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const user = await authService.registerUser(req.body);
    res.status(201).json({
      success: true,
      message: 'User registered successfully',
      data: user
    });
  } catch (error) {
    next(error);
  }
};

export const login = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { email, password } = req.body;
    const result = await authService.loginUser(email, password);
    res.json({
      success: true,
      message: 'Login successful',
      data: result
    });
  } catch (error) {
    next(error);
  }
};

export const me = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const userId = (req as any).user.id;
    const user = await authService.getUserProfile(userId);
    res.json({
      success: true,
      data: user
    });
  } catch (error) {
    next(error);
  }
};
