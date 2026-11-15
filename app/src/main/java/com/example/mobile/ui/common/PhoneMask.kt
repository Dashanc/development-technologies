package com.example.mobile.ui.common

internal class PhoneMask(val template: String) {
    private val slots = template.indices.filter { template[it] == '_' }
    val prefix: String = template.take(slots.first())

    fun format(value: String): String {
        if (value.isEmpty()) return ""
        if (value.any { it !in "0123456789+()- " }) return value
        if (value.count { it == '+' } > 1 || ('+' in value && !value.startsWith("+7"))) return value
        val digits = value.filter { it in '0'..'9' }
        val national = when {
            value.startsWith("+7") -> digits.drop(1)
            digits.length == 11 && digits.first() in "78" -> digits.drop(1)
            else -> digits
        }
        if (national.length > slots.size) return value
        if (national.isEmpty()) return if (value.startsWith("+7")) prefix else value
        val formatted = template.toCharArray()
        national.forEachIndexed { index, digit -> formatted[slots[index]] = digit }
        val end = slots.getOrNull(national.length) ?: template.length
        return String(formatted, 0, end)
    }

    fun remaining(value: String): String {
        if (value.isEmpty() || value.length >= template.length) return ""
        if (!value.indices.all { template[it] == value[it] || template[it] == '_' && value[it] in '0'..'9' }) return ""
        return template.drop(value.length)
    }
}