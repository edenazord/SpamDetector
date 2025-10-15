# 🔑 Setup GitHub Secrets - Spam Detector

## 🎯 Per APK firmati in produzione

Per ottenere APK release firmati automaticamente, segui questi passaggi:

### 1️⃣ Crea un keystore per firmare gli APK

```bash
# Genera keystore (una volta sola)
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias spamdetector

# Ti chiederà:
# - Password keystore (ricordala!)
# - Password chiave (ricordala!)  
# - Nome, organizzazione, ecc.
```

### 2️⃣ Converti keystore in Base64

```bash
# Windows
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-release-key.jks")) | Out-File keystore-base64.txt

# Linux/Mac
base64 -i my-release-key.jks -o keystore-base64.txt
```

### 3️⃣ Configura GitHub Secrets

Vai su GitHub: **Settings → Secrets and variables → Actions → New repository secret**

Aggiungi questi secrets:

| Secret Name | Valore | Descrizione |
|-------------|--------|-------------|
| `KEYSTORE_BASE64` | Contenuto di `keystore-base64.txt` | Keystore codificato |
| `KEYSTORE_PASSWORD` | Password del keystore | Password inserita al passo 1 |
| `KEY_ALIAS` | `spamdetector` | Alias della chiave |
| `KEY_PASSWORD` | Password della chiave | Password chiave al passo 1 |

### 4️⃣ Come funziona

#### 🐛 **APK Debug (automatico)**
- ✅ **Push su `main`** → Compila APK debug automaticamente
- ✅ **Pull Request** → Compila e testa
- ✅ **Scarica** da Actions → Artifacts → `SpamDetector-Debug-xxxxx`
- ✅ **Disponibile** per 7 giorni

#### 🚀 **APK Release (firmato)**
- ✅ **Crea tag** → `git tag v1.0.0 && git push origin v1.0.0`
- ✅ **Release automatica** → APK firmato in GitHub Releases
- ✅ **Permanente** → APK scaricabile per sempre
- ✅ **Pronto** per Google Play Store

## 📱 Come creare una release

```bash
# 1. Committa le tue modifiche
git add .
git commit -m "feat: nuova funzionalità spam detection"

# 2. Crea un tag di versione
git tag v1.0.0

# 3. Push del tag
git push origin v1.0.0

# 4. GitHub Actions farà tutto automaticamente:
#    - Compila APK release firmato
#    - Crea GitHub Release
#    - Allega APK alla release
```

## 🎯 Vantaggi GitHub Actions

- ✅ **Gratis** - 2000 minuti/mese per repo pubblici
- ✅ **Automatico** - Build ad ogni push
- ✅ **Artifacts** - APK scaricabili subito
- ✅ **Release** - APK firmati per produzione  
- ✅ **Multiple branches** - Debug per main/develop
- ✅ **Tests** - Test automatici prima del build

## 📥 Download APK

### Debug (sempre disponibili)
1. Vai su **Actions**
2. Clicca sull'ultimo workflow **"Build Android APK"**
3. Scarica **"SpamDetector-Debug-xxxxx"** da Artifacts

### Release (stabili)
1. Vai su **Releases** 
2. Scarica l'ultima versione **"SpamDetector-vX.X.X.apk"**

## 🚨 Troubleshooting

### ❌ Build fallito
- Controlla **Actions** → **Build Android APK** → Logs
- Verifica che `gradlew` abbia permessi: `chmod +x gradlew`

### ❌ APK non firmato
- Verifica i **GitHub Secrets** (devono essere tutti configurati)
- Controlla che `KEYSTORE_BASE64` sia corretto

### ❌ Release non creata
- Il tag deve iniziare con `v` (es: `v1.0.0`, non `1.0.0`)
- Verifica i permessi GitHub Actions in Settings → Actions