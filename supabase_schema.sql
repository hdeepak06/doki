-- ==============================================================================
-- DOKI AI ASSISTANT - SUPABASE POSTGRESQL SCHEMA
-- Description: Complete cloud schema supporting personal memory, conversation logs,
--              reminders, custom commands, user profiles, and assistant settings.
-- Security: Configured with Row Level Security (RLS) policies.
-- ==============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------------------------
-- 1. PROFILES TABLE
-- Stores authenticated user profiles and general preferences
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    email TEXT,
    preferred_language VARCHAR(10) DEFAULT 'en-US', -- 'en-US', 'hi-IN', 'kn-IN'
    voice_speed REAL DEFAULT 1.0,
    voice_pitch REAL DEFAULT 1.0,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 2. CONVERSATIONS TABLE
-- Tracks multi-turn conversational sessions
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT DEFAULT 'New Conversation',
    interaction_type VARCHAR(20) DEFAULT 'voice', -- 'voice' or 'chat'
    language_used VARCHAR(10) DEFAULT 'en-US',
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 3. MESSAGES TABLE
-- Individual turns in each conversation (user voice/text, DOKI response, tools)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- 'user', 'assistant', 'system', 'tool'
    content TEXT NOT NULL,
    tool_calls JSONB DEFAULT NULL,     -- Stores tool invocations (name, arguments)
    tool_results JSONB DEFAULT NULL,   -- Stores tool execution output
    tokens_used INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 4. MEMORIES TABLE (LONG-TERM PERSONAL MEMORY)
-- Stores persistent facts, preferences, user contacts, habits learned by DOKI
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.memories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    category VARCHAR(50) DEFAULT 'general', -- 'preference', 'fact', 'contact', 'routine', 'health'
    key TEXT NOT NULL,                      -- e.g. 'favorite_coffee', 'partner_birthday'
    value TEXT NOT NULL,                    -- e.g. 'Oat milk latte without sugar'
    confidence REAL DEFAULT 1.0,
    is_approved BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 5. REMINDERS TABLE
-- Tracks reminders and alarms created via voice or UI
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reminders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    notes TEXT,
    due_time TIMESTAMPTZ NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(50), -- 'daily', 'weekdays', 'weekly', etc.
    alarm_intent_uri TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 6. CUSTOM COMMANDS TABLE
-- User-defined voice macros / shortcuts (e.g. 'Good Night' -> execute actions)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.custom_commands (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    trigger_phrase TEXT NOT NULL,           -- e.g. 'Heading to bed'
    action_type VARCHAR(50) NOT NULL,       -- 'routine', 'intent', 'tool_chain'
    action_payload JSONB NOT NULL,          -- e.g. [{"tool": "toggle_flashlight", "state": false}, {"tool": "create_alarm", "time": "07:00"}]
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- 7. ASSISTANT SETTINGS TABLE
-- Synced configuration for the DOKI Assistant
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.assistant_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    model_identifier TEXT DEFAULT 'gemini-1.5-flash',
    wake_word_enabled BOOLEAN DEFAULT TRUE,
    wake_word_sensitivity REAL DEFAULT 0.5,
    auto_stop_speech BOOLEAN DEFAULT TRUE,
    interrupt_on_speech BOOLEAN DEFAULT TRUE,
    store_audio_logs BOOLEAN DEFAULT FALSE, -- Privacy: never store raw audio by default
    haptic_feedback BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- ------------------------------------------------------------------------------
-- INDEXES FOR HIGH-PERFORMANCE QUERYING
-- ------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_conversations_user ON public.conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON public.messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_memories_user_key ON public.memories(user_id, key);
CREATE INDEX IF NOT EXISTS idx_reminders_user_due ON public.reminders(user_id, due_time);
CREATE INDEX IF NOT EXISTS idx_custom_commands_user ON public.custom_commands(user_id);

-- ------------------------------------------------------------------------------
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Ensures users can ONLY read and write their own assistant data
-- ------------------------------------------------------------------------------
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.memories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reminders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.custom_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.assistant_settings ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "Users can view own profile" ON public.profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Users can insert own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- Conversations Policies
CREATE POLICY "Users can view own conversations" ON public.conversations FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can create own conversations" ON public.conversations FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own conversations" ON public.conversations FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own conversations" ON public.conversations FOR DELETE USING (auth.uid() = user_id);

-- Messages Policies
CREATE POLICY "Users can view own messages" ON public.messages FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own messages" ON public.messages FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own messages" ON public.messages FOR DELETE USING (auth.uid() = user_id);

-- Memories Policies
CREATE POLICY "Users can view own memories" ON public.memories FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can create own memories" ON public.memories FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own memories" ON public.memories FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own memories" ON public.memories FOR DELETE USING (auth.uid() = user_id);

-- Reminders Policies
CREATE POLICY "Users can manage own reminders" ON public.reminders FOR ALL USING (auth.uid() = user_id);

-- Custom Commands Policies
CREATE POLICY "Users can manage own custom commands" ON public.custom_commands FOR ALL USING (auth.uid() = user_id);

-- Assistant Settings Policies
CREATE POLICY "Users can manage own assistant settings" ON public.assistant_settings FOR ALL USING (auth.uid() = user_id);

