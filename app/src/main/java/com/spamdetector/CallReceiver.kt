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
        if (context == null || intent == null) return
        
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        Log.d(TAG, "Stato chiamata: $state, Numero: $incomingNumber")
        
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Chiamata in arrivo
                if (!incomingNumber.isNullOrEmpty()) {
                    Log.d(TAG, "Chiamata in arrivo da: $incomingNumber")
                    
                    // Avvia il servizio di rilevamento spam
                    val serviceIntent = Intent(context, CallDetectionService::class.java)
                    serviceIntent.putExtra("phone_number", incomingNumber)
                    serviceIntent.putExtra("action", "CHECK_SPAM")
                    context.startService(serviceIntent)
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