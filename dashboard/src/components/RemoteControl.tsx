import React, { useEffect, useRef, useState } from 'react';
import { socketService } from '../services/socket';
import {
    Lock, Power, RotateCw, Volume2, VolumeX,
    Smartphone, Wifi, Activity,
} from 'lucide-react';

interface RemoteControlProps {
    deviceId: string;
}

const RemoteControl: React.FC<RemoteControlProps> = ({ deviceId }) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const [latency, setLatency] = useState<number>(0);
    const [streaming, setStreaming] = useState(false);

    useEffect(() => {
        socketService.on('SCREEN_FRAME', (data: any) => {
            if (data.deviceId === deviceId) {
                const img = new Image();
                img.onload = () => {
                    const ctx = canvasRef.current?.getContext('2d');
                    ctx?.drawImage(img, 0, 0, canvasRef.current?.width || 0, canvasRef.current?.height || 0);
                    setStreaming(true);
                };
                img.src = `data:image/jpeg;base64,${data.frame}`;
                setLatency(Date.now() - data.timestamp);
            }
        });

        return () => {
            // Cleanup listener if needed
        };
    }, [deviceId]);

    const sendCommand = (type: string, params: any = {}) => {
        socketService.emit('SEND_COMMAND', {
            deviceId,
            command: { type, params, timestamp: Date.now() }
        });
    };

    return (
        <div className="p-6 h-full flex flex-col">
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-4">
                    <div className="p-3 bg-indigo-500/10 rounded-xl">
                        <Smartphone className="text-indigo-400" size={24} />
                    </div>
                    <div>
                        <h2 className="text-xl font-bold text-white">Remote Console</h2>
                        <div className="flex items-center gap-3 mt-1">
                            <span className="flex items-center text-xs text-slate-400">
                                <Wifi size={12} className="mr-1 text-green-400" /> Connected
                            </span>
                            <span className="flex items-center text-xs text-slate-400">
                                <Activity size={12} className="mr-1 text-indigo-400" /> {latency}ms
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <div className="flex-1 flex gap-6 overflow-hidden">
                {/* Device Screen View */}
                <div className="flex-1 bg-black rounded-3xl border-8 border-slate-800 shadow-2xl relative overflow-hidden flex items-center justify-center">
                    {!streaming && (
                        <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-900/50 backdrop-blur-sm z-10">
                            <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-500 border-t-transparent mb-4"></div>
                            <p className="text-slate-300 font-medium">Waiting for device stream...</p>
                        </div>
                    )}
                    <canvas
                        ref={canvasRef}
                        className="h-full w-auto object-contain cursor-crosshair"
                        width={1080}
                        height={1920}
                        onMouseDown={(e) => {
                            const rect = canvasRef.current?.getBoundingClientRect();
                            if (!rect) return;
                            const x = (e.clientX - rect.left) * (1080 / rect.width);
                            const y = (e.clientY - rect.top) * (1920 / rect.height);
                            sendCommand('CLICK', { x, y });
                        }}
                    />
                </div>

                {/* Control Panel */}
                <div className="w-80 flex flex-col gap-4">
                    <div className="bg-slate-800 p-6 rounded-2xl border border-slate-700">
                        <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Hardware Controls</h3>
                        <div className="grid grid-cols-2 gap-3">
                            <ControlBtn icon={Power} label="Power" onClick={() => sendCommand('power')} color="red" />
                            <ControlBtn icon={Lock} label="Lock" onClick={() => sendCommand('lock')} />
                            <ControlBtn icon={RotateCw} label="Reboot" onClick={() => sendCommand('reboot')} />
                            <ControlBtn icon={RotateCw} label="Screenshot" onClick={() => sendCommand('screenshot')} />
                        </div>

                        <div className="mt-6 pt-6 border-t border-slate-700">
                            <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Audio</h3>
                            <div className="grid grid-cols-2 gap-3">
                                <ControlBtn icon={Volume2} label="Vol Up" onClick={() => sendCommand('vol_up')} />
                                <ControlBtn icon={VolumeX} label="Mute" onClick={() => sendCommand('mute')} />
                            </div>
                        </div>
                    </div>

                    <div className="bg-slate-800 p-6 rounded-2xl border border-slate-700 flex-1">
                        <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Device Info</h3>
                        <div className="space-y-4">
                            <InfoRow label="OS" value="Android 14" />
                            <InfoRow label="Memory" value="12GB RAM" />
                            <InfoRow label="Storage" value="256GB" />
                            <InfoRow label="Battery" value="85%" color="green" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

const ControlBtn = ({ icon: Icon, label, onClick, color = 'indigo' }: any) => (
    <button
        onClick={onClick}
        className={`flex flex-col items-center gap-2 p-4 rounded-xl border border-slate-700 hover:border-${color}-500 bg-slate-900/50 transition-all hover:bg-slate-700 group`}
    >
        <Icon size={20} className={`text-slate-400 group-hover:text-${color}-400`} />
        <span className="text-xs font-semibold text-slate-400 group-hover:text-white">{label}</span>
    </button>
);

const InfoRow = ({ label, value, color = 'slate' }: any) => (
    <div className="flex justify-between items-center text-sm">
        <span className="text-slate-500">{label}</span>
        <span className={`font-medium text-${color}-400`}>{value}</span>
    </div>
);

export default RemoteControl;
