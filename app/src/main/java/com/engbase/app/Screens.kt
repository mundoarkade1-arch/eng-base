@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.engbase.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun hm(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (h == 0) "$m min" else if (m == 0) "${h}h" else "${h}h ${m}min"
}

// =====================================================================
// HOJE
// =====================================================================
@Composable
fun TodayScreen(vm: AppViewModel) {
    val blocks = vm.todayBlocks()
    val total = blocks.sumOf { it.minutes }
    val doneMin = blocks.filter { it.subject.id in vm.done }.sumOf { it.minutes }
    val next = blocks.firstOrNull { it.subject.id !in vm.done }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Hoje", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Um passo de cada vez.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tempo de estudo por dia: ${hm(vm.minutesPerDay)}", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = vm.minutesPerDay.toFloat(),
                        onValueChange = { vm.setMinutes((it.toInt() / 15) * 15) },
                        valueRange = 30f..480f
                    )
                    Text(
                        "Foco concluído hoje: ${hm(vm.studiedToday)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (blocks.isEmpty()) {
            item {
                Text("Cadastre as matérias na aba Matérias. Eu divido o tempo para você.")
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        if (next == null) {
                            Text("Tudo feito por hoje!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Descanse. Amanhã tem mais.")
                        } else {
                            Text("Agora faça só isto:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${next.subject.name} por ${hm(next.minutes)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Use a aba Foco para o cronômetro.")
                        }
                    }
                }
            }
            item {
                val progress = if (total == 0) 0f else doneMin.toFloat() / total
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("$doneMin de $total min do plano", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(blocks, key = { it.subject.id }) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = b.subject.id in vm.done,
                            onCheckedChange = { vm.toggleDone(b.subject.id) }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(b.subject.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${hm(b.minutes)} · ${levelName(b.subject.level)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// MATÉRIAS
// =====================================================================
private val suggestions = listOf(
    "Cálculo I", "Física I", "Estática", "Resistência dos Materiais",
    "Mecânica dos Solos", "Topografia", "Materiais de Construção",
    "Hidráulica", "Concreto Armado", "Desenho Técnico"
)

@Composable
fun SubjectsScreen(vm: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var level by remember { mutableIntStateOf(2) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Matérias do semestre", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Matéria difícil ganha mais tempo no plano.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da matéria") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { l ->
                    FilterChip(
                        selected = level == l,
                        onClick = { level = l },
                        label = { Text(levelName(l)) }
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    vm.addSubject(name, level)
                    name = ""
                },
                enabled = name.isNotBlank()
            ) { Text("Adicionar") }
        }
        item {
            Text("Toque para adicionar rápido:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { s ->
                    AssistChip(onClick = { vm.addSubject(s, level) }, label = { Text(s) })
                }
            }
        }
        items(vm.subjects, key = { it.id }) { s ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontWeight = FontWeight.SemiBold)
                        Text(levelName(s.level), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.removeSubject(s.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Apagar ${s.name}")
                    }
                }
            }
        }
    }
}

// =====================================================================
// TUTOR IA
// =====================================================================
@Composable
fun TutorScreen(vm: AppViewModel) {
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(vm.endpoint.isBlank()) }
    var url by remember { mutableStateOf(vm.endpoint) }
    var tk by remember { mutableStateOf(vm.token) }
    val listState = rememberLazyListState()

    LaunchedEffect(vm.messages.size, vm.loading) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(Modifier.fillMaxSize()) {
        if (showSettings) {
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ligar a IA", fontWeight = FontWeight.Bold)
                    Text("Cole o endereço do seu servidor da IA (começa com https://).")
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Endereço da IA") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tk,
                        onValueChange = { tk = it },
                        label = { Text("Código de acesso do app") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        vm.saveAiSettings(url, tk)
                        showSettings = vm.endpoint.isBlank()
                    }) { Text("Salvar") }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { vm.clearChat() }) { Text("Nova conversa") }
                TextButton(onClick = { showSettings = true }) { Text("Ajustes da IA") }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (vm.messages.isEmpty()) {
                item {
                    Text("Oi! Eu sou o tutor do Eng base.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Posso explicar a matéria, resolver cálculos no formato de caderno e fazer testes.")
                    Spacer(Modifier.size(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val next = vm.todayBlocks().firstOrNull { it.subject.id !in vm.done }?.subject?.name
                        AssistChip(
                            onClick = {
                                vm.send(
                                    if (next != null) "Explique a matéria $next do começo, bem simples, um passo de cada vez."
                                    else "Quais matérias da Engenharia Civil você recomenda estudar primeiro? Me pergunte as matérias do meu semestre."
                                )
                            },
                            label = { Text("Explicar a matéria de hoje") }
                        )
                        AssistChip(
                            onClick = { vm.send("Faça um teste de conhecimento comigo. Uma pergunta por vez.") },
                            label = { Text("Fazer um teste") }
                        )
                        AssistChip(
                            onClick = { vm.send("Me passe um exercício de cálculo e resolva no formato de caderno depois que eu responder.") },
                            label = { Text("Exercício de cálculo") }
                        )
                    }
                }
            }
            items(vm.messages) { m ->
                val mine = m.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(
                            m.text,
                            modifier = Modifier.padding(12.dp),
                            color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = if (mine) FontFamily.Default else FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            if (vm.loading) item { Text("Pensando...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            vm.error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Escreva aqui") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            IconButton(
                onClick = {
                    vm.send(input)
                    input = ""
                },
                enabled = input.isNotBlank() && !vm.loading && vm.endpoint.isNotBlank()
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar") }
        }
    }
}

// =====================================================================
// CADERNO (cálculos)
// =====================================================================
@Composable
fun NotebookPaper(lines: List<String>) {
    val lineH = 28.dp
    val density = LocalDensity.current
    val lineHpx = with(density) { lineH.toPx() }
    val marginPx = with(density) { 44.dp.toPx() }
    val lineSp = with(density) { lineH.toSp() }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .background(Color(0xFFFFFBEA))
            .drawBehind {
                var y = lineHpx
                while (y < size.height) {
                    drawLine(Color(0xFFB9D3EE), Offset(0f, y), Offset(size.width, y), 2f)
                    y += lineHpx
                }
                drawLine(Color(0xFFE57373), Offset(marginPx, 0f), Offset(marginPx, size.height), 2f)
            }
            .padding(start = 52.dp, end = 12.dp)
    ) {
        Column {
            lines.forEach {
                Text(
                    it.ifEmpty { " " },
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1B2A41),
                    fontSize = 15.sp,
                    lineHeight = lineSp
                )
            }
        }
    }
}

@Composable
fun NotebookScreen() {
    var mode by remember { mutableIntStateOf(0) }
    var expr by remember { mutableStateOf("") }
    var formulaIdx by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(mapOf<String, String>()) }
    var lines by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Caderno", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Cada conta aparece passo a passo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0; lines = emptyList(); error = null }, label = { Text("Conta livre") })
            FilterChip(selected = mode == 1, onClick = { mode = 1; lines = emptyList(); error = null }, label = { Text("Fórmulas") })
        }

        if (mode == 0) {
            OutlinedTextField(
                value = expr,
                onValueChange = { expr = it },
                label = { Text("Conta (use + - × ÷ ^ ( ) sqrt( ))") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                try {
                    lines = notebookFree(expr)
                    error = null
                } catch (e: Exception) {
                    error = e.message
                    lines = emptyList()
                }
            }, enabled = expr.isNotBlank()) { Text("Calcular") }
        } else {
            val f = Formulas.all[formulaIdx]
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Formulas.all.forEachIndexed { i, fm ->
                    FilterChip(
                        selected = formulaIdx == i,
                        onClick = { formulaIdx = i; values = emptyMap(); lines = emptyList(); error = null },
                        label = { Text(fm.name) }
                    )
                }
            }
            Text(f.formulaText, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
            f.vars.forEach { (name, label) ->
                OutlinedTextField(
                    value = values[name] ?: "",
                    onValueChange = { values = values + (name to it) },
                    label = { Text("$name — $label") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(onClick = {
                try {
                    lines = notebookFormula(f, values)
                    error = null
                } catch (e: Exception) {
                    error = e.message
                    lines = emptyList()
                }
            }) { Text("Calcular") }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (lines.isNotEmpty()) NotebookPaper(lines)
    }
}

// =====================================================================
// FOCO
// =====================================================================
@Composable
fun FocusScreen(vm: AppViewModel) {
    val mm = vm.secondsLeft / 60
    val ss = vm.secondsLeft % 60
    val next = vm.todayBlocks().firstOrNull { it.subject.id !in vm.done }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (vm.onBreak) "Pausa" else "Foco",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.size(8.dp))
        Text(
            String.format("%02d:%02d", mm, ss),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.size(8.dp))
        val total = if (vm.onBreak) 5 * 60 else 25 * 60
        LinearProgressIndicator(
            progress = { 1f - vm.secondsLeft.toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.size(16.dp))
        if (!vm.onBreak && next != null) {
            Text("Agora: ${next.subject.name}", fontWeight = FontWeight.SemiBold)
        }
        Text("Só uma coisa por vez. Celular longe.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vm.timerRunning) {
                Button(onClick = { vm.pauseTimer() }) { Text("Pausar") }
            } else {
                Button(onClick = { vm.startTimer() }) { Text("Começar") }
            }
            OutlinedButton(onClick = { vm.resetTimer() }) { Text("Zerar") }
        }
        Spacer(Modifier.size(16.dp))
        Text("Foco concluído hoje: ${hm(vm.studiedToday)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
