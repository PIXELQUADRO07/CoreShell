package com.example.coreshell.utils

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan

object AnsiColorParser {
    private val ANSI_REGEX = Regex("\u001B\\[([0-9;]*)m")

    fun parseToSpannable(text: String): Spannable {
        val spannable = SpannableStringBuilder()
        var currentColor = Color.WHITE
        var lastEnd = 0

        ANSI_REGEX.findAll(text).forEach { match ->
            val before = text.substring(lastEnd, match.range.first)
            if (before.isNotEmpty()) {
                val start = spannable.length
                spannable.append(before)
                spannable.setSpan(
                    ForegroundColorSpan(currentColor),
                    start, spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val codes = match.groupValues[1].split(";")
            currentColor = when (codes.firstOrNull()) {
                "31" -> Color.parseColor("#F85149") // rosso
                "32" -> Color.parseColor("#3FB950") // verde
                "33" -> Color.parseColor("#D29922") // giallo
                "34" -> Color.parseColor("#58A6FF") // blu
                "35" -> Color.parseColor("#BC8CFF") // magenta
                "36" -> Color.parseColor("#39C5CF") // cyan
                "0", "" -> Color.parseColor("#E6EDF3") // reset
                else -> currentColor
            }
            lastEnd = match.range.last + 1
        }

        if (lastEnd < text.length) {
            val start = spannable.length
            spannable.append(text.substring(lastEnd))
            spannable.setSpan(
                ForegroundColorSpan(currentColor),
                start, spannable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }
}
