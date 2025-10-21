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
                val cleanNumber = spamChecker.cleanPhoneNumber(phoneNumber)

                // Se il numero è già in rubrica, notifica come "legittimo" e non fare controlli
                if (spamChecker.isInContacts(cleanNumber)) {
                    // Logga l'esito in modalità verifica
                    EventLogger.logCheck(
                        this@CallDetectionService,
                        phoneNumber = phoneNumber,
                        isKnownContact = true,
                        wasCreated = false,
                        hasPhoto = true, // contatto noto: consideriamo "sicuro"
                        syncedWithSocial = false
                    )
                    showKnownContactNotification(phoneNumber)
                    return@launch
                }

                // Esegui le operazioni IO fuori dal main thread
                val tempInfo = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    spamChecker.createTempContactAndCheck(cleanNumber, phoneNumber)
                }
                // Log per verifica esito
                EventLogger.logCheck(
                    this@CallDetectionService,
                    phoneNumber = phoneNumber,
                    isKnownContact = false,
                    wasCreated = tempInfo.wasCreated,
                    hasPhoto = tempInfo.hasPhoto,
                        syncedWithSocial = tempInfo.syncedWithSocial,
                        hasWhatsApp = tempInfo.hasWhatsApp
                )
                showSpamNotification(phoneNumber, tempInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante controllo spam", e)
                showErrorNotification(phoneNumber, e.message)
            }
        }
    }

    private fun showKnownContactNotification(phoneNumber: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Contatto salvato in rubrica")
            .setContentText("Chiamata da $phoneNumber: nessun controllo spam necessario")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Notifica contatto noto mostrata")
    }

    private fun showSpamNotification(phoneNumber: String, tempInfo: SpamChecker.TempContactInfo) {
        val title = when {
            !tempInfo.wasCreated -> "Errore Controllo"
            tempInfo.hasWhatsApp || tempInfo.hasPhoto -> "Chiamata Prob. Legittima"
            else -> "Possibile Spam Rilevato"
        }

        val detailText = when {
            !tempInfo.wasCreated ->
                "Impossibile verificare $phoneNumber (creazione contatto fallita). Controlla i permessi ai Contatti."

            tempInfo.hasWhatsApp ->
                "WhatsApp rilevato per $phoneNumber. Classificato come NON spam."

            tempInfo.hasPhoto ->
                "$phoneNumber ha una foto profilo (fonte contatti). Classificato come NON spam."

            else ->
                "$phoneNumber non ha segnali (niente WhatsApp/foto). Possibile spam, call center o numero commerciale."
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
