# 🚀 Setup GitHub Repository

## 📋 Istruzioni Step-by-Step

### 1️⃣ **Crea Repository su GitHub**
```
1. Vai su: https://github.com/new
2. Repository name: SpamDetector  
3. Description: 🛡️ Android app per rilevare spam tramite verifica WhatsApp
4. ✅ Public (per GitHub Actions gratis)
5. ❌ NON aggiungere README, gitignore, license (già presenti)
6. Click "Create repository"
```

### 2️⃣ **Collega Repository Locale**
```bash
# Copia questi comandi dalla pagina GitHub e eseguili:

git remote add origin https://github.com/TUO_USERNAME/SpamDetector.git
git branch -M main
git push -u origin main
```

### 3️⃣ **Verifica Setup**
```
✅ Codice caricato su GitHub
✅ Actions tab visibile  
✅ Workflow file presente (.github/workflows/build-apk.yml)
```

## 🔄 **Auto-Build Process**

### 📦 Come Funziona
```
1. Push codice → GitHub Actions si attiva automaticamente
2. Ubuntu server compila l'APK (5-10 min)
3. APK disponibile in Actions → Artifacts
4. Download e installa sul telefono!
```

### 🎯 **URL Utili Dopo Setup**
```
🏠 Repository: https://github.com/TUO_USERNAME/SpamDetector
🚀 Actions: https://github.com/TUO_USERNAME/SpamDetector/actions  
📥 APK: Actions → Latest build → Artifacts → Download
```

## 🛠️ **Comandi Utili**

### 📤 **Push Future Changes**
```bash
git add .
git commit -m "🔧 Aggiornamento app"
git push
```

### 🔄 **Trigger Manual Build**  
```
1. Vai su Actions tab
2. Click "🛡️ Build Spam Detector APK"
3. Click "Run workflow" → "Run workflow"
4. Aspetta build → Download APK
```

### 🐛 **Se Build Fallisce**
```
1. Actions → Click build rosso ❌
2. Guarda logs per errori
3. Fixa errori → Push → Riprova
```

## 🎉 **Risultato Finale**

Dopo il setup avrai:
- 🏠 **Repository GitHub** con tutto il codice
- 🤖 **Build automatico** ad ogni push  
- 📱 **APK sempre aggiornato** scaricabile
- 🔄 **Zero setup locale** necessario
- 🌍 **Condivisibile** con chiunque

**Ready to build! 🚀**