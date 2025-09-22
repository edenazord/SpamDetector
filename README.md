# 🛡️ Spam Detector - Rilevamento WhatsApp

[![Build APK](https://github.com/FilippoTacchini/SpamDetector/actions/workflows/build-apk.yml/badge.svg)](https://github.com/FilippoTacchini/SpamDetector/actions/workflows/build-apk.yml)
[![Android API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Un'app Android intelligente per rilevare chiamate spam verificando se il numero chiamante ha **WhatsApp** installato.

## 📥 Download APK

### 🚀 Scarica l'ultima versione:
1. Vai su **[Actions](../../actions/workflows/build-apk.yml)**
2. Clicca sull'ultimo build verde ✅  
3. Scarica **🛡️-SpamDetector-Debug-APK**
4. Installa sul tuo Android!

> 💡 **Nuovo APK ogni push!** Build automatico sempre aggiornato.

## 🎯 Logica di Funzionamento

- ✅ **Numero ha WhatsApp** → Persona reale, chiamata lecita
- 🚨 **Numero NON ha WhatsApp** → Possibile spam/robot

## 🚀 Funzionalità Principali

- **🔍 Rilevamento WhatsApp**: Verifica se il numero chiamante ha WhatsApp attivo
- **🔔 Notifiche Intelligenti**: Avvisi solo per chiamate potenzialmente spam
- **🧪 Test Integrato**: Funzione per testare la verifica WhatsApp
- **🎨 Interfaccia Intuitiva**: Controlli semplici per attivare/disattivare il rilevamento

## 📱 Installazione Rapida

### 1️⃣ Scarica APK
```
👆 Clicca "Actions" sopra → Ultimo build → Download APK
```

### 2️⃣ Installa
```
📱 Android: Abilita "Fonti sconosciute" → Installa APK
```

### 3️⃣ Configura
```  
🔐 Concedi permessi → Attiva rilevamento → Test WhatsApp
```

## 🛠️ Sviluppo
- **Test Integrato**: Funzione di test per verificare il corretto funzionamento

## 🔧 Come Funziona

1. **Intercettazione**: L'app intercetta le chiamate in arrivo usando `BroadcastReceiver`
2. **Verifica**: Controlla se il numero contiene la stringa "wz"
3. **Classificazione**: 
   - ✅ **LECITO**: Numeri che contengono "wz"
   - 🚨 **SPAM**: Numeri che NON contengono "wz"
4. **Notifica**: Mostra notifiche per i numeri spam rilevati

## 📋 Permessi Richiesti

L'app richiede i seguenti permessi:
- `READ_PHONE_STATE`: Per leggere lo stato delle chiamate
- `READ_CALL_LOG`: Per accedere al registro chiamate
- `POST_NOTIFICATIONS`: Per inviare notifiche di spam

## 🏗️ Struttura del Progetto

```
├── MainActivity.kt           # Interfaccia utente principale
├── CallReceiver.kt          # Intercetta chiamate in arrivo
├── CallDetectionService.kt  # Servizio di rilevamento
├── SpamChecker.kt          # Logica di verifica spam
└── AndroidManifest.xml     # Configurazione permessi e componenti
```

## 🧪 Testing

L'app include una funzione di test che verifica:
- `+39123wz456789` → ✅ LECITO (contiene "wz")
- `+39123456789` → 🚨 SPAM (non contiene "wz")
- `333wz1234567` → ✅ LECITO (contiene "wz")
- `3331234567` → 🚨 SPAM (non contiene "wz")

## 🛠️ Installazione

1. Apri il progetto in Android Studio
2. Sincronizza le dipendenze Gradle
3. Compila e installa l'app su un dispositivo Android
4. Concedi i permessi richiesti
5. Attiva il rilevamento spam

## ⚠️ Note Importanti

- L'app richiede Android API 23+ (Android 6.0)
- I permessi devono essere concessi manualmente dall'utente
- Il rilevamento funziona solo su chiamate reali, non su chiamate di test dell'emulatore
- La logica di spam è personalizzabile modificando la classe `SpamChecker`

## 🔒 Privacy

L'app funziona completamente offline e non invia dati a server esterni. Tutte le verifiche vengono effettuate localmente sul dispositivo.