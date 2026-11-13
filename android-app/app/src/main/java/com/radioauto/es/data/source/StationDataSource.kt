package com.radioauto.es.data.source

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radioauto.es.data.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Fuente de datos encargada de leer el catálogo de emisoras desde red o almacenamiento local
 */
class StationDataSource(private val context: Context) {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    /**
     * Carga el catálogo estático de respaldo empaquetado en assets/stations.json
     * @return Lista de emisoras locales
     */
    fun loadFromAssets(): List<RadioStation> {
        return try {
            context.assets.open("stations.json").use { inputStream ->
                val reader = InputStreamReader(inputStream, Charsets.UTF_8)
                val type = object : TypeToken<List<RadioStation>>() {}.type
                gson.fromJson<List<RadioStation>>(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Descarga el catálogo actualizado desde el servidor API REST remoto
     * @param url Dirección HTTP del endpoint (ej. http://10.0.2.2:4112/api/stations)
     * @return Resultado con la lista de emisoras o la excepción ocurrida
     */
    suspend fun fetchFromRemote(url: String): Result<List<RadioStation>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("HTTP ${response.code}: ${response.message}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(IllegalStateException("Cuerpo de respuesta vacío"))

                    val type = object : TypeToken<List<RadioStation>>() {}.type
                    val list: List<RadioStation> = gson.fromJson(body, type) ?: emptyList()
                    Result.success(list)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
