package com.herbify.app.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.view.Gravity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.herbify.app.BuildConfig
import com.herbify.app.game.GameState
import com.herbify.app.model.OsmZoneRepository
import com.herbify.app.model.Zone
import com.herbify.app.model.ZoneDefaults
import com.herbify.app.model.ZoneType
import com.herbify.app.model.toGeoJsonFeatureCollection
import com.herbify.app.ui.theme.DarkBg
import com.herbify.app.ui.theme.DarkCard
import com.herbify.app.ui.theme.DarkSurface
import com.herbify.app.ui.theme.NeonGreen
import com.herbify.app.ui.theme.TextPrimary
import com.herbify.app.ui.theme.TextSecondary
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val MAPTILER_KEY = BuildConfig.MAPTILER_KEY
private val STYLE_URL get() = "https://api.maptiler.com/maps/dataviz-dark/style.json?key=$MAPTILER_KEY"

private const val ZONES_SOURCE_ID = "herbify-zones"
private const val ZONES_FILL_ID = "herbify-zones-fill"
private const val ZONES_LINE_ID = "herbify-zones-line"
private const val PLAYER_SOURCE_ID = "herbify-player"
private const val PLAYER_HALO_ID = "herbify-player-halo"
private const val PLAYER_DOT_ID = "herbify-player-dot"
private const val BUILDINGS_3D_ID = "herbify-buildings-3d"

private const val SEARCH_RADIUS_METERS = 850
private const val MIN_ZOOM = 14.2
private const val MAX_ZOOM = 17.8
private const val BUILDING_HEIGHT_MULTIPLIER = 1.12
private const val CAMERA_MOVE_THRESHOLD_METERS = 6f
private const val BIOME_RELOAD_DISTANCE_METERS = 120f
private const val BIOME_RELOAD_TIME_MS = 45000L

@Composable
fun MapScreen(gameState: GameState, onNavigateToScanner: (Zone) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val zones = remember { mutableStateListOf<Zone>() }
    var selectedZone by remember { mutableStateOf<Zone?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var playerLat by remember { mutableDoubleStateOf(ZoneDefaults.BASE_LAT) }
    var playerLng by remember { mutableDoubleStateOf(ZoneDefaults.BASE_LNG) }
    var hasRealLocation by remember { mutableStateOf(false) }

    var lastCameraLat by remember { mutableStateOf<Double?>(null) }
    var lastCameraLng by remember { mutableStateOf<Double?>(null) }

    var lastBiomeFetchLat by remember { mutableStateOf<Double?>(null) }
    var lastBiomeFetchLng by remember { mutableStateOf<Double?>(null) }
    var lastBiomeFetchTime by remember { mutableLongStateOf(0L) }
    var lastBiomeAttemptTime by remember { mutableLongStateOf(0L) }
    var biomeLoadError by remember { mutableStateOf(false) }

    val eco by gameState.eco.collectAsState()
    val level by gameState.level.collectAsState()
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    TrackPlayerLocation(
        context = context,
        enabled = hasLocationPermission,
        onLocationChanged = { lat, lng ->
            playerLat = lat
            playerLng = lng
            hasRealLocation = true
        }
    )

    LaunchedEffect(playerLat, playerLng, hasRealLocation, selectedZone) {
        if (!hasRealLocation) return@LaunchedEffect

        val map = mapRef.value
        val style = map?.style

        if (style != null) {
            ensureZoneLayers(style, zones, selectedZone)
            ensurePlayerLayers(style, playerLat, playerLng)
            updatePlayerLocation(style, playerLat, playerLng)
            updateZoneSelection(style, zones, selectedZone)
        }

        if (map != null) {
            val shouldMoveCamera = if (lastCameraLat == null || lastCameraLng == null) {
                true
            } else {
                distanceMeters(lastCameraLat!!, lastCameraLng!!, playerLat, playerLng) >= CAMERA_MOVE_THRESHOLD_METERS
            }

            if (shouldMoveCamera && selectedZone == null) {
                centerMapOnPlayer(map, playerLat, playerLng)
                lastCameraLat = playerLat
                lastCameraLng = playerLng
            }
        }
    }

    LaunchedEffect(playerLat, playerLng, hasRealLocation) {
        if (!hasRealLocation) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val neverSucceeded = lastBiomeFetchTime == 0L
        val neverAttempted = lastBiomeAttemptTime == 0L

        val movedEnough = if (lastBiomeFetchLat != null && lastBiomeFetchLng != null) {
            distanceMeters(lastBiomeFetchLat!!, lastBiomeFetchLng!!, playerLat, playerLng) >= BIOME_RELOAD_DISTANCE_METERS
        } else {
            false
        }

        val staleAttempt = neverAttempted || now - lastBiomeAttemptTime >= BIOME_RELOAD_TIME_MS
        val shouldReload = neverSucceeded || movedEnough || staleAttempt

        if (!shouldReload) {
            return@LaunchedEffect
        }

        if (neverSucceeded) {
            isLoading = true
        } else {
            isRefreshing = true
        }

        lastBiomeAttemptTime = now

        try {
            val loadedZones = OsmZoneRepository.loadZonesAround(
                lat = playerLat,
                lng = playerLng,
                radiusMeters = SEARCH_RADIUS_METERS
            )

            if (loadedZones.isNotEmpty()) {
                zones.clear()
                zones.addAll(loadedZones)

                lastBiomeFetchLat = playerLat
                lastBiomeFetchLng = playerLng
                lastBiomeFetchTime = now
                biomeLoadError = false
            } else if (zones.isEmpty()) {
                biomeLoadError = true
            }

            val map = mapRef.value
            val style = map?.style
            if (style != null) {
                ensureZoneLayers(style, zones, selectedZone)
                ensurePlayerLayers(style, playerLat, playerLng)
                updateZoneSelection(style, zones, selectedZone)
                updatePlayerLocation(style, playerLat, playerLng)
            }
        } catch (_: Exception) {
            biomeLoadError = true
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(selectedZone, zones.size) {
        val map = mapRef.value ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        ensureZoneLayers(style, zones, selectedZone)
        updateZoneSelection(style, zones, selectedZone)
        selectedZone?.let { flyToZone(map, it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HerbifyMapView(
            modifier = Modifier.fillMaxSize(),
            context = context,
            lifecycleOwner = lifecycleOwner,
            zones = zones,
            selectedZone = selectedZone,
            playerLat = playerLat,
            playerLng = playerLng,
            onMapReady = { map ->
                mapRef.value = map
            },
            onZoneTapped = { zone ->
                selectedZone = if (selectedZone?.id == zone?.id) null else zone
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudChip(label = "LVL $level", emoji = "⚔️")
            HudChip(label = "$eco ECO", emoji = "💚")
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 88.dp)
                .size(54.dp)
                .background(DarkCard.copy(alpha = 0.94f), CircleShape)
                .border(1.dp, NeonGreen.copy(alpha = 0.35f), CircleShape)
                .clickable {
                    mapRef.value?.let { map ->
                        centerMapOnPlayer(map, playerLat, playerLng)
                        lastCameraLat = playerLat
                        lastCameraLng = playerLng
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "◎",
                color = NeonGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(
            visible = selectedZone != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedZone?.let { zone ->
                ZoneDetailsCard(
                    zone = zone,
                    onExplore = { onNavigateToScanner(zone) },
                    onDismiss = { selectedZone = null }
                )
            }
        }

        AnimatedVisibility(
            visible = selectedZone == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val statusText = when {
                !hasLocationPermission -> "📍  Allow location access"
                !hasRealLocation -> "📡  Waiting for GPS..."
                isLoading -> "🛰️  Scanning nearby biomes..."
                isRefreshing -> "🔄  Updating biomes..."
                biomeLoadError && zones.isNotEmpty() -> "⚠️  Network issue, showing saved biomes"
                biomeLoadError -> "⚠️  Biomes server unavailable"
                zones.isEmpty() -> "❌  No nearby biomes found"
                else -> "🗺️  Nearby growth zones shown as hints"
            }

            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .background(DarkCard.copy(alpha = 0.88f), RoundedCornerShape(20.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = statusText,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TrackPlayerLocation(
    context: Context,
    enabled: Boolean,
    onLocationChanged: (Double, Double) -> Unit
) {
    val latestOnLocationChanged by rememberUpdatedState(onLocationChanged)

    DisposableEffect(context, enabled) {
        if (!enabled || !hasLocationPermission(context)) {
            onDispose {}
        } else {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000L
            )
                .setMinUpdateIntervalMillis(1500L)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    latestOnLocationChanged(location.latitude, location.longitude)
                }
            }

            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            latestOnLocationChanged(location.latitude, location.longitude)
                        }
                    }

                fusedLocationClient.requestLocationUpdates(
                    request,
                    callback,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {
            }

            onDispose {
                try {
                    fusedLocationClient.removeLocationUpdates(callback)
                } catch (_: SecurityException) {
                }
            }
        }
    }
}

@Composable
private fun HerbifyMapView(
    modifier: Modifier,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    zones: List<Zone>,
    selectedZone: Zone?,
    playerLat: Double,
    playerLng: Double,
    onMapReady: (MapLibreMap) -> Unit,
    onZoneTapped: (Zone?) -> Unit
) {
    val latestZones by rememberUpdatedState(zones)
    val latestSelectedZone by rememberUpdatedState(selectedZone)
    val latestPlayerLat by rememberUpdatedState(playerLat)
    val latestPlayerLng by rememberUpdatedState(playerLng)
    val latestOnZoneTapped by rememberUpdatedState(onZoneTapped)
    val latestOnMapReady by rememberUpdatedState(onMapReady)

    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.uiSettings.apply {
                        isLogoEnabled = false
                        isAttributionEnabled = true
                        attributionGravity = Gravity.BOTTOM or Gravity.START
                        setAttributionMargins(8, 0, 0, 8)
                    }
                    setupMap(
                        map = map,
                        zones = latestZones,
                        selectedZone = latestSelectedZone,
                        playerLat = latestPlayerLat,
                        playerLng = latestPlayerLng,
                        onZoneTapped = { tapped ->
                            latestOnZoneTapped(tapped)
                        },
                        getZones = { latestZones }
                    )
                    latestOnMapReady(map)
                }
            }
        },
        update = {
            mapView.getMapAsync { map ->
                val style = map.style ?: return@getMapAsync
                ensureZoneLayers(style, latestZones, latestSelectedZone)
                ensurePlayerLayers(style, latestPlayerLat, latestPlayerLng)
                updateZoneSelection(style, latestZones, latestSelectedZone)
                updatePlayerLocation(style, latestPlayerLat, latestPlayerLng)
            }
        }
    )

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }

            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun setupMap(
    map: MapLibreMap,
    zones: List<Zone>,
    selectedZone: Zone?,
    playerLat: Double,
    playerLng: Double,
    onZoneTapped: (Zone?) -> Unit,
    getZones: () -> List<Zone>
) {
    map.cameraPosition = CameraPosition.Builder()
        .target(LatLng(playerLat, playerLng))
        .zoom(15.4)
        .tilt(48.0)
        .bearing(20.0)
        .build()

    map.setMinZoomPreference(MIN_ZOOM)
    map.setMaxZoomPreference(MAX_ZOOM)

    map.uiSettings.apply {
        isCompassEnabled = false
        isScrollGesturesEnabled = false
        isRotateGesturesEnabled = false
        isTiltGesturesEnabled = false
        isZoomGesturesEnabled = true
        isDoubleTapGesturesEnabled = true
        isQuickZoomGesturesEnabled = true
    }

    map.setStyle(STYLE_URL) { style ->
        add3DBuildings(style)
        ensureZoneLayers(style, zones, selectedZone)
        ensurePlayerLayers(style, playerLat, playerLng)
        setupTapListener(map, getZones, onZoneTapped)
    }
}

private fun add3DBuildings(style: Style) {
    if (style.getLayer(BUILDINGS_3D_ID) != null) return

    val layer = FillExtrusionLayer(BUILDINGS_3D_ID, "openmaptiles").apply {
        sourceLayer = "building"
        setFilter(Expression.eq(Expression.get("extrude"), "true"))
        minZoom = 15f
        setProperties(
            PropertyFactory.fillExtrusionColor("#252525"),
            PropertyFactory.fillExtrusionOpacity(0.32f),
            PropertyFactory.fillExtrusionHeight(
                Expression.interpolate(
                    Expression.linear(),
                    Expression.zoom(),
                    Expression.stop(15, Expression.literal(0)),
                    Expression.stop(
                        16.5,
                        Expression.product(
                            Expression.get("height"),
                            Expression.literal(BUILDING_HEIGHT_MULTIPLIER)
                        )
                    )
                )
            ),
            PropertyFactory.fillExtrusionBase(
                Expression.interpolate(
                    Expression.linear(),
                    Expression.zoom(),
                    Expression.stop(15, Expression.literal(0)),
                    Expression.stop(16.5, Expression.get("min_height"))
                )
            )
        )
    }

    try {
        style.addLayerBelow(layer, "poi")
    } catch (_: Exception) {
        style.addLayer(layer)
    }
}

private fun ensureZoneLayers(style: Style, zones: List<Zone>, selectedZone: Zone?) {
    if (style.getSource(ZONES_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                ZONES_SOURCE_ID,
                zones.toGeoJsonFeatureCollection(selectedId = selectedZone?.id)
            )
        )
    }

    if (style.getLayer(ZONES_FILL_ID) == null) {
        val fillLayer = FillLayer(ZONES_FILL_ID, ZONES_SOURCE_ID).apply {
            setProperties(
                PropertyFactory.fillColor(zoneColorExpression()),
                PropertyFactory.fillOpacity(
                    Expression.switchCase(
                        Expression.toBool(Expression.get("selected")),
                        Expression.literal(0.58),
                        Expression.match(
                            Expression.get("zoneType"),
                            Expression.literal(ZoneType.WATER.name), Expression.literal(0.34),
                            Expression.literal(ZoneType.FOREST.name), Expression.literal(0.30),
                            Expression.literal(ZoneType.PARK.name), Expression.literal(0.24),
                            Expression.literal(ZoneType.GARDEN.name), Expression.literal(0.22),
                            Expression.literal(ZoneType.MEADOW.name), Expression.literal(0.20),
                            Expression.literal(ZoneType.URBAN_GREEN.name), Expression.literal(0.14),
                            Expression.literal(ZoneType.RUINS.name), Expression.literal(0.18),
                            Expression.literal(0.20)
                        )
                    )
                )
            )
        }
        style.addLayer(fillLayer)
    }

    if (style.getLayer(ZONES_LINE_ID) == null) {
        val lineLayer = LineLayer(ZONES_LINE_ID, ZONES_SOURCE_ID).apply {
            setProperties(
                PropertyFactory.lineColor(zoneColorExpression()),
                PropertyFactory.lineWidth(
                    Expression.switchCase(
                        Expression.toBool(Expression.get("selected")),
                        Expression.literal(4.2),
                        Expression.match(
                            Expression.get("zoneType"),
                            Expression.literal(ZoneType.WATER.name), Expression.literal(2.6),
                            Expression.literal(ZoneType.FOREST.name), Expression.literal(2.2),
                            Expression.literal(ZoneType.PARK.name), Expression.literal(1.9),
                            Expression.literal(ZoneType.GARDEN.name), Expression.literal(1.7),
                            Expression.literal(ZoneType.MEADOW.name), Expression.literal(1.6),
                            Expression.literal(ZoneType.URBAN_GREEN.name), Expression.literal(1.2),
                            Expression.literal(ZoneType.RUINS.name), Expression.literal(1.5),
                            Expression.literal(1.6)
                        )
                    )
                ),
                PropertyFactory.lineOpacity(0.95f)
            )
        }
        style.addLayerAbove(lineLayer, ZONES_FILL_ID)
    }
}

private fun ensurePlayerLayers(style: Style, playerLat: Double, playerLng: Double) {
    if (style.getSource(PLAYER_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                PLAYER_SOURCE_ID,
                buildPlayerGeoJson(playerLng, playerLat)
            )
        )
    }

    if (style.getLayer(PLAYER_HALO_ID) == null) {
        val haloLayer = CircleLayer(PLAYER_HALO_ID, PLAYER_SOURCE_ID).apply {
            setProperties(
                PropertyFactory.circleColor("#39FF14"),
                PropertyFactory.circleRadius(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.stop(13, 7f),
                        Expression.stop(16, 11f),
                        Expression.stop(18, 15f)
                    )
                ),
                PropertyFactory.circleOpacity(0.20f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(1.2f),
                PropertyFactory.circleStrokeOpacity(0.25f)
            )
        }
        style.addLayer(haloLayer)
    }

    if (style.getLayer(PLAYER_DOT_ID) == null) {
        val dotLayer = CircleLayer(PLAYER_DOT_ID, PLAYER_SOURCE_ID).apply {
            setProperties(
                PropertyFactory.circleColor("#39FF14"),
                PropertyFactory.circleRadius(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.stop(13, 3.5f),
                        Expression.stop(16, 5f),
                        Expression.stop(18, 6.5f)
                    )
                ),
                PropertyFactory.circleOpacity(1f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleStrokeOpacity(0.8f)
            )
        }
        style.addLayerAbove(dotLayer, PLAYER_HALO_ID)
    }
}

private fun updateZoneSelection(style: Style, zones: List<Zone>, selectedZone: Zone?) {
    val source = style.getSourceAs<GeoJsonSource>(ZONES_SOURCE_ID) ?: return
    source.setGeoJson(zones.toGeoJsonFeatureCollection(selectedId = selectedZone?.id))
}

private fun updatePlayerLocation(style: Style, lat: Double, lng: Double) {
    val source = style.getSourceAs<GeoJsonSource>(PLAYER_SOURCE_ID) ?: return
    source.setGeoJson(buildPlayerGeoJson(lng, lat))
}

private fun centerMapOnPlayer(map: MapLibreMap, lat: Double, lng: Double) {
    val target = CameraPosition.Builder()
        .target(LatLng(lat, lng))
        .zoom(15.8)
        .tilt(48.0)
        .bearing(map.cameraPosition.bearing)
        .build()

    map.easeCamera(
        CameraUpdateFactory.newCameraPosition(target),
        850
    )
}

private fun flyToZone(map: MapLibreMap, zone: Zone) {
    val target = CameraPosition.Builder()
        .target(LatLng(zone.centerLat, zone.centerLng))
        .zoom(16.1)
        .tilt(48.0)
        .bearing(map.cameraPosition.bearing)
        .build()

    map.easeCamera(
        CameraUpdateFactory.newCameraPosition(target),
        650
    )
}

private fun setupTapListener(
    map: MapLibreMap,
    getZones: () -> List<Zone>,
    onZoneTapped: (Zone?) -> Unit
) {
    map.addOnMapClickListener { latLng ->
        val hit = getZones().firstOrNull { zone ->
            pointInPolygon(latLng.longitude, latLng.latitude, zone.coordinates)
        }
        onZoneTapped(hit)
        hit != null
    }
}

private fun zoneColorExpression(): Expression = Expression.match(
    Expression.get("zoneType"),
    Expression.literal(ZoneType.FOREST.name), Expression.color(0xFF1B5E20.toInt()),
    Expression.literal(ZoneType.PARK.name), Expression.color(0xFF4CAF50.toInt()),
    Expression.literal(ZoneType.GARDEN.name), Expression.color(0xFF8BC34A.toInt()),
    Expression.literal(ZoneType.MEADOW.name), Expression.color(0xFFC0CA33.toInt()),
    Expression.literal(ZoneType.WATER.name), Expression.color(0xFF29B6F6.toInt()),
    Expression.literal(ZoneType.URBAN_GREEN.name), Expression.color(0xFF66BB6A.toInt()),
    Expression.literal(ZoneType.RUINS.name), Expression.color(0xFF8D6E63.toInt()),
    Expression.color(0xFF39FF14.toInt())
)

private fun buildPlayerGeoJson(lng: Double, lat: Double): String {
    return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[$lng,$lat]}}]}"""
}

private fun pointInPolygon(px: Double, py: Double, polygon: List<Pair<Double, Double>>): Boolean {
    var inside = false
    var j = polygon.size - 1

    for (i in polygon.indices) {
        val xi = polygon[i].first
        val yi = polygon[i].second
        val xj = polygon[j].first
        val yj = polygon[j].second

        if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
            inside = !inside
        }

        j = i
    }

    return inside
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

private fun distanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double
): Float {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lng1, lat2, lng2, result)
    return result[0]
}

@Composable
fun ZoneDetailsCard(zone: Zone, onExplore: () -> Unit, onDismiss: () -> Unit) {
    val zoneColor = Color(zone.type.accentColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, zoneColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(zoneColor.copy(alpha = 0.14f), CircleShape)
                            .border(1.dp, zoneColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(zone.type.emoji, fontSize = 24.sp)
                    }

                    Column {
                        Text(
                            zone.type.displayName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            zone.type.description,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .background(DarkSurface, CircleShape)
                        .padding(7.dp)
                ) {
                    Text("✕", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .background(zoneColor.copy(alpha = 0.09f), RoundedCornerShape(8.dp))
                    .border(1.dp, zoneColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨", fontSize = 13.sp)
                Text(
                    zone.type.rareChance,
                    color = zoneColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "POSSIBLE PLANTS",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val firstRow = zone.type.possiblePlants.take(3)
                val secondRow = zone.type.possiblePlants.drop(3)

                if (firstRow.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        firstRow.forEach { PlantChip(it, zoneColor) }
                    }
                }

                if (secondRow.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        secondRow.forEach { PlantChip(it, zoneColor) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onExplore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "🔍  Explore Zone",
                    color = DarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun HudChip(label: String, emoji: String) {
    Row(
        modifier = Modifier
            .background(DarkCard.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
            .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(label, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlantChip(name: String, zoneColor: Color) {
    Box(
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .border(1.dp, zoneColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(name, color = TextSecondary, fontSize = 11.sp)
    }
}