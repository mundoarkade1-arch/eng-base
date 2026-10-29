package com.engbase.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Subject(val id: String, val name: String, val level: Int)

data class StudyBlock(val subject: Subject, val minutes: Int)

data class ChatMsg(val role: String, val text: String)

fun levelName(level: Int): String = when (level) {
    1 -> "Fácil"
    3 -> "Difícil"
    else -> "Médio"
}

object Planner {
    /** Divide o tempo do dia entre as matérias. Matéria mais difícil ganha mais tempo. */
    fun divide(subjects: List<Subject>, totalMinutes: Int): List<StudyBlock> {
        if (subjects.isEmpty() || totalMinutes <= 0) return emptyList()
        val weights = subjects.map { it.level.coerceIn(1, 3) + 1 }
        val sum = weights.sum()
        return subjects.mapIndexed { i, s ->
            val raw = totalMinutes * weights[i] / sum
            val rounded = (raw / 5) * 5
            StudyBlock(s, if (rounded < 5) 5 else rounded)
        }
    }
}

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("engbase", Context.MODE_PRIVATE)

    fun loadSubjects(): List<Subject> {
        val raw = prefs.getString("subjects", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Subject(o.getString("id"), o.getString("name"), o.getInt("level"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSubjects(list: List<Subject>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("level", it.level))
        }
        prefs.edit().putString("subjects", arr.toString()).apply()
    }

    fun loadMinutesPerDay(): Int = prefs.getInt("minutes_per_day", 120)
    fun saveMinutesPerDay(v: Int) = prefs.edit().putInt("minutes_per_day", v).apply()

    fun loadDone(date: String): Set<String> =
        prefs.getStringSet("done_$date", emptySet())?.toSet() ?: emptySet()

    fun saveDone(date: String, ids: Set<String>) =
        prefs.edit().putStringSet("done_$date", ids).apply()

    fun loadStudied(date: String): Int = prefs.getInt("studied_$date", 0)
    fun saveStudied(date: String, minutes: Int) =
        prefs.edit().putInt("studied_$date", minutes).apply()

    fun loadEndpoint(): String = prefs.getString("endpoint", "") ?: ""
    fun saveEndpoint(v: String) = prefs.edit().putString("endpoint", v.trim()).apply()

    fun loadToken(): String = prefs.getString("token", "") ?: ""
    fun saveToken(v: String) = prefs.edit().putString("token", v.trim()).apply()
}
