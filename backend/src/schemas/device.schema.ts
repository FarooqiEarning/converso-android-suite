import { z } from 'zod';

export const registerDeviceSchema = z.object({
    body: z.object({
        deviceId: z.string(),
        manufacturer: z.string().optional(),
        model: z.string().optional(),
        androidVersion: z.string().optional(),
    }),
});
