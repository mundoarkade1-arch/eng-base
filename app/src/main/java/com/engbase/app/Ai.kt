package com.engbase.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    /** Chamada bloqueante: use sempre fora da thread principal. */
    fun ask(endpoint: String, token: String, system: String, history: List<ChatMsg>): String {
        if (!endpoint.startsWith("https://")) {
            throw IllegalStateException("O endereço da IA precisa começar com https://")
        }
        val msgs = JSONArray()
        history.takeLast(20).dropWhile { it.role != "user" }.forEach {
            msgs.put(JSONObject().put("role", it.role).put("content", it.text))
        }
        val body = JSONObject().put("system", system).put("messages", msgs).toString()

        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 90000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (token.isNotBlank()) conn.setRequestProperty("x-app-token", token)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val json = try { JSONObject(raw) } catch (e: Exception) { null }

            if (code !in 200..299) {
                val msg = json?.optString("error").orEmpty()
                throw IllegalStateException(if (msg.isNotBlank()) msg else "Erro $code ao falar com a IA")
            }
            val text = json?.optString("text").orEmpty()
            if (text.isBlank()) throw IllegalStateException("A IA não respondeu")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
