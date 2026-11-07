import React, { useState, useEffect } from 'react';
import { Plus, Trash2, Search, Brain, Check, RefreshCw } from 'lucide-react';

interface MemoryItem {
  id: string;
  key: string;
  value: string;
  category: string;
  created_at: string;
  updated_at: string;
}

export const MemoryView: React.FC = () => {
  const [memories, setMemories] = useState<MemoryItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState('all');
  const [isAdding, setIsAdding] = useState(false);
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');
  const [newCategory, setNewCategory] = useState('general');
  const [isLoading, setIsLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');

  const categories = ['all', 'general', 'preference', 'work', 'system'];

  useEffect(() => {
    loadMemories();
  }, [activeCategory, searchQuery]);

  const loadMemories = async () => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams();
      if (activeCategory !== 'all') params.append('category', activeCategory);
      if (searchQuery.trim()) params.append('q', searchQuery.trim());

      const res = await fetch(`/api/memory?${params.toString()}`);
      if (res.ok) {
        const data = await res.json();
        setMemories(data);
      }
    } catch {
      // Ignore
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveMemory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newKey.trim() || !newValue.trim()) return;

    try {
      const res = await fetch('/api/memory', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          key: newKey.trim(),
          value: newValue.trim(),
          category: newCategory
        })
      });

      if (res.ok) {
        setNewKey('');
        setNewValue('');
        setIsAdding(false);
        setStatusMsg('Memory successfully saved and linked to DOKI.');
        setTimeout(() => setStatusMsg(''), 2500);
        loadMemories();
      }
    } catch {
      setStatusMsg('Failed to save memory.');
    }
  };

  const handleDeleteMemory = async (id: string) => {
    if (!confirm('Are you sure you want DOKI to forget this memory?')) return;
    try {
      const res = await fetch(`/api/memory/${id}`, { method: 'DELETE' });
      if (res.ok) {
        setMemories((prev) => prev.filter((m) => m.id !== id));
      }
    } catch {
      alert('Failed to delete memory.');
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 select-none relative z-10 pb-8">
      {/* Top Header Card */}
      <div className="p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 backdrop-blur-md jarvis-glow">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-xl bg-cyan-950/60 border border-cyan-500/40 text-cyan-400">
              <Brain className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h1 className="text-xl font-bold font-mono tracking-wide text-slate-100">
                PERSISTENT NEURAL MEMORY
              </h1>
              <p className="text-xs text-slate-400 font-mono">
                DOKI injects these saved facts into conversation prompts to maintain continuous long-term recall.
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setIsAdding(!isAdding)}
              className="px-4 py-2 rounded-xl border border-cyan-500/50 bg-cyan-950/60 hover:bg-cyan-900/60 text-cyan-300 font-mono text-xs font-bold transition-all flex items-center space-x-1.5"
            >
              <Plus className="w-4 h-4" />
              <span>{isAdding ? 'Cancel' : 'Teach DOKI'}</span>
            </button>
            <button
              onClick={loadMemories}
              className="p-2 rounded-xl border border-slate-800 bg-slate-900/60 text-slate-400 hover:text-cyan-300 transition-colors"
              title="Refresh"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Add Memory Modal/Drawer */}
        {isAdding && (
          <form onSubmit={handleSaveMemory} className="mt-5 p-4 rounded-xl border border-cyan-500/30 bg-slate-900/90 space-y-3 font-mono text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="text-slate-400 block mb-1">FACT / TOPIC (KEY)</label>
                <input
                  type="text"
                  value={newKey}
                  onChange={(e) => setNewKey(e.target.value)}
                  placeholder="e.g. Primary Laptop"
                  required
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:border-cyan-400"
                />
              </div>
              <div className="sm:col-span-2">
                <label className="text-slate-400 block mb-1">DETAIL (VALUE)</label>
                <input
                  type="text"
                  value={newValue}
                  onChange={(e) => setNewValue(e.target.value)}
                  placeholder="e.g. Windows 11 ASUS ROG with 16GB RAM"
                  required
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-slate-200 focus:outline-none focus:border-cyan-400"
                />
              </div>
            </div>

            <div className="flex items-center justify-between pt-2">
              <div className="flex items-center space-x-2">
                <span className="text-slate-400">Category:</span>
                <select
                  value={newCategory}
                  onChange={(e) => setNewCategory(e.target.value)}
                  className="bg-slate-950 border border-slate-700 rounded-lg px-2.5 py-1 text-slate-200 focus:outline-none"
                >
                  <option value="general">General</option>
                  <option value="preference">Preference</option>
                  <option value="work">Work</option>
                  <option value="system">System</option>
                </select>
              </div>

              <button
                type="submit"
                className="px-4 py-1.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold tracking-wide transition-colors"
              >
                Store Fact
              </button>
            </div>
          </form>
        )}

        {statusMsg && (
          <div className="mt-3 text-xs font-mono text-emerald-400 flex items-center space-x-1">
            <Check className="w-4 h-4" />
            <span>{statusMsg}</span>
          </div>
        )}
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Category Pills */}
        <div className="flex items-center space-x-1.5 bg-slate-900/60 p-1.5 rounded-xl border border-slate-800 font-mono text-xs">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className={`px-3 py-1 rounded-lg uppercase tracking-wider transition-all ${
                activeCategory === cat
                  ? 'bg-cyan-950 border border-cyan-500/50 text-cyan-300 font-bold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-64">
          <Search className="w-3.5 h-3.5 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search stored facts..."
            className="w-full bg-slate-900/80 border border-slate-800 rounded-xl pl-9 pr-3 py-1.5 text-xs text-slate-200 font-mono focus:outline-none focus:border-cyan-400"
          />
        </div>
      </div>

      {/* Memories Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {memories.map((m) => (
          <div
            key={m.id}
            className="p-4 rounded-xl border border-slate-800/80 bg-slate-950/70 hover:border-cyan-500/30 transition-all flex items-start justify-between group"
          >
            <div className="space-y-1.5">
              <div className="flex items-center space-x-2">
                <span className="text-xs font-mono font-bold text-cyan-300">{m.key}</span>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full border border-slate-700 bg-slate-900 text-slate-400 uppercase">
                  {m.category}
                </span>
              </div>
              <p className="text-xs text-slate-300 font-sans leading-relaxed">{m.value}</p>
            </div>

            <button
              onClick={() => handleDeleteMemory(m.id)}
              className="p-1.5 rounded-lg text-slate-600 hover:text-red-400 hover:bg-red-950/30 transition-colors shrink-0 ml-2"
              title="Forget memory"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          </div>
        ))}

        {memories.length === 0 && !isLoading && (
          <div className="col-span-full p-8 text-center rounded-2xl border border-dashed border-slate-800 text-slate-500 font-mono text-xs">
            No memories stored yet. Click "Teach DOKI" or tell DOKI "Remember that..." in Chat!
          </div>
        )}
      </div>
    </div>
  );
};
