# 🌐 Compilazione Online - Spam Detector

## ☁️ Servizi Online Gratuiti per Compilare APK

### 1️⃣ **GitHub Actions** (Raccomandato)
```yaml
# Crea file: .github/workflows/build.yml
name: Build Android APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Make gradlew executable
      run: chmod +x ./gradlew
      
    - name: Build APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

**Come usare:**
1. Carica progetto su GitHub
2. Vai su Actions → Run workflow
3. Scarica APK compilato dai risultati

### 2️⃣ **Replit** (Browser)
```
1. Vai su replit.com
2. Crea nuovo repl → Import from GitHub
3. Incolla URL del tuo progetto
4. Run: ./gradlew assembleDebug
5. Scarica APK dalla cartella build/
```

### 3️⃣ **Gitpod** (VS Code Online)
```
1. Vai su gitpod.io
2. Incolla: gitpod.io/#https://github.com/tuo-repo
3. Aspetta caricamento ambiente
4. Terminale: ./gradlew assembleDebug
5. Scarica APK generato
```

### 4️⃣ **CodeSandbox** (Alternativa)
```
1. codesandbox.io → Import → GitHub
2. Seleziona il progetto SpamDetector
3. Apri terminale
4. Run: chmod +x gradlew && ./gradlew assembleDebug
```

## 📤 Setup Rapido GitHub

Crea questi file nel progetto:

**.github/workflows/android.yml:**
```yaml
name: Android CI

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        cache: gradle
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK to GitHub Artifacts
      uses: actions/upload-artifact@v3
      with:
        name: SpamDetector-Debug-APK
        path: app/build/outputs/apk/debug/*.apk
```

**Risultato:** APK scaricabile da GitHub senza installare nulla!

## 🎯 Pro e Contro

| Metodo | Pro | Contro |
|--------|-----|---------|
| **GitHub Actions** | ✅ Gratis, automatico | ❌ Serve account GitHub |
| **Replit** | ✅ Veloce, no setup | ❌ Limitazioni tempo |
| **Gitpod** | ✅ Ambiente completo | ❌ 50h/mese gratis |
| **CodeSandbox** | ✅ Facile da usare | ❌ Performance limitata |