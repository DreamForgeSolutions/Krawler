package sample.app.utils

import kotlinx.datetime.Clock

fun formatNumber(value: Double): String {
    return "%.2f".format(value)
}

fun formatLargeNumber(value: Long): String {
    return when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
        value >= 1_000 -> "%.1fK".format(value / 1_000.0)
        else -> value.toString()
    }
}

fun formatPercentage(value: Double): String {
    return "%.1f".format(value * 100)
}

fun formatDuration(startTime: Long?): String {
    if (startTime == null) return "00:00"

    val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
    val seconds = (elapsed / 1000) % 60
    val minutes = (elapsed / 60000) % 60
    val hours = elapsed / 3600000

    return when {
        hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%02d:%02d".format(minutes, seconds)
    }
}

fun String.format(vararg args: Any?): String {
    var result = this
    var argIndex = 0

    // Handle %d (integers)
    result = result.replace(Regex("%0?(\\d*)d")) { matchResult ->
        val padding = matchResult.groupValues[1].toIntOrNull() ?: 0
        val value = (args.getOrNull(argIndex++) as? Number)?.toLong()?.toString() ?: ""
        if (padding > 0) value.padStart(padding, '0') else value
    }

    // Handle %f and %.nf (floating point)
    result = result.replace(Regex("%(\\.\\d+)?f")) { matchResult ->
        val precision = matchResult.groupValues[1].removePrefix(".").toIntOrNull() ?: 6
        val value = (args.getOrNull(argIndex++) as? Number)?.toDouble() ?: 0.0
        formatDouble(value, precision)
    }

    // Handle %s (strings)
    result = result.replace(Regex("%s")) {
        args.getOrNull(argIndex++)?.toString() ?: ""
    }

    return result
}

private fun formatDouble(value: Double, precision: Int): String {
    val factor = pow10(precision)
    val rounded = kotlin.math.round(value * factor) / factor
    return if (precision == 0) {
        rounded.toInt().toString()
    } else {
        val intPart = rounded.toInt()
        val decimalPart = kotlin.math.abs((rounded - intPart) * factor).toInt()
        "$intPart.${decimalPart.toString().padStart(precision, '0')}"
    }
}

private fun pow10(exp: Int): Double {
    var result = 1.0
    repeat(exp) { result *= 10 }
    return result
}
