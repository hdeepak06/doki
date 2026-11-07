import React, { useState, useEffect } from 'react';
import { TopBar } from './components/TopBar';
import { SideNav, type NavTab } from './components/SideNav';
import { JarvisCore } from './components/JarvisCore';
import { DevicePanel } from './components/DevicePanel';
import { ChatInterface } from './components/ChatInterface';
import { SettingsModal } from './components/SettingsModal';
import { AudioWaveform } from './components/AudioWaveform';
import { VoiceAssistantView } from './components/VoiceAssistantView';
import { LaptopControlView } from './components/LaptopControlView';
import { MobileControlView } from './components/MobileControlView';
import { ResearchView } from './components/ResearchView';
import { MemoryView } from './components/MemoryView';
import type { Device, SystemConfig } from './types';
import { fetchConfig, fetchDevices, sendDeviceCommand } from './services/api';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTab>('home');
  const [config, setConfig] = useState<SystemConfig | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [backendOnline, setBackendOnline] = useState(true);
  const [coreState, setCoreState] = useState<'idle' | 'listening' | 'thinking' | 'speaking'>('idle');

  useEffect(() => {
    loadInitialData();
    const interval = setInterval(loadDevices, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadInitialData = async () => {
    try {
      const res = await fetch('/api/health');
      if (res.ok) {
        setBackendOnline(true);
      } else {
        setBackendOnline(false);
      }
    } catch {
      setBackendOnline(false);
    }

    try {
      const cfg = await fetchConfig();
      setConfig(cfg);
    } catch (e) {
      console.warn('Backend config not loaded yet:', e);
    }
    loadDevices();
  };

  const loadDevices = async () => {
    try {
      const data = await fetchDevices();
      setDevices(data.devices);
    } catch (e) {
      console.warn('Devices fetch failed:', e);
    }
  };

  const laptop = devices.find((d) => d.device_type === 'laptop') || null;
  const androidDevice = devices.find((d) => d.device_type === 'android') || null;

  const handleDeviceCommand = async (target: 'laptop' | 'android', action: string) => {
    try {
      const res = await sendDeviceCommand(target, action);
      alert(res.message || `Command '${action}' transmitted to ${target}.`);
    } catch {
      alert(`Failed to send command to ${target}`);
    }
  };

  const handleCoreClick = () => {
    if (activeTab !== 'chat') {
      setActiveTab('chat');
    }
  };

  return (
    <div className="flex flex-col h-screen bg-[#030712] text-slate-100 overflow-hidden select-none">
      {/* Top Header with live startup & AI Brain indicators */}
      <TopBar
        config={config}
        laptop={laptop}
        backendOnline={backendOnline}
        onOpenSettings={() => setIsSettingsOpen(true)}
        onEmergencyStop={() => {
          if ('speechSynthesis' in window) window.speechSynthesis.cancel();
          setCoreState('idle');
          alert('DOKI: Emergency speech halt triggered.');
        }}
      />

      {/* Main Layout */}
      <div className="flex flex-1 overflow-hidden">
        {/* Side Navigation */}
        <SideNav activeTab={activeTab} onTabChange={setActiveTab} />

        {/* Content Area */}
        <main className="flex-1 p-6 overflow-y-auto bg-gradient-to-b from-slate-950 via-[#050B18] to-slate-950 relative">
          {/* Subtle Cyber Grid Background */}
          <div className="absolute inset-0 bg-[linear-gradient(to_right,#06b6d40a_1px,transparent_1px),linear-gradient(to_bottom,#06b6d40a_1px,transparent_1px)] bg-[size:3rem_3rem] pointer-events-none" />

          {/* TAB 1: HOME DASHBOARD */}
          {activeTab === 'home' && (
            <div className="max-w-6xl mx-auto space-y-8 relative z-10">
              {/* Central Core & Welcome */}
              <div className="flex flex-col items-center justify-center text-center space-y-4 pt-4">
                <JarvisCore state={coreState} size={260} onClick={handleCoreClick} />

                <div className="space-y-1">
                  <h1 className="text-3xl font-extrabold tracking-wider bg-gradient-to-r from-cyan-400 via-sky-200 to-blue-500 bg-clip-text text-transparent">
                    DOKI AI ASSISTANT
                  </h1>
                  <p className="text-slate-400 font-mono text-sm">
                    "Hello {config?.owner_name || 'Deepak'}. How may I assist you today, Sir?"
                  </p>
                </div>

                <AudioWaveform active={coreState !== 'idle'} color="#06B6D4" height={36} />

                {/* Quick Action Triggers */}
                <div className="flex items-center space-x-3 pt-2">
                  <button
                    onClick={() => setActiveTab('voice')}
                    className="px-6 py-2.5 rounded-xl border border-cyan-500/50 bg-cyan-950/50 text-cyan-300 font-mono text-xs font-bold hover:bg-cyan-900/60 transition-all jarvis-glow"
                  >
                    🎙️ Voice Assistant
                  </button>
                  <button
                    onClick={() => setActiveTab('chat')}
                    className="px-6 py-2.5 rounded-xl border border-cyan-500/30 bg-cyan-950/20 text-cyan-300 font-mono text-xs font-bold hover:bg-cyan-900/40 transition-all"
                  >
                    💬 Neural Chat
                  </button>
                  <button
                    onClick={() => setIsSettingsOpen(true)}
                    className="px-6 py-2.5 rounded-xl border border-slate-800 bg-slate-900/60 text-slate-300 font-mono text-xs hover:border-slate-700 transition-all"
                  >
                    ⚙️ Configure Key & Models
                  </button>
                </div>
              </div>

              {/* AI Brain Status Indicators Grid (Section 5) */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 font-mono text-xs">
                <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950/80 space-y-1">
                  <div className="text-slate-500 text-[10px]">AI BRAIN</div>
                  <div className={`font-bold ${config?.has_gemini_key || config?.has_openrouter_key ? 'text-emerald-400' : 'text-amber-400'}`}>
                    {config?.has_gemini_key || config?.has_openrouter_key ? 'Connected' : 'Offline'}
                  </div>
                </div>

                <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950/80 space-y-1">
                  <div className="text-slate-500 text-[10px]">PRIMARY PROVIDER</div>
                  <div className="font-bold text-cyan-300">Google Gemini</div>
                </div>

                <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950/80 space-y-1">
                  <div className="text-slate-500 text-[10px]">PRIMARY MODEL</div>
                  <div className="font-bold text-cyan-400 truncate" title={config?.primary_model}>
                    {config?.primary_model || 'gemini-2.5-flash'}
                  </div>
                </div>

                <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950/80 space-y-1">
                  <div className="text-slate-500 text-[10px]">MODE</div>
                  <div className="font-bold text-emerald-400">FREE (₹0)</div>
                </div>

                <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950/80 space-y-1">
                  <div className="text-slate-500 text-[10px]">FALLBACK</div>
                  <div className="font-bold text-sky-400">
                    {config?.has_openrouter_key ? 'Ready' : 'OpenRouter Free'}
                  </div>
                </div>

                <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950/80 space-y-1">
                  <div className="text-slate-500 text-[10px]">ACTIVE ROUTE</div>
                  <div className="font-bold text-slate-200 uppercase">
                    {config?.active_provider === 'gemini' ? 'Gemini Primary' : 'OpenRouter Fallback'}
                  </div>
                </div>
              </div>

              {/* Connected Devices Grid */}
              <DevicePanel
                devices={devices}
                onRefresh={loadDevices}
                onSendCommand={handleDeviceCommand}
              />
            </div>
          )}

          {/* TAB 2: AI CHAT */}
          {activeTab === 'chat' && (
            <div className="h-full relative z-10">
              <ChatInterface
                activeModel={config?.active_model || 'gemini-2.5-flash'}
                onModelClick={() => setIsSettingsOpen(true)}
              />
            </div>
          )}

          {/* TAB 3: VOICE ASSISTANT (Continuous & Wake-Word) */}
          {activeTab === 'voice' && (
            <div className="h-full relative z-10">
              <VoiceAssistantView
                activeModel={config?.active_model || 'gemini-2.5-flash'}
                onOpenSettings={() => setIsSettingsOpen(true)}
              />
            </div>
          )}

          {/* TAB 4: WINDOWS LAPTOP CONTROL */}
          {activeTab === 'laptop' && (
            <div className="h-full relative z-10">
              <LaptopControlView laptop={laptop} onRefresh={loadDevices} />
            </div>
          )}

          {/* TAB 5: OPPO ANDROID CONTROL */}
          {activeTab === 'mobile' && (
            <div className="h-full relative z-10">
              <MobileControlView androidDevice={androidDevice} onRefresh={loadDevices} />
            </div>
          )}

          {/* TAB 6: INTERNET RESEARCH */}
          {activeTab === 'research' && (
            <div className="h-full relative z-10">
              <ResearchView />
            </div>
          )}

          {/* TAB 7: MEMORY */}
          {activeTab === 'memory' && (
            <div className="h-full relative z-10">
              <MemoryView />
            </div>
          )}

          {/* TAB 8: TASK HISTORY */}
          {activeTab === 'history' && (
            <div className="max-w-4xl mx-auto p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow relative z-10 space-y-4">
              <h2 className="text-lg font-bold font-mono text-slate-100">TASK EXECUTION TIMELINE</h2>
              <p className="text-sm text-slate-400 font-mono">
                All voice inputs, laptop automation executions, and neural chat turns are logged to local SQLite database.
              </p>
              <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50 font-mono text-xs text-slate-400 space-y-2">
                <div className="flex items-center justify-between text-slate-300">
                  <span>SYSTEM LOG STATUS</span>
                  <span className="text-emerald-400">ACTIVE</span>
                </div>
                <div>Storage: <code className="text-cyan-300">data/doki.db</code></div>
                <div>Retention: Persistent across sessions</div>
              </div>
            </div>
          )}

          {/* TAB 9: SETTINGS */}
          {activeTab === 'settings' && (
            <div className="max-w-3xl mx-auto p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow relative z-10 space-y-4 font-mono">
              <h2 className="text-lg font-bold text-slate-100">CONFIGURATION & PREFERENCES</h2>
              <p className="text-sm text-slate-400">
                Manage your free OpenRouter API key, model routers, Kannada voice preferences, and local companion device pairing.
              </p>
              <button
                onClick={() => setIsSettingsOpen(true)}
                className="px-5 py-2.5 rounded-xl border border-cyan-400 bg-cyan-950 text-cyan-300 text-xs font-bold hover:bg-cyan-900 transition-all jarvis-glow"
              >
                Open System Settings Modal ➔
              </button>
            </div>
          )}
        </main>
      </div>

      {/* Settings Modal */}
      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        config={config}
        onConfigUpdated={loadInitialData}
      />
    </div>
  );
};

export default App;
