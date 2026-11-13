package com.radioauto.es.data.repository

import android.content.Context
import com.radioauto.es.data.model.RadioStation
import com.radioauto.es.data.source.PreferencesManager
import com.radioauto.es.data.source.StationDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repositorio centralizado que coordina la carga, sincronización y estado de favoritas de las emisoras
 */
class RadioStationRepository(context: Context) {

    val preferencesManager = PreferencesManager(context)
    private val dataSource = StationDataSource(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _stationsFlow = MutableStateFlow<List<RadioStation>>(emptyList())
    val stationsFlow: StateFlow<List<RadioStation>> = _stationsFlow.asStateFlow()

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    private val _lastErrorFlow = MutableStateFlow<String?>(null)
    val lastErrorFlow: StateFlow<String?> = _lastErrorFlow.asStateFlow()

    init {
        // Carga inicial al instanciar el repositorio
        refreshStations()
    }

    /**
     * Refresca la lista de emisoras intentando conectar con el servidor o usando fallback de assets
     * @param forceRemote true para intentar llamada de red incluso si el modo offline está configurado
     */
    fun refreshStations(forceRemote: Boolean = false) {
        scope.launch {
            _isLoadingFlow.value = true
            _lastErrorFlow.value = null

            val favoriteIds = preferencesManager.getFavoriteIds()
            val forceOffline = preferencesManager.isForceOffline()
            val apiUrl = preferencesManager.getApiUrl()

            var loadedStations: List<RadioStation> = emptyList()

            // Intentar cargar desde el servidor si no está forzado a offline
            if (!forceOffline || forceRemote) {
                val remoteResult = dataSource.fetchFromRemote(apiUrl)
                if (remoteResult.isSuccess) {
                    loadedStations = remoteResult.getOrNull() ?: emptyList()
                } else {
                    _lastErrorFlow.value = remoteResult.exceptionOrNull()?.localizedMessage
                }
            }

            // Fallback a los datos locales incluidos en assets
            if (loadedStations.isEmpty()) {
                loadedStations = dataSource.loadFromAssets()
            }

            // Mapear estado de favorita a cada emisora
            val enrichedList = loadedStations.map { station ->
                station.copy(isFavorite = favoriteIds.contains(station.id))
            }

            _stationsFlow.value = enrichedList
            _isLoadingFlow.value = false
        }
    }

    /**
     * Alterna la marca de favorita de una emisora y actualiza el flujo reactivo
     * @param stationId Identificador de la emisora
     */
    fun toggleFavorite(stationId: String) {
        preferencesManager.toggleFavorite(stationId)
        val favoriteIds = preferencesManager.getFavoriteIds()

        val updatedList = _stationsFlow.value.map { station ->
            if (station.id == stationId) {
                station.copy(isFavorite = favoriteIds.contains(station.id))
            } else {
                station
            }
        }
        _stationsFlow.value = updatedList
    }

    /**
     * Obtiene una emisora específica a partir de su identificador
     * @param id Identificador de la emisora
     * @return Objeto RadioStation o null si no existe
     */
    fun getStationById(id: String): RadioStation? {
        return _stationsFlow.value.find { it.id == id }
    }

    /**
     * Obtiene la lista actual de emisoras marcadas como favoritas
     * @return Lista de emisoras favoritas
     */
    fun getFavorites(): List<RadioStation> {
        return _stationsFlow.value.filter { it.isFavorite }
    }

    /**
     * Agrupa todas las emisoras según sus géneros musicales, desglosando etiquetas compuestas
     * @return Mapa donde la clave es el género o etiqueta y el valor la lista de emisoras coincidentes
     */
    fun getStationsByGenre(): Map<String, List<RadioStation>> {
        val result = mutableMapOf<String, MutableList<RadioStation>>()
        _stationsFlow.value.forEach { station ->
            val tags = station.genre.split(",", "/").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isEmpty()) {
                result.getOrPut("Variedad") { mutableListOf() }.add(station)
            } else {
                tags.forEach { tag ->
                    result.getOrPut(tag) { mutableListOf() }.add(station)
                }
            }
        }
        return result
    }

    /**
     * Agrupa todas las emisoras según su país de origen
     * @return Mapa donde la clave es el país y el valor la lista de emisoras
     */
    fun getStationsByCountry(): Map<String, List<RadioStation>> {
        return _stationsFlow.value.groupBy { it.country.ifBlank { "Internacional" } }
    }

    /**
     * Realiza una búsqueda por texto en nombre, género, país o descripción
     * @param query Término de búsqueda introducido por el usuario o comando de voz
     * @return Lista de emisoras coincidentes
     */
    fun search(query: String): List<RadioStation> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _stationsFlow.value

        return _stationsFlow.value.filter { station ->
            station.name.lowercase().contains(q) ||
            station.genre.lowercase().contains(q) ||
            station.country.lowercase().contains(q) ||
            (station.description?.lowercase()?.contains(q) == true)
        }
    }
}
