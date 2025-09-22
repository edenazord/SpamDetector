#!/bin/bash

echo "🛡️ Spam Detector - Build Script"
echo "================================"

echo "📋 Controllo prerequisiti..."

# Verifica se Android SDK è configurato
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ ANDROID_HOME non configurato"
    echo "   Installa Android Studio e configura le variabili d'ambiente"
    exit 1
fi

echo "✅ Android SDK trovato: $ANDROID_HOME"

# Vai nella directory del progetto
cd "$(dirname "$0")"

echo "🔨 Compilazione APK in corso..."
echo "   Questo può richiedere alcuni minuti..."

# Compila l'APK di debug
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Compilazione completata con successo!"
    echo "📦 APK creato in: app/build/outputs/apk/debug/"
    echo ""
    echo "📱 Per installare sul telefono:"
    echo "   1. Copia il file APK sul telefono"
    echo "   2. Abilita 'Fonti sconosciute' nelle impostazioni"
    echo "   3. Apri il file APK per installare"
    echo ""
    
    # Apri la cartella dell'APK (se su desktop environment)
    if command -v xdg-open > /dev/null; then
        xdg-open "app/build/outputs/apk/debug/"
    elif command -v open > /dev/null; then
        open "app/build/outputs/apk/debug/"
    fi
else
    echo "❌ Errore durante la compilazione"
    echo "   Controlla i log sopra per i dettagli"
    exit 1
fi

echo ""
echo "Premi INVIO per continuare..."
read