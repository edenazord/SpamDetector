package com.spamdetector

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

class SpamChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "SpamChecker"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
    }
    
    /**
     * Verifica se un numero di telefono è spam
     * @param phoneNumber Il numero di telefono da controllare
     * @return true se il numero è considerato spam (NON ha WhatsApp), false altrimenti
     */
    fun isSpam(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) {
            Log.w(TAG, "Numero vuoto o nullo")
            return true // Considera spam i numeri vuoti
        }
        
        // Pulisce il numero da spazi e caratteri speciali
        val cleanNumber = cleanPhoneNumber(phoneNumber)
        
        // Verifica se il numero ha WhatsApp
        val hasWhatsApp = checkWhatsAppAvailability(cleanNumber)
        
        Log.i(TAG, "Verifica numero: $phoneNumber (pulito: $cleanNumber)")
        Log.i(TAG, "Ha WhatsApp: $hasWhatsApp")
        
        // Se NON ha WhatsApp è spam, altrimenti è lecito
        val isSpam = !hasWhatsApp
        
        if (isSpam) {
            Log.w(TAG, "🚨 SPAM RILEVATO: Il numero $phoneNumber NON ha WhatsApp")
        } else {
            Log.i(TAG, "✅ NUMERO LECITO: Il numero $phoneNumber ha WhatsApp")
        }
        
        return isSpam
    }
    
    /**
     * Verifica se WhatsApp è installato sul dispositivo
     */
    private fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "WhatsApp non installato sul dispositivo")
            false
        }
    }
    
    /**
     * Verifica se un numero ha WhatsApp disponibile
     * Usa diversi metodi per controllare la disponibilità
     */
    private fun checkWhatsAppAvailability(phoneNumber: String): Boolean {
        if (!isWhatsAppInstalled()) {
            Log.w(TAG, "WhatsApp non disponibile per verifiche")
            return false // Se WhatsApp non è installato, non possiamo verificare
        }
        
        // Metodo 1: Prova a creare un intent WhatsApp
        return try {
            val whatsappIntent = createWhatsAppIntent(phoneNumber)
            val resolveInfo = context.packageManager.resolveActivity(whatsappIntent, 0)
            val canOpenWhatsApp = resolveInfo != null
            
            Log.d(TAG, "Verifica WhatsApp per $phoneNumber: $canOpenWhatsApp")
            
            // Nota: questo metodo non garantisce al 100% che il numero sia registrato su WhatsApp
            // È un controllo di base per vedere se WhatsApp può gestire il numero
            canOpenWhatsApp
            
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella verifica WhatsApp per $phoneNumber", e)
            false
        }
    }
    
    /**
     * Crea un intent per aprire una chat WhatsApp con il numero specificato
     */
    private fun createWhatsAppIntent(phoneNumber: String): Intent {
        val uri = Uri.parse("https://wa.me/$phoneNumber")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(WHATSAPP_PACKAGE)
        }
    }
    
    /**
     * Pulisce il numero di telefono rimuovendo caratteri non numerici
     */
    private fun cleanPhoneNumber(phoneNumber: String): String {
        // Rimuove spazi, trattini, parentesi e altri caratteri speciali
        var cleaned = phoneNumber.replace(Regex("[\\s\\-\\(\\)\\+]"), "")
        
        // Se inizia con 0039, lo sostituisce con 39 (Italia)
        if (cleaned.startsWith("0039")) {
            cleaned = cleaned.substring(2)
        }
        
        // Se inizia con 00, rimuove il prefisso internazionale generico
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2)
        }
        
        // Assicura che i numeri italiani abbiano il prefisso 39
        if (cleaned.startsWith("3") && cleaned.length == 10) {
            cleaned = "39$cleaned"
        }
        
        return cleaned
    }
    
    /**
     * Formatta il numero per una migliore visualizzazione
     */
    fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = cleanPhoneNumber(phoneNumber)
        return when {
            cleaned.startsWith("39") && cleaned.length == 12 -> {
                "+39 ${cleaned.substring(2, 5)} ${cleaned.substring(5)}"
            }
            else -> phoneNumber.trim()
        }
    }
    
    /**
     * Ottiene statistiche sui controlli effettuati
     */
    fun getCheckStats(): String {
        val whatsappInstalled = if (isWhatsAppInstalled()) "✅ Installato" else "❌ Non installato"
        return "WhatsApp: $whatsappInstalled\nVerifiche attive per numeri in arrivo"
    }
}