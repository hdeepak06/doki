import React, { useState, useEffect } from 'react';
import {
  X,
  Key,
  Check,
  RefreshCw,
  ExternalLink,
  Radio,
  Sparkles,
  Mic,
  Bell,
  Laptop,
  Smartphone
} from 'lucide-react';
import type { SystemConfig } from '../types';
import {
  updateConfig,
  getStoredGeminiKey,
  setStoredGeminiKey,
  getStoredOpenRouterKey,
  setStoredOpenRouterKey,
  checkAiConnectivity
} from '../services/api';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  config: SystemConfig | null;
  onConfigUpdated: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  config,
  onConfigUpdated,
}) => {
  const [activeTab, setActiveTab] = useState<'ai' | 'permissions'>('ai');

  // AI Keys and Models
  const [geminiKeyInput, setGeminiKeyInput] = useState('');
  const [openrouterKeyInput, setOpenrouterKeyInput] = useState('');
  const [primaryProvider, setPrimaryProvider] = useState<'gemini' | 'openrouter'>('gemini');
  const [geminiModel, setGeminiModel] = useState('gemini-2.5-flash');
  const [openrouterModel, setOpenrouterModel] = useState('openrouter/free');
  const [freeOnlyMode, setFreeOnlyMode] = useState(true);
  const [autoFallback, setAutoFallback] = useState(true);

  // Connectivity Test State
  const [isTesting, setIsTesting] = useState(false);
  const [testResults, setTestResults] = useState<any>(null);

  // Status
  const [isSaving, setIsSaving] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');

  // Device Permissions State
  const [micStatus, setMicStatus] = useState<string>('checking');
  const [notificationStatus, setNotificationStatus] = useState<string>('checking');

  useEffect(() => {
    if (isOpen) {
      // Load saved keys
      const gKey = getStoredGeminiKey();
      if (gKey) setGeminiKeyInput(gKey);

      const oKey = getStoredOpenRouterKey();
      if (oKey) setOpenrouterKeyInput(oKey);

      setPrimaryProvider((config?.primary_provider as any) || 'gemini');
      setGeminiModel(config?.primary_model || 'gemini-2.5-flash');
      setOpenrouterModel(config?.fallback_model || 'openrouter/free');

      checkPermissions();
    }
  }, [isOpen, config]);

  const checkPermissions = async () => {
    if (navigator.permissions && navigator.permissions.query) {
      try {
        const mic = await navigator.permissions.query({ name: 'microphone' as PermissionName });
        setMicStatus(mic.state);
        mic.onchange = () => setMicStatus(mic.state);
      } catch {
        setMicStatus('supported');
      }

      try {
        const notif = await navigator.permissions.query({ name: 'notifications' as PermissionName });
        setNotificationStatus(notif.state);
        notif.onchange = () => setNotificationStatus(notif.state);
      } catch {
        setNotificationStatus('supported');
      }
    }
  };

  const handleTestConnection = async () => {
    setIsTesting(true);
    setTestResults(null);
    try {
      const res = await checkAiConnectivity(geminiKeyInput.trim(), openrouterKeyInput.trim());
      setTestResults(res);
    } catch (err: any) {
      setStatusMsg(`Connection test failed: ${err.message || 'Error'}`);
    } finally {
      setIsTesting(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setStatusMsg('');

    try {
      const gTrim = geminiKeyInput.trim();
      const oTrim = openrouterKeyInput.trim();

      if (gTrim) setStoredGeminiKey(gTrim);
      if (oTrim) setStoredOpenRouterKey(oTrim);

      await updateConfig({
        gemini_api_key: gTrim || undefined,
        openrouter_api_key: oTrim || undefined,
        ai_primary_provider: primaryProvider,
        gemini_model: geminiModel,
        active_model: primaryProvider === 'gemini' ? geminiModel : openrouterModel,
        free_only_mode: freeOnlyMode,
        auto_fallback_enabled: autoFallback
      });

      setStatusMsg('Configuration successfully updated and persisted!');
      onConfigUpdated();
      setTimeout(() => {
        setStatusMsg('');
        onClose();
      }, 1200);
    } catch (err: any) {
      setStatusMsg('Error saving configuration: ' + (err.message || 'Unknown'));
    } finally {
      setIsSaving(false);
    }
  };

  const handleRequestMic = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((t) => t.stop());
      setMicStatus('granted');
    } catch {
      setMicStatus('denied');
    }
  };

  const handleRequestNotification = async () => {
    if ('Notification' in window) {
      const res = await Notification.requestPermission();
      setNotificationStatus(res);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
      <div className="w-full max-w-2xl rounded-2xl border border-cyan-500/30 bg-slate-950 p-6 shadow-2xl jarvis-glow font-mono max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-cyan-500/20">
          <div className="flex items-center space-x-2">
            <Key className="w-5 h-5 text-cyan-400" />
            <h2 className="text-base font-bold tracking-wide text-slate-100">
              DOKI SYSTEM CONFIGURATION & PERMISSIONS
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-900 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Switcher */}
        <div className="flex items-center space-x-2 mt-4 pb-2 border-b border-slate-800 text-xs">
          <button
            onClick={() => setActiveTab('ai')}
            className={`px-4 py-1.5 rounded-lg transition-all ${
              activeTab === 'ai'
                ? 'bg-cyan-950 border border-cyan-500/50 text-cyan-300 font-bold'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            🧠 AI Brain & Providers
          </button>
          <button
            onClick={() => setActiveTab('permissions')}
            className={`px-4 py-1.5 rounded-lg transition-all ${
              activeTab === 'permissions'
                ? 'bg-cyan-950 border border-cyan-500/50 text-cyan-300 font-bold'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            🛡️ Device Permissions
          </button>
        </div>

        {activeTab === 'ai' ? (
          <form onSubmit={handleSave} className="mt-5 space-y-5 text-xs">
            {/* Primary AI Provider: Google Gemini */}
            <div className="p-4 rounded-xl border border-cyan-500/30 bg-slate-900/60 space-y-3">
              <div className="flex items-center justify-between">
                <label className="font-bold text-cyan-300 flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-cyan-400" />
                  <span>PRIMARY AI BRAIN: GOOGLE GEMINI (FREE TIER)</span>
                </label>
                <a
                  href="https://aistudio.google.com/app/apikey"
                  target="_blank"
                  rel="noreferrer"
                  className="text-cyan-400 hover:underline flex items-center gap-1 text-[11px]"
                >
                  <span>Get Gemini API Key</span>
                  <ExternalLink className="w-3 h-3" />
                </a>
              </div>

              <input
                type="password"
                value={geminiKeyInput}
                onChange={(e) => setGeminiKeyInput(e.target.value)}
                placeholder="AIzaSy..."
                className="w-full bg-slate-950 border border-slate-700 focus:border-cyan-400 rounded-lg px-3.5 py-2 text-slate-200 focus:outline-none"
              />

              <div className="flex items-center justify-between text-[11px] text-slate-400">
                <span>Default Model:</span>
                <select
                  value={geminiModel}
                  onChange={(e) => setGeminiModel(e.target.value)}
                  className="bg-slate-950 border border-slate-700 rounded px-2 py-1 text-cyan-300"
                >
                  <option value="gemini-2.5-flash">Google Gemini 2.5 Flash</option>
                  <option value="gemini-2.0-flash">Google Gemini 2.0 Flash</option>
                  <option value="gemini-1.5-flash">Google Gemini 1.5 Flash</option>
                  <option value="gemini-1.5-flash-8b">Google Gemini 1.5 Flash-8B</option>
                </select>
              </div>
            </div>

            {/* Secondary AI Provider: OpenRouter Fallback */}
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/40 space-y-3">
              <div className="flex items-center justify-between">
                <label className="font-bold text-slate-300 flex items-center gap-1.5">
                  <Radio className="w-4 h-4 text-sky-400" />
                  <span>SECONDARY FALLBACK: OPENROUTER FREE MODELS</span>
                </label>
                <a
                  href="https://openrouter.ai/keys"
                  target="_blank"
                  rel="noreferrer"
                  className="text-sky-400 hover:underline flex items-center gap-1 text-[11px]"
                >
                  <span>Get OpenRouter Key</span>
                  <ExternalLink className="w-3 h-3" />
                </a>
              </div>

              <input
                type="password"
                value={openrouterKeyInput}
                onChange={(e) => setOpenrouterKeyInput(e.target.value)}
                placeholder="sk-or-v1-..."
                className="w-full bg-slate-950 border border-slate-700 focus:border-sky-400 rounded-lg px-3.5 py-2 text-slate-200 focus:outline-none"
              />

              <div className="flex items-center justify-between text-[11px] text-slate-400">
                <span>Fallback Router:</span>
                <span className="text-sky-300 font-bold">openrouter/free (Dynamic Free Router)</span>
              </div>
            </div>

            {/* Safety & Fallback Toggles */}
            <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950 space-y-2">
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-bold text-slate-200">FREE-ONLY MODE</div>
                  <div className="text-[10px] text-slate-500">Strictly blocks all paid models (Budget: ₹0)</div>
                </div>
                <input
                  type="checkbox"
                  checked={freeOnlyMode}
                  onChange={(e) => setFreeOnlyMode(e.target.checked)}
                  className="w-4 h-4 accent-cyan-400 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-slate-800">
                <div>
                  <div className="font-bold text-slate-200">AUTOMATIC PROVIDER FALLBACK</div>
                  <div className="text-[10px] text-slate-500">
                    Switches to OpenRouter if Gemini hits 429 quota or capacity limits
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={autoFallback}
                  onChange={(e) => setAutoFallback(e.target.checked)}
                  className="w-4 h-4 accent-cyan-400 cursor-pointer"
                />
              </div>
            </div>

            {/* Live Connection Test Button */}
            <div className="flex items-center justify-between pt-1">
              <button
                type="button"
                onClick={handleTestConnection}
                disabled={isTesting}
                className="px-3.5 py-2 rounded-xl border border-cyan-500/40 bg-cyan-950/40 text-cyan-300 hover:bg-cyan-900/60 transition-all flex items-center space-x-1.5"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isTesting ? 'animate-spin' : ''}`} />
                <span>Test Live Connection</span>
              </button>

              <button
                type="submit"
                disabled={isSaving}
                className="px-6 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold transition-all shadow-lg"
              >
                {isSaving ? 'Saving...' : 'Save Configuration'}
              </button>
            </div>

            {/* Test Results Output */}
            {testResults && (
              <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-950/90 space-y-1.5 text-[11px]">
                <div className="font-bold text-slate-200 pb-1 border-b border-slate-800">
                  CONNECTION TEST RESULTS:
                </div>
                <div className="flex justify-between">
                  <span>Google Gemini Primary:</span>
                  <span className={testResults.gemini?.status === 'CONNECTED' ? 'text-emerald-400 font-bold' : 'text-amber-400'}>
                    {testResults.gemini?.status} ({testResults.gemini?.model})
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>OpenRouter Fallback:</span>
                  <span className={testResults.openrouter_fallback?.status === 'CONNECTED' ? 'text-emerald-400 font-bold' : 'text-slate-400'}>
                    {testResults.openrouter_fallback?.status}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Active Brain Route:</span>
                  <span className="text-cyan-300 font-bold uppercase">{testResults.active_provider}</span>
                </div>
              </div>
            )}
          </form>
        ) : (
          /* Section 7: Device Permissions Management Tab */
          <div className="mt-5 space-y-4 text-xs">
            <p className="text-slate-400 text-[11px] leading-relaxed">
              DOKI requires specific hardware and browser permissions to control laptop automation, process Kannada voice recognition, and sync with your OPPO phone.
            </p>

            {/* Permissions List */}
            <div className="space-y-3">
              {/* 1. Microphone */}
              <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-900/60 flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-400">
                    <Mic className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-slate-200">Microphone Access</div>
                    <div className="text-[10px] text-slate-500">
                      Required for Voice Assistant & Wake-Word continuous detection
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <span className={`px-2 py-0.5 rounded text-[10px] uppercase font-bold ${
                    micStatus === 'granted' ? 'bg-emerald-950 text-emerald-400 border border-emerald-500/30' : 'bg-amber-950 text-amber-400 border border-amber-500/30'
                  }`}>
                    {micStatus}
                  </span>
                  {micStatus !== 'granted' && (
                    <button
                      onClick={handleRequestMic}
                      className="px-2.5 py-1 rounded-lg border border-cyan-400 bg-cyan-950 text-cyan-300 hover:bg-cyan-900 text-[11px]"
                    >
                      Request
                    </button>
                  )}
                </div>
              </div>

              {/* 2. Notifications */}
              <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-900/60 flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-400">
                    <Bell className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-slate-200">System Notifications</div>
                    <div className="text-[10px] text-slate-500">
                      Alerts when long-running research or background tasks complete
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <span className={`px-2 py-0.5 rounded text-[10px] uppercase font-bold ${
                    notificationStatus === 'granted' ? 'bg-emerald-950 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-400'
                  }`}>
                    {notificationStatus}
                  </span>
                  {notificationStatus !== 'granted' && (
                    <button
                      onClick={handleRequestNotification}
                      className="px-2.5 py-1 rounded-lg border border-cyan-400 bg-cyan-950 text-cyan-300 hover:bg-cyan-900 text-[11px]"
                    >
                      Request
                    </button>
                  )}
                </div>
              </div>

              {/* 3. Windows Local Agent */}
              <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-900/60 flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-400">
                    <Laptop className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-slate-200">Windows Local Agent (Port 8000)</div>
                    <div className="text-[10px] text-slate-500">
                      Controls desktop apps (Chrome, VS Code), RAM, and lock screen
                    </div>
                  </div>
                </div>

                <span className="px-2 py-0.5 rounded text-[10px] uppercase font-bold bg-emerald-950 text-emerald-400 border border-emerald-500/30">
                  Ready
                </span>
              </div>

              {/* 4. OPPO Android Mesh */}
              <div className="p-3.5 rounded-xl border border-slate-800 bg-slate-900/60 flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-400">
                    <Smartphone className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-slate-200">OPPO Android Companion</div>
                    <div className="text-[10px] text-slate-500">
                      Encrypted local WebSocket mesh for WhatsApp, alarms, and battery sync
                    </div>
                  </div>
                </div>

                <span className="px-2 py-0.5 rounded text-[10px] uppercase font-bold bg-sky-950 text-sky-400 border border-sky-500/30">
                  Mesh Active
                </span>
              </div>
            </div>
          </div>
        )}

        {statusMsg && (
          <div className="mt-4 p-3 rounded-xl border border-emerald-500/40 bg-emerald-950/30 text-emerald-300 text-xs flex items-center space-x-1.5">
            <Check className="w-4 h-4" />
            <span>{statusMsg}</span>
          </div>
        )}
      </div>
    </div>
  );
};
