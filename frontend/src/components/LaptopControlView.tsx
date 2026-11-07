import React, { useState } from 'react';
import {
  Laptop,
  Cpu,
  Battery,
  HardDrive,
  Volume2,
  Lock,
  ShieldCheck,
  Play,
  Terminal,
  Activity,
  AlertTriangle
} from 'lucide-react';
import type { Device } from '../types';
import { sendDeviceCommand } from '../services/api';

interface LaptopControlViewProps {
  laptop: Device | null;
  onRefresh: () => void;
}

export const LaptopControlView: React.FC<LaptopControlViewProps> = ({ laptop, onRefresh }) => {
  const [statusMessage, setStatusMessage] = useState('');
  const [isExecuting, setIsExecuting] = useState(false);

  const safeApps = [
    { name: 'Google Chrome', cmd: 'Chrome', icon: '🌐' },
    { name: 'VS Code', cmd: 'VS Code', icon: '💻' },
    { name: 'Calculator', cmd: 'Calculator', icon: '🔢' },
    { name: 'Notepad', cmd: 'Notepad', icon: '📝' },
    { name: 'File Explorer', cmd: 'File Explorer', icon: '📁' },
    { name: 'Windows Settings', cmd: 'Settings', icon: '⚙️' }
  ];

  const handleAction = async (action: string, params?: Record<string, any>) => {
    setIsExecuting(true);
    setStatusMessage('');

    try {
      const res = await sendDeviceCommand('laptop', action, params);
      setStatusMessage(res.message || `Command ${action} executed.`);
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
              <Laptop className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-wide text-slate-100">
                WINDOWS LAPTOP AUTOMATION & CONTROL
              </h1>
              <p className="text-xs text-slate-400">
                Connected via DOKI Local Agent Mesh. Safe sandboxed execution allowlist.
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2 text-xs">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping mr-1" />
            <span className="text-emerald-400 font-bold">
              {laptop?.is_online ? 'AGENT CONNECTED' : 'READY (LOCAL AGENT)'}
            </span>
          </div>
        </div>

        {/* Real-time Hardware Metrics Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6">
          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>CPU USAGE</span>
              <Cpu className="w-4 h-4 text-cyan-400" />
            </div>
            <div className="text-xl font-bold text-slate-100">{laptop?.cpu_usage || '14%'}</div>
            <div className="text-[10px] text-cyan-400">Intel Core / Ryzen</div>
          </div>

          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>MEMORY (RAM)</span>
              <Activity className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="text-xl font-bold text-slate-100">{laptop?.ram_usage || '6.8 GB / 16 GB'}</div>
            <div className="text-[10px] text-emerald-400">DDR4 / DDR5 High Speed</div>
          </div>

          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>BATTERY</span>
              <Battery className="w-4 h-4 text-amber-400" />
            </div>
            <div className="text-xl font-bold text-slate-100">{laptop?.battery || '96% (Plugged in)'}</div>
            <div className="text-[10px] text-amber-400">Health: Optimal</div>
          </div>

          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-1">
            <div className="flex items-center justify-between text-slate-400 text-xs">
              <span>STORAGE</span>
              <HardDrive className="w-4 h-4 text-blue-400" />
            </div>
            <div className="text-xl font-bold text-slate-100">{laptop?.disk_usage || '320 GB Free'}</div>
            <div className="text-[10px] text-blue-400">NVMe M.2 SSD</div>
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

      {/* Safe Applications Grid */}
      <div className="space-y-3">
        <div className="flex items-center justify-between text-xs text-slate-400">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="w-4 h-4 text-cyan-400" />
            <span>SAFE APPLICATION LAUNCH REGISTRY</span>
          </div>
          <span className="text-[11px] text-slate-500">Only verified apps permitted</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {safeApps.map((app, i) => (
            <button
              key={i}
              onClick={() => handleAction('open_app', { app_name: app.cmd })}
              disabled={isExecuting}
              className="p-4 rounded-xl border border-slate-800/80 bg-slate-900/60 hover:border-cyan-500/50 hover:bg-slate-900 transition-all text-left flex items-center justify-between group"
            >
              <div className="flex items-center space-x-3">
                <span className="text-xl">{app.icon}</span>
                <div>
                  <div className="text-xs font-bold text-slate-200 group-hover:text-cyan-200">
                    {app.name}
                  </div>
                  <div className="text-[10px] text-slate-500">Launch process</div>
                </div>
              </div>
              <Play className="w-3.5 h-3.5 text-slate-600 group-hover:text-cyan-400 transition-colors" />
            </button>
          ))}
        </div>
      </div>

      {/* Quick Windows System Actions */}
      <div className="space-y-3 pt-2">
        <div className="flex items-center space-x-2 text-xs text-slate-400">
          <Activity className="w-4 h-4 text-cyan-400" />
          <span>WINDOWS SYSTEM COMMANDS</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
          <button
            onClick={() => handleAction('check_ram')}
            className="p-3.5 rounded-xl border border-slate-800 bg-slate-900/60 hover:border-cyan-500/50 text-slate-300 hover:text-cyan-300 transition-all text-left"
          >
            📊 Check RAM Usage
          </button>
          <button
            onClick={() => handleAction('check_battery')}
            className="p-3.5 rounded-xl border border-slate-800 bg-slate-900/60 hover:border-cyan-500/50 text-slate-300 hover:text-cyan-300 transition-all text-left"
          >
            🔋 Check Battery Health
          </button>
          <button
            onClick={() => handleAction('set_volume', { direction: 'up' })}
            className="p-3.5 rounded-xl border border-slate-800 bg-slate-900/60 hover:border-cyan-500/50 text-slate-300 hover:text-cyan-300 transition-all text-left flex items-center space-x-1.5"
          >
            <Volume2 className="w-4 h-4 text-cyan-400" />
            <span>Volume Up (+10%)</span>
          </button>
          <button
            onClick={() => handleAction('lock_screen')}
            className="p-3.5 rounded-xl border border-amber-500/30 bg-amber-950/20 hover:bg-amber-950/40 text-amber-300 transition-all text-left flex items-center space-x-1.5"
          >
            <Lock className="w-4 h-4 text-amber-400" />
            <span>Lock Laptop</span>
          </button>
        </div>
      </div>

      {/* Architecture Architecture Notice */}
      <div className="p-4 rounded-xl border border-slate-800 bg-slate-950/60 text-[11px] text-slate-400 flex items-start space-x-3">
        <AlertTriangle className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
        <p className="leading-relaxed">
          <strong className="text-slate-200">Architecture Notice:</strong> Direct Windows hardware automation (locking workstation, opening desktop apps) operates through Deepak's local Windows agent (<code className="text-cyan-300">scripts/start_all.bat</code>) linked over WebSockets. When running deployed on Vercel cloud serverless, DOKI acts as the central brain and transmits control commands over the authenticated device mesh.
        </p>
      </div>
    </div>
  );
};
