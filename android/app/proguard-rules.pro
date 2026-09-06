# R8 is enabled for release builds. Everything below exists because something in this app
# resolves names at runtime rather than at compile time, so R8 cannot see the use site.

# Crashlytics stack traces are unreadable without these, and R8 writes mapping.txt either way.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Reflection and generic-signature metadata that Firestore, Moshi and Retrofit all read.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*,RuntimeVisibleAnnotations,AnnotationDefault

# --- Firestore entities -------------------------------------------------------------------
# Firestore maps documents onto these by reflecting over property names, so a renamed field
# silently deserialises to its default instead of failing. Keep the names and the no-arg
# constructor the mapper needs.
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** {
    <init>();
    <fields>;
    void set*(***);
    *** get*();
}
-keepnames class com.example.data.model.**
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# --- Moshi (reflective adapters via KotlinJsonAdapterFactory) -----------------------------
# The PostgREST DTOs have no generated adapters, so Moshi reads Kotlin metadata at runtime.
-keep class com.example.data.supabase.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
    @com.squareup.moshi.Json <fields>;
}
-dontwarn com.squareup.moshi.**
-dontwarn org.jetbrains.annotations.**

# --- Retrofit / OkHttp -------------------------------------------------------------------
# Retrofit builds its implementations from the interface's annotations and generic return types.
-keep,allowobfuscation interface com.example.data.supabase.SupabaseApiService { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers,allowshrinking class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# --- Kotlin coroutines ------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# --- Firebase ---------------------------------------------------------------------------
# Messaging instantiates the service by name from the manifest; the manifest keeps it, but the
# FCM payload keys are read reflectively in places.
-keep class com.example.service.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
