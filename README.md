# 🚗📻 RadioAuto ES

**RadioAuto ES** es una plataforma completa de reproducción de emisoras de radio por internet (streaming MP3 / AAC / HLS) diseñada para funcionar tanto como una **aplicación móvil moderna con Jetpack Compose (Material Design 3)**, como de forma nativa e integrada en la pantalla del coche mediante **Android Auto (AndroidX Media3 MediaLibraryService + ExoPlayer)**.

Incluye un **entorno local contenerizado con Docker** en el puerto **`4112`** con una interfaz web avanzada (modo claro/oscuro) para probar la reproducción de flujos de audio, comprobar latencias y errores de CORS/SSL, y editar el catálogo de emisoras antes de compilar la app móvil.

---

## 🏛️ 1. Arquitectura del Proyecto

El repositorio está estructurado en dos módulos independientes y limpios:

```text
radioCoche/
├── server-mock/                        # [Módulo 1] Servidor Mock & Entorno Web de Pruebas
│   ├── Dockerfile                      # Imagen ultraligera basada en Node Alpine (0 dependencias npm)
│   ├── docker-compose.yml              # Mapeo estricto al puerto 4112
│   ├── server.js                       # Servidor HTTP con API REST y CORS habilitado
│   ├── stations.json                   # Catálogo maestro de emisoras
│   ├── default-stations.json           # Respaldo para restaurar el catálogo original
│   └── public/                         # Panel web con HTML5, Vanilla CSS y Vanilla JS
│       ├── index.html                  # Interfaz con controles, buscador y reproductor
│       ├── style.css                   # Diseño Material 3 con modo claro / oscuro
│       ├── app.js                      # Lógica de reproducción, tests de stream y CRUD
│       └── logos/                      # Logotipos vectoriales SVG en alta definición
│
├── android-app/                        # [Módulo 2] Aplicación Android + Android Auto
│   ├── app/
│   │   ├── build.gradle.kts            # Configuración de compilación, sabores y firma de Release
│   │   ├── release.keystore            # Almacén de claves de prueba para firma automática
│   │   └── src/main/
│   │       ├── AndroidManifest.xml     # Permisos de streaming y declaración Automotive Media
│   │       ├── assets/stations.json    # Catálogo local de respaldo para funcionamiento offline
│   │       ├── res/xml/
│   │       │   └── automotive_app_desc.xml # Descriptor nativo <uses name="media" />
│   │       └── java/com/radioauto/es/
│   │           ├── RadioAutoApplication.kt # Inicialización de Coil (soporte SVG) y Repositorio
│   │           ├── MainActivity.kt         # Entrada principal con Jetpack Compose
│   │           ├── data/
│   │           │   ├── model/RadioStation.kt           # Modelo y conversión a Media3 MediaItem
│   │           │   ├── source/StationDataSource.kt     # Carga desde API REST o assets locales
│   │           │   ├── source/PreferencesManager.kt    # Persistencia de favoritas y ajustes
│   │           │   └── repository/RadioStationRepository.kt # Coordinación y filtrado reactivo
│   │           ├── media/
│   │           │   └── RadioMediaService.kt    # MediaLibraryService, ExoPlayer y árbol Android Auto
│   │           └── ui/
│   │               ├── theme/                  # Paleta de colores M3, tipografía y temas
│   │               ├── viewmodel/RadioViewModel.kt # Conexión MediaController y estados
│   │               └── screens/
│   │                   ├── HomeScreen.kt       # Pantalla móvil, tarjetas y reproductor inferior
│   │                   └── SettingsDialog.kt   # Configuración de IP/URL y modo local
│   ├── build-apk.sh                    # Script para compilar Debug y Release desde terminal
│   ├── install-apk.sh                  # Instalación automática con adb simulando Google Play
│   └── README.md                       # Documentación completa del proyecto
```

---

## 🐳 2. Entorno Local Docker (Puerto 4112)

El servidor mock permite validar la conectividad de los streams y gestionar el archivo maestro de emisoras sin depender de compilaciones en Android.

### 2.1 Levantar el contenedor

Desde la raíz del proyecto, ejecuta:

```bash
docker compose up -d --build
```

O entrando en el directorio del servidor:

```bash
cd server-mock
docker compose up -d --build
```

### 2.2 Verificación de funcionamiento

1. **Panel Web de Pruebas:** Abre en tu navegador [http://localhost:4112/](http://localhost:4112/).
   - **Reproductor HTML5:** Prueba en tiempo real el audio de cada emisora con indicador "EN VIVO" y barras de ecualización animadas.
   - **Verificador de Stream ("Probar"):** Mide en milisegundos la respuesta de cada URL de audio y detecta bloqueos de CORS o certificados SSL caducados.
   - **Modo Claro / Oscuro:** Alterna el tema con persistencia en `localStorage`.
   - **Gestión de Emisoras:** Añade nuevas emisoras, edita los streams existentes o pulsa *"Restaurar"* para volver al catálogo inicial.
2. **Endpoint API JSON:** Comprueba la respuesta con `curl`:
   ```bash
   curl -i http://localhost:4112/api/stations
   ```
   *Responde con cabeceras CORS (`Access-Control-Allow-Origin: *`) y el array completo de emisoras.*

---

## 📱 3. Aplicación Android & Android Auto

### 3.1 Características Principales
- **Interfaz Móvil (Jetpack Compose Material 3):**
  - Soporte automático y manual para **Modo Claro y Modo Oscuro**.
  - Tarjetas con logotipos de alta calidad (cargados mediante Coil con decodificador SVG).
  - Indicador parpadeante **"EN VIVO"**.
  - Reproductor persistente en la parte inferior con control de reproducción y metadatos ICY (título de canción y artista actual).
  - Buscador en tiempo real por nombre, género o país.
- **Jerarquía Nativa para Android Auto (`MediaLibraryService`):**
  - ⭐ **Favoritas:** Aparece en primer lugar en la pantalla del salpicadero del coche.
  - 📻 **Por Género:** Subcarpetas agrupadas por estilo musical (*Indie / Alternativa, Éxitos / Pop, Dance / Remember, Flamenco / Rumba, etc.*).
  - 🌍 **Por País:** Subcarpetas por origen (*España, Internacional*).
- **Asistente de Voz (Google Assistant):**
  - Soporte para órdenes de voz: *"Pon La Indie"*, *"Reproduce Activa FM"*, etc., mediante resolución automática en `onSetMediaItems`.
- **Controles del Volante y Bluetooth:**
  - Soporte para botones físicos de Play/Pause, Siguiente emisora y Emisora anterior.
- **Doble Modo Red / Offline:**
  - En modo **Debug**, consume el catálogo en vivo desde `http://10.0.2.2:4112/api/stations` (o la IP local del PC configurable en la pantalla de Ajustes).
  - En modo **Release**, incluye un fallback local autónomo (`assets/stations.json`) que garantiza que la app funcione en el coche sin necesidad de tener el PC encendido.

---

## 🔨 4. Compilación del APK con Gradle

El proyecto incluye configuración de firma de prueba en `android-app/app/release.keystore` para que el APK de Release sea instalable y ejecutable inmediatamente.

### Opción A: Usando el script automatizado (Recomendado)

```bash
# Compilar variante Debug
./build-apk.sh debug

# Compilar variante Release (firmado y optimizado con R8)
./build-apk.sh release

# Compilar ambas variantes
./build-apk.sh all
```

### Opción B: Usando Gradle directamente

```bash
cd android-app

# Compilar APK Debug
./gradlew assembleDebug

# Compilar APK Release firmado
./gradlew assembleRelease
```

Los APKs generados se ubican en:
- **Debug:** `android-app/app/build/outputs/apk/debug/app-debug.apk`
- **Release:** `android-app/app/build/outputs/apk/release/app-release.apk`

---

## 📲 5. Instalación en el Móvil y Activación en Android Auto

Google impone restricciones de seguridad estrictas en Android Auto para que las apps que no proceden directamente de Google Play Store puedan mostrarse en la pantalla del vehículo. Sigue estos pasos para habilitarla en tu coche:

### Paso 1: Instalar el APK simulando Google Play Store

Conecta el teléfono móvil al ordenador mediante cable USB con la depuración USB habilitada y ejecuta:

```bash
./install-apk.sh
```

O manualmente con `adb`:

```bash
adb install -r -i "com.android.vending" android-app/app/build/outputs/apk/release/app-release.apk
```

> **¿Por qué `-i "com.android.vending"`?**  
> Este parámetro indica al sistema operativo Android que el instalador de la app ha sido la tienda oficial de Google Play, lo que autoriza al subsistema de Android Auto a confiar en el servicio multimedia.

---

### Paso 2: Activar "Orígenes desconocidos" en los Ajustes de Android Auto

Es un paso obligatorio que se realiza **una sola vez** en el teléfono móvil:

1. En tu teléfono móvil, ve a **Ajustes > Aplicaciones > Android Auto** (o busca *"Android Auto"* en el buscador de Ajustes).
2. Haz scroll hasta el final de la pantalla donde aparece el apartado **Versión**.
3. **Toca repetidamente 10 veces sobre la palabra "Versión"** hasta que aparezca una ventana emergente que dice: *"¿Permitir los ajustes de desarrollo?"*.
4. Pulsa en **Aceptar**.
5. Toca en el botón de **menú de 3 puntos verticales** en la esquina superior derecha y selecciona **"Ajustes de desarrollador"**.
6. Busca y marca la casilla **"Orígenes desconocidos"** (*Unknown sources*).
7. En el apartado **"Modo de aplicación"**, asegúrate de que esté marcado **"Versión comercial"** o **"Desarrollador"**.

---

### Paso 3: Conectar al coche

1. Conecta el teléfono al coche mediante cable USB o conexión inalámbrica de Android Auto.
2. Abre el menú de aplicaciones del salpicadero de tu coche.
3. Selecciona el icono de **RadioAuto ES** en la sección de aplicaciones de audio/música.
4. Verás las carpetas:
   - ⭐ **Favoritas**
   - 📻 **Por Género**
   - 🌍 **Por País**
5. ¡Selecciona cualquier emisora o usa los controles del volante para cambiar de dial!

---

## 📻 6. Catálogo de Emisoras Inicial

| Emisora | Género | País | URL de Streaming |
| :--- | :--- | :--- | :--- |
| **La Indie** | Indie / Alternativa | España | `https://stream.emisorasmusicales.net/listen/la_indie/laindie.mp3` |
| **La Indie Madrid** | Indie / Alternativa | España | `https://stream.emisorasmusicales.net/listen/la_indie/laindie-madrid.mp3` |
| **La Indie Valencia** | Indie / Alternativa | España | `https://stream.emisorasmusicales.net/radio/8130/laindie-valencia.mp3` |
| **Activa FM** | Éxitos / Pop | España | `https://stream.emisorasmusicales.net/listen/activa_fm/activafm.mp3` |
| **La Mega** | Latino / Urbano | España | `https://stream.emisorasmusicales.net/listen/la_mega/lamega.mp3` |
| **La Flamenca** | Flamenco / Rumba | España | `https://stream.emisorasmusicales.net/listen/la_flamenca/laflamenca.mp3` |
| **Muy Buena** | Pop Latino | España | `https://stream.emisorasmusicales.net/listen/muybuena/muybuena.mp3` |
| **Bikini FM** | Dance / Remember | España | `https://stream.emisorasmusicales.net/listen/bikini_fm/bikinifm.mp3` |
| **Esencia FM** | Oldies / Clásicos | España | `https://stream.emisorasmusicales.net/listen/esencia_fm/esenciafm.mp3` |
| **Comboi FM** | Variedad / Local | España | `https://stream.emisorasmusicales.net/listen/comboi/comboi.mp3` |
| **Gold FM** | Oldies / Clásicos | Internacional | `https://stream.emisorasmusicales.net/listen/gold_fm/goldfm.mp3` |
| **Party FM (Motiva)**| Dance / Electrónica | España | `https://stream.emisorasmusicales.net/listen/party_fm/partyfm.mp3` |
| **Cadena SER** | Noticias / Hablada | España | `https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3` |
| **Los 40 Principales**| Éxitos / Pop | España | `https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40AAC.aac` |
| **Cadena Dial** | Pop Latino | España | `https://playerservices.streamtheworld.com/api/livestream-redirect/CADENADIAL.mp3` |

---

## 🛠️ Tecnologías y Librerías Utilizadas

- **Frontend Mock & Servidor:** HTML5 semántico, CSS3 nativo con Custom Properties, JavaScript ES6+ (0 dependencias, 0 npm en cliente), Node.js HTTP estándar (puerto `4112`), Docker & Docker Compose.
- **Android UI Móvil:** Kotlin 2.0, Jetpack Compose, Material Design 3, Coil Image Loader (SVG Decoder).
- **Audio & Android Auto:** AndroidX Media3 (`media3-session`, `media3-exoplayer`, `media3-datasource-okhttp`), `MediaLibraryService`, `MediaItem`, Audio Focus Management, ICY Live Stream Metadata Extraction.
- **Persistencia & Red:** SharedPreferences / StateFlow reactivo, OkHttp, Gson.
