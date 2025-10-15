pac        val results = StringBuilder("🧪 Test 'Salva al Volo':\n\n")
        
        testNumbers.forEach { number ->
            val tempInfo = spamChecker.getTempContactInfo(number)
            val isSpam = spamChecker.isSpam(number)
            
            val status = when {
                !tempInfo.wasCreated -> "❌ Errore creazione (ERRORE)"
                !tempInfo.hasPhoto -> "🚨 NO foto sync (SPAM)"
                else -> "✅ Foto generata (SICURO)"
            }
            
            results.append("$number → $status\n")
            results.append("   📝 Creato: ${tempInfo.wasCreated}\n")
            results.append("   📸 Foto: ${tempInfo.hasPhoto}\n")
            results.append("   🔄 Sync: ${tempInfo.syncedWithSocial}\n")
        }ctor

import android.Manifest
import android.content.pm.Packag        val results = StringBuilder("🧪 Test Rilevamento Spam (WhatsApp + Foto):\n\n")
        
        testNumbers.forEach { number ->
            val whatsappInfo = spamChecker.getWhatsAppInfo(number)
            val isSpam = spamChecker.isSpam(number)
            
            val status = when {
                !whatsappInfo.hasWhatsApp -> "🚨 NO WhatsApp (SPAM)"
                whatsappInfo.hasWhatsApp && whatsappInfo.hasPhoto -> "✅ WhatsApp + Foto (SICURO)"
                whatsappInfo.hasWhatsApp && !whatsappInfo.hasPhoto -> "⚠️ WhatsApp senza foto (SOSPETTO)"
                else -> "❓ Indeterminato"
            }
            
            results.append("$number → $status\n")
            results.append("   💚 WhatsApp: ${whatsappInfo.hasWhatsApp}\n")
            results.append("   � Foto: ${whatsappInfo.hasPhoto}\n")
        }
        
        results.append("\n📊 ${spamChecker.getCheckStats()}")

        statusTextView.text = results.toString()
        
        Toast.makeText(this, "Test 'Salva al Volo' completato", Toast.LENGTH_SHORT).show()ort android.os.Bundle
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
        // Test con numeri di esempio
        val testNumbers = listOf(
            "+393331234567",  // Numero italiano tipico
            "+393401234567",  // Altro numero italiano
            "+12025551234",   // Numero USA (probabilmente non in rubrica)
            "+441234567890"   // Numero UK
        )

        val results = StringBuilder("🧪 Test Rilevamento Foto Profilo:\n\n")
        
        testNumbers.forEach { number ->
            val isSpam = spamChecker.isSpam(number)
            val status = if (isSpam) "🚨 NO Foto (SPAM)" else "✅ Ha Foto (SICURO)"
            results.append("$number → $status\n")
        }
        
        results.append("\n� ${spamChecker.getCheckStats()}")

        statusTextView.text = results.toString()
        
        Toast.makeText(this, "Test controllo foto completato", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val isEnabled = enableSwitch.isChecked
        val hasPermissions = hasRequiredPermissions()
        
        val status = when {
            !hasPermissions -> "❌ Permessi mancanti\n\nL'app ha bisogno dei permessi per accedere alle chiamate e inviare notifiche."
            isEnabled -> "✅ Rilevamento Attivo\n\nL'app sta monitorando le chiamate in arrivo.\n\n${spamChecker.getCheckStats()}"
            else -> "⏸️ Rilevamento Disattivato\n\nAttiva il rilevamento per iniziare a monitorare le chiamate spam."
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