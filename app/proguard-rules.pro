# R8 rules for the release build. Most libraries (Retrofit, OkHttp, Room, Coil,
# Compose) ship their own consumer rules, and the Kotlin serialization plugin
# emits keep rules for @Serializable classes automatically. The rules below are
# defensive backstops for the GitHub DTOs and kotlinx.serialization internals.

# Keep the GitHub API DTOs and their generated serializers.
-keep,includedescriptorclasses class com.rrajath.bloggo.data.github.**$$serializer { *; }
-keepclassmembers class com.rrajath.bloggo.data.github.** {
    *** Companion;
}
-keepclasseswithmembers class com.rrajath.bloggo.data.github.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# kotlinx.serialization core keep rules (backstop for the plugin-generated ones).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Tink (pulled in by androidx.security-crypto for the encrypted PAT store)
# references errorprone annotations that are not on the runtime classpath.
-dontwarn com.google.errorprone.annotations.**
