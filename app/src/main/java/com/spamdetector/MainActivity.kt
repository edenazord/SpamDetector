package com.spamdetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    private lateinit var statusTextView: TextView
    private lateinit var enableSwitch: Switch
    private lateinit var testButton: Button
    private lateinit var permissionsButton: Button
    
    private lateinit var spamChecker: SpamChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        spamChecker = SpamChecker(this)
        initializeViews()
        loadSwitchState() // 📱 Carica stato switch salvato
        setupListeners()
        updateStatus()
        checkPermissions()
    }
    
    private fun loadSwitchState() {
        // 📱 Carica stato switch da SharedPreferences
        val prefs = getSharedPreferences("spam_detector", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("spam_detection_enabled", false)
        enableSwitch.isChecked = isEnabled
    }

    private fun initializeViews() {
        try {
            statusTextView = findViewById(R.id.statusTextView)
            enableSwitch = findViewById(R.id.enableSwitch) 
            testButton = findViewById(R.id.testButton)
            permissionsButton = findViewById(R.id.permissionsButton)
            
            Log.d("MainActivity", "✅ Views inizializzate correttamente")
            
            // Test immediato per vedere se le views funzionano
            statusTextView.text = "🔄 Inizializzazione in corso..."
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Errore inizializzazione views", e)
        }
    }

    private fun setupListeners() {
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasRequiredPermissions()) {
                    enableSpamDetection()
                } else {
                    enableSwitch.isChecked = false
                    Toast.makeText(this, "Permessi necessari mancanti", Toast.LENGTH_SHORT).show()
                }
            } else {
                disableSpamDetection()
            }
            updateStatus()
        }

        testButton.setOnClickListener {
            testSpamDetection()
        }

        permissionsButton.setOnClickListener {
            requestPermissions()
        }
        
        // Long press per debug
        testButton.setOnLongClickListener {
            showDebugInfo()
            true
        }
    }

    private fun enableSpamDetection() {
        // 💾 Salva stato switch
        val prefs = getSharedPreferences("spam_detector", MODE_PRIVATE)
        prefs.edit().putBoolean("spam_detection_enabled", true).apply()
        
        Toast.makeText(this, "✅ Rilevamento spam attivato", Toast.LENGTH_SHORT).show()
    }

    private fun disableSpamDetection() {
        // 💾 Salva stato switch
        val prefs = getSharedPreferences("spam_detector", MODE_PRIVATE)
        prefs.edit().putBoolean("spam_detection_enabled", false).apply()
        
        Toast.makeText(this, "⏸️ Rilevamento spam disattivato", Toast.LENGTH_SHORT).show()
    }

    private fun testSpamDetection() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "⚠️ Prima concedi i permessi!", Toast.LENGTH_LONG).show()
            return
        }
        
        // Mostra dialogo di scelta del test
        val testOptions = arrayOf(
            "🧪 Test Completo (simula chiamata + controllo spam)",
            "📸 Test Solo Contatto (solo creazione temporanea)",
            "🔧 Test Sistema (verifica CallReceiver)"
        )
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Scegli tipo di test")
            .setItems(testOptions) { _, which ->
                when (which) {
                    0 -> runCompleteTest()
                    1 -> runContactTest()
                    2 -> runSystemTest()
                }
            }
            .show()
    }
    
    private fun runCompleteTest() {
        Toast.makeText(this, "🧪 Avvio test completo...", Toast.LENGTH_SHORT).show()
        
        // Salva timestamp del test
        val prefs = getSharedPreferences("spam_detector", MODE_PRIVATE)
        prefs.edit().putLong("last_test_time", System.currentTimeMillis()).apply()
        
        // Simula una chiamata in arrivo
        val testNumber = "+393331234567"  // Numero italiano tipico
        
        // Simula il processo completo CallReceiver -> CallDetectionService
        Log.d("MainActivity", "🎯 Simulazione chiamata da $testNumber")
        
        // Test del CallReceiver (simula una chiamata)
        val receiverIntent = android.content.Intent("android.intent.action.PHONE_STATE")
        receiverIntent.putExtra("state", "RINGING")
        receiverIntent.putExtra("incoming_number", testNumber)
        
        val callReceiver = CallReceiver()
        callReceiver.onReceive(this, receiverIntent)
        
        Toast.makeText(this, "✅ Test chiamata simulata! Controlla le notifiche.", Toast.LENGTH_LONG).show()
    }
    
    private fun runContactTest() {
        Toast.makeText(this, "📸 Test creazione contatto...", Toast.LENGTH_SHORT).show()
        
        val testNumber = "+393331234567"
        
        Thread {
            try {
                val cleanNumber = spamChecker.cleanPhoneNumber(testNumber)
                val tempInfo = spamChecker.createTempContactAndCheck(cleanNumber, testNumber)
                
                val status = when {
                    !tempInfo.wasCreated -> "❌ ERRORE: Impossibile creare contatto temporaneo"
                    !tempInfo.hasPhoto -> "🚨 SPAM: Numero senza foto profilo"
                    else -> "✅ SICURO: Numero con foto profilo"
                }
                
                val results = "📸 Test contatto completato!\n\n" +
                        "Numero testato: $testNumber\n" +
                        "Risultato: $status\n\n" +
                        "Dettagli:\n" +
                        "• Contatto creato: ${if (tempInfo.wasCreated) "✅" else "❌"}\n" +
                        "• Foto profilo: ${if (tempInfo.hasPhoto) "✅" else "❌"}\n" +
                        "• Sincronizzato: ${if (tempInfo.syncedWithSocial) "✅" else "❌"}"
                
                runOnUiThread {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("📸 Risultato Test Contatto")
                        .setMessage(results)
                        .setPositiveButton("OK", null)
                        .show()
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "❌ Errore durante test: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun runSystemTest() {
        // Test dei componenti del sistema
        val testResults = StringBuilder("🔧 Test Sistema:\n\n")
        
        // 1. Test permessi
        val hasPerms = hasRequiredPermissions()
        testResults.append("🔒 Permessi: ${if (hasPerms) "✅ OK" else "❌ MANCANTI"}\n")
        
        // 2. Test switch
        val switchEnabled = getSharedPreferences("spam_detector", MODE_PRIVATE)
            .getBoolean("spam_detection_enabled", false)
        testResults.append("🎛️ Switch: ${if (switchEnabled) "✅ ATTIVO" else "❌ SPENTO"}\n")
        
        // 3. Test CallReceiver
        try {
            val receiver = CallReceiver()
            testResults.append("📞 CallReceiver: ✅ OK\n")
        } catch (e: Exception) {
            testResults.append("📞 CallReceiver: ❌ ERRORE\n")
        }
        
        // 4. Test CallDetectionService
        try {
            val serviceIntent = android.content.Intent(this, CallDetectionService::class.java)
            testResults.append("🚀 CallDetectionService: ✅ OK\n")
        } catch (e: Exception) {
            testResults.append("🚀 CallDetectionService: ❌ ERRORE\n")
        }
        
        // 5. Test SpamChecker
        try {
            val checker = SpamChecker(this)
            testResults.append("🕵️ SpamChecker: ✅ OK\n")
        } catch (e: Exception) {
            testResults.append("🕵️ SpamChecker: ❌ ERRORE\n")
        }
        
        testResults.append("\n💡 Per test completo:\n")
        testResults.append("1. Attiva il rilevamento\n")
        testResults.append("2. Concedi tutti i permessi\n")
        testResults.append("3. Usa 'Test Completo'")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("🔧 Risultato Test Sistema")
            .setMessage(testResults.toString())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showDebugInfo() {
        val debugInfo = StringBuilder("🐛 Debug Info:\n\n")
        
        // Informazioni sui permessi dettagliate
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        debugInfo.append("🔒 Permessi:\n")
        requiredPermissions.forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            val permName = permission.split(".").last()
            debugInfo.append("  $permName: ${if (granted) "✅" else "❌"}\n")
        }
        
        // Info SharedPreferences
        val prefs = getSharedPreferences("spam_detector", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("spam_detection_enabled", false)
        debugInfo.append("\n🎛️ Stato Switch: ${if (isEnabled) "ATTIVO" else "SPENTO"}\n")
        
        // Info sistema
        debugInfo.append("\n📱 Sistema:\n")
        debugInfo.append("  Android: ${android.os.Build.VERSION.RELEASE}\n")
        debugInfo.append("  SDK: ${android.os.Build.VERSION.SDK_INT}\n")
        
        // Test rapido chiamata simulata
        debugInfo.append("\n💡 Suggerimenti:\n")
        debugInfo.append("• Long press = Debug info (questo)\n")
        debugInfo.append("• Tap normale = Test menu\n")
        debugInfo.append("• Per testare: scegli 'Test Completo'\n")
        debugInfo.append("• Controlla i log in logcat per 'CallReceiver'\n")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("🐛 Debug SpamDetector")
            .setMessage(debugInfo.toString())
            .setPositiveButton("OK", null)
            .setNegativeButton("Eventi Recenti", { _, _ ->
                showRecentEvents()
            })
            .show()
    }

    private fun updateStatus() {
        try {
            val isEnabled = enableSwitch.isChecked
            val hasPermissions = hasRequiredPermissions()
            
            Log.d("MainActivity", "🔄 Aggiornamento status: enabled=$isEnabled, permissions=$hasPermissions")
            
            val status = when {
                !hasPermissions -> {
                    Log.d("MainActivity", "🔒 Permessi mancanti")
                    "🔒 PERMESSI RICHIESTI\n\n❌ L'app ha bisogno dei permessi per:\n• Leggere stato chiamate\n• Accedere ai contatti\n• Creare contatti temporanei\n• Mostrare notifiche\n\n� Tocca 'CONCEDI PERMESSI' qui sotto"
                }
                isEnabled -> {
                    Log.d("MainActivity", "✅ Rilevamento attivo")
                    "✅ RILEVAMENTO ATTIVO\n\n📱 L'app sta monitorando le chiamate in background\n\n� Come funziona:\n📞 Chiamata sconosciuta arriva\n📝 Creo contatto temporaneo\n⏱️ Aspetto sincronizzazione\n� Controllo se appare foto\n🚨 Notifico se è spam\n\n💡 Funziona anche con app chiusa!"
                }
                else -> {
                    Log.d("MainActivity", "⏸️ Rilevamento spento")
                    "⏸️ RILEVAMENTO SPENTO\n\n🎛️ Attiva lo switch qui sopra per iniziare\n\n🧪 VUOI TESTARE?\nUsa il pulsante 'TEST WHATSAPP' per:\n• Verificare che tutto funzioni\n• Simulare una chiamata spam\n• Vedere come appare la notifica\n\n🔧 Usa long-press per debug"
                }
            }
            
            // Forza sempre l'aggiornamento
            statusTextView.text = status
            Log.d("MainActivity", "📝 Testo impostato: ${status.substring(0, minOf(50, status.length))}...")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Errore in updateStatus", e)
            statusTextView.text = "❌ Errore aggiornamento status\n\nProva a riavviare l'app"
        }
        
        permissionsButton.isEnabled = !hasPermissions
    }

    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,     // 📸 Per leggere contatti esistenti
            Manifest.permission.WRITE_CONTACTS,    // 📸 Per tecnica "salva al volo"
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,     // 📸 Per leggere contatti esistenti  
            Manifest.permission.WRITE_CONTACTS,    // 📸 Per tecnica "salva al volo"
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
    }

    private fun checkPermissions() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "Tocca 'Concedi Permessi' per utilizzare l'app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allPermissionsGranted) {
                Toast.makeText(this, "Permessi concessi!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Alcuni permessi sono stati negati", Toast.LENGTH_SHORT).show()
            }
            
            updateStatus()
        }
    }

    private fun showRecentEvents() {
        // Controlla SharedPreferences per eventi recenti
        val prefs = getSharedPreferences("spam_detector", MODE_PRIVATE)
        val lastTestTime = prefs.getLong("last_test_time", 0)
        val lastCallTime = prefs.getLong("last_call_time", 0)
        val lastCallNumber = prefs.getString("last_call_number", "Nessuno")
        
        val eventsInfo = StringBuilder("📋 Eventi Recenti:\n\n")
        
        if (lastTestTime > 0) {
            val testDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(lastTestTime))
            eventsInfo.append("🧪 Ultimo test: $testDate\n")
        } else {
            eventsInfo.append("🧪 Nessun test eseguito\n")
        }
        
        if (lastCallTime > 0) {
            val callDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(lastCallTime))
            eventsInfo.append("📞 Ultima chiamata: $callDate\n")
            eventsInfo.append("📱 Numero: $lastCallNumber\n")
        } else {
            eventsInfo.append("📞 Nessuna chiamata intercettata\n")
        }
        
        eventsInfo.append("\n💡 Come testare:\n")
        eventsInfo.append("1. Attiva il rilevamento\n")
        eventsInfo.append("2. Usa 'Test Completo'\n")
        eventsInfo.append("3. Oppure ricevi una chiamata vera\n")
        eventsInfo.append("4. Controlla le notifiche!\n\n")
        eventsInfo.append("🔧 Se non funziona:\n")
        eventsInfo.append("• Verifica tutti i permessi\n")
        eventsInfo.append("• Disattiva ottimizzazione batteria\n")
        eventsInfo.append("• Riavvia l'app")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("📋 Log Eventi SpamDetector")
            .setMessage(eventsInfo.toString())
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancella Log", { _, _ ->
                // Cancella i log salvati
                prefs.edit()
                    .remove("last_test_time")
                    .remove("last_call_time")
                    .remove("last_call_number")
                    .apply()
                Toast.makeText(this, "Log cancellati", Toast.LENGTH_SHORT).show()
            })
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "🔄 onResume - Aggiornamento UI")
        
        // Forza refresh dell'UI
        runOnUiThread {
            updateStatus()
        }
        
        // Backup: se ancora non funziona, mostra almeno lo stato base
        statusTextView.postDelayed({
            if (statusTextView.text.isEmpty() || statusTextView.text == "🔄 Inizializzazione in corso...") {
                statusTextView.text = "🛡️ SpamDetector Attivo\n\nControlla lo switch sopra per attivare/disattivare il rilevamento.\n\nUsa 'TEST WHATSAPP' per testare il sistema."
            }
        }, 500)
    }
}