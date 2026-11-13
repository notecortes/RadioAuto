package com.radioauto.es.data.model

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos que representa una emisora de radio online
 *
 * @property id Identificador único alfanumérico
 * @property name Nombre oficial de la emisora
 * @property genre Género musical principal o géneros asociados
 * @property country País de origen normalizado
 * @property streamUrl URL directa del flujo de audio (MP3, AAC, HLS)
 * @property logoUrl URL o ruta del logotipo de la emisora
 * @property description Descripción corta del estilo musical o formato
 * @property isFavorite Indica si el usuario la ha guardado como favorita
 * @property broadcaster Emisora matriz o grupo de radiodifusión
 * @property bitrate Tasa de bits en kbps (ej: "128", "192")
 * @property format Formato del flujo (ej: "MP3", "AAC")
 */
data class RadioStation(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("genre")
    val genre: String = "Variedad",

    @SerializedName("country")
    val country: String = "España",

    @SerializedName("streamUrl", alternate = ["stream_url"])
    val streamUrl: String,

    @SerializedName("logoUrl", alternate = ["logo_url"])
    val logoUrl: String = "",

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("isFavorite", alternate = ["favorite", "is_favorite"])
    val isFavorite: Boolean = false,

    @SerializedName("broadcaster")
    val broadcaster: String? = null,

    @SerializedName("bitrate")
    val bitrate: String? = null,

    @SerializedName("format")
    val format: String? = null
) {
    /**
     * Convierte la emisora en un MediaItem compatible con AndroidX Media3 y Android Auto
     * @return Objeto MediaItem con metadatos completos para salpicadero y controles al volante
     */
    fun toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putString("station_id", id)
            putString("station_name", name)
            putString("station_genre", genre)
            putString("station_country", country)
            putString("stream_url", streamUrl)
            putBoolean("is_favorite", isFavorite)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist("$genre • $country")
            .setAlbumTitle(genre)
            .setArtworkUri(if (logoUrl.isNotEmpty()) Uri.parse(logoUrl) else null)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }
}
