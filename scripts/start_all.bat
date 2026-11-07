@echo off
title DOKI AI Personal Assistant Launcher
cd /d "%~dp0\.."
echo ========================================================
echo        DOKI PERSONAL AI ASSISTANT (JARVIS-STYLE)        
echo        Owner: Deepak                                    
echo ========================================================
echo Starting Backend (FastAPI on Port 8000)...
start "DOKI Backend" cmd /c "scripts\start_backend.bat"
timeout /t 3 >nul
echo Starting Frontend (Vite on Port 3000)...
start "DOKI Frontend" cmd /c "scripts\start_frontend.bat"
echo.
echo DOKI is running!
echo Dashboard: http://localhost:3000
echo Backend API Docs: http://localhost:8000/docs
echo ========================================================
pause
