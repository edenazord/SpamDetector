package com.spamdetector

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import java.io.InputStream

class SpamChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "SpamChecker"
    }
    
    /**
     * � Informazioni WhatsApp di un numero
     */
    data class WhatsAppInfo(
        val hasWhatsApp: Boolean,
        val hasPhoto: Boolean,
        val profileName: String? = null
    )
    
    /**
     * 🔍 Verifica se un numero di telefono è spam usando logica contatti + foto
     * @param phoneNumber Il numero di telefono da controllare
     * @return true se il numero è considerato spam, false altrimenti
     */
    fun isSpam(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) { 
            Log.w(TAG, "Numero vuoto o nullo")
            return true // Considera spam i numeri vuoti
        }
        
        // Pulisce il numero da spazi e caratteri speciali
        val cleanNumber = cleanPhoneNumber(phoneNumber)
        
        // � Controlla se il numero è nei contatti
        val contactInfo = checkContactInfo(cleanNumber)
        
        Log.i(TAG, "🔍 Verifica numero: $phoneNumber (pulito: $cleanNumber)")
        Log.i(TAG, "� Nei contatti: ${contactInfo.isInContacts}")
        Log.i(TAG, "�📸 Ha foto profilo: ${contactInfo.hasPhoto}")
        
        val isSpam = when {
            // 📱 NON è nei contatti = SPAM certo
            !contactInfo.isInContacts -> {
                Log.w(TAG, "🚨 SPAM: Il numero $phoneNumber NON è nei contatti")
                true
            }
            // 📱 È nei contatti E ha foto = SICURO
            contactInfo.isInContacts && contactInfo.hasPhoto -> {
                Log.i(TAG, "✅ SICURO: Contatto $phoneNumber con foto profilo")
                false
            }
            // 📱 È nei contatti ma NO foto = SOSPETTO (potrebbe essere spam)
            contactInfo.isInContacts && !contactInfo.hasPhoto -> {
                Log.w(TAG, "⚠️ SOSPETTO: Contatto $phoneNumber senza foto profilo")
                true // Consideriamo sospetti anche i contatti senza foto
            }
            else -> true
        }
        
        return isSpam
    }
    
    /**
     * � Ottiene informazioni complete su un contatto
     * @param phoneNumber Il numero da verificare
     * @return ContactInfo con stato contatto e foto
     */
    private fun checkContactInfo(phoneNumber: String): ContactInfo {
        try {
            // 1️⃣ Cerca il contatto per numero di telefono
            val contactData = findContactByPhoneNumber(phoneNumber)
            
            if (contactData != null) {
                Log.d(TAG, "📱 Contatto trovato: ${contactData.first} (${contactData.second})")
                
                // 2️⃣ Controlla se ha una foto profilo
                val hasPhoto = checkContactHasPhoto(contactData.first)
                Log.d(TAG, "📸 Contatto ${contactData.first} ha foto: $hasPhoto")
                
                return ContactInfo(
                    isInContacts = true,
                    hasPhoto = hasPhoto,
                    contactName = contactData.second
                )
            } else {
                Log.d(TAG, "📱 Contatto NON trovato per numero: $phoneNumber")
                return ContactInfo(
                    isInContacts = false,
                    hasPhoto = false
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Errore controllo contatto per $phoneNumber", e)
            return ContactInfo(
                isInContacts = false,
                hasPhoto = false
            )
        }
    }
    
    /**
     * 🔍 Trova un contatto tramite numero di telefono
     * @return Pair<ContactId, ContactName> se trovato, null altrimenti
     */
    private fun findContactByPhoneNumber(phoneNumber: String): Pair<String, String>? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        
        cursor?.use {
            while (it.moveToNext()) {
                val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val savedNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val contactName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                
                // Confronta i numeri puliti
                if (phoneNumbersMatch(phoneNumber, savedNumber)) {
                    Log.d(TAG, "📱 Match trovato: $phoneNumber ≈ $savedNumber (ID: $contactId, Nome: $contactName)")
                    return Pair(contactId, contactName ?: "Sconosciuto")
                }
            }
        }
        
        return null
    }
    
    /**
     * 📸 Controlla se un contatto ha una foto profilo
     */
    private fun checkContactHasPhoto(contactId: String): Boolean {
        val photoUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            "$contactId/${ContactsContract.Contacts.Photo.CONTENT_DIRECTORY}"
        )
        
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(photoUri)
            val hasPhoto = inputStream != null
            inputStream?.close()
            hasPhoto
        } catch (e: Exception) {
            Log.d(TAG, "📸 Nessuna foto per contatto $contactId")
            false
        }
    }
    
    /**
     * 🔄 Verifica se due numeri di telefono corrispondono
     */
    private fun phoneNumbersMatch(number1: String, number2: String): Boolean {
        val clean1 = cleanPhoneNumber(number1)
        val clean2 = cleanPhoneNumber(number2)
        
        // Confronto diretto
        if (clean1 == clean2) return true
        
        // Confronto senza prefisso paese per numeri locali
        val short1 = clean1.removePrefix("39")
        val short2 = clean2.removePrefix("39")
        if (short1 == short2) return true
        
        // Confronto ultimi 9-10 cifre per numeri mobili
        if (clean1.length >= 9 && clean2.length >= 9) {
            val suffix1 = clean1.takeLast(9)
            val suffix2 = clean2.takeLast(9)
            if (suffix1 == suffix2) return true
        }
        
        return false
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
     * 📊 Ottiene statistiche sui controlli effettuati
     */
    fun getCheckStats(): String {
        return "📸 Controllo contatti + foto profilo attivo\n🔍 Logica: NON nei contatti = SPAM\n📱 Nei contatti + foto = SICURO\n⚠️ Nei contatti senza foto = SOSPETTO"
    }
    
    /**
     * 📋 Ottiene dettagli pubblici di un contatto (per notifiche)
     */
    fun getContactDetails(phoneNumber: String): ContactInfo {
        val cleanNumber = cleanPhoneNumber(phoneNumber)
        return checkContactInfo(cleanNumber)
    }
}