import { io, Socket } from 'socket.io-client';

class SocketService {
    private socket: Socket | null = null;

    connect(token: string) {
        if (this.socket?.connected) return;

        this.socket = io(import.meta.env.VITE_SOCKET_URL || 'https://backend-as.conversoempire.world', {
            auth: { token },
            reconnection: true,
            reconnectionAttempts: Infinity,
            reconnectionDelay: 1000,
            reconnectionDelayMax: 5000,
            timeout: 20000,
        });

        this.socket.on('connect', () => {
            console.log('%c[Socket] Connected to Relay Server', 'color: #10b981; font-weight: bold');
        });

        this.socket.on('disconnect', (reason) => {
            console.log(`%c[Socket] Disconnected: ${reason}`, 'color: #f59e0b');
        });

        this.socket.on('connect_error', (error) => {
            console.error('[Socket] Connection Error:', error.message);
        });
    }

    getSocket() {
        return this.socket;
    }

    emit(event: string, data: any) {
        if (this.socket?.connected) {
            this.socket.emit(event, data);
        } else {
            console.warn(`[Socket] Cannot emit ${event}: socket not connected`);
        }
    }

    on(event: string, callback: (data: any) => void) {
        this.socket?.on(event, callback);
    }

    off(event: string) {
        this.socket?.off(event);
    }
}

export const socketService = new SocketService();
