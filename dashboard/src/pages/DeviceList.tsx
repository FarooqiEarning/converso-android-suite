import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { Smartphone, Plus } from 'lucide-react';
import AddDeviceModal from '../components/AddDeviceModal';

interface DeviceListProps {
    onSelectDevice: (deviceId: string) => void;
}

const DeviceList: React.FC<DeviceListProps> = ({ onSelectDevice }) => {
    const [devices, setDevices] = useState<any[]>([]);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    const fetchDevices = async () => {
        try {
            const { data } = await api.get('/devices');
            setDevices(data.data);
        } catch (err) {
            console.error('Failed to fetch devices');
        }
    };

    useEffect(() => {
        fetchDevices();
    }, []);

    return (
        <div className="p-6">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-white">Device Management</h1>
                    <p className="text-slate-400 mt-1">Manage and monitor your connected Android fleet</p>
                </div>
                <button
                    onClick={() => setIsAddModalOpen(true)}
                    className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2.5 rounded-xl font-semibold shadow-lg shadow-indigo-500/20 transition-all active:scale-95"
                >
                    <Plus size={20} /> Add Device
                </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {devices.map(device => (
                    <div
                        key={device.id}
                        onClick={() => onSelectDevice(device.deviceId)}
                        className="bg-slate-800 p-6 rounded-2xl border border-slate-700 hover:border-indigo-500/50 transition-all cursor-pointer group hover:bg-slate-800/80"
                    >
                        <div className="flex items-center justify-between mb-5">
                            <div className="p-3 bg-slate-700 rounded-xl group-hover:bg-indigo-500/10 transition-colors">
                                <Smartphone className="text-indigo-400" size={24} />
                            </div>
                            {device.isOnline ? (
                                <div className="flex items-center bg-green-500/10 px-3 py-1 rounded-full text-green-400 text-xs font-bold uppercase tracking-wider">
                                    <div className="w-1.5 h-1.5 rounded-full bg-green-400 mr-2 animate-pulse" /> Online
                                </div>
                            ) : (
                                <div className="flex items-center bg-slate-700 px-3 py-1 rounded-full text-slate-500 text-xs font-bold uppercase tracking-wider">
                                    <div className="w-1.5 h-1.5 rounded-full bg-slate-500 mr-2" /> Offline
                                </div>
                            )}
                        </div>
                        <h3 className="text-xl font-bold text-white">{device.model || 'Unknown Device'}</h3>
                        <p className="text-slate-400 text-sm mt-1">{device.manufacturer || 'Generic'} • Android {device.androidVersion || '?'}</p>

                        <div className="mt-6 pt-6 border-t border-slate-700 flex justify-between gap-3">
                            <button className="flex-1 py-2.5 text-xs bg-slate-700 hover:bg-indigo-600 rounded-lg font-bold transition-all text-slate-200">View Console</button>
                            <button className="flex-1 py-2.5 text-xs bg-slate-700 hover:bg-slate-600 rounded-lg font-bold transition-all text-slate-200">Security</button>
                        </div>
                    </div>
                ))}

                {devices.length === 0 && !isAddModalOpen && (
                    <div
                        onClick={() => setIsAddModalOpen(true)}
                        className="col-span-1 md:col-span-2 lg:col-span-3 border-2 border-dashed border-slate-700 rounded-2xl p-12 flex flex-col items-center justify-center hover:border-indigo-500/50 transition-all cursor-pointer group"
                    >
                        <div className="p-4 bg-slate-800 rounded-full mb-4 group-hover:scale-110 transition-transform">
                            <Plus size={32} className="text-slate-500 group-hover:text-indigo-400" />
                        </div>
                        <h3 className="text-lg font-bold text-slate-300">No devices paired yet</h3>
                        <p className="text-slate-500 mt-1">Click to add your first Android device</p>
                    </div>
                )}
            </div>

            <AddDeviceModal
                isOpen={isAddModalOpen}
                onClose={() => setIsAddModalOpen(false)}
            />
        </div>
    );
};

export default DeviceList;
