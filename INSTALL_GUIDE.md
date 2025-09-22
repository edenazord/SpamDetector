# 📱 Guida Installazione - Spam Detector

## 🛠️ OPZIONE 1: Compila da Solo (Raccomandato)

### 📋 Prerequisiti
1. **Android Studio** installato sul PC
2. **Java JDK** (installato con Android Studio)
3. **Cavo USB** o **WiFi** per collegare il telefono

### 🔨 Compilazione
```bash
# Su Windows:
build_apk.bat

# Su Mac/Linux:
chmod +x build_apk.sh
./build_apk.sh
```

**Il file APK sarà creato in:** `app\build\outputs\apk\debug\app-debug.apk`

---

## 📱 OPZIONE 2: Usa Android Studio (Più Semplice)

### 🚀 Procedura Rapida
1. **Apri Android Studio**
2. **Open Project** → Seleziona cartella `SpamDetector`
3. **Collega il telefono** via USB
4. **Clicca il pulsante ▶️ Run**
5. **Seleziona il tuo dispositivo**
6. L'app si installerà automaticamente!

---

## 📲 OPZIONE 3: Installazione Manuale APK

### 📤 Trasferimento File
```bash
# Metodi per trasferire l'APK:
1. 📧 Email: Invia APK via email e scarica sul telefono
2. 💾 USB: Copia APK nella memoria del telefono
3. ☁️ Cloud: Google Drive, Dropbox, etc.
4. 📶 WiFi: Android File Transfer, AirDroid
```

### ⚙️ Configurazione Telefono Android

#### 1️⃣ **Abilita Fonti Sconosciute**
```
Android 8+ (Oreo):
Impostazioni → App e notifiche → Accesso speciale alle app → 
Installa app sconosciute → [Browser/File Manager] → Attiva

Android 7 e precedenti:
Impostazioni → Sicurezza → Origini sconosciute → Attiva
```

#### 2️⃣ **Abilita Opzioni Sviluppatore** (Opzionale)
```
Impostazioni → Info telefono → 
Tocca "Numero build" 7 volte → 
Torna indietro → Opzioni sviluppatore → 
Debug USB → Attiva
```

### 📦 Installazione APK
1. **Apri file manager** sul telefono
2. **Trova il file** `app-debug.apk`
3. **Tocca per aprire**
4. **Conferma installazione**
5. **Concedi permessi** richiesti

---

## 🔐 Configurazione Permessi App

### ⚠️ Permessi Critici da Abilitare
```
✅ Accesso al telefono (READ_PHONE_STATE)
✅ Notifiche (POST_NOTIFICATIONS)
✅ Accesso ai registri chiamate (READ_CALL_LOG)
✅ Rilevamento app installate (QUERY_ALL_PACKAGES)
```

### 📱 Percorso Impostazioni
```
Impostazioni → App → Spam Detector → Autorizzazioni
Abilita TUTTI i permessi richiesti
```

---

## 🧪 Test Funzionamento

### 1️⃣ **Test Base**
- Apri l'app
- Tocca "🔐 Concedi Permessi"
- Attiva lo switch "Rilevamento"
- Tocca "🧪 Test WhatsApp"

### 2️⃣ **Test Reale**
- Fai chiamare un numero senza WhatsApp
- Dovresti vedere la notifica spam
- Verifica nei log dell'app

---

## 🔧 Risoluzione Problemi

### ❌ **"Installazione bloccata"**
```
Soluzione:
1. Abilita "Fonti sconosciute"
2. Disabilita temporaneamente antivirus
3. Usa browser diverso per scaricare APK
```

### ❌ **"App non rileva chiamate"**
```
Controlli:
1. ✅ Permessi telefono abilitati?
2. ✅ App impostata come predefinita?
3. ✅ Notifiche abilitate?
4. ✅ Modalità risparmio energetico disattivata?
```

### ❌ **"WhatsApp non trovato"**
```
Verifica:
1. ✅ WhatsApp installato sul telefono?
2. ✅ Permessi "Query app" abilitati?
3. ✅ Versione Android compatibile?
```

---

## 🚀 Script Automatico Windows

Salva come `install_app.bat`:
```batch
@echo off
echo 🛡️ Installazione Automatica Spam Detector
echo ==========================================

echo 🔨 Compilazione APK...
call build_apk.bat

echo 📱 Collegamento dispositivo...
adb devices

echo 📦 Installazione su dispositivo...
adb install -r "app\build\outputs\apk\debug\app-debug.apk"

echo ✅ Installazione completata!
echo    Apri l'app e configura i permessi
pause
```

---

## 📋 Checklist Installazione

```
□ Android Studio installato
□ Progetto aperto e sincronizzato
□ Telefono collegato e riconosciuto
□ Debug USB abilitato
□ APK compilato con successo
□ APK trasferito sul telefono
□ Fonti sconosciute abilitate
□ App installata correttamente
□ Tutti i permessi concessi
□ Test funzionamento superato
□ WhatsApp rilevato correttamente
```

**🎉 Ora la tua app anti-spam è pronta! 🛡️**