El sistema multimedia de **BYD (DiLink)** funciona directamente sobre una versión completa de **Android** (como una tablet integrada en el salpicadero), por lo que puede instalar y ejecutar archivos `.apk` de forma nativa sin depender de un teléfono ni de Android Auto.

Para instalar tu app directamente en el coche de tu padre necesitas preparar un pendrive USB y habilitar la instalación de aplicaciones externas en la pantalla del vehículo.

### Requisitos previos importantes

* **Interfaz de pantalla completa:** Como en el BYD la app se abrirá directamente en la pantalla horizontal del coche (y no a través de la plantilla de Android Auto), asegúrate de que la interfaz principal de la app que te genere Antigravity tenga botones grandes y se vea bien en horizontal (*landscape*).
* **Conexión a Internet en el coche:** El BYD necesitará tener datos móviles activos (la tarjeta SIM integrada del coche con plan de datos o compartirle Wi-Fi desde un móvil).

1. **Preparar el pendrive USB:** 2 min.
Formatea una memoria USB en formato **FAT32** o **exFAT** (si el coche tiene puerto USB-C en la consola central, usa un pendrive USB-C o un adaptador). Copia el archivo `app-release.apk` compilado a la carpeta raíz del pendrive.
*Verification:* El archivo `.apk` es visible directamente al abrir la unidad USB en tu ordenador.


2. **Conectar el USB al puerto de datos del BYD:** 1 min.
Enciende el coche y conecta el pendrive en el puerto USB marcado con el símbolo de **transferencia de datos** (normalmente ubicado en la bandeja inferior de la consola central, junto al de carga).
*Verification:* En la barra superior de la pantalla del BYD aparece un icono de unidad USB conectada.


3. **Localizar el APK en el Gestor de Archivos:** 2 min.
En la pantalla del BYD, abre el cajón de aplicaciones y entra en **Gestor de archivos** (o *File Manager*). Selecciona la unidad **USB** en el menú lateral izquierdo y pulsa sobre tu archivo `app-release.apk`.
*Verification:* Aparece una ventana emergente indicando que por seguridad el sistema no tiene permiso para instalar aplicaciones desconocidas desde esa fuente.


4. **Autorizar orígenes desconocidos e instalar:** 2 min.
Pulsa en **Ajustes** en esa misma ventana emergente y activa el interruptor **Permitir desde esta fuente** para el Gestor de archivos. Vuelve atrás y pulsa **Instalar**.
*Verification:* La instalación finaliza mostrando el botón **Abrir** y el icono de "RadioAuto ES" aparece en el menú principal de aplicaciones del BYD.


*(Nota: En algunas versiones recientes del software de BYD, el sistema pide una contraseña al intentar instalar un `.apk` desde el gestor de archivos para bloquear instalaciones no autorizadas. Si te aparece ese cuadro de contraseña, el código de fábrica estándar en la mayoría de modelos BYD es **`20230607`** o **`123456`** —o el año/mes de la versión del sistema—).*

1. ¿El uso de datos es bajo? (Consumo de datos móviles)
Concepto	Consumo estimado	Comportamiento
Streaming de audio (128 kbps)	~57 MB por hora	Equivale a 1 MB por minuto de audio continuo en alta fidelidad. Es entre 3 y 5 veces menor que ver un vídeo en baja calidad.
Búfer de ExoPlayer	Máx. 15 segundos (~240 KB)	Configurado en RadioMediaService.kt: a diferencia de otras apps que descargan minutos por adelantado, aquí nunca se precarga más de 15 segundos. Si cambias de emisora, solo se descartan unos pocos kilobytes.
Catálogo de emisoras	0 MB de red	El catálogo maestro de 310 emisoras va embebido localmente en el APK (assets/stations.json). En modo normal no descarga nada de internet para mostrar la lista.
Logotipos	Caché en disco	Gestionados por Coil con caché local; se descargan una sola vez y se sirven desde la memoria del teléfono.