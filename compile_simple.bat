@echo off
echo 🚀 Compilazione Rapida Spam Detector
echo ===================================

REM Verifica Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java non trovato!
    echo 📥 Scarica Java da: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo ✅ Java trovato

REM Download Android SDK Command Line Tools se necessario
if not exist "%USERPROFILE%\android-sdk\cmdline-tools" (
    echo 📦 Download Android SDK Tools...
    mkdir "%USERPROFILE%\android-sdk\cmdline-tools"
    echo ⚠️  Scarica manualmente da: https://developer.android.com/studio#command-tools
    echo    Estrai in: %USERPROFILE%\android-sdk\cmdline-tools\
    pause
)

REM Imposta variabili ambiente
set ANDROID_HOME=%USERPROFILE%\android-sdk
set PATH=%PATH%;%ANDROID_HOME%\cmdline-tools\bin;%ANDROID_HOME%\platform-tools

REM Vai nella directory progetto
cd /d "%~dp0"

echo 🔨 Compilazione APK...
gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo ✅ SUCCESS! APK creato in:
    echo    app\build\outputs\apk\debug\app-debug.apk
    echo 
    echo 📱 Ora copia questo file sul telefono e installalo!
    explorer "app\build\outputs\apk\debug\"
) else (
    echo ❌ Errore compilazione
)

pause