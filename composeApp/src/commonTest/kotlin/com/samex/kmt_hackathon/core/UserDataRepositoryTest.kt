package com.samex.kmt_hackathon.core

import kotlin.test.Test
import kotlin.test.assertEquals

class UserDataRepositoryTest {
    @Test
    fun persistsUserDataAsStructuredJson() {
        val store = FakeKeyValueStore()
        val repository = UserDataRepository(store)
        val data = UserData(
            places = listOf(SavedPlace("place", "Home", GeoPoint(52.0, 4.0))),
            commutes = listOf(
                SavedCommute(
                    id = "commute",
                    originPlaceId = "place",
                    stopId = "stop",
                    selections = listOf(CommuteLineSelection("line", "direction")),
                    schedule = AutoStartSchedule(
                        days = listOf(Weekday.Monday, Weekday.Tuesday),
                        startMinutes = 7 * 60,
                        endMinutes = 9 * 60,
                    ),
                ),
            ),
            activeSession = PersistedWatchSession(
                commuteId = "commute",
                startedAtMinutes = 7 * 60,
                silenced = false,
                skippedGroupIds = listOf("group-1"),
            ),
        )

        repository.save(data)

        assertEquals(data, repository.load())
    }
}

private class FakeKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }
}
