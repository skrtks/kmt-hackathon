package com.samex.kmt_hackathon.core

internal const val SECONDS_PER_MINUTE = 60
internal const val SECONDS_PER_DAY = MINUTES_PER_DAY * SECONDS_PER_MINUTE

internal fun minutesBetween(start: Int, end: Int): Int =
    if (end >= start) end - start else (MINUTES_PER_DAY - start) + end

internal fun secondsBetween(start: Int, end: Int): Int =
    if (end >= start) end - start else (SECONDS_PER_DAY - start) + end
