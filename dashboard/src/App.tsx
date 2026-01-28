import React from 'react';
import { useAuthStore } from './store/authStore';
import Login from './pages/Login';
import DeviceList from './pages/DeviceList';
import RemoteControl from './components/RemoteControl';

function App() {
  const token = useAuthStore(state => state.token);
  const [selectedDevice, setSelectedDevice] = React.useState<string | null>(null);

  if (!token) {
    return <Login />;
  }

  return (
    <div className="min-h-screen bg-slate-900 text-white flex">
      {/* Sidebar Placeholder */}
      <aside className="w-64 bg-slate-950 border-r border-slate-800 hidden md:block">
        <div className="p-6">
          <h2 className="text-xl font-bold text-indigo-500">CONVERSO</h2>
        </div>
        <nav className="px-4 space-y-2">
          <div
            onClick={() => setSelectedDevice(null)}
            className={`p-3 rounded-lg cursor-pointer ${!selectedDevice ? 'bg-slate-800 text-indigo-400' : 'hover:bg-slate-900 text-slate-400'}`}
          >
            Dashboard
          </div>
          <div className="p-3 hover:bg-slate-900 rounded-lg text-slate-400 cursor-pointer">Users</div>
          <div className="p-3 hover:bg-slate-900 rounded-lg text-slate-400 cursor-pointer">Settings</div>
        </nav>
      </aside>

      <main className="flex-1 overflow-auto">
        <header className="p-4 bg-slate-950/50 border-b border-slate-800 flex justify-between items-center">
          <span className="text-slate-400">
            {selectedDevice ? `Managing Device: ${selectedDevice}` : 'Device Overview'}
          </span>
          <button
            onClick={() => useAuthStore.getState().logout()}
            className="text-sm text-slate-400 hover:text-white"
          >
            Logout
          </button>
        </header>

        {selectedDevice ? (
          <RemoteControl deviceId={selectedDevice} />
        ) : (
          <DeviceList onSelectDevice={setSelectedDevice} />
        )}
      </main>
    </div>
  );
}

export default App;
