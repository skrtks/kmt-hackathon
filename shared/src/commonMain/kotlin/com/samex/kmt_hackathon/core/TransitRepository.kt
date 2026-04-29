package com.samex.kmt_hackathon.core

import com.samex.kmt_hackathon.transit.Departure
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.transit.LineSelection
import com.samex.kmt_hackathon.transit.MockTransitData
import com.samex.kmt_hackathon.transit.TransitLine
import com.samex.kmt_hackathon.transit.TransitStop

interface TransitRepository {
    fun stops(): List<TransitStop>
    fun stopById(stopId: String): TransitStop?
    fun lineById(lineId: String): TransitLine?
    fun directionById(directionId: String): LineDirection?
    fun directionsServingStop(stopId: String): List<LineDirection>
    fun departuresFor(stopId: String, selections: List<CommuteLineSelection>, fromTimeMinutes: Int, limit: Int): List<Departure>
}

class MockTransitRepository : TransitRepository {
    override fun stops(): List<TransitStop> = MockTransitData.stops

    override fun stopById(stopId: String): TransitStop? = MockTransitData.stopById(stopId)

    override fun lineById(lineId: String): TransitLine? = MockTransitData.lineById(lineId)

    override fun directionById(directionId: String): LineDirection? = MockTransitData.directionById(directionId)

    override fun directionsServingStop(stopId: String): List<LineDirection> =
        MockTransitData.directionsServingStop(stopId)

    override fun departuresFor(
        stopId: String,
        selections: List<CommuteLineSelection>,
        fromTimeMinutes: Int,
        limit: Int,
    ): List<Departure> =
        MockTransitData.departuresFor(
            stopId = stopId,
            selections = selections.map { LineSelection(it.lineId, it.directionId) }.toSet(),
            fromTimeMinutes = fromTimeMinutes,
            limit = limit,
        )
}
