package com.spamdetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
        
        Toast.makeText(this, "🧪 Avvio test spam detection...", Toast.LENGTH_SHORT).show()
        
        // Test con un numero di esempio
        val testNumber = "+393331234567"  // Numero italiano tipico
        
        // Esegui il test in background
        Thread {
            try {
                val cleanNumber = spamChecker.cleanPhoneNumber(testNumber)
                val tempInfo = spamChecker.createTempContactAndCheck(cleanNumber, testNumber)
                
                val status = when {
                    !tempInfo.wasCreated -> "❌ ERRORE: Impossibile creare contatto temporaneo"
                    !tempInfo.hasPhoto -> "🚨 SPAM: Numero senza foto profilo"
                    else -> "✅ SICURO: Numero con foto profilo"
                }
                
                val results = "🧪 Test completato!\n\n" +
                        "Numero testato: $testNumber\n" +
                        "Risultato: $status\n\n" +
                        "Dettagli:\n" +
                        "• Contatto creato: ${if (tempInfo.wasCreated) "✅" else "❌"}\n" +
                        "• Foto profilo: ${if (tempInfo.hasPhoto) "✅" else "❌"}\n" +
                        "• Sincronizzato: ${if (tempInfo.syncedWithSocial) "✅" else "❌"}"
                
                runOnUiThread {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("🧪 Risultato Test")
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
        Toast.makeText(this, "Test 'Salva al Volo' completato", Toast.LENGTH_SHORT).show()
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