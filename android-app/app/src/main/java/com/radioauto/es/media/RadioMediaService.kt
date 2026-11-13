package com.radioauto.es.media

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.radioauto.es.MainActivity
import com.radioauto.es.RadioAutoApplication
import com.radioauto.es.data.model.RadioStation
import com.radioauto.es.data.repository.RadioStationRepository

/**
 * Servicio de reproducción en segundo plano e integración con Android Auto
 * Implementa MediaLibraryService de AndroidX Media3
 */
class RadioMediaService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private lateinit var repository: RadioStationRepository

    // Emisora actual en reproducción
    private var currentPlayingStation: RadioStation? = null

    /**
     * Inicializa ExoPlayer, la sesión de medios y configura los oyentes de eventos
     */
    override fun onCreate() {
        super.onCreate()
        repository = (application as RadioAutoApplication).repository

        initializePlayer()
        initializeMediaLibrarySession()
    }

    /**
     * Configura la instancia de ExoPlayer optimizada para streaming de radio online
     */
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Control de búfer afinado para radio por internet (baja latencia y rápida recuperación)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,   // Min buffer: 5 segundos
                15_000,  // Max buffer: 15 segundos
                1_500,   // Buffer para iniciar reproducción: 1.5s
                3_000    // Buffer tras rebuferización: 3s
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Gestión automática de foco de audio
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true) // Pausa al desconectar auriculares o Bluetooth
            .build()

        // Oyente de cambios de metadatos (como títulos ICY en directo desde el stream)
        player.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                // Si el stream ICY envía título de canción o artista, lo reflejamos en la sesión
                val streamTitle = mediaMetadata.title?.toString()
                if (!streamTitle.isNullOrBlank() && currentPlayingStation != null) {
                    val updatedMetadata = currentPlayingStation!!.toMediaItem().mediaMetadata.buildUpon()
                        .setSubtitle(streamTitle)
                        .setDisplayTitle(currentPlayingStation!!.name)
                        .build()

                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        val updatedItem = currentItem.buildUpon()
                            .setMediaMetadata(updatedMetadata)
                            .build()
                        // Mantener sincronizado
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // Manejo de fin de stream o reconexión automática si procede
                if (playbackState == Player.STATE_ENDED && currentPlayingStation != null) {
                    // La radio en vivo no debería terminar; si cae, reconectamos
                    player.prepare()
                    player.play()
                }
            }
        })
    }

    /**
     * Inicializa la sesión de librería multimedia para la interfaz de Android Auto
     */
    private fun initializeMediaLibrarySession() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, MediaSessionCallback())
            .setSessionActivity(pendingIntent)
            .setId("RadioAutoSession")
            .build()
    }

    /**
     * Devuelve la sesión activa cuando el sistema Android Auto o un MediaController se conecta
     * @param controllerInfo Información del controlador cliente
     * @return Sesión de biblioteca multimedia
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }

    /**
     * Libera los recursos del reproductor y la sesión al destruir el servicio
     */
    override fun onDestroy() {
        player.release()
        mediaLibrarySession.release()
        super.onDestroy()
    }

    /**
     * Si el usuario cierra la aplicación de recientes y no se está reproduciendo audio,
     * detenemos el servicio para no consumir batería en segundo plano
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    /**
     * Callback personalizado que responde a las peticiones del árbol de navegación de Android Auto
     */
    private inner class MediaSessionCallback : MediaLibrarySession.Callback {

        /**
         * Devuelve el nodo raíz de navegación para la pantalla del coche
         */
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("RadioAuto ES")
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                        .build()
                )
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        /**
         * Construye la lista de elementos para cada nivel de la jerarquía de Android Auto
         */
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = when (parentId) {
                // Nivel 0: Carpetas principales solicitadas
                ROOT_ID -> {
                    ImmutableList.of(
                        createCategoryFolder(CATEGORY_FAVORITES, "⭐ Favoritas"),
                        createCategoryFolder(CATEGORY_GENRES, "📻 Por Género"),
                        createCategoryFolder(CATEGORY_COUNTRIES, "🌍 Por País")
                    )
                }

                // Categoría 1: Favoritas
                CATEGORY_FAVORITES -> {
                    val favorites = repository.getFavorites()
                    if (favorites.isEmpty()) {
                        // Elemento informativo si aún no hay favoritas
                        ImmutableList.of(
                            MediaItem.Builder()
                                .setMediaId("info_no_favorites")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("Sin favoritas guardadas")
                                        .setArtist("Marca con el corazón tus emisoras en el móvil")
                                        .setIsPlayable(false)
                                        .setIsBrowsable(false)
                                        .build()
                                )
                                .build()
                        )
                    } else {
                        ImmutableList.copyOf(favorites.map { it.toMediaItem() })
                    }
                }

                // Categoría 2: Géneros (Lista de carpetas de género)
                CATEGORY_GENRES -> {
                    val genresMap = repository.getStationsByGenre()
                    val folderList = genresMap.keys.sorted().map { genreName ->
                        createSubFolder("genre:$genreName", genreName, "${genresMap[genreName]?.size ?: 0} emisoras")
                    }
                    ImmutableList.copyOf(folderList)
                }

                // Categoría 3: Países (Lista de carpetas de país)
                CATEGORY_COUNTRIES -> {
                    val countriesMap = repository.getStationsByCountry()
                    val folderList = countriesMap.keys.sorted().map { countryName ->
                        createSubFolder("country:$countryName", countryName, "${countriesMap[countryName]?.size ?: 0} emisoras")
                    }
                    ImmutableList.copyOf(folderList)
                }

                // Subcarpeta de Género específica (ej: "genre:Indie / Alternativa")
                else -> {
                    when {
                        parentId.startsWith("genre:") -> {
                            val genreName = parentId.removePrefix("genre:")
                            val stations = repository.getStationsByGenre()[genreName] ?: emptyList()
                            ImmutableList.copyOf(stations.map { it.toMediaItem() })
                        }
                        parentId.startsWith("country:") -> {
                            val countryName = parentId.removePrefix("country:")
                            val stations = repository.getStationsByCountry()[countryName] ?: emptyList()
                            ImmutableList.copyOf(stations.map { it.toMediaItem() })
                        }
                        else -> ImmutableList.of()
                    }
                }
            }

            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        /**
         * Obtiene un elemento multimedia concreto por su MediaId
         */
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val station = repository.getStationById(mediaId)
            return if (station != null) {
                Futures.immediateFuture(LibraryResult.ofItem(station.toMediaItem(), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(SessionResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        /**
         * Gestiona búsquedas de texto desde la pantalla del coche o Google Assistant
         */
        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            mediaLibrarySession.notifySearchResultChanged(browser, query, repository.search(query).size, params)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val results = repository.search(query).map { it.toMediaItem() }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(results), params))
        }

        /**
         * Maneja peticiones de reproducción directa y comandos de voz de Google Assistant
         * ("Pon La Indie", "reproduce activa fm", etc.) resolviendo la búsqueda en requestMetadata
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolvedItems = mediaItems.map { item ->
                resolveMediaItem(item)
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    resolvedItems,
                    startIndex,
                    startPositionMs
                )
            )
        }

        /**
         * Resuelve la adición de elementos multimedia (incluso desde búsquedas o enlaces)
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolvedItems = mediaItems.map { item ->
                resolveMediaItem(item)
            }.toMutableList()
            return Futures.immediateFuture(resolvedItems)
        }

        /**
         * Gestiona la reanudación de reproducción desde controles físicos del volante o Bluetooth
         */
        override fun onPlaybackResumption(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val station = currentPlayingStation 
                ?: repository.getFavorites().firstOrNull() 
                ?: repository.stationsFlow.value.firstOrNull()

            return if (station != null) {
                val mediaItem = station.toMediaItem()
                Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        ImmutableList.of(mediaItem),
                        0,
                        C.TIME_UNSET
                    )
                )
            } else {
                super.onPlaybackResumption(session, controller)
            }
        }

        /**
         * Soporte para salto de emisora (Siguiente / Anterior desde volante)
         */
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_NEXT_STATION -> {
                    playAdjacentStation(forward = true)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_PREV_STATION -> {
                    playAdjacentStation(forward = false)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    /**
     * Resuelve un MediaItem entrante buscando por voz (requestMetadata.searchQuery) o por ID
     * @param item MediaItem recibido
     * @return MediaItem completamente resuelto con streamUrl y metadatos
     */
    private fun resolveMediaItem(item: MediaItem): MediaItem {
        val query = item.requestMetadata.searchQuery
        if (!query.isNullOrBlank()) {
            val match = repository.search(query).firstOrNull()
            if (match != null) {
                currentPlayingStation = match
                return match.toMediaItem()
            }
        }

        val station = repository.getStationById(item.mediaId)
        if (station != null) {
            currentPlayingStation = station
            return station.toMediaItem()
        }

        return item
    }

    /**
     * Sintoniza y reproduce internamente una emisora en ExoPlayer
     * @param station Emisora a reproducir
     */
    private fun playStationInternal(station: RadioStation) {
        currentPlayingStation = station
        val mediaItem = station.toMediaItem()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    /**
     * Salta a la emisora siguiente o anterior de la lista
     * @param forward true para avanzar, false para retroceder
     */
    private fun playAdjacentStation(forward: Boolean) {
        val list = repository.stationsFlow.value
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == currentPlayingStation?.id }
        val targetIndex = if (forward) {
            (currentIndex + 1) % list.size
        } else {
            (currentIndex - 1 + list.size) % list.size
        }

        playStationInternal(list[targetIndex])
    }

    /**
     * Crea un elemento de carpeta principal para Android Auto
     */
    private fun createCategoryFolder(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    .build()
            )
            .build()
    }

    /**
     * Crea un elemento de subcarpeta (por género o país)
     */
    private fun createSubFolder(id: String, title: String, subtitle: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    .build()
            )
            .build()
    }

    companion object {
        const val ROOT_ID = "[root_radioauto]"
        const val CATEGORY_FAVORITES = "category_favorites"
        const val CATEGORY_GENRES = "category_genres"
        const val CATEGORY_COUNTRIES = "category_countries"

        const val ACTION_NEXT_STATION = "com.radioauto.es.ACTION_NEXT_STATION"
        const val ACTION_PREV_STATION = "com.radioauto.es.ACTION_PREV_STATION"
    }
}
