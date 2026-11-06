@echo off
setlocal enabledelayedexpansion

echo ==============================================================================
echo                      DOKI AI - APK BUILD SCRIPT
echo ==============================================================================
echo.

:: 1. Check for JAVA_HOME or system java
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        echo [INFO] Found JAVA_HOME: %JAVA_HOME%
        goto :CHECK_ANDROID
    )
)

:: 2. Check for Android Studio bundled JBR/JDK
set "AS_JBR=C:\Program Files\Android\Android Studio\jbr"
if exist "%AS_JBR%\bin\java.exe" (
    echo [INFO] Found Android Studio JBR: %AS_JBR%
    set "JAVA_HOME=%AS_JBR%"
    set "PATH=%AS_JBR%\bin;%PATH%"
    goto :CHECK_ANDROID
)

:: 3. Check for standard java in PATH
where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [INFO] Found Java in system PATH.
    goto :CHECK_ANDROID
)

echo [ERROR] Java Development Kit (JDK 17+) was not found!
echo Please do one of the following:
echo   1. Open G:\websites\doki directly in Android Studio and click Build -> Build APK(s)
echo   2. Install JDK 17 from https://adoptium.net/ and set JAVA_HOME.
echo.
pause
exit /b 1

:CHECK_ANDROID
:: Check for Android SDK
if not defined ANDROID_HOME (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
        echo [INFO] Detected Android SDK: %ANDROID_HOME%
    )
)

echo.
echo [INFO] Building DOKI Debug APK using Gradle...
echo.

call "%~dp0gradlew.bat" assembleDebug

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==============================================================================
    echo [SUCCESS] DOKI AI Debug APK successfully built!
    echo Location: %~dp0app\build\outputs\apk\debug\app-debug.apk
    echo ==============================================================================
    echo.
    echo You can now install it to your connected device using:
    echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo.
) else (
    echo.
    echo [ERROR] Gradle build encountered an issue.
    echo If building for the first time, opening the project in Android Studio
    echo will automatically download and configure all necessary SDK components.
    echo.
)

pause

