import React from 'react';
import { Cpu, Battery, Settings as SettingsIcon, VolumeX, Sparkles, ShieldCheck } from 'lucide-react';
import type { Device, SystemConfig } from '../types';

interface TopBarProps {
  config: SystemConfig | null;
  laptop: Device | null;
  backendOnline: boolean;
  onOpenSettings: () => void;
  onEmergencyStop?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  config,
  laptop,
  backendOnline,
  onOpenSettings,
  onEmergencyStop,
}) => {
  const hasKey = config?.has_gemini_key || config?.has_openrouter_key;
  const brainStatus = !backendOnline
    ? 'BACKEND OFFLINE'
    : !hasKey
    ? 'KEY NOT SET'
    : config?.ai_brain_status === 'CONNECTED'
    ? 'CONNECTED'
    : 'ONLINE';

  const isGeminiActive = config?.active_provider === 'gemini';

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

      {/* Center: Live Primary Gemini & Fallback Status Bar */}
      <div className="hidden md:flex items-center space-x-2.5 px-3.5 py-1.5 rounded-full border border-cyan-500/30 bg-slate-900/70 font-mono text-xs">
        {/* Brain Status Dot */}
        <div className="flex items-center space-x-1.5 pr-2 border-r border-slate-700">
          <span
            className={`w-2 h-2 rounded-full ${
              !backendOnline
                ? 'bg-red-500 animate-pulse'
                : brainStatus === 'CONNECTED' || brainStatus === 'ONLINE'
                ? 'bg-emerald-400 animate-ping'
                : 'bg-amber-400'
            }`}
          />
          <span
            className={`font-bold ${
              !backendOnline
                ? 'text-red-400'
                : brainStatus === 'KEY NOT SET'
                ? 'text-amber-300'
                : 'text-emerald-300'
            }`}
          >
            AI Brain: {brainStatus}
          </span>
        </div>

        {/* Primary Provider: Google Gemini */}
        <span className="text-slate-400">Primary:</span>
        <span className={`font-bold ${isGeminiActive ? 'text-cyan-300' : 'text-slate-300'}`}>
          Google Gemini
        </span>

        {/* Primary Model */}
        <span className="text-slate-600">/</span>
        <span className="text-cyan-400 font-bold">
          {config?.primary_model || 'gemini-2.5-flash'}
        </span>

        {/* Free Tier Mode Badge */}
        <span className="text-[10px] text-emerald-400 border border-emerald-500/30 bg-emerald-950/40 px-1.5 py-0.5 rounded uppercase font-bold flex items-center gap-1">
          <ShieldCheck className="w-3 h-3" />
          <span>FREE TIER</span>
        </span>

        {/* Fallback Badge */}
        <span
          className={`text-[10px] px-1.5 py-0.5 rounded uppercase border ${
            config?.has_openrouter_key
              ? 'text-sky-300 border-sky-500/30 bg-sky-950/40'
              : 'text-slate-400 border-slate-700 bg-slate-900'
          }`}
          title="Secondary Free Fallback Router"
        >
          Fallback: {config?.has_openrouter_key ? 'Ready' : 'OpenRouter'}
        </span>
      </div>

      {/* Right Controls & Hardware Metrics */}
      <div className="flex items-center space-x-4">
        {/* Laptop Stats */}
        {laptop && (
          <div className="hidden lg:flex items-center space-x-3 font-mono text-xs text-slate-300 bg-slate-900/80 border border-slate-800 px-3 py-1.5 rounded-lg">
            <div className="flex items-center space-x-1" title="Laptop CPU">
              <Cpu className="w-3.5 h-3.5 text-cyan-400" />
              <span>{laptop.cpu_usage || '14%'}</span>
            </div>
            <div className="w-[1px] h-3 bg-slate-700" />
            <div className="flex items-center space-x-1" title="Battery">
              <Battery className="w-3.5 h-3.5 text-emerald-400" />
              <span>{laptop.battery || '96%'}</span>
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
