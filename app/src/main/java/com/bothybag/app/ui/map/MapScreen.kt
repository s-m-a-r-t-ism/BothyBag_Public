package com.bothybag.app.ui.map

import android.Manifest
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bothybag.app.data.Bothy
import com.bothybag.app.data.BothyRepository
import com.bothybag.app.data.NewsRepository
import com.bothybag.app.data.VisitedRepository
import com.bothybag.app.location.LocationService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onBothyClick: (String) -> Unit,
    onNewsClick: () -> Unit,
    centerOnBothyId: String? = null
) {
    val context = LocalContext.current
    val bothyRepo = remember { BothyRepository(context) }
    val visitedRepo = remember { VisitedRepository(context) }
    val newsRepo = remember { NewsRepository(context) }
    val locationService = remember { LocationService(context) }
    val scope = rememberCoroutineScope()

    var bothies by remember { mutableStateOf(bothyRepo.loadBothies()) }
    var showMba by remember { mutableStateOf(true) }
    var showNonMba by remember { mutableStateOf(true) }
    var visitedMbaCount by remember { mutableStateOf(visitedRepo.visitedMbaCount(bothyRepo)) }
    val totalMba = remember { bothyRepo.mbaBothyCount }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // The MapView reference
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Request permissions on launch
    LaunchedEffect(Unit) {
        locationPermissions.launchMultiplePermissionRequest()
    }

    // Start/stop location updates based on permission
    DisposableEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            locationService.startUpdates { location ->
                userLocation = location
            }
        }
        onDispose {
            locationService.stopUpdates()
        }
    }

    // Refresh visited count when returning to this screen, and fetch news in background
    LaunchedEffect(Unit) {
        visitedMbaCount = visitedRepo.visitedMbaCount(bothyRepo)
        scope.launch { newsRepo.getNews(bothyRepo) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BothyBag",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Text(
                        text = "$visitedMbaCount / $totalMba MBA visited",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = onNewsClick) {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = "MBA News",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // OSMDroid MapView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    // Configure OSMDroid
                    Configuration.getInstance().apply {
                        userAgentValue = "BothyBag/1.0"
                        osmdroidTileCache = ctx.cacheDir
                    }

                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(6.5)
                        controller.setCenter(GeoPoint(56.5, -4.0))

                        // My location overlay
                        if (locationPermissions.allPermissionsGranted) {
                            val locationOverlay = MyLocationNewOverlay(
                                GpsMyLocationProvider(ctx), this
                            )
                            locationOverlay.enableMyLocation()
                            overlays.add(locationOverlay)
                        }

                        // Add bothy markers
                        addBothyMarkers(this, bothies, visitedRepo, showMba, showNonMba, onBothyClick)

                        mapView = this
                    }
                },
                update = { mv ->
                    // Clear and re-add markers when filters change
                    mv.overlays.removeAll { it is Marker }
                    addBothyMarkers(mv, bothies, visitedRepo, showMba, showNonMba, onBothyClick)
                    mv.invalidate()
                }
            )

            // Center on specific bothy if navigating back from detail
            LaunchedEffect(centerOnBothyId) {
                centerOnBothyId?.let { id ->
                    val bothy = bothyRepo.getBothyById(id)
                    bothy?.let {
                        mapView?.controller?.animateTo(GeoPoint(it.latitude, it.longitude), 14.0, 500L)
                    }
                }
            }

            // My Location FAB
            FloatingActionButton(
                onClick = {
                    userLocation?.let { loc ->
                        mapView?.controller?.animateTo(
                            GeoPoint(loc.latitude, loc.longitude), 14.0, 500L
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }

            // Filter FAB
            SmallFloatingActionButton(
                onClick = {
                    // Cycle through: All -> MBA only -> Non-MBA only -> All
                    when {
                        showMba && showNonMba -> { showMba = true; showNonMba = false }
                        showMba && !showNonMba -> { showMba = false; showNonMba = true }
                        else -> { showMba = true; showNonMba = true }
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }

            // Filter label
            if (!showMba || !showNonMba) {
                val filterLabel = when {
                    showMba -> "MBA only"
                    showNonMba -> "Non-MBA only"
                    else -> ""
                }
                Text(
                    text = filterLabel,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 72.dp, bottom = 88.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun addBothyMarkers(
    mapView: MapView,
    bothies: List<Bothy>,
    visitedRepo: VisitedRepository,
    showMba: Boolean,
    showNonMba: Boolean,
    onBothyClick: (String) -> Unit
) {
    val context = mapView.context
    val visitedMap = visitedRepo.getVisitedMap()

    for (bothy in bothies) {
        // Filter
        if (bothy.isMba && !showMba) continue
        if (!bothy.isMba && !showNonMba) continue

        val isVisited = bothy.id in visitedMap
        val icon = BothyMarkerUtils.getMarkerIcon(
            context,
            isMba = bothy.isMba,
            isClosed = bothy.isClosed,
            isVisited = isVisited
        )

        val marker = Marker(mapView).apply {
            position = GeoPoint(bothy.latitude, bothy.longitude)
            title = bothy.name
            snippet = buildString {
                append(if (bothy.isMba) "MBA" else "Non-MBA")
                if (bothy.region != null && bothy.region != "Unknown") {
                    append(" • ${bothy.region}")
                }
                if (isVisited) append(" ✓")
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.icon = icon
            infoWindow = object : MarkerInfoWindow(
                org.osmdroid.library.R.layout.bonuspack_bubble, mapView
            ) {
                override fun onOpen(item: Any?) {
                    super.onOpen(item)
                    mView.setOnClickListener {
                        onBothyClick(bothy.id)
                        close()
                    }
                }
            }
            setOnMarkerClickListener { m, _ ->
                if (!m.isInfoWindowShown) {
                    m.showInfoWindow()
                    mapView.controller.animateTo(m.position)
                }
                true
            }
        }
        mapView.overlays.add(marker)
    }
}
