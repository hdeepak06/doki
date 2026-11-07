@echo off
title DOKI AI Backend
cd /d "%~dp0\.."
echo ========================================================
echo        DOKI AI ASSISTANT - FASTAPI BACKEND SERVER       
echo ========================================================
set PYTHONPATH=%CD%\backend
call .\backend\venv\Scripts\activate.bat
python backend\run.py
pause

