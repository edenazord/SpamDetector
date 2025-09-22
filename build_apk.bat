@echo off
echo 🛡️ Spam Detector - Build Script
echo ================================

echo 📋 Controllo prerequisiti...

REM Verifica se Android SDK è configurato
if not defined ANDROID_HOME (
    echo ❌ ANDROID_HOME non configurato
    echo    Installa Android Studio e configura le variabili d'ambiente
    pause
    exit /b 1
)

echo ✅ Android SDK trovato: %ANDROID_HOME%

REM Vai nella directory del progetto
cd /d "%~dp0"

echo 🔨 Compilazione APK in corso...
echo    Questo può richiedere alcuni minuti...

REM Compila l'APK di debug
call gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo ✅ Compilazione completata con successo!
    echo 📦 APK creato in: app\build\outputs\apk\debug\
    echo 
    echo 📱 Per installare sul telefono:
    echo    1. Copia il file APK sul telefono
    echo    2. Abilita "Fonti sconosciute" nelle impostazioni
    echo    3. Apri il file APK per installare
    echo 
    explorer "app\build\outputs\apk\debug\"
) else (
    echo ❌ Errore durante la compilazione
    echo    Controlla i log sopra per i dettagli
)

echo 
echo Premi un tasto per continuare...
pause