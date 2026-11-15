package com.nooralislam.app.utils

import com.nooralislam.app.data.QuranRepository
import kotlin.math.max
import kotlin.math.roundToInt

data class TasmeeWordStatus(val displayWord: String, val matched: Boolean)

data class TasmeeMatchResult(
    val accuracyPercent: Int,
    val matchedWords: Int,
    val totalWords: Int,
    val wordStatuses: List<TasmeeWordStatus>,
    val missedWords: List<String>
)

object TasmeeNormalizer {
    private fun normalizeUthmaniToken(word: String): String {
        val w = word
            .replace("\u0635\u0644\u0648\u0670\u0629", "صلاة")
            .replace("\u0632\u0643\u0648\u0670\u0629", "زكاة")
            .replace("\u062d\u064a\u0648\u0670\u0629", "حياة")
        return QuranRepository.normalizeArabic(w)
            .replace(Regex("[^\\u0621-\\u064A0-9]"), "")
    }

    fun compare(targetText: String, spokenText: String): TasmeeMatchResult {
        val rawTarget = targetText.split(Regex("\\s+")).filter { normalizeUthmaniToken(it).isNotEmpty() }
        val normTarget = rawTarget.map { normalizeUthmaniToken(it) }
        val normSpoken = spokenText.split(Regex("\\s+")).map { normalizeUthmaniToken(it) }.filter { it.isNotEmpty() }

        val n = normTarget.size
        val m = normSpoken.size
        if (n == 0) return TasmeeMatchResult(0, 0, 0, emptyList(), emptyList())

        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 1..n) {
            for (j in 1..m) {
                dp[i][j] = if (normTarget[i - 1] == normSpoken[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    max(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val matched = BooleanArray(n)
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            if (normTarget[i - 1] == normSpoken[j - 1]) {
                matched[i - 1] = true
                i--; j--
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--
            } else {
                j--
            }
        }

        val statuses = rawTarget.mapIndexed { idx, token -> TasmeeWordStatus(token, matched[idx]) }
        val count = matched.count { it }
        val missed = rawTarget.filterIndexed { idx, _ -> !matched[idx] }
        val pct = ((count.toDouble() / n) * 100.0).roundToInt()
        return TasmeeMatchResult(pct, count, n, statuses, missed)
    }
}
