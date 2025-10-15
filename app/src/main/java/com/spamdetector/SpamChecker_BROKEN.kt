package com.spamdetector

import android.        // 1️⃣ Prima controlla se è già nei contatti
        val existingContact = findExistingContact(cleanNumber)
        if (existingContact != null) {
            Log.i(TAG, "✅ Contatto esistente trovato - AUTOMATICAMENTE SICURO")
            return false // Se è nei contatti = NON spam (lo conosci!)
        }.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.*

class SpamChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "SpamChecker"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
    }
    
    /**
     * � Risultato del controllo "salva al volo"
     */
    data class TempContactInfo(
        val wasCreated: Boolean,
        val hasPhoto: Boolean,
        val contactId: String? = null,
        val syncedWithSocial: Boolean = false
    )
    
    /**
     * � Verifica se un numero è spam con la tecnica "salva al volo"
     * @param phoneNumber Il numero di telefono da controllare  
     * @return true se è spam (NON ha foto dopo salvataggio), false se è sicuro (ha foto)
     */
    fun isSpam(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) { 
            Log.w(TAG, "Numero vuoto o nullo")
            return true // Considera spam i numeri vuoti
        }
        
        // Pulisce il numero da spazi e caratteri speciali
        val cleanNumber = cleanPhoneNumber(phoneNumber)
        
        Log.i(TAG, "� Inizio controllo 'salva al volo' per: $phoneNumber")
        
        // 1️⃣ Prima controlla se è già nei contatti
        val existingContact = findExistingContact(cleanNumber)
        if (existingContact != null) {
            val hasPhoto = checkContactHasPhoto(existingContact)
            Log.i(TAG, "� Contatto esistente trovato - Ha foto: $hasPhoto")
            return !hasPhoto // Se non ha foto = spam
        }
        
        // 2️⃣ Se non esiste, usa la tecnica "salva al volo"
        val tempResult = createTempContactAndCheck(cleanNumber, phoneNumber)
        
        Log.i(TAG, "� Risultato salvataggio temporaneo:")
        Log.i(TAG, "   - Creato: ${tempResult.wasCreated}")
        Log.i(TAG, "   - Ha foto: ${tempResult.hasPhoto}")
        Log.i(TAG, "   - Sincronizzato: ${tempResult.syncedWithSocial}")
        
        val isSpam = !tempResult.hasPhoto
        
        if (isSpam) {
            Log.w(TAG, "🚨 SPAM: Il numero $phoneNumber NON ha generato foto profilo")
        } else {
            Log.i(TAG, "✅ SICURO: Il numero $phoneNumber ha generato foto profilo")
        }
        
        return isSpam
    }
    
    /**
     * � Trova un contatto esistente per numero
     * @param phoneNumber Il numero da cercare
     * @return ID del contatto se trovato, null altrimenti
     */
    private fun findExistingContact(phoneNumber: String): String? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
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
                
                if (phoneNumbersMatch(phoneNumber, savedNumber)) {
                    Log.d(TAG, "📱 Contatto esistente trovato: $contactId per $phoneNumber")
                    return contactId
                }
            }
        }
        
        return null
    }
    
    /**
     * 📸 Tecnica "salva al volo" - crea contatto temporaneo e verifica foto
     * @param cleanNumber Il numero pulito
     * @param originalNumber Il numero originale per il nome
     * @return TempContactInfo con risultati
     */
    private fun createTempContactAndCheck(cleanNumber: String, originalNumber: String): TempContactInfo {
        var contactId: String? = null
        var wasCreated = false
        
        try {
            Log.d(TAG, "📸 Creo contatto temporaneo per $cleanNumber")
            
            // 1️⃣ Crea contatto temporaneo
            contactId = createTemporaryContact(cleanNumber, "TempSpamCheck_$originalNumber")
            wasCreated = contactId != null
            
            if (!wasCreated) {
                Log.w(TAG, "❌ Impossibile creare contatto temporaneo")
                return TempContactInfo(false, false)
            }
            
            Log.d(TAG, "✅ Contatto temporaneo creato con ID: $contactId")
            
            // 2️⃣ Aspetta sincronizzazione automatica (WhatsApp, social, ecc.)
            Thread.sleep(2000) // 2 secondi per sync
            
            // 3️⃣ Controlla se ha ottenuto una foto profilo
            val hasPhoto = checkContactHasPhoto(contactId!!)
            Log.d(TAG, "� Dopo sync - Ha foto: $hasPhoto")
            
            return TempContactInfo(
                wasCreated = true,
                hasPhoto = hasPhoto,
                contactId = contactId,
                syncedWithSocial = hasPhoto // Se ha foto, significa che si è sincronizzato
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante salvataggio temporaneo", e)
            return TempContactInfo(false, false)
            
        } finally {
            // 4️⃣ IMPORTANTE: Elimina sempre il contatto temporaneo
            contactId?.let { id ->
                try {
                    deleteTemporaryContact(id)
                    Log.d(TAG, "🗑️ Contatto temporaneo $id eliminato")
                } catch (e: Exception) {
                    Log.e(TAG, "Errore eliminazione contatto temporaneo $id", e)
                }
            }
        }
    }
    
    /**
     * � Crea un contatto temporaneo
     * @param phoneNumber Il numero di telefono
     * @param displayName Il nome da assegnare
     * @return ID del contatto creato, null se errore
     */
    private fun createTemporaryContact(phoneNumber: String, displayName: String): String? {
        return try {
            val values = ContentValues().apply {
                put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
                put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
            }
            
            val rawContactUri = context.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)
            val rawContactId = rawContactUri?.lastPathSegment?.toLong()
            
            if (rawContactId == null) {
                Log.e(TAG, "Errore creazione raw contact")
                return null
            }
            
            // Aggiungi nome
            val nameValues = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
            }
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)
            
            // Aggiungi numero di telefono
            val phoneValues = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            }
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
            
            // Ottieni contact ID finale
            val contactProjection = arrayOf(ContactsContract.RawContacts.CONTACT_ID)
            val contactCursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                contactProjection,
                "${ContactsContract.RawContacts._ID} = ?",
                arrayOf(rawContactId.toString()),
                null
            )
            
            contactCursor?.use {
                if (it.moveToFirst()) {
                    val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID))
                    Log.d(TAG, "📝 Contatto creato: Contact ID = $contactId, Raw ID = $rawContactId")
                    return contactId
                }
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Errore creazione contatto temporaneo", e)
            null
        }
    }
    
    /**
     * 🗑️ Elimina un contatto temporaneo
     * @param contactId L'ID del contatto da eliminare
     */
    private fun deleteTemporaryContact(contactId: String) {
        try {
            val deleteUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            val deletedRows = context.contentResolver.delete(deleteUri, null, null)
            Log.d(TAG, "🗑️ Contatto $contactId eliminato ($deletedRows righe)")
        } catch (e: Exception) {
            Log.e(TAG, "Errore eliminazione contatto $contactId", e)
        }
    }
    
    /**
     * � Controlla se un contatto ha una foto profilo
     * @param contactId L'ID del contatto da controllare
     * @return true se ha foto, false altrimenti
     */
    private fun checkContactHasPhoto(contactId: String): Boolean {
        val photoUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            "$contactId/${ContactsContract.Contacts.Photo.CONTENT_DIRECTORY}"
        )
        
        return try {
            val inputStream = context.contentResolver.openInputStream(photoUri)
            val hasPhoto = inputStream != null
            inputStream?.close()
            Log.d(TAG, "📸 Contatto $contactId ha foto: $hasPhoto")
            hasPhoto
        } catch (e: Exception) {
            Log.d(TAG, "📸 Nessuna foto per contatto $contactId")
            false
        }
    }
    
    /**
     * 🔄 Verifica se due numeri corrispondono
     */
    private fun phoneNumbersMatch(number1: String, number2: String): Boolean {
        val clean1 = cleanPhoneNumber(number1)
        val clean2 = cleanPhoneNumber(number2)
        
        // Confronto diretto
        if (clean1 == clean2) return true
        
        // Confronto senza prefisso paese
        val short1 = clean1.removePrefix("39")
        val short2 = clean2.removePrefix("39")
        if (short1 == short2) return true
        
        // Confronto ultimi 9-10 cifre
        if (clean1.length >= 9 && clean2.length >= 9) {
            val suffix1 = clean1.takeLast(9)
            val suffix2 = clean2.takeLast(9)
            if (suffix1 == suffix2) return true
        }
        
        return false
    }
    
    /**
     * 💚 Crea intent per aprire WhatsApp con numero
     */
    private fun createWhatsAppIntent(phoneNumber: String): Intent {
        val uri = Uri.parse("https://wa.me/$phoneNumber")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(WHATSAPP_PACKAGE)
        }
    }
    
    /**
     * 💚 Crea intent per aprire chat WhatsApp
     */
    private fun createWhatsAppChatIntent(phoneNumber: String): Intent {
        val uri = Uri.parse("whatsapp://send?phone=$phoneNumber")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(WHATSAPP_PACKAGE)
        }
    }
    
    /**
     * 🧹 Pulisce il numero di telefono
     */
    private fun cleanPhoneNumber(phoneNumber: String): String {
        var cleaned = phoneNumber.replace(Regex("[\\s\\-\\(\\)\\+]"), "")
        
        // Gestione prefissi Italia
        if (cleaned.startsWith("0039")) {
            cleaned = cleaned.substring(2)
        }
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2)
        }
        if (cleaned.startsWith("3") && cleaned.length == 10) {
            cleaned = "39$cleaned"
        }
        
        return cleaned
    }
    
    /**
     * � Ottiene informazioni "salva al volo" per un numero (metodo pubblico)
     */
    fun getTempContactInfo(phoneNumber: String): TempContactInfo {
        val cleanNumber = cleanPhoneNumber(phoneNumber)
        return createTempContactAndCheck(cleanNumber, phoneNumber)
    }
    
    /**
     * 📊 Statistiche del checker
     */
    fun getCheckStats(): String {
        return "📸 Controllo 'Salva al Volo' attivo\n🔍 Crea contatto temporaneo → Verifica foto → Elimina\n❌ NO foto dopo sync = SPAM\n✅ Foto generata = SICURO"
    }
    
    /**
     * 📱 Formatta numero per visualizzazione
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
}