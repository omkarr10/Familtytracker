# TrackIt ProGuard Rules

# Supabase
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable classes
-keep,includedescriptorclasses class com.familytracker.**$$serializer { *; }
-keepclassmembers class com.familytracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.familytracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase { void <init>(); }
-dontwarn androidx.room.paging.**

# Missing classes (JVM/Desktop dependencies not needed on Android)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.**

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
