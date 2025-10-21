package com.spamdetector

import android.content.ContentValues
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
     * 📸 Risultato del controllo "salva al volo"
     */
    data class TempContactInfo(
        val wasCreated: Boolean,
        val hasPhoto: Boolean,
        val contactId: String? = null,
        val syncedWithSocial: Boolean = false,
        val hasWhatsApp: Boolean = false
    )
    
    /**
     * 📸 Verifica se un numero è spam con la tecnica "salva al volo"
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
        
        Log.i(TAG, "🔍 Inizio controllo 'salva al volo' per: $phoneNumber")
        
        // 1️⃣ Prima controlla se è già nei contatti
        val existingContact = findExistingContact(cleanNumber)
        if (existingContact != null) {
            Log.i(TAG, "✅ Contatto esistente trovato - AUTOMATICAMENTE SICURO")
            return false // Se è nei contatti = NON spam (lo conosci!)
        }
        
        // 2️⃣ Se non esiste, usa la tecnica "salva al volo"
        val tempResult = createTempContactAndCheck(cleanNumber, phoneNumber)
        
        Log.i(TAG, "📸 Risultato salvataggio temporaneo:")
        Log.i(TAG, "   - Creato: ${tempResult.wasCreated}")
        Log.i(TAG, "   - Ha foto: ${tempResult.hasPhoto}")
        Log.i(TAG, "   - Sincronizzato: ${tempResult.syncedWithSocial}")
        
    val isSpam = !(tempResult.hasWhatsApp || tempResult.hasPhoto)
        
        if (isSpam) {
            Log.w(TAG, "🚨 SPAM: Il numero $phoneNumber NON ha generato foto profilo")
        } else {
            Log.i(TAG, "✅ SICURO: Il numero $phoneNumber ha generato foto profilo")
        }
        
        return isSpam
    }
    
    /**
     * 📱 Trova un contatto esistente per numero
     * @param phoneNumber Il numero da cercare
     * @return ID del contatto se trovato, null altrimenti
     */
    private fun findExistingContact(phoneNumber: String): String? {
        // Usa PhoneLookup per un matching più accurato e performante
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
                ContactsContract.PhoneLookup.NUMBER
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    val foundId = if (idIndex >= 0) cursor.getLong(idIndex).toString() else null
                    val savedNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER))
                    if (foundId != null && phoneNumbersMatch(phoneNumber, savedNumber)) {
                        Log.d(TAG, "📱 Contatto esistente trovato: $foundId per $phoneNumber")
                        return foundId
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Errore ricerca contatto esistente", e)
            null
        }
    }

    /**
     * 🔎 Verifica pubblica se un numero è già presente in rubrica
     */
    fun isInContacts(phoneNumber: String): Boolean {
        val clean = cleanPhoneNumber(phoneNumber)
        return findExistingContact(clean) != null
    }
    
    /**
     * 📸 Tecnica "salva al volo" - crea contatto temporaneo e verifica foto
     * @param cleanNumber Il numero pulito
     * @param originalNumber Il numero originale per il nome
     * @return TempContactInfo con risultati
     */
    fun createTempContactAndCheck(cleanNumber: String, originalNumber: String): TempContactInfo {
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
            
            // 2️⃣ Attende la possibile sincronizzazione (polling fino a timeout)
            val maxWait = 12000L
            val step = 500L
            var hasPhoto = false
            var hasWhatsApp = false
            val start = System.currentTimeMillis()
            do {
                if (!hasPhoto) hasPhoto = checkContactHasPhoto(contactId!!)
                if (!hasWhatsApp) hasWhatsApp = checkContactHasWhatsApp(contactId!!, cleanNumber)
                if (hasPhoto || hasWhatsApp) break
                try { Thread.sleep(step) } catch (_: InterruptedException) {}
            } while (System.currentTimeMillis() - start < maxWait)
            Log.d(TAG, "📸 Dopo sync - Ha foto: $hasPhoto | 📱 WhatsApp: $hasWhatsApp")
            
            return TempContactInfo(
                wasCreated = true,
                hasPhoto = hasPhoto,
                contactId = contactId,
                syncedWithSocial = hasPhoto || hasWhatsApp, // Se ha foto o WA, consideriamo sincronizzato
                hasWhatsApp = hasWhatsApp
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
     * 📱 Controlla se un contatto ha integrazione WhatsApp (Data/RawContacts)
     * Nota: richiede che l'utente abbia concesso a WhatsApp l'accesso ai contatti
     */
    private fun checkContactHasWhatsApp(contactId: String, cleanNumber: String): Boolean {
        // 1) Cerca RawContacts con account-type WhatsApp
        try {
            val rawProjection = arrayOf(
                ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE
            )
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                rawProjection,
                "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
                arrayOf(contactId, "com.whatsapp"),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    Log.d(TAG, "📱 WA: trovato RawContact com.whatsapp per contatto $contactId")
                    return true
                }
            }
        } catch (_: Exception) {}

        // 2) Cerca Data rows con mimetype WhatsApp
        try {
            val dataProjection = arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1
            )
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                dataProjection,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} LIKE ?",
                arrayOf(contactId, "%com.whatsapp%"),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    Log.d(TAG, "📱 WA: trovato Data mimetype WhatsApp per contatto $contactId")
                    return true
                }
            }
        } catch (_: Exception) {}

        return false
    }
    
    /**
     * 📝 Crea un contatto temporaneo
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
     * 📸 Controlla se un contatto ha una foto profilo
     * @param contactId L'ID del contatto da controllare
     * @return true se ha foto, false altrimenti
     */
    private fun checkContactHasPhoto(contactId: String): Boolean {
        // 1) Usa l'API helper per foto high-res quando disponibile
        try {
            val contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, contactUri, true)?.use {
                Log.d(TAG, "📸 Contatto $contactId ha foto (high-res)")
                return true
            }
        } catch (_: Exception) {}

        // 2) Fallback: controlla PHOTO_URI / PHOTO_THUMBNAIL_URI sulla tabella Contatti aggregati
        try {
            val projection = arrayOf(
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            )
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId),
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val photoUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                    val thumbUri = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
                    val has = !photoUri.isNullOrBlank() || !thumbUri.isNullOrBlank()
                    if (has) Log.d(TAG, "📸 Contatto $contactId ha photoUri=$photoUri thumb=$thumbUri")
                    return has
                }
            }
        } catch (_: Exception) {}

        // 3) Ultimo tentativo: vecchio percorso /photo
        val legacyUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_URI,
            "$contactId/${ContactsContract.Contacts.Photo.CONTENT_DIRECTORY}"
        )
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(legacyUri)
            val hasPhoto = inputStream != null
            inputStream?.close()
            if (hasPhoto) Log.d(TAG, "📸 Contatto $contactId ha foto (legacy)")
            hasPhoto
        } catch (_: Exception) {
            false
        }
    }

    /**
     * ⏳ Attende fino a maxWaitMs in polling che compaia una foto per il contatto
     */
    private fun waitForPhoto(contactId: String, maxWaitMs: Long, stepMs: Long): Boolean {
        val start = System.currentTimeMillis()
        do {
            if (checkContactHasPhoto(contactId)) return true
            try { Thread.sleep(stepMs) } catch (_: InterruptedException) {}
        } while (System.currentTimeMillis() - start < maxWaitMs)
        return false
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
     * 🧹 Pulisce il numero di telefono
     */
    fun cleanPhoneNumber(phoneNumber: String): String {
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
     * 📸 Ottiene informazioni "salva al volo" per un numero (metodo pubblico)
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