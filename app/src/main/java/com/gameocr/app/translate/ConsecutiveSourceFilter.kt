package com.gameocr.app.translate

/** Remembers only the last submitted source. Callers serialize access within their session. */
internal class ConsecutiveSourceFilter(private val ignoreOcrFormatting: Boolean = false) {
    private val whitespace = Regex("\\s+")
    private var previous: Pair<String, String>? = null

    fun accept(source: String, contextKey: String = ""): Boolean {
        val current = key(source, contextKey)
        if (current.second.isEmpty() || current == previous) return false
        previous = current
        return true
    }

    /** An older failed request must not forget a newer source. */
    fun forget(source: String, contextKey: String = "") {
        if (previous == key(source, contextKey)) previous = null
    }

    fun reset() { previous = null }

    private fun key(source: String, contextKey: String): Pair<String, String> {
        val text = if (ignoreOcrFormatting) {
            java.text.Normalizer.normalize(source, java.text.Normalizer.Form.NFKC)
                .lowercase(java.util.Locale.ROOT)
                .filter { it.isLetterOrDigit() }
        } else source.trim().replace(whitespace, " ")
        return contextKey to text
    }
}
