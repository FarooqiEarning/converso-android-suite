import { ConnectionManager } from './connection-manager.js';

export class CommandRelay {
    private io: any;

    constructor(private connectionManager: ConnectionManager) { }

    setIO(io: any) {
        this.io = io;
    }

    dispatchCommand(deviceId: string, command: any) {
        const socketId = this.connectionManager.getDeviceSocket(deviceId);
        if (socketId && this.io) {
            this.io.to(socketId).emit('COMMAND', command);
            return true;
        }
        return false;
    }
}
