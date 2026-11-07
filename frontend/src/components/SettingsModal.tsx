import React, { useState, useEffect } from 'react';
import { X, Key, Cpu, Check, RefreshCw, AlertCircle } from 'lucide-react';
import type { SystemConfig, ModelInfo } from '../types';
import { updateConfig, fetchModels, selectModel } from '../services/api';

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
  const [apiKeyInput, setApiKeyInput] = useState('');
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [activeModel, setActiveModel] = useState(config?.active_model || 'openrouter/free');
  const [isSaving, setIsSaving] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');
  const [isRefreshingModels, setIsRefreshingModels] = useState(false);

  useEffect(() => {
    if (isOpen) {
      loadModels();
      setActiveModel(config?.active_model || 'openrouter/free');
    }
  }, [isOpen, config]);

  const loadModels = async (refresh = false) => {
    setIsRefreshingModels(true);
    try {
      const data = await fetchModels(refresh);
      setModels(data.models);
      setActiveModel(data.active_model);
    } catch {
      setStatusMsg('Failed to fetch free models list.');
    } finally {
      setIsRefreshingModels(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setStatusMsg('');

    try {
      const updates: { openrouter_api_key?: string; active_model?: string } = {};
      if (apiKeyInput.trim()) {
        updates.openrouter_api_key = apiKeyInput.trim();
      }
      if (activeModel) {
        updates.active_model = activeModel;
        await selectModel(activeModel);
      }

      await updateConfig(updates);
      setApiKeyInput('');
      setStatusMsg('Settings successfully updated!');
      onConfigUpdated();
      setTimeout(() => {
        setStatusMsg('');
        onClose();
      }, 1200);
    } catch (err) {
      setStatusMsg('Error saving settings: ' + (err instanceof Error ? err.message : 'Unknown'));
    } finally {
      setIsSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
      <div className="w-full max-w-xl rounded-2xl border border-cyan-500/30 bg-slate-950 p-6 shadow-2xl jarvis-glow">
        <div className="flex items-center justify-between pb-4 border-b border-cyan-500/20">
          <div className="flex items-center space-x-2">
            <Key className="w-5 h-5 text-cyan-400" />
            <h2 className="text-base font-bold font-mono tracking-wide text-slate-100">
              DOKI SYSTEM CONFIGURATION
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-900 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSave} className="mt-5 space-y-5">
          {/* OpenRouter API Key Input */}
          <div className="space-y-2">
            <label className="text-xs font-mono text-cyan-300 flex items-center justify-between">
              <span>OPENROUTER API KEY (Free Tier, Zero Cost)</span>
              <span className="text-slate-500 text-[11px]">
                Current: {config?.masked_api_key || 'Not configured'}
              </span>
            </label>
            <input
              type="password"
              value={apiKeyInput}
              onChange={(e) => setApiKeyInput(e.target.value)}
              placeholder="sk-or-v1-..."
              className="w-full bg-slate-900/90 border border-slate-700 focus:border-cyan-400 rounded-xl px-4 py-2.5 text-sm text-slate-200 font-mono focus:outline-none transition-colors"
            />
            <p className="text-[11px] text-slate-400 font-sans">
              Keys are stored strictly in your local backend environment ({' '}
              <code className="text-cyan-400">backend/data/settings.json</code>) and are never committed or sent to frontend bundles.
            </p>
          </div>

          {/* Dynamic Free Model Selector */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-mono text-cyan-300 flex items-center gap-1.5">
                <Cpu className="w-4 h-4 text-cyan-400" />
                <span>ACTIVE AI MODEL (FREE MODELS ONLY)</span>
              </label>
              <button
                type="button"
                onClick={() => loadModels(true)}
                disabled={isRefreshingModels}
                className="text-[11px] text-cyan-400 hover:underline flex items-center gap-1"
              >
                <RefreshCw className={`w-3 h-3 ${isRefreshingModels ? 'animate-spin' : ''}`} />
                <span>Refresh Live Models</span>
              </button>
            </div>

            <select
              value={activeModel}
              onChange={(e) => setActiveModel(e.target.value)}
              className="w-full bg-slate-900/90 border border-slate-700 focus:border-cyan-400 rounded-xl px-4 py-2.5 text-sm text-slate-200 font-mono focus:outline-none transition-colors"
            >
              {models.map((m) => (
                <option key={m.id} value={m.id} className="bg-slate-900 text-slate-200">
                  {m.name} ({m.id})
                </option>
              ))}
            </select>
          </div>

          {/* Status Message */}
          {statusMsg && (
            <div
              className={`p-3 rounded-lg text-xs font-mono flex items-center space-x-2 ${
                statusMsg.includes('Error')
                  ? 'bg-red-950/60 border border-red-500/40 text-red-300'
                  : 'bg-emerald-950/60 border border-emerald-500/40 text-emerald-300'
              }`}
            >
              {statusMsg.includes('Error') ? <AlertCircle className="w-4 h-4" /> : <Check className="w-4 h-4" />}
              <span>{statusMsg}</span>
            </div>
          )}

          {/* Action Footer */}
          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-mono text-slate-400 hover:text-slate-200 hover:bg-slate-900 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="px-5 py-2.5 rounded-xl border border-cyan-400/50 bg-gradient-to-r from-cyan-600 to-blue-600 text-white font-mono text-xs font-bold hover:from-cyan-500 hover:to-blue-500 disabled:opacity-50 transition-all jarvis-glow"
            >
              {isSaving ? 'Saving...' : 'Save Configuration'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
