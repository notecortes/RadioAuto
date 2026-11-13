package com.radioauto.es.data.source

import android.content.Context
import android.content.SharedPreferences
import com.radioauto.es.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de preferencias persistentes locales (Favoritas, Configuración de Servidor, Tema)
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favoriteIdsFlow = MutableStateFlow(getFavoriteIds())
    val favoriteIdsFlow: StateFlow<Set<String>> = _favoriteIdsFlow.asStateFlow()

    private val _apiUrlFlow = MutableStateFlow(getApiUrl())
    val apiUrlFlow: StateFlow<String> = _apiUrlFlow.asStateFlow()

    private val _forceOfflineFlow = MutableStateFlow(isForceOffline())
    val forceOfflineFlow: StateFlow<Boolean> = _forceOfflineFlow.asStateFlow()

    /**
     * Obtiene el conjunto actual de IDs de emisoras favoritas
     * @return Conjunto de cadenas con los identificadores
     */
    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    /**
     * Alterna el estado de favorita para un ID de emisora específico
     * @param stationId Identificador de la emisora
     * @return Nuevo estado (true si quedó como favorita, false si se quitó)
     */
    fun toggleFavorite(stationId: String): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val newState = if (current.contains(stationId)) {
            current.remove(stationId)
            false
        } else {
            current.add(stationId)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        _favoriteIdsFlow.value = current
        return newState
    }

    /**
     * Verifica si una emisora está marcada como favorita
     * @param stationId Identificador de la emisora
     * @return true si es favorita
     */
    fun isFavorite(stationId: String): Boolean {
        return getFavoriteIds().contains(stationId)
    }

    /**
     * Obtiene la URL del servidor API configurada actualmente
     * @return URL completa del endpoint
     */
    fun getApiUrl(): String {
        return prefs.getString(KEY_API_URL, BuildConfig.DEFAULT_API_URL) ?: BuildConfig.DEFAULT_API_URL
    }

    /**
     * Actualiza la URL del endpoint API
     * @param url Nueva URL del servidor
     */
    fun setApiUrl(url: String) {
        prefs.edit().putString(KEY_API_URL, url).apply()
        _apiUrlFlow.value = url
    }

    /**
     * Indica si se fuerza el catálogo local de assets sin consultar la red
     * @return true si el modo offline forzado está activo
     */
    fun isForceOffline(): Boolean {
        return prefs.getBoolean(KEY_FORCE_OFFLINE, !BuildConfig.USE_REMOTE_FIRST)
    }

    /**
     * Activa o desactiva el uso exclusivo del catálogo local
     * @param force true para no llamar a la API
     */
    fun setForceOffline(force: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_OFFLINE, force).apply()
        _forceOfflineFlow.value = force
    }

    /**
     * Obtiene la preferencia de tema visual (null = seguir sistema)
     * @return Booleano con el estado o null
     */
    fun getThemePreference(): Boolean? {
        if (!prefs.contains(KEY_THEME_DARK)) return null
        return prefs.getBoolean(KEY_THEME_DARK, false)
    }

    /**
     * Guarda la preferencia de tema visual
     * @param isDark true para oscuro, false para claro, null para sistema
     */
    fun setThemePreference(isDark: Boolean?) {
        val editor = prefs.edit()
        if (isDark == null) {
            editor.remove(KEY_THEME_DARK)
        } else {
            editor.putBoolean(KEY_THEME_DARK, isDark)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "radioauto_preferences"
        private const val KEY_FAVORITES = "favorite_station_ids"
        private const val KEY_API_URL = "custom_api_url"
        private const val KEY_FORCE_OFFLINE = "force_offline_fallback"
        private const val KEY_THEME_DARK = "theme_is_dark"
    }
}
