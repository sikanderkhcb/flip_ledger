# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { @kotlinx.serialization.* <methods>; }
# SQLDelight
-keep class app.cash.sqldelight.** { *; }
# Coroutines
-dontwarn kotlinx.coroutines.**
