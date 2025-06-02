package solutions.dreamforge.krawler.utils

import kotlin.math.pow
import kotlin.math.round

/**
 * Multiplatform string formatting utilities
 */
object StringFormat {
    /**
     * Format a string with printf-style formatting
     */
    fun format(format: String, vararg args: Any?): String {
        var result = format
        var argIndex = 0
        
        // Simple implementation for common cases
        val patterns = listOf(
            "%.1f" to { arg: Any? -> (arg as? Number)?.toDouble()?.let { "%.1f".format(it) } ?: arg.toString() },
            "%.2f" to { arg: Any? -> (arg as? Number)?.toDouble()?.let { "%.2f".format(it) } ?: arg.toString() },
            "%d" to { arg: Any? -> (arg as? Number)?.toLong()?.toString() ?: arg.toString() },
            "%s" to { arg: Any? -> arg.toString() }
        )
        
        for ((pattern, formatter) in patterns) {
            while (result.contains(pattern) && argIndex < args.size) {
                result = result.replaceFirst(pattern, formatter(args[argIndex]))
                argIndex++
            }
        }
        
        return result
    }
}

private fun String.format(value: Double): String {
    return when (this) {
        "%.1f" -> value.formatDecimals(1)
        "%.2f" -> value.formatDecimals(2)
        else -> value.toString()
    }
}

private fun Double.formatDecimals(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(this * factor) / factor
    return rounded.toString().let { str ->
        val parts = str.split(".")
        if (parts.size == 2) {
            val decimalPart = parts[1].padEnd(decimals, '0').take(decimals)
            "${parts[0]}.$decimalPart"
        } else {
            "$str.${"0".repeat(decimals)}"
        }
    }
}


fun String.Companion.format(format: String, vararg args: Any?): String = StringFormat.format(format, *args)
