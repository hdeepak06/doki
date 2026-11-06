# ProGuard rules for DOKI AI Assistant

# Kotlin Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Picovoice Porcupine
-keep class ai.picovoice.porcupine.** { *; }

# Google Generative AI
-keep class com.google.ai.client.generativeai.** { *; }

# Ktor Client
-keep class io.ktor.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep models & entities
-keep class com.doki.ai.data.local.entity.** { *; }
-keep class com.doki.ai.domain.model.** { *; }
-keep class com.doki.ai.core.ai.tools.** { *; }

