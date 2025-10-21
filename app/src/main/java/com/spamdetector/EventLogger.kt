package com.spamdetector

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Logger semplice basato su SharedPreferences per tracciare gli esiti dei controlli.
 * Conserva gli ultimi MAX_ENTRIES elementi.
 */
object EventLogger {

    private const val PREFS = "spam_detector"
    private const val KEY_HISTORY = "history"
    private const val MAX_ENTRIES = 50

    data class HistoryItem(
        val timestamp: Long,
        val phoneNumber: String,
        val isKnownContact: Boolean,
        val wasCreated: Boolean,
        val hasPhoto: Boolean,
        val syncedWithSocial: Boolean,
        val hasWhatsApp: Boolean
    )

    fun logCheck(
        context: Context,
        phoneNumber: String,
        isKnownContact: Boolean,
        wasCreated: Boolean,
        hasPhoto: Boolean,
        syncedWithSocial: Boolean,
        hasWhatsApp: Boolean
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))

        val obj = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("phoneNumber", phoneNumber)
            put("isKnownContact", isKnownContact)
            put("wasCreated", wasCreated)
            put("hasPhoto", hasPhoto)
            put("syncedWithSocial", syncedWithSocial)
            put("hasWhatsApp", hasWhatsApp)
        }

        // Aggiungi in testa
        val newArr = JSONArray()
        newArr.put(obj)
        for (i in 0 until arr.length()) {
            if (i >= MAX_ENTRIES - 1) break
            newArr.put(arr.getJSONObject(i))
        }

        prefs.edit().putString(KEY_HISTORY, newArr.toString()).apply()
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        val list = mutableListOf<HistoryItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                HistoryItem(
                    timestamp = o.optLong("timestamp"),
                    phoneNumber = o.optString("phoneNumber"),
                    isKnownContact = o.optBoolean("isKnownContact"),
                    wasCreated = o.optBoolean("wasCreated"),
                    hasPhoto = o.optBoolean("hasPhoto"),
                    syncedWithSocial = o.optBoolean("syncedWithSocial"),
                    hasWhatsApp = o.optBoolean("hasWhatsApp")
                )
            )
        }
        return list
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
