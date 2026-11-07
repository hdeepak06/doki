import React, { useState, useEffect, useRef } from 'react';
import { Send, Mic, MicOff, Trash2, Bot, User, Sparkles, Volume2, VolumeX } from 'lucide-react';
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

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const recognitionRef = useRef<any>(null);

  useEffect(() => {
    initSpeechRecognition();
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isStreaming]);

  const initSpeechRecognition = () => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (SpeechRecognition) {
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = true;
      recognition.lang = 'en-US';

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
      recognitionRef.current.start();
    }
  };

  const speakText = (text: string) => {
    if (!ttsEnabled || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.05;
    utterance.pitch = 1.0;
    window.speechSynthesis.speak(utterance);
  };

  const handleSend = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || isStreaming) return;

    setInput('');
    const userMsg: Message = { role: 'user', content: trimmed };
    setMessages((prev) => [...prev, userMsg]);
    setIsStreaming(true);

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
      () => {},
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

  const handleClearHistory = async () => {
    if (confirm('Clear all conversation logs?')) {
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

          <div className="flex items-center space-x-3">
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
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center text-slate-500 font-mono space-y-3">
              <Bot className="w-12 h-12 text-cyan-500/40 animate-pulse" />
              <p className="text-cyan-400 font-bold">"Hello Deepak. DOKI is online and standing by."</p>
              <p className="text-xs text-slate-400 max-w-md">
                Ask questions, execute Windows or Android tasks, research topics, or speak in English, Kannada, or Hindi.
              </p>
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
                    className={`max-w-[75%] rounded-2xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap ${
                      isUser
                        ? 'bg-blue-600/20 border border-blue-500/30 text-blue-100 rounded-tr-none'
                        : 'bg-slate-900/80 border border-cyan-500/20 text-slate-200 rounded-tl-none font-sans'
                    }`}
                  >
                    {m.content || (isStreaming && idx === messages.length - 1 ? '...' : '')}
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
            <span className="font-mono text-xs text-emerald-400 animate-pulse">LISTENING ACTIVE...</span>
            <AudioWaveform active={true} color="#10B981" height={28} />
          </div>
        )}

        {/* Input Bar */}
        <form onSubmit={handleSend} className="p-4 border-t border-cyan-500/20 bg-slate-950/80 flex items-center space-x-3">
          <button
            type="button"
            onClick={toggleMic}
            className={`p-3 rounded-xl border transition-all ${
              isListening
                ? 'border-emerald-500 bg-emerald-950 text-emerald-400 shadow-[0_0_15px_#10B981]'
                : 'border-cyan-500/30 bg-cyan-950/30 text-cyan-400 hover:bg-cyan-900/40'
            }`}
            title="Voice input"
          >
            {isListening ? <MicOff className="w-5 h-5 animate-pulse" /> : <Mic className="w-5 h-5" />}
          </button>

          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Talk to DOKI or enter a command (English, Kannada, Hindi)..."
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
