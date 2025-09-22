package com.spamdetector

import android.app.NotificationChannel
import android.app.NotificationManager
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
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Possibile Spam Rilevato")
            .setContentText("Chiamata sospetta da: $phoneNumber")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Il numero $phoneNumber NON ha WhatsApp e potrebbe essere spam."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
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