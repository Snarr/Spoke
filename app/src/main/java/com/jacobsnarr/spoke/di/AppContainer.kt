package com.jacobsnarr.spoke.di

import android.content.Context
import com.jacobsnarr.spoke.BuildConfig
import com.jacobsnarr.spoke.data.auth.AuthRepository
import com.jacobsnarr.spoke.data.auth.TokenStore
import com.jacobsnarr.spoke.data.debug.DebugSettingsRepository
import com.jacobsnarr.spoke.data.network.ConnectivityRepository
import com.jacobsnarr.spoke.data.prefs.PreferencesStore
import com.jacobsnarr.spoke.data.remote.CheckoutApi
import com.jacobsnarr.spoke.data.remote.IndegoAuthApi
import com.jacobsnarr.spoke.data.remote.StationStatusApi
import com.jacobsnarr.spoke.data.remote.SummaryApi
import com.jacobsnarr.spoke.data.remote.TripsApi
import com.jacobsnarr.spoke.data.remote.UnpaidFeesApi
import com.jacobsnarr.spoke.data.ride.MyRidesRepository
import com.jacobsnarr.spoke.data.ride.RideRepository
import com.jacobsnarr.spoke.data.ride.TripRepository
import com.jacobsnarr.spoke.data.station.StationRepository
import com.jacobsnarr.spoke.data.system.SystemRepository
import com.jacobsnarr.spoke.location.LocationProvider
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Minimal hand-rolled DI container (service locator). Chosen over Hilt because Hilt's Gradle
 * plugin is not yet compatible with AGP 9. Holds long-lived singletons for the app.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    private val okHttpClient: OkHttpClient by lazy {
        val logging =
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
        OkHttpClient
            .Builder()
            .addInterceptor(logging)
            .build()
    }

    private val authApi: IndegoAuthApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(IndegoAuthApi::class.java)
    }

    private val stationApi: StationStatusApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(STATION_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(StationStatusApi::class.java)
    }

    private val checkoutApi: CheckoutApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(CHECKOUT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(CheckoutApi::class.java)
    }

    private val tripsApi: TripsApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(CHECKOUT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(TripsApi::class.java)
    }

    private val summaryApi: SummaryApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(CHECKOUT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(SummaryApi::class.java)
    }

    private val unpaidFeesApi: UnpaidFeesApi by lazy {
        Retrofit
            .Builder()
            .baseUrl(CHECKOUT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(UnpaidFeesApi::class.java)
    }

    private val tokenStore: TokenStore by lazy { TokenStore(appContext) }
    val preferencesStore: PreferencesStore by lazy { PreferencesStore(appContext) }
    val debugSettingsRepository: DebugSettingsRepository by lazy { DebugSettingsRepository(appContext) }

    val systemRepository: SystemRepository by lazy { SystemRepository(preferencesStore) }

    val stationRepository: StationRepository by lazy {
        StationRepository(stationApi, systemRepository)
    }
    val tripRepository: TripRepository by lazy { TripRepository(tripsApi, systemRepository) }
    val rideRepository: RideRepository by lazy {
        RideRepository(checkoutApi, systemRepository, tripRepository, connectivityRepository)
    }
    val authRepository: AuthRepository by lazy {
        AuthRepository(authApi, tokenStore, systemRepository, stationRepository, rideRepository)
    }
    val connectivityRepository: ConnectivityRepository by lazy { ConnectivityRepository(appContext) }
    val locationProvider: LocationProvider by lazy {
        LocationProvider(appContext).also { provider ->
            provider.setDebugLocationOverride(debugSettingsRepository.debugLocationOverride.value)
        }
    }
    val myRidesRepository: MyRidesRepository by lazy {
        MyRidesRepository(summaryApi, tripsApi, unpaidFeesApi, authRepository, systemRepository)
    }

    private companion object {
        const val AUTH_BASE_URL = "https://id.bcycle.com/"
        const val STATION_BASE_URL = "https://bts-status.bicycletransit.workers.dev/"
        const val CHECKOUT_BASE_URL = "https://portal-phl.bcycle.com/"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
