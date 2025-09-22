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
        setupListeners()
        updateStatus()
        checkPermissions()
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
        Toast.makeText(this, "Rilevamento spam attivato", Toast.LENGTH_SHORT).show()
    }

    private fun disableSpamDetection() {
        Toast.makeText(this, "Rilevamento spam disattivato", Toast.LENGTH_SHORT).show()
    }

    private fun testSpamDetection() {
        // Test con numeri di esempio
        val testNumbers = listOf(
            "+393331234567",  // Numero italiano tipico
            "+393401234567",  // Altro numero italiano
            "+12025551234",   // Numero USA (probabilmente senza WhatsApp locale)
            "+441234567890"   // Numero UK
        )

        val results = StringBuilder("🧪 Test Rilevamento WhatsApp:\n\n")
        
        testNumbers.forEach { number ->
            val isSpam = spamChecker.isSpam(number)
            val status = if (isSpam) "🚨 NO WhatsApp (SPAM)" else "✅ Ha WhatsApp (LECITO)"
            results.append("$number → $status\n")
        }
        
        results.append("\n📋 Stato WhatsApp:\n${spamChecker.getCheckStats()}")

        statusTextView.text = results.toString()
        
        Toast.makeText(this, "Test WhatsApp completato", Toast.LENGTH_SHORT).show()
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