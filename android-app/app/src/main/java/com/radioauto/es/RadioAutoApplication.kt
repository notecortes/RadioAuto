package com.radioauto.es

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.radioauto.es.data.repository.RadioStationRepository

/**
 * Clase principal de la aplicación para inicializar servicios y configuraciones globales
 */
class RadioAutoApplication : Application(), ImageLoaderFactory {

    // Repositorio de emisoras accesible en toda la aplicación
    lateinit var repository: RadioStationRepository
        private set

    /**
     * Inicializa componentes globales al arrancar la aplicación
     */
    override fun onCreate() {
        super.onCreate()
        repository = RadioStationRepository(applicationContext)
    }

    /**
     * Configura el cargador de imágenes Coil con soporte nativo para gráficos vectoriales SVG
     * @return Instancia configurada de ImageLoader
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
