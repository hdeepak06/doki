import type { SystemConfig, ModelInfo, Device, Conversation } from '../types';

const BASE_URL = '';

export function getStoredGeminiKey(): string {
  try {
    return localStorage.getItem('doki_gemini_api_key') || '';
  } catch {
    return '';
  }
}

export function setStoredGeminiKey(key: string): void {
  try {
    if (key && key.trim()) {
      localStorage.setItem('doki_gemini_api_key', key.trim());
    } else {
      localStorage.removeItem('doki_gemini_api_key');
    }
  } catch {}
}

export function getStoredOpenRouterKey(): string {
  try {
    return localStorage.getItem('doki_openrouter_api_key') || '';
  } catch {
    return '';
  }
}

export function setStoredOpenRouterKey(key: string): void {
  try {
    if (key && key.trim()) {
      localStorage.setItem('doki_openrouter_api_key', key.trim());
    } else {
      localStorage.removeItem('doki_openrouter_api_key');
    }
  } catch {}
}

// Backward-compatibility alias
export function getStoredApiKey(): string {
  return getStoredGeminiKey() || getStoredOpenRouterKey();
}

export function setStoredApiKey(key: string): void {
  if (key.startsWith('sk-') || key.startsWith('sk-or-')) {
    setStoredOpenRouterKey(key);
  } else {
    setStoredGeminiKey(key);
  }
}

export async function fetchConfig(): Promise<SystemConfig> {
  const res = await fetch(`${BASE_URL}/api/config`);
  if (!res.ok) throw new Error('Failed to load system config');
  return res.json();
}

export async function checkAiConnectivity(geminiKey?: string, openrouterKey?: string) {
  const params = new URLSearchParams();
  const gKey = geminiKey || getStoredGeminiKey();
  const oKey = openrouterKey || getStoredOpenRouterKey();
  if (gKey) params.append('gemini_key', gKey);
  if (oKey) params.append('openrouter_key', oKey);

  const res = await fetch(`${BASE_URL}/api/config/check?${params.toString()}`);
  if (!res.ok) throw new Error('Failed to check AI connectivity');
  return res.json();
}

export async function updateConfig(data: {
  gemini_api_key?: string;
  openrouter_api_key?: string;
  ai_primary_provider?: string;
  active_model?: string;
  gemini_model?: string;
  assistant_name?: string;
  free_only_mode?: boolean;
  auto_fallback_enabled?: boolean;
}): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/config`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error('Failed to update config');
}

export async function fetchModels(refresh = false): Promise<{ active_model: string; models: ModelInfo[] }> {
  const res = await fetch(`${BASE_URL}/api/models?refresh=${refresh}`);
  if (!res.ok) throw new Error('Failed to fetch models');
  return res.json();
}

export async function selectModel(modelId: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/models/select`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ model: modelId }),
  });
  if (!res.ok) throw new Error('Failed to select model');
}

export async function fetchDevices(): Promise<{ devices: Device[]; total: number }> {
  const res = await fetch(`${BASE_URL}/api/devices`);
  if (!res.ok) throw new Error('Failed to fetch devices');
  return res.json();
}

export async function sendDeviceCommand(target_device: 'laptop' | 'android', action: string, params?: Record<string, unknown>) {
  const res = await fetch(`${BASE_URL}/api/devices/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ target_device, action, params }),
  });
  return res.json();
}

export async function fetchConversations(): Promise<Conversation[]> {
  const res = await fetch(`${BASE_URL}/api/chat/conversations`);
  if (!res.ok) return [];
  return res.json();
}

export async function fetchConversation(id: string): Promise<Conversation> {
  const res = await fetch(`${BASE_URL}/api/chat/conversations/${id}`);
  if (!res.ok) throw new Error('Failed to fetch conversation');
  return res.json();
}

export async function deleteConversation(id: string): Promise<void> {
  await fetch(`${BASE_URL}/api/chat/conversations/${id}`, { method: 'DELETE' });
}

export async function clearAllConversations(): Promise<void> {
  await fetch(`${BASE_URL}/api/chat/conversations`, { method: 'DELETE' });
}

export async function streamChat(
  message: string,
  conversationId: string | null,
  model: string | null,
  onChunk: (chunk: string) => void,
  onStart: (modelUsed: string, providerUsed?: string) => void,
  onDone: () => void,
  onError: (err: string) => void
): Promise<string | null> {
  try {
    const geminiKey = getStoredGeminiKey();
    const openrouterKey = getStoredOpenRouterKey();
    const effectiveKey = geminiKey || openrouterKey;

    const res = await fetch(`${BASE_URL}/api/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(effectiveKey ? { 'X-AI-Key': effectiveKey } : {})
      },
      body: JSON.stringify({
        message,
        conversation_id: conversationId,
        model: model || undefined,
        stream: true,
        api_key: effectiveKey || undefined
      }),
    });

    const returnedConvId = res.headers.get('X-Conversation-Id');

    if (!res.ok || !res.body) {
      onError('Server error during chat stream');
      return returnedConvId;
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          try {
            const parsed = JSON.parse(line.slice(6));
            if (parsed.type === 'start') {
              onStart(parsed.model, parsed.provider);
            } else if (parsed.type === 'chunk') {
              onChunk(parsed.content);
            } else if (parsed.type === 'end') {
              onDone();
            } else if (parsed.type === 'error') {
              onError(parsed.content);
            }
          } catch {
            onChunk(line.slice(6));
          }
        }
      }
    }

    onDone();
    return returnedConvId;
  } catch (err) {
    onError(err instanceof Error ? err.message : 'Unknown stream error');
    return null;
  }
}
