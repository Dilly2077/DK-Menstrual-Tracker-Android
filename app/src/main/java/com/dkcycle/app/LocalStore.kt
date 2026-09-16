package com.dkcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("dkcycle", Context.MODE_PRIVATE)

    fun loadLogs(): Map<LocalDate, DailyLog> {
        val raw = prefs.getString(KEY_LOGS, null) ?: return emptyMap()
        return runCatching { decodeLogs(raw) }.getOrDefault(emptyMap())
    }

    fun saveLogs(logs: Map<LocalDate, DailyLog>) {
        prefs.edit().putString(KEY_LOGS, encodeLogs(logs)).apply()
    }

    fun exportJson(logs: Map<LocalDate, DailyLog>): String = JSONObject().apply {
        put("format", "DKCycle")
        put("version", 1)
        put("logs", JSONArray(encodeLogs(logs)))
    }.toString(2)

    fun importJson(raw: String): Map<LocalDate, DailyLog> {
        val root = JSONObject(raw)
        require(root.optString("format") == "DKCycle") { "Not a DKCycle export" }
        val logsArray = root.getJSONArray("logs")
        return decodeLogs(logsArray.toString())
    }

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
            result[date] = DailyLog(
                date = date,
                flow = runCatching { FlowIntensity.valueOf(item.optString("flow", "NONE")) }.getOrDefault(FlowIntensity.NONE),
                symptoms = symptoms,
                mood = item.optString("mood", ""),
                pain = item.optInt("pain", 0).coerceIn(0, 10),
                temperatureC = if (item.has("temperatureC")) item.optDouble("temperatureC") else null,
                notes = item.optString("notes", "")
            )
        }
        return result
    }

    companion object {
        private const val KEY_LOGS = "logs_json"
    }
}
