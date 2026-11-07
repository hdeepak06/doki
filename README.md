# DOKI – Personal JARVIS-Style AI Assistant

**DOKI** is a high-performance personal AI assistant built for **Deepak**. It features a futuristic JARVIS-style dashboard, free OpenRouter model routing, real-time voice and text interaction, and a unified architecture across Windows laptops and Android phones.

---

## ⚡ Key Highlights
- **Owner**: Deepak
- **Budget**: ₹0 (100% Free AI models via OpenRouter, no credit card or paid APIs required)
- **Primary AI Provider**: OpenRouter (`https://openrouter.ai/api/v1`)
- **Default AI Model**: `openrouter/free` (Dynamic free router with automatic fallback to `:free` models)
- **Supported Languages**: English, Kannada (ಕನ್ನಡ), Hindi (हिन्दी)
- **Interface**: Futuristic JARVIS circular AI core, particle shader, real-time waveform, cyber dark theme
- **Hardware Integration**: Live Windows laptop telemetry (CPU, RAM, Battery) & paired Android device mesh

---

## 🛠 Project Structure
```
g:\websites\doki\
├── backend\               # Python FastAPI + SQLite + SQLAlchemy async stack
│   ├── app\
│   │   ├── api\           # Chat, Models, Config, Devices, WebSockets
│   │   ├── models\        # SQLite ORM models (Conversation, Message, Memory, Device)
│   │   ├── schemas\       # Pydantic validation schemas
│   │   ├── services\      # OpenRouter client, dynamic free model discoverer, fallback router
│   │   ├── config.py      # Secure persistent settings (API keys never committed)
│   │   ├── database.py    # Local SQLite persistent database
│   │   └── main.py        # FastAPI ASGI app
│   ├── data\              # Local SQLite database (doki.db) and configuration
│   ├── venv\              # Python virtual environment
│   └── requirements.txt
├── frontend\              # Futuristic JARVIS React + Vite + TypeScript Dashboard
│   ├── src\
│   │   ├── components\    # JarvisCore (holographic AI orb), AudioWaveform, TopBar, SideNav, Chat
│   │   ├── services\      # REST & SSE streaming chat client
│   │   └── App.tsx        # Unified assistant workspace
│   ├── package.json
│   └── vite.config.ts
├── scripts\               # One-click Windows launch scripts
│   ├── start_backend.bat
│   ├── start_frontend.bat
│   └── start_all.bat
└── README.md
```

---

## 🚀 Quick Start (Stage 1)

### 1. Launch DOKI
Double-click [`scripts/start_all.bat`](file:///g:/websites/doki/scripts/start_all.bat) or run in terminal:
```cmd
# Terminal 1: Backend
scripts\start_backend.bat

# Terminal 2: Frontend
scripts\start_frontend.bat
```

### 2. Access the JARVIS Dashboard
- Open your browser at: **[http://localhost:3000](http://localhost:3000)**
- Interactive API Documentation: **[http://localhost:8000/docs](http://localhost:8000/docs)**

### 3. Add Free OpenRouter Key
1. Get a free API key at [openrouter.ai/keys](https://openrouter.ai/keys) (No card needed).
2. Click the **Settings Gear** icon in the top right of the DOKI dashboard.
3. Paste your key and click **Save Configuration**.
4. DOKI will automatically fetch and list all currently active free models (e.g. `openrouter/free`, `liquid/lfm-3-1.7b:free`, `meta-llama/llama-3.2-3b-instruct:free`).

---

## 🗺️ Roadmap & Development Stages
- [x] **Stage 1**: Architecture, Backend, OpenRouter Free Connection, Persistent SQLite, Device Telemetry & Working JARVIS Dashboard.
- [ ] **Stage 2**: Voice Recognition (Vosk / whisper.cpp), Wake-Word ("Hey DOKI"), and Text-to-Speech.
- [ ] **Stage 3**: Windows Desktop Agent (PyAutoGUI, App control, File management, OCR).
- [ ] **Stage 4**: Native Android Companion App (Jetpack Compose, AccessibilityService, Intents).
- [ ] **Stage 5**: Secure Device Pairing (QR Code / WebSocket Mesh) & Remote Command Dispatch.
- [ ] **Stage 6**: Internet Web Research & Personal Memory System.
- [ ] **Stage 7**: Final Production Builds (APK & Windows Executable).
