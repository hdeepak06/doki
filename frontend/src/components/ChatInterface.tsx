import React, { useState, useEffect, useRef } from 'react';
import {
  Send,
  Mic,
  MicOff,
  Trash2,
  User,
  Sparkles,
  Volume2,
  VolumeX,
  Languages,
  Plus,
  Copy,
  Check,
  RotateCcw,
  Square
} from 'lucide-react';
import type { Message } from '../types';
import { streamChat, clearAllConversations } from '../services/api';
import { AudioWaveform } from './AudioWaveform';

interface ChatInterfaceProps {
  activeModel: string;
  onModelClick?: () => void;
}

export const ChatInterface: React.FC<ChatInterfaceProps> = ({ activeModel, onModelClick }) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [currentConvId, setCurrentConvId] = useState<string | null>(null);
  const [isListening, setIsListening] = useState(false);
  const [ttsEnabled, setTtsEnabled] = useState(true);
  const [voiceLang, setVoiceLang] = useState<'en-US' | 'kn-IN'>('en-US');
  const [copiedIdx, setCopiedIdx] = useState<number | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const recognitionRef = useRef<any>(null);
  const abortControllerRef = useRef<boolean>(false);

  useEffect(() => {
    initSpeechRecognition(voiceLang);
  }, [voiceLang]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isStreaming]);

  const initSpeechRecognition = (lang: string) => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (SpeechRecognition) {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort();
        } catch {}
      }
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = true;
      recognition.lang = lang;

      recognition.onstart = () => {
        setIsListening(true);
      };

      recognition.onresult = (event: any) => {
        let transcript = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
          transcript += event.results[i][0].transcript;
        }
        setInput(transcript);
      };

      recognition.onerror = () => {
        setIsListening(false);
      };

      recognition.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognition;
    }
  };

  const toggleMic = () => {
    if (!recognitionRef.current) {
      alert('Speech recognition is not supported in this browser. Please use Chrome/Edge.');
      return;
    }
    if (isListening) {
      recognitionRef.current.stop();
    } else {
      try {
        recognitionRef.current.lang = voiceLang;
        recognitionRef.current.start();
      } catch {
        initSpeechRecognition(voiceLang);
        recognitionRef.current?.start();
      }
    }
  };

  const speakText = (text: string) => {
    if (!ttsEnabled || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.0;
    utterance.pitch = 1.0;

    // Detect if text contains Kannada script
    const isKannada = /[\u0C80-\u0CFF]/.test(text);
    utterance.lang = isKannada ? 'kn-IN' : 'en-US';

    const voices = window.speechSynthesis.getVoices();
    if (isKannada) {
      const knVoice = voices.find((v) => v.lang.startsWith('kn') || v.name.toLowerCase().includes('kannada'));
      if (knVoice) utterance.voice = knVoice;
    }

    window.speechSynthesis.speak(utterance);
  };

  const handleSend = async (textToSend?: string) => {
    const raw = textToSend !== undefined ? textToSend : input;
    const trimmed = raw.trim();
    if (!trimmed || isStreaming) return;

    setInput('');
    const userMsg: Message = { role: 'user', content: trimmed };
    setMessages((prev) => [...prev, userMsg]);
    setIsStreaming(true);
    abortControllerRef.current = false;

    let assistantAccumulated = '';
    const tempAssistantMsg: Message = {
      role: 'assistant',
      content: '',
      model: activeModel,
    };

    setMessages((prev) => [...prev, tempAssistantMsg]);

    const newConvId = await streamChat(
      trimmed,
      currentConvId,
      activeModel,
      (chunk) => {
        if (abortControllerRef.current) return;
        assistantAccumulated += chunk;
        setMessages((prev) => {
          const updated = [...prev];
          const lastIdx = updated.length - 1;
          if (lastIdx >= 0 && updated[lastIdx].role === 'assistant') {
            updated[lastIdx] = { ...updated[lastIdx], content: assistantAccumulated };
          }
          return updated;
        });
      },
      (modelUsed, providerUsed) => {
        setMessages((prev) => {
          const updated = [...prev];
          const lastIdx = updated.length - 1;
          if (lastIdx >= 0 && updated[lastIdx].role === 'assistant') {
            updated[lastIdx] = { ...updated[lastIdx], model: modelUsed, provider: providerUsed };
          }
          return updated;
        });
      },
      () => {
        setIsStreaming(false);
        speakText(assistantAccumulated);
      },
      (err) => {
        setIsStreaming(false);
        setMessages((prev) => {
          const updated = [...prev];
          const lastIdx = updated.length - 1;
          if (lastIdx >= 0) {
            updated[lastIdx] = {
              ...updated[lastIdx],
              content: `⚠️ ${err}`,
            };
          }
          return updated;
        });
      }
    );

    if (newConvId && !currentConvId) {
      setCurrentConvId(newConvId);
    }
  };

  const handleStopGeneration = () => {
    abortControllerRef.current = true;
    setIsStreaming(false);
  };

  const handleRegenerate = () => {
    if (isStreaming) return;
    // Find last user message
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role === 'user') {
        const lastQuery = messages[i].content;
        // Remove trailing assistant message
        setMessages((prev) => prev.slice(0, i + 1));
        handleSend(lastQuery);
        break;
      }
    }
  };

  const handleCopy = (text: string, idx: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIdx(idx);
    setTimeout(() => setCopiedIdx(null), 2000);
  };

  const handleNewChat = () => {
    setMessages([]);
    setCurrentConvId(null);
  };

  const handleClearHistory = async () => {
    if (confirm('Clear all conversation logs from database?')) {
      await clearAllConversations();
      setMessages([]);
      setCurrentConvId(null);
    }
  };

  return (
    <div className="flex h-full border border-cyan-500/20 rounded-2xl bg-slate-950/70 backdrop-blur-md overflow-hidden">
      {/* Messages Column */}
      <div className="flex-1 flex flex-col h-full">
        {/* Header inside chat */}
        <div className="px-6 py-3 border-b border-cyan-500/10 flex items-center justify-between font-mono text-xs text-slate-400">
          <div
            onClick={onModelClick}
            className="flex items-center space-x-2 cursor-pointer hover:text-cyan-300 transition-colors"
          >
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
            <span className="text-cyan-300 font-bold">NEURAL CHAT // {activeModel}</span>
          </div>

          <div className="flex items-center space-x-2.5">
            {/* New Chat Button */}
            <button
              onClick={handleNewChat}
              className="flex items-center space-x-1 px-2.5 py-1 rounded-lg border border-cyan-500/30 bg-cyan-950/30 text-cyan-300 hover:bg-cyan-900/40 text-[11px] font-mono transition-colors"
              title="Start a new conversation"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>New Chat</span>
            </button>

            {/* Voice Language Selector Pill */}
            <button
              onClick={() => setVoiceLang(voiceLang === 'en-US' ? 'kn-IN' : 'en-US')}
              className="flex items-center space-x-1.5 px-2.5 py-1 rounded-lg border border-cyan-500/30 bg-cyan-950/30 text-cyan-300 hover:bg-cyan-900/40 text-[11px] font-mono transition-colors"
              title="Toggle Speech Recognition language"
            >
              <Languages className="w-3.5 h-3.5 text-cyan-400" />
              <span>{voiceLang === 'kn-IN' ? 'ಕನ್ನಡ' : 'English'}</span>
            </button>

            <button
              onClick={() => setTtsEnabled(!ttsEnabled)}
              className={`p-1.5 rounded-lg border transition-colors ${
                ttsEnabled ? 'border-cyan-500/40 text-cyan-300 bg-cyan-950/40' : 'border-slate-800 text-slate-500'
              }`}
              title={ttsEnabled ? 'Voice output enabled' : 'Voice output muted'}
            >
              {ttsEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>

            <button
              onClick={handleClearHistory}
              className="p-1.5 rounded-lg border border-slate-800 text-slate-500 hover:text-red-400 hover:border-red-500/30 transition-colors"
              title="Clear all messages"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Message Stream */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5">
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center space-y-4 max-w-md mx-auto">
              <div className="p-4 rounded-full bg-cyan-950/40 border border-cyan-500/30 text-cyan-400 jarvis-glow">
                <Sparkles className="w-8 h-8 animate-pulse" />
              </div>
              <div className="space-y-1">
                <h3 className="font-mono text-base font-bold text-slate-200">
                  DOKI NEURAL CHAT INITIALIZED
                </h3>
                <p className="text-xs text-slate-400 font-mono">
                  Ask questions, compose code, execute laptop commands, or speak in Kannada, Hindi and English.
                </p>
              </div>

              {/* Sample Prompts */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full pt-2 font-mono text-xs">
                {[
                  'ನನ್ನ ಲ್ಯಾಪ್ಟಾಪ್ RAM ಎಷ್ಟು ಬಳಕೆಯಾಗಿದೆ?',
                  'DOKI, Chrome open madu',
                  'Explain quantum computing in simple terms',
                  'ಕನ್ನಡದಲ್ಲಿ ಒಂದು ಸ್ಪೂರ್ತಿದಾಯಕ ಮಾತು ಹೇಳು'
                ].map((chip, i) => (
                  <button
                    key={i}
                    onClick={() => handleSend(chip)}
                    className="px-3 py-2 rounded-xl border border-cyan-500/30 bg-cyan-950/20 text-cyan-300 hover:bg-cyan-900/50 hover:border-cyan-400 transition-all text-left"
                  >
                    💬 {chip}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((m, idx) => {
              const isUser = m.role === 'user';
              return (
                <div
                  key={idx}
                  className={`flex items-start space-x-3 ${isUser ? 'flex-row-reverse space-x-reverse' : ''}`}
                >
                  <div
                    className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 border ${
                      isUser
                        ? 'border-blue-500/40 bg-blue-950/50 text-blue-300'
                        : 'border-cyan-500/40 bg-cyan-950/50 text-cyan-300 jarvis-glow'
                    }`}
                  >
                    {isUser ? <User className="w-4 h-4" /> : <Sparkles className="w-4 h-4" />}
                  </div>

                  <div
                    className={`max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap group relative ${
                      isUser
                        ? 'bg-blue-600/20 border border-blue-500/30 text-blue-100 rounded-tr-none'
                        : 'bg-slate-900/80 border border-cyan-500/20 text-slate-200 rounded-tl-none font-sans'
                    }`}
                  >
                    <div>{m.content || (isStreaming && idx === messages.length - 1 ? '...' : '')}</div>

                    {/* Model badge on assistant response */}
                    {!isUser && m.model && (
                      <div className="mt-2 text-[10px] font-mono text-cyan-500/80 flex items-center justify-between pt-1 border-t border-slate-800">
                        <span>
                          {m.provider === 'openrouter' ? '⚡ OpenRouter Fallback: ' : '✨ Google Gemini: '}
                          {m.model}
                        </span>
                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => handleCopy(m.content, idx)}
                            className="text-slate-400 hover:text-cyan-300 flex items-center space-x-1"
                            title="Copy response"
                          >
                            {copiedIdx === idx ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                            <span>{copiedIdx === idx ? 'Copied' : 'Copy'}</span>
                          </button>
                        </div>
                      </div>
                    )}

                    {!isUser && (m.content.includes('OpenRouter API Key') || m.content.includes('401')) && onModelClick && (
                      <button
                        onClick={onModelClick}
                        className="mt-3 inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-cyan-400/60 bg-cyan-950/80 hover:bg-cyan-900 text-cyan-300 text-xs font-mono transition-all shadow-md cursor-pointer"
                      >
                        ⚙️ Configure OpenRouter Key
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Waveform indicator when listening */}
        {isListening && (
          <div className="px-6 py-2 bg-slate-900/60 border-t border-cyan-500/20 flex items-center justify-between">
            <span className="font-mono text-xs text-emerald-400 animate-pulse">
              LISTENING ({voiceLang === 'kn-IN' ? 'ಕನ್ನಡ' : 'English'})...
            </span>
            <AudioWaveform active={true} color="#10B981" height={28} />
          </div>
        )}

        {/* Bottom Action Bar (Stop Generation / Regenerate) */}
        {isStreaming && (
          <div className="px-6 py-1.5 bg-slate-900/70 border-t border-cyan-500/20 flex items-center justify-center">
            <button
              onClick={handleStopGeneration}
              className="flex items-center space-x-1.5 px-3 py-1 rounded-lg border border-amber-500/40 bg-amber-950/30 text-amber-300 text-xs font-mono hover:bg-amber-900/50"
            >
              <Square className="w-3.5 h-3.5" />
              <span>Stop Generating</span>
            </button>
          </div>
        )}

        {!isStreaming && messages.length > 0 && messages[messages.length - 1].role === 'assistant' && (
          <div className="px-6 py-1 bg-slate-950/50 flex items-center justify-end">
            <button
              onClick={handleRegenerate}
              className="flex items-center space-x-1 text-[11px] font-mono text-slate-400 hover:text-cyan-300 transition-colors"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Regenerate Response</span>
            </button>
          </div>
        )}

        {/* Input Bar */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          className="p-4 border-t border-cyan-500/20 bg-slate-950/80 flex items-center space-x-3"
        >
          <button
            type="button"
            onClick={toggleMic}
            className={`p-3 rounded-xl border transition-all ${
              isListening
                ? 'border-emerald-500 bg-emerald-950 text-emerald-400 shadow-[0_0_15px_#10B981]'
                : 'border-cyan-500/30 bg-cyan-950/30 text-cyan-400 hover:bg-cyan-900/40'
            }`}
            title={`Voice input (${voiceLang})`}
          >
            {isListening ? <MicOff className="w-5 h-5 animate-pulse" /> : <Mic className="w-5 h-5" />}
          </button>

          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={
              voiceLang === 'kn-IN'
                ? 'ಕನ್ನಡದಲ್ಲಿ ಮಾತನಾಡಿ ಅಥವಾ ಟೈಪ್ ಮಾಡಿ (ಉದಾ: ಲ್ಯಾಪ್ಟಾಪ್ RAM ಎಷ್ಟು?)...'
                : 'Talk to DOKI or enter a command (English, Kannada, Kanglish)...'
            }
            disabled={isStreaming}
            className="flex-1 bg-slate-900/80 border border-cyan-500/30 rounded-xl px-4 py-3 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-400 transition-colors font-mono"
          />

          <button
            type="submit"
            disabled={!input.trim() || isStreaming}
            className="p-3 rounded-xl border border-cyan-500/40 bg-gradient-to-r from-cyan-600 to-blue-600 text-white font-bold hover:from-cyan-500 hover:to-blue-500 disabled:opacity-40 disabled:cursor-not-allowed transition-all jarvis-glow"
            title="Send"
          >
            <Send className="w-5 h-5" />
          </button>
        </form>
      </div>
    </div>
  );
};
