package com.example.mobile.presenter.owner

import com.example.mobile.domain.model.Owner

data class OwnerInput(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
)

enum class OwnerFieldError { NAME_REQUIRED, EMAIL_REQUIRED, EMAIL_INVALID, PHONE_INVALID }

data class OwnerInputErrors(
    val name: OwnerFieldError? = null,
    val email: OwnerFieldError? = null,
    val phone: OwnerFieldError? = null,
) {
    val isEmpty: Boolean get() = name == null && email == null && phone == null

    fun clearChanged(previous: OwnerInput, current: OwnerInput) = copy(
        name = name.takeIf { previous.name == current.name },
        email = email.takeIf { previous.email == current.email },
        phone = phone.takeIf { previous.phone == current.phone },
    )
}

internal object OwnerFormValidator {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun validate(input: OwnerInput) = OwnerInputErrors(
        name = if (input.name.isBlank()) OwnerFieldError.NAME_REQUIRED else null,
        email = when {
            input.email.isBlank() -> OwnerFieldError.EMAIL_REQUIRED
            input.email.trim().length > 254 || !emailPattern.matches(input.email.trim()) -> OwnerFieldError.EMAIL_INVALID
            else -> null
        },
        phone = if (normalizePhone(input.phone) == null) OwnerFieldError.PHONE_INVALID else null,
    )

    fun owner(id: String, input: OwnerInput) = Owner(
        id = id,
        name = input.name.trim(),
        email = input.email.trim(),
        phone = requireNotNull(normalizePhone(input.phone)),
    )

    private fun normalizePhone(value: String): String? {
        if (value.any { it !in "0123456789+()- " }) return null
        val digits = value.filter { it in '0'..'9' }
        if (digits.isEmpty() && value.isBlank()) return ""
        if (value.trim() in listOf("+7", "+7 (")) return ""
        if (value.count { it == '+' } > 1 || ('+' in value && !value.trim().startsWith("+7"))) return null
        val national = when {
            digits.length == 11 && digits.first() in "78" -> digits.drop(1)
            digits.length == 10 && '+' !in value -> digits
            else -> return null
        }
        return "+7$national"
    }
}

internal fun Owner.toInput() = OwnerInput(name, email, phone)