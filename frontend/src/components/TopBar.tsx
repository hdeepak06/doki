import React from 'react';
import { Cpu, Battery, Settings as SettingsIcon, VolumeX, Sparkles } from 'lucide-react';
import type { Device, SystemConfig } from '../types';

interface TopBarProps {
  config: SystemConfig | null;
  laptop: Device | null;
  onOpenSettings: () => void;
  onEmergencyStop?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  config,
  laptop,
  onOpenSettings,
  onEmergencyStop,
}) => {
  return (
    <header className="h-16 border-b border-cyan-500/20 bg-slate-950/80 backdrop-blur-md px-6 flex items-center justify-between select-none z-30">
      {/* Brand & Persona */}
      <div className="flex items-center space-x-3">
        <div className="relative flex items-center justify-center w-9 h-9 rounded-lg bg-cyan-950/60 border border-cyan-400/50 jarvis-glow">
          <Sparkles className="w-5 h-5 text-cyan-400 animate-pulse" />
        </div>
        <div>
          <div className="flex items-center space-x-2">
            <span className="font-extrabold tracking-widest text-lg bg-gradient-to-r from-cyan-400 via-sky-300 to-blue-500 bg-clip-text text-transparent">
              DOKI
            </span>
            <span className="text-[10px] tracking-wider uppercase px-1.5 py-0.5 rounded border border-cyan-500/40 text-cyan-400 bg-cyan-950/40">
              JARVIS OS
            </span>
          </div>
          <p className="text-[11px] text-slate-400 font-mono">
            Owner: <span className="text-cyan-300 font-semibold">{config?.owner_name || 'Deepak'}</span>
          </p>
        </div>
      </div>

      {/* Center: Live Model Router Info */}
      <div className="hidden md:flex items-center space-x-2 px-3 py-1.5 rounded-full border border-cyan-500/30 bg-slate-900/60 font-mono text-xs text-slate-300">
        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping mr-1" />
        <span className="text-slate-400">Brain:</span>
        <span className="text-cyan-300 font-bold truncate max-w-[200px]">
          {config?.active_model || 'openrouter/free'}
        </span>
        <span className="text-[10px] text-emerald-400 border border-emerald-500/30 px-1 rounded uppercase">
          FREE TIER
        </span>
      </div>

      {/* Right Controls & Hardware Metrics */}
      <div className="flex items-center space-x-4">
        {/* Hardware Status */}
        {laptop && (
          <div className="hidden lg:flex items-center space-x-3 font-mono text-xs text-slate-300 bg-slate-900/80 border border-slate-800 px-3 py-1.5 rounded-lg">
            <div className="flex items-center space-x-1" title="Laptop CPU">
              <Cpu className="w-3.5 h-3.5 text-cyan-400" />
              <span>{laptop.cpu_usage || '12%'}</span>
            </div>
            <div className="w-[1px] h-3 bg-slate-700" />
            <div className="flex items-center space-x-1" title="Battery">
              <Battery className="w-3.5 h-3.5 text-emerald-400" />
              <span>{laptop.battery || '100%'}</span>
            </div>
          </div>
        )}

        {/* Emergency Stop / Mute */}
        {onEmergencyStop && (
          <button
            onClick={onEmergencyStop}
            className="flex items-center space-x-1 px-2.5 py-1.5 rounded-lg border border-red-500/40 bg-red-950/30 text-red-400 hover:bg-red-900/50 hover:text-red-200 transition-colors text-xs font-mono"
            title="Emergency Halt"
          >
            <VolumeX className="w-4 h-4" />
            <span className="hidden sm:inline">HALT</span>
          </button>
        )}

        {/* Settings Trigger */}
        <button
          onClick={onOpenSettings}
          className="p-2 rounded-lg border border-cyan-500/30 text-cyan-400 bg-cyan-950/20 hover:bg-cyan-900/40 hover:border-cyan-400 transition-all jarvis-glow"
          title="DOKI Settings"
        >
          <SettingsIcon className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};
