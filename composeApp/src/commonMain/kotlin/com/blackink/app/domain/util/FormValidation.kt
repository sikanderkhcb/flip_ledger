package com.blackink.app.domain.util

import com.blackink.app.core.AppError
import com.blackink.app.domain.model.AuthDraft
import com.blackink.app.domain.model.CostDraft
import com.blackink.app.domain.model.Device
import com.blackink.app.domain.model.DeviceDraft
import com.blackink.app.domain.model.SaleDraft
import com.blackink.app.domain.model.WorkspaceType

/**
 * Shared form rules. Screens use these rules for immediate field feedback and use cases apply
 * them again before writing data, so a caller cannot bypass validation by skipping a screen.
 */
object FormValidation {
    const val MAX_NAME_LENGTH = 80
    const val MAX_MODEL_LENGTH = 100
    const val MAX_STORAGE_LENGTH = 30
    const val MAX_NOTE_LENGTH = 200
    const val MAX_EMAIL_LENGTH = 254
    const val MAX_PHONE_LENGTH = 32
    const val MAX_ADDRESS_LENGTH = 240
    const val MAX_MONEY_CENTS = 99_999_999_999L // $999,999,999.99

    private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]{2,}$""")
    private val phoneCharacters = Regex("""^[+()\d.\-\s]+$""")
    private val categoryPreferences = setOf("phones", "laptops", "tablets", "gaming", "mixed")

    fun auth(draft: AuthDraft, isSignUp: Boolean): Map<String, String> = buildMap {
        if (isSignUp) {
            requiredText(
                field = "name",
                value = draft.name,
                label = "Full name",
                maxLength = MAX_NAME_LENGTH,
                minLength = 2,
            )?.let { put("name", it) }
        }

        val email = draft.email.trim()
        when {
            email.isEmpty() -> put("email", "Email is required.")
            email.length > 254 -> put("email", "Email must be 254 characters or fewer.")
            !emailPattern.matches(email) -> put("email", "Enter a valid email address.")
        }

        when {
            draft.password.isEmpty() -> put("password", "Password is required.")
            isSignUp && draft.password.length < 8 ->
                put("password", "Password must be at least 8 characters.")
            draft.password.length > 128 ->
                put("password", "Password must be 128 characters or fewer.")
            isSignUp && draft.password.none(Char::isLetter) ->
                put("password", "Password must include at least one letter.")
            isSignUp && draft.password.none(Char::isDigit) ->
                put("password", "Password must include at least one number.")
        }

        if (isSignUp && draft.phone.isNotBlank()) {
            val digits = draft.phone.count(Char::isDigit)
            when {
                !phoneCharacters.matches(draft.phone) ->
                    put("phone", "Use only numbers and phone symbols such as +, -, or parentheses.")
                digits !in 7..15 ->
                    put("phone", "Phone number must contain 7 to 15 digits.")
            }
        }

        if (isSignUp && draft.businessName.isNotBlank()) {
            requiredText(
                field = "businessName",
                value = draft.businessName,
                label = "Business name",
                maxLength = MAX_NAME_LENGTH,
                minLength = 2,
            )?.let { put("businessName", it) }
        }
    }

    fun setupBusinessName(businessName: String): Map<String, String> = buildMap {
        requiredText(
            field = "businessName",
            value = businessName,
            label = "Business name",
            maxLength = MAX_NAME_LENGTH,
            minLength = 2,
        )?.let { put("businessName", it) }
    }

    fun setupPreferences(
        workspaceType: WorkspaceType,
        partnerName: String,
        splitYou: Int,
        categoryPref: String,
    ): Map<String, String> = buildMap {
        if (workspaceType == WorkspaceType.PARTNER) {
            requiredText(
                field = "partnerName",
                value = partnerName,
                label = "Partner name",
                maxLength = MAX_NAME_LENGTH,
                minLength = 2,
            )?.let { put("partnerName", it) }
        }
        if (splitYou !in 0..100) {
            put("splitYou", "Profit split must be between 0% and 100%.")
        }
        if (categoryPref !in categoryPreferences) {
            put("categoryPref", "Choose a valid resale category.")
        }
    }

    fun deviceStep1(draft: DeviceDraft): Map<String, String> = buildMap {
        if (draft.category == null) put("category", "Choose a device category.")
        requiredText(
            field = "model",
            value = draft.model,
            label = "Model name",
            maxLength = MAX_MODEL_LENGTH,
            minLength = 2,
        )?.let { put("model", it) }
    }

    fun deviceStep2(draft: DeviceDraft): Map<String, String> = buildMap {
        moneyError(draft.price, "Purchase price", allowZero = false, required = true)
            ?.let { put("price", it) }
        dateError(draft.date, "Purchase date", futureAllowed = false)
            ?.let { put("date", it) }
        if (draft.source == null) put("source", "Choose where you acquired the device.")
    }

    fun deviceStep3(draft: DeviceDraft): Map<String, String> = buildMap {
        if (draft.identifierLast4.isNotBlank() &&
            (draft.identifierLast4.length != 4 || draft.identifierLast4.any { !it.isDigit() })
        ) {
            put("identifier", "Enter exactly the last 4 digits, or leave it blank.")
        }
        if (draft.storage.trim().length > MAX_STORAGE_LENGTH) {
            put("storage", "Storage must be $MAX_STORAGE_LENGTH characters or fewer.")
        } else if (draft.storage.any(Char::isISOControl)) {
            put("storage", "Storage contains unsupported characters.")
        }
    }

    fun device(draft: DeviceDraft): Map<String, String> =
        deviceStep1(draft) + deviceStep2(draft) + deviceStep3(draft)

    fun cost(draft: CostDraft, purchaseDate: String? = null): Map<String, String> = buildMap {
        if (draft.type == null) put("type", "Choose a cost type.")
        moneyError(draft.amount, "Amount", allowZero = false, required = true)
            ?.let { put("amount", it) }
        dateError(draft.date, "Cost date", futureAllowed = false)
            ?.let { put("date", it) }

        val enteredDate = Dates.parseIso(draft.date)
        val purchased = purchaseDate?.let(Dates::parseIso)
        if (enteredDate != null && purchased != null &&
            enteredDate.toEpochDays() < purchased.toEpochDays()
        ) {
            put("date", "Cost date cannot be before the device purchase date.")
        }
        if (draft.note.length > MAX_NOTE_LENGTH) {
            put("note", "Note must be $MAX_NOTE_LENGTH characters or fewer.")
        } else if (draft.note.any(Char::isISOControl)) {
            put("note", "Note contains unsupported characters.")
        }
    }

    fun sale(device: Device, draft: SaleDraft): Map<String, String> = buildMap {
        moneyError(draft.price, "Sale price", allowZero = false, required = true)
            ?.let { put("price", it) }
        dateError(draft.date, "Sale date", futureAllowed = false)
            ?.let { put("date", it) }

        val sold = Dates.parseIso(draft.date)
        val purchased = Dates.parseIso(device.purchaseDate)
        if (sold != null && purchased != null && sold.toEpochDays() < purchased.toEpochDays()) {
            put("date", "Sale date cannot be before the purchase date.")
        }
        if (draft.channel == null) put("channel", "Choose a sales channel.")
        if (draft.customerName.length > MAX_NAME_LENGTH) put("customerName", "Customer name is too long.")
        if (draft.customerEmail.isNotBlank() && !emailPattern.matches(draft.customerEmail.trim())) put("customerEmail", "Enter a valid email address.")
        if (draft.customerEmail.length > MAX_EMAIL_LENGTH) put("customerEmail", "Email is too long.")
        if (draft.customerPhone.length > MAX_PHONE_LENGTH) put("customerPhone", "Phone number is too long.")
        if (draft.customerPhone.isNotBlank() && !phoneCharacters.matches(draft.customerPhone.trim())) put("customerPhone", "Enter a valid phone number.")
        if (draft.customerAddress.length > MAX_ADDRESS_LENGTH) put("customerAddress", "Address is too long.")

        listOf(
            Triple("platformFee", "Platform fee", draft.platformFee),
            Triple("paymentFee", "Payment processing fee", draft.paymentFee),
            Triple("shipping", "Shipping", draft.shipping),
            Triple("packaging", "Packaging", draft.packaging),
            Triple("otherFee", "Other fee", draft.otherFee),
        ).forEach { (field, label, value) ->
            moneyError(value, label, allowZero = true, required = false)
                ?.let { put(field, it) }
        }
    }

    fun firstError(errors: Map<String, String>): AppError.Validation? =
        errors.entries.firstOrNull()?.let { (field, message) ->
            AppError.Validation(field, message)
        }

    private fun requiredText(
        field: String,
        value: String,
        label: String,
        maxLength: Int,
        minLength: Int,
    ): String? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> "$label is required."
            trimmed.length < minLength -> "$label must be at least $minLength characters."
            trimmed.length > maxLength -> "$label must be $maxLength characters or fewer."
            trimmed.any(Char::isISOControl) -> "$label contains unsupported characters."
            else -> null
        }
    }

    private fun moneyError(
        value: String,
        label: String,
        allowZero: Boolean,
        required: Boolean,
    ): String? {
        if (value.isBlank()) return if (required) "$label is required." else null
        val cents = Money.parseToCentsOrNull(value)
            ?: return "$label must be a valid amount with no more than two decimal places."
        return when {
            !allowZero && cents <= 0L -> "$label must be greater than zero."
            cents < 0L -> "$label cannot be negative."
            cents > MAX_MONEY_CENTS -> "$label must be less than $1,000,000,000."
            else -> null
        }
    }

    private fun dateError(value: String, label: String, futureAllowed: Boolean): String? {
        if (value.isBlank()) return "$label is required."
        val date = Dates.parseIso(value)
            ?: return "$label must use a valid YYYY-MM-DD date."
        if (!futureAllowed && date.toEpochDays() > Dates.today().toEpochDays()) {
            return "$label cannot be in the future."
        }
        return null
    }
}
