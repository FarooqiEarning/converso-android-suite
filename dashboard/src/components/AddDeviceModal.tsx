import React, { useEffect, useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import api from '../services/api';
import { X, Copy, RefreshCw, Smartphone } from 'lucide-react';

interface AddDeviceModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const AddDeviceModal: React.FC<AddDeviceModalProps> = ({ isOpen, onClose }) => {
    const [payload, setPayload] = useState<any>(null);
    const [loading, setLoading] = useState(false);

    const fetchPayload = async () => {
        setLoading(true);
        try {
            const { data } = await api.get('/devices/registration-payload');
            setPayload(data.data);
        } catch (err) {
            console.error('Failed to fetch registration payload');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            fetchPayload();
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const qrValue = payload ? JSON.stringify(payload) : '';

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl overflow-hidden">
                <div className="p-6 border-b border-slate-800 flex justify-between items-center bg-slate-900/50">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-indigo-500/10 rounded-lg">
                            <Smartphone className="text-indigo-400" size={20} />
                        </div>
                        <h2 className="text-xl font-bold text-white">Add New Device</h2>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-slate-800 rounded-lg transition-colors text-slate-400 hover:text-white">
                        <X size={20} />
                    </button>
                </div>

                <div className="p-8 flex flex-col items-center text-center">
                    <p className="text-slate-400 mb-8 max-w-sm">
                        Scan this QR code using the Converso Mobile App on your device to start the pairing process.
                    </p>

                    <div className="p-6 bg-white rounded-3xl shadow-inner mb-8">
                        {loading ? (
                            <div className="w-64 h-64 flex items-center justify-center bg-slate-50 transition-all rounded-2xl">
                                <RefreshCw className="animate-spin text-indigo-500" size={40} />
                            </div>
                        ) : (
                            <QRCodeSVG
                                value={qrValue}
                                size={256}
                                level="M"
                                includeMargin={false}
                            />
                        )}
                    </div>

                    <div className="grid grid-cols-2 gap-4 w-full">
                        <button
                            onClick={fetchPayload}
                            className="flex items-center justify-center gap-2 p-3 bg-slate-800 hover:bg-slate-700 rounded-xl transition-all text-sm font-semibold text-white"
                        >
                            <RefreshCw size={16} /> Refresh Code
                        </button>
                        <button
                            onClick={() => {
                                navigator.clipboard.writeText(qrValue);
                            }}
                            className="flex items-center justify-center gap-2 p-3 bg-slate-800 hover:bg-slate-700 rounded-xl transition-all text-sm font-semibold text-white"
                        >
                            <Copy size={16} /> Copy JSON
                        </button>
                    </div>
                </div>

                <div className="p-6 bg-indigo-600/5 border-t border-slate-800 text-center">
                    <p className="text-xs text-indigo-400 font-medium">Valid for 60 minutes • High Security Link</p>
                </div>
            </div>
        </div>
    );
};

export default AddDeviceModal;
