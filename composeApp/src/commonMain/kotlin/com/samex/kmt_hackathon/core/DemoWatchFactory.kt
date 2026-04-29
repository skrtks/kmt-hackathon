package com.samex.kmt_hackathon.core

internal data class DemoTiming(
    val windowOpenMinutes: Int,
    val finalCallMinutes: Int,
    val departureTimeMinutes: Int,
)

internal fun demoTimingFor(scenario: DemoWatchScenario, nowMinutes: Int): DemoTiming =
    when (scenario) {
        DemoWatchScenario.GetReady -> DemoTiming(
            windowOpenMinutes = nowMinutes + 1,
            finalCallMinutes = nowMinutes + 2,
            departureTimeMinutes = nowMinutes + 3,
        )
        DemoWatchScenario.LeaveNow -> DemoTiming(
            windowOpenMinutes = nowMinutes - 1,
            finalCallMinutes = nowMinutes + 1,
            departureTimeMinutes = nowMinutes + 2,
        )
        DemoWatchScenario.FinalCall -> DemoTiming(
            windowOpenMinutes = nowMinutes - 1,
            finalCallMinutes = nowMinutes,
            departureTimeMinutes = nowMinutes + 1,
        )
    }
