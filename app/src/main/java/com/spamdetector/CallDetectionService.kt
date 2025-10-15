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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CallDetectionService : Service() {
    
    companion object {
        private const val TAG = "CallDetectionService"
        private const val NOTIFICATION_CHANNEL_ID = "spam_detection_channel"
        private const val NOTIFICATION_ID = 1
    }
    
    private lateinit var notificationManager: NotificationManager
    private lateinit var spamChecker: SpamChecker
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

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
            val phoneNumber = serviceIntent.getStringExtra("phoneNumber")
            
            when (action) {
                "CHECK_SPAM" -> {
                    phoneNumber?.let { number ->
                        checkSpam(number)
                    }
                }
                else -> {
                    Log.w(TAG, "Azione sconosciuta: $action")
                }
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun checkSpam(phoneNumber: String) {
        Log.d(TAG, "Controllo spam per numero: $phoneNumber")
        
        serviceScope.launch {
            try {
                val tempInfo = spamChecker.createTempContactAndCheck(phoneNumber)
                showSpamNotification(phoneNumber, tempInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante controllo spam", e)
                showErrorNotification(phoneNumber, e.message)
            }
        }
    }

    private fun showSpamNotification(phoneNumber: String, tempInfo: SpamChecker.TempContactInfo) {
        val title = when {
            !tempInfo.wasCreated -> "Errore Controllo Spam"
            !tempInfo.hasPhoto -> "Possibile Spam Rilevato"
            else -> "Chiamata Legittima"
        }
        
        val detailText = when {
            !tempInfo.wasCreated -> 
                "Impossibile creare contatto temporaneo per verificare $phoneNumber. Controlla i permessi dell'app per accedere ai contatti."
            
            !tempInfo.hasPhoto -> 
                "Il numero $phoneNumber e stato salvato temporaneamente ma NON ha generato foto profilo. Probabilmente e spam, call center o numero commerciale."
            
            else -> 
                "Il numero $phoneNumber ha generato una foto profilo dopo il salvataggio. Probabilmente e una persona vera con account social attivi."
        }
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Tocca per dettagli")
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notifica mostrata: $title")
    }

    private fun showErrorNotification(phoneNumber: String, errorMessage: String?) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Errore SpamDetector")
            .setContentText("Errore controllo per $phoneNumber")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Errore durante il controllo spam per $phoneNumber: $errorMessage"
            ))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.e(TAG, "Notifica errore mostrata per $phoneNumber: $errorMessage")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "SpamDetector Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche per rilevamento spam automatico"
                enableLights(true)
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Canale notifiche creato")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Servizio distrutto")
    }
}
