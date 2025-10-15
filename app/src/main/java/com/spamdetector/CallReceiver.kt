package com.spamdetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "CallReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "🎯 CallReceiver attivato!")
        
        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context o Intent nulli!")
            return
        }
        
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        Log.d(TAG, "📞 Stato chiamata: $state, Numero: $incomingNumber")
        Log.d(TAG, "🔍 Action: ${intent.action}")
        Log.d(TAG, "📦 Extras: ${intent.extras}")
        
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Chiamata in arrivo
                if (!incomingNumber.isNullOrEmpty()) {
                    Log.d(TAG, "Chiamata in arrivo da: $incomingNumber")
                    
                    // 🔧 Controlla se il rilevamento spam è attivo
                    val prefs = context.getSharedPreferences("spam_detector", Context.MODE_PRIVATE)
                    val isSpamDetectionEnabled = prefs.getBoolean("spam_detection_enabled", false)
                    
                    if (isSpamDetectionEnabled) {
                        Log.d(TAG, "✅ Rilevamento spam attivo - Avvio controllo")
                        // Avvia il servizio di rilevamento spam
                        val serviceIntent = Intent(context, CallDetectionService::class.java)
                        serviceIntent.putExtra("phoneNumber", incomingNumber)
                        serviceIntent.putExtra("action", "CHECK_SPAM")
                        context.startService(serviceIntent)
                    } else {
                        Log.d(TAG, "⏸️ Rilevamento spam disattivato - Nessun controllo")
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Chiamata risposta o effettuata
                Log.d(TAG, "Chiamata in corso")
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Chiamata terminata
                Log.d(TAG, "Chiamata terminata")
            }
        }
    }
}