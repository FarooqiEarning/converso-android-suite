import React, { useEffect, useState } from 'react';
import { Settings, Users, Monitor, ShieldCheck, Activity, Globe, Save, RefreshCw } from 'lucide-react';
import api from './services/api';

const AdminApp: React.FC = () => {
    const [vpsIp, setVpsIp] = useState('');
    const [stats, setStats] = useState<any>(null);
    const [loading, setLoading] = useState(false);

    const fetchData = async () => {
        try {
            const [configRes, statsRes] = await Promise.all([
                api.get('/system/config'),
                api.get('/system/stats')
            ]);
            setVpsIp(configRes.data.data.VPS_IP || '');
            setStats(statsRes.data.data);
        } catch (err) {
            console.error('Admin sync failed');
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleUpdateIp = async () => {
        setLoading(true);
        try {
            await api.post('/system/config', { key: 'VPS_IP', value: vpsIp });
            alert('VPS IP Updated Successfully!');
            await fetchData();
        } catch (err) {
            alert('Update failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-950 text-slate-200 flex">
            {/* Sidebar */}
            <aside className="w-72 bg-slate-900 border-r border-slate-800 flex flex-col">
                <div className="p-8">
                    <h1 className="text-2xl font-black text-white tracking-tight italic">CONVERSO<span className="text-indigo-500 underline">ADMIN</span></h1>
                </div>

                <nav className="flex-1 px-4 space-y-2">
                    <NavItem icon={Activity} label="System Overview" active />
                    <NavItem icon={Users} label="User Management" />
                    <NavItem icon={Monitor} label="Global Devices" />
                    <NavItem icon={ShieldCheck} label="Security Audit" />
                    <NavItem icon={Settings} label="Server Settings" />
                </nav>
            </aside>

            {/* Main Content */}
            <main className="flex-1 p-12 overflow-auto">
                <header className="mb-12">
                    <h2 className="text-4xl font-bold text-white mb-2">Global Settings</h2>
                    <p className="text-slate-400 text-lg">Manage enterprise configuration and VPS orchestration</p>
                </header>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
                    {/* Server Config Card */}
                    <section className="bg-slate-900 p-8 rounded-3xl border border-slate-800 shadow-2xl relative overflow-hidden">
                        <div className="absolute top-0 right-0 p-8 opacity-5">
                            <Globe size={120} />
                        </div>

                        <h3 className="text-xl font-bold text-white mb-8 flex items-center gap-3">
                            <span className="p-2 bg-indigo-500/10 rounded-xl text-indigo-400"><Globe size={24} /></span>
                            Network Orchestration
                        </h3>

                        <div className="space-y-6">
                            <div>
                                <label className="block text-sm font-semibold text-slate-500 mb-2 uppercase tracking-widest">Master VPS IPv4</label>
                                <div className="flex gap-4">
                                    <input
                                        type="text"
                                        value={vpsIp}
                                        onChange={(e) => setVpsIp(e.target.value)}
                                        placeholder="123.456.78.90"
                                        className="flex-1 bg-slate-800 border-2 border-slate-700 rounded-2xl p-4 text-white font-mono focus:border-indigo-500 outline-none transition-all"
                                    />
                                    <button
                                        onClick={handleUpdateIp}
                                        disabled={loading}
                                        className="bg-indigo-600 hover:bg-indigo-500 text-white px-8 rounded-2xl font-bold flex items-center gap-2 transition-all disabled:opacity-50"
                                    >
                                        {loading ? <RefreshCw className="animate-spin" size={18} /> : <Save size={18} />} Update
                                    </button>
                                </div>
                                <p className="mt-3 text-sm text-slate-500">This IP will be pushed to all registered Android devices for WebSocket relay.</p>
                            </div>

                            <div className="pt-8 border-t border-slate-800">
                                <label className="block text-sm font-semibold text-slate-500 mb-2 uppercase tracking-widest">Relay Cluster Status</label>
                                <div className="flex items-center justify-between p-4 bg-slate-800/50 rounded-2xl border border-slate-800">
                                    <div className="flex items-center gap-3">
                                        <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
                                        <span className="font-bold text-white">{stats?.activeDevices || 0} Devices Online</span>
                                    </div>
                                    <button className="text-indigo-400 hover:text-white transition-colors flex items-center gap-2 text-sm font-bold">
                                        <RefreshCw size={14} /> Restart Cluster
                                    </button>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* System Health Card */}
                    <section className="bg-slate-900 p-8 rounded-3xl border border-slate-800 shadow-2xl">
                        <h3 className="text-xl font-bold text-white mb-8">Resources</h3>
                        <div className="space-y-8">
                            <HealthBar label="Backend Load" value={stats?.cpu || 0} color="indigo" />
                            <HealthBar label="Memory Usage" value={stats?.memory || 0} color="green" />
                            <HealthBar label="Active Connections" value={stats?.activeDevices || 0} color="blue" />
                            <HealthBar label="System Users" value={stats?.dbConnections || 0} color="purple" />
                        </div>
                    </section>
                </div>
            </main>
        </div>
    );
};

const NavItem = ({ icon: Icon, label, active = false }: any) => (
    <button className={`w-full flex items-center gap-4 px-6 py-4 rounded-2xl transition-all font-semibold ${active ? 'bg-indigo-600/10 text-indigo-400 border border-indigo-600/20' : 'text-slate-500 hover:text-white hover:bg-slate-800'}`}>
        <Icon size={22} /> {label}
    </button>
);

const HealthBar = ({ label, value, color }: any) => (
    <div className="space-y-2">
        <div className="flex justify-between text-sm font-bold">
            <span className="text-slate-400 uppercase tracking-widest">{label}</span>
            <span className="text-white">{value}%</span>
        </div>
        <div className="h-3 bg-slate-800 rounded-full overflow-hidden">
            <div className={`h-full bg-${color}-500 transition-all duration-1000`} style={{ width: `${value}%` }} />
        </div>
    </div>
);

export default AdminApp;
