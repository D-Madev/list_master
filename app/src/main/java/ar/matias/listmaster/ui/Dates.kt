package ar.matias.listmaster.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Spanish = Locale.forLanguageTag("es-AR")
private val LongDate = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Spanish)
private val ShortDate = DateTimeFormatter.ofPattern("EEE d MMM", Spanish)

fun LocalDate.formatLong(): String = format(LongDate).capitalizeSpanish()

fun LocalDate.formatShort(): String = format(ShortDate).capitalizeSpanish()

private fun String.capitalizeSpanish(): String =
    replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase(Spanish) else character.toString()
    }
