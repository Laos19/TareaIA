package com.tareia.app.ui.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Conversión entre minutos del día y texto HH:mm.

internal fun formatMinute(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

internal fun parseMinute(value: String): Int? = runCatching {
    val time = LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("H:mm"))
    time.hour * 60 + time.minute
}.getOrNull()
