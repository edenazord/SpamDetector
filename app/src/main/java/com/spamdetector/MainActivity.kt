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
        statusTextView = findViewById(R.id.statusTextView)
        enableSwitch = findViewById(R.id.enableSwitch)
        testButton = findViewById(R.id.testButton)
        permissionsButton = findViewById(R.id.permissionsButton)
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
            .setNegativeButton("Apri Log", { _, _ ->
                // Comando per aprire i log (se possibile)
                Toast.makeText(this, "Cerca 'CallReceiver' nei log ADB", Toast.LENGTH_LONG).show()
            })
            .show()
    }

    private fun updateStatus() {
        val isEnabled = enableSwitch.isChecked
        val hasPermissions = hasRequiredPermissions()
        
        val status = when {
            !hasPermissions -> "❌ Permessi mancanti\n\nL'app ha bisogno dei permessi per accedere alle chiamate e ai contatti.\n\n🔒 Tocca 'CONCEDI PERMESSI' per abilitare le funzionalità."
            isEnabled -> "✅ Rilevamento Attivo\n\nL'app sta monitorando le chiamate in arrivo in background.\n\n📸 Quando riceverai una chiamata sconosciuta, verrà creato un contatto temporaneo per testare se ha foto profilo.\n\n🚨 Numeri senza foto = Probabilmente spam\n✅ Numeri con foto = Probabilmente legittimi"
            else -> "⏸️ Rilevamento Disattivato\n\nAttiva il rilevamento per iniziare a monitorare le chiamate spam.\n\n🧪 Puoi usare il pulsante 'TEST WHATSAPP' per testare il sistema."
        }
        
        if (statusTextView.text != status) {
            statusTextView.text = status
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

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}