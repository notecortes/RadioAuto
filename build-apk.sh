#!/usr/bin/env bash
# ==============================================================================
# RadioAuto ES - Script de Compilación de APKs
# Uso:
#   ./build-apk.sh debug    -> Compila app-debug.apk
#   ./build-apk.sh release  -> Compila app-release.apk (firmado)
#   ./build-apk.sh all      -> Compila ambas variantes
# ==============================================================================

set -e

# Configurar Java si no está definido
if [ -z "$JAVA_HOME" ]; then
    if [ -d "$HOME/.jdks/temurin-21" ]; then
        export JAVA_HOME="$HOME/.jdks/temurin-21"
    fi
fi

# Configurar Android SDK si no está definido
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    fi
fi

echo "========================================================"
echo " RadioAuto ES - Compilador Gradle"
echo " JAVA_HOME:    $JAVA_HOME"
echo " ANDROID_HOME: $ANDROID_HOME"
echo "========================================================"

TARGET="${1:-all}"
cd "$(dirname "$0")/android-app"

case "$TARGET" in
    debug)
        echo ">> Compilando APK Debug..."
        ./gradlew assembleDebug
        echo ">> ¡APK Debug generado con éxito!"
        echo "   Ubicación: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        echo ">> Compilando APK Release firmado..."
        ./gradlew assembleRelease
        echo ">> ¡APK Release generado con éxito!"
        echo "   Ubicación: $(pwd)/app/build/outputs/apk/release/app-release.apk"
        ;;
    all)
        echo ">> Compilando variantes Debug y Release..."
        ./gradlew assembleDebug assembleRelease
        echo ">> ¡Ambos APKs generados con éxito!"
        echo "   Debug:   $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
        echo "   Release: $(pwd)/app/build/outputs/apk/release/app-release.apk"
        ;;
    clean)
        echo ">> Limpiando directorios de compilación..."
        ./gradlew clean
        ;;
    *)
        echo "Opción no reconocida: $TARGET"
        echo "Uso: $0 [debug|release|all|clean]"
        exit 1
        ;;
esac
