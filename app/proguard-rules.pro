# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Moshi (JSON) ---
# Moshi's codegen adapters are looked up by name at runtime, so the annotated
# model classes and their generated *JsonAdapter classes must keep their names.
-keepnames @com.squareup.moshi.JsonClass class *
-keep class * extends com.squareup.moshi.JsonAdapter {
    <init>(...);
}
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
}
-if @com.squareup.moshi.JsonClass class *
-keep class <1>_*JsonAdapter {
    <init>(...);
}
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# --- Retrofit / OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- SnakeYAML ---
# Only used to load/dump generic Map/List structures (SafeConstructor), never
# app-specific bean classes, but the library itself needs its own classes intact.
-dontwarn org.yaml.snakeyaml.**
-keep class org.yaml.snakeyaml.** { *; }

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**

# --- androidx.security.crypto (Google Tink) ---
# Tink references errorprone's compile-only annotations, which aren't on the
# runtime classpath; harmless to drop.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# --- App model classes crossing serialization boundaries (Moshi / Room / YAML) ---
-keep class com.rrajath.bloggo.data.network.** { *; }
-keep class com.rrajath.bloggo.data.local.** { *; }
-keep class com.rrajath.bloggo.domain.** { *; }