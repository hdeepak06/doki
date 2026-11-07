export interface Device {
  device_id: string;
  name: string;
  device_type: 'laptop' | 'android';
  status: 'online' | 'offline';
  os?: string;
  cpu_usage?: string;
  ram_usage?: string;
  battery?: string;
  is_charging?: boolean;
  note?: string;
}

export interface ModelInfo {
  id: string;
  name: string;
  description?: string;
  context_length?: number;
  is_free: boolean;
}

export interface Message {
  id?: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  model?: string;
  created_at?: string;
}

export interface Conversation {
  id: string;
  title: string;
  created_at: string;
  updated_at: string;
  messages?: Message[];
}

export interface SystemConfig {
  has_api_key: boolean;
  masked_api_key: string;
  active_model: string;
  assistant_name: string;
  owner_name: string;
  version: string;
}
