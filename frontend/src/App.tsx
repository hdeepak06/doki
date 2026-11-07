import React, { useState, useEffect } from 'react';
import { TopBar } from './components/TopBar';
import { SideNav, type NavTab } from './components/SideNav';
import { JarvisCore } from './components/JarvisCore';
import { DevicePanel } from './components/DevicePanel';
import { ChatInterface } from './components/ChatInterface';
import { SettingsModal } from './components/SettingsModal';
import { AudioWaveform } from './components/AudioWaveform';
import type { Device, SystemConfig } from './types';
import { fetchConfig, fetchDevices, sendDeviceCommand } from './services/api';
import { Globe, Database, Smartphone, Laptop } from 'lucide-react';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTab>('home');
  const [config, setConfig] = useState<SystemConfig | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [coreState, setCoreState] = useState<'idle' | 'listening' | 'thinking' | 'speaking'>('idle');

  useEffect(() => {
    loadInitialData();
    const interval = setInterval(loadDevices, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadInitialData = async () => {
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

  const handleDeviceCommand = async (target: 'laptop' | 'android', action: string) => {
    try {
      await sendDeviceCommand(target, action);
      alert(`Command '${action}' transmitted to ${target}.`);
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
      {/* Top Header */}
      <TopBar
        config={config}
        laptop={laptop}
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

                {/* Quick Action Trigger */}
                <div className="flex items-center space-x-3 pt-2">
                  <button
                    onClick={() => setActiveTab('chat')}
                    className="px-6 py-2.5 rounded-xl border border-cyan-500/40 bg-cyan-950/40 text-cyan-300 font-mono text-xs font-bold hover:bg-cyan-900/60 transition-all jarvis-glow"
                  >
                    Open Neural Chat
                  </button>
                  <button
                    onClick={() => setIsSettingsOpen(true)}
                    className="px-6 py-2.5 rounded-xl border border-slate-800 bg-slate-900/60 text-slate-300 font-mono text-xs hover:border-slate-700 transition-all"
                  >
                    Configure Models
                  </button>
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
                activeModel={config?.active_model || 'openrouter/free'}
                onModelClick={() => setIsSettingsOpen(true)}
              />
            </div>
          )}

          {/* TAB 3: VOICE ASSISTANT */}
          {activeTab === 'voice' && (
            <div className="max-w-3xl mx-auto h-full flex flex-col items-center justify-center text-center space-y-6 relative z-10">
              <JarvisCore state={coreState} size={300} />
              <div className="space-y-2">
                <h2 className="text-2xl font-bold font-mono text-cyan-400 tracking-wider">
                  VOICE ASSISTANT // CONTINUOUS MODE
                </h2>
                <p className="text-slate-400 font-mono text-xs">
                  Wake Word: <span className="text-cyan-300 font-bold">"Hey DOKI"</span> | Native Multilingual: English, Kannada, Hindi
                </p>
              </div>
              <AudioWaveform active={true} color="#06B6D4" height={48} />
              <p className="text-xs text-slate-500 font-mono max-w-md">
                Stage 2 Voice Module: Real-time browser speech recognition and synthesis are active. Full local Vosk and Whisper.cpp pipeline will link to wake-word continuous background listening.
              </p>
              <button
                onClick={() => setActiveTab('chat')}
                className="px-6 py-2 rounded-xl border border-cyan-500/40 bg-cyan-950/30 text-cyan-300 text-xs font-mono hover:bg-cyan-900/50"
              >
                Switch to Text Input
              </button>
            </div>
          )}

          {/* TAB 4: LAPTOP CONTROL (Stage 3 preview) */}
          {activeTab === 'laptop' && (
            <div className="max-w-4xl mx-auto space-y-6 relative z-10">
              <div className="p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow">
                <div className="flex items-center space-x-3 mb-4">
                  <Laptop className="w-6 h-6 text-cyan-400" />
                  <h2 className="text-lg font-bold font-mono text-slate-100">
                    WINDOWS LAPTOP AUTOMATION (STAGE 3)
                  </h2>
                </div>
                <p className="text-sm text-slate-400 mb-6 font-mono">
                  Controlled directly via local DOKI Python agent. PyAutoGUI, psutil, Window Management, OCR and safe command execution.
                </p>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 font-mono text-xs">
                  {['Open Chrome', 'Open VS Code', 'Check Battery', 'Check RAM', 'File Search', 'Lock Screen', 'Screenshot OCR', 'Volume Up'].map(
                    (action, i) => (
                      <button
                        key={i}
                        onClick={() => handleDeviceCommand('laptop', action)}
                        className="p-3 rounded-xl border border-slate-800 bg-slate-900/60 text-slate-300 hover:border-cyan-500/50 hover:text-cyan-300 transition-all text-left"
                      >
                        ⚡ {action}
                      </button>
                    )
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 5: MOBILE CONTROL (Stage 4 preview) */}
          {activeTab === 'mobile' && (
            <div className="max-w-4xl mx-auto space-y-6 relative z-10">
              <div className="p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow">
                <div className="flex items-center space-x-3 mb-4">
                  <Smartphone className="w-6 h-6 text-cyan-400" />
                  <h2 className="text-lg font-bold font-mono text-slate-100">
                    OPPO ANDROID COMPANION (STAGE 4)
                  </h2>
                </div>
                <p className="text-sm text-slate-400 mb-6 font-mono">
                  Native Android Companion App connects over WebSocket local network mesh. AccessibilityService, Alarms, Notifications, and App Launching.
                </p>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 font-mono text-xs">
                  {['Open WhatsApp', 'Open YouTube', 'Check Phone Battery', 'Set Alarm', 'Read Notifications', 'Send SMS Intent', 'Toggle Flashlight', 'Sync Clipboard'].map(
                    (action, i) => (
                      <button
                        key={i}
                        onClick={() => handleDeviceCommand('android', action)}
                        className="p-3 rounded-xl border border-slate-800 bg-slate-900/60 text-slate-300 hover:border-cyan-500/50 hover:text-cyan-300 transition-all text-left"
                      >
                        📱 {action}
                      </button>
                    )
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 6: INTERNET RESEARCH (Stage 6 preview) */}
          {activeTab === 'research' && (
            <div className="max-w-4xl mx-auto p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow relative z-10">
              <div className="flex items-center space-x-3 mb-4">
                <Globe className="w-6 h-6 text-cyan-400" />
                <h2 className="text-lg font-bold font-mono text-slate-100">INTERNET KNOWLEDGE & RESEARCH</h2>
              </div>
              <p className="text-sm text-slate-400 mb-4 font-mono">
                Real-time web research module using free search integrations. Ask questions in Chat to trigger live synthesis.
              </p>
              <button
                onClick={() => setActiveTab('chat')}
                className="px-4 py-2 rounded-xl border border-cyan-500/40 bg-cyan-950/30 text-cyan-300 text-xs font-mono"
              >
                Launch Research in Chat ➔
              </button>
            </div>
          )}

          {/* TAB 7: MEMORY (Stage 6 preview) */}
          {activeTab === 'memory' && (
            <div className="max-w-4xl mx-auto p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow relative z-10">
              <div className="flex items-center space-x-3 mb-4">
                <Database className="w-6 h-6 text-cyan-400" />
                <h2 className="text-lg font-bold font-mono text-slate-100">PERSONAL MEMORY SYSTEM</h2>
              </div>
              <p className="text-sm text-slate-400 mb-4 font-mono">
                Persistent SQLite memory bank stored at <code className="text-cyan-400">backend/data/doki.db</code>. Remembers Deepak's preferences, instructions, and past sessions.
              </p>
            </div>
          )}

          {/* TAB 8: TASK HISTORY */}
          {activeTab === 'history' && (
            <div className="max-w-4xl mx-auto p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow relative z-10">
              <h2 className="text-lg font-bold font-mono text-slate-100 mb-4">TASK EXECUTION TIMELINE</h2>
              <p className="text-sm text-slate-400 font-mono">
                All executed commands, AI responses, and device interactions are logged to local SQLite database.
              </p>
            </div>
          )}

          {/* TAB 9: SETTINGS */}
          {activeTab === 'settings' && (
            <div className="max-w-3xl mx-auto p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 jarvis-glow relative z-10 space-y-4">
              <h2 className="text-lg font-bold font-mono text-slate-100">CONFIGURATION & PREFERENCES</h2>
              <p className="text-sm text-slate-400 font-mono">
                Adjust OpenRouter free models, API keys, voice speeds, and local device pairing codes.
              </p>
              <button
                onClick={() => setIsSettingsOpen(true)}
                className="px-5 py-2.5 rounded-xl border border-cyan-400 bg-cyan-950 text-cyan-300 text-xs font-mono font-bold"
              >
                Open Settings Panel
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
