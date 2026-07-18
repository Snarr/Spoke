package com.jacobsnarr.spoke.ui.compass

import android.Manifest
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.data.prefs.UnitSystem
import com.jacobsnarr.spoke.ui.components.CenteredMessage
import com.jacobsnarr.spoke.ui.components.LoadingState
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.components.formatDistance
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.eInkTypography
import kotlin.math.abs
import kotlin.math.min

@Composable
fun CompassScreen(stationId: Int, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: CompassViewModel =
        viewModel(
            factory = CompassViewModel.provideFactory(container, stationId),
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unitSystem by container.preferencesStore.unitSystem.collectAsStateWithLifecycle()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
        }
    LaunchedEffect(Unit) {
        if (!viewModel.hasLocationPermission()) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = state.station?.name ?: "Compass",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }
            },
        )

        when {
            state.isLoading -> LoadingState()
            state.station == null -> CenteredMessage(text = state.error ?: "Station not found.")
            state.locationDenied ->
                CenteredMessage(text = "Location is needed to point you toward the station.")
            state.userLocation == null -> LoadingState()
            else -> {
                val station = state.station!!
                val user = state.userLocation!!
                // The rotation-vector sensor's world frame is magnetic-north referenced, so convert
                // the true-north geographic bearing to magnetic north by removing local declination.
                val declination =
                    remember(user) {
                        GeomagneticField(
                            user.latitude.toFloat(),
                            user.longitude.toFloat(),
                            0f,
                            System.currentTimeMillis(),
                        ).declination
                    }
                val trueBearing = state.bearingToStation ?: 0f
                val arrowAngle = rememberCompassArrow(trueBearing, declination)
                var calibrationOffset by remember { mutableFloatStateOf(0f) }
                val calibratedAngle = normalizeDegrees(arrowAngle - calibrationOffset)
                CompassContent(
                    arrowAngle = calibratedAngle,
                    stationName = station.name,
                    distanceMeters = state.distanceMeters,
                    unitSystem = unitSystem,
                    onRecalibrate = { calibrationOffset = arrowAngle },
                )
            }
        }
    }
}

@Composable
private fun CompassContent(
    arrowAngle: Float,
    stationName: String,
    distanceMeters: Double?,
    unitSystem: UnitSystem,
    onRecalibrate: () -> Unit,
) {
    val color = Color.Black
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = min(size.width, size.height) / 2f * 0.9f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = color,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3f),
                )
                rotate(degrees = arrowAngle, pivot = center) {
                    val tip = Offset(center.x, center.y - radius * 0.82f)
                    val tail = Offset(center.x, center.y + radius * 0.55f)
                    val halfWidth = radius * 0.22f
                    drawLine(
                        color = color,
                        start = tail,
                        end = tip,
                        strokeWidth = 6f,
                    )
                    val head =
                        Path().apply {
                            moveTo(tip.x, tip.y)
                            lineTo(tip.x - halfWidth, tip.y + halfWidth * 1.6f)
                            lineTo(tip.x + halfWidth, tip.y + halfWidth * 1.6f)
                            close()
                        }
                    drawPath(path = head, color = color)
                }
            }
        }
        distanceMeters?.let { meters ->
            TextMMD(
                text = formatDistance(meters, unitSystem),
                style = eInkTypography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
        TextMMD(
            text = "Pointing to $stationName",
            style = eInkTypography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        ButtonMMD(
            onClick = onRecalibrate,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            TextMMD("Recalibrate")
        }
    }
}

/**
 * Standard compass approach: extracts the device's magnetic heading using [SensorManager.getOrientation]
 * with coordinate remapping for the phone's current tilt, then computes the relative angle from
 * "screen up" to the target.
 *
 * - When the phone is mostly flat (screen up): uses default axes (heading from device-Y toward north).
 * - When the phone is mostly upright (screen facing user): remaps to AXIS_X/AXIS_Z so the heading
 *   is derived from the screen-normal direction.
 *
 * [trueBearingDegrees] is the geographic (true north) bearing to the target.
 * [declination] is the local magnetic declination (positive = magnetic north east of true north).
 */
@Composable
private fun rememberCompassArrow(trueBearingDegrees: Float, declination: Float): Float {
    val context = LocalContext.current
    val targetBearing = rememberUpdatedState(trueBearingDegrees)
    val decl = rememberUpdatedState(declination)
    var arrowAngle by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensorManager == null || rotationSensor == null) {
            return@DisposableEffect onDispose {}
        }

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    // Determine phone tilt: r[8] = how much device-Z aligns with world-Up.
                    // > 0.5 means phone is mostly flat; otherwise mostly upright.
                    val isFlat = abs(rotationMatrix[8]) > 0.5f

                    val success =
                        if (isFlat) {
                            // Phone flat: default mapping — Y is "forward" on screen.
                            SensorManager.remapCoordinateSystem(
                                rotationMatrix,
                                SensorManager.AXIS_X,
                                SensorManager.AXIS_Y,
                                remappedMatrix,
                            )
                        } else {
                            // Phone upright: Z (out of screen) acts as "forward."
                            SensorManager.remapCoordinateSystem(
                                rotationMatrix,
                                SensorManager.AXIS_X,
                                SensorManager.AXIS_Z,
                                remappedMatrix,
                            )
                        }
                    if (!success) return

                    SensorManager.getOrientation(remappedMatrix, orientation)
                    // orientation[0] = azimuth: rotation around Z, radians from magnetic north, positive clockwise.
                    val deviceMagHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()

                    // Convert device heading to true north, then compute relative angle to target.
                    val deviceTrueHeading = deviceMagHeading + decl.value
                    val degrees = normalizeDegrees(targetBearing.value - deviceTrueHeading)

                    if (angleDelta(degrees, arrowAngle) >= MIN_DEGREE_DELTA) {
                        arrowAngle = degrees
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    return arrowAngle
}

private fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

/** Smallest absolute difference between two headings, accounting for the 0/360 wrap-around. */
private fun angleDelta(a: Float, b: Float): Float {
    val diff = abs(a - b) % 360f
    return if (diff > 180f) 360f - diff else diff
}

private const val MIN_DEGREE_DELTA = 2f
