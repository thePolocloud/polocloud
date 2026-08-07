package de.polocloud.cli.command.arguments.type.int

import de.polocloud.cli.command.arguments.InputContext
import de.polocloud.cli.command.arguments.TerminalArgument
import de.polocloud.i18n.api.TranslationService

/**
 * A terminal argument that parses and validates integer input.
 *
 * Supports optional [minValue] and [maxValue] bounds as well as a [defaultValue]
 * that is used when the input is blank. Validation errors are reported via [wrongReason]
 * using i18n keys.
 *
 * Example:
 * ```kotlin
 * syntax({ ctx -> println(ctx.arg(portArg)) }, portArg)
 * val portArg = IntArgument("port", minValue = 1024, maxValue = 65535)
 * ```
 *
 * @param key A short identifier for this argument, shown in help output as `<key>`.
 * @param minValue Optional lower bound (inclusive). Validation fails if the value is below this.
 * @param maxValue Optional upper bound (inclusive). Validation fails if the value exceeds this.
 * @param defaultValue Fallback value used when the input is blank. If `null`, blank input fails validation.
 */
open class IntArgument(
    key: String,
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val defaultValue: Int? = null
) : TerminalArgument<Int>(key) {

    override fun predication(rawInput: String): Boolean {
        if (rawInput.isBlank() && defaultValue != null) return true
        return validate(rawInput) == null
    }

    override fun wrongReason(rawInput: String): String {
        return when (validate(rawInput)) {
            IntArgumentError.INVALID_INT -> TranslationService.tr("cli", "cli.argument.int.invalid", "value" to rawInput)
            IntArgumentError.LOWER_THAN_MIN -> TranslationService.tr("cli", "cli.argument.int.min", "key" to key, "min" to minValue)
            IntArgumentError.HIGHER_THAN_MAX -> TranslationService.tr("cli", "cli.argument.int.max", "key" to key, "max" to maxValue)
            null -> ""
        }
    }

    override fun defaultArgs(context: InputContext): MutableList<String> = mutableListOf()

    override fun buildResult(input: String, context: InputContext): Int {
        if (input.isBlank() && defaultValue != null) return defaultValue
        return input.toInt()
    }

    /**
     * Validates [rawInput] and returns the corresponding [IntArgumentError], or `null` if valid.
     *
     * @param rawInput The raw string token to validate.
     * @return The validation error, or `null` if the input is acceptable.
     */
    private fun validate(rawInput: String): IntArgumentError? {
        val value = rawInput.toIntOrNull() ?: return IntArgumentError.INVALID_INT
        if (maxValue != null && value > maxValue) return IntArgumentError.HIGHER_THAN_MAX
        if (minValue != null && value < minValue) return IntArgumentError.LOWER_THAN_MIN
        return null
    }
}