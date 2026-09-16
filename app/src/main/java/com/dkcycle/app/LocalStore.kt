package com.dkcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class LocalStore(context: Context) {
    // Keep the original preference file name so older DKCycle/Lunara data survives upgrades.
    private val prefs = context.getSharedPreferences("dkcycle", Context.MODE_PRIVATE)

    fun loadLogs(): Map<LocalDate, DailyLog> {
        val raw = prefs.getString(KEY_LOGS, null) ?: return emptyMap()
        return runCatching { decodeLogs(raw) }.getOrDefault(emptyMap())
    }

    fun saveLogs(logs: Map<LocalDate, DailyLog>) {
        prefs.edit().putString(KEY_LOGS, encodeLogs(logs)).apply()
    }

    fun exportJson(logs: Map<LocalDate, DailyLog>): String = JSONObject().apply {
        put("format", "Lunara")
        put("version", 3)
        put("logs", JSONArray(encodeLogs(logs)))
    }.toString(2)

    fun importJson(raw: String): Map<LocalDate, DailyLog> {
        val root = JSONObject(raw)
        val format = root.optString("format")
        require(format == "Lunara" || format == "DKCycle") { "Not a Lunara or DKCycle export" }
        return decodeLogs(root.getJSONArray("logs").toString())
    }

    fun hasShownHomeShortcutPrompt(): Boolean = prefs.getBoolean(KEY_HOME_PROMPT, false)
    fun markHomeShortcutPromptShown() { prefs.edit().putBoolean(KEY_HOME_PROMPT, true).apply() }

    fun intimacyMarkerEnabled(): Boolean = prefs.getBoolean(KEY_INTIMACY_MARKER, false)
    fun setIntimacyMarkerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTIMACY_MARKER, enabled).apply()
    }

    // Legacy V0.2 Partner Pass is retained only so an old value can be cleared during migration.
    fun clearLegacyPartnerPass() { prefs.edit().remove(KEY_PARTNER_PASS).apply() }

    private fun encodeLogs(logs: Map<LocalDate, DailyLog>): String {
        val array = JSONArray()
        logs.values.sortedBy { it.date }.forEach { log ->
            array.put(JSONObject().apply {
                put("date", log.date.toString())
                put("flow", log.flow.name)
                put("symptoms", JSONArray(log.symptoms.sorted()))
                put("mood", log.mood)
                put("pain", log.pain)
                if (log.temperatureC != null) put("temperatureC", log.temperatureC)
                put("intimacy", log.intimacy)
                put("notes", log.notes)
            })
        }
        return array.toString()
    }

    private fun decodeLogs(raw: String): Map<LocalDate, DailyLog> {
        val array = JSONArray(raw)
        val result = linkedMapOf<LocalDate, DailyLog>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val date = LocalDate.parse(item.getString("date"))
            val symptomsArray = item.optJSONArray("symptoms") ?: JSONArray()
            val symptoms = buildSet {
                for (j in 0 until symptomsArray.length()) add(symptomsArray.getString(j))
            }
            val log = DailyLog(
                date = date,
                flow = runCatching { FlowIntensity.valueOf(item.optString("flow", "NONE")) }
                    .getOrDefault(FlowIntensity.NONE),
                symptoms = symptoms,
                mood = item.optString("mood", ""),
                pain = item.optInt("pain", 0).coerceIn(0, 10),
                temperatureC = if (item.has("temperatureC")) item.optDouble("temperatureC") else null,
                intimacy = item.optBoolean("intimacy", false),
                notes = item.optString("notes", "")
            )
            if (log.hasMeaningfulData()) result[date] = log
        }
        return result
    }

    companion object {
        private const val KEY_LOGS = "logs_json"
        private const val KEY_HOME_PROMPT = "home_shortcut_prompt_shown"
        private const val KEY_INTIMACY_MARKER = "intimacy_marker_enabled"
        private const val KEY_PARTNER_PASS = "partner_pass"
    }
}
