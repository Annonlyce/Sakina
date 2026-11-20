package com.example.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun Int.toArabicDigits(): String = toString().withArabicDigits()

fun Long.toArabicDigits(): String = toString().withArabicDigits()

fun String.withArabicDigits(): String {
    val sb = StringBuilder(length)
    for (ch in this) {
        if (ch in '0'..'9') {
            sb.append(ARABIC_DIGITS[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

fun normalizeArabicForSearch(input: String): String {
    if (input.isEmpty()) return ""
    val sb = StringBuilder(input.length)
    for (ch in input) {
        when (ch) {
            in '\u064B'..'\u065F',
            '\u0670',
            '\u0640',
            in '\u06D6'..'\u06ED' -> continue
            'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
            'ى' -> sb.append('ي')
            'ة' -> sb.append('ه')
            'ؤ' -> sb.append('و')
            'ئ' -> sb.append('ي')
            else -> sb.append(ch.lowercaseChar())
        }
    }
    return sb.toString().trim()
}

fun formatPlayerTimeArabic(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val formatted = if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
    return formatted.withArabicDigits()
}

fun formatListeningSummaryArabic(totalSeconds: Long): String {
    val sec = totalSeconds.coerceAtLeast(0L)
    if (sec < 60L) {
        return "$sec ث".withArabicDigits()
    }
    val totalMinutes = sec / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0 && minutes > 0 -> "$hours س $minutes د".withArabicDigits()
        hours > 0 -> "$hours س".withArabicDigits()
        else -> "$minutes د".withArabicDigits()
    }
}

fun formatMegabytesArabic(bytes: Long): String {
    val mb = bytes.coerceAtLeast(0L) / (1024.0 * 1024.0)
    val text = String.format(Locale.US, "%.1f", mb).replace('.', '٫')
    return "${text.withArabicDigits()} م.ب"
}

fun formatPercentArabic(percent: Int): String {
    return "${percent.coerceIn(0, 100).toArabicDigits()}٪"
}

private val arabicDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar"))

fun formatArabicDate(date: LocalDate): String {
    return date.format(arabicDateFormatter).withArabicDigits()
}

fun formatSurahFileName(surahNumber: Int): String {
    return String.format(Locale.US, "%03d.mp3", surahNumber)
}
