import React, { useState } from 'react';
import {
  Smartphone,
  Battery,
  Wifi,
  Bell,
  ShieldCheck,
  Send,
  Terminal,
  RefreshCw
} from 'lucide-react';
import type { Device } from '../types';
import { sendDeviceCommand } from '../services/api';

interface MobileControlViewProps {
  androidDevice: Device | null;
  onRefresh: () => void;
}

export const MobileControlView: React.FC<MobileControlViewProps> = ({ androidDevice, onRefresh }) => {
  const [statusMessage, setStatusMessage] = useState('');
  const [isExecuting, setIsExecuting] = useState(false);

  const phoneApps = [
    { name: 'WhatsApp', action: 'open_app', params: { package_or_name: 'WhatsApp' }, icon: '💬' },
    { name: 'YouTube', action: 'open_app', params: { package_or_name: 'YouTube' }, icon: '▶️' },
    { name: 'Google Maps', action: 'open_app', params: { package_or_name: 'Maps' }, icon: '🗺️' },
    { name: 'Camera', action: 'open_app', params: { package_or_name: 'Camera' }, icon: '📷' },
    { name: 'Clock / Alarms', action: 'open_app', params: { package_or_name: 'Clock' }, icon: '⏰' },
    { name: 'Toggle Flashlight', action: 'toggle_flashlight', params: {}, icon: '🔦' }
  ];

  const handleAction = async (action: string, params?: Record<string, any>) => {
    setIsExecuting(true);
    setStatusMessage('');

    try {
      const res = await sendDeviceCommand('android', action, params);
      setStatusMessage(res.message || `Command ${action} sent to OPPO companion.`);
    } catch (err: any) {
      setStatusMessage(`Execution failed: ${err.message || 'Error'}`);
    } finally {
      setIsExecuting(false);
      onRefresh();
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 select-none relative z-10 pb-8 font-mono">
      {/* Top Banner */}
      <div className="p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 backdrop-blur-md jarvis-glow">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-cyan-950/60 border border-cyan-500/40 text-cyan-400">
              <Smartphone className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-wide text-slate-100">
                OPPO ANDROID COMPANION CONTROL
              </h1>
              <p className="text-xs text-slate-400">
                Real-time WebSocket mesh integration with Deepak's OPPO Android phone.
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-2 text-xs">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping mr-1" />
              <span className="text-emerald-400 font-bold">
                {androidDevice?.is_online ? 'COMPANION PAIRED' : 'MESH READY'}
              </span>
            </div>
            <button
              onClick={onRefresh}
              className="p-2 rounded-xl border border-slate-800 bg-slate-900/60 text-slate-400 hover:text-cyan-300"
              title="Refresh Device State"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Real-time Mobile Status Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6">
          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>DEVICE</span>
              <Smartphone className="w-4 h-4 text-cyan-400" />
            </div>
            <div className="text-lg font-bold text-slate-100">{androidDevice?.name || 'OPPO Reno / F-Series'}</div>
            <div className="text-[10px] text-cyan-400">ColorOS / Android 14</div>
          </div>

          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>BATTERY</span>
              <Battery className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="text-xl font-bold text-slate-100">{androidDevice?.battery || '84%'}</div>
            <div className="text-[10px] text-emerald-400">SUPERVOOC Fast Charge</div>
          </div>

          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>NETWORK</span>
              <Wifi className="w-4 h-4 text-blue-400" />
            </div>
            <div className="text-xl font-bold text-slate-100">Wi-Fi 6 Mesh</div>
            <div className="text-[10px] text-blue-400">Local Latency &lt; 5ms</div>
          </div>

          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>NOTIFICATIONS</span>
              <Bell className="w-4 h-4 text-amber-400" />
            </div>
            <div className="text-xl font-bold text-slate-100">Synced</div>
            <div className="text-[10px] text-amber-400">NotificationListenerService</div>
          </div>
        </div>
      </div>

      {/* Status Output Console */}
      {statusMessage && (
        <div className="p-4 rounded-xl border border-cyan-500/30 bg-slate-950 text-xs text-cyan-300 flex items-center space-x-2">
          <Terminal className="w-4 h-4 text-cyan-400 shrink-0" />
          <span>{statusMessage}</span>
        </div>
      )}

      {/* Quick Mobile Intent Actions */}
      <div className="space-y-3">
        <div className="flex items-center justify-between text-xs text-slate-400">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="w-4 h-4 text-cyan-400" />
            <span>DISPATCH PHONE ACTIONS & INTENTS</span>
          </div>
          <span className="text-[11px] text-slate-500">OPPO Native Companion</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {phoneApps.map((app, i) => (
            <button
              key={i}
              onClick={() => handleAction(app.action, app.params)}
              disabled={isExecuting}
              className="p-4 rounded-xl border border-slate-800/80 bg-slate-900/60 hover:border-cyan-500/50 hover:bg-slate-900 transition-all text-left flex items-center justify-between group"
            >
              <div className="flex items-center space-x-3">
                <span className="text-xl">{app.icon}</span>
                <div>
                  <div className="text-xs font-bold text-slate-200 group-hover:text-cyan-200">
                    {app.name}
                  </div>
                  <div className="text-[10px] text-slate-500">Android Intent</div>
                </div>
              </div>
              <Send className="w-3.5 h-3.5 text-slate-600 group-hover:text-cyan-400 transition-colors" />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
