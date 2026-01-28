export class ConnectionManager {
    private userSockets: Map<string, Set<string>> = new Map();
    private deviceSockets: Map<string, string> = new Map();
    private io: any;

    setIO(io: any) {
        this.io = io;
    }

    registerUserSocket(userId: string, socketId: string) {
        if (!this.userSockets.has(userId)) {
            this.userSockets.set(userId, new Set());
        }
        this.userSockets.get(userId)!.add(socketId);
    }

    removeUserSocket(userId: string, socketId: string) {
        this.userSockets.get(userId)?.delete(socketId);
    }

    registerDeviceSocket(deviceId: string, socketId: string) {
        this.deviceSockets.set(deviceId, socketId);
    }

    removeDeviceSocket(deviceId: string) {
        this.deviceSockets.delete(deviceId);
    }

    getDeviceSocket(deviceId: string): string | undefined {
        return this.deviceSockets.get(deviceId);
    }

    notifyUser(userId: string, event: string, data: any) {
        const sockets = this.userSockets.get(userId);
        if (sockets && this.io) {
            sockets.forEach(sid => {
                this.io.to(sid).emit(event, data);
            });
        }
    }
}
