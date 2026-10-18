package com.privatednsguard.data.model

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

object DnsValidation {

    private val HOSTNAME_REGEX = Regex(
        "^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])" +
                "(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*$"
    )

    fun validateDoTHostname(input: String?): ValidationResult {
        if (input.isNullOrBlank()) {
            return ValidationResult.Invalid("Hostname cannot be empty.")
        }

        val trimmed = input.trim()

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return ValidationResult.Invalid("Do not include http:// or https://. Enter only the DNS hostname (e.g., dns.nextdns.io).")
        }

        if (trimmed.contains("/")) {
            return ValidationResult.Invalid("Hostname must not contain slashes ('/').")
        }

        if (trimmed.contains(":")) {
            return ValidationResult.Invalid("Do not specify port numbers (e.g., :853). Android automatically uses port 853 for Private DNS.")
        }

        if (trimmed.length > 253) {
            return ValidationResult.Invalid("Hostname is too long (maximum 253 characters).")
        }

        if (!trimmed.contains(".")) {
            return ValidationResult.Invalid("Hostname must contain at least one dot (e.g., example.com).")
        }

        if (!HOSTNAME_REGEX.matches(trimmed)) {
            return ValidationResult.Invalid("Invalid hostname format. Only alphanumeric characters, dots, and hyphens are allowed.")
        }

        return ValidationResult.Valid
    }
}
