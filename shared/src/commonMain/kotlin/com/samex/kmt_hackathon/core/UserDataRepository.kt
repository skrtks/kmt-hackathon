package com.samex.kmt_hackathon.core

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserDataRepository(
    private val keyValueStore: KeyValueStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun load(): UserData {
        val raw = keyValueStore.getString(KEY) ?: return UserData()
        return try {
            json.decodeFromString<UserData>(raw)
        } catch (_: SerializationException) {
            UserData()
        } catch (_: IllegalArgumentException) {
            UserData()
        }
    }

    fun save(userData: UserData) {
        keyValueStore.putString(KEY, json.encodeToString(userData))
    }

    private companion object {
        const val KEY = "user_data_v1"
    }
}
