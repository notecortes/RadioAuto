package com.radioauto.es.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.radioauto.es.data.model.RadioStation
import com.radioauto.es.ui.theme.GoldFavorite
import com.radioauto.es.ui.theme.LiveRed
import com.radioauto.es.ui.viewmodel.RadioUiState
import com.radioauto.es.ui.viewmodel.RadioViewModel

/**
 * Pantalla principal de la aplicación móvil RadioAuto ES
 *
 * @param viewModel Instancia del ViewModel para observar el estado y disparar acciones
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: RadioViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            HomeTopAppBar(
                isDark = state.isDarkMode == true,
                onToggleTheme = {
                    val next = if (state.isDarkMode == true) false else true
                    viewModel.setThemePreference(next)
                },
                onRefresh = { viewModel.refreshStations() },
                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
            )
        },
        bottomBar = {
            PersistentBottomPlayer(
                state = state,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Buscador de emisoras
            SearchBarSection(
                query = state.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            // Pestañas principales de navegación (Favoritas, Todas, Género, País)
            CategoryTabRow(
                selectedTab = state.selectedTab,
                favoritesCount = state.favoritesCount,
                onSelectTab = { viewModel.setSelectedTab(it) }
            )

            // Filtros secundarios dinámicos si se seleccionó Género o País
            SecondaryFilterChips(
                state = state,
                onSelectChip = { viewModel.setSelectedFilterChip(it) }
            )

            // Lista de tarjetas de emisoras
            StationsList(
                state = state,
                onPlay = { viewModel.playStation(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
            )
        }
    }

    // Diálogo de ajustes de conexión del servidor
    if (state.showSettingsDialog) {
        SettingsDialog(
            currentUrl = state.currentApiUrl,
            isForceOffline = state.isForceOffline,
            onDismiss = { viewModel.setShowSettingsDialog(false) },
            onSave = { newUrl, forceOffline ->
                viewModel.updateApiUrl(newUrl)
                viewModel.setForceOffline(forceOffline)
                viewModel.setShowSettingsDialog(false)
            }
        )
    }
}

/**
 * Barra superior con título, icono de emisora y botones de tema, refresco y ajustes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "RadioAuto ES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Android Auto & Mobile Player",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refrescar catálogo")
            }
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar modo claro/oscuro"
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes de servidor")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Campo de texto para búsqueda interactiva de emisoras
 */
@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar por emisora, género o país...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * Fila de pestañas superiores para cambiar entre Favoritas, Todas, Género y País
 */
@Composable
fun CategoryTabRow(
    selectedTab: Int,
    favoritesCount: Int,
    onSelectTab: (Int) -> Unit
) {
    val tabs = listOf(
        "⭐ Favoritas ($favoritesCount)",
        "📻 Todas",
        "🏷️ Por Género",
        "🌍 Por País"
    )

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

/**
 * Filtros secundarios tipo píldora cuando se explora por género o por país
 */
@Composable
fun SecondaryFilterChips(
    state: RadioUiState,
    onSelectChip: (String?) -> Unit
) {
    if (state.selectedTab != 2 && state.selectedTab != 3) return

    val items = if (state.selectedTab == 2) {
        state.stations.flatMap { it.genre.split(",", "/").map { g -> g.trim() } }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    } else {
        state.stations.map { it.country }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.selectedFilterChip == null,
            onClick = { onSelectChip(null) },
            label = { Text("Todos") }
        )

        items.forEach { chipName ->
            FilterChip(
                selected = state.selectedFilterChip == chipName,
                onClick = { onSelectChip(chipName) },
                label = { Text(chipName) }
            )
        }
    }
}

/**
 * Lista de tarjetas de emisoras con soporte para estado vacío y carga
 */
@Composable
fun StationsList(
    state: RadioUiState,
    onPlay: (RadioStation) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    if (state.isLoading && state.stations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cargando emisoras de radio...")
            }
        }
        return
    }

    if (state.displayedStations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (state.selectedTab == 0) "No tienes emisoras favoritas aún" else "No se encontraron emisoras",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (state.selectedTab == 0) "Pulsa en la estrella o corazón de cualquier emisora para guardarla aquí." else "Prueba a cambiar los filtros o el texto de búsqueda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = state.displayedStations,
            key = { it.id }
        ) { station ->
            val isCurrent = state.currentStation?.id == station.id
            val isPlaying = isCurrent && state.isPlaying

            StationCard(
                station = station,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                onPlay = { onPlay(station) },
                onToggleFavorite = { onToggleFavorite(station.id) }
            )
        }
    }
}

/**
 * Tarjeta individual para visualizar y sintonizar una emisora
 */
@Composable
fun StationCard(
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logotipo de la emisora cargado con Coil
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(station.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Logo de ${station.name}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información textual y etiquetas
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LivePulsingBadge()
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = station.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = station.country,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!station.description.isNullOrBlank()) {
                    Text(
                        text = station.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Botón de Favoritas
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (station.isFavorite) "Quitar de favoritas" else "Añadir a favoritas",
                    tint = if (station.isFavorite) GoldFavorite else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón de Reproducir / Pausar
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Insignia visual parpadeante que indica emisión EN VIVO
 */
@Composable
fun LivePulsingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(LiveRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .scale(scale)
                .background(LiveRed, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "EN VIVO",
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LiveRed,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Barra inferior fija con el reproductor persistente
 */
@Composable
fun PersistentBottomPlayer(
    state: RadioUiState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val station = state.currentStation ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logotipo pequeño
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(station.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Textos del reproductor (Emisora y Canción actual ICY si está disponible)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.streamTrackTitle ?: "${station.genre} • ${station.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Indicador de buferización o controles
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Controles de reproducción
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior")
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Siguiente")
            }
        }
    }
}
