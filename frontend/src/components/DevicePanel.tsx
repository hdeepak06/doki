import React from 'react';
import { Laptop, Smartphone, Battery, Cpu, Activity, RefreshCw } from 'lucide-react';
import type { Device } from '../types';

interface DevicePanelProps {
  devices: Device[];
  onRefresh: () => void;
  onSendCommand?: (target: 'laptop' | 'android', action: string) => void;
}

export const DevicePanel: React.FC<DevicePanelProps> = ({
  devices,
  onRefresh,
  onSendCommand,
}) => {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-xs uppercase font-mono tracking-widest text-cyan-400 font-bold flex items-center space-x-2">
          <Activity className="w-4 h-4 text-cyan-400" />
          <span>CONNECTED DEVICES</span>
        </h3>
        <button
          onClick={onRefresh}
          className="text-xs text-slate-400 hover:text-cyan-300 p-1 rounded hover:bg-slate-800 transition-colors"
          title="Refresh Device Mesh"
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {devices.map((dev) => {
          const isLaptop = dev.device_type === 'laptop';
          const isOnline = dev.status === 'online';

          return (
            <div
              key={dev.device_id}
              className={`p-4 rounded-xl border transition-all ${
                isOnline
                  ? 'border-cyan-500/30 bg-slate-900/60 jarvis-glow'
                  : 'border-slate-800/80 bg-slate-950/40 opacity-70'
              }`}
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center space-x-3">
                  <div className={`p-2 rounded-lg border ${
                    isOnline ? 'border-cyan-500/40 bg-cyan-950/40 text-cyan-400' : 'border-slate-800 bg-slate-900 text-slate-500'
                  }`}>
                    {isLaptop ? <Laptop className="w-5 h-5" /> : <Smartphone className="w-5 h-5" />}
                  </div>
                  <div>
                    <h4 className="font-bold text-sm text-slate-100">{dev.name}</h4>
                    <p className="text-[11px] font-mono text-slate-400">{dev.os || (isLaptop ? 'Windows 11' : 'Android')}</p>
                  </div>
                </div>

                <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono flex items-center space-x-1 ${
                  isOnline
                    ? 'bg-emerald-950/60 border border-emerald-500/40 text-emerald-300'
                    : 'bg-slate-800/60 border border-slate-700 text-slate-400'
                }`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${isOnline ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`} />
                  <span className="uppercase">{dev.status}</span>
                </span>
              </div>

              {/* Hardware stats */}
              {isLaptop ? (
                <div className="space-y-1.5 font-mono text-xs text-slate-300 bg-slate-950/50 p-2.5 rounded-lg border border-slate-800/60">
                  <div className="flex justify-between items-center">
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Cpu className="w-3.5 h-3.5 text-cyan-400" /> CPU Load
                    </span>
                    <span className="text-cyan-300 font-bold">{dev.cpu_usage || '12%'}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Activity className="w-3.5 h-3.5 text-sky-400" /> RAM Usage
                    </span>
                    <span className="text-sky-300 font-bold truncate max-w-[150px]">{dev.ram_usage || '6.2 GB'}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Battery className="w-3.5 h-3.5 text-emerald-400" /> Battery
                    </span>
                    <span className="text-emerald-300 font-bold">{dev.battery || '100%'}</span>
                  </div>
                </div>
              ) : (
                <div className="font-mono text-xs text-slate-400 bg-slate-950/50 p-2.5 rounded-lg border border-slate-800/60">
                  <p>{dev.note || 'Android Companion: Connect via mobile app WebSocket pairing.'}</p>
                </div>
              )}

              {/* Action Buttons */}
              <div className="mt-3 flex items-center space-x-2">
                <button
                  onClick={() => onSendCommand && onSendCommand(dev.device_type, 'ping')}
                  className="w-full py-1.5 rounded-lg border border-cyan-500/20 bg-cyan-950/20 text-cyan-400 hover:bg-cyan-900/40 text-xs font-mono transition-colors"
                >
                  {isOnline ? 'Ping Device' : 'Check Pairing Code'}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
