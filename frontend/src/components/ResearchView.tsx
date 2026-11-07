import React, { useState } from 'react';
import { Search, Globe, ExternalLink, Sparkles, BookOpen, AlertCircle } from 'lucide-react';
import { getStoredApiKey } from '../services/api';

interface ResearchSource {
  title: string;
  url: string;
  snippet: string;
}

interface ResearchResult {
  query: string;
  synthesis: string;
  sources: ResearchSource[];
  model_used: string;
}

export const ResearchView: React.FC = () => {
  const [query, setQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<ResearchResult | null>(null);
  const [error, setError] = useState('');

  const sampleQueries = [
    'Latest advancements in open-source AI models',
    'What is the current status of Artemis space program?',
    'Bengaluru tech and startup ecosystem news',
    'Compare Python FastAPI vs Go for high-throughput APIs'
  ];

  const handleSearch = async (targetQuery?: string) => {
    const q = targetQuery || query;
    if (!q.trim() || isLoading) return;

    setIsLoading(true);
    setError('');
    setResult(null);

    try {
      const storedKey = getStoredApiKey();
      const res = await fetch('/api/research', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(storedKey ? { 'X-OpenRouter-Key': storedKey } : {})
        },
        body: JSON.stringify({
          query: q.trim(),
          api_key: storedKey || undefined
        })
      });

      if (!res.ok) {
        throw new Error(`Research failed with status ${res.status}`);
      }

      const data = await res.json();
      setResult(data);
    } catch (err: any) {
      setError(err.message || 'Failed to complete research search');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 select-none relative z-10 pb-8">
      {/* Header */}
      <div className="p-6 rounded-2xl border border-cyan-500/20 bg-slate-950/80 backdrop-blur-md jarvis-glow">
        <div className="flex items-center space-x-3 mb-2">
          <div className="p-2 rounded-xl bg-cyan-950/60 border border-cyan-500/40 text-cyan-400">
            <Globe className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <h1 className="text-xl font-bold font-mono tracking-wide text-slate-100">
              INTERNET KNOWLEDGE & DEEP RESEARCH
            </h1>
            <p className="text-xs text-slate-400 font-mono">
              Real-time multi-source web retrieval engine synthesized by DOKI's free AI brain.
            </p>
          </div>
        </div>

        {/* Search Input Bar */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSearch();
          }}
          className="mt-5 flex items-center space-x-2"
        >
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-cyan-400 absolute left-4 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search current affairs, tech research, news, or scientific papers..."
              className="w-full bg-slate-900/90 border border-slate-800 focus:border-cyan-400/70 rounded-xl pl-11 pr-4 py-3 text-sm text-slate-100 font-mono placeholder:text-slate-500 focus:outline-none transition-colors"
            />
          </div>
          <button
            type="submit"
            disabled={isLoading || !query.trim()}
            className="px-6 py-3 rounded-xl border border-cyan-500/50 bg-cyan-950/60 hover:bg-cyan-900/60 text-cyan-300 font-mono text-xs font-bold transition-all disabled:opacity-40 flex items-center space-x-2 jarvis-glow"
          >
            {isLoading ? (
              <>
                <Sparkles className="w-4 h-4 animate-spin text-cyan-400" />
                <span>Synthesizing...</span>
              </>
            ) : (
              <>
                <Search className="w-4 h-4" />
                <span>Research</span>
              </>
            )}
          </button>
        </form>

        {/* Suggested Queries */}
        <div className="mt-3 flex flex-wrap gap-2 pt-2">
          <span className="text-[11px] font-mono text-slate-500 py-1">Quick Suggestions:</span>
          {sampleQueries.map((sq, i) => (
            <button
              key={i}
              type="button"
              onClick={() => {
                setQuery(sq);
                handleSearch(sq);
              }}
              className="text-[11px] font-mono px-2.5 py-1 rounded-lg border border-slate-800 bg-slate-900/50 text-slate-400 hover:text-cyan-300 hover:border-cyan-500/30 transition-all"
            >
              {sq}
            </button>
          ))}
        </div>
      </div>

      {/* Error Notice */}
      {error && (
        <div className="p-4 rounded-xl border border-red-500/40 bg-red-950/30 text-red-300 text-xs font-mono flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
          <span>{error}</span>
        </div>
      )}

      {/* Research Output Section */}
      {result && (
        <div className="space-y-6">
          {/* Synthesized Intelligence Card */}
          <div className="p-6 rounded-2xl border border-cyan-500/30 bg-slate-950/90 backdrop-blur-md space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-cyan-500/20">
              <div className="flex items-center space-x-2 text-cyan-400 font-mono text-xs font-bold">
                <Sparkles className="w-4 h-4" />
                <span>RESEARCH SYNTHESIS // {result.model_used}</span>
              </div>
              <span className="text-[10px] font-mono text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded uppercase">
                Verified Sources Attached
              </span>
            </div>

            <div className="text-sm font-sans text-slate-200 leading-relaxed whitespace-pre-wrap">
              {result.synthesis}
            </div>
          </div>

          {/* Sources Grid */}
          <div className="space-y-3">
            <div className="flex items-center space-x-2 text-xs font-mono text-slate-400">
              <BookOpen className="w-4 h-4 text-cyan-400" />
              <span>RETRIEVED WEB SOURCES ({result.sources.length})</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {result.sources.map((src, idx) => (
                <a
                  key={idx}
                  href={src.url}
                  target="_blank"
                  rel="noreferrer"
                  className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 hover:border-cyan-500/40 hover:bg-slate-900 transition-all flex flex-col justify-between group"
                >
                  <div className="space-y-1.5">
                    <div className="flex items-start justify-between">
                      <span className="text-xs font-mono text-cyan-400 font-bold">[{idx + 1}]</span>
                      <ExternalLink className="w-3.5 h-3.5 text-slate-500 group-hover:text-cyan-300 transition-colors" />
                    </div>
                    <h3 className="text-sm font-bold text-slate-200 line-clamp-2 group-hover:text-cyan-200">
                      {src.title}
                    </h3>
                    <p className="text-xs text-slate-400 font-sans line-clamp-3">
                      {src.snippet}
                    </p>
                  </div>
                  <div className="mt-3 text-[11px] font-mono text-slate-500 truncate">
                    {src.url}
                  </div>
                </a>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
