package com.radioauto.es.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.radioauto.es.RadioAutoApplication
import com.radioauto.es.data.model.RadioStation
import com.radioauto.es.data.repository.RadioStationRepository
import com.radioauto.es.media.RadioMediaService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la pantalla principal de la aplicación
 */
data class RadioUiState(
    val stations: List<RadioStation> = emptyList(),
    val displayedStations: List<RadioStation> = emptyList(),
    val favoritesCount: Int = 0,
    val selectedTab: Int = 0, // 0 = Favoritas, 1 = Todas, 2 = Géneros, 3 = Países
    val selectedFilterChip: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentStation: RadioStation? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val streamTrackTitle: String? = null,
    val isDarkMode: Boolean? = null,
    val showSettingsDialog: Boolean = false,
    val currentApiUrl: String = "",
    val isForceOffline: Boolean = false
)

/**
 * ViewModel principal que coordina el reproductor Media3 y la lógica de negocio
 */
class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RadioStationRepository = (application as RadioAutoApplication).repository
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _uiState = MutableStateFlow(
        RadioUiState(
            isDarkMode = repository.preferencesManager.getThemePreference(),
            currentApiUrl = repository.preferencesManager.getApiUrl(),
            isForceOffline = repository.preferencesManager.isForceOffline()
        )
    )
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    init {
        setupMediaController()
        observeRepositoryData()
    }

    /**
     * Vincula el MediaController de AndroidX Media3 al servicio RadioMediaService
     */
    private fun setupMediaController() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(context, ComponentName(context, RadioMediaService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val isBuffering = playbackState == Player.STATE_BUFFERING
                        _uiState.value = _uiState.value.copy(isBuffering = isBuffering)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val stationId = mediaItem?.mediaId
                        val station = repository.getStationById(stationId ?: "")
                        _uiState.value = _uiState.value.copy(
                            currentStation = station,
                            streamTrackTitle = null
                        )
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        val title = mediaMetadata.title?.toString()
                        if (!title.isNullOrBlank()) {
                            _uiState.value = _uiState.value.copy(streamTrackTitle = title)
                        }
                    }
                })

                // Si ya estaba reproduciendo al abrir la app, sincronizar estado inicial
                val isPlaying = mediaController?.isPlaying == true
                val currentMediaId = mediaController?.currentMediaItem?.mediaId
                val currentStation = currentMediaId?.let { repository.getStationById(it) }

                _uiState.value = _uiState.value.copy(
                    isPlaying = isPlaying,
                    currentStation = currentStation
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Observa flujos del repositorio para refrescar la lista filtrada de emisoras
     */
    private fun observeRepositoryData() {
        viewModelScope.launch {
            combine(
                repository.stationsFlow,
                repository.isLoadingFlow,
                repository.lastErrorFlow
            ) { stations, isLoading, error ->
                Triple(stations, isLoading, error)
            }.collect { (stations, isLoading, error) ->
                val favCount = stations.count { it.isFavorite }
                val current = _uiState.value
                val filtered = applyFilters(
                    stations,
                    current.selectedTab,
                    current.selectedFilterChip,
                    current.searchQuery
                )

                _uiState.value = current.copy(
                    stations = stations,
                    displayedStations = filtered,
                    favoritesCount = favCount,
                    isLoading = isLoading,
                    errorMessage = error
                )
            }
        }
    }

    /**
     * Aplica la combinación de pestaña activa, filtro secundario y búsqueda de texto
     */
    private fun applyFilters(
        allStations: List<RadioStation>,
        tab: Int,
        filterChip: String?,
        query: String
    ): List<RadioStation> {
        // Filtrar por pestaña
        var list = when (tab) {
            0 -> allStations.filter { it.isFavorite } // Favoritas
            1 -> allStations                          // Todas
            2 -> {                                    // Por Género
                if (filterChip != null) {
                    allStations.filter { it.genre.contains(filterChip, ignoreCase = true) }
                } else allStations
            }
            3 -> {                                    // Por País
                if (filterChip != null) {
                    allStations.filter { it.country.equals(filterChip, ignoreCase = true) }
                } else allStations
            }
            else -> allStations
        }

        // Si la pestaña de favoritas está vacía pero se selecciona, devolver lista vacía con mensaje
        // Filtrar por búsqueda
        val q = query.trim().lowercase()
        if (q.isNotEmpty()) {
            list = list.filter { station ->
                station.name.lowercase().contains(q) ||
                station.genre.lowercase().contains(q) ||
                station.country.lowercase().contains(q) ||
                (station.description?.lowercase()?.contains(q) == true)
            }
        }

        return list
    }

    /**
     * Sintoniza y reproduce una emisora enviándola al servicio Media3
     * @param station Emisora seleccionada
     */
    fun playStation(station: RadioStation) {
        val controller = mediaController ?: return
        _uiState.value = _uiState.value.copy(currentStation = station, isBuffering = true)
        
        controller.setMediaItem(station.toMediaItem())
        controller.prepare()
        controller.play()
    }

    /**
     * Alterna entre pausa y reproducción de la emisora actual
     */
    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.currentMediaItem != null) {
                controller.play()
            } else {
                val candidate = _uiState.value.displayedStations.firstOrNull() 
                    ?: _uiState.value.stations.firstOrNull()
                candidate?.let { playStation(it) }
            }
        }
    }

    /**
     * Salta a la siguiente emisora de la lista actualmente visible
     */
    fun playNext() {
        val list = _uiState.value.displayedStations.ifEmpty { _uiState.value.stations }
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == _uiState.value.currentStation?.id }
        val nextIndex = (currentIndex + 1) % list.size
        playStation(list[nextIndex])
    }

    /**
     * Vuelve a la emisora anterior de la lista actualmente visible
     */
    fun playPrevious() {
        val list = _uiState.value.displayedStations.ifEmpty { _uiState.value.stations }
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == _uiState.value.currentStation?.id }
        val prevIndex = (currentIndex - 1 + list.size) % list.size
        playStation(list[prevIndex])
    }

    /**
     * Alterna el estado de favorita de una emisora
     * @param stationId Identificador de la emisora
     */
    fun toggleFavorite(stationId: String) {
        repository.toggleFavorite(stationId)
    }

    /**
     * Cambia la pestaña principal seleccionada (0 = Favoritas, 1 = Todas, etc.)
     * @param tab Índice de la pestaña
     */
    fun setSelectedTab(tab: Int) {
        val current = _uiState.value
        val filtered = applyFilters(current.stations, tab, null, current.searchQuery)
        _uiState.value = current.copy(
            selectedTab = tab,
            selectedFilterChip = null,
            displayedStations = filtered
        )
    }

    /**
     * Filtra la lista por un subcriterio (género específico o país específico)
     * @param chip Valor del filtro o null para ver todos
     */
    fun setSelectedFilterChip(chip: String?) {
        val current = _uiState.value
        val filtered = applyFilters(current.stations, current.selectedTab, chip, current.searchQuery)
        _uiState.value = current.copy(
            selectedFilterChip = chip,
            displayedStations = filtered
        )
    }

    /**
     * Actualiza el texto de búsqueda y recalcula las emisoras mostradas
     * @param query Término de búsqueda
     */
    fun setSearchQuery(query: String) {
        val current = _uiState.value
        val filtered = applyFilters(current.stations, current.selectedTab, current.selectedFilterChip, query)
        _uiState.value = current.copy(
            searchQuery = query,
            displayedStations = filtered
        )
    }

    /**
     * Cambia y persiste la preferencia de modo oscuro / claro
     * @param isDark true para oscuro, false para claro, null para seguir el sistema
     */
    fun setThemePreference(isDark: Boolean?) {
        repository.preferencesManager.setThemePreference(isDark)
        _uiState.value = _uiState.value.copy(isDarkMode = isDark)
    }

    /**
     * Muestra u oculta el diálogo de ajustes de conexión del servidor
     * @param show true para abrir el diálogo
     */
    fun setShowSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettingsDialog = show)
    }

    /**
     * Actualiza la URL del servidor API y fuerza una recarga de datos
     * @param url Nueva URL del servidor
     */
    fun updateApiUrl(url: String) {
        repository.preferencesManager.setApiUrl(url)
        _uiState.value = _uiState.value.copy(currentApiUrl = url)
        repository.refreshStations(forceRemote = true)
    }

    /**
     * Activa o desactiva el uso exclusivo del catálogo local de assets
     * @param force true para ignorar la red
     */
    fun setForceOffline(force: Boolean) {
        repository.preferencesManager.setForceOffline(force)
        _uiState.value = _uiState.value.copy(isForceOffline = force)
        repository.refreshStations()
    }

    /**
     * Solicita una recarga del catálogo de emisoras
     */
    fun refreshStations() {
        repository.refreshStations(forceRemote = true)
    }

    /**
     * Libera el MediaController al destruir el ViewModel
     */
    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
