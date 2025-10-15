package com.spamdetector

import android.app.NotificationChannel
import andro        val detailText = when {
            !tempInfo.wasCreated -> 
                "❌ Impossibile creare contatto temporaneo per verificare $phoneNumber.\n\n" +
                "🔒 Controlla i permessi dell'app per accedere ai contatti."
            
            !tempInfo.hasPhoto -> 
                "📸 Il numero $phoneNumber è stato salvato temporaneamente ma NON ha generato foto profilo.\n\n" +
                "🚨 Probabilmente è spam, call center o numero commerciale.\n\n" +
                "💡 I numeri veri solitamente si sincronizzano con foto da social/WhatsApp."
            
            else -> 
                "✅ Il numero $phoneNumber ha generato una foto profilo dopo il salvataggio.\n\n" +
                "👤 Probabilmente è una persona vera con account social attivi."
        }cationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class CallDetectionService : Service() {
    
    companion object {
        private const val TAG = "CallDetectionService"
        private const val NOTIFICATION_CHANNEL_ID = "spam_detection_channel"
        private const val NOTIFICATION_ID = 1
    }
    
    private lateinit var notificationManager: NotificationManager
    private lateinit var spamChecker: SpamChecker

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servizio creato")
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        spamChecker = SpamChecker(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Servizio avviato")
        
        intent?.let { serviceIntent ->
            val action = serviceIntent.getStringExtra("action")
            val phoneNumber = serviceIntent.getStringExtra("phone_number")
            
            when (action) {
                "CHECK_SPAM" -> {
                    if (!phoneNumber.isNullOrEmpty()) {
                        checkForSpam(phoneNumber)
                    }
                }
            }
        }
        
        return START_NOT_STICKY
    }

    private fun checkForSpam(phoneNumber: String) {
        Log.d(TAG, "Controllo spam per numero: $phoneNumber")
        
        val isSpam = spamChecker.isSpam(phoneNumber)
        
        if (isSpam) {
            Log.w(TAG, "Rilevato possibile spam da: $phoneNumber")
            showSpamNotification(phoneNumber)
        } else {
            Log.i(TAG, "Numero verificato come lecito: $phoneNumber")
        }
    }

    private fun showSpamNotification(phoneNumber: String) {
        // � Ottieni informazioni "salva al volo" per la notifica
        val tempInfo = spamChecker.getTempContactInfo(phoneNumber)
        
        val title = when {
            !tempInfo.wasCreated -> "🚨 ERRORE CONTROLLO"
            !tempInfo.hasPhoto -> "🚨 NESSUNA FOTO PROFILO"
            else -> "✅ NUMERO VERIFICATO"
        }
        
        val message = when {
            !tempInfo.wasCreated -> "Impossibile verificare numero"
            !tempInfo.hasPhoto -> "Nessuna foto dopo sincronizzazione"
            else -> "Foto profilo trovata"
        }
        
        val detailText = when {
            !whatsappInfo.hasWhatsApp -> 
                "� Il numero $phoneNumber NON ha WhatsApp.\n\n" +
                "� Probabilmente è un call center, spam o numero commerciale.\n\n" +
                "💡 Le persone vere solitamente hanno WhatsApp."
            
            !whatsappInfo.hasPhoto -> 
                "� Il numero $phoneNumber ha WhatsApp ma NON ha foto profilo.\n\n" +
                "⚠️ Potrebbe essere un account business, spam o fake.\n\n" +
                "💡 Le persone vere di solito hanno una foto profilo."
            
            else -> "🔍 Chiamata rilevata come sospetta dal sistema."
        }
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("⚠️ $phoneNumber - $message")
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID + System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Rilevamento Spam",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per chiamate spam rilevate"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Servizio distrutto")
    }
}