# 📝 Compilazione con VS Code

## 🔧 Setup VS Code per Android

### 📦 Estensioni Necessarie
```json
// File: .vscode/extensions.json
{
    "recommendations": [
        "vscjava.vscode-java-pack",
        "redhat.java",
        "vscjava.vscode-gradle",
        "ms-vscode.vscode-json"
    ]
}
```

### ⚙️ Configurazione Tasks
```json
// File: .vscode/tasks.json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Build APK Debug",
            "type": "shell",
            "command": "./gradlew",
            "args": ["assembleDebug"],
            "group": {
                "kind": "build",
                "isDefault": true
            },
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            },
            "problemMatcher": []
        },
        {
            "label": "Clean Build",
            "type": "shell",
            "command": "./gradlew",
            "args": ["clean"],
            "group": "build"
        },
        {
            "label": "Build Release APK",
            "type": "shell", 
            "command": "./gradlew",
            "args": ["assembleRelease"],
            "group": "build"
        }
    ]
}
```

### 🚀 Comandi Rapidi
```json
// File: .vscode/settings.json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic",
    "gradle.nestedProjects": true
}
```

## 📋 Procedura VS Code

1. **Installa Java Extension Pack**
2. **Apri cartella** SpamDetector in VS Code  
3. **Ctrl+Shift+P** → "Tasks: Run Task" → "Build APK Debug"
4. **Aspetta compilazione** (3-5 minuti)
5. **APK in:** `app/build/outputs/apk/debug/`

## 🔥 Script One-Click per VS Code

```bash
# File: build_vscode.ps1 (PowerShell)
Write-Host "🛡️ Spam Detector Build Script" -ForegroundColor Green
Write-Host "==============================" -ForegroundColor Green

# Verifica Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "✅ Java trovato: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java non trovato!" -ForegroundColor Red
    Write-Host "📥 Installa Java da: https://adoptium.net/" -ForegroundColor Yellow
    exit 1
}

# Build
Write-Host "🔨 Compilazione in corso..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    ./gradlew assembleDebug
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ APK creato con successo!" -ForegroundColor Green
    Write-Host "📦 Percorso: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
    
    # Apri cartella APK
    if (Test-Path "app\build\outputs\apk\debug\") {
        Start-Process "app\build\outputs\apk\debug\"
    }
} else {
    Write-Host "❌ Errore durante la compilazione" -ForegroundColor Red
}

Read-Host "Premi INVIO per continuare"
```