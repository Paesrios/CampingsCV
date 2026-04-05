package pabloesteban.joangiron.uv.es

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import pabloesteban.joangiron.uv.es.ui.theme.CampingsCVTheme
import java.util.Locale
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = CampingsDatabase.getDatabase(applicationContext)
        val favoriteRepository = FavoriteCampingRepository(database.favoriteCampingDao())

        enableEdgeToEdge()
        setContent {
            CampingsCVTheme {
                CampingsRoot(favoriteRepository = favoriteRepository)
            }
        }
    }
}

@Composable
fun CampingsRoot(favoriteRepository: FavoriteCampingRepository) {
    var campings by remember { mutableStateOf<List<Camping>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadToken) {
        isLoading = true
        errorText = null
        try {
            campings = withContext(Dispatchers.IO) {
                OpenDataApi.service.getCampings().result.records.map { it.toCamping() }
            }
        } catch (ex: Exception) {
            errorText = ex.message ?: "No se pudo obtener el listado de campings"
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> SplashScreen()
        errorText != null -> CampingsErrorScreen(
            message = errorText ?: "Error de carga",
            onRetry = { reloadToken += 1 }
        )
        else -> AppNavHost(campings = campings, favoriteRepository = favoriteRepository)
    }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Campings CV")
        CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
        Text(text = "Cargando campings...", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun CampingsErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No se pudieron descargar los datos")
        Text(text = message, modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text("Reintentar")
        }
    }
}

private const val ROUTE_LIST = "camping_list"
private const val ROUTE_FAVORITES = "camping_favorites"
private const val ARG_CAMPING_ID = "campingId"
private const val ROUTE_DETAIL = "camping_detail/{$ARG_CAMPING_ID}"

private fun detailRoute(campingId: Int): String = "camping_detail/$campingId"

private enum class SortField {
    NAME,
    CATEGORY,
    PLAZAS,
    DISTANCE
}

@Composable
fun AppNavHost(campings: List<Camping>, favoriteRepository: FavoriteCampingRepository) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val favorites by favoriteRepository.favoritesFlow.collectAsState(initial = emptyList())
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(route = ROUTE_LIST) {
            CampingListScreen(
                campings = campings,
                favoriteIds = favoriteIds,
                onCampingClick = { selectedCamping ->
                    navController.navigate(detailRoute(selectedCamping.id))
                },
                onToggleFavorite = { camping ->
                    scope.launch {
                        if (favoriteIds.contains(camping.id)) {
                            favoriteRepository.removeFavorite(camping.id)
                        } else {
                            favoriteRepository.addFavorite(camping)
                        }
                    }
                },
                onOpenFavorites = { navController.navigate(ROUTE_FAVORITES) }
            )
        }

        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument(ARG_CAMPING_ID) { type = NavType.IntType })
        ) { backStackEntry ->
            val campingId = backStackEntry.arguments?.getInt(ARG_CAMPING_ID)
            val selectedCamping = campings.firstOrNull { it.id == campingId }

            if (selectedCamping != null) {
                CampingDetailScreen(
                    camping = selectedCamping,
                    favoriteRepository = favoriteRepository,
                    onBack = { navController.popBackStack() },
                    onOpenFavorites = { navController.navigate(ROUTE_FAVORITES) }
                )
            } else {
                CampingNotFoundScreen(onBack = { navController.popBackStack() })
            }
        }

        composable(route = ROUTE_FAVORITES) {
            FavoritesScreen(
                favorites = favorites.map { it.toCamping() },
                onCampingClick = { selectedCamping ->
                    navController.navigate(detailRoute(selectedCamping.id))
                },
                onToggleFavorite = { camping ->
                    scope.launch {
                        favoriteRepository.removeFavorite(camping.id)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CampingListScreen(
    campings: List<Camping>,
    favoriteIds: Set<Int>,
    onCampingClick: (Camping) -> Unit,
    onToggleFavorite: (Camping) -> Unit,
    onOpenFavorites: () -> Unit
) {
    val context = LocalContext.current
    var isMenuOpen by remember { mutableStateOf(false) }
    var sortField by rememberSaveable { mutableStateOf(SortField.NAME.name) }
    var ascending by rememberSaveable { mutableStateOf(true) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Todas") }
    var selectedProvince by rememberSaveable { mutableStateOf("Todas") }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var locationStatus by rememberSaveable { mutableStateOf("Sin ubicacion") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val distanceByCampingId = remember { mutableStateMapOf<Int, Double>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val location = getLastKnownUserLocation(context)
            userLocation = location
            locationStatus = if (location != null) "Ubicacion obtenida" else "No se pudo obtener ubicacion"
        } else {
            locationStatus = "Permiso de ubicacion denegado"
        }
    }

    val categories = remember(campings) {
        listOf("Todas") + campings.map { it.categoria }.distinct().sorted()
    }
    val provinces = remember(campings) {
        listOf("Todas") + campings.map { it.provincia }.distinct().sorted()
    }

    LaunchedEffect(userLocation, campings) {
        val location = userLocation ?: return@LaunchedEffect
        locationStatus = "Calculando distancias..."

        for (camping in campings) {
            if (!distanceByCampingId.containsKey(camping.id)) {
                val coordinates = geocodeCampingCoordinates(context, camping)
                if (coordinates != null) {
                    val distanceMeters = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        coordinates.first,
                        coordinates.second,
                        distanceMeters
                    )
                    distanceByCampingId[camping.id] = distanceMeters[0] / 1000.0
                }
            }
            locationStatus = "Distancias: ${distanceByCampingId.size}/${campings.size}"
        }
    }

    val filteredSortedCampings = campings.filter { camping ->
        val searchLower = searchText.trim().lowercase()
        val matchesSearch = if (searchLower.isBlank()) {
            true
        } else {
            camping.nombre.lowercase().contains(searchLower) ||
                camping.municipio.lowercase().contains(searchLower) ||
                camping.provincia.lowercase().contains(searchLower)
        }

        val matchesCategory = selectedCategory == "Todas" || camping.categoria == selectedCategory
        val matchesProvince = selectedProvince == "Todas" || camping.provincia == selectedProvince

        matchesSearch && matchesCategory && matchesProvince
    }.let { searched ->
        val sorted = when (SortField.valueOf(sortField)) {
            SortField.NAME -> searched.sortedBy { it.nombre }
            SortField.CATEGORY -> searched.sortedBy { it.categoria }
            SortField.PLAZAS -> searched.sortedBy { it.plazas }
            SortField.DISTANCE -> searched.sortedBy { distanceByCampingId[it.id] ?: Double.MAX_VALUE }
        }
        if (ascending) sorted else sorted.reversed()
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }) {
                    Text("^")
                }
                FloatingActionButton(onClick = onOpenFavorites) {
                    Text("Fav")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Campings CV") },
                actions = {
                    TextButton(onClick = { ascending = !ascending }) {
                        Text(if (ascending) "Asc" else "Desc")
                    }
                    IconButton(onClick = { isMenuOpen = true }) {
                        Text("...")
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ordenar por nombre") },
                            onClick = {
                                sortField = SortField.NAME.name
                                isMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ordenar por categoria") },
                            onClick = {
                                sortField = SortField.CATEGORY.name
                                isMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ordenar por plazas") },
                            onClick = {
                                sortField = SortField.PLAZAS.name
                                isMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ordenar por distancia") },
                            onClick = {
                                sortField = SortField.DISTANCE.name
                                isMenuOpen = false
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre o ubicacion") },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = locationStatus,
                    style = MaterialTheme.typography.labelMedium
                )
                TextButton(onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        val location = getLastKnownUserLocation(context)
                        userLocation = location
                        locationStatus = if (location != null) "Ubicacion obtenida" else "No se pudo obtener ubicacion"
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Text("Usar mi ubicacion")
                }
            }

            Text(text = "Filtrar por categoria", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            Text(text = "Filtrar por provincia", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(provinces) { province ->
                    FilterChip(
                        selected = selectedProvince == province,
                        onClick = { selectedProvince = province },
                        label = { Text(province) }
                    )
                }
            }

            Text(
                text = "Resultados: ${filteredSortedCampings.size}",
                style = MaterialTheme.typography.labelMedium
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSortedCampings, key = { it.id }) { camping ->
                    CampingItem(
                        camping = camping,
                        isFavorite = favoriteIds.contains(camping.id),
                        distanceKm = distanceByCampingId[camping.id],
                        showDistance = userLocation != null,
                        onToggleFavorite = { onToggleFavorite(camping) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCampingClick(camping) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CampingDetailScreen(
    camping: Camping,
    favoriteRepository: FavoriteCampingRepository,
    onBack: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    val isFavorite by favoriteRepository.isFavoriteFlow(camping.id).collectAsState(initial = false)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenFavorites) {
                Text("Fav")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(camping.nombre) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Text("...")
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Abrir en Maps") },
                            onClick = {
                                isMenuOpen = false
                                openCampingInMaps(context, camping)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Abrir web") },
                            enabled = camping.web.isNotBlank(),
                            onClick = {
                                isMenuOpen = false
                                openCampingWebsite(context, camping.web)
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = camping.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(text = "${camping.categoria} | ${camping.modalidad}")
                        Text(text = "${camping.municipio} (${camping.provincia})")
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Favoritos", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isFavorite) "Este camping esta en favoritos" else "Este camping no esta en favoritos"
                        )
                        TextButton(onClick = {
                            scope.launch {
                                if (isFavorite) {
                                    favoriteRepository.removeFavorite(camping.id)
                                } else {
                                    favoriteRepository.addFavorite(camping)
                                }
                            }
                        }) {
                            Text(if (isFavorite) "Quitar de favoritos" else "Anadir a favoritos")
                        }
                    }
                }
            }

            item {
                DetailSectionCard(
                    title = "Capacidad",
                    lines = listOf(
                        "Plazas totales: ${camping.plazas}",
                        "Parcelas: ${camping.numParcelas}",
                        "Bungalows: ${camping.numBungalows}"
                    )
                )
            }

            item {
                DetailSectionCard(
                    title = "Ubicacion",
                    lines = listOf(
                        "Direccion: ${camping.direccion}",
                        "Via: ${camping.tipoVia} ${camping.via} ${camping.numero}".trim(),
                        "CP: ${camping.cp}",
                        "Municipio: ${camping.municipio}",
                        "Provincia: ${camping.provincia}"
                    )
                )
            }

            item {
                DetailSectionCard(
                    title = "Contacto y estado",
                    lines = listOf(
                        "Email: ${camping.email.ifBlank { "No disponible" }}",
                        "Web: ${camping.web.ifBlank { "No disponible" }}",
                        "Estado: ${camping.estado}",
                        "Periodo: ${camping.periodo.ifBlank { "No especificado" }}",
                        "Signatura: ${camping.signatura}"
                    )
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FavoritesScreen(
    favorites: List<Camping>,
    onCampingClick: (Camping) -> Unit,
    onToggleFavorite: (Camping) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }) {
                Text("^")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Campings favoritos") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Todavia no tienes favoritos")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favorites, key = { it.id }) { camping ->
                    CampingItem(
                        camping = camping,
                        isFavorite = true,
                        distanceKm = null,
                        showDistance = false,
                        onToggleFavorite = { onToggleFavorite(camping) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCampingClick(camping) }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailSectionCard(title: String, lines: List<String>) {
    Card(colors = CardDefaults.cardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold)
            lines.forEach { line ->
                Text(text = line)
            }
        }
    }
}

@Composable
fun CampingNotFoundScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No se encontro el camping seleccionado")
        TextButton(onClick = onBack) {
            Text("Volver")
        }
    }
}

@Composable
fun CampingItem(
    camping: Camping,
    isFavorite: Boolean,
    distanceKm: Double?,
    showDistance: Boolean,
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = camping.nombre,
                    fontWeight = FontWeight.Bold
                )
                if (onToggleFavorite != null) {
                    TextButton(onClick = onToggleFavorite) {
                        Text(if (isFavorite) "Quitar" else "Favorito")
                    }
                }
            }
            Text(text = "Categoria: ${camping.categoria}")
            Text(text = "Ubicacion: ${camping.municipio} (${camping.provincia})")
            Text(text = "Plazas: ${camping.plazas}")
            if (showDistance) {
                if (distanceKm != null) {
                    Text(text = "Distancia: ${formatDistanceKm(distanceKm)}")
                } else {
                    Text(text = "Distancia: no disponible")
                }
            }
            if (isFavorite) {
                Text(text = "Guardado en favoritos")
            }
            if (camping.web.isNotBlank()) {
                Text(text = "Web: ${camping.web}")
            }
        }
    }
}

private fun formatDistanceKm(distanceKm: Double): String = String.format(Locale.getDefault(), "%.2f km", distanceKm)

private fun getLastKnownUserLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = manager.getProviders(true)
    var bestLocation: Location? = null

    for (provider in providers) {
        val location = try {
            manager.getLastKnownLocation(provider)
        } catch (_: SecurityException) {
            null
        }
        if (location != null && (bestLocation == null || location.time > bestLocation!!.time)) {
            bestLocation = location
        }
    }

    return bestLocation
}

private suspend fun geocodeAddress(context: Context, query: String): Pair<Double, Double>? {
    if (query.isBlank()) return null

    val geocoder = Geocoder(context, Locale.getDefault())

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(query, 1) { addresses ->
                val address = addresses.firstOrNull()
                continuation.resume(address?.latitude?.let { lat -> lat to address.longitude })
            }
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 1)
            val address = addresses?.firstOrNull()
            address?.latitude?.let { lat -> lat to address.longitude }
        }
    }
}

private suspend fun geocodeCampingCoordinates(context: Context, camping: Camping): Pair<Double, Double>? {
    val queries = listOf(
        listOf(camping.direccion, camping.municipio, camping.provincia).filter { it.isNotBlank() }.joinToString(", "),
        listOf(camping.via, camping.numero, camping.municipio, camping.provincia).filter { it.isNotBlank() }.joinToString(", "),
        listOf(camping.cp, camping.municipio, camping.provincia).filter { it.isNotBlank() }.joinToString(", "),
        listOf(camping.municipio, camping.provincia).filter { it.isNotBlank() }.joinToString(", ")
    ).distinct().filter { it.isNotBlank() }

    for (query in queries) {
        val coordinates = geocodeAddress(context, query)
        if (coordinates != null) return coordinates
    }

    return null
}

private fun openCampingInMaps(context: Context, camping: Camping) {
    val query = listOf(camping.direccion, camping.municipio, camping.provincia)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)

    try {
        context.startActivity(mapIntent)
    } catch (_: ActivityNotFoundException) {
    }
}

private fun openCampingWebsite(context: Context, website: String) {
    if (website.isBlank()) return

    val normalizedUrl = if (website.startsWith("http://") || website.startsWith("https://")) {
        website
    } else {
        "https://$website"
    }

    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl))
    try {
        context.startActivity(browserIntent)
    } catch (_: ActivityNotFoundException) {
    }
}

@Preview(showBackground = true)
@Composable
fun CampingListPreview() {
    val sampleCampings = listOf(
        Camping(
            id = 1,
            signatura = "CV-CAM000102-A",
            estado = "ALTA",
            modalidad = "CAMPING",
            nombre = "LA COLINA",
            categoria = "CUATRO ESTRELLAS",
            municipio = "L'ALFAS DEL PI",
            provincia = "ALACANT",
            cp = "03581",
            direccion = "CALLE SERRA GELADA, S/N",
            tipoVia = "CALLE",
            via = "SERRA GELADA",
            numero = "S/N",
            email = "lacolinabeach@hotmail.com",
            periodo = "SIEMPRE ABIERTO",
            numParcelas = 150,
            numBungalows = 1,
            plazas = 449,
            web = "campinglacolina.com"
        )
    )

    CampingsCVTheme {
        CampingListScreen(
            campings = sampleCampings,
            favoriteIds = setOf(1),
            onCampingClick = {},
            onToggleFavorite = {},
            onOpenFavorites = {}
        )
    }
}
