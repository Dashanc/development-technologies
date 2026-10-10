package com.example.mobile

data class SentenceAnalysis(
    val sentences: List<String>,
    val wordCount: Int
)

object SentenceAnalyzer {
    private val sentenceSeparator = Regex("(?<=[.!?…])\\s+")
    private val wordPattern = Regex("\\p{L}+(?:[-’']\\p{L}+)*")

    fun findLongest(text: String): SentenceAnalysis? {
        val sentences = text.trim()
            .split(sentenceSeparator)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (sentences.isEmpty()) return null

        val counted = sentences.map { sentence ->
            sentence to wordPattern.findAll(sentence).count()
        }.filter { (_, count) -> count > 0 }

        if (counted.isEmpty()) return null

        val maxWords = counted.maxOf { it.second }
        return SentenceAnalysis(
            sentences = counted.filter { it.second == maxWords }.map { it.first },
            wordCount = maxWords
        )
    }
}
