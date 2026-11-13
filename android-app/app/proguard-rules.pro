# Reglas ProGuard y R8 de optimización para RadioAuto ES

# 1. Mantener modelos de datos JSON
-keep class com.radioauto.es.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 2. AndroidX Media3 y servicios para Android Auto
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# 3. Networking y utilidades
-dontwarn okhttp3.**
-dontwarn okio.**

# 4. Coil para carga de imágenes y SVG
-dontwarn coil.**
-keep class coil.** { *; }
