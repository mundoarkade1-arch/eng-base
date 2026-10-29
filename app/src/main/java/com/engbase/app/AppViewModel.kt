package com.engbase.app

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val today: String get() = LocalDate.now().toString()

    var subjects by mutableStateOf(store.loadSubjects())
        private set
    var minutesPerDay by mutableIntStateOf(store.loadMinutesPerDay())
        private set
    var done by mutableStateOf(store.loadDone(LocalDate.now().toString()))
        private set
    var studiedToday by mutableIntStateOf(store.loadStudied(LocalDate.now().toString()))
        private set
    var endpoint by mutableStateOf(store.loadEndpoint())
        private set
    var token by mutableStateOf(store.loadToken())
        private set

    val messages = mutableStateListOf<ChatMsg>()
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // ---------- Matérias e plano ----------
    fun addSubject(name: String, level: Int) {
        val n = name.trim()
        if (n.isEmpty()) return
        if (subjects.any { it.name.equals(n, ignoreCase = true) }) return
        subjects = subjects + Subject(UUID.randomUUID().toString(), n, level)
        store.saveSubjects(subjects)
    }

    fun removeSubject(id: String) {
        subjects = subjects.filter { it.id != id }
        store.saveSubjects(subjects)
    }

    fun setMinutes(v: Int) {
        minutesPerDay = v
        store.saveMinutesPerDay(v)
    }

    fun todayBlocks(): List<StudyBlock> = Planner.divide(subjects, minutesPerDay)

    fun toggleDone(id: String) {
        done = if (id in done) done - id else done + id
        store.saveDone(today, done)
    }

    fun addStudied(minutes: Int) {
        studiedToday += minutes
        store.saveStudied(today, studiedToday)
    }

    // ---------- Tutor IA ----------
    fun saveAiSettings(url: String, newToken: String) {
        endpoint = url.trim()
        token = newToken.trim()
        store.saveEndpoint(endpoint)
        store.saveToken(token)
        error = null
    }

    private fun systemPrompt(): String {
        val list = if (subjects.isEmpty()) {
            "O aluno ainda não cadastrou as matérias. Antes de ensinar, peça a lista de matérias do semestre."
        } else {
            "Matérias do semestre: " + subjects.joinToString("; ") { "${it.name} (dificuldade ${levelName(it.level).lowercase()})" } + "."
        }
        return """
Você é o tutor do app Eng base, para estudantes de Engenharia Civil.
Fale em português do Brasil, com frases curtas e linguagem simples.
O aluno pode ter TDAH e dificuldade em certos conteúdos. Por isso:
- Explique UMA ideia por vez e termine com uma pergunta curta ou um mini passo.
- Use exemplos reais de obra, estrutura, solo, concreto, água.
- Nunca escreva textos enormes. No máximo 12 linhas por resposta.
- Elogie o esforço de forma curta e sincera, sem exagero.
Cálculos: SEMPRE no formato de caderno, nesta ordem:
Dados:
Fórmula:
Substituindo:
Contas: (1), 2), 3) uma operação por linha)
Resposta: (com a unidade)
Testes: faça UMA pergunta por vez, espere a resposta, corrija com explicação curta e depois faça a próxima. Misture perguntas fáceis e difíceis.
Escreva em texto puro, sem markdown, sem asteriscos, sem tabelas. Use só letras, números e símbolos como × ÷ ² √.
Se não souber ou não tiver certeza, diga isso. Não invente normas ou valores.
$list
""".trimIndent()
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || loading) return
        messages.add(ChatMsg("user", t))
        loading = true
        error = null
        val history = messages.toList()
        val system = systemPrompt()
        val url = endpoint
        val tk = token
        viewModelScope.launch {
            try {
                val reply = withContext(Dispatchers.IO) { AiClient.ask(url, tk, system, history) }
                messages.add(ChatMsg("assistant", reply))
            } catch (e: Exception) {
                error = e.message ?: "Erro ao falar com a IA"
            }
            loading = false
        }
    }

    fun clearChat() {
        messages.clear()
        error = null
    }

    // ---------- Foco (25 min estudo / 5 min pausa) ----------
    var secondsLeft by mutableIntStateOf(25 * 60)
        private set
    var timerRunning by mutableStateOf(false)
        private set
    var onBreak by mutableStateOf(false)
        private set
    private var timerJob: Job? = null

    fun startTimer() {
        if (timerRunning) return
        timerRunning = true
        val endAt = SystemClock.elapsedRealtime() + secondsLeft * 1000L
        timerJob = viewModelScope.launch {
            while (timerRunning) {
                val left = ((endAt - SystemClock.elapsedRealtime() + 999) / 1000).toInt()
                if (left <= 0) {
                    if (!onBreak) addStudied(25)
                    onBreak = !onBreak
                    secondsLeft = if (onBreak) 5 * 60 else 25 * 60
                    timerRunning = false
                } else {
                    secondsLeft = left
                }
                delay(250)
            }
        }
    }

    fun pauseTimer() {
        timerRunning = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        onBreak = false
        secondsLeft = 25 * 60
    }
}
