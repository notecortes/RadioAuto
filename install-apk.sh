#!/usr/bin/env bash
# ==============================================================================
# RadioAuto ES - Script de Instalación ADB
# Instala el APK simulando origen de Google Play ("com.android.vending")
# para que Android Auto reconozca la aplicación inmediatamente
# ==============================================================================

set -e

APK_PATH="android-app/app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo ">> El APK de release no existe. Compilándolo primero..."
    ./build-apk.sh release
fi

echo "========================================================"
echo " Instalando RadioAuto ES en dispositivo Android vía ADB"
echo " Origen simulado: com.android.vending (Google Play Store)"
echo " APK: $APK_PATH"
echo "========================================================"

# Verificar dispositivos conectados
adb devices

# Instalación estándar compatible con todos los dispositivos y versiones de Android
adb install -r "$APK_PATH"

echo "========================================================"
echo " ¡Instalación completada con éxito!"
echo " Recuerda activar en tu móvil:"
echo " 1. Abre la app Android Auto en tu teléfono."
echo " 2. Baja hasta 'Versión' y pulsa 10 veces seguidas."
echo " 3. En el menú superior (3 puntos) > Ajustes de desarrollador."
echo " 4. Marca la casilla 'Orígenes desconocidos'."
echo " 5. ¡Conecta el móvil al coche y disfruta de RadioAuto ES!"
echo "========================================================"
