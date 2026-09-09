package cdglacier.mytool.data.repository

import cdglacier.mytool.domain.model.QrEcLevel
import cdglacier.mytool.domain.model.QrEntry
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * pages/QR.md の Markdown シリアライザ/パーサ。
 * 1 テーブル形式: | Title | Content | ECLevel | Mode | Version | CreatedAt |
 */
object QrMarkdown {

    private val DT_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun serialize(entries: List<QrEntry>): String = buildString {
        appendLine("# QR")
        appendLine()
        appendLine("## Entries")
        appendLine("| Title | Content | ECLevel | Mode | Version | CreatedAt |")
        appendLine("|---|---|---|---|---|---|")
        val sorted = entries.sortedByDescending { it.createdAt }
        for (e in sorted) {
            appendLine(
                "| ${escape(e.title)} | ${escape(e.content)} | ${e.ecLevel.name} | ${escape(e.mode)} | ${e.version?.toString().orEmpty()} | ${e.createdAt.format(DT_FMT)} |"
            )
        }
    }

    fun parse(text: String): List<QrEntry> {
        val lines = text.lines()
        val entries = mutableListOf<QrEntry>()
        var inEntries = false
        var awaitingHeader = false
        var afterHeader = false

        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("## ")) {
                inEntries = line.removePrefix("## ").trim().equals("Entries", ignoreCase = true)
                awaitingHeader = inEntries
                afterHeader = false
                continue
            }
            if (!inEntries) continue
            if (!line.startsWith("|")) continue
            if (line.startsWith("|---")) {
                afterHeader = true
                continue
            }
            if (awaitingHeader) {
                awaitingHeader = false
                continue
            }
            if (!afterHeader) continue
            val cells = line.trim('|').split("|").map { unescape(it.trim()) }
            if (cells.size < 6) continue
            val title = cells[0]
            val content = cells[1]
            val ec = runCatching { QrEcLevel.valueOf(cells[2]) }.getOrNull() ?: QrEcLevel.M
            val mode = cells[3]
            val version = cells[4].toIntOrNull()
            val createdAt = runCatching { LocalDateTime.parse(cells[5], DT_FMT) }.getOrNull()
                ?: continue
            entries += QrEntry(title, content, ec, mode, version, createdAt)
        }
        return entries
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "")

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    '\\' -> sb.append('\\')
                    '|' -> sb.append('|')
                    'n' -> sb.append('\n')
                    else -> { sb.append(c); sb.append(n) }
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
