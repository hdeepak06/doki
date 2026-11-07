import React, { useState, useEffect, useRef } from 'react';
import {
  Mic,
  MicOff,
  Volume2,
  VolumeX,
  Languages,
  Square,
  RotateCcw,
  AlertTriangle,
  Radio,
  Cpu
} from 'lucide-react';
import { JarvisCore } from './JarvisCore';
import { AudioWaveform } from './AudioWaveform';
import { streamChat } from '../services/api';

export type VoiceState =
  | 'INITIALIZING'
  | 'WAITING_FOR_PERMISSION'
  | 'PERMISSION_DENIED'
  | 'READY'
  | 'LISTENING'
  | 'SPEECH_DETECTED'
  | 'TRANSCRIBING'
  | 'PROCESSING'
  | 'SPEAKING'
  | 'RESUMING_LISTENING'
  | 'ERROR'
  | 'STOPPED';

interface VoiceAssistantViewProps {
  activeModel: string;
  onOpenSettings: () => void;
}

export const VoiceAssistantView: React.FC<VoiceAssistantViewProps> = ({
  activeModel,
  onOpenSettings
}) => {
  const [state, setState] = useState<VoiceState>('INITIALIZING');
  const [continuousMode, setContinuousMode] = useState<boolean>(true);
  const [selectedLang, setSelectedLang] = useState<'en-US' | 'kn-IN' | 'hi-IN'>('en-US');
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [liveTranscript, setLiveTranscript] = useState<string>('');
  const [interimText, setInterimText] = useState<string>('');
  const [lastQuestion, setLastQuestion] = useState<string>('');
  const [latestResponse, setLatestResponse] = useState<string>('');
  const [modelUsed, setModelUsed] = useState<string>(activeModel || 'gemini-2.5-flash');
  const [providerUsed, setProviderUsed] = useState<string>('gemini');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [wakeWordTriggered, setWakeWordTriggered] = useState<boolean>(false);
  const [showPermissionPrompt, setShowPermissionPrompt] = useState<boolean>(false);

  // References
  const recognitionRef = useRef<any>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const isSpeakingRef = useRef<boolean>(false);
  const isContinuousRef = useRef<boolean>(true);
  const selectedLangRef = useRef<string>('en-US');

  useEffect(() => {
    isContinuousRef.current = continuousMode;
  }, [continuousMode]);

  useEffect(() => {
    selectedLangRef.current = selectedLang;
  }, [selectedLang]);

  // Audio Chime Generator using Web Audio API
  const playChime = (type: 'activate' | 'done') => {
    try {
      if (!audioContextRef.current) {
        audioContextRef.current = new (window.AudioContext || (window as any).webkitAudioContext)();
      }
      const ctx = audioContextRef.current;
      if (ctx.state === 'suspended') {
        ctx.resume();
      }
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);

      if (type === 'activate') {
        osc.frequency.setValueAtTime(440, ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.12);
        gain.gain.setValueAtTime(0.15, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2);
        osc.start();
        osc.stop(ctx.currentTime + 0.2);
      } else {
        osc.frequency.setValueAtTime(600, ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(300, ctx.currentTime + 0.15);
        gain.gain.setValueAtTime(0.12, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2);
        osc.start();
        osc.stop(ctx.currentTime + 0.2);
      }
    } catch {
      // Handled silently
    }
  };

  // Check permission on mount
  useEffect(() => {
    checkMicrophonePermission();
    return () => {
      stopAllVoiceCapture();
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    };
  }, []);

  const checkMicrophonePermission = async () => {
    if (navigator.permissions && navigator.permissions.query) {
      try {
        const status = await navigator.permissions.query({ name: 'microphone' as PermissionName });
        if (status.state === 'granted') {
          initMicrophoneCapture();
          return;
        } else if (status.state === 'prompt') {
          setState('WAITING_FOR_PERMISSION');
          setShowPermissionPrompt(true);
          return;
        } else if (status.state === 'denied') {
          setState('PERMISSION_DENIED');
          setErrorMessage('Microphone access is blocked in your browser settings. Please allow access in the address bar.');
          return;
        }
      } catch {
        // Fallback for browsers without permissions.query for microphone
      }
    }
    // Show prompt if unsure
    setState('WAITING_FOR_PERMISSION');
    setShowPermissionPrompt(true);
  };

  const handleGrantPermission = async () => {
    setShowPermissionPrompt(false);
    setState('INITIALIZING');
    setErrorMessage('');

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setState('ERROR');
      setErrorMessage('Audio recording is not supported in this browser. Please use Chrome or Edge.');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      // Stop probe track
      stream.getTracks().forEach((track) => track.stop());
      initMicrophoneCapture();
    } catch (err: any) {
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        setState('PERMISSION_DENIED');
        setErrorMessage('Microphone permission was denied. Click the lock/tune icon in your address bar and select "Allow" for Microphone.');
      } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
        setState('ERROR');
        setErrorMessage('No microphone detected on your device. Please connect a microphone or headset and try again.');
      } else {
        setState('ERROR');
        setErrorMessage(`Microphone initialization error: ${err.message || 'Cannot access device'}`);
      }
    }
  };

  const initMicrophoneCapture = () => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setState('ERROR');
      setErrorMessage('Speech recognition is not supported in this browser. Please use Google Chrome or Microsoft Edge.');
      return;
    }

    if (recognitionRef.current) {
      try {
        recognitionRef.current.abort();
      } catch {}
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = selectedLangRef.current;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      if (!isSpeakingRef.current) {
        setState('LISTENING');
        setErrorMessage('');
      }
    };

    recognition.onspeechstart = () => {
      if (!isSpeakingRef.current) {
        setState('SPEECH_DETECTED');
      }
    };

    recognition.onresult = (event: any) => {
      if (isSpeakingRef.current) return;

      let interim = '';
      let finalTranscript = '';

      for (let i = event.resultIndex; i < event.results.length; ++i) {
        const transcriptPart = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          finalTranscript += transcriptPart;
        } else {
          interim += transcriptPart;
        }
      }

      if (interim) {
        setInterimText(interim);
      }

      if (finalTranscript.trim()) {
        setInterimText('');
        handleSpokenInput(finalTranscript.trim());
      }
    };

    recognition.onerror = (event: any) => {
      if (event.error === 'no-speech') return;
      if (event.error === 'audio-capture') {
        setState('ERROR');
        setErrorMessage('Microphone capture failed. Verify if another application is holding the audio lock.');
      } else if (event.error === 'not-allowed') {
        setState('PERMISSION_DENIED');
        setErrorMessage('Microphone access revoked. Allow microphone in browser settings.');
      } else if (event.error === 'network') {
        if (isContinuousRef.current && !isSpeakingRef.current) {
          setTimeout(() => startListening(), 1000);
        }
      }
    };

    recognition.onend = () => {
      if (isSpeakingRef.current) return;
      if (isContinuousRef.current) {
        try {
          recognition.start();
        } catch {}
      } else {
        setState('STOPPED');
      }
    };

    recognitionRef.current = recognition;
    startListening();
  };

  const startListening = () => {
    if (!recognitionRef.current) {
      handleGrantPermission();
      return;
    }
    try {
      recognitionRef.current.lang = selectedLangRef.current;
      recognitionRef.current.start();
      setState('LISTENING');
    } catch {
      try {
        recognitionRef.current.abort();
        setTimeout(() => {
          recognitionRef.current?.start();
          setState('LISTENING');
        }, 150);
      } catch {}
    }
  };

  const stopAllVoiceCapture = () => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.abort();
      } catch {}
    }
    setState('STOPPED');
  };

  // Continuous loop: Process speech -> Gemini AI -> OpenRouter Fallback -> TTS -> Resume listening
  const handleSpokenInput = async (spokenText: string) => {
    if (!spokenText || spokenText.length < 2) return;

    setLiveTranscript(spokenText);
    setState('TRANSCRIBING');

    const lower = spokenText.toLowerCase();
    const hasWakeWord =
      lower.includes('hey doki') ||
      lower.includes('hello doki') ||
      lower.startsWith('doki') ||
      lower.includes('ಡೋಕಿ');

    let cleanedQuery = spokenText;
    if (hasWakeWord) {
      setWakeWordTriggered(true);
      playChime('activate');
      cleanedQuery = spokenText
        .replace(/^(hey doki|hello doki|doki|ಡೋಕಿ)[,\s]*/i, '')
        .trim();
    }

    if (hasWakeWord && !cleanedQuery) {
      const ack = selectedLang === 'kn-IN' ? 'ಹೌದು ದೀಪಕ್ ಸರ್, ನಾನು ಕೇಳುತ್ತಿದ್ದೇನೆ.' : 'Yes Deepak, I am listening.';
      speakResponse(ack);
      return;
    }

    if (!cleanedQuery) return;

    setLastQuestion(cleanedQuery);
    setState('PROCESSING');

    // Pause recognition during AI generation and TTS speaking
    pauseRecognition();

    let accumulatedAnswer = '';

    try {
      await streamChat(
        cleanedQuery,
        null,
        activeModel || 'gemini-2.5-flash',
        (chunk) => {
          accumulatedAnswer += chunk;
          setLatestResponse(accumulatedAnswer);
        },
        (model, provider) => {
          setModelUsed(model);
          if (provider) setProviderUsed(provider);
        },
        () => {
          speakResponse(accumulatedAnswer);
        },
        (errorStr) => {
          setState('ERROR');
          setErrorMessage(errorStr);
          speakResponse('Sorry Deepak, an error occurred while generating the response.');
        }
      );
    } catch (err: any) {
      setState('ERROR');
      setErrorMessage(err.message || 'Error processing speech request');
      resumeListeningAfterSpeaking();
    }
  };

  const pauseRecognition = () => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.abort();
      } catch {}
    }
  };

  const speakResponse = (text: string) => {
    if (isMuted || !('speechSynthesis' in window) || !text) {
      resumeListeningAfterSpeaking();
      return;
    }

    setState('SPEAKING');
    isSpeakingRef.current = true;
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.0;
    utterance.pitch = 1.0;

    const isKannada = /[\u0C80-\u0CFF]/.test(text);
    const isHindi = /[\u0900-\u097F]/.test(text);

    if (isKannada) {
      utterance.lang = 'kn-IN';
    } else if (isHindi) {
      utterance.lang = 'hi-IN';
    } else {
      utterance.lang = selectedLang;
    }

    const voices = window.speechSynthesis.getVoices();
    if (isKannada) {
      const knVoice = voices.find((v) => v.lang.startsWith('kn') || v.name.toLowerCase().includes('kannada'));
      if (knVoice) utterance.voice = knVoice;
    } else if (isHindi) {
      const hiVoice = voices.find((v) => v.lang.startsWith('hi') || v.name.toLowerCase().includes('hindi'));
      if (hiVoice) utterance.voice = hiVoice;
    }

    utterance.onend = () => {
      isSpeakingRef.current = false;
      resumeListeningAfterSpeaking();
    };

    utterance.onerror = () => {
      isSpeakingRef.current = false;
      resumeListeningAfterSpeaking();
    };

    window.speechSynthesis.speak(utterance);
  };

  const resumeListeningAfterSpeaking = () => {
    isSpeakingRef.current = false;
    if (isContinuousRef.current) {
      setState('RESUMING_LISTENING');
      setTimeout(() => {
        startListening();
      }, 350);
    } else {
      setState('READY');
    }
  };

  const handleHaltSpeaking = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    isSpeakingRef.current = false;
    resumeListeningAfterSpeaking();
  };

  const coreState =
    state === 'SPEAKING'
      ? 'speaking'
      : state === 'LISTENING' || state === 'SPEECH_DETECTED'
      ? 'listening'
      : state === 'PROCESSING' || state === 'TRANSCRIBING'
      ? 'thinking'
      : 'idle';

  return (
    <div className="max-w-4xl mx-auto h-full flex flex-col items-center justify-between p-4 space-y-5 select-none relative z-10">
      {/* Microphone Permission Modal (Section 6) */}
      {showPermissionPrompt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
          <div className="w-full max-w-md rounded-2xl border border-cyan-500/40 bg-slate-950 p-6 shadow-2xl jarvis-glow space-y-4 font-mono text-center">
            <div className="w-14 h-14 mx-auto rounded-full bg-cyan-950/80 border border-cyan-400 flex items-center justify-center text-cyan-400">
              <Mic className="w-7 h-7 animate-pulse" />
            </div>

            <div className="space-y-1.5">
              <h2 className="text-base font-bold tracking-wider text-slate-100">
                DOKI NEEDS MICROPHONE ACCESS
              </h2>
              <p className="text-xs text-slate-400 leading-relaxed font-sans">
                Allow microphone access so DOKI can hear your voice commands, understand your questions in Kannada, Hindi and English, and respond naturally.
              </p>
            </div>

            <div className="flex items-center justify-center gap-3 pt-3">
              <button
                onClick={handleGrantPermission}
                className="px-5 py-2.5 rounded-xl border border-cyan-400 bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold transition-all shadow-lg"
              >
                ALLOW MICROPHONE ACCESS
              </button>
              <button
                onClick={() => {
                  setShowPermissionPrompt(false);
                  setState('STOPPED');
                }}
                className="px-4 py-2.5 rounded-xl border border-slate-800 bg-slate-900 text-slate-400 hover:text-slate-200 text-xs transition-colors"
              >
                NOT NOW
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Top Status & Controls Header */}
      <div className="w-full flex items-center justify-between bg-slate-900/60 border border-cyan-500/20 backdrop-blur-md rounded-2xl px-5 py-3">
        <div className="flex items-center space-x-3">
          <div
            className={`w-3 h-3 rounded-full ${
              state === 'LISTENING'
                ? 'bg-emerald-400 animate-ping'
                : state === 'SPEAKING'
                ? 'bg-cyan-400 animate-pulse'
                : state === 'PROCESSING'
                ? 'bg-amber-400 animate-spin'
                : state === 'PERMISSION_DENIED' || state === 'ERROR'
                ? 'bg-red-500'
                : 'bg-slate-500'
            }`}
          />
          <span className="font-mono text-xs font-bold text-slate-200 tracking-wider">
            VOICE ENGINE // {state.replace('_', ' ')}
          </span>
        </div>

        {/* Language & Continuous Mode Switch */}
        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-1.5 bg-slate-950 border border-cyan-500/30 rounded-xl px-2.5 py-1 text-xs font-mono">
            <Languages className="w-3.5 h-3.5 text-cyan-400" />
            <select
              value={selectedLang}
              onChange={(e) => {
                const newLang = e.target.value as 'en-US' | 'kn-IN' | 'hi-IN';
                setSelectedLang(newLang);
                if (recognitionRef.current) {
                  recognitionRef.current.lang = newLang;
                }
              }}
              className="bg-transparent text-cyan-300 focus:outline-none cursor-pointer"
            >
              <option value="en-US" className="bg-slate-900 text-slate-100">
                English (en-US)
              </option>
              <option value="kn-IN" className="bg-slate-900 text-slate-100">
                ಕನ್ನಡ (Kannada)
              </option>
              <option value="hi-IN" className="bg-slate-900 text-slate-100">
                हिन्दी (Hindi)
              </option>
            </select>
          </div>

          <button
            onClick={() => {
              const next = !continuousMode;
              setContinuousMode(next);
              if (!next && state === 'LISTENING') {
                stopAllVoiceCapture();
              } else if (next && state === 'STOPPED') {
                startListening();
              }
            }}
            className={`px-3 py-1 rounded-xl text-xs font-mono border transition-all ${
              continuousMode
                ? 'border-emerald-500/50 bg-emerald-950/40 text-emerald-300'
                : 'border-slate-800 bg-slate-950 text-slate-500'
            }`}
          >
            Continuous: {continuousMode ? 'ON' : 'OFF'}
          </button>
        </div>
      </div>

      {/* Main Holographic Core */}
      <div className="flex flex-col items-center justify-center space-y-4 my-auto">
        <div className="relative">
          <JarvisCore state={coreState} size={280} onClick={handleHaltSpeaking} />
          {wakeWordTriggered && (
            <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full border border-cyan-400 bg-cyan-950/90 text-cyan-200 text-[11px] font-mono tracking-wider animate-bounce shadow-lg">
              ✨ DOKI ACTIVATED
            </div>
          )}
        </div>

        {/* Live Audio Activity Waveform */}
        <div className="w-full max-w-md">
          <AudioWaveform
            active={state === 'LISTENING' || state === 'SPEECH_DETECTED' || state === 'SPEAKING'}
            color={state === 'SPEAKING' ? '#00f0ff' : state === 'LISTENING' ? '#10b981' : '#f59e0b'}
            height={44}
          />
        </div>

        {/* Wake Word Guide */}
        <p className="font-mono text-xs text-slate-400 text-center max-w-lg">
          Wake Words: <span className="text-cyan-300 font-bold">"Hey DOKI"</span>,{' '}
          <span className="text-cyan-300 font-bold">"Hello DOKI"</span>, or{' '}
          <span className="text-cyan-300 font-bold">"ಡೋಕಿ"</span>.
          <br />
          <span className="text-slate-500 text-[11px]">
            Primary Brain: <strong className="text-cyan-400">Google Gemini</strong> | Automatic Free Fallback: <strong className="text-sky-400">OpenRouter</strong>.
          </span>
        </p>
      </div>

      {/* Live Transcript & Dialogue Console */}
      <div className="w-full bg-slate-950/80 border border-cyan-500/20 rounded-2xl p-4 space-y-3 font-mono text-xs">
        <div className="flex items-start space-x-2">
          <span className="text-cyan-400 shrink-0 font-bold">HEARING:</span>
          <span className="text-slate-200 italic">
            {interimText || liveTranscript || (state === 'LISTENING' ? 'Listening for your voice...' : 'Microphone idle.')}
          </span>
        </div>

        {lastQuestion && (
          <div className="flex items-start space-x-2 pt-1 border-t border-slate-800">
            <span className="text-blue-400 shrink-0 font-bold">QUESTION:</span>
            <span className="text-slate-300">{lastQuestion}</span>
          </div>
        )}

        {latestResponse && (
          <div className="flex items-start space-x-2 pt-1 border-t border-slate-800">
            <span className="text-emerald-400 shrink-0 font-bold">DOKI:</span>
            <span className="text-slate-100 whitespace-pre-wrap">{latestResponse}</span>
          </div>
        )}

        {/* Error / Permission Denied Notice */}
        {errorMessage && (
          <div className="flex items-center justify-between p-3 rounded-xl border border-red-500/40 bg-red-950/30 text-red-300">
            <div className="flex items-center space-x-2">
              <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
              <span>{errorMessage}</span>
            </div>
            {state === 'PERMISSION_DENIED' ? (
              <button
                onClick={handleGrantPermission}
                className="px-2.5 py-1 rounded-lg border border-red-400/50 bg-red-900/50 text-red-200 text-[11px] font-mono hover:bg-red-800"
              >
                Try Again
              </button>
            ) : errorMessage.includes('Key') ? (
              <button
                onClick={onOpenSettings}
                className="px-2.5 py-1 rounded-lg border border-red-400/50 bg-red-900/50 text-red-200 text-[11px] font-mono hover:bg-red-800"
              >
                Configure Key
              </button>
            ) : null}
          </div>
        )}
      </div>

      {/* Bottom Control Actions */}
      <div className="w-full flex flex-wrap items-center justify-between gap-3 bg-slate-900/70 border border-cyan-500/20 backdrop-blur-md rounded-2xl px-5 py-3 font-mono text-xs">
        <div className="flex items-center space-x-2">
          {state === 'LISTENING' ? (
            <button
              onClick={stopAllVoiceCapture}
              className="flex items-center space-x-1.5 px-4 py-2 rounded-xl border border-red-500/40 bg-red-950/40 text-red-300 hover:bg-red-900/50 transition-all font-bold"
            >
              <MicOff className="w-4 h-4" />
              <span>Stop Listening</span>
            </button>
          ) : (
            <button
              onClick={startListening}
              className="flex items-center space-x-1.5 px-4 py-2 rounded-xl border border-emerald-500/50 bg-emerald-950/40 text-emerald-300 hover:bg-emerald-900/50 transition-all font-bold jarvis-glow"
            >
              <Mic className="w-4 h-4" />
              <span>Start Listening</span>
            </button>
          )}

          <button
            onMouseDown={startListening}
            onMouseUp={() => {
              if (!continuousMode) stopAllVoiceCapture();
            }}
            className="hidden sm:flex items-center space-x-1 px-3 py-2 rounded-xl border border-cyan-500/30 bg-cyan-950/30 text-cyan-300 hover:bg-cyan-900/50"
            title="Hold to speak, release to send"
          >
            <Radio className="w-3.5 h-3.5" />
            <span>Push To Talk</span>
          </button>
        </div>

        <div className="flex items-center space-x-2 text-[11px] text-slate-400">
          <Cpu className="w-3.5 h-3.5 text-cyan-400" />
          <span className="text-cyan-300 font-bold truncate max-w-[170px]">
            {providerUsed === 'gemini' ? 'Google Gemini' : 'OpenRouter'}: {modelUsed}
          </span>
        </div>

        <div className="flex items-center space-x-2">
          {state === 'SPEAKING' && (
            <button
              onClick={handleHaltSpeaking}
              className="flex items-center space-x-1 px-3 py-1.5 rounded-xl border border-amber-500/40 bg-amber-950/30 text-amber-300 hover:bg-amber-900/50"
            >
              <Square className="w-3.5 h-3.5" />
              <span>Stop Speaking</span>
            </button>
          )}

          {latestResponse && state !== 'SPEAKING' && (
            <button
              onClick={() => speakResponse(latestResponse)}
              className="flex items-center space-x-1 px-3 py-1.5 rounded-xl border border-slate-700 bg-slate-800/60 text-slate-300 hover:text-cyan-300"
              title="Repeat last response"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Repeat</span>
            </button>
          )}

          <button
            onClick={() => {
              const nextMute = !isMuted;
              setIsMuted(nextMute);
              if (nextMute && 'speechSynthesis' in window) {
                window.speechSynthesis.cancel();
              }
            }}
            className={`p-2 rounded-xl border transition-all ${
              isMuted
                ? 'border-red-500/40 bg-red-950/40 text-red-400'
                : 'border-slate-800 bg-slate-900 text-slate-300 hover:text-cyan-300'
            }`}
            title={isMuted ? 'Unmute DOKI Speech' : 'Mute DOKI Speech'}
          >
            {isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
          </button>
        </div>
      </div>
    </div>
  );
};
