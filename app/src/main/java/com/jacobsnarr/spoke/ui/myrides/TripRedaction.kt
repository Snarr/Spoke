package com.jacobsnarr.spoke.ui.myrides

import com.jacobsnarr.spoke.data.remote.dto.TripDto
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

internal fun redactTrip(trip: TripDto): TripDto = dummyizeTrip(trip, stationNames = emptyList())

internal fun redactTrips(trips: List<TripDto>, stationNames: List<String> = emptyList()): List<TripDto> = trips.map {
    dummyizeTrip(it, stationNames)
}

internal fun buildDummyTrips(
    stationNames: List<String>,
    count: Int = DEFAULT_DUMMY_TRIP_COUNT,
    now: Instant = Instant.now(),
): List<TripDto> = (0 until count).map { index ->
    buildDummyTrip(
        tripId = 10_000L + index,
        stationNames = stationNames,
        index = index,
        now = now,
    )
}

internal fun buildDummyTrip(tripId: Long, stationNames: List<String>, index: Int = 0, now: Instant = Instant.now()): TripDto {
    val seeded = Random(tripId)
    val (from, to) = pickStationPair(stationNames, seeded)
    val minutes = 9 + ((tripId + index) % 38).toInt()
    val checkout = now.minus(Duration.ofHours(((index % 7) * 4 + 2).toLong())).minus(Duration.ofDays(index.toLong() / 2))
    val checkin = checkout.plus(Duration.ofMinutes(minutes.toLong()))
    val miles = 0.8 + (((tripId + index) % 60).toDouble() / 10.0)
    val cost = 1.75 + (((tripId + index) % 35).toDouble() / 10.0)
    val saved = cost + 1.5

    return TripDto(
        tripId = tripId,
        programName = "Spoke Demo",
        checkOutDate = checkout.toString(),
        checkOutLocation = from,
        checkOutLat = 0.0,
        checkOutLon = 0.0,
        checkInDate = checkin.toString(),
        checkInLocation = to,
        checkInLat = 0.0,
        checkInLon = 0.0,
        miles = miles,
        cost = cost,
        moneySaved = saved,
        duration = minutes,
        isDurationAdjusted = false,
    )
}

private fun dummyizeTrip(trip: TripDto, stationNames: List<String>): TripDto {
    val seeded = Random(trip.tripId)
    val (from, to) = pickStationPair(stationNames, seeded)
    val minutes = if (trip.duration > 0) trip.duration else 12 + (trip.tripId % 33).toInt()
    val checkoutInstant = parseTripInstantSafe(trip.checkOutDate) ?: Instant.now().minus(Duration.ofHours((trip.tripId % 72) + 1))
    val checkout = checkoutInstant.toString()
    val checkin = (parseTripInstantSafe(trip.checkInDate) ?: checkoutInstant.plus(Duration.ofMinutes(minutes.toLong()))).toString()
    val miles = if (trip.miles > 0) trip.miles else 1.2 + ((trip.tripId % 40).toDouble() / 10.0)
    val cost = if (trip.cost > 0) trip.cost else 1.95 + ((trip.tripId % 25).toDouble() / 10.0)
    val moneySaved = if (trip.moneySaved > 0) trip.moneySaved else cost + 1.25

    return trip.copy(
        programName = "Spoke Demo",
        checkOutDate = checkout,
        checkOutLocation = from,
        checkOutLat = 0.0,
        checkOutLon = 0.0,
        checkInDate = checkin,
        checkInLocation = to,
        checkInLat = 0.0,
        checkInLon = 0.0,
        miles = miles,
        cost = cost,
        moneySaved = moneySaved,
        duration = minutes,
    )
}

private fun parseTripInstantSafe(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
}

private fun pickStationPair(stationNames: List<String>, random: Random): Pair<String, String> {
    val pool = stationNames.filter { it.isNotBlank() }
    if (pool.isEmpty()) return DUMMY_CHECKOUT_LOCATION to DUMMY_CHECKIN_LOCATION
    if (pool.size == 1) return pool.first() to pool.first()

    val fromIndex = random.nextInt(pool.size)
    var toIndex = random.nextInt(pool.size)
    while (toIndex == fromIndex) {
        toIndex = random.nextInt(pool.size)
    }
    return pool[fromIndex] to pool[toIndex]
}

private const val DUMMY_CHECKOUT_LOCATION = "Origin Station"
private const val DUMMY_CHECKIN_LOCATION = "Destination Station"
private const val DEFAULT_DUMMY_TRIP_COUNT = 10
