package com.jacobsnarr.spoke.data.system

import com.jacobsnarr.spoke.BuildConfig

/**
 * OAuth (ROPC) configuration for a bikeshare system. Absent when the system's login credentials
 * aren't known yet, in which case the system is browse-only and sign-in is hidden.
 */
data class AuthConfig(val acrValues: String, val clientId: String, val clientSecret: String, val scope: String)

/**
 * Checkout (bike unlock) configuration for a bikeshare system. Absent when the system's checkout
 * endpoint is not yet supported, in which case unlocking is disabled in the UI.
 */
data class CheckoutConfig(val checkoutUrl: String, val programId: Int, val systemId: String)

/**
 * Trips configuration for a bikeshare system. Absent when the system's `/user/trips` endpoint is
 * not yet known, in which case automatic ride-session tracking is disabled.
 */
data class TripsConfig(val tripsUrl: String)

/**
 * User summary and unpaid-fees configuration for a bikeshare system. Absent when the system's
 * endpoints are not yet known, in which case the My Rides stats and Unpaid Fees screens are hidden.
 */
data class SummaryConfig(val summaryUrl: String, val unpaidFeesUrl: String)

/**
 * A bikeshare system operated on the bcycle platform. Carries every piece of per-system
 * configuration so the rest of the app can stay system-agnostic. [auth] and [checkout] are null
 * for systems that only support read-only station browsing so far.
 */
data class BikeSystem(
    val id: String,
    val displayName: String,
    val stationPath: String,
    val auth: AuthConfig?,
    val checkout: CheckoutConfig?,
    val trips: TripsConfig? = null,
    val summary: SummaryConfig? = null,
)

/**
 * Registry of supported bikeshare systems.
 */
object BikeSystems {
    val PHL =
        BikeSystem(
            id = "phl",
            displayName = "Indego",
            stationPath = "phl",
            auth =
            AuthConfig(
                acrValues = "tenant:phl",
                clientId = "philadelphia_client_ropc",
                clientSecret = BuildConfig.INDEGO_SECRET,
                scope = "user:modify user:read offline_access trip:modify billing:modify",
            ),
            checkout =
            CheckoutConfig(
                checkoutUrl = "https://portal-phl.bcycle.com/integrator/1/checkout",
                programId = 3,
                systemId = "bcycle_indego",
            ),
            trips =
            TripsConfig(
                tripsUrl = "https://portal-phl.bcycle.com/1/user/trips",
            ),
            summary =
            SummaryConfig(
                summaryUrl = "https://portal-phl.bcycle.com/1/user/summary",
                unpaidFeesUrl = "https://api-phl.bicycletransit.com/user/unpaid-fees",
            ),
        )

    val LAX =
        BikeSystem(
            id = "lax",
            displayName = "Metro Bike Share",
            stationPath = "lax",
            auth =
            AuthConfig(
                acrValues = "tenant:lax systemId:bcycle_lametro",
                clientId = "los_angeles_client_ropc",
                clientSecret = BuildConfig.METRO_BIKE_SHARE_SECRET,
                scope =
                "user:modify user:read offline_access tap_card:read tap_card:modify " +
                    "trip:modify billing:modify mobile:read",
            ),
            checkout = null,
        )

    val LAS =
        BikeSystem(
            id = "las",
            displayName = "RTC Bike Share",
            stationPath = "las",
            auth = null,
            checkout = null,
        )

    val all: List<BikeSystem> = listOf(PHL, LAX, LAS)

    val default: BikeSystem = PHL

    fun fromId(id: String?): BikeSystem = all.firstOrNull { it.id == id } ?: default
}
