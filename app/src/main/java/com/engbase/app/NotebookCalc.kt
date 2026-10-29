package com.engbase.app

import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

fun fmt(d: Double): String {
    val t = String.format(Locale.US, "%.4f", d).trimEnd('0').trimEnd('.')
    val r = if (t == "-0" || t.isEmpty()) "0" else t
    return r.replace('.', ',')
}

class CalcResult(val steps: List<String>, val value: Double)

/** Calculadora que mostra cada conta como no caderno. */
object NotebookCalc {
    fun calc(input: String): CalcResult {
        val clean = input
            .replace(" ", "")
            .replace(',', '.')
            .replace('×', '*')
            .replace('x', '*')
            .replace('X', '*')
            .replace('÷', '/')
            .replace(":", "/")
            .lowercase()
        if (clean.isEmpty()) throw IllegalArgumentException("Escreva uma conta")
        val p = Parser(clean)
        val v = p.parse()
        return CalcResult(p.steps, v)
    }

    private class Parser(val s: String) {
        var pos = 0
        val steps = mutableListOf<String>()

        fun parse(): Double {
            val v = expr()
            if (pos < s.length) throw IllegalArgumentException("Não entendi o símbolo: ${s[pos]}")
            return v
        }

        private fun record(a: Double, op: String, b: Double, r: Double) {
            steps.add("${fmt(a)} $op ${fmt(b)} = ${fmt(r)}")
        }

        private fun expr(): Double {
            var v = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val r = term()
                val res = if (op == '+') v + r else v - r
                record(v, op.toString(), r, res)
                v = res
            }
            return v
        }

        private fun term(): Double {
            var v = unary()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                val op = s[pos++]
                val r = unary()
                if (op == '/' && r == 0.0) throw IllegalArgumentException("Não dá para dividir por zero")
                val res = if (op == '*') v * r else v / r
                record(v, if (op == '*') "×" else "÷", r, res)
                v = res
            }
            return v
        }

        private fun unary(): Double {
            if (pos < s.length && s[pos] == '-') {
                pos++
                return -unary()
            }
            if (pos < s.length && s[pos] == '+') {
                pos++
                return unary()
            }
            return power()
        }

        private fun power(): Double {
            val base = atom()
            if (pos < s.length && s[pos] == '^') {
                pos++
                val e = unary()
                val res = base.pow(e)
                if (res.isNaN() || res.isInfinite()) throw IllegalArgumentException("Essa potência não dá um número válido")
                record(base, "^", e, res)
                return res
            }
            return base
        }

        private fun atom(): Double {
            if (pos >= s.length) throw IllegalArgumentException("A conta está incompleta")
            if (s[pos] == '(') {
                pos++
                val v = expr()
                if (pos >= s.length || s[pos] != ')') throw IllegalArgumentException("Falta fechar o parêntese")
                pos++
                return v
            }
            if (s.startsWith("sqrt(", pos)) {
                pos += 5
                val v = expr()
                if (pos >= s.length || s[pos] != ')') throw IllegalArgumentException("Falta fechar o parêntese")
                pos++
                if (v < 0) throw IllegalArgumentException("Não existe raiz de número negativo")
                val r = sqrt(v)
                steps.add("√${fmt(v)} = ${fmt(r)}")
                return r
            }
            if (s.startsWith("pi", pos)) {
                pos += 2
                return Math.PI
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("Não entendi o símbolo: ${s[pos]}")
            return s.substring(start, pos).toDoubleOrNull()
                ?: throw IllegalArgumentException("Número inválido: ${s.substring(start, pos)}")
        }
    }
}

data class Formula(
    val name: String,
    val formulaText: String,
    val vars: List<Pair<String, String>>,
    val expr: String,
    val unit: String
)

object Formulas {
    val all = listOf(
        Formula("Área do retângulo", "A = b × h",
            listOf("b" to "base (m)", "h" to "altura (m)"), "{b}*{h}", "m²"),
        Formula("Volume de concreto", "V = c × l × e",
            listOf("c" to "comprimento (m)", "l" to "largura (m)", "e" to "espessura (m)"),
            "{c}*{l}*{e}", "m³"),
        Formula("Peso do concreto", "P = V × 25",
            listOf("V" to "volume (m³)"), "{V}*25", "kN"),
        Formula("Tensão normal", "σ = F ÷ A",
            listOf("F" to "força (kN)", "A" to "área (cm²)"), "{F}/{A}", "kN/cm²"),
        Formula("Reação de apoio (viga biapoiada, carga distribuída)", "R = q × L ÷ 2",
            listOf("q" to "carga (kN/m)", "L" to "vão (m)"), "{q}*{L}/2", "kN"),
        Formula("Momento máximo (viga biapoiada, carga distribuída)", "M = q × L² ÷ 8",
            listOf("q" to "carga (kN/m)", "L" to "vão (m)"), "{q}*{L}^2/8", "kN·m")
    )
}

private val numberRegex = Regex("^-?[0-9]+([.,][0-9]+)?$")

/** Monta as linhas do caderno para uma conta livre. */
fun notebookFree(input: String): List<String> {
    val r = NotebookCalc.calc(input)
    val lines = mutableListOf<String>()
    lines.add("Conta: ${input.trim()}")
    lines.add("")
    r.steps.forEachIndexed { i, st -> lines.add("${i + 1}) $st") }
    if (r.steps.isEmpty()) lines.add("(não precisa de contas)")
    lines.add("")
    lines.add("Resposta: ${fmt(r.value)}")
    return lines
}

/** Monta as linhas do caderno para uma fórmula com dados. */
fun notebookFormula(f: Formula, values: Map<String, String>): List<String> {
    var substituted = f.expr
    f.vars.forEach { (name, label) ->
        val v = (values[name] ?: "").trim()
        if (!numberRegex.matches(v)) {
            throw IllegalArgumentException("Preencha $label com um número")
        }
        val shown = if (v.startsWith("-")) "($v)" else v
        substituted = substituted.replace("{$name}", shown)
    }
    val r = NotebookCalc.calc(substituted)
    val resultName = f.formulaText.substringBefore(" =")
    val pretty = substituted.replace("*", " × ").replace("/", " ÷ ")
    val lines = mutableListOf<String>()
    lines.add("Dados:")
    f.vars.forEach { (name, label) -> lines.add("  $name = ${values[name]?.trim()}   ($label)") }
    lines.add("")
    lines.add("Fórmula:")
    lines.add("  ${f.formulaText}")
    lines.add("")
    lines.add("Substituindo:")
    lines.add("  $resultName = $pretty")
    lines.add("")
    lines.add("Contas:")
    r.steps.forEachIndexed { i, st -> lines.add("  ${i + 1}) $st") }
    lines.add("")
    lines.add("Resposta: $resultName = ${fmt(r.value)} ${f.unit}")
    return lines
}
